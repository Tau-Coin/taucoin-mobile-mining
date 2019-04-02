package io.taucoin.http.client;

import io.taucoin.http.RequestManager;
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

    private RequestManager requestManager;
    private Provider<TauMessageCodec> messageCodecProvider;
    private Provider<TauHandler> handlerProvider;

    @Inject
    public HttpClientInitializer(RequestManager requestManager,
            Provider<TauMessageCodec> messageCodecProvider,
            Provider<TauHandler> handlerProvider) {
        this.requestManager = requestManager;
        this.messageCodecProvider = messageCodecProvider;
        this.handlerProvider = handlerProvider;
    }

    @Override
    public void initChannel(SocketChannel ch) {

        ChannelPipeline p = ch.pipeline();

        p.addLast(new HttpClientCodec());

        // Remove the following line if you don't want automatic content decompression.
        //p.addLast(new HttpContentDecompressor());

        p.addLast(this.messageCodecProvider.get());
        p.addLast(this.handlerProvider.get());
        p.addLast(this.requestManager);
    }
}
