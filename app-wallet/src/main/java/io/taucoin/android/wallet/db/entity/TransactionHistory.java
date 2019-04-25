package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import org.greenrobot.greendao.annotation.Generated;

/**
 * @version 1.0
 * Edited by yang on 19-02-23
 */

@Entity
public class TransactionHistory {

    @Id
    private long id;

    private String txId;

    private String fromAddress;

    private String toAddress;

    private String createTime;

    private String amount;

    private String memo;

    private String fee;

    private String result;

    private String message;

    private long blockNum;

    private String blockHash;

    private long blockTime;
    // tx expiration date(expire block num)
    private long transExpiry;
    // Time basis for acquiring transaction records
    private int timeBasis;
    @Generated(hash = 1568833116)
    public TransactionHistory(long id, String txId, String fromAddress,
            String toAddress, String createTime, String amount, String memo,
            String fee, String result, String message, long blockNum,
            String blockHash, long blockTime, long transExpiry, int timeBasis) {
        this.id = id;
        this.txId = txId;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.createTime = createTime;
        this.amount = amount;
        this.memo = memo;
        this.fee = fee;
        this.result = result;
        this.message = message;
        this.blockNum = blockNum;
        this.blockHash = blockHash;
        this.blockTime = blockTime;
        this.transExpiry = transExpiry;
        this.timeBasis = timeBasis;
    }
    @Generated(hash = 63079048)
    public TransactionHistory() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTxId() {
        return this.txId;
    }
    public void setTxId(String txId) {
        this.txId = txId;
    }
    public String getFromAddress() {
        return this.fromAddress;
    }
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
    public String getToAddress() {
        return this.toAddress;
    }
    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }
    public String getCreateTime() {
        return this.createTime;
    }
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    public String getAmount() {
        return this.amount;
    }
    public void setAmount(String amount) {
        this.amount = amount;
    }
    public String getMemo() {
        return this.memo;
    }
    public void setMemo(String memo) {
        this.memo = memo;
    }
    public String getFee() {
        return this.fee;
    }
    public void setFee(String fee) {
        this.fee = fee;
    }
    public String getResult() {
        return this.result;
    }
    public void setResult(String result) {
        this.result = result;
    }
    public String getMessage() {
        return this.message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public long getBlockNum() {
        return this.blockNum;
    }
    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
    }
    public String getBlockHash() {
        return this.blockHash;
    }
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    public long getBlockTime() {
        return this.blockTime;
    }
    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }
    public long getTransExpiry() {
        return this.transExpiry;
    }
    public void setTransExpiry(long transExpiry) {
        this.transExpiry = transExpiry;
    }
    public int getTimeBasis() {
        return this.timeBasis;
    }
    public void setTimeBasis(int timeBasis) {
        this.timeBasis = timeBasis;
    }
}