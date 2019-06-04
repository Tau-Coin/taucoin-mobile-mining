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
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.greendao.BlockInfoDao;

/**
 * @version 1.0
 * Edited by yang
 * @description: mining info
 */
public class BlockInfoDaoUtils {

    private final GreenDaoManager daoManager;
    private static BlockInfoDaoUtils mBlockInfoDaoUtils;

    private BlockInfoDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static BlockInfoDaoUtils getInstance() {
        if (mBlockInfoDaoUtils == null) {
            mBlockInfoDaoUtils = new BlockInfoDaoUtils();
        }
        return mBlockInfoDaoUtils;
    }

    private BlockInfoDao getBlockInfoDao() {
        return daoManager.getDaoSession().getBlockInfoDao();
    }

    public int getBlockHeight() {
        BlockInfo blockInfo = query();
        if(blockInfo != null){
            return blockInfo.getBlockHeight();
        }
        return 0;
    }

    public BlockInfo query() {
        List<BlockInfo> list = getBlockInfoDao().queryBuilder()
                .orderDesc(BlockInfoDao.Properties.Id)
                .list();
        if(list != null && list.size() > 0){
            return list.get(0);
        }
        return null;
    }

    public synchronized void insertOrReplace(BlockInfo blockInfo) {
        getBlockInfoDao().insertOrReplace(blockInfo);
    }

    public void reloadBlocks() {
        BlockInfo blockInfo = query();
        if(blockInfo != null){
            blockInfo.setBlockSync(0);
            insertOrReplace(blockInfo);
        }
    }
}