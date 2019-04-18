package io.taucoin.http.tau.message;

import io.taucoin.core.Transaction;
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

public class PoolTxsMessage extends Message {

    // Min tx fee.
    private long minFee;

    // List of Pool txs.
    private List<Transaction> listTxs = new ArrayList<Transaction>();

    public PoolTxsMessage() {
    }

    public PoolTxsMessage(long minFee, List<Transaction> listTxs) {
        this.minFee = minFee;
        this.listTxs.addAll(listTxs);
    }

    public long getMinFee() {
        return minFee;
    }

    public void setMinFee(long minFee) {
        this.minFee = minFee;
    }

    public List<Transaction> getTransactions() {
        List<Transaction> txs = new ArrayList<Transaction>();
        txs.addAll(this.listTxs);
        return txs;
    }

    public void setTransaction(List<Transaction> txs) {
        this.listTxs.clear();
        this.listTxs.addAll(txs);
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
        payload.append("\nPoolTxsMessage[\n");
        payload.append("\tminFee:" + minFee + ",\n");

        StringBuilder txsHashes = new StringBuilder();
        txsHashes.append("[");
        for (Transaction tx : this.listTxs) {
            txsHashes.append(Hex.toHexString(tx.getHash()) + ",");
        }
        txsHashes.append("]");

        payload.append("\thashes:" + txsHashes.toString() + "\n");
        payload.append("]\n");
        return payload.toString();
    }

    public static class Serializer extends StdSerializer<PoolTxsMessage> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<PoolTxsMessage> c) {
            super(c);
        }

        @Override
        public void serialize(
                PoolTxsMessage message, JsonGenerator jsonGenerator, SerializerProvider serializer) {
            try {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("minfee", message.getMinFee());

                jsonGenerator.writeArrayFieldStart("txs");
                for (Transaction tx : message.getTransactions()) {
                    jsonGenerator.writeString(
                            new String(Hex.toHexString(tx.getEncoded())));
                }
                jsonGenerator.writeEndArray();

                jsonGenerator.writeEndObject();

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Tau serializing message exception");
            }
        }
    }

    public static class Deserializer extends StdDeserializer<PoolTxsMessage> {

        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> c) {
            super(c);
        }

        @Override
        public PoolTxsMessage deserialize(JsonParser parser, DeserializationContext deserializer) {
            PoolTxsMessage message = new PoolTxsMessage();

            ObjectCodec codec = parser.getCodec();
            JsonNode node = null;
            try {
                node = codec.readTree(parser);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("deserialize message erorr {}", e);
                return null;
            }

            JsonNode minfeeNode = node.get("minfee");
            message.setMinFee(minfeeNode.asLong());

            JsonNode txsNode = node.get("txs");
            List<Transaction> txsList = new ArrayList<Transaction>();
            if (txsNode.isArray()) {
                Iterator<JsonNode> it = txsNode.iterator();
                while (it.hasNext()) {
                    txsList.add(new Transaction(
                            Hex.decode(it.next().asText())));
                }
            } else {
                logger.error("deserialize message erorr: not array json");
                return null;
            }
            message.setTransaction(txsList);

            return message;
        }
    }
}
