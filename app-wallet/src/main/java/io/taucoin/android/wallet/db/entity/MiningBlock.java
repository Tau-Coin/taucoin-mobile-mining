package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

/**
 * @version 1.0
 * mining block
 */
@Entity
public class MiningBlock {

    @Id
    private Long id;
    private String pubKey;
    private String blockNo;
    private String blockHash;
    private String reward;
    private int valid;
    private int total;
    private int type;
    @Generated(hash = 529138808)
    public MiningBlock(Long id, String pubKey, String blockNo, String blockHash,
            String reward, int valid, int total, int type) {
        this.id = id;
        this.pubKey = pubKey;
        this.blockNo = blockNo;
        this.blockHash = blockHash;
        this.reward = reward;
        this.valid = valid;
        this.total = total;
        this.type = type;
    }
    @Generated(hash = 1637629893)
    public MiningBlock() {
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
    public int getType() {
        return this.type;
    }
    public void setType(int type) {
        this.type = type;
    }
}