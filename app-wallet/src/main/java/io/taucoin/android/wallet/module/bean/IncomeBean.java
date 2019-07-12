package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class IncomeBean {

    @SerializedName(value = "avgincome")
    private String avgIncome;
    @SerializedName(value = "medianfee")
    private String medianFee;
    @SerializedName(value = "minerinfo")
    private String minerInfo;
    @SerializedName(value = "totalpower")
    private String totalPower;
    @SerializedName(value = "txsno")
    private String txsPool;

    public String getAvgIncome() {
        return avgIncome;
    }

    public void setAvgIncome(String avgIncome) {
        this.avgIncome = avgIncome;
    }

    public String getMedianFee() {
        return medianFee;
    }

    public void setMedianFee(String medianFee) {
        this.medianFee = medianFee;
    }

    public String getMinerInfo() {
        return minerInfo;
    }

    public void setMinerInfo(String minerInfo) {
        this.minerInfo = minerInfo;
    }

    public String getTotalPower() {
        return totalPower;
    }

    public void setTotalPower(String totalPower) {
        this.totalPower = totalPower;
    }

    public String getTxsPool() {
        return txsPool;
    }

    public void setTxsPool(String txsPool) {
        this.txsPool = txsPool;
    }
}
