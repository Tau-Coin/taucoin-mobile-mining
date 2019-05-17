package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class RawTxBean {

    @SerializedName(value = "txid")
    private String txId;
    private String sender;
    private String receiver;
    private String amount;
    private String fee;
    @SerializedName(value = "blockheight")
    private int blockHeight;
    @SerializedName(value = "txtime")
    private String txTime;
    @SerializedName(value = "expiredheight")
    private long expiredHeight;

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }

    public String getTxTime() {
        return txTime;
    }

    public void setTxTime(String txTime) {
        this.txTime = txTime;
    }

    public long getExpiredHeight() {
        return expiredHeight;
    }

    public void setExpiredHeight(long expiredHeight) {
        this.expiredHeight = expiredHeight;
    }
}
