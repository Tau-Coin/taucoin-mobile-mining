package io.taucoin.core;

import java.util.List;
import java.util.Set;

/**
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
public interface PendingState extends io.taucoin.facade.PendingState{

    /**
     * Initialized pending state <br>
     * Must be called when {@link Repository} has been initialized
     */
    void init();

    /**
     * Adds transactions received from the net to the list of wire transactions <br>
     * Don't have an impact on pending state
     *
     * @param transactions txs received from the net
     */
    List<Transaction> addWireTransactions(Set<Transaction> transactions);

    /**
     * It should be called on each block imported as <b>BEST</b> <br>
     * Does several things:
     * <ul>
     *     <li>removes block's txs from pending state and wire lists</li>
     *     <li>removes outdated wire txs</li>
     *     <li>updates pending state</li>
     * </ul>
     *
     * @param block block imported into blockchain as a <b>BEST</b> one
     */
    void processBest(Block block);

    void setBlockchain(Blockchain blockchain);

    /**
     * Fires inner logic related to main sync done event
     */
    void onSyncDone();

    /**
     * Return pending state transactions amount.
     */
    int size();
}
