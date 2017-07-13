package com.agold.sos;

import com.agold.sos.database.NumberProvider;
import com.agold.sos.sensor.SensorEventHelper;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.Toast;

import com.agold.sos.services.CallService;
import com.agold.sos.services.CircularSmsService;
import com.agold.sos.services.SmsService;
import com.agold.sos.services.ClearService;
import com.agold.sos.utils.PermissionHelper;
import com.agold.sos.utils.Utils;
import com.agold.sos.view.SlideView;
import com.agold.sos.view.RecycleViewDivider;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener {

    private TextView mTextMessage;
    private String key;
    private Context mContext;
    private NumberProvider mNumberprovider;

    private AMap aMap;
    private MapView mapView;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;

    private TextView mLocationErrText;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private boolean mFirstFix = false;
    private Marker mLocMarker;
    private SensorEventHelper mSensorHelper = null;
    private Circle mCircle;
    public static final String LOCATION_MARKER_FLAG = "mylocation";
    private static String location_info = null;

    private LinearLayout mSildeLayout;
    private SlideView callSlideView;
    private SlideView smsSlideView;
    private SlideView sosSlideView;

    private LinearLayout mNullContactLayout;
    private TextView addContact;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter mAdapter;
    private ArrayList<String> mDatas;
    private ArrayList<String> mNames;
    private Cursor mCursor;

    private FloatingActionMenu fab_menu;
    private FloatingActionButton fab1;
    private RelativeLayout relativeLayout;

    private static boolean hasLocationPermission = true;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    if (mapView != null) {
                        mapView.setVisibility(View.VISIBLE);
                    }
                    if (mSildeLayout != null) {
                        mSildeLayout.setVisibility(View.INVISIBLE);
                    }
                    if (mNullContactLayout != null) {
                        mNullContactLayout.setVisibility(View.INVISIBLE);
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.INVISIBLE);
                        relativeLayout.setVisibility(View.INVISIBLE);
                    }
                    return true;
                case R.id.navigation_dashboard:
                    if (mNullContactLayout != null) {
                        mNullContactLayout.setVisibility(View.VISIBLE);
                    }
                    if (mSildeLayout != null) {
                        mSildeLayout.setVisibility(View.INVISIBLE);
                    }
                    if (mapView != null) {
                        mapView.setVisibility(View.INVISIBLE);
                    }
                    refreshContactFrag();
                    return true;
                case R.id.navigation_notifications:
                    if (mSildeLayout != null) {
                        mSildeLayout.setVisibility(View.VISIBLE);
                    }
                    if (mapView != null) {
                        mapView.setVisibility(View.INVISIBLE);
                    }
                    if (mNullContactLayout != null) {
                        mNullContactLayout.setVisibility(View.INVISIBLE);
                    }
                    if (recyclerView != null) {
                        recyclerView.setVisibility(View.INVISIBLE);
                        relativeLayout.setVisibility(View.INVISIBLE);
                    }
                    return true;
            }
            return false;
        }
    };

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.i("ly20170511","onCreate method");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170511", "no ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2000);
            hasLocationPermission = false;
        }
        //全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        IntentFilter notifier = new IntentFilter();
        notifier.addAction("agold.sos.should.refresh");
        this.registerReceiver(mBroadcastReceiver, notifier);

        mContext = this;
        mNumberprovider = new NumberProvider(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170427", " return by permission" + "CALL_PHONE");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170427", " return by permission" + "ACCESS_COARSE_LOCATION");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170427", " return by permission" + "INTERNET");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170505", " return by permission" + "ACCESS_FINE_LOCATION");
        }

        setContentView(R.layout.activity_main);

        mSildeLayout = (LinearLayout) findViewById(R.id.sliders_view);
        mSildeLayout.setVisibility(View.INVISIBLE);

        mNullContactLayout = (LinearLayout) findViewById(R.id.null_contact_layout);
        mNullContactLayout.setVisibility(View.INVISIBLE);

        addContact = (TextView) findViewById(R.id.text_view_add);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.i("ly20170505", "MainActivity onCreate click addContact");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View addContact = getLayoutInflater().inflate(R.layout.add_contact, null);
                final EditText editNumber = (EditText) addContact.findViewById(R.id.et_number);
                final EditText editName = (EditText) addContact.findViewById(R.id.et_name);
                builder.setView(addContact);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mNumberprovider.open();
                        android.util.Log.i("ly20170419", "now we click the ok button");
                        String name = null;
                        String number = null;
                        if (editName.getText() != null) {
                            name = editName.getText().toString();
                            android.util.Log.i("ly20170505", "now we set the data name --->" + name);
                        }
                        if (editNumber.getText() != null) {
                            number = editNumber.getText().toString();
                        }
                        if (number != null && !TextUtils.isEmpty(number)) {
                            Long insertResult = mNumberprovider.insertData(name, number, 1);
                            if (insertResult > 0) {
                                Toast.makeText(mContext, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                        mNumberprovider.close();
                        refreshContactFrag();

                    }
                });
                builder.create().show();
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setVisibility(View.INVISIBLE);
        fab_menu = (FloatingActionMenu) findViewById(R.id.fab_menu);
        fab1 = (FloatingActionButton) findViewById(R.id.fab_1);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fab_menu.close(true);
                android.util.Log.i("ly20170509", "MainActivity onCreate click fab");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View addContact = getLayoutInflater().inflate(R.layout.add_contact, null);
                final EditText editNumber = (EditText) addContact.findViewById(R.id.et_number);
                final EditText editName = (EditText) addContact.findViewById(R.id.et_name);
                builder.setView(addContact);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mNumberprovider.open();
                        android.util.Log.i("ly20170509", "now we click the ok button");
                        String name = null;
                        String number = null;
                        if (editName.getText() != null) {
                            name = editName.getText().toString();
                            android.util.Log.i("ly20170509", "now we set the data name --->" + name);
                        }
                        if (editNumber.getText() != null) {
                            number = editNumber.getText().toString();
                        }
                        if (number != null && !TextUtils.isEmpty(number)) {
                            Long insertResult = mNumberprovider.insertData(name, number, 1);
                            if (insertResult > 0) {
                                Toast.makeText(mContext, R.string.success, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, R.string.fail, Toast.LENGTH_SHORT).show();
                            }
                        }
                        mNumberprovider.close();
                        refreshContactFrag();

                    }
                });
                builder.create().show();
            }
        });

        relativeLayout = (RelativeLayout) findViewById(R.id.list_view);
        relativeLayout.setVisibility(View.INVISIBLE);

        mDatas = new ArrayList<String>();
        mNames = new ArrayList<String>();
        initData();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new RecycleViewDivider(this, LinearLayoutManager.HORIZONTAL, R.drawable.divider_bg01));
        mAdapter = new RecyclerViewAdapter(mContext, mDatas, mNames);
        mAdapter.setMode(com.daimajia.swipe.util.Attributes.Mode.Single);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setOnScrollListener(onScrollListener);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        //init();
        android.util.Log.i("ly20170427", "SHA1-->" + Utils.getSHA1(this));

        callSlideView = ((SlideView) findViewById(R.id.slider1));
        callSlideView.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                if(!isEmptyContact()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.call_dialog_hint);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(600);
                            android.util.Log.i("ly20170418", "slider 1 complete");
                            Intent callService = new Intent(getApplicationContext(), CallService.class);
                            startService(callService);

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.create().show();
                }else{
                    Toast.makeText(getApplicationContext(), R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
                }
            }
        });

        smsSlideView = ((SlideView) findViewById(R.id.slider2));
        smsSlideView.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                if(!isEmptyContact()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.sms_dialog_hint);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(600);
                            android.util.Log.i("ly20170418", "slider 2 complete");
                            Intent smsService = new Intent(getApplicationContext(), SmsService.class);
                            startService(smsService);

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.create().show();
                }else{
                    Toast.makeText(getApplicationContext(), R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
                }
            }
        });

        sosSlideView = ((SlideView) findViewById(R.id.slider3));
        sosSlideView.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {

                if (!isEmptyContact()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.sos_dialog_hint);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(600);
                            android.util.Log.i("ly20170418", "slider 3 complete");
                            Intent sosService = new Intent(getApplicationContext(), ClearService.class);
                            startService(sosService);
                            Intent circularSms = new Intent(getApplicationContext(), CircularSmsService.class);
                            startService(circularSms);

                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.create().show();
                }else {
                    Toast.makeText(getApplicationContext(), R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void refreshContactFrag() {
        boolean emptyContact = true;
        mNumberprovider.open();
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mNumberprovider.query();
        if (mCursor != null) {
            if (mCursor.getCount() > 0) {
                emptyContact = false;
                android.util.Log.i("20170505", "now we think the database is not empty");
            }
        }
        if (!emptyContact) {
            if (mNullContactLayout != null) {
                mNullContactLayout.setVisibility(View.INVISIBLE);
            }
            if (recyclerView != null) {
                initData();
                recyclerView.setAdapter(mAdapter);
                recyclerView.setVisibility(View.VISIBLE);
                relativeLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (mNullContactLayout != null) {
                mNullContactLayout.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.INVISIBLE);
                relativeLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    public boolean isEmptyContact(){
        boolean emptyContact = true;
        mNumberprovider.open();
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mNumberprovider.query();
        if (mCursor != null) {
            if (mCursor.getCount() > 0) {
                emptyContact = false;
                android.util.Log.i("20170505", "now we think the database is not empty");
            }
        }
        return emptyContact;
    }

    /**
     * Substitute for our onScrollListener for RecyclerView
     */
    RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.e("ListView", "onScrollStateChanged");
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            // Could hide open views here if you wanted. //
        }
    };

    private void initData() {
        android.util.Log.i("ly20170504", "initData");
        mNumberprovider.open();
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mNumberprovider.query();
        if (mCursor != null) {
            mDatas.clear();
            mNames.clear();
            mCursor.moveToFirst();
            for (int i = 0; i < mCursor.getCount(); i++) {
                android.util.Log.i("ly20170504", "initData contact number -->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                android.util.Log.i("ly20170504", "initData contact name-->" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME)));
                mDatas.add("" + mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NUM)));
                if (mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME)).isEmpty()) {
                    mNames.add(getString(R.string.contact_no_name));
                } else {
                    mNames.add(mCursor.getString(mCursor.getColumnIndexOrThrow(NumberProvider.KEY_NAME)));
                }
                mCursor.moveToNext();
            }
        }
        mCursor.close();
        mNumberprovider.close();
    }

    /**
     * 初始化
     */
    private void init() {

        if (aMap == null) {
            android.util.Log.i("ly20170511","init() aMap is NOT null");
            aMap = mapView.getMap();
            if (aMap == null) {
                android.util.Log.i("ly20170408", "amap is null RETURN");
                return;
            } else {
                android.util.Log.i("ly20170408", "aMap is not NULL");
            }
            setUpMap();
        }else{
            android.util.Log.i("ly20170511","init() aMap is null");
        }
        if (mSensorHelper == null) {
            android.util.Log.i("ly20170511", "init() now we create the new SensorHelper");
            mSensorHelper = new SensorEventHelper(this);
            if (mSensorHelper != null) {
                android.util.Log.i("ly20170511", "init() now the SensorHelper is gonna register");
                mSensorHelper.registerSensorListener();
                mSensorHelper.setCurrentMarker(mLocMarker);
            }
        }
        mLocationErrText = (TextView) findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE); // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        android.util.Log.i("ly20170511","onResume method");
        super.onResume();
        android.util.Log.i("ly20170511","onResume we init mapView here");
        if(hasLocationPermission){
            init();
        }
        mapView.onResume();
        if(mlocationClient != null){
            mlocationClient.startLocation();
        }
    }


