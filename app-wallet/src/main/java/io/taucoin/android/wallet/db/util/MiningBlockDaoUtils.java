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
import io.taucoin.android.wallet.db.greendao.MiningBlockDao;

/**
 * @version 1.0
 * Edited by yang
 *  mining block
 */
public class MiningBlockDaoUtils {

    private final GreenDaoManager daoManager;
    private static MiningBlockDaoUtils mMiningDaoUtils;

    private MiningBlockDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static MiningBlockDaoUtils getInstance() {
        if (mMiningDaoUtils == null) {
            mMiningDaoUtils = new MiningBlockDaoUtils();
        }
        return mMiningDaoUtils;
    }

    private MiningBlockDao getMiningBlockDao() {
        return daoManager.getDaoSession().getMiningBlockDao();
    }


    public List<MiningBlock> queryByPubicKey(String pubicKey) {
        return getMiningBlockDao().queryBuilder()
                .where(MiningBlockDao.Properties.PubKey.eq(pubicKey),
                        MiningBlockDao.Properties.Valid.eq(1))
                .orderDesc(MiningBlockDao.Properties.Id)
                .list();
    }

    public synchronized boolean insertOrReplace(MiningBlock miningInfo) {
        long result = getMiningBlockDao().insertOrReplace(miningInfo);
        return result > -1;
    }

    public MiningBlock queryByBlockHash(String blockHash, String pubicKey) {
        List<MiningBlock> list = getMiningBlockDao().queryBuilder()
                .where(MiningBlockDao.Properties.BlockHash.eq(blockHash),
                    MiningBlockDao.Properties.PubKey.eq(pubicKey))
                .orderDesc(MiningBlockDao.Properties.Id)
                .list();
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public void rollbackByBlockHash(String blockHash) {
        List<MiningBlock> list = getMiningBlockDao().queryBuilder()
                .where(MiningBlockDao.Properties.BlockHash.eq(blockHash))
                .orderDesc(MiningBlockDao.Properties.Id)
                .list();
        if(list != null && list.size() > 0){
            for (MiningBlock block : list) {
                block.setValid(0);
                insertOrReplace(block);
            }
        }
    }

    public void clear() {
        getMiningBlockDao().deleteAll();
    }
}