package io.taucoin.android.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;

import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.interop.Transaction;
import io.taucoin.net.peerdiscovery.PeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;


public class TaucoinConnector extends ServiceConnector {

    private static final Logger logger = LoggerFactory.getLogger("TaucoinConnector");

    public TaucoinConnector(Context context, Class serviceClass) {

        super(context, serviceClass);
    }

    public void init(String identifier, List<String> privateKeys) {

        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_INIT, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        Bundle data = new Bundle();
        data.putStringArrayList("privateKeys", (ArrayList) privateKeys);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(init) to service: " + e.getMessage());
        }
    }

    /**
     * Import block forger private key.
     *
     * @param privateKey block forger private key
     * Please handle TaucoinClientMessage.MSG_IMPORT_FORGER_PRIVKEY_RESULT
     * for result of importing forger private key.
     */
    public void importForgerPrivkey(String privateKey) {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_IMPORT_FORGER_PRIVKEY, 0, 0);
        msg.replyTo = clientMessenger;
        Bundle data = new Bundle();
        data.putString("privateKey", privateKey);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(init) to service: " + e.getMessage());
        }
    }

    /**
     * Start block forging.
     *
     * @param targetAmount block forger wanted
     * Please handle TaucoinClientMessage.MSG_START_FORGING_RESULT for result
     * of starting forging.
     */
    public void startBlockForging(int targetAmount) {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_START_FORGING, 0, 0);
        msg.replyTo = clientMessenger;
        Bundle data = new Bundle();
        data.putLong("forgedAmount", (long)targetAmount);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(init) to service: " + e.getMessage());
        }
    }

    /**
     * Stop block forging.
     */
    public void stopBlockForging(int targetAmount) {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_STOP_FORGING, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(init) to service: " + e.getMessage());
        }
    }

    /**
     * Start block syncing.
     * Please handle TaucoinClientMessage.MSG_START_SYNC_RESULT for result
     * of starting syncing.
     */
    public void startSync() {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_START_SYNC, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(init) to service: " + e.getMessage());
        }
    }

    /**
     * Get block hash list.
     *
     * @param start start height
     * @param limit
     * For response please handle message TaucoinClientMessage.MSG_BLOCK_HASH_LIST
     *      {"hashList": <array list of block hash>}
     */
    public void getBlockHashList(long start, long limit) {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCK_HASH_LIST, 0, 0);
        msg.replyTo = clientMessenger;
        Bundle data = new Bundle();
        data.putLong("start", start);
        data.putLong("limit", limit);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(get block hash list) to service: " + e.getMessage());
        }
    }

    /**
     * Get pending transactions from memeory pool.
     *
     * For response please handle message TaucoinClientMessage.MSG_POOL_TXS
     *      {"txs": <array list of tx id>}
     */
    public void getPendingTxs() {
        if (!isBound) {
            System.out.println(" Not bound ???");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_POOL_TXS, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(get pending txs) to service: " + e.getMessage());
        }
    }

    /**
     * Connect ethereum to peer
     * @param ip String Peer ip address
     * @param port int Peer port
     * @param remoteId String Peer remote id
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "ip": String [Peer ip address]
     *     "port": int [Peer port]
     *     "remoteId": String [Peer remote id]
     * }
     */
    public void connect(String ip, int port, String remoteId) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_CONNECT, 0, 0);
        Bundle data = new Bundle();
        data.putString("ip", ip);
        data.putInt("port", port);
        data.putString("remoteId", remoteId);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(connect) to service: " + e.getMessage());
        }
    }

    /**
     * Load blocks from dump file
     * @param dumpFile String Blocks dump file path
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "dumpFile": String [Blocks dump file paths]
     * }
     */
    public void loadBlocks(String dumpFile) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_LOAD_BLOCKS, 0, 0);
        Bundle data = new Bundle();
        data.putString("dumpFile", dumpFile);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(loadBlocks) to service: " + e.getMessage());
        }
    }

    /**
     * Start the json rpc server
     *
     * Sends message parameters: none
     */
    public void startJsonRpc() {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_START_JSON_RPC_SERVER, 0, 0);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(startJsonRpc) to service: " + e.getMessage());
        }
    }

    /**
     * Change the json rpc server url
     *
     * Sends message parameters: ( "key": type [description] ):
     * {
     *     "rpc_server": String [Rpc server url]
     * }
     */
    public void changeJsonRpc(String serverUrl) {

        if (!isBound) {
            System.out.println("Connector is not bound.");
            return;
        }

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_CHANGE_JSON_RPC_SERVER, 0, 0);
        Bundle data = new Bundle();
        data.putString("rpc_server", serverUrl);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
            System.out.println("Sent change rpc server message");
        } catch (RemoteException e) {
            logger.error("Exception sending message(changeJsonRpc) to service: " + e.getMessage());
        }
    }

    /**
     * Find an online peer
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void findOnlinePeer(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_FIND_ONLINE_PEER, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(findOnlinePeer1) to service: " + e.getMessage());
        }
    }

    /**
     * Find an online peer
     * @param identifier String Caller identifier used to return the response
     * @param excludePeer PeerInfo Excluded peer from search
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "excludePeer": Parcelable(PeerInfo) [Exclude peer from search]
     * }
     */
    public void findOnlinePeer(String identifier, PeerInfo excludePeer) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_FIND_ONLINE_PEER, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        Bundle data = new Bundle();
        data.putParcelable("excludePeer", (io.taucoin.android.interop.PeerInfo) excludePeer);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(findOnlinePeer2) to service: " + e.getMessage());
        }
    }

    /**
     * Find an online peer
     * @param identifier String Caller identifier used to return the response
     * @param excludePeerSet PeerInfo[] Excluded peers from search
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "excludePeerSet": ParcelableArray(PeerInfo[]) [Excluded peers from search]
     * }
     */
    public void findOnlinePeer(String identifier, PeerInfo[] excludePeerSet) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_FIND_ONLINE_PEER, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        Bundle data = new Bundle();
        data.putParcelableArray("excludePeerSet", (io.taucoin.android.interop.PeerInfo[]) excludePeerSet);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(findOnlinePeer3) to service: " + e.getMessage());
        }
    }

    /**
     * Get etherum peers
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void getPeers(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_PEERS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getPeers) to service: " + e.getMessage());
        }
    }

    /**
     * Starts ethereum peer discovery
     *
     * Sends message parameters: none
     */
    public void startPeerDiscovery() {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_START_PEER_DISCOVERY, 0, 0);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(startPeerDiscovery) to service: " + e.getMessage());
        }
    }

    /**
     * Stops ethereum peer discovery
     *
     * Sends message parameters: none
     */
    public void stopPeerDiscovery() {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_LOAD_BLOCKS, 0, 0);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(stopPeerDiscovery) to service: " + e.getMessage());
        }
    }

    protected Bundle getIdentifierBundle(String identifier) {

        Bundle bundle = new Bundle();
        bundle.putString("identifier", identifier);
        return bundle;
    }


    /**
     * Gets the blockchain status
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void getBlockchainStatus(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCKCHAIN_STATUS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getBlockchainStatus) to service: " + e.getMessage());
        }
    }

    /**
     * Add ethereum event listener
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *      "flags": Serializable(EnumSet<ListenerFlag>) [sets flags to listen to specific events]
     * }
     */
    public void addListener(String identifier, EnumSet<EventFlag> flags) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_ADD_LISTENER, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        Bundle data = new Bundle();
        data.putSerializable("flags", flags);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(addListener) to service: " + e.getMessage());
        }
    }

    /**
     * Remove ethereum event listener
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void removeListener(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_REMOVE_LISTENER, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(removeListener) to service: " + e.getMessage());
        }
    }


    /**
     * Closes ethereum
     *
     * Sends message parameters: none
     */
    public void closeEthereum() {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_CLOSE, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(closeEthereum) to service: " + e.getMessage());
        }
    }

    /**
     * Get connection status
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void getConnectionStatus(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_CONNECTION_STATUS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getConnectionStatus) to service: " + e.getMessage());
        }
    }

    /**
     * Submit ethereum transaction
     * @param identifier String Caller identifier used to return the response
     * @param transaction Transaction Transaction to submit
     *
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "transaction": Parcelable(Transaction) [transaction to submit]
     * }
     */
    public void submitTransaction(String identifier, Transaction transaction) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_SUBMIT_TRANSACTION, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        Bundle data = new Bundle();
        data.putParcelable("transaction", transaction);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(submitTransaction) to service: " + e.getMessage());
        }
    }

    /**
     * Get admin info
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void getAdminInfo(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_ADMIN_INFO, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getAdminInfo) to service: " + e.getMessage());
        }
    }

    /**
     * Get pending transactions
     * @param identifier String Caller identifier used to return the response
     *
     * Sends message parameters: none
     */
    public void getPendingTransactions(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_PENDING_TRANSACTIONS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getPendingTransactions) to service: " + e.getMessage());
        }
    }

    /**
     * Get chain height.
     * @param identifier String Caller identifier used to return the response
     *
     * For response, please handle TaucoinClientMessage.MSG_CHAIN_HEIGHT:
     *                {"height": <chain height with long data type>}
     */
    public void getChainHeight(String identifier) {

        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_CHAIN_HEIGHT, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getChainHeight) to service: " + e.getMessage());
        }
    }

    /**
     * Get block by number.
     * @param identifier String Caller identifier used to return the response
     * @param number long requested block number
     *
     * For response, please handle TaucoinClientMessage.MSG_BLOCK:
     *                {
     *                    "number": <requested block number>,
     *                    "block": <io.taucoin.android.interop.Block>
     *                }
     */
    public void getBlockByNumber(String identifier, long number) {

        if (!isBound || number < 0)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCK, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);

        Bundle data = new Bundle();
        data.putLong("number", number);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getBlockByNumber) to service: " + e.getMessage());
        }
    }

    /**
     * Get block by hash.
     * @param identifier String Caller identifier used to return the response
     * @param hash byte[] requested block hash
     *
     * For response, please handle TaucoinClientMessage.MSG_BLOCK:
     *                {
     *                    "hash": <requested block hash>,
     *                    "block": <io.taucoin.android.interop.Block>
     *                }
     */
    public void getBlockByHash(String identifier, byte[] hash) {

        if (!isBound || hash == null)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCK, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);

        Bundle data = new Bundle();
        data.putByteArray("hash", hash);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getBlockByHash) to service: " + e.getMessage());
        }
    }

    /**
     * Get block list by starting number.
     * @param identifier String Caller identifier used to return the response
     * @param number long requested block number
     * @param limit int max requested block account
     *
     * For response, please handle TaucoinClientMessage.MSG_BLOCKS:
     *                {
     *                    "number": <requested block number>,
     *                    "limit": <max requested block account>,
     *                    "blocks": <ArrayList of io.taucoin.android.interop.Block>
     *                }
     */
    public void getBlockListByStartNumber(String identifier, long number, int limit) {

        if (!isBound || number < 0 || limit <= 0)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCKS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);

        Bundle data = new Bundle();
        data.putLong("number", number);
        data.putInt("limit", limit);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getBlockByNumber) to service: " + e.getMessage());
        }
    }

    /**
     * Get block list by starting hash.
     * @param identifier String Caller identifier used to return the response
     * @param hash byte[] requested starting block hash
     * @param limit int max requested block account
     *
     * For response, please handle TaucoinClientMessage.MSG_BLOCKS:
     *                {
     *                    "hash": <requested starting block hash>,
     *                    "limit": <max requested block account>,
     *                    "blocks": <ArrayList of io.taucoin.android.interop.Block>
     *                }
     */
    public void getBlockListByStartHash(String identifier, byte[] hash, int limit) {

        if (!isBound || hash == null || limit <= 0)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_GET_BLOCKS, 0, 0);
        msg.replyTo = clientMessenger;
        msg.obj = getIdentifierBundle(identifier);

        Bundle data = new Bundle();
        data.putByteArray("hash", hash);
        data.putInt("limit", limit);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(getBlockByNumber) to service: " + e.getMessage());
        }
    }

    public void sendMiningNotify(String object) {
        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_SEND_MINING_NOTIFY, 0, 0);
        msg.replyTo = clientMessenger;
        Bundle data = new Bundle();
        data.putString("data", object);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(sendMiningNotify) to service: " + e.getMessage());
        }
    }

    public void cancelMiningNotify() {
        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_CLOSE_MINING_NOTIFY, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(cancelMiningNotify) to service: " + e.getMessage());
        }
    }

    public void cancelMiningProgress() {
        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_CLOSE_MINING_PROGRESS, 0, 0);
        msg.replyTo = clientMessenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(cancelMiningProgress) to service: " + e.getMessage());
        }
    }

    public void sendBlockNotify(String object) {
        if (!isBound)
            return;

        Message msg = Message.obtain(null, TaucoinServiceMessage.MSG_SEND_BLOCK_NOTIFY, 0, 0);
        msg.replyTo = clientMessenger;
        Bundle data = new Bundle();
        data.putString("data", object);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            logger.error("Exception sending message(sendBlockNotify) to service: " + e.getMessage());
        }
    }

}
