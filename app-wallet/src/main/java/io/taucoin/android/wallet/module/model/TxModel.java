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
package io.taucoin.android.wallet.module.model;

import com.github.naturs.logger.Logger;

import io.taucoin.android.wallet.R;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.db.util.TransactionHistoryDaoUtils;
import io.taucoin.android.wallet.module.bean.AccountBean;
import io.taucoin.android.wallet.module.bean.ChainBean;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.bean.NewTxBean;
import io.taucoin.android.wallet.module.bean.RawTxBean;
import io.taucoin.android.wallet.module.bean.RawTxList;
import io.taucoin.android.wallet.module.bean.TransactionBean;
import io.taucoin.android.wallet.module.bean.TxDataBean;
import io.taucoin.android.wallet.module.bean.TxStatusBean;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.net.service.TransactionService;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.core.Transaction;
import io.taucoin.core.Utils;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.foundation.net.NetWorkManager;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.net.callback.NetResultCode;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.util.ByteUtil;

public class TxModel implements ITxModel {

    @Override
    public void getBalance(TxObserver<AccountBean> observer) {
        String rawAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.RAW_ADDRESS, "");
        Map<String,String> map = new HashMap<>();
        map.put("address",  rawAddress);
        NetWorkManager.createMainApiService(TransactionService.class)
            .getBalance(map)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(observer);
    }

    @Override
    public void updateBalance(AccountBean accountInfo, LogicObserver<KeyValue> observer) {
        String publicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");

        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(publicKey);
            try {
                if(accountInfo != null && StringUtil.isNotEmpty(accountInfo.getAccountInfo())){
                    keyValue.setBalance(accountInfo.getBalance().longValue());
                    keyValue.setPower(accountInfo.getPower().longValue());
                    KeyValueDaoUtils.getInstance().update(keyValue);
                }
            }catch (Exception ignore){}
            emitter.onNext(keyValue);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getTxPendingListDelay(LogicObserver<List<List<String>>> observer){
        KeyValue keyValue = MyApplication.getKeyValue();
        String address = keyValue.getAddress();
        Observable.create((ObservableOnSubscribe<List<List<String>>>) emitter -> {
            List<TransactionHistory> txList = TransactionHistoryDaoUtils.getInstance().getTxPendingListDelay(address);
            List<List<String>> lists = new ArrayList<>();
            if(txList != null && txList.size() > 0){
                List<String> idsList = new ArrayList<>();
                for (int i = 0; i < txList.size(); i++) {
                    if(idsList.size() == 5){
                        lists.add(idsList);
                        idsList = new ArrayList<>();
                    }
                    idsList.add(txList.get(i).getTxId());
                }
                if(idsList.size() > 0){
                    lists.add(idsList);
                }
            }
            emitter.onNext(lists);
        }).subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void checkRawTransaction(List<String> txIds, LogicObserver<Boolean> observer) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("txids", txIds);
        NetWorkManager.createMainApiService(TransactionService.class)
            .getRawTransaction(map)
            .subscribeOn(Schedulers.io())
            .subscribe(new TxObserver<TxDataBean>() {
                @Override
                public void handleData(TxDataBean resResult) {
                    super.handleData(resResult);
                    if(resResult == null || resResult.getStatus() != NetResultCode.MAIN_SUCCESS_CODE){
                        observer.onNext(false);
                        return;
                    }
                    List<TxStatusBean> txsStatus = resResult.getTxsStatus();
                    if(txsStatus == null){
                        observer.onNext(false);
                        return;
                    }
                    boolean isRefresh = false;
                    for (TxStatusBean txStatus : txsStatus) {
                        boolean isSuccess = updateTransactionStatus(txStatus);
                        if(isSuccess){
                            isRefresh = true;
                        }
                    }
                    observer.onNext(isRefresh);
                }
            });

    }

    /**
     * update transaction status
     *
     * 0-Legitimate trading, trading pool waiting for the chain
     * 1-Legitimate transactions, not overdue on-line transactions
     * 2-Legitimate transactions are on the chain
     *
     * 11-Verification fails, illegal transactions, transaction information is incorrect (unresolved, field length is incorrect)
     * 12-Verification fails, illegal transactions, signature verification fails
     * 13-Verification failed, illegal transactions, insufficient balance
     * 20-Chain-end backend lost transactions for unknown reasons (txId cannot be queried in the status database due to unknown errors)
     * */
    private synchronized boolean updateTransactionStatus(TxStatusBean txStatus) {
        String txId = txStatus.getTxId();
        TransactionHistory history = TransactionHistoryDaoUtils.getInstance().queryTransactionById(txId);
        if(history == null){
            return false;
        }
        boolean isRefresh = false;
        switch (txStatus.getStatus()){
            case 0:
                history.setResult(TransmitKey.TxResult.CONFIRMING);
                isRefresh = true;
                break;
            case 1:
                history.setResult(TransmitKey.TxResult.FAILED);
                history.setMessage(ResourcesUtil.getText(R.string.send_tx_status_expired));
                isRefresh = true;
                break;
            case 2:
                history.setResult(TransmitKey.TxResult.SUCCESSFUL);
                isRefresh = true;
                break;
            case 11:
                history.setResult(TransmitKey.TxResult.FAILED);
                history.setMessage(ResourcesUtil.getText(R.string.send_tx_illegal));
                isRefresh = true;
                break;
            case 12:
                history.setResult(TransmitKey.TxResult.FAILED);
                history.setMessage(ResourcesUtil.getText(R.string.send_tx_verify_signature));
                isRefresh = true;
                break;
            case 13:
                history.setResult(TransmitKey.TxResult.FAILED);
                history.setMessage(ResourcesUtil.getText(R.string.send_tx_insufficient_balance));
                isRefresh = true;
                break;
            case 20:
                history.setResult(TransmitKey.TxResult.FAILED);
                history.setMessage(ResourcesUtil.getText(R.string.send_tx_unknown_error));
                isRefresh = true;
                break;
            default:
                break;
        }

        if(isRefresh){
            TransactionHistoryDaoUtils.getInstance().insertOrReplace(history);
        }
        return isRefresh;
    }

    @Override
    public void createTransaction(TransactionHistory txHistory, LogicObserver<TransactionBean> observer) {
        Observable.create((ObservableOnSubscribe<TransactionBean>) emitter -> {
            KeyValue keyValue = MyApplication.getKeyValue();
            if(keyValue == null || StringUtil.isEmpty(keyValue.getPriKey())){
                emitter.onError(CodeException.getError());
                return;
            }
            long timeStamp = (new Date().getTime())/1000;
            byte[] amount = (new BigInteger(txHistory.getAmount())).toByteArray();
            byte[] fee = (new BigInteger(txHistory.getFee())).toByteArray();

            byte[] privateKey = io.taucoin.util.Utils.getRawPrivateKeyString(keyValue.getPriKey());
            byte[] toAddress;
            String txToAddress = txHistory.getToAddress();
            if(txToAddress.startsWith("T")) {
                toAddress = (new io.taucoin.core.VersionedChecksummedBytes(txToAddress)).getBytes();
            }else{
                toAddress = Utils.parseAsHexOrBase58(txToAddress);
            }
            long expiryBlock = UserUtil.getTransExpiryBlock();
            byte[] expireTimeByte = ByteUtil.longToBytes(expiryBlock);
            io.taucoin.core.Transaction transaction = new io.taucoin.core.Transaction(TransactionVersion.V01.getCode(),
                    TransactionOptions.TRANSACTION_OPTION_DEFAULT, ByteUtil.longToBytes(timeStamp), toAddress, amount, fee, expireTimeByte);
            transaction.sign(privateKey);

            Logger.i("Create tx success");
            Logger.i(transaction.toString());
            txHistory.setTxId(transaction.getTxid());
            txHistory.setResult(TransmitKey.TxResult.BROADCASTING);
            txHistory.setFromAddress(keyValue.getAddress());
            txHistory.setCreateTime(String.valueOf(timeStamp));
            txHistory.setTransExpiry(expiryBlock);

//            insertTransactionHistory(txHistory);
            TransactionBean transactionBean = new TransactionBean();
            transactionBean.setLocalData(txHistory);
            transactionBean.setRawData(transaction);
            emitter.onNext(transactionBean);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void sendRawTransaction(TransactionBean transactionBean, LogicObserver<Boolean> observer) {
        Transaction transaction = transactionBean.getRawData();
        TransactionHistory transactionHistory = transactionBean.getLocalData();
        String txHash = Hex.toHexString(transaction.getEncoded());
        String txId = transaction.getTxid();
        Logger.d("txId=" + txId  + "\ttxHash=" + txHash);
        Map<String,String> map = new HashMap<>();
        map.put("transaction", txHash);
        NetWorkManager.createMainApiService(TransactionService.class)
            .sendRawTransaction(map)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new TxObserver<NewTxBean>() {
                @Override
                public void handleError(String msg, int msgCode) {
                    String result = ResourcesUtil.getText(R.string.send_tx_network_error);
                    handleError(result);
                    super.handleError(result, msgCode);
                }

                @Override
                public void handleData(NewTxBean dataResult) {
                    super.handleData(dataResult);
                    if(dataResult != null){
                        if(dataResult.getStatus() == NetResultCode.MAIN_SUCCESS_CODE){
                            Logger.d("sendRawTransaction.handleData=" +dataResult);
                            insertTransactionHistory(transactionHistory, new LogicObserver<Boolean>() {
                                @Override
                                public void handleData(Boolean aBoolean) {
                                    ToastUtils.showShortToast(R.string.send_tx_success);
                                    EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
                                    EventBusUtil.post(MessageEvent.EventCode.BALANCE);
                                    MiningUtil.checkRawTransaction();
                                }
                            });
                            observer.onNext(true);
                        }else{
                            String result = StringUtil.isEmpty(dataResult.getMessage()) ? "" : dataResult.getMessage();
                            handleError(result);
                        }
                    }
                }

                void handleError(String result) {
                    transactionHistory.setResult(TransmitKey.TxResult.FAILED);
                    transactionHistory.setMessage(result);
                    insertTransactionHistory(transactionHistory, new LogicObserver<Boolean>() {
                        @Override
                        public void handleData(Boolean aBoolean) {
                            EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
                        }
                    });
                    observer.onNext(false);
                }
            });
    }

    @Override
    public void updateTransactionHistory(TransactionHistory txHistory, LogicObserver<Boolean> observer){
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            TransactionHistory transactionHistory = TransactionHistoryDaoUtils.getInstance().queryTransactionById(txHistory.getTxId());
            transactionHistory.setResult(txHistory.getResult());
            transactionHistory.setMessage(txHistory.getMessage());
            TransactionHistoryDaoUtils.getInstance().insertOrReplace(transactionHistory);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private void insertTransactionHistory(TransactionHistory txHistory, LogicObserver<Boolean> logicObserver){
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            TransactionHistoryDaoUtils.getInstance().insertOrReplace(txHistory);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(logicObserver);
    }

    @Override
    public void queryTransactionHistory(int pageNo, String time, LogicObserver<List<TransactionHistory>> logicObserver) {
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null || StringUtil.isEmpty(keyValue.getAddress())){
            return;
        }
        Observable.create((ObservableOnSubscribe<List<TransactionHistory>>) emitter -> {
            List<TransactionHistory> result = TransactionHistoryDaoUtils.getInstance().queryData(pageNo, time, keyValue.getAddress());
            emitter.onNext(result);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(logicObserver);
    }

    @Override
    public void getTxRecords(TxObserver<RawTxList> observer) {
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            observer.onError(CodeException.getError());
            return;
        }
        String address = keyValue.getAddress();
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            String time = TransactionHistoryDaoUtils.getInstance().getNewestTxTime(address);
            if(StringUtil.isNotEmpty(time)){
                emitter.onNext(time);
            }else {
                observer.onError();
            }
        }).observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<String>() {
                @Override
                public void handleData(String time) {

                    Map<String,String> map = new HashMap<>();
                    map.put("address", address);
                    map.put("time", time);
                    NetWorkManager.createMainApiService(TransactionService.class)
                        .getTxRecords(map)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(observer);
                }
            });

    }

    @Override
    public void saveTxRecords(RawTxList rawTxList, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            if(null != rawTxList){
                List<RawTxBean> txList = rawTxList.getRecords();
                if(null != txList && txList.size() > 0){
                    for (RawTxBean bean : txList) {
                        TransactionHistory tx = TransactionHistoryDaoUtils.getInstance().queryTransactionById(bean.getTxId());
                        if(tx == null){
                            tx = new TransactionHistory();
                            tx.setFromAddress(bean.getSender());
                            tx.setToAddress(bean.getReceiver());
                            // createTime and blockTime need set value here!
                            tx.setCreateTime(DateUtil.formatUTCTime(bean.getTxTime()));

                            tx.setTxId(bean.getTxId());
                            tx.setAmount(FmtMicrometer.fmtTxValue(bean.getAmount()));
                            tx.setFee(FmtMicrometer.fmtTxValue(bean.getFee()));
                        }
                        tx.setTimeBasis(1);
                        tx.setBlockHeight(bean.getBlockHeight());
                        tx.setResult(TransmitKey.TxResult.SUCCESSFUL);
                        TransactionHistoryDaoUtils.getInstance().insertOrReplace(tx);
                    }
                }
            }
            emitter.onNext(true);
        }).observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getBlockHeight(TxObserver<ChainBean> observer) {
        NetWorkManager.createMainApiService(TransactionService.class)
                .getBlockHeight()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    @Override
    public void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            BlockInfo entry = BlockInfoDaoUtils.getInstance().query();
            if(entry == null){
                entry = new BlockInfo();
            }
            entry.setBlockHeight(blockHeight);
            BlockInfoDaoUtils.getInstance().insertOrReplace(entry);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}