package io.taucoin.net.p2p;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.listener.EthereumListener;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.MessageQueue;
import io.taucoin.net.client.Capability;
import io.taucoin.net.client.ConfigCapabilities;
import io.taucoin.net.tau.message.NewBlockMessage;
import io.taucoin.net.tau.message.TransactionsMessage;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.message.StaticMessages;
import io.taucoin.net.peerdiscovery.PeerDiscovery;
import io.taucoin.net.peerdiscovery.PeerInfo;
import io.taucoin.net.server.Channel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.net.tau.TauVersion.*;
import static io.taucoin.net.message.StaticMessages.*;

/**
 * Process the basic protocol messages between every peer on the network.
 *
 * Peers can send/receive
 * <ul>
 *  <li>HELLO       :   Announce themselves to the network</li>
 *  <li>DISCONNECT  :   Disconnect themselves from the network</li>
 *  <li>GET_PEERS   :   Request a list of other knows peers</li>
 *  <li>PEERS       :   Send a list of known peers</li>
 *  <li>PING        :   Check if another peer is still alive</li>
 *  <li>PONG        :   Confirm that they themselves are still alive</li>
 * </ul>
 */
public class P2pHandler extends SimpleChannelInboundHandler<P2pMessage> {

    public final static byte VERSION = 4;

    public final static byte[] SUPPORTED_VERSIONS = {4, 5};

    private final static Logger logger = LoggerFactory.getLogger("net");

