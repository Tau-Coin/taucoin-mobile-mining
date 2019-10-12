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

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.taucoin.android.service.events.NextBlockForgedPOTDetail;
import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.widget.DashboardLayout;
import io.taucoin.android.wallet.widget.LoadingTextView;
import io.taucoin.android.wallet.widget.ProgressView;
import io.taucoin.foundation.util.DimensionsUtil;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;

public class UserUtil {

    public static String parseNickName(KeyValue keyValue) {
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

    public static boolean isSelf(String address) {
        if(isImportKey() &&
                StringUtil.isSame(address, MyApplication.getKeyValue().getAddress())){
            return true;
        }
       return false;
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

    public static void setApplicationInfo(TextView tvCPU, TextView tvMemory, TextView tvDataStorage, TextView tvStorage, Object data) {
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
                if(StringUtil.isNotEmpty(notifyData.dataSize)){
                    String dataSize = notifyData.dataSize.substring(0, notifyData.dataSize.length() - 1);
                    tvStorage.setText(dataSize);
                }
            }
        }catch (Exception ignore){

        }
    }

    /**
     * set state of mining conditions
     * */
    public static void setMiningConditions(TextView tvVerify, ProgressView ivVerify, BlockInfo blockInfo) {
        try{
            if(blockInfo != null){
                long chainHeight =  blockInfo.getBlockHeight();
                long syncHeight =  blockInfo.getBlockSync();
                long downloadHeight =  blockInfo.getBlockDownload();

                String progressStr = FmtMicrometer.fmtPower(syncHeight);
                tvVerify.setText(progressStr);
                tvVerify.setTag(syncHeight);

                if(syncHeight < downloadHeight && syncHeight < chainHeight){
                    ivVerify.setOn();
                }else{
                    ivVerify.setOff();
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
                List<String> keys = new ArrayList<>();
                while (iterator.hasNext()) {
                    keys.add(iterator.next());
                }
                Collections.sort(keys, (o1, o2) -> {
                    try{
                        double pre = StringUtil.getDoubleString(o1);
                        double latter = StringUtil.getDoubleString(o2);
                        return pre <= latter ? -1 : 1;
                    }catch (Exception ignore){ }
                    return 0;
                });
                for (String key : keys) {
                    String value = jsonObject.getString(key);
                    if(minersTitle.length() > 0){
                        minersTitle.append("/");
                        minersValue.append("/");
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
            String minersTitleRes = ResourcesUtil.getText(R.string.home_miners_title_value);
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
                progressStr = FmtMicrometer.fmtDecimal(progress) + progressStr;
                tvDownload.setText(progressStr);

                // 6K / block
                double data = (double) chainHeight * 6 / 1024;
                String dataStr = ResourcesUtil.getText(R.string.home_full_data);
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

    public static void setMiningRankAndOther(TextView tvMiningRank, TextView tvTxsPool, TextView tvMedianFee, TextView tvCirculation, BlockInfo blockInfo) {
        if(tvMiningRank == null || tvTxsPool == null || tvMedianFee == null || blockInfo == null || tvCirculation == null){
            return;
        }
        long miningRank = 0L;
        Drawable drawable = null;
        if(isImportKey()){
            String miningRankStr = MyApplication.getKeyValue().getMiningRank();
            String miningRankType = StringUtil.getPlusOrMinus(miningRankStr);
            miningRank = StringUtil.getLongString(miningRankStr);
            miningRank = Math.abs(miningRank);
            if(StringUtil.isNotEmpty(miningRankType)){
                int resId = StringUtil.isSame(miningRankType, "+") ? R.mipmap.icon_rank_up : R.mipmap.icon_rank_down;
                drawable = DrawablesUtil.getDrawable(tvMiningRank.getContext(), resId, 10, 10);
            }
        }
        String miningRankStr = ResourcesUtil.getText(R.string.home_no_point);
        miningRankStr = String.format(miningRankStr, FmtMicrometer.fmtPower(miningRank));
        SpanUtils spannable = new SpanUtils()
            .append(miningRankStr);
        if(drawable != null){
            spannable.append(" ")
            .appendImage(drawable, SpanUtils.ALIGN_CENTER);
        }
        tvMiningRank.setText(spannable.create());

        tvTxsPool.setText(FmtMicrometer.fmtPower(blockInfo.getTxsPool()));
        tvMedianFee.setText(FmtMicrometer.fmtFeeValue(blockInfo.getMedianFee()));

        double circulation =  StringUtil.getDoubleString(blockInfo.getCirculation());
        double million =  circulation / 100_0000;
        String circulationStr;
        if(million >= 1){
            circulationStr = FmtMicrometer.fmtPower(String.valueOf(million)) + " M";
        }else {
            circulationStr = FmtMicrometer.fmtPower(String.valueOf(circulation));
        }
        tvCirculation.setText(circulationStr);
    }

    public static void initSuccessRequires(TextView tvSuccessRequires) {
        if(tvSuccessRequires == null){
            return;
        }
        SpannableStringBuilder spannable = new SpanUtils()
            .append("H")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(Html.fromHtml("it&nbsp;"))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("<")
            .setCenterFontSize(true)
            .setFontSize(DimensionsUtil.dip2px(tvSuccessRequires.getContext(), 20))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(Html.fromHtml("&nbsp;"))
            .append("T")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(Html.fromHtml("arget&nbsp;*&nbsp;"))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("P")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(Html.fromHtml("ower&nbsp;*&nbsp;"))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .append("C")
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(Html.fromHtml("ountdown&nbsp;Time"))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
            .create();
        tvSuccessRequires.setText(spannable);
    }

    public static void setNextBlockNo(TextView tvNextBlockNo, BlockInfo blockInfo) {
        if(blockInfo == null || tvNextBlockNo == null){
            return;
        }
        String nextBlockNoStr = ResourcesUtil.getText(R.string.home_total_blocks);
        long nextBlockNo = blockInfo.getBlockHeight() + 1;
        nextBlockNoStr = String.format(nextBlockNoStr, FmtMicrometer.fmtPower(nextBlockNo));
        tvNextBlockNo.setText(nextBlockNoStr);
        tvNextBlockNo.setTag(blockInfo.getBlockHeight());
    }

    public static void setHistoryParticipantReward(TextView tvHistoryMinerReward, TextView tvHistoryTxReward) {
        if(tvHistoryMinerReward == null || tvHistoryTxReward == null || !isImportKey()){
            return;
        }
        double minerReward = StringUtil.getDoubleString(MyApplication.getKeyValue().getMinerReward());
        SpannableStringBuilder spannableMiner = new SpanUtils()
                .append(FmtMicrometer.fmtDecimal(minerReward))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(" ")
                .append(ResourcesUtil.getText(R.string.common_balance_unit))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_home_grey_dark))
                .create();
        tvHistoryMinerReward.setText(spannableMiner);

        double partReward = StringUtil.getDoubleString(MyApplication.getKeyValue().getPartReward());
        SpannableStringBuilder spannableTx = new SpanUtils()
                .append(FmtMicrometer.fmtDecimal(partReward))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                .append(" ")
                .append(ResourcesUtil.getText(R.string.common_balance_unit))
                .setForegroundColor(ResourcesUtil.getColor(R.color.color_home_grey_dark))
                .create();
        tvHistoryTxReward.setText(spannableTx);
    }

    public static void setCountDown(TextView tvCurrentCondition, LoadingTextView tvForgedTime, Object data) {
        if(tvCurrentCondition == null || tvForgedTime == null || data == null || !isImportKey()){
            return;
        }
        NextBlockForgedPOTDetail detail = (NextBlockForgedPOTDetail) data;
        long timeInternal = detail.timeInternal;
        long initStartTime  = detail.timePoint - detail.previousBlockTime - timeInternal;
        initStartTime = initStartTime >= 0 ? initStartTime : 0;
        long startCountTime = initStartTime;
        UserUtil.setCurrentCondition(tvCurrentCondition, detail, startCountTime);
        tvForgedTime.setCountDown(timeInternal, count -> {
            count = startCountTime + timeInternal - count;
            UserUtil.setCurrentCondition(tvCurrentCondition, detail, count);
        });
    }

    private static final int digit = 13;
    private static void setCurrentCondition(TextView tvCurrentCondition, NextBlockForgedPOTDetail detail, long timeInternal) {
        if(tvCurrentCondition == null){
            return;
        }
        if(detail != null){
            BigInteger forgingPower = detail.forgingPower;
            long localPower = MyApplication.getKeyValue().getPower();
            if(forgingPower.longValue() < localPower){
                forgingPower = new BigInteger(String.valueOf(localPower));
            }
            BigInteger leftValue = detail.hitValue;
            BigInteger rightValue = detail.baseTarget.multiply(forgingPower);
            rightValue = rightValue.multiply(new BigInteger(String.valueOf(timeInternal)));
            int result = leftValue.compareTo(rightValue);
            String resultStr;
            if(result > 0){
                resultStr = ">";
            }else if(result == 0){
                resultStr = "=";
            }else {
                resultStr = "<";
            }
            String leftStr = FmtMicrometer.fmtPower(leftValue.longValue());
            String rightStr = FmtMicrometer.fmtPower(rightValue.longValue());
            boolean isSciCounting = isNeedSciCounting(leftStr, rightStr);
            if(isSciCounting){
                leftStr = handleDigit(leftValue.longValue());
                rightStr = handleDigit(rightValue.longValue());
            }
            SpanUtils spanUtils = new SpanUtils()
                    .append("Hit(")
                    .append(leftStr)
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue));
            if(isSciCounting){
                spanUtils.append(Html.fromHtml("*10"))
                        .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                        .append(String.valueOf(digit))
                        .setSuperscript()
                        .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                        .setFontSize(DimensionsUtil.dip2px(tvCurrentCondition.getContext(), 10));
            }
            spanUtils.append(")")
                    .append(Html.fromHtml("&nbsp;"))
                    .append(resultStr)
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                    .setFontSize(DimensionsUtil.dip2px(tvCurrentCondition.getContext(), 16))
                    .append(Html.fromHtml("&nbsp;"))
                    .append("Target(")
                    .append(FmtMicrometer.fmtPower(detail.baseTarget.longValue()))
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                    .append(")")
                    .append(Html.fromHtml("*"))
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                    .append("Power(")
                    .append(FmtMicrometer.fmtPower(forgingPower.longValue()))
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                    .append(")")
                    .append(Html.fromHtml("*"))
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_grey_light))
                    .append("Time(")
                    .append(FmtMicrometer.fmtPower(timeInternal))
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                    .append(Html.fromHtml(")"))
                    .append(Html.fromHtml("≈"))
                    .append(Html.fromHtml("("))
                    .append(rightStr)
                    .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue));
            if(isSciCounting){
                spanUtils.append(Html.fromHtml("*10"))
                        .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                        .append(String.valueOf(digit))
                        .setSuperscript()
                        .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
                        .setFontSize(DimensionsUtil.dip2px(tvCurrentCondition.getContext(), 10));
            }
            spanUtils.append(")");
            tvCurrentCondition.setText(spanUtils.create());
        }
    }

    private static String handleDigit(long num) {
        double result = num / Math.pow(10, digit);
        BigDecimal bigDecimal = new BigDecimal(result);
        String resultStr = bigDecimal.toPlainString();
        int pos = resultStr.indexOf(".");
        if(pos > 0){
            char[] chars = resultStr.toCharArray();
            for (int i = pos + 1; i < chars.length; i++) {
                if((chars[i] != '0' && i < chars.length -1 && result < 1 ) || result > 1){
                    resultStr = resultStr.substring(0, i + 1);
                    break;
                }
            }
        }
        return resultStr;
    }

    private static boolean isNeedSciCounting(String left, String right) {
        boolean isNeed = true;
        try {
            if(left.length() == right.length() &&
                    StringUtil.isSame(left.substring(0, 1), right.substring(0, 1))){
                isNeed =  false;
            }
        }catch (Exception e){
            isNeed =  false;
            Logger.e("isNeedSciCounting is error", e);
        }
        return isNeed;
    }

    public static String getLastThreeAddress() {
        String address = "";
        if(isImportKey()){
            address = MyApplication.getKeyValue().getAddress();
            if(address.length() > 3){
                address = address.substring(address.length() - 3);
            }
        }
        return address;
    }

    private static String getEllipsisAddress() {
        String address = "";
        if(isImportKey()){
            address = MyApplication.getKeyValue().getAddress();
            if(address.length() >= 4){
                address = address.substring(0, 1);
                address += "......";
                address += getLastThreeAddress();
            }
        }
        return address;
    }

    public static void setHitTip(TextView tvHitTip) {
        if(tvHitTip == null){
            return;
        }
        String title = ResourcesUtil.getText(R.string.home_hit_tip);
        title = String.format(title, UserUtil.getEllipsisAddress());
        tvHitTip.setText(title);
    }
}