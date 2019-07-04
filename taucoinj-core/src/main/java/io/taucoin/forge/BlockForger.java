package io.taucoin.forge;


import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.facade.Taucoin;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import io.taucoin.sync2.ChainInfoManager;
import io.taucoin.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * Created by Anton Nashatyrev on 10.12.2015.
 * Modified by Taucoin Core Developers on 01.29.2019.
 */
@Singleton
public class BlockForger {
    private static final Logger logger = LoggerFactory.getLogger("forge");

    private static final long PULL_TX_POOL_TIMEOUT = 30000;

    private static ExecutorService executor = null;//Executors.newSingleThreadExecutor();

    private Repository repository;

    private Blockchain blockchain;

    private BlockStore blockStore;

    private Taucoin taucoin;

    private CompositeTaucoinListener listener;

    protected PendingState pendingState;

    private ChainInfoManager chainInfoManager;

    private List<ForgerListener> listeners = new CopyOnWriteArrayList<>();

    private Block miningBlock;

    private volatile boolean stopForge;

    private volatile boolean isForging = false;

    private static final int TNO = 50;

    private long nextBlockForgedTimePoint = 0;

    private Object pullTxPoolLock = new Object();

    // Indicate whether pulling pool tx is successful or not.
    private boolean txsGot = false;

    // Sync with remote peer.
    private final Object syncLock = new Object();

    private AtomicBoolean isWaitingSyncDone = new AtomicBoolean(false);

    private ChainInfoManager.ChainInfoListener chainInfoListener
            = new ChainInfoManager.ChainInfoListener() {
        @Override
        public void onChainInfoChanged(long height, byte[] previousBlockHash,
                byte[] currentBlockHash, BigInteger totalDiff, long medianFee) {
            BlockForger.this.onChainInfoChanged(height, currentBlockHash);
        }
    };

    @Inject
    public BlockForger(ChainInfoManager chainInfoManager) {
        this.chainInfoManager = chainInfoManager;
	}

    public void setTaucoin(Taucoin taucoin) {
        this.taucoin = taucoin;
        this.repository = taucoin.getRepository();
        this.blockchain = taucoin.getBlockchain();
        this.blockStore = taucoin.getBlockStore();
        this.pendingState = taucoin.getWorldManager().getPendingState();
        this.listener = (CompositeTaucoinListener)taucoin.getWorldManager().getListener();
    }

    public void init() {
        listener.addListener(new TaucoinListenerAdapter() {

            @Override
            public void onBlockConnected(Block block) {
                BlockForger.this.onBlockConnected(block);
            }

            @Override
            public void onSyncDone() {
                if (CONFIG.forgerStart() && CONFIG.isSyncEnabled()) {
                    logger.info("Sync complete, start forging...");
                    startForging((long)CONFIG.getForgedAmount());
                }
            }
        });

        if (CONFIG.forgerStart() && !CONFIG.isSyncEnabled()) {
            logger.info("Start forging now...");
            startForging((long)CONFIG.getForgedAmount());
        }

        chainInfoManager.addListener(chainInfoListener);
    }

    public void startForging() {
        startForging(-1);
    }

    public synchronized void startForging(long amount) {
        if (isForging()) {
            long timeNow = System.currentTimeMillis() / 1000;
            if (nextBlockForgedTimePoint > timeNow) {
                fireNextBlockForgedInternal(nextBlockForgedTimePoint - timeNow);
            }
            return;
        }

        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }

