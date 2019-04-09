package io.taucoin.android.di.components;

import android.content.Context;

import io.taucoin.android.di.modules.TaucoinModule;
import io.taucoin.android.Taucoin;
import io.taucoin.manager.WorldManager;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.sync.PeersPool;
import io.taucoin.validator.ParentBlockHeaderValidator;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = TaucoinModule.class)
public interface TaucoinComponent {

    Context context();
    Taucoin taucoin();
    //ChannelManager channelManager();
    WorldManager worldManager();
    ParentBlockHeaderValidator parentBlockHeaderValidator();
    //PeersPool peersPool();
}
