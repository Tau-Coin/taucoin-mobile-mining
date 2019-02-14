package io.taucoin.net.message;

import io.taucoin.config.SystemProperties;
import io.taucoin.net.client.Capability;
import io.taucoin.net.client.ConfigCapabilities;
import io.taucoin.net.p2p.*;
import org.spongycastle.util.encoders.Hex;

import java.util.List;
import javax.inject.Inject;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * This class contains static values of messages on the network. These message
 * will always be the same and therefore don't need to be created each time.
 *
 * @author Roman Mandeleil
 * @since 13.04.14
 */
public class StaticMessages {

    private static SystemProperties config = SystemProperties.CONFIG;

    public final static PingMessage PING_MESSAGE = new PingMessage();
    public final static PongMessage PONG_MESSAGE = new PongMessage();
    public final static GetPeersMessage GET_PEERS_MESSAGE = new GetPeersMessage();
    public final static DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage(ReasonCode.REQUESTED);

    public static final byte[] SYNC_TOKEN = Hex.decode("22400891");

    public static HelloMessage createHelloMessage(String peerId) {
        return createHelloMessage(peerId, config.listenPort());
    }
    public static HelloMessage createHelloMessage(String peerId, int listenPort) {

        String helloAnnouncement = buildHelloAnnouncement();
        byte p2pVersion = (byte) config.defaultP2PVersion();
        List<Capability> capabilities = ConfigCapabilities.getConfigCapabilities();

        return new HelloMessage(p2pVersion, helloAnnouncement,
                capabilities, listenPort, peerId);
    }

    private static String buildHelloAnnouncement() {
        String version = config.projectVersion();
        String system = System.getProperty("os.name");
        if (system.contains(" "))
            system = system.substring(0, system.indexOf(" "));
        if (System.getProperty("java.vm.vendor").contains("Android"))
            system = "Android";
        String phrase = config.helloPhrase();

        return String.format("Ethereum(J)/v%s/%s/%s/Java", version, phrase, system);
    }
}
