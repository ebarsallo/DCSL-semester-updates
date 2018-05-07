package edu.purdue.android.fuzzer.squibble;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import edu.purdue.android.fuzzer.squibble.common.ExperimentConstants;
import edu.purdue.android.fuzzer.squibble.common.FuzzType;
import edu.purdue.android.fuzzer.squibble.common.IntentFuzzer;
import edu.purdue.android.fuzzer.squibble.common.IPCType;

import static android.R.id.list;


/**
 * @author Amiya Maji
 * @author ebarsallo
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FUZZ/Mobile";

    // Mode (0: Android Mobile, 1: Android Wear)
    private int mode=1;

    // UI
    private Spinner  mSpinTargetApp = null;
    private Spinner  mSpinIntent = null;
    private Spinner  mSpinType = null;
    private Spinner  mSpinIntentType = null;

    private CheckBox mChkInput = null;

    private EditText mEdtInput = null;
    private EditText mComp  = null;

    private Button   mBtnAdd = null;
    private Button   mBtnFuzzSingle = null;
    private Button   mBtnFuzzAll = null;
    private Button   mBtnRunAll = null;
    private Button   mBtnClear = null;

    private TextView mTxtOut = null;
    private TextView mComponentsLabel = null;

    // the Intent Fuzzer
    private IntentFuzzer fuzzer;
    // the list of Known Component Names loaded
    private ArrayList<String> mKnownComponentNames = null;
    // the list of Types supported
    private ArrayList<String> mTypesSupported = new ArrayList<String>();

    // the currently selected IPC type, specified as a string.
    private String mCurrentType = null;
    // the currently selected Fuzz Intent type selected.
    private FuzzType mCurrentFuzzType = null;
    // the currently target app selected
    private String mCurrentTargetApp = null;

    // Remote Manager
    IntentFuzzerManager mIFuzzManager ;

    /* ---------------------------------------------------------------------------
     * Nested Classes
     * ---------------------------------------------------------------------------
     */

    /**
     * Nested class (extended from BroadcastReceiver) used to receive message from
     * the wearable device, and show the data on the UI.
     */
    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String> knownComponentNames = intent.getStringArrayListExtra("message");
            int c = intent.getIntExtra("collision", 0);

            mKnownComponentNames = knownComponentNames;
            populateActions(mKnownComponentNames, c);

        }
    }

    /* ---------------------------------------------------------------------------
     * Override AppCompatActivity methods
     * ---------------------------------------------------------------------------
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the local broadcast receiver
        // @see <a href="http://android-wear-docs.readthedocs.io/en/latest/sync.html">
        // Data Layer Messages</a>
        IntentFilter msgFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver msgReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(msgReceiver, msgFilter);

        if (mode == 0)
        // Android Mobile
        {

            // Construct an instance fuzzer service
            fuzzer = IntentFuzzer.getInstance(this);

        } else
        // Android Wear
        {

            // FIXME: Class fuzzer should not be called in this context
            // Construct an instance fuzzer service
            fuzzer = IntentFuzzer.getInstance(this);

            // Get an instance of the FuzzManager
            mIFuzzManager = IntentFuzzerManager.getInstance(this);

        }

        // Initialize Activity
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIFuzzManager.destroy();
    }

    /**
     * Init App
     */
    private void init() {

        // Load Device's Component Names
        if (mode == 0)
        // Android Mobile
        {
            // Retrieve Component Names from target device
            mKnownComponentNames = fuzzer.getComponentNames(IPCType.BROADCASTS);
            // Initialize UI controls
            defineControls();
            addListeners();
            populateTypes();
            populateActions(mKnownComponentNames, 0);
        } else
        // Android Wear
        {
            // Initialize UI controls
            defineControls();
            addListeners();
            populateTypes();

            // Populate mSpinIntent spinner (Action/Component) with the corresponding Component
            // Names associate to the current type.
            updateType();

            // Resuts would be updated when received from the wearable
        }

    }

    /* ---------------------------------------------------------------------------
     * UI control methods
     * ---------------------------------------------------------------------------
     */

    /**
     * Assign each UI control with their corresponding instance variable.
     */
    private void defineControls() {
        Log.d(TAG, "defineControls");

        mComponentsLabel = (TextView) this.findViewById(R.id.lbl_Actions);

        // Selection spinner
        mSpinTargetApp  = (Spinner) this.findViewById(R.id.spn_Target);
        mSpinIntent     = (Spinner) this.findViewById(R.id.spn_Intent);
        mSpinType       = (Spinner) this.findViewById(R.id.spn_Type);
        mSpinIntentType = (Spinner) this.findViewById(R.id.spn_FuzzType);

        // Action buttons
        mBtnAdd        = (Button) this.findViewById(R.id.btn_AddIntent);
        mBtnFuzzSingle = (Button) this.findViewById(R.id.btn_FuzzSingle);
        mBtnFuzzAll    = (Button) this.findViewById(R.id.btn_FuzzAll);
        mBtnRunAll     = (Button) this.findViewById(R.id.btn_RunAll);
        mBtnClear      = (Button) this.findViewById(R.id.btn_Clear);

        // Input/Output
        mTxtOut   = (TextView) this.findViewById(R.id.output);
        mChkInput = (CheckBox) this.findViewById(R.id.chk_InputRange);
        mEdtInput = (EditText) this.findViewById(R.id.edt_InputRange);
        mComp     = (EditText) this.findViewById(R.id.comp);
    }

    /**
     * Populate static data on controls.
     * <ul>
     *     <item>
     * <code>mIntentType</code> spinner is populated with the defined fuzz intent supported on the
     * IntentFuzzer class.
     *     </item>
     *     <item>
     * <code>mSpinType</code> spinner is populated with the enumeration <code>IPCType</code>, which
     * contains the supported types of Components IPC supported by the fuzzer.
     *     </item>
     * </ul>
     *
     */
    private void populateTypes() {
        Log.d(TAG, "populateTypes");

        // spinner: Target apps
        populateTargetApp();

        // Load Fuzz types supported (Jar Jar Binks)
        ArrayList<String> fuzzingIntents = fuzzer.getFuzzTypes();
        ArrayAdapter<String> intentAA = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, fuzzingIntents);
        mSpinIntentType.setAdapter(intentAA);
        mCurrentFuzzType = FuzzType.NULL;

        // Load Types supported from IPCType enum
        for (IPCType type : IPCType.values())
            mTypesSupported.add(IPCType.toName(type));
        mCurrentType = mTypesSupported.get(0);

        // spinner: Types Supported
        ArrayAdapter<String> typeAA = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, mTypesSupported);
        mSpinType.setAdapter(typeAA);
    }

    private void populateTargetApp() {
        ArrayList<String> list = new ArrayList<>();

        ArrayAdapter<String> targetAA = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, list);
        mSpinTargetApp.setAdapter(targetAA);
    }

    /**
     * Add listeners to spinners and command buttons.
     */
    private void addListeners() {
        Log.d(TAG, "addListeners");

        // checkbox: Input Range
        mChkInput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "mChkInput.setOnClickListener");
                mEdtInput.setEnabled(isChecked);
            }
        });

        // spinner: Target App
        mSpinTargetApp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int i, long l) {
                Object selected = mSpinIntentType.getSelectedItem();

                mCurrentTargetApp = (selected != null)
                        ? selected.toString()
                        : null;
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // spinner: Supported Types
        mSpinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> a, View v, int i, long l) {
                Log.d(TAG, "mSpinType.setOnItemSelectedListener");
                updateType();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // spinner: Fuzz Intent Types
        mSpinIntentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "mSpinIntentType.setOnItemSelectedListener");
                Object selected = mSpinIntentType.getSelectedItem();

                // Set the Fuzz type to NULL if nothing has been selected. This is the default
                // behavior of Intent Fuzzer tool
                mCurrentFuzzType = (selected != null)
                        ? FuzzType.fromName(selected.toString())
                        : fuzzer.getDefaultType();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // spinner: Types Supported
        ArrayAdapter<String> typeAA = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, mTypesSupported);
        mSpinType.setAdapter(typeAA);

        // button: Add
        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                runClicked();
            }
        });

        // button: Fuzz Single
        mBtnFuzzSingle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fuzzSingle(IPCType.fromName(mCurrentType));
            }
        });

        // button: Fuzz All
        mBtnFuzzAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fuzzAll(IPCType.fromName(mCurrentType));
            }
        });

        // button: Run All Experiments
        mBtnRunAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fuzzExpt(IPCType.fromName(mCurrentType));
            }
        });

        // button: Clear
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Clear text from output text area
                mTxtOut.setText("");
            }
        });
    }

    /**
     * Populate data on the spinners (Type, Action). Refresh the UI with the Component Names
     * associated to the current Type (just selected)
     *
     * @param componentNames the Component Names associated to the Type (current type)
     */
    private void populateActions(ArrayList<String> componentNames, int collisions) {
        Log.d(TAG, "populateActions");

        // spinner: Actions/Components
        ArrayAdapter<String> actionAA = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, componentNames);
        mSpinIntent.setAdapter(actionAA);

        // Update label with the number of Component Names
        if (componentNames != null) {
            mComponentsLabel.setText("Components ("
                    + Integer.toString(componentNames.size()) + "):");
        }

        // Show collisions on UI
        if (collisions > 0) {
            Toast.makeText(this, collisions + " component name collision(s)",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Updates the Actions/Component Names (<code>spin_Intent</code> spinner) given a component
     * type (e.g. Activities, Services, Content Providers, Broadcast Receivers).
     */
    protected void updateType() {
        Log.d(TAG, "updateType");

        Object sel = mSpinType.getSelectedItem();
        if (sel != null) {
            mCurrentType = mSpinType.getSelectedItem().toString();
            updateComponents();
        }
    }

    /**
     * Updates <code>Component Names</code> spinner.
     */
    protected void updateComponents() {
        Log.d(TAG, "updateComponents");

        int c = 0;

        // Get current Component Type
        IPCType cur = IPCType.fromName(mCurrentType);


        if (mode == 0)
        // Android Mobile
        {
            mKnownComponentNames = fuzzer.getComponentNames(cur);
            c = fuzzer.getCollisions();

            if (c != 0) {
                Toast.makeText(this, c + " component name collision(s)",
                        Toast.LENGTH_SHORT).show();
            }

            if (mKnownComponentNames.size() > 0) {
                // Update Action (components) spinner
                ArrayAdapter<String> actionAA = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item, mKnownComponentNames);
                mSpinIntent.setAdapter(actionAA);
            }

        } else
        // Android Wearable
        {
            // Retrieve Component Names from target device
            mIFuzzManager.load(cur);
        }

        // do something to build the list of actions
    }

    protected void runClicked() {
        mTxtOut.append("Button Click Not Implemented\n");
        // mKnownComponents.add(mInput.getText().toString());
    }


//    if(!mInput.getText().toString().equals(""))
//    {
//        try{
//            String tt[] = mInput.getText().toString().split(",");
//            if(tt == null || tt.length < 2)
//            {
//                mOut.setText("You must type <begin,end> component indices before clicking RUN!");
//                return;
//            }
//            beg = Integer.parseInt(tt[0]);
//            end = Integer.parseInt(tt[1]);
//            System.err.println("Begin: "+beg+" End: "+end);
//        }
//        catch(NumberFormatException ne)
//        {
//            beg = 0; end = 0;
//            //while(!(mIntentSpin.getAdapter().getItem(beg).equals(mInput.getText().toString())) && beg < mIntentSpin.getAdapter().getCount())
//            //	beg++;
//        }
//    }


    /* ---------------------------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------------------------
     */

    /**
     * Return the limits (begin, end) for iterate the components chosen. If there's no input or if
     * the input is invalid, default limits are used instead (iterate all components).
     *
     * @return the limits set on the UI, otherwise null.
     */
    ArrayList<Integer> getLimits() {
        ArrayList<Integer> limits = new ArrayList<>();

        // If checked use the range in the input, otherwise iterate thru all components
        if (mChkInput.isChecked()) {
            String tt[] = mEdtInput.getText().toString().split(",");

            // Sanity check
            if (tt == null || tt.length < 2) {
                mTxtOut.setText("You must type <begin, end> component indices before clicking RUN!");
                return null;
            }

            try {
                limits.add(0, Integer.parseInt(tt[0]));
                limits.add(1, Integer.parseInt(tt[1]));

                Log.d(TAG, String.format("getLimits |  begin {%s} end {%s}", tt[0], tt[1]));
                return limits;
            } catch (NumberFormatException ex) {
                mTxtOut.setText("Invalid format for range. \n" +
                        "You must type <begin, end> component indices before clicking RUN!");
            }

        }

        return null;
    }


    /* ---------------------------------------------------------------------------
     * Actions
     * ---------------------------------------------------------------------------
     */

    public void fuzzAllComponents() {

    }

    /**
     * Handles the clicking of the "Run all" (Experiments) GUI button, passed the type of IPC
     * to fuzz. The type must be matched to the currently populated known components as this code
     * is tighly coupled to the UIs implementation.
     *
     * @param type the type of component, to IPC.
     * @return the String that gives a summary of what was done.
     */
    public void fuzzExpt(IPCType type) {

        mTxtOut.setText(String.format("Now running all Expt. for %s", mCurrentType));

        if (mode == 0)
        // Android Mobile
        {
            try {
                mTxtOut.append(fuzzer.runAllExpts(type));
            } catch (Exception ex) {
                mTxtOut.append(String.format("%s \n %s", ex.getMessage(), ex.getCause()));
            }

        } else
        // Android Wear
        {
            mIFuzzManager.runAllExpts(type, getLimits());
        }
    }

    /**
     * Handles the clicking of the "Fuzz all" GUI button, passed the type of IPC to fuzz.
     * The type must be matched to the currently populated known components as this code
     * is tighly coupled to the UIs implementation.
     *
     * @param type the type of component, to IPC.
     * @return the String that gives a summary of what was done.
     */
    public void fuzzAll(IPCType type) {

        mTxtOut.setText(String.format("Now fuzzing all {%s} for %s",
                IPCType.toName(type),
                mCurrentFuzzType));

        if (mode == 0)
        // Android Mobile
        {
            try {
                mTxtOut.append(fuzzer.fuzzAll(type, mCurrentFuzzType));
            } catch (Exception ex) {
                mTxtOut.append(String.format("%s \n %s", ex.getMessage(), ex.getCause()));
            }

        } else
        // Android Wear
        {
            mIFuzzManager.fuzzAll(type, mCurrentFuzzType);
        }
    }

    /**
     * Handles the clicking of the "Fuzz Single" GUI button, passed the type of IPC to fuzz.
     * The type must be matched to the currently populated known components as this code
     * is tighly coupled to the UIs implementation. Just one a component is fuzzed.
     *
     * @param type the type of component, to IPC.
     * @return the String that gives a summary of what was done.
     */
    public void fuzzSingle(IPCType type) {
        String className = mSpinIntent.getSelectedItem().toString();

        mTxtOut.setText(String.format("Now fuzzing {%s} %s for {%s}",
                IPCType.toName(type),
                className,
                mCurrentFuzzType
                ));


        // The following types are not based or use Intents for communication
        switch (type) {
            case PROVIDERS:
            {
                Toast.makeText(this,
                        "Providers don't use Intents, ignore this setting.",
                        Toast.LENGTH_SHORT).show();
                mTxtOut.append("Not Implemented.");
                return;
            }

            case INSTRUMENTATIONS:
            {
                Toast.makeText(this,
                        "Instrumentations aren't Intent based... starting Instrumentation.",
                        Toast.LENGTH_SHORT).show();
                break;
            }
        }

        if (mode == 0)
        // Android Mobile
        {
            fuzzer.my = this;
            mTxtOut.append(fuzzer.runSingle(type, mCurrentFuzzType, className));

        } else
        // Android Wear
        {
            mIFuzzManager.runSingle(type, mCurrentFuzzType, className);
        }
    }



    /* ---------------------------------------------------------------------------
     * Experimental
     * ---------------------------------------------------------------------------
     */

    /**
     * Initiate an IPC of a given type from the Activity by sending an Intent.
     * Invoked internally by Null Fuzz Single.
     *
     * @param intent the Intent that is going to be sent.
     * @param type the type of IPC.
     * @param index the index of a ComponentName in the mKnownComponents array.
     * @return {@link String} that gives a summary the action.
     */
    private String sendIntent(Intent intent, IPCType type, int index) throws Exception {

        Log.d(TAG, "SendIntentbyType | " + intent);

        try {

            // Initiate IPC according component type
            switch (type) {
                case ACTIVITIES:

//                    startActivity(intent);
//                    Thread.sleep(100);

                    startActivityForResult(intent, 1999+index);
                    Thread.sleep(100);
                    finishActivity(1999+index);


                    Log.d(TAG, "SendIntentbyType | Started Activity: {" + intent.getComponent() + "}");
                    return "Started: " + intent.getPackage();

                case BROADCASTS:
                    sendBroadcast(intent);
                    Thread.sleep(100);

                    Log.d(TAG, "SendIntentbyType | Sent broadcast: {" + intent.getPackage() + "}");
                    return "Sent broadcast: " + intent.getPackage();

                case SERVICES:
                    startService(intent);
                    Thread.sleep(100);
                    // Stopping service
                    stopService(intent);

                    Log.d(TAG, "SendIntentbyType | Started service: {" + intent.getPackage() + "}");
                    return "Started: " + intent.getPackage();

                case PROVIDERS:
                    // uh - providers don't use Intents...what am I doing...
                    Log.d(TAG, "Not implemeted");
                    return "Not Implemented";

                case INSTRUMENTATIONS:
                    startInstrumentation(intent.getComponent(), null, null);
                    // not intent based you could fuzz these params, if anyone cared.
                    Log.d(TAG, "Not implemeted");
                    return "Not Implemented";
            }
        }
        catch (Exception ex) {
            throw ex;
        }

        return "";
    }



}