    private static ScheduledExecutorService pingTimer =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    return new Thread(r, "P2pPingTimer");
                }
            });

    private MessageQueue msgQueue;

    private boolean peerDiscoveryMode = false;

    private HelloMessage handshakeHelloMessage = null;
    private Set<PeerInfo> lastPeersSent;

    private int ethInbound;
    private int ethOutbound;

    EthereumListener ethereumListener;

    PeerDiscovery peerDiscovery;

    private Channel channel;
    private ScheduledFuture<?> pingTask;

    @Inject
    public P2pHandler(PeerDiscovery peerDiscovery, EthereumListener listener) {
        this.peerDiscovery = peerDiscovery;
        this.ethereumListener = listener;
        this.peerDiscoveryMode = false;
    }

    public P2pHandler(MessageQueue msgQueue, boolean peerDiscoveryMode) {
        this.msgQueue = msgQueue;
        this.peerDiscoveryMode = peerDiscoveryMode;
    }


    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("P2P protocol activated");
        msgQueue.activate(ctx);
        ethereumListener.trace("P2P protocol activated");
        startTimers();
    }


    @Override
    public void channelRead0(final ChannelHandlerContext ctx, P2pMessage msg) throws InterruptedException {

        if (P2pMessageCodes.inRange(msg.getCommand().asByte()))
            logger.trace("P2PHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("P2PHandler invoke: [%s]", msg.getCommand()));

        switch (msg.getCommand()) {
            case HELLO:
                msgQueue.receivedMessage(msg);
                setHandshake((HelloMessage) msg, ctx);
//                sendGetPeers();
                break;
            case DISCONNECT:
                msgQueue.receivedMessage(msg);
                channel.getNodeStatistics().nodeDisconnectedRemote(((DisconnectMessage) msg).getReason());
                processDisconnect((DisconnectMessage) msg);
                break;
            case PING:
                msgQueue.receivedMessage(msg);
                ctx.writeAndFlush(PONG_MESSAGE);
                break;
            case PONG:
                msgQueue.receivedMessage(msg);
                break;
            case GET_PEERS:
                msgQueue.receivedMessage(msg);
                sendPeers(); // todo: implement session management for peer request
                break;
            case PEERS:
                msgQueue.receivedMessage(msg);
                processPeers(ctx, (PeersMessage) msg);

                if (peerDiscoveryMode ||
                        !handshakeHelloMessage.getCapabilities().contains(Capability.TAU)) {
                    disconnect(ReasonCode.REQUESTED);
                    killTimers();
                    ctx.close().sync();
                    ctx.disconnect().sync();
                }
                break;
            default:
                ctx.fireChannelRead(msg);
                break;
        }
    }

    private void disconnect(ReasonCode reasonCode) {
        msgQueue.sendMessage(new DisconnectMessage(reasonCode));
        channel.getNodeStatistics().nodeDisconnectedLocal(reasonCode);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("channel inactive: ", ctx.toString());
        this.killTimers();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("P2p handling failed", cause);
        ctx.close();
        killTimers();
    }

    private void processDisconnect(DisconnectMessage msg) {

        if (!logger.isInfoEnabled() || msg.getReason() != ReasonCode.USELESS_PEER) {
            return;
        }

        if (channel.getNodeStatistics().ethInbound.get() - ethInbound > 1 ||
            channel.getNodeStatistics().ethOutbound.get() - ethOutbound > 1) {

            // it means that we've been disconnected
            // after some incorrect action from our peer
            // need to log this moment
            logger.info("From: \t{}\t [DISCONNECT reason=BAD_PEER_ACTION]", channel);
        }
    }

    private void processPeers(ChannelHandlerContext ctx, PeersMessage peersMessage) {
        peerDiscovery.addPeers(peersMessage.getPeers());
    }

    private void sendGetPeers() {
        msgQueue.sendMessage(StaticMessages.GET_PEERS_MESSAGE);
    }

    private void sendPeers() {

        Set<PeerInfo> peers = peerDiscovery.getPeers();

        if (lastPeersSent != null && peers.equals(lastPeersSent)) {
            logger.info("No new peers discovered don't answer for GetPeers");
            return;
        }

        Set<Peer> peerSet = new HashSet<>();
        for (PeerInfo peer : peers) {
            new Peer(peer.getAddress(), peer.getPort(), peer.getPeerId());
        }

        PeersMessage msg = new PeersMessage(peerSet);
        lastPeersSent = peers;
        msgQueue.sendMessage(msg);
    }



    public void setHandshake(HelloMessage msg, ChannelHandlerContext ctx) {

        channel.getNodeStatistics().setClientId(msg.getClientId());

        this.ethInbound = channel.getNodeStatistics().ethInbound.get();
        this.ethOutbound = channel.getNodeStatistics().ethOutbound.get();

        this.handshakeHelloMessage = msg;
        if (!isProtocolVersionSupported(msg.getP2PVersion())) {
            disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
        }
        else {
            List<Capability> capInCommon = getSupportedCapabilities(msg);
            channel.initMessageCodes(capInCommon);
            for (Capability capability : capInCommon) {
                if (capability.getName().equals(Capability.TAU)) {

                    // Activate TauHandler for this peer
                    channel.activateEth(ctx, fromCode(capability.getVersion()));
                }
            }

            InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
            int port = msg.getListenPort();
            PeerInfo confirmedPeer = new PeerInfo(address, port, msg.getPeerId());
            confirmedPeer.setOnline(false);
            confirmedPeer.getCapabilities().addAll(msg.getCapabilities());

            //todo calculate the Offsets
            peerDiscovery.getPeers().add(confirmedPeer);
            ethereumListener.onHandShakePeer(channel, msg);

        }
    }

    /**
     * submit transaction to the network
     *
     * @param tx - fresh transaction object
     */
    public void sendTransaction(Transaction tx) {

        TransactionsMessage msg = new TransactionsMessage(tx);
        msgQueue.sendMessage(msg);
    }

    public void sendNewBlock(Block block) {

        NewBlockMessage msg = new NewBlockMessage(block, block.getCumulativeDifficulty().toByteArray());
        msgQueue.sendMessage(msg);
    }

    public void sendDisconnect() {
        msgQueue.disconnect();
    }

    public HelloMessage getHandshakeHelloMessage() {
        return handshakeHelloMessage;
    }

    private void startTimers() {
        // sample for pinging in background
        pingTask = pingTimer.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    msgQueue.sendMessage(PING_MESSAGE);
                } catch (Throwable t) {
                    logger.error("Unhandled exception", t);
                }
            }
        }, 2, CONFIG.getProperty("peer.p2p.pingInterval", 5), TimeUnit.SECONDS);
    }

    public void killTimers() {
        pingTask.cancel(false);
        msgQueue.close();
    }


    public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public static boolean isProtocolVersionSupported(byte ver) {
        for (byte v : SUPPORTED_VERSIONS) {
            if (v == ver) return true;
        }
        return false;
    }

    public List<Capability> getSupportedCapabilities(HelloMessage hello) {
        List<Capability> configCaps = ConfigCapabilities.getConfigCapabilities();
        List<Capability> supported = new ArrayList<>();

        List<Capability> eths = new ArrayList<>();

        for (Capability cap : hello.getCapabilities()) {
            if (configCaps.contains(cap)) {
                if (cap.isEth()) {
                    eths.add(cap);
                } else {
                    supported.add(cap);
                }
            }
        }

        if (eths.isEmpty()) {
            return supported;
        }

        // we need to pick up
        // the most recent Tau version
        Capability highest = null;
        for (Capability eth : eths) {
            if (highest == null || highest.getVersion() < eth.getVersion()) {
                highest = eth;
            }
        }

        supported.add(highest);
        return supported;
    }

}
