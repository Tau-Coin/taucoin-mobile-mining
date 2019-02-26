package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by ly on 18-10-30
 *
 * @version 1.0
 * @description:
 *
 * @version 2.0
 * Edited by yang on 19-02-23
 */

@Entity
public class TransactionHistory {

    @Id
    private Long id;

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
    // -1 finishState
    private int notRolled;

    @Generated(hash = 329318752)
    public TransactionHistory(Long id, String txId, String fromAddress,
            String toAddress, String createTime, String amount, String memo,
            String fee, String result, String message, long blockNum,
            String blockHash, long blockTime, int notRolled) {
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
        this.notRolled = notRolled;
    }

    @Generated(hash = 63079048)
    public TransactionHistory() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
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

    public int getNotRolled() {
        return this.notRolled;
    }

    public void setNotRolled(int notRolled) {
        this.notRolled = notRolled;
    }

}