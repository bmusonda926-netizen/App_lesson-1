package com.christocentricus.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.DashboardStats;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.model.StockTransaction;
import com.christocentricus.easytracker.util.Fmt;
import com.christocentricus.easytracker.util.Prefs;

import java.util.List;

public class DashboardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupNav(NAV_HOME);

        findViewById(R.id.btn_add_product).setOnClickListener(v ->
                startActivity(new Intent(this, ProductFormActivity.class)));
        findViewById(R.id.btn_stock_in).setOnClickListener(v -> openStock(true));
        findViewById(R.id.btn_stock_out).setOnClickListener(v -> openStock(false));
        findViewById(R.id.btn_inventory).setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));
        findViewById(R.id.tv_see_all).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.tile_products).setOnClickListener(v ->
                startActivity(new Intent(this, InventoryActivity.class)));
        findViewById(R.id.tile_low).setOnClickListener(v -> openInventory(Product.LOW_STOCK));
        findViewById(R.id.tile_out).setOnClickListener(v -> openInventory(Product.OUT_OF_STOCK));
    }

    private void openStock(boolean in) {
        Intent it = new Intent(this, StockActivity.class);
        it.putExtra(StockActivity.EXTRA_MODE, in ? "IN" : "OUT");
        startActivity(it);
    }

    private void openInventory(int status) {
        Intent it = new Intent(this, InventoryActivity.class);
        it.putExtra(InventoryActivity.EXTRA_STATUS, status);
        startActivity(it);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    /** Re-reads every figure from the database, so the dashboard always matches inventory. */
    private void refresh() {
        DashboardStats s = repo.getStats();
        boolean retail = Prefs.VALUATION_RETAIL.equals(Prefs.valuation(this));
        ((TextView) findViewById(R.id.tv_value_label)).setText(
                retail ? "Total stock value (selling price)" : "Total stock value (purchase price)");
        ((TextView) findViewById(R.id.tv_value)).setText(Fmt.money(this, retail ? s.valueRetail : s.valueCost));
        ((TextView) findViewById(R.id.tv_value_sub)).setText(
                retail ? "At cost: " + Fmt.money(this, s.valueCost)
                        : "At selling price: " + Fmt.money(this, s.valueRetail));
        ((TextView) findViewById(R.id.tv_products)).setText(Fmt.number(s.totalProducts));
        ((TextView) findViewById(R.id.tv_units)).setText(Fmt.number(s.totalUnits));
        ((TextView) findViewById(R.id.tv_low)).setText(Fmt.number(s.lowCount));
        ((TextView) findViewById(R.id.tv_out)).setText(Fmt.number(s.outCount));

        LinearLayout low = (LinearLayout) findViewById(R.id.low_container);
        low.removeAllViews();
        List<Product> lows = repo.getLowStock(5);
        findViewById(R.id.tv_low_empty).setVisibility(lows.isEmpty() ? View.VISIBLE : View.GONE);
        for (final Product p : lows) {
            View row = productRow(this, low, null, p);
            row.setOnClickListener(v -> openDetail(p.id));
            low.addView(row);
        }

        LinearLayout recent = (LinearLayout) findViewById(R.id.recent_container);
        recent.removeAllViews();
        List<StockTransaction> txns = repo.getTransactions(null, 0, null, 5);
        findViewById(R.id.tv_recent_empty).setVisibility(txns.isEmpty() ? View.VISIBLE : View.GONE);
        for (final StockTransaction t : txns) {
            View row = txnRow(this, recent, null, t);
            row.setOnClickListener(v -> showTxnDialog(this, t));
            recent.addView(row);
        }
    }

    private void openDetail(long id) {
        Intent it = new Intent(this, ProductDetailActivity.class);
        it.putExtra(ProductDetailActivity.EXTRA_ID, id);
        startActivity(it);
    }
}
