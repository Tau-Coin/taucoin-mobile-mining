package io.taucoin.net.tau.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.taucoin.config.SystemProperties;
import io.taucoin.core.*;
import io.taucoin.db.BlockStore;
import io.taucoin.listener.CompositeTaucoinListener;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.listener.TaucoinListenerAdapter;
import io.taucoin.net.server.ChannelManager;
import io.taucoin.net.submit.TransactionExecutor;
import io.taucoin.net.submit.TransactionTask;
import io.taucoin.sync.SyncManager;
import io.taucoin.sync.SyncQueue;
import io.taucoin.net.MessageQueue;
import io.taucoin.net.submit.NewBlockHeaderBroadcaster;
import io.taucoin.net.submit.NewBlockHeaderTask;
import io.taucoin.net.submit.TransactionExecutor;
import io.taucoin.net.submit.TransactionTask;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.tau.message.*;
import io.taucoin.sync.SyncStateName;
import io.taucoin.sync.SyncStatistics;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.server.Channel;
import io.taucoin.util.BIUtil;
import io.taucoin.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;

import java.math.BigInteger;
import java.util.*;

import static io.taucoin.config.SystemProperties.CONFIG;
import static io.taucoin.sync.SyncStateName.*;
import static io.taucoin.util.BIUtil.isLessThan;

/**
 * Process the messages between peers with 'eth' capability on the network<br>
 * Contains common logic to all supported versions
 * delegating version specific stuff to its descendants
 *
 * Peers with 'eth' capability can send/receive:
 * <ul>
 * <li>STATUS                           :   Announce their status to the peer</li>
 * <li>NEW_BLOCK_HASHES                 :   Send a list of NEW block hashes</li>
 * <li>TRANSACTIONS                     :   Send a list of pending transactions</li>
 * <li>GET_BLOCK_HASHES                 :   Request a list of known block hashes</li>
 * <li>BLOCK_HASHES                     :   Send a list of known block hashes</li>
 * <li>GET_BLOCKS                       :   Request a list of blocks</li>
 * <li>BLOCKS                           :   Send a list of blocks</li>
 * <li>GET_BLOCK_HASHES_BY_NUMBER       :   Request list of know block hashes starting from the block</li>
 * </ul>
 */
public abstract class TauHandler extends SimpleChannelInboundHandler<TauMessage> implements Tau {

    private final static Logger loggerNet = LoggerFactory.getLogger("net");
    private final static Logger loggerSync = LoggerFactory.getLogger("sync");

    protected static final int MAX_HASHES_TO_SEND = 65536;

    protected SystemProperties config = SystemProperties.CONFIG;

    protected Blockchain blockchain;

    protected BlockStore blockstore;

    protected SyncManager syncManager;

    protected SyncQueue queue;

    protected CompositeTaucoinListener ethereumListener;

    protected Wallet wallet;

    protected PendingState pendingState;

    protected ChannelManager channelManager;

    protected Channel channel;

    private MessageQueue msgQueue = null;

    protected TauVersion version;
    protected EthState ethState = EthState.INIT;

    protected boolean peerDiscoveryMode = false;

    private static final int BLOCKS_LACK_MAX_HITS = 5;
    private int blocksLackHits = 0;

    protected SyncStateName syncState = IDLE;
    protected boolean syncDone = false;
    protected boolean processTransactions = false;

    protected byte[] bestHash;

    private Block bestBlock;
    private TaucoinListener listener = new TaucoinListenerAdapter() {
        @Override
        public void onBlock(Block block) {
            bestBlock = block;
        }
    };

    /**
     * Last block hash to be asked from the peer,
     * its usage depends on Tau version
     *
     * @see Tau60
     * @see Tau61
     * @see Tau62
     */
    protected byte[] lastHashToAsk;
    protected int maxHashesAsk;

    protected final SyncStatistics syncStats = new SyncStatistics();

    /**
     * The number above which blocks are treated as NEW,
     * filled by data gained from NewBlockHashes and NewBlock messages
     */
    protected long newBlockLowerNumber = Long.MAX_VALUE;

    protected TauHandler(TauVersion version) {
        this.version = version;
    }

