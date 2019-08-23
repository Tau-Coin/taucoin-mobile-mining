package io.taucoin.listener;

import io.taucoin.core.*;
import io.taucoin.net.tau.message.StatusMessage;
import io.taucoin.net.message.Message;
import io.taucoin.net.p2p.HelloMessage;
import io.taucoin.net.rlpx.Node;
import io.taucoin.net.server.Channel;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Roman Mandeleil
 * @since 08.08.2014
 */
public class TaucoinListenerAdapter implements TaucoinListener {

    @Override
    public void trace(String output) {
    }

    @Override
    public void onBlock(Block block) {
    }

    @Override
    public void onBlockConnected(Block block) {
    }

    @Override
    public void onBlockDisconnected(Block block) {
    }

    @Override
    public void onRecvMessage(Channel channel, Message message) {
    }

    @Override
    public void onSendMessage(Channel channel, Message message) {
    }

    @Override
    public void onPeerDisconnect(String host, long port) {
    }

    @Override
    public void onPendingTransactionsReceived(List<Transaction> transactions) {
    }

    @Override
    public void onPendingStateChanged(PendingState pendingState) {
    }

    @Override
    public void onSyncDone() {

    }

    @Override
    public void onHandShakePeer(Channel channel, HelloMessage helloMessage) {

    }

    @Override
    public void onNoConnections() {

    }

    @Override
    public void onNodeDiscovered(Node node) {

    }

    @Override
    public void onEthStatusUpdated(Channel channel, StatusMessage statusMessage) {

    }

    @Override
    public void onPeerAddedToSyncPool(Channel peer) {

    }

    @Override
    public void onTransactionExecuated(TransactionExecuatedOutcome outcome) {

    }

    @Override
    public void onSendHttpPayload(String payload) {

    }

    @Override
    public void onRecvHttpPayload(String payload) {

    }

    @Override
    public void onChainInfoChanged(long height, byte[] previousBlockHash,
            byte[] currentBlockHash, BigInteger totalDiff, long medianFee) {

    }

    @Override
    public void onBlocksDownloaded(long from, long end) {
    }

    @Override
    public void onSyncHibernation(long number) {
    }

    @Override
    public void onBlockQueueRollback(long number) {
    }

    @Override
    public void onStatesLoaded(long hasLoaded, long total) {
    }

    @Override
    public void onStatesLoadedCompleted() {
    }
}
