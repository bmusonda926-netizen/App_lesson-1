package com.christocentricus.easytracker.util;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formatting and parsing helpers. */
public final class Fmt {
    private Fmt() {}

    public static String money(Context c, double v) {
        return Prefs.currency(c) + " " + String.format(Locale.US, "%,.2f", v);
    }

    public static String number(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public static String plain(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    public static String date(long ms) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(ms));
    }

    public static String dateTime(long ms) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(ms));
    }

    /** Returns null when the text is empty or not a valid finite number. */
    public static Double parseDouble(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.contains(",") && !s.contains(".")) {
            s = s.replace(',', '.');
        } else {
            s = s.replace(",", "");
        }
        try {
            double d = Double.parseDouble(s);
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns null when the text is empty or not a valid whole number. */
    public static Integer parseInt(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
