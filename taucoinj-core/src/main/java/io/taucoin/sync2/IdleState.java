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

        if ((syncManager.queue.isMoreBlocksNeeded() || syncManager.queue.noParent) &&
                ((!syncManager.queue.isHashesEmpty()  && syncManager.requestManager.hasCompatible(V61)) ||
                (!syncManager.queue.isHeadersEmpty() && syncManager.requestManager.hasCompatible(V62)))) {

            // there are new hashes in the store
            // it's time to download blocks
            syncManager.changeState(BLOCK_RETRIEVING);

        } else if ((syncManager.queue.isBlocksEmpty() || syncManager.queue.noParent) && !syncManager.isSyncDone()) {

            // queue is empty and sync not done yet
            // try to download hashes again
            syncManager.resetGapRecovery();
            syncManager.changeState(HASH_RETRIEVING);

        }
    }
}
