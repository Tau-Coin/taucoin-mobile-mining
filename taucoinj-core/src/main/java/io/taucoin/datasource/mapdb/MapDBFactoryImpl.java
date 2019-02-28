package io.taucoin.datasource.mapdb;

import io.taucoin.config.SystemProperties;
import io.taucoin.datasource.KeyValueDataSource;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;

import javax.inject.Singleton;

import static java.lang.System.getProperty;

@Singleton
public class MapDBFactoryImpl implements MapDBFactory {

    @Override
    public KeyValueDataSource createDataSource() {
        return new MapDBDataSource();
    }

    @Override
    public DB createDB(String name) {
        return createDB(name, false);
    }

    @Override
    public DB createTransactionalDB(String name) {
        return createDB(name, true);
    }

    private DB createDB(String name, boolean transactional) {
        File dbFile = new File(name);
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
        DBMaker.Maker dbMaker = DBMaker.fileDB(dbFile)
                .closeOnJvmShutdown();
        if (!transactional) {
            dbMaker.transactionDisable();
        }
        return dbMaker.make();
    }
}
