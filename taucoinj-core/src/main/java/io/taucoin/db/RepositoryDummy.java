package io.taucoin.db;

import io.taucoin.core.AccountState;
import org.ethereum.core.Block;
import io.taucoin.core.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.ethereum.crypto.SHA3Helper.sha3;
import static io.taucoin.util.ByteUtil.wrap;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 */
public class RepositoryDummy extends RepositoryImpl {

    private static final Logger logger = LoggerFactory.getLogger("repository");
    private Map<ByteArrayWrapper, AccountState> worldState = new HashMap<>();

    public RepositoryDummy() {
        super(false);
    }

    @Override
    public void reset() {

        worldState.clear();
    }

    @Override
    public void close() {
        worldState.clear();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> stateCache) {

        for (ByteArrayWrapper hash : stateCache.keySet()) {

            AccountState accountState = stateCache.get(hash);

            if (accountState.isDeleted()) {
                worldState.remove(hash);

                logger.debug("delete: [{}]",
                        Hex.toHexString(hash.getData()));

            } else {

                if (accountState.isDirty()) {
                    //TODO: two tries may confused
                    //accountState.setStateRoot(contractDetails.getStorageHash());
                    worldState.put(hash, accountState);
                    if (logger.isDebugEnabled()) {
                        logger.debug("update: [{}],forgePower: [{}] balance: [{}] \n",
                                Hex.toHexString(hash.getData()),
                                accountState.getforgePower(),
                                accountState.getBalance());
                    }

                }

            }
        }

        stateCache.clear();
    }


    @Override
    public void flush() {
        throw new UnsupportedOperationException();
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
    public void syncToRoot(byte[] root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository startTracking() {
        return new RepositoryTrack(this);
    }

    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {

    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        return null;
    }

    public Set<ByteArrayWrapper> getFullAddressSet() {
        return worldState.keySet();
    }


    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {
        AccountState account = getAccountState(addr);

        if (account == null)
            account = createAccount(addr);

        BigInteger result = account.addToBalance(value);
        worldState.put(wrap(addr), account);

        return result;
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        AccountState account = getAccountState(addr);

        if (account == null)
            return BigInteger.ZERO;

        return account.getBalance();
    }

    @Override
    public BigInteger getforgePower(byte[] addr) {
        AccountState account = getAccountState(addr);

        if (account == null)
            account = createAccount(addr);

        return account.getforgePower();
    }

    @Override
    public BigInteger increaseforgePower(byte[] addr) {
        AccountState account = getAccountState(addr);

        if (account == null)
            account = createAccount(addr);

        account.incrementforgePower();
        worldState.put(wrap(addr), account);

        return account.getforgePower();
    }

    public BigInteger setforgePower(byte[] addr, BigInteger nonce) {

        AccountState account = getAccountState(addr);

        if (account == null)
            account = createAccount(addr);

        account.setforgePower(nonce);
        worldState.put(wrap(addr), account);

        return account.getforgePower();
    }


    @Override
    public void delete(byte[] addr) {
        worldState.remove(wrap(addr));
    }

    @Override
    public AccountState getAccountState(byte[] addr) {
        return worldState.get(wrap(addr));
    }

    @Override
    public AccountState createAccount(byte[] addr) {
        AccountState accountState = new AccountState();
        worldState.put(wrap(addr), accountState);

        return accountState;
    }


    @Override
    public boolean isExist(byte[] addr) {
        return getAccountState(addr) != null;
    }

    @Override
    public byte[] getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts) {

        AccountState account = getAccountState(addr);

        if (account == null)
            account = new AccountState();
        else
            account = account.clone();

        cacheAccounts.put(wrap(addr), account);
    }
}
