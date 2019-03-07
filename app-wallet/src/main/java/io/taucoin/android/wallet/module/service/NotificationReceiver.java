package io.taucoin.android.wallet.module.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.naturs.logger.Logger;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.module.view.SplashActivity;
import io.taucoin.android.wallet.module.view.main.MainActivity;
import io.taucoin.foundation.util.ActivityManager;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        if(intent != null){
////            int notifyId = intent.getIntExtra(TransmitKey.ID, -1);
////            if(notifyId != RemoteService.NOTIFICATION_ID){
////                MyApplication.getRemoteConnector().cancelMiningNotify();
//            }
//        }
        Context contextApp = MyApplication.getInstance();
        if(contextApp != null && MyApplication.getInstance().isBackground()){
            if(ActivityManager.getInstance().getActivitySize() > 0){
                Logger.d("NotificationReceiver immediate enter MainActivity");
                Intent intentMain = new Intent(contextApp, MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                contextApp.startActivity(intentMain);
            }else{
                Logger.d("NotificationReceiver restart app");
                Intent intentSplash = new Intent(contextApp, SplashActivity.class);
                intentSplash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                contextApp.startActivity(intentSplash);
            }
        }
    }
}