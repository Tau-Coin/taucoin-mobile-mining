package io.taucoin.core;

import io.taucoin.config.Constants;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.datasource.DBCorruptionException;
import io.taucoin.db.BlockStore;
import io.taucoin.db.file.FileBlockStore;
import io.taucoin.debug.RefWatcher;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.sync2.ChainInfoManager;
import io.taucoin.util.AdvancedDeviceUtils;
import io.taucoin.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.Runtime.getRuntime;
import static java.math.BigInteger.ZERO;
import static java.util.Collections.emptyList;
import static io.taucoin.core.ImportResult.*;
import static io.taucoin.util.BIUtil.isMoreThan;

/**
 * The Ethereum blockchain is in many ways similar to the Bitcoin blockchain,
 * although it does have some differences.
 * <p>
 * The main difference between Ethereum and Bitcoin with regard to the blockchain architecture
 * is that, unlike Bitcoin, Ethereum blocks contain a copy of both the transaction list
 * and the most recent state. Aside from that, two other values, the block number and
 * the difficulty, are also stored in the block.
 * </p>
 * The block validation algorithm in Ethereum is as follows:
 * <ol>
 * <li>Check if the previous block referenced exists and is valid.</li>
 * <li>Check that the timestamp of the block is greater than that of the referenced previous block and less than 15 minutes into the future</li>
 * <li>Check that the block number, difficulty, transaction root, uncle root and gas limit (various low-level Ethereum-specific concepts) are valid.</li>
 * <li>Check that the proof of work on the block is valid.</li>
 * <li>Let S[0] be the STATE_ROOT of the previous block.</li>
 * <li>Let TX be the block's transaction list, with n transactions.
 * For all in in 0...n-1, set S[i+1] = APPLY(S[i],TX[i]).
 * If any applications returns an error, or if the total gas consumed in the block
 * up until this point exceeds the GASLIMIT, return an error.</li>
 * <li>Let S_FINAL be S[n], but adding the block reward paid to the miner.</li>
 * <li>Check if S_FINAL is the same as the STATE_ROOT. If it is, the block is valid; otherwise, it is not valid.</li>
 * </ol>
 * See <a href="https://github.com/ethereum/wiki/wiki/White-Paper#blockchain-and-mining">Ethereum Whitepaper</a>
 *
 * @author Roman Mandeleil
 * @author Nick Savers
 * @since 20.05.2014
 */
@Singleton
public class BlockchainImpl implements io.taucoin.facade.Blockchain {


    private static final Logger logger = LoggerFactory.getLogger("blockchain");


    private Repository repository;
    private Repository track;

    private BlockStore blockStore;

    private Block bestBlock;

    private BigInteger totalDifficulty = ZERO;

    private TaucoinListener listener;

    private PendingState pendingState;

    private ChainInfoManager chainInfoManager;

    private FileBlockStore fileBlockStore;

    private TransactionExecutor executor;

    private StakeHolderIdentityUpdate stakeHolderIdentityUpdate;

    SystemProperties config = SystemProperties.CONFIG;

    private Object lock = new Object();

    private List<Chain> altChains = new ArrayList<>();
    private List<Block> garbage = new ArrayList<>();

    long exitOn = Long.MAX_VALUE;

    private boolean fork = false;

    private RefWatcher refWatcher;

    public BlockchainImpl() {
    }

    //todo: autowire over constructor
    @Inject
    public BlockchainImpl(BlockStore blockStore, Repository repository,
            PendingState pendingState, TaucoinListener listener,
            ChainInfoManager chainInfoManager, FileBlockStore fileBlockStore,
            RefWatcher refWatcher) {
        this.blockStore = blockStore;
        this.repository = repository;
        this.pendingState = pendingState;
        this.listener = listener;
        this.chainInfoManager = chainInfoManager;
        this.fileBlockStore = fileBlockStore;
        this.refWatcher = refWatcher;
        this.executor = new TransactionExecutor(this, listener);
        this.stakeHolderIdentityUpdate = new StakeHolderIdentityUpdate();
    }

    @Override
    public byte[] getBestBlockHash() {
        return getBestBlock().getHash();
    }

    @Override
    public long getSize() {
        return bestBlock.getNumber() + 1;
    }

    @Override
    public Block getBlockByNumber(long blockNr) {
        return blockStore.getChainBlockByNumber(blockNr);
    }

    @Override
    public long getBlockTimeByNumber(long blockNumber) {
        return blockStore.getBlockTimeByNumber(blockNumber);
    }

