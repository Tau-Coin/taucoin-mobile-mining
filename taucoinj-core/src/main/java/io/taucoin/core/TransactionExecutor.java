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
        if (basicTxFee < 0 ) {
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

        // Transfer fees to miner
        track.addBalance(coinbase, toBI(tx.transactionCost()));

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
