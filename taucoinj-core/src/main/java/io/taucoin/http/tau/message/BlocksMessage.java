package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class BlocksMessage extends Message {

    public BlocksMessage() {
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
        return "GetChainInfoMessage()";
    }

    public static class Serializer extends StdSerializer<BlocksMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<BlocksMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                BlocksMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<BlocksMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public BlocksMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            return new BlocksMessage();
        }
    }
}
