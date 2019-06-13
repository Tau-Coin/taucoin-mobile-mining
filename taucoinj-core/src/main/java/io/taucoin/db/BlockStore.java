package io.taucoin.db;

import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import org.hibernate.SessionFactory;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 08.01.2015
 * @author taucoin core
 * @since 01.16.2019
 */
public interface BlockStore {

    byte[] getBlockHashByNumber(long blockNumber);

    Block getChainBlockByNumber(long blockNumber);

    Block getBlockByHash(byte[] hash);
    boolean isBlockExist(byte[] hash);

    List<byte[]> getListHashesEndWith(byte[] hash, long qty);

    List<Block> getListBlocksEndWith(byte[] hash, long qty);

    void saveBlock(Block block, BigInteger cummDifficulty, boolean mainChain);

    public void delNonChainBlock(byte[] hash);

    public void delNonChainBlocksEndWith(byte[] hash);

    public void delNonChainBlocksByNumber(long number);

    BigInteger getTotalDifficultyForHash(byte[] hash);

    BigInteger getTotalDifficulty();

    Block getBestBlock();

    long getMaxNumber();

    void flush();

    boolean getForkBlocksInfo(Block forkBlock, List<Block> undoBlocks, List<Block> newBlocks);

    void reBranchBlocks(List<Block> undoBlocks, List<Block> newBlocks);

    void load();
    void setSessionFactory(SessionFactory sessionFactory);

    long getBlockTimeByNumber(long blockNumber);

    void close();
}
