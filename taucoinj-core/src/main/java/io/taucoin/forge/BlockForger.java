package io.taucoin.forge;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.taucoin.facade.TaucoinImpl;
import org.apache.commons.collections4.CollectionUtils;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.db.IndexedBlockStore;
import io.taucoin.facade.Taucoin;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.BigIntegers;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Math.max;
import static io.taucoin.config.SystemProperties.CONFIG;


/**
 * Created by Anton Nashatyrev on 10.12.2015.
 * Modified by Taucoin Core Developers on 01.29.2019.
 */
@Singleton
public class BlockForger {
    private static final Logger logger = LoggerFactory.getLogger("forge");

    private static ExecutorService executor = null;//Executors.newSingleThreadExecutor();

    private Repository repository;

    private Blockchain blockchain;

    private BlockStore blockStore;

    private Taucoin taucoin;

    private CompositeTaucoinListener listener;

    protected PendingState pendingState;

    @Inject
    public BlockForger() {
	}

    public void setTaucoin(Taucoin taucoin) {
        this.taucoin = taucoin;
        this.repository = taucoin.getRepository();
        this.blockchain = taucoin.getBlockchain();
        this.blockStore = taucoin.getBlockStore();
        this.pendingState = taucoin.getWorldManager().getPendingState();
        this.listener = (CompositeTaucoinListener)taucoin.getWorldManager().getListener();
    }

    private List<ForgerListener> listeners = new CopyOnWriteArrayList<>();

    private Block miningBlock;

    private volatile boolean stopForge;

    private volatile boolean isForging = false;

    private static final int TNO = 50;

    public void init() {
        listener.addListener(new TaucoinListenerAdapter() {

            @Override
            public void onBlock(Block block) {
                BlockForger.this.onNewBlock(block);
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
    }

    public void startForging() {
        startForging(-1);
    }

    public void startForging(long amount) {
        if (isForging()) {
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

    public void stopForging() {
        this.isForging = false;
        this.stopForge = true;
        executor.shutdownNow();
        executor = null;
        fireForgerStopped();
    }

    public void onForgingStopped() {
        this.isForging = false;
        this.stopForge = true;
        fireForgerStopped();
    }

    public boolean isForging() {
        return this.isForging;
    }

    public boolean isForgingStopped() {
        return this.stopForge;
    }

    protected List<Transaction> getAllPendingTransactions() {
        List<Transaction> txList = new ArrayList<Transaction>();
        txList.addAll(pendingState.getPendingTransactions());
        txList.addAll(pendingState.getWireTransactions());

        if (txList.size() <= TNO) {
            return txList;
        } else {
            // Order, Transaction Fee, Time
            Collections.sort(txList);
            return txList.subList(0, TNO);
        }
    }

    private void onNewBlock(Block newBlock) {
        logger.info("On new block {}", newBlock.getNumber());

        // TODO: wakeup forging sleep thread or interupt forging process.
    }

    public boolean restartForging() {

        Block bestBlock;
        BigInteger baseTarget;
        byte[] generationSignature;
        BigInteger cumulativeDifficulty;

        bestBlock = blockchain.getBestBlock();
        baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(bestBlock, blockStore);
        BigInteger forgingPower = repository.getforgePower(CONFIG.getForgerCoinbase());
        if (forgingPower.longValue() < 0) {
            logger.error("Forging Power < 0!!!");
            return false;
        }

        logger.info("base target {}, forging power {}", baseTarget, forgingPower);

        generationSignature = ProofOfTransaction.
                calculateNextBlockGenerationSignature(bestBlock.getGenerationSignature(), CONFIG.getForgerPubkey());
        logger.info("generationSignature {}", new BigInteger(generationSignature));

        BigInteger hit = ProofOfTransaction.calculateRandomHit(generationSignature);
        logger.info("hit {}", hit.longValue());

        long timeInterval = ProofOfTransaction.calculateForgingTimeInterval(hit, baseTarget, forgingPower);
        logger.info("timeInterval {}", timeInterval);
        BigInteger targetValue = ProofOfTransaction.calculateMinerTargetValue(baseTarget, forgingPower, timeInterval);
        logger.info("target value {}", hit.longValue(), targetValue);
        long timeNow = System.currentTimeMillis() / 1000;
        long timePreBlock = new BigInteger(bestBlock.getTimestamp()).longValue();
        logger.info("Block forged time {}", timePreBlock + timeInterval);

        if (timeNow < timePreBlock + timeInterval) {
            long sleepTime = timePreBlock + timeInterval - timeNow;
            logger.debug("Sleeping " + sleepTime + " s before importing...");
            synchronized (blockchain.getLockObject()) {
                try {
                    blockchain.getLockObject().wait(sleepTime * 1000);
                } catch (InterruptedException e) {
                    logger.warn("Forging task is interrupted");
                    return false;
                }
            }
        }

        if (stopForge) {
            logger.info("~~~~~~~~~~~~~~~~~~Stop forging!!!~~~~~~~~~~~~~~~~~~");
            return false;
        }

        cumulativeDifficulty = ProofOfTransaction.
                calculateCumulativeDifficulty(bestBlock.getCumulativeDifficulty(), baseTarget);

        if (bestBlock.equals(blockchain.getBestBlock())) {
            logger.info("~~~~~~~~~~~~~~~~~~Forging a new block...~~~~~~~~~~~~~~~~~~");
        } else {
            logger.info("~~~~~~~~~~~~~~~~~~Got a new best block, continue forging...~~~~~~~~~~~~~~~~~~");
            return true;
        }

        miningBlock = blockchain.createNewBlock(bestBlock, baseTarget,
                generationSignature, cumulativeDifficulty, getAllPendingTransactions());

        try {
            // wow, block mined!
            blockForged(miningBlock);
        } catch (InterruptedException | CancellationException e) {
            // OK, we've been cancelled, just exit
            return false;
        } catch (Exception e) {
            logger.warn("Exception during mining: ", e);
            return false;
        }

        fireBlockStarted(miningBlock);
        return true;
    }

    protected void blockForged(Block newBlock) throws InterruptedException {

        fireBlockForged(newBlock);
        logger.info("Wow, block mined !!!: {}", newBlock.toString());

        miningBlock = null;

        // broadcast the block
        logger.debug("Importing newly mined block " + newBlock.getShortHash() + " ...");
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

    protected void fireForgerStopped() {
        for (ForgerListener l : listeners) {
            l.forgingStopped();
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
            boolean forgingResult = true;
            while (forgingResult && !Thread.interrupted() && !forger.isForgingStopped()
                    && (forgeTargetAmount == -1
                            || (forgeTargetAmount > 0 && forgedBlockAmount < forgeTargetAmount))) {
               forgingResult = forger.restartForging();
            }

            forger.onForgingStopped();
        }

        @Override
        public void forgingStarted() {
            logger.info("Forging started...");
        }

        @Override
        public void forgingStopped() {
            logger.info("Forging stopped...");
        }

        @Override
        public void blockForgingStarted(Block block) {
            logger.info("Block forging started...");
        }

        @Override
        public void blockForged(Block block) {
            forgedBlockAmount++;
            logger.info("New Block: {}", Hex.toHexString(block.getHash()));
        }

        @Override
        public void blockForgingCanceled(Block block) {
            logger.info("Block froging canceled: {}", Hex.toHexString(block.getHash()));
        }
     }
}
