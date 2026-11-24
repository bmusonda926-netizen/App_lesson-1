package com.christocentricus.easytracker.ui;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.db.Repository;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.model.StockTransaction;
import com.christocentricus.easytracker.util.Fmt;

import java.util.List;

/** Shared helpers: bottom navigation, chips, row binding, spinner adapters. */
public abstract class BaseActivity extends AppCompatActivity {
    protected static final int NAV_NONE = -1;
    protected static final int NAV_HOME = 0;
    protected static final int NAV_INVENTORY = 1;
    protected static final int NAV_HISTORY = 2;
    protected static final int NAV_SETTINGS = 3;

    protected Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = Repository.get(this);
    }

    protected void setupNav(int selected) {
        int[] ids = {R.id.nav_home, R.id.nav_inventory, R.id.nav_history, R.id.nav_settings};
        final Class<?>[] targets = {DashboardActivity.class, InventoryActivity.class,
                HistoryActivity.class, SettingsActivity.class};
        for (int i = 0; i < ids.length; i++) {
            View item = findViewById(ids[i]);
            if (item == null) continue;
            boolean sel = i == selected;
            int color = getResources().getColor(sel ? R.color.green : R.color.text_hint);
            ViewGroup g = (ViewGroup) item;
            ((ImageView) g.getChildAt(0)).setColorFilter(color);
            ((TextView) g.getChildAt(1)).setTextColor(color);
            final int idx = i;
            final Class<?> target = targets[i];
            item.setOnClickListener(v -> {
                if (idx == selected) return;
                Intent it = new Intent(this, target);
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(it);
                if (idx != NAV_HOME) overridePendingTransition(0, 0);
            });
        }
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_selected : R.drawable.bg_chip);
        chip.setTextColor(selected ? Color.WHITE : getResources().getColor(R.color.text_secondary));
    }

    protected <T> ArrayAdapter<T> spinnerAdapter(List<T> items) {
        ArrayAdapter<T> a = new ArrayAdapter<>(this, R.layout.spinner_item, items);
        a.setDropDownViewResource(R.layout.spinner_dropdown);
        return a;
    }

    protected void confirm(String title, String message, String positive, Runnable onYes) {
        new AlertDialog.Builder(this, R.style.ET_Dialog)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .show();
    }

    // ---------------------------------------------------------------- row binding

    public static void applyStatus(Context c, TextView chip, Product p) {
        int s = p.status();
        chip.setText(Product.statusLabel(s));
        if (s == Product.OUT_OF_STOCK) {
            chip.setBackgroundResource(R.drawable.bg_status_out);
            chip.setTextColor(c.getResources().getColor(R.color.red));
        } else if (s == Product.LOW_STOCK) {
            chip.setBackgroundResource(R.drawable.bg_status_low);
            chip.setTextColor(c.getResources().getColor(R.color.amber));
        } else {
            chip.setBackgroundResource(R.drawable.bg_status_ok);
            chip.setTextColor(c.getResources().getColor(R.color.green));
        }
    }

    public static View productRow(Context c, ViewGroup parent, View convert, Product p) {
        View v = convert != null ? convert
                : LayoutInflater.from(c).inflate(R.layout.item_product, parent, false);
        ((TextView) v.findViewById(R.id.tv_name)).setText(p.name);
        ((TextView) v.findViewById(R.id.tv_sub)).setText(p.sku + "  •  " + p.categoryName);
        ((TextView) v.findViewById(R.id.tv_qty)).setText(Fmt.number(p.quantity));
        ((TextView) v.findViewById(R.id.tv_price)).setText(Fmt.money(c, p.sellingPrice));
        applyStatus(c, (TextView) v.findViewById(R.id.tv_status), p);
        return v;
    }

    public static View txnRow(Context c, ViewGroup parent, View convert, StockTransaction t) {
        View v = convert != null ? convert
                : LayoutInflater.from(c).inflate(R.layout.item_txn, parent, false);
        boolean in = t.isIn();
        ImageView icon = (ImageView) v.findViewById(R.id.iv_type);
        icon.setImageResource(in ? R.drawable.ic_in : R.drawable.ic_out);
        icon.setBackgroundResource(in ? R.drawable.bg_circle_in : R.drawable.bg_circle_out);
        icon.setColorFilter(c.getResources().getColor(in ? R.color.green : R.color.amber));
        ((TextView) v.findViewById(R.id.tv_name)).setText(t.productName);
        ((TextView) v.findViewById(R.id.tv_sub)).setText(
                (in ? "Stock In" : "Stock Out") + "  •  " + t.prevQty + " → " + t.newQty
                        + "  •  " + Fmt.dateTime(t.txnDate));
        TextView q = (TextView) v.findViewById(R.id.tv_qty);
        q.setText((in ? "+" : "-") + Fmt.number(t.quantity));
        q.setTextColor(c.getResources().getColor(in ? R.color.green : R.color.amber));
        return v;
    }

    public static void showTxnDialog(Context c, StockTransaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Product: ").append(t.productName).append(" (").append(t.sku).append(")\n");
        sb.append("Type: ").append(t.isIn() ? "Stock In" : "Stock Out").append('\n');
        sb.append("Quantity: ").append(t.quantity).append('\n');
        sb.append("Previous quantity: ").append(t.prevQty).append('\n');
        sb.append("New quantity: ").append(t.newQty).append('\n');
        sb.append(t.isIn() ? "Unit cost: " : "Unit selling price: ")
                .append(Fmt.money(c, t.unitPrice)).append('\n');
        sb.append("Date: ").append(Fmt.dateTime(t.txnDate)).append('\n');
        if (t.note != null && !t.note.isEmpty()) sb.append("Note: ").append(t.note);
        new AlertDialog.Builder(c, R.style.ET_Dialog)
                .setTitle("Movement details")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }
}
