package io.taucoin.android.wallet.module.model;

import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface IMiningModel {
    /** Get mining information */
    void getMiningInfo(LogicObserver<KeyValue> observer);
    /** update or save mining state */
    void updateMiningState(LogicObserver<Boolean> observer);
    /** update or save current block height */
    void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer);
    /** update or save current synchronized block */
    void updateBlockSynchronized(int blockSynchronized, LogicObserver<Boolean> observer);
}