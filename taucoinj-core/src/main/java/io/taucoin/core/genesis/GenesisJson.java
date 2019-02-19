package io.taucoin.core.genesis;

import java.util.ArrayList;
import java.util.Map;
/*
*Items 1-9 construct tau ,1-6 will be recorded in genesis block.
* what more 7-8 combined also, to reduce block size item9 is excepted.
*/
public class GenesisJson {

    String version;
    String timestamp;
    String previousHeaderHash;
    String generatorPublicKey;

    String blockSignature;
    String option;

    ArrayList<String> coinbase;
    Map<String, AllocatedAccount> alloc;

    String geneBasetarget;
    public GenesisJson() {
    }


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getPreviousHeaderHash() {
        return previousHeaderHash;
    }

    public void setPreviousHeaderHash(String previousHeaderHash) {
        this.previousHeaderHash = previousHeaderHash;
    }

    public String getGeneratorPublicKey() {
        return generatorPublicKey;
    }

    public void setGeneratorPublicKey(String generatorPublicKey) {
        this.geneBasetarget = generatorPublicKey;
    }

    public String getBlockSignature() {
        return blockSignature;
    }

    public void setBlockSignature(String blockSignature) {
        this.blockSignature = blockSignature;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public String getGeneBasetarget() {
        return geneBasetarget;
    }

    public void setGeneBasetarget(String geneBasetarget) {
        this.geneBasetarget = geneBasetarget;
    }

    public ArrayList<String> getCoinbase() {
        return coinbase;
    }

    public void setCoinbase(ArrayList<String> coinbase) {
        this.coinbase = coinbase;
    }

    public Map<String, AllocatedAccount> getAlloc() {
        return alloc;
    }

    public void setAlloc(Map<String, AllocatedAccount> alloc) {
        this.alloc = alloc;
    }
}
