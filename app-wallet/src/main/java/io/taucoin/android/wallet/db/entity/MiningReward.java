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
    private String address;
    private String txHash;
    private long minerFee;
    private long partFee;
    private String time;
    private String blockHash;
    private int valid;
    private String senderAddress;
    private String receiverAddress;
    @Generated(hash = 647148558)
    public MiningReward(Long id, String address, String txHash, long minerFee,
            long partFee, String time, String blockHash, int valid,
            String senderAddress, String receiverAddress) {
        this.id = id;
        this.address = address;
        this.txHash = txHash;
        this.minerFee = minerFee;
        this.partFee = partFee;
        this.time = time;
        this.blockHash = blockHash;
        this.valid = valid;
        this.senderAddress = senderAddress;
        this.receiverAddress = receiverAddress;
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
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getTxHash() {
        return this.txHash;
    }
    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
    public long getMinerFee() {
        return this.minerFee;
    }
    public void setMinerFee(long minerFee) {
        this.minerFee = minerFee;
    }
    public long getPartFee() {
        return this.partFee;
    }
    public void setPartFee(long partFee) {
        this.partFee = partFee;
    }
    public String getTime() {
        return this.time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getBlockHash() {
        return this.blockHash;
    }
    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
    public int getValid() {
        return this.valid;
    }
    public void setValid(int valid) {
        this.valid = valid;
    }
    public String getSenderAddress() {
        return this.senderAddress;
    }
    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }
    public String getReceiverAddress() {
        return this.receiverAddress;
    }
    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

}