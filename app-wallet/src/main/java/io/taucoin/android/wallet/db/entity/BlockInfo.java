package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.util.List;

/**
 * Created by yang on 18-11-21
 *
 * @version 1.0
 * @description: mining information
 */
@Entity
public class BlockInfo {
    @Id
    private Long id;
    private int blockHeight;
    private int blockSynchronized;

    @Transient
    private List<MiningInfo> miningInfos;

    public List<MiningInfo> getMiningInfos() {
        return miningInfos;
    }

    public void setMiningInfos(List<MiningInfo> miningInfos) {
        this.miningInfos = miningInfos;
    }

    @Generated(hash = 1508347332)
    public BlockInfo(Long id, int blockHeight, int blockSynchronized) {
        this.id = id;
        this.blockHeight = blockHeight;
        this.blockSynchronized = blockSynchronized;
    }
    @Generated(hash = 1647740766)
    public BlockInfo() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getBlockHeight() {
        return this.blockHeight;
    }
    public void setBlockHeight(int blockHeight) {
        this.blockHeight = blockHeight;
    }
    public int getBlockSynchronized() {
        return this.blockSynchronized;
    }
    public void setBlockSynchronized(int blockSynchronized) {
        this.blockSynchronized = blockSynchronized;
    }
}