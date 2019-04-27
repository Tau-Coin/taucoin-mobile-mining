package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TxDataBean {
    private int status;
    private String message;
    @SerializedName(value = "txsstatus")
    private List<TxStatusBean> txsStatus;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TxStatusBean> getTxsStatus() {
        return txsStatus;
    }

    public void setTxsStatus(List<TxStatusBean> txsStatus) {
        this.txsStatus = txsStatus;
    }
}
