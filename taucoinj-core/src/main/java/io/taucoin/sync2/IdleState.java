package io.taucoin.sync2;

import static io.taucoin.net.tau.TauVersion.*;
import static io.taucoin.sync2.SyncStateEnum.*;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public class IdleState extends AbstractSyncState {

    public IdleState() {
        super(SyncStateEnum.IDLE);
    }

    @Override
    public void doOnTransition() {

        super.doOnTransition();

        syncManager.requestManager.changeSyncState(IDLE);
    }

    @Override
    public void doMaintain() {
        super.doMaintain();
        if (!syncManager.queue.isHashesEmpty()) {
            // there are new hashes in the store
            // it's time to download blocks
            syncManager.changeState(BLOCK_RETRIEVING);
            return;
        }

        if (syncManager.queue.isBlocksEmpty() && syncManager.queue.isHashesEmpty()) {
            syncManager.changeState(CHAININFO_RETRIEVING);
            return;
        }
        //a lucky peer!
        syncManager.requestManager.changeStateForIdles(IDLE);
    }
}
