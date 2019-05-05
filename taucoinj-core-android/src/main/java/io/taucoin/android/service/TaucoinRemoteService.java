package io.taucoin.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;

import android.util.Pair;
import io.taucoin.android.di.components.DaggerTaucoinComponent;
import io.taucoin.android.di.modules.TaucoinModule;
import io.taucoin.android.manager.BlockLoader;
import io.taucoin.android.service.events.*;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.Block;
import io.taucoin.core.DumpedPrivateKey;
import io.taucoin.core.Genesis;
import io.taucoin.core.Transaction;
import io.taucoin.core.VersionedChecksummedBytes;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.HashUtil;
import io.taucoin.android.Taucoin;
import io.taucoin.forge.ForgeStatus;
import io.taucoin.http.ConnectionManager;
import io.taucoin.manager.AdminInfo;
import io.taucoin.net.peerdiscovery.PeerInfo;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.sync.PeersPool;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.http.ConnectionState.*;

public class TaucoinRemoteService extends TaucoinService {

    private static final Logger logger = LoggerFactory.getLogger("TaucoinRemoteService");

    static HashMap<String, Messenger> clientListeners = new HashMap<>();
    static EnumMap<EventFlag, List<String>> listenersByType = new EnumMap<EventFlag, List<String>>(EventFlag.class);

    public boolean isTaucoinStarted = false;
    private String currentJsonRpcServer = null;

    protected String ipBootstrap = null;
    protected int portBootstrap = 30606;
    protected String remoteIdBootstrap = null;

    private ConnectionManager connectionManager = null;
    private IntentFilter intentFilter = null;
    private NetworkStateListener networkStateListener = null;

    public TaucoinRemoteService() {

        super();
    }

    /** Handles incoming messages from clients. */
    static class IncomingHandler extends Handler {

        private final WeakReference<TaucoinRemoteService> service;

        IncomingHandler(TaucoinRemoteService service) {

            this.service = new WeakReference<TaucoinRemoteService>(service);
        }

