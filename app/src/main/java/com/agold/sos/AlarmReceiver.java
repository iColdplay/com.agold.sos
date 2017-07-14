package com.agold.sos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.agold.sos.services.CircularSmsService;

/**
 * Created by root on 17-7-13.
 */

public class AlarmReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        android.util.Log.i("ly20170713","wake up system to go another alarm");
        Intent i = new Intent(context, CircularSmsService.class);
        context.startService(i);
    }
}
