package io.taucoin.http.message;

import io.taucoin.core.Blockchain;
import io.taucoin.http.message.Message;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.sync2.ChainInfoManager;
import io.taucoin.sync2.SyncStateEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     (1) Sync state change invoke getting chain information or getting hashes.
 *     (2) Handle inbound messages, etc, ChainInfoMessage, HashesMessage and so on.
 */
@Singleton
public class RequestManager {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    protected Blockchain blockchain;
    protected TaucoinListener listener;
    protected ChainInfoManager chainInfoManager;

    private Object stateLock = new Object();
    private SyncStateEnum syncState = SyncStateEnum.IDLE;

    @Inject
    public RequestManager(Blockchain blockchain, TaucoinListener listener,
            ChainInfoManager chainInfoManager) {
        this.blockchain = blockchain;
        this.listener = listener;
        this.chainInfoManager = chainInfoManager;
    }

    public void changeSyncState(SyncStateEnum state) {
        synchronized(stateLock) {
            this.syncState = state;
        }
    }

    public SyncStateEnum getSyncState() {
        synchronized(stateLock) {
            return this.syncState;
        }
    }

    public void handleMessage(Message message) {
    }
}
