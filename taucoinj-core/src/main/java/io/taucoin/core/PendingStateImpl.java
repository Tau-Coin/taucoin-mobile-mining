package io.taucoin.core;

import io.taucoin.db.BlockStore;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import io.taucoin.manager.WorldManager;
import io.taucoin.util.FastByteComparisons;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import javax.annotation.Resource;

import javax.inject.Singleton;
import javax.inject.Inject;

import java.math.BigInteger;
import java.util.*;

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

    private static final Logger logger = LoggerFactory.getLogger("state");

    private TaucoinListener listener;
    private Repository repository;
    private BlockStore blockStore;
    private Blockchain blockchain;
    private boolean isSyncdone = false;
    @Resource
    private final PriorityQueue<MemoryPoolEntry> wireTransactions = new PriorityQueue<MemoryPoolEntry>(1,new MemoryPoolPolicy());
    // To filter out the transactions we have already processed
    // transactions could be sent by peers even if they were already included into blocks
    private final Map<ByteArrayWrapper, Object> receivedTxs = new LRUMap<>(500000);

    private final Map<String, BigInteger> expendList = new HashMap<String, BigInteger>(500000);

    @Resource
    private final List<Transaction> pendingStateTransactions = new ArrayList<>();

    private Repository pendingState;

    private Block best = null;

    private TaucoinListener ProcessBlockListener = new TaucoinListenerAdapter() {
        @Override
        public void onBlockConnected(final Block block) {
            EventDispatchThread.invokeLater(new Runnable() {
                @Override
                public void run() {
                    processBest(block);
                }
            });
        }
    };

    public PendingStateImpl() {
    }

    @Inject
    public PendingStateImpl(TaucoinListener listener, Repository repository,BlockStore blockStore) {
        this.listener = listener;
        ((CompositeTaucoinListener)this.listener).addListener(ProcessBlockListener);
        this.repository = repository;
        this.blockStore = blockStore;
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

    /**
     * Return Transactions in mempool
     * these transactions are valid at best block chain
     * @return
     */
    public List<Transaction> getWireTransactions() {
        List<Transaction> list = new ArrayList<Transaction>();
        for(MemoryPoolEntry entry : wireTransactions){
            list.add(entry.tx);
        }
        return list;
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
            for(Transaction tx : newTxs) {
                MemoryPoolEntry entry = MemoryPoolEntry.with(tx);
                wireTransactions.offer(entry);
            }
        }

        /**
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

        logger.info("Wire transaction list added: {} new, {} valid of received {}, #of known txs: {}", unknownTx, newTxs.size(), transactions.size(), receivedTxs.size());
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
        long expireTime = ByteUtil.byteArrayToLong(tx.getExpireTime());
        long unlockTime = blockchain.getBestBlock().getNumber() - expireTime;
        Block benchBlock = null;
        if(unlockTime >= 0) {
            benchBlock = blockStore.getChainBlockByNumber(unlockTime);
        }else{
            /**
             * this behavior is dangerous , whether node should prevent this into wire transaction pool.
             */
            logger.warn("dangerous behavior expire " +
                            "time too long tx:{} expire time:{}",
                    ByteUtil.toHexString(tx.getHash()),
                    ByteUtil.byteArrayToLong(tx.getExpireTime()));
        }

        if(benchBlock != null && !tx.checkTime(benchBlock)) {
            if (logger.isWarnEnabled())
                logger.warn("Invalid transaction in time");
            tx.TRANSACTION_STATUS = "Invalid transaction in time";
			return false;
        }
        
        TransactionExecutor executor = new TransactionExecutor(tx, getRepository(),blockchain);

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

        /**
         *update precessed transaction memory pool
         *if transactions survived in this container that means
         *it has been added into memory pool (wire or pending )
         *node needn't to add it again.
         */
        synchronized (receivedTxs){
            if (!receivedTxs.containsKey(hash)) {
                receivedTxs.put(hash, null);
                return true;
            } else {
                tx.TRANSACTION_STATUS = "have processed transaction,can't be accepted";
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
            boolean retval = isValid(tx);
            if(retval){
                wireTransactions.offer(new MemoryPoolEntry(tx));
                pendingStateTransactions.remove(tx);
            }
            return retval;
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
            for (MemoryPoolEntry entry : wireTransactions) {
                long expireTime = ByteUtil.byteArrayToLong(entry.tx.getExpireTime());
                long unlockTime = blockchain.getBestBlock().getNumber() - expireTime;
                Block benchBlock = null;
                if(unlockTime >= 0) {
                    benchBlock = blockStore.getChainBlockByNumber(unlockTime);
                }else{
                    /**
                     * if user set a large expire time , this behavior may be harmful to net
                     * in the future, more actions will be get to protect net from hack.
                     */
                    logger.warn("dangerous behavior expire " +
                            "time too long tx:{} expire time:{}",
                            ByteUtil.toHexString(entry.tx.getHash()),
                            ByteUtil.byteArrayToLong(entry.tx.getExpireTime()));
                }
                if (benchBlock != null && !entry.tx.checkTime(benchBlock) ) {
                    removeExpendList(entry.tx);
                    outdated.add(entry.tx);
                }
            }

		    if(!outdated.isEmpty()) {
                for (Transaction tr:
                     outdated) {
                    MemoryPoolEntry entry = new MemoryPoolEntry(tr);
                    wireTransactions.remove(entry);
                }
            }
        }
        outdated.clear();

        //clear pending transactions
        synchronized (pendingStateTransactions) {
            for (Transaction tx : pendingStateTransactions) {
                long expireTime = ByteUtil.byteArrayToLong(tx.getExpireTime());
                long unlockTime = blockchain.getBestBlock().getNumber() - expireTime;
                Block benchBlock = null;
                if(unlockTime >= 0) {
                    benchBlock = blockStore.getChainBlockByNumber(unlockTime);
                }else{
                    logger.warn("dangerous behavior expire " +
                                    "time too long tx:{} expire time:{}",
                            ByteUtil.toHexString(tx.getHash()),
                            ByteUtil.byteArrayToLong(tx.getExpireTime()));
                }
                if (benchBlock != null && !tx.checkTime(benchBlock)) {
                    removeExpendList(tx);
                    outdated.add(tx);
                }
            }
		    if(!outdated.isEmpty())
                pendingStateTransactions.removeAll(outdated);
        }
    }

    private void clearWire(List<Transaction> txs) {
        synchronized (wireTransactions) {
            for (Transaction tx : txs) {
                MemoryPoolEntry entry = new MemoryPoolEntry(tx);
                if (wireTransactions.contains(entry)){
                    wireTransactions.remove(entry);
                }

                removeExpendList(tx);
            }
        }
    }

    private void clearPendingState(List<Transaction> txs) {
        synchronized (pendingStateTransactions) {
            for (Transaction tx : txs){
                if (pendingStateTransactions.contains(tx)){
                    pendingStateTransactions.remove(tx);
                }
            }
            /**
             * because a new block has been insert into block chain
             * a smart node should move some unsure transactions into
             * wire transaction because it is valid time now.
             * state changes from pending to wire
             */
            synchronized (wireTransactions){
                for(Transaction tx: pendingStateTransactions){
                    if(isValid(tx)){
                        MemoryPoolEntry entry = new MemoryPoolEntry(tx);
                        wireTransactions.offer(entry);
                        pendingStateTransactions.remove(tx);
                        logger.info("transaction: {} change from invalid to valid",
                                ByteUtil.toHexString(entry.tx.getHash()));
                    }
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

    @Override
    public int size() {
        return wireTransactions.size() + pendingStateTransactions.size();
    }
}
