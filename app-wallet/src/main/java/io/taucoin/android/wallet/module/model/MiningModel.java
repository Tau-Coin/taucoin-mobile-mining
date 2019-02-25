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
    public void updateSynchronizedBlockNum(int blockSynchronized, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            updateSynchronizedBlockNum(blockSynchronized);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private void updateSynchronizedBlockNum(int blockSynchronized){
        String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
        if(entry.getBlockHeight() < blockSynchronized){
            entry.setBlockHeight(blockSynchronized);
        }
        entry.setBlockSynchronized(blockSynchronized);
        KeyValueDaoUtils.getInstance().update(entry);
    }

    @Override
    public void updateMyMiningBlock(List<BlockEventData> blocks, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            for (BlockEventData blockEvent : blocks) {
                if(blockEvent == null || blockEvent.block == null){
                    continue;
                }
                Block block = blockEvent.block;

                saveMiningInfo(block, true);
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private boolean saveMiningInfo(Block block, boolean isConnect) {
        String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        String number = String.valueOf(block.getNumber());
        String hash = Hex.toHexString(block.getHash());
        String generatorPublicKey = Hex.toHexString(block.getGeneratorPublicKey());
        MiningInfo entry = MiningInfoDaoUtils.getInstance().queryByBlockHash(hash);
        boolean isMine = StringUtil.isSame(pubicKey, generatorPublicKey);
        if(entry == null){
            if(isMine && isConnect){
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
            }
        }else{
            entry.setValid(isConnect ? 1 : 0);
            MiningInfoDaoUtils.getInstance().insertOrReplace(entry);
        }
        return isMine;
    }

    private boolean handleSynchronizedTransaction(Block block, boolean isConnect) {
        List<Transaction> txList = block.getTransactionsList();
        String blockHash = Hex.toHexString(block.getHash());
        long blockNumber = block.getNumber();
        if(txList != null && txList.size() > 0){
            for (Transaction transaction : txList) {
                String txId = Hex.toHexString(transaction.getHash());
                TransactionHistory txHistory = TransactionHistoryDaoUtils.getInstance().queryTransactionById(txId);
                if(txHistory != null){
                    txHistory.setResult(TransmitKey.TxResult.SUCCESSFUL);
                    txHistory.setIsInvalid(isConnect ? 1 : 0);
                    TransactionHistoryDaoUtils.getInstance().insertOrReplace(txHistory);
                }else{
                    if(isConnect){
                        long blockTime = new BigInteger(transaction.getTime()).longValue();
                        TransactionHistory history = TransactionHistoryDaoUtils.getInstance().queryTransactionById(txId);
                        if(history != null){
                            history.setResult(TransmitKey.TxResult.SUCCESSFUL);
                            history.setIsInvalid(1);
                            history.setBlockTime(blockTime);
                            history.setBlockNum(blockNumber);
                            history.setBlockHash(blockHash);

                            TransactionHistoryDaoUtils.getInstance().insertOrReplace(history);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void handleSynchronizedBlock(BlockEventData blockEvent, boolean isConnect, LogicObserver<MessageEvent.EventCode> logicObserver) {
        Observable.create((ObservableOnSubscribe<MessageEvent.EventCode>) emitter -> {
            MessageEvent.EventCode eventCode = MessageEvent.EventCode.MINING_INFO;
            if(blockEvent != null){
                Block block = blockEvent.block;
                if(block != null){
                    long blockNumber = block.getNumber();
                    updateSynchronizedBlockNum((int) blockNumber);
                    saveMiningInfo(block, isConnect);

                    boolean isUpdate = handleSynchronizedTransaction(block, isConnect);
                    if(isUpdate){
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
                MiningUtil.saveTransactionSuccess();
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}