package com.christocentricus.easytracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Creates and upgrades the Easy Tracker SQLite database. */
public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "easytracker.db";
    private static final int DB_VERSION = 1;

    private static final String[] DEFAULT_CATEGORIES = {
            "General", "Electronics", "Food & Beverage", "Clothing", "Stationery", "Hardware", "Other"
    };

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        // Keep a single database file (no -wal file) so backups are always complete.
        setWriteAheadLoggingEnabled(false);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE COLLATE NOCASE)");

        db.execSQL("CREATE TABLE products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "sku TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                + "category_id INTEGER, "
                + "purchase_price REAL NOT NULL DEFAULT 0 CHECK(purchase_price >= 0), "
                + "selling_price REAL NOT NULL DEFAULT 0 CHECK(selling_price >= 0), "
                + "quantity INTEGER NOT NULL DEFAULT 0 CHECK(quantity >= 0), "
                + "min_stock INTEGER NOT NULL DEFAULT 0 CHECK(min_stock >= 0), "
                + "description TEXT, "
                + "date_added INTEGER NOT NULL, "
                + "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL)");

        // product_name / product_sku are snapshots so history survives product deletion.
        db.execSQL("CREATE TABLE stock_transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "product_id INTEGER, "
                + "product_name TEXT NOT NULL, "
                + "product_sku TEXT NOT NULL, "
                + "txn_type TEXT NOT NULL CHECK(txn_type IN ('IN','OUT')), "
                + "quantity INTEGER NOT NULL CHECK(quantity > 0), "
                + "prev_qty INTEGER NOT NULL, "
                + "new_qty INTEGER NOT NULL CHECK(new_qty >= 0), "
                + "unit_price REAL NOT NULL DEFAULT 0, "
                + "note TEXT, "
                + "txn_date INTEGER NOT NULL, "
                + "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE SET NULL)");

        db.execSQL("CREATE INDEX idx_products_name ON products(name COLLATE NOCASE)");
        db.execSQL("CREATE INDEX idx_products_category ON products(category_id)");
        db.execSQL("CREATE INDEX idx_txn_product ON stock_transactions(product_id)");
        db.execSQL("CREATE INDEX idx_txn_date ON stock_transactions(txn_date)");

        for (String name : DEFAULT_CATEGORIES) {
            ContentValues v = new ContentValues();
            v.put("name", name);
            db.insert("categories", null, v);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1.0 – nothing to migrate yet. Add migrations here in later milestones.
    }
}
