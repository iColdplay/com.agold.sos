
package com.agold.sos.location;

import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

public class AMapLocation extends MapLocation {

    private AMap mMap;
    private Marker mCurrentMarker;
    private TextureMapView mMapView;

    public AMapLocation(Context context, LinearLayout containerLayout) {
        super(context, containerLayout);
    }

    @Override
    public String getMapType() {
        return "amap";
    }

    @Override
    public void createMapView() {
        mMapView = new TextureMapView(mContext);
    }

    @Override
    public void addMapView() {
        addView(mMapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        mMap = mMapView.getMap();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
    }

    public void moveCamera(double latitude, double longitude) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), ZOOM));
    }

    @Override
    public void drawCurrentMarker(double latitude, double longitude) {
        if (mCurrentMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title("Current");
            markerOptions.position(new LatLng(latitude, longitude));
            markerOptions.icon(BitmapDescriptorFactory.fromResource(ID_ICON_MARKER));
            mCurrentMarker = mMap.addMarker(markerOptions);
        } else {
            mCurrentMarker.setPosition(new LatLng(latitude, longitude));
        }
    }

    @Override
    public void clear() {
        mCurrentMarker = null;
        mMap.clear();
        removeView(mMapView);
        mMapView.onDestroy();
    }

}
