package io.taucoin.db;

import io.taucoin.core.Block;
import io.taucoin.datasource.KeyValueDataSource;
import io.taucoin.util.ByteUtil;

import org.hibernate.SessionFactory;
import org.mapdb.DB;
import org.mapdb.DataIO;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigInteger.ZERO;
import static io.taucoin.crypto.HashUtil.shortHash;
import static org.spongycastle.util.Arrays.areEqual;

public class IndexedBlockStore implements BlockStore {

    private static final Logger logger = LoggerFactory.getLogger("general");

    IndexedBlockStore cache;
    Map<Long, List<BlockInfo>> index;
    KeyValueDataSource blocks;

    DB indexDB;

    // Block time cache: height -> block time.
    private static final int BLOCKTIME_CACHE_CAPACITY = 288;

    private static LRUCache sBlockTimeCache
            = new LRUCache(BLOCKTIME_CACHE_CAPACITY, 0.75f);

    private static Object sBlockTimeLock = new Object();

    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();

    public IndexedBlockStore(){
    }

    public void init(Map<Long, List<BlockInfo>> index, KeyValueDataSource blocks, IndexedBlockStore cache, DB indexDB) {
        this.cache = cache;
        this.index = index;
        this.blocks = blocks;
        this.indexDB  = indexDB;
    }

