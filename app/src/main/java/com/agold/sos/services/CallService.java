package com.agold.sos.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.util.ArrayList;
import com.agold.sos.database.NumberProvider;
/**
 * Created by root on 17-4-27.
 * CallService 从数据库中读取紧急联系人信息，然后进行一轮的循环拨号任务
 * 可以提升的空间有 增加拨号顺序功能 增加拨号循环次数功能 增加成功拨号统计 增加拨号录音功能等
 */

public class CallService extends Service {

    private NumberProvider mNumberprovider;
    private Cursor mCursor;
    private Context mContext;
    private TelephonyManager telephonyManager;
    private PhoneStateListener mListener;
    private static int numberId = 0;
    private static boolean fromOffHook = false;
    private static boolean fromIdle = false;

    private ArrayList<String> numbers = new ArrayList<String>();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        android.util.Log.i("ly20170427", "CallService onBind");
        return null;
    }

    @Override
    public void onCreate() {
        android.util.Log.i("ly20170427", "CallService onCreate");
        android.util.Log.i("ly20170430","onCreate and the numberId is -->" + numberId);

        mContext = this;
        mNumberprovider = new NumberProvider(this);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mListener = new PhoneCallListener();
        telephonyManager.listen(mListener, PhoneCallListener.LISTEN_CALL_STATE);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170427", " return by permission" + "");
        }

        android.util.Log.i("ly20170427", "CallService onStartCommand");

        setNumbers();
        //check if the emergency number is NULL
        if (numbers.size() == 0) {
            Toast.makeText(this, "NO emergency number", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        numberId = 0;
        makeCall();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        android.util.Log.i("ly20170427", "CallService onDestory");
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
                android.util.Log.i("ly20170427", "this is number in data base -->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                android.util.Log.i("ly20170427", "this is name in data base -->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME)));
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

    public void makeCall() {
        android.util.Log.i("ly20170430","we gonna make the call and the numberId is --->"+numberId);
        if(numberId < numbers.size()) {
            //这里使用handler是为了解决可能出现拨号沉溺在后台的情况
            new Handler().postDelayed(new Runnable(){
                public void run() {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    android.util.Log.i("ly20170427", "makeCall");
                    Uri data = Uri.parse("tel:" + numbers.get(numberId));
                    callIntent.setData(data);
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivity(callIntent);
                    numberId = numberId + 1;
                }
            }, 500);
        }else{
            //在这里 stopService 以便再次使用次service
            stopSelf();
        }
    }

    public class PhoneCallListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    android.util.Log.i("ly20170427","CALL_STATE_OFFHOOK");
                    fromOffHook = true;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    android.util.Log.i("ly20170427","CALL_STATE_RINGING");
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    android.util.Log.i("ly20170427","CALL_STATE_IDLE");
                    fromIdle = true;
                    //两个状态检测 用于识别下一次拨号的条件
                    if(fromOffHook && fromIdle){
                        android.util.Log.i("ly20170430","the status matched and we gonna make the next call");
                        fromOffHook = false;
                        fromIdle = false;
                        makeCall();
                    }
                    break;
            }
        }
    }

}