        @Override
        public void handleMessage(Message message) {

            TaucoinRemoteService service = this.service.get();
            if (service != null) {
                if (!service.handleMessage(message)) {
                    super.handleMessage(message);
                }
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.Note
     * that calls to its binder are sequential!
     */
    final Messenger serviceMessenger = new Messenger(new IncomingHandler(this));

    /**
     * When binding to the service, we return an interface to our messenger for
     * sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {

        return serviceMessenger.getBinder();
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        // super.onDestroy will call taucoin.close()
        super.onDestroy();
        unregisterNetworkStateListener();
        TaucoinModule.close();
        isTaucoinStarted = false;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (taucoin != null) {
            taucoin.close();
            taucoin = null;
        }
        TaucoinModule.close();
        isTaucoinStarted = false;
    }

    protected void broadcastEvent(EventFlag event, EventData data) {

        Message message = null;
        List<String> listeners = listenersByType.get(event);
        if (listeners != null) {
            for (String identifier: listeners) {
                Messenger listener = clientListeners.get(identifier);
                if (listener != null) {
                    //if (message == null) {
                        message = createEventMessage(event, data);
                    //}
                    message.obj = getIdentifierBundle(identifier);
                    try {
                        listener.send(message);
                    } catch (RemoteException e) {
                        logger.error("Exception sending event message to client listener: " + e.getMessage());
                    }
                }
            }
        }
    }

    protected Bundle getIdentifierBundle(String identifier) {

        Bundle bundle = new Bundle();
        bundle.putString("identifier", identifier);
        return bundle;
    }

    protected Message createEventMessage(EventFlag event, EventData data) {

        Message message = Message.obtain(null, TaucoinClientMessage.MSG_EVENT, 0, 0);
        Bundle replyData = new Bundle();
        replyData.putSerializable("event", event);
        replyData.putParcelable("data", data);
        message.setData(replyData);

        return message;
    }


    protected boolean handleMessage(Message message) {

        switch (message.what) {

            case TaucoinServiceMessage.MSG_INIT:
                init(message);
                break;

            case TaucoinServiceMessage.MSG_CONNECT:
                connect(message);
                break;

            case TaucoinServiceMessage.MSG_LOAD_BLOCKS:
                loadBlocks(message);
                break;

            case TaucoinServiceMessage.MSG_START_JSON_RPC_SERVER:
                startJsonRpc(message);
                break;

            case TaucoinServiceMessage.MSG_CHANGE_JSON_RPC_SERVER:
                changeJsonRpc(message);
                break;

            case TaucoinServiceMessage.MSG_FIND_ONLINE_PEER:
                findOnlinePeer(message);
                break;

            case TaucoinServiceMessage.MSG_GET_PEERS:
                getPeers(message);
                break;

            case TaucoinServiceMessage.MSG_START_PEER_DISCOVERY:
                startPeerDiscovery(message);
                break;

            case TaucoinServiceMessage.MSG_STOP_PEER_DISCOVERY:
                stopPeerDiscovery(message);
                break;

            case TaucoinServiceMessage.MSG_GET_BLOCKCHAIN_STATUS:
                getBlockchainStatus(message);
                break;

            case TaucoinServiceMessage.MSG_ADD_LISTENER:
                addListener(message);
                break;

            case TaucoinServiceMessage.MSG_REMOVE_LISTENER:
                removeListener(message);
                break;

            case TaucoinServiceMessage.MSG_GET_CONNECTION_STATUS:
                getConnectionStatus(message);
                break;

            case TaucoinServiceMessage.MSG_CLOSE:
                closeTaucoin(message);
                break;

            case TaucoinServiceMessage.MSG_SUBMIT_TRANSACTION:
                submitTransaction(message);
                break;

            case TaucoinServiceMessage.MSG_GET_ADMIN_INFO:
                getAdminInfo(message);
                break;

            case TaucoinServiceMessage.MSG_GET_PENDING_TRANSACTIONS:
                getPendingTransactions(message);
                break;

            case TaucoinServiceMessage.MSG_IMPORT_FORGER_PRIVKEY:
                importForgerPrivkey(message);
                break;

            case TaucoinServiceMessage.MSG_START_FORGING:
                startBlockForging(message);
                break;

            case TaucoinServiceMessage.MSG_STOP_FORGING:
                stopBlockForging(message);
                break;

            case TaucoinServiceMessage.MSG_START_SYNC:
                startSync(message);
                break;

            case TaucoinServiceMessage.MSG_GET_BLOCK_HASH_LIST:
                getBlockHashList(message);
                break;

            case TaucoinServiceMessage.MSG_GET_POOL_TXS:
                getPendingTxs(message);
                break;

            case TaucoinServiceMessage.MSG_GET_BLOCK:
                getBlock(message);
                break;

            case TaucoinServiceMessage.MSG_GET_BLOCKS:
                getBlockList(message);
                break;

            case TaucoinServiceMessage.MSG_GET_CHAIN_HEIGHT:
                getChainHeight(message);
                break;

            case TaucoinServiceMessage.MSG_GET_ACCOUNT_STATE:
                getAccountState(message);
                break;

            default:
                return false;
        }

        return true;
    }

    @Override
    protected void onTaucoinCreated(List<String> privateKeys, Messenger replyTo, Object reply) {

        if (taucoin != null) {
            System.out.println("Loading genesis");
            broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData("Loading genesis block. This may take a few minutes..."));

            long startTime1 = System.nanoTime();

            if (privateKeys == null || privateKeys.size() == 0) {
                privateKeys = new ArrayList<>();
                byte[] cowAddr = HashUtil.sha3("cow".getBytes());
                privateKeys.add(Hex.toHexString(cowAddr));

                String secret = CONFIG.coinbaseSecret();
                byte[] cbAddr = HashUtil.sha3(secret.getBytes());
                privateKeys.add(Hex.toHexString(cbAddr));
            }
            taucoin.init(privateKeys);

            long endTime1 = System.nanoTime();
            logger.info("Loading genesis cost {} ms", (endTime1 - startTime1)/1000000);
            logger.info("Genesis loaded");

            taucoin.initSync();
            isTaucoinStarted = true;
            isInitialized = true;

            // Start rpc server
            if (CONFIG.isRpcEnabled()) {
                logger.info("Starting json rpc...");
                startJsonRpc(null);
            }

            // Init network status and register listener.
            initNetwork();

            // Send reply
            if (replyTo != null && reply != null) {
                Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_EVENT, 0, 0, reply);
                Bundle replyData = new Bundle();
                replyData.putSerializable("event", EventFlag.EVENT_TAUCOIN_CREATED);
                replyMessage.setData(replyData);
                try {
                    replyTo.send(replyMessage);
                    logger.info("Taucoin created.");
                } catch (RemoteException e) {
                    logger.error("Exception sending taucoin created event: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void createTaucoin(String privateKey) {

        System.setProperty("sun.arch.data.model", "32");
        System.setProperty("leveldb.mmap", "false");

        // Import private key
        CONFIG.importForgerPrikey(TextUtils.isEmpty(privateKey) ?
                null : Utils.getRawPrivateKeyString(privateKey));
        String databaseFolder = getApplicationInfo().dataDir;
        logger.info("Database folder: {}", databaseFolder);
        CONFIG.setDataBaseDir(databaseFolder);

        component = DaggerTaucoinComponent.builder()
                .taucoinModule(new TaucoinModule(this))
                .build();

        taucoin = component.taucoin();
        connectionManager = component.connectionManager();
        taucoin.addListener(new TaucoinListener());
        taucoin.getBlockForger().addListener(new TaucoinForgerListener());
        taucoin.getPendingState().setBlockchain(taucoin.getBlockchain());
        // You can also add some other initialization logic.
    }


    protected void init(Message message) {

        if (isTaucoinStarted) {
            Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_EVENT, 0, 0, message.obj);
            Bundle replyData = new Bundle();
            replyData.putSerializable("event", EventFlag.EVENT_TAUCOIN_EXIST);
            replyMessage.setData(replyData);
            try {
                message.replyTo.send(replyMessage);
                logger.info("Taucoin has created.");
            } catch (RemoteException e) {
                logger.error("Exception sending taucoin created event: " + e.getMessage());
            }
            return;
        }
        Bundle data = message.getData();
        List<String> privateKeys = data.getStringArrayList("privateKeys");
        new InitializeTask(privateKeys, message.replyTo, message.obj).execute();
    }

    private void initNetwork() {
        initNetworkStatus();
        registerNetworkStateListener();
    }

    private void initNetworkStatus() {
        ConnectivityManager manager
                = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            connectionManager.setConnectionState(CONNECTED);
        } else {
            connectionManager.setConnectionState(DISCONNECTED);
        }
    }

    private void registerNetworkStateListener() {
        intentFilter = new IntentFilter();
        networkStateListener = new NetworkStateListener(connectionManager);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(networkStateListener, intentFilter);
    }

    private void unregisterNetworkStateListener() {
        this.unregisterReceiver(networkStateListener);
    }

    private static class NetworkStateListener extends BroadcastReceiver {

        private ConnectionManager connectionManager;

        public NetworkStateListener(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo info
                        = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    if (NetworkInfo.State.CONNECTED == info.getState()
                            && info.isAvailable()) {
                        connectionManager.setConnectionState(CONNECTED);
                    } else {
                        connectionManager.setConnectionState(DISCONNECTED);
                    }
                }
            }
        }
    }

    /*
    protected void init(Message message) {

        if (isTaucoinStarted) {
            stopJsonRpcServer();
            closeTaucoin(null);
            taucoin = null;
            component = null;
            isInitialized = false;
            isConnected = false;
        }
        createTaucoin();
        Bundle data = message.getData();
        List<String> privateKeys = data.getStringArrayList("privateKeys");
        onTaucoinCreated(privateKeys);
    }
*/
    /**
     * Connect to peer
     *
     * Incoming message parameters ( "key": type [description] ):
     * {
     *     "ip": String  [peer ip address]
     *     "port": int  [peer port]
     *     "remoteId": String  [peer remoteId]
     * }
     * Sends message: none
     */
    protected void connect(Message message) {

        if (!isConnected && taucoin != null) {
            isConnected = true;
            Bundle data = message.getData();
            String ip = data.getString("ip");
            if (ip == null && this.ipBootstrap != null) {
                ip = this.ipBootstrap;
            }
            int port = data.getInt("port");
            if (port == -1) {
                port = this.portBootstrap;
            }
            String remoteId = data.getString("remoteId");
            if (remoteId == null && this.remoteIdBootstrap != null) {
                remoteId = this.remoteIdBootstrap;
            }
            System.out.println("Trying to connect to: " + ip + ":" + port + "@" + remoteId);
            new ConnectTask(ip, port, remoteId).execute(taucoin);
        }
    }

    protected class ConnectTask extends AsyncTask<Taucoin, Message, Void> {

        String ip;
        int port;
        String remoteId;

        public ConnectTask(String ip, int port, String remoteId) {

            this.ip = ip;
            this.port = port;
            this.remoteId = remoteId;
        }

        protected Void doInBackground(Taucoin... args) {

            Taucoin taucoin = args[0];
            System.out.println("Connecting to: " + ip + ":" + port + "@" + remoteId);
            taucoin.initSync();
            //taucoin.connect(ip, port, remoteId);
            logger.info("Taucoin connecting to : " + ip + ":" + port);
            return null;
        }

        protected void onPostExecute(Void results) {


        }
    }

    /**
     * Load blocks from dump file
     *
     * Incoming message parameters ( "key": type [description] ):
     * {
     *     "dumpFile": String  [blocks dump file path]
     * }
     * Sends message: none
     */
    protected void loadBlocks(Message message) {

        if (!isConnected) {
            isConnected = true;
            new LoadBlocksTask(message).execute(taucoin);
        }
    }

    protected class LoadBlocksTask extends AsyncTask<Taucoin, Message, Void> {

        String dumpFile;

        public LoadBlocksTask(Message message) {

            Bundle data = message.getData();
            dumpFile = data.getString("dumpFile");
        }

        protected Void doInBackground(Taucoin... args) {

            Taucoin taucoin = args[0];
            logger.info("Loading blocks from: " + dumpFile);
            BlockLoader blockLoader = (BlockLoader)taucoin.getBlockLoader();
            blockLoader.loadBlocks(dumpFile);
            logger.info("Finished loading blocks from: " + dumpFile);
            return null;
        }

        protected void onPostExecute(Void results) {


        }
    }

    protected void stopJsonRpcServer() {
        if (jsonRpcServerThread != null) {
            jsonRpcServerThread.interrupt();
            jsonRpcServerThread = null;
        }
        if (jsonRpcServer != null) {
            jsonRpcServer.stop();
            jsonRpcServer = null;
        }
    }

    /**
     * Start the json rpc server
     *
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void startJsonRpc(Message message) {

        if (jsonRpcServer == null) {
            //TODO: add here switch between full and light version
            jsonRpcServer = new io.taucoin.android.rpc.server.full.JsonRpcServer(taucoin);
        }
        if (jsonRpcServerThread == null) {
            jsonRpcServerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        jsonRpcServer.start(CONFIG.rpcListenPort());
                        logger.info("Started json rpc server!");
                    } catch (Exception e) {
                        logger.error("Exception starting json rpc server: " + e.getMessage());
                    }
                }
            });
            jsonRpcServerThread.start();
        }
    }

    /**
     * Start the json rpc server
     *
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void changeJsonRpc(Message message) {

        String server = null;
        if (message == null) {
            if (currentJsonRpcServer != null) {
                server = currentJsonRpcServer;
            }
        } else {
            Bundle data = message.getData();
            server = data.getString("rpc_server");
            currentJsonRpcServer = server;
        }
        if (jsonRpcServer != null && server != null) {
            //((io.taucoin.android.rpc.server.light.JsonRpcServer)jsonRpcServer).addRemoteServer(server, true);
        } else {
            System.out.println("jsonRpcServer or server is null on changeJsonRpc");
        }
    }



    /**
     * Find an online peer
     *
     * Incoming message parameters ( "key": type [description] ):
     * {
     *      "excludePeer": Parcelable(PeerInfo) [peer to exclude from search]
     * }
     * Sends message parameters ( "key": type [description] ):
     * {
     *     "peerInfo": Parcelable(PeerInfo) [found online peer, or null if error / online peer not found]
     * }
     */
    protected void findOnlinePeer(Message message) {

        Bundle data = message.getData();
        PeerInfo foundPeerInfo;
        PeerInfo peerInfo = data.getParcelable("excludePeer");
        if (peerInfo != null) {
            foundPeerInfo = taucoin.findOnlinePeer(peerInfo);
        } else {
            PeerInfo[] excludePeerSet = (PeerInfo[])data.getParcelableArray("excludePeerSet");
            if (excludePeerSet != null) {
                foundPeerInfo = taucoin.findOnlinePeer(new HashSet<>(Arrays.asList(excludePeerSet)));
            } else {
                foundPeerInfo = taucoin.findOnlinePeer();
            }
        }
        // Send reply
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_ONLINE_PEER, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        replyData.putParcelable("peerInfo", new io.taucoin.android.interop.PeerInfo(foundPeerInfo));
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent online peer to client: " + foundPeerInfo.toString());
        } catch (RemoteException e) {
            logger.error("Exception sending online peer to client: " + e.getMessage());
        }
    }

    /**
     * Get etherum peers
     *
     * Incoming message parameters: none
     * Sends message ( "key": type [description] ):
     * {
     *     "peers": Parcelable[](PeerInfo[]) [taucoin peers]
     * }
     */
    protected void getPeers(Message message) {

        Set<PeerInfo> peers = taucoin.getPeers();
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_PEERS, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        io.taucoin.android.interop.PeerInfo[] convertedPeers = new io.taucoin.android.interop.PeerInfo[peers.size()];
        int index = 0;
        for (PeerInfo peerInfo: peers) {
            convertedPeers[index] = new io.taucoin.android.interop.PeerInfo(peerInfo);
            index++;
        }
        replyData.putParcelableArray("peers", convertedPeers);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent peers to client: " + peers.size());
        } catch (RemoteException e) {
            logger.error("Exception sending peers to client: " + e.getMessage());
        }
    }

