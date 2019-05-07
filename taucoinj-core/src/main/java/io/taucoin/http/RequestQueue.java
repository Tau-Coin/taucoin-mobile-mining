package io.taucoin.http;

import io.taucoin.http.client.ClientsPool;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.http.message.Message;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class contains the logic for sending messages in a queue
 *
 * Messages open by send and answered by receive of appropriate message
 *      GetChainInfoMessage by ChainInfoMessage
 *      GetHashesMessage by HashesMessage
 *      GetBlocksMessage by BlocksMessage
 *      GetPoolTxsMessage by PoolTxsMessage
 *
 * The following messages will not be answered, but DummyMessage will be received.
 *      NewBlockMessage, NewTxMessage
 *
 * @author Taucoin Core Developers
 */
@Singleton
public class RequestQueue {

    private static final Logger logger = LoggerFactory.getLogger("http");

    private ScheduledExecutorService timer = null;

    // If two many messages were queued and can't connect to remote peer, clear all.
    public static final int CAPABILITY = 100;

    private Queue<RequestRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();

    TaucoinListener ethereumListener;
    private ScheduledFuture<?> timerTask;

    private List<MessageListener> listeners = new ArrayList<>();

    private ChannelHandlerContext ctx = null;
    private final Object ctxLock = new Object();

    @Inject
    public RequestQueue(TaucoinListener listener) {
        this.ethereumListener = listener;
    }

    public void registerListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListenersTimeout(Message message) {
        for (MessageListener l : listeners) {
            l.onMessageTimeout(message);
        }
    }

    public void activate(ChannelHandlerContext ctx) {

        removeAllTimeoutMessage();

        synchronized(ctxLock) {
            this.ctx = ctx;

            timer = Executors.newScheduledThreadPool(0);

            timerTask = timer.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        nudgeQueue();
                    } catch (Throwable t) {
                        logger.error("Unhandled exception ", t);
                    }
                }
            }, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    public void sendMessage(Message msg) {
        requestQueue.add(new RequestRoundtrip(msg));
    }

    public int size() {
        int size = requestQueue.size();
        logger.info("Request queue size {}", size);
        return size;
    }

    public void clear() {
        requestQueue.clear();
    }

    public void removeAllTimeoutMessage() {
        while (requestQueue.peek() != null) {
            RequestRoundtrip requestRoundtrip = requestQueue.peek();
            if (requestRoundtrip.isTimeout()) {
                requestQueue.remove();
                notifyListenersTimeout(requestRoundtrip.getMsg());
            } else {
                break;
            }
        }
    }

    public void receivedMessage(Message msg) throws InterruptedException {

        ethereumListener.trace("[Recv: " + msg + "]");
        logger.info("Recv message {}", msg);

        if (requestQueue.peek() != null) {
            RequestRoundtrip requestRoundtrip = requestQueue.peek();
            Message waitingMessage = requestRoundtrip.getMsg();

            if (waitingMessage.getAnswerMessage() != null
                    && msg.getClass() == waitingMessage.getAnswerMessage()) {
                requestRoundtrip.answer();
                requestQueue.remove();
                logger.trace("Message round trip covered: [{}] ",
                        requestRoundtrip.getMsg().getClass());
            }
        }
    }

    private void removeTimeoutMessage(RequestRoundtrip requestRoundtrip) {
        if (requestRoundtrip != null && requestRoundtrip.isTimeout()) {
            requestQueue.remove();
            notifyListenersTimeout(requestRoundtrip.getMsg());
        }
    }

    private void removeAnsweredMessage(RequestRoundtrip requestRoundtrip) {
        if (requestRoundtrip != null && requestRoundtrip.isAnswered())
            requestQueue.remove();
    }

    private void nudgeQueue() {

        if (Thread.interrupted()) {
            logger.warn("Timer is interrupted");
            return;
        }

        synchronized(ctxLock) {
            if (this.ctx == null) {
                logger.warn("Interrupt timer task");
                Thread.currentThread().interrupt();
                //return;
            }
        }

        // remove timeout message
        removeTimeoutMessage(requestQueue.peek());
        // remove last answered message on the queue
        //removeAnsweredMessage(requestQueue.peek());
        // Now send the next message
        sendToWire(requestQueue.peek());
    }

    private void sendToWire(RequestRoundtrip requestRoundtrip) {

        if (requestRoundtrip != null && (requestRoundtrip.getRetryTimes() == 0
                /*|| requestRoundtrip.hasToRetry()*/)) {

            Message msg = requestRoundtrip.getMsg();

            //ethereumListener.onSendMessage(channel, msg);

            // send this request
            ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (msg.getAnswerMessage() == null)
                requestQueue.remove();
            else {
                requestRoundtrip.incRetryTimes();
                requestRoundtrip.saveTime();
            }
        }
    }

    public void deactivate() {
        synchronized(ctxLock) {
            this.ctx = null;

            if (timerTask != null && timer != null) {
                timerTask.cancel(true);
                timerTask = null;
                timer.shutdownNow();
                timer = null;
                System.gc();
            }
        }
    }

    public interface MessageListener {
        void onMessageTimeout(Message message);
    }
}