    @Override
    public Transaction getTransactionByHash(byte[] hash) {
        throw new UnsupportedOperationException("TODO: will be implemented soon "); // FIXME: go and fix me
    }

    @Override
    public Block getBlockByHash(byte[] hash) {
        return blockStore.getBlockByHash(hash);
    }

    @Override
    public Object getLockObject() {
        return lock;
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty) {
        return blockStore.getListHashesEndWith(hash, qty);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty) {
        long bestNumber = bestBlock.getNumber();

        if (blockNumber > bestNumber) {
            return emptyList();
        }

        if (blockNumber + qty - 1 > bestNumber) {
            qty = (int) (bestNumber - blockNumber + 1);
        }

        long endNumber = blockNumber + qty - 1;

        Block block = getBlockByNumber(endNumber);

        List<byte[]> hashes = blockStore.getListHashesEndWith(block.getHash(), qty);

        // asc order of hashes is required in the response
        Collections.reverse(hashes);

        return hashes;
    }


    public Repository getRepository() {
        return repository;
    }

    public BlockStore getBlockStore() {
        return blockStore;
    }

    public synchronized ImportResult tryConnectAndFork(final Block block) {
        if (isMoreThan(block.getCumulativeDifficulty(), this.totalDifficulty)) {
            //cumulative difficulty is more than current chain

            //try to roll back and reconnect
            List<Block> undoBlocks = new ArrayList<>();
            List<Block> newBlocks = new ArrayList<>();
            // Note: 'getForkBlocksInfo' method will add 'block' into 'newBlocks'.
            if (!blockStore.getForkBlocksInfo(block, undoBlocks, newBlocks)) {
                logger.error("Can not find continuous branch!");
                blockStore.delNonChainBlock(block.getPreviousHeaderHash());
                return DISCONTINUOUS_BRANCH;
            }

            if (undoBlocks.size() > config.getMutableRange()) {
                logger.info("Blocks to be rolled back are out of mutable range !");
                return IMMUTABLE_BRANCH;
            }

            track = repository.startTracking();

            for (Block undoBlock : undoBlocks) {
                Repository cacheTrack = track.startTracking();
                logger.info("Try to disconnect block, block number: {}, hash: {}",
                        undoBlock.getNumber(), Hex.toHexString(undoBlock.getHash()));
                logger.debug("before roll back.....");
                cacheTrack.showRepositoryChange();
                //roll back
                List<Transaction> txs = undoBlock.getTransactionsList();
                int size = txs.size();
                for (int i = size - 1; i >= 0; i--) {
                    StakeHolderIdentityUpdate stakeHolderIdentityUpdate =
                            new StakeHolderIdentityUpdate(txs.get(i), cacheTrack, undoBlock.getForgerAddress(), undoBlock.getNumber() - 1);
                    stakeHolderIdentityUpdate.rollbackStakeHolderIdentity();
                }

                for (int i = size - 1; i >= 0; i--) {
                    //roll back
                    TransactionExecutor executor = new TransactionExecutor(txs.get(i), cacheTrack, this, listener);
                    executor.setCoinbase(undoBlock.getForgerAddress());
                    executor.undoTransaction();
                }
                logger.debug("after roll back.....");
                cacheTrack.showRepositoryChange();
                cacheTrack.commit();

                if(Hex.toHexString(undoBlock.getForgerAddress()).equals("847ca210e2b61e9722d1584fcc0daea4c3639b09")){
                    logger.warn("after undo special address forge power {} balance {}",
                            track.getforgePower(undoBlock.getForgerAddress()),track.getBalance(undoBlock.getForgerAddress()));
                }
            }

            boolean isValid = true;
            Repository cacheTrack;
            int size = newBlocks.size();
            for (int i = size - 1; i >= 0; i--) {
                cacheTrack = track.startTracking();

                Block newBlock = newBlocks.get(i);
                logger.info("Try to connect block, block number: {}, hash: {}",
                        newBlock.getNumber(), Hex.toHexString(newBlock.getHash()));

                if (!isValidBlock(newBlock, track)) {
                    isValid = false;
                    logger.info("Connect block fail! Cannot verify block, block number: {}, hash: {}",
                            newBlock.getNumber(), Hex.toHexString(newBlock.getHash()));
                    break;
                }

                if (newBlock.getNumber() >= config.traceStartBlock() && config.traceStartBlock() != -1) {
                    AdvancedDeviceUtils.adjustDetailedTracing(newBlock.getNumber());
                }

                if (!processBlock(newBlock, cacheTrack)) {
                    isValid = false;
                    break;
                }

                cacheTrack.commit();
            }

            if (isValid) {
                logger.info("Beginning to re-branch.");
                track.commit();

                blockStore.saveBlock(block, totalDifficulty, true);
                setBestBlock(block);

                blockStore.reBranchBlocks(undoBlocks, newBlocks);

                //broadcast disconnected blocks
                for(Block undoBlock : undoBlocks) {
                    listener.onBlockDisconnected(undoBlock);
                }

                //broadcast connected blocks
                for(int i = newBlocks.size() - 1; i >= 0; i--) {
                    listener.onBlockConnected(newBlocks.get(i));
                }

                //if (needFlush(block)) {
                    repository.flush(block.getNumber());
                    blockStore.flush();
                    System.gc();
                //}

                return IMPORTED_BEST;
            } else {
                track.rollback();

                return INVALID_BLOCK;
            }

        } else {
            //cumulative difficulty is less than current
            //just verify block simply
            if (!verifyBlockSimply(block)) {
                return INVALID_BLOCK;
            }

            blockStore.saveBlock(block, totalDifficulty, false);

            return IMPORTED_NOT_BEST;
        }
    }


