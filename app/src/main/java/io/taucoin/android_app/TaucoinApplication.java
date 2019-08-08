package io.taucoin.android_app;

import android.support.multidex.MultiDexApplication;

import io.taucoin.android_app.net.NetWorkManager;

public class TaucoinApplication extends MultiDexApplication {

    private static TaucoinApplication instance;
    private static RemoteConnectorManager mRemoteConnector;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        NetWorkManager.getInstance().init();
        mRemoteConnector = new RemoteConnectorManager();
    }

    public static TaucoinApplication getInstance(){
        return instance;
    }

    public static RemoteConnectorManager getRemoteConnector(){
        return mRemoteConnector;
    }
}
