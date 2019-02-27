package io.taucoin.android.wallet.module.presenter;

import android.annotation.SuppressLint;
import android.content.Context;

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
import io.taucoin.android.service.TaucoinConnector;
import io.taucoin.android.service.TaucoinRemoteService;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.core.Transaction;

public abstract class ConnectorManager implements ConnectorHandler {

    TaucoinConnector mTaucoinConnector = null;

    @SuppressLint("SimpleDateFormat")
    private DateFormat mDateFormatter = new SimpleDateFormat("HH:mm:ss:SSS");
    String mHandlerIdentifier = UUID.randomUUID().toString();
    private String mConsoleLog = "";
    private boolean isTaucoinConnected = false;
    boolean isInit = false;
    boolean isSync = false;
    boolean isSyncMe = false;

    private final static int CONSOLE_LENGTH = 10000;
    private static final int BOOT_UP_DELAY_INIT_SECONDS = 2;

    public void createRemoteConnector(){
        if (mTaucoinConnector == null) {
            addLogEntry("Create Remote Connector...");
            Context context = MyApplication.getInstance();
            mTaucoinConnector = new TaucoinConnector(context, TaucoinRemoteService.class);
            mTaucoinConnector.registerHandler(this);
            mTaucoinConnector.bindService();
        }
    }

    public void cancelRemoteConnector(){
        if (mTaucoinConnector != null) {
            addLogEntry("Cancel Remote Connector...");
            closeTaucoin();
            mTaucoinConnector.removeHandler(this);
            mTaucoinConnector.unbindService();
            mTaucoinConnector = null;
        }
        EventBusUtil.post(MessageEvent.EventCode.MINING_STATE);
    }

    public boolean isInit() {
        return isInit;
    }

    public boolean isSync() {
        return isSync;
    }

    public boolean isSyncMe() {
        return isSyncMe;
    }

    @Override
    public void onConnectorConnected() {
        if (!isTaucoinConnected) {
            addLogEntry("Connector Connected");
            isTaucoinConnected = true;
            mTaucoinConnector.addListener(mHandlerIdentifier, EnumSet.allOf(EventFlag.class));

            if(!isInit){
                init();
            }
        }
    }

    void addLogEntry(String message) {
        Date date = new Date();
        addLogEntry(date.getTime(), message);
    }

    @Override
    public void onConnectorDisconnected() {
        if (mTaucoinConnector != null) {
            addLogEntry("Connector Disconnected");
            mTaucoinConnector.removeListener(mHandlerIdentifier);
            isTaucoinConnected = false;
            isInit = false;
        }
        EventBusUtil.post(MessageEvent.EventCode.MINING_STATE);
    }

    @Override
    public String getID() {
        return mHandlerIdentifier;
    }

    void addLogEntry(long timestamp, String message) {
        Date date = new Date(timestamp);
        Logger.d("consoleLog=" + message);
        mConsoleLog += mDateFormatter.format(date) + " -> " + (message.length() > 100 ? message.substring(0, 100) + "..." : message) + "\n";

        int length = mConsoleLog.length();
        if (length > CONSOLE_LENGTH) {
            mConsoleLog = mConsoleLog.substring(CONSOLE_LENGTH * ((length / CONSOLE_LENGTH) - 1) + length % CONSOLE_LENGTH);
        }
        EventBusUtil.post(MessageEvent.EventCode.CONSOLE_LOG);
    }

    public void importForgerPrivkey(String privateKey){
        Logger.d("importForgerPrivkey");
        if(mTaucoinConnector != null){
            mTaucoinConnector.importForgerPrivkey(privateKey);
        }
    }

    public void importPrivkeyAndInit(String privateKey){
        Logger.d("importPrivkeyAndInit");
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
            if(mTaucoinConnector != null){
                taucoinConnector.init(handlerIdentifier, privateKeys);
            }
        }
    }

    /**
     * start sync block
     * */
    public void startSync(){
        Logger.d("startSync");
        if(mTaucoinConnector != null){
            mTaucoinConnector.startSync();
        }
    }

    /**
     * 1、start sync block
     * 2、get block height
     * 3、get block list (update my mining block)
     * */
    public void startSyncAll(){
        Logger.d("startSync");
        if(mTaucoinConnector != null){
            mTaucoinConnector.startSync();
            getChainHeight();
        }
    }

    public void submitTransaction(Transaction transaction){
        Logger.d("submitTransaction");
        io.taucoin.android.interop.Transaction interT = new io.taucoin.android.interop.Transaction(transaction);
        if(mTaucoinConnector != null){
            mTaucoinConnector.submitTransaction(mHandlerIdentifier, interT);
        }
    }

    public void startBlockForging(){
        Logger.d("startBlockForging=-1");
        startBlockForging(-1);
    }

    public void startBlockForging(int targetAmount){
        Logger.d("startBlockForging=" + targetAmount);
        if(mTaucoinConnector != null){
            mTaucoinConnector.startBlockForging(targetAmount);
        }
    }

    public void stopBlockForging(){
        Logger.d("stopBlockForging=-1");
        stopBlockForging(-1);
    }

    public void stopBlockForging(int targetAmount){
        Logger.d("stopBlockForging=" + targetAmount);
        if(mTaucoinConnector != null){
            mTaucoinConnector.stopBlockForging(targetAmount);
        }
    }

    public void getBlockHashList(long start, long limit){
        Logger.d("getBlockHashList");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockHashList(start, limit);
        }
    }

    public void getPendingTxs(){
        Logger.d("getPendingTxs");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getPendingTxs();
        }
    }
    /**
     * get block data by number
     * */
    public void getBlockByNumber(long number){
        Logger.d("getBlockByNumber");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockByNumber(mHandlerIdentifier, number);
        }
    }

    /**
     * get block data by number
     *
     * @param height
     * */

    public void getBlockList(long height){
        getBlockList(-1, height);
    }

    public void getBlockList(int num, long height){
        Logger.d("getBlockList num=" + num + "\theight=" + height);
        isSyncMe = false;
        int limit = (int) height - num + 1;
        if(mTaucoinConnector != null){
            mTaucoinConnector.getBlockListByStartNumber(mHandlerIdentifier, num, limit);
        }
    }

    /**
     * get chain height (block sync)
     * */
    public void getChainHeight(){
        Logger.d("getChainHeight");
        if(mTaucoinConnector != null){
            mTaucoinConnector.getChainHeight(mHandlerIdentifier);
        }
    }

    public void closeTaucoin(){
        Logger.d("closeTaucoin");
        if(mTaucoinConnector != null){
            mTaucoinConnector.closeEthereum();
        }
    }

    /**
     * init
     * 1、import key and init or only import key
     * 2、start sync block
     * 3、start block forging
     * */
    public void init(){
        Logger.d("init");
        if(UserUtil.isImportKey()){
            String privateKey = MyApplication.getKeyValue().getPrivkey();
            // import privateKey and init
            if(isTaucoinConnected){
                if(isInit){
                    importForgerPrivkey(privateKey);
                    startSyncAll();
                }else{
                    importPrivkeyAndInit(privateKey);
                }
            }else{
                createRemoteConnector();
            }
        }
    }
}