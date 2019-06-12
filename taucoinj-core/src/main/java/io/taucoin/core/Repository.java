package io.taucoin.core;

import io.taucoin.db.ByteArrayWrapper;

import java.math.BigInteger;
import java.util.HashMap;

/**
 * @author Roman Mandeleil
 * @since 08.09.2014
 * @author taucoin core
 * @since 01.07.2019
 */
public interface Repository {

    /**
     * Create a new account in the database
     *
     * @param addr of the account
     * @return newly created account state
     */
    AccountState createAccount(byte[] addr);
    AccountState createGenesisAccount(final byte[] addr);
    BigInteger addGenesisBalance(byte[] addr, BigInteger value);

    /**
     * @param addr - account to check
     * @return - true if account exist,
     *           false otherwise
     */
    boolean isExist(byte[] addr);

    /**
     * Retrieve an account
     *
     * @param addr of the account
     * @return account state as stored in the database
     */
    AccountState getAccountState(byte[] addr);

    /**
     * Deletes the account
     *
     * @param addr of the account
     */
    void delete(byte[] addr);

    /**
     * Increase the account forgePower of the given account by one
     *
     * @param addr of the account
     * @return new value of the nonce
     */
    BigInteger increaseforgePower(byte[] addr);

    /**
     * Reduce the account forgePower of the given account by one
     *
     * @param addr of the account
     * @return forge power
     */
    BigInteger reduceForgePower(byte[] addr);

    /**
     * Get current forgePower of a given account
     *
     * @param addr of the account
     * @return value of the forgePower
     */
    BigInteger getforgePower(byte[] addr);


    /**
     * Retrieve balance of an account
     *
     * @param addr of the account
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] addr);

    /**
     * Add value to the balance of an account
     *
     * @param addr of the account
     * @param value to be added
     * @return new balance of the account
     */
    BigInteger addBalance(byte[] addr, BigInteger value);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    void flush();

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    void commit();

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    void rollback();

    /**
     * Check to see if the current repository has an open connection to the database
     *
     * @return <tt>true</tt> if connection to database is open
     */
    boolean isClosed();

    /**
     * Close the database
     */
    void close();

    /**
     * Reset
     */
    void reset();

    void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates);

    void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts);
}
