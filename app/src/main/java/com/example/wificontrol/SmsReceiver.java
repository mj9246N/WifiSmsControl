package com.example.wificontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

                // ۲) منتظر اتصال واقعی می‌مانیم (حداکثر ۶۰ ثانیه، بررسی هر ۵ ثانیه)
                boolean connected = waitForWifiConnection(context, 12, 5000);

                // ۳) حالا که اینترنت برقرار است (یا نشده)، پیام‌ها را ارسال می‌کنیم
                if (connected) {
                    sendBaleMessage("📩 دستور دریافت شد: روشن کردن وای‌فای");
                    sendBaleMessage("✅ وای‌فای روشن شد و به اینترنت متصل است.");
                } else {
                    // در این حالت اینترنت قطع است، اما سعی می‌کنیم پیام هشدار را بفرستیم
                    sendBaleMessage("⚠️ وای‌فای روشن شد ولی اتصال برقرار نشد.");
                }

            } else if (messageBody.contains("wifioff")) {
                // چون وای‌فای هنوز روشن است، هر دو پیام را قبل از خاموش کردن می‌فرستیم
                sendBaleMessage("📩 دستور دریافت شد: خاموش کردن وای‌فای");
                sendBaleMessage("🔴 وای‌فای خاموش شد.");

                // سپس وای‌فای را خاموش می‌کنیم
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    // حلقه انتظار برای اتصال وای‌فای
    private boolean waitForWifiConnection(Context context, int maxAttempts, int delayMs) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isWifiConnected(context)) {
                return true;
            }
        }
        return false;
    }

    // بررسی اتصال واقعی به اینترنت از طریق وای‌فای
    private boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null &&
               networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
               networkInfo.isConnected();
    }

    // ارسال پیام به کانال بله
    private void sendBaleMessage(String message) {
        try {
            String baseUrl = "https://tapi.bale.ai/bot" + BALE_BOT_TOKEN + "/sendMessage";
            String urlString = baseUrl +
                    "?chat_id=" + URLEncoder.encode(BALE_CHAT_ID, "UTF-8") +
                    "&text=" + URLEncoder.encode(message, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Bale API response code: " + responseCode);
            }
        } catch (Exception e) {
            // برای نمایش خطا از context موجود در کلاس استفاده می‌کنیم
            // اینجا context نداریم، بنابراین خطا را فقط log می‌کنیم (یا به روش دیگر)
            e.printStackTrace();
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
