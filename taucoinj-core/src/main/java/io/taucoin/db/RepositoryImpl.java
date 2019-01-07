package io.taucoin.db;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Repository;
import io.taucoin.datasource.KeyValueDataSource;
import org.ethereum.json.EtherObjectMapper;
import org.ethereum.json.JSONHelper;
import io.taucoin.trie.SecureTrie;
import io.taucoin.trie.Trie;
import io.taucoin.trie.TrieImpl;
import org.ethereum.util.Functional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;
import static org.ethereum.config.SystemProperties.CONFIG;
import static org.ethereum.crypto.HashUtil.EMPTY_DATA_HASH;
import static org.ethereum.crypto.HashUtil.EMPTY_TRIE_HASH;
import static org.ethereum.crypto.SHA3Helper.sha3;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static io.taucoin.util.ByteUtil.wrap;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 * @author taucoin core
 * @since 01.07.2019
 */
public class RepositoryImpl implements Repository , io.taucoin.facade.Repository{

    public final static String STATE_DB = "state";

    private static final Logger logger = LoggerFactory.getLogger("repository");
    private static final Logger gLogger = LoggerFactory.getLogger("general");

    private Trie worldState;

    private DatabaseImpl stateDB = null;

    private KeyValueDataSource stateDS = null;

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicInteger accessCounter = new AtomicInteger();

    private boolean isSnapshot = false;

    public RepositoryImpl() {

    }

    public RepositoryImpl(boolean createDb) {
    }

    public RepositoryImpl(KeyValueDataSource stateDS) {

        stateDS.setName(STATE_DB);
        stateDS.init();
        this.stateDS = stateDS;

        stateDB = new DatabaseImpl(stateDS);
        worldState = new SecureTrie(stateDB.getDb());
    }

    public RepositoryImpl(String stateDbName) {

        stateDB = new DatabaseImpl(stateDbName);
        worldState = new SecureTrie(stateDB.getDb());
    }


    @Override
    public void reset() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                close();

                stateDS.init();
                stateDB = new DatabaseImpl(stateDS);
                worldState = new SecureTrie(stateDB.getDb());
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
                    stateDB = null;
                }
            }
        });
    }

    @Override
    public boolean isClosed() {
        return stateDB == null;
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
    public void flushNoReconnect() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                gLogger.info("flushing to disk");
                worldState.sync();
            }
        });
    }


    @Override
    public void flush() {
        doWithLockedAccess(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                gLogger.info("flushing to disk");
                worldState.sync();

                byte[] root = worldState.getRootHash();
                reset();
                worldState.setRoot(root);
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
    public void syncToRoot(final byte[] root) {
        doWithAccessCounting(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                worldState.setRoot(root);
            }
        });
    }

    @Override
    public Repository startTracking() {
        return new RepositoryTrack(this);
    }

    @Override
    public void dumpState(Block block, long trFee, int txNumber, byte[] txHash) {
        dumpTrie(block);
        //TODO: getNumber() needed
        if (!(CONFIG.dumpFull() /*CONFIG.dumpBlock() == block.getNumber()*/))
            return;

        // todo: dump block header and the relevant tx

        if (txNumber == 0)
            if (CONFIG.dumpCleanOnRestart()) {
                try{
                	FileUtils.forceDelete(new File(CONFIG.dumpDir()));
                }catch(IOException e){
                	logger.error(e.getMessage(), e);
                }
            }

        String dir = CONFIG.dumpDir() + "/";

        String fileName = "";
        if (txHash != null)
            // here block height is needed
            fileName = String.format("%07d_%d_%s.dmp",/*block.getNumber()*/0, txNumber,
                    Hex.toHexString(txHash).substring(0, 8));
        else {
            fileName = String.format("%07d_c.dmp", /*block.getNumber()*/0);
        }

        File dumpFile = new File(System.getProperty("user.dir") + "/" + dir + fileName);
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {

            dumpFile.getParentFile().mkdirs();
            dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile());
            bw = new BufferedWriter(fw);

            //List<ByteArrayWrapper> keys = this.detailsDB.dumpKeys();

            JsonNodeFactory jsonFactory = new JsonNodeFactory(false);
            ObjectNode blockNode = jsonFactory.objectNode();
            //todo: if no contrast
            //JSONHelper.dumpBlock(blockNode, block, trFee,
            //        this.getRoot(),
            //        keys, this);

            EtherObjectMapper mapper = new EtherObjectMapper();
            bw.write(mapper.writeValueAsString(blockNode));

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getTrieDump() {
        return doWithAccessCounting(new Functional.InvokeWrapperWithResult<String>() {
            @Override
            public String invoke() {
                return worldState.getTrieDump();
            }
        });
    }

    public void dumpTrie(Block block) {
        //todo: same as 206
        if (!(CONFIG.dumpFull() /*CONFIG.dumpBlock() == block.getNumber()*/))
            return;

        String fileName = String.format("%07d_trie.dmp",/*block.getNumber()*/0);
        String dir = CONFIG.dumpDir() + "/";
        File dumpFile = new File(System.getProperty("user.dir") + "/" + dir + fileName);
        FileWriter fw = null;
        BufferedWriter bw = null;

        String dump = getTrieDump();

        try {

            dumpFile.getParentFile().mkdirs();
            dumpFile.createNewFile();

            fw = new FileWriter(dumpFile.getAbsoluteFile());
            bw = new BufferedWriter(fw);

            bw.write(dump);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Set<byte[]> getAccountsKeys() {
        return doWithAccessCounting(new Functional.InvokeWrapperWithResult<Set<byte[]>>() {
            @Override
            public Set<byte[]> invoke() {
                Set<byte[]> result = new HashSet<>();
                //todo: need state Cache
                //for (ByteArrayWrapper key : stateCache.keySet()) {
                    //todo: all accountstate key needed probably
                //}

                return result;
            }
        });
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {

        AccountState account = getAccountStateOrCreateNew(addr);

        BigInteger result = account.addToBalance(value);
        updateAccountState(addr, account);

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

    private void updateAccountState(final byte[] addr, final AccountState accountState) {
        doWithAccessCounting(new Functional.InvokeWrapper() {
            @Override
            public void invoke() {
                worldState.update(addr, accountState.getEncoded());
            }
        });
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
                worldState.delete(addr);
//                dds.remove(addr);
            }
        });
    }

    @Override
    public AccountState getAccountState(final byte[] addr) {
        return doWithAccessCounting(new Functional.InvokeWrapperWithResult<AccountState>() {
            @Override
            public AccountState invoke() {
                AccountState result = null;
                byte[] accountData = worldState.get(addr);

                if (accountData.length != 0)
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

    @Override
    public byte[] getRoot() {
        return worldState.getRootHash();
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


    @Override
    public Repository getSnapshotTo(byte[] root){

        TrieImpl trie = new SecureTrie(stateDS);
        trie.setRoot(root);
        trie.setCache(((TrieImpl)(worldState)).getCache());

        RepositoryImpl repo = new RepositoryImpl();
        repo.worldState = trie;
        repo.stateDB = this.stateDB;
        repo.stateDS = this.stateDS;

        repo.isSnapshot = true;

        return repo;
    }
}
