package io.taucoin.sync2;

import static io.taucoin.sync2.SyncStateEnum.*;

public class ChainInfoRetrievingState extends AbstractSyncState {

    public ChainInfoRetrievingState() {
        super(CHAININFO_RETRIEVING);
    }

    @Override
    public void doOnTransition() {
        super.doOnTransition();
    }

    @Override
    public void doMaintain() {
        super.doMaintain();

        if (requestManager.isChainInfoRetrievingDone() &&
                syncManager.blockchain.getTotalDifficulty()
                        .compareTo(syncManager.chainInfoManager.getTotalDiff()) >= 0) {
            syncManager.changeState(IDLE);
            return;
        }

        if(syncManager.requestManager.isChainInfoRetrievingDone() &&
                syncManager.blockchain.getTotalDifficulty()
                        .compareTo(syncManager.chainInfoManager.getTotalDiff()) < 0){
            syncManager.changeState(HASH_RETRIEVING);
            return;
        }
    }
}
