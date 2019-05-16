package io.taucoin.android.wallet.module.bean;


import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.core.Transaction;

public class TransactionBean {
    private TransactionHistory localData;
    private Transaction rawData;

    public TransactionHistory getLocalData() {
        return localData;
    }

    public void setLocalData(TransactionHistory localData) {
        this.localData = localData;
    }

    public Transaction getRawData() {
        return rawData;
    }

    public void setRawData(Transaction rawData) {
        this.rawData = rawData;
    }
}