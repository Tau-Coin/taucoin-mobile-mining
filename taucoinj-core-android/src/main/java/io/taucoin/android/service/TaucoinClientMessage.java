package io.taucoin.android.service;

public class TaucoinClientMessage {

    /**
     * Send online peer to the client ("peerInfo" => PeerInfo)
     */
    public static final int MSG_ONLINE_PEER = 1;

    /**
     * Send peers to the client ("peers" => PeerInfo[])
     */
    public static final int MSG_PEERS = 2;

    /**
     * Send blockchain status to the client ("status" => "Loaded/Loading")
     */
    public static final int MSG_BLOCKCHAIN_STATUS = 3;

    /**
     * Send ethereum connection status to the client
     */
    public static final int MSG_CONNECTION_STATUS = 4;

    /**
     * Send submitted transaction to client
     */
    public static final int MSG_SUBMIT_TRANSACTION_RESULT = 5;

    /**
     * Send admin info to client
     */
    public static final int MSG_ADMIN_INFO = 6;

    /**
     * Send peers to the client
     */
    public static final int MSG_PENDING_TRANSACTIONS = 7;

    /**
     * Send event to the client
     */
    public static final int MSG_EVENT = 8;

    /**
     * Send importing forger private key result to client
     */
    public static final int MSG_IMPORT_FORGER_PRIVKEY_RESULT = 9;

    /**
     * Send starting sync  result to client
     */
    public static final int MSG_START_SYNC_RESULT = 10;

    /**
     * Send importing forger private key result to client
     */
    public static final int MSG_START_FORGING_RESULT = 11;

    /**
     * Send block hash list to client
     */
    public static final int MSG_BLOCK_HASH_LIST = 12;

    /**
     * Send pending transactions from mem pool to client
     */
    public static final int MSG_POOL_TXS = 13;

    /**
     * Send chain height to client
     */
    public static final int MSG_CHAIN_HEIGHT = 14;

    /**
     * Send block indexed by number or hash to client
     */
    public static final int MSG_BLOCK = 15;

    /**
     * Send block indexed by starting number or hash to client
     */
    public static final int MSG_BLOCKS = 16;

    /**
     * Send taucoin close done event to client
     */
    public static final int MSG_CLOSE_DONE = 17;

    /**
     * Send account state event to client
     */
    public static final int MSG_ACCOUNT_STATE = 18;

    /**
     * Send block tx reindex Msg to client
     */
    public static final int MSG_BLOCK_TX_REINDEX = 19;

    /**
     * Send stopping sync result to client
     */
    public static final int MSG_STOP_SYNC_RESULT = 20;
}
