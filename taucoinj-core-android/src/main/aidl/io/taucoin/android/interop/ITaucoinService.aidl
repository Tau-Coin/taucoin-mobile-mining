package io.taucoin.android.interop;

import io.taucoin.android.interop.IListener;
import io.taucoin.android.interop.IAsyncCallback;

oneway interface ITaucoinService {

    void loadBlocks(String dumpFile);
    void connect(String ip, int port, String remoteId);
    void startJsonRpcServer();
    void addListener(IListener listener);
    void removeListener(IListener listener);
    void getLog(IAsyncCallback callback);
}
