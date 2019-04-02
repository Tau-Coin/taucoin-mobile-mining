package io.taucoin.http.tau.message;

import io.taucoin.http.message.Message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * TAU Http Message Factory.
 * It mainly contains two functions:
 *     (1) create Message from json string.
 *     (2) create json string from Message.
 */
public class MessageFactory {

    private static final Logger logger = LoggerFactory.getLogger("http");

    private static Map<String, Class<?> > Name2ClassMap
            = new HashMap<String, Class<?> >();

    static {
        Name2ClassMap.put("getchaininfo", GetChainInfoMessage.class);
        Name2ClassMap.put("chaininfo", ChainInfoMessage.class);
        Name2ClassMap.put("gethashes", GetHashesMessage.class);
        Name2ClassMap.put("hashes", HashesMessage.class);
        Name2ClassMap.put("getblocks", GetBlocksMessage.class);
        Name2ClassMap.put("blocks", BlocksMessage.class);
        Name2ClassMap.put("getpooltransactions", GetPoolTxsMessage.class);
        Name2ClassMap.put("pooltransactions", PoolTxsMessage.class);
        Name2ClassMap.put("newblock", NewBlockMessage.class);
        Name2ClassMap.put("newtransaction", NewTxMessage.class);
    }

    private static ObjectMapper sObjectMapper = new ObjectMapper();
    static {
        sObjectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    private static SimpleModule sModule = 
            new SimpleModule("TauMessageSerializerDeserializer", new Version(1, 0, 0, null, null, null));

    static {
        // Add serializers
        sModule.addSerializer(GetChainInfoMessage.class, new GetChainInfoMessage.Serializer());
        sModule.addSerializer(ChainInfoMessage.class, new ChainInfoMessage.Serializer());
        sModule.addSerializer(GetHashesMessage.class, new GetHashesMessage.Serializer());
        sModule.addSerializer(HashesMessage.class, new HashesMessage.Serializer());
        sModule.addSerializer(GetBlocksMessage.class, new GetBlocksMessage.Serializer());
        sModule.addSerializer(BlocksMessage.class, new BlocksMessage.Serializer());
        sModule.addSerializer(GetPoolTxsMessage.class, new GetPoolTxsMessage.Serializer());
        sModule.addSerializer(PoolTxsMessage.class, new PoolTxsMessage.Serializer());
        sModule.addSerializer(NewBlockMessage.class, new NewBlockMessage.Serializer());
        sModule.addSerializer(NewTxMessage.class, new NewTxMessage.Serializer());
        sModule.addSerializer(MessageResponse.class, new MessageResponse.Serializer());

        // Add deserializers
        sModule.addDeserializer(GetChainInfoMessage.class, new GetChainInfoMessage.Deserializer());
        sModule.addDeserializer(ChainInfoMessage.class, new ChainInfoMessage.Deserializer());
        sModule.addDeserializer(GetHashesMessage.class, new GetHashesMessage.Deserializer());
        sModule.addDeserializer(HashesMessage.class, new HashesMessage.Deserializer());
        sModule.addDeserializer(GetBlocksMessage.class, new GetBlocksMessage.Deserializer());
        sModule.addDeserializer(BlocksMessage.class, new BlocksMessage.Deserializer());
        sModule.addDeserializer(GetPoolTxsMessage.class, new GetPoolTxsMessage.Deserializer());
        sModule.addDeserializer(PoolTxsMessage.class, new PoolTxsMessage.Deserializer());
        sModule.addDeserializer(NewBlockMessage.class, new NewBlockMessage.Deserializer());
        sModule.addDeserializer(NewTxMessage.class, new NewTxMessage.Deserializer());
        sModule.addDeserializer(MessageResponse.class, new MessageResponse.Deserializer());

        sObjectMapper.registerModule(sModule);
    }

    public static Message create(String response) {
        MessageResponse messageResp = null;
        try {
            messageResp = sObjectMapper.readValue(response, MessageResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("deserialize response erorr {}", e);
            return null;
        }

        String message = messageResp.getMessage();
        String payload = messageResp.getPayload();
        if (message == null || message.isEmpty() || payload == null
                || payload.isEmpty() || Name2ClassMap.get(message) == null) {
            return null;
        }

        return MessageFactory.create(message, payload);
    }

    public static Message create(String messageName, String payload) {
        if (messageName == null || messageName.isEmpty() || payload == null
                || payload.isEmpty() || Name2ClassMap.get(messageName) == null) {
            return null;
        }

        Message message = null;
        try {
            message = (Message)sObjectMapper.readValue(payload, Name2ClassMap.get(messageName));
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("deserialize response erorr {}", e);
            return null;
        }
        return message;
    }

    public static String createJsonString(Message message) {
        if (message == null) {
            return null;
        }

        String jsonString = null;
        try {
            jsonString = sObjectMapper.writeValueAsString(message);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("serialize response erorr {}", e);
            return null;
        }
        return jsonString;
    }

    private static class MessageResponse {

        private String message;
        private String payload;

        public MessageResponse() {
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public static class Serializer extends StdSerializer<MessageResponse> {

            public Serializer() {
                this(null);
            }

            public Serializer(Class<MessageResponse> c) {
                super(c);
            }

            @Override
            public void serialize(
                    MessageResponse response, JsonGenerator jsonGenerator, SerializerProvider serializer) {
                try {
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField("message", response.getMessage());
                    jsonGenerator.writeStringField("payload", response.getPayload());
                    jsonGenerator.writeEndObject();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Tau serializing message exception");
                }
            }
        }

        public static class Deserializer extends StdDeserializer<MessageResponse> {

            public Deserializer() {
                this(null);
            }

            public Deserializer(Class<?> c) {
                super(c);
            }

            @Override
            public MessageResponse deserialize(JsonParser parser, DeserializationContext deserializer) {
                MessageResponse response = new MessageResponse();

                ObjectCodec codec = parser.getCodec();
                JsonNode node = null;
                try {
                    node = codec.readTree(parser);
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("deserialize response erorr {}", e);
                    return null;
                }

                JsonNode messageNode = node.get("message");
                String message = messageNode.asText();
                JsonNode payloadNode = node.get("payload");
                String payload = payloadNode.asText();
                response.setMessage(message);
                response.setPayload(payload);

                return response;
            }
        }
    }
}
