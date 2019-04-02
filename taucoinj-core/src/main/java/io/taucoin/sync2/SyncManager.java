package io.taucoin.sync2;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.BlockWrapper;
import io.taucoin.core.Blockchain;
import io.taucoin.http.RequestManager;
import io.taucoin.listener.TaucoinListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.net.tau.TauVersion.*;
import static io.taucoin.net.message.ReasonCode.USELESS_PEER;
import static io.taucoin.sync2.SyncStateEnum.*;
import static io.taucoin.util.BIUtil.isIn20PercentRange;
import static io.taucoin.util.TimeUtils.secondsToMillis;

/**
 * @author Mikhail Kalinin
 * @since 14.07.2015
 */
@Singleton
public class SyncManager {

    private final static Logger logger = LoggerFactory.getLogger("sync");

    private static final long WORKER_TIMEOUT = secondsToMillis(3);
    private static final long GAP_RECOVERY_TIMEOUT = secondsToMillis(2);

    SystemProperties config = SystemProperties.CONFIG;

    @Resource
    private Map<SyncStateEnum, SyncState> syncStates = new IdentityHashMap<>();

    private SyncState state;
    private final Object stateMutex = new Object();

    /**
     * block which gap recovery is running for
     */
    private BlockWrapper gapBlock;

    /**
     * true if sync done event was triggered
     * when want n+1 ,get n already.
     */
    private boolean syncDone = false;

    private BigInteger lowerUsefulDifficulty = BigInteger.ZERO;
    private BigInteger highestKnownDifficulty = BigInteger.ZERO;

    private ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    Blockchain blockchain;

    SyncQueue queue;

    TaucoinListener taucoinListener;

    RequestManager requestManager;

    Thread workerThread = null;
    ScheduledExecutorService logWorker = null;

    @Inject
    public SyncManager(Blockchain blockchain, SyncQueue queue,TaucoinListener taucoinListener
                        , RequestManager requestManager) {
        this.blockchain = blockchain;
        this.queue = queue;
        this.queue.setSyncManager(this);
        this.taucoinListener = taucoinListener;
        this.requestManager = requestManager;

        syncStates.put(SyncStateEnum.IDLE, new IdleState());
        syncStates.put(CHAININFO_RETRIEVING,new ChainInfoRetrievingState());
        syncStates.put(SyncStateEnum.HASH_RETRIEVING, new HashRetrievingState());
        syncStates.put(SyncStateEnum.BLOCK_RETRIEVING, new BlockRetrievingState());
        /**
         * preparation 4 state and set state transfer manager.
         * to operate this node smoothly.
         */
        for (SyncState state : syncStates.values()) {
            ((AbstractSyncState)state).setSyncManager(this);
        }
    }

