package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

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
    private int blockDownload;
    private String avgIncome;
    private String medianFee;
    private String minerInfo;
    private String totalPower;
    @Generated(hash = 1747879799)
    public BlockInfo(Long id, int blockHeight, int blockSync, int blockDownload,
            String avgIncome, String medianFee, String minerInfo,
            String totalPower) {
        this.id = id;
        this.blockHeight = blockHeight;
        this.blockSync = blockSync;
        this.blockDownload = blockDownload;
        this.avgIncome = avgIncome;
        this.medianFee = medianFee;
        this.minerInfo = minerInfo;
        this.totalPower = totalPower;
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
    public int getBlockSync() {
        return this.blockSync;
    }
    public void setBlockSync(int blockSync) {
        this.blockSync = blockSync;
    }
    public int getBlockDownload() {
        return this.blockDownload;
    }
    public void setBlockDownload(int blockDownload) {
        this.blockDownload = blockDownload;
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
    public String getMinerInfo() {
        return this.minerInfo;
    }
    public void setMinerInfo(String minerInfo) {
        this.minerInfo = minerInfo;
    }
    public String getTotalPower() {
        return this.totalPower;
    }
    public void setTotalPower(String totalPower) {
        this.totalPower = totalPower;
    }
}