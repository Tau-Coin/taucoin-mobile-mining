package io.taucoin.sync2;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.datasource.DBCorruptionException;
import io.taucoin.datasource.mapdb.MapDBFactory;
import io.taucoin.db.*;
import io.taucoin.db.file.BlockQueueFileSys;
import io.taucoin.db.file.FileBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;

import static java.lang.Thread.sleep;
import static io.taucoin.core.ImportResult.IMPORTED_NOT_BEST;
import static io.taucoin.core.ImportResult.NO_PARENT;
import static io.taucoin.core.ImportResult.IMPORTED_BEST;
import static io.taucoin.core.ImportResult.EXIST;

/**
 * The processing queue for blocks to be validated and added to the blockchain.
 * This class also maintains the list of hashes from the peer with the heaviest sub-tree.
 * Based on these hashes, blocks are added to the queue.
 *
 * @author Roman Mandeleil
 * @author Mikhail Kalinin
 * @since 27.07.2014
 */
@Singleton
public class SyncQueue {

    private static final Logger logger = LoggerFactory.getLogger("blockqueue");

    private static final int SCAN_BLOCKS_LIMIT = 1000;

    // BlockQueueMem implementation configuration.
    //private static final int BLOCK_QUEUE_LIMIT = 20000;

    // BlockQueueImpl implementation configuration.
    private static final int BLOCK_QUEUE_LIMIT = Integer.MAX_VALUE;

    /**
     * Store holding a list of hashes of the heaviest chain on the network,
     * for which this client doesn't have the blocks yet
     */
    private HashStore hashStore;

    /**
     * Store holding a list of block headers of the heaviest chain on the network,
     * for which this client doesn't have the blocks yet
     * this may be redundant.
     */
    private HeaderStore headerStore;

    /**
     * Store holding a list of block numbers of the blocks which need sync from peers.
     */
    private BlockNumberStore blockNumbersStore;

    /**
     * Queue with blocks to be validated and added to the blockchain
     */
    private BlockQueue blockQueue;

    private FileBlockStore fileBlockStore;

    private AtomicBoolean noParent = new AtomicBoolean(false);

    private final Object noParentLock = new Object();

    SystemProperties config = SystemProperties.CONFIG;

    private static final long HIBERNATION_CYCLE
            = SystemProperties.CONFIG.hibernationCycle();

    private static final long HIBERNATION_DURATION
            = SystemProperties.CONFIG.hibernationDuration();

    private Blockchain blockchain;

    private SyncManager syncManager;

    private MapDBFactory mapDBFactory;

    private Thread worker = null;

    private byte[] genesisBlockHash = null;

    private AtomicBoolean inited = new AtomicBoolean(false);

    // The task of connecting blocks.
    private Runnable queueProducer = new Runnable(){

        @Override
        public void run() {
            produceQueue();
        }
   };

    private AtomicBoolean isImportingBlocks = new AtomicBoolean(false);

    // As soon as possbile stop connecting worker.
    private AtomicBoolean isRequestStopped = new AtomicBoolean(true);
    private final Object requestStoppedLock = new Object();

    private AtomicBoolean isRequestClose = new AtomicBoolean(false);

    public SyncQueue(Blockchain blockchain, MapDBFactory mapDBFactory,
            FileBlockStore fileBlockStore) {
        this.blockchain = blockchain;
        this.mapDBFactory = mapDBFactory;
        this.fileBlockStore = fileBlockStore;
    }

    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    /**
     * Loads HashStore and BlockQueue from disk,
     * starts {@link #produceQueue()} thread
     */
    public void init() {
        if (inited.get()) {
            return;
        }

        logger.info("Start loading sync queue");

        hashStore = new HashStoreMem();
        headerStore = new HeaderStoreMem();
        blockNumbersStore = new BlockNumberStoreMem();
        blockQueue = new BlockQueueFileSys(fileBlockStore);
        ((BlockQueueFileSys)blockQueue).setBlockchain(blockchain);

        hashStore.open();
        headerStore.open();
        blockNumbersStore.open();
        blockQueue.open();

        if (!config.isSyncEnabled()) {
            logger.warn("Sync disabled");
            return;
        }

        // Init thread of connecting blocks.
        this.worker = new Thread (queueProducer);
        worker.start();

        inited.set(true);
    }

