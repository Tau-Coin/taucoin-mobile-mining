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
import io.taucoin.android.wallet.R;

import java.math.BigInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.platform.adress.KeyManager;

public class Wallet {

    /**
     * validate transaction parameter
     */
    public synchronized static void validateTxParameter(TransactionHistory tx, LogicObserver<Boolean> logicObserver) {
        if (tx == null) {
            logicObserver.onNext(false);
            return;
        }
        // validate receive address
        if (StringUtil.isEmpty(tx.getToAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            logicObserver.onNext(false);
            return;
        }
        if (!KeyManager.validateAddress(tx.getToAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            logicObserver.onNext(false);
            return;
        }
        // validate transaction amount
        if (StringUtil.isEmpty(tx.getAmount())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_amount);
            logicObserver.onNext(false);
            return;
        }
        BigInteger minAmount = Constants.MIN_AMOUNT;
        BigInteger maxAmount = Constants.MAX_AMOUNT;
        String amountStr = FmtMicrometer.fmtTxValue(tx.getAmount());
        BigInteger amount = new BigInteger(amountStr, 10);
        if (amount.compareTo(minAmount) < 0) {
            ToastUtils.showShortToast(R.string.send_amount_too_low);
            logicObserver.onNext(false);
            return;
        } else if (amount.compareTo(maxAmount) >= 0) {
            ToastUtils.showShortToast(R.string.send_amount_too_high);
            logicObserver.onNext(false);
            return;
        }
        // validate transaction fee
        if (StringUtil.isEmpty(tx.getFee())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_fee);
            logicObserver.onNext(false);
            return;
        }
        tx.setAmount(amountStr);

        BigInteger minFee = Constants.MIN_FEE;
        BigInteger maxFee = Constants.MAX_FEE;
        String feeStr = FmtMicrometer.fmtTxValue(tx.getFee());
        BigInteger fee = new BigInteger(feeStr, 10);
        if (fee.compareTo(minFee) < 0) {
            ToastUtils.showShortToast(R.string.send_fee_too_low);
            logicObserver.onNext(false);
            return;
        } else if (fee.compareTo(maxFee) > 0) {
            ToastUtils.showShortToast(R.string.send_fee_too_high);
            logicObserver.onNext(false);
            return;
        }
        tx.setFee(feeStr);
        // validate balance is enough
        Observable.create((ObservableOnSubscribe<Long>) emitter -> {
            KeyValue keyValue = MyApplication.getKeyValue();
            long balance = keyValue.getBalance();
            balance -= MiningUtil.pendingAmount();
            balance = balance < 0 ? 0 : balance;
            emitter.onNext(balance);
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<Long>() {
                @Override
                public void handleData(Long balance) {
                    BigInteger balanceBig = new BigInteger(balance.toString(), 10);
                    if (balanceBig.compareTo(amount.add(fee)) < 0) {
                        ToastUtils.showShortToast(R.string.send_no_enough_coins);
                        logicObserver.onNext(false);
                    }else{
                        logicObserver.onNext(true);
                    }
                }
            });
    }
}