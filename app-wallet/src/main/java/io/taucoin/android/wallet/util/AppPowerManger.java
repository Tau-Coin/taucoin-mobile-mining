package io.taucoin.android.wallet.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;

public class AppPowerManger {

    private static PowerManager.WakeLock mWakeLock = null;

    @SuppressLint("WakelockTimeout")
    public static void acquireWakeLock(Context context) {
        Context applicationContext = context.getApplicationContext();
        if(mWakeLock == null){
            PowerManager mPowerManager = (PowerManager) applicationContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, applicationContext.getClass().getCanonicalName());
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
