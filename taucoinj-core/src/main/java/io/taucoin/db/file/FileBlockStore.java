package io.taucoin.db.file;

import io.taucoin.core.BlockWrapper;
import io.taucoin.db.file.LargeFileStoreGroup.OpFilePosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;

import static io.taucoin.config.SystemProperties.CONFIG;

public class FileBlockStore {

    private static final Logger logger = LoggerFactory.getLogger("fileblockqueue");

    private static final String BACKEND_DIRECTORY = "store-backend";
    private static final String BLOCKS_STORE_DIRECTORY = BACKEND_DIRECTORY + "/" + "blocks";
    private static final String BLOCKS_INDEX_DIRECTORY = BACKEND_DIRECTORY + "/" + "index";

    private static final String BLOCKS_STORE_PREFIX = "blk";
    private static final String BLOCKS_STORE_SUFFIX = "dat";

    private static final String INDEX_STORE_PREFIX = "idx";
    private static final String INDEX_STORE_SUFFIX = "dat";

    private LargeFileStoreGroup blockStore;
    private LargeFileStoreGroup indexStore;

    private long maxNumber;

    public FileBlockStore() {
    }

    public synchronized void open() {

        initDirectory(BACKEND_DIRECTORY);
        initDirectory(BLOCKS_STORE_DIRECTORY);
        initDirectory(BLOCKS_INDEX_DIRECTORY);

        // Create file block store and index store.
        int blocksMaxFile = detectFileAmount(BLOCKS_STORE_DIRECTORY, BLOCKS_STORE_PREFIX);
        blockStore = new LargeFileStoreGroup(
                CONFIG.databaseDir() + "/" + BLOCKS_STORE_DIRECTORY,
                BLOCKS_STORE_PREFIX, BLOCKS_STORE_SUFFIX,
                blocksMaxFile, CONFIG.blockStoreFileMaxSize());

        int indexMaxFile = detectFileAmount(BLOCKS_INDEX_DIRECTORY, INDEX_STORE_PREFIX);
        indexStore = new LargeFileStoreGroup(
                CONFIG.databaseDir() + "/" + BLOCKS_INDEX_DIRECTORY,
                INDEX_STORE_PREFIX, INDEX_STORE_SUFFIX,
                indexMaxFile,
                CONFIG.indexStoreFileMetaMaxAmount() * BlockIndex.ENCODED_SIZE);

        // Get max block number.
        long lastIndexFileSize = 0;
        try {
            lastIndexFileSize = indexStore.getWriteFileSize();
        } catch(Exception e) {
            throw new RuntimeException("FileSys block store get write file size error:"
                    + e.getMessage());
        }

        maxNumber = (long)indexMaxFile * (long)CONFIG.indexStoreFileMetaMaxAmount()
                + lastIndexFileSize / (long)BlockIndex.ENCODED_SIZE;

        logger.info("max block file {}, max index file {}", blocksMaxFile, indexMaxFile);
        logger.info("File block queue blocks amount {}", maxNumber);

        checkSanity();
    }

    public synchronized void put(long number, BlockWrapper block) {
        if (number <= maxNumber) {
            logger.error("Block with the number {} has existed.", number);
            return;
        }

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

        maxNumber = number;
        logger.info("save block with number {} hash {} cost {}ns, postion {}",
                number, Hex.toHexString(block.getHash()),
                System.nanoTime() - startTime, position);
    }

    public synchronized BlockWrapper get(long number) {
        if (number > maxNumber || number <= 0) {
            logger.error("Block with the number {}/{} hasn't existed.",
                    number, maxNumber);
            return null;
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

        logger.info("read block with number {} hash {} cost {}ns, postion {}",
                number, Hex.toHexString(block.getHash()),
                System.nanoTime() - startTime, position);
        return block;
    }

    public synchronized long getMaxBlockNumber() {
        return maxNumber;
    }

    public synchronized void close() {
        blockStore.close();
        indexStore.close();
    }

    private void initDirectory(String dir) {
        String absolutePath = CONFIG.databaseDir() + "/" + dir;
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
        File f = new File(CONFIG.databaseDir() + "/" + dir);
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
        if (maxNumber <= 0) {
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
            throw new RuntimeException("Blcokqueue filesys is corrupted");
        }
    }
}
