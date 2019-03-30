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

    private static final Logger logger = LoggerFactory.getLogger("sync");

    public HashRetrievingState() {
        super(HASH_RETRIEVING);
    }

    @Override
    public void doMaintain() {

        super.doMaintain();

        if (!syncManager.queue.isMoreBlocksNeeded() && !syncManager.queue.noParent) {
            syncManager.changeState(IDLE);
            return;
        }

        RequestManager peer = syncManager.requestManager;

        // if hash retrieving is done all we need to do is just change state and quit
        if (peer.isHashRetrievingDone()) {
            syncManager.changeState(BLOCK_RETRIEVING);
            return;
        }

        if (peer.isHashRetrieving()) {
            //todo
            /**
             * node should come into a new state.
             */
        }

        if (peer != null) {
            // if master is stuck ban it and process data it sent
            if(syncManager.isPeerStuck(peer)) {
                syncManager.requestManager.ban(peer);
                logger.info("Master peer {}: banned due to stuck timeout exceeding", peer.getPeerIdShort());

                // let's see what do we have
                // before proceed with HASH_RETRIEVING
                syncManager.changeState(BLOCK_RETRIEVING);
                return;
            }
        }

        if (peer == null) {
            logger.trace("HASH_RETRIEVING is in progress, starting master peer");

            // recovering gap with gap block peer
            if (syncManager.getGapBlock() != null) {
                peer = syncManager.requestManager.getByNodeId(syncManager.getGapBlock().getNodeId());
            }

            if (peer == null) {
                peer = syncManager.requestManager.getMaster();
            }

            if (peer == null) {
                return;
            }
            syncManager.startMaster(peer);
        }

        // Since Eth V61 it makes sense to download blocks and hashes simultaneously
        if (peer.getTauVersion().getCode() > V60.getCode()) {
            syncManager.requestManager.changeStateForIdles(BLOCK_RETRIEVING, syncManager.masterVersion);
        }
    }
}
