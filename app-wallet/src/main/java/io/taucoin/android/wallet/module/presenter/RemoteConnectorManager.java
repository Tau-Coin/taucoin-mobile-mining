package io.taucoin.android.wallet.module.presenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.github.naturs.logger.Logger;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.TaucoinConnector;
import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.MessageEventData;
import io.taucoin.android.service.events.PeerDisconnectEventData;
import io.taucoin.android.service.events.PendingTransactionsEventData;
import io.taucoin.android.service.events.TraceEventData;
import io.taucoin.android.service.events.VMTraceCreatedEventData;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.core.Transaction;
import io.taucoin.core.Utils;
import io.taucoin.core.transaction.TransactionOptions;
import io.taucoin.core.transaction.TransactionVersion;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.util.ByteUtil;

public class RemoteConnectorManager implements ConnectorHandler {

    private static final String TAG = "RemoteManager";

    private TaucoinConnector mTaucoinConnector = null;

    @SuppressLint("SimpleDateFormat")
    private DateFormat mDateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
    private String mHandlerIdentifier = UUID.randomUUID().toString();
    private String mConsoleLog = "";
    private boolean isTaucoinConnected = false;
    private boolean isInit = false;

    private final static int CONSOLE_LENGTH = 10000;
    private static final int BOOT_UP_DELAY_INIT_SECONDS = 2;

    static final String ACTION_CONSOLE_LOG = "intent.action.console.log";
    static final String ACTION_BLOCK_SYNC = "intent.action.block.sync";
    static final String ACTION_BLOCK_HASH = "intent.action.block.hash";
    static final String ACTION_POOL_TXS = "intent.action.pool.txs";

    public void createRemoteConnector(boolean isInit){
        if (mTaucoinConnector == null) {
            addLogEntry("Create Remote Connector...");
            Context context = MyApplication.getInstance();
            mTaucoinConnector = new TaucoinConnector(context, TaucoinRemoteService.class);
            mTaucoinConnector.registerHandler(this);
            mTaucoinConnector.bindService();

            if(isInit){
                init();
            }
        }
    }

    public void createRemoteConnector(){
        createRemoteConnector(false);
    }

    public void cancelRemoteConnector(){
        if (mTaucoinConnector != null) {
            addLogEntry("Cancel Remote Connector...");
            mTaucoinConnector.removeHandler(this);
            mTaucoinConnector.unbindService();
            mTaucoinConnector = null;
        }
    }

    @Override
    public void onConnectorConnected() {
        if (!isTaucoinConnected) {
            addLogEntry("Connector Connected");
            isTaucoinConnected = true;
            mTaucoinConnector.addListener(mHandlerIdentifier, EnumSet.allOf(EventFlag.class));
        }
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent();
        broadcastAction(intent, action);
    }

    private void broadcastAction(Intent intent, String action) {
        intent.setAction(action);
        LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(intent);
    }

    private void addLogEntry(String message) {
        Date date = new Date();
        addLogEntry(date.getTime(), message);
    }

    public String getConsoleLog() {
        return mConsoleLog;
    }

    @Override
    public void onConnectorDisconnected() {
        addLogEntry("Connector Disconnected");
        mTaucoinConnector.removeListener(mHandlerIdentifier);
        isTaucoinConnected = false;
        isInit = false;
    }

    @Override
    public String getID() {
        return mHandlerIdentifier;
    }

