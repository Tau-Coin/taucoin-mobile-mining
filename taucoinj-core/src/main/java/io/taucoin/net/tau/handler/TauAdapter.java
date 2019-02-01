package io.taucoin.net.tau.handler;

import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.Transaction;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.sync.SyncStateName;
import io.taucoin.sync.SyncStatistics;

import java.util.List;

import static io.taucoin.net.tau.TauVersion.*;

/**
 * It's quite annoying to always check {@code if (eth != null)} before accessing it. <br>
 *
 * This adapter helps to avoid such checks. It provides meaningful answers to Tau client
 * assuming that Tau hasn't been initialized yet. <br>
 *
 * Check {@link io.taucoin.net.server.Channel} for example.
 *
 * @author Mikhail Kalinin
 * @since 20.08.2015
 */
public class TauAdapter implements Tau {

    private final SyncStatistics syncStats = new SyncStatistics();

    @Override
    public boolean hasStatusPassed() {
        return false;
    }

    @Override
    public boolean hasStatusSucceeded() {
        return false;
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void logSyncStats() {
    }

    @Override
    public void changeState(SyncStateName newState) {
    }

    @Override
    public boolean hasBlocksLack() {
        return false;
    }

    @Override
    public boolean isHashRetrievingDone() {
        return false;
    }

    @Override
    public boolean isHashRetrieving() {
        return false;
    }

    @Override
    public boolean isIdle() {
        return true;
    }

    @Override
    public void setMaxHashesAsk(int maxHashesAsk) {
    }

    @Override
    public int getMaxHashesAsk() {
        return 0;
    }

    @Override
    public void setLastHashToAsk(byte[] lastHashToAsk) {
    }

    @Override
    public byte[] getLastHashToAsk() {
        return new byte[0];
    }

    @Override
    public byte[] getBestKnownHash() {
        return new byte[0];
    }

    @Override
    public SyncStatistics getStats() {
        return syncStats;
    }

    @Override
    public void disableTransactions() {
    }

    @Override
    public void enableTransactions() {
    }

    @Override
    public void sendTransaction(List<Transaction> tx) {
    }

    @Override
    public void sendNewBlock(Block newBlock) {
    }

    @Override
    public void sendNewBlockHeader(BlockHeader header) {
    }

    @Override
    public TauVersion getVersion() {
        return fromCode(UPPER);
    }

    @Override
    public void onSyncDone() {
    }
}
