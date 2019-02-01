package io.taucoin.facade;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.core.PendingState;
import io.taucoin.listener.CompositeEthereumListener;
import io.taucoin.listener.EthereumListener;
import io.taucoin.manager.AdminInfo;
import io.taucoin.manager.BlockLoader;
import io.taucoin.manager.WorldManager;
import io.taucoin.forge.BlockForger;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.peerdiscovery.PeerInfo;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.net.server.PeerServer;
import io.taucoin.net.submit.NewBlockHeaderBroadcaster;
import io.taucoin.net.submit.NewBlockHeaderTask;
import io.taucoin.net.submit.TransactionExecutor;
import io.taucoin.net.submit.TransactionTask;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
//import org.springframework.context.ApplicationContext;

//import org.springframework.util.concurrent.FutureAdapter;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Roman Mandeleil
 * @since 27.07.2014
 */

public class TaucoinImpl implements Taucoin {

    private static final Logger logger = LoggerFactory.getLogger("facade");
    private static final Logger gLogger = LoggerFactory.getLogger("general");

    @Inject
    WorldManager worldManager;

    @Inject
    AdminInfo adminInfo;

    @Inject
    ChannelManager channelManager;

    @Inject
    PeerServer peerServer;

    //@Inject
    //ApplicationContext ctx;

    @Inject
    BlockLoader blockLoader;

    @Inject
    PendingState pendingState;

    @Inject
    SystemProperties config;

    @Inject
    CompositeEthereumListener compositeEthereumListener;

    public TaucoinImpl() {
        System.out.println();
    }

    @PostConstruct
    public void init() {
        if (config.listenPort() > 0) {
            Executors.newSingleThreadExecutor().submit(
                    new Runnable() {
                        public void run() {
                            peerServer.start(config.listenPort());
                        }
                    }
            );
        }
        //compositeEthereumListener.addListener(gasPriceTracker);

        gLogger.info("EthereumJ node started: enode://" + Hex.toHexString(config.nodeId()) + "@" + config.externalIp() + ":" + config.listenPort());
    }

    /**
     * Find a peer but not this one
     *
     * @param peer - peer to exclude
     * @return online peer
     */
    @Override
    public PeerInfo findOnlinePeer(PeerInfo peer) {
        Set<PeerInfo> excludePeers = new HashSet<>();
        excludePeers.add(peer);
        return findOnlinePeer(excludePeers);
    }

    @Override
    public PeerInfo findOnlinePeer() {
        Set<PeerInfo> excludePeers = new HashSet<>();
        return findOnlinePeer(excludePeers);
    }

    @Override
    public PeerInfo findOnlinePeer(Set<PeerInfo> excludePeers) {
        logger.info("Looking for online peers...");

        final EthereumListener listener = worldManager.getListener();
        listener.trace("Looking for online peer");

        worldManager.startPeerDiscovery();

        final Set<PeerInfo> peers = worldManager.getPeerDiscovery().getPeers();
        for (PeerInfo peer : peers) { // it blocks until a peer is available.
            if (peer.isOnline() && !excludePeers.contains(peer)) {
                logger.info("Found peer: {}", peer.toString());
                listener.trace(String.format("Found online peer: [ %s ]", peer.toString()));
                return peer;
            }
        }
        return null;
    }

    @Override
    public PeerInfo waitForOnlinePeer() {
        PeerInfo peer = null;
        while (peer == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            peer = this.findOnlinePeer();
        }
        return peer;
    }

    @Override
    public Set<PeerInfo> getPeers() {
        return worldManager.getPeerDiscovery().getPeers();
    }

    @Override
    public void startPeerDiscovery() {
        worldManager.startPeerDiscovery();
    }

    @Override
    public void stopPeerDiscovery() {
        worldManager.stopPeerDiscovery();
    }

    @Override
    public void connect(InetAddress addr, int port, String remoteId) {
        connect(addr.getHostName(), port, remoteId);
    }

    @Override
    public void connect(final String ip, final int port, final String remoteId) {
        logger.info("Connecting to: {}:{}", ip, port);
        final PeerClient peerClient = new PeerClient();// ctx.getBean(PeerClient.class);
        peerClient.connectAsync(ip, port, remoteId, false);
    }

    @Override
    public void connect(Node node) {
        connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
    }

    @Override
    public io.taucoin.facade.Blockchain getBlockchain() {
        return (io.taucoin.facade.Blockchain)worldManager.getBlockchain();
    }

    @Override
    public io.taucoin.db.BlockStore getBlockStore(){
        return (io.taucoin.db.BlockStore) worldManager.getBlockStore();
    }

    @Override
    public ImportResult addNewMinedBlock(Block block) {
        ImportResult importResult = worldManager.getBlockchain().tryToConnect(block);
        if (importResult == ImportResult.IMPORTED_BEST) {
            channelManager.sendNewBlock(block, null);
        }
        return importResult;
    }

    @Override
    public boolean addNewForgedBlockHeader(BlockHeader header) {
        // TODO: import this block header into blockchain.
        boolean importResult = false;
        if (importResult) {
            NewBlockHeaderTask task = new NewBlockHeaderTask(header, channelManager);
            NewBlockHeaderBroadcaster.instance.submitNewBlockHeader(task);
        }

        return importResult;
    }

    @Override
    public BlockForger getBlockForger() {
        return new BlockForger();// ctx.getBean(BlockForger.class);
    }

    @Override
    public void addListener(EthereumListener listener) {
        worldManager.addListener(listener);
    }

    @Override
    public void close() {
        worldManager.close();
    }

    @Override
    public PeerClient getDefaultPeer() {

        PeerClient peer = worldManager.getActivePeer();
        if (peer == null) {

            peer = new PeerClient();
            worldManager.setActivePeer(peer);
        }
        return peer;
    }

    @Override
    public boolean isConnected() {
        return worldManager.getActivePeer() != null;
    }

    @Override
    public Transaction createTransaction(byte version,
                                         byte option,
                                         byte[] timeStamp,
                                         byte[] toAddress,
                                         byte[] amount,
                                         byte[] fee) {

        return new Transaction(version, option, timeStamp, toAddress, amount, fee);
    }


    @Override
    public Future<Transaction> submitTransaction(Transaction transaction) {

        boolean submitResult = pendingState.addPendingTransaction(transaction);
        if (submitResult) {
            TransactionTask transactionTask = new TransactionTask(transaction, channelManager);

            final Future<List<Transaction>> listFuture =
                    TransactionExecutor.instance.submitTransaction(transactionTask);

            //return new FutureAdapter<Transaction, List<Transaction>>(listFuture) {
            //    @Override
            //    protected Transaction adapt(List<Transaction> adapteeResult) throws ExecutionException {
            //        return adapteeResult.get(0);
        }
        return null;
    }

    @Override
    public Wallet getWallet() {
        return worldManager.getWallet();
    }


    @Override
    public io.taucoin.facade.Repository getRepository() {
        return worldManager.getRepository();
    }

    @Override
    public io.taucoin.facade.Repository getPendingState() {
        return (io.taucoin.facade.Repository) worldManager.getPendingState().getRepository();
    }

    @Override
    public AdminInfo getAdminInfo() {
        return adminInfo;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    @Override
    public List<Transaction> getWireTransactions() {
        return worldManager.getPendingState().getWireTransactions();
    }

    @Override
    public List<Transaction> getPendingStateTransactions() {
        return worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public BlockLoader getBlockLoader(){
        return  blockLoader;
    }

}
