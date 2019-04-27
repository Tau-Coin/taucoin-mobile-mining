package io.taucoin.platform.adress;

public class Key {
    private String priKey;
    private String pubKey;
    private String address;
    private String rawAddress;

    Key() {
        Reset();
    }

    void Reset() {
        this.priKey = null;
        this.pubKey  = null;
        this.address = null;
        this.rawAddress = null;
    }

    public String getPriKey() {
        return priKey;
    }

    public void setPriKey(String priKey) {
        this.priKey = priKey;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public void setRawAddress(String rawAddress) {
        this.rawAddress = rawAddress;
    }

    public String ToString() {
        return "{\n"
            + "\t priKey:" + this.priKey + "\n"
            + "\t pubKey :" + this.pubKey  + "\n"
            + "\t address:" + this.address + "\n"
            + "\t rawAddress:" + this.rawAddress + "\n"
            + "}\n";
    }
}
