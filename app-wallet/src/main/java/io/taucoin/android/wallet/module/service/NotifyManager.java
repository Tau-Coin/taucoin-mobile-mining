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
import android.app.NotificationManager;
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
import io.taucoin.android.wallet.R;

import java.util.Date;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.ActivityManager;
import io.taucoin.foundation.util.StringUtil;

public class NotifyManager {

    private static final String ACTION_NOTIFICATION_MINING = "tau.intent.action.notify.mining";
    private static final String ACTION_NOTIFICATION_ONGOING = "tau.intent.action.notify.ongoing";

    private static final int NOTIFICATION_ID = 0x99;

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
        mBuilder = createNotificationBuilder(service, mNotificationManager);
    }

    NotificationCompat.Builder createNotificationBuilder(Service service, android.app.NotificationManager notificationManager) {
        String channelId = createNotificationChannel(notificationManager);

        Intent intent = new Intent(service, TxService.class);
        intent.setAction(ACTION_NOTIFICATION_ONGOING);
        int id = (int) (System.currentTimeMillis() / 1000);
        PendingIntent pendingIntent = PendingIntent.getService(service, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, channelId);
        builder.setContentTitle(service.getString(R.string.app_name));
        builder.setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            builder.setSmallIcon(R.mipmap.icon_notify_logo);
        }else{
            builder.setSmallIcon(service.getApplicationInfo().icon);
        }
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setSound(null);

        return builder;
    }


    private String createNotificationChannel(android.app.NotificationManager notificationManager) {
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
            notificationManager.createNotificationChannel(notificationChannel);
            return channelId;
        } else {
            return "default";
        }
    }

    void initNotify() {
        mNotifyData = new NotifyData();
        mNotifyData.miningState = TransmitKey.MiningState.Stop;
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            boolean isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
            if(isStart){
                mNotifyData.miningState = TransmitKey.MiningState.Start;
            }
        }
        sendNotify();
        mResManager = new ResManager();
        mResManager.startResThread(new ResManager.ResCallBack(){

            @Override
            void updateCpuAndMemory(String cpuInfo, String memoryInfo, String networkData) {
                mNotifyData.cpuUsage = cpuInfo;
                mNotifyData.memorySize = memoryInfo;
                mNotifyData.netDataSize = networkData;
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
        if(mNotifyData == null){
            mNotifyData = new NotifyData();
        }
        mNotifyData.miningState = miningState;
        sendNotify();

    }

    synchronized void sendNotify(Service service){
        if(mService == null){
            initNotificationManager(service);
            initNotify();
        }else{
            sendNotify();
        }
    }

    private synchronized void sendNotify(){
        if(mService == null){
            return;
        }
        sendNotify(mService, mBuilder, mNotifyData);
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setData(mNotifyData);
        messageEvent.setCode(MessageEvent.EventCode.APPLICATION_INFO);
        EventBusUtil.post(messageEvent);
    }

    synchronized void sendNotify(Service service, NotificationCompat.Builder builder, NotifyManager.NotifyData notifyData) {
        if(service == null){
            return;
        }
        if(notifyData == null){
            notifyData = new NotifyData();
        }
        RemoteViews remoteViews = new RemoteViews(service.getPackageName(), R.layout.notification_notice);
        if(StringUtil.isNotEmpty(notifyData.cpuUsage)){
            remoteViews.setTextViewText(R.id.tv_cpu, notifyData.cpuUsage);
        }
        if(StringUtil.isNotEmpty(notifyData.memorySize)){
            remoteViews.setTextViewText(R.id.tv_memory, notifyData.memorySize);
        }
        if(StringUtil.isNotEmpty(notifyData.netDataSize)){
            remoteViews.setTextViewText(R.id.tv_data, notifyData.netDataSize);
        }
        int miningState = R.mipmap.icon_end;
        boolean isLoading = false;
        if(StringUtil.isSame(notifyData.miningState, TransmitKey.MiningState.Start)){
            miningState = R.mipmap.icon_start;
        }else if(StringUtil.isSame(notifyData.miningState, TransmitKey.MiningState.LOADING)){
            isLoading = true;
        }
        remoteViews.setImageViewResource(R.id.iv_mining, miningState);

        remoteViews.setViewVisibility(R.id.iv_mining_loading, isLoading ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.iv_mining, !isLoading ? View.VISIBLE : View.GONE);

        builder.setCustomContentView(remoteViews);
        Intent intent = new Intent(service, TxService.class);
        intent.setAction(ACTION_NOTIFICATION_MINING);
        intent.putExtra(TransmitKey.SERVICE_TYPE, notifyData.miningState);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(service, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }else{
            pendingIntent = PendingIntent.getService(service, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        remoteViews.setOnClickPendingIntent(R.id.iv_mining, pendingIntent);

        Notification mNotification = builder.build();
        mNotification.flags = Notification.FLAG_NO_CLEAR;
        service.startForeground(NOTIFICATION_ID, mNotification);
    }

    void sendBlockNotify(Service service, NotificationManager notificationManager, NotificationCompat.Builder builder, String rewardMsg) {
        if(service == null || !PermissionUtils.isNotificationEnabled()){
            return;
        }

        int notifyId = (int) (System.currentTimeMillis() / 1000);
        RemoteViews remoteViews = new RemoteViews(service.getPackageName(), R.layout.notification_mining);
        remoteViews.setImageViewResource(R.id.iv_logo, service.getApplicationInfo().icon);
        remoteViews.setTextViewText(R.id.tv_msg, service.getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.tv_tip, rewardMsg);

        long time = new Date().getTime();
        remoteViews.setTextViewText(R.id.tv_time, DateUtil.format(time, DateUtil.pattern0));
        builder.setCustomContentView(remoteViews);
        builder.setSound(null);
        Notification notification = builder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(notifyId, notification);
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
                updateMiningState(serviceType);
                Logger.d("NotificationReceiver immediate enter MainActivity");
                break;
            default:
                break;
        }
    }

    private void gotoImportKeyActivity() {
        Logger.d("Notification immediate enter MainActivity");
        boolean isSuccess = ActivityUtil.moveTaskToFront();
        if(isSuccess){
            Activity activity = ActivityManager.getInstance().currentActivity();
            if(activity != null){
                Intent intentImportKey = new Intent(activity, ImportKeyActivity.class);
                activity.startActivityForResult(intentImportKey, 100);
            }
        }else{
            Logger.d("Notification restart app");
            ActivityUtil.restartAppTask();
        }
    }

    private void gotoMainActivity() {
        MyApplication contextApp = MyApplication.getInstance();
        if(contextApp != null && contextApp.isBackground()){
            Logger.d("Notification immediate enter moveTaskToFront");
            boolean isSuccess = ActivityUtil.moveTaskToFront();
            if(!isSuccess){
                Logger.d("Notification restart app");
                ActivityUtil.restartAppTask();
            }
        }
    }

    private void updateMiningState(String serviceType) {
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            boolean isSyncStart = StringUtil.isSame(keyValue.getSyncState(), TransmitKey.MiningState.Start);
            if(!isSyncStart){
                ToastUtils.showShortToast(R.string.mining_need_sync_open);
                return;
            }
        }
        boolean isOn = StringUtil.isSame(serviceType, TransmitKey.MiningState.Start);
        isOn = !isOn;
        String miningState = isOn ? TransmitKey.MiningState.Start : TransmitKey.MiningState.Stop;
        sendNotify(miningState);
        new MiningModel().updateMiningState(miningState, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                boolean isStart = false;
                KeyValue keyValue = MyApplication.getKeyValue();
                if (keyValue != null) {
                    isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                }
                if(isStart){
                    MyApplication.getRemoteConnector().startBlockForging();
                }else{
                    MyApplication.getRemoteConnector().stopBlockForging();
                }
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setCode(MessageEvent.EventCode.MINING_NOTIFY);
                messageEvent.setData(isStart);
                EventBusUtil.post(messageEvent);
            }
        });
    }

    public static class NotifyData implements Parcelable {
        public String cpuUsage;
        public String memorySize;
        public String netDataSize;
        public String dataSize;
        String miningState;

        NotifyData() {}

        NotifyData(Parcel in) {
            cpuUsage = in.readString();
            memorySize = in.readString();
            netDataSize = in.readString();
            dataSize = in.readString();
            miningState = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(cpuUsage);
            dest.writeString(memorySize);
            dest.writeString(dataSize);
            dest.writeString(miningState);
            dest.writeString(netDataSize);
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