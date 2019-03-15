package io.taucoin.core;

import io.taucoin.listener.TaucoinListener;
import io.taucoin.util.FastByteComparisons;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Resource;

import javax.inject.Singleton;
import javax.inject.Inject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeSet;

import static java.math.BigInteger.ZERO;
import org.apache.commons.collections4.map.LRUMap;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.BIUtil.toBI;
import static io.taucoin.util.BIUtil.*;
import io.taucoin.db.ByteArrayWrapper;

/**
 * Keeps logic providing pending state management
 *
 * @author Mikhail Kalinin
 * @since 28.09.2015
 */
@Singleton
public class PendingStateImpl implements PendingState {

    /*
    public static class TransactionSortedSet extends TreeSet<Transaction> {

        public TransactionSortedSet() {

            super(public int compareFee(Transaction tx1, Transaction tx2){
                      return FastByteComparisons.compareTo(tx1.getFee(), 0, 2, tx2.getFee(), 0, 2);
	            }
			);
        }
    }
    */
    private static final Logger logger = LoggerFactory.getLogger("state");

    private TaucoinListener listener;
    private Repository repository;
    private Blockchain blockchain;
    private boolean isSyncdone = false;
    @Resource
    private final List<Transaction> wireTransactions = new ArrayList<>();

    // To filter out the transactions we have already processed
    // transactions could be sent by peers even if they were already included into blocks
    private final Map<ByteArrayWrapper, Object> redceivedTxs = new LRUMap<>(500000);

    private final Map<String, BigInteger> expendList = new HashMap<String, BigInteger>(500000);

    @Resource
    private final List<Transaction> pendingStateTransactions = new ArrayList<>();

    private Repository pendingState;

    private Block best = null;

    public PendingStateImpl() {
    }

    @Inject
    public PendingStateImpl(TaucoinListener listener, Repository repository) {
        this.listener = listener;
        this.repository = repository;
    }

    @Override
    public void init() {
        this.pendingState = repository.startTracking();
    }

    public Repository getRepository() {
        if (this.pendingState == null) {
            init();
        }
        return pendingState;
    }

    // Return Transaction Received From Network
    public List<Transaction> getWireTransactions() {
        return wireTransactions;
    }

    public Block getBestBlock() {
        if (best == null) {
            best = blockchain.getBestBlock();
        }
        return best;
    }

    @Override
    public List<Transaction> addWireTransactions(Set<Transaction> transactions) {

        final List<Transaction> newTxs = new ArrayList<>();
        int unknownTx = 0;

        if (transactions.isEmpty()) return newTxs;

        for (Transaction tx : transactions) {
            logger.info("from network coming TX: {}" + tx.getHash());
            if (addNewTxIfNotExist(tx)) {
                unknownTx++;
                if (isValid(tx)) {
                    newTxs.add(tx);
                } else {
                    logger.info("Non valid TX: {} " + tx.getHash());
                }
            }
        }

        // tight synchronization here since a lot of duplicate transactions can arrive from many peers
        // and isValid(tx) call is very expensive
        synchronized (this) {
            wireTransactions.addAll(newTxs);
        }

        /*
        if (!newTxs.isEmpty()) {
            EventDispatchThread.invokeLater(new Runnable() {
                @Override
                public void run() {
                    listener.onPendingTransactionsReceived(newTxs);
                    listener.onPendingStateChanged(PendingStateImpl.this);
                }
            });
        }
*/

        logger.info("Wire transaction list added: {} new, {} valid of received {}, #of known txs: {}", unknownTx, newTxs.size(), transactions.size(), redceivedTxs.size());
        return newTxs;
    }

    /*
     * Validation Of Transaction.
     * Nonce in ETH, Time In TAU.
     * 1. Transaction data structure
     * 2. Check in time
     * 3. Signature
     * 4. Balance
    */
    private boolean isValid(Transaction tx) {

        if(!tx.verify()) {
            if (logger.isWarnEnabled())
                logger.warn("Invalid transaction in structure");
            tx.TRANSACTION_STATUS = "Invalid transaction in structure";
			return false;
        }

        if(!tx.checkTime()) {
            if (logger.isWarnEnabled())
                logger.warn("Invalid transaction in time");
            tx.TRANSACTION_STATUS = "Invalid transaction in time";
			return false;
        }
        
        TransactionExecutor executor = new TransactionExecutor(tx, getRepository());

        return executor.init();
    }

