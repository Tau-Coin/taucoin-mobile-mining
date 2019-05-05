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
package io.taucoin.android.wallet.db.util;

import java.util.List;

import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.MiningBlock;
import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.db.greendao.MiningBlockDao;
import io.taucoin.android.wallet.db.greendao.MiningRewardDao;

/**
 * @version 1.0
 * Edited by yang
 *  mining reward
 */
public class MiningRewardDaoUtils {

    private final GreenDaoManager daoManager;
    private static MiningRewardDaoUtils mMiningDaoUtils;

    private MiningRewardDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static MiningRewardDaoUtils getInstance() {
        if (mMiningDaoUtils == null) {
            mMiningDaoUtils = new MiningRewardDaoUtils();
        }
        return mMiningDaoUtils;
    }

    private MiningRewardDao getMiningRewardDao() {
        return daoManager.getDaoSession().getMiningRewardDao();
    }


    public List<MiningReward> queryByPubicKey(String pubicKey) {
        return getMiningRewardDao().queryBuilder()
                .where(MiningRewardDao.Properties.PubKey.eq(pubicKey),
                        MiningRewardDao.Properties.Valid.eq(1))
                .orderDesc(MiningRewardDao.Properties.Id)
                .list();
    }

    public synchronized boolean insertOrReplace(MiningReward miningReward) {
        long result = getMiningRewardDao().insertOrReplace(miningReward);
        return result > -1;
    }

    public MiningReward queryByTxHash(String txHash) {
        List<MiningReward> list = getMiningRewardDao().queryBuilder()
                .where(MiningRewardDao.Properties.TxHash.eq(txHash))
                .orderDesc(MiningRewardDao.Properties.Id)
                .list();
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }
}