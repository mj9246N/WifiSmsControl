package com.example.wificontrol;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WifiService extends Service {

    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot";
    private static final long WIFI_ON_DELAY_MS = 30000; // ۳۰ ثانیه

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        final String command = intent.getStringExtra("command");
        if (command == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        final int finalStartId = startId;

        // اجرای کار در Thread جداگانه (بدون صف)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleCommand(command);
                } catch (Exception e) {
                    showToast("خطا: " + e.getMessage());
                } finally {
                    stopSelf(finalStartId);
                }
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void handleCommand(String command) {
        WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (command.equals("off")) {
            // برای خاموش کردن، پیام‌ها را قبل از قطع وای‌فای می‌فرستیم
            sendBaleMessageWithRetry("📩 دستور دریافت شد: خاموش کردن وای‌فای");
            sendBaleMessageWithRetry("🔴 وای‌فای خاموش شد.");
            wifiManager.setWifiEnabled(false);

        } else if (command.equals("on")) {
            // ۱) وای‌فای را بلافاصله روشن می‌کنیم
            wifiManager.setWifiEnabled(true);

            // ۲) یک آلارم برای ۳۰ ثانیه بعد تنظیم می‌کنیم
            scheduleDelayedMessage();
        }
    }

    private void scheduleDelayedMessage() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + WIFI_ON_DELAY_MS;
        if (alarmManager != null) {
            // استفاده از setExactAndAllowWhileIdle برای اجرای دقیق‌تر در حالت Doze
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
