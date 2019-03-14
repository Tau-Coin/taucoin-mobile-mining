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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.github.naturs.logger.Logger;
import com.mofei.tau.R;

import java.util.Date;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.module.view.SplashActivity;
import io.taucoin.android.wallet.module.view.main.MainActivity;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.ActivityManager;
import io.taucoin.foundation.util.StringUtil;

public class NotifyManager {

    static final String ACTION_NOTIFICATION_MINING = "tau.intent.action.notify.mining";
    static final String ACTION_NOTIFICATION_ONGOING = "tau.intent.action.notify.ongoing";

    static final int NOTIFICATION_ID = 0x99;

    private android.app.NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Service mService;
    private NotifyData mNotifyData;
    private ResManager mResManager;

    @SuppressLint("StaticFieldLeak")
    private static NotifyManager mInstance;

    public static synchronized NotifyManager getInstance(){
        if(mInstance == null){
            synchronized (NotifyManager.class){
                if(mInstance == null){
                    mInstance = new NotifyManager();
                }
            }
        }
        return mInstance;
    }

    public synchronized NotifyData getNotifyData(){
        return mNotifyData;
    }

    void initNotificationManager(Service service) {
        mService = service;
        mNotificationManager = (android.app.NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel();

        Intent intent = new Intent(service, TxService.class);
        intent.setAction(ACTION_NOTIFICATION_ONGOING);
        int id = (int) (System.currentTimeMillis() / 1000);
        PendingIntent pendingIntent = PendingIntent.getService(service, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(service, channelId);
        mBuilder.setContentTitle(service.getString(R.string.app_name));
        mBuilder.setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            mBuilder.setSmallIcon(R.mipmap.icon_notify_logo);
        }else{
            mBuilder.setSmallIcon(service.getApplicationInfo().icon);
        }
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setOngoing(true);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setSound(null);
    }


    private String createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "tauChannelId";
            CharSequence channelName = "tauChannelName";
            String channelDescription ="tauChannelDescription";
            int channelImportance = android.app.NotificationManager.IMPORTANCE_DEFAULT;

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

    void initNotify() {
        mNotifyData = new NotifyData();
        mNotifyData.miningState = TransmitKey.MiningState.Stop;
        sendNotify();
        mResManager = new ResManager();
        mResManager.startResThread(new ResManager.ResCallBack(){

            @Override
            void updateCpuAndMemory(String cpuInfo, String memoryInfo) {
                mNotifyData.cpuUsage = cpuInfo;
                mNotifyData.memorySize = memoryInfo;
                sendNotify();
            }

            @Override
            void updateDataSize(String dataInfo) {
                mNotifyData.dataSize = dataInfo;
                sendNotify();
            }
        });
    }

    public void sendNotify(String miningState) {
        if(mNotifyData != null){
            mNotifyData.miningState = miningState;
            sendNotify();
        }

    }

    private synchronized void sendNotify() {
        if(mService == null || mNotifyData == null){
            return;
        }
        RemoteViews remoteViews = new RemoteViews(mService.getPackageName(), R.layout.notification_notice);
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

        mBuilder.setCustomContentView(remoteViews);
        Intent intent = new Intent(mService, TxService.class);
        intent.setAction(ACTION_NOTIFICATION_MINING);
        intent.putExtra(TransmitKey.SERVICE_TYPE, mNotifyData.miningState);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(mService, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }else{
            pendingIntent = PendingIntent.getService(mService, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        remoteViews.setOnClickPendingIntent(R.id.iv_mining, pendingIntent);

        Notification mNotification = mBuilder.build();
        mNotification.flags = Notification.FLAG_NO_CLEAR;
        mService.startForeground(NOTIFICATION_ID, mNotification);
    }

    public void sendBlockNotify(String reward) {
        if(mService == null){
            return;
        }
        reward = FmtMicrometer.fmtAmount(reward);
        String msg = MyApplication.getInstance().getString(R.string.mining_new_block);
        msg = String.format(msg, reward);

        int notifyId = (int) (System.currentTimeMillis() / 1000);
        RemoteViews remoteViews = new RemoteViews(mService.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, mService.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, mService.getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.tv_tip, msg);

        long time = new Date().getTime();
        remoteViews.setTextViewText(R.id.tv_time, DateUtil.format(time, DateUtil.pattern0));
        mBuilder.setCustomContentView(remoteViews);
        mBuilder.setSound(null);
        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(notifyId, notification);
    }

    void cancelNotify(){
        if(mService == null){
            return;
        }
        if(mNotificationManager != null){
            mNotificationManager.cancelAll();
        }
        if(mResManager != null){
            mResManager.stopResThread();
        }
        mService.stopForeground(true);
        mService = null;
        mInstance = null;
    }

    void handlerNotifyClickEvent(String action, String serviceType){
        if(StringUtil.isEmpty(action)){
            return;
        }
        Logger.d("notify_onClock=\taction:" + action + "\tserviceType=" + serviceType);
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            gotoImportKeyActivity();
            return;
        }
        switch (action){
            case ACTION_NOTIFICATION_ONGOING:
                gotoMainActivity();
                break;
            case ACTION_NOTIFICATION_MINING:
                if(StringUtil.isNotSame(serviceType, TransmitKey.MiningState.LOADING)){
                    sendNotify(TransmitKey.MiningState.LOADING);
                    updateMiningState();
                    Logger.d("NotificationReceiver immediate enter MainActivity");
                }
                break;
            default:
                break;
        }
    }

    private void gotoImportKeyActivity() {
        Context contextApp = MyApplication.getInstance();
        if(ActivityManager.getInstance().getActivitySize() > 0){
            Logger.d("Notification immediate enter MainActivity");
            Intent intentMain = new Intent(contextApp, ImportKeyActivity.class);
            intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = ActivityManager.getInstance().currentActivity();
            if(activity != null){
                intentMain = new Intent(activity, ImportKeyActivity.class);
                activity.startActivityForResult(intentMain, 100);
            }else {
                contextApp.startActivity(intentMain);
            }
        }else{
            Logger.d("Notification restart app");
            Intent intentSplash = new Intent(contextApp, SplashActivity.class);
            intentSplash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            contextApp.startActivity(intentSplash);
        }
    }

    private void gotoMainActivity() {
        Context contextApp = MyApplication.getInstance();
        if(contextApp != null && MyApplication.getInstance().isBackground()){
            if(ActivityManager.getInstance().getActivitySize() > 0){
                Logger.d("Notification immediate enter MainActivity");
                Intent intentMain = new Intent(contextApp, MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                contextApp.startActivity(intentMain);
            }else{
                Logger.d("Notification restart app");
                Intent intentSplash = new Intent(contextApp, SplashActivity.class);
                intentSplash.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                contextApp.startActivity(intentSplash);
            }
        }
    }

    private void updateMiningState() {
        EventBusUtil.post(MessageEvent.EventCode.NOTIFY_MINING);
        new MiningModel().updateMiningState(new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                boolean isStart = false;
                KeyValue keyValue = MyApplication.getKeyValue();
                if (keyValue != null) {
                    isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                }
                if(isStart){
                    MyApplication.getRemoteConnector().init();
                }else{
                    MyApplication.getRemoteConnector().cancelRemoteConnector();
                }
                EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
            }
        });
    }

    static class NotifyData implements Parcelable {
        String cpuUsage;
        String memorySize;
        String dataSize;
        String miningState;

        NotifyData() {}

        NotifyData(Parcel in) {
            cpuUsage = in.readString();
            memorySize = in.readString();
            dataSize = in.readString();
            miningState = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(cpuUsage);
            dest.writeString(memorySize);
            dest.writeString(dataSize);
            dest.writeString(miningState);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<NotifyData> CREATOR = new Creator<NotifyData>() {
            @Override
            public NotifyData createFromParcel(Parcel in) {
                return new NotifyData(in);
            }

            @Override
            public NotifyData[] newArray(int size) {
                return new NotifyData[size];
            }
        };
    }
}