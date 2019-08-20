package io.taucoin.core;

import io.taucoin.config.Constants;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class AccountState implements Serializable {
    private static final Logger log = LoggerFactory.getLogger("accountState");
    private byte[] rlpEncoded;

    /**
     *  A list size equal to the number of transactions sent since 12 h before.
     */
    private TreeMap<Long,byte[]> tranHistory = new TreeMap<Long,byte[]>();

    /**
    * power owned by this account to new block.
    */
    private BigInteger forgePower;

    /**
     *A scalar value equal to the number of iTau owned by this address
     */
    private BigInteger balance;

    /**
     *account address that forger who confirm this state change.
     */
    private byte[] witnessAddress;

    /**
     *account address that associated to newest account state change.
     */
    private ArrayList<byte[]> associatedAddress = new ArrayList<>();

    /**
     *indicate height where transactions occur to lead to this account state
     *change.
     */
    private long stateHeight = 0;

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
        this.witnessAddress = items.get(2).getRLPData();

        RLPList associateList = (RLPList) items.get(3);
        for(int i=0;i < associateList.size();++i) {
            this.associatedAddress.add(associateList.get(i).getRLPData());
        }

        this.stateHeight = items.get(4).getRLPData() == null ? 0
                : ByteUtil.byteArrayToLong(items.get(4).getRLPData());

        if(items.size() > 5) {
            //RLPList trHis = (RLPList) items.get(2);
            for (int i = 5; i < items.size(); ++i) {
                byte[] transactionHis = items.get(i).getRLPData();
                TransactionInfo trinfo = new TransactionInfo(transactionHis);
                this.tranHistory.put(trinfo.gettrTime(),trinfo.gettrHashcode());
            }
        }
    }

    public BigInteger getforgePower() {
        return forgePower;
    }

    public long getStateHeight() {
        return stateHeight;
    }

    public void setStateHeight(long stateHeight) {
        rlpEncoded = null;
        this.stateHeight = stateHeight;
    }

    public void setforgePower(BigInteger forgePower) {
        rlpEncoded = null;
        this.forgePower = forgePower;
    }

    public void setWitnessAddress(byte[] witnessAddress) {
        rlpEncoded = null;
        if (this.stateHeight < Constants.FEE_TERMINATE_HEIGHT) {
            this.witnessAddress = witnessAddress;
        } else {
            //nothing to do!
        }
    }

    public byte[] getWitnessAddress() {
        return witnessAddress;
    }

    public void updateAssociatedAddress(byte[] associatedAddress,long stateHeight) {
        rlpEncoded = null;
        if (stateHeight < Constants.FEE_TERMINATE_HEIGHT) {
            if (this.stateHeight == stateHeight) {
                this.associatedAddress.add(associatedAddress);
            } else {
                this.associatedAddress.clear();
                this.associatedAddress.add(associatedAddress);
                this.stateHeight = stateHeight;
            }
        } else {
            this.associatedAddress.clear();
        }
    }

    public void updateAssociatedAddress(List<byte[]> associatedAddressList,long stateHeight) {
        rlpEncoded = null;
        if (stateHeight < Constants.FEE_TERMINATE_HEIGHT) {
            this.stateHeight = stateHeight;
            this.associatedAddress.clear();
            this.associatedAddress.addAll(associatedAddressList);
        } else {
            this.associatedAddress.clear();
        }
    }

    public void setAssociatedAddress(ArrayList<byte[]> associatedAddress) {
        rlpEncoded = null;
        this.associatedAddress = associatedAddress;
    }

    public ArrayList<byte[]> getAssociatedAddress() {
        return associatedAddress;
    }

    public void setTranHistory(TreeMap<Long,byte[]> tranHistory) {
        rlpEncoded = null;
        this.tranHistory = tranHistory;
    }
    public void incrementforgePower() {
        rlpEncoded = null;
        this.forgePower = forgePower.add(BigInteger.ONE);
        setDirty(true);
    }

    public void reduceForgePower() {
        rlpEncoded = null;
        this.forgePower = forgePower.subtract(BigInteger.ONE);
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

    public BigInteger subFromBalance(BigInteger value) {
        if (value.signum() != 0) rlpEncoded = null;
        this.balance = balance.subtract(value);
        setDirty(true);
        return this.balance;
    }

    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            byte[][] trHisEncoded = new byte[tranHistory.size() + 5][];
            if (this.forgePower.compareTo(BigInteger.ZERO) < 0) {
                throw new IllegalArgumentException("forege power less than 0");
            }
            byte[] forgePower = RLP.encodeBigInteger(this.forgePower);

            if (this.balance.compareTo(BigInteger.ZERO) < 0) {
                if (stateHeight > Constants.VERIFY_BLOCK_HEIGHT) {
                    throw new IllegalArgumentException("unknown error lead to balance less than 0");
                } else {
                    this.balance = BigInteger.ZERO;
                }
            }
            byte[] balance = RLP.encodeBigInteger(this.balance);
            byte[] witnessAddress = RLP.encodeElement(this.witnessAddress);
            byte[][] tempAssociate = new byte[this.associatedAddress.size()][];
            for (int i=0; i< this.associatedAddress.size();++i) {
                 tempAssociate[i] = RLP.encodeElement(this.associatedAddress.get(i));
            }
            byte[] associatedAddress = RLP.encodeList(tempAssociate);
            byte[] stateHeight = RLP.encodeElement(ByteUtil.longToBytes(this.stateHeight));

            trHisEncoded[0] = forgePower;
            trHisEncoded[1] = balance;
            trHisEncoded[2] = witnessAddress;
            trHisEncoded[3] = associatedAddress;
            trHisEncoded[4] = stateHeight;

            int i = 5;
            for (long txTime : tranHistory.keySet()) {
                TransactionInfo txf = new TransactionInfo(txTime,tranHistory.get(txTime));
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
        accountState.setWitnessAddress(this.getWitnessAddress());
        accountState.setAssociatedAddress(this.getAssociatedAddress());
        accountState.setStateHeight(this.getStateHeight());
        accountState.setTranHistory(this.getTranHistory());
        accountState.setDirty(false);

        return accountState;
    }

    public String toString() {
        String ret = "  Nonce: " + this.getforgePower().toString() + "\n" +
                "  Balance: " + getBalance() + "\n";
        return ret;
    }
    public TreeMap<Long,byte[]> getTranHistory(){
        return tranHistory;
    }
}
