package io.taucoin.http;

import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.http.message.Message;
import io.taucoin.http.tau.message.*;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.sync2.ChainInfoManager;
import io.taucoin.sync2.IdleState;
import io.taucoin.sync2.SyncManager;
import io.taucoin.sync2.SyncQueue;
import io.taucoin.sync2.SyncStateEnum;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigInteger;
import java.util.List;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     (1) Sync state change invoke getting chain information or getting hashes.
 *     (2) Handle inbound messages, etc, ChainInfoMessage, HashesMessage and so on.
 */
@Singleton
public class RequestManager extends SimpleChannelInboundHandler<Message> {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected TaucoinListener listener;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected RequestQueue requestQueue;

    protected ChainInfoManager chainInfoManager;

    private Object stateLock = new Object();
    private SyncStateEnum syncState = SyncStateEnum.IDLE;

    @Inject
    public RequestManager(Blockchain blockchain, BlockStore blockstore, TaucoinListener listener,
            SyncManager syncManager, SyncQueue queue, RequestQueue requestQueue,
            ChainInfoManager chainInfoManager) {
        this.blockchain = blockchain;
        this.blockstore = blockstore;
        this.listener = listener;
        this.syncManager = syncManager;
        this.queue = queue;
        this.requestQueue = requestQueue;
        this.chainInfoManager = chainInfoManager;
    }

    public void changeSyncState(SyncStateEnum state) {
        synchronized(stateLock) {
            this.syncState = state;
        }
    }

    public SyncStateEnum getSyncState() {
        synchronized(stateLock) {
            return this.syncState;
        }
    }

    public boolean isHashRetrievingDone(){
        //todo
        return false;
    }

    public boolean isHashRetrieving(){
        //todo
        return false;
    }

    public boolean isChainInfoRetrievingDone(){
        return false;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, Message msg) throws InterruptedException {

        listener.trace(String.format("RequestManager invoke: [%s]", msg.getClass()));

        if (msg instanceof ChainInfoMessage) {
            processChainInfoMessage((ChainInfoMessage)msg);
        } else if (msg instanceof HashesMessage) {
            processHashesMessage((HashesMessage)msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void processChainInfoMessage(ChainInfoMessage msg) {
    }

    private void processHashesMessage(HashesMessage msg) {
    }
}
