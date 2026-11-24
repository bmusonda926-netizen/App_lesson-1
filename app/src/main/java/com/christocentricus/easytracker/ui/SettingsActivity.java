package com.christocentricus.easytracker.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.christocentricus.easytracker.BuildConfig;
import com.christocentricus.easytracker.R;
import com.christocentricus.easytracker.util.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {
    private static final int REQ_CSV = 101;
    private static final int REQ_BACKUP = 102;
    private static final List<String> CURRENCIES = Arrays.asList(
            "ZMW", "USD", "EUR", "GBP", "ZAR", "KES", "TZS", "UGX", "NGN", "GHS", "BWP", "MWK", "INR", "AUD", "CAD");

    private TextView chipCost, chipRetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupNav(NAV_SETTINGS);

        ((TextView) findViewById(R.id.tv_version)).setText("Version " + BuildConfig.VERSION_NAME + " (MVP)");

        Spinner sp = (Spinner) findViewById(R.id.sp_currency);
        sp.setAdapter(spinnerAdapter(CURRENCIES));
        sp.setSelection(Math.max(0, CURRENCIES.indexOf(Prefs.currency(this))));
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Prefs.setCurrency(SettingsActivity.this, CURRENCIES.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        chipCost = (TextView) findViewById(R.id.chip_cost);
        chipRetail = (TextView) findViewById(R.id.chip_retail);
        chipCost.setOnClickListener(v -> { Prefs.setValuation(this, Prefs.VALUATION_COST); refreshChips(); });
        chipRetail.setOnClickListener(v -> { Prefs.setValuation(this, Prefs.VALUATION_RETAIL); refreshChips(); });
        refreshChips();

        findViewById(R.id.btn_csv).setOnClickListener(v -> createFile(REQ_CSV, "text/csv",
                "EasyTracker_inventory_" + stamp() + ".csv"));
        findViewById(R.id.btn_backup).setOnClickListener(v -> createFile(REQ_BACKUP,
                "application/octet-stream", "EasyTracker_backup_" + stamp() + ".db"));
    }

    private void refreshChips() {
        boolean retail = Prefs.VALUATION_RETAIL.equals(Prefs.valuation(this));
        styleChip(chipCost, !retail);
        styleChip(chipRetail, retail);
    }

    private String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
    }

    private void createFile(int req, String mime, String name) {
        Intent it = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        it.addCategory(Intent.CATEGORY_OPENABLE);
        it.setType(mime);
        it.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(it, req);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) throw new IllegalStateException("Cannot open destination");
            try {
                if (requestCode == REQ_CSV) {
                    out.write(repo.buildInventoryCsv().getBytes(StandardCharsets.UTF_8));
                    toast("Inventory exported.");
                } else if (requestCode == REQ_BACKUP) {
                    File db = repo.databaseFile(this);
                    InputStream in = new FileInputStream(db);
                    try {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    } finally {
                        in.close();
                    }
                    toast("Database backup saved.");
                }
            } finally {
                out.close();
            }
        } catch (Exception e) {
            toast("Export failed: " + e.getMessage());
        }
    }
}
