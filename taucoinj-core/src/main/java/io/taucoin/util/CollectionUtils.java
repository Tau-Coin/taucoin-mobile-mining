package io.taucoin.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mikhail Kalinin
 * @since 14.07.2015
 */
public class CollectionUtils {

    public static <K, V> List<V> collectList(Collection<K> items, Functional.Function<K, V> collector) {
        List<V> collected = new ArrayList<>(items.size());
        for(K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <K, V> Set<V> collectSet(Collection<K> items, Functional.Function<K, V> collector) {
        Set<V> collected = new HashSet<>();
        for(K item : items) {
            collected.add(collector.apply(item));
        }
        return collected;
    }

    public static <T> List<T> truncate(List<T> items, int limit) {
        if(limit > items.size()) {
            return new ArrayList<>(items);
        }
        List<T> truncated = new ArrayList<>(limit);
        for(T item : items) {
            truncated.add(item);
            if(truncated.size() == limit) {
                break;
            }
        }
        return truncated;
    }

    public static <T> List<T> selectList(Collection<T> items, Functional.Predicate<T> predicate) {
        List<T> selected = new ArrayList<>();
        for(T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static <T> Set<T> selectSet(Collection<T> items, Functional.Predicate<T> predicate) {
        Set<T> selected = new HashSet<>();
        for(T item : items) {
            if(predicate.test(item)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static <T> List<T> truncateRand(final List<T> items, int limit) {
        if(limit > items.size()) {
            return new ArrayList<>(items);
        }
        List<T> truncated = new ArrayList<>(limit);

        LinkedList<Integer> index = new LinkedList<>();
        for (int i = 0; i < items.size(); ++i) {
            index.add(i);
        }

        if (limit * 2 < items.size()) {
            // Limit is very small comparing to items.size()
            Set<Integer> smallIndex = new HashSet<>();
            for (int i = 0; i < limit; ++i) {
                int randomNum = ThreadLocalRandom.current().nextInt(0, index.size());
                smallIndex.add(index.remove(randomNum));
            }

            for (int i : smallIndex) {
                truncated.add(items.get(i));
            }
        } else {
            // Limit is compared to items.size()
            for (int i = 0; i < items.size() - limit; ++i) {
                int randomNum = ThreadLocalRandom.current().nextInt(0, index.size());
                index.remove(randomNum);
            }

            for (int i : index) {
                truncated.add(items.get(i));
            }
        }

        return truncated;
    }
}
