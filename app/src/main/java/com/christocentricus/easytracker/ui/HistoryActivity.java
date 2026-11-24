package com.christocentricus.easytracker.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.model.StockTransaction;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private final List<StockTransaction> items = new ArrayList<>();
    private BaseAdapter adapter;
    private EditText search;
    private TextView chipAll, chipIn, chipOut;
    private String typeFilter;
    private long productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        setupNav(NAV_HISTORY);

        productId = getIntent().getLongExtra(EXTRA_PRODUCT_ID, 0);
        if (productId > 0) {
            Product p = repo.getProduct(productId);
            ((TextView) findViewById(R.id.tv_title)).setText(p != null ? p.name.toUpperCase() : "PRODUCT HISTORY");
        }

        search = (EditText) findViewById(R.id.et_search);
        chipAll = (TextView) findViewById(R.id.chip_all);
        chipIn = (TextView) findViewById(R.id.chip_in);
        chipOut = (TextView) findViewById(R.id.chip_out);
        chipAll.setOnClickListener(v -> { typeFilter = null; load(); });
        chipIn.setOnClickListener(v -> { typeFilter = StockTransaction.IN; load(); });
        chipOut.setOnClickListener(v -> { typeFilter = StockTransaction.OUT; load(); });

        ListView list = (ListView) findViewById(R.id.list);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public Object getItem(int i) { return items.get(i); }
            @Override public long getItemId(int i) { return items.get(i).id; }
            @Override public View getView(int i, View convert, ViewGroup parent) {
                return txnRow(HistoryActivity.this, parent, convert, items.get(i));
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> showTxnDialog(this, items.get(pos)));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { load(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        items.clear();
        items.addAll(repo.getTransactions(typeFilter, productId, search.getText().toString(), 0));
        adapter.notifyDataSetChanged();
        styleChip(chipAll, typeFilter == null);
        styleChip(chipIn, StockTransaction.IN.equals(typeFilter));
        styleChip(chipOut, StockTransaction.OUT.equals(typeFilter));
        ((TextView) findViewById(R.id.tv_count)).setText(
                items.size() + (items.size() == 1 ? " movement" : " movements"));
        findViewById(R.id.tv_empty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
