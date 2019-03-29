package io.taucoin.sync2;

/**
 * @author Taucoin Core Developers
 * @since 29.03.2019
 */
public enum SyncStateEnum {

    // Sync module
    IDLE,
    CHAININFO_RETRIEVING,
    HASH_RETRIEVING,
    BLOCK_RETRIEVING,

    // RequestManager
    DONE_CHAININFO_RETRIEVING,
    DONE_HASH_RETRIEVING
}
