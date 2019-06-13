package io.taucoin.net.tau.handler;

import io.netty.channel.ChannelHandlerContext;
import io.taucoin.core.Block;
import io.taucoin.core.BlockHeader;
import io.taucoin.core.BlockIdentifier;
import io.taucoin.core.BlockWrapper;
import io.taucoin.net.tau.message.*;
import io.taucoin.sync.SyncQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.*;

import javax.inject.Inject;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.reverse;
import static java.util.Collections.singletonList;
import static io.taucoin.net.tau.TauVersion.V62;
import static io.taucoin.sync.SyncStateName.*;
import static io.taucoin.sync.SyncStateName.BLOCK_RETRIEVING;
import static io.taucoin.util.BIUtil.isMoreThan;

/**
 * Tau 62
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class Tau62 extends TauHandler {

    private final static Logger logger = LoggerFactory.getLogger("sync");

    private static final int FORK_COVER_BATCH_SIZE = 144;

    /**
     * Header list sent in GET_BLOC_BODIES message,
     * useful if returned BLOCKS msg doesn't cover all sent hashes
     * or in case when peer is disconnected
     */
    private final List<BlockHeader> sentHeaders = Collections.synchronizedList(new ArrayList<BlockHeader>());

    private boolean commonAncestorFound = false;

    @Inject
    public Tau62() {
        super(V62);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, TauMessage msg) throws InterruptedException {

        super.channelRead0(ctx, msg);

        switch (msg.getCommand()) {
            case NEW_BLOCK_HASHES:
                processNewBlockHashes((NewBlockHashes62Message) msg);
                break;
            case BLOCK_HEADERS:
                processBlockHeaders((BlockHeadersMessage) msg);
                break;
            case GET_BLOCK_BODIES:
                processGetBlockBodies((GetBlockBodiesMessage) msg);
                break;
            case BLOCK_BODIES:
                processBlockBodies((BlockBodiesMessage) msg);
                break;
            default:
                break;
        }
    }

    @Override
    public void sendNewBlockHashes(Block block) {

        BlockIdentifier identifier = new BlockIdentifier(block.getHash(), block.getNumber());
        NewBlockHashes62Message msg = new NewBlockHashes62Message(singletonList(identifier));
        sendMessage(msg);
    }

    @Override
    public void onShutdown() {
        super.onShutdown();
        returnHeaders();
    }

    @Override
    protected void startHashRetrieving() {
        startForkCoverage();
    }

    @Override
    protected boolean startBlockRetrieving() {
        return sendGetBlockBodies();
    }

    protected void processNewBlockHashes(NewBlockHashes62Message msg) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing NewBlockHashes, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockIdentifiers().size()
        );

        List<BlockIdentifier> identifiers = msg.getBlockIdentifiers();

        if (identifiers.isEmpty()) {
            return;
        }

        this.bestHash = identifiers.get(identifiers.size() - 1).getHash();

        for (BlockIdentifier identifier : identifiers) {
            if (newBlockLowerNumber == Long.MAX_VALUE) {
                newBlockLowerNumber = identifier.getNumber();
            }
        }

        if (syncState != HASH_RETRIEVING) {
            long firstBlockNumber = identifiers.get(0).getNumber();
            long lastBlockNumber = identifiers.get(identifiers.size() - 1).getNumber();
            int maxBlocksAsk = (int) (lastBlockNumber - firstBlockNumber + 1);
            sendGetBlockHeaders(firstBlockNumber, maxBlocksAsk);
        }
    }

    protected void processBlockHeaders(BlockHeadersMessage msg) {

        // todo check if remote peer responds with same headers on different GET_BLOCK_HEADERS

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: processing BlockHeaders, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockHeaders().size()
        );

        List<BlockHeader> received = msg.getBlockHeaders();

        // treat empty headers response as end of header sync
        if (received.isEmpty()) {
            syncStats.setEmptyHashesGot();
            changeState(DONE_HASH_RETRIEVING);
        } else {
            // Anyway, firstly fillup block headers number.
            SyncQueue.fillupHeadersNumber(received, msg.getStartNumber(), msg.getLastNumber());
            syncStats.setHashesGot();
            syncStats.addHashes(received.size());

            if (syncState == HASH_RETRIEVING && !commonAncestorFound) {
                maintainForkCoverage(received);
                return;
            }

            List<BlockHeader> adding = new ArrayList<>(received.size());
            for(BlockHeader header : received) {

                adding.add(header);

                if (Arrays.equals(header.getHash(), lastHashToAsk)) {
                    changeState(DONE_HASH_RETRIEVING);
                    logger.trace("Peer {}: got terminal hash [{}]", channel.getPeerIdShort(), Hex.toHexString(lastHashToAsk));
                    break;
                }
            }

            logger.debug("Adding " + adding.size() + " headers to the queue.");
            queue.addAndValidateHeaders(adding, channel.getNodeId());
        }

        if (syncState == HASH_RETRIEVING) {
            long lastNumber = received.get(received.size() - 1).getNumber();
            sendGetBlockHeaders(lastNumber + 1, maxHashesAsk);

            queue.logHeadersSize();
        }

        if (syncState == DONE_HASH_RETRIEVING) {
            logger.info(
                    "Peer {}: header sync completed, [{}] headers in queue",
                    channel.getPeerIdShort(),
                    queue.headerStoreSize()
            );
        }
    }

    protected void processGetBlockBodies(GetBlockBodiesMessage msg) {
        List<byte[]> bodies = blockchain.getListOfBodiesByHashes(msg.getBlockHashes());

        BlockBodiesMessage response = new BlockBodiesMessage(bodies);
        sendMessage(response);
    }

    protected void processBlockBodies(BlockBodiesMessage msg) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: process BlockBodies, size [{}]",
                channel.getPeerIdShort(),
                msg.getBlockBodies().size()
        );

        List<byte[]> bodyList = msg.getBlockBodies();

        syncStats.addBlocks(bodyList.size());

        // create blocks and add them to the queue
        Iterator<byte[]> bodies = bodyList.iterator();
        Iterator<BlockHeader> headers = sentHeaders.iterator();

        List<Block> blocks = new ArrayList<>(bodyList.size());
        List<BlockHeader> coveredHeaders = new ArrayList<>(sentHeaders.size());

        while (bodies.hasNext() && headers.hasNext()) {
            BlockHeader header = headers.next();
            byte[] body = bodies.next();

            Block b = new Block.Builder()
                    .withHeader(header)
                    .withBody(body, true)
                    .create();

            if (b == null) {
                break;
            }

            logger.info("Set block number {}", header.getNumber());
            b.setNumber(header.getNumber());
            coveredHeaders.add(header);
            blocks.add(b);
        }

        // return headers not covered by response
        sentHeaders.removeAll(coveredHeaders);
        returnHeaders();

        if(!blocks.isEmpty()) {

            List<Block> regularBlocks = new ArrayList<>(blocks.size());

            for (Block block : blocks) {
                if (block.getNumber() < newBlockLowerNumber) {
                    regularBlocks.add(block);
                } else {
                    queue.addNew(block, channel.getNodeId());
                }
            }

            queue.addList(regularBlocks, channel.getNodeId());
            queue.logHeadersSize();
        } else {
            changeState(BLOCKS_LACK);
        }

        if (syncState == BLOCK_RETRIEVING) {
            sendGetBlockBodies();
        }
    }

    protected void sendGetBlockHeaders(long blockNumber, int maxBlocksAsk) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send GetBlockHeaders, blockNumber [{}], maxBlocksAsk [{}]",
                channel.getPeerIdShort(),
                blockNumber,
                maxBlocksAsk
        );

        GetBlockHeadersMessage msg = new GetBlockHeadersMessage(blockNumber, maxBlocksAsk);

        sendMessage(msg);
    }

    protected void sendGetBlockHeaders(byte[] blockHash, int maxBlocksAsk, int skip, boolean reverse) {

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send GetBlockHeaders, blockHash [{}], maxBlocksAsk [{}], skip[{}], reverse [{}]",
                channel.getPeerIdShort(),
                "0x" + Hex.toHexString(blockHash).substring(0, 8),
                maxBlocksAsk, skip, reverse
        );

        GetBlockHeadersMessage msg = new GetBlockHeadersMessage(0, blockHash, maxBlocksAsk, skip, reverse);

        sendMessage(msg);
    }

    protected boolean sendGetBlockBodies() {

        List<BlockHeader> headers = queue.pollHeaders();
        if (headers.isEmpty()) {
            if(logger.isInfoEnabled()) logger.trace(
                    "Peer {}: no more headers in queue, idle",
                    channel.getPeerIdShort()
            );
            changeState(IDLE);
            return false;
        }

        sentHeaders.clear();
        sentHeaders.addAll(headers);

        if(logger.isTraceEnabled()) logger.trace(
                "Peer {}: send GetBlockBodies, hashes.count [{}]",
                channel.getPeerIdShort(),
                sentHeaders.size()
        );

        List<byte[]> hashes = new ArrayList<>(headers.size());
        for (BlockHeader header : headers) {
            hashes.add(header.getHash());
        }

        GetBlockBodiesMessage msg = new GetBlockBodiesMessage(hashes);

        sendMessage(msg);

        return true;
    }

    private void returnHeaders() {
        if(logger.isDebugEnabled()) logger.debug(
                "Peer {}: return [{}] headers back to store",
                channel.getPeerIdShort(),
                sentHeaders.size()
        );

        synchronized (sentHeaders) {
            queue.returnHeaders(sentHeaders);
        }

        sentHeaders.clear();
    }

    /*************************
     *     Fork Coverage     *
     *************************/


    private void startForkCoverage() {

        commonAncestorFound = false;

        if (isNegativeGap()) {

            logger.trace("Peer {}: start fetching remote fork", channel.getPeerIdShort());
            BlockWrapper gap = syncManager.getGapBlock();
            sendGetBlockHeaders(gap.getHash(), FORK_COVER_BATCH_SIZE, 0, true);
            return;
        }

        logger.trace("Peer {}: start looking for common ancestor", channel.getPeerIdShort());

        long bestNumber = blockchain.getBestBlock().getNumber();
        long blockNumber = max(0, bestNumber - FORK_COVER_BATCH_SIZE + 1);
        sendGetBlockHeaders(blockNumber, min(FORK_COVER_BATCH_SIZE, (int) (bestNumber - blockNumber + 1)));
    }

    private void maintainForkCoverage(List<BlockHeader> received) {

        if (!isNegativeGap()) reverse(received);

        ListIterator<BlockHeader> it = received.listIterator();
        // start downloading hashes from blockNumber of the block with known hash
        List<BlockHeader> headers = new ArrayList<>();

        if (isNegativeGap()) {

            BlockWrapper gap = syncManager.getGapBlock();

            // gap block didn't come, drop remote peer
            BlockHeader gapHeader = it.next();
            if (!Arrays.equals(gapHeader.getHash(), gap.getHash())) {

                logger.trace("Peer {}: gap block is missed in response, drop", channel.getPeerIdShort());
                syncManager.reportBadAction(channel.getNodeId());
                return;
            }

            headers.add(gapHeader);
        }

        while (it.hasNext()) {
            BlockHeader header = it.next();
            logger.info("isBlockExist {}", Hex.toHexString(header.getHash()));
            if (blockchain.isBlockExist(header.getHash())) {
                commonAncestorFound = true;
                logger.trace(
                        "Peer {}: common ancestor found: block.number {}, block.hash {}",
                        channel.getPeerIdShort(),
                        header.getNumber(),
                        Hex.toHexString(header.getHash())
                );

                break;
            }
            headers.add(header);
        }

        if (!commonAncestorFound) {

            logger.trace("Peer {}: common ancestor is not found, drop", channel.getPeerIdShort());
            syncManager.reportBadAction(channel.getNodeId());
            return;
        }

        // add missed headers
        queue.addAndValidateHeaders(headers, channel.getNodeId());

        if (isNegativeGap()) {

            // fork headers should already be fetched here
            logger.trace("Peer {}: remote fork is fetched", channel.getPeerIdShort());
            changeState(DONE_HASH_RETRIEVING);
            return;
        }

        // start header sync
        sendGetBlockHeaders(blockchain.getBestBlock().getNumber() + 1, maxHashesAsk);
    }

    private boolean isNegativeGap() {

        if (syncManager.getGapBlock() == null) return false;

        return syncManager.getGapBlock().getNumber() <= blockchain.getBestBlock().getNumber();
    }
}
