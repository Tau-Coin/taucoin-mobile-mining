package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class RankInfoBean extends BaseBean{

    @SerializedName(value = "rankno")
    private long rankNo;

    public long getRankNo() {
        return rankNo;
    }

    public void setRankNo(long rankNo) {
        this.rankNo = rankNo;
    }
}
