package io.taucoin.sync;

import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.facade.Taucoin;
import io.taucoin.listener.EthereumListener;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.Channel;
import io.taucoin.util.Functional;
import io.taucoin.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.taucoin.net.tau.TauVersion.V62;
import static io.taucoin.sync.SyncStateName.IDLE;
import static io.taucoin.util.BIUtil.isIn20PercentRange;
import static io.taucoin.util.BIUtil.isMoreThan;
import static io.taucoin.util.TimeUtils.*;

/**
 * <p>Encapsulates logic which manages peers involved in blockchain sync</p>
 *
 * Holds connections, bans, disconnects and other peers logic<br>
 * The pool is completely threadsafe<br>
 * Implements {@link Iterable} and can be used in "foreach" loop<br>
 * Used by {@link SyncManager}
 *
 * @author Mikhail Kalinin
 * @since 10.08.2015
 */
@Singleton
public class PeersPool implements Iterable<Channel> {

    public static final Logger logger = LoggerFactory.getLogger("sync");

    private static final long WORKER_TIMEOUT = 3; // 3 seconds

    private static final int DISCONNECT_HITS_THRESHOLD = 5;
    private static final long DEFAULT_BAN_TIMEOUT = minutesToMillis(1);
    private static final long CONNECTION_TIMEOUT = secondsToMillis(30);

    private static final int MIN_PEERS_COUNT = 3;

    private final Map<ByteArrayWrapper, Channel> activePeers = new HashMap<>();
    private final Set<Channel> bannedPeers = new HashSet<>();
    private final Map<String, Integer> disconnectHits = new HashMap<>();
    private final Map<String, Long> bans = new HashMap<>();
    private final Map<String, Long> pendingConnections = new HashMap<>();

    private Taucoin taucoin;

    private EthereumListener ethereumListener;

    public void setTaucoin(Taucoin taucoin) {
        this.taucoin = taucoin;
        this.ethereumListener = taucoin.getWorldManager().getListener();
    }
    
	public PeersPool(){
        init();
    }

