package io.taucoin.core;

import io.taucoin.crypto.HashUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.Utils;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.List;

import static io.taucoin.config.Constants.DIFFICULTY_BOUND_DIVISOR;
import static io.taucoin.config.Constants.EXP_DIFFICULTY_PERIOD;
import static io.taucoin.config.Constants.MINIMUM_DIFFICULTY;
import static io.taucoin.crypto.HashUtil.EMPTY_TRIE_HASH;
import static io.taucoin.util.BIUtil.max;
import static io.taucoin.util.ByteUtil.toHexString;

/**
 * Block header is a value object containing
 * the basic information of a block
 */
public class BlockHeader {

    /* ripemd160 result length, with 20 bytes */
    public static final int BLOCK_HEADER_HASH_LENGTH = 20;

    /* 8 bits, keeping version is for upgrade to define the transition grace peroid */
    private byte version;
    /* 32 bits, unix time stamp related to block */
    private byte[] timeStamp;
    /* 160 bits, The SHA256 previous block header and RIPEMD 160 second 160 bits */
    private byte[] previousHeaderHash;
    /* The compressed public key in ECDSA 264 bits */
    private byte[] generatorPublicKey;

    /**
     * Memory cache only. Just for sync block headers.
     */
    private long blockNumber = -1;

    public BlockHeader(byte[] encoded) {
        this((RLPList) RLP.decode2(encoded).get(0));
    }

    public BlockHeader(RLPList rlpHeader) {

        this.version = rlpHeader.get(0).getRLPData()[0];
        this.timeStamp = rlpHeader.get(1).getRLPData();
        this.previousHeaderHash = rlpHeader.get(2).getRLPData();
        this.generatorPublicKey = rlpHeader.get(3).getRLPData();

    }

    public BlockHeader(byte version, byte[] timeStamp, byte[] previousHeaderHash,
                       byte[] generatorPublicKey) {
        this.version = version;
        this.timeStamp = timeStamp;
        this.previousHeaderHash = previousHeaderHash;
        this.generatorPublicKey = generatorPublicKey;
    }
    /*
    *TODO: details at other place
     */
    public boolean isGenesis() {
        return false;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getVersion(){ return version;}

    public void setPreviousHeaderHash(byte[] previousHeaderHash) {
        this.previousHeaderHash = previousHeaderHash;
    }

    public byte[] getPreviousHeaderHash() {
        return previousHeaderHash;
    }

    public void setTimeStamp(byte[] timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getTimeStamp() {
        return timeStamp;
    }

    public void setGeneratorPublicKey(byte[] generatorPublicKey) {
        this.generatorPublicKey = generatorPublicKey;
    }

    public byte[] getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public byte[] getEncoded() {

        byte[] version = RLP.encodeByte(this.version);
        byte[] timestamp = RLP.encodeElement(this.timeStamp);
        byte[] previousHeaderHash = RLP.encodeElement(this.previousHeaderHash);
        byte[] generatorPublicKey = RLP.encodeElement(this.generatorPublicKey);

        return RLP.encodeList(version, timestamp, previousHeaderHash,
                    generatorPublicKey);
    }

    public byte[] getHeaderHash() {
       return HashUtil.ripemd160(HashUtil.sha256(this.getEncoded()));
    }

    public byte[] getHash() {
       return getHeaderHash();
    }

    /*
    * TODO:
     */
    public byte[] calcPotValue() {

        return null;
    }
    /*
    *TODO:
     */
    public BigInteger calcDifficulty(BlockHeader parent) {
        BigInteger difficulty = max(MINIMUM_DIFFICULTY, BigInteger.valueOf(10000));

        return difficulty;
    }

    /**
     * Get memory cache number for block header sync.
     */
    public long getNumber(){
        if (this.blockNumber >= 0) {
            return this.blockNumber;
        } else {
            return 0;
        }
    }

    /**
     * Set memory cache number for block header sync.
     */
    public void setNumber(long number) {
        if (number < 0) {
            return;
        }
        this.blockNumber = number;
    }

    //temporary method will be discarded when smooth
    public byte[] getPotBoundary(){
        return null;
    }
    
    public String toString() {
        return toStringWithSuffix("\n");
    }

    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        //toStringBuff.append("  version=").append(toHexString(version)).append(suffix);
        toStringBuff.append("  timestamp=").append(toHexString(timeStamp)).append(suffix);
        toStringBuff.append("  previousHeaderHash=").append(toHexString(previousHeaderHash)).append(suffix);
        toStringBuff.append("  generatorPublicKey=").append(toHexString(generatorPublicKey)).append(suffix);
        return toStringBuff.toString();
    }

    public String toFlatString() {
        return toStringWithSuffix("");
    }

}
