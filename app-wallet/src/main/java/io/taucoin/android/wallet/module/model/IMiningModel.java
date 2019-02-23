package io.taucoin.android.wallet.module.model;

import java.util.List;

import io.taucoin.android.interop.Transaction;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
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
    /** update my mining block */
    void updateMyMiningBlock(List<BlockEventData> blocks, LogicObserver<Boolean> logicObserver);
    /** handle synchronized block */
    void handleSynchronizedBlock(BlockEventData block, LogicObserver<MessageEvent.EventCode> logicObserver);
    /** handle send transaction return data and updateTransactionHistory*/
    void updateTransactionHistory(Transaction transaction);
}