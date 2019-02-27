package io.taucoin.core;

import io.taucoin.db.ByteArrayWrapper;

import java.math.BigInteger;

import java.util.HashMap;
import java.util.Set;

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
     * @return Returns set of all the account addresses
     */
    Set<byte[]> getAccountsKeys();


    /**
     * Dump the full state of the current repository into a file with JSON format
     * It contains all the account, their attributes and
     *
     * @param block of the current state
     * @param trFee the amount of trFee used in the block until that point
     * @param txNumber is the number of the transaction for which the dump has to be made
     * @param txHash is the hash of the given transaction.
     * If null, the block state post coinbase is dumped.
     */
    void dumpState(Block block, long trFee, int txNumber, byte[] txHash);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    void flush();

    void flushNoReconnect();


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
     * Return to one of the previous snapshots
     * by moving the root.
     *
     * @param root - new root
     */
    void syncToRoot(byte[] root);

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


    byte[] getRoot();

    void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts);

    Repository getSnapshotTo(byte[] root);

}
