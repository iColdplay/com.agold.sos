package com.agold.sos.services;

import com.agold.sos.*;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.agold.sos.R;
import com.agold.sos.database.NumberProvider;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;


import static android.telephony.SmsManager.getDefault;

/**
 * Created by root on 17-5-26.
 */

public class CircularSmsService extends Service {

    private NumberProvider mNumberprovider;
    private Cursor mCursor;
    private Context mContext;
    private ArrayList<String> numbers = new ArrayList<String>();
    private SmsManager smsManager;


    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private static String location_info = null;

    private LocationManager mLocationManager;
    private Location mLocation;

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
            Toast.makeText(this, R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        smsManager = getDefault();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.i("ly20170430", "SmsService onSTartCommand");
        android.util.Log.i("ly20170430", "SmsService now we gonna send messages");

        //20170713 wake system
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int triggerTime = 10 * 60 * 1000;
        long keepTime = SystemClock.elapsedRealtime() + triggerTime;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, keepTime, pendingIntent);
        //end

        locateAndSendMessages(this);
//        Intent gotoMmsApplication = new Intent();
//        gotoMmsApplication.setClassName("com.android.mms","com.android.mms.ui.ConversationList");
//        gotoMmsApplication.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        this.startActivity(gotoMmsApplication);

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

    private void sendMessage(String location, String position) {
        Intent sendSucess = new Intent("agold.sos.sms.send.success");
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, sendSucess, PendingIntent.FLAG_UPDATE_CURRENT);
        for (int i = 0; i < numbers.size(); i++) {
            android.util.Log.i("ly20170430", "send message to the --->" + numbers.get(i));
            //此处支持自定义短信内容
            try {
                android.util.Log.i("ly20170718", "now we should know the default Subscription-->" + SmsManager.getDefaultSmsSubscriptionId());
                if (SmsManager.getDefaultSmsSubscriptionId() != 1 && SmsManager.getDefaultSmsSubscriptionId() != 2) {
                    Toast.makeText(this, R.string.sms_set_default_id, Toast.LENGTH_SHORT).show();
                    return;
                }
                SmsManager duo = SmsManager.getSmsManagerForSubscriptionId(SmsManager.getDefaultSmsSubscriptionId());
                duo.sendTextMessage(numbers.get(i), null,
                        getString(R.string.sms_help_me) + getString(R.string.sms_location) + location + getString(R.string.sms_position) + position, pIntent, null);
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.i("ly20170718", "send sms failed");
                Toast.makeText(this, R.string.send_sms_failed, Toast.LENGTH_SHORT).show();
            }
        }
        //ly 20170712 遇到了CallService一样的问题 回调销毁service方法
        this.onDestroy();
    }

    public void locateAndSendMessages(Context context) {
        mlocationClient = new AMapLocationClient(context);
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                LatLng location = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                android.util.Log.i("ly20170523", "show the location info--->" + location.toString());
                aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息
                aMapLocation.getCountry();//国家信息
                aMapLocation.getProvince();//省信息
                aMapLocation.getCity();//城市信息
                aMapLocation.getDistrict();//城区信息
                aMapLocation.getStreet();//街道信息
                aMapLocation.getStreetNum();//街道门牌号信息
                aMapLocation.getCityCode();//城市编码
                aMapLocation.getAdCode();//地区编码

                StringBuffer buffer = new StringBuffer();
                buffer.append(aMapLocation.getCountry() + ""
                        + aMapLocation.getProvince() + ""
                        + aMapLocation.getCity() + ""
                        + aMapLocation.getDistrict() + ""
                        + aMapLocation.getStreet() + ""
                        + aMapLocation.getStreetNum());

                android.util.Log.i("ly20170523", "onLocationChanged() location information -->" + buffer.toString());
                location_info = buffer.toString();
                mlocationClient.stopLocation();
                mlocationClient.onDestroy();
                if (location.toString().contains("(0.0,0.0)")) {
                    android.util.Log.i("ly20171011", "no netWork");
                    if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    }else{
                        Toast.makeText(mContext, R.string.no_gps_hint, Toast.LENGTH_SHORT);
                    }

                }else {
                    sendMessage(location.toString(), buffer.toString());
                    //SmsService.this.onDestroy();
                    mContext.stopService(new Intent(mContext, SmsService.class));
                }
            }
        });
        //设置为高精度定位模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回位置信息
        mLocationOption.setNeedAddress(true);
        mLocationOption.setGpsFirst(true);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除

        mlocationClient.startLocation();
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            android.util.Log.i("ly20171011", "we got a location by call back");
            mLocation = location;
            mLocationManager.removeUpdates(locationListener);
            String locationInfo = mLocation.getLatitude() + " " + mLocation.getLongitude();
            sendMessage(locationInfo, " ");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


}
