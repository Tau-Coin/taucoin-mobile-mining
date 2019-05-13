package io.taucoin.android.wallet.module.bean;

public class RewardBean{

    private long minerReward;
    private long partReward;
    private long totalReward;

    public long getTotalReward() {
        return totalReward;
    }

    public void setTotalReward(long totalReward) {
        this.totalReward = totalReward;
    }

    public long getMinerReward() {
        return minerReward;
    }

    public void setMinerReward(long minerReward) {
        this.minerReward = minerReward;
    }

    public long getPartReward() {
        return partReward;
    }

    public void setPartReward(long partReward) {
        this.partReward = partReward;
    }
}