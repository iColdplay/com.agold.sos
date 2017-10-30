
package com.agold.sos.location;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GMapLocation extends MapLocation implements OnMapReadyCallback {

    private static final String TAG = "Sos/GMapTrack";

    private GoogleMap mMap;
    private Marker mCurrentMarker;
    private MapView mMapView;

    public GMapLocation(Context context, LinearLayout containerLayout) {
        super(context, containerLayout);
    }

    @Override
    public String getMapType() {
        return "gmap";
    }

    @Override
    public void createMapView() {
        int result = MapsInitializer.initialize(mContext);
        Log.d(TAG, "createMapView: result = " + result);
        // 没有做判断，result=0时才支持Google地图
        mMapView = new MapView(mContext);
    }

    @Override
    public void addMapView() {
        addView(mMapView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
