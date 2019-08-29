package io.taucoin.android.datasource.sqlite;

import io.taucoin.datasource.DBCorruptionException;
import io.taucoin.datasource.KeyValueDataSource;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.taucoin.config.SystemProperties.CONFIG;
import static org.spongycastle.util.encoders.Hex.toHexString;
import static org.spongycastle.util.encoders.Hex.decode;

/**
 * @author Taucoin Core Developers
 * @since 28.08.2019
 */
public class SqliteDataSource implements KeyValueDataSource {

    private static final Logger logger = LoggerFactory.getLogger("db");

    private static final String KEY_COLUMN = "key";
    private static final String VALUE_COLUMN = "value";

    private static final int DATABASE_VERSION = 1;

    private Context context;
    private String name;
    private String create_statements = null;
    private String drop_statements = null;
    private boolean alive;

    private DataBaseHelper dbHelper;
    private SQLiteDatabase db;

    public SqliteDataSource(Context context) {
        this.context = context;
    }

    public SqliteDataSource(Context context, String name) {
        this.context = context;
        this.name = name;
    }

    @Override
    public void init() {

        if (isAlive()) return;
        if (name == null) throw new NullPointerException("no name set to the db");

        dbHelper = new DataBaseHelper(context, name + ".db", null, DATABASE_VERSION);
        db = dbHelper.getWritableDatabase();
        alive = true;
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
        long t1 = System.nanoTime();

        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(name, null, KEY_COLUMN + "=?",
                new String[]{toHexString(key)}, null, null, null);

        if (cursor == null || (cursor != null && cursor.getCount() < 1)) {
            return null;
        }

        cursor.moveToFirst();
        byte[] value = cursor.getBlob(cursor.getColumnIndex(VALUE_COLUMN));
        cursor.close();

        long t2 = System.nanoTime();
        logger.info("Sqlite read in: {} ms", ((float)(t2 - t1) / 1_000_000));

        return value;
    }

    @Override
    public byte[] put(byte[] key, byte[] value) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_COLUMN, toHexString(key));
        newValues.put(VALUE_COLUMN, value);

        db = dbHelper.getWritableDatabase();
        long result = db.insertWithOnConflict(name, null, newValues,
                SQLiteDatabase.CONFLICT_IGNORE);

        if (result == -1L) {
            db.update(name, newValues, KEY_COLUMN + "=?",
                    new String[] {toHexString(key)});
        }

        return value;
    }

    private void put(SQLiteDatabase db, byte[] key, byte[] value) {
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_COLUMN, toHexString(key));
        newValues.put(VALUE_COLUMN, value);

        long result = db.insertWithOnConflict(name, null, newValues,
                SQLiteDatabase.CONFLICT_IGNORE);

        if (result == -1L) {
            db.update(name, newValues, KEY_COLUMN + "=?",
                    new String[] {toHexString(key)});
        }
    }

    @Override
    public void delete(byte[] key) {
        db = dbHelper.getWritableDatabase();
        db.delete(name, KEY_COLUMN + "=?", new String[] {toHexString(key)}) ;
    }

    @Override
    public Set<byte[]> keys() {
        // TODO:
        return null;
    }

    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
        long t1 = System.nanoTime();

        db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                put(db, entry.getKey(), entry.getValue());
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            logger.error("update batch error {}", e);
        } finally {
            db.endTransaction();
        }

        long t2 = System.nanoTime();
        logger.info("Sqlite update batch in: {} ms", ((float)(t2 - t1) / 1_000_000));
    }

    @Override
    public void close() {
        if (!isAlive()) return;

        logger.info("Close db: {}", name);
        db = dbHelper.getWritableDatabase();
        db.close();

        alive = false;
    }

    String getCreateStatements() {
        if (create_statements == null) {
            create_statements = "CREATE TABLE " + name + "" +
                    "(" +
                    KEY_COLUMN + " TEXT PRIMARY KEY not null," +
                    VALUE_COLUMN + " blob not null)";
        }

        return create_statements;
    }

    String getDropStatements() {
        if (drop_statements == null) {
            drop_statements = "DROP TABLE IF EXIST " + name;
        }

        return drop_statements;
    }
    
    private class DataBaseHelper extends SQLiteOpenHelper {
    
        public DataBaseHelper(Context context, String name, CursorFactory factory,
                int version) {
            super(context, name, factory, version);
        }
    
        // Called when no database exists in disk and the helper class needs
        // to create a new one.
        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(SqliteDataSource.this.getCreateStatements());
            } catch(Exception e){
                logger.error("Create db exception: {}", e);
            }
        }

        // Called when there is a database version mismatch meaning that the version
        // of the database on disk needs to be upgraded to the current version.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            logger.warn("Upgrading from version {} to {}", oldVersion, newVersion);

            // Upgrade the existing database to conform to the new version. Multiple
            // previous versions can be handled by comparing _oldVersion and _newVersion
            // values.
            // The simplest case is to drop the old table and create a new one.
            db.execSQL(SqliteDataSource.this.getDropStatements());
        
            // Create a new one.
            onCreate(db);
        }
    }
}
