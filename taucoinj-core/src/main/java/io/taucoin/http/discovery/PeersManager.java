package io.taucoin.http.discovery;

import io.taucoin.net.rlpx.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Taucoin Core Developers
 * @since 01.04.2019
 */
@Singleton
public class PeersManager {

    private final static Logger logger = LoggerFactory.getLogger("http");

    private List<Node> activePeers = new CopyOnWriteArrayList<>();

    @Inject
    public PeersManager() {
        init();
    }

    private void init() {
        List<String> bootPeers = CONFIG.httpDiscoveryActivePeers();

        for (String boot: bootPeers) {
            Node n = Node.instanceOf(boot);
            addActivePeer(n);
        }
        Collections.shuffle(activePeers);
    }

    public Node getRandomPeer() {
        Random random = new Random();
        int n = random.nextInt(activePeers.size());
        return activePeers.get(n);
    }

    public void addActivePeer(Node node) {
        if (!activePeers.contains(node)) {
            activePeers.add(node);
        }
    }
}
