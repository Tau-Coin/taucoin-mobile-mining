package io.taucoin.android.db;

import io.taucoin.core.Block;
import io.taucoin.db.BlockStore;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.util.*;

import com.tencent.mmkv.MMKV;
import com.tencent.mmkv.MMKVHandler;
import com.tencent.mmkv.MMKVLogLevel;
import com.tencent.mmkv.MMKVRecoverStrategic;
import org.apache.commons.io.FileUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * This implementation stores the latest CONFIG.blockStoreCapability() blocks
 * in tencent mmkv database.
 */

@Singleton
public class MMKVIndexedBlockStore implements BlockStore {

    private static final Logger logger = LoggerFactory.getLogger("mmkv-blockstore");

    private static final String BLOCKSTORE_DIRECTORY = "blockstore";
    private static final String BLOCKS_DIRECTORY = "blocks";
    private static final String INDEXES_DIRECTORY = "indexes";

    private MMKV blocksDB;
    private MMKV indexesDB;

    // Blocks and blockinfo cache.
    private Map<ByteArrayWrapper, Block> blocksCache
            = new HashMap<ByteArrayWrapper, Block>();
    private Map<Long, ArrayList<BlockInfo>> indexCache
            = new HashMap<Long, ArrayList<BlockInfo>>();

    private TreeMap<Long, ArrayList<BlockInfo>> index
                    = new TreeMap<Long, ArrayList<BlockInfo>>();

    // Block time cache: height -> block time.
    private static final int BLOCKTIME_CACHE_CAPACITY = CONFIG.blockStoreCapability();
    private static LRUCache sBlockTimeCache
            = new LRUCache(BLOCKTIME_CACHE_CAPACITY, 0.75f);
    private static Object sBlockTimeLock = new Object();

    // Read and write lock.
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();

    private volatile boolean isAlive = false;

    @Inject
    public MMKVIndexedBlockStore() {
        init();
    }

    private void init() {
        // create directory
        initDirectory(CONFIG.databaseDir() + File.separator + BLOCKSTORE_DIRECTORY);
        initDirectory(getAbsoluteFileName(BLOCKS_DIRECTORY));
        initDirectory(getAbsoluteFileName(INDEXES_DIRECTORY));

        // initialize mmkv db.
        initMMKVDB();

        // Load and print chain.
        load();
        printChain();
    }

    @Override
    public void close(){
        logger.info("close block store data base...");

        blocksDB.close();
        indexesDB.close();

        isAlive = false;
    }

    @Override
    public void reset(){
        close();
        clearBlockStore();

        // init everything
        init();
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
                saveBlockIntoDB(block);
            }

            for (Map.Entry<Long, ArrayList<BlockInfo>> e : indexCache.entrySet()) {
                Long number = e.getKey();
                ArrayList<BlockInfo> infos = e.getValue();
                saveBlockInfoIntoDB(number, infos);
            }

            blocksCache.clear();
            indexCache.clear();

            blocksDB.commit();
            indexesDB.commit();

            long t2 = System.nanoTime();
            logger.debug("Flush block store in: {} ms", ((float)(t2 - t1) / 1_000_000));
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

