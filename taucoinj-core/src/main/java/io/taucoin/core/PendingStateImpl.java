package io.taucoin.core;

import io.taucoin.config.Constants;
import io.taucoin.db.BlockStore;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import io.taucoin.util.ByteUtil;
import io.taucoin.db.ByteArrayWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.inject.Inject;

import org.apache.commons.collections4.map.LRUMap;

import static io.taucoin.util.BIUtil.*;


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
    private static final int MaxExpireTime = 144;
    private static final int MAXTNO= 50;
    private boolean isSyncdone = false;
    @Resource
    private final PriorityQueue<MemoryPoolEntry> wireTransactions = new PriorityQueue<MemoryPoolEntry>(1,new MemoryPoolPolicy());

    private final Map<String, BigInteger> expendList = new LRUMap<String, BigInteger>(50000);

    private final Map<String, HashSet<Long>> particleTx = new HashMap<>(50);

    private Repository pendingState;

    private Block best = null;

    private TaucoinListener ProcessBlockListener = new TaucoinListenerAdapter() {
        @Override
        public void onBlockConnected(final Block block) {
            processBest(block);
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

        synchronized (wireTransactions) {
            for(MemoryPoolEntry entry : wireTransactions){
                list.add(entry.tx);
            }
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

        logger.info("From network coming TXs: {} " + transactions.size());

        for (Transaction tx : transactions) {
            if (addNewTxIfNotExist(tx)) {
                if (isValid(tx)) {
                    newTxs.add(tx);
                    unknownTx++;
                }
                if(unknownTx >= MAXTNO){
                    break;
                }
            }
        }

        // tight synchronization here since a lot of duplicate transactions can arrive from many peers
        // and isValid(tx) call is very expensive
        synchronized (wireTransactions) {
            for(Transaction tx : newTxs) {
                MemoryPoolEntry entry = MemoryPoolEntry.with(tx);
                wireTransactions.offer(entry);
            }
        }

        logger.info("Wire transaction list added: {} new, {} valid of received {}", unknownTx, newTxs.size(), transactions.size());
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

        // Verification of transaction time 
        long lockTimeEND = System.currentTimeMillis() / 1000+ Constants.MAX_TIMEDRIFT;
        long txTime = ByteUtil.byteArrayToLong(tx.getTime());
        if(txTime > lockTimeEND){
            logger.error("Invalid transaction time: {}, contrast with system time: {}", txTime, System.currentTimeMillis() / 1000);
		    return false;		  
        }

        long expireTime = ByteUtil.byteArrayToLong(tx.getExpireTime());

        if (expireTime > MaxExpireTime) {
            tx.TRANSACTION_STATUS = "this transaction expire time is invalid";
            return false;
        }

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

        TransactionExecutor executor = new TransactionExecutor(tx, getRepository(),blockchain,listener);

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
                logger.info("Transaction into pool, no enough balance");
                tx.TRANSACTION_STATUS = "sorry,No enough balance";
                return false;
            }
        }

        synchronized (particleTx) {
            String senderTmp = ByteUtil.toHexString(tx.getSender());
            long txTime = ByteUtil.byteArrayToLong(tx.getTime());
            if (!particleTx.containsKey(senderTmp)) {
                HashSet<Long> hashSet = new HashSet<>();
                particleTx.put(senderTmp,hashSet);
            } else {
                HashSet<Long> hashSet = particleTx.get(senderTmp);
                if (hashSet.contains(txTime)) {
                    logger.info("Transaction into pool, already have txTime");
                    return false;
                } else {
                    hashSet.add(txTime);
                }
            }
        }

        // wire transactionś job 
        synchronized (wireTransactions) {
            MemoryPoolEntry entry = new MemoryPoolEntry(tx);
            if (wireTransactions.contains(entry)){
                logger.info("Transaction into pool, already have txHash");
                return false;
            }
        }

		return true;
    }

    @Override
    public void processBest(Block block) {

        clearWireTransactions(block.getTransactionsList());

        clearOutdatedTransactions();

        clearParticleTx(block.getTransactionsList());
    }

    // Remove wire transactions in block
    private void clearWireTransactions(List<Transaction> txs) {
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

    //Block Number -> Time
    private void clearOutdatedTransactions() {

        //clear wired transactions
        synchronized (wireTransactions) {
            Iterator<MemoryPoolEntry> iterator = wireTransactions.iterator();
            while(iterator.hasNext()){
                MemoryPoolEntry entry = iterator.next();
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
                    iterator.remove();
                }
            }

        }
    }

    private void clearParticleTx(List<Transaction> txs) {
        synchronized (particleTx) {
            for (Transaction tx : txs) {
                String senderTmp = ByteUtil.toHexString(tx.getSender());
                long txTime = ByteUtil.byteArrayToLong(tx.getTime());
                if (particleTx.containsKey(senderTmp)) {
                    HashSet<Long> hashSet = particleTx.get(senderTmp);
                    if (hashSet.contains(txTime)) {
                        hashSet.remove(txTime);
                    }
                }
            }
        }
    }

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

    @Override
    public void onSyncDone(){
        isSyncdone = true;
    }

    @Override
    public int size() {

        synchronized (wireTransactions) {
            return wireTransactions.size();
        }

    }
}
