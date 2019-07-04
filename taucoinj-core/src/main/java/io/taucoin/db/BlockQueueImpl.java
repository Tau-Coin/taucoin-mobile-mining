package io.taucoin.db;

import io.taucoin.core.Blockchain;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockWrapper;
import io.taucoin.datasource.mapdb.MapDBFactory;
import io.taucoin.datasource.mapdb.Serializers;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Mikhail Kalinin
 * @since 09.07.2015
 * @author taucoin core
 * @since 01.07.2019
 */
public class BlockQueueImpl implements BlockQueue {

    private final static Logger logger = LoggerFactory.getLogger("blockqueue");

    private static final int READ_HITS_COMMIT_THRESHOLD = 100;
    private int readHits;

    private final static String STORE_NAME = "blockqueue";
    private final static String HASH_SET_NAME = "hashset";
    private MapDBFactory mapDBFactory;

    private DB db;
    private Map<Long, BlockWrapper> blocks;
    private Set<ByteArrayWrapper> hashes;
    private Index index;

    private boolean initDone = false;
    private final ReentrantLock initLock = new ReentrantLock();
    private final Condition init = initLock.newCondition();

    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    private final Object writeMutex = new Object();
    private final Object readMutex = new Object();

    private Blockchain blockchain;

    @Override
    public void open() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initLock.lock();
                    db = mapDBFactory.createTransactionalDB(dbName());
                    blocks = db.hashMapCreate(STORE_NAME)
                            .keySerializer(Serializer.LONG)
                            .valueSerializer(Serializers.BLOCK_WRAPPER)
                            .makeOrGet();
                    hashes = db.hashSetCreate(HASH_SET_NAME)
                            .serializer(Serializers.BYTE_ARRAY_WRAPPER)
                            .makeOrGet();

                    if(CONFIG.databaseReset()) {
                        blocks.clear();
                        hashes.clear();
                        db.commit();
                    }

                    index = new ArrayListIndex(blocks.keySet());

                    initDone = true;
                    readHits = 0;

                    removeUnusedBlocks();
                    init.signalAll();

