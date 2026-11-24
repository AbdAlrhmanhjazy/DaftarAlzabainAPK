package com.daftar.alzabain;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "daftar_alzabain.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "balance REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_id INTEGER, " +
                "tx_type TEXT, " +
                "amount REAL, " +
                "note TEXT, " +
                "tx_date DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        db.execSQL("DROP TABLE IF EXISTS customers");
        onCreate(db);
    }

    public void addCustomer(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("phone", phone);
        cv.put("balance", 0.0);
        db.insert("customers", null, cv);
    }

    public void addTransaction(int customerId, String type, double amount, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("customer_id", customerId);
        cv.put("tx_type", type);
        cv.put("amount", amount);
        cv.put("note", note);
        db.insert("transactions", null, cv);

        String op = "DEBT".equals(type) ? "+" : "-";
        db.execSQL("UPDATE customers SET balance = balance " + op + " ? WHERE id = ?",
                new Object[]{amount, customerId});
    }

    public Cursor getAllCustomers(String keyword) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (keyword == null || keyword.trim().isEmpty()) {
            return db.rawQuery("SELECT * FROM customers ORDER BY id DESC", null);
        }
        String p = "%" + keyword.trim() + "%";
        return db.rawQuery("SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY id DESC",
                new String[]{p, p});
    }

    public double[] getSummary() {
        SQLiteDatabase db = this.getReadableDatabase();
        double debts = 0, payments = 0;
        Cursor c = db.rawQuery("SELECT " +
                "COALESCE(SUM(CASE WHEN tx_type='DEBT' THEN amount ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN tx_type='PAYMENT' THEN amount ELSE 0 END), 0) " +
                "FROM transactions", null);
        if (c.moveToFirst()) {
            debts = c.getDouble(0);
            payments = c.getDouble(1);
        }
        c.close();
        return new double[]{debts, payments, debts - payments};
    }
}
