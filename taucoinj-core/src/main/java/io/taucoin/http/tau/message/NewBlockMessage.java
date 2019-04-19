package io.taucoin.http.tau.message;

import io.taucoin.core.Block;
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
import java.math.BigInteger;
import java.nio.charset.Charset;

public class NewBlockMessage extends Message {

    // The number of the new block.
    private long number;

    // Previous block hash.
    private byte[] previousBlockHash;

    // Total difficulity of the new block.
    private BigInteger totalDiff;

    private Block newBlock;

    public NewBlockMessage() {
    }

    public NewBlockMessage(long number, BigInteger totalDiff, Block newBlock) {
        this.number = number;
        this.previousBlockHash = newBlock.getPreviousHeaderHash();
        this.totalDiff = totalDiff;
        this.newBlock = newBlock;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public void setPreviousBlockHash(byte[] previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public BigInteger getTotalDiff() {
        return totalDiff;
    }

    public void setTotalDiff(BigInteger totalDiff) {
        this.totalDiff = totalDiff;
    }

    public Block getNewBlock() {
        return newBlock;
    }

    public void setNewBlock(Block newBlock) {
        this.newBlock = newBlock;
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
        payload.append("\nNewBlockMessage[\n");
        payload.append("\tnumber:" + number + ",\n");
        payload.append("\tpreviousBlockHash:" + Hex.toHexString(previousBlockHash) + ",\n");
        payload.append("\ttotalDiff:" + totalDiff.toString(16) + ",\n");
        payload.append("\tnewBlock:" + Hex.toHexString(newBlock.getHash()) + "\n");
        payload.append("]\n");
        return payload.toString();
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
                jsonGenerator.writeNumberField("number", message.getNumber());
                jsonGenerator.writeStringField("previoushash",
                        Hex.toHexString(message.getPreviousBlockHash()));
                jsonGenerator.writeStringField("totaldifficulty",
                        message.getTotalDiff().toString(16));
                jsonGenerator.writeStringField("block",
                        new String(Hex.toHexString(message.getNewBlock().getEncodedMsg())));
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
            NewBlockMessage message = new NewBlockMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode numberNode = node.get("number");
            message.setNumber(numberNode.asLong());
            JsonNode prevHashNode = node.get("previoushash");
            message.setPreviousBlockHash(Hex.decode(prevHashNode.asText()));
            JsonNode totalDiffNode = node.get("totaldifficulty");
            message.setTotalDiff(new BigInteger(totalDiffNode.asText()).abs());
            JsonNode blockNode = node.get("block");
            message.setNewBlock(
                    new Block(Hex.decode(blockNode.asText()), true));

            return message;
        }
    }
}
