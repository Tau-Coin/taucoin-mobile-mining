package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import org.greenrobot.greendao.annotation.Generated;

/**
 * @version 1.0
 * Key Value
 */
@Entity
public class KeyValue {

    @Id
    private Long id;
    private String pubKey;
    private String priKey;
    private String address;
    private String rawAddress;
    private long balance;
    private long power;
    private long miningIncome;
    private String nickName;
    private String miningState;
    private long transExpiry;
    private long blocksMined;
    private long MinedNo;
    private String miningRank;
    private String minerReward;
    private String partReward;
    @Generated(hash = 1367625504)
    public KeyValue(Long id, String pubKey, String priKey, String address,
            String rawAddress, long balance, long power, long miningIncome,
            String nickName, String miningState, long transExpiry, long blocksMined,
            long MinedNo, String miningRank, String minerReward,
            String partReward) {
        this.id = id;
        this.pubKey = pubKey;
        this.priKey = priKey;
        this.address = address;
        this.rawAddress = rawAddress;
        this.balance = balance;
        this.power = power;
        this.miningIncome = miningIncome;
        this.nickName = nickName;
        this.miningState = miningState;
        this.transExpiry = transExpiry;
        this.blocksMined = blocksMined;
        this.MinedNo = MinedNo;
        this.miningRank = miningRank;
        this.minerReward = minerReward;
        this.partReward = partReward;
    }
    @Generated(hash = 92014137)
    public KeyValue() {
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
    public String getPriKey() {
        return this.priKey;
    }
    public void setPriKey(String priKey) {
        this.priKey = priKey;
    }
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getRawAddress() {
        return this.rawAddress;
    }
    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
    }
    public long getBalance() {
        return this.balance;
    }
    public void setBalance(long balance) {
        this.balance = balance;
    }
    public long getPower() {
        return this.power;
    }
    public void setPower(long power) {
        this.power = power;
    }
    public long getMiningIncome() {
        return this.miningIncome;
    }
    public void setMiningIncome(long miningIncome) {
        this.miningIncome = miningIncome;
    }
    public String getNickName() {
        return this.nickName;
    }
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
    public String getMiningState() {
        return this.miningState;
    }
    public void setMiningState(String miningState) {
        this.miningState = miningState;
    }
    public long getTransExpiry() {
        return this.transExpiry;
    }
    public void setTransExpiry(long transExpiry) {
        this.transExpiry = transExpiry;
    }
    public long getBlocksMined() {
        return this.blocksMined;
    }
    public void setBlocksMined(long blocksMined) {
        this.blocksMined = blocksMined;
    }
    public long getMinedNo() {
        return this.MinedNo;
    }
    public void setMinedNo(long MinedNo) {
        this.MinedNo = MinedNo;
    }
    public String getMiningRank() {
        return this.miningRank;
    }
    public void setMiningRank(String miningRank) {
        this.miningRank = miningRank;
    }
    public String getMinerReward() {
        return this.minerReward;
    }
    public void setMinerReward(String minerReward) {
        this.minerReward = minerReward;
    }
    public String getPartReward() {
        return this.partReward;
    }
    public void setPartReward(String partReward) {
        this.partReward = partReward;
    }
}