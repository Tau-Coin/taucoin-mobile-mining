package io.taucoin.facade;

import io.taucoin.core.Block;
import io.taucoin.core.ImportResult;
import io.taucoin.core.Transaction;
import io.taucoin.db.BlockStore;
import io.taucoin.manager.WorldManager;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.manager.BlockLoader;
import io.taucoin.forge.BlockForger;
import io.taucoin.net.client.PeerClient;
import io.taucoin.net.peerdiscovery.PeerInfo;
import io.taucoin.net.rlpx.Node;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * @author Roman Mandeleil
 * @since 27.07.2014
 */
public interface Taucoin {

    /**
     * Find a peer but not this one
     *
     * @param excludePeer - peer to exclude
     * @return online peer if available otherwise null
     */
    PeerInfo findOnlinePeer(PeerInfo excludePeer);

    /**
     * Find an online peer but not from excluded list
     *
     * @param excludePeerSet - peers to exclude
     * @return online peer if available otherwise null
     */
    PeerInfo findOnlinePeer(Set<PeerInfo> excludePeerSet);

    /**
     * @return online peer if available
     */
    PeerInfo findOnlinePeer();


    /**
     * That block will block until online peer was found.
     *
     * @return online peer.
     */
    PeerInfo waitForOnlinePeer();

    /*
     *
     *  The set of peers returned
     *  by the method is not thread
     *  safe then should be traversed
     *  sync safe:
     *    synchronized (peers){
     *        for (final Peer peer : newPeers) {}
     *    }
     *
     */
    Set<PeerInfo> getPeers();

    void startPeerDiscovery();

    void stopPeerDiscovery();

    void startSync();

    void stopSync();

    void connect(InetAddress addr, int port, String remoteId);

    void connect(String ip, int port, String remoteId);

    void connect(Node node);

    WorldManager getWorldManager();

    Blockchain getBlockchain();

    BlockStore getBlockStore();

    void addListener(TaucoinListener listener);

    PeerClient getDefaultPeer();

    boolean isConnected();

    void close();

    /**
     * Submit new forged block into wire network.
     */
    ImportResult addNewMinedBlock(Block block);

    /**
     * Factory for general transaction
     *
     *
     * @param nonce - account nonce, based on number of transaction submited by
     *                this account
     * @param gasPrice - gas price bid by miner , the user ask can be based on
     *                   lastr submited block
     * @param gas - the quantity of gas requested for the transaction
     * @param receiveAddress - the target address of the transaction
     * @param value - the ether value of the transaction
     * @param data - can be init procedure for creational transaction,
     *               also msg data for invoke transaction for only value
     *               transactions this one is empty.
     * @return newly created transaction
     */
    Transaction createTransaction(byte version,
                                  byte option,
                                  byte[] timeStamp,
                                  byte[] toAddress,
                                  byte[] amount,
                                  byte[] fee);

    /**
     * @param transaction submit transaction to the net, return option to wait for net
     *                    return this transaction as approved
     */
    Transaction submitTransaction(Transaction transaction);

    /**
     * @return - repository for all state data.
     */
    Repository getRepository();

    /**
     * @return - pending state repository
     */
    io.taucoin.core.PendingState getPendingState();

    public void init();

    /**
     * @return - currently pending transactions received from the net
     */
    List<Transaction> getWireTransactions();


    BlockLoader getBlockLoader();

    /**
     *  Gets the Miner component
     */
    BlockForger getBlockForger();
}
