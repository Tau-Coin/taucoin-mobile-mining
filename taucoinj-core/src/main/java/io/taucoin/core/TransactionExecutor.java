package io.taucoin.core;

import io.taucoin.config.Constants;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.util.ByteUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.BIUtil.*;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public class TransactionExecutor {

    private static final Logger logger = LoggerFactory.getLogger("execute");

    private Transaction tx;
    private Repository track;
    private byte[] coinbase;
    private Blockchain blockchain;
    private TaucoinListener listener;

    private TransactionExecuatedOutcome outcome = new TransactionExecuatedOutcome();
    private FeeDistributor feeDistributor = new FeeDistributor();
    private AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor();

    private HashMap<byte[],Long> currentWitness = new HashMap<>();
    private HashMap<byte[],Long> lastWitness = new HashMap<>();

    // indicate that this is witness by self to show mining income asap.
    private boolean isAssociatedByself = false;
    /**
     * this is a temporary strategy.
     * a transaction per second.
     * allow old but low fee transaction have opportunity
     * to be recorded in block.
     */
    private static final int MaxHistoryCount = 144;

    long basicTxAmount = 0;
    long basicTxFee = 0;

    public void setAssociatedByself(boolean forgedByself) {
        isAssociatedByself = forgedByself;
    }

    //constructor
    public TransactionExecutor(Transaction tx, Repository track,Blockchain blockchain,TaucoinListener listener) {
        this.tx= tx;
        this.track= track;
        this.blockchain = blockchain;
        this.listener = listener;
    }

    public TransactionExecutor(Blockchain blockchain, TaucoinListener listener) {
        this.blockchain = blockchain;
        this.listener = listener;
    }

    public void init(Transaction tx, Repository track) {
        this.tx= tx;
        this.track= track;
    }

    /**
     * Do all the basic validation
     */
    public boolean init() {

		// Check In Transaction Amount
        basicTxAmount = toBI(tx.getAmount()).longValue();
        if (basicTxAmount < 0 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction amount [{}] is invalid!", basicTxAmount);
            return false;
        }

        // Check In Transaction Fee
        basicTxFee = toBI(tx.transactionCost()).longValue();
        if (basicTxFee < 1 ) {
            if (logger.isWarnEnabled())
                logger.warn("Transaction fee [{}] is invalid!", basicTxFee);
            tx.TRANSACTION_STATUS = "Not enough fee for transaction";
            return false;
        }

        /**
         * node need to check whether this transaction has been recorded in block.
         * a honest node need to avoid transactions duplicated in block chain.
         */
        AccountState accountState = track.getAccountState(tx.getSender());
        if(accountState == null){
            if(logger.isErrorEnabled())
                logger.error("in valid account ,address is: {}", ByteUtil.toHexString(tx.getSender()));
            return false;
        }
        long tranTime = ByteUtil.byteArrayToLong(tx.getTime());
        Set<Long> txHistory = accountState.getTranHistory().keySet();
        if(!txHistory.isEmpty()) {
            long txTimeCeil = Collections.max(txHistory);
            long txTimeFloor = Collections.min(txHistory);

            /**
             * System should be concurrency high rather than 1 transaction per second.
             */
            if (tranTime <= txTimeCeil) {
                if (accountState.getTranHistory().containsKey(tranTime)) {
                    logger.error("duplicate transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                    return false;
                }

                if (tranTime < txTimeFloor && blockchain.getSize() > MaxHistoryCount) {
                    long freshTime = blockchain.getSize() - MaxHistoryCount;
                    if(freshTime == 1 && tranTime < ByteUtil.byteArrayToLong(CONFIG.getGenesis().getTimestamp())){
                        logger.error("overflow attacking transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                        return false;
                    }

                    if (freshTime > 1 && tranTime < blockchain.getBlockTimeByNumber(freshTime -1)) {
                        logger.error("attacking transaction ,tx is: {}", ByteUtil.toHexString(tx.getHash()));
                        return false;
                    }
                }
            }
        }

        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        BigInteger senderBalance = track.getBalance(tx.getSender());

        if (!isCovers(senderBalance, totalCost)) {

            if (logger.isWarnEnabled())
                logger.warn("No enough balance: require: {}, sender's balance: {}, txid: {}, sender:{}",
                        totalCost, senderBalance, ByteUtil.toHexString(tx.getHash()),
                        ByteUtil.toHexString(tx.getSender()));
            tx.TRANSACTION_STATUS = "No enough balance";
            return false;
        }
        return true;
    }

    /**
     * when process block all txs from block are valid.
     * @return
     */
    public boolean chainInit(){
        return true;
    }

    /**
     * Do the executation
     * 1. add balance to received address 
     * 2. add transaction fee to actually miner 
     */
    public void executeFinal(byte[] blockhash, boolean isTxCompleted) {
        outcome.setBlockHash(blockhash);
        logger.debug("in executation block hash is {}",Hex.toHexString(blockhash));
        outcome.setTxComplete(isTxCompleted);
        logger.debug("in executation isTxCompleted is {}", isTxCompleted);

        // Sender subtract balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        track.addBalance(tx.getSender(), totalCost.negate());

        // Increase forge power.
        track.increaseforgePower(tx.getSender());

        // Receiver add balance
        String receiverHexAddress = Hex.toHexString(tx.getReceiveAddress());
        if (!receiverHexAddress.equals(Constants.BURN_COIN_ADDR)) {
            track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()));
        }

        feeDistributor.setTxFee(ByteUtil.byteArrayToLong(tx.transactionCost()));

        logger.debug("in executation total fee is {}",Hex.toHexString(tx.transactionCost()));
        //lookup sender account state.
        AccountState senderAccountState = track.getAccountState(tx.getSender());
        if (blockchain.getSize() < Constants.FEE_TERMINATE_HEIGHT + 1) {
            if (feeDistributor.distributeFee()) {
                // Transfer fees to forger
                String coinbaseHexAddress = Hex.toHexString(coinbase);
                if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                    track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()));
                }
                HashMap<byte[], Long> currentWintess = new HashMap<>();
                currentWintess.put(coinbase, feeDistributor.getCurrentWitFee());
                logger.debug("in executation current wit {} fee is {}", coinbaseHexAddress,
                        feeDistributor.getCurrentWitFee());
                outcome.setCurrentWintess(currentWintess);

                if (senderAccountState.getWitnessAddress() != null) {
                    // Transfer fees to last witness
                    String witnessHexAddress = Hex.toHexString(senderAccountState.getWitnessAddress());
                    if (!witnessHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(senderAccountState.getWitnessAddress(), toBI(feeDistributor.getLastWitFee()));
                    }
                    HashMap<byte[], Long> lastWintess = new HashMap<>();
                    lastWintess.put(senderAccountState.getWitnessAddress(), feeDistributor.getLastWitFee());
                    logger.debug("in executation last wit {} fee is {}", witnessHexAddress,
                            feeDistributor.getLastWitFee());
                    outcome.setLastWintess(lastWintess);
                }

                int senderAssSize = senderAccountState.getAssociatedAddress().size();
                if (senderAssSize != 0) {
                    // Transfer fees to last associate
                    AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor(
                            senderAssSize,
                            feeDistributor.getLastAssociFee());

                    if (assDistributor.assDistributeFee()) {
                        ArrayList<byte[]> senderAssAddress = senderAccountState.getAssociatedAddress();
                        for (int i = 0; i < senderAssSize; ++i) {
                            String senderHexAssAddress = Hex.toHexString(senderAssAddress.get(i));
                            if (i != senderAssSize - 1) {
                                if (!senderHexAssAddress.equals(Constants.BURN_COIN_ADDR)) {
                                    track.addBalance(senderAssAddress.get(i), toBI(assDistributor.getAverageShare()));
                                }
                                logger.debug("in executation Ass {} fee is {}", senderHexAssAddress,
                                        assDistributor.getAverageShare());
                                outcome.updateSenderAssociated(senderAssAddress.get(i), assDistributor.getAverageShare());
                            } else {
                                if (!senderHexAssAddress.equals(Constants.BURN_COIN_ADDR)) {
                                    track.addBalance(senderAssAddress.get(i), toBI(assDistributor.getLastShare()));
                                }
                                logger.debug("in executation last Ass {} fee is {}", senderHexAssAddress,
                                        assDistributor.getLastShare());
                                outcome.updateSenderAssociated(senderAssAddress.get(i), assDistributor.getLastShare());
                            }
                        }
                    }
                }

                /**
                 * 2 special situation is dealt by distribute associated fee to current forger
                 */
                if (senderAccountState.getWitnessAddress() == null) {
                    // Transfer fees to current witness
                    if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(coinbase, toBI(feeDistributor.getLastWitFee()));
                    }
                    logger.debug("in executation special last wit {} fee is {}", coinbaseHexAddress,
                            feeDistributor.getLastWitFee());
                    outcome.updateCurrentWintessBalance(coinbase, feeDistributor.getLastWitFee());
                }

                if (senderAssSize == 0) {
                    // Transfer fees to current associate
                    if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(coinbase, toBI(feeDistributor.getLastAssociFee()));
                    }
                    logger.debug("in executation special last ass {} fee is {}", coinbaseHexAddress,
                            feeDistributor.getLastAssociFee());
                    outcome.updateCurrentWintessBalance(coinbase, feeDistributor.getLastAssociFee());
                }
                if (isAssociatedByself) {
                    listener.onTransactionExecuated(outcome);
                }
            }
        } else {
            //all fee was distributed to current miner.
            feeDistributor.setDistribute(false);
            if (feeDistributor.distributeFee()) {
                String coinbaseHexAddress = Hex.toHexString(coinbase);
                if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                    track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()));
                }
                HashMap<byte[], Long> currentWintess = new HashMap<>();
                currentWintess.put(coinbase, feeDistributor.getCurrentWitFee());
                logger.debug("in executation current wit {} fee is {}", coinbaseHexAddress,
                        feeDistributor.getCurrentWitFee());
                outcome.setCurrentWintess(currentWintess);

                if (isAssociatedByself) {
                    listener.onTransactionExecuated(outcome);
                }
            }
        }

        if(isTxCompleted) {
            logger.debug("in executation finish =========================");
        }

        logger.debug("Pay fees to miner: [{}], feesEarned: [{}]", Hex.toHexString(coinbase), basicTxFee);

        //AccountState accountState = track.getAccountState(tx.getSender());
        if (blockchain.getSize() > MaxHistoryCount + 1
                && senderAccountState.getTranHistory().size() != 0) {
            // if earliest transaction is beyond expire time
            // it will be removed.
            // long freshTime = blockchain.getSize() - MaxHistoryCount;
            // long bechTime = blockchain.getBlockTimeByNumber(freshTime - 1);
            long bestBlockTime = blockchain.getBlockTimeByNumber(
                    blockchain.getBestBlock().getNumber());
            long bechTime = bestBlockTime - Constants.TRANSACTION_EXPIRE_DURATION;

            if (bechTime > 0) {
                Iterator<Map.Entry<Long,byte[]>> it = senderAccountState.getTranHistory().entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Long, byte[]> entry = it.next();
                    long txTime = entry.getKey();
                    if (txTime < bechTime) {
                        it.remove();
                    } else {
                        break;
                    }
                }
            }
        }

        // Store current tx time
        senderAccountState.getTranHistory().put(ByteUtil.byteArrayToLong(tx.getTime()),
                tx.getHash());
    }

    public void undoTransaction() {
        // add sender balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        track.addBalance(tx.getSender(), totalCost);

        // Subtract receiver balance
        String receiverHexAddress = Hex.toHexString(tx.getReceiveAddress());
        if (!receiverHexAddress.equals(Constants.BURN_COIN_ADDR)) {
            track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()).negate());
        }

        feeDistributor.setTxFee(ByteUtil.byteArrayToLong(tx.transactionCost()));

        AccountState senderAccountState = track.getAccountState(tx.getSender());

        String coinbaseHexAddress = Hex.toHexString(coinbase);

        if (blockchain.getSize() < Constants.FEE_TERMINATE_HEIGHT + 1) {
            if (feeDistributor.distributeFee()) {
                // Transfer fees to forger
                if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                    track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()).negate());
                }
                if (senderAccountState.getWitnessAddress() != null) {
                    // Transfer fees to last witness
                    String witnessHexAddress = Hex.toHexString(senderAccountState.getWitnessAddress());
                    if (!witnessHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(senderAccountState.getWitnessAddress(),
                                toBI(feeDistributor.getLastWitFee()).negate());
                    }
                }

                int size = senderAccountState.getAssociatedAddress().size();
                if (size != 0) {
                    // Transfer fees to last associate
                    AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor(
                            size, feeDistributor.getLastAssociFee());

                    if (assDistributor.assDistributeFee()) {
                        for (int i = 0; i < size; ++i) {
                            String associateHexAddress = Hex.toHexString(senderAccountState.getAssociatedAddress().get(i));
                            if (i != size - 1) {
                                if (!associateHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                                    track.addBalance(senderAccountState.getAssociatedAddress().get(i),
                                            toBI(assDistributor.getAverageShare()).negate());
                                }
                            } else {
                                if (!associateHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                                    track.addBalance(senderAccountState.getAssociatedAddress().get(i),
                                            toBI(assDistributor.getLastShare()).negate());
                                }
                            }
                        }
                    }
                }

                /**
                 * 2 special situation is dealt by distribute associated fee to current forger
                 */
                if (senderAccountState.getWitnessAddress() == null) {
                    // Transfer fees to last witness
                    if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(coinbase,
                                toBI(feeDistributor.getLastWitFee()).negate());
                    }
                }

                if (size == 0) {
                    // Transfer fees to last associate
                    if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                        track.addBalance(coinbase,
                                toBI(feeDistributor.getLastAssociFee()).negate());
                    }
                }
            }
        } else {
            feeDistributor.setDistribute(false);
            if (feeDistributor.distributeFee()) {
                // Transfer fees to forger
                if (!coinbaseHexAddress.equals(Constants.BURN_COIN_ADDR)) {
                    track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()).negate());
                }
            }
        }

        // undo account transaction history
        if (senderAccountState.getTranHistory().keySet().contains( ByteUtil.byteArrayToLong(tx.getTime()) ) ) {
            senderAccountState.getTranHistory().remove( ByteUtil.byteArrayToLong(tx.getTime()) );
        }

        track.reduceForgePower(tx.getSender());
    }

	/**
	 * Set Miner Address
	 */
	public void setCoinbase(byte [] address){
        this.coinbase= address;
    }
}
