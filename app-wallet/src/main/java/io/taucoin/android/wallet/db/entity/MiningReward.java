package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * @version 1.0
 * mining reward
 */
@Entity
public class MiningReward {

    @Id
    private Long id;
    private String pubKey;
    private String txId;
    private String txHash;
    private String fee;
    private String time;
    private int status;
    private int valid;
    @Generated(hash = 122383812)
    public MiningReward(Long id, String pubKey, String txId, String txHash,
            String fee, String time, int status, int valid) {
        this.id = id;
        this.pubKey = pubKey;
        this.txId = txId;
        this.txHash = txHash;
        this.fee = fee;
        this.time = time;
        this.status = status;
        this.valid = valid;
    }
    @Generated(hash = 364800917)
    public MiningReward() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getPubKey() {
        return this.pubKey;
    }
    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }
    public String getTxId() {
        return this.txId;
    }
    public void setTxId(String txId) {
        this.txId = txId;
    }
    public String getTxHash() {
        return this.txHash;
    }
    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
    public String getFee() {
        return this.fee;
    }
    public void setFee(String fee) {
        this.fee = fee;
    }
    public String getTime() {
        return this.time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public int getStatus() {
        return this.status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public int getValid() {
        return this.valid;
    }
    public void setValid(int valid) {
        this.valid = valid;
    }

}