package io.taucoin.android_app;

import io.taucoin.android.service.TaucoinAidlService;
import io.taucoin.android.interop.IListener;

public class TaucoinService extends TaucoinAidlService {

    public TaucoinService() {

    }

    @Override
    protected void broadcastMessage(String message) {

        updateLog(message);
        for (IListener listener: clientListeners) {
            try {
                listener.trace(message);
            } catch (Exception e) {
                // Remove listener
                clientListeners.remove(listener);
            }
        }
    }

    private void updateLog(String message) {

        TaucoinService.log += message;
        int logLength = TaucoinService.log.length();
        if (logLength > 5000) {
            TaucoinService.log = TaucoinService.log.substring(2500);
        }
    }

}
