package io.taucoin.net.submit;

import io.taucoin.core.BlockHeader;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Taucoin Core Developers
 * @since 01.28.2019
 */
public class NewBlockHeaderBroadcaster {

    static {
        instance = new NewBlockHeaderBroadcaster();
    }

    public static NewBlockHeaderBroadcaster instance;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public Future<BlockHeader> submitNewBlockHeader(NewBlockHeaderTask task) {
        return executor.submit(task);
    }
}
