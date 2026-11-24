package com.christocentricus.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.model.StockTransaction;
import com.christocentricus.easytracker.util.Fmt;

import java.util.List;

public class ProductDetailActivity extends BaseActivity {
    public static final String EXTRA_ID = "product_id";

    private long productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        productId = getIntent().getLongExtra(EXTRA_ID, 0);

        findViewById(R.id.btn_in).setOnClickListener(v -> openStock("IN"));
        findViewById(R.id.btn_out).setOnClickListener(v -> openStock("OUT"));
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent it = new Intent(this, ProductFormActivity.class);
            it.putExtra(ProductFormActivity.EXTRA_ID, productId);
            startActivity(it);
        });
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirm(
                "Delete product?",
                "This permanently removes the product. Its past stock movements stay in the history.",
                "Delete",
                () -> {
                    if (repo.deleteProduct(productId)) {
                        toast("Product deleted.");
                        finish();
                    } else {
                        toast("Could not delete the product.");
                    }
                }));
        findViewById(R.id.tv_all_history).setOnClickListener(v -> {
            Intent it = new Intent(this, HistoryActivity.class);
            it.putExtra(HistoryActivity.EXTRA_PRODUCT_ID, productId);
            startActivity(it);
        });
    }

    private void openStock(String mode) {
        Intent it = new Intent(this, StockActivity.class);
        it.putExtra(StockActivity.EXTRA_MODE, mode);
        it.putExtra(StockActivity.EXTRA_PRODUCT_ID, productId);
        startActivity(it);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Product p = repo.getProduct(productId);
        if (p == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.tv_name)).setText(p.name);
        ((TextView) findViewById(R.id.tv_sub)).setText(p.sku + "  •  " + p.categoryName);
        ((TextView) findViewById(R.id.tv_qty)).setText(Fmt.number(p.quantity));
        applyStatus(this, (TextView) findViewById(R.id.tv_status), p);
        ((TextView) findViewById(R.id.tv_purchase)).setText(Fmt.money(this, p.purchasePrice));
        ((TextView) findViewById(R.id.tv_selling)).setText(Fmt.money(this, p.sellingPrice));
        ((TextView) findViewById(R.id.tv_min)).setText(String.valueOf(p.minStock));
        ((TextView) findViewById(R.id.tv_value)).setText(Fmt.money(this, p.quantity * p.purchasePrice));
        ((TextView) findViewById(R.id.tv_date)).setText(Fmt.date(p.dateAdded));
        ((TextView) findViewById(R.id.tv_desc)).setText(
                p.description == null || p.description.isEmpty() ? "No description" : p.description);

        LinearLayout box = (LinearLayout) findViewById(R.id.txn_container);
        box.removeAllViews();
        List<StockTransaction> txns = repo.getTransactions(null, productId, null, 5);
        findViewById(R.id.tv_txn_empty).setVisibility(txns.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_all_history).setVisibility(txns.isEmpty() ? View.GONE : View.VISIBLE);
        for (final StockTransaction t : txns) {
            View row = txnRow(this, box, null, t);
            row.setOnClickListener(v -> showTxnDialog(this, t));
            box.addView(row);
        }
    }
}
