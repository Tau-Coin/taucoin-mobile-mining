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

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import io.fabric.sdk.android.Fabric;
import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.service.TaucoinServiceMessage;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.TransactionExecuatedEvent;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.core.TransactionExecuatedOutcome;

public class RemoteService extends TaucoinRemoteService {
    private static final Logger logger = LoggerFactory.getLogger("RemoteService");
    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private ServiceConnection serviceConnection;

    public RemoteService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        // Crashlytics
        Fabric.with(this, new Crashlytics());

        sendNotify();
        serviceConnection = new RemoteServiceConnection();
        startLocalService();
        logger.debug("RemoteService onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            NotifyManager.NotifyData mData = intent.getParcelableExtra("bean");
            if(mData != null){
                mData.miningState = TransmitKey.MiningState.Start;
            }
            sendNotify(mData);
//            int type = intent.getIntExtra(TransmitKey.SERVICE_TYPE, -1);
//            if(type == TaucoinServiceMessage.MSG_CLOSE_MINING_PROGRESS){
//                android.os.Process.killProcess(android.os.Process.myPid());
//                System.exit(0);
//            }
        }else{
            sendNotify();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendNotify() {
        NotifyManager.NotifyData mData = new NotifyManager.NotifyData();
        sendNotify(mData);
    }

    private void sendNotify(NotifyManager.NotifyData mData) {
        NotifyManager.getInstance().sendNotify(this, builder, mData);
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

    @Override
    protected void broadcastEvent(EventFlag event, EventData data) {
        super.broadcastEvent(event, data);
        if(event != null && event == EventFlag.EVENT_TRANSACTION_EXECUATED){
            TransactionExecuatedEvent txRewards = (TransactionExecuatedEvent) data;
            if(txRewards != null && txRewards.outcome != null){
                TransactionExecuatedOutcome outcome = txRewards.outcome;
                long rewards = 0L;
                rewards += parseRewards(outcome.getLastWintess());
                rewards += parseRewards(outcome.getSenderAssociated());

                long minerRewards = parseRewards(outcome.getCurrentWintess());
                int notifyRes = -1;
                if(rewards > 0){
                    if(minerRewards > 0){
                        notifyRes = R.string.income_miner_participant;
                    }else{
                        notifyRes = R.string.income_participant;
                    }
                }else if(minerRewards > 0){
                    notifyRes = R.string.income_miner;
                }
                rewards += minerRewards;
                if(rewards > 0 && notifyRes > 0){
                    String notifyStr = getText(notifyRes).toString();
                    String rewardStr = FmtMicrometer.fmtFormat(String.valueOf(rewards));
                    notifyStr = String.format(notifyStr, rewardStr);
                    NotifyManager.getInstance().sendBlockNotify(this, mNotificationManager, builder, notifyStr);
                }
            }
        }else if(event != null && event == EventFlag.EVENT_BLOCK_DISCONNECT){
            String rollback = getText(R.string.income_miner_rollback).toString();
            NotifyManager.getInstance().sendBlockNotify(this, mNotificationManager, builder, rollback);
        }
    }

    private long parseRewards(Map<byte[],Long> map) {
        long rewards = 0;
        if(map != null && map.size() > 0){
            for (Map.Entry<byte[], Long> entry : map.entrySet()) {
                rewards += entry.getValue();
            }
        }
        return rewards;
    }

    private void init() {
        initNotificationManager();
    }

    private void initNotificationManager() {
        // notification manager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = NotifyManager.getInstance().createNotificationBuilder(this, mNotificationManager);
    }

    class RemoteServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logger.debug("RemoteService onServiceDisconnected");
            startLocalService();
        }
    }

    private void startLocalService() {
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        bindService(new Intent(this, TxService.class), serviceConnection, BIND_AUTO_CREATE);
    }
}