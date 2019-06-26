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

    public MemoryPoolEntry(Transaction tx) {
        this.tx = tx;
        this.buildTime = ByteUtil.byteArrayToLong(tx.getTime());
        this.fee = ByteUtil.byteArrayToLong(tx.getFee());
    }

    public static MemoryPoolEntry with(Transaction tx) {
        return new MemoryPoolEntry(tx);
    }
    @Override
    public boolean equals(Object entry){
        if (entry == null) return false;
        if (entry instanceof MemoryPoolEntry) {
            MemoryPoolEntry tempEntry = (MemoryPoolEntry)entry;
            if (this.hashCode() == tempEntry.hashCode())
                return true;
        }
        return false;
    }
}
