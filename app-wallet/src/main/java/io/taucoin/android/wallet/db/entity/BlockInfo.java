package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.util.List;

/**
 * @version 1.0
 * mining information
 */
@Entity
public class BlockInfo {
    @Id
    private long id;
    private int blockHeight;
    private int blockSync;

    @Transient
    private List<MiningInfo> miningInfo;

    @Generated(hash = 220266045)
    public BlockInfo(long id, int blockHeight, int blockSync) {
        this.id = id;
        this.blockHeight = blockHeight;
        this.blockSync = blockSync;
    }

    @Generated(hash = 1647740766)
    public BlockInfo() {
    }

    public List<MiningInfo> getMiningInfo() {
        return miningInfo;
    }

    public void setMiningInfo(List<MiningInfo> miningInfo) {
        this.miningInfo = miningInfo;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getBlockHeight() {
        return this.blockHeight;
    }

    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }

    public int getBlockSync() {
        return this.blockSync;
    }

    public void setBlockSync(int blockSync) {
        this.blockSync = blockSync;
    }

}