package io.taucoin.db;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Taucoin Core Developers
 * @since 03.04.2019
 */
public interface BlockNumberStore extends DiskStore {

    void add(Long number);

    void addBatch(Collection<Long> numbers);

    void addBatch(Long startNumber, Long endNumber);

    Long peek();

    Long poll();

    List<Long> pollBatch(int qty);

    boolean isEmpty();

    int size();

    void clear();
}
