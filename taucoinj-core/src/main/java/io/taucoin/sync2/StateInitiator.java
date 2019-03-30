package io.taucoin.sync2;

/**
 * @author Mikhail Kalinin and taucoin core.
 * @since 16.12.2015,modify at 30.4.2019.
 * adjustment to taucoin main net when it in test run.
 */
public interface StateInitiator {

    SyncStateEnum initiate();
}
