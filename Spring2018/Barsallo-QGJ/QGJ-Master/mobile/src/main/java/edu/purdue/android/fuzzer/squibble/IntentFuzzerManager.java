package edu.purdue.android.fuzzer.squibble;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.purdue.android.fuzzer.squibble.common.FuzzType;
import edu.purdue.android.fuzzer.squibble.common.IFuzzAction;
import edu.purdue.android.fuzzer.squibble.common.IPCType;

import static edu.purdue.android.fuzzer.squibble.common.IFuzzAction.*;

/**
 * Remote Intent Fuzzer Manager
 *
 * @author ebarsallo
 */

public class IntentFuzzerManager {

    private static final String TAG = "FUZZ/IFuzzMngr";

    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;

    private static IntentFuzzerManager instance;

    private static Context context;
    private GoogleApiClient mApiClient;
    private ExecutorService mExecService;

    /**
     * Construct an instance of {@link IntentFuzzerManager} given an specified Application
     * Context.
     *
     * @param context the Application Context
     */
    private IntentFuzzerManager(Context context) {
        Log.d(TAG, "Constructor");
        this.context = context;
        this.mApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        this.mApiClient.connect();
        this.mExecService = Executors.newCachedThreadPool();
    }

    /**
     * Get an instance of {@link IntentFuzzerManager} given an specified Application Context
     * using a singleton pattern. The method is thread safe.
     *
     * @param context the Application Context
     * @return the instance of {@link IntentFuzzerManager}
     */
    public static synchronized IntentFuzzerManager getInstance(Context context) {
        if (instance == null) {
            instance = new IntentFuzzerManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Validates the connection with the paired device (phone).
     *
     * @return True if the connection has been established, false otherwise
     */
    private boolean validateConnection () {
        Log.d(TAG, "validateConnection");
        if (mApiClient.isConnected()) return true;

        /* Try to connect */
        ConnectionResult res = mApiClient.blockingConnect(
                CLIENT_CONNECTION_TIMEOUT,
                TimeUnit.MILLISECONDS);
        return res.isSuccess();
    }

    /* ---------------------------------------------------------------------------
     * Actions
     * ---------------------------------------------------------------------------
     */

    /**
     * Load the Component Names associated to the Type (IPC) specified from the target
     * device: wearable device. Thus, a message must be sent using the Android Wear
     * Message API.
     *
     * @param type the Type of Component (IPC) to retrieve from the target device.
     */
    public void load(final IPCType type) {
        Log.d(TAG, String.format("load {%s}", IPCType.toName(type)));
        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                doAction(IFuzzAction.toPath(LOAD_ACTION), IPCType.toName(type).toString().getBytes());
            }
        });
    }

    /**
     * Apply an Intent Fuzz Test to a Component Name, identified by a <code>classname</code>. The
     * Component Name and the Type (IPC) must match.
     *
     * @param type the Type of Component (IPC) to be fuzz tested.
     * @param clazz the classname to be fuzz tested.
     */
    public void runSingle(final IPCType type, FuzzType fuzzType, final String clazz) {
        Log.d(TAG, String.format("fuzzOne {%s} {%s}", IPCType.toName(type), clazz));

        final DataMap dataMap = new DataMap();
        dataMap.putString("type", IPCType.toName(type));
        dataMap.putString("fuzz", FuzzType.toName(fuzzType));
        dataMap.putString("classname", clazz);

        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                doAction(IFuzzAction.toPath(FUZZ_SINGLE_ACTION), dataMap.toByteArray());
            }
        });
    }

    /**
     * Apply an Intent Fuzz Test to all the known components from an specific Type (IPC). The type
     * must be matched to the currently populated known components by the Fuzz Manager.
     *
     * @param type the Type of the Components (IPC) to be fuzz tested.
     */
    public void fuzzAll(final IPCType type, FuzzType fuzzType) {
        Log.d(TAG, String.format("fuzzAll {%s}", IPCType.toName(type)));

        final DataMap dataMap = new DataMap();
        dataMap.putString("type", IPCType.toName(type));
        dataMap.putString("fuzz", FuzzType.toName(fuzzType));

        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                doAction(IFuzzAction.toPath(FUZZ_ALL_ACTION), dataMap.toByteArray());
            }
        });
    }

    public void runAllExpts(final IPCType type, ArrayList<Integer> limits) {
        Log.d(TAG, String.format("RunAllExpts {%s}", IPCType.toName(type)));

        final DataMap dataMap = new DataMap();
        dataMap.putString("type", IPCType.toName(type));
        dataMap.putIntegerArrayList("limits", limits);

        mExecService.submit(new Runnable() {
            @Override
            public void run() {
                doAction(IFuzzAction.toPath(RUN_ALL_EXPERIMENTS), dataMap.toByteArray());
            }
        });
    }



    public void destroy() {
        mApiClient.disconnect();
        mExecService.shutdown();
    }

    /* ---------------------------------------------------------------------------
     * Communication Methods
     * ---------------------------------------------------------------------------
     */

    /**
     * Execute action by sending a message to the paired devices (nodes). The communication between
     * the Android Phone and the wearables is done via MessageApi.
     *
     * @param action the action requested
     * @param data the data associated to the action
     */
    private void doAction (final String action, final byte[] data) {
        if (validateConnection()) {
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await().getNodes();
            Log.d(TAG, String.format("doAction | Nodes {%d}.", nodes.size()));

            /* Iterate thru all nodes */
            for (Node node : nodes) {

                /* Send an RPC msg to the wearable nodes */
                Wearable.MessageApi.sendMessage(mApiClient, node.getId(), action, data)
                        .setResultCallback(
                                new ResultCallback<MessageApi.SendMessageResult>() {

                                    @Override
                                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                        if (!sendMessageResult.getStatus().isSuccess()) {
                                            Log.d(TAG, String.format(
                                                    "doAction | Failed to send msg {%s} Error {%s}.",
                                                    action,
                                                    sendMessageResult.getStatus().getStatusMessage()));
                                        } else {
                                            Log.d(TAG, String.format("doAction | Message sent {%s} {%s}.",
                                                    action, new String (data)));
                                        }
                                    }
                                }
                        );
            }
        }
    }

}
