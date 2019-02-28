package io.taucoin.android.wallet.module.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.view.main.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null){
//            int notifyId = intent.getIntExtra(TransmitKey.ID, -1);
//            if(notifyId != RemoteService.NOTIFICATION_ID){
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(RemoteService.NOTIFICATION_ID);
//            }
        }
        if(MyApplication.getInstance().isBackground()){
            Intent intentMain = new Intent(context, MainActivity.class);
            context.startActivity(intentMain);
        }
    }
}