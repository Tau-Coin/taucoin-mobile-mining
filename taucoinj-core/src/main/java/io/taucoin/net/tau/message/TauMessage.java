package io.taucoin.net.tau.message;

import io.taucoin.net.message.Message;

public abstract class TauMessage extends Message {

    public TauMessage() {
    }

    public TauMessage(byte[] encoded) {
        super(encoded);
    }

    abstract public TauMessageCodes getCommand();
}
