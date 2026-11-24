package com.christocentricus.easytracker.model;

public class Product {
    public static final int IN_STOCK = 0;
    public static final int LOW_STOCK = 1;
    public static final int OUT_OF_STOCK = 2;

    public long id;
    public String name = "";
    public String sku = "";
    public long categoryId;
    public String categoryName = "Uncategorised";
    public double purchasePrice;
    public double sellingPrice;
    public int quantity;
    public int minStock;
    public String description = "";
    public long dateAdded;

    /** Out of stock at 0, low stock at or below the minimum threshold. */
    public int status() {
        if (quantity <= 0) return OUT_OF_STOCK;
        if (quantity <= minStock) return LOW_STOCK;
        return IN_STOCK;
    }

    public static String statusLabel(int status) {
        switch (status) {
            case OUT_OF_STOCK: return "Out of Stock";
            case LOW_STOCK: return "Low Stock";
            default: return "In Stock";
        }
    }

    @Override
    public String toString() {
        return name + "  (" + sku + ")";
    }
}
