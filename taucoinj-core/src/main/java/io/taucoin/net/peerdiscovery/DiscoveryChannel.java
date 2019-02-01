package io.taucoin.net.peerdiscovery;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.taucoin.listener.EthereumListener;
import io.taucoin.net.MessageQueue;
import io.taucoin.net.client.Capability;
import io.taucoin.net.tau.handler.TauHandler;
import io.taucoin.net.tau.message.StatusMessage;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.p2p.P2pHandler;
import io.taucoin.net.rlpx.MessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.context.ApplicationContext;
import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * This class creates the connection to an remote address using the Netty framework
 *
 * @see <a href="http://netty.io">http://netty.io</a>
 */
public class DiscoveryChannel {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private boolean peerDiscoveryMode = false;

    @Inject
    EthereumListener ethereumListener;

    @Inject
    MessageQueue messageQueue;

    @Inject
    P2pHandler p2pHandler;

    @Inject
    TauHandler ethHandler;

    @Inject
    //ApplicationContext ctx;


    public DiscoveryChannel() {

    }

    public void connect(String host, int port) {

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ethereumListener.trace("Connecting to: " + host + ":" + port);

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);

            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONFIG.peerConnectionTimeout());
            b.remoteAddress(host, port);


            p2pHandler.setMsgQueue(messageQueue);
            p2pHandler.setPeerDiscoveryMode(true);

            ethHandler.setMsgQueue(messageQueue);
            ethHandler.setPeerDiscoveryMode(true);

            final MessageCodec decoder = new MessageCodec();// ctx.getBean(MessageCodec.class);

            b.handler(

                    new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {

                            logger.info("Open connection, channel: {}", ch.toString());

                            ch.pipeline().addLast("readTimeoutHandler",
                                    new ReadTimeoutHandler(CONFIG.peerChannelReadTimeout(), TimeUnit.SECONDS));
//                            ch.pipeline().addLast("initiator", decoder.getInitiator());
                            ch.pipeline().addLast("messageCodec", decoder);
                            ch.pipeline().addLast(Capability.P2P, p2pHandler);
                            ch.pipeline().addLast(Capability.TAU, ethHandler);

                            // limit the size of receiving buffer to 1024
                            ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(32368));
                            ch.config().setOption(ChannelOption.SO_RCVBUF, 32368);
                            ch.config().setOption(ChannelOption.SO_BACKLOG, 1024);
                        }
                    }
            );

            // Start the client.
            ChannelFuture f = b.connect().sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
            logger.debug("Connection is closed");

        } catch (Exception e) {
            logger.debug("Exception: {} ({})", e.getMessage(), e.getClass().getName());
            throw new Error("Disconnnected");
        } finally {
            workerGroup.shutdownGracefully();

            if (!peerDiscoveryMode) {
//                EthereumListener listener =  WorldManager.getInstance().getListener();
//                listener.onPeerDisconnect(host, port);
            }

        }
    }

    public HelloMessage getHelloHandshake() {
        return p2pHandler.getHandshakeHelloMessage();
    }

    public StatusMessage getStatusHandshake() {
        return ethHandler.getHandshakeStatusMessage();
    }
}
