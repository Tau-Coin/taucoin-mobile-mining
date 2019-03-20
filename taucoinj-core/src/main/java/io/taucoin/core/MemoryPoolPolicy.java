package io.taucoin.core;

import org.slf4j.*;

import java.util.Comparator;

public class MemoryPoolPolicy implements Comparator<MemoryPoolEntry> {

    private static final Logger log = LoggerFactory.getLogger(MemoryPoolPolicy.class);

    @Override
    public int compare(MemoryPoolEntry entry1, MemoryPoolEntry entry2) {
        if (Utils.bytesToHexString(entry1.tx.getHash()).equals(Utils.bytesToHexString(entry2.tx.getHash()))) {
            return 0;
        }

        if ( entry2.fee > 0 && entry1.fee > entry2.fee ) {
            return 1;
        }

        if (entry1.buildTime < entry2.buildTime) {
            return 1;
        }

        return -1;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
