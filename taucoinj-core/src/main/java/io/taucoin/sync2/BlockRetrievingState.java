package io.taucoin.sync2;

import static io.taucoin.sync2.SyncStateEnum.*;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 * @since 01.4.2019 by taucoin core
 */
public class BlockRetrievingState extends AbstractSyncState {

    public BlockRetrievingState() {
        super(BLOCK_RETRIEVING);
    }

    @Override
    public void doOnTransition() {
        super.doOnTransition();
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        // block queue is full or exceed maximum.
        // if retrieving block further will lead to memory disaster.
        if (!syncManager.queue.isMoreBlocksNeeded()) {
            syncManager.changeState(IDLE);
            return;
        }

        // hash queue is empty.
        // there are no blocks corresponding to hashes for retrieving.
        if (syncManager.queue.isHashesEmpty()) {

            syncManager.changeState(IDLE);
            return;
        }
    }
}
