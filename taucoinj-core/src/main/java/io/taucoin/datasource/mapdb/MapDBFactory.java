package io.taucoin.datasource.mapdb;

import io.taucoin.datasource.KeyValueDataSource;
import org.mapdb.DB;

public interface MapDBFactory {

    KeyValueDataSource createDataSource();

    DB createDB(String name);

    DB createTransactionalDB(String name);
}
