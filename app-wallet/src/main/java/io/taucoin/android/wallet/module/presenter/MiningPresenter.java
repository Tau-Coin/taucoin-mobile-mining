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
package io.taucoin.android.wallet.module.presenter;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.module.bean.ParticipantListBean;
import io.taucoin.android.wallet.module.model.IMiningModel;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.foundation.net.callback.LogicObserver;

public class MiningPresenter {
    private IMiningModel mMiningModel;
    private IHomeView mHomeView;

    public MiningPresenter() {
        mMiningModel = new MiningModel();
    }

    public MiningPresenter(IHomeView homeView) {
        mHomeView = homeView;
        mMiningModel = new MiningModel();
    }

    public void getMiningInfo(LogicObserver<BlockInfo> logicObserver) {
        mMiningModel.getMiningInfo(logicObserver);
    }

    public void updateMiningState(String miningState) {
        mMiningModel.updateMiningState(miningState, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean isSuccess) {
//                mHomeView.handleMiningView();
            }
        });
    }

    public void updateSyncState(String syncState) {
//        mMiningModel.updateSyncState(syncState, new LogicObserver<Boolean>() {
//            @Override
//            public void handleData(Boolean isSuccess) {
////                mHomeView.handleMiningView();
//            }
//        });
    }

    public void getParticipantInfo() {
        mMiningModel.getParticipantInfo(new LogicObserver<KeyValue>(){
            @Override
            public void handleError(int code, String msg) {
                if(mHomeView != null){
                    mHomeView.handleRewardView();
                }
            }

            @Override
            public void handleData(KeyValue keyValue) {
                if(keyValue != null){
                    MyApplication.setKeyValue(keyValue);
                }
                if(mHomeView != null){
                    mHomeView.handleRewardView();
                }
            }
        });
    }

    public void getMinerHistory(LogicObserver<List<MinerListBean.MinerBean>> logicObserver) {
        mMiningModel.getMinerHistory(new TxObserver<MinerListBean>() {
            @Override
            public void handleError(String msg, int msgCode) {
                handleData();
            }
            @Override
            public void handleData(MinerListBean historyList) {
                if(historyList != null && historyList.getStatus() == 200){
                    if(historyList.getMinerHistory() != null){
                        logicObserver.onNext(historyList.getMinerHistory());
                    }else{
                        handleData();
                    }
                }
            }
            private void handleData(){
                logicObserver.onNext(new ArrayList<>());
            }
        });
    }

    public void getParticipantHistory(LogicObserver<List<ParticipantListBean.ParticipantBean>> logicObserver) {
        mMiningModel.getParticipantHistory(new TxObserver<ParticipantListBean>() {
            @Override
            public void handleError(String msg, int msgCode) {
                handleData();
            }
            @Override
            public void handleData(ParticipantListBean historyList) {
                if(historyList != null && historyList.getStatus() == 200){
                    if(historyList.getPartHistory() != null){
                        logicObserver.onNext(historyList.getPartHistory());
                    }else{
                        handleData();
                    }
                }
            }
            private void handleData(){
                logicObserver.onNext(new ArrayList<>());
            }
        });
    }
}