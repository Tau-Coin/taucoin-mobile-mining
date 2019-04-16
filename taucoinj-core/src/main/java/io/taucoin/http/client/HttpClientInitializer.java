package io.taucoin.http.client;

import io.taucoin.http.RequestManager;
import io.taucoin.http.RequestQueue;
import io.taucoin.http.tau.codec.TauMessageCodec;
import io.taucoin.http.tau.handler.TauHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger("http");

    private Provider<TauMessageCodec> messageCodecProvider;
    private Provider<TauHandler> handlerProvider;
    private HttpClient httpClient;
    private RequestQueue requestQueue;
    private String host;

    @Inject
    public HttpClientInitializer(Provider<TauMessageCodec> messageCodecProvider,
            Provider<TauHandler> handlerProvider) {
        this.messageCodecProvider = messageCodecProvider;
        this.handlerProvider = handlerProvider;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public void initChannel(SocketChannel ch) {

        ChannelPipeline p = ch.pipeline();

        p.addLast(new HttpClientCodec());

        // Remove the following line if you don't want automatic content decompression.
        //p.addLast(new HttpContentDecompressor());

        TauMessageCodec codec = this.messageCodecProvider.get();
        codec.setHost(host);
        p.addLast(codec);
        TauHandler handler = handlerProvider.get();
        handler.setHttpClient(httpClient);
        handler.setRequestQueue(requestQueue);
        p.addLast(handler);
    }
}
