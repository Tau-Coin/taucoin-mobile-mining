package io.taucoin.http.submit;

import io.taucoin.core.Transaction;
import io.taucoin.http.RequestManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Roman Mandeleil
 * @since 23.05.2014
 */
public class TransactionTask implements Callable<List<Transaction>> {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private final List<Transaction> tx;
    private final RequestManager requestManager;

    public TransactionTask(Transaction tx, RequestManager requestManager) {
        this(Collections.singletonList(tx), requestManager);
    }

    public TransactionTask(List<Transaction> tx, RequestManager requestManager) {
        this.tx = tx;
        this.requestManager = requestManager;
    }

    @Override
    public List<Transaction> call() throws Exception {

        try {
            logger.info("submit tx: {}", tx.toString());
            requestManager.submitNewTransaction(tx);
            return tx;

        } catch (Throwable th) {
            logger.warn("Exception caught: {}", th);
        }
        return null;
    }
}
