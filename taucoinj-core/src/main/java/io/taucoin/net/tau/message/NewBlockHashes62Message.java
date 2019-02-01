package io.taucoin.net.tau.message;

import io.taucoin.core.BlockIdentifier;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around an Ethereum NewBlockHashes message on the network<br>
 * Represents new message structure introduced in Tau V62
 *
 * @see TauMessageCodes#NEW_BLOCK_HASHES
 *
 * @author Mikhail Kalinin
 * @since 05.09.2015
 */
public class NewBlockHashes62Message extends TauMessage {

    /**
     * List of identifiers holding hash and number of the blocks
     */
    private List<BlockIdentifier> blockIdentifiers;

    public NewBlockHashes62Message(byte[] payload) {
        super(payload);
    }

    public NewBlockHashes62Message(List<BlockIdentifier> blockIdentifiers) {
        this.blockIdentifiers = blockIdentifiers;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        blockIdentifiers = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
            blockIdentifiers.add(new BlockIdentifier(rlpData));
        }
        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (BlockIdentifier identifier : blockIdentifiers)
            encodedElements.add(identifier.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        this.encoded = RLP.encodeList(encodedElementArray);
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

    public List<BlockIdentifier> getBlockIdentifiers() {
        return blockIdentifiers;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.NEW_BLOCK_HASHES;
    }

    @Override
    public String toString() {
        if (!parsed) parse();

        return "[" + this.getCommand().name() + "] (" + blockIdentifiers.size() + ")";
    }

}