    /**
     * Starts taucoin peer discovery
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void startPeerDiscovery(Message message) {

        taucoin.startPeerDiscovery();
        logger.info("Started peer discovery.");
    }

    /**
     * Stops taucoin peer discovery
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void stopPeerDiscovery(Message message) {

        taucoin.stopPeerDiscovery();
        logger.info("Stopped peer discovery.");
    }

    /**
     * Gets the blockchain status
     *
     * Incoming message parameters: none
     * Sends message ( "key": type [description] ):
     * {
     *     "status": String [blockchain status: Loading/Loaded]
     * }
     */
    protected void getBlockchainStatus(Message message) {

        boolean isLoading = false;//taucoin.isBlockchainLoading();
        String status = isLoading ? "Loading" : "Loaded";
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_BLOCKCHAIN_STATUS, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        replyData.putString("status", status);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent blockchain status: " + status);
        } catch (RemoteException e) {
            logger.error("Exception sending blockchain status to client: " + e.getMessage());
        }
    }

    /**
     * Add taucoin event listener
     *
     * Incoming message parameters ( "key": type [description] ):
     * {
     *      "flags": Serializable(EnumSet<ListenerFlag>) [defines flags to listen to specific events]
     * }
     * Sends message: none
     */
    protected void addListener(Message message) {

        // Register the client's messenger
        String identifier = ((Bundle)message.obj).getString("identifier");
        logger.info("Adding listener: " + identifier + " - " + message.replyTo.toString());
        clientListeners.put(identifier, message.replyTo);
        Bundle data = message.getData();
        data.setClassLoader(EventFlag.class.getClassLoader());
        EnumSet<EventFlag> flags = (EnumSet<EventFlag>)data.getSerializable("flags");
        EnumSet<EventFlag> list = (flags == null || flags.contains(EventFlag.EVENT_ALL)) ? EnumSet.allOf(EventFlag.class) : flags;
        for (EventFlag flag: list) {
            List<String> listeners = listenersByType.get(flag);
            boolean shouldAdd = false;
            if (listeners == null) {
                listeners = new ArrayList<>();
                shouldAdd = true;
            }
            if (shouldAdd || !listeners.contains(identifier)) {
                listeners.add(identifier);
                listenersByType.put(flag, listeners);
            }
        }
        logger.info("Client listener registered!");
    }