    @Override
    public void close(){
        logger.info("close block store data base...");

        w.lock();
        try {
            if (indexDB != null && blocks != null) {
                indexDB.close();
                indexDB = null;
                blocks.close();
                blocks = null;
            }
        } finally {
            w.unlock();
        }
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
            if (cache == null) return;

            long t1 = System.nanoTime();

            for (byte[] hash : cache.blocks.keys()){
                blocks.put(hash, cache.blocks.get(hash));
            }

            for (Map.Entry<Long, List<BlockInfo>> e : cache.index.entrySet()) {
                Long number = e.getKey();
                List<BlockInfo> infos = e.getValue();

                if (index.containsKey(number)) infos.addAll(index.get(number));
                index.put(number, infos);
            }

            cache.blocks.close();
            cache.index.clear();

            long t2 = System.nanoTime();

            if (indexDB != null) {
                indexDB.commit();
            }
            logger.debug("Flush block store in: {} ms", ((float)(t2 - t1) / 1_000_000));
        } finally {
            w.unlock();
        }
    }

    @Override
    public void saveBlock(Block block, BigInteger cummDifficulty, boolean mainChain){
        w.lock();
        try {
            if (cache == null)
                addInternalBlock(block, cummDifficulty, mainChain);
            else
                cache.saveBlock(block, cummDifficulty, mainChain);
        } finally {
            w.unlock();
        }

        // If this block is on main chain, cache its timestamp.
        if (mainChain) {
            addBlockTime(block);
        }
    }

    private void addInternalBlock(Block block, BigInteger cummDifficulty, boolean mainChain){

        List<BlockInfo> blockInfos = index.get(block.getNumber());
        if (blockInfos == null){
            blockInfos = new ArrayList<>();
        }

        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setCummDifficulty(cummDifficulty);
        blockInfo.setHash(block.getHash());
        blockInfo.setMainChain(mainChain); // FIXME:maybe here I should force reset main chain for all uncles on that level

        blockInfos.add(blockInfo);
        index.put(block.getNumber(), blockInfos);

        blocks.put(block.getHash(), block.getEncoded());
        logger.debug("Save block with number {}, hash {}, raw {}", block.getNumber(),
                Hex.toHexString(block.getHash()), block.getHash());
    }

    public void delNonChainBlock(byte[] hash) {
        w.lock();
        try {
            if (cache != null) {
                cache.delNonChainBlock(hash);
            }

            byte[] blockRlp = blocks.get(hash);
            if (blockRlp == null)
                return;
            Block block = new Block(blockRlp);

            List<BlockInfo> blockInfos = index.get(block.getNumber());
            if (blockInfos == null)
                return;
            for (BlockInfo blockInfo : blockInfos) {
                if (areEqual(blockInfo.getHash(), hash)) {
                    if (blockInfo.mainChain) {
                        return;
                    }
                    blockInfos.remove(blockInfo);
                    break;
                }
            }
            index.put(block.getNumber(), blockInfos);
            blocks.delete(hash);
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
            if (cache != null) {
                cache.delNonChainBlocksByNumber(number);
            }

            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null)
                return;
            List<BlockInfo> newBlockInfos = new ArrayList<>();
            for (BlockInfo blockInfo : blockInfos) {
                if (blockInfo.mainChain) {
                    newBlockInfos.add(blockInfo);
                } else {
                    blocks.delete(blockInfo.hash);
                }
            }
            index.put(number, newBlockInfos);
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
            if (cache != null) {
                cache.delChainBlockByNumber(number);
            }

            // Remove blocks
            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null)
                return;
            for (BlockInfo blockInfo : blockInfos) {
                blocks.delete(blockInfo.hash);
            }

            // Remove index
            index.remove(number);
            if (indexDB != null) {
                indexDB.commit();
            }
            logger.info("remove block with number {}", number);
        } finally {
            w.unlock();
        }
    }

    public List<Block> getBlocksByNumber(long number){

        r.lock();
        try {
            List<Block> result = new ArrayList<>();
            if (cache != null)
                result = cache.getBlocksByNumber(number);

            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null){
                return result;
            }

            for (BlockInfo blockInfo : blockInfos){

                byte[] hash = blockInfo.getHash();
                byte[] blockRlp = blocks.get(hash);

                result.add(new Block(blockRlp));
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
            if (cache != null) {
                Block block = cache.getChainBlockByNumber(number);
                if (block != null) return block;
            }

            List<BlockInfo> blockInfos = index.get(number);
            if (blockInfos == null){
                return null;
            }

            for (BlockInfo blockInfo : blockInfos){

                if (blockInfo.isMainChain()){

                    byte[] hash = blockInfo.getHash();
                    byte[] blockRlp = blocks.get(hash);
                    if (blockRlp == null){
                        return null;
                    }
                    return new Block(blockRlp);
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
            if (cache != null) {
                Block cachedBlock = cache.getBlockByHash(hash);
                if (cachedBlock != null) return cachedBlock;
            }

            byte[] blockRlp = blocks.get(hash);
            if (blockRlp == null)
                return null;
            return new Block(blockRlp);
        } finally {
            r.unlock();
        }
    }

    @Override
    public boolean isBlockExist(byte[] hash) {

        r.lock();
        try {
            if (cache != null) {
                Block cachedBlock = cache.getBlockByHash(hash);
                if (cachedBlock != null) return true;
            }

            byte[] blockRlp = blocks.get(hash);
            return blockRlp != null;
        } finally {
            r.unlock();
        }
    }


    @Override
    public BigInteger getTotalDifficultyForHash(byte[] hash){

        r.lock();
        try {
            if (cache != null && cache.getBlockByHash(hash) != null) {

                return cache.getTotalDifficultyForHash(hash);
            }

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
            if (cache != null) {

                List<BlockInfo> infos = getBlockInfoForLevel(maxNumber);

                if (infos != null){
                    for (BlockInfo blockInfo : infos){
                        if (blockInfo.isMainChain()){
                            return blockInfo.getCummDifficulty();
                        }
                    }

                    // todo: need better testing for that place
                    // here is the place when you know
                    // for sure that the potential fork
                    // branch is higher than main branch
                    // in that case the correct td is the
                    // first level when you have [mainchain = true] Blockinfo
                    boolean found = false;
                    Map<Long, List<BlockInfo>> searching = cache.index;
                    while (!found){

                        --maxNumber;
                        infos = getBlockInfoForLevel(maxNumber);

                        for (BlockInfo blockInfo : infos) {
                            if (blockInfo.isMainChain()) {
                                found = true;
                                return blockInfo.getCummDifficulty();
                            }
                        }
                    }
                }
            }

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
                maxNumber = Collections.max(index.keySet());
                minNumber = Collections.min(index.keySet());
                logger.debug("Block store min number {}, max number {}, size {}",
                        minNumber, maxNumber, index.size());
            }

            if (cache != null){
                long cacheMaxNumber = cache.getIndexMaxNumber();
                return maxNumber >= cacheMaxNumber ? maxNumber : cacheMaxNumber;
            } else
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
            if (cache == null)
                return getListBlocksEndWithInner(hash, qty);

            List<Block> cachedBlocks = cache.getListBlocksEndWith(hash, qty);

            if (cachedBlocks.size() == qty) return cachedBlocks;

            if (cachedBlocks.isEmpty())
                return getListBlocksEndWithInner(hash, qty);

            Block latestCached = cachedBlocks.get(cachedBlocks.size() - 1);

            List<Block> notCachedBlocks = getListBlocksEndWithInner(latestCached.getPreviousHeaderHash(), qty - cachedBlocks.size());
            cachedBlocks.addAll(notCachedBlocks);

            return cachedBlocks;
        } finally {
            r.unlock();
        }
    }

    private List<Block> getListBlocksEndWithInner(byte[] hash, long qty) {

        r.lock();
        try {
            byte[] rlp = this.blocks.get(hash);

            if (rlp == null) return new ArrayList<>();

            List<Block> blocks = new ArrayList<>((int) qty);

            for (int i = 0; i < qty; ++i) {

                Block block = new Block(rlp);
                blocks.add(block);
                rlp = this.blocks.get(block.getPreviousHeaderHash());
                if (rlp == null) break;
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
                List<BlockInfo> blocks = getBlockInfoForLevel(block.getNumber());
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
                List<BlockInfo> blocks = getBlockInfoForLevel(block.getNumber());
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
            maxBlocks -= i;

            if (cache != null)
                result.addAll( cache.getListHashesStartWith(number, maxBlocks) );
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
        byte[] hash;
        BigInteger cummDifficulty;
        boolean mainChain;

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


    public static final Serializer<List<BlockInfo>> BLOCK_INFO_SERIALIZER = new Serializer<List<BlockInfo>>(){

        @Override
        public void serialize(DataOutput out, List<BlockInfo> value) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);

            byte[] data = bos.toByteArray();
            DataIO.packInt(out, data.length);
            out.write(data);
        }

        @Override
        public List<BlockInfo> deserialize(DataInput in, int available) throws IOException {

            List<BlockInfo> value = null;
            try {
                int size = DataIO.unpackInt(in);
                byte[] data = new byte[size];
                in.readFully(data);

                ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, data.length);
                ObjectInputStream ois = new ObjectInputStream(bis);
                value = (List<BlockInfo>)ois.readObject();

            } catch (ClassNotFoundException e) {e.printStackTrace();}

            return value;
        }
    };

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

    public void printChain(){

        Long number = getMaxNumber();

        for (long i = 0; i < number; ++i){
            List<BlockInfo> levelInfos = index.get(i);

            if (levelInfos != null) {
                System.out.print(i);
                for (BlockInfo blockInfo : levelInfos){
                    if (blockInfo.isMainChain())
                        System.out.print(" [" + shortHash(blockInfo.getHash()) + "] ");
                    else
                        System.out.print(" " + shortHash(blockInfo.getHash()) + " ");
                }
                System.out.println();
            }

        }

        if (cache != null)
            cache.printChain();
    }

    private List<BlockInfo> getBlockInfoForLevel(Long level){

        r.lock();
        try {
            if (cache != null){
                List<BlockInfo> infos =  cache.index.get(level);
                if (infos != null) return infos;
            }

            return index.get(level);
        } finally {
            r.unlock();
        }
    }

    private void updateBlockInfoForLevel(Long level, List<BlockInfo> infos) {

        if (cache != null && cache.index != null) {
            cache.index.put(level, infos);
            return;
        }

        index.put(level, infos);
    }

    private static BlockInfo getBlockInfoForHash(List<BlockInfo> blocks, byte[] hash){

        for (BlockInfo blockInfo : blocks)
            if (areEqual(hash, blockInfo.getHash())) return blockInfo;

        return null;
    }

    @Override
    public void load() {
    }

    public void setSessionFactory(SessionFactory sessionFactory){
        throw new UnsupportedOperationException();
    }

}
