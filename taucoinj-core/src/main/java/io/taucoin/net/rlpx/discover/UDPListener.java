package io.taucoin.net.rlpx.discover;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.taucoin.net.rlpx.NodeType;
import io.taucoin.config.SystemProperties;
import io.taucoin.net.rlpx.Node;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class UDPListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("discover");

    private int port;
    private String address;
    private String[] bootPeers;

    private NodeManager nodeManager;

    SystemProperties config = SystemProperties.CONFIG;

    private Channel channel;
    private volatile boolean shutdown = false;
    private volatile boolean initialized = false;
    private DiscoveryExecutor discoveryExecutor;

    @Inject
    public UDPListener(NodeManager nodeManager) {
        this.nodeManager = nodeManager;
        this.address = SystemProperties.CONFIG.bindIp();
        port = SystemProperties.CONFIG.listenPort();
        if (SystemProperties.CONFIG.peerDiscovery()) {
            bootPeers = SystemProperties.CONFIG.peerDiscoveryIPList().toArray(new String[0]);
        }
    }

    public UDPListener(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public void init() {
        this.address = config.bindIp();
        port = config.listenPort();
        if (config.peerDiscovery()) {
            bootPeers = config.peerDiscoveryIPList().toArray(new String[0]);
        }
        if (config.peerDiscovery()) {
            new Thread("UDPListener") {
                @Override
                public void run() {
                    try {
                        UDPListener.this.start(bootPeers);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
        initialized = true;
    }

    public static Node parseNode(String s) {
        int idx1 = s.indexOf('@');
        int idx2 = s.indexOf(':');
        String id = s.substring(0, idx1);
        String host = s.substring(idx1 + 1, idx2);
        int port = Integer.parseInt(s.substring(idx2+1));
        return new Node(Hex.decode(id), host, port);
    }

    public void start(String[] args) throws Exception {

        logger.info("Discovery UDPListener started");
        NioEventLoopGroup group = new NioEventLoopGroup(1);

        final List<Node> bootNodes = new ArrayList<>();

        String localAddress = config.bindIp();
        String externalAddress = config.externalIp();
        // FIXME: setting nodes from ip.list and attaching node nodeId [f35cc8] constantly
        for (String boot: args) {
            Node n = Node.instanceOf(boot);
            if (!localAddress.equals(n.getHost())
                && !externalAddress.equals(n.getHost())) {
                n.setType(NodeType.SUPER);
                bootNodes.add(n);
            }
        }

        nodeManager.setBootNodes(bootNodes);

        try {
            while(!shutdown) {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioDatagramChannel.class)
                        .handler(new ChannelInitializer<NioDatagramChannel>() {
                            @Override
                            public void initChannel(NioDatagramChannel ch)
                                    throws Exception {
                                ch.pipeline().addLast(new PacketDecoder());
                                MessageHandler messageHandler = new MessageHandler(ch, nodeManager);
                                nodeManager.setMessageSender(messageHandler);
                                ch.pipeline().addLast(messageHandler);
                            }
                        });

                channel = b.bind(address, port).sync().channel();

                discoveryExecutor = new DiscoveryExecutor(nodeManager);
                discoveryExecutor.start();

                channel.closeFuture().sync();
                if (shutdown) {
                    logger.info("Shutdown discovery UDPListener");
                    break;
                }
                logger.warn("UDP channel closed. Recreating after 5 sec pause...");
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            logger.error("{}", e);
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public boolean isStarted() {
        return initialized;
    }

    public void shutdown() {
        logger.info("Closing UDPListener...");
        shutdown = true;
        initialized = false;
        if (channel != null) {
            try {
                channel.close().await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Problems closing UDPListener", e);
            }
        }

        if (discoveryExecutor != null) {
            try {
                discoveryExecutor.close();
            } catch (Exception e) {
                logger.warn("Problems closing DiscoveryExecutor", e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String address = "0.0.0.0";
        int port = 30606;
        if (args.length >= 2) {
            address = args[0];
            port = Integer.parseInt(args[1]);
        }
        new UDPListener(address, port).start(Arrays.copyOfRange(args, 2, args.length));
    }
}
