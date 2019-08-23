package io.taucoin.db.file;

import io.taucoin.core.BlockWrapper;
import io.taucoin.core.Utils;
import io.taucoin.db.file.LargeFileStoreGroup.OpFilePosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class FileBlockStore {

    private static final Logger logger = LoggerFactory.getLogger("fileblockqueue");

    private static final String BACKEND_DIRECTORY = "store-backend";
    private static final String BLOCKS_STORE_DIRECTORY = BACKEND_DIRECTORY + File.separator + "blocks";
    private static final String BLOCKS_INDEX_DIRECTORY = BACKEND_DIRECTORY + File.separator + "index";

    private static final String START_NUMBER_FILE = BACKEND_DIRECTORY + File.separator + "startno";

    private static final String BLOCKS_STORE_PREFIX = "blk";
    private static final String BLOCKS_STORE_SUFFIX = "dat";

    private static final String INDEX_STORE_PREFIX = "idx";
    private static final String INDEX_STORE_SUFFIX = "dat";

    private LargeFileStoreGroup blockStore;
    private LargeFileStoreGroup indexStore;

    private long maxNumber;

    private long startNumber;

    private LRUCache blocksCache
            = new LRUCache(10, 0.75f);

    // Blocks must be stored by continuous number ascending order.
    private TreeMap<Long, BlockWrapper> discontinuousBlocks
            = new TreeMap<Long, BlockWrapper>();

    @Inject
    public FileBlockStore() {
        open();
    }

    public synchronized void open() {

        initDirectory(BACKEND_DIRECTORY);
        initDirectory(BLOCKS_STORE_DIRECTORY);
        initDirectory(BLOCKS_INDEX_DIRECTORY);

        // Create file block store and index store.
        int blocksMaxFile = detectFileAmount(BLOCKS_STORE_DIRECTORY, BLOCKS_STORE_PREFIX);
        blockStore = new LargeFileStoreGroup(
                CONFIG.databaseDir() + File.separator + BLOCKS_STORE_DIRECTORY,
                BLOCKS_STORE_PREFIX, BLOCKS_STORE_SUFFIX,
                blocksMaxFile, CONFIG.blockStoreFileMaxSize());

        int indexMaxFile = detectFileAmount(BLOCKS_INDEX_DIRECTORY, INDEX_STORE_PREFIX);
        indexStore = new LargeFileStoreGroup(
                CONFIG.databaseDir() + File.separator + BLOCKS_INDEX_DIRECTORY,
                INDEX_STORE_PREFIX, INDEX_STORE_SUFFIX,
                indexMaxFile,
                CONFIG.indexStoreFileMetaMaxAmount() * BlockIndex.ENCODED_SIZE);

        // Read start number and set start number for 'BlockIndex'.
        startNumber = readStartNumber();
        BlockIndex.setStartNumber(startNumber);

        // Get max block number.
        long lastIndexFileSize = 0;
        try {
            lastIndexFileSize = indexStore.getWriteFileSize();
        } catch(Exception e) {
            throw new RuntimeException("FileSys block store get write file size error:"
                    + e.getMessage());
        }

        maxNumber = (long)indexMaxFile * (long)CONFIG.indexStoreFileMetaMaxAmount()
                + lastIndexFileSize / (long)BlockIndex.ENCODED_SIZE
                + (startNumber - 1);

        logger.info("max block file {}, max index file {}", blocksMaxFile, indexMaxFile);
        logger.info("File block queue blocks amount {}, start {}", maxNumber, startNumber);

        checkSanity();
    }

    public synchronized boolean put(long number, BlockWrapper block) {
        if (number <= maxNumber) {
            logger.error("Block with the number {} has existed.", number);
            return false;
        }

        if (number != maxNumber + 1) {
            discontinuousBlocks.put(number, block);
            logger.warn("discontinuous block with number {}, hash {}",
                    number, Hex.toHexString(block.getHash()));
            return true;
        }

        if (putContinuousBlock(number, block)) {
            checkContinuousBlocks(maxNumber + 1);
            return true;
        }

        return false;
    }

    private void checkContinuousBlocks(long startNumber) {
        synchronized(discontinuousBlocks) {
            if (discontinuousBlocks.size() == 0) {
                return;
            }

            long firstNumber = discontinuousBlocks.firstKey();
            long lastNumber = discontinuousBlocks.lastKey();
            if (startNumber != firstNumber) {
                logger.info("discontinuous blocks first {} last {}",
                        firstNumber, lastNumber);
                return;
            }

            long currentNumber = firstNumber;
            while (currentNumber <= lastNumber) {
                putContinuousBlock(currentNumber, discontinuousBlocks.get(currentNumber));
                discontinuousBlocks.remove(currentNumber);

                if (discontinuousBlocks.get(currentNumber + 1) == null) {
                    break;
                }

                currentNumber++;
            }
        }
    }

    private boolean putContinuousBlock(long number, BlockWrapper block) {
        long startTime = System.nanoTime();

        byte[] encoded = block.getBytes();
        OpFilePosition position;

        try {
            position = blockStore.write(encoded);
        } catch(Exception e) {
            logger.error("Write block error: {}", e);
            throw new RuntimeException("FileSys block store write error:"
                    + e.getMessage());
        }

        BlockIndex index = new BlockIndex(position);
        try {
            indexStore.write(index.getEncoded());
        } catch(Exception e) {
            try {
                blockStore.rollback(position);
            } catch(Exception e2) {
                logger.error("Block store rollback error: {}", e2);
                throw new RuntimeException("FileSys block store rollback error:"
                        + e2.getMessage());
            }
            logger.error("Write block index error: {}", e);
            throw new RuntimeException("FileSys block store read error:"
                    + e.getMessage());
        }

        synchronized(blocksCache) {
            blocksCache.put(number, block);
        }
        maxNumber = number;
        logger.debug("save block with number {} hash {} cost {}ns, postion {}",
                number, Hex.toHexString(block.getHash()),
                System.nanoTime() - startTime, position);

        return true;
    }

    public synchronized BlockWrapper get(long number) {
        if (number <= 0) {
            logger.error("Block with the number {}/{} hasn't existed.",
                    number, maxNumber);
            return null;
        }

        if (number > maxNumber) {
            synchronized(discontinuousBlocks) {
                return discontinuousBlocks.get(number);
            }
        }

        synchronized(blocksCache) {
            BlockWrapper blockCache = blocksCache.get(number);
            if (blockCache != null) {
                return blockCache;
            }
        }

        long startTime = System.nanoTime();

        OpFilePosition position = BlockIndex.withBlockNumber(number)
                .getOpFilePosition();
        BlockIndex index;
        byte[] indexEncoded;

        try {
            indexEncoded = indexStore.read(position);
            index = new BlockIndex(indexEncoded);
        } catch(Exception e) {
            logger.error("Read block index error: {}", e);
            throw new RuntimeException("FileSys block store read error:"
                    + e.getMessage());
        }

        BlockWrapper block;
        byte[] blockEncoded;

        try {
            blockEncoded = blockStore.read(index.getOpFilePosition());
            block = new BlockWrapper(blockEncoded);
        } catch(Exception e) {
            logger.error("Read block error: {}", e);
            throw new RuntimeException("FileSys block store read error:"
                    + e.getMessage());
        }

        synchronized(blocksCache) {
            blocksCache.put(number, block);
        }

        logger.debug("read block with number {} hash {} cost {}ns, postion {}",
                number, Hex.toHexString(block.getHash()),
                System.nanoTime() - startTime, position);
        return block;
    }

    public synchronized long getMaxBlockNumber() {
        return maxNumber;
    }

    public synchronized boolean rollbackTo(long number) {
        if (number < 0) {
            logger.error("Invalid block number {}", number);
            return false;
        }

        long startTime = System.nanoTime();
        logger.warn("Rollback blockstore to number {}, now max {}", number, maxNumber);

        OpFilePosition position = BlockIndex.withBlockNumber(number)
                .getOpFilePosition();
        BlockIndex index;
        byte[] indexEncoded;

        try {
            indexEncoded = indexStore.read(position);
            index = new BlockIndex(indexEncoded);

            // Rollback block index
            indexStore.rollbackTo(position);

            // Rollback blocks.
            blockStore.rollbackTo(index.getOpFilePosition());
        } catch(Exception e) {
            logger.error("Rollback block error: {}", e);
            return false;
        }

        logger.warn("rollback to block with number {}  cost {}ns, postion {}",
                number, System.nanoTime() - startTime, position);

        // Reopen
        indexStore = null;
        blockStore = null;
        open();

        return true;
    }

    public synchronized void setStartNumber(long startNumber) {
        this.startNumber = startNumber;
        maxNumber = startNumber - 1;
        writeStartNumber(startNumber);
    }

    public synchronized void close() {
        blockStore.close();
        indexStore.close();
    }

    private void initDirectory(String dir) {
        String absolutePath = CONFIG.databaseDir() + File.separator + dir;
        File f = new File(absolutePath);

        if (f.exists()) {
            if (!f.isDirectory()) {
                throw new RuntimeException(dir + " isn't a directory");
            }
        } else {
            f.mkdir();
        }
    }

    private static int detectFileAmount(String dir, String prefix) {
        File f = new File(CONFIG.databaseDir() + File.separator + dir);
        int amount = 0;

        if (!f.exists() || !f.isDirectory()) {
            throw new RuntimeException("Invalid directory:" + dir);
        }

        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isFile()) {
                continue;
            }

            String name = files[i].getName();
            logger.debug("file name: {}", name);
            // blkxxxxx.dat or idxxxxxx.dat
            if (name.length() == 12 && (name.startsWith(prefix))) {
                String number = name.substring(3, 8);
                int intNumber = Integer.parseInt(number);

                if (intNumber > amount) {
                    amount = intNumber;
                }
            }
        }

        return amount;
    }

    private void checkSanity() {
        if (maxNumber < startNumber) {
            return;
        }

        OpFilePosition position = BlockIndex.withBlockNumber(maxNumber)
                .getOpFilePosition();

        // Read block index
        byte[] indexEncoded = null;
        try {
            indexEncoded = indexStore.read(position);
        } catch(Exception e) {
            logger.error("check sanity error:{}", e);
            throw new RuntimeException("Blcokqueue filesys is corrupted");
        }
        BlockIndex index = new BlockIndex(indexEncoded);

        byte[] blockEncoded = null;
        try {
            blockEncoded = blockStore.read(index.getOpFilePosition());
        } catch(Exception e) {
            logger.error("check sanity error:{}", e);
            throw new RuntimeException("Blcokqueue filesys is corrupted");
        }

        BlockWrapper block = new BlockWrapper(blockEncoded);
        long number = block.getNumber();
        if (number != maxNumber) {
            String errorMessage = String.format(
                    "Block index corrupted, max number %d vs block number %d",
                    maxNumber, number);
            logger.error(errorMessage);
            //throw new RuntimeException(errorMessage);
        }
    }

    private long readStartNumber() {
        if (!checkFile(START_NUMBER_FILE)) {
            return 1L;
        }

        InputStream in = null;
        byte[] startNumberBytes = new byte[8];

        try {
            in = new FileInputStream(CONFIG.databaseDir() + File.separator
                + START_NUMBER_FILE);
            in.read(startNumberBytes, 0, 8);
            in.close();
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("read start number fatal err:{}", e);
            return 0;
        } catch (IOException e) {
            logger.error("read start number fatal err:{}", e);
            return 0;
        }

        long startNo = Utils.readInt64(startNumberBytes, 0);
        return startNo;
    }

    private void writeStartNumber(long startNumber) {
        checkFile(START_NUMBER_FILE);

        OutputStream out = null;
        byte[] startNumberBytes = new byte[8];

        Utils.uint64ToByteArrayLE(startNumber, startNumberBytes, 0);
        try {
            out = new FileOutputStream(CONFIG.databaseDir() + File.separator
                + START_NUMBER_FILE);
            out.write(startNumberBytes, 0 ,8);
            out.close();
        } catch (FileNotFoundException e) {
            // This should never happen.
            logger.error("write start number fatal err:{}", e);
        } catch (IOException e) {
            logger.error("write start number fatal err:{}", e);
        }
    }

    // If not exist, create file.
    // Return false if not exist. Else return true;
    private static boolean checkFile(String file) {
        String absoluteFile = CONFIG.databaseDir() + "/" + file;
        File f = new File(absoluteFile);

        try {
            if (!f.exists()) {
               f.createNewFile();
               return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static class LRUCache extends LinkedHashMap<Long, BlockWrapper> {
        private static final long serialVersionUID = 1L;
        private int capacity;

        public LRUCache(int capacity, float loadFactor) {
            super(capacity, loadFactor, false);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, BlockWrapper> eldest) {
            return size() > this.capacity;
        }
    }
}
