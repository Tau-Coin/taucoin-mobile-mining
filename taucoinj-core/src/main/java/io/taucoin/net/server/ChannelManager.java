package io.taucoin.net.server;

import org.apache.commons.collections4.map.LRUMap;
import io.taucoin.config.NodeFilter;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockWrapper;
import io.taucoin.core.Transaction;
import io.taucoin.core.PendingState;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.listener.TaucoinListener;

import io.taucoin.net.message.ReasonCode;
import io.taucoin.sync.SyncManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.net.message.ReasonCode.DUPLICATE_PEER;
import static io.taucoin.net.message.ReasonCode.TOO_MANY_PEERS;
import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Roman Mandeleil
 * @since 11.11.2014
 */
@Singleton
public class ChannelManager {

    private static final Logger logger = LoggerFactory.getLogger("net");

    // If the inbound peer connection was dropped by us with a reason message
    // then we ban that peer IP on any connections for some time to protect from
    // too active peers
    private static final int inboundConnectionBanTimeout = 10 * 1000;

    private List<Channel> newPeers = new CopyOnWriteArrayList<>();
    private final Map<ByteArrayWrapper, Channel> newPeersMap = Collections.synchronizedMap(new HashMap<ByteArrayWrapper, Channel>());
    private final Map<ByteArrayWrapper, Channel> activePeers = Collections.synchronizedMap(new HashMap<ByteArrayWrapper, Channel>());

    private ScheduledExecutorService mainWorker = Executors.newSingleThreadScheduledExecutor();
    private int maxActivePeers;
    private Map<InetAddress, Date> recentlyDisconnected = Collections.synchronizedMap(new LRUMap<InetAddress, Date>(500));
    private NodeFilter trustedPeers;

    /**
     * Queue with new blocks from other peers
     */
    private BlockingQueue<BlockWrapper> newForeignBlocks = new LinkedBlockingQueue<>();

    /**
     * Queue with new peers used for after channel init tasks
     */
    private BlockingQueue<Channel> newActivePeers = new LinkedBlockingQueue<>();

    private Thread blockDistributeThread;
    private Thread txDistributeThread;

    SyncManager syncManager;

    private PendingState pendingState;
    TaucoinListener listener;

    @Inject
    public ChannelManager(TaucoinListener listener, SyncManager syncManager, PendingState pendingState) {
        this.listener = listener;
        this.syncManager = syncManager;
        this.syncManager.setChannelManager(this);
        this.pendingState = pendingState;
    }

