package io.taucoin.core;

import io.taucoin.crypto.ECKey;
import io.taucoin.util.Utils;

import javax.inject.Inject;

import java.math.BigInteger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Representation of an actual account or contract
 */
public class Account {

    private ECKey ecKey;
    private Address address;

    private Set<Transaction> pendingTransactions =
            Collections.synchronizedSet(new HashSet<Transaction>());
    @Inject
    Repository repository;


    public Account() {
    }

    public void init() {
        this.ecKey = new ECKey(Utils.getRandom());
        address = this.ecKey.getAccountAddress();
    }

    public void init(ECKey ecKey) {
        this.ecKey = ecKey;
        address = this.ecKey.getAccountAddress();
    }

    public BigInteger getforgePower() {
        return repository.getforgePower(getAddress());
    }

    public BigInteger getBalance() {

        BigInteger balance =
                repository.getBalance(this.getAddress());

        synchronized (getPendingTransactions()) {
            if (!getPendingTransactions().isEmpty()) {

                for (Transaction tx : getPendingTransactions()) {
                    if (Arrays.equals(getAddress(), tx.getSender())) {
                        balance = balance.subtract(new BigInteger(1, tx.getAmount()));
                    }

                    if (Arrays.equals(getAddress(), tx.getReceiveAddress())) {
                        balance = balance.add(new BigInteger(1, tx.getAmount()));
                    }
                }
                // todo: calculate the fee for pending
            }
        }


        return balance;
    }


    public ECKey getEcKey() {
        return ecKey;
    }

    public byte[] getAddress() {
        return address.getHash160();
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Set<Transaction> getPendingTransactions() {
        return this.pendingTransactions;
    }

    public void addPendingTransaction(Transaction transaction) {
        synchronized (pendingTransactions) {
            pendingTransactions.add(transaction);
        }
    }

    public void clearAllPendingTransactions() {
        synchronized (pendingTransactions) {
            pendingTransactions.clear();
        }
    }
}
