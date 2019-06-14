package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class MinerInfoBean {

    private int status;
    private String message;
    @SerializedName(value = "mpno")
    private long minerPartNo;

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

    public long getMinerPartNo() {
        return minerPartNo;
    }

    public void setMinerPartNo(long minerPartNo) {
        this.minerPartNo = minerPartNo;
    }
}
