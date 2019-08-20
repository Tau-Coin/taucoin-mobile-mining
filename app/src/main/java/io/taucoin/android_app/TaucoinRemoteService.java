package io.taucoin.android_app;


import android.content.Intent;

import io.taucoin.android.kotlin.IPFSManager;

public class TaucoinRemoteService extends io.taucoin.android.service.TaucoinRemoteService {

    protected IPFSManager mIPFSManager;

    public TaucoinRemoteService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIPFSManager = new IPFSManager(this);
        mIPFSManager.init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(mIPFSManager != null){
            mIPFSManager.onStartCommand(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mIPFSManager != null){
            mIPFSManager.stop();
        }
    }
}
