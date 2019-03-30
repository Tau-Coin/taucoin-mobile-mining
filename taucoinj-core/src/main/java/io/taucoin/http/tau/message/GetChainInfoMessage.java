package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class GetChainInfoMessage extends Message {

    public GetChainInfoMessage() {
    }

    @Override
    public Class<ChainInfoMessage> getAnswerMessage() {
        return ChainInfoMessage.class;
    }

    @Override
    public String toJsonString() {
        return null;
    }

    @Override
    public String toString() {
        return "GetChainInfoMessage[]";
    }

    public static class Serializer extends StdSerializer<GetChainInfoMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<GetChainInfoMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                GetChainInfoMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<GetChainInfoMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public GetChainInfoMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            return new GetChainInfoMessage();
        }
    }
}
