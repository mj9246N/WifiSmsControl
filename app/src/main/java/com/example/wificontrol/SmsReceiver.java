package com.example.wificontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {

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

            // ساخت intent برای سرویس
            Intent serviceIntent = new Intent(context, WifiService.class);
            serviceIntent.putExtra("command", messageBody.contains("wifion") ? "on" : "off");
            serviceIntent.putExtra("sender", sms.getOriginatingAddress());

            // شروع سرویس
            context.startService(serviceIntent);
        }
    }
}
