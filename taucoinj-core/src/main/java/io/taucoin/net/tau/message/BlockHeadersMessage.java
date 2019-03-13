package io.taucoin.net.tau.message;

import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;
import io.taucoin.util.Utils;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.taucoin.util.ByteUtil.byteArrayToLong;

/**
 * Wrapper around an Ethereum BlockHeaders message on the network
 *
 * @see TauMessageCodes#BLOCK_HEADERS
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockHeadersMessage extends TauMessage {

    /**
     * List of block headers from the peer
     */
    private List<BlockHeader> blockHeaders;

    /**
     * Number of first block header.
     * This field is ignored if blockHeaders are empty.
     */
    private long startNumber;

    /**
     * Number of last block header.
     * This field is ignored if blockHeaders are empty.
     */
    private long lastNumber;

    public BlockHeadersMessage(byte[] encoded) {
        super(encoded);
    }

    public BlockHeadersMessage(List<BlockHeader> headers) {
        this.blockHeaders = headers;
        if (headers != null && headers.size() > 0) {
            this.startNumber = headers.get(0).getNumber();
            this.lastNumber = headers.get(headers.size() - 1).getNumber();
        } else {
            this.startNumber = -1;
            this.lastNumber = -1;
        }
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        // parse block headers
        RLPList encodeHeaders = (RLPList)paramsList.get(0);
        blockHeaders = new ArrayList<>();
        for (int i = 0; i < encodeHeaders.size(); ++i) {
            RLPList rlpData = ((RLPList) encodeHeaders.get(i));
            blockHeaders.add(new BlockHeader(rlpData));
        }

        // parse start number and last number
        if (blockHeaders.size() > 0) {
            byte[] startNumberBytes = paramsList.get(1).getRLPData();
            byte[] lastNumberBytes = paramsList.get(2).getRLPData();
            this.startNumber = byteArrayToLong(startNumberBytes);
            this.lastNumber = byteArrayToLong(lastNumberBytes);
        } else {
            this.startNumber = -1;
            this.lastNumber = -1;
        }

        parsed = true;
    }

    private void encode() {
        List<byte[]> encodedHeaders = new ArrayList<>();

        // encode headers
        for (BlockHeader blockHeader : blockHeaders) {
            encodedHeaders.add(blockHeader.getEncoded());
        }
        byte[][] encodedHeadersArray = encodedHeaders.toArray(new byte[encodedHeaders.size()][]);
        byte[] encodedHeadersBytes = RLP.encodeList(encodedHeadersArray);

        // encode start number and last number
        if (encodedHeaders.size() == 0) {
            this.encoded = RLP.encodeList(encodedHeadersBytes);
        } else {
            byte[] startNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(this.startNumber));
            byte[] lastNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(this.lastNumber));
            this.encoded = RLP.encodeList(encodedHeadersBytes, startNumberBytes, lastNumberBytes);
        }
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

    public List<BlockHeader> getBlockHeaders() {
        if (!parsed) parse();
        return blockHeaders;
    }

    public long getStartNumber() {
        if (!parsed) parse();
        return this.startNumber;
    }

    public long getLastNumber() {
        if (!parsed) parse();
        return this.lastNumber;
    }

    @Override
    public TauMessageCodes getCommand() {
        return TauMessageCodes.BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        if (!parsed) parse();

        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(blockHeaders.size()).append(" )");
        payload.append(" start( ").append(startNumber).append(" )");
        payload.append(" last( ").append(lastNumber).append(" )");

        if (logger.isDebugEnabled()) {
            payload.append(" ");
            for (BlockHeader header : blockHeaders) {
                payload.append(Hex.toHexString(header.getHash()).substring(0, 6)).append(" | ");
            }
            if (!blockHeaders.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
