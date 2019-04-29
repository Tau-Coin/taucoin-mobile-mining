package io.taucoin.http.client;

import io.taucoin.config.SystemProperties;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.http.discovery.PeersManager;
import io.taucoin.http.message.Message;
import io.taucoin.http.RequestQueue;
import io.taucoin.net.rlpx.Node;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;
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

    public static final int RECONNECT_DELAY = 2;

    private PeersManager peersManager;

    private TaucoinListener listener;

    private Provider<HttpClientInitializer> provider;

    private RequestQueue requestQueue;

    private Node peer = null;
    private HttpClientInitializer httpInitializer = null;
    private Channel channel = null;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);

    private static EventLoopGroup workerGroup = new NioEventLoopGroup(0, new ThreadFactory() {
        AtomicInteger cnt = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "HttpClientWorker-" + cnt.getAndIncrement());
        }
    });

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "ConnectTimer-" + cnt.getAndIncrement());
        }
    });
    private ScheduledFuture<?> timerTask;

    @Inject
    public HttpClient(TaucoinListener listener, Provider<HttpClientInitializer> provider,
            PeersManager peersManager) {
        this.listener = listener;
        this.provider = provider;
        this.peersManager = peersManager;
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void sendRequest(Message message) {
        requestQueue.sendMessage(message);
        tryConnect();
    }

    public void setIsConnecting(boolean IsConnecting) {
        isConnecting.set(IsConnecting);
    }

    public void tryConnect() {
        if (isConnected.get() || isConnecting.get()) {
            logger.info("Peer {} is still alive", channel);
            return;
        }

        isConnecting.set(true);
        // schedule connect task
        scheduleConnect(RECONNECT_DELAY * 1000);
    }

    public void activate(ChannelHandlerContext ctx) {
        isConnected.set(true);
        isConnecting.set(false);
        requestQueue.activate(ctx);
        channel = ctx.channel();
        logger.info("Peer {} connected", channel);
    }

    public void deactivate(ChannelHandlerContext ctx) {
        isConnected.set(false);
        isConnecting.set(false);
        requestQueue.deactivate();
        logger.info("Peer {} disconnected", ctx.channel());
        channel = null;

        if (requestQueue.size() > 0) {
            tryConnect();
        }
    }

    private void doConnect() {
        try {
            connect(configureBootstrap(new Bootstrap()));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Connect exception {} ", e);
            if (requestQueue.size() >= RequestQueue.CAPABILITY) {
                requestQueue.clear();
            }
        }
    }

    private void scheduleConnect(long millis) {
        timerTask = timer.schedule(new Runnable() {
            public void run() {
                try {
                    doConnect();
                } catch (Throwable t) {
                    logger.error("Unhandled exception", t);
                }
            }
        }, millis, TimeUnit.MILLISECONDS);
    }

    private Bootstrap configureBootstrap(Bootstrap b) {
        return configureBootstrap(b, workerGroup);
    }

    public Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {
        b.group(g);
        b.channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONFIG.peerConnectionTimeout());
        peer = peersManager.getRandomPeer();
        httpInitializer = provider.get();
        httpInitializer.setHttpClient(this);
        httpInitializer.setRequestQueue(requestQueue);
        httpInitializer.setHost(peer.getHost());
        logger.info("Config remote peer {}:{}", peer.getHost(), peer.getPort());
        b.remoteAddress(peer.getHost(), peer.getPort());
        b.handler(httpInitializer);

        return b;
    }

    public void connect(Bootstrap b) {
        b.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.cause() != null) {
                    logger.error("Failed to connect: " + future.cause());
                    future.cause().printStackTrace();

                    setIsConnecting(false);
                    tryConnect();
                }
            }
        });
    }
}
