package io.taucoin.http;

import io.taucoin.listener.TaucoinListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.http.ConnectionState.*;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     Maintain network state.
 */
@Singleton
public class ConnectionManager {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    private Object stateLock = new Object();
    private ConnectionState state = CONNECTED;

    private TaucoinListener listener;

    @Inject
    public ConnectionManager(TaucoinListener listener) {
        this.listener = listener;
    }

    public ConnectionState getConnectionState() {
        synchronized(stateLock) {
            return this.state;
        }
    }

    public void setConnectionState(ConnectionState state) {
        synchronized(stateLock) {
            this.state = state;
        }
    }

    public boolean isNetworkConnected() {
        synchronized(stateLock) {
            return this.state == CONNECTED;
        }
    }
}
