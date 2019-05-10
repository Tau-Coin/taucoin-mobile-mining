package io.taucoin.android.wallet.module.bean;

public class RewardBean{

    private boolean isPart;
    private long minerReward;
    private long partReward;

    public boolean isPart() {
        return isPart;
    }

    public void setPart(boolean part) {
        isPart = part;
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