package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class GetHashesMessage extends Message {

    public GetHashesMessage() {
    }

    @Override
    public Class<HashesMessage> getAnswerMessage() {
        return HashesMessage.class;
    }

    @Override
    public String toJsonString() {
        return null;
    }

    @Override
    public String toString() {
        return "GetChainInfoMessage()";
    }

    public static class Serializer extends StdSerializer<GetHashesMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<GetHashesMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                GetHashesMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<GetHashesMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public GetHashesMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            return new GetHashesMessage();
        }
    }
}
