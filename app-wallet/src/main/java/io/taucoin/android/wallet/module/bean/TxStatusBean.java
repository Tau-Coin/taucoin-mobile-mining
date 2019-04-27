package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class TxStatusBean {
    @SerializedName(value = "txid")
    private String txId;
    private int status;

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
