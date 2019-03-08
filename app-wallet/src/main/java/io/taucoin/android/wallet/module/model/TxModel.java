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
import com.mofei.tau.R;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
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
import io.taucoin.android.wallet.module.bean.BalanceBean;
import io.taucoin.android.wallet.module.bean.RawTxBean;
import io.taucoin.android.wallet.module.bean.RawTxList;
import io.taucoin.android.wallet.net.callback.TAUObserver;
import io.taucoin.android.wallet.net.service.TransactionService;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.core.Utils;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.foundation.net.NetWorkManager;
import io.taucoin.foundation.net.callback.DataResult;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.util.ByteUtil;

public class TxModel implements ITxModel {

    @Override
    public void getBalance(TAUObserver<DataResult<BalanceBean>> observer) {
        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        Map<String,String> map=new HashMap<>();
        map.put("address",  address);
        NetWorkManager.createApiService(TransactionService.class)
            .getBalance(map)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe(observer);
    }

    @Override
    public void updateBalance(BalanceBean balancebean, LogicObserver<KeyValue> observer) {
        String publicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");

        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(publicKey);
            try {
                BigInteger balance = new BigInteger(balancebean.getBalance());
                BigInteger power = new BigInteger(balancebean.getForgepower());
                keyValue.setBalance(balance.longValue());
                keyValue.setPower(power.longValue());
                KeyValueDaoUtils.getInstance().update(keyValue);
            }catch (Exception ignore){}
            emitter.onNext(keyValue);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getTxPendingList(LogicObserver<List<TransactionHistory>> observer){
        KeyValue keyValue = MyApplication.getKeyValue();
        String address = keyValue.getAddress();
        Observable.create((ObservableOnSubscribe<List<TransactionHistory>>) emitter -> {
            List<TransactionHistory> list = TransactionHistoryDaoUtils.getInstance().getTxPendingList(address);
            emitter.onNext(list);
        }).subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void checkRawTransaction(String txId, LogicObserver<Boolean> observer) {
        Map<String,String> map = new HashMap<>();
        map.put("txid", txId);
        NetWorkManager.createApiService(TransactionService.class)
            .getRawTransaction(map)
            .subscribeOn(Schedulers.io())
            .subscribe(new TAUObserver<DataResult<RawTxBean>>() {
                @Override
                public void handleData(DataResult<RawTxBean> rawTxBeanResResult) {
                    super.handleData(rawTxBeanResResult);
                    RawTxBean rawTx = rawTxBeanResResult.getData();
                    TransactionHistory history = TransactionHistoryDaoUtils.getInstance().queryTransactionById(txId);
                    Object isFinish = null;
                    if(history != null){
                        if(rawTx == null){
                            boolean isFinishState = MiningUtil.isFinishState((int) history.getBlockNum());
                            if(isFinishState){
                                isFinish = false;
                                history.setNotRolled(-1);
                                history.setMessage(ResourcesUtil.getText(R.string.send_tx_fail_in_pool));
                                history.setResult(TransmitKey.TxResult.FAILED);
                            }
                        }else{
                            isFinish = true;
                            int state = MiningUtil.parseTxState(rawTx.getNotRolled(), rawTx.getBlockNum());
                            history.setNotRolled(state);
                            history.setBlockNum(rawTx.getBlockNum());
                            history.setBlockHash(rawTx.getBlockHash());
                            history.setBlockTime(rawTx.getBlockTime());
                            history.setResult(TransmitKey.TxResult.SUCCESSFUL);
                        }

                        if(isFinish != null){
                            TransactionHistoryDaoUtils.getInstance().insertOrReplace(history);
                            observer.onNext((Boolean) isFinish);
                        }
                    }
                }

                @Override
                public void handleError(String msg, int msgCode) {
                }
            });

    }

    @Override
    public void createTransaction(TransactionHistory txHistory, LogicObserver<io.taucoin.core.Transaction> observer) {
        Observable.create((ObservableOnSubscribe<io.taucoin.core.Transaction>) emitter -> {
            KeyValue keyValue = MyApplication.getKeyValue();
            if(keyValue == null || StringUtil.isEmpty(keyValue.getPrivkey())){
                emitter.onError(CodeException.getError());
                return;
            }
            long timeStamp = (new Date().getTime())/1000;
            byte[] amount = (new BigInteger(txHistory.getAmount())).toByteArray();
            byte[] fee = (new BigInteger(txHistory.getFee())).toByteArray();

            byte[] privateKey = io.taucoin.util.Utils.getRawPrivateKeyString(keyValue.getPrivkey());
            byte[] toAddress;
            String txToAddress = txHistory.getToAddress();
            if(txToAddress.startsWith("T")) {
                toAddress = (new io.taucoin.core.VersionedChecksummedBytes(txToAddress)).getBytes();
            }else{
                toAddress = Utils.parseAsHexOrBase58(txToAddress);
            }

            io.taucoin.core.Transaction transaction = new io.taucoin.core.Transaction(TransactionVersion.V01.getCode(),
                    TransactionOptions.TRANSACTION_OPTION_DEFAULT, ByteUtil.longToBytes(timeStamp), toAddress, amount, fee);
            transaction.sign(privateKey);

            int blockHeight = BlockInfoDaoUtils.getInstance().getBlockHeight();
            Logger.i("Create tx success");
            Logger.i(transaction.toString());
            txHistory.setTxId(Hex.toHexString(transaction.getHash()));
            txHistory.setResult(TransmitKey.TxResult.CONFIRMING);
            txHistory.setFromAddress(keyValue.getAddress());
            txHistory.setCreateTime(DateUtil.getCurrentTime());
            txHistory.setNotRolled(1);
            txHistory.setBlockNum(blockHeight);

            insertTransactionHistory(txHistory);
            emitter.onNext(transaction);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void sendRawTransaction(String txHex, String txId, LogicObserver<Boolean> observer) {
        Logger.d("txId=" + txId  + "\ttx_hex=" + txHex);
        Map<String,String> map = new HashMap<>();
        map.put("tx_hex", txHex);
        NetWorkManager.createApiService(TransactionService.class)
                .sendRawTransaction(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TAUObserver<DataResult<String>>() {
                    @Override
                    public void handleError(String msg, int msgCode) {
                        String result = "Error in network, send failed";
                        if(msgCode == 401){
                            result = ResourcesUtil.getText(R.string.send_tx_fail);
                        }else if(msgCode == 402){
                            result = msg;
                        }
                        MiningUtil.saveTransactionFail(txId, result);
                        observer.onNext(false);
                        super.handleError(result, msgCode);
                    }

                    @Override
                    public void handleData(DataResult<String> dataResult) {
                        super.handleData(dataResult);
                        Logger.d("get_tx_id_after_sendTX=" + dataResult.getData());
                        MiningUtil.saveTransactionSuccess();
                        observer.onNext(true);
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

    @Override
    public void insertTransactionHistory(TransactionHistory txHistory){
        Observable.create((ObservableOnSubscribe<TransactionHistory>) emitter -> {
            TransactionHistoryDaoUtils.getInstance().insertOrReplace(txHistory);
            emitter.onNext(txHistory);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
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
    public void getTxRecords(TAUObserver<DataResult<RawTxList>> observer) {
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
                    NetWorkManager.createApiService(TransactionService.class)
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
                        TransactionHistory tx = TransactionHistoryDaoUtils.getInstance().queryTransactionById(bean.getTxid());
                        if(tx == null){
                            tx = new TransactionHistory();
                            tx.setFromAddress(bean.getAddin());
                            tx.setToAddress(bean.getAddout());
                            // createTime and blockTime need set value here!
                            tx.setCreateTime(String.valueOf(bean.getBlockTime()));

                            tx.setTxId(bean.getTxid());
                            tx.setAmount(bean.getVout());
                            tx.setFee(bean.getFee());
                        }
                        tx.setBlockTime(bean.getBlockTime());
                        tx.setBlockNum(bean.getBlockNum());
                        tx.setBlockHash(bean.getBlockHash());
                        tx.setResult(TransmitKey.TxResult.SUCCESSFUL);
                        tx.setNotRolled(MiningUtil.parseTxState(bean.getNotRolled(), bean.getBlockNum()));
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
    public void getBlockHeight(TAUObserver<DataResult<Integer>> observer) {
        NetWorkManager.createApiService(TransactionService.class)
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

            if(blockHeight > entry.getBlockHeight()){
                entry.setBlockHeight(blockHeight);
            }
            BlockInfoDaoUtils.getInstance().insertOrReplace(entry);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void getBlockInfo(LogicObserver<BlockInfo> observer) {
        Observable.create((ObservableOnSubscribe<BlockInfo>) emitter -> {
            BlockInfo entry = BlockInfoDaoUtils.getInstance().query();
            emitter.onNext(entry);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}