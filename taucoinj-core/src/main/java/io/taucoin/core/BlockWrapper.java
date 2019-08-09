package io.taucoin.core;


import io.taucoin.util.RLP;
import io.taucoin.util.RLPElement;
import io.taucoin.util.RLPList;

import java.math.BigInteger;
import java.util.List;

import static io.taucoin.util.TimeUtils.secondsToMillis;

/**
 * <p> Wraps {@link Block} </p>
 * Adds some additional data required by core during blocks processing
 *
 * @author Mikhail Kalinin
 * @since 24.07.2015
 */
public class BlockWrapper implements Comparable<BlockWrapper> {

    private static final long SOLID_BLOCK_DURATION_THRESHOLD = secondsToMillis(60);

    private Block block;

    private long importFailedAt = 0;
    private long receivedAt = 0;
    private boolean newBlock;
    private byte[] nodeId;

    // Raw block from network doesn't inlucde 'number' feild.
    private long number = 0;

    public BlockWrapper(Block block, byte[] nodeId) {
        this(block, false, nodeId);
    }

    public BlockWrapper(Block block, boolean newBlock, byte[] nodeId) {
        this.block = block;
        this.newBlock = newBlock;
        this.nodeId = nodeId;
    }

    public BlockWrapper(byte[] bytes) {
        parse(bytes);
    }

    public Block getBlock() {
        return block;
    }

    public boolean isNewBlock() {
        return newBlock;
    }

    public boolean isSolidBlock() {
        return !newBlock || timeSinceReceiving() > SOLID_BLOCK_DURATION_THRESHOLD;
    }

    public long getImportFailedAt() {
        return importFailedAt;
    }

    public void setImportFailedAt(long importFailedAt) {
        this.importFailedAt = importFailedAt;
    }

    public byte[] getHash() {
        return block.getHash();
    }

    public long getNumber() {
        if (number == 0) {
            number = block.getNumber();
        }

        return number;
    }

    public byte[] getEncoded() {
        return block.getEncoded();
    }

    public String getShortHash() {
        return block.getShortHash();
    }

    public byte[] getParentHash() {
        return block.getPreviousHeaderHash();
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public byte[] getNodeId() {
        return nodeId;
    }

    public void importFailed() {
        if (importFailedAt == 0) {
            importFailedAt = System.currentTimeMillis();
        }
    }

    public void resetImportFail() {
        importFailedAt = 0;
    }

    public long timeSinceFail() {
        if(importFailedAt == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - importFailedAt;
        }
    }

    public long timeSinceReceiving() {
        return System.currentTimeMillis() - receivedAt;
    }

    public byte[] getBytes() {
        byte[] blockBytes = block.getEncodedCacheData();

        /**
        byte[] importFailedBytes = RLP.encodeBigInteger(BigInteger.valueOf(importFailedAt));
        byte[] receivedAtBytes = RLP.encodeBigInteger(BigInteger.valueOf(receivedAt));
        byte[] newBlockBytes = RLP.encodeByte((byte) (newBlock ? 1 : 0));
        byte[] nodeIdBytes = RLP.encodeElement(nodeId);
        return RLP.encodeList(blockBytes, importFailedBytes,
                receivedAtBytes, newBlockBytes, nodeIdBytes);
        */
        byte[] numberBytes = RLP.encodeBigInteger(BigInteger.valueOf(this.number));
        return RLP.encodeList(blockBytes, numberBytes);
    }

    private void parse(byte[] bytes) {
        List<RLPElement> params = RLP.decode2(bytes);
        List<RLPElement> wrapper = (RLPList) params.get(0);

        byte[] blockBytes = wrapper.get(0).getRLPData();
        this.block = new Block(blockBytes, true);

        /**
        byte[] importFailedBytes = wrapper.get(1).getRLPData();
        byte[] receivedAtBytes = wrapper.get(2).getRLPData();
        byte[] newBlockBytes = wrapper.get(3).getRLPData();

        this.block = new Block(blockBytes, true);
        this.importFailedAt = importFailedBytes == null ? 0 : new BigInteger(1, importFailedBytes).longValue();
        this.receivedAt = receivedAtBytes == null ? 0 : new BigInteger(1, receivedAtBytes).longValue();
        byte newBlock = newBlockBytes == null ? 0 : new BigInteger(1, newBlockBytes).byteValue();
        this.newBlock = newBlock == 1;
        this.nodeId = wrapper.get(4).getRLPData();
        */
        byte[] numberBytes = wrapper.get(1).getRLPData();
        this.number = numberBytes == null ?
                0 : (new BigInteger(1, numberBytes)).longValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockWrapper wrapper = (BlockWrapper) o;

        return block.isEqual(wrapper.block);
    }

    @Override
    public int compareTo(BlockWrapper other) {
        return (int)(getNumber() - other.getNumber());
    }
}