    private boolean addNewTxIfNotExist(Transaction tx) {
        ByteArrayWrapper hash = new ByteArrayWrapper(tx.getHash());

        //update transaction balance list
        synchronized (expendList) {
            String senderTmp= ByteUtil.toHexString(tx.getSender());
            if (!expendList.containsKey(senderTmp)) {
                expendList.put(senderTmp, tx.getTotoalCost());
            } else {
                expendList.put(senderTmp, expendList.get(senderTmp).add(tx.getTotoalCost()));
            }

            BigInteger senderBalance = getRepository().getBalance(tx.getSender());
            if (!isCovers(senderBalance, expendList.get(senderTmp))) {
                if (logger.isWarnEnabled())
                    logger.warn("No enough balance: Require: {}, Sender's balance: {}", expendList.get(senderTmp), senderBalance);
                tx.TRANSACTION_STATUS = "sorry,No enough balance";
                return false;
            }
        }

        //update transaction memory pool
        synchronized (redceivedTxs) {
            if (!redceivedTxs.containsKey(hash)) {
                redceivedTxs.put(hash, null);
                return true;
            } else {
                tx.TRANSACTION_STATUS = "repeated transaction,can't be accepted";
                return false;
            }
        }
    }

    @Override
    public boolean addPendingTransaction(Transaction tx) {
        if (addNewTxIfNotExist(tx)) {
            synchronized (this) {
                pendingStateTransactions.add(tx);
            }
            return isValid(tx);
        }

        return false;
    }

    @Override
    public List<Transaction> getPendingTransactions() {
        return pendingStateTransactions;
    }

    @Override
    public void processBest(Block block) {

        clearWire(block.getTransactionsList());

        clearPendingState(block.getTransactionsList());

        clearOutdated();

        //updateState(Block block);
    }

    //Block Number -> Time
    private void clearOutdated() {

        List<Transaction> outdated = new ArrayList<>();

        //clear wired transactions
        synchronized (wireTransactions) {
            for (Transaction tx : wireTransactions)
                if (!tx.checkTime()){
                    removeExpendList(tx);
                    outdated.add(tx);
			    }

		    if(!outdated.isEmpty())
                wireTransactions.removeAll(outdated);
        }
        outdated.clear();

        //clear pending transactions
        synchronized (pendingStateTransactions) {
            for (Transaction tx : pendingStateTransactions)
                if (!tx.checkTime()){
                    removeExpendList(tx);
                    outdated.add(tx);
                }

		    if(!outdated.isEmpty())
                pendingStateTransactions.removeAll(outdated);
        }
    }

    private void clearWire(List<Transaction> txs) {
        synchronized (wireTransactions) {
            for (Transaction tx : txs) {
                if (wireTransactions.contains(tx)){
                    wireTransactions.remove(tx);
                }

                removeExpendList(tx);
            }
        }
    }

    private void clearPendingState(List<Transaction> txs) {
        synchronized (wireTransactions) {
            for (Transaction tx : txs){
                if (pendingStateTransactions.contains(tx)){
                    pendingStateTransactions.remove(tx);
                }
            }
        }
    }

    /*
    private void updateState(Block block) {

        for (Transaction tx : block.getTransactionsList()) {
        }
    }
    */

    /*
     * Transaction execution, which can be seen in transactionExecutor.java
     * 1. validation of sender's balance, subtract
    */
    private void removeExpendList(Transaction tx) {
        
        //update transaction balance list
        synchronized (expendList) {
            String senderTmp= ByteUtil.toHexString(tx.getSender());
            if(expendList.containsKey(senderTmp)) {
                expendList.put(senderTmp, expendList.get(senderTmp).add(tx.getTotoalCost().negate()));
            }
        }
    }

    public void setBlockchain(Blockchain blockchain) {
        this.blockchain = blockchain;
    }

    public boolean pendingStateContains(Transaction tx) {
        if (pendingStateTransactions.contains(tx)||wireTransactions.contains(tx))
            return true;
		return false;
    }

    @Override
    public void onSyncDone(){
        isSyncdone = true;
    }

}
