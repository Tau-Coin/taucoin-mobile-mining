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

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "RequestQueueTimer-" + cnt.getAndIncrement());
        }
    });

    private Queue<RequestRoundtrip> requestQueue = new ConcurrentLinkedQueue<>();

    TaucoinListener ethereumListener;
    private ScheduledFuture<?> timerTask;

    private List<MessageListener> listeners = new ArrayList<>();

    private ChannelHandlerContext ctx = null;

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
        this.ctx = ctx;
        timerTask = timer.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    nudgeQueue();
                } catch (Throwable t) {
                    logger.error("Unhandled exception ", t);
                }
            }
        }, 250, 250, TimeUnit.MILLISECONDS);
    }

    public void sendMessage(Message msg) {
        requestQueue.add(new RequestRoundtrip(msg));
    }

    public int size() {
        return requestQueue.size();
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
        if (this.ctx == null) {
            logger.warn("channel deactived");
            return;
        }

        // remove timeout message
        removeTimeoutMessage(requestQueue.peek());
        // remove last answered message on the queue
        removeAnsweredMessage(requestQueue.peek());
        // Now send the next message
        sendToWire(requestQueue.peek());
    }

    private void sendToWire(RequestRoundtrip requestRoundtrip) {

        if (requestRoundtrip != null && (requestRoundtrip.getRetryTimes() == 0
                || requestRoundtrip.hasToRetry())) {

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
        this.ctx = null;
        if (timerTask != null) {
            timerTask.cancel(true);
        }
    }

    public interface MessageListener {
        void onMessageTimeout(Message message);
    }
}
