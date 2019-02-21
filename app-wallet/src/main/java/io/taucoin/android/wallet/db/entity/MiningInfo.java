package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

/**
 * Created by yang on 18-11-21
 *
 * @version 1.0
 * @description: mining information
 */
@Entity
public class MiningInfo {
    @Id
    private Long mid;
    private String publicKey;
    private String blockNo;
    private String blockHash;
    private String reward;
    private int valid;
    private int total;
    @Generated(hash = 776442791)
    public MiningInfo(Long mid, String publicKey, String blockNo, String blockHash,
            String reward, int valid, int total) {
        this.mid = mid;
        this.publicKey = publicKey;
        this.blockNo = blockNo;
        this.blockHash = blockHash;
        this.reward = reward;
        this.valid = valid;
        this.total = total;
    }
    @Generated(hash = 636345119)
    public MiningInfo() {
    }
    public Long getMid() {
        return this.mid;
    }
    public void setMid(Long mid) {
        this.mid = mid;
    }
    public String getPublicKey() {
        return this.publicKey;
    }
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
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