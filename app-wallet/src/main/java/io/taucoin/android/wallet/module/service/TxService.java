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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.github.naturs.logger.Logger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.ChainBean;
import io.taucoin.android.wallet.module.bean.ChainDetail;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.bean.RewardInfoBean;
import io.taucoin.android.wallet.module.model.AppModel;
import io.taucoin.android.wallet.module.model.IAppModel;
import io.taucoin.android.wallet.module.model.ITxModel;
import io.taucoin.android.wallet.module.model.TxModel;
import io.taucoin.android.wallet.module.view.main.HomeFragment;
import io.taucoin.android.wallet.module.view.manage.CongratulationActivity;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.util.AppPowerManger;
import io.taucoin.android.wallet.util.AppWifiManger;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.net.callback.NetResultCode;
import io.taucoin.foundation.util.ActivityManager;
import io.taucoin.foundation.util.StringUtil;

public class TxService extends Service {

    private ITxModel mTxModel;
    private IAppModel mAppModel;
    private boolean mIsChecked;
    private boolean mIsGetBalance;
    private boolean mIsGetBlockHeight;

    public TxService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTxModel = new TxModel();
        mAppModel = new AppModel();
        mIsChecked = false;
        mIsGetBalance = false;
        mIsGetBlockHeight = false;
        NotifyManager.getInstance().initNotificationManager(this);
        NotifyManager.getInstance().initNotify();
        Logger.i("TxService onCreate");
        AppPowerManger.acquireWakeLock(this);
        AppWifiManger.acquireWakeLock(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MyApplication.getRemoteConnector().restoreConnection();
        NotifyManager.getInstance().sendNotify(this);
        KeyValue keyValue = MyApplication.getKeyValue();
        if(intent != null){
            String action = intent.getAction();
            String serviceType = intent.getStringExtra(TransmitKey.SERVICE_TYPE);
            serviceType = StringUtil.isNotEmpty(serviceType) ? serviceType : "";
            if(StringUtil.isNotEmpty(action) || keyValue == null){
                NotifyManager.getInstance().handlerNotifyClickEvent(action, serviceType);
                return super.onStartCommand(intent, flags, startId);
            }
            switch (serviceType){
                case TransmitKey.ServiceType.GET_HOME_DATA:
                    if(!mIsGetBalance){
                        getBalance(serviceType);
                        getMinerInfo(true);
                        getRankInfo(true);
                    }
                    if(!mIsChecked){
                        checkRawTransaction();
                    }
                    break;
                case TransmitKey.ServiceType.GET_SEND_DATA:
                    getBalance(serviceType);
                    break;
                case TransmitKey.ServiceType.GET_BALANCE:
                    getBalance(serviceType);
                    getMinerInfo(false);
                    getRankInfo(false);
                    break;
                case TransmitKey.ServiceType.GET_RAW_TX:
                    if(!mIsChecked){
                        checkRawTransactionDelay();
                    }
                    break;
                case TransmitKey.ServiceType.GET_INFO:
                    getInfo();
                    break;
                case TransmitKey.ServiceType.GET_BLOCK_HEIGHT:
                    getBlockHeight(!mIsGetBlockHeight);
                    break;
                case TransmitKey.ServiceType.GET_REWARD_INFO:
                    getRewardInfo();
                    break;
                default:
                    break;
            }
            Logger.i("TxService onStartCommand, ServiceType=" + serviceType);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void getInfo() {
        mAppModel.getInfo();
    }

    private void checkRawTransactionDelay() {
        mIsChecked = true;
        Observable.timer(2 * 60, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    checkRawTransaction();
                }
            });
    }

    private void checkRawTransaction() {
        mIsChecked = true;
        mTxModel.getTxPendingListDelay(new LogicObserver<List<List<String>>>(){

            @Override
            public void handleData(List<List<String>> txIdsList) {
                if(txIdsList != null){
                    Logger.d("checkRawTransaction start size=" + txIdsList.size());
                    for (int i = 0; i < txIdsList.size(); i++) {
                        try {
                            mTxModel.checkRawTransaction(txIdsList.get(i), new LogicObserver<Boolean>(){

                                @Override
                                public void handleData(Boolean isRefresh) {
                                    if(isRefresh){
                                        EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
                                        getBalance(TransmitKey.ServiceType.GET_BALANCE);
                                    }
                                }
                            });
                            Thread.sleep(3000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Logger.d("checkRawTransaction end");
                    checkRawTransactionDelay();
                }else{
                    mIsChecked = false;
                }
            }
        });
    }

    private void getBalanceDelay(String serviceType) {
        mIsChecked = true;
        mIsGetBalance = true;
        Observable.timer(30, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    getBalance(serviceType);
                }
            });
    }

    private void getBalance(String serviceType) {
        mIsGetBalance = true;
        getIncomeInfo();
        mTxModel.getBalance(new LogicObserver<KeyValue>() {

            @Override
            public void handleError(int msgCode, String msg) {
                handleBalanceDisplay(serviceType, false);
            }

            @Override
            public void handleData(KeyValue entry) {
                if(entry != null){
                    Logger.i("getBalance success");
                    MyApplication.setKeyValue(entry);
                    handleBalanceDisplay(serviceType, true);
                }else{
                    handleBalanceDisplay(serviceType, false);
                }
            }
        });
    }

    private void getIncomeInfo() {
        mTxModel.getIncomeInfo(new LogicObserver<BlockInfo>(){

            @Override
            public void handleData(BlockInfo blockInfo) {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setData(blockInfo);
                messageEvent.setCode(MessageEvent.EventCode.MINING_INCOME);
                EventBusUtil.post(messageEvent);
            }
        });
    }

    private void getMinerInfoDelay() {
        Observable.timer(2, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .subscribe(new CommonObserver<Long>() {
                    @Override
                    public void onComplete() {
                        getMinerInfo(true);
                    }
                });
    }

    private void getMinerInfo(boolean isDelay) {
        mTxModel.getMinerInfo(new LogicObserver<KeyValue>(){

            @Override
            public void handleError(int code, String msg) {
                if(isDelay){
                    getMinerInfoDelay();
                }
            }

            @Override
            public void handleData(KeyValue keyValue) {
                MyApplication.setKeyValue(keyValue);
                EventBusUtil.post(MessageEvent.EventCode.BALANCE);
                if(isDelay){
                    getMinerInfoDelay();
                }
            }
        });
    }

    private void getRankInfoDelay() {
        Observable.timer(5, TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    getRankInfo(true);
                }
            });
    }

