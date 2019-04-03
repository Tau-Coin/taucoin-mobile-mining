package io.taucoin.db;

import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockWrapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mikhail Kalinin
 * @since 09.07.2015
 * @author taucoin core
 * @since 01.07.2019
 */
public interface BlockQueue extends DiskStore {

    void addAll(Collection<BlockWrapper> blockList);

    void add(BlockWrapper block);

    void addOrReplace(BlockWrapper block);

    BlockWrapper poll();

    BlockWrapper peek();

    BlockWrapper take();

    int size();

    boolean isEmpty();

    void clear();

    List<byte[]> filterExisting(Collection<byte[]> hashes);

    List<BlockHeader> filterExistingHeaders(Collection<BlockHeader> headers);

    List<Long> filterExistingNumbers(Collection<Long> numbers);

    boolean isBlockExist(byte[] hash);

    void drop(byte[] nodeId, int scanLimit);
}
