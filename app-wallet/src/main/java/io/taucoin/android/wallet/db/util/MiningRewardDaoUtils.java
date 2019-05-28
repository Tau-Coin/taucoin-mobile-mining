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

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.GreenDaoManager;
import io.taucoin.android.wallet.db.entity.MiningReward;
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


    public List<MiningReward> queryData(int pageNo, String time, String rawAddress) {
        QueryBuilder<MiningReward> qb = getMiningRewardDao().queryBuilder();
        qb.where(MiningRewardDao.Properties.Time.lt(time),
            MiningRewardDao.Properties.Address.eq(rawAddress),
            MiningRewardDao.Properties.Valid.eq(1),
            qb.or(MiningRewardDao.Properties.MinerFee.gt(0),
                    MiningRewardDao.Properties.PartFee.gt(0)))
            .orderDesc(MiningRewardDao.Properties.Time)
            .offset((pageNo - 1) * TransmitKey.PAGE_SIZE).limit(TransmitKey.PAGE_SIZE);
        return qb.list();
    }

    public List<MiningReward> queryData(String rawAddress) {
        QueryBuilder<MiningReward> qb = getMiningRewardDao().queryBuilder();
        qb.where(MiningRewardDao.Properties.Address.eq(rawAddress),
                MiningRewardDao.Properties.Valid.eq(1),
                qb.or(MiningRewardDao.Properties.MinerFee.gt(0),
                    MiningRewardDao.Properties.PartFee.gt(0)));
        return qb.list();
    }

    public synchronized boolean insertOrReplace(MiningReward miningReward) {
        long result = getMiningRewardDao().insertOrReplace(miningReward);
        return result > -1;
    }

    public List<MiningReward> queryData(String blockHash, String rawAddress) {
        return getMiningRewardDao().queryBuilder()
                .where(MiningRewardDao.Properties.BlockHash.eq(blockHash),
                        MiningRewardDao.Properties.Address.eq(rawAddress))
                .orderDesc(MiningRewardDao.Properties.Id)
                .list();
    }

    public MiningReward query(String blockHash, String txHash, String rawAddress) {
        List<MiningReward> list = getMiningRewardDao().queryBuilder()
                .where(MiningRewardDao.Properties.BlockHash.eq(blockHash),
                        MiningRewardDao.Properties.TxHash.eq(txHash),
                        MiningRewardDao.Properties.Address.eq(rawAddress))
                .orderDesc(MiningRewardDao.Properties.Id)
                .list();
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public void rollBackByBlockHash(String blockHash) {
        List<MiningReward> list = getMiningRewardDao().queryBuilder()
                .where(MiningRewardDao.Properties.BlockHash.eq(blockHash))
                .list();
        if(list != null && list.size() > 0){
            for (MiningReward reward : list) {
                reward.setValid(0);
                insertOrReplace(reward);
            }
        }
    }
}