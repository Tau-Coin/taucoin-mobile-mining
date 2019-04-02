package io.taucoin.http.client;

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

    private Provider<HttpClient> provider;

    private List<HttpClient> pool = Collections.synchronizedList(new ArrayList<HttpClient>());
    private AtomicInteger index = new AtomicInteger(0);

    @Inject
    public ClientsPool(Provider<HttpClient> provider) {
        this.provider = provider;
        init();
    }

    private void init() {
        int n = CONFIG.httpClientPoolSize();
        for (int i = 0; i < n; i++) {
            pool.add(this.provider.get());
        }
    }

    // Retrive next idle client
    public synchronized HttpClient next() {
        while (true) {
            if (pool.get(index.get()).compareAndSetIdle(true, false)) {
                logger.debug("{}th client hit", index.get());
                return pool.get(index.get());
            } else {
                index.set(index.addAndGet(1) % pool.size());
            }
        }
    }
}