        logger.debug("Save block with number {}, hash {}, raw {}", block.getNumber(),
                Hex.toHexString(block.getHash()), block.getHash());
    }

    public void delNonChainBlock(byte[] hash) {
        w.lock();
        try {
            byte[] blockBytes = blocksDB.decodeBytes(Hex.toHexString(hash));
            if (blockBytes == null)
                return;
            Block block = new Block(blockBytes);

            ArrayList<BlockInfo> blockInfos = index.get(block.getNumber());
            if (blockInfos == null)
                return;

            for (BlockInfo blockInfo : blockInfos) {
                if (areEqual(blockInfo.getHash(), hash)) {
                    if (blockInfo.mainChain) {
                        return;
                    }
                    blockInfos.remove(blockInfo);
                    removeBlockFromDB(blockInfo);
                    break;
                }
            }

            if (indexCache.get(block.getNumber()) != null) {
                indexCache.put(block.getNumber(), blockInfos);
            }
            blocksCache.remove(new ByteArrayWrapper(hash));

            index.put(block.getNumber(), blockInfos);
            saveBlockInfoIntoDB(block.getNumber(), blockInfos);
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
                    removeBlockFromDB(blockInfo);
                }
            }

            if (indexCache.get(number) != null) {
                indexCache.put(number, newBlockInfos);
            }
            index.put(number, newBlockInfos);
            saveBlockInfoIntoDB(number, newBlockInfos);
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
                blocksCache.remove(new ByteArrayWrapper(blockInfo.hash));
                removeBlockFromDB(blockInfo);
            }

            // Remove index
            index.remove(number);
            indexCache.remove(number);
            removeBlockInfoFromDB(number);

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
                byte[] blockBytes = blocksDB.decodeBytes(Hex.toHexString(hash));
                if (blockBytes != null) {
                    result.add(new Block(blockBytes));
                }
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
                    Block block = null;
                    byte[] blockBytes = blocksDB.decodeBytes(Hex.toHexString(hash));

                    if (blockBytes != null) {
                        block = new Block(blockBytes);
                    }

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
            byte[] blocksBytes = blocksDB.decodeBytes(Hex.toHexString(hash));
            if (blocksBytes == null) {
                return null;
            }

            return new Block(blocksBytes);
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
                logger.debug("Block store min number {}, max number {}, size {}",
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
            byte[] lastBlockBytes = this.blocksDB.decodeBytes(Hex.toHexString(hash));

            if (lastBlockBytes == null) return new ArrayList<>();

            Block lastBlock = new Block(lastBlockBytes);
            List<Block> blocks = new ArrayList<>((int) qty);
            Block block = lastBlock;
            byte[] blockBytes;

            for (int i = 0; i < qty; ++i) {
                blocks.add(block);
                blockBytes = this.blocksDB.decodeBytes(
                        Hex.toHexString(block.getPreviousHeaderHash()));
                if (blockBytes == null) break;
                block = new Block(blockBytes);
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

        public BlockInfo() {
        }

        public BlockInfo(byte[] bytes) {
            parse(bytes);
        }

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

        private void parse(byte[] bytes) {
            List<RLPElement> params = RLP.decode2(bytes);
            List<RLPElement> info = (RLPList) params.get(0);

            byte[] numberBytes = info.get(0).getRLPData();
            byte[] hashBytes = info.get(1).getRLPData();
            byte[] cummDifficultyBytes = info.get(2).getRLPData();
            byte[] mainChainBytes = info.get(3).getRLPData();

            this.number = numberBytes == null ?
                    0 : new BigInteger(1, numberBytes).longValue();
            this.hash = hashBytes;
            this.cummDifficulty = cummDifficultyBytes == null ?
                    BigInteger.ZERO : new BigInteger(1, cummDifficultyBytes);
            byte mainChain = mainChainBytes == null ? (byte)0 : mainChainBytes[0];
            this.mainChain = mainChain == 1;
        }

        public byte[] getEncoded() {
            byte[] numberBytes = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
            byte[] hashBytes = RLP.encodeElement(this.hash);
            byte[] cummDifficultyBytes = RLP.encodeBigInteger(this.cummDifficulty);
            byte[] mainChainBytes = RLP.encodeByte((byte) (mainChain ? 1 : 0));

            return RLP.encodeList(numberBytes, hashBytes, cummDifficultyBytes,
                    mainChainBytes);
        }
    }

    private byte[] getBlockInfoListEncoded(ArrayList<BlockInfo> infos) {
        byte[][] infosEncoded = new byte[infos.size()][];
        int i = 0;
        for (BlockInfo info : infos) {
            infosEncoded[i] = info.getEncoded();
            ++i;
        }

        return RLP.encodeList(infosEncoded);
    }

    private ArrayList<BlockInfo> parseBlockInfoList(byte[] bytes) {
        ArrayList<BlockInfo> infos = new ArrayList<BlockInfo>();

        if (bytes != null && bytes.length > 0) {
            RLPList params = RLP.decode2(bytes);
            RLPList infosList = (RLPList) params.get(0);

            for (int i = 0; i < infosList.size(); i++) {
                RLPElement infoBytes = infosList.get(i);
                infos.add(new BlockInfo(infoBytes.getRLPData()));
            }
        }

        return infos;
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

        for (Map.Entry<Long, ArrayList<BlockInfo>> e : index.entrySet()) {
            Long number = e.getKey();
            ArrayList<BlockInfo> infos = e.getValue();
            for (BlockInfo blockInfo : infos){
                if (blockInfo.isMainChain()) {
                    logger.info(blockInfo.getNumber() + " [" + shortHash(blockInfo.getHash()) + "] ");
                } else
                    logger.info(blockInfo.getNumber() + " " + shortHash(blockInfo.getHash()) + " ");
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

    private void initMMKVDB() {
        String root = MMKV.getRootDir();
        if (root == null) {
            root = MMKV.initialize(CONFIG.databaseDir());
            MMKV.registerHandler(new MMKVHandlerImpl());
        }

        blocksDB = MMKV.mmkvWithID(BLOCKS_DIRECTORY, getAbsoluteFileName(BLOCKS_DIRECTORY));
        indexesDB = MMKV.mmkvWithID(INDEXES_DIRECTORY, getAbsoluteFileName(INDEXES_DIRECTORY));
        if (blocksDB != null && indexesDB != null) {
            logger.info("Blocks and indexes db is alive");
            isAlive = true;
        } else {
            logger.error("Blocks and indexes db is dead");
        }
    }

    @Override
    public void load() {
        String[] keys = indexesDB.allKeys();
        if (keys == null || keys.length == 0) {
            logger.info("Empty block indexes db");
            return;
        }

        for (String number : keys) {
            ArrayList<BlockInfo> infos = parseBlockInfoList(
                    indexesDB.decodeBytes(number));
            if (infos.size() > 0) {
                index.put(Long.parseLong(number), infos);
            }
        }

        checkSanity();
    }

    public void setSessionFactory(SessionFactory sessionFactory){
        throw new UnsupportedOperationException();
    }

    private static void initDirectory(String dir) {
        File f = new File(dir);

        if (f.exists()) {
            if (!f.isDirectory()) {
                throw new RuntimeException(dir + " isn't a directory");
            }
        } else {
            f.mkdir();
        }
    }

    private void clearBlockStore() {
        String absolutePath = CONFIG.databaseDir() + File.separator + BLOCKSTORE_DIRECTORY;

        logger.warn("Clear block store: {}", absolutePath);
        try {
            // Clear cache
            index.clear();
            blocksCache.clear();
            indexCache.clear();
            sBlockTimeCache.clear();

            FileUtils.deleteDirectory(new File(absolutePath));
        } catch (IOException e) {
            logger.error("Clear block store error:{}", e);
        }
    }

    private void saveBlockIntoDB(Block block) {
        blocksDB.encode(Hex.toHexString(block.getHash()), block.getEncoded());
    }

    private void saveBlockInfoIntoDB(long number, ArrayList<BlockInfo> infoList) {
        indexesDB.encode(String.valueOf(number), getBlockInfoListEncoded(infoList));
    }

    private void removeBlockFromDB(BlockInfo info) {
        blocksDB.removeValueForKey(Hex.toHexString(info.getHash()));
    }

    private void removeBlockInfoFromDB(BlockInfo info) {
        removeBlockInfoFromDB(info.getNumber());
    }

    private void removeBlockInfoFromDB(long number) {
        indexesDB.removeValueForKey(String.valueOf(number));
    }

    private String getAbsoluteFileName(String name) {
        return CONFIG.databaseDir() + File.separator + BLOCKSTORE_DIRECTORY
                + File.separator + name;
    }

    private void checkSanity() {
        for (Map.Entry<Long, ArrayList<BlockInfo>> entry : index.entrySet()) {
            ArrayList<BlockInfo> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                for (BlockInfo info : list) {
                    String key = Hex.toHexString(info.getHash());
                    if (blocksDB.decodeBytes(key) == null) {
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

    private static class MMKVHandlerImpl implements MMKVHandler {

        public MMKVHandlerImpl() {
        }

        @Override
        public MMKVRecoverStrategic onMMKVCRCCheckFail(String mmapID) {
            logger.error("MMKV CRC Check Fail");
            return MMKVRecoverStrategic.OnErrorRecover;
        }

        @Override
        public MMKVRecoverStrategic onMMKVFileLengthError(String mmapID) {
            logger.error("MMKV File Length Error");
            return MMKVRecoverStrategic.OnErrorRecover;
        }

        @Override
        public boolean wantLogRedirecting() {
            return true;
        }

        @Override
        public void mmkvLog(MMKVLogLevel level, String file, int line, String function, String message) {
            String log = "<" + file + ":" + line + "::" + function + "> " + message;

            switch (level) {
                case LevelDebug:
                    break;
                case LevelInfo:
                    break;
                case LevelWarning:
                    logger.warn(log);
                    break;
                case LevelError:
                    logger.error(log);
                    break;
                case LevelNone:
                    break;
            }
        }
    }
}
