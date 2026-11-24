package com.daftar.alzabain;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextView tvDebts, tvPayments, tvNet;
    private EditText etSearch;
    private ListView lvCustomers;
    private final ArrayList<Integer> customerIds = new ArrayList<>();
    private final ArrayList<String> customerItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            db = new DatabaseHelper(this);
            tvDebts = findViewById(R.id.tvDebts);
            tvPayments = findViewById(R.id.tvPayments);
            tvNet = findViewById(R.id.tvNet);
            etSearch = findViewById(R.id.etSearch);
            lvCustomers = findViewById(R.id.lvCustomers);
            Button btnAddCustomer = findViewById(R.id.btnAddCustomer);

            if (btnAddCustomer != null) {
                btnAddCustomer.setOnClickListener(v -> showAddCustomerDialog());
            }

            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        refreshData(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }

            if (lvCustomers != null) {
                lvCustomers.setOnItemClickListener((parent, view, position, id) -> {
                    int custId = customerIds.get(position);
                    String label = customerItems.get(position);
                    showAddTransactionDialog(custId, label);
                });
            }

            refreshData("");
        } catch (Exception e) {
            Toast.makeText(this, "سبب الخروج: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshData(String query) {
        if (db == null) return;

        double totalDebts = 0;
        double totalPayments = 0;

        Cursor totalsCursor = db.getReadableDatabase().rawQuery(
                "SELECT " +
                        "COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), " +
                        "COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) " +
                        "FROM transactions",
                null
        );
        if (totalsCursor != null) {
            if (totalsCursor.moveToFirst()) {
                totalDebts = totalsCursor.getDouble(0);
                totalPayments = totalsCursor.getDouble(1);
            }
            totalsCursor.close();
        }

        double net = totalDebts - totalPayments;
        if (tvDebts != null) {
            tvDebts.setText(String.format(Locale.getDefault(), "الديون: %.2f", totalDebts));
        }
        if (tvPayments != null) {
            tvPayments.setText(String.format(Locale.getDefault(), "الدفعات: %.2f", totalPayments));
        }
        if (tvNet != null) {
            tvNet.setText(String.format(Locale.getDefault(), "الصافي: %.2f", net));
        }

        customerIds.clear();
        customerItems.clear();

        String sql = "SELECT c.id, c.name, c.phone, " +
                "COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance " +
                "FROM customers c " +
                "LEFT JOIN transactions t ON c.id = t.customer_id ";
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            sql += "WHERE c.name LIKE ? OR c.phone LIKE ? ";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }
        sql += "GROUP BY c.id, c.name, c.phone ORDER BY c.name ASC";

        Cursor cursor = db.getReadableDatabase().rawQuery(sql, args);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String phone = cursor.getString(2);
                double balance = cursor.getDouble(3);

                customerIds.add(id);
                String phonePart = (phone != null && !phone.trim().isEmpty()) ? " - " + phone : "";
                customerItems.add(name + phonePart + " | الرصيد: " + String.format(Locale.getDefault(), "%.2f", balance));
            }
            cursor.close();
        }

        if (lvCustomers != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    customerItems
            );
            lvCustomers.setAdapter(adapter);
        }
    }

    private void showAddCustomerDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etName = new EditText(this);
        etName.setHint("اسم الزبون");
        layout.addView(etName);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("رقم الجوال (اختياري)");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("إضافة زبون")
                .setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "يرجى إدخال اسم الزبون", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put("name", name);
                    values.put("phone", phone);
                    db.getWritableDatabase().insert("customers", null, values);
                    refreshData(etSearch != null ? etSearch.getText().toString() : "");
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAddTransactionDialog(int customerId, String customerLabel) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("المبلغ");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etNote = new EditText(this);
        etNote.setHint("ملاحظة (اختياري)");
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle(customerLabel)
                .setView(layout)
                .setPositiveButton("تسجيل دين", (dialog, which) -> saveTransaction(customerId, "DEBT", etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNeutralButton("تسجيل دفعة", (dialog, which) -> saveTransaction(customerId, "PAYMENT", etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void saveTransaction(int customerId, String type, String amountStr, String note) {
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.ContentValues values = new android.content.ContentValues();
            values.put("customer_id", customerId);
            values.put("type", type);
            values.put("amount", amount);
            values.put("note", note);
            db.getWritableDatabase().insert("transactions", null, values);
            refreshData(etSearch != null ? etSearch.getText().toString() : "");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "صيغة المبلغ غير صحيحة", Toast.LENGTH_SHORT).show();
        }
    }
}
