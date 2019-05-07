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

import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.db.util.MiningRewardDaoUtils;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.platform.adress.KeyManager;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningBlock;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.db.util.MiningBlockDaoUtils;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.TxPresenter;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.facade.TaucoinImpl;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class MiningModel implements IMiningModel{
    @Override
    public void getMiningInfo(LogicObserver<BlockInfo> observer) {
        Observable.create((ObservableOnSubscribe<BlockInfo>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            BlockInfo blockInfo = BlockInfoDaoUtils.getInstance().query();
            if(blockInfo == null){
                blockInfo = new BlockInfo();
            }
            // set mining info
            List<MiningBlock> list = MiningBlockDaoUtils.getInstance().queryByPubicKey(pubicKey);
            blockInfo.setMiningBlocks(list);

            emitter.onNext(blockInfo);
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
    public void updateSynchronizedBlockNum(int blockSynchronized, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            updateSynchronizedBlockNum(blockSynchronized);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
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
    public void updateMyMiningBlock(List<BlockEventData> blocks, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            for (BlockEventData blockEvent : blocks) {
                if(blockEvent == null || blockEvent.block == null){
                    continue;
                }
                Block block = blockEvent.block;

                saveMiningBlock(block, true, false);
                saveMiningReward(block, true);
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private synchronized void saveMiningBlock(Block block, boolean isConnect, boolean isSendNotify) {
        String blockNo = String.valueOf(block.getNumber());
        String blockHash = Hex.toHexString(block.getHash());

        String generatorPublicKey = KeyManager.signatureToKey(block);
        String pubicKey = KeyValueDaoUtils.getInstance().querySignatureKey(generatorPublicKey);
        if(StringUtil.isNotEmpty(pubicKey)){
            MiningBlock entry = MiningBlockDaoUtils.getInstance().queryByBlockHash(blockHash);
            if(entry == null){
                if(isConnect){
                    entry = new MiningBlock();
                    entry.setBlockNo(blockNo);
                    entry.setPubKey(pubicKey);
                    entry.setBlockHash(blockHash);
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
                    MiningBlockDaoUtils.getInstance().insertOrReplace(entry);

                    // TODO Whether to send notification or not
                    if(isSendNotify){
                        NotifyManager.getInstance().sendBlockNotify(reward);
                    }
                }
            }else{
                entry.setValid(isConnect ? 1 : 0);
                MiningBlockDaoUtils.getInstance().insertOrReplace(entry);
            }
        }

        String currentPubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(currentPubicKey);
        if(keyValue != null){
            keyValue.setSyncBlockNum((int)block.getNumber());
            KeyValueDaoUtils.getInstance().update(keyValue);
        }
    }

    private synchronized void saveMiningReward(Block block, boolean isConnect) {
        String generatorPublicKey = KeyManager.signatureToKey(block);
        KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(generatorPublicKey);
        // Update data for all local private keys
        if(keyValue != null){
            List<Transaction> transactions = block.getTransactionsList();
            if(transactions != null && transactions.size() > 0){
                for (Transaction reward : transactions) {
                    MiningReward entry = MiningRewardDaoUtils.getInstance().query(reward.getTxid(), keyValue.getPubKey());
                    if(null == entry){
                        String txFee = Hex.toHexString(reward.getFee());
                        String txId = reward.getTxid();
                        String txHash = Hex.toHexString(reward.getHash());
                        entry = new MiningReward();
                        entry.setTxId(txId);
                        entry.setTxHash(txHash);
                        entry.setFee(txFee);
                        entry.setPubKey(keyValue.getPubKey());
                        entry.setTime(DateUtil.getCurrentTime());
                        entry.setValid(1);
                        entry.setStatus(1);
                    }else{
                        entry.setValid(isConnect ? 1 : 0);
                        MiningRewardDaoUtils.getInstance().insertOrReplace(entry);
                    }
                }
            }
        }
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
                    saveMiningBlock(block, isConnect, true);
                    saveMiningReward(block, isConnect);
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
            if(transaction != null){
                String txId = transaction.getTxid();
                String result = transaction.TRANSACTION_STATUS;
                if(StringUtil.isSame(result, TaucoinImpl.TRANSACTION_SUBMITSUCCESS) ||
                        StringUtil.isSame(result, TaucoinImpl.TRANSACTION_RELAYTOREMOTE)){
                    MiningUtil.saveTransactionSuccess();
                    EventBusUtil.post(MessageEvent.EventCode.CLEAR_SEND);
                }else if(StringUtil.isSame(result, TaucoinImpl.TRANSACTION_SUBMITFAIL)){
                    new TxPresenter().sendRawTransaction(transaction, new LogicObserver<Boolean>() {
                        @Override
                        public void handleData(Boolean isSuccess) {
                            if(isSuccess){
                                // clear all editText data
                                EventBusUtil.post(MessageEvent.EventCode.CLEAR_SEND);
                            }
                        }
                    });
                }else{
                    if(StringUtil.isNotEmpty(result)){
                        ToastUtils.showShortToast(result);
                    }
                    MiningUtil.saveTransactionFail(txId, result);
                    EventBusUtil.post(MessageEvent.EventCode.CLEAR_SEND);
                }
            }
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @Override
    public void getMaxBlockNum(long height, LogicObserver<Long> logicObserver){
        Observable.create((ObservableOnSubscribe<Long>) emitter -> {
           String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
           KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(pubicKey);
           long maxBlockNum = 0;
           if(keyValue != null){
               maxBlockNum = keyValue.getSyncBlockNum();
           }
           if(maxBlockNum > height ){
               maxBlockNum = height;
           }
           emitter.onNext(maxBlockNum);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(logicObserver);
    }

    @Override
    public void getMiningRewards(int pageNo, String time, LogicObserver<List<MiningReward>> logicObserver){
        Observable.create((ObservableOnSubscribe<List<MiningReward>>) emitter -> {
            String pubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
            List<MiningReward> list = null;
            if(StringUtil.isNotEmpty(pubicKey)){
                list = MiningRewardDaoUtils.getInstance().queryData(pageNo, time, pubicKey);
            }
            if(list == null){
                list = new ArrayList<>();
            }
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(logicObserver);
    }
}