    /**
     * Remove etherum event listener
     *
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void removeListener(Message message) {

        String identifier = ((Bundle)message.obj).getString("identifier");
        clientListeners.remove(identifier);
        for (EventFlag flag: EventFlag.values()) {
            List<String> listeners = listenersByType.get(flag);
            if (listeners != null && listeners.contains(identifier)) {
                listeners.remove(identifier);
            }
        }
        logger.info("Client listener unregistered!");
    }

    /**
     * Closes taucoin
     *
     * Incoming message parameters: none
     * Sends message: none
     */
    protected void closeTaucoin(Message message) {

        if (taucoin != null) {
            taucoin.close();
            taucoin = null;
        }
        TaucoinModule.close();
        isTaucoinStarted = false;

        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_CLOSE_DONE, 0, 0);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent taucoin close reply message");
        } catch (RemoteException e) {
            logger.error("Exception sending taucoin close reply message to client: " + e.getMessage());
        }
        logger.info("Closed taucoin.");
    }

    /**
     * Get connection status
     *
     * Incoming message parameters: none
     * Sends message ( "key": type [description] ):
     * {
     *     "status": String [taucoin connection status: Connected/Not Connected]
     * }
     */
    protected void getConnectionStatus(Message message) {

        String status = taucoin.isConnected() ? "Connected" : "Not Connected";
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_CONNECTION_STATUS, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        replyData.putString("status", status);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent taucoin connection status: " + status);
        } catch (RemoteException e) {
            logger.error("Exception sending taucoin connection status to client: " + e.getMessage());
        }
    }

    /**
     * Submit taucoin transaction
     *
     * Incoming message parameters ( "key": type [description] ):
     * {
     *     "transaction": Parcelable(Transaction) [taucoin transaction to submit]
     * }
     * Sends message ( "key": type [description] ):
     * {
     *     "transaction": Parcelable(Transaction) [submitted transaction]
     * }
     */
    protected void submitTransaction(Message message) {

        logger.info("transaction has been submited to services");
        if (!isConnected) {
            logger.info("submited to services {}",isConnected);
            new SubmitTransactionTask(message).execute(taucoin);
        } else {
            logger.warn("Taucoin not connected.");
        }
    }

    protected class SubmitTransactionTask extends AsyncTask<Taucoin, Void, Transaction> {

        Transaction transaction;
        Messenger messenger;
        Object obj;

        public SubmitTransactionTask(Message message) {

            this.messenger = message.replyTo;
            this.obj = message.obj;
            Bundle data = message.getData();
            //transaction = data.getParcelable("transaction");
            data.setClassLoader(io.taucoin.android.interop.Transaction.class.getClassLoader());
            io.taucoin.android.interop.Transaction  temptransaction = data.getParcelable("transaction");
            transaction = temptransaction;
        }

        protected Transaction doInBackground(Taucoin... args) {
            return taucoin.submitTransaction(transaction);
        }

        protected void onPostExecute(Transaction submittedTransaction) {

            Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_SUBMIT_TRANSACTION_RESULT, 0, 0, obj);
            Bundle replyData = new Bundle();
            try {
                replyData.putParcelable("transaction", (io.taucoin.android.interop.Transaction)submittedTransaction);
                replyMessage.setData(replyData);
                messenger.send(replyMessage);
                logger.info("Sent submitted transaction: " + (submittedTransaction != null ? submittedTransaction.toString() : "null"));
            } catch (RemoteException e) {
                logger.error("Exception sending submitted transaction to client: " + e.getMessage());
            }
        }
    }

    /**
     * Get admin info
     *
     * Incoming message parameters: none
     * Sends message ( "key": type [description] ):
     * {
     *     "adminInfo": Parcelable(AdminInfo) [taucoin admin info]
     * }
     */
    protected void getAdminInfo(Message message) {

        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_ADMIN_INFO, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        AdminInfo info = taucoin.getAdminInfo();
        replyData.putParcelable("adminInfo", new io.taucoin.android.interop.AdminInfo(info));
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent admin info: " + info.toString());
        } catch (RemoteException e) {
            logger.error("Exception sending admin info to client: " + e.getMessage());
        }
    }

    /**
     * Get pending transactions
     *
     * Incoming message parameters: none
     * Sends message ( "key": type [description] ):
     * {
     *     "transactions": ParcelableArray(Transaction[]) [taucoin pending transactions]
     * }
     */
    protected void getPendingTransactions(Message message) {

        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_PENDING_TRANSACTIONS, 0, 0, message.obj);
        Bundle replyData = new Bundle();
        List<Transaction> transactions = null;//taucoin.getPendingTransactions();
        io.taucoin.android.interop.Transaction[] convertedTransactions = new io.taucoin.android.interop.Transaction[transactions.size()];
        int index = 0;
        for (Transaction transaction: transactions) {
            convertedTransactions[index] = new io.taucoin.android.interop.Transaction(transaction);
            index++;
        }
        replyData.putParcelableArray("transactions", convertedTransactions);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
            logger.info("Sent pending transactions: " + transactions.size());
        } catch (RemoteException e) {
            logger.error("Exception sending pending transactions to client: " + e.getMessage());
        }
    }

    /**
     * Import block forger private key.
     *
     * Incoming message:{"privateKey":<private key>}
     *
     * Response message:{"result": "OK" or "Fail"}
     */
    protected void importForgerPrivkey(Message message) {

        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_IMPORT_FORGER_PRIVKEY_RESULT, 0, 0);
        Bundle replyData = new Bundle();

        Bundle data = message.getData();
        String privateKey = data.getString("privateKey");
        //support that importing key to base58 or Hex format.
        ECKey key;
        if (privateKey.length() == 51 || privateKey.length() == 52) {
            DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(MainNetParams.get(),privateKey);
            key = dumpedPrivateKey.getKey();
        } else {
            BigInteger privKey = new BigInteger(privateKey,16);
            key = ECKey.fromPrivate(privKey);
            logger.info("import forge prikey wif:{}",key.getPrivateKeyAsWiF(MainNetParams.get()));
        }

        if (taucoin != null && !TextUtils.isEmpty(privateKey)) {
            taucoin.getWorldManager().getWallet().importKey(key.getPrivKeyBytes());
            CONFIG.importForgerPrikey(key.getPrivKeyBytes());
            replyData.putString("result", "OK");
        } else {
            replyData.putString("result", "Fail");
        }

        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
        } catch (RemoteException e) {
            logger.error("Exception sending importing privkey result to client: " + e.getMessage());
        }
    }

    protected void startBlockForging(Message message) {
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_START_FORGING_RESULT, 0, 0);
        Bundle replyData = new Bundle();

        Bundle data = message.getData();
        long targetAmount = data.getLong("forgedAmount");

        if (taucoin != null && targetAmount >= -1) {
            taucoin.getBlockForger().startForging((long)targetAmount);
            replyData.putString("result", "OK");
        } else {
            replyData.putString("result", "Fail");
        }

        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
        } catch (RemoteException e) {
            logger.error("Exception sending forging result to client: " + e.getMessage());
        }

    }

    protected void stopBlockForging(Message message) {
       if (taucoin != null) {
           taucoin.getBlockForger().stopForging();
       }
    }

    protected void startSync(Message message) {
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_START_SYNC_RESULT, 0, 0);
        Bundle replyData = new Bundle();

        if (taucoin != null) {
            replyData.putSerializable("event", EventFlag.EVENT_START_SYNC);
            taucoin.startSync();
        }

        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
        } catch (RemoteException e) {
            logger.error("Exception sending importing privkey result to client: " + e.getMessage());
        }
    }

    protected void getBlockHashList(Message message) {
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_BLOCK_HASH_LIST, 0, 0);
        Bundle replyData = new Bundle();

        Bundle data = message.getData();
        long start = data.getLong("start");
        long limit = data.getLong("limit");
        ArrayList<String> blockHashList = new ArrayList<String>();

        if (taucoin != null && start >= 0 && limit > 0) {
            List<byte[]> hashList = taucoin.getBlockchain().getListOfHashesStartFromBlock(start, (int)limit);
            int count=0;
            for (byte[] hash : hashList) {
                blockHashList.add("0x" + Hex.toHexString(hash));
                logger.info("num:{} hash:{}",count,Hex.toHexString(hash));
                count++;
            }
        }

        replyData.putStringArrayList("hashList", blockHashList);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
        } catch (RemoteException e) {
            logger.error("Exception sending block hash list to client: " + e.getMessage());
        }

    }

    protected void getPendingTxs(Message message) {
        Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_POOL_TXS, 0, 0);
        Bundle replyData = new Bundle();

        ArrayList<String> pendingTxs = new ArrayList<String>();
        if (taucoin != null) {
            for(Transaction tx: taucoin.getPendingStateTransactions()){
               pendingTxs.add("0x" + Hex.toHexString(tx.getHash()));
            }
            for(Transaction tx: taucoin.getWireTransactions()) {
               pendingTxs.add("0x" + Hex.toHexString(tx.getHash()));
            }
        }

        replyData.putStringArrayList("txs", pendingTxs);
        replyMessage.setData(replyData);
        try {
            message.replyTo.send(replyMessage);
        } catch (RemoteException e) {
            logger.error("Exception sending pending txs to client: " + e.getMessage());
        }
    }

    protected void getBlock(Message message) {

        logger.info("get block from taucoin service");
        if (taucoin != null) {
            new GetBlockTask(message).execute(taucoin);
        } else {
            logger.warn("Taucoin not connected.");
        }
    }

    protected class GetBlockTask extends AsyncTask<Taucoin, Void, Block> {

        Messenger messenger;
        Object obj;
        long number;
        byte[] hash;

        public GetBlockTask(Message message) {

            this.messenger = message.replyTo;
            this.obj = message.obj;
            this.number = -1;
            this.hash = null;

            Bundle data = message.getData();
            if (data.containsKey("number")) {
                this.number = data.getLong("number");
            }
            if (data.containsKey("hash")) {
                this.hash = data.getByteArray("hash");
            }
        }

        protected Block doInBackground(Taucoin... args) {

            Block block = null;
            if (taucoin != null) {
                if (number != -1) {
                    block = taucoin.getBlockchain().getBlockByNumber(number);
                } else if (hash != null) {
                    block = taucoin.getBlockchain().getBlockByHash(hash);
                }
            }

            return block;
        }

        protected void onPostExecute(Block block) {

            Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_BLOCK, 0, 0, obj);
            Bundle replyData = new Bundle();
            if (number != -1) {
                replyData.putLong("number", number);
            }
            if (hash != null) {
                replyData.putByteArray("hash", hash);
            }
            try {
                replyData.putParcelable("block", new io.taucoin.android.service.events.BlockEventData(block));
                replyMessage.setData(replyData);
                messenger.send(replyMessage);
                logger.info("Sent to app block: " + block.toString());
            } catch (Exception e) {
                logger.error("Exception sending block to client: " + e.getMessage());
            }
        }
    }

    protected void getBlockList(Message message) {

        logger.info("get block list from taucoin service");
        if (taucoin != null) {
            new GetBlockListTask(message).execute(taucoin);
        } else {
            logger.warn("Taucoin not connected.");
        }
    }

    protected class GetBlockListTask extends AsyncTask<Taucoin, Void, ArrayList<Block>> {

        Messenger messenger;
        Object obj;
        long number;
        byte[] hash;
        int limit;

        public GetBlockListTask(Message message) {

            this.messenger = message.replyTo;
            this.obj = message.obj;
            this.number = -1;
            this.hash = null;
            this.limit = 500;

            Bundle data = message.getData();
            if (data.containsKey("number")) {
                this.number = data.getLong("number");
            }
            if (data.containsKey("hash")) {
                this.hash = data.getByteArray("hash");
            }
            if (data.containsKey("limit")) {
                this.limit = data.getInt("limit");
            }
        }

        protected ArrayList<Block> doInBackground(Taucoin... args) {

            ArrayList<Block> blockList = new ArrayList<Block>();
            List<byte[]> hashList = null;
            if (taucoin != null) {
                if (number != -1) {
                    hashList = taucoin.getBlockchain().getListOfHashesStartFromBlock(number, limit);
                } else if (hash != null) {
                    hashList = taucoin.getBlockchain().getListOfHashesStartFrom(hash, limit);
                }

                if (hashList != null) {
                    for (byte[] hash : hashList) {
                        Block block = taucoin.getBlockchain().getBlockByHash(hash);
                        if (block != null) {
                            blockList.add(block);
                        }
                    }
                }
            }

            return blockList;
        }

        protected void onPostExecute(ArrayList<Block> blockList) {

            Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_BLOCKS, 0, 0, obj);
            Bundle replyData = new Bundle();
            if (number != -1) {
                replyData.putLong("number", number);
            }
            if (hash != null) {
                replyData.putByteArray("hash", hash);
            }
            replyData.putInt("limit", limit);
            ArrayList<io.taucoin.android.service.events.BlockEventData> parcelBlockList
                    = new ArrayList<>();
            for (Block block : blockList) {
                parcelBlockList.add(new io.taucoin.android.service.events.BlockEventData(block));
            }
            replyData.putParcelableArrayList("blocks", parcelBlockList);
            replyMessage.setData(replyData);
            try {
                messenger.send(replyMessage);
                logger.info("Sent blocks to app");
            } catch (RemoteException e) {
                logger.error("Exception sending blocks to client: " + e.getMessage());
            }
        }
    }

    protected void getAccountState(Message message) {

        logger.info("get account state from taucoin service");
        if (taucoin != null) {
            new GetAccountStateTask(message).execute(taucoin);
        } else {
            logger.warn("Taucoin not connected.");
        }
    }

    private static class AccountStateWraper {
        BigInteger balance;
        BigInteger power;
        ArrayList<String> txHis;
        public AccountStateWraper(BigInteger balance, BigInteger power,ArrayList<String> txHis) {
            this.balance = balance;
            this.power   = power;
            this.txHis   = txHis;
        }
    }

    protected class GetAccountStateTask extends AsyncTask<Taucoin, Void, AccountStateWraper> {

        Messenger messenger;
        Object obj;
        String address;

        public GetAccountStateTask(Message message) {

            this.messenger = message.replyTo;
            this.obj = message.obj;

            Bundle data = message.getData();
            if (data.containsKey("address")) {
                this.address = data.getString("address");
            }
        }

        protected AccountStateWraper doInBackground(Taucoin... args) {

            BigInteger balance = BigInteger.ZERO;
            BigInteger power   = BigInteger.ZERO;
            TreeMap<Long,byte[]> txHistory = null;
            byte[] addressBytes = null;

            VersionedChecksummedBytes toEncoedAddress= new VersionedChecksummedBytes(address);
            addressBytes = toEncoedAddress.getBytes();

            if (taucoin != null && address != null && addressBytes != null) {
                balance = taucoin.getRepository().getBalance(addressBytes);
                power = taucoin.getRepository().getforgePower(addressBytes);
                txHistory = taucoin.getRepository().getAccountState(addressBytes).getTranHistory();
            }

            ArrayList<String> retHistory = new ArrayList<>();
            if (txHistory != null && txHistory.size() > 10) {
                for (Long index : txHistory.keySet()) {
                    byte[] txid = txHistory.get(index);
                    retHistory.add("time: " + index.toString() + " hash: " + Hex.toHexString(txid).substring(0, 5));
                }
            }

            if(txHistory != null && txHistory.size() < 10){
                for (Long index : txHistory.keySet()) {
                    System.out.println("======> size is: "+txHistory.size());
                    byte[] txid = txHistory.get(index);
                    retHistory.add("time: " + index.toString() + " hash: " + Hex.toHexString(txid).substring(0, 5));
                }
            }
            return new AccountStateWraper(balance, power,retHistory);
        }

        protected void onPostExecute(AccountStateWraper state) {

            Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_ACCOUNT_STATE, 0, 0, obj);
            Bundle replyData = new Bundle();

            replyData.putString("address", address);
            replyData.putString("balance", state.balance.toString(10));
            replyData.putString("power", state.power.toString(10));
            replyData.putStringArrayList("txHistory",state.txHis);
            replyMessage.setData(replyData);

            try {
                messenger.send(replyMessage);
                logger.info("Send account state to app");
            } catch (RemoteException e) {
                logger.error("Exception sending account state to client: " + e.getMessage());
            }
        }
    }

    protected void getChainHeight(Message message) {
         Message replyMessage = Message.obtain(null, TaucoinClientMessage.MSG_CHAIN_HEIGHT, 0, 0, message.obj);
         Bundle replyData = new Bundle();
         long height = 0;

         if (taucoin != null) {
             height = taucoin.getBlockchain().getSize();
         }

         replyData.putLong("height", height - 1);
         replyMessage.setData(replyData);
         try {
             message.replyTo.send(replyMessage);
         } catch (RemoteException e) {
             logger.error("Exception sending chain height to client: " + e.getMessage());
         }
    }

    protected class TaucoinForgerListener implements io.taucoin.forge.ForgerListener {
        @Override
        public void forgingStarted() {}

        @Override
        public void forgingStopped(ForgeStatus status) {
            broadcastEvent(EventFlag.EVENT_BLOCK_FORGE_STOP,new BlockForgeExceptionStopEvent(status));
        }

        @Override
        public void blockForgingStarted(Block block) {}

        @Override
        public void nextBlockForgedInternal(long internal) {
            broadcastEvent(EventFlag.EVENT_BLOCK_FORGED_TIME_INTERNAL,
                   new BlockForgedInternalEventData(internal));
        }

        @Override
        public void blockForged(Block block) {}

        @Override
        public void blockForgingCanceled(Block block) {}
    }

}
