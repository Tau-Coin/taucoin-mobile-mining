package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class IncomeBean {

    @SerializedName(value = "avgincome")
    private String avgIncome;
    @SerializedName(value = "medianfee")
    private String medianFee;
    @SerializedName(value = "minerno")
    private String minerNo;

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

    public String getMinerNo() {
        return minerNo;
    }

    public void setMinerNo(String minerNo) {
        this.minerNo = minerNo;
    }
}
