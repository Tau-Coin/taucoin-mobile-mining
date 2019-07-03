package io.taucoin.facade;

import io.taucoin.core.*;

import java.util.List;

/**
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
public interface PendingState {

    /**
     * @return pending state repository
     */
    io.taucoin.core.Repository getRepository();

    /**
     * @return currently pending transactions received from the net
     */
    List<Transaction> getWireTransactions();

}
