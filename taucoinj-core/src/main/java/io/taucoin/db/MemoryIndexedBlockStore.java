package io.taucoin.db;

import io.taucoin.core.Block;
import io.taucoin.util.ByteUtil;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.math.BigInteger.ZERO;
import static io.taucoin.crypto.HashUtil.shortHash;
import static io.taucoin.config.SystemProperties.CONFIG;
import static org.spongycastle.util.Arrays.areEqual;

/**
 * This implementation stores the latest CONFIG.blockStoreCapability() blocks in memory.
 * The block data and block info data are stored in file system.
 *
 * Block file name format:
 *     <number>_<shorthash>, etc, 1000_aabbcc
 * Its content is block's rlp encoded bytes.
 *
 * Block info file name format:
 *     <number>, etc, 1000
 * Its content is ArrayList<BlockInfo> java serialized bytes.
 */

@Singleton
public class MemoryIndexedBlockStore implements BlockStore {

    private static final Logger logger = LoggerFactory.getLogger("memblockstore");

    private static final String BLOCKSTORE_DIRECTORY = "blockstore";

    // Memory blocks.
    private Map<ByteArrayWrapper, Block> blocks = new HashMap<ByteArrayWrapper, Block>();

    // Memory blocks index.
    private TreeMap<Long, ArrayList<BlockInfo>> index
            = new TreeMap<Long, ArrayList<BlockInfo>>();

    // Blocks and blockinfo cache.
    private Map<ByteArrayWrapper, Block> blocksCache
            = new HashMap<ByteArrayWrapper, Block>();
    private Map<Long, ArrayList<BlockInfo>> indexCache
            = new HashMap<Long, ArrayList<BlockInfo>>();

    // Block time cache: height -> block time.
    private static final int BLOCKTIME_CACHE_CAPACITY = CONFIG.blockStoreCapability();
    private static LRUCache sBlockTimeCache
            = new LRUCache(BLOCKTIME_CACHE_CAPACITY, 0.75f);
    private static Object sBlockTimeLock = new Object();

    // Read and write lock.
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();

    @Inject
    public MemoryIndexedBlockStore() {
        init();
    }

    private void init() {
        initDirectory(BLOCKSTORE_DIRECTORY);
        load();
        printChain();
    }

    @Override
    public void close(){
        logger.info("close block store data base...");
    }

    public Block getBestBlock(){

        Long maxLevel = getMaxNumber();
        logger.info("getBestBlock maxLevel is {}", maxLevel);
        if (maxLevel < 0) return null;

        Block bestBlock = getChainBlockByNumber(maxLevel);
        if (bestBlock != null) return  bestBlock;

        // That can scenario can happen
        // if there is fork branch that is
        // higher than main branch but has
        // less TD than the main branch TD
        while (bestBlock == null){
            --maxLevel;
            bestBlock = getChainBlockByNumber(maxLevel);
        }

        return bestBlock;
    }

    public byte[] getBlockHashByNumber(long blockNumber){
        return getChainBlockByNumber(blockNumber).getHash(); // FIXME: can be improved by accessing the hash directly in the index
    }

    @Override
    public void flush(){

        w.lock();
        try {
            long t1 = System.nanoTime();

            for (Block block : blocksCache.values()) {
                saveBlockIntoDisk(block);
            }

            for (Map.Entry<Long, ArrayList<BlockInfo>> e : indexCache.entrySet()) {
                Long number = e.getKey();
                ArrayList<BlockInfo> infos = e.getValue();
                saveBlockInfoIntoDisk(number, infos);
            }

            blocksCache.clear();
            indexCache.clear();

            long t2 = System.nanoTime();
            logger.info("Flush block store in: {} ms", ((float)(t2 - t1) / 1_000_000));
        } finally {
            w.unlock();
        }
    }

    @Override
    public void saveBlock(Block block, BigInteger cummDifficulty, boolean mainChain){
        w.lock();
        try {
            addInternalBlock(block, cummDifficulty, mainChain);
        } finally {
            w.unlock();
        }

        // If this block is on main chain, cache its timestamp.
        if (mainChain) {
            addBlockTime(block);
        }
    }