    public void init() {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            releaseBans();
                            processConnections();
                        } catch (Throwable t) {
                            logger.error("Unhandled exception", t);
                        }
                    }
                }, WORKER_TIMEOUT, WORKER_TIMEOUT, TimeUnit.SECONDS
        );
    }

    public void add(Channel peer) {
        synchronized (activePeers) {
            activePeers.put(peer.getNodeIdWrapper(), peer);
            bannedPeers.remove(peer);
        }
        synchronized (pendingConnections) {
            pendingConnections.remove(peer.getPeerId());
        }
        synchronized (bans) {
            bans.remove(peer.getPeerId());
        }

        ethereumListener.onPeerAddedToSyncPool(peer);

        logger.info("Peer {}: added to pool", Utils.getNodeIdShort(peer.getPeerId()));
    }

    public void remove(Channel peer) {
        synchronized (activePeers) {
            activePeers.values().remove(peer);
        }
    }

    @Nullable
    public Channel getMaster() {

        synchronized (activePeers) {

            if (activePeers.isEmpty()) {
                return null;
            }

            Channel best61 = null;
            Channel best62 = null;
            int count62 = 0;
            int count61 = 0;

            for (Channel peer : activePeers.values()) {

                if (peer.getTauVersion().getCode() >= V62.getCode()) {

                    if (best62 == null || isMoreThan(peer.getTotalDifficulty(), best62.getTotalDifficulty())) {
                        best62 = peer;
                    }
                    count62++;
                } else {

                    if (best61 == null || isMoreThan(peer.getTotalDifficulty(), best61.getTotalDifficulty())) {
                        best61 = peer;
                    }
                    count61++;
                }

            }

            if (best61 == null) return best62;
            if (best62 == null) return best61;

            if (count62 >= MIN_PEERS_COUNT) return best62;
            if (count61 >= MIN_PEERS_COUNT) return best61;

            if (isIn20PercentRange(best62.getTotalDifficulty(), best61.getTotalDifficulty())) {
                return best62;
            } else {
                return best61;
            }
        }
    }

    @Nullable
    public Channel getByNodeId(byte[] nodeId) {
        return activePeers.get(new ByteArrayWrapper(nodeId));
    }

    public void onDisconnect(Channel peer) {
        if (peer.getNodeId() == null) {
            return;
        }

        boolean existed;
        synchronized (activePeers) {
            existed = activePeers.values().remove(peer);
            bannedPeers.remove(peer);
        }

        // do not count disconnects for nodeId
        // if exact peer is not an active one
        if (!existed) {
            return;
        }

        logger.info("Peer {}: disconnected", peer.getPeerIdShort());

        synchronized (disconnectHits) {
            Integer hits = disconnectHits.get(peer.getPeerId());
            if (hits == null) {
                hits = 0;
            }
            if (hits > DISCONNECT_HITS_THRESHOLD) {
                ban(peer);
                logger.info("Peer {}: banned due to disconnects exceeding", Utils.getNodeIdShort(peer.getPeerId()));
                disconnectHits.remove(peer.getPeerId());
            } else {
                disconnectHits.put(peer.getPeerId(), hits + 1);
            }
        }
    }

    public void connect(Node node) {
        if (logger.isTraceEnabled()) logger.trace(
                "Peer {}: initiate connection",
                node.getHexIdShort()
        );
        if (isInUse(node.getHexId())) {
            if (logger.isTraceEnabled()) logger.trace(
                    "Peer {}: connection already initiated",
                    node.getHexIdShort()
            );
            return;
        }

        synchronized (pendingConnections) {
            taucoin.connect(node);
            pendingConnections.put(node.getHexId(), timeAfterMillis(CONNECTION_TIMEOUT));
        }
    }

    public void ban(Channel peer) {

        peer.changeSyncState(IDLE);

        synchronized (activePeers) {
            if (activePeers.containsKey(peer.getNodeIdWrapper())) {
                activePeers.remove(peer.getNodeIdWrapper());
                bannedPeers.add(peer);
            }
        }

        synchronized (bans) {
            bans.put(peer.getPeerId(), timeAfterMillis(DEFAULT_BAN_TIMEOUT));
        }
    }

    public Set<String> nodesInUse() {
        Set<String> ids = new HashSet<>();
        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                ids.add(peer.getPeerId());
            }
        }
        synchronized (bans) {
            ids.addAll(bans.keySet());
        }
        synchronized (pendingConnections) {
            ids.addAll(pendingConnections.keySet());
        }
        return ids;
    }

    public boolean isInUse(String nodeId) {
        return nodesInUse().contains(nodeId);
    }

    public void changeState(SyncStateName newState) {
        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                peer.changeSyncState(newState);
            }
        }
    }

    public void changeStateForIdles(SyncStateName newState, TauVersion compatibleVersion) {

        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                if (peer.isIdle() && peer.getTauVersion().isCompatible(compatibleVersion))
                    peer.changeSyncState(newState);
            }
        }
    }

    public void changeStateForIdles(SyncStateName newState) {

        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                if (peer.isIdle())
                    peer.changeSyncState(newState);
            }
        }
    }

    public boolean hasCompatible(TauVersion version) {

        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                if (peer.getTauVersion().isCompatible(version))
                    return true;
            }
        }

        return false;
    }

    @Nullable
    public Channel findOne(Functional.Predicate<Channel> filter) {

        synchronized (activePeers) {
            for (Channel peer : activePeers.values()) {
                if (filter.test(peer))
                    return peer;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return activePeers.isEmpty();
    }

    public int activeCount() {
        return activePeers.size();
    }

    @Override
    public Iterator<Channel> iterator() {
        synchronized (activePeers) {
            return new ArrayList<>(activePeers.values()).iterator();
        }
    }

    void logActivePeers() {
        if (activePeers.size() > 0) {
            logger.info("\n");
            logger.info("Active peers");
            logger.info("============");
            for(Channel peer : this) {
                peer.logSyncStats();
            }
        }
    }

    void logBannedPeers() {
        synchronized (bans) {
            if (bans.size() > 0) {
                logger.info("\n");
                logger.info("Banned peers");
                logger.info("============");
                for (Map.Entry<String, Long> e : bans.entrySet()) {
                    logger.info(
                            "Peer {} | {} seconds ago",
                            Utils.getNodeIdShort(e.getKey()),
                            millisToSeconds(System.currentTimeMillis() - (e.getValue() - DEFAULT_BAN_TIMEOUT))
                    );
                }
            }
        }
    }

    private void releaseBans() {

        Set<String> released;

        synchronized (bans) {
            released = getTimeoutExceeded(bans);

            synchronized (activePeers) {
                for (Channel peer : bannedPeers) {
                    if (released.contains(peer.getPeerId())) {
                        activePeers.put(peer.getNodeIdWrapper(), peer);
                    }
                }
                bannedPeers.removeAll(activePeers.values());
            }

            bans.keySet().removeAll(released);
        }

        synchronized (disconnectHits) {
            disconnectHits.keySet().removeAll(released);
        }
    }

    private void processConnections() {
        synchronized (pendingConnections) {
            Set<String> exceeded = getTimeoutExceeded(pendingConnections);
            pendingConnections.keySet().removeAll(exceeded);
        }
    }

    private Set<String> getTimeoutExceeded(Map<String, Long> map) {
        Set<String> exceeded = new HashSet<>();
        final Long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : map.entrySet()) {
            if (now >= e.getValue()) {
                exceeded.add(e.getKey());
            }
        }
        return exceeded;
    }
}
