package io.taucoin.http.client;

import io.taucoin.config.SystemProperties;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.http.discovery.PeersManager;
import io.taucoin.http.message.Message;
import io.taucoin.net.rlpx.Node;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Provider;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * This class creates the connection to an remote address using the Netty framework
 *
 * @see <a href="http://netty.io">http://netty.io</a>
 */
public class HttpClient {

    private static final Logger logger = LoggerFactory.getLogger("http");

    PeersManager peersManager;

    TaucoinListener listener;

    Provider<HttpClientInitializer> provider;

    private AtomicBoolean isIdle = new AtomicBoolean(true);

    @Inject
    public HttpClient(TaucoinListener listener, Provider<HttpClientInitializer> provider,
            PeersManager peersManager) {
        this.listener = listener;
        this.provider = provider;
        this.peersManager = peersManager;
    }

    private static EventLoopGroup workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "HttpClientWorker-" + cnt.getAndIncrement());
        }
    });

    public boolean isIdle() {
        return this.isIdle.get();
    }

    public boolean compareAndSetIdle(boolean expect, boolean update) {
        return this.isIdle.compareAndSet(expect, update);
    }

    public boolean sendRequest(Message message) {
        listener.trace("Send request: " + message.toString());

        HttpClientInitializer httpInitializer = provider.get();
        Node peer = peersManager.getRandomPeer();

        logger.debug("Send request {} to {}:{}", message, peer.getHost(), peer.getPort());

        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
         .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONFIG.httpConnectionTimeout())
         .channel(NioSocketChannel.class)
         .handler(httpInitializer);

        // Make the connection attempt.
        try {
            Channel ch = b.connect(peer.getHost(), peer.getPort()).sync().channel();
            // Send the message.
            ch.writeAndFlush(message);
            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("Sending message exception {}", e);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Sending message exception {}", e);
            return false;
        } finally {
            this.isIdle.set(true);
        }

        return true;
    }
}
