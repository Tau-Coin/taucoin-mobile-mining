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

    private boolean inited = false;

    private PeersManager peersManager;

    private TaucoinListener listener;

    private Provider<HttpClientInitializer> provider;

    private RequestQueue requestQueue;

    private Node peer = null;
    private Bootstrap bootstrap = null;
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

    private void init() {
        peer = peersManager.getRandomPeer();
        httpInitializer = provider.get();
        httpInitializer.setHttpClient(this);
        httpInitializer.setRequestQueue(requestQueue);
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONFIG.peerConnectionTimeout());
        bootstrap.remoteAddress(peer.getHost(), peer.getPort());
        bootstrap.handler(httpInitializer);
    }

    public void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void sendRequest(Message message) {
        if (!inited) {
            inited = true;
            init();
        }

        requestQueue.sendMessage(message);
        tryConnect();
    }

    public void tryConnect() {
        if (isConnected.get() || isConnecting.get()) {
            logger.info("Peer {} is still alive", channel);
            return;
        }

        isConnecting.set(true);
        // schedule connect task
        scheduleConnect(100);
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
        requestQueue.close();
        logger.info("Peer {} disconnected", ctx.channel());
        channel = null;

        // schedule reconnect task
        if (requestQueue.size() > 0) {
            scheduleConnect(100);
        }
    }

    private void doConnect() {
        try {
            ChannelFuture f = bootstrap.connect();
            f.sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

            logger.debug("Connection is closed");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Connect exception {} ", e);
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

    /*
    public static class ConnectChannelFutureListener implements ChannelFutureListener {
        HttpClient client;
        private Message message;
        private int reconnectTimes = 0;

        public ConnectChannelFutureListener(HttpClient client, Message message) {
            this.client = client;
            this.message = message;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            Channel channel = future.channel();
            if (future.isSuccess()) {
                logger.info("Connect sucess {}", channel);
                channel.writeAndFlush(message);
                client.compareAndSetIdle(false, true);
            } else {
                channel.close();
                if (reconnectTimes <= 3) {
                    logger.info("Connect fail, try again");
                    reconnectTimes++;
                    client.doConnect().addListener(this);
                } else {
                    logger.info("No need to reconnect");
                    client.compareAndSetIdle(false, true);
                }
            }
        }
    }
    */
}
