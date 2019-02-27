package io.taucoin.trie;

import io.taucoin.datasource.KeyValueDataSource;

import static io.taucoin.crypto.SHA3Helper.sha3;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;

public class SecureTrie extends TrieImpl implements Trie {

    public SecureTrie(KeyValueDataSource db) {
        this(db, "");
    }

    public SecureTrie(KeyValueDataSource db, Object root) {
        super(db, root);
    }

    @Override
    public byte[] get(byte[] key) {
        return super.get(sha3(key));
    }

    @Override
    public void update(byte[] key, byte[] value) {
        super.update(sha3(key), value);
    }

    @Override
    public void delete(byte[] key) {
        this.update(key, EMPTY_BYTE_ARRAY);
    }

    @Override
    public SecureTrie clone() {
        this.getCache();
        this.getRoot();

        return null;
    }
}
