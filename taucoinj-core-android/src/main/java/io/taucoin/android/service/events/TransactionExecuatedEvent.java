package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.core.TransactionExecuatedOutcome;

public class TransactionExecuatedEvent extends EventData {
    public TransactionExecuatedOutcome outcome;

    public TransactionExecuatedEvent(TransactionExecuatedOutcome outcome) {
        super();
        this.outcome = outcome;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeParcelable(new io.taucoin.android.interop.TransactionExecuatedOutcome(outcome), i);
    }

    public static final Parcelable.Creator<TransactionExecuatedEvent> CREATOR = new Parcelable.Creator<TransactionExecuatedEvent>() {

        public  TransactionExecuatedEvent createFromParcel(Parcel in) {

            return new TransactionExecuatedEvent(in);
        }

        public TransactionExecuatedEvent[] newArray(int size) {

            return new TransactionExecuatedEvent[size];
        }
    };

    protected TransactionExecuatedEvent(Parcel in) {

        super(in);
        outcome = in.readParcelable(io.taucoin.android.interop.TransactionExecuatedOutcome.class.getClassLoader());
    }
}
