package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

/**
 * @version 1.0
 * mining information
 */
@Entity
public class MiningInfo {

    @Id
    private Long id;
    private String pubKey;
    private String blockNo;
    private String blockHash;
    private String reward;
    private int valid;
    private int total;
    @Generated(hash = 1481233812)
    public MiningInfo(Long id, String pubKey, String blockNo, String blockHash,
            String reward, int valid, int total) {
        this.id = id;
        this.pubKey = pubKey;
        this.blockNo = blockNo;
        this.blockHash = blockHash;
        this.reward = reward;
        this.valid = valid;
        this.total = total;
    }
    @Generated(hash = 636345119)
    public MiningInfo() {
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
    public String getBlockNo() {
        return this.blockNo;
    }
    public void setBlockNo(String blockNo) {
        this.blockNo = blockNo;
    }
    public String getBlockHash() {
        return this.blockHash;
    }
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    public String getReward() {
        return this.reward;
    }
    public void setReward(String reward) {
        this.reward = reward;
    }
    public int getValid() {
        return this.valid;
    }
    public void setValid(int valid) {
        this.valid = valid;
    }
    public int getTotal() {
        return this.total;
    }
    public void setTotal(int total) {
        this.total = total;
    }
}