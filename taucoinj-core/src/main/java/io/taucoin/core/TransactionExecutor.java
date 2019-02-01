package io.taucoin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.List;

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
    private static final Logger stateLogger = LoggerFactory.getLogger("state");

    private Transaction tx;
    private Repository track;
    private byte[] coinbase;

    private boolean readyToExecute = false;

    long basicTxAmount = 0;
    long basicTxFee = 0;

    boolean localCall = false;

    //constructor
    public TransactionExecutor(Transaction tx, Repository track) {
        this.tx= tx;
        this.track= track;
    }

    //setLocal
    public TransactionExecutor setLocalCall(boolean localCall) {
        this.localCall = localCall;
	    return this;
    }

    /**
     * Do all the basic validation, if the executor
     * will be ready to run the transaction at the end
     * set readyToExecute = true
     */
    public void init() {

        if (localCall) {
            readyToExecute = true;
            return;
        }

		// Check In Transaction Amount
        basicTxAmount = toBI(tx.getAmount()).longValue();

        // Check In Transaction Fee
        basicTxFee = toBI(tx.transactionCost()).longValue();
        if (basicTxFee < 0 ) {
            if (logger.isWarnEnabled())
                logger.warn("Not enough gas for transaction execution: Require: {} Got: {}", basicTxFee);
            // TODO: save reason for failure
            return;
        }

        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        BigInteger senderBalance = track.getBalance(tx.getSender());

        if (!isCovers(senderBalance, totalCost)) {

            if (logger.isWarnEnabled())
                logger.warn("No enough balance: Require: {}, Sender's balance: {}", totalCost, senderBalance);

            // TODO: save reason for failure
            return;
        }

        readyToExecute = true;
    }

    /**
     * Do the executation
     * 1. add balance to received address 
     * 2. add transaction fee to actually miner 
     */
    public void executeFinal() {
        if (!readyToExecute) return;

		// Sender subtract balance
        BigInteger totalCost = toBI(tx.getAmount()).add(toBI(tx.transactionCost()));
        logger.info("in executation sender is "+Hex.toHexString(tx.getSender()));
        track.addBalance(tx.getSender(), totalCost.negate());

		// Receiver add balance
        track.addBalance(tx.getReceiveAddress(), toBI(tx.getAmount()));

        // Transfer fees to miner
        track.addBalance(coinbase, toBI(tx.transactionCost()));

        // Increase forge power.
        track.increaseforgePower(tx.getSender());

        logger.info("Pay fees to miner: [{}], feesEarned: [{}]", Hex.toHexString(coinbase), basicTxFee);
    }

	/**
	 * Set Miner Address
	 */
	public void setCoinbase(byte [] address){
        this.coinbase= address;
    }
}
