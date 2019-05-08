package io.taucoin.core;

import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.util.BIUtil.*;
import static io.taucoin.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static io.taucoin.util.ByteUtil.toHexString;

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
    /**
     * this is a temporary strategy.
     * a transaction per second.
     * allow old but low fee transaction have opportunity
     * to be recorded in block.
     */
    private static final int MaxHistoryCount = 144;

    long basicTxAmount = 0;
    long basicTxFee = 0;

    //constructor
    public TransactionExecutor(Transaction tx, Repository track,Blockchain blockchain) {
        this.tx= tx;
        this.track= track;
        this.blockchain = blockchain;
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
                    logger.error("attacking transaction ,tx is: {}",ByteUtil.toHexString(tx.getHash()));
                    return false;
                }
            }
        }

        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        BigInteger senderBalance = track.getBalance(tx.getSender());

        if (!isCovers(senderBalance, totalCost)) {

            if (logger.isWarnEnabled())
                logger.warn("No enough balance: Require: {}, Sender's balance: {}", totalCost, senderBalance);
            tx.TRANSACTION_STATUS = "No enough balance";
            return false;
        }
        return true;
    }

    /**
     * Do the executation
     * 1. add balance to received address 
     * 2. add transaction fee to actually miner 
     */
    public void executeFinal() {
		// Sender subtract balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        logger.info("in executation sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost.negate());

		// Receiver add balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()));

        FeeDistributor feeDistributor = new FeeDistributor(ByteUtil.byteArrayToLong(tx.transactionCost()));

        if (feeDistributor.distributeFee()) {
            // Transfer fees to forger
//            logger.error("1 current witness share {}",feeDistributor.getCurrentWitFee());
            track.addBalance(coinbase, toBI(feeDistributor.getCurrentWitFee()));
            // Transfer fees to receiver
            //track.addBalance(tx.getReceiveAddress(), toBI(feeDistributor.getReceiveFee()));
            if (track.getAccountState(tx.getSender()).getWitnessAddress() != null) {
//                logger.error("2 last witness {} share {}",
//                        Hex.toHexString(track.getAccountState(tx.getSender()).getWitnessAddress()),
//                        feeDistributor.getLastWitFee());
                // Transfer fees to last witness
                track.addBalance(track.getAccountState(tx.getSender()).getWitnessAddress(),
                        toBI(feeDistributor.getLastWitFee()));
            }

            if (track.getAccountState(tx.getSender()).getAssociatedAddress().size() != 0) {
//                logger.error("3 associated size {} share {}",
//                        track.getAccountState(tx.getSender()).getAssociatedAddress().size(),
//                        feeDistributor.getLastAssociFee());
                // Transfer fees to last associate
                AssociatedFeeDistributor assDistributor = new AssociatedFeeDistributor(
                        track.getAccountState(tx.getSender()).getAssociatedAddress().size(),
                        feeDistributor.getLastAssociFee());

                if (assDistributor.assDistributeFee()) {
                    for (int i = 0; i < track.getAccountState(tx.getSender()).getAssociatedAddress().size(); ++i) {
                        if(i != track.getAccountState(tx.getSender()).getAssociatedAddress().size() -1) {
//                            logger.info("transaction execuated associated address size is====> {}",
//                                    track.getAccountState(tx.getSender()).getAssociatedAddress().size());
//                            logger.info("associated address is {} index is {}",Hex.toHexString(
//                                    track.getAccountState(tx.getSender()).getAssociatedAddress().get(i)),i);
                            track.addBalance(track.getAccountState(tx.getSender()).getAssociatedAddress().get(i),
                                    toBI(assDistributor.getAverageShare()));
                        } else {
                            track.addBalance(track.getAccountState(tx.getSender()).getAssociatedAddress().get(i),
                                    toBI(assDistributor.getLastShare()));
                        }
                    }
                }
            }

            /**
             * 2 special situation is dealt by distribute associated fee to current forger
             */
            if (track.getAccountState(tx.getSender()).getWitnessAddress() == null) {
//                logger.error("4 coinbase {} share {}",
//                        Hex.toHexString(coinbase),
//                        feeDistributor.getLastWitFee());
                // Transfer fees to last witness
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastWitFee()));
            }

            if (track.getAccountState(tx.getSender()).getAssociatedAddress().size() == 0) {
//                logger.error("5 coinbase {} share {}",
//                        Hex.toHexString(coinbase),
//                        feeDistributor.getLastAssociFee());
                // Transfer fees to last associate
                track.addBalance(coinbase,
                        toBI(feeDistributor.getLastAssociFee()));
            }
        }

        // Increase forge power.
        logger.info("before increase sender address is {} power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));
        track.increaseforgePower(tx.getSender());
        logger.info("after increase sender address is {} power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));

        logger.info("Pay fees to miner: [{}], feesEarned: [{}]", Hex.toHexString(coinbase), basicTxFee);


        AccountState accountState = track.getAccountState(tx.getSender());
        if(blockchain.getSize() > MaxHistoryCount){
            long txTime = Collections.min(accountState.getTranHistory().keySet());
            // if earliest transaction is beyond expire time
            // it will be removed.
            long freshTime = blockchain.getSize() - MaxHistoryCount;
            if (txTime < ByteUtil.byteArrayToLong(blockchain.getBlockByNumber(freshTime).getTimestamp())) {
                accountState.getTranHistory().remove(txTime);
            } else {
                long txTimeTemp = ByteUtil.byteArrayToLong(tx.getTime());
                accountState.getTranHistory().put(txTimeTemp, tx.getHash());
            }
        }else{
            long txTime = ByteUtil.byteArrayToLong(tx.getTime());
            accountState.getTranHistory().put(txTime,tx.getHash());
        }

    }

    public void undoTransaction() {
        // add sender balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        logger.info("Tx sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost);

        // Subtract receiver balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()).negate());

        // Transfer fees to miner
        track.addBalance(coinbase, toBI(tx.transactionCost()).negate());

        // undo account transaction history
        AccountState accountState = track.getAccountState(tx.getSender());
        if ( accountState.getTranHistory().keySet().contains( ByteUtil.byteArrayToLong(tx.getTime()) ) ) {
            accountState.getTranHistory().remove( ByteUtil.byteArrayToLong(tx.getTime()) );
        }

        // Increase forge power.
        logger.info("before undo sender address is {} forge power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));
        track.reduceForgePower(tx.getSender());
        logger.info("after undo sender address is {} forge power is {}",Hex.toHexString(tx.getSender()),track.getforgePower(tx.getSender()));
    }

	/**
	 * Set Miner Address
	 */
	public void setCoinbase(byte [] address){
        this.coinbase= address;
    }
}
