package com.example.wificontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    // حداکثر سن مجاز پیامک (۵ دقیقه)
    private static final long MAX_SMS_AGE_MS = 5 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null ||
                !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }

        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            String messageBody = sms.getMessageBody().toLowerCase();

            // بررسی سن پیامک
            long smsTime = sms.getTimestampMillis();
            long now = System.currentTimeMillis();

            // اگر timestamp موجود بود و پیامک قدیمی‌تر از حد مجاز است، نادیده بگیر
            if (smsTime > 0 && (now - smsTime) > MAX_SMS_AGE_MS) {
                Toast.makeText(context, "پیامک قدیمی نادیده گرفته شد", Toast.LENGTH_SHORT).show();
                continue;
            }

            // نمایش Toast (اختیاری)
            Toast.makeText(context, "پیام دریافت شد: " + messageBody, Toast.LENGTH_SHORT).show();

            // ساخت intent برای سرویس
            Intent serviceIntent = new Intent(context, WifiService.class);
            serviceIntent.putExtra("command", messageBody.contains("wifion") ? "on" : "off");
            serviceIntent.putExtra("sender", sms.getOriginatingAddress());
            context.startService(serviceIntent);
        }
    }
}