                    logger.info("Block queue loaded, size [{}]", size());
                } catch (Exception e) {
                    logger.error("Block queue open error:{}", e);

                    tryToRecoverDB();
                } finally {
                    initLock.unlock();
                }
            }
        }).start();
    }

    private String dbName() {
        return String.format("%s/%s", STORE_NAME, STORE_NAME);
    }

    private void removeUnusedBlocks() {
        synchronized (readMutex) {
            if (index.isEmpty()) {
                logger.info("Empty index and no need to remove unused blocks");
                return;
            }

            long bestNumber = blockchain.getBestBlock().getNumber();
            long removedStart = 0;
            BlockWrapper wrapper = poll();
            removedStart = wrapper.getNumber();
            if (removedStart > bestNumber) {
                logger.info("No need to remove unused blocks start {} best {}",
                        removedStart, bestNumber);
                add(wrapper);
                return;
            }

            logger.info("Remove unused blocks from {} to {}", removedStart, bestNumber);

            while ((removedStart < bestNumber) && !index.isEmpty()) {
                wrapper = poll();
                removedStart = wrapper.getNumber();
            }

            db.commit();
        }
    }

    private void tryToRecoverDB() {
        logger.warn("Try to recover block queue database");

        if (db != null) {
            db.close();
        }

        // Remove db file and create it again.
        File dbFile = new File(CONFIG.databaseDir() + "/" + STORE_NAME);
        if (dbFile.exists() && dbFile.isDirectory()) {
            for (File f : dbFile.listFiles()) {
                f.delete();
            }

            dbFile.delete();
        }

        db = mapDBFactory.createTransactionalDB(dbName());
        blocks = db.hashMapCreate(STORE_NAME)
                .keySerializer(Serializer.LONG)
                .valueSerializer(Serializers.BLOCK_WRAPPER)
                .makeOrGet();
        hashes = db.hashSetCreate(HASH_SET_NAME)
                .serializer(Serializers.BYTE_ARRAY_WRAPPER)
                .makeOrGet();

        index = new ArrayListIndex(blocks.keySet());

        initDone = true;
        readHits = 0;
        init.signalAll();
    }

    public void flush() {
        awaitInit();
        db.commit();
    }

    @Override
    public void close() {
        awaitInit();
        db.close();
        initDone = false;
    }

    @Override
    public void addAll(Collection<BlockWrapper> blockList) {
        awaitInit();
        synchronized (writeMutex) {
            List<Long> numbers = new ArrayList<>(blockList.size());
            Set<ByteArrayWrapper> newHashes = new HashSet<>();
            for (BlockWrapper b : blockList) {
                if(!index.contains(b.getNumber()) &&
                   !numbers.contains(b.getNumber())) {

                    blocks.put(b.getNumber(), b);
                    numbers.add(b.getNumber());
                    newHashes.add(new ByteArrayWrapper(b.getHash()));
                }
            }
            hashes.addAll(newHashes);

            takeLock.lock();
            try {
                index.addAll(numbers);
                notEmpty.signalAll();
            } finally {
                takeLock.unlock();
            }
        }
        db.commit();
    }

    @Override
    public void add(BlockWrapper block) {
        awaitInit();
        synchronized (writeMutex) {
            if (index.contains(block.getNumber())) {
                return;
            }
            blocks.put(block.getNumber(), block);
            hashes.add(new ByteArrayWrapper(block.getHash()));

            takeLock.lock();
            try {
                index.add(block.getNumber());
                notEmpty.signalAll();
            } finally {
                takeLock.unlock();
            }
        }
        db.commit();
    }

    @Override
    public void addOrReplace(BlockWrapper block) {
        awaitInit();
        synchronized (writeMutex) {

            if (!index.contains(block.getNumber())) {
                addInner(block);
            } else {
                replaceInner(block);
            }
        }
        db.commit();
    }

    private void replaceInner(BlockWrapper block) {

        BlockWrapper old = blocks.get(block.getNumber());

        if (block.equals(old)) return;

        if (old != null) {
            hashes.remove(new ByteArrayWrapper(old.getHash()));
        }

        blocks.put(block.getNumber(), block);
        hashes.add(new ByteArrayWrapper(block.getHash()));
    }

    private void addInner(BlockWrapper block) {
        blocks.put(block.getNumber(), block);
        hashes.add(new ByteArrayWrapper(block.getHash()));

        takeLock.lock();
        try {
            index.add(block.getNumber());
            notEmpty.signalAll();
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public BlockWrapper poll() {
        awaitInit();
        BlockWrapper block = pollInner();
        commitReading();
        return block;
    }

    private BlockWrapper pollInner() {
        synchronized (readMutex) {
            if (index.isEmpty()) {
                return null;
            }

            Long idx = index.poll();
            BlockWrapper block = blocks.get(idx);
            if (block.getNumber() == 0) {
                block.getBlock().setNumber(idx);
            }
            blocks.remove(idx);

            if (block != null) {
                hashes.remove(new ByteArrayWrapper(block.getHash()));
            } else {
                logger.error("Block for index {} is null", idx);
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
            BlockWrapper wrapper = blocks.get(idx);
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
            commitReading();
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

        blocks.clear();
        hashes.clear();
        index.clear();

        db.commit();
    }

    @Override
    public List<byte[]> filterExisting(final Collection<byte[]> hashList) {
        awaitInit();

        List<byte[]> filtered = new ArrayList<>();
        for (byte[] hash : hashList) {
            if (!hashes.contains(new ByteArrayWrapper(hash))) {
                filtered.add(hash);
            }
        }

        return filtered;
    }

    @Override
    public List<BlockHeader> filterExistingHeaders(Collection<BlockHeader> headers) {
        awaitInit();

        List<BlockHeader> filtered = new ArrayList<>();
        for (BlockHeader header : headers) {
            if (!hashes.contains(new ByteArrayWrapper(header.getHeaderHash()))) {
                filtered.add(header);
            }
        }

        return filtered;
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
        return hashes.contains(new ByteArrayWrapper(hash));
    }

    @Override
    public void drop(byte[] nodeId, int scanLimit){

    }

    @Override
    public Long getMaxBlockNumber() {
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

    private void commitReading() {
        if(++readHits >= READ_HITS_COMMIT_THRESHOLD) {
            db.commit();
            readHits = 0;
        }
    }

    public void setMapDBFactory(MapDBFactory mapDBFactory) {
        this.mapDBFactory = mapDBFactory;
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public interface Index {

        void addAll(Collection<Long> nums);

        void add(Long num);

        Long peek();

        Long poll();

        boolean contains(Long num);

        boolean isEmpty();

        int size();

        void clear();

        Long getMax();
    }

    public static class ArrayListIndex implements Index {

        private List<Long> index;

        public ArrayListIndex(Collection<Long> numbers) {
            index = new ArrayList<>(numbers);
            sort();
        }

        @Override
        public synchronized void addAll(Collection<Long> nums) {
            index.addAll(nums);
            sort();
        }

        @Override
        public synchronized void add(Long num) {
            index.add(num);
            sort();
        }

        @Override
        public synchronized Long peek() {
            return index.get(0);
        }

        @Override
        public synchronized Long poll() {
            Long num = index.get(0);
            index.remove(0);
            return num;
        }

        @Override
        public synchronized boolean contains(Long num) {
            return Collections.binarySearch(index, num) >= 0;
        }

        @Override
        public boolean isEmpty() {
            return index.isEmpty();
        }

        @Override
        public int size() {
            return index.size();
        }

        @Override
        public synchronized void clear() {
            index.clear();
        }

        private void sort() {
            Collections.sort(index);
        }

        @Override
        public synchronized Long getMax() {
            if (isEmpty()) {
                return Long.MIN_VALUE;
            }

            return index.get(size() - 1);
        }
    }
}