//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           String[] permissions, int[] grantResults) {
//        android.util.Log.i("ly20170509", " is not granted !");
//        PermissionHelper.getInstance().onPermissionsResult(requestCode, permissions, grantResults);
//    }
//
//    private PermissionHelper.PermissionCallback mPermissionCallback = new PermissionHelper.PermissionCallback() {
//        public void onPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//            if (grantResults != null && grantResults.length > 0) {
//                mAllGranted = true;
//                for (int i = 0; i < grantResults.length; i++) {
//                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
//                        mAllGranted = false;
//                        android.util.Log.i("ly20170509", permissions[i] + " is not granted !");
//                        break;
//                    }
//                }
//                if (!mAllGranted) {
//                    String toastStr = "禁止的权限";
//                    Toast.makeText(getApplicationContext(), toastStr, Toast.LENGTH_LONG).show();
//                    finish();
//                }else{
//                    init();
//                    mapView.onResume();
//                }
//            }
//        }
//    };


    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }
        mapView.onPause();
        deactivate();
        mFirstFix = false;
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocMarker != null) {
            android.util.Log.i("ly20170516","now we destory mLocMarker");
            mLocMarker.destroy();
        }
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {

                mLocationErrText.setVisibility(View.GONE);
                LatLng location = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());

                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                amapLocation.getLatitude();//获取纬度
                amapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间

                android.util.Log.i("20170412", "onLocationChanged() location getLocationType -->" + amapLocation.getLocationType());
                android.util.Log.i("20170412", "onLocationChanged() location getLatitude -->" + amapLocation.getLatitude());
                android.util.Log.i("20170412", "onLocationChanged() location getAccuracy -->" + amapLocation.getAccuracy());
                android.util.Log.i("20170412", "onLocationChanged() location time -->" + df.format(date));

                amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息
                amapLocation.getCountry();//国家信息
                amapLocation.getProvince();//省信息
                amapLocation.getCity();//城市信息
                amapLocation.getDistrict();//城区信息
                amapLocation.getStreet();//街道信息
                amapLocation.getStreetNum();//街道门牌号信息
                amapLocation.getCityCode();//城市编码
                amapLocation.getAdCode();//地区编码

                StringBuffer buffer = new StringBuffer();
                buffer.append(amapLocation.getCountry() + ""
                        + amapLocation.getProvince() + ""
                        + amapLocation.getCity() + ""
                        + amapLocation.getDistrict() + ""
                        + amapLocation.getStreet() + ""
                        + amapLocation.getStreetNum());

                android.util.Log.i("20170412", "onLocationChanged() location information -->" + buffer.toString());
                location_info = buffer.toString();

                if (!mFirstFix) {
                    mFirstFix = true;
                    addCircle(location, amapLocation.getAccuracy());//添加精度圆
                    addMarker(location);//添加定位图标
                    android.util.Log.i("ly20170511", "use sensor helper to change the direction");
                    mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 18));
                    //mListener.onLocationChanged(amapLocation);//点击定位图标可以将视图移动到定位的位置
                } else {
                    mCircle.setCenter(location);
                    mCircle.setRadius(amapLocation.getAccuracy());
                    mLocMarker.setPosition(location);
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(location));
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                android.util.Log.i("AmapErr", errText);
                mLocationErrText.setVisibility(View.VISIBLE);
                mLocationErrText.setText(errText);
            }
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
            //设置是否返回位置信息
            mLocationOption.setNeedAddress(true);
            //设置定位循环时间
            mLocationOption.setInterval(1000);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
//        mListener = null;
//        if (mlocationClient != null) {
//            mlocationClient.stopLocation();
//            mlocationClient.onDestroy();
//        }
//        mlocationClient = null;
        if(mlocationClient != null){
            mlocationClient.stopLocation();
        }
    }

    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        options.visible(false);
        mCircle =  aMap.addCircle(options);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked)));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = aMap.addMarker(options);
        if (location_info != null) {
            mLocMarker.setTitle(location_info);
        } else {
            mLocMarker.setTitle(LOCATION_MARKER_FLAG);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.i("ly20170505", "receive a broadcast -->" + intent.getAction());
            initData();
            if (mNullContactLayout.getVisibility() == View.VISIBLE || recyclerView.getVisibility() == View.VISIBLE) {
                refreshContactFrag();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), "No LOCATION Permission", Toast.LENGTH_LONG).show();
                android.util.Log.i("ly20170511","onRequestPermissionsResult we dont have permission and we finish it");
                finish();
            }else{
                android.util.Log.i("ly20170511","onRequestPermissionsResult we got the permission and we try to refresh the mapview");
                init();
                hasLocationPermission = true;
            }
        }
    }
}