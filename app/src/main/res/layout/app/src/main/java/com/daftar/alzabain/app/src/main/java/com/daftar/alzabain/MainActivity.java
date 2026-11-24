package com.daftar.alzabain;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("التطبيق فتح بنجاح - جاري فحص العناصر");
        tv.setTextSize(20f);
        setContentView(tv);
    }
}
