package com.christocentricus.easytracker.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.model.Product;
import com.christocentricus.easytracker.util.Fmt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** One screen for both Stock In and Stock Out (mode passed in the intent). */
public class StockActivity extends BaseActivity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private boolean isIn;
    private List<Product> products = new ArrayList<>();
    private Spinner spProduct;
    private EditText etQty, etPrice, etNote;
    private TextView tvCurrent, tvDate;
    private final Calendar date = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        isIn = !"OUT".equals(getIntent().getStringExtra(EXTRA_MODE));
        spProduct = (Spinner) findViewById(R.id.sp_product);
        etQty = (EditText) findViewById(R.id.et_qty);
        etPrice = (EditText) findViewById(R.id.et_price);
        etNote = (EditText) findViewById(R.id.et_note);
        tvCurrent = (TextView) findViewById(R.id.tv_current);
        tvDate = (TextView) findViewById(R.id.tv_date);

        ((TextView) findViewById(R.id.tv_title)).setText(isIn ? "STOCK IN" : "STOCK OUT");
        ((TextView) findViewById(R.id.tv_subtitle)).setText(
                isIn ? "Record stock received" : "Record stock issued or sold");
        ((TextView) findViewById(R.id.lbl_qty)).setText(isIn ? "Quantity received *" : "Quantity removed *");
        ((TextView) findViewById(R.id.lbl_note)).setText(isIn ? "Notes (optional)" : "Reason (optional)");
        etNote.setHint(isIn ? "e.g. Supplier delivery" : "e.g. Sale, damaged, internal use");
        Button save = (Button) findViewById(R.id.btn_save);
        save.setText(isIn ? "Record stock in" : "Record stock out");
        if (!isIn) {
            findViewById(R.id.lbl_price).setVisibility(View.GONE);
            etPrice.setVisibility(View.GONE);
        }

        products = repo.getProducts(null, -1, 0);
        if (products.isEmpty()) {
            toast("Add a product first.");
            finish();
            return;
        }
        spProduct.setAdapter(spinnerAdapter(products));
        long preselect = getIntent().getLongExtra(EXTRA_PRODUCT_ID, 0);
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).id == preselect) spProduct.setSelection(i);
        }
        spProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { showCurrent(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        updateDateText();
        tvDate.setOnClickListener(v -> new DatePickerDialog(this, android.R.style.Theme_Material_Dialog_Alert,
                (view, y, m, d) -> {
                    date.set(y, m, d);
                    updateDateText();
                }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show());

        save.setOnClickListener(v -> save());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
    }

    private Product selected() {
        int pos = spProduct.getSelectedItemPosition();
        if (pos < 0 || pos >= products.size()) return null;
        // Re-read so the quantity shown is always current.
        return repo.getProduct(products.get(pos).id);
    }

    private void showCurrent() {
        Product p = selected();
        if (p == null) return;
        tvCurrent.setText(Fmt.number(p.quantity));
        if (isIn) etPrice.setHint("Current: " + Fmt.plain(p.purchasePrice));
    }

    private void updateDateText() {
        tvDate.setText(Fmt.date(date.getTimeInMillis()));
    }

    private void save() {
        Product p = selected();
        if (p == null) {
            toast("Select a product.");
            return;
        }
        Integer qty = Fmt.parseInt(etQty.getText().toString());
        if (qty == null || qty <= 0) {
            etQty.setError("Enter a whole number greater than 0");
            etQty.requestFocus();
            return;
        }
        if (!isIn && qty > p.quantity) {
            etQty.setError("Only " + p.quantity + " unit(s) in stock");
            etQty.requestFocus();
            return;
        }
        Double price = null;
        if (isIn && !etPrice.getText().toString().trim().isEmpty()) {
            price = Fmt.parseDouble(etPrice.getText().toString());
            if (price == null || price < 0) {
                etPrice.setError("Enter a valid price (0 or more)");
                etPrice.requestFocus();
                return;
            }
        }

        // Selected day combined with the current time of day, so history sorts correctly.
        Calendar now = Calendar.getInstance();
        Calendar when = (Calendar) date.clone();
        when.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        when.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        when.set(Calendar.SECOND, now.get(Calendar.SECOND));
        long ts = when.getTimeInMillis();
        if (ts > System.currentTimeMillis()) ts = System.currentTimeMillis();

        String note = etNote.getText().toString();
        String error = isIn ? repo.stockIn(p.id, qty, price, ts, note) : repo.stockOut(p.id, qty, ts, note);
        if (error != null) {
            toast(error);
            showCurrent();
            return;
        }
        toast(isIn ? "Stock received: +" + qty : "Stock issued: -" + qty);
        finish();
    }
}
