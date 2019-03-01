package io.taucoin.android.wallet.module.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.mofei.tau.R;

import java.util.Date;

import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.foundation.util.StringUtil;

public class RemoteService extends TaucoinRemoteService {
    // notification manager
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;
    private Notification notification;

    public static final int NOTIFICATION_ID = 0x99;

    public RemoteService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    protected boolean handleMessage(Message message) {
        switch (message.what) {
            case TaucoinClientMessage.MSG_SEND_MINING_NOTIFY:
                sendMiningNotify(message.getData());
                break;
            case TaucoinClientMessage.MSG_CLOSE_MINING_NOTIFY:
                cancelMiningNotify();
                break;
            case TaucoinClientMessage.MSG_SEND_BLOCK_NOTIFY:
                sendBlockNotify(message.getData());
                break;
            default:
                return super.handleMessage(message);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        cancelMiningNotify();
        if(mNotificationManager != null){
            mNotificationManager.cancelAll();
        }
        super.onDestroy();
    }

    private void init() {
        initNotificationManager();
    }

    private void initNotificationManager() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel();

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra(TransmitKey.ID, NOTIFICATION_ID);
        int id = (int) (System.currentTimeMillis() / 1000);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(getApplicationInfo().icon);
        builder.setContentIntent(pendingIntent);
    }

    public String createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "tauChannelId";
            CharSequence channelName = "tauChannelName";
            String channelDescription ="tauChannelDescription";
            int channelImportance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, channelImportance);
            // Set description up to 30 characters
            notificationChannel.setDescription(channelDescription);
            // Whether the channel notification uses vibration or not
            notificationChannel.enableVibration(false);
            // Setting Display Mode
            notificationChannel.setSound(null, null);
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mNotificationManager.createNotificationChannel(notificationChannel);
            return channelId;
        } else {
            return "default";
        }
    }

    private void sendMiningNotify(Bundle bundle) {
        if(bundle != null){
            String msg = bundle.getString("data", "");
            if(StringUtil.isNotEmpty(msg)){
                sendMiningNotify(msg);
            }
        }
    }

    private void sendMiningNotify(String msg) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.tv_tip, msg);
        builder.setCustomContentView(remoteViews);
        builder.setSmallIcon(getApplicationInfo().icon);
        builder.setSound(null);
        notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForeground(NOTIFICATION_ID, notification);
//        }else{
//            mNotificationManager.notify(NOTIFICATION_ID, notification);
//        }
        startForeground(NOTIFICATION_ID, notification);
    }

    private void sendBlockNotify(Bundle bundle) {
        if(bundle != null){
            String msg = bundle.getString("data", "");
            if(StringUtil.isNotEmpty(msg)){
                sendBlockNotify(msg);
            }
        }
    }

    private void sendBlockNotify(String msg) {

        int notifyId = (int) (System.currentTimeMillis() / 1000);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.tv_tip, msg);

        long time = new Date().getTime();
        remoteViews.setTextViewText(R.id.tv_time, DateUtil.format(time, DateUtil.pattern0));
        builder.setCustomContentView(remoteViews);
        builder.setSound(null);
        notification = builder.build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(notifyId, notification);
    }

    private void cancelMiningNotify(){
        stopForeground(true);
    }
}