    public void init(Blockchain blockchain, BlockStore blockstore, SyncManager syncManager,
            SyncQueue queue,
            Wallet wallet, PendingState pendingState, ChannelManager channelManager) {
        this.blockchain = blockchain;
        this.blockstore = blockstore;
        this.syncManager = syncManager;
        this.queue = queue;
        this.wallet = wallet;
        this.pendingState = pendingState;
        this.channelManager = channelManager;
        this.ethereumListener = (CompositeTaucoinListener)channelManager.getListener();

        maxHashesAsk = config.maxHashesAsk();
        bestBlock = blockchain.getBestBlock();
        ethereumListener.addListener(listener);
        // when sync enabled we delay transactions processing until sync is complete
        processTransactions = !config.isSyncEnabled();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TauMessage msg) throws InterruptedException {

        if (TauMessageCodes.inRange(msg.getCommand().asByte(), version))
            loggerNet.trace("TauHandler invoke: [{}]", msg.getCommand());

        ethereumListener.trace(String.format("TauHandler invoke: [%s]", msg.getCommand()));

        channel.getNodeStatistics().ethInbound.add();

        msgQueue.receivedMessage(msg);

        switch (msg.getCommand()) {
            case STATUS:
                processStatus((StatusMessage) msg, ctx);
                break;
            case TRANSACTIONS:
                processTransactions((TransactionsMessage) msg);
                break;
            case NEW_BLOCK:
                processNewBlock((NewBlockMessage) msg);
                break;
            case NEW_BLOCK_HEADER:
                processNewBlockHeader((NewBlockHeaderMessage)msg);
                break;
            default:
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        loggerNet.error("Eth handling failed", cause);
        ctx.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        loggerNet.debug("handlerRemoved: kill timers in TauHandler");
        ethereumListener.removeListener(listener);
        onShutdown();
    }

    public void activate() {
        loggerNet.info("ETH protocol activated");
        ethereumListener.trace("ETH protocol activated");
        sendStatus();
    }

    protected void disconnect(ReasonCode reason) {
        msgQueue.disconnect(reason);
        channel.getNodeStatistics().nodeDisconnectedLocal(reason);
    }

    /**
     * Checking if peer is using the same genesis, protocol and network</li>
     *
     * @param msg is the StatusMessage
     * @param ctx the ChannelHandlerContext
     */
    private void processStatus(StatusMessage msg, ChannelHandlerContext ctx) throws InterruptedException {
        channel.getNodeStatistics().ethHandshake(msg);
        ethereumListener.onEthStatusUpdated(channel, msg);

        try {
            if (!Arrays.equals(msg.getGenesisHash(), config.getGenesis().getHash())
                    || msg.getProtocolVersion() != version.getCode()) {
                loggerNet.info("Removing TauHandler for {} due to protocol incompatibility", ctx.channel().remoteAddress());
                ethState = EthState.STATUS_FAILED;
                disconnect(ReasonCode.INCOMPATIBLE_PROTOCOL);
                ctx.pipeline().remove(this); // Peer is not compatible for the 'eth' sub-protocol
                return;
            } else if (msg.getNetworkId() != config.networkId()) {
                ethState = EthState.STATUS_FAILED;
                disconnect(ReasonCode.NULL_IDENTITY);
                return;
            } else if (peerDiscoveryMode) {
                loggerNet.debug("Peer discovery mode: STATUS received, disconnecting...");
                disconnect(ReasonCode.REQUESTED);
                ctx.close().sync();
                ctx.disconnect().sync();
                return;
            }
        } catch (NoSuchElementException e) {
            loggerNet.debug("TauHandler already removed");
            return;
        }

        ethState = EthState.STATUS_SUCCEEDED;

        bestHash = msg.getBestHash();
    }

    protected void sendStatus() {
        byte protocolVersion = version.getCode();
        int networkId = config.networkId();

        BigInteger totalDifficulty = blockchain.getTotalDifficulty();
        byte[] bestHash = bestBlock.getHash();
        StatusMessage msg = new StatusMessage(protocolVersion, networkId,
                ByteUtil.bigIntegerToBytes(totalDifficulty), bestHash, config.getGenesis().getHash());
        sendMessage(msg);
    }

    /*
     * The wire gets data for signed transactions and
     * sends it to the net.
     */
    @Override
    public void sendTransaction(List<Transaction> txs) {
        TransactionsMessage msg = new TransactionsMessage(txs);
        sendMessage(msg);
    }

    private void processTransactions(TransactionsMessage msg) {
        if(!processTransactions) {
            return;
        }

        List<Transaction> txList = msg.getTransactions();
        Set<Transaction>  txSet = new HashSet<Transaction>();
        for (Transaction tx : txList) {
            txSet.add(tx);
        }
        List<Transaction> txListBroadcasted = pendingState.addWireTransactions(txSet);

        // broadcast transactions only after tx is verified.
        if (!txListBroadcasted.isEmpty()) {
            TransactionTask transactionTask = new TransactionTask(txListBroadcasted, channelManager, channel);
            TransactionExecutor.instance.submitTransaction(transactionTask);
        }
    }

    public void sendNewBlock(Block block) {
        BigInteger parentTD = blockstore.getTotalDifficultyForHash(block.getPreviousHeaderHash());
        byte[] td = ByteUtil.bigIntegerToBytes(parentTD.add(new BigInteger(1, block.getCumulativeDifficulty().toByteArray())));
        NewBlockMessage msg = new NewBlockMessage(block, td);
        sendMessage(msg);
    }

    public void sendNewBlockHeader(BlockHeader header) {
        NewBlockHeaderMessage msg = new NewBlockHeaderMessage(header);
        sendMessage(msg);
    }

    public abstract void sendNewBlockHashes(Block block);

    private void processNewBlock(NewBlockMessage newBlockMessage) {

        Block newBlock = newBlockMessage.getBlock();

//        loggerSync.info("New block received: block.index [{}]", newBlock.getNumber());

        // skip new block if TD is lower than ours
        if (isLessThan(newBlockMessage.getDifficultyAsBigInt(), blockchain.getTotalDifficulty())) {
            loggerSync.trace(
                    "New block difficulty lower than ours: [{}] vs [{}], skip",
                    newBlockMessage.getDifficultyAsBigInt(),
                    blockchain.getTotalDifficulty()
            );
            return;
        }

        channel.getNodeStatistics().setEthTotalDifficulty(newBlockMessage.getDifficultyAsBigInt());
        bestHash = newBlock.getHash();

        // adding block to the queue
        // there will be decided how to
        // connect it to the chain
        queue.addNew(newBlock, channel.getNodeId());

//        if (newBlockLowerNumber == Long.MAX_VALUE) {
//            newBlockLowerNumber = newBlock.getNumber();
//        }
    }

    private void processNewBlockHeader(NewBlockHeaderMessage msg) {

        if(loggerNet.isTraceEnabled()) loggerNet.trace(
                "Peer {}: processing NewBlockHashes, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockHeader().toFlatString()
        );

        BlockHeader header = msg.getBlockHeader();
        if (header == null) {
            loggerNet.error("Peer {}: empty new block header");
            // TODO: ban this channel
            return;
        }

        List<BlockHeader> adding = new ArrayList<>();
        adding.add(header);
        loggerNet.debug("Adding " + adding.size() + " headers to the queue.");
        List<BlockHeader> added = queue.addAndValidateHeaders(adding, channel.getNodeId());

        if (!added.isEmpty()) {
            // broadcast this new blockheader
            NewBlockHeaderTask task = new NewBlockHeaderTask(header, channelManager, channel);
            NewBlockHeaderBroadcaster.instance.submitNewBlockHeader(task);
        }

        // Added this block header into syncqueue and sync state machine will
        // automatically get the block body.
        // New block header is designed to broadcast current forging basetarget,
        // forging power and other forge info.
        // TODO: update forging infomation.
    }

    protected void sendMessage(TauMessage message) {
        msgQueue.sendMessage(message);
        channel.getNodeStatistics().ethOutbound.add();
    }

    abstract protected void startHashRetrieving();

    abstract protected boolean startBlockRetrieving();

    @Override
    public void changeState(SyncStateName newState) {
        if (syncState == newState) {
            return;
        }

        loggerSync.trace(
                "Peer {}: changing state from {} to {}",
                channel.getPeerIdShort(),
                syncState,
                newState
        );

        if (newState == HASH_RETRIEVING) {
            if (syncStats.isEmptyHashesGotTimeout()) {
                syncStats.reset();
                startHashRetrieving();
            } else {
                loggerSync.info("Peer {} ignore hash retrieving, please wait {}s.",
                        channel.getPeerIdShort(), syncStats.secondsSinceLastEmptyHashes());
                changeState(DONE_HASH_RETRIEVING);
                return;
            }
        }
        if (newState == BLOCK_RETRIEVING) {
            syncStats.reset();
            boolean started = startBlockRetrieving();
            if (!started) {
                newState = IDLE;
            }
        }
        if (newState == BLOCKS_LACK) {
            if (syncDone || ++blocksLackHits < BLOCKS_LACK_MAX_HITS) {
                return;
            }
            blocksLackHits = 0; // reset
        }
        syncState = newState;
    }

    @Override
    public boolean isHashRetrievingDone() {
        return syncState == DONE_HASH_RETRIEVING;
    }

    @Override
    public boolean isHashRetrieving() {
        return syncState == HASH_RETRIEVING;
    }

    @Override
    public boolean hasBlocksLack() {
        return syncState == BLOCKS_LACK;
    }

    @Override
    public boolean hasStatusPassed() {
        return ethState != EthState.INIT;
    }

    @Override
    public boolean hasStatusSucceeded() {
        return ethState == EthState.STATUS_SUCCEEDED;
    }

    @Override
    public void onShutdown() {
        changeState(IDLE);
    }

    @Override
    public void logSyncStats() {
        if(!loggerSync.isInfoEnabled()) {
            return;
        }
        switch (syncState) {
            case BLOCK_RETRIEVING: loggerSync.info(
                    "Peer {}: [ {}, state {}, blocks count {} ]",
                    version,
                    channel.getPeerIdShort(),
                    syncState,
                    syncStats.getBlocksCount()
            );
                break;
            case HASH_RETRIEVING: loggerSync.info(
                    "Peer {}: [ {}, state {}, hashes count {} ]",
                    version,
                    channel.getPeerIdShort(),
                    syncState,
                    syncStats.getHashesCount()
            );
                break;
            default: loggerSync.info(
                    "Peer {}: [ {}, state {} ]",
                    version,
                    channel.getPeerIdShort(),
                    syncState
            );
        }
    }

    @Override
    public boolean isIdle() {
        return syncState == IDLE;
    }

    @Override
    public byte[] getBestKnownHash() {
        return bestHash;
    }

    @Override
    public void setMaxHashesAsk(int maxHashesAsk) {
        this.maxHashesAsk = maxHashesAsk;
    }

    @Override
    public int getMaxHashesAsk() {
        return maxHashesAsk;
    }

    @Override
    public void setLastHashToAsk(byte[] lastHashToAsk) {
        this.lastHashToAsk = lastHashToAsk;
    }

    @Override
    public byte[] getLastHashToAsk() {
        return lastHashToAsk;
    }

    @Override
    public void enableTransactions() {
        processTransactions = true;
    }

    @Override
    public void disableTransactions() {
        processTransactions = false;
    }

    @Override
    public SyncStatistics getStats() {
        return syncStats;
    }

    @Override
    public TauVersion getVersion() {
        return version;
    }

    @Override
    public void onSyncDone() {
        syncDone = true;
    }

    public StatusMessage getHandshakeStatusMessage() {
        return channel.getNodeStatistics().getEthLastInboundStatusMsg();
    }

    public void setMsgQueue(MessageQueue msgQueue) {
        this.msgQueue = msgQueue;
    }

    public void setPeerDiscoveryMode(boolean peerDiscoveryMode) {
        this.peerDiscoveryMode = peerDiscoveryMode;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    enum EthState {
        INIT,
        STATUS_SUCCEEDED,
        STATUS_FAILED
    }
}