    public synchronized void start() {
        // If not inited, has been started or has been closed, just return;
        if (!inited.get() || !isRequestStopped.get() || isRequestClose.get()) {
            return;
        }

        isRequestStopped.set(false);
        notifyStart();
    }

    public synchronized void stop() {
        if (isRequestStopped.get() || isRequestClose.get()) {
            return;
        }

        isRequestStopped.set(true);

        // Clear cache
        if (hashStore != null) {
            hashStore.clear();
        }
        if (headerStore != null) {
            headerStore.clear();
        }
        if (blockNumbersStore != null) {
            blockNumbersStore.clear();
        }
        if (blockQueue != null) {
            if (blockQueue instanceof BlockQueueImpl) {
                ((BlockQueueImpl)blockQueue).flush();
            }
        }
    }

    public synchronized void close() {

        if (!inited.get() || isRequestClose.get()) {
            return;
        }

        isRequestClose.set(true);

        // Close db.
        if (hashStore != null) {
            hashStore.close();
        }
        if (headerStore != null) {
            headerStore.close();
        }
        if (blockNumbersStore != null) {
            blockNumbersStore.close();
        }
        if (blockQueue != null) {
            if (blockQueue instanceof BlockQueueImpl) {
                ((BlockQueueImpl)blockQueue).flush();
            }
            blockQueue.close();
        }

        // If is stopped, maybe blocks for start, wakeup asap.
        if (isRequestStopped.get()) {
            notifyStart();
        }
        // If now is waiting for gap recovery, wakeup asap.
        if (noParent.get()) {
            notifyRecovery();
        } else if (blockQueue.size() == 0) {
            // In this condition, thread blocks for taking block wrapper,
            // so interrupt it.
            worker.interrupt();
        }

        isImportingBlocks.set(false);
        inited.set(false);
    }

    private void waitForStart() {
        while (isRequestStopped.get()) {
            synchronized(requestStoppedLock) {
                try {
                    requestStoppedLock.wait();
                } catch (InterruptedException e) {
                    logger.error("Waiting for start is interrupted {}", e);
                    isImportingBlocks.set(false);
                }
            }
        }
    }

    private void notifyStart() {
        synchronized(requestStoppedLock) {
            requestStoppedLock.notify();
        }
    }

    private boolean isStoppedOrClosed() {
        return isRequestStopped.get() || isRequestClose.get();
    }

