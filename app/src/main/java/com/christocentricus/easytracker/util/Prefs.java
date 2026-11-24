package com.christocentricus.easytracker.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Small wrapper around SharedPreferences for app settings. */
public final class Prefs {
    public static final String VALUATION_COST = "COST";
    public static final String VALUATION_RETAIL = "RETAIL";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("easytracker_prefs", Context.MODE_PRIVATE);
    }

    public static String currency(Context c) {
        return sp(c).getString("currency", "ZMW");
    }

    public static void setCurrency(Context c, String code) {
        sp(c).edit().putString("currency", code).apply();
    }

    public static String valuation(Context c) {
        return sp(c).getString("valuation", VALUATION_COST);
    }

    public static void setValuation(Context c, String v) {
        sp(c).edit().putString("valuation", v).apply();
    }
}
