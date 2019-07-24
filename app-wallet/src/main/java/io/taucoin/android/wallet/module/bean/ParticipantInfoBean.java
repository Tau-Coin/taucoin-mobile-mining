package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class ParticipantInfoBean extends BaseBean{

    // Participant history tx rewards TAU
    @SerializedName(value = "preward")
    private String partReward;
    // Participant history miner rewards TAU
    @SerializedName(value = "mreward")
    private String minerReward;

    public String getPartReward() {
        return partReward;
    }

    public void setPartReward(String partReward) {
        this.partReward = partReward;
    }

    public String getMinerReward() {
        return minerReward;
    }

    public void setMinerReward(String minerReward) {
        this.minerReward = minerReward;
    }
}