    /**
     * Processing the queue adding blocks to the chain.
     */
    private void produceQueue() {

        while (!Thread.interrupted() && !isRequestClose.get()){

            waitForStart();

            BlockWrapper wrapper = null;
            try {
                wrapper = blockQueue.take();
                logger.debug("BlockQueue size: {}", blockQueue.size());
                isImportingBlocks.set(true);
                ImportResult importResult = blockchain.tryToConnect(wrapper.getBlock());
                isImportingBlocks.set(false);

                if (wrapper.isNewBlock() && importResult.isSuccessful())
                    syncManager.notifyNewBlockImported(wrapper);

                if (importResult == IMPORTED_BEST)
                    logger.info("Success importing BEST: block.number: {}, block.hash: {}, tx.size: {} ",
                            wrapper.getNumber(), wrapper.getBlock().getShortHash(),
                            wrapper.getBlock().getTransactionsList().size());

                if (importResult == IMPORTED_NOT_BEST)
                    logger.info("Success importing NOT_BEST: block.number: {}, block.hash: {}, tx.size: {} ",
                            wrapper.getNumber(), wrapper.getBlock().getShortHash(),
                            wrapper.getBlock().getTransactionsList().size());

                if (importResult == IMPORTED_BEST || importResult == IMPORTED_NOT_BEST) {
                    if (logger.isDebugEnabled()) logger.debug(Hex.toHexString(wrapper.getBlock().getEncoded()));
                } else {
                    logger.error("Import block failed: result: {}, block.number: {}, block.hash: {}",
                            importResult.name(), wrapper.getNumber(), wrapper.getBlock().getShortHash());
                }

                // In case we don't have a parent on the chain
                // return the try and wait for more blocks to come.
                if (importResult == NO_PARENT) {
                    logger.info("No parent on the chain for block.number: {} block.hash: {}", wrapper.getNumber(), wrapper.getBlock().getShortHash());
                    wrapper.importFailed();

                    // Add this block into block queue, and try to add his parent
                    blockQueue.add(wrapper);
                    if (!tryGapRecoveryAndVerify(wrapper)) {
                        rollbackBlockQueue();
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        logger.error("Sync queue wait recovery interrupted {}", ie);
                    }
                    noParent.set(true);

                    // If 'blockQueue' implementation is BlockQueueMem or BlockQueueImpl, it takes several
                    // seconds to download blocks from network.
                    if (blockQueue instanceof BlockQueueMem || blockQueue instanceof BlockQueueImpl) {
                        waitForRecovery();
                    }
                } else {
                    noParent.set(false);
                }
            } catch (Throwable e) {
                e.printStackTrace();

                // Note: for the application with the version V1.9.0.3, OOM usually happens.
                // For the version V1.9.0.4, there is no OOM. But for special case, taucoin
                // service exists and wallet will start it again.
                if (e instanceof OutOfMemoryError) {
                    logger.error("OOM fatal error: free {}, used {}, max {}, err:{}",
                            Runtime.getRuntime().freeMemory(),
                            Runtime.getRuntime().totalMemory(),
                            Runtime.getRuntime().maxMemory(), e);
                    System.exit(1);
                }

                // Leveldb sometimes throw exceptions which we don't know how to handle.
                // So restart taucoin service again.
                if (e instanceof DBCorruptionException) {
                    Exception internalException = ((DBCorruptionException)e).getException();
                    if (internalException != null
                            && internalException instanceof OverlappingFileLockException) {
                        logger.error("Leveldb fatal error:{}", internalException);
                        System.exit(2);
                    }
                }

                // If request close, break loop asap.
                if (isRequestClose.get() || wrapper == null) {
                    logger.warn("Sync worker quits");
                    break;
                }

                logger.error("Error processing block {}: ", wrapper.getBlock().toString(), e);
                //logger.error("Block dump: {}", Hex.toHexString(wrapper.getBlock().getEncoded()));
                isImportingBlocks.set(false);

                // Return this block into queue, wait for a while and try again.
                if (wrapper != null) {
                    blockQueue.add(wrapper);
                    logger.warn("Try connecting block again with number {}", wrapper.getNumber());
                } else {
                    logger.warn("Peek null and try again");
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    logger.error("Sync queue interrupted {}", ie);
                }
            } finally {
                // Note: this is just the test feature.
                // It shows that android system is under bigger memory pressure with
                // more and more blocks connected. This blockchain service exists periodly.
                // The wallet will start it again.
                if (wrapper != null && wrapper.getNumber() != 0 &&
                        wrapper.getNumber() % HIBERNATION_CYCLE == 0) {
                    logger.warn("Hibernation starts at block {}", wrapper.getNumber());
                    syncManager.notifyHibernation(wrapper.getNumber());
                    syncManager.stopSyncWithPeer();
                    try {
                        Thread.sleep(HIBERNATION_DURATION);
                    } catch (InterruptedException ie) {
                        logger.error("Sync queue hibernation interrupted {}", ie);
                    }
                    System.exit(3);
                }
            }
        }

        isImportingBlocks.set(false);
        if (isRequestClose.get()) {
            logger.warn("Close sync worker");
        }
    }

