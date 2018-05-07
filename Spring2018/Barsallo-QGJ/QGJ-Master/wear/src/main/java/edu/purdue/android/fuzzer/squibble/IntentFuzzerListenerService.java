package edu.purdue.android.fuzzer.squibble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;

import edu.purdue.android.fuzzer.squibble.common.FuzzType;
import edu.purdue.android.fuzzer.squibble.common.IFuzzAction;
import edu.purdue.android.fuzzer.squibble.common.IPCType;
import edu.purdue.android.fuzzer.squibble.common.IntentFuzzer;

/**
 * Intent Fuzzer Listener Service.
 * Listen for actions requested by the paired device (phone).
 *
 * @author ebarsallo
 */

public class IntentFuzzerListenerService extends WearableListenerService {

    private static final String TAG = "FUZZ/ListenerService";

    private Context context;

    /* Actions */
    private static final String LOAD_ACTION        = "/load";
    private static final String FUZZ_SINGLE_ACTION = "/fuzzsingle";
    private static final String FUZZ_ALL_ACTION    = "/fuzzall";

    /* Wear Client */
    WearClient client;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart WearableListenerService");

        /* Get WearClient instance */
        client = WearClient.getInstance(this.getApplicationContext());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate WearableListenerService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy WearableListenerService");

        client.destroy();
    }

    /* ---------------------------------------------------------------------------
     * Communication Methods
     * ---------------------------------------------------------------------------
     */

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
        Log.d(TAG, "onDataChanged");
    }

    @Override
    public void onMessageReceived(MessageEvent msg) {
        super.onMessageReceived(msg);
        Log.d(TAG, String.format("onMsgReceived | msg {%s}", msg));

        switch (IFuzzAction.fromPath(msg.getPath())) {

            /* Start Service */
            case START_ACTION:
                startService(new Intent(this, IntentFuzzer.class));
                break;

            /* Load Component Names */
            case LOAD_ACTION:
            {
                String type = new String(msg.getData());
                Log.d(TAG, String.format("onMsgReceived | LOAD_ACTION type {%s} {%s}",
                        type, IPCType.fromName(type)));
                client.getComponentNames(IPCType.fromName(type));
                break;
            }

             /* Fuzzing Individually */
            case FUZZ_SINGLE_ACTION:
            {
                byte[] rawData = msg.getData();
                DataMap dataMap = DataMap.fromByteArray(rawData);

                String type = dataMap.getString("type");
                String fuzzType = dataMap.getString("fuzz");
                String clazz = dataMap.getString("classname");
                Log.d(TAG, String.format("onMsgReceived | FUZZ_SINGLE_ACTION {%s} {%s} {%s}",
                        type, fuzzType, clazz));
                client.fuzzSingle(IPCType.fromName(type), FuzzType.fromName(fuzzType), clazz);
                break;
            }

            /* Fuzzing All */
            case FUZZ_ALL_ACTION:
            {
                byte[] rawData = msg.getData();
                DataMap dataMap = DataMap.fromByteArray(rawData);

                String type = dataMap.getString("type");
                String fuzzType = dataMap.getString("fuzz");
                Log.d(TAG, String.format("onMsgReceived | FUZZ_ALL_ACTION {%s} {%s}",
                        type, fuzzType));
                client.fuzzAll(IPCType.fromName(type), FuzzType.fromName(fuzzType));
                break;
            }

            /* Run All Experiments */
            case RUN_ALL_EXPERIMENTS:
            {
                byte[] rawData = msg.getData();
                DataMap dataMap = DataMap.fromByteArray(rawData);

                // type of component
                String type = dataMap.getString("type");
                // limits
                ArrayList<Integer> list = dataMap.getIntegerArrayList("limits");
                Integer[] limits = (list == null) ? null : list.toArray(new Integer[list.size()]);

                Log.d(TAG, String.format("onMsgReceived | RUN_ALL_ACTION {%s}",
                        type));
                client.runAll(IPCType.fromName(type), limits);
                break;
            }
        }

    }
}
