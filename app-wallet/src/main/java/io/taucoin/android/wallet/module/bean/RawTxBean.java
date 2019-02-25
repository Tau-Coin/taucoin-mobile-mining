package io.taucoin.android.wallet.module.bean;

public class RawTxBean {

    /**
     * txid : 0xe8dd583ebb7049641cc707b39a10b8ef079e37588726ff7db5f096b16044aaf5
     * addin : TJdpA3upc9MYQdvGDAET786ehGscK8Vqen
     * addout : TLQr4jCQXzopTVfzgpGPP4h9wihshMkz2b
     * vout : 2000000000000000
     * fee : 10
     * blockNum : 0
     * blockHash : 0x0e19e167b7160f7ce173dbe8dbf50d2266365907
     * blockTime : 1548147259
     */

    private String txid;
    private String addin;
    private String addout;
    private String vout;
    private String fee;
    private int blockNum;
    private String blockHash;
    private long blockTime;
    private int state;

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getAddin() {
        return addin;
    }

    public void setAddin(String addin) {
        this.addin = addin;
    }

    public String getAddout() {
        return addout;
    }

    public void setAddout(String addout) {
        this.addout = addout;
    }

    public String getVout() {
        return vout;
    }

    public void setVout(String vout) {
        this.vout = vout;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public int getBlockNum() {
        return blockNum;
    }

    public void setBlockNum(int blockNum) {
        this.blockNum = blockNum;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public long getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(long blockTime) {
        this.blockTime = blockTime;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
