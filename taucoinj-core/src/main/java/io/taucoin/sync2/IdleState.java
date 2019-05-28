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
    }

    @Override
    public void doMaintain() {
        super.doMaintain();
        if (!syncManager.queue.isBlockNumbersEmpty()) {
            // there are new block numbers in the store
            // it's time to download blocks
            syncManager.changeState(BLOCK_RETRIEVING);
            return;
        }

        if (syncManager.queue.isImportingBlocksFinished() && syncManager.queue.isBlockNumbersEmpty()
                && syncManager.hasToPullChainInfo()) {
            syncManager.changeState(CHAININFO_RETRIEVING);
            return;
        }
    }
}
