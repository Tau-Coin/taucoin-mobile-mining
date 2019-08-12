package io.taucoin.sync2;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.BlockWrapper;
import io.taucoin.core.Blockchain;
import io.taucoin.http.ConnectionManager;
import io.taucoin.http.RequestManager;
import io.taucoin.listener.TaucoinListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.sync2.SyncStateEnum.*;
import static io.taucoin.util.TimeUtils.secondsToMillis;

/**
 * @author Mikhail Kalinin
 * @since 14.07.2015
 */
@Singleton
public class SyncManager {

    private final static Logger logger = LoggerFactory.getLogger("sync2");

    private static final long WORKER_TIMEOUT = secondsToMillis(3);

    SystemProperties config = SystemProperties.CONFIG;

    @Resource
    private Map<SyncStateEnum, SyncState> syncStates = new IdentityHashMap<>();

    private SyncState state;
    private final Object stateMutex = new Object();

    private ScheduledExecutorService worker;

    Blockchain blockchain;

    SyncQueue queue;

    TaucoinListener taucoinListener;

    RequestManager requestManager;

    ChainInfoManager chainInfoManager;

    PoolSynchronizer poolSynchronizer;

    ConnectionManager connectionManager;

    private long getChainInfoTimestamp = 0;
    private long pullChainInfoPeriod = config.pullChainInfoPeriod();

    private AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public SyncManager(Blockchain blockchain, SyncQueue queue,
            TaucoinListener taucoinListener, ChainInfoManager chainInfoManager,
            PoolSynchronizer poolSynchronizer, ConnectionManager connectionManager) {
        this.blockchain = blockchain;
        this.queue = queue;
        this.queue.setSyncManager(this);
        this.taucoinListener = taucoinListener;
        this.requestManager = requestManager;
        this.chainInfoManager = chainInfoManager;
        this.poolSynchronizer = poolSynchronizer;
        this.connectionManager = connectionManager;

        syncStates.put(IDLE, new IdleState());
        syncStates.put(CHAININFO_RETRIEVING, new ChainInfoRetrievingState());
        syncStates.put(HASH_RETRIEVING, new HashRetrievingState());
        syncStates.put(BLOCK_RETRIEVING, new BlockRetrievingState());

        /**
         * preparation 4 state and set state transfer manager
         * to operate this node smoothly.
         */
        for (SyncState state : syncStates.values()) {
            ((AbstractSyncState)state).setSyncManager(this);
        }
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
        this.poolSynchronizer.setRequestManager(requestManager);

        for (SyncState state : syncStates.values()) {
            ((AbstractSyncState)state).setRequestManager(requestManager);
        }
    }

    public void init() {
        // Init sync queue
        this.queue.init();
    }

    public void start() {
        startImport();
        startSyncWithPeer();
    }

    public void stop() {
        stopSyncWithPeer();
        stopImport();
    }

    /**
     * Start state machine. But importing procedure is still on.
     */
    public void startSyncWithPeer() {
        if (started.get()) {
            return;
        }
        started.set(true);

        logger.info("Start sync");

        //set IDLE state at the beginning
        state = syncStates.get(IDLE);
        //set current net work initial sync state.
        changeState(initialState());

        worker = Executors.newSingleThreadScheduledExecutor();
        worker.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    maintainState();
                } catch (Throwable t) {
                    t.printStackTrace();
                    logger.error("Exception in main sync worker", t);
                }
            }
        }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop state machine. But importing procedure is still on.
     */
    public void stopSyncWithPeer() {
        if (!started.get()) {
            return;
        }
        started.set(false);

        logger.info("Stop sync");

        if (worker != null) {
            worker.shutdownNow();
            worker = null;
        }
    }

    public boolean isSyncRunning() {
        return started.get();
    }

    public void startImport() {
        if (queue != null) {
            logger.info("Start importing");
            queue.start();
        }
    }

    public void stopImport() {
        if (queue != null) {
            logger.info("Stop importing");
            queue.stop();
        }
    }

    public void close() {
        if (queue != null) {
            queue.close();
        }
    }

    public void notifyNewBlockImported(BlockWrapper wrapper) {
    }


    /**
     * Local node has it own strategy to disconnect peer his behavior
     * is wrong according local node .and drop block queue saved block
     * coming from this peer.
     */
    public void reportBadAction(byte[] nodeId) {
        queue.dropBlocks(nodeId);

        // TODO decrease peer's reputation

    }

    private SyncStateEnum initialState() {
        return CHAININFO_RETRIEVING;

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

        requestManager.changeSyncState(newStateName);
        if (newStateName == CHAININFO_RETRIEVING) {
            savePullChainInfoTime();
        }
    }

    private void maintainState() {
        synchronized (stateMutex) {
            state.doMaintain();
        }
    }

    private void savePullChainInfoTime() {
        getChainInfoTimestamp = System.currentTimeMillis();
    }

    public boolean hasToPullChainInfo() {
        return pullChainInfoPeriod < System.currentTimeMillis() - getChainInfoTimestamp;
    }

    // Get total blocks amount which were synchronized from peers
    // but have not been imported.
    public long getSynchronizedBlocksAmount() {
        if (queue != null) {
            return queue.getBlockqueueMaxNumber();
        }

        return 0;
    }

    public void notifyHibernation(long number) {
        taucoinListener.onSyncHibernation(number);
    }

    public void notifyBlockQueueRollback(long number) {
        taucoinListener.onBlockQueueRollback(number);
    }
}
