package io.taucoin.core;

public class AssociatedFeeDistributor {
    private int assCount = 0;
    private long assTxFee = 0;

    private long averageShare = 0;
    private long lastShare = 0;

    public AssociatedFeeDistributor() {
    }

    public AssociatedFeeDistributor(int assCount, long assTxFee) {
        this.assCount = assCount;
        this.assTxFee = assTxFee;
    }

    public void init(int assCount, long assTxFee) {
        this.assCount = assCount;
        this.assTxFee = assTxFee;
    }

    public boolean assDistributeFee(){
        if (assCount == 0) return false;
        long residual = assTxFee % assCount;
        this.averageShare = assTxFee / assCount;
        this.lastShare = averageShare + residual;
        if (lastShare + averageShare*(assCount -1) != assTxFee) {
            return false;
        }
        return true;
    }

    public long getAverageShare() {
        return averageShare;
    }

    public long getLastShare() {
        return lastShare;
    }
}
