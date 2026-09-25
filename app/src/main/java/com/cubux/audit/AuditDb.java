package com.cubux.audit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class AuditDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "cubux_audit.db";
    private static final int DB_VERSION = 2;

    public AuditDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        createBaseTables(db);
        createV2Tables(db);
    }

    private void createBaseTables(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "transaction_id TEXT PRIMARY KEY," +
                "data TEXT NOT NULL," +
                "first_seen TEXT NOT NULL," +
                "last_seen TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS changes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "detected_at TEXT NOT NULL," +
                "transaction_id TEXT NOT NULL," +
                "change_type TEXT NOT NULL," +
                "old_data TEXT," +
                "new_data TEXT" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS audit_runs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "started_at TEXT NOT NULL," +
                "finished_at TEXT," +
                "total_records INTEGER NOT NULL," +
                "new_count INTEGER NOT NULL," +
                "changed_count INTEGER NOT NULL," +
                "deleted_count INTEGER NOT NULL," +
                "status TEXT NOT NULL" +
                ")"
        );
    }

    private void createV2Tables(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS period_baselines (" +
                "period_key TEXT PRIMARY KEY," +
                "period_type TEXT NOT NULL," +
                "created_at TEXT NOT NULL," +
                "record_count INTEGER NOT NULL," +
                "income_total REAL NOT NULL," +
                "expense_total REAL NOT NULL," +
                "control_hash TEXT NOT NULL," +
                "data TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS daily_closures (" +
                "date_key TEXT PRIMARY KEY," +
                "closed_at TEXT NOT NULL," +
                "record_count INTEGER NOT NULL," +
                "income_total REAL NOT NULL," +
                "expense_total REAL NOT NULL," +
                "control_hash TEXT NOT NULL," +
                "status TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS bank_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bank_uid TEXT," +
                "operation_date TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "currency TEXT," +
                "description TEXT," +
                "counterparty TEXT," +
                "raw_data TEXT NOT NULL," +
                "imported_at TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS bank_matches (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bank_transaction_id INTEGER NOT NULL," +
                "cubux_transaction_id TEXT," +
                "match_status TEXT NOT NULL," +
                "match_score REAL NOT NULL," +
                "checked_at TEXT NOT NULL," +
                "details TEXT" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS retry_queue (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "task_type TEXT NOT NULL," +
                "created_at TEXT NOT NULL," +
                "next_retry_at TEXT NOT NULL," +
                "retry_count INTEGER NOT NULL," +
                "last_error TEXT," +
                "status TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS notification_queue (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "created_at TEXT NOT NULL," +
                "notification_type TEXT NOT NULL," +
                "destination TEXT NOT NULL," +
                "payload TEXT NOT NULL," +
                "retry_count INTEGER NOT NULL," +
                "next_retry_at TEXT," +
                "status TEXT NOT NULL" +
                ")"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS app_state (" +
                "state_key TEXT PRIMARY KEY," +
                "state_value TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
                ")"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

        if (oldVersion < 2) {
            createV2Tables(db);
        }
    }

    public SQLiteDatabase beginTransaction() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        return db;
    }

    public void endTransaction(
            SQLiteDatabase db,
            boolean successful
    ) {

        if (successful) {
            db.setTransactionSuccessful();
        }

        db.endTransaction();
    }

    public Map<String, String> getTransactions() {

        Map<String, String> result = new HashMap<>();

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT transaction_id, data FROM transactions",
                null
        );

        try {

            while (cursor.moveToNext()) {

                result.put(
                        cursor.getString(0),
                        cursor.getString(1)
                );
            }

        } finally {
            cursor.close();
        }

        return result;
    }

    public long createRunningRun(
            String startedAt
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("started_at", startedAt);
        values.put("total_records", 0);
        values.put("new_count", 0);
        values.put("changed_count", 0);
        values.put("deleted_count", 0);
        values.put("status", "RUNNING");

        return db.insert(
                "audit_runs",
                null,
                values
        );
    }

    public void finishRun(
            long runId,
            String finishedAt,
            int total,
            int newCount,
            int changedCount,
            int deletedCount,
            String status
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("finished_at", finishedAt);
        values.put("total_records", total);
        values.put("new_count", newCount);
        values.put("changed_count", changedCount);
        values.put("deleted_count", deletedCount);
        values.put("status", status);

        db.update(
                "audit_runs",
                values,
                "id=?",
                new String[]{
                        String.valueOf(runId)
                }
        );
    }

    public void saveTransaction(
            String id,
            String data,
            String now
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("transaction_id", id);
        values.put("data", data);
        values.put("first_seen", now);
        values.put("last_seen", now);

        db.insertWithOnConflict(
                "transactions",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public void updateTransaction(
            String id,
            String data,
            String now
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("data", data);
        values.put("last_seen", now);

        db.update(
                "transactions",
                values,
                "transaction_id=?",
                new String[]{id}
        );
    }

    public void saveChange(
            String detectedAt,
            String transactionId,
            String type,
            String oldData,
            String newData
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("detected_at", detectedAt);
        values.put("transaction_id", transactionId);
        values.put("change_type", type);

        if (oldData != null) {
            values.put("old_data", oldData);
        }

        if (newData != null) {
            values.put("new_data", newData);
        }

        db.insert(
                "changes",
                null,
                values
        );
    }

    public void deleteTransaction(
            String id
    ) {

        SQLiteDatabase db = getWritableDatabase();

        db.delete(
                "transactions",
                "transaction_id=?",
                new String[]{id}
        );
    }

    public void saveState(
            String key,
            String value,
            String now
    ) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("state_key", key);
        values.put("state_value", value);
        values.put("updated_at", now);

        db.insertWithOnConflict(
                "app_state",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public String getState(
            String key
    ) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT state_value FROM app_state " +
                "WHERE state_key=?",
                new String[]{key}
        );

        try {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }

            return null;

        } finally {
            cursor.close();
        }
    }
}
