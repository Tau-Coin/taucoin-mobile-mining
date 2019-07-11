package io.taucoin.android.wallet.module.bean;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class NetworkInfoBean extends BaseBean{

    @SerializedName(value = "minerinfo")
    private JsonObject minerInfo;
    @SerializedName(value = "rankno")
    private int rankNo;
    @SerializedName(value = "totalpower")
    private String totalPower;

    public JsonObject getMinerInfo() {
        return minerInfo;
    }

    public void setMinerInfo(JsonObject minerInfo) {
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
