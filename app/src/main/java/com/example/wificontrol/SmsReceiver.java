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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SmsReceiver extends BroadcastReceiver {

    // ⚙️ اطلاعات ربات بله شما
    private static final String BALE_BOT_TOKEN = "1049445193:OogZ6zCpmyqd1AGY1brAptWpK3mMsZE_RcE";
    private static final String BALE_CHAT_ID = "@pooovirbot"; // کانال یا چت موردنظر

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == null ||
                !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }

        // دریافت PendingResult برای ادامه‌ی کار در پس‌زمینه
        final PendingResult pendingResult = goAsync();

        // اجرای عملیات در یک Thread جداگانه
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleSms(context, intent);
                } catch (Exception e) {
                    // نمایش خطا با Toast
                    showToast(context, "خطای کلی: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // اعلام پایان کار به سیستم
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
                // ارسال پیام دریافت دستور
                try {
                    sendBaleMessage("📩 دستور دریافت شد: روشن کردن وای‌فای");
                } catch (Exception e) {
                    showToast(context, "خطا در ارسال پیام به بله: " + e.getMessage());
                }

                // اجرای دستور
                wifiManager.setWifiEnabled(true);

                // صبر برای اتصال و ارسال نتیجه
                try {
                    Thread.sleep(10000); // ۱۰ ثانیه صبر
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isWifiConnected(context)) {
                    try {
                        sendBaleMessage("✅ وای‌فای روشن شد و به اینترنت متصل است.");
                    } catch (Exception e) {
                        showToast(context, "خطا در ارسال پیام موفقیت به بله: " + e.getMessage());
                    }
                } else {
                    try {
                        sendBaleMessage("⚠️ وای‌فای روشن شد ولی هنوز اتصال برقرار نشده است.");
                    } catch (Exception e) {
                        showToast(context, "خطا در ارسال پیام هشدار به بله: " + e.getMessage());
                    }
                }

            } else if (messageBody.contains("wifioff")) {
                try {
                    sendBaleMessage("📩 دستور دریافت شد: خاموش کردن وای‌فای");
                } catch (Exception e) {
                    showToast(context, "خطا در ارسال پیام به بله: " + e.getMessage());
                }

                wifiManager.setWifiEnabled(false);

                try {
                    sendBaleMessage("🔴 وای‌فای خاموش شد.");
                } catch (Exception e) {
                    showToast(context, "خطا در ارسال پیام خاموشی به بله: " + e.getMessage());
                }
            }
        }
    }

    // بررسی اتصال واقعی به اینترنت از طریق وای‌فای
    private boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null &&
               networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
               networkInfo.isConnected();
    }

    // ارسال پیام به کانال بله
    private void sendBaleMessage(String message) throws Exception {
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
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Bale API response code: " + responseCode);
        }
    }

    // نمایش Toast از هر Thread
    private void showToast(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
