package edu.purdue.android.fuzzer.squibble;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Intent Fuzzer Service
 *
 * @author ebarsallo
 */

public class IntentFuzzerService extends Service {

    private static final String TAG = "FUZZ/IFuzzService";

    /* Android Wear Client */
    private WearClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Start IntentFuzzerService");

        /* Get instance of Wear Client */
        client = WearClient.getInstance(this);

        /* Enable notifications */
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Intent Fuzzer");
        builder.setContentText("Ready for some fuzzing");
        builder.setSmallIcon(R.mipmap.ic_launcher);

        startForeground(1, builder.build());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy IntentFuzzerService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