    public void init() {
        maxActivePeers = CONFIG.maxActivePeers();
        trustedPeers = CONFIG.peerTrusted();
        mainWorker.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    processNewPeers();
                } catch (Throwable t) {
                    logger.error("Error", t);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        // Resending new blocks to network in loop
        this.blockDistributeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newBlocksDistributeLoop();
            }
        }, "NewSyncThreadBlocks");
        this.blockDistributeThread.start();

        // Resending pending txs to newly connected peers
        this.txDistributeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newTxDistributeLoop();
            }
        }, "NewSyncThreadTx");
        this.txDistributeThread.start();
    }

    private void processNewPeers() {
        if (newPeers.isEmpty()) return;

        List<Channel> processed = new ArrayList<>();

        int addCnt = 0;
        for(Channel peer : newPeers) {

            if(peer.isProtocolsInitialized()) {

                if (!activePeers.containsKey(peer.getNodeIdWrapper())) {
                    if (!peer.isActive() &&
                        activePeers.size() >= maxActivePeers &&
                        !trustedPeers.accept(peer.getNode())) {

                        // restricting inbound connections unless this is a trusted peer

                        disconnect(peer, TOO_MANY_PEERS);
                    } else {
                        process(peer);
                        addCnt++;
                    }
                } else {
                    disconnect(peer, DUPLICATE_PEER);
                }

                processed.add(peer);
            }
        }

        logger.info("New peers processed: " + processed + ", active peers added: " + addCnt + ", total active peers: " + activePeers.size());

        newPeers.removeAll(processed);
        for (Channel channel : processed) {
            newPeersMap.values().remove(channel);
        }
    }

    private void disconnect(Channel peer, ReasonCode reason) {
        logger.debug("Disconnecting peer with reason " + reason + ": " + peer);
        peer.disconnect(reason);
        recentlyDisconnected.put(peer.getInetSocketAddress().getAddress(), new Date());
    }

    public TaucoinListener getListener() {
        return listener;
    }

    public boolean isRecentlyDisconnected(InetAddress peerAddr) {
        Date disconnectTime = recentlyDisconnected.get(peerAddr);
        if (disconnectTime != null &&
                System.currentTimeMillis() - disconnectTime.getTime() < inboundConnectionBanTimeout) {
            return true;
        } else {
            recentlyDisconnected.remove(peerAddr);
            return false;
        }
    }

    private void process(Channel peer) {
        if(peer.hasEthStatusSucceeded()) {
            // prohibit transactions processing until main sync is done
            if (syncManager.isSyncDone()) {
                peer.onSyncDone();
                // So we could perform some tasks on recently connected peer
                newActivePeers.add(peer);
            }
            syncManager.addPeer(peer);
            activePeers.put(peer.getNodeIdWrapper(), peer);
        }
    }

    public Channel getActivePeer(byte[] nodeId) {
        return activePeers.get(new ByteArrayWrapper(nodeId));
    }

    /**
     * Propagates the transactions message across active peers with exclusion of
     * 'receivedFrom' peer.
     * @param tx  transactions to be sent
     * @param receivedFrom the peer which sent original message or null if
     *                     the transactions were originated by this peer
     */
    public void sendTransaction(List<Transaction> tx, Channel receivedFrom) {
        synchronized (activePeers) {
            for (Channel channel : activePeers.values()) {
                if (channel != receivedFrom) {
                    channel.sendTransaction(tx);
                }
            }
        }
    }

    /**
     * Propagates the new block message across active peers with exclusion of
     * 'receivedFrom' peer.
     * @param block  new Block to be sent
     * @param receivedFrom the peer which sent original message or null if
     *                     the block has been mined by us
     */
    public void sendNewBlock(Block block, Channel receivedFrom) {
        synchronized (activePeers) {
            for (Channel channel : activePeers.values()) {
                if (channel != receivedFrom) {
                    channel.sendNewBlock(block);
                }
            }
        }
    }

    /**
     * Propagates the new block header message across active peers with exclusion of
     * 'receivedFrom' peer.
     * @param block  new Block to be sent
     * @param receivedFrom the peer which sent original message or null if
     *                     the block has been mined by us
     */
    public void sendNewBlockHeader(BlockHeader header, Channel receivedFrom) {
        synchronized (activePeers) {
            for (Channel channel : activePeers.values()) {
                if (channel != receivedFrom) {
                    channel.sendNewBlockHeader(header);
                }
            }
        }
    }


    /**
     * Called on new blocks received from other peers
     * @param blockWrapper  Block with additional info
     */
    public void onNewForeignBlock(BlockWrapper blockWrapper) {
        newForeignBlocks.add(blockWrapper);
    }

    /**
     * Processing new blocks received from other peers from queue
     */
    private void newBlocksDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            BlockWrapper wrapper = null;
            try {
                wrapper = newForeignBlocks.take();
                Channel receivedFrom = getActivePeer(wrapper.getNodeId());
                sendNewBlock(wrapper.getBlock(), receivedFrom);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (wrapper != null) {
                    logger.error("Error broadcasting new block {}: ", wrapper.getBlock().toString(), e);
                    logger.error("Block dump: {}", wrapper.getBlock());
                } else {
                    logger.error("Error broadcasting unknown block", e);
                }
            }
        }
    }

    /**
     * Sends all pending txs to new active peers
     */
    private void newTxDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Channel channel = null;
            try {
                channel = newActivePeers.take();
                List<Transaction> pendingTransactions = pendingState.getPendingTransactions();
                if (!pendingTransactions.isEmpty()) {
                    channel.sendTransactionsCapped(pendingTransactions);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (channel != null) {
                    logger.error("Error sending transactions to peer {}: ", channel.getNode().getHexIdShort(), e);
                } else {
                    logger.error("Unknown error when sending transactions to new peer", e);
                }
            }
        }
    }


    public void add(Channel peer) {
        newPeers.add(peer);
        newPeersMap.put(peer.getNodeIdWrapper(), peer);
    }

    public boolean isPeerExist(byte[] nodeId) {
        if (nodeId == null) {
            return false;
        }
        ByteArrayWrapper key = new ByteArrayWrapper(nodeId);
        return newPeersMap.containsKey(key) || activePeers.containsKey(key);
    }

    // Total acount of penging and active peers
    public int getAllPeersCount() {
        return newPeersMap.size() + activePeers.size();
    }

    public void notifyDisconnect(Channel channel) {
        logger.debug("Peer {}: notifies about disconnect", channel.getPeerIdShort());
        channel.onDisconnect();
        syncManager.onDisconnect(channel);
        activePeers.values().remove(channel);
        newPeers.remove(channel);
        newPeersMap.values().remove(channel);
    }

    public void onSyncDone() {

        synchronized (activePeers) {
            for (Channel channel : activePeers.values())
                channel.onSyncDone();
        }
    }

    public void shutdown() {
        // disconnect all channels
        synchronized (activePeers) {
            for (Channel channel : activePeers.values()) {
                channel.disconnect(ReasonCode.PEER_QUITING);
            }
        }
    }
}
