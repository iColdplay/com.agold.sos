package com.agold.sos.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.util.ArrayList;
import com.agold.sos.database.NumberProvider;

import static android.telephony.SmsManager.getDefault;

/**
 * Created by liuyi on 2017/4/30.
 * SmsService 用于实现一次性向所有紧急联系人发送短信
 * 短信内容可以定制
 */

public class SmsService extends Service {

    private NumberProvider mNumberprovider;
    private Cursor mCursor;
    private Context mContext;
    private ArrayList<String> numbers = new ArrayList<String>();
    private SmsManager smsManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.i("ly20170430", "SmsService onCreate");

        mContext = this;
        mNumberprovider = new NumberProvider(this);

        setNumbers();
        //check if the emergency number is NULL
        if (numbers.size() == 0) {
            Toast.makeText(this, "NO emergency number", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        smsManager = getDefault();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.i("ly20170430", "SmsService onSTartCommand");
        android.util.Log.i("ly20170430", "SmsService now we gonna send messages");
        sendMessage();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        android.util.Log.i("ly20170430", "SmsService onDestroy");
        super.onDestroy();
    }

    public void setNumbers() {
        mNumberprovider.open();
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mNumberprovider.query();
        if (mCursor != null) {
            mCursor.moveToFirst();
            numbers.clear();
            for (int i = 0; i < mCursor.getCount(); i++) {
                android.util.Log.i("ly20170430", "this is number in data base -->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                android.util.Log.i("ly20170430", "this is name in data base -->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME)));
                String number = mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM));
                if (!numbers.contains(number)) {
                    numbers.add(number);
                }
                mCursor.moveToNext();
            }
        }
        mCursor.close();
        mNumberprovider.close();
    }

    private void sendMessage(){
        Intent sendSucess = new Intent("agold.sos.sms.send.success");
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, sendSucess, PendingIntent.FLAG_UPDATE_CURRENT);
        for(int i = 0;i < numbers.size();i++){
            android.util.Log.i("ly20170430","send message to the --->" + numbers.get(i));
            //此处支持自定义短信内容
            smsManager.sendTextMessage(numbers.get(i),null,"fuck you!",pIntent,null);
        }
    }

}
