
package com.agold.sos.location;

import com.agold.sos.R;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public abstract class MapLocation {

    public static final float ZOOM = 16f;
    public static final int ID_ICON_MARKER = R.mipmap.navi_map_gps_locked;

    public Context mContext;
    private LinearLayout mContainerLayout;
    private LayoutParams mParams;

    public MapLocation(Context context, LinearLayout containerLayout) {
        mContext = context;
        mContainerLayout = containerLayout;
        mParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * 获取地图类型
     */
    public abstract String getMapType();

    /**
     * 获取到定位信息后添加地图View
     */
    public abstract void createMapView();

    /**
     * 将地图View添加到容器
     */
    public abstract void addMapView();

    /**
     * 移动到坐标
     */
    public abstract void moveCamera(double latitude, double longitude);

    /**
     * 绘制当前位置标记
     */
    public abstract void drawCurrentMarker(double latitude, double longitude);

    public abstract void onCreate(Bundle savedInstanceState);

    public abstract void onSaveInstanceState(Bundle outState);

    public abstract void onResume();

    public abstract void onPause();

    public abstract void onDestroy();

    /**
     * 清除
     */
    public abstract void clear();

    /**
     * 将地图View添加到容器
     */
    public void addView(View view) {
        if (view.getParent() == null) {
            mContainerLayout.addView(view, mParams);
        }
    }

    /**
     * 将地图View添加到容器
     */
    public void removeView(View view) {
        if (view.getParent() != null) {
            view.setVisibility(View.GONE);
            mContainerLayout.removeView(view);
        }
    }

}
