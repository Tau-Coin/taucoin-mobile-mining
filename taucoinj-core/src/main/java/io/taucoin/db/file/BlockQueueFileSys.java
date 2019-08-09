package io.taucoin.db.file;

import io.taucoin.core.Blockchain;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockWrapper;
import io.taucoin.db.BlockQueue;
import io.taucoin.db.BlockQueueImpl.Index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.db.BlockQueueImpl.ArrayListIndex;

/**
 * @author Taucoin Core Developers
 * @since 07.07.2019
 */
public class BlockQueueFileSys implements BlockQueue {

    private final static Logger logger = LoggerFactory.getLogger("fileblockqueue");

    private Index index;

    private boolean initDone = false;
    private final ReentrantLock initLock = new ReentrantLock();
    private final Condition init = initLock.newCondition();

    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    private final Object writeMutex = new Object();
    private final Object readMutex = new Object();

    private Blockchain blockchain;

    private FileBlockStore fileBlockStore;

    public BlockQueueFileSys(FileBlockStore fileBlockStore) {
        this.fileBlockStore = fileBlockStore;
    }

    @Override
    public void open() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initLock.lock();

                    //fileBlockStore.open();

                    // Collect unimported block number.
                    Set<Long> unimportedNumbers = new HashSet<Long>();
                    long startNumber = blockchain.getBestBlock().getNumber() + 1;
                    long maxNumber = fileBlockStore.getMaxBlockNumber();
                    logger.info("File block queue init from {} to {}",
                            startNumber, maxNumber);

                    while(startNumber <= maxNumber) {
                        unimportedNumbers.add(startNumber);
                        startNumber++;
                    }
                    index = new ArrayListIndex(unimportedNumbers);

                    initDone = true;
                    init.signalAll();

                    logger.info("File block queue loaded, size [{}]", size());
                } catch (Exception e) {
                    logger.error("File block queue open error:{}", e);
                } finally {
                    initLock.unlock();
                }
            }
        }).start();
    }

    public void flush() {
    }

    @Override
    public void close() {
        awaitInit();
        fileBlockStore.close();
        initDone = false;
    }

    @Override
    public void addAll(Collection<BlockWrapper> blockList) {
        awaitInit();
        synchronized (writeMutex) {
            Collections.sort((List<BlockWrapper>)blockList);
            List<Long> numbers = new ArrayList<>(blockList.size());
            for (BlockWrapper b : blockList) {
                if(!index.contains(b.getNumber()) &&
                   !numbers.contains(b.getNumber())) {

                    if (fileBlockStore.put(b.getNumber(), b)) {
                        numbers.add(b.getNumber());
                    } else {
                        numbers.add(b.getNumber());
                    }
                }
            }

            takeLock.lock();
            try {
                index.addAll(numbers);
                notEmpty.signalAll();
            } finally {
                takeLock.unlock();
            }

            if (!numbers.isEmpty()) {
                logger.info("Add block from {} to {}", numbers.get(0),
                        numbers.get(numbers.size() - 1));
            }
        }
    }

    @Override
    public void add(BlockWrapper block) {
        awaitInit();
        synchronized (writeMutex) {
            if (index.contains(block.getNumber())) {
                return;
            }
            if (fileBlockStore.put(block.getNumber(), block)) {

                takeLock.lock();
                try {
                    index.add(block.getNumber());
                    notEmpty.signalAll();
                } finally {
                    takeLock.unlock();
                }
            } else {
                takeLock.lock();
                try {
                    index.add(block.getNumber());
                    notEmpty.signalAll();
                } finally {
                    takeLock.unlock();
                }
            }
        }
    }

    @Override
    public void addOrReplace(BlockWrapper block) {
    }

    public boolean reloadBlock(long number) {
        awaitInit();
        synchronized (writeMutex) {
            if (index.contains(number)) {
                logger.warn("Reload block has existed {}", number);
                return false;
            }

            takeLock.lock();
            try {
                index.add(number);
                logger.info("Reload block {}", number);
                notEmpty.signalAll();
            } finally {
                takeLock.unlock();
            }

            return true;
        }
    }

    @Override
    public BlockWrapper poll() {
        awaitInit();
        BlockWrapper block = pollInner();
        return block;
    }

    private BlockWrapper pollInner() {
        synchronized (readMutex) {
            if (index.isEmpty()) {
                return null;
            }

            Long idx = index.poll();
            BlockWrapper block = fileBlockStore.get(idx);
            if (block.getNumber() == 0) {
                block.getBlock().setNumber(idx);
            }

            return block;
        }
    }

    @Override
    public BlockWrapper peek() {
        awaitInit();
        synchronized (readMutex) {
            if(index.isEmpty()) {
                return null;
            }

            Long idx = index.peek();
            BlockWrapper wrapper = fileBlockStore.get(idx);
            if (wrapper.getNumber() == 0) {
                wrapper.getBlock().setNumber(idx);
            }

            return wrapper;
        }
    }

    @Override
    public BlockWrapper take() {
        awaitInit();
        takeLock.lock();
        try {
            BlockWrapper block;
            while (null == (block = pollInner())) {
                notEmpty.awaitUninterruptibly();
            }
            return block;
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public int size() {
        awaitInit();
        return index.size();
    }

    @Override
    public boolean isEmpty() {
        awaitInit();
        return index.isEmpty();
    }

    @Override
    public void clear() {
        awaitInit();

        index.clear();
    }

    @Override
    public List<byte[]> filterExisting(final Collection<byte[]> hashList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<BlockHeader> filterExistingHeaders(Collection<BlockHeader> headers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> filterExistingNumbers(Collection<Long> numbers) {
        awaitInit();

        List<Long> filtered = new ArrayList<>();
        for (Long n : numbers) {
            if (!index.contains(n)) {
                filtered.add(n);
            }
        }

        return filtered;
    }

    @Override
    public boolean isBlockExist(byte[] hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drop(byte[] nodeId, int scanLimit){
    }

    @Override
    public Long getMaxBlockNumber() {
        awaitInit();

        return index.getMax();
    }

    private void awaitInit() {
        initLock.lock();
        try {
            if(!initDone) {
                init.awaitUninterruptibly();
            }
        } finally {
            initLock.unlock();
        }
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public void rollbackTo(long number) {
        logger.warn("Roll back to block {}", number);

        fileBlockStore.rollbackTo(number);

        //Rebuild everything
        initDone = false;
        open();
    }
}
