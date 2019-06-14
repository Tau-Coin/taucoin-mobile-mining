package io.taucoin.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.http.ConnectionState.*;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     Maintain network state.
 */
public class ConnectionManager {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    private Object stateLock = new Object();
    private ConnectionState state = CONNECTED;

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
