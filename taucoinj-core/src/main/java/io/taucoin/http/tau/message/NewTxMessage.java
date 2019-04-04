package io.taucoin.http.tau.message;

import io.taucoin.core.Transaction;
import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.charset.Charset;

public class NewTxMessage extends Message {

    // New tx
    private Transaction newTx;

    public NewTxMessage() {
    }

    public NewTxMessage(Transaction newTx) {
        this.newTx = newTx;
    }

    public Transaction getNewTransaction() {
        return newTx;
    }

    public void setNewTransaction(Transaction newTx) {
        this.newTx = newTx;
    }

    @Override
    public Class<DummyMessage> getAnswerMessage() {
        return DummyMessage.class;
    }

    @Override
    public String toJsonString() {
        return MessageFactory.createJsonString(this);
    }

    @Override
    public String toString() {
        StringBuilder payload = new StringBuilder();
        payload.append("\nPoolTxsMessage[\n");
        payload.append("\tnewTx:" + Hex.toHexString(newTx.getHash()) + "\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<NewTxMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<NewTxMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                NewTxMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("transaction",
                        new String(Base64.encode(message.getNewTransaction().getEncoded()),
                                    Charset.forName("UTF-8")));
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<NewTxMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public NewTxMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            NewTxMessage message = new NewTxMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode txNode = node.get("transaction");
            message.setNewTransaction(new Transaction(Base64.decode(txNode.asText())));

            return message;
        }
    }
}
