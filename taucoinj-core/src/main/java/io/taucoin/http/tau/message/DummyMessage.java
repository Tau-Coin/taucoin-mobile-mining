package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

public class DummyMessage extends Message {

    public DummyMessage() {
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public String toJsonString() {
        return null;
    }

    @Override
    public String toString() {
        return "DummyMessage[]";
    }
}
