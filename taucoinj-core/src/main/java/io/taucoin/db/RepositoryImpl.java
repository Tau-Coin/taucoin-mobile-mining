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

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger accessCounter = new AtomicInteger();

    public RepositoryImpl() {
    }

    public RepositoryImpl(KeyValueDataSource stateDS) {

        stateDS.setName(STATE_DB);
        stateDS.init();
        this.stateDB = stateDS;
    }

    @Override
    public void reset() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                close();

                stateDB.init();
            }
        });
    }

    @Override
    public void close() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {

                if (stateDB != null) {
                    stateDB.close();
                }
            }
        });
    }

    @Override
    public boolean isClosed() {
        return stateDB.isAlive();
    }

    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> stateCache) {

        logger.info("updatingBatch: stateCache.size: {}", stateCache.size());

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


        logger.info("updated: stateCache.size: {}", stateCache.size());

        stateCache.clear();
    }

    @Override
    public void flush() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                gLogger.info("flushing to disk");
            }
        });
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository startTracking() {
        return new RepositoryTrack(this);
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {

        AccountState account = getAccountStateOrCreateNew(addr);

        BigInteger result = account.addToBalance(value);
        updateAccountState(addr, account);

        return result;
    }

    public BigInteger addGenesisBalance(byte[] addr, BigInteger value) {

        AccountState account = getAccountStateOrCreateNew(addr);

        BigInteger result = account.addToBalance(value);
        updateGenesisAccountState(addr, account);

        return result;
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? BigInteger.ZERO : account.getBalance();
    }

    @Override
    public BigInteger getforgePower(byte[] addr) {
        return getAccountStateOrCreateNew(addr).getforgePower();
    }

    @Nonnull
    private AccountState getAccountStateOrCreateNew(byte[] addr) {
        AccountState account = getAccountState(addr);
        return (account == null) ? createAccount(addr) : account;
    }

    @Override
    public BigInteger increaseforgePower(byte[] addr) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.incrementforgePower();
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    @Override
    public BigInteger reduceForgePower(byte[] addr) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.reduceForgePower();
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    private void updateAccountState(final byte[] addr, final AccountState accountState) {
        doWithAccessCounting(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                stateDB.put(addr, accountState.getEncoded());
            }
        });
    }
    private void updateGenesisAccountState(final byte[] addr, final AccountState accountState) {
            stateDB.put(addr, accountState.getEncoded());
    }

    public BigInteger setforgePower(final byte[] addr, final BigInteger forgePower) {
        AccountState account = getAccountStateOrCreateNew(addr);

        account.setforgePower(forgePower);
        updateAccountState(addr, account);

        return account.getforgePower();
    }

    @Override
    public void delete(final byte[] addr) {
        doWithAccessCounting(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                stateDB.delete(addr);
            }
        });
    }

    @Override
    public AccountState getAccountState(final byte[] addr) {
        return doWithAccessCounting(new Functional.InvokeWrapperWithResult<AccountState>() {
            @Override
            public AccountState invoke() {
                AccountState result = null;
                byte[] accountData = stateDB.get(addr);

                if (accountData != null)
                    result = new AccountState(accountData);

                return result;
            }
        });
    }

    @Override
    public AccountState createAccount(final byte[] addr) {
        AccountState accountState = new AccountState();

        updateAccountState(addr, accountState);

        return accountState;
    }

    //used for me
    public AccountState createGenesisAccount(final byte[] addr) {
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

    private void doWithLockedAccess(Functional.InvokeWrapper wrapper) {
        lock.lock();
        try {
            while (accessCounter.get() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for access ...");
                }
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    logger.error("Error occurred during access waiting: ", e);
                }
            }

            wrapper.invoke();
        } finally {
            lock.unlock();
        }
    }

    public <R> R doWithAccessCounting(Functional.InvokeWrapperWithResult<R> wrapper) {
        while (lock.isLocked()) {
            if (logger.isDebugEnabled()) {
                logger.debug("waiting for lock releasing ...");
            }
            try {
                sleep(100);
            } catch (InterruptedException e) {
                logger.error("Error occurred during locked access waiting: ", e);
            }
        }
        accessCounter.incrementAndGet();
        try {
            return wrapper.invoke();
        } finally {
            accessCounter.decrementAndGet();
        }
    }

    public void doWithAccessCounting(final Functional.InvokeWrapper wrapper) {
        doWithAccessCounting(new Functional.InvokeWrapperWithResult<Object>() {
            @Override
            public Object invoke() {
                wrapper.invoke();
                return null;
            }
        });
    }

}
