package io.taucoin.android.wallet.module.bean;

public class BalanceBean {

    private String address;
    private String balance;
    private String forgepower;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getForgepower() {
        return forgepower;
    }

    public void setForgepower(String forgepower) {
        this.forgepower = forgepower;
    }
}