    private void addLogEntry(long timestamp, String message) {
        Date date = new Date(timestamp);
        Logger.d("consoleLog=" + message);
        mConsoleLog += mDateFormatter.format(date) + " -> " + (message.length() > 100 ? message.substring(0, 100) + "..." : message) + "\n";

        int length = mConsoleLog.length();
        if (length > CONSOLE_LENGTH) {
            mConsoleLog = mConsoleLog.substring(CONSOLE_LENGTH * ((length / CONSOLE_LENGTH) - 1) + length % CONSOLE_LENGTH);
        }
        broadcastAction(ACTION_CONSOLE_LOG);

    }
    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean handleMessage(Message message) {
        Logger.d("message.what=\t" + message.what);
        boolean isClaimed = true;
        long time = new Date().getTime();
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
                switch (event) {
                    case EVENT_BLOCK:
                        BlockEventData blockEventData = data.getParcelable("data");
                        logMessage = "Added block with " + /*blockEventData.receipts.size() +*/ " transaction receipts.";
                        time = blockEventData.registeredTime;
                        addLogEntry(time, logMessage);
                        broadcastAction(ACTION_BLOCK_SYNC);
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
                    // import key and init return
                    case EVENT_ETHEREUM_CREATED:
                        isInit = true;
                        startSync();
                        break;

                }
                break;
            case TaucoinClientMessage.MSG_BLOCK_HASH_LIST:
                Intent intent = new Intent();
                intent.putExtras(message.getData());
                broadcastAction(intent, ACTION_BLOCK_HASH);
                break;
            case TaucoinClientMessage.MSG_POOL_TXS:
                intent = new Intent();
                intent.putExtras(message.getData());
                broadcastAction(intent, ACTION_POOL_TXS);
                break;
            // sync block return
            case TaucoinClientMessage.MSG_START_SYNC_RESULT:
                Bundle replyData = message.getData();
                String result =  replyData.getString(TransmitKey.RESULT);
                if(StringUtil.isSame(result, TransmitKey.RemoteResult.OK)){
                    startBlockForging();
                }
                logMessage = "start sync result: " + result;
                addLogEntry(time, logMessage);
                break;
            // get block return
            case TaucoinClientMessage.MSG_BLOCK:
                MessageEvent msgEvent = new MessageEvent();
                msgEvent.setCode(MessageEvent.EventCode.GET_BLOCK);
                msgEvent.setData(message.getData());
                EventBusUtil.post(msgEvent);
                break;
            // get block list return
            case TaucoinClientMessage.MSG_BLOCKS:
                break;
            default:
                isClaimed = false;
        }
        return isClaimed;
    }

    public void importForgerPrivkey(String privateKey){
        mTaucoinConnector.importForgerPrivkey(privateKey);
    }

    public void importPrivkeyAndInit(String privateKey){
        ScheduledExecutorService initializer = Executors.newSingleThreadScheduledExecutor();
        initializer.schedule(new InitTask(mTaucoinConnector, mHandlerIdentifier, privateKey),
                BOOT_UP_DELAY_INIT_SECONDS, TimeUnit.SECONDS);
    }

    private class InitTask implements Runnable {
        private TaucoinConnector taucoinConnector;
        private String handlerIdentifier;
        private List<String> privateKeys = new ArrayList<>();

        InitTask(TaucoinConnector taucoinConnector, String handlerIdentifier, String privateKey) {
            this.taucoinConnector = taucoinConnector;
            this.handlerIdentifier = handlerIdentifier;
            this.privateKeys.add(privateKey);
        }

        @Override
        public void run() {
            taucoinConnector.init(handlerIdentifier, privateKeys);
        }
    }

    /**
     * start sync block
     * */
    public void startSync(){
        mTaucoinConnector.startSync();
    }

    public void submitTransaction(String senderPrivateKey, String txToAddress, String txAmount, String txFee){
        long timeStamp = (new Date().getTime())/1000;
        byte[] privateKey = Utils.parseAsHexOrBase58(senderPrivateKey);
        byte[] toAddress = Utils.parseAsHexOrBase58(txToAddress);
        byte[] amount = (new BigInteger(txAmount)).toByteArray();
        byte[] fee = (new BigInteger(txFee)).toByteArray();

        Transaction transaction = new Transaction(TransactionVersion.V01.getCode(),
                TransactionOptions.TRANSACTION_OPTION_DEFAULT, ByteUtil.longToBytes(timeStamp), toAddress, amount, fee);
        transaction.sign(privateKey);
        io.taucoin.android.interop.Transaction interT = new io.taucoin.android.interop.Transaction(transaction);
        mTaucoinConnector.submitTransaction(mHandlerIdentifier, interT);
    }

    public void startBlockForging(){
        startBlockForging(-1);
    }

    public void startBlockForging(int targetAmount){
        mTaucoinConnector.startBlockForging(targetAmount);
    }

    public void stopBlockForging(){
        stopBlockForging(-1);
    }

    public void stopBlockForging(int targetAmount){
        mTaucoinConnector.stopBlockForging(targetAmount);
    }

    public void getBlockHashList(long start, long limit){
        Toast.makeText(MyApplication.getInstance(), "getBlockHashList", Toast.LENGTH_SHORT).show();
        mTaucoinConnector.getBlockHashList(start, limit);
    }

    public void getPendingTxs(){
        Toast.makeText(MyApplication.getInstance(), "getPendingTxs", Toast.LENGTH_SHORT).show();
        mTaucoinConnector.getPendingTxs();
    }

    public void getBlockByNumber(long number){
        Toast.makeText(MyApplication.getInstance(), "getBlockByNumber", Toast.LENGTH_SHORT).show();
        mTaucoinConnector.getBlockByNumber(mHandlerIdentifier, number);
    }

    /**
     * init
     * 1、import key and init or only import key
     * 2、start sync block
     * 3、start block forging
     * */
    public void init(){
        if(UserUtil.isImportKey()){
            String privateKey = MyApplication.getKeyValue().getPrivkey();
            // import privateKey and init
            if(isTaucoinConnected){
                if(isInit){
                    importForgerPrivkey(privateKey);
                    startSync();
                }else{
                    importPrivkeyAndInit(privateKey);
                }
            }else{
                createRemoteConnector(true);
            }
        }
    }
}