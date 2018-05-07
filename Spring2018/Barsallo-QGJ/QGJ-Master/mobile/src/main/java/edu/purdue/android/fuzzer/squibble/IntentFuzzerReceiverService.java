package edu.purdue.android.fuzzer.squibble;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;


/**
 * Intent Fuzzer Receiver Service
 * Receive the messages from the Android Wearable device.
 *
 * @author ebarsallo
 */

public class IntentFuzzerReceiverService extends WearableListenerService {

    private static final String TAG = "FUZZ/IFuzzReceiver";

    private static final String LOAD_DATA = "/load/";
    private static final String FUZZ_DATA = "/fuzz/";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged");

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String  path = dataEvent.getDataItem().getUri().getPath();

                /* Loading Victims (Activities, Services, etc) */
                if (path.startsWith(LOAD_DATA)) {
                    Log.d(TAG, "onDataChanged | ComponentNames ...");

                    ArrayList<String> list = dataMap.getStringArrayList("components");
                    int collisions = dataMap.getInt("collision");

                    Log.d(TAG, String.format("onDataChanged | size {%d} collision {%d}",
                            list.size(), collisions));

                    for (String x: list) {
                        Log.d(TAG, " -> " + x);
                    }

                    // Broadcast message to activity for display
                    Intent msgIntent = new Intent();
                    msgIntent.setAction(Intent.ACTION_SEND);
                    msgIntent.putExtra("message", list);
                    msgIntent.putExtra("collision", collisions);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(msgIntent);

                }
                /* Fuzzing Results */
                else if (path.startsWith(FUZZ_DATA)) {

                }

            }
        }
    }

}
