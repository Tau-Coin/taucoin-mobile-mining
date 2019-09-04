package io.taucoin.android.datasource.mmkv;

import io.taucoin.datasource.DBCorruptionException;
import io.taucoin.datasource.KeyValueDataSource;

import com.tencent.mmkv.MMKV;
import com.tencent.mmkv.MMKVHandler;
import com.tencent.mmkv.MMKVLogLevel;
import com.tencent.mmkv.MMKVRecoverStrategic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.taucoin.config.SystemProperties.CONFIG;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * @author Taucoin Core Developers
 * @since 30.08.2019
 */
public class MmkvDataSource implements KeyValueDataSource {

    private static final Logger logger = LoggerFactory.getLogger("db");

    private String name;
    private MMKV db;
    private boolean alive;

    public MmkvDataSource() {
    }

    public MmkvDataSource(String name) {
        this.name = name;
    }

    @Override
    public void init() {

        if (isAlive()) return;
        if (name == null) throw new NullPointerException("no name set to the db");

        // Check database directory
        String dbDir = CONFIG.databaseDir() + File.separator + name;
        File f = new File(dbDir);

        if (f.exists()) {
            if (!f.isDirectory()) {
                // This should never happen.
                throw new RuntimeException(dbDir + " isn't a directory");
            }
        } else {
            f.mkdir();
        }

        String root = MMKV.initialize(dbDir);
        MMKV.registerHandler(new MMKVHandlerImpl());
        db = MMKV.defaultMMKV(); 
        alive = true;
        logger.info("Mmkv is alive with path: {}", root);
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public byte[] get(byte[] key) {
        byte[] value = db.decodeBytes(toHexString(key));

        return value;
    }

    @Override
    public byte[] put(byte[] key, byte[] value) {
        db.encode(toHexString(key), value);
        return value;
    }

    @Override
    public void delete(byte[] key) {
        db.removeValueForKey(toHexString(key));
    }

    @Override
    public Set<byte[]> keys() {
        // Temp solution
        return null;
    }

    private void updateBatchInternal(Map<byte[], byte[]> rows) {
        for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        db.commit();
    }

    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        updateBatchInternal(rows);
    }

    @Override
    public void close() {
        if (!isAlive()) return;

        logger.debug("Close db: {}", name);
        db.close();
        alive = false;
    }

    private static class MMKVHandlerImpl implements MMKVHandler {

        public MMKVHandlerImpl() {
        }

        @Override
        public MMKVRecoverStrategic onMMKVCRCCheckFail(String mmapID) {
            logger.error("MMKV CRC Check Fail");
            return MMKVRecoverStrategic.OnErrorRecover;
        }

        @Override
        public MMKVRecoverStrategic onMMKVFileLengthError(String mmapID) {
            logger.error("MMKV File Length Error");
            return MMKVRecoverStrategic.OnErrorRecover;
        }

        @Override
        public boolean wantLogRedirecting() {
            return true;
        }

        @Override
        public void mmkvLog(MMKVLogLevel level, String file, int line, String function, String message) {
            String log = "<" + file + ":" + line + "::" + function + "> " + message;

            switch (level) {
                case LevelDebug:
                    break;
                case LevelInfo:
                    break;
                case LevelWarning:
                    logger.warn(log);
                    break;
                case LevelError:
                    logger.error(log);
                    break;
                case LevelNone:
                    break;
            }
        }
    }
}
