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
package io.taucoin.android.wallet.util;

import android.content.Context;
import android.os.Environment;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.TxModel;
import io.taucoin.android.wallet.module.service.StateTagManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.widget.EditInput;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.AppUtil;

public class MiningUtil {

    public static void setBlockHeight(TextView textView) {
        if(textView == null){
            return;
        }
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            int blockHeight = BlockInfoDaoUtils.getInstance().getBlockHeight();
            emitter.onNext(blockHeight);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new LogicObserver<Integer>() {
                    @Override
                    public void handleData(Integer blockHeight) {
                        Logger.d("UserUtil.setBlockHeight=" + blockHeight);
                        long chainHeight = blockHeight;
                        String chainHeightStr = FmtMicrometer.fmtPower(chainHeight);
                        textView.setText(chainHeightStr);
                    }
                });
    }

    public static String parseBlockTxFee(List<Transaction> txList) {
        BigInteger reward = new BigInteger("0");
        if(txList != null && txList.size() > 0){
            for (Transaction transaction : txList) {
                try{
                    BigInteger fee = new BigInteger(transaction.getFee());
                    reward = reward.add(fee);
                }catch (Exception e){
                    Logger.e(e, "parseBlockTxFee is error");
                }
            }
        }
        return reward.toString();
    }

    public static void saveTransactionFail(String txId, String result) {
        TransactionHistory transactionHistory = new TransactionHistory();
        transactionHistory.setTxId(txId);
        transactionHistory.setResult(TransmitKey.TxResult.FAILED);
        transactionHistory.setMessage(result);
        new TxModel().updateTransactionHistory(transactionHistory, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
            }
        });
    }

    public static void saveTransactionSuccess() {
        ToastUtils.showShortToast(R.string.send_tx_success);
        EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
        EventBusUtil.post(MessageEvent.EventCode.BALANCE);
        checkRawTransaction();
    }

    public static void checkRawTransaction() {
        TxService.startTxService(TransmitKey.ServiceType.GET_RAW_TX);
    }

    public static void sendingBudgetTransaction() {
        TxService.startTxService(TransmitKey.ServiceType.SEND_BUDGET_TX);
    }

    public static void initSenderTxFee(EditInput textView) {
        if(textView == null){
            return;
        }
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String senderTxFee = "";
            BlockInfo blockInfo = BlockInfoDaoUtils.getInstance().query();
            if(blockInfo != null){
                String medianFee = String.valueOf(blockInfo.getMedianFee());
                senderTxFee = FmtMicrometer.fmtFormat(medianFee);
            }
            emitter.onNext(senderTxFee);
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<String>() {
                @Override
                public void handleData(String senderTxFee) {
                    Logger.d("MiningUtil.initSenderTxFee=" + senderTxFee);
                    textView.setText(senderTxFee);
                }
            });
    }

    /**
     * handle upgrade compatibility
     * only version 1.9.0.3、1.9.0.5、1.9.1、1.9.2
     * */
    public static void handleUpgradeCompatibility() {
        Context context = MyApplication.getInstance();
        String forgingReloadKey = TransmitKey.FORGING_RELOAD + AppUtil.getVersionCode(context);
        boolean isReload = SharedPreferencesHelper.getInstance().getBoolean(forgingReloadKey, false);
        if(!isReload){
            MiningUtil.clearAndReloadBlocks(null, false, true);
            SharedPreferencesHelper.getInstance().putBoolean(forgingReloadKey, true);
        }
    }

    /**
     * handle logs compatibility
     * only version 1.9.0.4
     * */
    public static void handleLogCompatibility() {
        boolean isReload = SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.FORGING_LOG, false);
        if(!isReload){
            MiningUtil.clearLogDirectory();
            SharedPreferencesHelper.getInstance().putBoolean(TransmitKey.FORGING_LOG, true);
        }
    }

    public static void clearAndReloadBlocks() {
        clearAndReloadBlocks(null);
    }

    public static void clearAndReloadBlocks(LogicObserver<Boolean> logicObserver) {
        clearAndReloadBlocks(logicObserver, true, false);
    }

    private static void clearAndReloadBlocks(LogicObserver<Boolean> logicObserver, boolean isSleep, boolean isReDownload) {
        MyApplication.getRemoteConnector().stopSyncAll();
        MyApplication.getRemoteConnector().stopBlockForging();
        MyApplication.getRemoteConnector().cancelRemoteConnector();
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                if(isSleep){
                    Thread.sleep(2000);
                }
                deleteBlockChainFileDir(isReDownload);
                int blockSync = BlockInfoDaoUtils.getInstance().reloadBlocks(isReDownload);
                if(logicObserver == null){
                    EventBusUtil.post(MessageEvent.EventCode.IRREPARABLE_ERROR, blockSync);
                }
                emitter.onNext(true);
            }catch (Exception ex){
                emitter.onNext(false);
                Logger.e(ex, "clearAndReloadBlocks is error");
            }
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean isSuccess) {
                    Logger.d("MiningUtil.clearAndReloadBlocks=" + isSuccess);
                    if(logicObserver != null){
                        logicObserver.onNext(isSuccess);
                    }
                    if(isSuccess){
                        StateTagManager.reLoadStateTags();
                        StateTagManager.reDownLoadStateTags();
                        AppUtil.killProcess(MyApplication.getInstance(), false);
                        EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                        EventBusUtil.post(MessageEvent.EventCode.FORGED_POT_DETAIL);
                    }
                    initRemoteConnectorDelay();
                }
            });
    }

    public static void deleteBlockChainFileDir(boolean isReDownload) {
        Context context = MyApplication.getInstance();
        String dataDir =  context.getApplicationInfo().dataDir;
        Logger.d(dataDir);
        // Ver1.9.0.6 will discard these two folders later
        String blocksDir = dataDir + File.separator + "blocks";
        String blockQueueDir = dataDir + File.separator + "blockqueue";
        FileUtil.deleteFile(new File(blocksDir));
        FileUtil.deleteFile(new File(blockQueueDir));

        String stateDir = dataDir + File.separator + "state";
        String blockStoreDir = dataDir + File.separator + "blockstore";
        FileUtil.deleteFile(new File(stateDir));
        FileUtil.deleteFile(new File(blockStoreDir));

        // block chain block download dir
        if(isReDownload){
            String storeBackend = dataDir + File.separator + "store-backend";
            FileUtil.deleteFile(new File(storeBackend));
        }
    }

    public static void deleteStatesTagFileDir() {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                Context context = MyApplication.getInstance();
                String dataDir =  context.getApplicationInfo().dataDir;
                String tagFileDir = dataDir + File.separator + StateTagManager.tagFileDirName;
                FileUtil.deleteFile(new File(tagFileDir));
                emitter.onNext(true);
            }catch (Exception ex){
                emitter.onNext(false);
            }
        }).observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean aBoolean) {
                    Logger.d("deleteStatesTagFileDir result=%s", aBoolean);
                }
            });
    }

    private static void initRemoteConnectorDelay() {
        Observable.timer(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new CommonObserver<Long>() {

                @Override
                public void onComplete() {
                    MyApplication.getRemoteConnector().init();
                }
            });
    }

    private static void clearLogDirectory() {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                Logger.d(sdPath);
                String logsDir = sdPath + File.separator + "tau-mobile" + File.separator + "logs";
                FileUtil.deleteFile(new File(logsDir));
                emitter.onNext(true);
            }catch (Exception ex){
                emitter.onNext(false);
                Logger.e(ex, "clearLogDirectory is error");
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new LogicObserver<Boolean>() {
                    @Override
                    public void handleData(Boolean isSuccess) {
                        Logger.d("MiningUtil.clearLogDirectory=" + isSuccess);
                    }
                });
    }
}