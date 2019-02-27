package io.taucoin.net.server;

import io.taucoin.config.SystemProperties;
import io.taucoin.listener.TaucoinListener;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.encoders.Hex;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * This class establishes a listener for incoming connections.
 * See <a href="http://netty.io">http://netty.io</a>.
 *
 * @author Roman Mandeleil
 * @since 01.11.2014
 */
@Singleton
public class PeerServer {

    private static final Logger logger = LoggerFactory.getLogger("net");

    ChannelManager channelManager;

    public TauChannelInitializer tauChannelInitializer;

    TaucoinListener ethereumListener;

    @Inject
    public PeerServer(ChannelManager channelManager, TauChannelInitializer tauChannelInitializer, TaucoinListener listener) {
        this.channelManager = channelManager;
        this.tauChannelInitializer = tauChannelInitializer;
        this.ethereumListener = listener;
    }

    public void start(int port) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ethereumListener.trace("Listening on port " + port);

        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONFIG.peerConnectionTimeout());

            b.handler(new LoggingHandler());
            b.childHandler(tauChannelInitializer);

            // Start the client.
            logger.info("Listening for incoming connections, port: [{}] ", port);
            logger.info("NodeId: [{}] ", Hex.toHexString(CONFIG.nodeId()));

            ChannelFuture f = b.bind(port).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
            logger.debug("Connection is closed");

        } catch (Exception e) {
            logger.debug("Exception: {} ({})", e.getMessage(), e.getClass().getName());
            throw new Error("Server Disconnected");
        } finally {
            workerGroup.shutdownGracefully();

        }
    }

}