    private void getRankInfo(boolean isDelay) {
        mTxModel.getRankInfo(new LogicObserver<KeyValue>(){
            @Override
            public void handleError(int code, String msg) {
                if(isDelay){
                    getRankInfoDelay();
                }
            }
            @Override
            public void handleData(KeyValue keyValue) {
                MyApplication.setKeyValue(keyValue);
                EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                if(isDelay){
                    getRankInfoDelay();
                }
            }
        });
    }

    private void handleBalanceDisplay(String serviceType, boolean isSuccess) {
        ProgressManager.closeProgressDialog();
        if(StringUtil.isSame(serviceType, TransmitKey.ServiceType.GET_HOME_DATA)){
            if(isSuccess){
                EventBusUtil.post(MessageEvent.EventCode.ALL);
            }
            getBalanceDelay(serviceType);
        }else{
            if(StringUtil.isSame(serviceType, TransmitKey.ServiceType.GET_BALANCE) && !isSuccess
                    && HomeFragment.mIsToast){
                ToastUtils.showShortToast(R.string.common_refresh_failed);
                HomeFragment.mIsToast = false;
            }
            EventBusUtil.post(MessageEvent.EventCode.BALANCE);
        }
    }

    private void getBlockHeight(boolean isDelayRefresh){
        mIsGetBlockHeight = true;
        mTxModel.getBlockHeight(new TxObserver<ChainBean>(){

            @Override
            public void handleData(ChainBean result) {
                if(result != null && result.getStatus() == NetResultCode.MAIN_SUCCESS_CODE &&
                        result.getPayLoad() != null){
                    ChainDetail chainDetail = result.getPayLoad();
                    Logger.d("getBlockHeight =" + chainDetail.getTotalHeight());
                    int blockHeight = chainDetail.getTotalHeight();
                    mTxModel.updateBlockHeight(blockHeight, new LogicObserver<Boolean>() {
                        @Override
                        public void handleData(Boolean keyValue) {
                            EventBusUtil.post(MessageEvent.EventCode.BLOCK_HEIGHT);
                        }
                    });
                }
                if(isDelayRefresh){
                    getBlockHeightDelay();
                }
            }

            @Override
            public void handleError(String msg, int msgCode) {
                if(isDelayRefresh){
                    getBlockHeightDelay();
                }
            }
        });
    }

    private void getBlockHeightDelay() {
        Observable.timer(5, TimeUnit.MINUTES)
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    getBlockHeight(true);
                }
            });
    }

    private void getRewardInfo() {
        mTxModel.getRewardInfo(new TxObserver<RewardInfoBean>() {
            @Override
            public void handleData(RewardInfoBean rewardInfo) {
                super.handleData(rewardInfo);
                if(rewardInfo != null){
                    Context context = ActivityManager.getInstance().currentActivity();
                    Intent intent = new Intent(context, CongratulationActivity.class);
                    intent.putExtra(TransmitKey.BEAN, rewardInfo);
                    context.startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        Logger.i("TxService onDestroy");
        NotifyManager.getInstance().cancelNotify();
        AppPowerManger.releaseWakeLock();
        AppWifiManger.releaseWakeLock();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotifyManager.getInstance().cancelNotify();
        AppPowerManger.releaseWakeLock();
        AppWifiManger.releaseWakeLock();
        super.onTaskRemoved(rootIntent);
    }

    public static void startTxService(String serviceType){
        Intent intent = new Intent();
        intent.putExtra(TransmitKey.SERVICE_TYPE, serviceType);
        startTxService(intent);
    }

    public static void startTxService(Intent intent){
        Context context = MyApplication.getInstance();
        intent.setClass(context, TxService.class);
        context.startService(intent);
    }

    public static void stopService() {
        Context context = MyApplication.getInstance();
        Intent intent = new Intent();
        intent.setClass(context, TxService.class);
        context.stopService(intent);
    }
}