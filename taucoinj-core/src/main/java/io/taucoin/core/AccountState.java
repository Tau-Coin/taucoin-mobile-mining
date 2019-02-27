package io.taucoin.core;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.RLPElement;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;

import static io.taucoin.crypto.HashUtil.*;

public class AccountState implements Serializable {
    private static final Logger log = LoggerFactory.getLogger("accountState");
    private byte[] rlpEncoded;

    /* A list size equal to the number of transactions sent since 24 h before.
     */
    private HashMap<Integer,Long> tranHistory = new HashMap<Integer,Long>();

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
    }

    public AccountState(BigInteger forgePower, BigInteger balance) {
        this.forgePower = forgePower;
        this.balance = balance;
    }
    
    //used to initial a account from reposity
    public AccountState(byte[] rlpData) {
        this.rlpEncoded = rlpData;
        RLPList items = (RLPList) RLP.decode2(rlpEncoded).get(0);
        if(items.size()==0){
          log.error("create account sate fail{%d}",items.size());
          System.exit(-1);
        }
        //log.info("account state size is {}",items.size());
        //log.info("forge power in account state is {}",ByteUtil.byteArrayToLong(items.get(0).getRLPData()));

        this.forgePower = items.get(0).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(0).getRLPData());
        this.balance = items.get(1).getRLPData() == null ? BigInteger.ZERO
                : new BigInteger(1, items.get(1).getRLPData());

        if(items.size() > 2) {
            RLPList trHis = (RLPList) items.get(2);
            for (int i = 0; i < trHis.size(); ++i) {
                RLPElement transactionHis = trHis.get(i);
                TransactionInfo trinfo = new TransactionInfo(transactionHis.getRLPData());
                this.tranHistory.put(trinfo.gettrHashcode(), trinfo.gettrTime());
            }
        }
    }

    public BigInteger getforgePower() {
        return forgePower;
    }

    public void setforgePower(BigInteger forgePower) {
        rlpEncoded = null;
        this.forgePower = forgePower;
    }

    public void incrementforgePower() {
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
    public HashMap<Integer,Long> getTranHistory(){
        return tranHistory;
    }
}
