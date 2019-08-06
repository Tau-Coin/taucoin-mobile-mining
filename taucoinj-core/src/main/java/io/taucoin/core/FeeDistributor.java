package io.taucoin.core;

import java.util.concurrent.atomic.AtomicBoolean;

public class FeeDistributor {

    public static int lastWitShare = 1;
    public static int currentWitShare = 1;
    public static int lastAssShare = 1;
    public static int integrityShare = lastWitShare + currentWitShare + lastAssShare;

    private long txFee;

    private long lastWitFee = 0;
    private long currentWitFee = 0;
    private long lastAssociFee = 0;
    private volatile boolean isDistribute = true;

    public FeeDistributor(long txFee) {
        this.txFee = txFee;
    }

    public boolean distributeFee(){
        if (lastWitShare + currentWitShare + lastAssShare != integrityShare) {
            return false;
        }

        //before special height 21000, fee is distributed.
        //but after this height ,fee will not be distributed
        //because of system pressure from android.

        if (isDistribute) {
            long residual = txFee % integrityShare;
            long stakeShare = txFee / integrityShare;
            this.lastWitFee = stakeShare * lastWitShare;
            this.currentWitFee = stakeShare * currentWitShare + residual;
            this.lastAssociFee = stakeShare * lastAssShare;
        } else {
            this.currentWitFee = txFee;
        }

        if (isDistribute) {
            if (lastWitFee + currentWitFee + lastAssociFee != txFee) {
                return false;
            }
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

    public void setDistribute(boolean judge) {
        this.isDistribute = judge;
    }
}
