package io.taucoin.android.di.components;

import android.content.Context;

import io.taucoin.android.di.modules.TaucoinModule;
import io.taucoin.android.Taucoin;
import io.taucoin.http.ConnectionManager;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = TaucoinModule.class)
public interface TaucoinComponent {

    Context context();
    Taucoin taucoin();
    ConnectionManager connectionManager();

}
