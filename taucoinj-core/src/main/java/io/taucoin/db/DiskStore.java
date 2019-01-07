package io.taucoin.db;

/**
 * @author Mikhail Kalinin
 * @since 09.07.2015
 * @author taucoin core
 * @since 01.07.2019
 */
public interface DiskStore {

    void open();

    void close();
}
