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
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import org.json.JSONObject;

import java.math.BigInteger;
import java.util.Iterator;

import io.taucoin.android.service.events.NextBlockForgedPOTDetail;
import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.widget.DashboardLayout;
import io.taucoin.android.wallet.widget.ProgressView;
import io.taucoin.foundation.util.DimensionsUtil;
import io.taucoin.foundation.util.DrawablesUtil;
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
        setBalance(tvBalance, false);
    }

    public static void setBalance(TextView tvBalance, boolean isSpan) {
        if(tvBalance == null){
            return;
        }
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null){
            setBalance(tvBalance, 0L, isSpan);
            return;
        }
        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        if(StringUtil.isNotSame(address, keyValue.getAddress())){
            TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
            return;
        }
        setBalance(tvBalance, keyValue.getBalance(), isSpan);
    }

    private static void setBalance(TextView tvBalance, long balance, boolean isSpan) {
        if(isSpan){
            SpannableStringBuilder spannableBalance = new SpanUtils()
                .append(FmtMicrometer.fmtBalance(balance))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_yellow))
                .append(" ")
                .append(ResourcesUtil.getText(R.string.common_balance_unit))
                .setFontSize(DimensionsUtil.dip2px(tvBalance.getContext(), 16))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_black))
                .create();
            tvBalance.setText(spannableBalance);
        }else{
            String balanceStr = MyApplication.getInstance().getResources().getString(R.string.common_balance);
            balanceStr = String.format(balanceStr, FmtMicrometer.fmtBalance(balance));
            tvBalance.setText(Html.fromHtml(balanceStr));
        }
        Logger.d("UserUtil.setBalance=" + balance);
    }

    public static void setMiningIncome(TextView tvMiningIncome) {
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue == null || tvMiningIncome == null){
            return;
        }
        String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        if(StringUtil.isNotSame(address, keyValue.getAddress())){
            TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
            return;
        }

        String miningIncome = FmtMicrometer.fmtMiningIncome(keyValue.getMiningIncome());
        SpannableStringBuilder spannableIncome = new SpanUtils()
                .append(miningIncome)
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_yellow))
                .append(" ")
                .append(ResourcesUtil.getText(R.string.common_balance_unit))
                .setFontSize(DimensionsUtil.dip2px(tvMiningIncome.getContext(), 12))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_black))
                .create();
        tvMiningIncome.setText(spannableIncome);
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

    /**
     * set state of mining conditions
     * */
    public static void setMiningConditions(TextView tvMining, ProgressView ivMining, TextView tvVerify, ProgressView ivVerify, BlockInfo blockInfo) {
        try{
            if(blockInfo != null && isImportKey()){
                String miningState = MyApplication.getKeyValue().getMiningState();
                boolean isStart = StringUtil.isSame(miningState, TransmitKey.MiningState.Start);
                long minedNo = MyApplication.getKeyValue().getMinedNo();
                long chainHeight =  blockInfo.getBlockHeight();
                long syncHeight =  blockInfo.getBlockSync();

                double progress = StringUtil.getProgress(syncHeight, chainHeight);
                double progressMined = StringUtil.getProgress(minedNo, chainHeight);

                String progressUnit = ResourcesUtil.getText(R.string.common_percentage);
                String progressStr = (int)progress + progressUnit;
                tvVerify.setText(progressStr);

                if(progress != 100 && isStart){
                    ivVerify.setOn();
                }else{
                    ivVerify.setOff();
                }

                String progressMinedStr = (int)progressMined + progressUnit;
                tvMining.setText(progressMinedStr);
                if(progress == 100 && isStart){
                    ivMining.setOn();
                }else{
                    ivMining.setOff();
                }
            }
        }catch (Exception ignore){

        }
    }

    public static void setMinersOnline(TextView tvMinersOnline, TextView tvMinersOnlineTitle, BlockInfo blockInfo) {
        if(tvMinersOnline == null || tvMinersOnlineTitle == null || blockInfo == null){
            return;
        }
        try {
            StringBuilder minersTitle = new StringBuilder();
            StringBuilder minersValue = new StringBuilder();
            if(StringUtil.isNotEmpty(blockInfo.getMinerInfo())){
                JSONObject jsonObject = new JSONObject(blockInfo.getMinerInfo());
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String value = jsonObject.getString(key);

                    if(minersTitle.length() > 0){
                        minersTitle.append(" / ");
                        minersValue.append(" / ");
                    }
                    minersTitle.append(key);
                    long miner = StringUtil.getLongString(value);
                    minersValue.append(FmtMicrometer.fmtPower(miner));
                }
            }
            if(minersTitle.length() == 0){
                minersTitle.append(ResourcesUtil.getText(R.string.home_miners_default_title));
                minersValue.append(ResourcesUtil.getText(R.string.home_miners_default_value));
            }
            String minersTitleRes = ResourcesUtil.getText(R.string.home_miners_title);
            minersTitleRes = String.format(minersTitleRes, minersTitle.toString());
            tvMinersOnline.setText(minersValue);
            tvMinersOnlineTitle.setText(minersTitleRes);
        }catch (Exception e){
            Logger.e(e, "setMinersOnline is error=%s", blockInfo.getMinerInfo());
        }

    }

    public static void setPowerConditions(DashboardLayout dashboardLayout, BlockInfo blockInfo, boolean isClearError) {
        try{
            if(blockInfo != null){
                long totalPower = StringUtil.getLongString(blockInfo.getTotalPower());
                KeyValue keyValue = MyApplication.getKeyValue();
                boolean isSynced = blockInfo.getBlockHeight() != 0 && blockInfo.getBlockHeight() <= blockInfo.getBlockSync();

                long power = keyValue.getPower();
                boolean isPowerError = isSynced && MyApplication.getRemoteConnector().getErrorCode() == 3;
                if(power > 0 && isPowerError && isClearError){
                    MyApplication.getRemoteConnector().clearErrorCode();
                    isPowerError = false;
                    MyApplication.getRemoteConnector().startBlockForging();
                }
                dashboardLayout.setError(isPowerError);
                if(totalPower > 0 && totalPower > power){
                    dashboardLayout.changeValue(power, totalPower);
                }
            }
        }catch (Exception ignore){

        }
    }

    public static void setDownloadConditions(TextView tvDownload, ProgressView ivDownload, TextView tvBlockChainData, BlockInfo blockInfo) {
        try{
            if(blockInfo != null && isImportKey()){
                String miningState = MyApplication.getKeyValue().getMiningState();
                boolean isStart = StringUtil.isSame(miningState, TransmitKey.MiningState.Start);
                int chainHeight =  blockInfo.getBlockHeight();
                int downloadHeight =  blockInfo.getBlockDownload();
                double progress = StringUtil.getProgress(downloadHeight, chainHeight);
                String progressStr = ResourcesUtil.getText(R.string.common_percentage);
                progressStr = (int)progress + progressStr;
                tvDownload.setText(progressStr);

                // 6K / block
                double data = downloadHeight * 6 / 1024;
                String dataStr = ResourcesUtil.getText(R.string.home_download_data_size);
                dataStr = String.format(dataStr, FmtMicrometer.fmtDecimal(data));
                tvBlockChainData.setText(dataStr);

                if(progress != 100 && isStart){
                    ivDownload.setOn();
                }else{
                    ivDownload.setOff();
                }
            }
        }catch (Exception ignore){

        }
    }

    public static void setMiningRank(TextView tvMiningRank) {
        if(tvMiningRank == null){
            return;
        }
        long miningRank = 0;
        if(isImportKey()){
            miningRank = MyApplication.getKeyValue().getMiningRank();
        }
        if(miningRank != 0){
            int icon = miningRank >= 0 ? R.mipmap.icon_rank_up : R.mipmap.icon_rank_down;
            DrawablesUtil.setEndDrawable(tvMiningRank, icon, 12);
        }
        String miningRankStr = ResourcesUtil.getText(R.string.home_no_point);
        miningRank = Math.abs(miningRank);
        miningRankStr = String.format(miningRankStr, FmtMicrometer.fmtPower(miningRank));
        tvMiningRank.setText(miningRankStr);
    }

    public static void initSuccessRequires(TextView tvSuccessRequires) {
        if(tvSuccessRequires == null){
            return;
        }
        SpannableStringBuilder spannable = new SpanUtils()
            .append("H")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append("it ")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("(Generation signature)")
            .setFontSize(DimensionsUtil.dip2px(tvSuccessRequires.getContext(), 10))
            .append(" < ")
            .setCenterFontSize(true)
            .setFontSize(DimensionsUtil.dip2px(tvSuccessRequires.getContext(), 22))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append("T")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append("arget * ")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("P")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append("ower * ")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("C")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append("ountdown Time")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .create();
        tvSuccessRequires.setText(spannable);
    }

    public static void setNextBlockNo(TextView tvNextBlockNo, BlockInfo blockInfo) {
        if(blockInfo == null || tvNextBlockNo == null){
            return;
        }
        String nextBlockNoStr = ResourcesUtil.getText(R.string.home_no_point);
        long nextBlockNo = blockInfo.getBlockHeight() + 1;
        nextBlockNoStr = String.format(nextBlockNoStr, FmtMicrometer.fmtPower(nextBlockNo));
        tvNextBlockNo.setText(nextBlockNoStr);
    }

    public static void setNextBlockReward(RadioButton rbMiner, TextView tvNextBlockReward) {
        if(tvNextBlockReward == null){
            return;
        }
        BlockInfo blockInfo = (BlockInfo) tvNextBlockReward.getTag();
        setNextBlockReward(rbMiner, tvNextBlockReward, blockInfo);
    }

    public static void setNextBlockReward(RadioButton rbMiner, TextView tvNextBlockReward, BlockInfo blockInfo) {
        if(blockInfo == null || rbMiner == null || tvNextBlockReward == null || !isImportKey()){
            return;
        }
        long reward;
        if(rbMiner.isChecked()){
            reward = StringUtil.getLongString(blockInfo.getAvgIncome());
            reward = reward / 3;
        }else{
            String nextPart = MyApplication.getKeyValue().getNextPart();
            nextPart = FmtMicrometer.fmtTxValue(nextPart);
            reward = StringUtil.getLongString(nextPart);
        }
        tvNextBlockReward.setTag(blockInfo);
        SpannableStringBuilder spannableReward = new SpanUtils()
                .append(FmtMicrometer.fmtBalance(reward))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(" ")
                .append(ResourcesUtil.getText(R.string.common_balance_unit))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_home_grey_dark))
                .create();
        tvNextBlockReward.setText(spannableReward);
    }

    public static void setCurrentCondition(TextView tvCurrentCondition, long timeInternal) {
        if(tvCurrentCondition == null){
            return;
        }
        Object data = tvCurrentCondition.getTag();
        NextBlockForgedPOTDetail detail = (NextBlockForgedPOTDetail) data;
        if(detail != null){
            BigInteger leftValue = detail.hitValue;
            BigInteger rightValue = detail.baseTarget.multiply(detail.forgingPower);
            rightValue = rightValue.multiply(new BigInteger(String.valueOf(timeInternal)));
            int result = leftValue.compareTo(rightValue);
            String resultStr;
            if(result > 0){
                resultStr = " > ";
            }else if(result == 0){
                resultStr = " = ";
            }else {
                resultStr = " < ";
            }
            SpannableStringBuilder spannable = new SpanUtils()
                .append(FmtMicrometer.fmtPower(detail.hitValue.longValue()))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(resultStr)
                .setCenterFontSize(true)
                .setFontSize(DimensionsUtil.dip2px(tvCurrentCondition.getContext(), 22))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                .append(FmtMicrometer.fmtPower(detail.baseTarget.longValue()))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(" * ")
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                .append(FmtMicrometer.fmtPower(detail.forgingPower.longValue()))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(" * ")
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                .append(FmtMicrometer.fmtPower(timeInternal))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .create();
            tvCurrentCondition.setText(spannable);
        }
    }

    public static void setTxParticipantInfo(TextView tvHistoryMiner, TextView tvTxParticipant) {
        if(tvHistoryMiner == null || tvTxParticipant == null){
            return;
        }
        double historyMiner = 0;
        double historyTx = 0;
        if(isImportKey()){
            historyMiner = MyApplication.getKeyValue().getHistoryMiner() * 100;
            historyTx = MyApplication.getKeyValue().getHistoryTx() * 100;
        }
        String unit = "%";

        if(historyMiner != 0){
            int icon = historyMiner >= 0 ? R.mipmap.icon_income_up : R.mipmap.icon_rank_down;
            DrawablesUtil.setEndDrawable(tvHistoryMiner, icon, 12);
        }
        historyMiner = Math.abs(historyMiner);
        String historyMinerStr = FmtMicrometer.fmtValue(historyMiner);
        historyMinerStr += unit;
        tvHistoryMiner.setText(historyMinerStr);

        if(historyTx != 0){
            int icon = historyTx >= 0 ? R.mipmap.icon_income_up : R.mipmap.icon_rank_down;
            DrawablesUtil.setEndDrawable(tvTxParticipant, icon, 12);
        }
        historyTx = Math.abs(historyTx);
        String historyTxStr = FmtMicrometer.fmtValue(historyTx);
        historyTxStr += unit;
        tvTxParticipant.setText(historyTxStr);
    }
}