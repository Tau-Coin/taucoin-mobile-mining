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
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;

public class ChainInfoMessage extends Message {

    // Genesis block hash
    private byte[] genesisHash;

    // Current block chain height
    private long height;

    // Previous block hash
    private byte[] previousBlockHash;

    // Current block hash
    private byte[] currentBlockHash;

    // Total difficulity
    private BigInteger totalDiff;

    // Media fee of latest 144 blocks.
    long medianFee;

    public ChainInfoMessage() {
    }

    public ChainInfoMessage(byte[] genesisHash, long height, byte[] currentBlockHash,
            BigInteger totalDiff, byte[] previousBlockHash, long medianFee) {
        this.genesisHash = genesisHash;
        this.height = height;
        this.currentBlockHash = currentBlockHash;
        this.totalDiff = totalDiff;
        this.previousBlockHash = previousBlockHash;
        this.medianFee = medianFee;
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

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
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

    public long getMedianFee() {
        return medianFee;
    }

    public void setMedianFee(long medianFee) {
        this.medianFee = medianFee;
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
        payload.append("\nChainInfoMessage[\n");
        payload.append("\tgenesisHash:" + Hex.toHexString(genesisHash) + ",\n");
        payload.append("\theight:" + height + ",\n");
        payload.append("\tpreviousBlockHash:" +
                (previousBlockHash == null ? "null" : Hex.toHexString(previousBlockHash)) + ",\n");
        payload.append("\tcurrentBlockHash:" + Hex.toHexString(currentBlockHash) + ",\n");
        payload.append("\ttotalDiff:" + totalDiff.toString(16) + ",\n");
        payload.append("\tmedianFee:" + medianFee + "\n");
        payload.append("]\n");
        return payload.toString();
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
                jsonGenerator.writeStringField("genesishash",
                        Hex.toHexString(message.getGenesisHash()));
                jsonGenerator.writeNumberField("totalheight", message.getHeight());
                jsonGenerator.writeStringField("previoushash",
                        Hex.toHexString(message.getPreviousBlockHash()));
                jsonGenerator.writeStringField("currenthash",
                        Hex.toHexString(message.getCurrentBlockHash()));
                jsonGenerator.writeStringField("totaldifficulty",
                        message.getTotalDiff().toString(16));
                jsonGenerator.writeNumberField("medianfee", message.getMedianFee());
                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("taumessage serialize fatal error {}", e);
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
            ChainInfoMessage message = new ChainInfoMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode genesisHashNode = node.get("genesishash");
            message.setGenesisHash(Hex.decode(genesisHashNode.asText()));
            JsonNode heightNode = node.get("totalheight");
            message.setHeight(heightNode.asLong());
            JsonNode previousHashNode = node.get("previoushash");
            String previousHash = null;
            if (previousHashNode != null) {
                previousHash = previousHashNode.asText();
            }
            if (previousHash == null || previousHash.isEmpty()) {
                message.setPreviousBlockHash(null);
            } else {
                message.setPreviousBlockHash(Hex.decode(previousHash));
            }
            JsonNode blockHashNode = node.get("currenthash");
            message.setCurrentBlockHash(Hex.decode(blockHashNode.asText()));
            JsonNode totalDiffNode = node.get("totaldifficulty");
            String totalDiff = totalDiffNode.asText();
            message.setTotalDiff(new BigInteger(totalDiff, 16).abs());
            JsonNode medianFeeNode = node.get("medianfee");
            message.setMedianFee(medianFeeNode.asLong());

            return message;
        }
    }
}
