package com.agold.sos;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserManager;
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
import android.widget.Toast;

import com.agold.sos.database.NumberProvider;
import com.agold.sos.location.AMapLocationManager;
import com.agold.sos.location.GMapLocation;
import com.agold.sos.location.MapLocation;
import com.agold.sos.sensor.SensorEventHelper;
import com.agold.sos.services.CallService;
import com.agold.sos.services.CircularSmsService;
import com.agold.sos.services.ClearService;
import com.agold.sos.services.SmsService;
import com.agold.sos.utils.Utils;
import com.agold.sos.view.RecycleViewDivider;
import com.agold.sos.view.SlideView;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        AMapLocationListener {

    private TextView mTextMessage;
    private String key;
    private Context mContext;
    private NumberProvider mNumberprovider;

    private LinearLayout mContainerLayout;
    private AMapLocationManager mAMapLocationManager;
    private MapLocation mMapLocation;

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
                    if (mContainerLayout != null) {
                        mContainerLayout.setVisibility(View.VISIBLE);
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
                    if (mContainerLayout != null) {
                        mContainerLayout.setVisibility(View.INVISIBLE);
                    }
                    refreshContactFrag();
                    return true;
                case R.id.navigation_notifications:
                    if (mSildeLayout != null) {
                        mSildeLayout.setVisibility(View.VISIBLE);
                    }
                    if (mContainerLayout != null) {
                        mContainerLayout.setVisibility(View.INVISIBLE);
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
        android.util.Log.i("ly20170511", "onCreate method");

        //20170824 solve non user mode issues
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (!userManager.isSystemUser()) {
            android.util.Log.i("ly20170824", "this is not system user");
            Toast.makeText(getApplicationContext(), R.string.system_user_hint, Toast.LENGTH_LONG).show();
            this.finish();
        }
        //end

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.i("ly20170511", "no ACCESS_FINE_LOCATION");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS}, 2000);
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

        checkPermissions();

        setContentView(R.layout.activity_main);

        mSildeLayout = (LinearLayout) findViewById(R.id.sliders_view);
        mSildeLayout.setVisibility(View.INVISIBLE);

        mNullContactLayout = (LinearLayout) findViewById(R.id.null_contact_layout);
        mNullContactLayout.setVisibility(View.INVISIBLE);

        addContact = (TextView) findViewById(R.id.text_view_add);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View addContact = getLayoutInflater().inflate(R.layout.add_contact, null);
                final EditText editNumber = (EditText) addContact.findViewById(R.id.et_number);
                final EditText editName = (EditText) addContact.findViewById(R.id.et_name);
                builder.setView(addContact);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mNumberprovider.open();

                        String name = null;
                        String number = null;
                        if (editName.getText() != null) {
                            name = editName.getText().toString();

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

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                View addContact = getLayoutInflater().inflate(R.layout.add_contact, null);
                final EditText editNumber = (EditText) addContact.findViewById(R.id.et_number);
                final EditText editName = (EditText) addContact.findViewById(R.id.et_name);
                builder.setView(addContact);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mNumberprovider.open();

                        String name = null;
                        String number = null;
                        if (editName.getText() != null) {
                            name = editName.getText().toString();

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
        //init();


        callSlideView = ((SlideView) findViewById(R.id.slider1));
        callSlideView.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                if (!isEmptyContact()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.call_dialog_hint);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(600);

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
                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
                }
            }
        });

        smsSlideView = ((SlideView) findViewById(R.id.slider2));
        smsSlideView.setOnSlideCompleteListener(new SlideView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(SlideView slideView) {
                if (!isEmptyContact()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.sms_dialog_hint);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(600);

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
                } else {
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
                } else {
                    Toast.makeText(getApplicationContext(), R.string.no_emergency_number, Toast.LENGTH_SHORT).show();
                }
            }
        });
        mContainerLayout = (LinearLayout) findViewById(R.id.map_container);
        mLocationErrText = (TextView) findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
        mAMapLocationManager = new AMapLocationManager(this);
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Log.i("ly20170427", " return by permission" + "ACCESS_COARSE_LOCATION");
//            return;
//        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
//            Log.i("ly20170427", " return by permission" + "INTERNET");
//            return;
//        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Log.i("ly20170505", " return by permission" + "ACCESS_FINE_LOCATION");
//            return;
//        }
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

    public boolean isEmptyContact() {
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
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission) {
            mAMapLocationManager.registerLocationListener(this);
        }
        if (mMapLocation != null) {
            mMapLocation.onResume();
        }
    }


    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mAMapLocationManager.unregisterLocationListener();
        if (mMapLocation != null) {
            mMapLocation.onPause();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapLocation != null) {
            mMapLocation.onSaveInstanceState(outState);
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMapLocation != null) {
            mMapLocation.onDestroy();
        }
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
            mLocationErrText.setVisibility(View.GONE);
            showMapLocation(amapLocation);
        } else {
            String errText = "Error code: " + amapLocation.getErrorCode() + ", Location detail: " + amapLocation.getLocationDetail();
            mLocationErrText.setVisibility(View.VISIBLE);
            mLocationErrText.setText(errText);
        }
    }

    /**
     * 切换为高德地图显示
     */
    private void showMapLocation(AMapLocation amapLocation) {
        String type = "amap";
        if (!amapLocation.getCountry().equals("中国")) {
            type = "gmap";
        }
        if (mMapLocation == null || !mMapLocation.getMapType().equals(type)) {
            if (mMapLocation != null) {
                mMapLocation.clear();
            }
            mMapLocation = createMapLocation(type);
        }

        double latitude = amapLocation.getLatitude();
        double longitude = amapLocation.getLongitude();
        mMapLocation.moveCamera(latitude, longitude);
        mMapLocation.drawCurrentMarker(latitude, longitude);

    }

    private MapLocation createMapLocation(String type) {
        if ("gmap".equals(type)) {
            mMapLocation = new GMapLocation(this, mContainerLayout);
        } else {
            mMapLocation = new com.agold.sos.location.AMapLocation(this, mContainerLayout);
        }
        mMapLocation.createMapView();
        mMapLocation.onCreate(null);
        mMapLocation.onResume();
        mMapLocation.addMapView();
        return mMapLocation;
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
                android.util.Log.i("ly20170511", "onRequestPermissionsResult we dont have permission and we finish it");
                finish();
            } else {
                android.util.Log.i("ly20170511", "onRequestPermissionsResult we got the permission and we try to refresh the mapview");
                hasLocationPermission = true;
                mAMapLocationManager.registerLocationListener(this);
            }
        }
    }
}