package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class RewardInfoBean extends BaseBean{

    @SerializedName(value = "minerinfo")
    private String minerInfo;
    @SerializedName(value = "rankno")
    private int rankNo;
    @SerializedName(value = "totalpower")
    private String totalPower;

    public String getMinerInfo() {
        return minerInfo;
    }

    public void setMinerInfo(String minerInfo) {
        this.minerInfo = minerInfo;
    }

    public int getRankNo() {
        return rankNo;
    }

    public void setRankNo(int rankNo) {
        this.rankNo = rankNo;
    }

    public String getTotalPower() {
        return totalPower;
    }

    public void setTotalPower(String totalPower) {
        this.totalPower = totalPower;
    }
}
