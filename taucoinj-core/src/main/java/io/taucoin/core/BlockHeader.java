package io.taucoin.core;

import org.ethereum.crypto.HashUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.util.Utils;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.BigIntegers;

import java.math.BigInteger;
import java.util.List;

import static org.ethereum.config.Constants.DIFFICULTY_BOUND_DIVISOR;
import static org.ethereum.config.Constants.DURATION_LIMIT;
import static org.ethereum.config.Constants.EXP_DIFFICULTY_PERIOD;
import static org.ethereum.config.Constants.MINIMUM_DIFFICULTY;
import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.ethereum.util.BIUtil.max;
import static org.ethereum.util.ByteUtil.toHexString;

/**
 * Block header is a value object containing
 * the basic information of a block
 */
public class BlockHeader {

    /* version of block 8 bits */
    private byte version;
    /* unix time stamp related to block */
    private byte[] timeStamp;
    /* The SHA256 previous block header and RIPEMD 160 second 160 bits */
    private byte[] previousHeaderHash;
    /* The compressed public key in ECDSA 264 bits */
    private byte[] generatorPublicKey;

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

    public byte getVersion(){ return version;}
    public byte[] getPreviousHeaderHash() {
        return previousHeaderHash;
    }

    public byte[] getTimeStamp() {
        return timeStamp;
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

    public byte[] getHeaderHash(){
       return HashUtil.ripemd160(HashUtil.sha256(this.getEncoded()));
    }

    public byte[] getUnclesEncoded(List<BlockHeader> uncleList) {

        byte[][] unclesEncoded = new byte[uncleList.size()][];
        int i = 0;
        for (BlockHeader uncle : uncleList) {
            unclesEncoded[i] = uncle.getEncoded();
            ++i;
        }
        return RLP.encodeList(unclesEncoded);
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
