package io.taucoin.android.wallet.module.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import com.github.naturs.logger.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.taucoin.android.interop.Transaction;
import io.taucoin.android.service.ConnectorHandler;
import io.taucoin.android.service.TaucoinClientMessage;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.service.events.EventData;
import io.taucoin.android.service.events.EventFlag;
import io.taucoin.android.service.events.MessageEventData;
import io.taucoin.android.service.events.PeerDisconnectEventData;
import io.taucoin.android.service.events.PendingTransactionsEventData;
import io.taucoin.android.service.events.TraceEventData;
import io.taucoin.android.service.events.VMTraceCreatedEventData;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.IMiningModel;
import io.taucoin.android.wallet.module.model.MiningModel;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.net.p2p.HelloMessage;

public class RemoteConnectorManager extends ConnectorManager implements ConnectorHandler {

    private IMiningModel mMiningModel;

    private synchronized IMiningModel getMiningModel(){
        if(mMiningModel == null){
            synchronized (MiningModel.class){
                if(mMiningModel == null){
                    mMiningModel = new MiningModel();
                }
            }
        }
        return mMiningModel;
    }

    @Override
    public void onConnectorConnected() {
        super.onConnectorConnected();
        mMiningModel = null;
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
                        handleSynchronizedBlock(blockEventData);
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
                    // import key and init return
                    case EVENT_ETHEREUM_CREATED:
                        isInit = true;
                        startSyncAll();
                        break;

                }
                break;
            case TaucoinClientMessage.MSG_POOL_TXS:
                Intent intent = new Intent();
                intent.putExtras(message.getData());
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
                replyData = message.getData();
                replyData.setClassLoader(BlockEventData.class.getClassLoader());
                List<BlockEventData> blocks = replyData.getParcelableArrayList(TransmitKey.RemoteResult.BLOCKS);
                updateMyMiningBlock(blocks);
                break;
            // get block chain height return
            case TaucoinClientMessage.MSG_CHAIN_HEIGHT:
                replyData = message.getData();
                long height =  replyData.getLong(TransmitKey.RemoteResult.HEIGHT);
                updateBlockSynchronized(height);
                getBlockList(height);
                break;
            case TaucoinClientMessage.MSG_SUBMIT_TRANSACTION_RESULT:
                replyData = message.getData();
                replyData.setClassLoader(Transaction.class.getClassLoader());
                Transaction transaction = replyData.getParcelable(TransmitKey.RemoteResult.TRANSACTION);
                ToastUtils.showShortToast("MSG_SUBMIT_TRANSACTION_RESULT");
                break;
            default:
                isClaimed = false;
        }
        return isClaimed;
    }

    private void handleSynchronizedBlock(BlockEventData block) {
        if(block != null){
            getMiningModel().handleSynchronizedBlock(block, new LogicObserver<MessageEvent.EventCode>() {
                @Override
                public void handleData(MessageEvent.EventCode eventCode) {
                    if(eventCode != null){
                        if(eventCode == MessageEvent.EventCode.ALL){
                            EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                            EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
                        }else{
                            EventBusUtil.post(eventCode);
                        }
                    }
                }
            });
        }
    }

    private void updateMyMiningBlock(List<BlockEventData> blocks) {
        if(blocks != null){
            getMiningModel().updateMyMiningBlock(blocks, new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean aBoolean) {
                    EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
                }
            });
        }
    }

    private void updateBlockSynchronized(long height){
        int blockHeight = (int) height;
        getMiningModel().updateBlockSynchronized(blockHeight, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean aBoolean) {
                EventBusUtil.post(MessageEvent.EventCode.MINING_INFO);
            }
        });
    }
}