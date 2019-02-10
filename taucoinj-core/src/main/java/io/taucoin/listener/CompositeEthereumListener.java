package io.taucoin.listener;

import io.taucoin.core.*;
import io.taucoin.net.tau.message.StatusMessage;
import io.taucoin.net.message.Message;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.Channel;
import javax.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Roman Mandeleil
 * @since 12.11.2014
 */
@Singleton
public class CompositeEthereumListener implements EthereumListener {

    List<EthereumListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(EthereumListener listener) {
        listeners.add(listener);
    }
    public void removeListener(EthereumListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void trace(String output) {
        for (EthereumListener listener : listeners) {
            listener.trace(output);
        }
    }

    @Override
    public void onBlock(Block block) {
        for (EthereumListener listener : listeners) {
            listener.onBlock(block);
        }
    }

    @Override
    public void onRecvMessage(Channel channel, Message message) {
        for (EthereumListener listener : listeners) {
            listener.onRecvMessage(channel, message);
        }
    }

    @Override
    public void onSendMessage(Channel channel, Message message) {
        for (EthereumListener listener : listeners) {
            listener.onSendMessage(channel, message);
        }
    }

    @Override
    public void onPeerDisconnect(String host, long port) {
        for (EthereumListener listener : listeners) {
            listener.onPeerDisconnect(host, port);
        }
    }

    @Override
    public void onPendingTransactionsReceived(List<Transaction> transactions) {
        for (EthereumListener listener : listeners) {
            listener.onPendingTransactionsReceived(transactions);
        }
    }

    @Override
    public void onPendingStateChanged(PendingState pendingState) {
        for (EthereumListener listener : listeners) {
            listener.onPendingStateChanged(pendingState);
        }
    }

    @Override
    public void onSyncDone() {
        for (EthereumListener listener : listeners) {
            listener.onSyncDone();
        }
    }

    @Override
    public void onNoConnections() {
        for (EthereumListener listener : listeners) {
            listener.onNoConnections();
        }
    }

    @Override
    public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {
        for (EthereumListener listener : listeners) {
            listener.onHandShakePeer(channel, helloMessage);
        }
    }

    @Override
    public void onNodeDiscovered(Node node) {
        for (EthereumListener listener : listeners) {
            listener.onNodeDiscovered(node);
        }
    }

    @Override
    public void onEthStatusUpdated(Channel channel, StatusMessage status) {
        for (EthereumListener listener : listeners) {
            listener.onEthStatusUpdated(channel, status);
        }
    }


    @Override
    public void onPeerAddedToSyncPool(Channel peer) {
        for (EthereumListener listener : listeners) {
            listener.onPeerAddedToSyncPool(peer);
        }
    }
}
