package io.taucoin.sync2;

import io.taucoin.core.Genesis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Taucoin Core Developers
 * @since 29.03.2019
 */
@Singleton
public class ChainInfoManager {

    private final static Logger logger = LoggerFactory.getLogger("sync2");

    private long height;
    private byte[] previousBlockHash;
    private byte[] currentBlockHash;
    private BigInteger totalDiff;

    @Inject
    public ChainInfoManager() {
        this.height = 0;
        this.previousBlockHash = null;
        this.currentBlockHash = Genesis.getInstance().getHash();
        this.totalDiff = BigInteger.ZERO;
    }

    public synchronized void update(long height, byte[] previousBlockHash,
            byte[] currentBlockHash, BigInteger totalDiff) {
        this.height = height;
        this.previousBlockHash = previousBlockHash;
        this.currentBlockHash = currentBlockHash;
        this.totalDiff = totalDiff;
    }

    public synchronized long getHeight() {
        return height;
    }

    public synchronized byte[] getPreviousBlockHash() {
        return currentBlockHash;
    }

    public synchronized byte[] getCurrentBlockHash() {
        return currentBlockHash;
    }

    public synchronized BigInteger getTotalDiff() {
        return totalDiff;
    }
}
