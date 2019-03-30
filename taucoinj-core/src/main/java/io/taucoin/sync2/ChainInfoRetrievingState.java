package io.taucoin.sync2;

import static io.taucoin.sync2.SyncStateEnum.*;

public class ChainInfoRetrievingState extends AbstractSyncState {
    public ChainInfoRetrievingState(){
        super(CHAININFO_RETRIEVING);
    }
}
