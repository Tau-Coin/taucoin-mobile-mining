package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class ParticipantInfoBean extends BaseBean{

    // Participant rewards TAU
    @SerializedName(value = "reward")
    private String reward;
    // History Miner Self Participation: +0.0011, 0 < value < 1;
    @SerializedName(value = "hmsp")
    private double minerPart;
    // TX Participant Self Participation: -0.0001, 0 < value < 1;
    @SerializedName(value = "tpsp")
    private double txPart;

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
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