        this.isForging = true;
        this.stopForge = false;
        executor.submit(new ForgeTask(this, amount));
        fireForgerStarted();
    }

    public synchronized void stopForging() {
        if (!isForging() || stopForge) {
            return;
        }

        this.isForging = false;
        this.stopForge = true;
        executor.shutdownNow();
        executor = null;
        fireForgerStopped(ForgeStatus.FORGE_NORMAL_EXIT);
    }

    public void onForgingStopped(ForgeStatus status) {
        this.isForging = false;
        this.stopForge = true;
        fireForgerStopped(status);
    }

    public boolean isForging() {
        return this.isForging;
    }

    public boolean isForgingStopped() {
        return this.stopForge;
    }

    protected List<Transaction> getAllPendingTransactions() {
        List<Transaction> txList = new ArrayList<Transaction>();
        txList.addAll(pendingState.getWireTransactions());

        List<Transaction> txListTemp;
        if (txList.size() <= TNO) {
            txListTemp = txList;
        } else {
            // Order, Transaction Fee, Time
            // a honest forger who doesn't accept transactions that may come from future.
            txListTemp =  txList.subList(0, TNO);
        }

        return txListTemp;
    }

    private void onBlockConnected(Block newBlock) {
        logger.debug("On block {} {} connected, remote block {}",
                newBlock.getNumber(), Hex.toHexString(newBlock.getHash()),
                Hex.toHexString(chainInfoManager.getCurrentBlockHash()));

        // If forging is running and current block is sync done with remote peer,
        // wakeup forging thread.
        if (isForging() && isWaitingSyncDone.get()
                && Utils.hashEquals(
                        newBlock.getHash(), chainInfoManager.getCurrentBlockHash())) {
            notifySyncDone();
        }
    }

    private void onChainInfoChanged(long height, byte[] currentHash) {
        logger.info("On chain info changed {} {}", height, Hex.toHexString(currentHash));

        // If forging is running and current block is sync done with remote peer,
        // wakeup forging thread.
        if (isForging() && isWaitingSyncDone.get()
                && Utils.hashEquals(
                        currentHash, chainInfoManager.getCurrentBlockHash())) {
            notifySyncDone();
        }
    }

    public ForgeStatus restartForging() {

        Block bestBlock;
        BigInteger baseTarget;
        byte[] generationSignature;
        BigInteger cumulativeDifficulty;

        bestBlock = blockchain.getBestBlock();
        if (!Utils.hashEquals(bestBlock.getHash(), chainInfoManager.getCurrentBlockHash())) {
            try {
                waitForSyncDone();
            } catch (InterruptedException e) {
                logger.warn("Forging task is interrupted when waiting for sync done");
                isWaitingSyncDone.set(false);
                return ForgeStatus.FORGE_TASK_INTERRUPTED_NOT_SYNCED;
            }
            return ForgeStatus.BLOCK_SYNC_PROCESSING;
        }

        baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(bestBlock, blockStore);
        BigInteger forgingPower = repository.getforgePower(CONFIG.getForgerCoinbase());
//        BigInteger balance = repository.getBalance(CONFIG.getForgerCoinbase());
        if (forgingPower.longValue() <= 0) {
            logger.error("Forging Power < 0!!!");
            return ForgeStatus.FORGE_POWER_LESS_THAN_ZERO;
        }

//        long hisAverageFee = bestBlock.getCumulativeFee().longValue()/(bestBlock.getNumber()+1);
//        long medianFee = chainInfoManager.getMedianFee();
//        If medianFee hasn't been pulled from network, wait for a while.
//        while (medianFee <= 0) {
//            logger.warn("Forging task will sleep 2s for getting chaininfo");
//            try {
//                Thread.sleep(2000);
//            } catch (Throwable e) {
//                e.printStackTrace();
//                logger.error("Waiting for chaininfo interrupted");
//                return ForgeStatus.FORGE_NORMAL_EXIT;
//            }
//        }
//        logger.info("balance: {}, median fee: {}", balance, medianFee);
//        if (balance.longValue() < medianFee){
//            logger.info("balance less than median fee");
//            return new ForgeStatus(4,
//                    String.valueOf(medianFee));
//        }

        logger.debug("base target {}, forging power {}", baseTarget, forgingPower);

        generationSignature = ProofOfTransaction.
                calculateNextBlockGenerationSignature(bestBlock.getGenerationSignature(), CONFIG.getForgerPubkey());
        logger.debug("generationSignature {}", Hex.toHexString(generationSignature));

        BigInteger hit = ProofOfTransaction.calculateRandomHit(generationSignature);
        logger.debug("hit {}", hit.longValue());

        long timeInterval = ProofOfTransaction.calculateForgingTimeInterval(hit, baseTarget, forgingPower);
        logger.info("timeInterval {}", timeInterval);
        BigInteger targetValue = ProofOfTransaction.calculateMinerTargetValue(baseTarget, forgingPower, timeInterval);
        logger.debug("target value {}", hit.longValue(), targetValue);
        long timeNow = System.currentTimeMillis() / 1000;
        long timePreBlock = new BigInteger(bestBlock.getTimestamp()).longValue();
        logger.info("Block forged time {}", timePreBlock + timeInterval);

        if (timeNow < timePreBlock + timeInterval) {
            nextBlockForgedTimePoint = timePreBlock + timeInterval;
            long sleepTime = nextBlockForgedTimePoint - timeNow;
            logger.debug("Sleeping " + sleepTime + " s before importing...");
            fireNextBlockForgedInternal(sleepTime);
            fireNextBlockForgedDetail(new NextBlockForgedDetail(baseTarget,
                    new BigInteger(1, generationSignature),
                    bestBlock.getCumulativeDifficulty(), forgingPower, hit));

            synchronized (blockchain.getLockObject()) {
                try {
                    blockchain.getLockObject().wait(sleepTime * 1000);
                } catch (InterruptedException e) {
                    logger.warn("Forging task is interrupted");
                    return ForgeStatus.FORGE_TASK_INTERRUPTED;
                }
            }
        } else {
            logger.info("Forged time has lapsed");
            fireNextBlockForgedInternal(0);
        }

        if (stopForge) {
            logger.info("~~~~~~~~~~~~~~~~~~Stop forging!!!~~~~~~~~~~~~~~~~~~");
            return ForgeStatus.FORGE_NORMAL_EXIT;
        }

        try {
            waitForPullTxPool();
        } catch (InterruptedException e) {
            logger.warn("Forging task is interrupted");
            return ForgeStatus.FORGE_TASK_INTERRUPTED;
        }

        logger.info("Forging thread wakeup...");

        if (!txsGot) {
            logger.warn("Pull pool tx timeout, retry again.");
            return ForgeStatus.PULL_POOL_TX_TIMEOUT;
        }

        cumulativeDifficulty = ProofOfTransaction.
                calculateCumulativeDifficulty(bestBlock.getCumulativeDifficulty(), baseTarget);

        if (bestBlock.equals(blockchain.getBestBlock())) {
            logger.debug("~~~~~~~~~~~~~~~~~~Forging a new block...~~~~~~~~~~~~~~~~~~");
        } else {
            logger.debug("~~~~~~~~~~~~~~~~~~Got a new best block, continue forging...~~~~~~~~~~~~~~~~~~");
            return ForgeStatus.FORGE_CONTINUE;
        }

        miningBlock = blockchain.createNewBlock(bestBlock, baseTarget,
                generationSignature, cumulativeDifficulty, getAllPendingTransactions());

        try {
            // wow, block mined!
            blockForged(miningBlock);
        } catch (InterruptedException | CancellationException e) {
            // OK, we've been cancelled, just exit
            return ForgeStatus.FORGE_INTERRUPTED_OR_CANCELED;
        } catch (Exception e) {
            logger.warn("Exception during mining: ", e);
            return ForgeStatus.EXCEPTION_DURING_FORGING;
        }

        fireBlockStarted(miningBlock);
        return ForgeStatus.FORGE_NORMAL;
    }

    protected void blockForged(Block newBlock) throws InterruptedException {

        fireBlockForged(newBlock);
        logger.debug("Wow, block mined !!!: {}", newBlock.toString());

        miningBlock = null;

        // broadcast the block
        logger.debug("Importing newly mined block:{} fee is: {}",newBlock.getShortHash(),newBlock.getCumulativeFee());
        ImportResult importResult =  taucoin.addNewMinedBlock(newBlock);
        logger.debug("Mined block import result is " + importResult + " : " + newBlock.getShortHash());
    }

    /*****  Listener boilerplate  ******/

    public void addListener(ForgerListener l) {
        listeners.add(l);
    }

    public void removeListener(ForgerListener l) {
        listeners.remove(l);
    }

    protected void fireForgerStarted() {
        for (ForgerListener l : listeners) {
            l.forgingStarted();
        }
    }

    protected void fireForgerStopped(ForgeStatus status) {
        for (ForgerListener l : listeners) {
            l.forgingStopped(status);
        }
    }

    protected void fireBlockStarted(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForgingStarted(b);
        }
    }

    protected void fireBlockCancelled(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForgingCanceled(b);
        }
    }

    protected void fireBlockForged(Block b) {
        for (ForgerListener l : listeners) {
            l.blockForged(b);
        }
    }

    protected void fireNextBlockForgedInternal(long internal) {
        for (ForgerListener l : listeners) {
            l.nextBlockForgedInternal(internal);
        }
    }

    protected void fireNextBlockForgedDetail(NextBlockForgedDetail detail) {
        for (ForgerListener l : listeners) {
            l.nextBlockForgedDetail(detail);
        }
    }

    private void waitForPullTxPool() throws InterruptedException {
        logger.info("Wait for pulling pool txs");
        synchronized(pullTxPoolLock) {
            txsGot = false;
            pullTxPoolLock.wait(PULL_TX_POOL_TIMEOUT);
        }
    }

    public void notifyPullTxPoolFinished() {
        logger.info("Pulling pool txs finished");
        synchronized(pullTxPoolLock) {
            txsGot = true;
            pullTxPoolLock.notify();
        }
    }

    private void waitForSyncDone() throws InterruptedException {
        logger.info("Wait for sync done");
        synchronized(syncLock) {
            isWaitingSyncDone.set(true);
            syncLock.wait();
            isWaitingSyncDone.set(false);
        }
    }

    private void notifySyncDone() {
        logger.info("notify sync done");
        synchronized(syncLock) {
            syncLock.notify();
        }
    }

    // Forge task implementation.
    private static class ForgeTask implements Runnable, ForgerListener {

        BlockForger forger;

        private long forgeTargetAmount = -1;
        private long forgedBlockAmount = 0;

        public ForgeTask(BlockForger forger) {
            this.forger = forger;
            forgeTargetAmount = -1;
            registerForgeListener();
        }

        public ForgeTask(BlockForger forger, long forgeTargetAmount) {
           this.forger = forger;
           forgedBlockAmount = 0;
           this.forgeTargetAmount = forgeTargetAmount;
           registerForgeListener();
        }

        private void registerForgeListener() {
            forger.addListener(this);
        }

        @Override
        public void run() {
            ForgeStatus forgingResult = ForgeStatus.FORGE_NORMAL;
            while (forgingResult.isContinue() && !Thread.interrupted() && !forger.isForgingStopped()
                    && (forgeTargetAmount == -1
                            || (forgeTargetAmount > 0 && forgedBlockAmount < forgeTargetAmount))) {
               forgingResult = forger.restartForging();
            }

            forger.onForgingStopped(forgingResult);
        }

        @Override
        public void forgingStarted() {
            logger.debug("Forging started...");
        }

        @Override
        public void forgingStopped(ForgeStatus status) {
            logger.debug("Forging stopped status: {}",status.getMsg());
            forger.removeListener(this);
        }

        @Override
        public void blockForgingStarted(Block block) {
            logger.debug("Block forging started...");
        }

        @Override
        public void nextBlockForgedInternal(long internal) {
            logger.info("Next block forged wait itme {}s", internal);
        }

        @Override
        public void nextBlockForgedDetail(NextBlockForgedDetail detail) {
        }

        @Override
        public void blockForged(Block block) {
            forgedBlockAmount++;
            logger.info("New Block: {}", Hex.toHexString(block.getHash()));
        }

        @Override
        public void blockForgingCanceled(Block block) {
            logger.info("Block froging canceled: {}", Hex.toHexString(block.getHash()));
            forger.removeListener(this);
        }
     }
}
