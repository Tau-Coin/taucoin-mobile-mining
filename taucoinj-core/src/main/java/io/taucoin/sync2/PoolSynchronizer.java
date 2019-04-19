package io.taucoin.sync2;

import io.taucoin.core.Block;
import io.taucoin.core.PendingState;
import io.taucoin.config.SystemProperties;
import io.taucoin.forge.BlockForger;
import io.taucoin.forge.ForgerListener;
import io.taucoin.http.RequestManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class PoolSynchronizer implements ForgerListener {

    private static final Logger logger = LoggerFactory.getLogger("sync2");

    private static final long PullPoolTxsTime = CONFIG.pullPoolTxsTime();
    private static final long PullPoolTxsAmount = CONFIG.pullPoolTxsAmount();
    private static final long PullPoolTxsMinFee = CONFIG.pullPoolTxsMinFee();

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "PoolSyncTimer-" + cnt.getAndIncrement());
        }
    });

    BlockForger blockForger;

    PendingState pendingState;

    RequestManager requestManager;

    private ScheduledFuture<?> timerTask = null;

    private Runnable task = new Runnable() {
        public void run() {
            long requestAmount = PullPoolTxsAmount - (long)pendingState.size();
            if (requestAmount <= 0) {
                return;
            }
            try {
                requestManager.startPullPoolTxs(requestAmount, PullPoolTxsMinFee);
            } catch (Throwable t) {
                logger.error("Unhandled exception ", t);
            }
        }
    };

    @Inject
    public PoolSynchronizer(BlockForger blockForger, PendingState pendingState) {
        this.blockForger = blockForger;
        this.blockForger.addListener(this);
        this.pendingState = pendingState;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public void close() {
        if (timerTask != null) {
            timerTask.cancel(true);
        }
    }

    @Override
    public void forgingStarted() {
    }

    @Override
    public void forgingStopped(String outcome) {
        close();
    }

    @Override
    public void blockForgingStarted(Block block) {
    }

    @Override
    public void blockForged(Block block) {
    }

    @Override
    public void blockForgingCanceled(Block block) {
    }

    @Override
    public void nextBlockForgedInternal(long internal) {
        logger.info("Next block forged wait itme {}s", internal);

        if (internal <= PullPoolTxsTime) {
            startTimerTask(10);
        } else {
            // Start a timer to pull pool txs
            startTimerTask((internal - PullPoolTxsTime) * 1000);
        }
    }

    private void startTimerTask(long delay) {
        if (timerTask != null) {
            timerTask.cancel(true);
        }

        timerTask = timer.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
}
