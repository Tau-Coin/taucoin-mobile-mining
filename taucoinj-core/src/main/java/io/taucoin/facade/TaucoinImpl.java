package io.taucoin.facade;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.core.PendingState;
import io.taucoin.debug.RefWatcher;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.manager.BlockLoader;
import io.taucoin.manager.WorldManager;
import io.taucoin.forge.BlockForger;
import io.taucoin.http.RequestManager;
import io.taucoin.http.submit.TransactionExecutor;
import io.taucoin.http.submit.TransactionTask;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.peerdiscovery.PeerInfo;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.rlpx.NodeType;
import io.taucoin.util.ByteUtil;
import org.apache.commons.collections4.list.TransformedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
import javax.inject.Provider;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Roman Mandeleil
 * @since 27.07.2014
 */

public class TaucoinImpl implements Taucoin {

    private static final Logger logger = LoggerFactory.getLogger("facade");
    private static final Logger gLogger = LoggerFactory.getLogger("general");
    public static final String TRANSACTION_RELAYTOREMOTE = "transaction has been relayed to remote peer";
    public static final String TRANSACTION_SUBMITSUCCESS = "submit transaction success,wait to confirm";
    public static final String TRANSACTION_SUBMITFAIL = "submission failure, please relay to rpc";
    protected WorldManager worldManager;

    protected BlockLoader blockLoader;

    protected PendingState pendingState;

    protected BlockForger blockForger;

    protected RequestManager requestManager;

    protected RefWatcher refWatcher;

    @Inject
    public TaucoinImpl(WorldManager worldManager,
            BlockLoader blockLoader, PendingState pendingState, BlockForger blockForger,
            RequestManager requestManager, RefWatcher refWatcher) {
        this.worldManager = worldManager;
        this.blockLoader = blockLoader;
        this.pendingState = pendingState;
        this.blockForger = blockForger;
        this.requestManager = requestManager;
        this.refWatcher = refWatcher;
        this.blockForger.setTaucoin(this);
        this.blockForger.init();
    }

    public TaucoinImpl() {
        System.out.println();
    }

    public void init() {
        // TODO: initialization logic
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
        return null;
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
    public void startSync() {
        worldManager.startSync();
    }

    @Override
    public void stopSync() {
        worldManager.stopSync();
    }

    @Override
    public void connect(InetAddress addr, int port, String remoteId) {
        connect(addr.getHostName(), port, remoteId);
    }

    @Override
    public void connect(final String ip, final int port, final String remoteId) {
        logger.info("Connecting to: {}:{}", ip, port);
    }

    @Override
    public void connect(Node node) {
        connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
    }

    @Override
    public WorldManager getWorldManager() {
        return this.worldManager;
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
            requestManager.submitNewBlock(block);
        }
        return importResult;
    }

    @Override
    public BlockForger getBlockForger() {
        return blockForger;
    }

    @Override
    public void addListener(TaucoinListener listener) {
        worldManager.addListener(listener);
    }

    @Override
    public void close() {
        // Firstly, stop forging anyway.
        if (blockForger.isForging()) {
            blockForger.stopForging();
        }
        worldManager.close();

        refWatcher.watch(worldManager);
        refWatcher.watch(blockLoader);
        refWatcher.watch(blockForger);
        refWatcher.watch(this);
    }

    @Override
    public PeerClient getDefaultPeer() {
        return null;
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
    public Transaction submitTransaction(Transaction transaction) {
         boolean submitResult = pendingState.addPendingTransaction(transaction);
         if (submitResult) {
             boolean sendResult = requestManager.submitNewTransaction(transaction);
             if (sendResult) {
                 return transaction;
             }
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
    public io.taucoin.core.PendingState getPendingState() {
        return worldManager.getPendingState();
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