    private void waitForRecovery() {
        while (noParent.get()) {
            synchronized(noParentLock) {
                try {
                    noParentLock.wait();
                } catch (InterruptedException e) {
                    logger.error("Waiting for recovery is interrupted {}", e);
                    isImportingBlocks.set(false);
                }
            }
        }
    }

    private void notifyRecovery() {
        synchronized(noParentLock) {
            noParentLock.notify();
        }
    }

    private boolean tryGapRecovery(BlockWrapper wrapper) {
        long bestBlockNumber = blockchain.getBestBlock().getNumber();
        long expectedEndNumber = wrapper.getNumber() - 1;

        logger.warn("Try gap recovery from {} end {}", bestBlockNumber + 1, expectedEndNumber);

        if (expectedEndNumber > bestBlockNumber) {
            return ((BlockQueueFileSys)blockQueue).reloadBlock(expectedEndNumber);
        }

        return false;
    }

    /**
     * For BlockQueueFileSys, if some blocks is lost, download them again.
     */
    private boolean tryGapRecoveryAndVerify(BlockWrapper wrapper) {
        long bestBlockNumber = blockchain.getBestBlock().getNumber();
        long expectedEndNumber = wrapper.getNumber() - 1;

        logger.warn("Try gap recovery from {} end {}", bestBlockNumber + 1, expectedEndNumber);

        if (expectedEndNumber > bestBlockNumber) {
            ((BlockQueueFileSys)blockQueue).reloadBlock(expectedEndNumber);
        }

        // Try to peek and verify.
        BlockWrapper expectedBlockWrapper = blockQueue.peek();
        if (expectedBlockWrapper != null
                && expectedBlockWrapper.getNumber() == expectedEndNumber) {
            return true;
        }

        logger.error("file system fatal error: block {} lost, got {}",
                expectedEndNumber, expectedBlockWrapper.getNumber());
        return false;
    }

    private void rollbackBlockQueue() {
        syncManager.stopSyncWithPeer();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            logger.error("Wait for shutdown interrupted {}", ie);
        }

        long bestNumber = blockchain.getBestBlock().getNumber();
        ((BlockQueueFileSys)blockQueue).rollbackTo(bestNumber);
        syncManager.notifyBlockQueueRollback(bestNumber);

