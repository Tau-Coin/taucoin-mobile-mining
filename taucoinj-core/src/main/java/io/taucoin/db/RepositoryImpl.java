package io.taucoin.db;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Repository;
import io.taucoin.datasource.KeyValueDataSource;
import io.taucoin.json.EtherObjectMapper;
import io.taucoin.json.JSONHelper;
import io.taucoin.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.crypto.HashUtil.EMPTY_DATA_HASH;
import static io.taucoin.crypto.HashUtil.EMPTY_TRIE_HASH;
import static io.taucoin.crypto.SHA3Helper.sha3;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static io.taucoin.util.ByteUtil.wrap;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 * @author taucoin core
 * @since 01.07.2019
 */
public class RepositoryImpl implements io.taucoin.facade.Repository{

    public final static String STATE_DB = "state";

    private static final Logger logger = LoggerFactory.getLogger("repository");
    private static final Logger gLogger = LoggerFactory.getLogger("general");

    private KeyValueDataSource stateDB = null;

    public RepositoryImpl() {
    }

    public RepositoryImpl(KeyValueDataSource stateDS) {

        stateDS.setName(STATE_DB);
        stateDS.init();
        this.stateDB = stateDS;
    }

    @Override
    public synchronized void reset() {
        close();
        stateDB.init();
    }

    @Override
    public synchronized void close() {
        if (stateDB != null) {
            stateDB.close();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return stateDB.isAlive();
    }

    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> stateCache) {

        logger.debug("updatingBatch: stateCache.size: {}", stateCache.size());

        for (ByteArrayWrapper hash : stateCache.keySet()) {

            AccountState accountState = stateCache.get(hash);

            if (accountState.isDeleted()) {
                delete(hash.getData());
                logger.debug("delete: [{}]",
                        Hex.toHexString(hash.getData()));

            } else {
                updateAccountState(hash.getData(), accountState);

                if (logger.isDebugEnabled()) {
                    logger.debug("update: [{}],forgePower: [{}] balance: [{}] \n",
                            Hex.toHexString(hash.getData()),
                            accountState.getforgePower(),
                            accountState.getBalance());
                }
            }
        }


        logger.debug("updated: stateCache.size: {}", stateCache.size());

        stateCache.clear();
    }

    @Override
    public synchronized void flush() {
        gLogger.info("flushing to disk");
    }

    @Override
    public synchronized void rollback() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Repository startTracking() {
        return new RepositoryTrack(this);
    }

    @Override
    public synchronized BigInteger addBalance(byte[] addr, BigInteger value) {

        AccountState account = getAccountStateOrCreateNew(addr);

        BigInteger result = account.addToBalance(value);
        updateAccountState(addr, account);

        return result;
    }

    public synchronized BigInteger addGenesisBalance(byte[] addr, BigInteger value) {

        AccountState account = getAccountStateOrCreateNew(addr);

        BigInteger result = account.addToBalance(value);
        updateGenesisAccountState(addr, account);

        return result;
    }

    @Override
    public synchronized BigInteger getBalance(byte[] addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? BigInteger.ZERO : account.getBalance();
    }

    @Override
    public synchronized BigInteger getforgePower(byte[] addr) {
        return getAccountStateOrCreateNew(addr).getforgePower();
    }

    @Nonnull
    private AccountState getAccountStateOrCreateNew(byte[] addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? createAccount(addr) : account;
    }

    @Override
    public synchronized BigInteger increaseforgePower(byte[] addr) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.incrementforgePower();
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    @Override
    public synchronized BigInteger reduceForgePower(byte[] addr) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.reduceForgePower();
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    private synchronized void updateAccountState(final byte[] addr, final AccountState accountState) {
        stateDB.put(addr, accountState.getEncoded());
    }

    private synchronized void updateGenesisAccountState(final byte[] addr, final AccountState accountState) {
        stateDB.put(addr, accountState.getEncoded());
    }

    public synchronized BigInteger setforgePower(final byte[] addr, final BigInteger forgePower) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.setforgePower(forgePower);
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    @Override
    public synchronized void delete(final byte[] addr) {
        stateDB.delete(addr);
    }

    @Override
    public synchronized AccountState getAccountState(final byte[] addr) {
        AccountState result = null;
        byte[] accountData = stateDB.get(addr);

        if (accountData != null)
            result = new AccountState(accountData);

        return result;
    }

    @Override
    public synchronized AccountState createAccount(final byte[] addr) {
        AccountState accountState = new AccountState();

        updateAccountState(addr, accountState);

        return accountState;
    }

    //used for me
    public synchronized AccountState createGenesisAccount(final byte[] addr) {
        AccountState accountState = new AccountState();

        updateGenesisAccountState(addr, accountState);

        return accountState;
    }
    @Override
    public boolean isExist(byte[] addr) {
        return getAccountState(addr) != null;
    }

    @Override
    public void loadAccount(byte[] addr,
                            HashMap<ByteArrayWrapper, AccountState> cacheAccounts) {

        AccountState account = getAccountState(addr);

        account = (account == null) ? new AccountState() : account.clone();

        ByteArrayWrapper wrappedAddress = wrap(addr);
        cacheAccounts.put(wrappedAddress, account);
    }
}
