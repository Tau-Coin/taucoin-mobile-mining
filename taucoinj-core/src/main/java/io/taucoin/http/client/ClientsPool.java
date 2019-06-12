package io.taucoin.http.client;

import io.taucoin.http.message.Message;
import io.taucoin.http.RequestManager;
import io.taucoin.http.RequestQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static io.taucoin.config.SystemProperties.CONFIG;

@Singleton
public class ClientsPool {

    private final static Logger logger = LoggerFactory.getLogger("http");

    private Provider<HttpClient> clientProvider;
    private Provider<RequestQueue> queueProvider;
    private RequestManager requestManager;

    private List<HttpClient> pool = Collections.synchronizedList(new ArrayList<HttpClient>());
    private AtomicInteger index = new AtomicInteger(0);

    private HttpClient mainHttpClient;

    @Inject
    public ClientsPool(Provider<HttpClient> clientProvider,
            Provider<RequestQueue> queueProvider) {
        this.clientProvider = clientProvider;
        this.queueProvider = queueProvider;
    }

    public void setRequestManager(RequestManager requestManager) {
        this.requestManager = requestManager;
        init();
    }

    private void init() {
        int n = CONFIG.httpClientPoolSize();
        for (int i = 0; i < n; i++) {
            RequestQueue queue = queueProvider.get();
            queue.registerListener(requestManager);
            HttpClient client = clientProvider.get();
            client.setRequestQueue(queue);
            pool.add(client);
        }

        mainHttpClient = pool.get(0);
    }

    public synchronized void sendMessage(Message message) {
        mainHttpClient.sendRequest(message);
    }

    public synchronized void close() {
        if (mainHttpClient != null) {
            mainHttpClient.close();
        }

        pool.clear();
    }
}
