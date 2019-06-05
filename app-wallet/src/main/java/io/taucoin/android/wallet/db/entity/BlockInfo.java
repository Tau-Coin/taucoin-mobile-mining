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
    private Long id;
    private int blockHeight;
    private int blockSync;
    private String avgIncome;
    private String medianFee;
    private String minerNo;

    @Transient
    private List<MiningBlock> miningBlocks;

    @Generated(hash = 206698040)
    public BlockInfo(Long id, int blockHeight, int blockSync, String avgIncome,
            String medianFee, String minerNo) {
        this.id = id;
        this.blockHeight = blockHeight;
        this.blockSync = blockSync;
        this.avgIncome = avgIncome;
        this.medianFee = medianFee;
        this.minerNo = minerNo;
    }

    @Generated(hash = 1647740766)
    public BlockInfo() {
    }

    public List<MiningBlock> getMiningInfo() {
        return miningBlocks;
    }

    public void setMiningBlocks(List<MiningBlock> miningBlocks) {
        this.miningBlocks = miningBlocks;
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

    public int getBlockSync() {
        return this.blockSync;
    }

    public void setBlockSync(int blockSync) {
        this.blockSync = blockSync;
    }

    public String getAvgIncome() {
        return this.avgIncome;
    }

    public void setAvgIncome(String avgIncome) {
        this.avgIncome = avgIncome;
    }

    public String getMedianFee() {
        return this.medianFee;
    }

    public void setMedianFee(String medianFee) {
        this.medianFee = medianFee;
    }

    public String getMinerNo() {
        return this.minerNo;
    }

    public void setMinerNo(String minerNo) {
        this.minerNo = minerNo;
    }
}