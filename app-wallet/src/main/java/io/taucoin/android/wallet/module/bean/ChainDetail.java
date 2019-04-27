package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class ChainDetail {

    @SerializedName(value = "genesishash")
    private String genesisHash;
    @SerializedName(value = "totalheight")
    private int totalHeight;
    @SerializedName(value = "previoushash")
    private String previousHash;
    @SerializedName(value = "currenthash")
    private String currentHash;
    @SerializedName(value = "totaldifficulty")
    private String totalDifficulty;

    public String getGenesisHash() {
        return genesisHash;
    }

    public void setGenesisHash(String genesisHash) {
        this.genesisHash = genesisHash;
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    public void setTotalHeight(int totalHeight) {
        this.totalHeight = totalHeight;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public String getTotalDifficulty() {
        return totalDifficulty;
    }

    public void setTotalDifficulty(String totalDifficulty) {
        this.totalDifficulty = totalDifficulty;
    }
}
