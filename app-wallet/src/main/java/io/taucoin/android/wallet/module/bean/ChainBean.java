package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class ChainBean {

    private int status;
    private String message;
    @SerializedName(value = "payload")
    private ChainDetail payLoad;

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

    public ChainDetail getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(ChainDetail payLoad) {
        this.payLoad = payLoad;
    }
}
