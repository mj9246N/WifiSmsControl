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

    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot";

    // حداکثر زمان انتظار برای اتصال اینترنت (به میلی‌ثانیه)
    private static final int MAX_WAIT_FOR_INTERNET_MS = 60000; // 60 ثانیه
    private static final int CHECK_INTERVAL_MS = 5000; // هر ۵ ثانیه

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

                // ۲) منتظر برقراری اینترنت واقعی می‌شویم
                boolean internetAvailable = waitForInternet();

                // ۳) ارسال پیام‌ها
                if (internetAvailable) {
                    sendBaleMessageWithRetry(context, "📩 دستور دریافت شد: روشن کردن وای‌فای");
                    sendBaleMessageWithRetry(context, "✅ وای‌فای روشن شد و به اینترنت متصل است.");
                } else {
                    sendBaleMessageWithRetry(context, "⚠️ وای‌فای روشن شد ولی اتصال برقرار نشد.");
                }

            } else if (messageBody.contains("wifioff")) {
                // برای خاموش کردن، پیام‌ها را قبل از قطع اینترنت می‌فرستیم
                sendBaleMessageWithRetry(context, "📩 دستور دریافت شد: خاموش کردن وای‌فای");
                sendBaleMessageWithRetry(context, "🔴 وای‌فای خاموش شد.");
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    // منتظر می‌ماند تا اینترنت واقعاً در دسترس باشد (با تست اتصال)
    private boolean waitForInternet() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_FOR_INTERNET_MS) {
            if (isInternetAvailable()) {
                return true;
            }
            try {
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // تست اتصال واقعی به یک سرور مطمئن
    private boolean isInternetAvailable() {
        try {
            // تست اتصال به آدرس خود بله (یا هر سرور دیگر)
            URL url = new URL("https://tapi.bale.ai");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            return (code == 200 || code == 401); // 401 یعنی سرور پاسخ داد (اتصال برقرار است)
        } catch (Exception e) {
            return false;
        }
    }

    // ارسال پیام با چند بار تلاش
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
        if (context == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
