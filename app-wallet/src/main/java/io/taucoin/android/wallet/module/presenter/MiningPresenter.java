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

import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.module.model.IMiningModel;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
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

    public void updateMiningState() {
        mMiningModel.updateMiningState(new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean isSuccess) {
                mHomeView.handleMiningView();
            }
        });
    }
}