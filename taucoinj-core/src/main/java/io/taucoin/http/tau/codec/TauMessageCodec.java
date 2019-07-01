package io.taucoin.http.tau.codec;

import io.taucoin.config.SystemProperties;
import io.taucoin.http.message.Message;
import io.taucoin.http.tau.message.MessageFactory;
import io.taucoin.listener.TaucoinListener;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import javax.inject.Inject;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * The Netty codec which encodes/decodes Message to http request/response
 */
public class TauMessageCodec extends MessageToMessageCodec<HttpObject, Message> {

    private static final Logger logger = LoggerFactory.getLogger("http");

    private TaucoinListener tauListener;

    private ByteArrayOutputStream contentsStream = new ByteArrayOutputStream();

    private String host;

    @Inject
    public TauMessageCodec(TaucoinListener listener) {
        this.tauListener = listener;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        String output = String.format("From: \t%s \tRecv: \t%s", ctx.channel().remoteAddress(), msg);
        tauListener.trace(output);
        logger.debug("Receive http response {}", msg);

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            logger.debug("Http status {}", response.getStatus());
            if (response.getStatus() != HttpResponseStatus.OK) {
                throw new DecoderException("Incorrect repsonse status " + response.getStatus().toString());
            }
        } else if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            logger.debug("http content part {}", content.content().toString(CharsetUtil.UTF_8));

            ByteBuf contents = content.content();
            contents.getBytes(0, contentsStream, contents.readableBytes());

            if (content instanceof LastHttpContent) {
                Message message;
                try {
                    logger.debug("http response payload {}", contentsStream);
                    message = createMessage(contentsStream);
                } catch (Exception e) {
                    throw new DecoderException("decode exception", e);
                } finally {
                    contentsStream.reset();
                }
                out.add(message);
            }
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        String output = String.format("To: \t%s \tSend: \t%s", ctx.channel().remoteAddress(), msg);
        tauListener.trace(output);

        HttpMethod method = MessageEncodeResolver.resolveHttpMethod(msg.getClass());
        String path = MessageEncodeResolver.resolveHttpPath(msg.getClass());
        if (method == null || path == null) {
            throw new EncoderException("Can't find method/path for " + msg.getClass());
        }

        String jsonPayload = msg.toJsonString();
        logger.info("Send request {}", jsonPayload);
        tauListener.onSendHttpPayload(jsonPayload);
        HttpRequest request;
        if (jsonPayload != null) {
            byte[] bytes = jsonPayload.getBytes();
            ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(bytes.length);
            buf.writeBytes(bytes);
            request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, method, path, buf);
        } else {
            request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, method, path);
        }

        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, "UTF-8");
        request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        //request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
        if (jsonPayload != null) {
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(jsonPayload.length()));
        }

        out.add(request);
    }

    private Message createMessage(ByteArrayOutputStream contentsStream) {
        String jsonString = null;
        if (contentsStream.size() > 0) {
            jsonString = new String(contentsStream.toByteArray());
            tauListener.onRecvHttpPayload(jsonString);
        }
        Message message = MessageFactory.create(jsonString);
        if (message == null) {
            throw new IllegalArgumentException("No such message: " + jsonString);
        }

        return message;
    }
}
