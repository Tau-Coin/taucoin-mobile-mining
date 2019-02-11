package io.taucoin.android_app;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.support.multidex.MultiDexApplication;

import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.TaucoinConnector;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.MessageEventData;
import io.taucoin.android.service.events.PeerDisconnectEventData;
import io.taucoin.android.service.events.PendingTransactionsEventData;
import io.taucoin.android.service.events.TraceEventData;
import io.taucoin.android.service.events.VMTraceCreatedEventData;
import org.ethereum.config.SystemProperties;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

public class TaucoinApplication extends MultiDexApplication implements ConnectorHandler {

    public static TaucoinConnector ethereumConnector = null;
    public static TaucoinApplication instance;

    public static String consoleLog = "";

    public static String handlerIdentifier = UUID.randomUUID().toString();

    @SuppressLint("SimpleDateFormat")
    private DateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");



    public boolean isEthereumConnected = false;

    public TaucoinApplication() {

        instance = this;
    }

    @Override public void onCreate() {

        super.onCreate();
        if (ethereumConnector == null) {
            System.out.println("Creating ethereum connector");
            ethereumConnector = new TaucoinConnector(this, TaucoinRemoteService.class);
            ethereumConnector.registerHandler(this);
            ethereumConnector.bindService();
        }
    }

    @Override
    public void onTerminate() {

        super.onTerminate();
        System.out.println("Terminating application");
        ethereumConnector.removeHandler(this);
        ethereumConnector.unbindService();
        ethereumConnector = null;
    }

    private ArrayList<String> getDefaultAccounts() {

        ArrayList<String> accounts = new ArrayList<String>();

        return null;
    }

    @Override
    public void onConnectorConnected() {
        System.out.println("Connector connected");
        if (!isEthereumConnected) {
            isEthereumConnected = true;
            ethereumConnector.addListener(handlerIdentifier, EnumSet.allOf(EventFlag.class));
            //ethereumConnector.init(getDefaultAccounts());
            //Node node = SystemProperties.CONFIG.peerActive().get(0);
            //ethereumConnector.connect(node.getHost(), node.getPort(), node.getHexId());
        }
    }

    @Override
    public void onConnectorDisconnected() {
        System.out.println("Connector Disconnected");
        ethereumConnector.removeListener(handlerIdentifier);
        isEthereumConnected = false;
    }

    @Override
    public String getID() {
        return handlerIdentifier;
    }

    private void addLogEntry(long timestanp, String message) {
        Date date = new Date(timestanp);
        consoleLog += dateFormatter.format(date) + " -> " + (message.length() > 100 ? message.substring(0, 100) + "..." : message) + "\n";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean handleMessage(Message message) {

        boolean isClaimed = true;
        switch (message.what) {
            case TaucoinClientMessage.MSG_EVENT:
                Bundle data = message.getData();
                data.setClassLoader(EventFlag.class.getClassLoader());
                EventFlag event = (EventFlag) data.getSerializable("event");
                if (event == null)
                    return false;
                EventData eventData;
                MessageEventData messageEventData;
                String logMessage;
                long time;
                switch (event) {
                    case EVENT_BLOCK:
                        BlockEventData blockEventData = data.getParcelable("data");
                        logMessage = "Added block with " + /*blockEventData.receipts.size() +*/ " transaction receipts.";
                        time = blockEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_HANDSHAKE_PEER:
                        messageEventData = data.getParcelable("data");
                        logMessage = "Peer " + new HelloMessage(messageEventData.message).getPeerId() + " said hello";
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_NO_CONNECTIONS:
                        eventData = data.getParcelable("data");
                        logMessage = "No connections";
                        time = eventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_PEER_DISCONNECT:
                        PeerDisconnectEventData peerDisconnectEventData = data.getParcelable("data");
                        logMessage = "Peer " + peerDisconnectEventData.host + ":" + peerDisconnectEventData.port + " disconnected.";
                        time = peerDisconnectEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_PENDING_TRANSACTIONS_RECEIVED:
                        PendingTransactionsEventData pendingTransactionsEventData = data.getParcelable("data");
                        logMessage = "Received " + pendingTransactionsEventData.transactions.size() + " pending transactions";
                        time = pendingTransactionsEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_RECEIVE_MESSAGE:
                        messageEventData = data.getParcelable("data");
                        logMessage = "Received message: " + messageEventData.messageClass.getName();
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_SEND_MESSAGE:
                        messageEventData = data.getParcelable("data");
                        logMessage = "Sent message: " + messageEventData.messageClass.getName();
                        time = messageEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_SYNC_DONE:
                        eventData = data.getParcelable("data");
                        logMessage = "Sync done";
                        time = eventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_VM_TRACE_CREATED:
                        VMTraceCreatedEventData vmTraceCreatedEventData = data.getParcelable("data");
                        logMessage = "VM trace created: " + vmTraceCreatedEventData.transactionHash;// + " - " + vmTraceCreatedEventData.trace);
                        time = vmTraceCreatedEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                    case EVENT_TRACE:
                        TraceEventData traceEventData = data.getParcelable("data");
                        //System.out.println("We got a trace message: " + traceEventData.message);
                        logMessage = traceEventData.message;
                        time = traceEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        break;
                }
                break;
            default:
                isClaimed = false;
        }
        return isClaimed;
    }
}
