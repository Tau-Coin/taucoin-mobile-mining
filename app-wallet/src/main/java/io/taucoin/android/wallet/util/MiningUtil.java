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

import com.github.naturs.logger.Logger;
import com.mofei.tau.R;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.db.util.TransactionHistoryDaoUtils;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.TxModel;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.net.callback.LogicObserver;

public class MiningUtil {

    public static int parseMinedBlocks(BlockInfo blockInfo) {
        if(blockInfo != null){
            List<MiningInfo> list= blockInfo.getMiningInfos();
            if(list != null){
                return list.size();
            }
        }
        return 0;
    }

    public static String parseMiningIncome(BlockInfo blockInfo) {
        BigDecimal number = new BigDecimal("0");
        if(blockInfo != null){
            List<MiningInfo> list= blockInfo.getMiningInfos();
            if(list != null && list.size() > 0){
                for (MiningInfo bean : list) {
                    try {
                        number = number.add(new BigDecimal(bean.getReward()));
                    }catch (Exception ignore){}
                }
            }
        }
        return FmtMicrometer.fmtFormat(number.toString());
    }

    public static void setBlockHeight(ItemTextView textView) {
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
                        textView.setRightText(blockHeight);
                    }
                });
    }

    public static String parseBlockReward(List<Transaction> txList) {
        BigInteger reward = new BigInteger("0");
        if(txList != null && txList.size() > 0){
            for (Transaction transaction : txList) {
                BigInteger fee = new BigInteger(transaction.getFee());
                reward = reward.add(fee);
            }
        }
        return reward.toString();
    }

    public static void saveTransactionFail(String txId, String result) {
        TransactionHistory transactionHistory = new TransactionHistory();
        transactionHistory.setTxId(txId);
        transactionHistory.setResult(TransmitKey.TxResult.FAILED);
        transactionHistory.setMessage(result);
        transactionHistory.setNotRolled(-1);
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

    private static void checkRawTransaction() {
        TxService.startTxService(TransmitKey.ServiceType.GET_RAW_TX);
    }

    public static boolean isFinishState(TransactionHistory history) {
        long txTime = history.getBlockTime();
        long currentTime = DateUtil.getTime();
        long expireTime = history.getExpireTime();
        if(txTime <= 0){
            try {
                txTime = new BigInteger(history.getCreateTime()).longValue();
            }catch (Exception ignore){}
        }
        return currentTime - txTime > expireTime;
    }

    public static int parseTxState(int state, TransactionHistory history) {
        if(state == 1) {
            if(isFinishState(history)){
                state = -1;
            }
        }
        return state;
    }

    public static long pendingAmount() {
        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        List<TransactionHistory> txPendingList = TransactionHistoryDaoUtils.getInstance().getPendingAmountList(address);
        BigInteger pendingAmount = new BigInteger("0");
        if(txPendingList != null && txPendingList.size() > 0){
            for (TransactionHistory transaction : txPendingList) {
                BigInteger amount = new BigInteger(transaction.getAmount());
                pendingAmount = pendingAmount.add(amount);
                BigInteger fee = new BigInteger(transaction.getFee());
                pendingAmount = pendingAmount.add(fee);
            }
        }
        return pendingAmount.longValue();
    }

    public static int getMiningMsg(){
        int msg = R.string.mining_in_progress;
        boolean isInit = MyApplication.getRemoteConnector().isInit();
        if(!isInit){
            msg = R.string.mining_init_data;
        }
        return msg;
    }
}