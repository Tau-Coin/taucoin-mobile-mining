package io.taucoin.android.db;

import io.taucoin.config.SystemProperties;
import io.taucoin.core.AccountState;
import io.taucoin.datasource.KeyValueDataSource;
import io.taucoin.db.ByteArrayWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class AccountStateDatabaseImpl implements KeyValueDataSource {

    private static final Logger logger = LoggerFactory.getLogger("accountdb");

    private static final int MAX_CACHE_SIZE = 5000;

    KeyValueDataSource dbImpl;

    private LRUCache accountsCache
            = new LRUCache(MAX_CACHE_SIZE, 0.75f);

    private class LRUCache extends LinkedHashMap<byte[], byte[]> {
        private static final long serialVersionUID = 1L;
        private int capacity;

        public LRUCache(int capacity, float loadFactor) {
            super(capacity, loadFactor, false);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<byte[], byte[]> eldest) {
            return size() > this.capacity;
        }
    }

    public AccountStateDatabaseImpl(KeyValueDataSource dbImpl) {
        this.dbImpl = dbImpl;
    }

    @Override
    public void init() {
        dbImpl.init();
    }

    @Override
    public boolean isAlive() {
        return dbImpl.isAlive();
    }

    @Override
    public void setName(String name) {
        dbImpl.setName(name);
    }

    @Override
    public String getName() {
        return dbImpl.getName();
    }

    @Override
    public byte[] get(byte[] key) {
        byte[] value = accountsCache.get(key);
        if (value == null) {
            value = dbImpl.get(key);
            if (value != null) {
                accountsCache.put(key, value);
            }
        }

        return value;
    }

    @Override
    public byte[] put(byte[] key, byte[] value) {
        dbImpl.put(key, value);
        accountsCache.put(key, value);
        return value;
    }

    @Override
    public void delete(byte[] key) {
        dbImpl.delete(key);
        accountsCache.remove(key);
    }

    @Override
    public Set<byte[]> keys() {
        Set<byte[]> result = dbImpl.keys();
        /**
        for (Map.Entry<byte[], byte[]> entry : accountsCache.entrySet()) {
            result.add(entry.getKey());
        }
        */

        return result;
    }

    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        dbImpl.updateBatch(rows);
        accountsCache.putAll(rows);
    }

    @Override
    public void close() {
        dbImpl.close();
        accountsCache.clear();
    }
}
