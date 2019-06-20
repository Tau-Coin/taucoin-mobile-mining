package io.taucoin.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.taucoin.config.SystemProperties.CONFIG;

/**
 * @author Taucoin Core Devlopers
 * @since 03.04.2019
 */
public class BlockNumberStoreMem implements BlockNumberStore {

    private static final Logger logger = LoggerFactory.getLogger("blockqueue");

    private BlockQueueImpl.Index numbers = new BlockQueueImpl.ArrayListIndex(Collections.<Long>emptySet());

    private final Object mutex = new Object();

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public void add(Long number) {
        synchronized (mutex) {
            if (!numbers.contains(number)) {
                numbers.add(number);
            }
        }
    }

    @Override
    public void addBatch(Collection<Long> numbers) {
        synchronized (mutex) {
            List<Long> numbersList = new ArrayList<>(numbers.size());
            for (Long n : numbers) {
                if (!this.numbers.contains(n)) {
                    numbersList.add(n);
                }
            }

            this.numbers.addAll(numbersList);
        }
    }

    @Override
    public void addBatch(Long startNumber, Long endNumber) {
        synchronized (mutex) {
            long start = startNumber;
            while (start <= endNumber) {
                if (!this.numbers.contains(start)) {
                    this.numbers.add(start);
                }
                start++;
            }
        }
    }


    @Override
    public Long peek() {
        synchronized (mutex) {
            if(this.numbers.isEmpty()) {
                return null;
            }

            Long n = this.numbers.peek();
            return n;
        }
    }

    @Override
    public Long poll() {
        synchronized (mutex) {
            if(this.numbers.isEmpty()) {
                return null;
            }

            Long n = this.numbers.poll();
            return n;
        }
    }

    @Override
    public List<Long> pollBatch(int qty) {
        synchronized (mutex) {
            if (this.numbers.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> numbers = new ArrayList<>();
            if (qty > size()) {
                qty = size();
            }

            // We only poll continuously numbers;
            Long prevNumber = this.numbers.poll();
            numbers.add(prevNumber);
            qty -= 1;

            while (numbers.size() < qty) {
                Long n = this.numbers.poll();
                if (n == prevNumber + 1) {
                    numbers.add(n);
                    prevNumber = n;
                } else {
                    logger.info("Not continuously numbers prev {} current {}",
                            prevNumber, n);
                    add(n);
                    break;
                }
            }

            return numbers;
        }
    }

    @Override
    public boolean isEmpty() {
        return this.numbers.isEmpty();
    }

    @Override
    public int size() {
        return this.numbers.size();
    }

    @Override
    public void clear() {
        this.numbers.clear();
    }
}
