package com.agold.sos.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
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

public class CircularSmsService extends Service{

    private NumberProvider mNumberprovider;
    private Cursor mCursor;
    private Context mContext;
    private ArrayList<String> numbers = new ArrayList<String>();
    private SmsManager smsManager;


    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private static String location_info = null;

    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            locateAndSendMessages(mContext);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1,10*60000);

        }
    };

    private BroadcastReceiver mQuitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.i("ly20170526","now we know we should end this service");
            mContext.stopService(new Intent(mContext,CircularSmsService.class));
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
        mNumberprovider = new NumberProvider(this);

        setNumbers();
        //check if the emergency number is NULL
        if (numbers.size() == 0) {
            Toast.makeText(this, R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
            stopSelf();
        }
        smsManager = getDefault();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter quitFilter = new IntentFilter();
        quitFilter.addAction("agold.sos.quit");
        mContext.registerReceiver(mQuitReceiver,quitFilter);

        mHandler.removeMessages(1);
        mHandler.sendEmptyMessage(1);

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext.unregisterReceiver(mQuitReceiver);
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

    private void sendMessage(String location, String position){
        Intent sendSucess = new Intent("agold.sos.sms.send.success");
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, sendSucess, PendingIntent.FLAG_UPDATE_CURRENT);
        for(int i = 0;i < numbers.size();i++){
            android.util.Log.i("ly20170430","send message to the --->" + numbers.get(i));
            //此处支持自定义短信内容
            smsManager.sendTextMessage(numbers.get(i),null,getString(R.string.help_me)+getString(R.string.latitude_longitude)+location+getString(R.string.location_info)+position,pIntent,null);
        }
    }

    public void locateAndSendMessages(Context context){
        mlocationClient = new AMapLocationClient(context);
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                LatLng location = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                android.util.Log.i("ly20170523","show the location info--->"+location.toString());
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
                sendMessage(location.toString(),buffer.toString());
                //SmsService.this.onDestroy();
                mContext.stopService(new Intent(mContext,SmsService.class));
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


}
