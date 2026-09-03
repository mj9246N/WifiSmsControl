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

            // بررسی سن پیامک (برای جلوگیری از اجرای پیامک‌های قدیمی)
            long smsTime = sms.getTimestampMillis();
            long now = System.currentTimeMillis();
            if (smsTime > 0 && (now - smsTime) > MAX_SMS_AGE_MS) {
                Toast.makeText(context, "پیامک قدیمی نادیده گرفته شد", Toast.LENGTH_SHORT).show();
                continue;
            }

            // تعیین دستور بر اساس محتوای پیامک
            String command = null;
            if (messageBody.contains("wifion")) {
                command = "on";
            } else if (messageBody.contains("wifioff")) {
                command = "off";
            }

            // اگر دستور null باشد، یعنی پیامک مربوط به ما نیست → نادیده بگیر
            if (command == null) {
                continue;
            }

            // نمایش Toast (اختیاری)
            Toast.makeText(context, "پیام دریافت شد: " + messageBody, Toast.LENGTH_SHORT).show();

            // ساخت intent برای سرویس
            Intent serviceIntent = new Intent(context, WifiService.class);
            serviceIntent.putExtra("command", command);
            serviceIntent.putExtra("sender", sms.getOriginatingAddress());
            context.startService(serviceIntent);
        }
    }
}
