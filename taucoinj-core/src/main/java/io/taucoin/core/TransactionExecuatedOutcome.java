package io.taucoin.core;

import java.util.HashMap;
import java.util.Map;

public class TransactionExecuatedOutcome {
    private byte[] blockhash = null;
    private HashMap<byte[],Long> senderAssociated = new HashMap<>();
    private Map<byte[],Long> lastWintess;
    private Map<byte[],Long> currentWintess;

    private byte[] senderAddress;
    private byte[] receiveAddress;
    private byte[] txid;

    private boolean isTxComplete = false;

    public TransactionExecuatedOutcome() {
    }

    public void setBlockHash(byte[] blockhash) {
        this.blockhash = blockhash;
    }

    public void setSenderAssociated(HashMap<byte[], Long> senderAssociated) {
        this.senderAssociated.putAll(senderAssociated);
    }

    public void setLastWintess(Map<byte[], Long> lastWintess) {
        this.lastWintess = lastWintess;
    }

    public void setCurrentWintess(Map<byte[], Long> currentWintess) {
        this.currentWintess = currentWintess;
    }

    public void setSenderAddress(byte[] senderAddress) {
        this.senderAddress = senderAddress;
    }

    public void setReceiveAddress(byte[] receiveAddress) {
        this.receiveAddress = receiveAddress;
    }

    public void setTxComplete(boolean txComplete) {
        isTxComplete = txComplete;
    }

    public void setTxid(byte[] txid) {
        this.txid = txid;
    }

    public byte[] getTxid() {
        return txid;
    }

    public byte[] getBlockhash() {
        return blockhash;
    }

    public HashMap<byte[], Long> getSenderAssociated() {
        return senderAssociated;
    }

    public Map<byte[], Long> getLastWintess() {
        return lastWintess;
    }

    public Map<byte[], Long> getCurrentWintess() {
        return currentWintess;
    }

    public byte[] getSenderAddress() {
        return senderAddress;
    }

    public byte[] getReceiveAddress() {
        return receiveAddress;
    }

    public boolean isTxComplete() {
        return isTxComplete;
    }

    /**
     * a sender that may associate multiple address that will share
     * this fee cake.
     * @param address
     * @param sharefee
     */
    public void updateSenderAssociated(byte[] address,long sharefee){
        if (this.senderAssociated.containsKey(address)) {
            long temp = senderAssociated.get(address) + sharefee;
            this.senderAssociated.put(address,temp);
        } else {
            this.senderAssociated.put(address,sharefee);
        }
    }

    public void updateCurrentWintessBalance(byte[] address,long deltafee){
        if (this.currentWintess.containsKey(address)) {
            long temp = currentWintess.get(address) + deltafee;
            this.currentWintess.put(address, temp);
        } else {
            this.currentWintess.put(address, deltafee);
        }
    }
}
