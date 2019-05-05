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

import android.text.Html;
import android.view.View;
import android.widget.TextView;

import com.github.naturs.logger.Logger;
import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.foundation.util.StringUtil;

public class UserUtil {

    private static String parseNickName(KeyValue keyValue) {
        String nickName = null;
        if(keyValue != null){
            nickName = keyValue.getNickName();
            if(StringUtil.isEmpty(nickName)){
                String address = keyValue.getAddress();
                if(StringUtil.isNotEmpty((address))){
                    int length = address.length();
                    nickName = length < 6 ? address : address.substring(length - 6);
                }
            }
        }
        return nickName;
    }

    public static void setNickName(TextView tvNick) {
        if(tvNick == null){
            return;
        }
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            return;
        }
        String nickName = parseNickName(keyValue);
        tvNick.setText(nickName);
        Logger.d("UserUtil.setNickName=" + nickName);
    }

    public static void setBalance(TextView tvBalance) {
        if(tvBalance == null){
            return;
        }
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            setBalance(tvBalance, 0L);
            return;
        }
        setBalance(tvBalance, keyValue.getBalance());
//        Observable.create((ObservableOnSubscribe<Long>) emitter -> {
//            long balance = keyValue.getBalance();
//            balance -= MiningUtil.pendingAmount();
//            balance = balance < 0 ? 0 : balance;
//            emitter.onNext(balance);
//        }).observeOn(AndroidSchedulers.mainThread())
//            .subscribeOn(Schedulers.io())
//            .subscribe(new LogicObserver<Long>() {
//                @Override
//                public void handleData(Long balance) {
//                    setBalance(tvBalance, balance);
//                }
//            });
    }

    private static void setBalance(TextView tvBalance, long balance) {
        String balanceStr = MyApplication.getInstance().getResources().getString(R.string.common_balance);
        balanceStr = String.format(balanceStr, FmtMicrometer.fmtBalance(balance));
        tvBalance.setText(Html.fromHtml(balanceStr));
        Logger.d("UserUtil.setBalance=" + balanceStr);
    }

    public static void setPower(TextView tvPower) {
        if(tvPower == null){
            return;
        }
        String power = "0";
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue != null){
            power = String.valueOf(keyValue.getPower());
        }
        tvPower.setText(power);
        Logger.d("UserUtil.setPower=" + power);
    }

    public static boolean isImportKey() {
        KeyValue keyValue = MyApplication.getKeyValue();
        return  keyValue != null;
    }

    public static void setAddress(TextView tvAddress) {
        if(tvAddress == null){
            return;
        }
        KeyValue keyValue = MyApplication.getKeyValue();
        String newAddress = "";
        if(keyValue != null){
            newAddress = keyValue.getAddress();
        }
        String oldAddress = StringUtil.getText(tvAddress);
        if(StringUtil.isNotSame(newAddress, oldAddress)){
            String address = MyApplication.getInstance().getResources().getString(R.string.send_tx_address);
            address = String.format(address, newAddress);
            tvAddress.setText(address);
        }
        int visibility = StringUtil.isEmpty(newAddress) ? View.GONE : View.VISIBLE;
        tvAddress.setVisibility(visibility);
    }

    public static long getTransExpiryTime() {
        long minTime = TransmitKey.MIN_TRANS_EXPIRY;
        long maxTime = TransmitKey.MAX_TRANS_EXPIRY;
        long expiryTime = maxTime;
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue != null && keyValue.getTransExpiry() >= minTime
                && keyValue.getTransExpiry() <= maxTime){
            expiryTime = keyValue.getTransExpiry();
        }
        return expiryTime;
    }

    public static long getTransExpiryBlock() {
        return getTransExpiryTime() / 5;
    }
}