package io.taucoin.android.wallet.module.model;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.greendao.TransactionHistoryDao;
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.db.util.MiningInfoDaoUtils;
import io.taucoin.android.wallet.db.util.TransactionHistoryDaoUtils;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class MiningModel implements IMiningModel{
    @Override
    public void getMiningInfo(LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
            if(keyValue != null){
                // set mining info
                List<MiningInfo> list = MiningInfoDaoUtils.getInstance().queryByPubicKey(pubicKey);
                keyValue.setMiningInfos(list);
                // set max block sync
                int blockSynchronized = KeyValueDaoUtils.getInstance().getMaxBlockSynchronized();
                keyValue.setBlockSynchronized(blockSynchronized);
                // set max block height
                int blockHeight = KeyValueDaoUtils.getInstance().getMaxBlockHeight();
                blockHeight = blockSynchronized > blockHeight ? blockSynchronized : blockHeight;
                keyValue.setBlockHeight(blockHeight);
            }
            MyApplication.setKeyValue(keyValue);
            emitter.onNext(keyValue);
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(observer);
    }

    @Override
    public void updateMiningState(LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
            boolean isSuccess = false;
            if(entry != null){
                String state = TransmitKey.MiningState.Start;
                if(StringUtil.isSame(entry.getMiningState(), state)){
                    state = TransmitKey.MiningState.Stop;
                }
                entry.setMiningState(state);
                isSuccess = KeyValueDaoUtils.getInstance().updateMiningState(entry);
            }
            emitter.onNext(isSuccess);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
            entry.setBlockHeight(blockHeight);
            KeyValueDaoUtils.getInstance().update(entry);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void updateBlockSynchronized(int blockSynchronized, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
            if(entry.getBlockHeight() < blockSynchronized){
                entry.setBlockHeight(blockSynchronized);
            }
            entry.setBlockSynchronized(blockSynchronized);
            KeyValueDaoUtils.getInstance().update(entry);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void updateMyMiningBlock(List<BlockEventData> blocks, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            for (BlockEventData blockEvent : blocks) {
                if(blockEvent == null || blockEvent.block == null){
                    continue;
                }
                Block block = blockEvent.block;

                saveMiningInfo(block);
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private boolean saveMiningInfo(Block block) {
        String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        String number = String.valueOf(block.getNumber());
        String generatorPublicKey = Hex.toHexString(block.getGeneratorPublicKey());
        if(StringUtil.isSame(pubicKey, generatorPublicKey)){
            MiningInfo entry = MiningInfoDaoUtils.getInstance().queryByNumber(number);
            if(entry == null){
                String hash = Hex.toHexString(block.getHash());
                entry = new MiningInfo();
                entry.setBlockNo(number);
                entry.setPublicKey(pubicKey);
                entry.setBlockHash(hash);
                entry.setValid(1);

                List<Transaction> txList = block.getTransactionsList();
                int total = 0;
                String reward = "0";
                if(txList != null){
                    total = txList.size();
                    reward = MiningUtil.parseBlockReward(txList);
                }
                entry.setTotal(total);
                entry.setReward(reward);

                MiningInfoDaoUtils.getInstance().insertOrReplace(entry);
                return true;
            }
        }
        return true;
    }

    @Override
    public void handleSynchronizedBlock(BlockEventData blockEvent, LogicObserver<MessageEvent.EventCode> logicObserver) {
        Observable.create((ObservableOnSubscribe<MessageEvent.EventCode>) emitter -> {
            MessageEvent.EventCode eventCode = null;
            if(blockEvent != null){
                Block block = blockEvent.block;
                if(block != null){
                    boolean isMy = saveMiningInfo(block);
                    boolean isUpdate = false;
                    if(isMy){
                        eventCode = MessageEvent.EventCode.MINING_INFO;
                    }
                    List<Transaction> txList = block.getTransactionsList();
                    if(txList != null && txList.size() > 0){
                        for (Transaction transaction : txList) {
                            String txId = Hex.toHexString(transaction.getHash());
                            long blockTime = new BigInteger(transaction.getTime()).longValue();
                            String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
                            List<TransactionHistory> txHistoryList = TransactionHistoryDaoUtils.getInstance().getTxPendingList(address);
                            if(txHistoryList != null && txHistoryList.size() > 0){
                                for (TransactionHistory txHistory : txHistoryList) {
                                    if(StringUtil.isSame(txId, txHistory.getTxId())){
                                        isUpdate = true;
                                        eventCode = MessageEvent.EventCode.TRANSACTION;
                                        txHistory.setBlockTime(blockTime);
                                        txHistory.setResult(TransmitKey.TxResult.SUCCESSFUL);
                                        TransactionHistoryDaoUtils.getInstance().insertOrReplace(txHistory);
                                    }
                                }
                            }
                        }
                    }
                    if(isUpdate && isMy){
                        eventCode = MessageEvent.EventCode.ALL;
                    }
                }
            }
            emitter.onNext(eventCode);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(logicObserver);
    }

    @Override
    public void updateTransactionHistory(io.taucoin.android.interop.Transaction transaction) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            // TODO send tx fail logic
            if(transaction != null){
                String txId = Hex.toHexString(transaction.getHash());
                MiningUtil.saveTransactionSuccess(txId);
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}