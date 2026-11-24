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

        db = new DatabaseHelper(this);
        tvDebts = findViewById(R.id.tvDebts);
        tvPayments = findViewById(R.id.tvPayments);
        tvNet = findViewById(R.id.tvNet);
        etSearch = findViewById(R.id.etSearch);
        lvCustomers = findViewById(R.id.lvCustomers);
        Button btnAddCustomer = findViewById(R.id.btnAddCustomer);

        btnAddCustomer.setOnClickListener(v -> showAddCustomerDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        lvCustomers.setOnItemClickListener((parent, view, position, id) -> {
            int custId = customerIds.get(position);
            String label = customerItems.get(position);
            showAddTransactionDialog(custId, label);
        });

        refreshData("");
    }

    private void refreshData(String keyword) {
        double[] stats = db.getSummary();
        tvDebts.setText(String.format(Locale.US, "الديون\n%.2f", stats[0]));
        tvPayments.setText(String.format(Locale.US, "المدفوعات\n%.2f", stats[1]));
        tvNet.setText(String.format(Locale.US, "المتبقي\n%.2f", stats[2]));

        customerIds.clear();
        customerItems.clear();
        Cursor c = db.getAllCustomers(keyword);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            String name = c.getString(1);
            String phone = c.getString(2);
            double bal = c.getDouble(3);
            customerIds.add(id);
            customerItems.add(name + " (" + (phone == null ? "-" : phone) + ") — الرصيد: " +
                    String.format(Locale.US, "%.2f", bal));
        }
        c.close();

        lvCustomers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, customerItems));
    }

    private void showAddCustomerDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etName = new EditText(this);
        etName.setHint("اسم الزبون");
        EditText etPhone = new EditText(this);
        etPhone.setHint("رقم الهاتف");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);

        layout.addView(etName);
        layout.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("إضافة زبون جديد")
                .setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (!name.isEmpty()) {
                        db.addCustomer(name, phone);
                        refreshData(etSearch.getText().toString());
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAddTransactionDialog(int custId, String title) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etAmount = new EditText(this);
        etAmount.setHint("المبلغ");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText etNote = new EditText(this);
        etNote.setHint("ملاحظة");

        layout.addView(etAmount);
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("إضافة دين (+)", (d, w) -> saveTx(custId, "DEBT", etAmount, etNote))
                .setNeutralButton("تسجيل دفعة (-)", (d, w) -> saveTx(custId, "PAYMENT", etAmount, etNote))
                .setNegativeButton("إغلاق", null)
                .show();
    }

    private void saveTx(int custId, String type, EditText etAmount, EditText etNote) {
        try {
            double amount = Double.parseDouble(etAmount.getText().toString().trim());
            if (amount > 0) {
                db.addTransaction(custId, type, amount, etNote.getText().toString().trim());
                refreshData(etSearch.getText().toString());
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {}
    }
}
