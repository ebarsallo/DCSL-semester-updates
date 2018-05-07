package edu.purdue.android.fuzzer.squibble;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.WearableListView;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.purdue.android.fuzzer.squibble.common.FuzzType;
import edu.purdue.android.fuzzer.squibble.common.IPCType;
import edu.purdue.android.fuzzer.squibble.common.IntentFuzzer;

/**
 * Android Wear Client
 *
 * @author ebarsallo
 */

public class WearClient implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "FUZZ/WearClient";
    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;

    private static WearClient instance;

    private Context context;
    private GoogleApiClient mApiClient;
    private ExecutorService mExecService;

    // the Intent Fuzzer
    private IntentFuzzer fuzzer;
    // the list of Known Component Names loaded
    private ArrayList<String> mKnownComponentNames = null;


    /**
     * Construct an instance of {@link WearClient} with the specified Application Context. Data
     * connection is created via Google Wearable API, and an execution service is instanced using
     * a cached thread pool (to send message to the Android device).
     *
     * @param context the Application Context.
     */
    private WearClient(Context context) {
        Log.d(TAG, "Starting WearClient");

        this.context = context;
        this.fuzzer = IntentFuzzer.getInstance();
//        this.fuzzer = IntentFuzzer.getInstance(context.getApplicationContext());
//        this.fuzzer  = new IntentFuzzer(context.getPackageManager());

        // Init Google API communication components
        mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
        mExecService = Executors.newCachedThreadPool();
    }

    /**
     * Get an instance of {@link WearClient} using a singleton pattern, with the specified
     * Application Context.
     *
     * @param context  the Application Context
     * @return  the instance of WearClient
     */
    public static WearClient getInstance(Context context) {
        if (instance == null) {
            instance = new WearClient(context);
        }
        return instance;
    }

    /**
     * Validates the connection with the paired device (phone).
     *
     * @return True if the connection has been established, false otherwise
     */
    private boolean validateConnection () {
        if (mApiClient.isConnected()) return true;

        // Try to connect
        ConnectionResult res = mApiClient.blockingConnect(
                CLIENT_CONNECTION_TIMEOUT,
                TimeUnit.MILLISECONDS);

        if (!res.isSuccess()) {
            Log.e(TAG, String.format("validateConnection | Connection Failed {%d} {%s}",
                    res.getErrorCode(),
                    res.getErrorMessage()));
        }

        return res.isSuccess();
    }

    /* ---------------------------------------------------------------------------
     * Actions
     * ---------------------------------------------------------------------------
     */

    /**
     * Get Component Names of the specified Type from the wearable device. The Components are
     * retrieved thru the {@link IntentFuzzer} common instance.
     *
     * @param type the Type of Component
     */
    void getComponentNames(IPCType type) {
        Log.d(TAG, String.format("get | Retrieving Component Names"));
        Log.d(TAG, String.format("get | type {%s}", IPCType.toName(type)));

        mKnownComponentNames = this.fuzzer.getComponentNames(type);

        Log.d(TAG, String.format("get | mKnownComponents size {%d} collisions {%d}",
                mKnownComponentNames.size(),
                this.fuzzer.getCollisions()));

        sendComponentNames(mKnownComponentNames, this.fuzzer.getCollisions(), type);
    }

    /**
     * Submit a background task to the {@link ExecutorService} instance to send the retrieved
     * Component Names to the paired device (handheld).
     *
     * @param knownComponentNames the arraylist with the Component Names retrieved
     * @param collision the number of collision found in the Component Names retrieved (from the
     *                  IPC type specified).
     * @param type the Type of Component (for IPC)
     */
    private void sendComponentNames(final ArrayList<String> knownComponentNames, final int collision, final IPCType type) {
        Log.d(TAG, "sendCN | Sending Component Names to Phone");

        // Send data
        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                sendComponentNamesData(knownComponentNames, collision, type);
            }
        });
    }

    /**
     * Create a {@link PutDataMapRequest} instance to store all the retrieved data from the target
     * device (wearable), to be send to the paired device (handheld) using the Android Wearable
     * DataApi.
     *
     * @param knownComponentNames the arraylist with the Component Names retrieved
     * @param collision the number of collision found in the Component Names retrieved (from the
     *                  IPC type specified).
     * @param type the Type of Component (for IPC)
     */
    private void sendComponentNamesData(final ArrayList<String> knownComponentNames, final int collision, final IPCType type) {
        Log.d(TAG, String.format("sendCNData | collision {%d}", collision));
        Log.d(TAG, String.format("sendCNData | size {%d}", knownComponentNames.size()));

        // Create data item
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/load/" + IPCType.toName(type));

        putDataMapRequest.getDataMap().putStringArrayList("components", knownComponentNames);
        putDataMapRequest.getDataMap().putInt("collision", collision);
        putDataMapRequest.getDataMap().putLong("time", System.currentTimeMillis());

        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        send(request);
    }

    /**
     * Execute an intent fuzz test with a specified Component Name (<code>classname</code>) from
     * an specific Type supported.
     *
     * @param type the Type of Component (for IPC)
     * @param classname
     */
    void fuzzSingle(IPCType type, FuzzType fuzzType, String classname) {
        Log.d(TAG, "fuzzSingle");
        String out = this.fuzzer.runSingle(type, fuzzType, classname);
        Log.d(TAG, String.format("fuzzSingle | out {%s}", out));
    }

    /**
     * Execute an intent fuzz test with all the Component Names from the IPC type specified. The
     * Component Names type last retrieved by the {@link IntentFuzzerService} must match with the
     * IPC type sent as argument.
     *
     * @param type the Type of Component (for IPC)
     */
    void fuzzAll(IPCType type, FuzzType fuzzType) {
        Log.d(TAG, "fuzzAll");
        String out="";
        try {
            out = this.fuzzer.fuzzAll(type, fuzzType);
        } catch (Exception ex) {
            out = ex.getMessage();
        }
        Log.e(TAG, String.format("fuzzAll | out {%s}", out));
    }

    void runAll(IPCType type, Integer limits[]) {
        Log.d(TAG, "runAll");
        String out="";
        try {
            if (limits == null)
                out = this.fuzzer.runAllExpts(type);
            else
                out = this.fuzzer.runAllExpts(type, limits[0], limits[1]);
        } catch (Exception ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            out = ex.getMessage();
        }
        Log.e(TAG, String.format("runAll | out {%s}", out));
    }

    /**
     * Send data to the Android paired device (mobile) using the Android Wear DataApi. This method
     * is preferred to send light data to the handheld.
     *
     * @param data the data (<code>PutDataRequest</code>) to be sent to the Android paired device.
     */
    private void send(final PutDataRequest data) {
        Log.d(TAG, String.format("send | path {%s}", data.getUri().getPath()));

        if (validateConnection()) {
            // Put data item
            Wearable.DataApi.putDataItem(mApiClient, data).setResultCallback(
                    new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, String.format("send | Failed to send data {%s} {%s}",
                                        data.getUri().getPath(),
                                        dataItemResult.getStatus().getStatusMessage()));
                            } else {
                                Log.d(TAG, String.format("send | Succesfully sent data item {%s}",
                                        data.getUri().getPath()));
                            }
                        }
                    }
            );
        } else {
            Log.e(TAG, "send | No valid connection found");
        }
    }

    public void destroy() {
        Log.d(TAG, "Shutdown WearClient");

        mApiClient.disconnect();
        mExecService.shutdown();
    }

    /* ---------------------------------------------------------------------------
     * Override methods: GoogleApiClient
     * ---------------------------------------------------------------------------
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, String.format("onConnectionFailed | {%s}", connectionResult.getErrorMessage()));
    }
}
