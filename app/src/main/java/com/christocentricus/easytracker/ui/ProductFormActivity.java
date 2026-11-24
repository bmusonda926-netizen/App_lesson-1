package com.christocentricus.easytracker.ui;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.Category;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.util.Fmt;

import java.util.List;

public class ProductFormActivity extends BaseActivity {
    public static final String EXTRA_ID = "product_id";

    private long productId;
    private Product existing;
    private EditText etName, etSku, etPurchase, etSelling, etQty, etMin, etDesc;
    private Spinner spCategory;
    private List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_form);

        etName = (EditText) findViewById(R.id.et_name);
        etSku = (EditText) findViewById(R.id.et_sku);
        etPurchase = (EditText) findViewById(R.id.et_purchase);
        etSelling = (EditText) findViewById(R.id.et_selling);
        etQty = (EditText) findViewById(R.id.et_qty);
        etMin = (EditText) findViewById(R.id.et_min);
        etDesc = (EditText) findViewById(R.id.et_desc);
        spCategory = (Spinner) findViewById(R.id.sp_category);

        productId = getIntent().getLongExtra(EXTRA_ID, 0);
        loadCategories(0);

        if (productId > 0) {
            existing = repo.getProduct(productId);
            if (existing == null) {
                toast("Product not found.");
                finish();
                return;
            }
            ((TextView) findViewById(R.id.tv_title)).setText("EDIT PRODUCT");
            etName.setText(existing.name);
            etSku.setText(existing.sku);
            etPurchase.setText(Fmt.plain(existing.purchasePrice));
            etSelling.setText(Fmt.plain(existing.sellingPrice));
            etQty.setText(String.valueOf(existing.quantity));
            etQty.setEnabled(false);
            etQty.setAlpha(0.5f);
            ((TextView) findViewById(R.id.lbl_qty)).setText("Current quantity");
            findViewById(R.id.tv_qty_note).setVisibility(View.VISIBLE);
            etMin.setText(String.valueOf(existing.minStock));
            etDesc.setText(existing.description);
            selectCategory(existing.categoryId);
        } else {
            etSku.setText(repo.suggestSku());
        }

        findViewById(R.id.btn_auto_sku).setOnClickListener(v -> etSku.setText(repo.suggestSku()));
        findViewById(R.id.btn_new_category).setOnClickListener(v -> showNewCategory());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private void loadCategories(long select) {
        categories = repo.getCategories();
        spCategory.setAdapter(spinnerAdapter(categories));
        if (select > 0) selectCategory(select);
    }

    private void selectCategory(long id) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id == id) {
                spCategory.setSelection(i);
                return;
            }
        }
    }

    private void showNewCategory() {
        final EditText input = new EditText(this);
        input.setHint("Category name");
        input.setSingleLine(true);
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_hint));
        new AlertDialog.Builder(this, R.style.ET_Dialog)
                .setTitle("New category")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        toast("Enter a category name.");
                        return;
                    }
                    long id = repo.addCategory(name);
                    if (id < 0) {
                        toast("That category already exists.");
                    } else {
                        loadCategories(id);
                    }
                })
                .show();
    }

    private void save() {
        String name = etName.getText().toString().trim();
        String sku = etSku.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Product name is required");
            etName.requestFocus();
            return;
        }
        if (sku.isEmpty()) {
            etSku.setError("SKU / code is required");
            etSku.requestFocus();
            return;
        }
        if (repo.skuExists(sku, productId)) {
            etSku.setError("This product code is already in use");
            etSku.requestFocus();
            return;
        }

        double purchase = 0, selling = 0;
        String ps = etPurchase.getText().toString();
        if (!ps.trim().isEmpty()) {
            Double d = Fmt.parseDouble(ps);
            if (d == null || d < 0) {
                etPurchase.setError("Enter a valid price (0 or more)");
                etPurchase.requestFocus();
                return;
            }
            purchase = d;
        }
        String ss = etSelling.getText().toString();
        if (!ss.trim().isEmpty()) {
            Double d = Fmt.parseDouble(ss);
            if (d == null || d < 0) {
                etSelling.setError("Enter a valid price (0 or more)");
                etSelling.requestFocus();
                return;
            }
            selling = d;
        }

        int qty = 0;
        if (existing == null && !etQty.getText().toString().trim().isEmpty()) {
            Integer q = Fmt.parseInt(etQty.getText().toString());
            if (q == null || q < 0) {
                etQty.setError("Enter a whole number (0 or more)");
                etQty.requestFocus();
                return;
            }
            qty = q;
        }
        int min = 0;
        if (!etMin.getText().toString().trim().isEmpty()) {
            Integer m = Fmt.parseInt(etMin.getText().toString());
            if (m == null || m < 0) {
                etMin.setError("Enter a whole number (0 or more)");
                etMin.requestFocus();
                return;
            }
            min = m;
        }

        Product p = existing != null ? existing : new Product();
        p.name = name;
        p.sku = sku;
        p.purchasePrice = purchase;
        p.sellingPrice = selling;
        p.minStock = min;
        p.description = etDesc.getText().toString();
        int pos = spCategory.getSelectedItemPosition();
        p.categoryId = (pos >= 0 && pos < categories.size()) ? categories.get(pos).id : 0;

        boolean ok;
        if (existing != null) {
            ok = repo.updateProduct(p);
        } else {
            p.quantity = qty;
            ok = repo.insertProduct(p) > 0;
        }
        if (ok) {
            toast(existing != null ? "Product updated." : "Product added.");
            finish();
        } else {
            toast("Could not save the product. The code may already exist.");
        }
    }
}