    public void init() {

        // make it asynchronously
        this.workerThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // sync queue
                queue.init();
                // request manager to complete corresponding work at
                // different state that was set by sync manager.
                // what more it should connect to remote peer.
                requestManager.init();
                if (!config.isSyncEnabled()) {
                    logger.info("Sync Manager: OFF");
                    return;
                }
                logger.info("Sync Manager: ON");
                //set IDLE state at the beginning
                state = syncStates.get(IDLE);
                //set current local chain difficulty.
                updateDifficulties();
                //set current net work initial sync state.
                changeState(initialState());

                worker.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updateDifficulties();
                            removeUselessPeers();
                            fillUpPeersPool();
                            maintainState();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            logger.error("Exception in main sync worker", t);
                        }
                    }
                }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.MILLISECONDS);

                if (logger.isInfoEnabled()) {
                    startLogWorker();
                }

            }
        });

        workerThread.start();
    }

    public void stop() {
        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
        if (logWorker != null) {
            logWorker.shutdownNow();
            logWorker = null;
        }
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }

        if (queue != null) {
            queue.stop();
            queue = null;
        }

        if (requestManager != null) {
            requestManager.stop();
            requestManager = null;
        }
    }

    /**
     * after initial state setting, at special situation node will
     * trigger change state in sync queue.
     * this trigger will lead to state in current changing and
     * function maintainState will maintain current state cycle.
     */
    public void onDisconnect() {

        // if master peer has been disconnected
        // we need to process data it sent
        if (requestManager.isHashRetrieving() || requestManager.isHashRetrievingDone()) {
            changeState(BLOCK_RETRIEVING);
        }
    }

    /**
     *if node local block chain number is different from remote peer block chain
     *number,local node should try to recovery this gap between local and
     *remote.
     */
    public void tryGapRecovery(BlockWrapper wrapper) {
        if (!isGapRecoveryAllowed(wrapper)) {
            return;
        }

        if (logger.isDebugEnabled()) logger.debug(
                "Recovering gap: best.number [{}] vs block.number [{}]",
                blockchain.getBestBlock().getNumber(),
                wrapper.getNumber()
        );

        gapBlock = wrapper;

        changeState(HASH_RETRIEVING);
    }

    public BlockWrapper getGapBlock() {
        return gapBlock;
    }

    void resetGapRecovery() {
        this.gapBlock = null;
    }

    /**
     *before time if a peer received a new block from remote peer after it has get
     *all blocks from this peers.
     *local node will think that blocks have sync completed.
     */
    public void notifyNewBlockImported(BlockWrapper wrapper) {
        if (syncDone) {
            return;
        }
        //if new block isn't solid block ,node only get a header
        //then it need to complete work to sync body further.
        //otherwise node need to sync further step.
        if (!wrapper.isSolidBlock()) {
            syncDone = true;
            onSyncDone();

            logger.debug("NEW block.number [{}] imported", wrapper.getNumber());
            // put this block into broadcasting queue.
            //channelManager.onNewForeignBlock(wrapper);
        } else if (logger.isInfoEnabled()) {
            logger.debug(
                    "NEW block.number [{}] block.minsSinceReceiving [{}] exceeds import time limit, continue sync",
                    wrapper.getNumber(),
                    wrapper.timeSinceReceiving() / 1000 / 60
            );
        }
    }

    public boolean isSyncDone() {
        return syncDone;
    }

    /**
     *local node has it own strategy to disconnect peer his behavior
     *is wrong according local node .and drop block queue saved block
     *coming from this peer.
     */
    public void reportBadAction(byte[] nodeId) {

        RequestManager peer = requestManager.getByNodeId(nodeId);

        if (peer != null) {
            logger.info("Peer {}: bad action, drop it", peer.getPeerIdShort());
            peer.disconnect(USELESS_PEER);
        }

        queue.dropBlocks(nodeId);

        // TODO decrease peer's reputation

    }

    /**
     * 2 possible state will be returned.
     * if Block queue is empty hash retrieving
     * if Block queue is not empty imply that request manager
     * is getting block from remote peer and block retrieving
     * is returned.
     */
    private SyncStateEnum initialState() {
        if (queue.isBlocksEmpty() && isIdleTimeout() && state.is(IDLE)){
            return CHAININFO_RETRIEVING;
        }
        if (requestManager.isChainInfoRetrievingDone() &&
                blockchain.getTotalDifficulty().compareTo(requestManager.getTotalDifficulty()) >= 0
        && state.is(CHAININFO_RETRIEVING)){
            return IDLE;
        }
        if (requestManager.isChainInfoRetrievingDone() &&
                blockchain.getTotalDifficulty().compareTo(requestManager.getTotalDifficulty()) < 0
        && state.is(CHAININFO_RETRIEVING)){
            return HASH_RETRIEVING;
        }
        if (!queue.isMoreBlocksNeeded() && state.is(HASH_RETRIEVING)){
            return IDLE;
        }
        if (requestManager.isHashRetrievingDone() && state.is(HASH_RETRIEVING)) {
            logger.info("It seems that BLOCK_RETRIEVING was interrupted, starting from this state now");
            return BLOCK_RETRIEVING;
        }
        if (!queue.isMoreBlocksNeeded() || queue.isHashesEmpty() && state.is(BLOCK_RETRIEVING)){
            return IDLE;
        }
        if (!queue.isHashesEmpty() && state.is(IDLE)){
            return BLOCK_RETRIEVING;
        }
        return IDLE;
    }

    private int gapSize(BlockWrapper block) {
        Block bestBlock = blockchain.getBestBlock();
        return (int) (block.getNumber() - bestBlock.getNumber());
    }

    private void onSyncDone() {
        taucoinListener.onSyncDone();
        logger.info("Main synchronization is finished");
    }

    private boolean isGapRecoveryAllowed(BlockWrapper block) {
        // hashes are not downloaded yet, recovery doesn't make sense at all
        if (state.is(HASH_RETRIEVING)) {
            return false;
        }

        // gap for this block is being recovered
        if (block.equals(gapBlock) && !state.is(IDLE) && !state.is(CHAININFO_RETRIEVING)) {
            logger.trace("Gap recovery is already in progress for block.number [{}]", gapBlock.getNumber());
            return false;
        }

        // ALL blocks are downloaded, we definitely have a gap
        if (!hasBlockHashes()) {
            logger.trace("No hashes/headers left, recover the gap");
            return true;
        }

        // if blocks downloading is in progress
        // and import fails during some period of time
        // then we assume that faced with a gap
        // but anyway NEW blocks must wait until SyncManager becomes idle
        // if block isn't new block and import it failed. node must wait
        // to at least GAP_RECOVERY_TIMEOUT to recovery gap between with
        // remote.
        // if local node is in IDLE state. it can recovery gap between with
        // remote.
        if (!block.isNewBlock()) {
            return block.timeSinceFail() > GAP_RECOVERY_TIMEOUT;
        } else {
            return state.is(IDLE);
        }
    }

    void changeState(SyncStateEnum newStateName) {
        SyncState newState = syncStates.get(newStateName);

        if (state == newState) {
            return;
        }

        logger.info("Changing state from {} to {}", state, newState);

        synchronized (stateMutex) {
            newState.doOnTransition();
            state = newState;
        }
    }

    /**
     *if request manager lost connection with remote peer
     *local node will use proper strategy to avoid being
     *stuck.
     */
    boolean isPeerStuck(RequestManager peer) {
//        SyncStatistics stats = peer.getSyncStats();
//
//        return stats.millisSinceLastUpdate() > PEER_STUCK_TIMEOUT
//                || stats.getEmptyResponsesCount() > 0;
        return false;
    }

    /**
     *if a preparation work is finished, local node (sync manager)
     *can start link with remote peer to run state mechanism .
     *currently this mechanism may not be fully proper.
     */
    void startMaster(RequestManager master) {
        if (requestManager == null || queue == null) {
            logger.warn("Sync manager has been stopped");
            return;
        }

        requestManager.changeSyncState(SyncStateEnum.IDLE);

        //if local block height different from remote block height(means a gap block survived)
        //local node will set last hash to ask at gap block and try to eliminate this gap.
        //else set hash that will be asked to best known according remote peer block chain info.
        if(requestManager.isChainInfoRetrievingDone()) {
            if (gapBlock != null) {
                master.setLastHashToAsk(gapBlock.getHash());
            } else {
                master.setLastHashToAsk(master.getBestKnownHash());
                queue.clearHashes();
                queue.clearHeaders();
            }

            if (logger.isInfoEnabled()) logger.info(
                    "Peer {}: {} initiated, lastHashToAsk [{}], askLimit [{}]",
                    master.getPeerIdShort(),
                    state,
                    Hex.toHexString(master.getLastHashToAsk()),
                    master.getMaxHashesAsk()
            );
            master.changeSyncState(HASH_RETRIEVING);
        }else{
            master.changeSyncState(CHAININFO_RETRIEVING);
        }
    }

    /**
     *as currently protocol tau coin main net.
     *node firstly  will get blocks from it,
     *so only second else branch will be meaningful.
     */
    boolean hasBlockHashes() {
        return !queue.isHashesEmpty() || !queue.isBlocksEmpty();
    }

    private void updateDifficulties() {
        updateLowerUsefulDifficulty(blockchain.getTotalDifficulty());
        updateHighestKnownDifficulty(blockchain.getTotalDifficulty());
    }

    private void updateLowerUsefulDifficulty(BigInteger difficulty) {
        if (difficulty.compareTo(lowerUsefulDifficulty) > 0) {
            lowerUsefulDifficulty = difficulty;
        }
    }

    private void updateHighestKnownDifficulty(BigInteger difficulty) {
        if (difficulty.compareTo(highestKnownDifficulty) > 0) {
            highestKnownDifficulty = difficulty;
        }
    }


    private void startLogWorker() {
        this.logWorker = Executors.newSingleThreadScheduledExecutor();
        logWorker.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    requestManager.logActivePeers();
                    requestManager.logBannedPeers();
                    logger.info("\n");
                    logger.info("State {}\n", state);
                } catch (Throwable t) {
                    t.printStackTrace();
                    logger.error("Exception in log worker", t);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private void removeUselessPeers() {
        List<RequestManager> removed = new ArrayList<>();
        if (requestManager.hasBlocksLack()) {
                logger.info("Peer {}: has no more blocks, ban", requestManager.getPeerIdShort());
                removed.add(requestManager);
                updateLowerUsefulDifficulty(requestManager.getTotalDifficulty());
        }

        // todo decrease peers' reputation

        for (RequestManager peer : removed) {
            requestManager.ban(peer);
        }
    }

    private void fillUpPeersPool() {
        //todo
    }

    private void maintainState() {
        synchronized (stateMutex) {
            state.doMaintain();
        }
    }

    private boolean isIdleTimeout(){
        //todo
        return true;
    }
}
