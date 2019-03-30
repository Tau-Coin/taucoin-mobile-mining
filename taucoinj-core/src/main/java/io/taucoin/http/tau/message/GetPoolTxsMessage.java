package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class GetPoolTxsMessage extends Message {

    // Max amount of txs.
    private long max;

    // Min tx fee.
    private long minFee;

    public GetPoolTxsMessage() {
    }

    public GetPoolTxsMessage(long max, long minFee) {
        this.max = max;
        this.minFee = minFee;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMinFee() {
        return minFee;
    }

    public void setMinFee(long minFee) {
        this.minFee = minFee;
    }

    @Override
    public Class<PoolTxsMessage> getAnswerMessage() {
        return PoolTxsMessage.class;
    }

    @Override
    public String toJsonString() {
        return MessageFactory.createJsonString(this);
    }

    @Override
    public String toString() {
        StringBuilder payload = new StringBuilder();
        payload.append("\nGetPoolTxsMessage[\n");
        payload.append("\tmax:" + max + ",\n");
        payload.append("\tminfee:" + minFee + "\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<GetPoolTxsMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<GetPoolTxsMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                GetPoolTxsMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("max", message.getMax());
                jsonGenerator.writeNumberField("minfee", message.getMinFee());
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<GetPoolTxsMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public GetPoolTxsMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            GetPoolTxsMessage message = new GetPoolTxsMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode maxNode = node.get("max");
            message.setMax(maxNode.asLong());
            JsonNode minFeeNode = node.get("minfee");
            message.setMinFee(minFeeNode.asLong());

            return message;
        }
    }
}
