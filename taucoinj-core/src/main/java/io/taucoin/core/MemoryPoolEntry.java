package io.taucoin.core;

import io.taucoin.util.ByteUtil;
import org.slf4j.*;

/**
 * Transaction wrapper in tx memory pool entry.
 */
public class MemoryPoolEntry {

    private static final Logger log = LoggerFactory.getLogger(MemoryPoolEntry.class);

    public Transaction tx;
    public long buildTime;
    public long fee;

    protected MemoryPoolEntry(Transaction tx) {
        this.tx = tx;
        this.buildTime = ByteUtil.byteArrayToLong(tx.getTime());
        this.fee = ByteUtil.byteArrayToLong(tx.getFee());
    }

    public static MemoryPoolEntry with(Transaction tx) {
        return new MemoryPoolEntry(tx);
    }
}
