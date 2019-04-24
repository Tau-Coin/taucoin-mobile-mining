package io.taucoin.android.service.events;


import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.core.Transaction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PendingTransactionsEventData extends EventData {

    public Set<Transaction> transactions = new HashSet<Transaction>();

    public PendingTransactionsEventData(List<Transaction> transactionsList) {

        super();
        for (Transaction tx : transactionsList) {
            this.transactions.add(tx);
        }
    }

    public PendingTransactionsEventData(Set<Transaction> transactions) {

        super();
        this.transactions = transactions;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        io.taucoin.android.interop.Transaction[] txs = new io.taucoin.android.interop.Transaction[transactions.size()];
        int index = 0;
        for (Transaction transaction: transactions) {
            txs[index] = new io.taucoin.android.interop.Transaction(transaction);
        }
        parcel.writeParcelableArray(txs, i);
    }

    public static final Parcelable.Creator<PendingTransactionsEventData> CREATOR = new Parcelable.Creator<PendingTransactionsEventData>() {

        public PendingTransactionsEventData createFromParcel(Parcel in) {

            return new PendingTransactionsEventData(in);
        }

        public PendingTransactionsEventData[] newArray(int size) {

            return new PendingTransactionsEventData[size];
        }
    };

    private PendingTransactionsEventData(Parcel in) {

        super(in);
        Parcelable[] transactions = in.readParcelableArray(io.taucoin.android.interop.Transaction.class.getClassLoader());
        Transaction[] txs = new Transaction[transactions.length];
        int index = 0;
        for (Parcelable transaction: transactions) {
            txs[index] = (Transaction)transaction;
        }
        this.transactions = new HashSet<Transaction>(Arrays.asList(txs));
    }

}
