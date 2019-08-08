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

import io.reactivex.Scheduler;
import io.taucoin.android.wallet.module.bean.MessageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.net.service.TransactionService;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.core.Block;
import io.taucoin.foundation.net.NetWorkManager;
import io.taucoin.foundation.net.callback.LogicObserver;

public class MiningModel implements IMiningModel{
    private Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(30));
    @Override
    public void getMiningInfo(LogicObserver<BlockInfo> observer) {
        Observable.create((ObservableOnSubscribe<BlockInfo>) emitter -> {
            BlockInfo blockInfo = BlockInfoDaoUtils.getInstance().query();
            if(blockInfo == null){
                blockInfo = new BlockInfo();
            }

            emitter.onNext(blockInfo);
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe(observer);
    }

    @Override
    public void updateMiningState(String miningState, LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
            if(entry != null){
                entry.setMiningState(miningState);
                KeyValueDaoUtils.getInstance().updateMiningState(entry);
            }
            emitter.onNext(entry);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

//    @Override
//    public void updateSyncState(String syncState, LogicObserver<Boolean> observer) {
//        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
//            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
//            KeyValue entry = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
//            boolean isSuccess = false;
//            if(entry != null){
//                entry.setSyncState(syncState);
//                if(StringUtil.isSame(syncState, TransmitKey.MiningState.Stop)){
//                    entry.setMiningState(syncState);
//                }
//                isSuccess = KeyValueDaoUtils.getInstance().updateMiningState(entry);
//            }
//            emitter.onNext(isSuccess);
//        }).observeOn(AndroidSchedulers.mainThread())
//                .subscribeOn(scheduler)
//                .unsubscribeOn(scheduler)
//                .subscribe(observer);
//    }

    @Override
    public void updateSynchronizedBlockNum(int blockSynchronized, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            updateSynchronizedBlockNum(blockSynchronized);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    private void updateSynchronizedBlockNum(int blockSync){
        BlockInfo entry = BlockInfoDaoUtils.getInstance().query();
        if(entry == null){
            entry = new BlockInfo();
        }
        entry.setBlockSync(blockSync);
        BlockInfoDaoUtils.getInstance().insertOrReplace(entry);
    }

    @Override
    public synchronized void handleSynchronizedBlock(BlockEventData blockEvent, boolean isConnect, LogicObserver<MessageEvent.EventCode> logicObserver) {
        Observable.create((ObservableOnSubscribe<MessageEvent.EventCode>) emitter -> {
            MessageEvent.EventCode eventCode = MessageEvent.EventCode.MINING_INFO;
            if(blockEvent != null){
                Block block = blockEvent.block;
                if(block != null){
                    long blockNumber = block.getNumber();
                    updateSynchronizedBlockNum((int) blockNumber);
                }
            }
            emitter.onNext(eventCode);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(logicObserver);
    }

    @Override
    public void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            BlockInfo entry = BlockInfoDaoUtils.getInstance().query();
            if(entry == null){
                entry = new BlockInfo();
            }

            if(entry.getBlockHeight() != blockHeight){
                entry.setBlockHeight(blockHeight);
                BlockInfoDaoUtils.getInstance().insertOrReplace(entry);
                emitter.onNext(true);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void updateBlocksDownloaded(long blockDownloaded, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            BlockInfo entry = BlockInfoDaoUtils.getInstance().query();
            if(entry == null){
                entry = new BlockInfo();
            }
            if(blockDownloaded > 0 && entry.getBlockDownload() != blockDownloaded){
                entry.setBlockDownload((int) blockDownloaded);
                BlockInfoDaoUtils.getInstance().insertOrReplace(entry);
                emitter.onNext(true);
            }
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe(observer);
    }

    @Override
    public void getMinerHistory(TxObserver<MinerListBean> observer) {
        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        Map<String,String> map = new HashMap<>();
        map.put("address",  address);
        NetWorkManager.createMysqlApiService(TransactionService.class)
            .getMinerHistory(map)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe(observer);
    }
}