package com.agold.sos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.StringBuilderPrinter;
import android.widget.Toast;

import com.agold.sos.database.NumberProvider;
import com.agold.sos.services.CallService;

/**
 * Created by root on 17-5-23.
 */

public class PhoneWindowManagerReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneWindowManagerReceiver";
    private NumberProvider mNumberprovider;
    private Cursor mCursor;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        android.util.Log.i("ly20170523",TAG + " show this action-->"+action);
        if(("check.emergency.contact").equals(action)){
            android.util.Log.i("ly20170523",TAG+" now we should check if the there is any contact");
            mNumberprovider = new NumberProvider(context);
            mNumberprovider.open();
            if (mCursor != null) {
                mCursor.close();
            }
            mCursor = mNumberprovider.query();
            if (mCursor != null) {
                if (mCursor.getCount() > 0) {
                    Intent callService = new Intent(context, CallService.class);
                    context.startService(callService);
                }else{
                    android.util.Log.i("20170505", TAG+" now we think the database is not empty");
                    Toast.makeText(context,"No Emergency Contact",Toast.LENGTH_SHORT).show();
                }
            }
            mNumberprovider.close();
        }
    }
}
