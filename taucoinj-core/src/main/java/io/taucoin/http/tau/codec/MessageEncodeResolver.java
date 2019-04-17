package io.taucoin.http.tau.codec;

import io.taucoin.http.tau.message.*;

import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

public class MessageEncodeResolver {

    private static final Logger logger = LoggerFactory.getLogger("http");

    private static Map<Class<?>, HttpMethod> Message2Method
            = new HashMap<Class<?>, HttpMethod>();
    static {
        Message2Method.put(GetChainInfoMessage.class, HttpMethod.POST);
        Message2Method.put(GetHashesMessage.class, HttpMethod.POST);
        Message2Method.put(GetBlocksMessage.class, HttpMethod.POST);
        Message2Method.put(GetPoolTxsMessage.class, HttpMethod.POST);
        Message2Method.put(NewBlockMessage.class, HttpMethod.POST);
        Message2Method.put(NewTxMessage.class, HttpMethod.POST);
    }

    private static Map<Class<?>, String> Message2Path
            = new HashMap<Class<?>, String>();

    static {
        Message2Path.put(GetChainInfoMessage.class, "/getchaininfo");
        Message2Path.put(GetHashesMessage.class, "/gethashes");
        Message2Path.put(GetBlocksMessage.class, "/getblocks");
        Message2Path.put(GetPoolTxsMessage.class, "/getpooltransactions");
        Message2Path.put(NewBlockMessage.class, "/newblock");
        Message2Path.put(NewTxMessage.class, "/newtransaction");
    }

    public static HttpMethod resolveHttpMethod(Class<?> type) {
        return Message2Method.get(type);
    }

    public static String resolveHttpPath(Class<?> type) {
        return Message2Path.get(type);
    }
}
