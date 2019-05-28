package io.taucoin.http;

import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
import io.taucoin.db.BlockStore;
import io.taucoin.core.Blockchain;
import io.taucoin.http.client.ClientsPool;
import io.taucoin.http.discovery.PeersManager;
import io.taucoin.http.message.Message;
import io.taucoin.http.tau.message.*;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.net.rlpx.Node;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;

import static io.taucoin.http.ConnectionState.*;
import static io.taucoin.sync2.SyncStateEnum.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * HTTP Module Core Manager.
 *
 * Main Functions:
 *     (1) Sync state change invoke getting chain information or getting hashes.
 *     (2) Handle inbound messages, etc, ChainInfoMessage, HashesMessage,
 *         GetBlocksMessage and so on.
 */
@Singleton
public class RequestManager implements RequestQueue.MessageListener {

    protected static final Logger logger = LoggerFactory.getLogger("http");

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected TaucoinListener listener;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected ClientsPool clientsPool;

    protected ChainInfoManager chainInfoManager;

    protected PeersManager peersManager;

    protected ConnectionManager connectionManager;

    private Object stateLock = new Object();
    private SyncStateEnum syncState = IDLE;

    private boolean commonAncestorFound = false;

    // Get some amount hashes in the following size.
    private static final long[] FORK_MAINTAIN_HASHES_AMOUNT
            = {6, 12, 24, 72, 144, 288, 288 * 10, 288 * 30};
    private int hashesAmountArrayIndex = 0;

    /**
     * Block number list sent in GetBlocksMessage,
     * useful if returned BLOCKS msg doesn't cover all sent numbers
     * or in case when peer is disconnected
     */
    private final List<Long> sentNumbers = Collections.synchronizedList(new ArrayList<Long>());

    /**
     * Queue with new blocks forged.
     */
    private BlockingQueue<Block> newBlocks = new LinkedBlockingQueue<>();

    /**
     * Queue with new transactions.
     */
    private BlockingQueue<Transaction> newTransactions = new LinkedBlockingQueue<>();

    private Thread blockDistributeThread;
    private Thread txDistributeThread;

    private AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public RequestManager(Blockchain blockchain, BlockStore blockstore, TaucoinListener listener,
            SyncManager syncManager, SyncQueue queue, ClientsPool clientsPool,
            ChainInfoManager chainInfoManager, PeersManager peersManager,
            ConnectionManager connectionManager) {
        this.blockchain = blockchain;
        this.blockstore = blockstore;
        this.listener = listener;
        this.syncManager = syncManager;
        this.queue = queue;
        this.clientsPool = clientsPool;
        this.chainInfoManager = chainInfoManager;
        this.peersManager = peersManager;
        this.connectionManager = connectionManager;

        this.clientsPool.setRequestManager(this);
    }

