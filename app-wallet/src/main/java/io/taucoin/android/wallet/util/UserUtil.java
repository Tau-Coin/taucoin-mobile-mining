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

import java.math.BigDecimal;
import java.math.BigInteger;

import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.widget.LoadingTextView;
import io.taucoin.android.wallet.widget.ProgressView;
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

    public static void setPower(TextView tvPower, ProgressView ivMiningPower) {
        if(tvPower == null || ivMiningPower == null){
            return;
        }
        String power = "0";
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue != null){
            power = String.valueOf(keyValue.getPower());
            if(keyValue.getPower() <= 0 || MyApplication.getRemoteConnector().getErrorCode() == 3){
                ivMiningPower.setOff();
                ivMiningPower.setEnabled(true);
            }else {
                ivMiningPower.setOn();
                ivMiningPower.setEnabled(false);
            }
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

    public static String getTransExpiryTime(long blocks) {
        return String.valueOf(blocks * 5);
    }

    public static void setApplicationInfo(TextView tvCPU, TextView tvMemory, TextView tvDataStorage, Object data) {
        try{
            if(data != null){
                NotifyManager.NotifyData notifyData = (NotifyManager.NotifyData)data;
                if(StringUtil.isNotEmpty(notifyData.cpuUsage)){
                    String cpuUsage = notifyData.cpuUsage.substring(0, notifyData.cpuUsage.length() - 1);
                    tvCPU.setText(cpuUsage);
                }
                if(StringUtil.isNotEmpty(notifyData.memorySize)){
                    String memorySize = notifyData.memorySize.substring(0, notifyData.memorySize.length() - 1);
                    tvMemory.setText(memorySize);
                }
                if(StringUtil.isNotEmpty(notifyData.netDataSize)){
                    String dataSize = notifyData.netDataSize.substring(0, notifyData.netDataSize.length() - 1);
                    tvDataStorage.setText(dataSize);
                }
            }
        }catch (Exception ignore){

        }
    }

    public static void setBalanceAndSync(ProgressView ivMiningBalance, ProgressView ivMiningSync, Object data) {
        try{
            if(data != null){
                BlockInfo blockInfo = (BlockInfo)data;
                KeyValue keyValue = MyApplication.getKeyValue();
                BigDecimal balance = new BigDecimal(keyValue.getBalance());
                BigDecimal medianFee = new BigDecimal(BigInteger.ZERO);
                if(StringUtil.isNotEmpty(blockInfo.getMedianFee())){
                    medianFee = new BigDecimal(blockInfo.getMedianFee());
                }
                if(balance.compareTo(medianFee) < 0 || MyApplication.getRemoteConnector().getErrorCode() == 4){
                    ivMiningBalance.setOff();
                    ivMiningBalance.setEnabled(true);
                }else {
                    ivMiningBalance.setOn();
                    ivMiningBalance.setEnabled(false);
                }
                if(blockInfo.getBlockHeight() != 0 && blockInfo.getBlockHeight() == blockInfo.getBlockSync() ){
                    ivMiningSync.setOn();
                    ivMiningSync.setEnabled(false);
                }else {
                    ivMiningSync.setOff();
                    ivMiningSync.setEnabled(true);
                }
            }
        }catch (Exception ignore){

        }
    }
}