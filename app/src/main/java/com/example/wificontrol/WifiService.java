package com.example.wificontrol;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WifiService extends IntentService {

    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot";

    public WifiService() {
        super("WifiService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;

        String command = intent.getStringExtra("command");
        if (command == null) return;

        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (command.equals("off")) {
            // برای خاموش کردن، پیام‌ها را قبل از قطع وای‌فای می‌فرستیم
            sendBaleMessageWithRetry("📩 دستور دریافت شد: خاموش کردن وای‌فای");
            sendBaleMessageWithRetry("🔴 وای‌فای خاموش شد.");
            wifiManager.setWifiEnabled(false);
        } else if (command.equals("on")) {
            // وای‌فای را روشن می‌کنیم
            wifiManager.setWifiEnabled(true);

            // صبر می‌کنیم تا اتصال برقرار شود (مثلاً ۳۰ ثانیه)
            try {
                Thread.sleep(30000); // ۳۰ ثانیه
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // حالا پیام‌ها را ارسال می‌کنیم
            sendBaleMessageWithRetry("📩 دستور دریافت شد: روشن کردن وای‌فای");
            sendBaleMessageWithRetry("✅ وای‌فای روشن شد و به اینترنت متصل است.");
        }
    }

    private void sendBaleMessageWithRetry(String message) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (sendBaleMessage(message)) {
                return;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        showToast("ارسال پیام ناموفق ماند: " + message);
    }

    private boolean sendBaleMessage(String message) {
        try {
            String baseUrl = "https://tapi.bale.ai/bot" + BALE_BOT_TOKEN + "/sendMessage";
            String urlString = baseUrl +
                    "?chat_id=" + URLEncoder.encode(BALE_CHAT_ID, "UTF-8") +
                    "&text=" + URLEncoder.encode(message, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int responseCode = conn.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }

    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WifiService.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
