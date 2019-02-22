package io.taucoin.android.wallet.module.model;

import org.spongycastle.util.encoders.Hex;

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
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.db.util.MiningInfoDaoUtils;
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
                // set max block height
                int blockHeight = KeyValueDaoUtils.getInstance().getMaxBlockHeight();
                keyValue.setBlockHeight(blockHeight);
                // set max block sync
                int blockSynchronized = KeyValueDaoUtils.getInstance().getMaxBlockSynchronized();
                keyValue.setBlockSynchronized(blockSynchronized);
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
                entry.setBlockSynchronized(blockSynchronized);
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
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            for (BlockEventData BlockEvent : blocks) {
                Block block = BlockEvent.block;
                if(block == null){
                    continue;
                }
                String number = String.valueOf(block.getNumber());
                String generatorPublicKey = Hex.toHexString(block.getGeneratorPublicKey());
                if(StringUtil.isNotSame(pubicKey, generatorPublicKey)){
                    continue;
                }
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
                }
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}