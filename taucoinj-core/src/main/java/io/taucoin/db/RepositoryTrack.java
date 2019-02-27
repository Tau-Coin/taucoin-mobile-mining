package io.taucoin.db;

import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.core.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static io.taucoin.crypto.HashUtil.EMPTY_DATA_HASH;
import static io.taucoin.crypto.HashUtil.EMPTY_TRIE_HASH;
import static io.taucoin.crypto.SHA3Helper.sha3;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static io.taucoin.util.ByteUtil.wrap;

/**
 * @author Roman Mandeleil
 * @since 17.11.2014
 */
public class RepositoryTrack implements Repository {

    private static final Logger logger = LoggerFactory.getLogger("repository");

    HashMap<ByteArrayWrapper, AccountState> cacheAccounts = new HashMap<>();

    Repository repository;

    public RepositoryTrack() {
        this.repository = new RepositoryDummy();
    }

    public RepositoryTrack(Repository repository) {
        this.repository = repository;
    }

    @Override
    public AccountState createAccount(byte[] addr) {

        logger.trace("createAccount: [{}]", Hex.toHexString(addr));

        AccountState accountState = new AccountState();
        cacheAccounts.put(wrap(addr), accountState);

        return accountState;
    }
    @Override
    public AccountState createGenesisAccount(final byte[] addr){
        return new AccountState();
    }
    @Override
    public BigInteger addGenesisBalance(byte[] addr, BigInteger value){
        return BigInteger.ZERO;
    }

    @Override
    public AccountState getAccountState(byte[] addr) {

        AccountState accountState = cacheAccounts.get(wrap(addr));

        if (accountState == null) {
            repository.loadAccount(addr, cacheAccounts);

            accountState = cacheAccounts.get(wrap(addr));
        }
        return accountState;
    }

    @Override
    public boolean isExist(byte[] addr) {

        AccountState accountState = cacheAccounts.get(wrap(addr));
        if (accountState != null) return !accountState.isDeleted();

        return repository.isExist(addr);
    }

    @Override
    public void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts) {

        AccountState accountState = this.cacheAccounts.get(wrap(addr));

        if (accountState == null) {
            repository.loadAccount(addr, this.cacheAccounts);
            accountState = this.cacheAccounts.get(wrap(addr));
        }

        cacheAccounts.put(wrap(addr), accountState.clone());
    }


    @Override
    public void delete(byte[] addr) {
        logger.trace("delete account: [{}]", Hex.toHexString(addr));

        getAccountState(addr).setDeleted(true);
    }

    @Override
    public BigInteger increaseforgePower(byte[] addr) {

        AccountState accountState = getAccountState(addr);

        if (accountState == null)
            accountState = createAccount(addr);

        BigInteger savePower = accountState.getforgePower();
        accountState.incrementforgePower();

        logger.trace("increase forgePower addr: [{}], from: [{}], to: [{}]", Hex.toHexString(addr),
                savePower, accountState.getforgePower());

        return accountState.getforgePower();
    }

    public BigInteger setforgePower(byte[] addr, BigInteger bigInteger) {
        AccountState accountState = getAccountState(addr);

        if (accountState == null)
            accountState = createAccount(addr);

        BigInteger savePower = accountState.getforgePower();
        accountState.setforgePower(bigInteger);

        logger.trace("increase forgePower addr: [{}], from: [{}], to: [{}]", Hex.toHexString(addr),
                savePower, accountState.getforgePower());

        return accountState.getforgePower();

    }


    @Override
    public BigInteger getforgePower(byte[] addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? BigInteger.ZERO : accountState.getforgePower();
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        AccountState accountState = getAccountState(addr);
        return accountState == null ? BigInteger.ZERO : accountState.getBalance();
    }

    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {

        AccountState accountState = getAccountState(addr);
        if (accountState == null) {
            accountState = createAccount(addr);
        }
        BigInteger newBalance = accountState.addToBalance(value);

        logger.trace("adding to balance addr: [{}], balance: [{}], delta: [{}]", Hex.toHexString(addr),
                newBalance, value);

        return newBalance;
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        throw new UnsupportedOperationException();
        //return null;
    }


    public Set<ByteArrayWrapper> getFullAddressSet() {
        return cacheAccounts.keySet();
    }

    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository startTracking() {
        logger.debug("start tracking");

        Repository repository = new RepositoryTrack(this);

        return repository;
    }


    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushNoReconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {

        repository.updateBatch(cacheAccounts);
        cacheAccounts.clear();
        logger.debug("committed changes");
    }


    @Override
    public void syncToRoot(byte[] root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() {
        logger.debug("rollback changes");

        cacheAccounts.clear();
    }

    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates) {

        for (ByteArrayWrapper hash : accountStates.keySet()) {
            cacheAccounts.put(hash, accountStates.get(hash));
        }
    }

    @Override // that's the idea track is here not for root calculations
    public byte[] getRoot() {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        throw new UnsupportedOperationException();
    }

    public Repository getOriginRepository() {
        return (repository instanceof RepositoryTrack)
                ? ((RepositoryTrack) repository).getOriginRepository()
                : repository;
    }
}
