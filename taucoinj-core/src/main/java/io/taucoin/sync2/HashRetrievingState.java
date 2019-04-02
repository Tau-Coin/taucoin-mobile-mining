package io.taucoin.sync2;

import io.taucoin.http.RequestManager;
import io.taucoin.net.server.Channel;
import io.taucoin.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.net.tau.TauVersion.*;
import static io.taucoin.sync2.SyncStateEnum.HASH_RETRIEVING;
import static io.taucoin.sync2.SyncStateEnum.BLOCK_RETRIEVING;
import static io.taucoin.sync2.SyncStateEnum.IDLE;


/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public class HashRetrievingState extends AbstractSyncState {

    private static final Logger logger = LoggerFactory.getLogger("sync2");

    public HashRetrievingState() {
        super(HASH_RETRIEVING);
    }

    @Override
    public void doOnTransition() {
        super.doOnTransition();
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        if (!syncManager.queue.isMoreBlocksNeeded()) {
            syncManager.changeState(IDLE);
            return;
        }

        // if hash retrieving is done all we need to do is just change state and quit
        if (requestManager.isHashRetrievingDone()) {
            syncManager.changeState(BLOCK_RETRIEVING);
            return;
        }
    }
}
