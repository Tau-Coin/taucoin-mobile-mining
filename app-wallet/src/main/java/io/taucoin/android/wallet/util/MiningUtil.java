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
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.widget.EditInput;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.AppUtil;

public class MiningUtil {

//    public static int parseMinedBlocks(BlockInfo blockInfo) {
//        if(blockInfo != null){
//            List<MiningBlock> list= blockInfo.getMiningInfo();
//            if(list != null){
//                return list.size();
//            }
//        }
//        return 0;
//    }
//
//    private static String parseMiningIncome(List<MiningReward> rewards) {
//        BigDecimal number = new BigDecimal("0");
//        if(rewards != null && rewards.size() > 0){
//            for (MiningReward bean : rewards) {
//                try {
//                    number = number.add(new BigDecimal(bean.getMinerFee()));
//                    number = number.add(new BigDecimal(bean.getPartFee()));
//                }catch (Exception ignore){}
//            }
//        }
//        return FmtMicrometer.fmtMiningIncome(number.longValue());
//    }
//
//    public static RewardBean parseMiningReward(List<MiningReward> rewards) {
//        RewardBean reward = new RewardBean();
//        BigDecimal minerReward = new BigDecimal("0");
//        BigDecimal partReward = new BigDecimal("0");
//        if(rewards != null && rewards.size() > 0){
//            for (MiningReward bean : rewards) {
//                try {
//                    partReward = partReward.add(new BigDecimal(bean.getPartFee()));
//                    minerReward = minerReward.add(new BigDecimal(bean.getMinerFee()));
//                }catch (Exception ignore){}
//            }
//            long totalReward = partReward.add(minerReward).longValue();
//            reward.setTotalReward(totalReward);
//            reward.setMinerReward(minerReward.longValue());
//            reward.setPartReward(partReward.longValue());
//        }
//        return reward;
//    }

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

//    public static long pendingAmount() {
//        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
//        List<TransactionHistory> txPendingList = TransactionHistoryDaoUtils.getInstance().getPendingAmountList(address);
//        BigInteger pendingAmount = new BigInteger("0");
//        if(txPendingList != null && txPendingList.size() > 0){
//            for (TransactionHistory transaction : txPendingList) {
//                BigInteger amount = new BigInteger(transaction.getAmount());
//                pendingAmount = pendingAmount.add(amount);
//                BigInteger fee = new BigInteger(transaction.getFee());
//                pendingAmount = pendingAmount.add(fee);
//            }
//        }
//        return pendingAmount.longValue();
//    }
//
//    public static void setMiningIncome(TextView tvMiningIncome) {
//        Observable.create((ObservableOnSubscribe<List<MiningReward>>) emitter -> {
//            String rawAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.RAW_ADDRESS, "");
//            List<MiningReward> miningRewards = MiningRewardDaoUtils.getInstance().queryData(rawAddress);
//            emitter.onNext(miningRewards);
//        }).observeOn(AndroidSchedulers.mainThread())
//            .subscribeOn(Schedulers.io())
//            .subscribe(new LogicObserver<List<MiningReward>>() {
//                @Override
//                public void handleData(List<MiningReward> miningRewards) {
//                    if(tvMiningIncome != null){
//                        String miningIncomeText = ResourcesUtil.getText(R.string.common_balance);
//                        String miningIncome = MiningUtil.parseMiningIncome(miningRewards);
//                        miningIncomeText = String.format(miningIncomeText, miningIncome);
//                        tvMiningIncome.setText(Html.fromHtml(miningIncomeText));
//                        Logger.d("MiningUtil.setMiningIncome=" + miningIncomeText);
//                    }
//                }
//            });
//    }

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
     * only version 1.9.0.3
     * */
    public static void handleUpgradeCompatibility() {
        boolean isReload = SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.FORGING_RELOAD, false);
        if(!isReload){
            MiningUtil.clearAndReloadBlocks();
            SharedPreferencesHelper.getInstance().putBoolean(TransmitKey.FORGING_RELOAD, true);
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
        MyApplication.getRemoteConnector().stopSyncAll();
        MyApplication.getRemoteConnector().stopBlockForging();
        MyApplication.getRemoteConnector().cancelRemoteConnector();
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            try {
                Thread.sleep(2000);
                Context context = MyApplication.getInstance();
                String dataDir =  context.getApplicationInfo().dataDir;
                Logger.d(dataDir);
                String blocksDir = dataDir + File.separator + "blocks";
                String stateDir = dataDir + File.separator + "state";
                String blockQueueDir = dataDir + File.separator + "blockqueue";
                String blockStoreDir = dataDir + File.separator + "blockstore";
                FileUtil.deleteFile(new File(blocksDir));
                FileUtil.deleteFile(new File(stateDir));
                FileUtil.deleteFile(new File(blockQueueDir));
                FileUtil.deleteFile(new File(blockStoreDir));

                int blockSync = BlockInfoDaoUtils.getInstance().reloadBlocks();
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
                        AppUtil.killProcess(MyApplication.getInstance(), false);
                        EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                        EventBusUtil.post(MessageEvent.EventCode.FORGED_POT_DETAIL);
                    }
                    initRemoteConnectorDelay();
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