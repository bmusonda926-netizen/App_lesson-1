package com.christocentricus.easytracker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.Category;
import com.christocentricus.easytracker.model.Product;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends BaseActivity {
    public static final String EXTRA_STATUS = "status";

    private final List<Product> items = new ArrayList<>();
    private BaseAdapter adapter;
    private EditText search;
    private Spinner spCategory;
    private TextView[] chips;
    private int statusFilter = -1;
    private List<Category> categories = new ArrayList<>();
    private boolean loadingCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        setupNav(NAV_INVENTORY);

        statusFilter = getIntent().getIntExtra(EXTRA_STATUS, -1);
        search = (EditText) findViewById(R.id.et_search);
        spCategory = (Spinner) findViewById(R.id.sp_category);
        chips = new TextView[]{(TextView) findViewById(R.id.chip_all), (TextView) findViewById(R.id.chip_in),
                (TextView) findViewById(R.id.chip_low), (TextView) findViewById(R.id.chip_out)};
        final int[] values = {-1, Product.IN_STOCK, Product.LOW_STOCK, Product.OUT_OF_STOCK};
        for (int i = 0; i < chips.length; i++) {
            final int val = values[i];
            chips[i].setOnClickListener(v -> {
                statusFilter = val;
                load();
            });
        }

        ListView list = (ListView) findViewById(R.id.list);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public Object getItem(int i) { return items.get(i); }
            @Override public long getItemId(int i) { return items.get(i).id; }
            @Override public View getView(int i, View convert, ViewGroup parent) {
                return productRow(InventoryActivity.this, parent, convert, items.get(i));
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent it = new Intent(this, ProductDetailActivity.class);
            it.putExtra(ProductDetailActivity.EXTRA_ID, items.get(pos).id);
            startActivity(it);
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { load(); }
        });
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (!loadingCategories) load();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, ProductFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
        load();
    }

    private void loadCategories() {
        long selected = selectedCategoryId();
        loadingCategories = true;
        categories = new ArrayList<>();
        categories.add(new Category(0, "All categories"));
        categories.addAll(repo.getCategories());
        spCategory.setAdapter(spinnerAdapter(categories));
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id == selected) spCategory.setSelection(i);
        }
        loadingCategories = false;
    }

    private long selectedCategoryId() {
        int pos = spCategory.getSelectedItemPosition();
        if (pos < 0 || pos >= categories.size()) return 0;
        return categories.get(pos).id;
    }

    private void load() {
        items.clear();
        items.addAll(repo.getProducts(search.getText().toString(), statusFilter, selectedCategoryId()));
        adapter.notifyDataSetChanged();

        int[] values = {-1, Product.IN_STOCK, Product.LOW_STOCK, Product.OUT_OF_STOCK};
        for (int i = 0; i < chips.length; i++) styleChip(chips[i], values[i] == statusFilter);
        ((TextView) findViewById(R.id.tv_count)).setText(items.size() + (items.size() == 1 ? " product" : " products"));
        findViewById(R.id.tv_empty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
