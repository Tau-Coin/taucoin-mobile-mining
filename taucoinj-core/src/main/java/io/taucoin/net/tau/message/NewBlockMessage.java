package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

/**
 * Wrapper around an Ethereum Blocks message on the network
 *
 * @see TauMessageCodes#NEW_BLOCK
 */
public class NewBlockMessage extends TauMessage {

    private Block block;
    private byte[] difficulty;

    public NewBlockMessage(byte[] encoded) {
        super(encoded);
    }

    public NewBlockMessage(Block block, byte[] difficulty) {
        this.block = block;
        this.difficulty = difficulty;
        encode();
    }

    private void encode() {
        byte[] block = this.block.getEncodedMsg();
        byte[] diff = RLP.encodeElement(this.difficulty);

        this.encoded = RLP.encodeList(block, diff);
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        RLPList blockRLP = ((RLPList) paramsList.get(0));
        block = new Block(blockRLP.getRLPData(), true);
        difficulty = paramsList.get(1).getRLPData();

        parsed = true;
    }

    public Block getBlock() {
        if (!parsed) parse();
        return block;
    }

    public byte[] getCumulativeDifficulty() {
        if (!parsed) parse();
        return difficulty;
    }

    public BigInteger getDifficultyAsBigInt() {
        return new BigInteger(1, difficulty);
    }

    @Override
    public byte[] getEncoded() {
        return encoded;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.NEW_BLOCK;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) parse();

//        String hash = this.getBlock().getShortHash();
        long number = this.getBlock().getNumber();
        return "NEW_BLOCK [ number: " + number + " hash:" + /*hash +*/ " difficulty: " + Hex.toHexString(difficulty) + " ]";
    }
}