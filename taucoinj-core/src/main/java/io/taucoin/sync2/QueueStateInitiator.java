package io.taucoin.sync2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import static io.taucoin.sync2.SyncStateEnum.BLOCK_RETRIEVING;
import static io.taucoin.sync2.SyncStateEnum.HASH_RETRIEVING;

/**
 * @author Mikhail Kalinin
 * @since 16.12.2015
 */
public class QueueStateInitiator implements StateInitiator {

    private final static Logger logger = LoggerFactory.getLogger("sync");

    private SyncQueue queue;

    @Override
    public SyncStateEnum initiate() {
        if (queue.hasSolidBlocks()) {
            logger.info("It seems that BLOCK_RETRIEVING was interrupted, starting from this state now");
            return BLOCK_RETRIEVING;
        } else {
            return HASH_RETRIEVING;
        }
    }
}
