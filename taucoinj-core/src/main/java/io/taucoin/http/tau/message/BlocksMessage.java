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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class BlocksMessage extends Message {

    // Starting block number.
    private long startNumber;

    // get blocks in the reverse order.
    private boolean reverse;

    // blocks list
    private List<Block> blocks = new ArrayList<Block>();

    public BlocksMessage() {
    }

    public BlocksMessage(long startNumber, boolean reverse, List<Block> blocks) {
        this.startNumber = startNumber;
        this.reverse = reverse;
        this.blocks.addAll(blocks);
    }

    public long getStartNumber() {
        return startNumber;
    }

    public void setStartNumber(long startNumber) {
        this.startNumber = startNumber;
    }

    public boolean getReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        blocks.addAll(this.blocks);
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks.clear();
        this.blocks.addAll(blocks);
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
        payload.append("\nBlocksMessage[\n");
        payload.append("\tstartNumber:" + startNumber + ",\n");
        payload.append("\treverse:" + reverse + ",\n");

        StringBuilder blockHashes = new StringBuilder();
        blockHashes.append("[");
        for (Block block : this.blocks) {
            blockHashes.append(Hex.toHexString(block.getHash()) + ",");
        }
        blockHashes.append("]");

        payload.append("\thashes:" + blockHashes.toString() + "\n");
        payload.append("]\n");
        return payload.toString();
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
                jsonGenerator.writeNumberField("start", message.getStartNumber());
                jsonGenerator.writeBooleanField("reverse", message.getReverse());

                jsonGenerator.writeArrayFieldStart("blocks");
                for (Block block : message.getBlocks()) {
                    jsonGenerator.writeString(
                            new String(Base64.encode(block.getEncodedMsg()),
                                    Charset.forName("UTF-8")));
                }
                jsonGenerator.writeEndArray();

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
            BlocksMessage message = new BlocksMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode startNode = node.get("start");
            message.setStartNumber(startNode.asLong());
            JsonNode reverseNode = node.get("reverse");
            message.setReverse(reverseNode.asBoolean());

            JsonNode blocksNode = node.get("blocks");
            List<Block> blocksList = new ArrayList<Block>();
            if (blocksNode.isArray()) {
                Iterator<JsonNode> it = blocksNode.iterator();
                while (it.hasNext()) {
                    blocksList.add(new Block(
                            Base64.decode(it.next().asText()), true));
                }
            } else {
                logger.error("deserialize message erorr: not array json");
                return null;
            }
            message.setBlocks(blocksList);

            return message;
        }
    }
}
