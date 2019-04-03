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
import io.taucoin.util.Utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.sync2.SyncStateEnum.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     (1) Sync state change invoke getting chain information or getting hashes.
 *     (2) Handle inbound messages, etc, ChainInfoMessage, HashesMessage and so on.
 */
@Singleton
public class RequestManager extends SimpleChannelInboundHandler<Message>
        implements RequestQueue.MessageListener {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected TaucoinListener listener;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected RequestQueue requestQueue;

    protected ChainInfoManager chainInfoManager;

    private Object stateLock = new Object();
    private SyncStateEnum syncState = IDLE;

    private boolean commonAncestorFound = false;

    // Get some amount hashes in the following size.
    private static final long[] FORK_MAINTAIN_HASHES_AMOUNT
            = {6, 12, 24, 72, 144, 288, 288 * 10, 288 * 30};
    private int hashesAmountArrayIndex = 0;

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

        this.requestQueue.registerListener(this);
    }

    public void changeSyncState(SyncStateEnum state) {
        synchronized(stateLock) {
            if (this.syncState == state) {
                return;
            }

            this.syncState = state;
        }

        switch (state) {
            case CHAININFO_RETRIEVING:
                startPullChainInfo();
                break;

            case HASH_RETRIEVING:
                startForkCoverage();
                break;

            case BLOCK_RETRIEVING:
                break;

            default:
                break;
        }
    }

    public SyncStateEnum getSyncState() {
        synchronized(stateLock) {
            return this.syncState;
        }
    }

    public boolean isHashRetrievingDone(){
        synchronized(stateLock) {
            return this.syncState == DONE_HASH_RETRIEVING;
        }
    }

    public boolean isHashRetrieving(){
        synchronized(stateLock) {
            return this.syncState == HASH_RETRIEVING;
        }
    }

    public boolean isChainInfoRetrievingDone(){
        synchronized(stateLock) {
            return this.syncState == DONE_CHAININFO_RETRIEVING;
        }
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
        chainInfoManager.update(msg.getHeight(), msg.getPreviousBlockHash(),
                msg.getCurrentBlockHash(), msg.getTotalDiff());
        changeSyncState(DONE_CHAININFO_RETRIEVING);
    }

    private void startPullChainInfo() {
        GetChainInfoMessage message = new GetChainInfoMessage();
        requestQueue.sendMessage(message);
    }

    private void processHashesMessage(HashesMessage msg) {
        if (logger.isTraceEnabled()) {
            logger.trace("Processing BlockHashes, size [{}]", msg.getHashes().size());
        }

        List<byte[]> received = msg.getHashes();

        // treat empty hashes response as end of hashes sync
        if (received.isEmpty()) {
            changeSyncState(DONE_HASH_RETRIEVING);
        } else {
            if (syncState == HASH_RETRIEVING && !commonAncestorFound) {
                maintainForkCoverage(received, msg.getStartNumber(), msg.getReverse());
                return;
            }

            logger.error("Processing BlockHashes fatal error start {}, reverse {}",
                    msg.getStartNumber(), msg.getReverse());
        }
    }

    /*************************
     *     Fork Coverage     *
     *************************/

    private void startForkCoverage() {

        commonAncestorFound = false;
        hashesAmountArrayIndex = 0;

        long bestNumber = blockchain.getBestBlock().getNumber();
        byte[] bestHash = blockchain.getBestBlock().getHash();

        logger.debug("Start looking for common ancestor, height {}, hash {}",
                bestNumber, Hex.toHexString(bestHash));

        if (chainInfoManager.getHeight() == bestNumber + 1
                && Utils.hashEquals(chainInfoManager.getPreviousBlockHash(), bestHash)) {
            commonAncestorFound = true;
            pushBlockNumbers(bestNumber + 1);
        } else {
            sendGetBlockHashes(bestNumber,
                    FORK_MAINTAIN_HASHES_AMOUNT[hashesAmountArrayIndex++], true);
        }
    }

    private void maintainForkCoverage(List<byte[]> received, long startNumber,
            boolean reverse) {
        long ancestorNumber = startNumber;
        if (!reverse) {
            Collections.reverse(received);
            ancestorNumber = startNumber + received.size() - 1;
        }

        ListIterator<byte[]> it = received.listIterator();

        while (it.hasNext()) {
            byte[] hash = it.next();
            logger.info("isBlockExist {}", Hex.toHexString(hash));
            if (blockchain.isBlockExist(hash)) {
                commonAncestorFound = true;
                logger.info(
                        "common ancestor found: block.number {}, block.hash {}",
                        ancestorNumber, Hex.toHexString(hash));
                break;
            }
            ancestorNumber--;
        }

        if (commonAncestorFound) {
            pushBlockNumbers(ancestorNumber + 1);
            changeSyncState(DONE_HASH_RETRIEVING);
        } else {
            if (hashesAmountArrayIndex >= FORK_MAINTAIN_HASHES_AMOUNT.length) {
                logger.error("common ancestor is not found, drop");
                syncManager.reportBadAction(null);
                changeSyncState(DONE_HASH_RETRIEVING);
                // TODO: add this peer into black list
                return;
            } else {
                logger.info("Continue finding common ancestor {}",
                        blockchain.getBestBlock().getNumber() + 1);
                sendGetBlockHashes(blockchain.getBestBlock().getNumber(),
                        FORK_MAINTAIN_HASHES_AMOUNT[hashesAmountArrayIndex++], true);
            }
        }
    }

    private void pushBlockNumbers(long startNumber) {
        queue.addBlockNumbers(startNumber, chainInfoManager.getHeight());
    }

    protected void sendGetBlockHashes(long blockNumber, long maxBlocksAsk, boolean reverse) {
        if (logger.isTraceEnabled()) {
            logger.trace(
                    "send GetBlockHeaders, blockNumber [{}], maxBlocksAsk [{}], reverse [{}]",
                    blockNumber, maxBlocksAsk, reverse);
        }

        GetHashesMessage msg = new GetHashesMessage(blockNumber, maxBlocksAsk, reverse);
        requestQueue.sendMessage(msg);
    }

    @Override
    public void onMessageTimeout(Message message) {
        logger.warn("Message timeout {}", message);
    }
}
