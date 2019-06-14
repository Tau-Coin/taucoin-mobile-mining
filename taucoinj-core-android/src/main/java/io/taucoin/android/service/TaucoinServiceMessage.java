package io.taucoin.android.service;


public class TaucoinServiceMessage {

    /**
     * Command to the service to connect to a peer
     */
    public static final int MSG_CONNECT = 1;

    /**
     * Command to the service to load blocks dumpr
     */
    public static final int MSG_LOAD_BLOCKS = 2;

    /**
     * Command to the service to start json rpc server
     */
    public static final int MSG_START_JSON_RPC_SERVER = 3;

    /**
     * Command to the service to find an online peer
     */
    public static final int MSG_FIND_ONLINE_PEER = 4;

    /**
     * Command to the service to list the peers
     */
    public static final int MSG_GET_PEERS = 5;

    /**
     * Command to the service to start peer discovery
     */
    public static final int MSG_START_PEER_DISCOVERY = 6;

    /**
     * Command to the service to get blockchain status (Loading/Loaded)
     */
    public static final int MSG_GET_BLOCKCHAIN_STATUS = 8;

    /**
     * Command to the service to add a listener
     */
    public static final int MSG_ADD_LISTENER = 9;

    /**
     * Command to the service to remove a listener
     */
    public static final int MSG_REMOVE_LISTENER = 10;

    /**
     * Command to the service to get connection status (Connected/Not Connected)
     */
    public static final int MSG_GET_CONNECTION_STATUS = 11;

    /**
     * Command to the service to close
     */
    public static final int MSG_CLOSE = 12;

    /**
     * Command to the service to submit a transaction
     */
    public static final int MSG_SUBMIT_TRANSACTION = 13;

    /**
     * Command to the service to get the admin info
     */
    public static final int MSG_GET_ADMIN_INFO = 14;

    /**
     * Command to the service to get the pernding transactions
     */
    public static final int MSG_GET_PENDING_TRANSACTIONS = 15;

    /**
     * Command to the service to initialize with specific addresses. If already initialized, restart the service
     */
    public static final int MSG_INIT = 16;

    /**
     * Command to the service to change the json rpc server
     */
    public static final int MSG_CHANGE_JSON_RPC_SERVER = 17;

    /**
     * Command to the service to import block forger private key
     */
    public static final int MSG_IMPORT_FORGER_PRIVKEY = 18;

    /**
     * Command to the service to start block forging
     */
    public static final int MSG_START_FORGING = 19;

    /**
     * Command to the service to stop block forging
     */
    public static final int MSG_STOP_FORGING = 20;

    /**
     * Command to the service to start sync
     */
    public static final int MSG_START_SYNC = 21;

    /**
     * Command to the service to get block hash list from some height
     */
    public static final int MSG_GET_BLOCK_HASH_LIST = 22;

    /**
     * Command to the service to get pending transactions from mem pool
     */
    public static final int MSG_GET_POOL_TXS = 23;

    /**
     * Command to the service to get chain height
     */
    public static final int MSG_GET_CHAIN_HEIGHT = 24;

    /**
     * Command to the service to get block by number or hash
     */
    public static final int MSG_GET_BLOCK = 25;

    /**
     * Command to the service to get blocks by starting number
     * or starting hash
     */
    public static final int MSG_GET_BLOCKS = 26;

    /**
     * Send mining notify event
     */
    public static final int MSG_SEND_MINING_NOTIFY = 27;

    /**
     * Send close mining notify event
     */
    public static final int MSG_CLOSE_MINING_NOTIFY = 28;

    /**
     * Send block notify event
     */
    public static final int MSG_SEND_BLOCK_NOTIFY = 29;

    /**
     * Command to the service to close mining progress
     */
    public static final int MSG_CLOSE_MINING_PROGRESS = 30;

    /**
     * Command to the service to get account state
     */
    public static final int MSG_GET_ACCOUNT_STATE = 31;

    /**
     * Command to the service to get block tx reindex
     */
    public static final int MSG_GET_BLOCK_TX_REINDEX = 32;

    /**
     * Command to the service to stop sync
     */
    public static final int MSG_STOP_SYNC = 33;

}
