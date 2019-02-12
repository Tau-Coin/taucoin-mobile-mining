package io.taucoin.android_app;

import android.support.multidex.MultiDexApplication;

public class TaucoinApplication extends MultiDexApplication {

    private static TaucoinApplication instance;
    private static RemoteConnectorManager mRemoteConnector;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        mRemoteConnector = new RemoteConnectorManager();
    }

    public static TaucoinApplication getInstance(){
        return instance;
    }

    public static RemoteConnectorManager getRemoteConnector(){
        return mRemoteConnector;
    }
}
