package io.taucoin.net.p2p;

import io.taucoin.net.message.ReasonCode;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import static io.taucoin.net.message.ReasonCode.REQUESTED;
import static io.taucoin.net.p2p.P2pMessageCodes.DISCONNECT;

/**
 * Wrapper around an Ethereum Disconnect message on the network
 *
 * @see io.taucoin.net.p2p.P2pMessageCodes#DISCONNECT
 */
public class DisconnectMessage extends P2pMessage {

    private ReasonCode reason;

    public DisconnectMessage(byte[] encoded) {
        super(encoded);
    }

    public DisconnectMessage(ReasonCode reason) {
        this.reason = reason;
        parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        byte[] reasonBytes = paramsList.get(0).getRLPData();
        if (reasonBytes == null)
            this.reason = REQUESTED;
        else
            this.reason = ReasonCode.fromInt(reasonBytes[0]);

        parsed = true;
    }

    private void encode() {
        byte[] encodedReason = RLP.encodeByte(this.reason.asByte());
        this.encoded = RLP.encodeList(encodedReason);
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public P2pMessageCodes getCommand() {
        return P2pMessageCodes.DISCONNECT;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    public ReasonCode getReason() {
        if (!parsed) parse();
        return reason;
    }

    public String toString() {
        if (!parsed) parse();
        return "[" + this.getCommand().name() + " reason=" + reason + "]";
    }
}