    public void start() {
        if (started.get()) {
            return;
        }
        started.set(true);
        logger.info("RequestManager is starting...");

        // sending new blocks to network in loop
        this.blockDistributeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newBlocksDistributeLoop();
            }
        }, "NewBlocksDistributeThread");
        this.blockDistributeThread.start();

        // sending pending txs to remote peers
        this.txDistributeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                newTxDistributeLoop();
            }
        }, "NewTxDistributeThread");
        this.txDistributeThread.start();
    }

    public void stop() {
        started.set(false);
        if (blockDistributeThread != null) {
            blockDistributeThread.interrupt();
            blockDistributeThread = null;
        }
        if (txDistributeThread != null) {
            txDistributeThread.interrupt();
            txDistributeThread = null;
        }
    }

    public void changeSyncState(SyncStateEnum state) {
        logger.info("sync state changed from {} to {}", getSyncState(), state);

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
                startBlockRetrieving();
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

    public synchronized void processMessage(Message msg) {

        listener.trace(String.format("RequestManager invoke: [%s]", msg.getClass()));

        if (msg instanceof ChainInfoMessage) {
            processChainInfoMessage((ChainInfoMessage)msg);
        } else if (msg instanceof HashesMessage) {
            processHashesMessage((HashesMessage)msg);
        } else if (msg instanceof BlocksMessage) {
            processBlocksMessage((BlocksMessage)msg);
        }
    }

    private void processChainInfoMessage(ChainInfoMessage msg) {
        chainInfoManager.update(msg.getHeight(), msg.getPreviousBlockHash(),
                msg.getCurrentBlockHash(), msg.getTotalDiff());
        changeSyncState(DONE_CHAININFO_RETRIEVING);
        listener.onChainInfoChanged(msg.getHeight(), msg.getPreviousBlockHash(),
                msg.getCurrentBlockHash(), msg.getTotalDiff());
    }

    private void startPullChainInfo() {
        if (connectionManager.isNetworkConnected()) {
            GetChainInfoMessage message = new GetChainInfoMessage();
            clientsPool.sendMessage(message);
        } else {
            logger.warn("network disconnected, change chaininfo retriving done");
            changeSyncState(DONE_CHAININFO_RETRIEVING);
        }
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

        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, change hash retriving done");
            changeSyncState(DONE_HASH_RETRIEVING);
            return;
        }

        // Note: if now is connecting blocks, just return directly.
        if (!queue.isImportingBlocksFinished()) {
            logger.warn("is connecting blocks, change hash retriving done");
            changeSyncState(DONE_HASH_RETRIEVING);
            return;
        }

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
            changeSyncState(DONE_HASH_RETRIEVING);
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

        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, discard getting block hash");
            return;
        }

        GetHashesMessage msg = new GetHashesMessage(blockNumber, maxBlocksAsk, reverse);
        clientsPool.sendMessage(msg);
    }

    private void processBlocksMessage(BlocksMessage msg) {
        if (logger.isDebugEnabled()) {
            logger.debug("Process blocks, size [{}]", msg.getBlocks().size());
        }

        List<Long> coveredNumbers = preprocessBlocksMessage(msg);

        List<Block> blocksList = msg.getBlocks();

        // return numbers not covered by response
        sentNumbers.removeAll(coveredNumbers);
        returnBlockNumbers();

        // TODO: record peer node id.
        // Here, just simply get random peer id to use orginal api.
        queue.addList(blocksList, peersManager.getRandomPeer().getId());

        if (syncState == BLOCK_RETRIEVING) {
            sendGetBlocks();
        }
    }

    private void startBlockRetrieving() {
        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, discard getting block");
            return;
        }

        sendGetBlocks();
    }

    private void sendGetBlocks() {

        List<Long> numbers = queue.pollBlockNumbers();
        if (numbers.isEmpty()) {
            if(logger.isDebugEnabled()) {
                logger.debug("No more numbers in queue, idle");
            }
            changeSyncState(IDLE);
            return;
        }

        sentNumbers.clear();
        sentNumbers.addAll(numbers);

        long start = numbers.get(0);
        if (logger.isDebugEnabled()) {
            logger.debug("Send GetBlocksMessage, numbers.count [{}], start [{}]",
                    sentNumbers.size(), start);
        }

        GetBlocksMessage msg = new GetBlocksMessage(start, sentNumbers.size(), false);

        clientsPool.sendMessage(msg);
    }

    private List<Long> preprocessBlocksMessage(BlocksMessage msg) {
        List<Long> receivedNumbers = new ArrayList<>();
        long delta = msg.getReverse() ? -1 : 1;
        long number = msg.getStartNumber();

        for (Block b : msg.getBlocks()) {
            b.setNumber(number);
            receivedNumbers.add(number);
            number += delta;
        }

        return receivedNumbers;
    }

    private void returnBlockNumbers() {
        if (logger.isDebugEnabled()) {
            logger.debug("Return [{}] numbers back to store", sentNumbers.size());
        }

        synchronized (sentNumbers) {
            queue.returnBlockNumbers(sentNumbers);
        }
        sentNumbers.clear();
    }

    @Override
    public void onMessageTimeout(Message message) {
        logger.warn("Message timeout {}", message);

        if (message instanceof GetChainInfoMessage) {
            if (getSyncState() == CHAININFO_RETRIEVING) {
                changeSyncState(DONE_CHAININFO_RETRIEVING);
            }
        } else if (message instanceof GetHashesMessage) {
            if (getSyncState() == HASH_RETRIEVING) {
                changeSyncState(DONE_HASH_RETRIEVING);
            }
        } else if (message instanceof GetBlocksMessage) {
            // Note: when 'GetBlocksMessage' is timeout, don't return
            // block numbers into syncqueue. Next 'chaininfo' will trigger
            // block sync again.
            //returnBlockNumbers();
            sentNumbers.clear();
        }
    }

    public boolean submitNewBlock(Block block) {
        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, discard block {}", block);
            return false;
        }

        this.newBlocks.add(block);
        return true;
    }

    public boolean submitNewTransaction(Transaction tx) {
        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, discard tx {}", tx);
            return false;
        }

        this.newTransactions.add(tx);
        return true;
    }

    public boolean submitNewTransaction(List<Transaction> txs) {
        if (!connectionManager.isNetworkConnected()) {
            logger.warn("network disconnected, discard tx {}", txs);
            return false;
        }

        for (Transaction tx : txs) {
            this.newTransactions.add(tx);
        }
        return true;
    }

    /**
     * Processing new blocks forged from queue
     */
    private void newBlocksDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Block block = null;
            try {
                block = newBlocks.take();
                sendNewBlock(block);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (block != null) {
                    logger.error("Error broadcasting new block {} {}: ", block, e);
                } else {
                    logger.error("Error broadcasting unknown block", e);
                }
            }
        }
    }

    /**
     * Sends all pending txs from wallet to new active peers
     */
    private void newTxDistributeLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            Transaction tx = null;
            try {
                tx = newTransactions.take();
                sendNewTransaction(tx);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (tx != null) {
                    logger.error("Error sending transaction {}: ",  e);
                } else {
                    logger.error("Unknown error when sending transaction {}", e);
                }
            }
        }
    }

    private void sendNewBlock(Block block) {
        NewBlockMessage message = new NewBlockMessage(block.getNumber(),
                block.getCumulativeDifficulty(), block);
        clientsPool.sendMessage(message);
    }

    private void sendNewTransaction(Transaction tx) {
        NewTxMessage message = new NewTxMessage(tx);
        clientsPool.sendMessage(message);
    }

    public void startPullPoolTxs(long max) {
        if (connectionManager.isNetworkConnected()) {
            GetPoolTxsMessage message = new GetPoolTxsMessage(max);
            clientsPool.sendMessage(message);
        } else {
            logger.warn("network disconnected, discard pool tx sync");
            return;
        }
    }
}
