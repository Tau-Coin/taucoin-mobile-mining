package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;
import io.taucoin.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an TAU NewBlockHeader message on the network
 *
 * @see TauMessageCodes#NEW_BLOCK_HEADER
 *
 * @author Taucoin Core Developers
 * @since 01.28.2019
 */
public class NewBlockHeaderMessage extends TauMessage {

    /**
     * New block header from the peer
     */
    private BlockHeader blockHeader;

    public NewBlockHeaderMessage(byte[] encoded) {
        super(encoded);
    }

    public NewBlockHeaderMessage(BlockHeader header) {
        this.blockHeader = header;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockHeader = new BlockHeader(paramsList);
        parsed = true;
    }

    private void encode() {
        this.encoded = blockHeader.getEncoded();
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public BlockHeader getBlockHeader() {
        if (!parsed) parse();
        return blockHeader;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.NEW_BLOCK_HEADER;
    }

    @Override
    public String toString() {
        if (!parsed) parse();
        return "[" + getCommand().name() + " " + blockHeader.toString() + "]";
    }
}
