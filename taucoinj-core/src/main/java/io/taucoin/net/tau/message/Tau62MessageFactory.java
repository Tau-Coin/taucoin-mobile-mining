package io.taucoin.net.tau.message;

import io.taucoin.net.message.Message;
import io.taucoin.net.message.MessageFactory;

import static io.taucoin.net.tau.TauVersion.V62;

/**
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class Tau62MessageFactory implements MessageFactory {

    @Override
    public Message create(byte code, byte[] encoded) {

        TauMessageCodes receivedCommand = TauMessageCodes.fromByte(code, V62);
        switch (receivedCommand) {
            case STATUS:
                return new StatusMessage(encoded);
            case NEW_BLOCK_HASHES:
                return new NewBlockHashes62Message(encoded);
            case TRANSACTIONS:
                return new TransactionsMessage(encoded);
            case GET_BLOCK_HEADERS:
                return new GetBlockHeadersMessage(encoded);
            case BLOCK_HEADERS:
                return new BlockHeadersMessage(encoded);
            case GET_BLOCK_BODIES:
                return new GetBlockBodiesMessage(encoded);
            case BLOCK_BODIES:
                return new BlockBodiesMessage(encoded);
            case NEW_BLOCK:
                return new NewBlockMessage(encoded);
            case NEW_BLOCK_HEADER:
                return new NewBlockHeaderMessage(encoded);
            default:
                throw new IllegalArgumentException("No such message");
        }
    }
}
