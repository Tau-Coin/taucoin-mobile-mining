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
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.service.TaucoinServiceMessage;
import io.taucoin.android.wallet.base.TransmitKey;

public class RemoteService extends TaucoinRemoteService {
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
            NotifyManager.getInstance().sendNotify(this, builder, mData);

            int type = intent.getIntExtra(TransmitKey.SERVICE_TYPE, -1);
            if(type == TaucoinServiceMessage.MSG_CLOSE_MINING_PROGRESS){
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
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
        // notification manager
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = NotifyManager.getInstance().createNotificationBuilder(this, mNotificationManager);
    }
}