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

public class NewTxStatusMessage extends Message {

    private String result;

    public NewTxStatusMessage() {
    }

    public NewTxStatusMessage(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public String toJsonString() {
        return MessageFactory.createJsonString(this);
    }

    @Override
    public String toString() {
        StringBuilder payload = new StringBuilder();
        payload.append("\nNewTxStatusMessage[\n");
        payload.append("\tresult:" + result + ",\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<NewTxStatusMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<NewTxStatusMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                NewTxStatusMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("result", message.getResult());
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<NewTxStatusMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public NewTxStatusMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            NewTxStatusMessage message = new NewTxStatusMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode resultNode = node.get("result");
            message.setResult(resultNode.asText());

            return message;
        }
    }
}
