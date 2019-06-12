package io.taucoin.android.debug;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class TauMobileRefWatcher implements io.taucoin.debug.RefWatcher {

    private com.squareup.leakcanary.RefWatcher refWatcherImpl;

    public TauMobileRefWatcher() {
        refWatcherImpl = LeakCanary.installedRefWatcher();
    }

    @Override
    public void watch(Object o) {
        if (o != null) {
            refWatcherImpl.watch(o);
        }
    }
}
