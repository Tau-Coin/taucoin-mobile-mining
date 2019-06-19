package io.taucoin.sync2;

import io.taucoin.http.RequestManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public abstract class AbstractSyncState implements SyncState {

    protected static final Logger logger = LoggerFactory.getLogger("sync2");

    protected SyncManager syncManager;

    protected RequestManager requestManager;

    protected SyncStateEnum name;

    protected AbstractSyncState(SyncStateEnum name) {
        this.name = name;
    }

    @Override
    public boolean is(SyncStateEnum name) {
        return this.name == name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public void doOnTransition() {
        logger.trace("Transit to {} state", name);
    }

    @Override
    public void doMaintain() {
        logger.trace("Maintain {} state", name);
    }

    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }
}
