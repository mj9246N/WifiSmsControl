package com.example.wificontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot";

    @Override
    public void onReceive(Context context, Intent intent) {
        // برای استفاده در کلاس داخلی، متغیر final می‌سازیم
        final Context finalContext = context;

        new Thread(new Runnable() {
            @Override
            public void run() {
                sendBaleMessageWithRetry(finalContext, "📩 دستور دریافت شد: روشن کردن وای‌فای");
                sendBaleMessageWithRetry(finalContext, "✅ وای‌فای روشن شد و به اینترنت متصل است.");
            }
        }).start();
    }

    private void sendBaleMessageWithRetry(final Context context, String message) {
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
        showToast(context, "ارسال پیام ناموفق ماند: " + message);
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

    private void showToast(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
