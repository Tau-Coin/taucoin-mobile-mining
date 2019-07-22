package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class ParticipantInfoBean extends BaseBean{

    // Participant history tx rewards TAU
    @SerializedName(value = "preward")
    private String partReward;
    // Participant history miner rewards TAU
    @SerializedName(value = "mreward")
    private String minerReward;
    // History Miner Self Participation: +0.0011, 0 < value < 1;
    @SerializedName(value = "hmsp")
    private double minerPart;
    // TX Participant Self Participation: -0.0001, 0 < value < 1;
    @SerializedName(value = "tpsp")
    private double txPart;

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

    public double getMinerPart() {
        return minerPart;
    }

    public void setMinerPart(double minerPart) {
        this.minerPart = minerPart;
    }

    public double getTxPart() {
        return txPart;
    }

    public void setTxPart(double txPart) {
        this.txPart = txPart;
    }
}