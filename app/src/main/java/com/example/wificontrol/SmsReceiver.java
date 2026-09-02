package com.example.wificontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SmsReceiver extends BroadcastReceiver {

    // ⚙️ اطلاعات ربات بله شما
    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot"; // کانال یا چت موردنظر

    // زمان صبر بعد از روشن کردن وای‌فای (به میلی‌ثانیه)
    private static final int WIFI_ON_WAIT_MS = 25000; // ۲۵ ثانیه

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null ||
                !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }

        final PendingResult pendingResult = goAsync();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleSms(context, intent);
                } catch (Exception e) {
                    showToast(context, "خطای کلی: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    pendingResult.finish();
                }
            }
        }).start();
    }

    private void handleSms(Context context, Intent intent) throws Exception {
        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            String messageBody = sms.getMessageBody().toLowerCase();

            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            if (messageBody.contains("wifion")) {
                // ۱) وای‌فای را روشن می‌کنیم
                wifiManager.setWifiEnabled(true);

                // ۲) به‌جای تشخیص اتصال، صبر می‌کنیم تا وای‌فای زمان کافی برای اتصال داشته باشد
                Thread.sleep(WIFI_ON_WAIT_MS);

                // ۳) پیام‌ها را ارسال می‌کنیم (با تلاش مجدد در صورت خطا)
                sendBaleMessageWithRetry(context, "📩 دستور دریافت شد: روشن کردن وای‌فای", 3);
                sendBaleMessageWithRetry(context, "✅ وای‌فای روشن شد و به اینترنت متصل است.", 3);

            } else if (messageBody.contains("wifioff")) {
                // برای خاموش کردن، ابتدا پیام‌ها را می‌فرستیم و بعد وای‌فای را خاموش می‌کنیم
                sendBaleMessageWithRetry(context, "📩 دستور دریافت شد: خاموش کردن وای‌فای", 3);
                sendBaleMessageWithRetry(context, "🔴 وای‌فای خاموش شد.", 3);

                wifiManager.setWifiEnabled(false);
            }
        }
    }

    // ارسال پیام با چند بار تلاش در صورت خطا
    private void sendBaleMessageWithRetry(final Context context, String message, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            boolean success = sendBaleMessage(message);
            if (success) {
                return;
            }
            // اگر خطا بود، چند ثانیه صبر می‌کنیم و دوباره تلاش می‌کنیم
            try {
                Thread.sleep(5000); // ۵ ثانیه بین تلاش‌ها
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        showToast(context, "ارسال پیام به بله ناموفق ماند: " + message);
    }

    // ارسال پیام به بله و برگرداندن true در صورت موفقیت
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
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // نمایش Toast از هر Thread
    private void showToast(final Context context, final String message) {
        if (context == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
