package io.taucoin.core;

import java.math.BigInteger;
import java.util.List;

public interface Blockchain {

    public static final byte[] GENESIS_HASH = Genesis.getInstance().getHash();

    public long getSize();

    public boolean addBlock(Block block);

    public ImportResult tryToConnect(Block block);

    public void storeBlock(Block block);

    public Block getBlockByNumber(long blockNr);

    public void setBestBlock(Block block);

    public Block getBestBlock();

    public boolean hasParentOnTheChain(Block block);

    void close();

    public void updateTotalDifficulty(Block block);

    public BigInteger getTotalDifficulty();

    public void setTotalDifficulty(BigInteger totalDifficulty);

    public byte[] getBestBlockHash();

    public List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty);

    public List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty);

    public Block getBlockByHash(byte[] hash);

    public List<Chain> getAltChains();

    public List<Block> getGarbage();

    public void setExitOn(long exitOn);

    boolean isBlockExist(byte[] hash);

    List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse);

    List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes);

    Transaction getTransactionByHash(byte[] hash);

    Block createNewBlock(Block parent, BigInteger baseTarget, byte[] generationSignature,
                         BigInteger cumulativeDifficulty, List<Transaction> transactions);

    /**
     * Get object to wait
     * @return
     */
    Object getLockObject();
}
