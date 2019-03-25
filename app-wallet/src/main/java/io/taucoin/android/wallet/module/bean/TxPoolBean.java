package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class TxPoolBean {

    @SerializedName(value = "errorinfo")
    private String errorInfo;
    private int status;
    @SerializedName(value = "txid")
    private String txId;

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }
}
