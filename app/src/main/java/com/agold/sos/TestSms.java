package com.agold.sos;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

/**
 * Created by root on 17-7-19.
 */

public class TestSms extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {

        android.util.Log.i("ly20170727","now we test sms function");

        SmsManager sms = SmsManager.getDefault();

        sms.sendTextMessage("13052532871", null, "救命", PendingIntent.getBroadcast(
                context, 0, new Intent("test.send.sms"), 0), null);

    }
}