    private void addInternalBlock(Block block, BigInteger cummDifficulty, boolean mainChain){

        ArrayList<BlockInfo> blockInfos = index.get(block.getNumber());
        if (blockInfos == null){
            blockInfos = new ArrayList<>();
        }

        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setNumber(block.getNumber());
        blockInfo.setCummDifficulty(cummDifficulty);
        blockInfo.setHash(block.getHash());
        blockInfo.setMainChain(mainChain); // FIXME:maybe here I should force reset main chain for all uncles on that level
        blockInfos.add(blockInfo);

        blocksCache.put(new ByteArrayWrapper(block.getHash()), block);
        indexCache.put(block.getNumber(), blockInfos);

        index.put(block.getNumber(), blockInfos);
        blocks.put(new ByteArrayWrapper(block.getHash()), block);

        logger.debug("Save block with number {}, hash {}, raw {}", block.getNumber(),
                Hex.toHexString(block.getHash()), block.getHash());
    }

    public void delNonChainBlock(byte[] hash) {
        w.lock();
        try {
            Block block = blocks.get(new ByteArrayWrapper(hash));
            if (block == null)
                return;

            ArrayList<BlockInfo> blockInfos = index.get(block.getNumber());
            if (blockInfos == null)
                return;

            for (BlockInfo blockInfo : blockInfos) {
                if (areEqual(blockInfo.getHash(), hash)) {
                    if (blockInfo.mainChain) {
                        return;
                    }
                    blockInfos.remove(blockInfo);
                    removeBlockFromDisk(blockInfo);
                    break;
                }
            }

            index.put(block.getNumber(), blockInfos);
            if (indexCache.get(block.getNumber()) != null) {
                indexCache.put(block.getNumber(), blockInfos);
            }
            blocks.remove(new ByteArrayWrapper(hash));
            blocksCache.remove(new ByteArrayWrapper(hash));
        } finally {
            w.unlock();
        }
    }

    //Do not use this interface easily (different blocks in different branch may have common parent)
    public void delNonChainBlocksEndWith(byte[] hash) {
        Block block = getBlockByHash(hash);
        if (block == null)
            return;
        List<BlockInfo> blockInfos = getBlockInfoForLevel(block.getNumber());
        if (blockInfos == null)
            return;
        BlockInfo blockInfo = getBlockInfoForHash(blockInfos, hash);
        if (blockInfo == null || blockInfo.mainChain)
            return;

        delNonChainBlock(hash);

        delNonChainBlocksEndWith(block.getPreviousHeaderHash());
    }