        // This block chain service exists and start again.
        logger.error("File block queue roll back to block {}", bestNumber);
        System.exit(4);
    }

    public boolean isImportingBlocksFinished() {
        logger.debug("Block queue size: {}, is connecting: {}",
                blockQueue.size(), isImportingBlocks);
        return isBlocksEmpty() && !isImportingBlocks.get();
    }

    /**
     * Add a list of blocks to the processing queue. <br>
     * Runs BlockHeader validation before adding
     *
     * @param blocks the blocks received from a peer to be added to the queue
     * @param nodeId of the remote peer which these blocks are received from
     */
    public void addAndValidate(List<Block> blocks, byte[] nodeId) {

        // run basic checks
	    /*
        for (Block b : blocks) {
            if (!isValid(b.getHeader())) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid block RLP: {}", Hex.toHexString(b.getEncoded()));
                }

                syncManager.reportBadAction(nodeId);
                return;
            }
        }
        */
        addList(blocks, nodeId);
    }

    /**
     * Adds a list of blocks to the queue
     *
     * @param blocks block list received from remote peer and be added to the queue
     * @param nodeId nodeId of remote peer which these blocks are received from
     */
    public void addList(List<Block> blocks, byte[] nodeId) {

        if (blocks.isEmpty()) {
            return;
        }

        /**
        if (isStoppedOrClosed()) {
            logger.warn("Drop blocks");
            return;
        }
        */

        List<BlockWrapper> wrappers = new ArrayList<>(blocks.size());
        for (Block b : blocks) {
            wrappers.add(new BlockWrapper(b, nodeId));
        }

        blockQueue.addAll(wrappers);

        if (logger.isDebugEnabled()) logger.debug(
                "Blocks waiting to be proceed:  queue.size: [{}] lastBlock.number: [{}]",
                blockQueue.size(),
                blocks.get(blocks.size() - 1).getNumber()
        );

        if (noParent.get() && (blockQueue instanceof BlockQueueMem
                || blockQueue instanceof BlockQueueImpl)) {
            BlockWrapper firstEntry = blockQueue.peek();
            if (firstEntry.getNumber() <= blockchain.getBestBlock().getNumber() + 1) {
                notifyRecovery();
            }
        }
    }

    /**
     * Adds NEW block to the queue
     *
     * @param block new block
     * @param nodeId nodeId of the remote peer which this block is received from
     */
    public void addNew(Block block, byte[] nodeId) {

        // run basic checks
		/*
        if (!isValid(block.getHeader())) {
            syncManager.reportBadAction(nodeId);
            return;
        }
		*/

        BlockWrapper wrapper = new BlockWrapper(block, true, nodeId);
        wrapper.setReceivedAt(System.currentTimeMillis());

        blockQueue.addOrReplace(wrapper);

//        logger.debug("Blocks waiting to be proceed:  queue.size: [{}] lastBlock.number: [{}]",
//                blockQueue.size(),
//                wrapper.getNumber());
    }

    /**
     * Adds hash to the beginning of HashStore queue
     *
     * @param hash hash to be added
     */
    public void addHash(byte[] hash) {
        hashStore.addFirst(hash);
        if (logger.isTraceEnabled()) logger.trace(
                "Adding hash to a hashQueue: [{}], hash queue size: {} ",
                Hex.toHexString(hash).substring(0, 6),
                hashStore.size()
        );
    }

    /**
     * Adds list of hashes to the end of HashStore queue. <br>
     * Sorts out those hashes which blocks are already added to BlockQueue
     *
     * @param hashes hashes
     */
    public void addHashesLast(List<byte[]> hashes) {
        List<byte[]> filtered = blockQueue.filterExisting(hashes);

        hashStore.addBatch(filtered);

        if(logger.isDebugEnabled())
            logger.debug("{} hashes filtered out, {} added", hashes.size() - filtered.size(), filtered.size());
    }

    /**
     * Adds list of hashes to the beginning of HashStore queue. <br>
     * Sorts out those hashes which blocks are already added to BlockQueue
     *
     * @param hashes hashes
     */
    public void addHashes(List<byte[]> hashes) {
        List<byte[]> filtered = blockQueue.filterExisting(hashes);
        hashStore.addFirstBatch(filtered);

        if (logger.isDebugEnabled())
            logger.debug("{} hashes filtered out, {} added", hashes.size() - filtered.size(), filtered.size());
    }

    /**
     * Adds hashes received in NEW_BLOCK_HASHES message. <br>
     * Excludes hashes representing already imported blocks,
     * hashes are added to the end of HashStore queue
     *
     * @param hashes list of hashes
     */
    public void addNewBlockHashes(List<byte[]> hashes) {
        List<byte[]> notInQueue = blockQueue.filterExisting(hashes);

        List<byte[]> notInChain = new ArrayList<>();
        for (byte[] hash : notInQueue) {
            if (!blockchain.isBlockExist(hash)) {
                notInChain.add(hash);
            }
        }

        hashStore.addBatch(notInChain);
    }

    /**
     * Puts back given hashes. <br>
     * Hashes are added to the beginning of queue
     *
     * @param hashes returning hashes
     */
    public void returnHashes(List<ByteArrayWrapper> hashes) {

        if (hashes.isEmpty()) return;

        ListIterator iterator = hashes.listIterator(hashes.size());
        while (iterator.hasPrevious()) {

            byte[] hash = ((ByteArrayWrapper) iterator.previous()).getData();

            if (logger.isDebugEnabled())
                logger.debug("Return hash: [{}]", Hex.toHexString(hash));
            hashStore.addFirst(hash);
        }
    }

    /**
     * Return a list of hashes from blocks that still need to be downloaded.
     *
     * @return A list of hashes for which blocks need to be retrieved.
     */
    public List<byte[]> pollHashes() {
        return hashStore.pollBatch(config.maxBlocksAsk());
    }

    /**
     * Adds list of block numbers to the beginning of BlockNumberStore queue. <br>
     * Sorts out those block numbers which blocks are already added to BlockQueue
     *
     * @param numbers numbers
     */
    public synchronized void addBlockNumbers(List<Long> numbers) {
        if (isStoppedOrClosed()) {
            logger.warn("Drop block numbers");
            return;
        }

        List<Long> filtered = blockQueue.filterExistingNumbers(numbers);
        blockNumbersStore.addBatch(filtered);

        if (logger.isDebugEnabled())
            logger.debug("{} numbers filtered out, {} added", numbers.size() - filtered.size(), filtered.size());
    }

    /**
     * Adds list of block numbers to the beginning of BlockNumberStore queue. <br>
     * Sorts out those numbers which blocks are already added to BlockQueue
     *
     * @param numbers numbers
     */
    public synchronized void addBlockNumbers(long startNumber, long endNumber) {
        if (isStoppedOrClosed()) {
            logger.warn("Drop block numbers, from {} to {}", startNumber, endNumber);
            return;
        }

        List<Long> numbers = new ArrayList<>();
        while (startNumber <= endNumber) {
            numbers.add(startNumber);
            startNumber++;
        }

        List<Long> filtered = blockQueue.filterExistingNumbers(numbers);
        blockNumbersStore.addBatch(filtered);

        if (logger.isDebugEnabled())
            logger.debug("{} numbers filtered out, {} added", numbers.size() - filtered.size(), filtered.size());
    }

    /**
     * Puts back given block numbers. <br>
     * Numbers are added to the beginning of queue
     *
     * @param numbers returning numbers
     */
    public synchronized void returnBlockNumbers(List<Long> numbers) {

        if (numbers.isEmpty()) return;

        List<Long> filtered = blockQueue.filterExistingNumbers(numbers);
        blockNumbersStore.addBatch(filtered);
    }

    /**
     * Return a list of block numbers from blocks that still need to be downloaded.
     *
     * @return A list of block numbers for which blocks need to be retrieved.
     */
    public synchronized List<Long> pollBlockNumbers() {
        return blockNumbersStore.pollBatch(config.maxBlocksAsk());
    }

    public synchronized long getBlockqueueMaxNumber() {
        long maxNumber = blockQueue.getMaxBlockNumber();
        return maxNumber > 0 ? maxNumber : 0;
    }

    /**
     * Adds list of headers received from remote host <br>
     * Runs header validation before addition <br>
     * It also won't add headers of those blocks which are already presented in the queue
     *
     * @param headers list of headers got from remote host
     * @param nodeId remote host nodeId
     */
    public List<BlockHeader> addAndValidateHeaders(List<BlockHeader> headers, byte[] nodeId) {
        List<BlockHeader> newHeaders = new ArrayList<BlockHeader>();
        List<BlockHeader> filtered = blockQueue.filterExistingHeaders(headers);
        /*
        for (BlockHeader header : filtered) {

            if (!isValid(header)) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid header RLP: {}", Hex.toHexString(header.getEncoded()));
                }

                syncManager.reportBadAction(nodeId);
                return newHeaders;
            } else {
                newHeaders.add(header);
            }
        }
        */
        headerStore.addBatch(newHeaders);

        if (logger.isDebugEnabled())
            logger.debug("{} headers filtered out, {} added", headers.size() - filtered.size(), filtered.size());

        return newHeaders;
    }

    /**
     * Adds headers previously taken from the store <br>
     * Doesn't run any validations and checks
     *
     * @param headers list of headers
     */
    public void returnHeaders(List<BlockHeader> headers) {
        headerStore.addBatch(headers);
    }

    /**
     * Returns list of headers for blocks required to be downloaded
     *
     * @return list of headers
     */
    public List<BlockHeader> pollHeaders() {
        return headerStore.pollBatch(config.maxBlocksAsk());
    }

    // a bit ugly but really gives
    // good result
    public void logHashesSize() {
        logger.debug("Hashes list size: [{}]", hashStore.size());
    }

    public void logHeadersSize() {
        logger.debug("Headers list size: [{}]", headerStore.size());
    }

    public boolean isHashesEmpty() {
        return hashStore.isEmpty();
    }

    public boolean isHeadersEmpty() {
        return headerStore.isEmpty();
    }

    public boolean isBlockNumbersEmpty() {
        return blockNumbersStore.isEmpty();
    }

    public boolean isBlocksEmpty() {
        return blockQueue.isEmpty();
    }

    public boolean isMoreBlocksNeeded() {
        logger.debug("blockQueue size/limit {}/{}", blockQueue.size(),
                BLOCK_QUEUE_LIMIT);
        return blockQueue.size() < BLOCK_QUEUE_LIMIT;
    }

    public void clearHashes() {
        if (!hashStore.isEmpty())
            hashStore.clear();
    }

    public void clearHeaders() {
        if (!headerStore.isEmpty())
            headerStore.clear();
    }

    public int hashStoreSize() {
        return hashStore.size();
    }

    public int headerStoreSize() {
        return headerStore.size();
    }

    /**
     * Scans {@link #SCAN_BLOCKS_LIMIT} first blocks in the queue
     * and removes blocks sent by given peer
     *
     * @param nodeId peer's node id
     */
    public void dropBlocks(byte[] nodeId) {
        blockQueue.drop(nodeId, SCAN_BLOCKS_LIMIT);
    }

    /**
     * Checks whether BlockQueue contains solid blocks or not. <br>
     * Block is assumed to be solid in two cases:
     * <ul>
     *     <li>it was downloading during main sync</li>
     *     <li>NEW block with exceeded solid timeout</li>
     * </ul>
     *
     * @see BlockWrapper
     * #SOLID_BLOCK_DURATION_THRESHOLD
     *
     * @return true if queue contains solid blocks, false otherwise
     */
    public boolean hasSolidBlocks() {
        BlockWrapper wrapper = blockQueue.peek();
        return wrapper != null && wrapper.isSolidBlock();
    }

    /**
     * Checks if block exists in the queue
     *
     * @param hash block hash
     *
     * @return true if block exists, false otherwise
     */
    public boolean isBlockExist(byte[] hash) {
        return blockQueue.isBlockExist(hash);
    }

    /**
     * Runs checks against block's header. <br>
     * All these checks make sense before block is added to queue
     * in front of checks running by {@link BlockchainImpl#isValid(BlockHeader)}
     *
     * @param header block header
     * @return true if block is valid, false otherwise
     */
	/*
    private boolean isValid(BlockHeader header) {

        if (!headerValidator.validate(header)) {

            if (logger.isErrorEnabled())
                headerValidator.logErrors(logger);

            return false;
        }

        return true;
    }
    */
    public static void fillupHeadersNumber(List<BlockHeader> headers,
            long startNumber, long lastNumber) {
        if (headers == null || headers.size() == 0) {
            logger.error("Can't fillup headers number due to empty block headers");
            return;
        }
        if (startNumber < 0 || lastNumber < 0) {
            logger.error("Can't fillup headers number due to number {} {}", startNumber, lastNumber);
            return;
        }

        logger.info("fillup number start {} {} last {} {}", startNumber, Hex.toHexString(headers.get(0).getHash()),
                lastNumber, Hex.toHexString(headers.get(headers.size() - 1).getHash()));

        long delta;
        long start = startNumber;
        if (startNumber <= lastNumber) {
            delta = 1;
        } else {
            delta = -1;
        }

        for (BlockHeader header : headers) {
            header.setNumber(start);
            start += delta;
        }

        assert headers.get(headers.size() - 1).getNumber() == lastNumber;
    }
}
