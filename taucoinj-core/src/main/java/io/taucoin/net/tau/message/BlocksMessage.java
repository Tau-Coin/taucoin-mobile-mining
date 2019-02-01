package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Wrapper around an Ethereum Blocks message on the network
 *
 * @see TauMessageCodes#BLOCKS
 */
public class BlocksMessage extends TauMessage {

    private List<Block> blocks;

    public BlocksMessage(byte[] encoded) {
        super(encoded);
    }

    public BlocksMessage(List<Block> blocks) {
        this.blocks = blocks;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blocks = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
            Block blockData = new Block(rlpData.getRLPData());
            blocks.add(blockData);
        }
        parsed = true;
    }

    private void encode() {

        List<byte[]> encodedElements = new Vector<>();

        for (Block block : blocks)
            encodedElements.add(block.getEncoded());

        byte[][] encodedElementArray = encodedElements
                .toArray(new byte[encodedElements.size()][]);

        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public List<Block> getBlocks() {
        if (!parsed) parse();
        return blocks;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.BLOCKS;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) parse();


        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blocks.size()).append(" )");

        if (logger.isDebugEnabled()) {
            payload.append(" ");
            for (Block block : blocks) {
                payload.append(Hex.toHexString(block.getEncoded())).append(" | ");
            }
            if (!blocks.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}