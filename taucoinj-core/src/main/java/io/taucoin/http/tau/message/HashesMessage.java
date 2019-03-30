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
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class HashesMessage extends Message {

    // Starting block number.
    private long startNumber;

    // get block hashes in the reverse order.
    private boolean reverse;

    // hashes list
    private List<byte[]> hashes = new ArrayList<byte[]>();

    public HashesMessage() {
    }

    public HashesMessage(long startNumber, boolean reverse, List<byte[]> hashes) {
        this.startNumber = startNumber;
        this.reverse = reverse;
        this.hashes.addAll(hashes);
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

    public List<byte[]> getHashes() {
        List<byte[]> hashes = new ArrayList<byte[]>();
        hashes.addAll(this.hashes);
        return hashes;
    }

    public void setHashes(List<byte[]> hashes) {
        this.hashes.clear();
        this.hashes.addAll(hashes);
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
        payload.append("\nHashesMessage[\n");
        payload.append("\tstartNumber:" + startNumber + ",\n");
        payload.append("\treverse:" + reverse + ",\n");

        StringBuilder hashes = new StringBuilder();
        hashes.append("[");
        for (byte[] hash : this.hashes) {
            hashes.append(Hex.toHexString(hash) + ",");
        }
        hashes.append("]");

        payload.append("\thashes:" + hashes.toString() + "\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<HashesMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<HashesMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                HashesMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("start", message.getStartNumber());
                jsonGenerator.writeBooleanField("reverse", message.getReverse());

                jsonGenerator.writeArrayFieldStart("hashes");
                for (byte[] hash : message.getHashes()) {
                    jsonGenerator.writeString(Hex.toHexString(hash));
                }
                jsonGenerator.writeEndArray();

                jsonGenerator.writeEndObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<HashesMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public HashesMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            HashesMessage message = new HashesMessage();

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

            JsonNode hashesNode = node.get("hashes");
            List<byte[]> hashesList = new ArrayList<byte[]>();
            if (hashesNode.isArray()) {
                Iterator<JsonNode> it = hashesNode.iterator();
                while (it.hasNext()) {
                    hashesList.add(Hex.decode(it.next().asText()));
                }
            } else {
                logger.error("deserialize message erorr: not array json");
                return null;
            }
            message.setHashes(hashesList);

            return message;
        }
    }
}
