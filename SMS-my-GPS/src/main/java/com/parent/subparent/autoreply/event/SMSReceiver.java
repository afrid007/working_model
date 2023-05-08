package com.parent.subparent.autoreply.event;

import com.parent.subparent.autoreply.R;
import com.parent.subparent.autoreply.data_model.ListItem;
import com.parent.subparent.autoreply.data_model.Preferences;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import java.util.ArrayList;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG               = "SMSReceiver";
    private static final String TEXT_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String DATA_SMS_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";

    public void onReceive(Context context, Intent intent) {
        if (!Preferences.isEnabled(context))
            return;

        final String action = intent.getAction();
        final Bundle extras = intent.getExtras();

        if (extras == null)
            return;

        final ArrayList<ListItem> listItems = Preferences.getListItems(context);

        if (listItems.isEmpty())
            return;

        SmsMessage[] messages = null;
        String sender         = null;
        String body           = null;
        boolean is_match      = false;

        if (!is_match && action.equals(TEXT_SMS_RECEIVED)) {
            messages = get_SmsMessages(extras);

            for (SmsMessage message : messages) {
                if (message == null)
                    continue;

                try {
                    sender   = message.getOriginatingAddress().trim();
                    body     = message.getMessageBody().trim();
                    is_match = ListItem.matches(listItems, sender, body);  // must match at least one whitelist rule ('phone number' and 'message prefix' pair)

                    if (is_match)
                        break;
                }
                catch (Exception e) { continue; }
            }

            if (is_match) {
                Log.i(TAG, "SMS received.\nfrom: " + sender + "\nmessage: " + body);

                com.parent.subparent.autoreply.event.GPSSender.notify(context,sender);
            }
        }

        if (!is_match && action.equals(DATA_SMS_RECEIVED)) {
            messages = get_SmsMessages(extras);

            byte[] data;
            StringBuilder data_sb;

            for (SmsMessage message : messages) {
                if (message == null)
                    continue;

                try {
                    data = message.getUserData();
                    if ((data == null) || (data.length == 0)) continue;
                    if (data.length != 11) continue;

                    data_sb = new StringBuilder();
                    for (byte b : data) {
                        data_sb.append(String.format("%02x", b));
                    }
                    body = data_sb.toString().toLowerCase();
                    if (!body.equals("0a0603b0af8203066a0005")) continue;

                    sender   = message.getOriginatingAddress().trim();
                    is_match = ListItem.matches(listItems, sender);  // must originate from a whitelisted phone number (excluding globs)

                    if (is_match)
                        break;
                }
                catch (Exception e) { continue; }
            }

            if (is_match) {
                Log.i(TAG, "Silent SMS received.\nfrom: " + sender);

                com.parent.subparent.autoreply.event.GPSSender.notify(context, sender);
            }
        }
    }

    private final static SmsMessage[] get_SmsMessages(Bundle extras) {
        final Object[] pdus = (Object[])extras.get("pdus");
        String format = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            format = extras.getString("format", "3gpp");
        }
        final SmsMessage[] messages = new SmsMessage[pdus.length];

        for (int i = 0; i < pdus.length; i++) {
            try {
                messages[i] = (Build.VERSION.SDK_INT >= 23)
                    ? SmsMessage.createFromPdu((byte[])pdus[i], format)
                    : SmsMessage.createFromPdu((byte[])pdus[i]);
            }
            catch (Exception e) {}
        }
        return messages;
    }
}
