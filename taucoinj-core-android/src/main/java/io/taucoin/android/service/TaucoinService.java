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
import io.taucoin.android.service.events.*;
import io.taucoin.core.Genesis;
import io.taucoin.core.PendingState;
import io.taucoin.core.Transaction;
import io.taucoin.core.TransactionExecuatedOutcome;
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
import java.math.BigInteger;
import java.util.*;

import static io.taucoin.config.SystemProperties.CONFIG;

public class TaucoinService extends Service {

    static boolean isConnected = false;

    static boolean isInitialized = false;

    protected static Taucoin taucoin = null;
    protected static TaucoinComponent component = null;

    protected static JsonRpcServer jsonRpcServer;
    protected static Thread jsonRpcServerThread;
    protected  TransactionExecuatedOutcome appWanted = new TransactionExecuatedOutcome();
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
        if (taucoin != null) {
            taucoin.close();
            taucoin = null;
        }
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

            createTaucoin(this.privateKeys.size() != 0 ? this.privateKeys.get(0) : "");
            return null;
        }

        protected void onPostExecute(Void results) {

            onTaucoinCreated(privateKeys, replyTo, reply);
        }
    }

    protected void onTaucoinCreated(List<String> privateKeys, Messenger replyTo, Object reply) {
    }

    protected void createTaucoin(String privateKey) {
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

    protected class TaucoinListener implements io.taucoin.listener.TaucoinListener {

        @Override
        public void trace(String output) {
            // app don't care trace event.
            // broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData(output));
        }

        @Override
        public void onBlock(io.taucoin.core.Block block) {

            broadcastEvent(EventFlag.EVENT_BLOCK, new BlockEventData(block));
        }

        @Override
        public void onBlockConnected(io.taucoin.core.Block block) {
            broadcastEvent(EventFlag.EVENT_BLOCK_CONNECT, new BlockEventData(block));
        }

        @Override
        public void onBlockDisconnected(io.taucoin.core.Block block) {
            broadcastEvent(EventFlag.EVENT_BLOCK_DISCONNECT, new BlockEventData(block));
        }

        @Override
        public void onRecvMessage(Channel channel, Message message) {

            //broadcastEvent(EventFlag.EVENT_RECEIVE_MESSAGE, new MessageEventData(message.getClass(), message.getEncoded()));
        }

        @Override
        public void onSendMessage(Channel channel, Message message) {
            //broadcastEvent(EventFlag.EVENT_SEND_MESSAGE, new MessageEventData(message.getClass(), message.getEncoded()));
        }

        @Override
        public void onPeerDisconnect(String host, long port) {

            //broadcastEvent(EventFlag.EVENT_PEER_DISCONNECT, new PeerDisconnectEventData(host, port));
        }

        @Override
        public void onPendingTransactionsReceived(List<Transaction> transactions) {

            //broadcastEvent(EventFlag.EVENT_PENDING_TRANSACTIONS_RECEIVED, new PendingTransactionsEventData(transactions));
        }

        @Override
        public void onPendingStateChanged(PendingState pendingState) {
        }

        @Override
        public void onSyncDone() {

            //broadcastEvent(EventFlag.EVENT_SYNC_DONE, new EventData());
        }

        @Override
        public void onNoConnections() {

            //broadcastEvent(EventFlag.EVENT_NO_CONNECTIONS, new EventData());
        }

        @Override
        public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {

            //broadcastEvent(EventFlag.EVENT_HANDSHAKE_PEER, new MessageEventData(helloMessage.getClass(), helloMessage.getEncoded()));
        }

        @Override
        public void onEthStatusUpdated(Channel channel, StatusMessage status) {

            //broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData("Eth status update: " + status.toString()));
        }

        @Override
        public void onNodeDiscovered(Node node) {

            broadcastEvent(EventFlag.EVENT_TRACE, new TraceEventData("Node discovered: " + node.toString()));
        }

        @Override
        public void onPeerAddedToSyncPool(Channel peer) {
        }

        @Override
        public void onTransactionExecuated(TransactionExecuatedOutcome outcome) {
//            System.out.println("outcome is: "+ Hex.toHexString(outcome.getBlockhash())
//                              +" \ntxid: "+Hex.toHexString(outcome.getTxid()));
            if (!outcome.isTxComplete()) {
                Iterator<Map.Entry<byte[],Long>> iterc = outcome.getCurrentWintess().entrySet().iterator();
                if (iterc.hasNext()) {
                    Map.Entry<byte[], Long> entry = iterc.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateCurrentWintessBalance(entry.getKey(), entry.getValue());
                    }
                }

                Iterator<Map.Entry<byte[],Long>> iterl = outcome.getLastWintess().entrySet().iterator();
                if (iterl.hasNext()) {
                    Map.Entry<byte[], Long> entry = iterl.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateLastWintessBalance(entry.getKey(), entry.getValue());
                    }
                }

                Iterator<Map.Entry<byte[],Long>> iters = outcome.getSenderAssociated().entrySet().iterator();
                while (iters.hasNext()) {
                    Map.Entry<byte[], Long> entry = iters.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateSenderAssociated(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                Iterator<Map.Entry<byte[],Long>> iterc = outcome.getCurrentWintess().entrySet().iterator();
                if (iterc.hasNext()) {
                    Map.Entry<byte[], Long> entry = iterc.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateCurrentWintessBalance(entry.getKey(), entry.getValue());
                    }
                }

                Iterator<Map.Entry<byte[],Long>> iterl = outcome.getLastWintess().entrySet().iterator();
                if (iterl.hasNext()) {
                    Map.Entry<byte[], Long> entry = iterl.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateLastWintessBalance(entry.getKey(), entry.getValue());
                    }
                }

                Iterator<Map.Entry<byte[],Long>> iters = outcome.getSenderAssociated().entrySet().iterator();
                while (iters.hasNext()) {
                    Map.Entry<byte[], Long> entry = iters.next();
                    if (entry.getKey().equals(CONFIG.getForgerCoinbase())) {
                        appWanted.updateSenderAssociated(entry.getKey(), entry.getValue());
                    }
                }
                appWanted.setBlockHash(outcome.getBlockhash());
                broadcastEvent(EventFlag.EVENT_TRANSACTION_EXECUATED, new TransactionExecuatedEvent(appWanted));
                appWanted.getSenderAssociated().clear();
                appWanted.getLastWintess().clear();
                appWanted.getCurrentWintess().clear();
            }
        }

        @Override
        public void onSendHttpPayload(String payload) {
            if (payload != null && payload.length() > 0) {
                broadcastEvent(EventFlag.EVENT_NETWORK_TRAFFIC, new NetworkTrafficData(payload.length()));
            }
        }

        @Override
        public void onRecvHttpPayload(String payload) {
            if (payload != null && payload.length() > 0) {
                broadcastEvent(EventFlag.EVENT_NETWORK_TRAFFIC, new NetworkTrafficData(payload.length()));
            }
        }

        @Override
        public void onChainInfoChanged(long height, byte[] previousBlockHash,
                byte[] currentBlockHash, BigInteger totalDiff, long medianFee) {
            broadcastEvent(EventFlag.EVENT_CHAININFO_CHANGED, new ChainInfoChangedData(height, medianFee));
        }
    }
}
