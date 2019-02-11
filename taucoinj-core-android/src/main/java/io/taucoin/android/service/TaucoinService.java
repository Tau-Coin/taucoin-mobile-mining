package io.taucoin.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Messenger;

import io.taucoin.android.di.components.DaggerTaucoinComponent;
import io.taucoin.android.di.components.TaucoinComponent;
import io.taucoin.android.di.modules.TaucoinModule;
import io.taucoin.android.rpc.server.JsonRpcServer;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.MessageEventData;
import io.taucoin.android.service.events.PeerDisconnectEventData;
import io.taucoin.android.service.events.PendingTransactionsEventData;
import io.taucoin.android.service.events.TraceEventData;
import io.taucoin.android.service.events.VMTraceCreatedEventData;
import io.taucoin.core.Genesis;
import io.taucoin.core.PendingState;
import io.taucoin.core.Transaction;
import io.taucoin.crypto.HashUtil;
import io.taucoin.android.Taucoin;
import io.taucoin.net.message.Message;
import io.taucoin.net.tau.message.StatusMessage;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.Channel;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.sync.PeersPool;
import org.spongycastle.util.encoders.Hex;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.taucoin.config.SystemProperties.CONFIG;

public class TaucoinService extends Service {

    static boolean isConnected = false;

    static boolean isInitialized = false;

    protected static Taucoin taucoin = null;
    protected static TaucoinComponent component = null;

    protected static JsonRpcServer jsonRpcServer;
    protected static Thread jsonRpcServerThread;

    public TaucoinService() {
    }

    protected void broadcastEvent(EventFlag event, EventData data) {}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (jsonRpcServerThread != null) {
            jsonRpcServerThread.interrupt();
            jsonRpcServerThread = null;
        }
        taucoin.close();
    }

    protected class InitializeTask extends AsyncTask<Void, Void, Void> {

        protected List<String> privateKeys = null;
        protected Object reply = null;
        protected Messenger replyTo = null;

        public InitializeTask(List<String> privateKeys, Messenger replyTo, Object reply) {

            this.privateKeys = privateKeys;
            this.replyTo = replyTo;
            this.reply = reply;
        }

        protected Void doInBackground(Void... args) {

            createTaucoin();
            return null;
        }

        protected void onPostExecute(Void results) {

            onTaucoinCreated(privateKeys, replyTo, reply);
        }
    }

    protected void onTaucoinCreated(List<String> privateKeys, Messenger replyTo, Object reply) {
    }

    protected void createTaucoin() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    protected class EthereumListener implements io.taucoin.listener.EthereumListener {

        @Override
        public void trace(String output) {

            broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData(output));
        }

        @Override
        public void onBlock(io.taucoin.core.Block block) {

            broadcastEvent(EventFlag.EVENT_BLOCK, new BlockEventData(block));
        }

        @Override
        public void onRecvMessage(Channel channel, Message message) {

            broadcastEvent(EventFlag.EVENT_RECEIVE_MESSAGE, new MessageEventData(message.getClass(), message.getEncoded()));
        }

        @Override
        public void onSendMessage(Channel channel, Message message) {
            broadcastEvent(EventFlag.EVENT_SEND_MESSAGE, new MessageEventData(message.getClass(), message.getEncoded()));
        }

        @Override
        public void onPeerDisconnect(String host, long port) {

            broadcastEvent(EventFlag.EVENT_PEER_DISCONNECT, new PeerDisconnectEventData(host, port));
        }

        @Override
        public void onPendingTransactionsReceived(List<Transaction> transactions) {

            broadcastEvent(EventFlag.EVENT_PENDING_TRANSACTIONS_RECEIVED, new PendingTransactionsEventData(transactions));
        }

        @Override
        public void onPendingStateChanged(PendingState pendingState) {
        }

        @Override
        public void onSyncDone() {

            broadcastEvent(EventFlag.EVENT_SYNC_DONE, new EventData());
        }

        @Override
        public void onNoConnections() {

            broadcastEvent(EventFlag.EVENT_NO_CONNECTIONS, new EventData());
        }

        @Override
        public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {

            broadcastEvent(EventFlag.EVENT_HANDSHAKE_PEER, new MessageEventData(helloMessage.getClass(), helloMessage.getEncoded()));
        }

        @Override
        public void onEthStatusUpdated(Channel channel, StatusMessage status) {

            broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData("Eth status update: " + status.toString()));
        }

        @Override
        public void onNodeDiscovered(Node node) {

            broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData("Node discovered: " + node.toString()));
        }

        @Override
        public void onPeerAddedToSyncPool(Channel peer) {
        }
    }
}
