package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigInteger;

public class ChainInfoMessage extends Message {

    // Genesis block hash
    private byte[] genesisHash;

    // Current block chain height
    private long height;

    // Current block hash
    private byte[] currentBlockHash;

    // Total difficulity
    private BigInteger totalDiff;

    public ChainInfoMessage() {
    }

    public ChainInfoMessage(byte[] genesisHash, long height, byte[] currentBlockHash,
            BigInteger totalDiff) {
        this.genesisHash = genesisHash;
        this.height = height;
        this.currentBlockHash = currentBlockHash;
        this.totalDiff = totalDiff;
    }

    public byte[] getGenesisHash() {
        return genesisHash;
    }

    public void setGenesisHash(byte[] genesisHash) {
        this.genesisHash = genesisHash;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public byte[] getCurrentBlockHash() {
        return currentBlockHash;
    }

    public void setCurrentBlockHash(byte[] currentBlockHash) {
        this.currentBlockHash = currentBlockHash;
    }

    public BigInteger getTotalDiff() {
        return totalDiff;
    }

    public void setTotalDiff(BigInteger totalDiff) {
        this.totalDiff = totalDiff;
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

    public static class Serializer extends StdSerializer<ChainInfoMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<ChainInfoMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                ChainInfoMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<ChainInfoMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public ChainInfoMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            return new ChainInfoMessage();
        }
    }
}
