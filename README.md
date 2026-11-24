# Easy Tracker – Android Stock Management (MVP v1.0)

Offline stock tracker by **Christocentricus Innovations**. Java + XML + SQLite. The UI follows the logo's colour theme
(deep navy `#050B17`, electric blue `#2F8FE8`, green `#3DBE7E`, ice `#BAECFF`) – see `res/values/colors.xml`.

## Requirements
- Minimum Android: **5.0 (API 21)**; target/compile SDK 34
- Android Studio Hedgehog (2023.1) or newer, JDK 17

## Open the project
1. Unzip `EasyTracker_Android_MVP.zip`.
2. Android Studio → **File ▸ Open** → select the `EasyTracker` folder. Let Gradle sync (needs internet once to download dependencies).

## Build the APK
**Android Studio:** *Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)*. Output: `app/build/outputs/apk/debug/app-debug.apk`.

**Command line:** `gradle assembleDebug` (or `./gradlew assembleDebug` after Android Studio generates the wrapper).

**GitHub (no local setup):** create a repository, push this folder's contents, open the **Actions** tab ▸ *Build APK* ▸ *Run workflow*.
Download the `EasyTracker-debug-apk` artifact when it finishes.

## Install the APK
Copy `app-debug.apk` to the phone, open it, and allow "Install unknown apps" if asked. Or run `adb install app-debug.apk`.

## Features (v1.0)
Dashboard (live totals, low/out-of-stock, recent movements, quick actions) · Product add/edit/delete/details/search ·
Categories (add new from the product form) · Stock In / Stock Out (negative stock blocked) · Inventory with search, status and
category filters · Full movement history (tap a row for details) · Settings (currency, valuation method, CSV export, database backup).

## Project structure
```
app/src/main/java/com/christocentricus/easytracker/
  db/      DbHelper (schema), Repository (all SQL + transactions)
  model/   Product, Category, StockTransaction, DashboardStats
  ui/      BaseActivity (nav + shared rows), Dashboard, Inventory, ProductForm,
           ProductDetail, Stock (in/out), History, Settings
  util/    Fmt (formatting/parsing), Prefs (currency, valuation)
app/src/main/res/  layouts, drawables, values (colors/themes), launcher icons made from the logo
```

## Database (`easytracker.db`, SQLite)
- **categories**(id PK, name UNIQUE)
- **products**(id PK, name, sku UNIQUE, category_id FK→categories, purchase_price, selling_price,
  quantity CHECK ≥ 0, min_stock, description, date_added)
- **stock_transactions**(id PK, product_id FK→products ON DELETE SET NULL, product_name, product_sku, txn_type IN/OUT,
  quantity, prev_qty, new_qty, unit_price, note, txn_date)
- Indexes on product name, category, transaction product and date. Every stock change updates the product and inserts the
  movement inside one database transaction. Deleting a product keeps its history (name/SKU are stored on each movement).
- Status rules: **Out of Stock** = 0 · **Low Stock** = quantity ≤ minimum threshold · otherwise **In Stock**.

## Roadmap
1.1 Reporting · 1.2 Business management (suppliers, customers, barcode) · 1.3 Accounts, cloud sync. Database restore is planned for 1.1.
