package io.taucoin.core;

import io.taucoin.config.Constants;
import io.taucoin.config.SystemProperties;
import io.taucoin.crypto.ECKey;
import io.taucoin.crypto.HashUtil;
import io.taucoin.crypto.SHA3Helper;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.manager.AdminInfo;
import io.taucoin.util.AdvancedDeviceUtils;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.validator.DependentBlockHeaderRule;
import io.taucoin.validator.ParentBlockHeaderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static java.math.BigInteger.ONE;
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
    private static final Logger stateLogger = LoggerFactory.getLogger("state");


    private Repository repository;
    private Repository track;


    private BlockStore blockStore;

    private Block bestBlock;

    private BigInteger totalDifficulty = ZERO;


    Wallet wallet;


    private TaucoinListener listener;


    private AdminInfo adminInfo;


    private DependentBlockHeaderRule parentHeaderValidator;


    private PendingState pendingState;


    SystemProperties config = SystemProperties.CONFIG;

    private Object lock = new Object();

    private List<Chain> altChains = new ArrayList<>();
    private List<Block> garbage = new ArrayList<>();

    long exitOn = Long.MAX_VALUE;

    public boolean byTest = false;
    private boolean fork = false;

    public BlockchainImpl() {
    }

    //todo: autowire over constructor
    @Inject
    public BlockchainImpl(BlockStore blockStore, Repository repository,
                          Wallet wallet, AdminInfo adminInfo,
                          ParentBlockHeaderValidator parentHeaderValidator, PendingState pendingState, TaucoinListener listener) {
        this.blockStore = blockStore;
        this.repository = repository;
        this.wallet = wallet;
        this.adminInfo = adminInfo;
        this.parentHeaderValidator = parentHeaderValidator;
        this.pendingState = pendingState;
        this.listener = listener;
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
                ECKey key;
                try {
                    key = ECKey.signatureToKey(undoBlock.getRawHash(), undoBlock.getblockSignature().toBase64());
                } catch (SignatureException e) {
                    return DISCONNECTED_FAILED;
                }
                if(Hex.toHexString(key.getAddress()).equals("847ca210e2b61e9722d1584fcc0daea4c3639b09")){
                    logger.warn("before undo special address forge power {} balance {}",
                            track.getforgePower(key.getAddress()),track.getBalance(key.getAddress()));
                }

                List<Transaction> txs = undoBlock.getTransactionsList();
                for (int i = txs.size() - 1; i >= 0; i--) {
                    StakeHolderIdentityUpdate stakeHolderIdentityUpdate =
                            new StakeHolderIdentityUpdate(txs.get(i), cacheTrack, key.getAddress(), undoBlock.getNumber());
                    stakeHolderIdentityUpdate.rollbackStakeHolderIdentity();
                }
                cacheTrack.commit();
                cacheTrack = track.startTracking();
                for (int i = txs.size() - 1; i >= 0; i--) {
                    //roll back
                    TransactionExecutor executor = new TransactionExecutor(txs.get(i), cacheTrack, this, listener);
                    executor.setCoinbase(key.getAddress());
                    executor.undoTransaction();
                }
                cacheTrack.commit();

                if(Hex.toHexString(key.getAddress()).equals("847ca210e2b61e9722d1584fcc0daea4c3639b09")){
                    logger.warn("after undo special address forge power {} balance {}",
                            track.getforgePower(key.getAddress()),track.getBalance(key.getAddress()));
                }
            }

            boolean isValid = true;
            Repository cacheTrack;
            for (int i = newBlocks.size() - 1; i >= 0; i--) {
                cacheTrack = track.startTracking();

                Block newBlock = newBlocks.get(i);
                logger.info("Try to connect block, block number: {}, hash: {}",
                        newBlock.getNumber(), Hex.toHexString(newBlock.getHash()));

                if (!isValid(newBlock, track)) {
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

//              if (!byTest && needFlush(block)) {
                repository.flush();
                blockStore.flush();
                System.gc();
//              }

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
            logger.error("Cannot find parent block! Block hash [{}], previous block hash [{}].",
                    Hex.toHexString(block.getHash()), Hex.toHexString(block.getPreviousHeaderHash()));
            return NO_PARENT;
        }

        //wrap the block
        if (block.isMsg()) {
            block.setNumber(preBlock.getNumber() + 1);

            BigInteger baseTarget = ProofOfTransaction.calculateRequiredBaseTarget(preBlock, blockStore);
            block.setBaseTarget(baseTarget);

            ECKey key;
            try {
                key = ECKey.signatureToKey(block.getRawHash(), block.getblockSignature().toBase64());
            }catch (SignatureException e){
                return INVALID_BLOCK;
            }

            byte[] generationSignature = ProofOfTransaction.
                    calculateNextBlockGenerationSignature(preBlock.getGenerationSignature(), key.getCompressedPubKey());
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
            recordBlock(block);

            if (add(block)) {
                listener.onBlockConnected(block);
                //notify
                synchronized (lock) {
                    lock.notify();
                }

                EventDispatchThread.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        //pendingState.processBest(block);
                        if (block.getNumber() > config.getMutableRange()) {
                            blockStore.delNonChainBlocksByNumber(block.getNumber() - config.getMutableRange());
                        }
                    }
                });
                return IMPORTED_BEST;
            } else {
                return INVALID_BLOCK;
            }
        } else {

            if (blockStore.isBlockExist(block.getPreviousHeaderHash())) {
                recordBlock(block);
                ImportResult result = tryConnectAndFork(block);

                if (result == IMPORTED_BEST) {
                    //notify
                    synchronized (lock) {
                        lock.notify();
                    }

                    EventDispatchThread.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            //pendingState.processBest(block);
                            if (block.getNumber() > config.getMutableRange()) {
                                blockStore.delNonChainBlocksByNumber(block.getNumber() - config.getMutableRange());
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
        byte version = (byte)1;
        byte option = (byte)1;
        Block block = new Block(version,
                timeStamp,
                parent.getHash(),
                option,
                txs);
        BigInteger curTotalfee = parent.getCumulativeFee();
        for(Transaction tr: txs){
            curTotalfee = curTotalfee.add(new BigInteger(tr.getFee()));
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
    public synchronized boolean add(Block block) {

        if (exitOn < block.getNumber()) {
            System.out.print("Exiting after block.number: " + bestBlock.getNumber());
            repository.flush();
            blockStore.flush();
            System.exit(-1);
        }

        if (!isValid(block, repository)) {
            logger.warn("Invalid block with number: {}", block.getNumber());
            return false;
        }

        track = repository.startTracking();

        // keep chain continuity
        if (!Arrays.equals(bestBlock.getHash(), block.getPreviousHeaderHash()))
            return false;

        if (block.getNumber() >= config.traceStartBlock() && config.traceStartBlock() != -1) {
            AdvancedDeviceUtils.adjustDetailedTracing(block.getNumber());
        }

        if (!processBlock(block, track)) {
            track.rollback();
            return false;
        }

        track.commit();

        storeBlock(block);


//        if (!byTest && needFlush(block)) {
            repository.flush();
            blockStore.flush();
            System.gc();
//        }

        // Remove all wallet transactions as they already approved by the net
        wallet.removeTransactions(block.getTransactionsList());

        listener.trace(String.format("Block chain size: [ %d ]", this.getSize()));

        listener.onBlock(block);

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


    public boolean isValid(BlockHeader header) {

        Block parentBlock = getParent(header);

        if (!parentHeaderValidator.validate(header, parentBlock.getHeader())) {

            if (logger.isErrorEnabled())
                parentHeaderValidator.logErrors(logger);

            return false;
        }

        return true;
    }

    /**
     * verify block version
     * @param version
     * @return
     */
    private boolean verifyVersion(byte version) {
        return version == (byte) 1;
    }

    /**
     * verify block option
     * @param option
     * @return
     */
    private boolean verifyOption(byte option) {
        return option == (byte) 1;
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
     * verify block signature with public key and signature
     * @param block
     * @return
     */
    private boolean verifyBlockSignature(Block block) {
        return block.verifyBlockSignature();
    }

    /**
     * verify transaction time
     * @param tx
     * @param block
     * @return
     */
    private boolean verifyTransactionTime(Transaction tx, Block block) {
        long blockTime = ByteUtil.byteArrayToLong(block.getTimestamp());

        long txTime = ByteUtil.byteArrayToLong(tx.getTime());

        long lockTime = ByteUtil.byteArrayToLong(tx.getExpireTime());

        if (txTime - Constants.MAX_TIMEDRIFT > blockTime) {
            logger.error("Tx time {} exceeds block time {} by {} seconds", txTime, blockTime, Constants.MAX_TIMEDRIFT);
            return false;
        }
        long referenceTime = 0;
        long referenceHeight = bestBlock.getNumber() - lockTime;
        // this is dangerous behavior,a smart node will not accept it because this may be memory overflow hack.
        if (referenceHeight < 0){
            return true;
        }
        if (referenceHeight <= bestBlock.getNumber()) {
            referenceTime = ByteUtil.byteArrayToLong(blockStore.
                    getChainBlockByNumber(referenceHeight).getTimestamp());
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
            if (Arrays.equals(block.getHash(), genesisHash)) {
                return true;
            } else {
                logger.error("Genesis block hash is not right!!! ({})", block.getGenerationSignature());
                return false;
            }
        }
        ECKey key = null;
        try{
            key = ECKey.signatureToKey(block.getRawHash(),block.getblockSignature().toBase64());
        } catch (SignatureException e){

        }

        if( key == null ){
            logger.error("miner pubkey is null .....");
            key = new ECKey();
        }

        //ECKey key = ECKey.fromPublicOnly(block.getGeneratorPublicKey());
        byte[] address = key.getAddress();
        BigInteger forgingPower = repo.getforgePower(address);
        logger.info("Address: {}, forge power: {}", Hex.toHexString(address), forgingPower);

        long blockTime = ByteUtil.byteArrayToLong(block.getTimestamp());
        Block preBlock = blockStore.getBlockByHash(block.getPreviousHeaderHash());
        if (preBlock == null) {
            logger.error("Previous block is null!");
            return false;
        }
        long preBlockTime = ByteUtil.byteArrayToLong(preBlock.getTimestamp());

        BigInteger targetValue = ProofOfTransaction.
                calculateMinerTargetValue(block.getBaseTarget(), forgingPower, blockTime - preBlockTime);

        logger.info("Generation Signature {}", Hex.toHexString(block.getGenerationSignature()));
        BigInteger hit = ProofOfTransaction.calculateRandomHit(block.getGenerationSignature());
        logger.info("verify block target value {}, hit {}", targetValue, hit);

        if (targetValue.compareTo(hit) < 0) {
            logger.error("Target value is smaller than hit!");
            return false;
        }

        return true;
    }

    private boolean verifyBlockSimply(Block block) {
        if (!verifyVersion(block.getVersion())) {
            logger.error("Block version is invalid, block number {}, version {}",
                    block.getNumber(), block.getVersion());
            return false;
        }

        if (!verifyOption(block.getOption())) {
            logger.error("Block version is invalid, block number {}, version {}",
                    block.getNumber(), block.getOption());
            return false;
        }

        if (!verifyBlockSignature(block)) {
            logger.error("Block signature is invalid, block number {}", block.getNumber());
            return false;
        }

        if (!verifyBlockTime(block)) {
            logger.error("Verify Block time fail, block number {}", block.getNumber());
            return false;
        }

//        if (!isValid(block.getHeader())) {
//            return false;
//        }

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
    private boolean isValid(Block block, Repository repo) {

        if (!block.isGenesis()) {
            if (!verifyBlockSimply(block))
                return false;

            List<Transaction> txs = block.getTransactionsList();
            if (!txs.isEmpty()) {
                for (Transaction tx: txs) {
                    if (!verifyTransactionTime(tx, block)) {
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

    private void wrapBlockTransactions(Block block, Repository repo) {
        for (Transaction tx : block.getTransactionsList()) {
            tx.setIsCompositeTx(false);
            if(tx.getSender() != null) {
                logger.info("tx sender address is ====> {}",Hex.toHexString(tx.getSender()));
                logger.info("is sender account empty ====> {}",repo.getAccountState(tx.getSender()) == null);
                byte[] senderWitnessAddress = repo.getAccountState(tx.getSender()).getWitnessAddress();
                ArrayList<byte[]> senderAssociateAddress = repo.getAccountState(tx.getSender()).getAssociatedAddress();
                byte[] receiverWitnessAddress = repo.getAccountState(tx.getReceiveAddress()).getWitnessAddress();
                ArrayList<byte[]> receiverAssociateAddress = repo.getAccountState(tx.getReceiveAddress()).getAssociatedAddress();
                if (senderWitnessAddress != null) {
                    tx.setSenderWitnessAddress(senderWitnessAddress);
                }

                if (receiverWitnessAddress != null) {
                    tx.setReceiverWitnessAddress(receiverWitnessAddress);
                }

                if (senderAssociateAddress != null && senderAssociateAddress.size()> 0) {
                    tx.setSenderAssociatedAddress(senderAssociateAddress);
                }

                if (receiverAssociateAddress != null && receiverAssociateAddress.size()> 0) {
                    tx.setReceiverAssociatedAddress(receiverAssociateAddress);
                }

                tx.setIsCompositeTx(true);
            }
        }
    }

    private boolean processBlock(Block block, Repository repo) {

        if (!block.isGenesis()) {
            wrapBlockTransactions(block, repo);

            if (!config.blockChainOnly()) {
//                wallet.addTransactions(block.getTransactionsList());
                return applyBlock(block, repo);
//                wallet.processBlock(block);
            }
        }
        return true;
    }

    private boolean applyBlock(Block block, Repository repo) {

        logger.info("applyBlock: block: [{}] tx.list: [{}]", block.getNumber(), block.getTransactionsList().size());
        long saveTime = System.nanoTime();

        Repository cacheTrack;
        boolean isValid = true;
        ECKey key = null;
        try {
             key = ECKey.signatureToKey(block.getRawHash(), block.getblockSignature().toBase64());
        }catch (SignatureException e){
            return false;
        }
        int txCount = 0;
        for (Transaction tx : block.getTransactionsList()) {
            stateLogger.info("apply block: [{}] tx: [{}] ", block.getNumber(), tx.toString());

            cacheTrack = repo.startTracking();
            TransactionExecutor executor = new TransactionExecutor(tx, cacheTrack,this,listener);

            //ECKey key = ECKey.fromPublicOnly(block.getGeneratorPublicKey());
            if (!executor.init()) {
                isValid = false;
                cacheTrack.rollback();
                logger.error("Transaction [{}] is invalid.", tx.getHash());
                break;
            }
            executor.setCoinbase(key.getAddress());
            if (txCount < block.getTransactionsList().size()-1) {
                executor.executeFinal(block.getHash(), false);
            } else {
                executor.executeFinal(block.getHash(), true);
            }

            cacheTrack.commit();
            txCount++;
        }

        if (!isValid) {
            return false;
        }

        for (Transaction tx : block.getTransactionsList()) {
            StakeHolderIdentityUpdate stakeHolderIdentityUpdate =
                    new StakeHolderIdentityUpdate(tx, repo, key.getAddress(), block.getNumber());
            stakeHolderIdentityUpdate.updateStakeHolderIdentity();
        }

        updateTotalDifficulty(block);

        long totalTime = System.nanoTime() - saveTime;
        adminInfo.addBlockExecTime(totalTime);
        logger.info("block: num: [{}] hash: [{}], executed after: [{}]nano", block.getNumber(), block.getShortHash(), totalTime);

        return true;
    }

    @Override
    public synchronized void storeBlock(Block block) {

        if (fork)
            blockStore.saveBlock(block, totalDifficulty, false);
        else
            blockStore.saveBlock(block, totalDifficulty, true);

        logger.info("Block saved: number: {}, hash: {}, TD: {}",
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
    public void setBestBlock(Block block) {
        bestBlock = block;
    }

    @Override
    public synchronized Block getBestBlock() {
        // the method is synchronized since the bestBlock might be
        // temporarily switched to the fork while importing non-best block
        return bestBlock;
    }

    @Override
    public void close() {
        blockStore.close();
    }

    @Override
    public BigInteger getTotalDifficulty() {
        return totalDifficulty;
    }

    @Override
    public synchronized void updateTotalDifficulty(Block block) {
        totalDifficulty = block.getCumulativeDifficulty();
        logger.info("TD: updated to {}", totalDifficulty);
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

    public void setParentHeaderValidator(DependentBlockHeaderRule parentHeaderValidator) {
        this.parentHeaderValidator = parentHeaderValidator;
    }

    public void setPendingState(PendingState pendingState) {
        this.pendingState = pendingState;
    }

    public PendingState getPendingState() {
        return pendingState;
    }

    @Override
    public synchronized List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse) {
        long blockNumber = identifier.getNumber();

        if (identifier.getHash() != null) {
            Block block = getBlockByHash(identifier.getHash());

            if (block == null) {
                return emptyList();
            }

            blockNumber = block.getNumber();
        }

        long bestNumber = bestBlock.getNumber();

        if (bestNumber < blockNumber) {
            return emptyList();
        }

        int qty = getQty(blockNumber, bestNumber, limit, reverse);

        byte[] startHash = getStartHash(blockNumber, skip, qty, reverse);

        if (startHash == null) {
            return emptyList();
        }

        List<BlockHeader> headers = blockStore.getListHeadersEndWith(startHash, qty);

        // blocks come with falling numbers
        if (!reverse) {
            Collections.reverse(headers);
        }

        return headers;
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

}
