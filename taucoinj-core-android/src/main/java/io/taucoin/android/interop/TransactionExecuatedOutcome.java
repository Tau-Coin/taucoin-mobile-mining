package io.taucoin.android.interop;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

public class TransactionExecuatedOutcome extends io.taucoin.core.TransactionExecuatedOutcome implements Parcelable {

    private  io.taucoin.core.TransactionExecuatedOutcome outcome;
    public TransactionExecuatedOutcome(io.taucoin.core.TransactionExecuatedOutcome outcome) {
        super();
        this.outcome = outcome;
    }

    public TransactionExecuatedOutcome(Parcel in) {
        byte[] blockhash = new byte[20];
        in.readByteArray(blockhash);
        setBlockHash(blockhash);
        setSenderAssociated(in.readHashMap(io.taucoin.core.TransactionExecuatedOutcome.class.getClassLoader()));
        setLastWintess(in.readHashMap(io.taucoin.core.TransactionExecuatedOutcome.class.getClassLoader()));
        setCurrentWintess(in.readHashMap(io.taucoin.core.TransactionExecuatedOutcome.class.getClassLoader()));
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(outcome.getBlockhash());
        parcel.writeMap(outcome.getSenderAssociated());
        parcel.writeMap(outcome.getLastWintess());
        parcel.writeMap(outcome.getCurrentWintess());
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public TransactionExecuatedOutcome createFromParcel(Parcel parcel) {
            return new TransactionExecuatedOutcome(parcel);
        }

        @Override
        public TransactionExecuatedOutcome[] newArray(int i) {
            return new TransactionExecuatedOutcome[i];
        }
    };
}
