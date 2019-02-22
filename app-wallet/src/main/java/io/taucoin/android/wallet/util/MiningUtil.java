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
package io.taucoin.android.wallet.util;

import com.github.naturs.logger.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.core.Transaction;

public class MiningUtil {

    public static int parseMinedBlocks(KeyValue keyValue) {
        if(keyValue != null){
            List<MiningInfo> list= keyValue.getMiningInfos();
            if(list != null){
                return list.size();
            }
        }
        return 0;
    }

    public static String parseMiningIncome(KeyValue keyValue) {
        BigDecimal number = new BigDecimal("0");
        if(keyValue != null){
            List<MiningInfo> list= keyValue.getMiningInfos();
            if(list != null && list.size() > 0){
                for (MiningInfo bean : list) {
                    try {
                        number = number.add(new BigDecimal(bean.getReward()));
                    }catch (Exception ignore){}
                }
            }
        }
        return FmtMicrometer.fmtFormat(number.toString());
    }

    public static void setBlockHeight(ItemTextView textView) {
        if(textView == null){
            return;
        }
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            return;
        }
        int blockHeight = keyValue.getBlockHeight();
        textView.setRightText(blockHeight);
        Logger.d("UserUtil.setBlockHeight=" + blockHeight);
    }

    public static String parseBlockReward(List<Transaction> txList) {
        BigInteger reward = new BigInteger("0");
        if(txList != null && txList.size() > 0){
            for (Transaction transaction : txList) {
                BigInteger fee = new BigInteger(transaction.getFee());
                reward = reward.add(fee);
            }
        }
        return reward.toString();
    }
}