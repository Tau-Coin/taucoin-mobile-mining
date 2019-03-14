/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.module.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.mofei.tau.R;

import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.service.TaucoinServiceMessage;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.foundation.util.StringUtil;

public class RemoteService extends TaucoinRemoteService {
    // notification manager
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;

    public RemoteService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            NotifyManager.NotifyData mData = intent.getParcelableExtra("bean");
            initNotify(mData);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected boolean handleMessage(Message message) {
        switch (message.what) {
            case TaucoinServiceMessage.MSG_SEND_MINING_NOTIFY:
                break;
            case TaucoinServiceMessage.MSG_CLOSE_MINING_NOTIFY:
                break;
            case TaucoinServiceMessage.MSG_CLOSE_MINING_PROGRESS:
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                break;
            case TaucoinServiceMessage.MSG_SEND_BLOCK_NOTIFY:
                break;
            default:
                return super.handleMessage(message);
        }
        return true;
    }

    private void init() {
        initNotificationManager();
    }

    private void initNotificationManager() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel();

        Intent intent = new Intent(this, TxService.class);
        intent.setAction(NotifyManager.ACTION_NOTIFICATION_ONGOING);
        int id = (int) (System.currentTimeMillis() / 1000);
        PendingIntent pendingIntent = PendingIntent.getService(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(getApplicationInfo().icon);
        builder.setContentIntent(pendingIntent);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            builder.setSmallIcon(R.mipmap.icon_notify_logo);
        }else{
            builder.setSmallIcon(getApplicationInfo().icon);
        }
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

    private void initNotify(NotifyManager.NotifyData mNotifyData) {
        if(mNotifyData == null){
            return;
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_notice);
        if(StringUtil.isNotEmpty(mNotifyData.cpuUsage)){
            remoteViews.setTextViewText(R.id.tv_cpu, mNotifyData.cpuUsage);
        }
        if(StringUtil.isNotEmpty(mNotifyData.memorySize)){
            remoteViews.setTextViewText(R.id.tv_memory, mNotifyData.memorySize);
        }
        if(StringUtil.isNotEmpty(mNotifyData.dataSize)){
            remoteViews.setTextViewText(R.id.tv_data, mNotifyData.dataSize);
        }
        int miningState = R.mipmap.icon_end;
        boolean isLoading = false;
        if(StringUtil.isSame(mNotifyData.miningState, TransmitKey.MiningState.Start)){
            miningState = R.mipmap.icon_start;
        }else if(StringUtil.isSame(mNotifyData.miningState, TransmitKey.MiningState.LOADING)){
            isLoading = true;
        }
        remoteViews.setImageViewResource(R.id.iv_mining, miningState);

        remoteViews.setViewVisibility(R.id.iv_mining_loading, isLoading ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.iv_mining, !isLoading ? View.VISIBLE : View.GONE);
        remoteViews.setImageViewResource(R.id.iv_mining, miningState);

        builder.setCustomContentView(remoteViews);
        Intent intent = new Intent(this, TxService.class);
        intent.setAction(NotifyManager.ACTION_NOTIFICATION_MINING);
        intent.putExtra(TransmitKey.SERVICE_TYPE, mNotifyData.miningState);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }else{
            pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        remoteViews.setOnClickPendingIntent(R.id.iv_mining, pendingIntent);

        Notification mNotification = builder.build();
        mNotification.flags = Notification.FLAG_NO_CLEAR;
        startForeground(NotifyManager.NOTIFICATION_ID, mNotification);
    }
}