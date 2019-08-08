package io.taucoin.android.wallet.module.model;

import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface IMiningModel {
    /** Get mining information */
    void getMiningInfo(LogicObserver<BlockInfo> observer);
    /** update or save mining state */
    void updateMiningState(String miningState, LogicObserver<KeyValue> observer);
//    /** update or save sync state */
//    void updateSyncState(String syncState, LogicObserver<Boolean> observer);
    /** update or save current synchronized block */
    void updateSynchronizedBlockNum(int blockSynchronized, LogicObserver<Boolean> observer);
    /** handle synchronized block */
    void handleSynchronizedBlock(BlockEventData block, boolean isConnect, LogicObserver<MessageEvent.EventCode> logicObserver);
    /** update or save current block height */
    void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer);
    /** update blocks downloaded data*/
    void updateBlocksDownloaded(long blockDownloaded, LogicObserver<Boolean> observer);

    void getMinerHistory(TxObserver<MinerListBean> observer);
}