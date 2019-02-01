package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum BlockBodies message on the network
 *
 * @see TauMessageCodes#BLOCK_BODIES
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockBodiesMessage extends TauMessage {

    private List<byte[]> blockBodies;

    public BlockBodiesMessage(byte[] encoded) {
        super(encoded);
    }

    public BlockBodiesMessage(List<byte[]> blockBodies) {
        this.blockBodies = blockBodies;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockBodies = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
            blockBodies.add(rlpData.getRLPData());
        }
        parsed = true;
    }

    private void encode() {

        byte[][] encodedElementArray = blockBodies
                .toArray(new byte[blockBodies.size()][]);

        this.encoded = RLP.encodeList(encodedElementArray);
    }


    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    public List<byte[]> getBlockBodies() {
        if (!parsed) parse();
        return blockBodies;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.BLOCK_BODIES;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public String toString() {
        if (!parsed) parse();

        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blockBodies.size()).append(" )");

        if (logger.isDebugEnabled()) {
            payload.append(" ");
            for (byte[] body : blockBodies) {
                payload.append(Hex.toHexString(body)).append(" | ");
            }
            if (!blockBodies.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
