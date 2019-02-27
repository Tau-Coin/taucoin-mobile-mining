package io.taucoin.android.wallet.module.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.mofei.tau.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.util.ToastUtils;

public class TauNotificationManager {
    // notification manager
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;
    private Notification notification;
    private Context context;

    private boolean isInit = false;
    static final int NOTIFICATION_ID = 0x99;
    @SuppressLint("StaticFieldLeak")
    private static TauNotificationManager tauNotificationManager;

    private TauNotificationManager(Context context) {
        isInit = false;
        this.context = context;
    }

    public static synchronized TauNotificationManager getInstance() {
        synchronized(TauNotificationManager.class){
            if (null == tauNotificationManager) {
                tauNotificationManager = new TauNotificationManager(MyApplication.getInstance());
            }
        }
        return tauNotificationManager;
    }

    public void init() {
        ToastUtils.showShortToast("init()");
        isInit = true;
        initNotificationManager();
    }

    public void close() {
        if (null != tauNotificationManager) {
            tauNotificationManager = null;
        }
        isInit = false;
    }

    private void initNotificationManager() {
        mNotificationManager = (NotificationManager) tauNotificationManager.context.getSystemService(Service.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(TransmitKey.ID, NOTIFICATION_ID);
        int id = (int) (System.currentTimeMillis() / 1000);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(context, "default");
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(context.getApplicationInfo().icon);
        builder.setContentIntent(pendingIntent);
    }

    public void showMiningNotify() {
        if(!isInit){
            return;
        }
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, context.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, context.getString(R.string.notify_mining));
        remoteViews.setTextViewText(R.id.tv_tip, context.getString(R.string.notify_tip));
        builder.setCustomContentView(remoteViews);
        notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void showBlockNotify() {
        if(!isInit){
            return;
        }
        int notifyId = (int) (System.currentTimeMillis() / 1000);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, context.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, context.getString(R.string.notify_mining));
        remoteViews.setTextViewText(R.id.tv_tip, context.getString(R.string.notify_tip));
        builder.setCustomContentView(remoteViews);
        notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(notifyId, notification);
    }

    public void cancelMiningNotify(){
        if(!isInit){
            return;
        }
        if (mNotificationManager != null){
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void updateNotify() {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, context.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, "mining");
        builder.setCustomContentView(remoteViews);
        notification = builder.build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void failNotify() {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, context.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, "mining");
        builder.setCustomContentView(remoteViews);
        notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

}
