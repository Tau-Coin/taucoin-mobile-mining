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
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.R;

import java.math.BigInteger;
import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.IncreasePower;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.util.IncreasePowerDaoUtils;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
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
        String amountStr = FmtMicrometer.fmtTxValue(tx.getAmount());
        BigInteger amount = new BigInteger(amountStr, 10);
        if (amount.compareTo(minAmount) < 0) {
            ToastUtils.showShortToast(R.string.send_amount_too_low);
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
        String feeStr = FmtMicrometer.fmtTxValue(tx.getFee());
        BigInteger fee = new BigInteger(feeStr, 10);
        if (fee.compareTo(minFee) < 0) {
            ToastUtils.showShortToast(R.string.send_fee_too_low);
            logicObserver.onNext(false);
            return;
        }
        tx.setFee(feeStr);
        // validate balance is enough
        KeyValue keyValue = MyApplication.getKeyValue();
        long balance = keyValue.getBalance();
        BigInteger balanceBig = new BigInteger(String.valueOf(balance), 10);
        if (balanceBig.compareTo(amount.add(fee)) < 0) {
            ToastUtils.showShortToast(R.string.send_no_enough_coins);
            logicObserver.onNext(false);
        }else{
            logicObserver.onNext(true);
        }
    }

    public static void validateTxBudget(IncreasePower budget, LogicObserver<Boolean> logicObserver) {
        if (budget == null) {
            logicObserver.onNext(false);
            return;
        }
        // validate receive address
        if (StringUtil.isEmpty(budget.getAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            logicObserver.onNext(false);
            return;
        }
        if (!KeyManager.validateAddress(budget.getAddress())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_address);
            logicObserver.onNext(false);
            return;
        }
        // validate transaction amount
        if (StringUtil.isEmpty(budget.getBudget())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_budget);
            logicObserver.onNext(false);
            return;
        }

        BigInteger minAmount = Constants.MIN_AMOUNT;
        String amountStr = FmtMicrometer.fmtTxValue(budget.getBudget());
        BigInteger amount = new BigInteger(amountStr, 10);
        if (amount.compareTo(minAmount) < 0) {
            ToastUtils.showShortToast(R.string.send_budget_too_low);
            logicObserver.onNext(false);
            return;
        }
        budget.setBudget(amountStr);
        String feeStr = FmtMicrometer.fmtTxValue(budget.getFee());
        BigInteger fee = new BigInteger(feeStr, 10);
        if (amount.compareTo(fee) < 0) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_budget_no_enough);
            logicObserver.onNext(false);
            return;
        }
        // validate transaction fee
        if (StringUtil.isEmpty(budget.getFee())) {
            ToastUtils.showShortToast(R.string.send_tx_invalid_fee);
            logicObserver.onNext(false);
            return;
        }

        BigInteger minFee = Constants.MIN_FEE;
        if (fee.compareTo(minFee) < 0) {
            ToastUtils.showShortToast(R.string.send_fee_too_low);
            logicObserver.onNext(false);
            return;
        }
        budget.setFee(feeStr);
        // validate balance is enough
        KeyValue keyValue = MyApplication.getKeyValue();
        long balance = keyValue.getBalance();
        BigInteger balanceBig = new BigInteger(String.valueOf(balance), 10);
        if (balanceBig.compareTo(amount.add(fee)) < 0) {
            ToastUtils.showShortToast(R.string.send_no_enough_coins_budget);
            logicObserver.onNext(false);
        }
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
            List<IncreasePower> list = IncreasePowerDaoUtils.getInstance().queryPoolByAddress(address);
            if(list != null && list.size() > 0){
                emitter.onNext(false);
            }else{
                emitter.onNext(true);
            }
        }).observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .subscribe(new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean aBoolean) {
                    if(!aBoolean){
                        ToastUtils.showShortToast(R.string.send_budget_in_pool);
                        MiningUtil.sendingBudgetTransaction();
                    }
                    logicObserver.onNext(aBoolean);
                }
            });
        }
}