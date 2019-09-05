package io.taucoin.listener;

import io.taucoin.core.*;
import io.taucoin.net.tau.message.StatusMessage;
import io.taucoin.net.message.Message;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.Channel;
import javax.inject.Singleton;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Roman Mandeleil
 * @since 12.11.2014
 */
@Singleton
public class CompositeTaucoinListener implements TaucoinListener {

    List<TaucoinListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(TaucoinListener listener) {
        listeners.add(listener);
    }
    public void removeListener(TaucoinListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void trace(String output) {
        for (TaucoinListener listener : listeners) {
            listener.trace(output);
        }
    }

    @Override
    public void onBlock(Block block) {
        for (TaucoinListener listener : listeners) {
            listener.onBlock(block);
        }
    }

    @Override
    public void onBlockConnected(Block block) {
        for (TaucoinListener listener : listeners) {
            listener.onBlockConnected(block);
        }
    }

    @Override
    public void onBlockDisconnected(Block block) {
        for (TaucoinListener listener : listeners) {
            listener.onBlockDisconnected(block);
        }
    }

    @Override
    public void onRecvMessage(Channel channel, Message message) {
        for (TaucoinListener listener : listeners) {
            listener.onRecvMessage(channel, message);
        }
    }

    @Override
    public void onSendMessage(Channel channel, Message message) {
        for (TaucoinListener listener : listeners) {
            listener.onSendMessage(channel, message);
        }
    }

    @Override
    public void onPeerDisconnect(String host, long port) {
        for (TaucoinListener listener : listeners) {
            listener.onPeerDisconnect(host, port);
        }
    }

    @Override
    public void onPendingTransactionsReceived(List<Transaction> transactions) {
        for (TaucoinListener listener : listeners) {
            listener.onPendingTransactionsReceived(transactions);
        }
    }

    @Override
    public void onPendingStateChanged(PendingState pendingState) {
        for (TaucoinListener listener : listeners) {
            listener.onPendingStateChanged(pendingState);
        }
    }

    @Override
    public void onSyncDone() {
        for (TaucoinListener listener : listeners) {
            listener.onSyncDone();
        }
    }

    @Override
    public void onNoConnections() {
        for (TaucoinListener listener : listeners) {
            listener.onNoConnections();
        }
    }

    @Override
    public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {
        for (TaucoinListener listener : listeners) {
            listener.onHandShakePeer(channel, helloMessage);
        }
    }

    @Override
    public void onNodeDiscovered(Node node) {
        for (TaucoinListener listener : listeners) {
            listener.onNodeDiscovered(node);
        }
    }

    @Override
    public void onEthStatusUpdated(Channel channel, StatusMessage status) {
        for (TaucoinListener listener : listeners) {
            listener.onEthStatusUpdated(channel, status);
        }
    }


    @Override
    public void onPeerAddedToSyncPool(Channel peer) {
        for (TaucoinListener listener : listeners) {
            listener.onPeerAddedToSyncPool(peer);
        }
    }

    @Override
    public void onTransactionExecuated(TransactionExecuatedOutcome outcome) {
        for (TaucoinListener listener : listeners) {
            listener.onTransactionExecuated(outcome);
        }
    }

    @Override
    public void onSendHttpPayload(String payload) {
        for (TaucoinListener listener : listeners) {
            listener.onSendHttpPayload(payload);
        }
    }

    @Override
    public void onRecvHttpPayload(String payload) {
        for (TaucoinListener listener : listeners) {
            listener.onRecvHttpPayload(payload);
        }
    }

    @Override
    public void onChainInfoChanged(long height, byte[] previousBlockHash,
            byte[] currentBlockHash, BigInteger totalDiff, long medianFee) {
        for (TaucoinListener listener : listeners) {
            listener.onChainInfoChanged(height, previousBlockHash,
                    currentBlockHash, totalDiff, medianFee);
        }
    }

    @Override
    public void onBlocksDownloaded(long from, long end) {
        for (TaucoinListener listener : listeners) {
            listener.onBlocksDownloaded(from, end);
        }
    }

    @Override
    public void onSyncHibernation(long number) {
        for (TaucoinListener listener : listeners) {
            listener.onSyncHibernation(number);
        }
    }

    @Override
    public void onBlockQueueRollback(long number) {
        for (TaucoinListener listener : listeners) {
            listener.onBlockQueueRollback(number);
        }
    }

    @Override
    public void onStatesLoaded(long hasLoaded, long total) {
        for (TaucoinListener listener : listeners) {
            listener.onStatesLoaded(hasLoaded, total);
        }
    }

    @Override
    public void onStatesLoadedCompleted(long tagHeight) {
        for (TaucoinListener listener : listeners) {
            listener.onStatesLoadedCompleted(tagHeight);
        }
    }

    @Override
    public void onStatesLoadedFailed(long tagHeight) {
        for (TaucoinListener listener : listeners) {
            listener.onStatesLoadedFailed(tagHeight);
        }
    }
}
