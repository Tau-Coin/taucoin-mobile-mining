package io.taucoin.core;

import java.math.BigInteger;

public class FeeDistributor {
    public static int integrityShare = 4;
    public static int receiveShare = 1;
    public static int lastWitShare = 1;
    public static int currentWitShare = 1;
    public static int lastAssShare = 1;

    private long txFee;

    private long receiveFee = 0;
    private long lastWitFee = 0;
    private long currentWitFee = 0;
    private long lastAssociFee = 0;

    public FeeDistributor(long txFee) {
        this.txFee = txFee;
    }

    public boolean distributeFee(){
        if (receiveShare + lastWitShare + currentWitShare + lastAssShare != integrityShare) {
            return false;
        }

        long residual = txFee % integrityShare;
        long stakeShare = txFee / integrityShare;
        this.receiveFee = stakeShare * receiveShare;
        this.lastWitFee = stakeShare * lastWitShare;
        this.currentWitFee = stakeShare * currentWitShare + residual;
        this.lastAssociFee = stakeShare * lastAssShare;

        if (receiveFee + lastWitFee + currentWitFee + lastAssociFee != txFee) {
            return false;
        }
        return true;
    }
    public long getReceiveFee() {
        return receiveFee;
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
