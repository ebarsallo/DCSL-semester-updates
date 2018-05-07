package edu.purdue.android.fuzzer.squibble;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.wearable.compat.WearableActivityController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.jar.Manifest;

import com.google.android.wearable.*;

import edu.purdue.android.fuzzer.squibble.common.IntentFuzzer;


/**
 * @author ebarsallo
 */
public class MainActivityWear extends WearableActivity {

    private static final String TAG = "FUZZ/Wear";

    private static final int ID_PERMISSIONS_REQUEST = 412;

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mTextCurrent;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate MainActivityWear wear");
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.txt_main);
        mClockView = (TextView) findViewById(R.id.clock);
        mTextCurrent = (TextView) findViewById(R.id.txt_component);

        IntentFuzzer.getInstance(this);

        // Start Listener as a service running in the background
        if ( !isServiceRunning() )
            startService(new Intent(this, IntentFuzzerListenerService.class));

        // Check permissions
        checkPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Stopping `Listener` service");
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.VISIBLE);
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mTextCurrent.setTextColor(getResources().getColor(android.R.color.white));

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mClockView.setVisibility(View.GONE);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mTextCurrent.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("edu.purdue.android.fuzzer.squibble.IntentFuzzerListenerService".equals(info.service.getClassName())) {
                return true;
            }
        }

        return false;
    }



    /* ---------------------------------------------------------------------------
     * Extras
     * ---------------------------------------------------------------------------
     */

    void temp() {
        WearableActivityController controller;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ID_PERMISSIONS_REQUEST: {

                for (int i=0; i<permissions.length; i++) {
                    Log.i(TAG, "Permissions: " + permissions[i] + " {" + grantResults[i] + "}");
                }

            }
        }
    }

    public void checkPermission(){

        String list[] = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.WAKE_LOCK
            };
        ArrayList<String> toAsk = new ArrayList<>();

        for (String permission : list) {
            int permissionState = ActivityCompat.checkSelfPermission(this, permission);
            if ( permissionState != PackageManager.PERMISSION_GRANTED) {
                toAsk.add(permission);
            }

            Log.i(TAG, "Permission: " + permission + " {" + permissionState + "}");
        }

        if (!toAsk.isEmpty())
            ActivityCompat.requestPermissions(this, toAsk.toArray(new String[toAsk.size()]), ID_PERMISSIONS_REQUEST);

    }


}
