package com.example.wificontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView statusText = findViewById(R.id.statusText);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS},
                    SMS_PERMISSION_CODE);
        } else {
            statusText.setText("وضعیت: برنامه آماده است.\nبا پیامک wifion یا wifioff وای‌فای را کنترل کنید.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            TextView statusText = findViewById(R.id.statusText);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("مجوز صادر شد.\nآماده دریافت فرمان با پیامک.");
                Toast.makeText(this, "مجوز پیامک با موفقیت داده شد", Toast.LENGTH_SHORT).show();
            } else {
                statusText.setText("مجوز رد شد!\nبرنامه نمی‌تواند پیامک بخواند.");
                Toast.makeText(this, "برای کارکرد برنامه مجوز پیامک الزامی است", Toast.LENGTH_LONG).show();
            }
        }
    }
}
