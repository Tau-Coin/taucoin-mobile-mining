package io.taucoin.sync2;

import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.core.PendingState;
import io.taucoin.config.SystemProperties;
import io.taucoin.forge.BlockForger;
import io.taucoin.forge.ForgeStatus;
import io.taucoin.forge.ForgerListener;
import io.taucoin.forge.NextBlockForgedDetail;
import io.taucoin.http.RequestManager;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
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

    TaucoinListener tauListener;

    private TaucoinListener txReceivedListener = new TaucoinListenerAdapter() {
        @Override
        public void onPendingTransactionsReceived(List<Transaction> transactions) {
            logger.info("txs received");
            blockForger.notifyPullTxPoolFinished();
        }
    };

    private ScheduledFuture<?> timerTask = null;

    private Runnable task = new Runnable() {
        public void run() {
            try {
                requestManager.startPullPoolTxs(PullPoolTxsAmount);
            } catch (Throwable t) {
                logger.error("Unhandled exception ", t);
            }
        }
    };

    @Inject
    public PoolSynchronizer(TaucoinListener listener,
            BlockForger blockForger, PendingState pendingState) {
        this.tauListener = listener;
        ((CompositeTaucoinListener)this.tauListener).addListener(txReceivedListener);
        this.blockForger = blockForger;
        this.blockForger.addListener(this);
        this.pendingState = pendingState;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel(true);
        }
    }

    public void close() {
        stop();
    }

    @Override
    public void forgingStarted() {
    }

    @Override
    public void forgingStopped(ForgeStatus status) {
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
            startTimerTask(internal * 1000);
        }
    }

    @Override
    public void nextBlockForgedDetail(NextBlockForgedDetail detail) {
    }

    private void startTimerTask(long delay) {
        if (timerTask != null) {
            timerTask.cancel(true);
        }

        timerTask = timer.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
}
