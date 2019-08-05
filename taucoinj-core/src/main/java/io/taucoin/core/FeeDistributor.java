package io.taucoin.core;

public class FeeDistributor {

    public static int lastWitShare = 1;
    public static int currentWitShare = 1;
    public static int lastAssShare = 1;
    public static int integrityShare = lastWitShare + currentWitShare + lastAssShare;

    private long txFee;

    private long lastWitFee = 0;
    private long currentWitFee = 0;
    private long lastAssociFee = 0;

    public FeeDistributor() {
    }

    public FeeDistributor(long txFee) {
        this.txFee = txFee;
    }

    public void setTxFee(long txFee) {
        this.txFee = txFee;
    }

    public boolean distributeFee(){
        if (lastWitShare + currentWitShare + lastAssShare != integrityShare) {
            return false;
        }

        long residual = txFee % integrityShare;
        long stakeShare = txFee / integrityShare;
        this.lastWitFee = stakeShare * lastWitShare;
        this.currentWitFee = stakeShare * currentWitShare + residual;
        this.lastAssociFee = stakeShare * lastAssShare;

        if (lastWitFee + currentWitFee + lastAssociFee != txFee) {
            return false;
        }
        return true;
    }

    public long getLastWitFee() {
        return lastWitFee;
    }

    public long getCurrentWitFee() {
        return currentWitFee;
    }

    public long getLastAssociFee() {
        return lastAssociFee;
    }
}
