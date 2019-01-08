package io.taucoin.core;

import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.BlockIdentifier;
import org.ethereum.core.Chain;
import org.ethereum.core.Genesis;
import org.ethereum.core.ImportResult;
import org.ethereum.core.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;

public interface Blockchain {

    public static final byte[] GENESIS_HASH = Genesis.getInstance().getHash();

    public long getSize();

    public void add(Block block);

    public ImportResult tryToConnect(Block block);

    public void storeBlock(Block block, List<TransactionReceipt> receipts);

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

    TransactionReceipt getTransactionReceiptByHash(byte[] hash);

    public Block getBlockByHash(byte[] hash);

    public List<Chain> getAltChains();

    public List<Block> getGarbage();

    public void setExitOn(long exitOn);

    boolean isBlockExist(byte[] hash);

    List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse);

    List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes);
}
