package io.taucoin.core;

import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.ethereum.util.RLPElement;
import java.util.*;

import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;

import static org.ethereum.crypto.HashUtil.*;

public class AccountState implements Serializable {

    private byte[] rlpEncoded;

    /* A list size equal to the number of transactions sent since 24 h before.
     */
    private HashMap<Integer,Long> tranHistory;

    /*
    * power owned by this account to new block.
    */
    private BigInteger forgePower;

    /* A scalar value equal to the number of iTau owned by this address */
    private BigInteger balance;

    private boolean dirty = false;
    private boolean deleted = false;


    public AccountState() {
        this(BigInteger.ZERO, BigInteger.ZERO);
        tranHistory = new HashMap<Integer,Long>();
    }

    public AccountState(BigInteger forgePower, BigInteger balance) {
        this.forgePower = forgePower;
        this.balance = balance;
    }
    
    //used to initial a account from reposity
    public AccountState(byte[] rlpData) {
        this.rlpEncoded = rlpData;

        RLPList items = (RLPList) RLP.decode2(rlpEncoded).get(0);

        this.forgePower = new BigInteger(1, items.get(0).getRLPData());
        this.balance = new BigInteger(1, items.get(1).getRLPData());
        RLPList trHis = (RLPList) items.get(2);
        for(int i=0; i < trHis.size();++i){
             RLPElement transactionHis = trHis.get(i);
             TransactionInfo trinfo = new TransactionInfo(transactionHis.getRLPData());
             this.tranHistory.put(trinfo.gettrHashcode(),trinfo.gettrTime());
        }

    }

    public BigInteger getforgePower() {
        return forgePower;
    }

    public void setforgePower(BigInteger forgePower) {
        rlpEncoded = null;
        this.forgePower = forgePower;
    }

    public void incrementPower() {
        rlpEncoded = null;
        this.forgePower = forgePower.add(BigInteger.ONE);
        setDirty(true);
    }

    public BigInteger getBalance() {
        return balance;
    }

    public BigInteger addToBalance(BigInteger value) {
        if (value.signum() != 0) rlpEncoded = null;
        this.balance = balance.add(value);
        setDirty(true);
        return this.balance;
    }

    public void subFromBalance(BigInteger value) {
        if (value.signum() != 0) rlpEncoded = null;
        this.balance = balance.subtract(value);
        setDirty(true);
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[][] trHisEncoded = new byte[tranHistory.size() + 2][];
            byte[] forgePower = RLP.encodeBigInteger(this.forgePower);
            byte[] balance = RLP.encodeBigInteger(this.balance);

            trHisEncoded[0] = forgePower;
            trHisEncoded[1] = balance;
            int i = 2;
            for (int hashCode : tranHistory.keySet()) {
              TransactionInfo txf = new TransactionInfo(hashCode,tranHistory.get(hashCode));
              trHisEncoded[i] = txf.getEncoded();
              ++i;
            }
            this.rlpEncoded = RLP.encodeList(trHisEncoded);
         }
        return rlpEncoded;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public AccountState clone() {
        AccountState accountState = new AccountState();
        accountState.addToBalance(this.getBalance());
        accountState.setforgePower(this.getforgePower());
        accountState.setDirty(false);

        return accountState;
    }

    public String toString() {
        String ret = "  Nonce: " + this.getforgePower().toString() + "\n" +
                "  Balance: " + getBalance() + "\n";
        return ret;
    }
}
