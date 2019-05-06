package io.taucoin.core;

public class AssociatedAccount {
    private byte[] senderAddress;
    private AccountState senderAccount;
    private byte[] receiveAddress;
    private AccountState receiveAccount;

    public AssociatedAccount(byte[] senderAddress, AccountState senderAccount,
                             byte[] receiveAddress, AccountState receiveAccount) {
        this.senderAddress = senderAddress;
        this.senderAccount = senderAccount;
        this.receiveAddress = receiveAddress;
        this.receiveAccount = receiveAccount;
    }

    public byte[] getSenderAddress() {
        return senderAddress;
    }

    public AccountState getSenderAccount() {
        return senderAccount;
    }

    public byte[] getReceiveAddress() {
        return receiveAddress;
    }

    public AccountState getReceiveAccount() {
        return receiveAccount;
    }
}
