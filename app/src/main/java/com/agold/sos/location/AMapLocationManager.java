
package com.agold.sos.location;

import android.content.Context;
import android.util.Log;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

public class AMapLocationManager {

    private static final String TAG = "Sos/AMapLocationManager";

    private Context mContext;
    private AMapLocationClient mLocationClient;

    public AMapLocationManager(Context context) {
        mContext = context;
    }

    public void registerLocationListener(AMapLocationListener listener) {
        if (mLocationClient != null) {
            Log.d(TAG, "registerLocationListener: The client already exists.");
            return;
        }
        mLocationClient = new AMapLocationClient(mContext);
        // 构造定位参数
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        // 设置定位监听
        mLocationClient.setLocationListener(listener);
        // 设置定位模式
        mLocationOption.setLocationMode(AMapLocationMode.Battery_Saving);
        // 设置定位间隔，单位毫秒，最低1000`
        mLocationOption.setInterval(2000);
        // 设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        mLocationClient.startLocation();

    }

    public void unregisterLocationListener() {
        if (mLocationClient == null) {
            Log.d(TAG, "unregisterLocationListener: The client not exists.");
            return;
        }
        mLocationClient.stopLocation();
        mLocationClient.onDestroy();
        mLocationClient = null;
    }

}
