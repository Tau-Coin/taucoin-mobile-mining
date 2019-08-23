package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @version 1.0
 * increase power
 */
@Entity
public class IncreasePower {
    @Id
    private Long id;
    private String address;
    private String budget;
    private String fee;
    private long consume;
    private long count;
    @Generated(hash = 951333179)
    public IncreasePower(Long id, String address, String budget, String fee,
            long consume, long count) {
        this.id = id;
        this.address = address;
        this.budget = budget;
        this.fee = fee;
        this.consume = consume;
        this.count = count;
    }
    @Generated(hash = 1275249500)
    public IncreasePower() {
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
    public String getBudget() {
        return this.budget;
    }
    public void setBudget(String budget) {
        this.budget = budget;
    }
    public String getFee() {
        return this.fee;
    }
    public void setFee(String fee) {
        this.fee = fee;
    }
    public long getConsume() {
        return this.consume;
    }
    public void setConsume(long consume) {
        this.consume = consume;
    }
    public long getCount() {
        return this.count;
    }
    public void setCount(long count) {
        this.count = count;
    }
}
