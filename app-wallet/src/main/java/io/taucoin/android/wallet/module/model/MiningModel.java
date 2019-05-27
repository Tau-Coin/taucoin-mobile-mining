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

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.db.util.MiningRewardDaoUtils;
import io.taucoin.android.wallet.module.bean.BlockNoComparator;
import io.taucoin.android.wallet.module.bean.RewardBean;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.core.TransactionExecuatedOutcome;

import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("MiningModel");
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
            Collections.sort(list, new BlockNoComparator());
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
    public void updateMyMiningBlock(BlockEventData blockData, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            Block block = blockData.block;
            if(block == null){
                emitter.onNext(false);
                return;
            }
            saveMiningBlock(block, true, true);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    private synchronized void saveMiningBlock(Block block, boolean isConnect, boolean isNeedSync) {
        String generatorPublicKey = Hex.toHexString(block.getForgerPublicKey());
//        Logger.d("generatorPublicKey=" + generatorPublicKey);
        String currentPubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        if(StringUtil.isSame(generatorPublicKey.toLowerCase(), currentPubicKey.toLowerCase())){
            saveMiningBlock(block, isConnect, 0);// miner
        }else{
            List<Transaction> transactions = block.getTransactionsList();
            String rawAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.RAW_ADDRESS, "");
            if(transactions != null && transactions.size() > 0){
                for (Transaction transaction : transactions) {
                    if(transaction.getSenderWitnessAddress() != null){
                        String witnessAddress = Hex.toHexString(transaction.getSenderWitnessAddress());
                        boolean isSave = false;
                        if(StringUtil.isSame(rawAddress, witnessAddress)){
                            isSave = true;
                        }else{
                            List<byte[]> addresses = transaction.getSenderAssociatedAddress();
                            if(addresses != null && addresses.size() > 0){
                                for (byte[] address : addresses) {
                                    String associatedAddress = Hex.toHexString(address);
                                    if(StringUtil.isSame(rawAddress, associatedAddress)){
                                        isSave = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if(isSave){
                            saveMiningBlock(block, isConnect, 1);// participated
                            break;
                        }
                    }
                }
            }
        }

        if(isNeedSync){
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(currentPubicKey);
            if(keyValue != null){
                keyValue.setSyncBlockNum((int)block.getNumber());
                KeyValueDaoUtils.getInstance().update(keyValue);
            }
        }
    }

    private void saveMiningBlock(Block block, boolean isConnect, int type) {
        String blockNo = String.valueOf(block.getNumber());
        String blockHash = Hex.toHexString(block.getHash());
//        Logger.d("blockNo=" + blockNo);
//        Logger.d("blockHash=" + blockHash);
        String currentPubicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        MiningBlock entry = MiningBlockDaoUtils.getInstance().queryByBlockHash(blockHash, currentPubicKey);
        if(entry == null){
            if(isConnect){
                entry = new MiningBlock();
                entry.setBlockNo(blockNo);
                entry.setPubKey(currentPubicKey);
                entry.setBlockHash(blockHash);
                entry.setValid(1);
                entry.setType(type);

                List<Transaction> txList = block.getTransactionsList();
                int total = 0;
                String reward = "0";
                if(txList != null){
                    total = txList.size();
                    reward = MiningUtil.parseBlockTxFee(txList);
                }
                entry.setTotal(total);
                entry.setReward(reward);
                MiningBlockDaoUtils.getInstance().insertOrReplace(entry);
            }
        }else{
            entry.setValid(isConnect ? 1 : 0);
            entry.setValid(type);
            MiningBlockDaoUtils.getInstance().insertOrReplace(entry);
            if(!isConnect){
                MiningBlockDaoUtils.getInstance().rollbackByBlockHash(blockHash);
            }
        }
    }

    @Override
    public synchronized void saveMiningReward(TransactionExecuatedOutcome outCome) {
        if(outCome == null){
            return;
        }
        saveMiningReward(outCome.getCurrentWintess(), outCome, true);
        saveMiningReward(outCome.getLastWintess(), outCome, false);
        saveMiningReward(outCome.getSenderAssociated(), outCome, false);

        if(outCome.isTxComplete()){
            String blockHash = Hex.toHexString(outCome.getBlockhash());
            handleRefreshNotify(blockHash, false);
        }
    }

    private synchronized void saveMiningReward(Map<byte[], Long> addressMap, TransactionExecuatedOutcome outCome, boolean isMiner) {
        if(null == addressMap || addressMap.size() == 0){
            return;
        }
        String blockHash = Hex.toHexString(outCome.getBlockhash());
        String txHash = Hex.toHexString(outCome.getTxid());
        String formAddress = Hex.toHexString(outCome.getSenderAddress());
        String toAddress = Hex.toHexString(outCome.getReceiveAddress());
//        Logger.i("*****************************");
//        Logger.i("txHash=" + txHash);
//        Logger.i("form=" + formAddress + "\tto=" +toAddress);
        for (Map.Entry<byte[], Long> entry : addressMap.entrySet()) {
            String rewardAddress = Hex.toHexString(entry.getKey());
            long rewardFee = entry.getValue();
//            Logger.i("reward=" + rewardAddress + "\t," + rewardFee);
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByRawAddress(rewardAddress);
            // Update data for all local private keys
            if (keyValue != null) {
                MiningReward reward = MiningRewardDaoUtils.getInstance().query(txHash, keyValue.getRawAddress());
                if (null == reward) {
                    reward = new MiningReward();
                    reward.setTxHash(txHash);
                    reward.setAddress(keyValue.getRawAddress());
                    reward.setTime(DateUtil.getDateTime());
                    reward.setSenderAddress(formAddress);
                    reward.setReceiverAddress(toAddress);
                } else {
                    if (reward.getValid() == 0) {
                        reward.setMinerFee(0);
                        reward.setPartFee(0);
                        reward.setTime(DateUtil.getDateTime());
                    }
                }
                reward.setBlockHash(blockHash);
                reward.setValid(1);
                if (isMiner) {
                    reward.setMinerFee(rewardFee);
                } else {
                    reward.setPartFee(rewardFee + reward.getPartFee());
                }
                MiningRewardDaoUtils.getInstance().insertOrReplace(reward);
            }
            logger.info("in executation blockHash={}, txHash={} , rewardAddress={}, rewardFee={}", blockHash, txHash, rewardAddress, rewardFee);
        }
    }

    private void handleRefreshNotify(String blockHash, boolean isRollBack) {
        String rawAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.RAW_ADDRESS, "");
        List<MiningReward> list = MiningRewardDaoUtils.getInstance().queryData(blockHash, rawAddress);
        if(list.size() > 0){
            EventBusUtil.post(MessageEvent.EventCode.MINING_REWARD);
            int notifyRes;
            long reward = 0;
            String notifyStr;
            RewardBean bean = MiningUtil.parseMiningReward(list);
            if(isRollBack){
                notifyRes = R.string.income_miner_rollback;
                notifyStr = ResourcesUtil.getText(notifyRes);
            }else{
                if(bean.getMinerReward() > 0 && bean.getPartReward() > 0){
                    notifyRes = R.string.income_miner_participant;
                    reward = bean.getTotalReward();
                }else if(bean.getMinerReward() > 0){
                    notifyRes = R.string.income_miner;
                    reward = bean.getMinerReward();
                }else{
                    notifyRes = R.string.income_participant;
                    reward = bean.getPartReward();
                }
                notifyStr = ResourcesUtil.getText(notifyRes);
                String rewardStr = FmtMicrometer.fmtFormat(String.valueOf(reward));
                notifyStr = String.format(notifyStr, rewardStr);
            }
            if(isRollBack || reward > 0){
                NotifyManager.getInstance().sendBlockNotify(notifyStr);
            }
            logger.info("in executation total:" + notifyStr);
        }
    }

    /**
     * roll back mining reward
     * */
    private synchronized void rollBackMiningReward(String blockHash) {
        MiningRewardDaoUtils.getInstance().rollBackByBlockHash(blockHash);
        handleRefreshNotify(blockHash, true);
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
                    saveMiningBlock(block, isConnect, MyApplication.getRemoteConnector().isSyncMe());

                    if(!isConnect){
                        String blockHash = Hex.toHexString(block.getHash());
                        rollBackMiningReward(blockHash);
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
            if(transaction != null){
                String txId = transaction.getTxid();
                String result = transaction.TRANSACTION_STATUS;
                if(StringUtil.isSame(result, TaucoinImpl.TRANSACTION_SUBMITSUCCESS) ||
                        StringUtil.isSame(result, TaucoinImpl.TRANSACTION_RELAYTOREMOTE)){
                    MiningUtil.saveTransactionSuccess();
                    EventBusUtil.post(MessageEvent.EventCode.CLEAR_SEND);
                }else if(StringUtil.isSame(result, TaucoinImpl.TRANSACTION_SUBMITFAIL)){
//                    new TxPresenter().sendRawTransaction(transaction, new LogicObserver<Boolean>() {
//                        @Override
//                        public void handleData(Boolean isSuccess) {
//                            if(isSuccess){
//                                // clear all editText data
//                                EventBusUtil.post(MessageEvent.EventCode.CLEAR_SEND);
//                            }
//                        }
//                    });
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
            String rawAddress = SharedPreferencesHelper.getInstance().getString(TransmitKey.RAW_ADDRESS, "");
            List<MiningReward> list = null;
            if(StringUtil.isNotEmpty(rawAddress)){
                list = MiningRewardDaoUtils.getInstance().queryData(pageNo, time, rawAddress);
            }
            if(list == null){
                list = new ArrayList<>();
            }
            emitter.onNext(list);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
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
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }
}