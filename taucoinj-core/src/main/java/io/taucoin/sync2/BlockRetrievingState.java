package io.taucoin.sync2;

import static io.taucoin.net.tau.TauVersion.V61;
import static io.taucoin.net.tau.TauVersion.V62;
import static io.taucoin.sync2.SyncStateEnum.*;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public class BlockRetrievingState extends AbstractSyncState {

    public BlockRetrievingState() {
        super(BLOCK_RETRIEVING);
    }

    @Override
    public void doOnTransition() {

        super.doOnTransition();

        syncManager.requestManager.changeSyncState(BLOCK_RETRIEVING);
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        if (!syncManager.queue.isMoreBlocksNeeded()) {
            syncManager.changeState(IDLE);
            return;
        }
        
        if ((syncManager.queue.isHashesEmpty()  || !syncManager.requestManager.hasCompatible(V61)) &&
            (syncManager.queue.isHeadersEmpty() || !syncManager.requestManager.hasCompatible(V62))) {

            syncManager.changeState(IDLE);
            return;
        }

        syncManager.requestManager.changeStateForIdles(BLOCK_RETRIEVING);
    }
}
