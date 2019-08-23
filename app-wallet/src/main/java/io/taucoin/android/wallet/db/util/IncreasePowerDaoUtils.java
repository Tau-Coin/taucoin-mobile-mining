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
import io.taucoin.android.wallet.db.entity.IncreasePower;
import io.taucoin.android.wallet.db.greendao.IncreasePowerDao;

/**
 * @version 1.0
 * Edited by yang
 */
public class IncreasePowerDaoUtils {

    private final GreenDaoManager daoManager;
    private static IncreasePowerDaoUtils mIncreasePowerDaoUtils;

    private IncreasePowerDaoUtils() {
        daoManager = GreenDaoManager.getInstance();
    }

    public static IncreasePowerDaoUtils getInstance() {
        if (mIncreasePowerDaoUtils == null) {
            mIncreasePowerDaoUtils = new IncreasePowerDaoUtils();
        }
        return mIncreasePowerDaoUtils;
    }

    private IncreasePowerDao getIncreasePowerDao() {
        return daoManager.getDaoSession().getIncreasePowerDao();
    }

    public List<IncreasePower> queryPoolByAddress(String address) {
        return getIncreasePowerDao().queryBuilder()
            .where(IncreasePowerDao.Properties.Count.gt(0),
                    IncreasePowerDao.Properties.Address.eq(address))
            .orderAsc(IncreasePowerDao.Properties.Id)
            .list();
    }

    public void update(IncreasePower entry) {
        getIncreasePowerDao().insertOrReplace(entry);
    }
}