/**
 * Copyright 2018 TauCoin Core Developers.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.taucoin.android.wallet.core;
import com.mofei.tau.R;

import java.math.BigInteger;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.platform.adress.KeyManager;

public class Wallet {

    /**
     * validate transaction parameter
     */
    public synchronized static boolean validateTxParameter(TransactionHistory tx) {
        if (tx == null) {
            return false;
        }
        // validate receive address
        if (StringUtil.isEmpty(tx.getToAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            return false;
        }
        if (!KeyManager.validateAddress(tx.getToAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            return false;
        }
        // validate transaction amount
        if (StringUtil.isEmpty(tx.getAmount())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_amount);
            return false;
        }
        BigInteger minAmount = Constants.MIN_CHANGE;
        BigInteger maxAmount = Constants.MAX_MONEY;
        String amountStr = FmtMicrometer.fmtTxValue(tx.getAmount());
        BigInteger amount = new BigInteger(amountStr, 10);
        if (amount.compareTo(minAmount) < 0) {
            ToastUtils.showShortToast(TransactionFailReason.AMOUNT_TO_SMALL.getMsg());
            return false;
        } else if (amount.compareTo(maxAmount) >= 0) {
            ToastUtils.showShortToast(TransactionFailReason.AMOUNT_TO_LARGE.getMsg());
            return false;
        }
        // validate transaction fee
        if (StringUtil.isEmpty(tx.getFee())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_fee);
            return false;
        }
        tx.setAmount(amountStr);

        BigInteger minFee = Constants.DEFAULT_TX_FEE_MIN;
        BigInteger maxFee = Constants.DEFAULT_TX_FEE_MAX;
        String feeStr = FmtMicrometer.fmtTxValue(tx.getFee());
        BigInteger fee = new BigInteger(feeStr, 10);
        if (fee.compareTo(minFee) < 0) {
            ToastUtils.showShortToast(TransactionFailReason.TX_FEE_TOO_SMALL.getMsg());
            return false;
        } else if (fee.compareTo(maxFee) > 0) {
            ToastUtils.showShortToast(TransactionFailReason.TX_FEE_TOO_LARGE.getMsg());
            return false;
        }
        tx.setFee(feeStr);
        // validate balance is enough
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            String balanceStr = String.valueOf(keyValue.getBalance());
            BigInteger balance = new BigInteger(balanceStr, 10);
            if (balance.compareTo(amount.add(fee)) < 0) {
                ToastUtils.showShortToast(TransactionFailReason.NO_ENOUGH_COINS.getMsg());
                return false;
            }
        }
        return true;
    }
}