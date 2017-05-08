package com.agold.sos.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by root on 17-5-3.
 */

public class ClearService extends Service{


    @Override
    public void onCreate() {
        android.util.Log.i("ly20170503","ClearService onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.i("ly20170503","ClearService onStartCommand");

        //非system app 无法调用的接口以及操作 通过广播发送到系统进程中
        Intent clearIntent = new Intent("agold.sos.clear.init");
        sendBroadcast(clearIntent);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        android.util.Log.i("ly20170503","ClearService onDestory");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