    public synchronized ImportResult tryToConnect(final Block block) {

        Block preBlock = blockStore.getBlockByHash(block.getPreviousHeaderHash());
        if (preBlock == null) {
            logger.error("Cannot find parent block! Block hash [{}], previous block hash [{}], raw byte array {} {}.",
                    Hex.toHexString(block.getHash()), Hex.toHexString(block.getPreviousHeaderHash()),
                    block.getHash(), block.getPreviousHeaderHash());
            return NO_PARENT;
        }

        //wrap the block
        if (block.isMsg()) {
            block.setNumber(preBlock.getNumber() + 1);

            BigInteger baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(preBlock, blockStore);
            block.setBaseTarget(baseTarget);

            if (!block.extractForgerPublicKey()) {
                logger.error("Extract forger public key fail!!!");
                return INVALID_BLOCK;
            }

            byte[] generationSignature = ProofOfTransaction.
                    calculateNextBlockGenerationSignature(preBlock.getGenerationSignature(), block.getForgerPublicKey());
            block.setGenerationSignature(generationSignature);

            BigInteger lastCumulativeDifficulty = preBlock.getCumulativeDifficulty();
            BigInteger cumulativeDifficulty = ProofOfTransaction.
                    calculateCumulativeDifficulty(lastCumulativeDifficulty, baseTarget);
            block.setCumulativeDifficulty(cumulativeDifficulty);

            BigInteger curTotalFee = preBlock.getCumulativeFee();
            for(Transaction tr: block.getTransactionsList()){
                curTotalFee = curTotalFee.add(new BigInteger(tr.getFee()));
            }
            block.setCumulativeFee(curTotalFee);
        }

        if (logger.isInfoEnabled())
            logger.info("Try connect block hash: {}, number: {}",
                    Hex.toHexString(block.getHash()).substring(0, 6),
                    block.getNumber());

        if (blockStore.getMaxNumber() >= block.getNumber() &&
                blockStore.isBlockExist(block.getHash())) {

            if (logger.isDebugEnabled())
                logger.debug("Block already exist hash: {}, number: {}",
                        Hex.toHexString(block.getHash()).substring(0, 6),
                        block.getNumber());

            // retry of well known block
            return EXIST;
        }

        // The simple case got the block
        // to connect to the main chain
        if (bestBlock.isParentOf(block)) {
            //recordBlock(block);

            if (addBlock(block)) {
                listener.onBlockConnected(block);
                //notify
                synchronized (lock) {
                    lock.notify();
                }

                final long currentBlockNumber = block.getNumber();
                EventDispatchThread.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (currentBlockNumber > config.blockStoreCapability()) {
                            blockStore.delChainBlockByNumber(
                                    currentBlockNumber - config.blockStoreCapability());
                        }
                    }
                });
                return IMPORTED_BEST;
            } else {
                return INVALID_BLOCK;
            }
        } else {

            if (blockStore.isBlockExist(block.getPreviousHeaderHash())) {
                //recordBlock(block);
                ImportResult result = tryConnectAndFork(block);

                if (result == IMPORTED_BEST) {
                    //notify
                    synchronized (lock) {
                        lock.notify();
                    }

                    final long currentBlockNumber = block.getNumber();
                    EventDispatchThread.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (currentBlockNumber > config.blockStoreCapability()) {
                                blockStore.delChainBlockByNumber(
                                        currentBlockNumber - config.blockStoreCapability());
                            }
                        }
                    });
                }

                return result;
            }

        }

        return NO_PARENT;
    }

    public synchronized Block createNewBlock(Block parent, BigInteger baseTarget, byte[] generationSignature,
                                             BigInteger cumulativeDifficulty, List<Transaction> txs) {

        // adjust time to parent block this may happen due to system clocks difference
        Long time = System.currentTimeMillis() / 1000;
        logger.info("create new block time {}", time);
        byte[] timeStamp = new BigInteger(time.toString()).toByteArray();
        byte version = Constants.BLOCK_VERSION;
        byte option = Constants.BLOCK_OPTION;
        Block block = new Block(version,
                timeStamp,
                parent.getHash(),
                option,
                txs);

        BigInteger curTotalfee = parent.getCumulativeFee();
        if (txs != null) {
            for (Transaction tr : txs) {
                curTotalfee = curTotalfee.add(new BigInteger(tr.getFee()));
            }
        }

        block.setNumber(parent.getNumber() + 1);
        block.setBaseTarget(baseTarget);
        block.setGenerationSignature(generationSignature);
        block.setCumulativeDifficulty(cumulativeDifficulty);
        block.setCumulativeFee(curTotalfee);
        block.sign(config.getForgerPrikey());

        return block;
    }

    @Override
    public synchronized boolean addBlock(Block block) {

        /**
        if (exitOn < block.getNumber()) {
            logger.warn("Exiting after block.number {}", bestBlock.getNumber());
            repository.flush();
            blockStore.flush();
            System.exit(-1);
        }
        */

        /**
        if (!isValidBlock(block, repository)) {
            logger.error("Invalid block with number: {}", block.getNumber());
            return false;
        }
         */

        /**
        if (block.getNumber() >= config.traceStartBlock() && config.traceStartBlock() != -1) {
            AdvancedDeviceUtils.adjustDetailedTracing(block.getNumber());
        }
        */

        track = repository.startTracking();

        if (!processBlock(block, track)) {
            track.rollback();
            return false;
        }

//        try {
            track.commit();
//        }catch (RuntimeException e) {
//            logger.error("track commit error {}",e.getMessage());
//            track.rollback();

//            undoBlockTransactionWrap(block);
//            if (!processBlock(block, track)) {
//                track.rollback();
//                return false;
//            }
//            track.commit();
//        }

        storeBlock(block);

        //if (needFlush(block)) {
            repository.flush(block.getNumber());
            blockStore.flush();
            System.gc();
        //}

        listener.onBlock(block);
        listener.trace(String.format("Block chain size: [ %d ]", this.getSize()));

        return true;
    }

    private boolean needFlush(Block block) {
        if (config.cacheFlushMemory() > 0) {
            return needFlushByMemory(config.cacheFlushMemory());
        } else if (config.cacheFlushBlocks() > 0) {
            return block.getNumber() % config.cacheFlushBlocks() == 0;
        } else {
            return needFlushByMemory(.7);
        }
    }

    private boolean needFlushByMemory(double maxMemoryPercents) {
        return getRuntime().freeMemory() < (getRuntime().totalMemory() * (1 - maxMemoryPercents));
    }


    public Block getParent(BlockHeader header) {

        return blockStore.getBlockByHash(header.getPreviousHeaderHash());
    }


    /**
     * verify block version
     * @param version
     * @return
     */
    private boolean verifyBlockVersion(byte version) {
        return version == Constants.BLOCK_VERSION;
    }

    /**
     * verify block option
     * @param option
     * @return
     */
    private boolean verifyBlockOption(byte option) {
        return option == Constants.BLOCK_OPTION;
    }

    private boolean verifyBlockTime(Block block) {
        long blockTime = ByteUtil.byteArrayToLong(block.getTimestamp());

        Block parentBlock = blockStore.getBlockByHash(block.getPreviousHeaderHash());
        if (parentBlock == null) {
            logger.error("Cannot find parent block!");
            return false;
        }
        long parentBlockTime = ByteUtil.byteArrayToLong(parentBlock.getTimestamp());
        if (blockTime  <= parentBlockTime) {
            logger.error("Block time {} is less than parent block time {}",
                    blockTime, parentBlockTime);
            return false;
        }


        Long localTime = System.currentTimeMillis() / 1000;

        if (blockTime - Constants.MAX_TIMEDRIFT > localTime) {
            logger.error("Block time {} exceeds local time {} by {} seconds",
                    blockTime, localTime, Constants.MAX_TIMEDRIFT);
            return false;
        }

        return true;
    }

    /**
     * verify transaction version,currently version as follows:
     * 0x00
     * 0x01
     * @param tx
     * @return
     */
    private boolean verifyTransactionVersion(Transaction tx) {
        if (tx.getVersion() == TransactionVersion.V01.getCode()) {
            return true;
        }

        logger.error("Tx [{}] version [{}] is mismatch!", Hex.toHexString(tx.getHash()),
                tx.getVersion());
        return false;
    }

    /**
     * verify transaction option,currently option as follows:
     * 0x00
     * 0x01
     * @param tx
     * @return
     */
    private boolean verifyTransactionOption(Transaction tx) {
        if (tx.getOption() == TransactionOptions.TRANSACTION_OPTION_DEFAULT) {
            return true;
        }

        logger.error("Tx [{}] option [{}] is mismatch!", Hex.toHexString(tx.getHash()),
                tx.getOption());
        return false;
    }

    /**
     * verify transaction time
     * @param tx
     * @param blockTime
     * @param bestBlockNum
     * @return
     */
    private boolean verifyTransactionTime(Transaction tx, long blockTime, long bestBlockNum) {

        long txTime = ByteUtil.byteArrayToLong(tx.getTime());
        long lockTime = ByteUtil.byteArrayToLong(tx.getExpireTime());

        if (lockTime > Constants.TX_MAXEXPIRETIME) {
            logger.error("Tx {} expire time is in valid ",ByteUtil.toHexString(tx.getHash()));
            return false;
        }

        if (txTime - Constants.MAX_TIMEDRIFT > blockTime) {
            logger.error("Tx time {} exceeds block time {} by {} seconds", txTime, blockTime, Constants.MAX_TIMEDRIFT);
            return false;
        }

        long referenceTime = 0;
        long referenceHeight = bestBlockNum - lockTime;
        // this is dangerous behavior,a smart node will not accept it because this may be memory overflow hack.
        if (referenceHeight < 0){
            return true;
        }

        if (referenceHeight <= bestBlockNum) {
            referenceTime = getBlockTimeByNumber(referenceHeight);
        } else {
            return false;
        }

        if (txTime < referenceTime) {
            logger.error("Block contains expiration transaction, tx time: {}, reference time : {}", txTime, referenceTime);
            return false;
        }

        return true;
    }

    /**
     * verify Proof of Transaction
     * @param block
     * @return
     */
    private boolean verifyProofOfTransaction(Block block, Repository repo) {
        if (block.getNumber() == 0 ) {
            byte[] genesisHash = Hex.decode(Constants.GENESIS_BLOCK_HASH);
            if (java.util.Arrays.equals(block.getHash(), genesisHash)) {
                return true;
            } else {
                logger.error("Genesis block hash is not right!!! ({})", block.getGenerationSignature());
                return false;
            }
        }

        byte[] address = block.getForgerAddress();
        BigInteger forgingPower = repo.getforgePower(address);
        logger.debug("Address: {}, forge power: {}", Hex.toHexString(address), forgingPower);

        long blockTime = ByteUtil.byteArrayToLong(block.getTimestamp());
        Block preBlock = blockStore.getBlockByHash(block.getPreviousHeaderHash());
        if (preBlock == null) {
            logger.error("Previous block is null with hash {}!",
                    Hex.toHexString(block.getPreviousHeaderHash()));
            return false;
        }
        long preBlockTime = ByteUtil.byteArrayToLong(preBlock.getTimestamp());

        BigInteger targetValue = ProofOfTransaction.
                calculateMinerTargetValue(block.getBaseTarget(), forgingPower, blockTime - preBlockTime);

        logger.debug("Generation Signature {}", Hex.toHexString(block.getGenerationSignature()));
        BigInteger hit = ProofOfTransaction.calculateRandomHit(block.getGenerationSignature());
        logger.debug("verify block target value {}, hit {}", targetValue, hit);

        if (targetValue.compareTo(hit) < 0) {
            logger.error("Target value is smaller than hit!");
            logger.error("dump POT details...");
            logger.error("address: {}, forge power: {}, time: {}, pretime: {}",
                    Hex.toHexString(address), forgingPower, blockTime, preBlockTime);
            logger.error("base target: {}, target value: {}, geneSig: {}, hit: {}",
                    block.getBaseTarget(), targetValue,
                    Hex.toHexString(block.getGenerationSignature()), hit);

            return false;
        }

        return true;
    }

    private boolean verifyBlockSimply(Block block) {
        if (!verifyBlockVersion(block.getVersion())) {
            logger.error("Block version is invalid, block number {}, version {}",
                    block.getNumber(), block.getVersion());
            return false;
        }

        if (!verifyBlockOption(block.getOption())) {
            logger.error("Block version is invalid, block number {}, version {}",
                    block.getNumber(), block.getOption());
            return false;
        }

        if (!verifyBlockTime(block)) {
            logger.error("Verify Block time fail, block number {}", block.getNumber());
            return false;
        }

        List<Transaction> txs = block.getTransactionsList();
        if (txs.size() > Constants.MAX_BLOCKTXSIZE) {
            logger.error("Too many transactions, block number {}", block.getNumber());
            return false;
        }

        return true;
    }

    /**
     * This mechanism enforces a homeostasis in terms of the time between blocks;
     * a smaller period between the last two blocks results in an increase in the
     * difficulty level and thus additional computation required, lengthening the
     * likely next period. Conversely, if the period is too large, the difficulty,
     * and expected time to the next block, is reduced.
     */
    private boolean isValidBlock(Block block, Repository repo) {

        if (!block.isGenesis()) {
            if (!verifyBlockSimply(block))
                return false;

            List<Transaction> txs = block.getTransactionsList();
            long blockTime = ByteUtil.byteArrayToLong(block.getTimestamp());
            long bestBlockNum= bestBlock.getNumber();

            if (!txs.isEmpty()) {
                for (Transaction tx: txs) {
                    if (!verifyTransactionVersion(tx)) {
                        logger.error("Block contains mismatched tx version, block number {}", block.getNumber());
                        return false;
                    }

                    if (!verifyTransactionOption(tx)) {
                        logger.error("Block contains mismatched tx option, block number {}", block.getNumber());
                        return false;
                    }

                    if (!verifyTransactionTime(tx, blockTime, bestBlockNum)) {
                        logger.error("Block contains expiration transaction, block number {}", block.getNumber());
                        return false;
                    }
                }
            }

            if (!verifyProofOfTransaction(block, repo)) {
                logger.error("Verify ProofOfTransaction fail, block number {}", block.getNumber());
                return false;
            }
        }

        return true;
    }

    private void undoBlockTransactionWrap(Block block){
        for (Transaction tx : block.getTransactionsList()) {

        }
    }
    private void wrapBlockTransactions(Block block, Repository repo) {
        long saveTime = System.nanoTime();
        if (block.getNumber() < Constants.FEE_TERMINATE_HEIGHT) {
            for (Transaction tx : block.getTransactionsList()) {
                tx.setIsCompositeTx(false);
                byte[] txSenderAdd = tx.getSender();
                byte[] txReceiverAdd = tx.getReceiveAddress();
                AccountState txSenderAccount = repo.getAccountState(txSenderAdd);
                AccountState txReceiverAccount = repo.getAccountState(txReceiverAdd);

                if (txSenderAdd != null) {
                    byte[] senderWitnessAddress = txSenderAccount.getWitnessAddress();
                    ArrayList<byte[]> senderAssociateAddress = txSenderAccount.getAssociatedAddress();
                    byte[] receiverWitnessAddress = txReceiverAccount.getWitnessAddress();
                    ArrayList<byte[]> receiverAssociateAddress = txReceiverAccount.getAssociatedAddress();

                    if (senderWitnessAddress != null) {
                        tx.setSenderWitnessAddress(senderWitnessAddress);
                    }

                    if (receiverWitnessAddress != null) {
                        tx.setReceiverWitnessAddress(receiverWitnessAddress);
                    }

                    if (senderAssociateAddress != null && senderAssociateAddress.size() > 0) {
                        tx.setSenderAssociatedAddress(senderAssociateAddress);
                    }

                    if (receiverAssociateAddress != null && receiverAssociateAddress.size() > 0) {
                        tx.setReceiverAssociatedAddress(receiverAssociateAddress);
                    }

                    tx.setIsCompositeTx(true);
                }
            }
        }
        long totalTime = System.nanoTime() - saveTime;
        logger.debug("wrap block: num: [{}] hash: [{}], executed after: [{}]nano", block.getNumber(), block.getShortHash(), totalTime);
    }

    private boolean processBlock(Block block, Repository repo) {

        if (!block.isGenesis()) {
            wrapBlockTransactions(block, repo);
            /**
             * maybe i should compare 1 level change and 2 level change in repo.
             * total 3 levels changes.
             */
            logger.debug("=====below operation is level 1 , on level 1 wrap transaction occur =====");
            repo.showRepositoryChange();
            logger.debug("=====below operation is level 2 , on level 2 fee distribute occur =====");
            if (!config.blockChainOnly()) {
                return applyBlock(block, repo);
            }
        }
        return true;
    }

    private boolean applyBlock(Block block, Repository repo) {

        logger.debug("applyBlock: block: [{}] tx.list: [{}]", block.getNumber(), block.getTransactionsList().size());
        long saveTime = System.nanoTime();

        Repository cacheTrack;
        boolean isValid = true;
        int txCount = 0;

        /**
         * when this block is forged by self or newest block synced
         * this will broadcast.
         */
        boolean isAssociatedSelf = false;
        if (chainInfoManager.getHeight() <= block.getNumber() && chainInfoManager.getHeight() != 0) {
            isAssociatedSelf = true;
        }

        for (Transaction tx : block.getTransactionsList()) {
            //logger.info("apply block: [{}] tx: [{}] ", block.getNumber(), tx.toString());
            /**
             * repo is level 1 repo track;
             * cache track is level 2 repo track;
             */
            cacheTrack = repo.startTracking();
            executor.init(tx, cacheTrack);
            executor.setAssociatedByself(isAssociatedSelf);

            //ECKey key = ECKey.fromPublicOnly(block.getGeneratorPublicKey());
            if (!executor.chainInit()) {
                isValid = false;
                cacheTrack.rollback();
                logger.error("Transaction {} is invalid.", Hex.toHexString(tx.getHash()));
                break;
            }
            executor.setCoinbase(block.getForgerAddress());
            if (txCount < block.getTransactionsList().size()-1) {
                executor.executeFinal(block.getHash(), false);
            } else {
                executor.executeFinal(block.getHash(), true);
            }

            cacheTrack.commit();
            txCount++;
        }

        /**
         * here will show changes between wrap tx and distribute fee on level 1
         * lastly level 1 will commit changes to level 0
         * what more level 0 will save these into disk.
         * why process do like this is showing illegal balance.
         */
        repo.showRepositoryChange();
        if (!isValid) {
            return false;
        }

        stakeHolderIdentityUpdate.init(repo, block.getForgerAddress(), block.getNumber());
        for (Transaction tx : block.getTransactionsList()) {
            stakeHolderIdentityUpdate.setTransaction(tx);
            stakeHolderIdentityUpdate.updateStakeHolderIdentity();
        }

        updateTotalDifficulty(block);

        cacheTrack = null;

        long totalTime = System.nanoTime() - saveTime;
        logger.debug("apply block: num: [{}] hash: [{}], executed after: [{}]nano", block.getNumber(), block.getShortHash(), totalTime);

        return true;
    }

    @Override
    public synchronized void storeBlock(Block block) {

        if (fork)
            blockStore.saveBlock(block, totalDifficulty, false);
        else
            blockStore.saveBlock(block, totalDifficulty, true);

        logger.debug("Block saved: number: {}, hash: {}, TD: {}",
                block.getNumber(), block.getShortHash(), totalDifficulty);

        setBestBlock(block);

        if (logger.isDebugEnabled())
            logger.debug("block added to the blockChain: index: [{}]", block.getNumber());
        if (block.getNumber() % 100 == 0)
            logger.info("*** Last block added [ #{} ]", block.getNumber());

    }


    public boolean hasParentOnTheChain(Block block) {
        return getParent(block.getHeader()) != null;
    }

    @Override
    public List<Chain> getAltChains() {
        return altChains;
    }

    @Override
    public List<Block> getGarbage() {
        return garbage;
    }

    @Override
    public synchronized void setBestBlock(Block block) {
        bestBlock = block;
        logger.debug("Set best block with number {}, hash {}, raw {}", bestBlock.getNumber(),
                 Hex.toHexString(bestBlock.getHash()), bestBlock.getHash());
    }

    @Override
    public synchronized Block getBestBlock() {
        // the method is synchronized since the bestBlock might be
        // temporarily switched to the fork while importing non-best block
        return bestBlock;
    }

    @Override
    public void close() {
        blockStore.flush();
        blockStore.close();
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    @Override
    public synchronized void updateTotalDifficulty(Block block) {
        totalDifficulty = block.getCumulativeDifficulty();
        logger.debug("TD: updated to {}", totalDifficulty);
    }

    @Override
    public void setTotalDifficulty(BigInteger totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }

    private void recordBlock(Block block) {

        if (!config.recordBlocks()) return;

        String dumpDir = config.databaseDir() + "/" + config.dumpDir();

        File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {

            dumpFile.getParentFile().mkdirs();
            if (!dumpFile.exists()) dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            if (bestBlock.isGenesis()) {
                bw.write(Hex.toHexString(bestBlock.getEncoded()));
                bw.write("\n");
            }

            bw.write(Hex.toHexString(block.getEncoded()));
            bw.write("\n");

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void startTracking() {
        track = repository.startTracking();
    }

    public void commitTracking() {
        track.commit();
    }

    public void setExitOn(long exitOn) {
        this.exitOn = exitOn;
    }

    public boolean isBlockExist(byte[] hash) {
        return blockStore.isBlockExist(hash);
    }

    public void setPendingState(PendingState pendingState) {
        this.pendingState = pendingState;
    }

    public PendingState getPendingState() {
        return pendingState;
    }

    private int getQty(long blockNumber, long bestNumber, int limit, boolean reverse) {
        if (reverse) {
            return blockNumber - limit + 1 < 0 ? (int) (blockNumber + 1) : limit;
        } else {
            if (blockNumber + limit - 1 > bestNumber) {
                return (int) (bestNumber - blockNumber + 1);
            } else {
                return limit;
            }
        }
    }

    private byte[] getStartHash(long blockNumber, int skip, int qty, boolean reverse) {

        long startNumber;

        if (reverse) {
            startNumber = blockNumber - skip;
        } else {
            startNumber = blockNumber + skip + qty - 1;
        }

        Block block = getBlockByNumber(startNumber);

        if (block == null) {
            return null;
        }

        return block.getHash();
    }

    @Override
    public synchronized List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes) {
        List<byte[]> bodies = new ArrayList<>(hashes.size());

        for (byte[] hash : hashes) {
            Block block = blockStore.getBlockByHash(hash);
            if (block == null) break;
            bodies.add(block.getEncodedBody());
        }

        return bodies;
    }

    @Override
    public synchronized boolean checkSanity() {
        long blockStoreMaxNumber = blockStore.getMaxNumber();
        long stateMaxNumber = repository.getMaxNumber();

        logger.info("blockstore max number {}, state max number {}",
                blockStoreMaxNumber, stateMaxNumber);
        if (blockStoreMaxNumber == stateMaxNumber || stateMaxNumber == -1L) {
            return false;
        }

        // From method 'tryConnect' and 'tryConnectAndFork' it can be found
        // repository state database is updated firstly and then block store.
        // But when app is killed, database consistency maybe happens.
        if (stateMaxNumber == blockStoreMaxNumber + 1) {
            // Rollback state database
            Block undoBlock = fileBlockStore.get(blockStoreMaxNumber + 1).getBlock();

            track = repository.startTracking();
            logger.info("Try to disconnect block with number: {}, hash: {}",
                    undoBlock.getNumber(), Hex.toHexString(undoBlock.getHash()));

            List<Transaction> txs = undoBlock.getTransactionsList();
            int size = txs.size();
            for (int i = size - 1; i >= 0; i--) {
                StakeHolderIdentityUpdate stakeHolderIdentityUpdate =
                        new StakeHolderIdentityUpdate(txs.get(i), track, undoBlock.getForgerAddress(),
                                undoBlock.getNumber() - 1);
                stakeHolderIdentityUpdate.rollbackStakeHolderIdentity();
            }

            for (int i = size - 1; i >= 0; i--) {
                TransactionExecutor executor = new TransactionExecutor(txs.get(i), track, this, listener);
                executor.setCoinbase(undoBlock.getForgerAddress());
                executor.undoTransaction();
            }

            track.commit();
            repository.flush(blockStoreMaxNumber);
        } else if (blockStoreMaxNumber == stateMaxNumber + 1) {
            // Maybe this condition never happens.
            blockStore.delChainBlockByNumber(blockStoreMaxNumber);
        } else {
            String errorStr = String.format(
                    "database corruption, blockstore number %s, statedb number %s",
                    blockStoreMaxNumber, stateMaxNumber);
            logger.error(errorStr);
            //throw new DBCorruptionException(errorStr);
        }

        return true;
    }
}
