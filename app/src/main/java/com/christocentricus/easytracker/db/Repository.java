package com.christocentricus.easytracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.christocentricus.easytracker.model.Category;
import com.christocentricus.easytracker.model.DashboardStats;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.model.StockTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All database access lives here. Activities never touch SQL directly. */
public final class Repository {
    private static Repository instance;
    private final DbHelper helper;

    private Repository(Context c) {
        helper = new DbHelper(c.getApplicationContext());
    }

    public static synchronized Repository get(Context c) {
        if (instance == null) instance = new Repository(c);
        return instance;
    }

    // ------------------------------------------------------------------ categories

    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase()
                .rawQuery("SELECT id, name FROM categories ORDER BY name COLLATE NOCASE", null);
        try {
            while (c.moveToNext()) list.add(new Category(c.getLong(0), c.getString(1)));
        } finally {
            c.close();
        }
        return list;
    }

    /** Returns the new id, or -1 if the name already exists / is invalid. */
    public long addCategory(String name) {
        if (name == null || name.trim().isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        return helper.getWritableDatabase().insert("categories", null, v);
    }

    // ------------------------------------------------------------------ products

    private static final String PRODUCT_SELECT =
            "SELECT p.id, p.name, p.sku, p.category_id, c.name, p.purchase_price, p.selling_price, "
                    + "p.quantity, p.min_stock, p.description, p.date_added "
                    + "FROM products p LEFT JOIN categories c ON c.id = p.category_id ";

    private Product readProduct(Cursor c) {
        Product p = new Product();
        p.id = c.getLong(0);
        p.name = c.getString(1);
        p.sku = c.getString(2);
        p.categoryId = c.isNull(3) ? 0 : c.getLong(3);
        p.categoryName = c.isNull(4) ? "Uncategorised" : c.getString(4);
        p.purchasePrice = c.getDouble(5);
        p.sellingPrice = c.getDouble(6);
        p.quantity = c.getInt(7);
        p.minStock = c.getInt(8);
        p.description = c.isNull(9) ? "" : c.getString(9);
        p.dateAdded = c.getLong(10);
        return p;
    }

    /**
     * @param statusFilter -1 for all, otherwise Product.IN_STOCK / LOW_STOCK / OUT_OF_STOCK
     * @param categoryId   0 for all categories
     */
    public List<Product> getProducts(String query, int statusFilter, long categoryId) {
        StringBuilder sql = new StringBuilder(PRODUCT_SELECT);
        List<String> where = new ArrayList<>();
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim() + "%";
            where.add("(p.name LIKE ? OR p.sku LIKE ? OR c.name LIKE ?)");
            args.add(q);
            args.add(q);
            args.add(q);
        }
        if (categoryId > 0) {
            where.add("p.category_id = ?");
            args.add(String.valueOf(categoryId));
        }
        if (statusFilter == Product.OUT_OF_STOCK) {
            where.add("p.quantity <= 0");
        } else if (statusFilter == Product.LOW_STOCK) {
            where.add("p.quantity > 0 AND p.quantity <= p.min_stock");
        } else if (statusFilter == Product.IN_STOCK) {
            where.add("p.quantity > 0 AND p.quantity > p.min_stock");
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(TextUtils.join(" AND ", where)).append(' ');
        sql.append("ORDER BY p.name COLLATE NOCASE");

        List<Product> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) list.add(readProduct(c));
        } finally {
            c.close();
        }
        return list;
    }

    /** Products at or below their minimum threshold (includes out-of-stock), lowest first. */
    public List<Product> getLowStock(int limit) {
        List<Product> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().rawQuery(
                PRODUCT_SELECT + "WHERE p.quantity <= p.min_stock "
                        + "ORDER BY p.quantity ASC, p.name COLLATE NOCASE LIMIT " + limit, null);
        try {
            while (c.moveToNext()) list.add(readProduct(c));
        } finally {
            c.close();
        }
        return list;
    }

    public Product getProduct(long id) {
        Cursor c = helper.getReadableDatabase()
                .rawQuery(PRODUCT_SELECT + "WHERE p.id = ?", new String[]{String.valueOf(id)});
        try {
            return c.moveToFirst() ? readProduct(c) : null;
        } finally {
            c.close();
        }
    }

    public boolean skuExists(String sku, long excludeId) {
        Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT 1 FROM products WHERE sku = ? COLLATE NOCASE AND id != ?",
                new String[]{sku.trim(), String.valueOf(excludeId)});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public String suggestSku() {
        Cursor c = helper.getReadableDatabase().rawQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM products", null);
        long next = 1;
        try {
            if (c.moveToFirst()) next = c.getLong(0);
        } finally {
            c.close();
        }
        String sku = String.format(Locale.US, "ET-%04d", next);
        while (skuExists(sku, 0)) {
            next++;
            sku = String.format(Locale.US, "ET-%04d", next);
        }
        return sku;
    }

    private ContentValues productValues(Product p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name.trim());
        v.put("sku", p.sku.trim());
        if (p.categoryId > 0) v.put("category_id", p.categoryId);
        else v.putNull("category_id");
        v.put("purchase_price", p.purchasePrice);
        v.put("selling_price", p.sellingPrice);
        v.put("min_stock", p.minStock);
        v.put("description", p.description == null ? "" : p.description.trim());
        return v;
    }

    /** Inserts a product. A non-zero opening quantity is also logged as a Stock In movement. */
    public long insertProduct(Product p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            ContentValues v = productValues(p);
            v.put("quantity", p.quantity);
            v.put("date_added", now);
            long id = db.insert("products", null, v);
            if (id < 0) return -1;
            if (p.quantity > 0) {
                db.insertOrThrow("stock_transactions", null,
                        txnValues(id, p.name.trim(), p.sku.trim(), StockTransaction.IN,
                                p.quantity, 0, p.quantity, p.purchasePrice, "Opening stock", now));
            }
            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /** Updates product details (not quantity – that changes only via stock movements). */
    public boolean updateProduct(Product p) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            int rows = db.update("products", productValues(p), "id = ?", new String[]{String.valueOf(p.id)});
            if (rows != 1) return false;
            ContentValues snap = new ContentValues();
            snap.put("product_name", p.name.trim());
            snap.put("product_sku", p.sku.trim());
            db.update("stock_transactions", snap, "product_id = ?", new String[]{String.valueOf(p.id)});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes the product. Its movement history is kept (product link is cleared). */
    public boolean deleteProduct(long id) {
        return helper.getWritableDatabase().delete("products", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // ------------------------------------------------------------------ stock movements

    private ContentValues txnValues(long productId, String name, String sku, String type, int qty,
                                    int prev, int next, double unitPrice, String note, long date) {
        ContentValues v = new ContentValues();
        v.put("product_id", productId);
        v.put("product_name", name);
        v.put("product_sku", sku);
        v.put("txn_type", type);
        v.put("quantity", qty);
        v.put("prev_qty", prev);
        v.put("new_qty", next);
        v.put("unit_price", unitPrice);
        v.put("note", note == null ? "" : note.trim());
        v.put("txn_date", date);
        return v;
    }

    /** @return null on success, otherwise an error message for the user. */
    public String stockIn(long productId, int qty, Double priceOverride, long date, String note) {
        return applyStock(productId, StockTransaction.IN, qty, priceOverride, date, note);
    }

    /** @return null on success, otherwise an error message for the user. */
    public String stockOut(long productId, int qty, long date, String reason) {
        return applyStock(productId, StockTransaction.OUT, qty, null, date, reason);
    }

    private String applyStock(long productId, String type, int qty, Double price, long date, String note) {
        if (qty <= 0) return "Quantity must be greater than zero.";
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String name;
            String sku;
            int prev;
            double purchase;
            double selling;
            Cursor c = db.rawQuery("SELECT name, sku, quantity, purchase_price, selling_price "
                    + "FROM products WHERE id = ?", new String[]{String.valueOf(productId)});
            try {
                if (!c.moveToFirst()) return "Product not found.";
                name = c.getString(0);
                sku = c.getString(1);
                prev = c.getInt(2);
                purchase = c.getDouble(3);
                selling = c.getDouble(4);
            } finally {
                c.close();
            }

            boolean isIn = StockTransaction.IN.equals(type);
            long next = isIn ? (long) prev + qty : (long) prev - qty;
            if (next < 0) return "Not enough stock. Only " + prev + " unit(s) available.";
            if (next > Integer.MAX_VALUE) return "Quantity is too large.";

            ContentValues pv = new ContentValues();
            pv.put("quantity", (int) next);
            double unit = isIn ? purchase : selling;
            if (isIn && price != null) {
                pv.put("purchase_price", price);
                unit = price;
            }
            db.update("products", pv, "id = ?", new String[]{String.valueOf(productId)});
            db.insertOrThrow("stock_transactions", null,
                    txnValues(productId, name, sku, type, qty, prev, (int) next, unit, note, date));
            db.setTransactionSuccessful();
            return null;
        } catch (Exception e) {
            return "Could not save the movement: " + e.getMessage();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @param type      "IN", "OUT" or null for both
     * @param productId 0 for all products
     * @param query     optional text matched against product name / SKU / note
     * @param limit     0 for no limit
     */
    public List<StockTransaction> getTransactions(String type, long productId, String query, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, product_id, product_name, product_sku, txn_type, "
                + "quantity, prev_qty, new_qty, unit_price, note, txn_date FROM stock_transactions ");
        List<String> where = new ArrayList<>();
        List<String> args = new ArrayList<>();
        if (type != null) {
            where.add("txn_type = ?");
            args.add(type);
        }
        if (productId > 0) {
            where.add("product_id = ?");
            args.add(String.valueOf(productId));
        }
        if (query != null && !query.trim().isEmpty()) {
            String q = "%" + query.trim() + "%";
            where.add("(product_name LIKE ? OR product_sku LIKE ? OR note LIKE ?)");
            args.add(q);
            args.add(q);
            args.add(q);
        }
        if (!where.isEmpty()) sql.append("WHERE ").append(TextUtils.join(" AND ", where)).append(' ');
        sql.append("ORDER BY txn_date DESC, id DESC");
        if (limit > 0) sql.append(" LIMIT ").append(limit);

        List<StockTransaction> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                StockTransaction t = new StockTransaction();
                t.id = c.getLong(0);
                t.productId = c.isNull(1) ? 0 : c.getLong(1);
                t.productName = c.getString(2);
                t.sku = c.getString(3);
                t.type = c.getString(4);
                t.quantity = c.getInt(5);
                t.prevQty = c.getInt(6);
                t.newQty = c.getInt(7);
                t.unitPrice = c.getDouble(8);
                t.note = c.isNull(9) ? "" : c.getString(9);
                t.txnDate = c.getLong(10);
                list.add(t);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ------------------------------------------------------------------ dashboard & export

    public DashboardStats getStats() {
        DashboardStats s = new DashboardStats();
        Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(quantity), 0), "
                        + "COALESCE(SUM(quantity * purchase_price), 0), "
                        + "COALESCE(SUM(quantity * selling_price), 0), "
                        + "COALESCE(SUM(CASE WHEN quantity <= 0 THEN 1 ELSE 0 END), 0), "
                        + "COALESCE(SUM(CASE WHEN quantity > 0 AND quantity <= min_stock THEN 1 ELSE 0 END), 0) "
                        + "FROM products", null);
        try {
            if (c.moveToFirst()) {
                s.totalProducts = c.getInt(0);
                s.totalUnits = c.getLong(1);
                s.valueCost = c.getDouble(2);
                s.valueRetail = c.getDouble(3);
                s.outCount = c.getInt(4);
                s.lowCount = c.getInt(5);
            }
        } finally {
            c.close();
        }
        return s;
    }

    public String buildInventoryCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name,SKU,Category,Quantity,Min Stock,Purchase Price,Selling Price,Stock Value (Cost),Status,Date Added\n");
        for (Product p : getProducts(null, -1, 0)) {
            sb.append(csv(p.name)).append(',')
                    .append(csv(p.sku)).append(',')
                    .append(csv(p.categoryName)).append(',')
                    .append(p.quantity).append(',')
                    .append(p.minStock).append(',')
                    .append(String.format(Locale.US, "%.2f", p.purchasePrice)).append(',')
                    .append(String.format(Locale.US, "%.2f", p.sellingPrice)).append(',')
                    .append(String.format(Locale.US, "%.2f", p.quantity * p.purchasePrice)).append(',')
                    .append(csv(Product.statusLabel(p.status()))).append(',')
                    .append(csv(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            .format(new java.util.Date(p.dateAdded))))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public java.io.File databaseFile(Context c) {
        // Make sure the database exists on disk before it is copied.
        helper.getReadableDatabase();
        return c.getDatabasePath(DbHelper.DB_NAME);
    }
}
