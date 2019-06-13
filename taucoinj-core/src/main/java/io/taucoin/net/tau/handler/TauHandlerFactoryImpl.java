package io.taucoin.net.tau.handler;

import io.taucoin.net.tau.TauVersion;

import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.sync.SyncManager;
import io.taucoin.sync.SyncQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default factory implementation
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
@Singleton
public class TauHandlerFactoryImpl implements TauHandlerFactory {

    Provider<Tau60> tau60Provider;
    Provider<Tau61> tau61Provider;
    Provider<Tau62> tau62Provider;

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected PendingState pendingState;

    protected ChannelManager channelManager;

    @Inject
    public TauHandlerFactoryImpl(Provider<Tau60> tau60Provider, Provider<Tau61> tau61Provider, Provider<Tau62> tau62Provider,
            Blockchain blockchain, BlockStore blockstore, SyncManager syncManager,
            SyncQueue queue,
            PendingState pendingState, ChannelManager channelManager) {
        this.tau60Provider = tau60Provider;
        this.tau61Provider = tau61Provider;
        this.tau62Provider = tau62Provider;

        this.blockchain = blockchain;
        this.blockstore = blockstore;
        this.syncManager = syncManager;
        this.queue = queue;
        this.pendingState = pendingState;
        this.channelManager = channelManager;
    }

    @Override
    public TauHandler create(TauVersion version) {
        TauHandler handler;
        switch (version) {
            case V60:
                handler = tau60Provider.get();
                break;
            case V61:
                handler = tau61Provider.get();
                break;
            case V62:
                handler = tau62Provider.get();
                break;
            default:    throw new IllegalArgumentException("Eth " + version + " is not supported");
        }

        handler.init(blockchain, blockstore, syncManager, queue, pendingState, channelManager);
        return handler;
    }
}
