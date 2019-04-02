package io.taucoin.sync2;

import static io.taucoin.sync2.SyncStateEnum.*;

public class ChainInfoRetrievingState extends AbstractSyncState {
    public ChainInfoRetrievingState(){
        super(CHAININFO_RETRIEVING);
    }
    @Override
    public void doOnTransition(){
        syncManager.requestManager.changeSyncState(CHAININFO_RETRIEVING);
    }
    @Override
    public void doMaintain(){
        super.doMaintain();

        if(syncManager.requestManager.isChainInfoRetrievingDone() &&
         syncManager.blockchain.getTotalDifficulty().compareTo(syncManager.requestManager.getTotalDifficulty()) >= 0){
            syncManager.changeState(IDLE);
            return;
        }
        if(syncManager.requestManager.isChainInfoRetrievingDone() &&
        syncManager.blockchain.getTotalDifficulty().compareTo(syncManager.requestManager.getTotalDifficulty()) < 0){
            syncManager.changeState(HASH_RETRIEVING);
            return;
        }
        syncManager.requestManager.changeStateForIdles(CHAININFO_RETRIEVING);
    }
}
