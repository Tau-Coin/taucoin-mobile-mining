package io.taucoin.android.interop;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

@Deprecated
public class BlockTxReindex implements Parcelable {
    boolean isCompleted = false;
    boolean isFind = true;
    private byte[] txid;
    private byte[] blockhash;
    private HashMap<byte[],Long> minerFee = new HashMap<>();
    private HashMap<byte[],Long> lastWitFee = new HashMap<>();
    private HashMap<byte[],Long> lastAssociateFee = new HashMap<>();
    public BlockTxReindex(){

    }

    public BlockTxReindex(Parcel in) {
        boolean[] semaphoreFlag = new boolean[2];
        in.readBooleanArray(semaphoreFlag);
        this.isCompleted = semaphoreFlag[0];
        this.isFind = semaphoreFlag[1];
        byte[] txhash = new byte[32];
        in.readByteArray(txhash);
        this.txid = txhash;
        byte[] blockHash = new byte[20];
        in.readByteArray(blockHash);
        this.blockhash = blockHash;
        this.minerFee = in.readHashMap(this.getClass().getClassLoader());
        this.lastWitFee = in.readHashMap(this.getClass().getClassLoader());
        this.lastAssociateFee = in.readHashMap(this.getClass().getClassLoader());
    }

    public HashMap<byte[], Long> getMinerFee() {
        return minerFee;
    }

    public HashMap<byte[], Long> getLastWitFee() {
        return lastWitFee;
    }

    public HashMap<byte[], Long> getLastAssociateFee() {
        return lastAssociateFee;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setTxid(byte[] txid) {
        this.txid = txid;
    }

    public boolean isFind() {
        return isFind;
    }

    public void setFind(boolean find) {
        this.isFind = find;
    }

    public void updateAssociatedFee(byte[] addr,long fee) {
        if (lastAssociateFee.containsKey(addr)) {
            long temp = lastAssociateFee.get(addr).longValue() + fee;
            lastAssociateFee.put(addr,temp);
        } else {
            lastAssociateFee.put(addr,fee);
        }
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        boolean[] semaphoreFlag = new boolean[2];
        semaphoreFlag[0] = this.isCompleted;
        semaphoreFlag[1] = this.isFind;
        parcel.writeBooleanArray(semaphoreFlag);

        parcel.writeByteArray(txid);
        parcel.writeByteArray(blockhash);
        parcel.writeMap(minerFee);
        parcel.writeMap(lastWitFee);
        parcel.writeMap(lastAssociateFee);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BlockTxReindex createFromParcel(Parcel parcel) {
            return new BlockTxReindex(parcel);
        }

        @Override
        public BlockTxReindex[] newArray(int i) {
            return new BlockTxReindex[i];
        }
    };

    public byte[] getTxid() {
        return txid;
    }

    public byte[] getBlockhash() {
        return blockhash;
    }

    public void setBlockhash(byte[] blockhash) {
        this.blockhash = blockhash;
    }
}