    public void delNonChainBlocksByNumber(long number) {
        w.lock();
        try {
            ArrayList<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null)
                return;

            ArrayList<BlockInfo> newBlockInfos = new ArrayList<>();
            for (BlockInfo blockInfo : blockInfos) {
                if (blockInfo.mainChain) {
                    newBlockInfos.add(blockInfo);
                } else {
                    blocksCache.remove(new ByteArrayWrapper(blockInfo.hash));
                    if (blocks.remove(new ByteArrayWrapper(blockInfo.hash)) != null) {
                        removeBlockFromDisk(blockInfo);
                    }
                }
            }

            index.put(number, newBlockInfos);
            if (indexCache.get(number) != null) {
                indexCache.put(number, newBlockInfos);
            }
        } finally {
            w.unlock();
        }
    }

    @Override
    public void delChainBlocksWithNumberLessThan(long number) {
        long startTime = System.nanoTime();
        final long startNumber = number;

        while (--number > 0) {
            delChainBlockByNumber(number);
        }

        logger.info("Remove block with numbe from {} to {} cost {}ns",
                startNumber, number, System.nanoTime() - startTime);
    }

    @Override
    public void delChainBlockByNumber(long number) {
        if (number <= 0) {
            return;
        }

        w.lock();
        try {
            // Remove blocks
            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null)
                return;

            for (BlockInfo blockInfo : blockInfos) {
                blocks.remove(new ByteArrayWrapper(blockInfo.hash));
                blocksCache.remove(new ByteArrayWrapper(blockInfo.hash));
                removeBlockFromDisk(blockInfo);
            }

            // Remove index
            index.remove(number);
            indexCache.remove(number);
            removeBlockInfoFromDisk(number);

            logger.debug("remove block with number {}", number);
        } finally {
            w.unlock();
        }
    }

    public List<Block> getBlocksByNumber(long number){

        r.lock();
        try {
            List<Block> result = new ArrayList<>();

            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null){
                return result;
            }

            for (BlockInfo blockInfo : blockInfos){

                byte[] hash = blockInfo.getHash();
                Block block = blocks.get(new ByteArrayWrapper(hash));
                result.add(block);
            }

            return result;
        } finally {
            r.unlock();
        }
    }

    @Override
    public Block getChainBlockByNumber(long number){

        r.lock();
        try {
            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null){
                return null;
            }

            for (BlockInfo blockInfo : blockInfos){

                if (blockInfo.isMainChain()){

                    byte[] hash = blockInfo.getHash();
                    Block block = blocks.get(new ByteArrayWrapper(hash));

                    return block;
                }
            }
        } finally {
            r.unlock();
        }

        return null;
    }

    @Override
    public Block getBlockByHash(byte[] hash) {

        r.lock();
        try {
            return blocks.get(new ByteArrayWrapper(hash));
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean isBlockExist(byte[] hash) {

        r.lock();
        try {
            return getBlockByHash(hash) != null;
        } finally {
            r.unlock();
        }
    }

    @Override
    public BigInteger getTotalDifficultyForHash(byte[] hash){

        r.lock();
        try {
            Block block = this.getBlockByHash(hash);
            if (block == null) return ZERO;

            Long level  =  block.getNumber();
            List<BlockInfo> blockInfos =  index.get(level);
            if (blockInfos != null) {
                for (BlockInfo blockInfo : blockInfos) {
                    if (areEqual(blockInfo.getHash(), hash)) {
                        return blockInfo.cummDifficulty;
                    }
                }
            }
        } finally {
            r.unlock();
        }

        return ZERO;
    }


    @Override
    public BigInteger getTotalDifficulty(){

        BigInteger cacheTotalDifficulty = ZERO;

        long maxNumber = getMaxNumber();
        r.lock();
        try {
            List<BlockInfo> blockInfos = index.get(maxNumber);
            if (blockInfos != null) {
                for (BlockInfo blockInfo : blockInfos) {
                    if (blockInfo.isMainChain()) {
                        return blockInfo.getCummDifficulty();
                    }
                }
            }
        } finally {
            r.unlock();
        }

        return cacheTotalDifficulty;
    }

    @Override
    public long getMaxNumber() {
        return getIndexMaxNumber();
    }

    public long getIndexMaxNumber() {
        long maxNumber = -1;
        long minNumber = -1;

        r.lock();
        try {
            if (index.size() > 0){
                maxNumber = index.lastKey();
                minNumber = index.firstKey();
                logger.info("Block store min number {}, max number {}, size {}",
                        minNumber, maxNumber, index.size());
            }

            return maxNumber;
        } finally {
            r.unlock();
        }
    }

    @Override
    public List<byte[]> getListHashesEndWith(byte[] hash, long number){

        List<Block> blocks = getListBlocksEndWith(hash, number);
        List<byte[]> hashes = new ArrayList<>(blocks.size());

        for (Block b : blocks) {
            hashes.add(b.getHash());
        }

        return hashes;
    }

    @Override
    public List<Block> getListBlocksEndWith(byte[] hash, long qty) {

        r.lock();
        try {
            return getListBlocksEndWithInner(hash, qty);
        } finally {
            r.unlock();
        }
    }

    private List<Block> getListBlocksEndWithInner(byte[] hash, long qty) {

        r.lock();
        try {
            Block lastBlock = this.blocks.get(new ByteArrayWrapper(hash));

            if (lastBlock == null) return new ArrayList<>();

            List<Block> blocks = new ArrayList<>((int) qty);
            Block block = lastBlock;

            for (int i = 0; i < qty; ++i) {
                blocks.add(block);
                block = this.blocks.get(new ByteArrayWrapper(block.getPreviousHeaderHash()));
                if (block == null) break;
            }

            return blocks;
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean getForkBlocksInfo(Block forkBlock, List<Block> undoBlocks, List<Block> newBlocks) {
        Block bestBlock = getBestBlock();

        long maxLevel = Math.max(bestBlock.getNumber(), forkBlock.getNumber());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        Block forkLine = forkBlock;
        if (forkBlock.getNumber() > bestBlock.getNumber()){

            while(currentLevel > bestBlock.getNumber()){
                newBlocks.add(forkLine);

                forkLine = getBlockByHash(forkLine.getPreviousHeaderHash());
                if (forkLine == null)
                    return false;
                --currentLevel;
            }
        }

        Block bestLine = bestBlock;
        if (bestBlock.getNumber() > forkBlock.getNumber()){

            while(currentLevel > forkBlock.getNumber()){
                undoBlocks.add(bestLine);

                bestLine = getBlockByHash(bestLine.getPreviousHeaderHash());
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while( !bestLine.isEqual(forkLine) ) {
            newBlocks.add(forkLine);
            undoBlocks.add(bestLine);

            bestLine = getBlockByHash(bestLine.getPreviousHeaderHash());
            forkLine = getBlockByHash(forkLine.getPreviousHeaderHash());

            if (forkLine == null)
                return false;

            --currentLevel;
        }

        return true;
    }

    @Override
    public void reBranchBlocks(List<Block> undoBlocks, List<Block> newBlocks) {
        if (undoBlocks != null) {
            for (Block block : undoBlocks) {
                ArrayList<BlockInfo> blocks = getBlockInfoForLevel(block.getNumber());
                BlockInfo blockInfo = getBlockInfoForHash(blocks, block.getHash());
                if (blockInfo != null) {
                    blockInfo.setMainChain(false);
                    updateBlockInfoForLevel(block.getNumber(), blocks);
                }

                // Remove block time cache entry for undoBlocks.
                removeBlockTime(block.getNumber());
            }
        }

        if (newBlocks != null) {
            for (Block block : newBlocks) {
                ArrayList<BlockInfo> blocks = getBlockInfoForLevel(block.getNumber());
                BlockInfo blockInfo = getBlockInfoForHash(blocks, block.getHash());
                if (blockInfo != null) {
                    blockInfo.setMainChain(true);
                    updateBlockInfoForLevel(block.getNumber(), blocks);
                }

                // Add block time cache entry for newBlocks.
                addBlockTime(block);
            }
        }
    }


    public List<byte[]> getListHashesStartWith(long number, long maxBlocks){

        List<byte[]> result = new ArrayList<>();

        int i;
        r.lock();
        try {
            for ( i = 0; i < maxBlocks; ++i){
                List<BlockInfo> blockInfos =  index.get(number);
                if (blockInfos == null) break;

                for (BlockInfo blockInfo : blockInfos)
                   if (blockInfo.isMainChain()){
                       result.add(blockInfo.getHash());
                       break;
                   }

                ++number;
            }
        } finally {
            r.unlock();
        }

        return result;
    }

    @Override
    public long getBlockTimeByNumber(long blockNumber) {
        Long blockTime = getBlockTime(blockNumber);

        if (blockTime != null) {
            return blockTime;
        }

        Block block = getChainBlockByNumber(blockNumber);
        if (block == null) {
            return 0;
        }

        long timeStamp = ByteUtil.byteArrayToLong(block.getTimestamp());
        addBlockTime(block.getNumber(), timeStamp);

        return timeStamp;
    }

    public static class BlockInfo implements Serializable {
        long number;
        byte[] hash;
        BigInteger cummDifficulty;
        boolean mainChain;

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }

        public byte[] getHash() {
            return hash;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

        public BigInteger getCummDifficulty() {
            return cummDifficulty;
        }

        public void setCummDifficulty(BigInteger cummDifficulty) {
            this.cummDifficulty = cummDifficulty;
        }

        public boolean isMainChain() {
            return mainChain;
        }

        public void setMainChain(boolean mainChain) {
            this.mainChain = mainChain;
        }
    }

    private static class LRUCache extends LinkedHashMap<Long, Long> {
        private static final long serialVersionUID = 1L;
        private int capacity;

        public LRUCache(int capacity, float loadFactor) {
            super(capacity, loadFactor, false);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
            return size() > this.capacity;
        }
    }

    private static void addBlockTime(long blockNumber, long timeStamp) {
        synchronized(sBlockTimeLock) {
            sBlockTimeCache.put(blockNumber, timeStamp);
        }
    }

    private static void addBlockTime(Block block) {
        long timeStamp = ByteUtil.byteArrayToLong(block.getTimestamp());
        addBlockTime(block.getNumber(), timeStamp);
    }

    private static void removeBlockTime(long blockNumber) {
        synchronized(sBlockTimeLock) {
            sBlockTimeCache.remove(blockNumber);
        }
    }

    private static Long getBlockTime(long blockNumber) {
        synchronized(sBlockTimeLock) {
            return sBlockTimeCache.get(blockNumber);
        }
    }

    public void printChain() {
        logger.info("****** print chain start ******");

        Long number = getMaxNumber();

        for (long i = 0; i < number; ++i){
            List<BlockInfo> levelInfos = index.get(i);

            if (levelInfos != null) {
                System.out.print(i);
                for (BlockInfo blockInfo : levelInfos){
                    if (blockInfo.isMainChain())
                        logger.info(" [" + shortHash(blockInfo.getHash()) + "] ");
                    else
                        logger.info(" " + shortHash(blockInfo.getHash()) + " ");
                }
            }

        }

        logger.info("****** print chain end ******");
    }

    private ArrayList<BlockInfo> getBlockInfoForLevel(Long level){

        r.lock();
        try {
            return index.get(level);
        } finally {
            r.unlock();
        }
    }

    private void updateBlockInfoForLevel(Long level, ArrayList<BlockInfo> infos) {
        indexCache.put(level, infos);
        index.put(level, infos);
    }

    private static BlockInfo getBlockInfoForHash(List<BlockInfo> blocks, byte[] hash){

        for (BlockInfo blockInfo : blocks)
            if (areEqual(hash, blockInfo.getHash())) return blockInfo;

        return null;
    }

    @Override
    public void load() {
        // Load every thing from file system.
        logger.info("load everything from file system");
        long t1 = System.nanoTime();

        String blockStoreDir = CONFIG.databaseDir() + File.separator + BLOCKSTORE_DIRECTORY;
        File f = new File(blockStoreDir);
        File[] files = f.listFiles();

        try {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile()) {
                    continue;
                }

                String name = files[i].getName();
                logger.debug("file name: {}", name);

                if (name.contains("_")) {
                    // block data file
                    Block block = readBlock(name);
                    blocks.put(new ByteArrayWrapper(block.getHash()), block);
                } else {
                    // block info data file
                    ArrayList<BlockInfo> infoList = readBlockInfoList(name);
                    if (infoList != null && !infoList.isEmpty()) {
                        index.put(infoList.get(0).getNumber(), infoList);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("load fatal error {}", e);
            throw new RuntimeException(e.getMessage());
        }

        checkSanity();

        long t2 = System.nanoTime();
        logger.info("Load block store in: {} ms", ((float)(t2 - t1) / 1_000_000));
    }

    public void setSessionFactory(SessionFactory sessionFactory){
        throw new UnsupportedOperationException();
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

    private void saveBlockIntoDisk(Block block) {
        try {
            writeBlock(blockFileName(block), block);
        } catch (Exception e) {
            logger.error("save block {} {} exception: {}", block.getNumber(),
                    block.getShortHash(), e);
        }
    }

    private void saveBlockInfoIntoDisk(long number, ArrayList<BlockInfo> infoList) {
        try {
            writeBlockInfoList(blockInfoFileName(number), infoList);
        } catch (Exception e) {
            logger.error("save info {} exception: {}", number, e);
        }
    }

    private void writeBlockInfoList(String fileName, ArrayList<BlockInfo> obj)
            throws Exception {
        String filePath = getAbsoluteFileName(fileName);
        removeIfExist(filePath);
        checkFile(filePath);

		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
			logger.debug("Write {} blockinfo success", fileName);
		} catch (FileNotFoundException e) {
			logger.error("Write block info {} exception:{}", fileName, e);
			throw e;
		} catch (IOException e) {
			logger.error("Write block info {} exception:{}", fileName, e);
			throw e;
		}
    }

    private ArrayList<BlockInfo> readBlockInfoList(String fileName)
            throws Exception {
        String filePath = getAbsoluteFileName(fileName);
        ArrayList<BlockInfo> list = null;

		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			list = (ArrayList<BlockInfo>)in.readObject();
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
			logger.error("Read block info {} exception:{}", fileName, e);
			throw e;
		} catch (IOException e) {
			logger.error("Read block info {} exception:{}", fileName, e);
			throw e;
		}

		return list;
    }

    private void writeBlock(String fileName, Block block) throws Exception {
        String filePath = getAbsoluteFileName(fileName);
        checkFile(filePath);

        FileChannel writeFileChan = new RandomAccessFile(filePath, "rw").getChannel();
        byte[] bytes = block.getEncoded();
        ByteBuffer bf = ByteBuffer.wrap(bytes);
        FileLock fileLock = null;

        try {
            fileLock = writeFileChan.lock();
            writeFileChan.truncate(0L);
            writeFileChan.position(0L);
            writeFileChan.write(bf);
            writeFileChan.force(true);
        } catch (IOException e) {
            logger.error("Write block {} {} exception: {}", block.getNumber(),
                    Hex.toHexString(block.getHash()), e);
            throw e;
        } finally {
            if (fileLock != null) {
                fileLock.release();
            }
            writeFileChan.close();
        }
    }

    private Block readBlock(String fileName) throws Exception {
        Block block = null;
        String filePath = getAbsoluteFileName(fileName);    

        FileChannel readFileChan = new RandomAccessFile(filePath, "rw").getChannel();
        FileLock fileLock = null;

        try {
            fileLock = readFileChan.lock();
            ByteBuffer bf = ByteBuffer.allocate((int)readFileChan.size());
            int readSize = readFileChan.read(bf, 0L);

            if (readSize != (int)readFileChan.size()) {
                throw new IOException(String.format(
                        "Can't read full file %s: read %d bytes, file size %d",
                        fileName, readSize, readFileChan.size()));
            }
        
            byte[] bytes = bf.array();
            block = new Block(bytes);
        } catch(IOException e) {
            logger.error("Read {} exception: {}", fileName, e);
            throw e;
        } finally {
            if (fileLock != null) {
                fileLock.release();
            }
            readFileChan.close();
        }

        return block;
    }

    private void removeBlockFromDisk(BlockInfo info) {
        String fileName = getAbsoluteFileName(blockFileName(
                info.getNumber(), info.getHash()));
        removeIfExist(fileName);
    }

    private void removeBlockInfoFromDisk(BlockInfo info) {
        String fileName = getAbsoluteFileName(blockInfoFileName(info.getNumber()));
        removeIfExist(fileName);
    }

    private void removeBlockInfoFromDisk(long number) {
        String fileName = getAbsoluteFileName(blockInfoFileName(number));
        removeIfExist(fileName);
    }

    private static String getAbsoluteFileName(String name) {
        return CONFIG.databaseDir() + File.separator + BLOCKSTORE_DIRECTORY
                + File.separator + name;
    }

    private static String blockFileName(Block block) {
        return blockFileName(block.getNumber(), block.getHash());
    }

    private static String blockFileName(long number, byte[] hash) {
        return String.format("%d_%s", number, Hex.toHexString(hash).substring(0, 6));
    }

    private static String blockInfoFileName(Block block) {
        return blockInfoFileName(block.getNumber());
    }

    private static String blockInfoFileName(long number) {
        return String.format("%d", number);
    }

    private static void removeIfExist(String file) {
        File f = new File(file);

        try {
            if (f.exists()) {
               f.delete();
            }
        } catch (Exception e) {
            logger.error("remove file {} exception: {}", file, e);
        }
    }

    // If not exist, create file
    private static void checkFile(String file) throws Exception {
        File f = new File(file);

        try {
            if (!f.exists()) {
               f.createNewFile();
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private void checkSanity() {
        for (Map.Entry<Long, ArrayList<BlockInfo>> entry : index.entrySet()) {
            ArrayList<BlockInfo> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                for (BlockInfo info : list) {
                    ByteArrayWrapper key = new ByteArrayWrapper(info.getHash());
                    if (blocks.get(key) == null) {
                        String errorString = String.format(
                                "Block %d %s not exist.",
                                info.getNumber(),
                                Hex.toHexString(info.getHash()));
                        logger.error(errorString);
                        //throw new RuntimeException(errorString);
                    }
                }
            }
        }
    }
}
