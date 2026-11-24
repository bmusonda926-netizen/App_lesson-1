package com.christocentricus.easytracker.model;

public class StockTransaction {
    public static final String IN = "IN";
    public static final String OUT = "OUT";

    public long id;
    public long productId;
    public String productName = "";
    public String sku = "";
    public String type = IN;
    public int quantity;
    public int prevQty;
    public int newQty;
    public double unitPrice;
    public String note = "";
    public long txnDate;

    public boolean isIn() {
        return IN.equals(type);
    }
}
