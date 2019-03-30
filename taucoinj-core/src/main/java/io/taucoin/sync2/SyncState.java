package io.taucoin.sync2;

/**
 * @author Mikhail Kalinin
 * @since 13.08.2015
 */
public interface SyncState {

    boolean is(SyncStateEnum name);

    void doOnTransition();

    void doMaintain();
}
