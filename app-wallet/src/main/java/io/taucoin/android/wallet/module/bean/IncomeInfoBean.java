package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class IncomeInfoBean {

    private int status;
    private String message;
    @SerializedName(value = "payload")
    private IncomeBean payLoad;

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

    public IncomeBean getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(IncomeBean payLoad) {
        this.payLoad = payLoad;
    }
}
