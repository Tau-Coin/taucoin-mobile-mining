package io.taucoin.android.wallet.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;

public class AppWifiManger {

    private static WifiManager.WifiLock mWakeLock = null;

    @SuppressLint("WakelockTimeout")
    public static void acquireWakeLock(Context context) {

        Context applicationContext = context.getApplicationContext();
        if(mWakeLock == null){
            WifiManager mWifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
            mWakeLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, applicationContext.getClass().getCanonicalName());
            mWakeLock.setReferenceCounted(false);
        }
        if(mWakeLock != null && !mWakeLock.isHeld()){
            mWakeLock.acquire();
        }
    }

    public static void releaseWakeLock() {
        if(mWakeLock != null && mWakeLock.isHeld()){
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
