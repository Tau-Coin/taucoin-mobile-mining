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
        //all peers change state to block retrieving.
        //means that current time these peer are proper to do retrieving block work.
        syncManager.requestManager.changeSyncState(BLOCK_RETRIEVING);
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        //block queue is full or exceed maximum.
        //if retrieving block further will lead to memory disaster.
        if (!syncManager.queue.isMoreBlocksNeeded()) {
            syncManager.changeState(IDLE);
            return;
        }

        //hash queue is empty.
        //there are no blocks corresponding to hashes for retrieving.
        if (syncManager.queue.isHashesEmpty()) {

            syncManager.changeState(IDLE);
            return;
        }
        //app node only has a peer,so this peer will be very busy.
        //make use of every single idle free peer node.
        syncManager.requestManager.changeStateForIdles(BLOCK_RETRIEVING);
    }
}
