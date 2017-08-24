package com.agold.sos;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

/**
 * Created by root on 17-7-19.
 */

public class SmsSendStateReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = null;
        boolean error = true;
        switch (getResultCode()) {
            case Activity.RESULT_OK:
                message = "Message sent!";
                error = false;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                message = "Error.";
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                message = "Error: No service.";
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                message = "Error: Null PDU.";
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                message = "Error: Radio off.";
                break;
        }
        android.util.Log.i("ly20170719","show the state if sms send success--->" + message);
    }
}
