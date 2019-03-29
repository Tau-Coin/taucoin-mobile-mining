package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class NewBlockMessage extends Message {

    public NewBlockMessage() {
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

    public static class Serializer extends StdSerializer<NewBlockMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<NewBlockMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                NewBlockMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<NewBlockMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public NewBlockMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            return new NewBlockMessage();
        }
    }
}
