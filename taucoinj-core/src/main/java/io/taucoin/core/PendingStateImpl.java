package io.taucoin.core;

//import org.ethereum.listener.EthereumListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.math.BigInteger.ZERO;
import static org.ethereum.config.SystemProperties.CONFIG;
import static org.ethereum.util.BIUtil.toBI;

/**
 * Keeps logic providing pending state management
 *
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
@Singleton
public class PendingStateImpl implements PendingState {

    private static final Logger logger = LoggerFactory.getLogger("state");

//    private EthereumListener listener;
    private Repository repository;
    private Blockchain blockchain;

    @Resource
    private final List<Transaction> wireTransactions = new ArrayList<>();

    @Resource
    private final List<Transaction> pendingStateTransactions = new ArrayList<>();

    private Repository pendingState;

    public PendingStateImpl() {
    }

	//constructor
    //public PendingStateImpl(EthereumListener listener, Repository repository) {
    @Inject
    public PendingStateImpl(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void init() {
        this.pendingState = repository.startTracking();
    }

    public Repository getRepository() {
        return pendingState;
    }

	// Return Transaction Received From Network
    public List<Transaction> getWireTransactions() {

        List<Transaction> txs = new ArrayList<>();

        for (Transaction tx : wireTransactions) {
            txs.add(tx);
        }

        return txs;
    }

    @Override
    public void addWireTransactions(Set<Transaction> transactions) {

        if (transactions.isEmpty()) return;

        logger.info("Wire transaction list added: size: [{}]", transactions.size());

        //listener.onPendingTransactionsReceived(transactions);

        for (Transaction tx : transactions) {
            if (isValid(tx)) wireTransactions.add(tx);
        }
    }

	/*
	 * Validation Of Transaction.
	 * Nonce in ETH, Time In TAU.
	*/
    private boolean isValid(Transaction tx) {

        BigInteger txTime = toBI(tx.getTime());

		// Current Time
        BigInteger nowTime = toBI(tx.getTime());

		// nowTime- txTime< 12 hours
		
        return true;
    }

    @Override
    public void addPendingTransaction(Transaction tx) {
        pendingStateTransactions.add(tx);
        executeTx(tx);
    }

    public List<Transaction> getPendingTransactions() {
        return pendingStateTransactions;
    }

    @Override
    public void processBest(Block block) {

        clearWire(block.getTransactionsList());

        //clearOutdated(block.getNumber());

        clearPendingState(block.getTransactionsList());

        updateState();
    }

	//Block Number -> Time
	/*
    private void clearOutdated(final long blockNumber) {
        List<Transaction> outdated = new ArrayList<>();

        synchronized (wireTransactions) {
            for (Transaction tx : wireTransactions)
                if (blockNumber - tx.getBlockNumber() > CONFIG.txOutdatedThreshold())
                    outdated.add(tx);
        }

        if (outdated.isEmpty()) return;

        if (logger.isInfoEnabled())
            for (Transaction tx : outdated)
                logger.info(
                        "Clear outdated wire transaction, block.number: [{}] hash: [{}]",
                        tx.getBlockNumber(),
                        Hex.toHexString(tx.getHash())
                );

        wireTransactions.removeAll(outdated);
    }
	*/
    private void clearWire(List<Transaction> txs) {
        for (Transaction tx : txs) {
            if (logger.isInfoEnabled() && wireTransactions.contains(tx))
                logger.info("Clear wire transaction, hash: [{}]", Hex.toHexString(tx.getHash()));

            wireTransactions.remove(tx);
        }
    }

    private void clearPendingState(List<Transaction> txs) {
        if (logger.isInfoEnabled()) {
            for (Transaction tx : txs)
                if (pendingStateTransactions.contains(tx))
                    logger.info("Clear pending state transaction, hash: [{}]", Hex.toHexString(tx.getHash()));
        }

        pendingStateTransactions.removeAll(txs);
    }

    private void updateState() {

        pendingState = repository.startTracking();

        synchronized (pendingStateTransactions) {
            for (Transaction tx : pendingStateTransactions) executeTx(tx);
        }
    }

	/*
	 * Transaction execution, which can be seen in transactionExecutor.java
	 * 1. validation of sender's balance
	 * 2. update of receiver and miner's balance
	*/
    private void executeTx(Transaction tx) {

        logger.info("Apply pending state tx: {}", Hex.toHexString(tx.getHash()));

        //Block best = blockchain.getBestBlock();

        /* ********* Need getCoinbase method, block wrapper's address *******
		 * TransactionExecutor executor = new TransactionExecutor(tx, best.getCoinbase(), pendingState);
         * executor.init();
         * executor.execute();
		*/
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

}
