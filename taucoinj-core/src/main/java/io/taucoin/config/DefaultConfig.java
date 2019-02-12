package io.taucoin.config;

import io.taucoin.datasource.HashMapDB;
import io.taucoin.datasource.KeyValueDataSource;
import io.taucoin.datasource.LevelDbDataSource;
import io.taucoin.db.BlockStore;
import io.taucoin.db.IndexedBlockStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.taucoin.db.IndexedBlockStore.BLOCK_INFO_SERIALIZER;

/**
 *
 * @author Roman Mandeleil
 * Created on: 27/01/2015 01:05
 */
public class DefaultConfig {
    private static Logger logger = LoggerFactory.getLogger("general");

 
    //ApplicationContext appCtx;

    CommonConfig commonConfig;

    SystemProperties config;

    @PostConstruct
    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("Uncaught exception", e);
            }
        });
    }

    public BlockStore blockStore(){

        String database = config.databaseDir();

        String blocksIndexFile = database + "/blocks/index";
        File dbFile = new File(blocksIndexFile);
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

        DB indexDB = DBMaker.fileDB(dbFile)
                .closeOnJvmShutdown()
                .make();

        Map<Long, List<IndexedBlockStore.BlockInfo>> indexMap = indexDB.hashMapCreate("index")
                .keySerializer(Serializer.LONG)
                .valueSerializer(BLOCK_INFO_SERIALIZER)
                .counterEnable()
                .makeOrGet();

        KeyValueDataSource blocksDB = new LevelDbDataSource("blocks");
        blocksDB.init();


        IndexedBlockStore cache = new IndexedBlockStore();
        cache.init(new HashMap<Long, List<IndexedBlockStore.BlockInfo>>(), new HashMapDB(), null, null);

        IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
        indexedBlockStore.init(indexMap, blocksDB, cache, indexDB);


        return indexedBlockStore;
    }


    LevelDbDataSource levelDbDataSource() {
        return new LevelDbDataSource();
    }
   
    LevelDbDataSource levelDbDataSource(String name) {
        return new LevelDbDataSource(name);
    }
}
