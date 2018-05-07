package edu.purdue.android.fuzzer.squibble.common;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.nfc.TagLostException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * @author NCC Group
 * @author Amiya Maji
 * @author ebarsallo
 *
 * Intent Fuzzer
 * Common Fuzzer class based on the Intent Fuzzer developed by NCC Group and the modifications
 * introduced on:
 *
 * <p>
 * Maji, A. K., Arshad, F. A., Bagchi, S., & Rellermeyer, J. S. (2012, June).
 * <a href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.473.9121">An empirical study
 * of the robustness of inter-component communication in Android</a>. In Dependable systems and
 * networks (DSN), 2012 42nd annual IEEE/IFIP international conference on (pp. 1-12). IEEE.
 * </p>
 *
 * The tool has been adapted to be used with an Android device (Android OS) or either a Wearable
 * device (Android Wear). For more information about the Intent Fuzzer refer to
 * <a href="https://www.nccgroup.trust/us/about-us/resources/intent-fuzzer/">NCC Group Intent Fuzzer</a>.
 *
 * Original description from Intent Fuzzer:
 * Intent Fuzzer is a tool that can be used on any device using the Google Android operating system
 * (OS). Intent Fuzzer is exactly what is seems, which is a fuzzer. It often finds bugs that cause
 * the system to crash or performance issues on the device. The tool can either fuzz a single
 * component or all components. It works well on Broadcast receivers, and average on Services.
 *
 * For Activities, only single Activities can be fuzzed, not all them. Instrumentations can also be
 * started using this interface, and content providers are listed, but are not an Intent based
 * IPC mechanism.
 *
 * @see <a href="https://www.nccgroup.trust/us/about-us/resources/intent-fuzzer/">Intent Fuzzer</a>
 *
 *
 */
public class IntentFuzzer extends Activity {

    private static final String TAG = "FUZZ/IFuzz";

    private static final String DEFAULT_FUZZ_INTENT_TYPE = "Null-old";


    /* ---------------------------------------------------------------------------
     * Experiments Constants
     * ---------------------------------------------------------------------------
     */

    // # of intents needed to call Garbage Collector
    private static final int NUM_FREQ_GC = 100;
    // # of URI to generate (experiment 1)
    private static final int NUM_RANDOM = 3;
    // # of random content URI to be generated
    private static final int NUM_CONTENT = 5;
    // # of same exception accepted for each component
    private static final int NUM_RUN_SKIP_EXCEPTION = 10;
    // # of repeated exception accepted (default)
    private static final int NUM_RUN_SKIP_EXCEPTION_DEFAULT = 99999;
    // # of repeated exception accepted (per exception)
    private static final Map<String, Integer> NUM_RUN_SKIP_PER_EXCEPTION;
    static
    {
        NUM_RUN_SKIP_PER_EXCEPTION = new HashMap<>();
        NUM_RUN_SKIP_PER_EXCEPTION.put("SecurityException", 2);
    }


    // # of ms of delay every time the Garbage Collector is invoked
    private static final int NUM_DELAY_GC = 100;
    // # of ms of delay every time an Activity is started
    private static final int NUM_DELAY_START_ACT = 300;
    // # of ms of delay every time an Activity is started
    private static final int NUM_DELAY_START_SRV = 100;

    // Activity's Package Manager
    PackageManager mPackageManager;
    // Context
    Context context;

    // list of supported Types
    private ArrayList<String> mTypes = new ArrayList<String>();
    // list of ComponentsName
    private ArrayList<String> mComponentNames = new ArrayList<String>();
    // list of ComponentName for the current IPC type
    private ArrayList<ComponentName> mKnownComponents = new ArrayList<ComponentName>();
    // the list of Providers (essential for building content URI)
    private ArrayList<String> mKnownProviders = new ArrayList<String>();
    // list of type of fuzzing intents
    private ArrayList<String> mFuzzingIntents = new ArrayList<String>();

    // Number of collisions on Component Names retrieved
    private int collisions;

    // Instance to generate pseudorandom numbers
    private long seed = System.currentTimeMillis();
    private Random rnd = new Random(seed);
    //
    private String version = "20180130.1435";

    /*
	 * String constants from Android Intent documentation
	 */
    private static String LIST_ACTION [] = {
            "ACTION_AIRPLANE_MODE_CHANGED",
            "ACTION_ALL_APPS",
            "ACTION_ANSWER",
            "ACTION_APPLICATION_PREFERENCES",
            "ACTION_APPLICATION_RESTRICTIONS_CHANGED",
            "ACTION_APP_ERROR",
            "ACTION_ASSIST",
            "ACTION_ATTACH_DATA",
            "ACTION_BATTERY_CHANGED",
            "ACTION_BATTERY_LOW",
            "ACTION_BATTERY_OKAY",
            "ACTION_BOOT_COMPLETED",
            "ACTION_BUG_REPORT",
            "ACTION_CALL",
            "ACTION_CALL_BUTTON",
            "ACTION_CAMERA_BUTTON",
            "ACTION_CARRIER_SETUP",
            "ACTION_CHOOSER",
            "ACTION_CLOSE_SYSTEM_DIALOGS",
            "ACTION_CONFIGURATION_CHANGED",
            "ACTION_CREATE_DOCUMENT",
            "ACTION_CREATE_SHORTCUT",
            "ACTION_DATE_CHANGED",
            "ACTION_DEFAULT",
            "ACTION_DELETE",
            "ACTION_DEVICE_STORAGE_LOW",
            "ACTION_DEVICE_STORAGE_OK",
            "ACTION_DIAL",
            "ACTION_DOCK_EVENT",
            "ACTION_DREAMING_STARTED",
            "ACTION_DREAMING_STOPPED",
            "ACTION_EDIT",
            "ACTION_EXTERNAL_APPLICATIONS_AVAILABLE",
            "ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE",
            "ACTION_FACTORY_TEST",
            "ACTION_GET_CONTENT",
            "ACTION_GET_RESTRICTION_ENTRIES",
            "ACTION_GTALK_SERVICE_CONNECTED",
            "ACTION_GTALK_SERVICE_DISCONNECTED",
            "ACTION_HEADSET_PLUG",
            "ACTION_INPUT_METHOD_CHANGED",
            "ACTION_INSERT",
            "ACTION_INSERT_OR_EDIT",
            "ACTION_INSTALL_FAILURE",
            "ACTION_INSTALL_PACKAGE",
            "ACTION_LOCALE_CHANGED",
            "ACTION_LOCKED_BOOT_COMPLETED",
            "ACTION_MAIN",
            "ACTION_MANAGED_PROFILE_ADDED",
            "ACTION_MANAGED_PROFILE_AVAILABLE",
            "ACTION_MANAGED_PROFILE_REMOVED",
            "ACTION_MANAGED_PROFILE_UNAVAILABLE",
            "ACTION_MANAGED_PROFILE_UNLOCKED",
            "ACTION_MANAGE_NETWORK_USAGE",
            "ACTION_MANAGE_PACKAGE_STORAGE",
            "ACTION_MEDIA_BAD_REMOVAL",
            "ACTION_MEDIA_BUTTON",
            "ACTION_MEDIA_CHECKING",
            "ACTION_MEDIA_EJECT",
            "ACTION_MEDIA_MOUNTED",
            "ACTION_MEDIA_NOFS",
            "ACTION_MEDIA_REMOVED",
            "ACTION_MEDIA_SCANNER_FINISHED",
            "ACTION_MEDIA_SCANNER_SCAN_FILE",
            "ACTION_MEDIA_SCANNER_STARTED",
            "ACTION_MEDIA_SHARED",
            "ACTION_MEDIA_UNMOUNTABLE",
            "ACTION_MEDIA_UNMOUNTED",
            "ACTION_MY_PACKAGE_REPLACED",
            "ACTION_NEW_OUTGOING_CALL",
            "ACTION_OPEN_DOCUMENT",
            "ACTION_OPEN_DOCUMENT_TREE",
            "ACTION_PACKAGES_SUSPENDED",
            "ACTION_PACKAGES_UNSUSPENDED",
            "ACTION_PACKAGE_ADDED",
            "ACTION_PACKAGE_CHANGED",
            "ACTION_PACKAGE_DATA_CLEARED",
            "ACTION_PACKAGE_FIRST_LAUNCH",
            "ACTION_PACKAGE_FULLY_REMOVED",
            "ACTION_PACKAGE_INSTALL",
            "ACTION_PACKAGE_NEEDS_VERIFICATION",
            "ACTION_PACKAGE_REMOVED",
            "ACTION_PACKAGE_REPLACED",
            "ACTION_PACKAGE_RESTARTED",
            "ACTION_PACKAGE_VERIFIED",
            "ACTION_PASTE",
            "ACTION_PICK",
            "ACTION_PICK_ACTIVITY",
            "ACTION_POWER_CONNECTED",
            "ACTION_POWER_DISCONNECTED",
            "ACTION_POWER_USAGE_SUMMARY",
            "ACTION_PROCESS_TEXT",
            "ACTION_PROVIDER_CHANGED",
            "ACTION_QUICK_CLOCK",
            "ACTION_QUICK_VIEW",
            "ACTION_REBOOT",
            "ACTION_RUN",
            "ACTION_SCREEN_OFF",
            "ACTION_SCREEN_ON",
            "ACTION_SEARCH",
            "ACTION_SEARCH_LONG_PRESS",
            "ACTION_SEND",
            "ACTION_SENDTO",
            "ACTION_SEND_MULTIPLE",
            "ACTION_SET_WALLPAPER",
            "ACTION_SHOW_APP_INFO",
            "ACTION_SHUTDOWN",
            "ACTION_SYNC",
            "ACTION_SYSTEM_TUTORIAL",
            "ACTION_TIMEZONE_CHANGED",
            "ACTION_TIME_CHANGED",
            "ACTION_TIME_TICK",
            "ACTION_UID_REMOVED",
            "ACTION_UMS_CONNECTED",
            "ACTION_UMS_DISCONNECTED",
            "ACTION_UNINSTALL_PACKAGE",
            "ACTION_USER_BACKGROUND",
            "ACTION_USER_FOREGROUND",
            "ACTION_USER_INITIALIZE",
            "ACTION_USER_PRESENT",
            "ACTION_USER_UNLOCKED",
            "ACTION_VIEW",
            "ACTION_VOICE_COMMAND",
            "ACTION_WALLPAPER_CHANGED",
            "ACTION_WEB_SEARCH"
    };


    String iActions[] = {
            "ACTION_AIRPLANE_MODE_CHANGED",
            "ACTION_ALL_APPS",
            "ACTION_ANSWER",
            "ACTION_APP_ERROR",
            "ACTION_ATTACH_DATA",
            "ACTION_BATTERY_CHANGED",
            "ACTION_BATTERY_LOW",
            "ACTION_BATTERY_OKAY",
            "ACTION_BOOT_COMPLETED",
            "ACTION_BUG_REPORT",
            "ACTION_CALL",
            "ACTION_CALL_BUTTON",
            "ACTION_CAMERA_BUTTON",
            "ACTION_CHOOSER",
            "ACTION_CLOSE_SYSTEM_DIALOGS",
            "ACTION_CONFIGURATION_CHANGED",
            "ACTION_CREATE_SHORTCUT",
            "ACTION_DATE_CHANGED",
            "ACTION_DEFAULT",
            "ACTION_DELETE",
            "ACTION_DEVICE_STORAGE_LOW",
            "ACTION_DEVICE_STORAGE_OK",
            "ACTION_DIAL",
            "ACTION_DOCK_EVENT",
            "ACTION_EDIT",
            "ACTION_EXTERNAL_APPLICATIONS_AVAILABLE",
            "ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE",
            "ACTION_FACTORY_TEST",
            "ACTION_GET_CONTENT",
            "ACTION_GTALK_SERVICE_CONNECTED",
            "ACTION_GTALK_SERVICE_DISCONNECTED",
            "ACTION_HEADSET_PLUG",
            "ACTION_INPUT_METHOD_CHANGED",
            "ACTION_INSERT",
            "ACTION_INSERT_OR_EDIT",
            "ACTION_INSTALL_PACKAGE",
            "ACTION_LOCALE_CHANGED",
            "ACTION_MAIN",
            "ACTION_MANAGE_NETWORK_USAGE",
            "ACTION_MANAGE_PACKAGE_STORAGE",
            "ACTION_MEDIA_BAD_REMOVAL",
            "ACTION_MEDIA_BUTTON",
            "ACTION_MEDIA_CHECKING",
            "ACTION_MEDIA_EJECT",
            "ACTION_MEDIA_MOUNTED",
            "ACTION_MEDIA_NOFS",
            "ACTION_MEDIA_REMOVED",
            "ACTION_MEDIA_SCANNER_FINISHED",
            "ACTION_MEDIA_SCANNER_SCAN_FILE",
            "ACTION_MEDIA_SCANNER_STARTED",
            "ACTION_MEDIA_SHARED",
            "ACTION_MEDIA_UNMOUNTABLE",
            "ACTION_MEDIA_UNMOUNTED",
            "ACTION_MY_PACKAGE_REPLACED",
            "ACTION_NEW_OUTGOING_CALL",
            "ACTION_PACKAGE_ADDED",
            "ACTION_PACKAGE_CHANGED",
            "ACTION_PACKAGE_DATA_CLEARED",
            "ACTION_PACKAGE_FIRST_LAUNCH",
            "ACTION_PACKAGE_FULLY_REMOVED",
            "ACTION_PACKAGE_INSTALL",
            "ACTION_PACKAGE_NEEDS_VERIFICATION",
            "ACTION_PACKAGE_REMOVED",
            "ACTION_PACKAGE_REPLACED",
            "ACTION_PACKAGE_RESTARTED",
            "ACTION_PASTE",
            "ACTION_PICK",
            "ACTION_PICK_ACTIVITY",
            "ACTION_POWER_CONNECTED",
            "ACTION_POWER_DISCONNECTED",
            "ACTION_POWER_USAGE_SUMMARY",
            "ACTION_PROVIDER_CHANGED",
            "ACTION_REBOOT",
            "ACTION_RUN",
            "ACTION_SCREEN_OFF",
            "ACTION_SCREEN_ON",
            "ACTION_SEARCH",
            "ACTION_SEARCH_LONG_PRESS",
            "ACTION_SEND",
            "ACTION_SENDTO",
            "ACTION_SEND_MULTIPLE",
            "ACTION_SET_WALLPAPER",
            "ACTION_SHUTDOWN",
            "ACTION_SYNC",
            "ACTION_SYSTEM_TUTORIAL",
            "ACTION_TIMEZONE_CHANGED",
            "ACTION_TIME_CHANGED",
            "ACTION_TIME_TICK",
            "ACTION_UID_REMOVED",
            "ACTION_UMS_CONNECTED",
            "ACTION_UMS_DISCONNECTED",
            "ACTION_UNINSTALL_PACKAGE",
            "ACTION_USER_PRESENT",
            "ACTION_VIEW",
            "ACTION_VOICE_COMMAND",
            "ACTION_WALLPAPER_CHANGED",
            "ACTION_WEB_SEARCH"
    };


    String STANDARD_ACTIVITY_ACTIONS[] = {
            "ACTION_MAIN",
            "ACTION_VIEW",
            "ACTION_ATTACH_DATA",
            "ACTION_EDIT",
            "ACTION_PICK",
            "ACTION_CHOOSER",
            "ACTION_GET_CONTENT",
            "ACTION_DIAL",
            "ACTION_CALL",
            "ACTION_SEND",
            "ACTION_SENDTO",
            "ACTION_ANSWER",
            "ACTION_INSERT",
            "ACTION_DELETE",
            "ACTION_RUN",
            "ACTION_SYNC",
            "ACTION_PICK_ACTIVITY",
            "ACTION_SEARCH",
            "ACTION_WEB_SEARCH",
            "ACTION_FACTORY_TEST"
    };

    String STANDARD_BROADCAST_ACTIONS[] = {
            "ACTION_TIME_TICK",
            "ACTION_TIME_CHANGED",
            "ACTION_TIMEZONE_CHANGED",
            "ACTION_BOOT_COMPLETED",
            "ACTION_PACKAGE_ADDED",
            "ACTION_PACKAGE_CHANGED",
            "ACTION_PACKAGE_REMOVED",
            "ACTION_PACKAGE_RESTARTED",
            "ACTION_PACKAGE_DATA_CLEARED",
            "ACTION_PACKAGES_SUSPENDED",
            "ACTION_PACKAGES_UNSUSPENDED",
            "ACTION_UID_REMOVED",
            "ACTION_BATTERY_CHANGED",
            "ACTION_POWER_CONNECTED",
            "ACTION_POWER_DISCONNECTED",
            "ACTION_SHUTDOWN"
    };

    String iCategories[] = {
            "CATEGORY_DEFAULT",
            "CATEGORY_BROWSABLE",
            "CATEGORY_TAB",
            "CATEGORY_ALTERNATIVE",
            "CATEGORY_SELECTED_ALTERNATIVE",
            "CATEGORY_LAUNCHER",
            "CATEGORY_INFO",
            "CATEGORY_HOME",
            "CATEGORY_PREFERENCE",
            "CATEGORY_TEST",
            "CATEGORY_CAR_DOCK",
            "CATEGORY_DESK_DOCK",
            "CATEGORY_CAR_MODE"
    };

    String iExtras[] = {
            "EXTRA_ALARM_COUNT",
            "EXTRA_BCC",
            "EXTRA_CC",
            "EXTRA_CHANGED_COMPONENT_NAME",
            "EXTRA_DATA_REMOVED",
            "EXTRA_DOCK_STATE",
            "EXTRA_DOCK_STATE_CAR",
            "EXTRA_DOCK_STATE_DESK",
            "EXTRA_DOCK_STATE_UNDOCKED",
            "EXTRA_DONT_KILL_APP",
            "EXTRA_EMAIL",
            "EXTRA_INITIAL_INTENTS",
            "EXTRA_INTENT",
            "EXTRA_KEY_EVENT",
            "EXTRA_PHONE_NUMBER",
            "EXTRA_REMOTE_INTENT_TOKEN",
            "EXTRA_REPLACING",
            "EXTRA_SHORTCUT_ICON",
            "EXTRA_SHORTCUT_ICON_RESOURCE",
            "EXTRA_SHORTCUT_INTENT",
            "EXTRA_STREAM",
            "EXTRA_SHORTCUT_NAME",
            "EXTRA_SUBJECT",
            "EXTRA_TEMPLATE",
            "EXTRA_TEXT",
            "EXTRA_TITLE",
            "EXTRA_UID"
    };

    // http://web.archive.org/web/20111219175703/http://f-del.com/blog/list-of-android-mime-types-and-uris

    String iMimeTypes[] = {
            "application/vnd.android.package-archive",
            "media/*",
            "audio/*",
            "video/*",
            "application/ogg",
            "application/x-ogg",
            "application/atom+xml",
            "application/rss+xml",
            "vnd.android.cursor.item/*",
            "vnd.android.cursor.dir/*"

    };
    String iUriTypes[] = {
            "content://",
            "file://",
            "folder://",
            "directory://",
            "geo:",
            "google.streetview:",
            "http://",
            "https://",
            "mailto:",
            "ssh:",
            "tel:",
            "voicemail:"
    };

    // list of components that are avoided during bulk testing
    static String iBlackList[] = {
            "android.accounts.GrantCredentialsPermissionActivity",
            "com.android.settings.AccountSyncSettings",
            "com.android.settings.AccountSyncSettingsInAddAccount",
            "com.android.voicedialer.VoiceDialerActivity",
            "com.android.cardock.CarDockActivity",
            "com.android.carhome.CarHome",
            "com.android.providers.media.MediaScannerService",

            // squibble components
            "edu.purdue.android.fuzzer.squibble.common.IntentFuzzer",
            "edu.purdue.android.fuzzer.squibble.MainActivity",
            "edu.purdue.android.fuzzer.squibble.IntentFuzzerListenerService",
            "edu.purdue.android.fuzzer.squibble.IntentFuzzerService",
            "edu.purdue.android.fuzzer.squibble.WearClient",
    };

    // blacklist loaded into a HashMap for faster searching
    static Map<String, String> blkList = new HashMap<String, String>();
    static {
        for(String c : iBlackList)
        {
            blkList.put(c, c);
        }
    }

    uriGen ug = null;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    // this is a dummy object for getting acquainted with memory profiling
    Intent dummyIn = null;

    //------------------------
    private static IntentFuzzer instance;

    /**
     * Construct an instance of {@link IntentFuzzer} given an specified Application
     * Activity. This activity, either a mobile activity or a wearable activity, is the one that
     * will initiated the IPC communication.
     *
     * @param activity the Activity that will initiate the IPC fuzz tests.
     */
    private IntentFuzzer(Activity activity) {
        Log.d(TAG, "Constructor");

        this.my = activity;
        this.mPackageManager = activity.getPackageManager();

        // init class
        init(activity.getApplicationContext());

        Log.i(TAG, "Seed: {" + this.seed + "} Version: {" + this.version + "}");
    }


    /**
     * Get an instance of {@link IntentFuzzer} given an specified Application Activity
     * using a singleton pattern. The method is thread safe.
     *
     * @param activity the Activity that will initiate the IPC fuzz tests.
     * @return the instance of {@link IntentFuzzer}
     */
    public static synchronized IntentFuzzer getInstance(Activity activity) {
        if (instance == null) {
            instance = new IntentFuzzer(activity);
        }

        return instance;
    }

    public static synchronized IntentFuzzer getInstance() {
        if (instance == null) {
            Log.e(TAG, "IntentFuzzer must be initiated by the Main Activity");
        }
        return instance;
    }


    /**
     * Construct an instance of {@link IntentFuzzer} given a specified manager. The Intent fuzzer
     * primary focus or objectives are the apps installed on the Android OS. Therefore, the
     * Package Manager from the target device is needed.
     *
     * @param mngr the Package Manager
     */
    public IntentFuzzer(PackageManager mngr) {
        this.mPackageManager = mngr;
    }

    /* ---------------------------------------------------------------------------
     * Access method
     * ---------------------------------------------------------------------------
     */

    public int getCollisions() {
        return this.collisions;
    }

    /**
     * Return the types of Fuzzing Intent supported by the tool.
     *
     * @return the types of Fuzzing Intent supported
     */
    public ArrayList<String> getFuzzTypes() {
        return this.mFuzzingIntents;
    }

    /**
     * Return the default type of Fuzzing Intent supported by the tool.
     *
     * @return the FuzzType instance with the default type of Fuzzing Intent supported.
     */
    public FuzzType getDefaultType() {
        return FuzzType.fromName(DEFAULT_FUZZ_INTENT_TYPE);
    }

    /* ---------------------------------------------------------------------------
     * Helper methods
     * ---------------------------------------------------------------------------
     */

    static String ByteArrayToString(byte[] ba) {
        char hexmap[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder hex = new StringBuilder(ba.length * 3);
        for (byte b : ba)
        {
		  /*if(b == 0)
			  hex.append("00."); //"{0:x2}",
		  else
			  hex.append(String.format("%x.", b));*/
            byte t = b;
            hex.append(hexmap[(t>>>4)& 0xf]);
            hex.append(hexmap[ t & 0xf]);
            hex.append(".");
        }
        return hex.toString();
    }

    boolean isBlackListed(String name) {
        if (blkList.get(name) != null)
            return true;
        else
            return false;
    }

    String getTypeString(String t[]) {
        return t[rnd.nextInt(t.length)];
    }




    byte[] readFile(int i, int maxBufferSize) {
        String basedir = "t";
        String filename = basedir+i;
        System.err.println("******************Filename: "+filename+"\n");

        //File f = new File(filename);
        int offset = rnd.nextInt(getFileSize(i));
        byte b[] = new byte[rnd.nextInt(maxBufferSize)+1];//(int)sz];
        try
        {
            final InputStream is = getResources().getAssets().open(filename);


            //FileInputStream fi = new FileInputStream(f);
            is.skip(offset);
            is.read(b);
//            mOut.setText("Offset: "+offset+" buffer_size: "+b.length+"\n");
//            mOut.append(new String(b));
            is.close();
        }
        catch(Exception ex)
        {
            System.err.println("******************Error reading file.****************\n");
        }
        return b;
    }

    //due to problem with finding sizes of resources files in Android we are using
    //this static map from Fuzz input files
    int getFileSize(int i) {
        if(i%3 == 1)
            return 1000;
        if(i%3 == 2)
            return 10000;
        else
            return 100000;

    }

    int getComponentNameIndex(String clazz) throws Exception {
        String index;

        for (int i=0; i < mKnownComponents.size(); i++) {
            if (mKnownComponents.get(i).getClassName().equals(clazz)) {
                return i;
            }
        }

        new Exception("Cannot find classname: " + clazz);

        return -1;
    }


    /* ---------------------------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------------------------
     */

    /**
     * Generates pseudo random bytes of data of a given size.
     *
     * @param bufferSize the buffer size
     * @param sizeIsFixed indicates whether buffersize should be = or < the argument
     *                    specified
     * @return
     */
    byte[] getRandomData(int bufferSize, boolean sizeIsFixed) {
        if(!sizeIsFixed)
            bufferSize = rnd.nextInt(bufferSize)+1;
        byte b[] = new byte[bufferSize];
        for(int i=0; i<bufferSize; i++) {
            b[i] = (byte)rnd.nextInt(256);
        }
        return b;
    }

    /**
     * Generates a pseudo random URI of a given quality. If the quality is "dumb", the generated
     * URI does not take in consideration the different types of URI supported. Otherwise, with
     * quality "good", the generated URI use as schema any of the supported URI types.
     *
     * Note.
     * A URI consist of three parts:
     * <code>
     * schema/path?query
     * </code>
     *
     * @param type the type of the desired quality. Either "dumb" or "good".
     * @return the random URI.
     */
    Uri getRandomUri(String type) {
        String s = "";
        try{
            if(type.equals("dumb")) {
                s = new String(getRandomData(256, false));
                //	System.err.println("******************Uri: "+s+"\n");
                return Uri.parse(s);
            } else if(type.equals("good")) {
                s = getTypeString(iUriTypes);
                s.concat(new String(getRandomData(1024, false)));
                return Uri.parse(s);
            }

        }
        catch(Exception ex) {
            System.err.println("******************An Uri exception occurred.\n"+s+"\n****************\n");
        }
        return null;
    }

    /**
     * Retrieve the <code>Components Names</code> installed on the the target device given a
     * component type (e.g. Activities, Services, Content Providers, Broadcast Receivers).
     *
     * @param type the current Component Type.
     * @return
     */
    public ArrayList<String> getComponentNames(IPCType type) {
        Log.d(TAG, String.format("getComponentNames | type {%s}", type));

        int diff = 0;

        // Export Component Names from Package Manager target device
        mKnownComponents.clear();
        mKnownComponents.addAll(getExportedComponents(type));

        // Verify collisions.
        mComponentNames.clear();
        for (ComponentName n : mKnownComponents) {
            if (mComponentNames.contains(n.getClassName()))
                diff++;
            else
                mComponentNames.add(n.getClassName());
        }

        // Update collisions on Component Names
        this.collisions = diff;

        return mComponentNames;
    }

    /**
     * Class fuzzer init
     */
    void init(Context context) {

        // Init Fuzz Intent type list
        mFuzzingIntents.add("Null-old");
        mFuzzingIntents.add("Random-old");
        mFuzzingIntents.add("Semivalid-old");
        mFuzzingIntents.add("Semivalid-act-or-data(1)");
        mFuzzingIntents.add("Blank-act-or-data(2)");
        mFuzzingIntents.add("Random-act-or-data(3)");
        mFuzzingIntents.add("Random-extras(4)");
        mFuzzingIntents.add("Implicit-rand-data(5)");

        // Populate Provider Authority list
        this.populateProviders();

        // Init URI generator
        ug = new uriGen(context.getFilesDir().getPath(), rnd, mKnownProviders, context);
    }

    /* ---------------------------------------------------------------------------
     *
     * ---------------------------------------------------------------------------
     */


    private boolean filterPackage(String name) {

        if ( name.startsWith(ExperimentConstants.TARGET_APP) ) {
            return true;
        }

        return false;
    }


    /**
     * For any type, provide the registered instances based on what the package
     * manager has on file. Only provide exported components.
     *
     * @param type the IPC requested, activity, broadcast, etc.
     *
     * @return
     */
    protected ArrayList<ComponentName> getExportedComponents(IPCType type) {
        Log.d(TAG, String.format("getExportedComponents | type {%s}", type));

        ArrayList<ComponentName> found = new ArrayList<ComponentName>();
        PackageManager pm = this.mPackageManager;

        // TODO: Separate the function call per component type (that should reduce the return value)

        // Retrieve components installed on device from the Android Package Manager
        // GET_DISABLED_COMPONENTS was deprecated in API 24, for MATCH_DISABLED_COMPONENTS
        for (PackageInfo pi : pm
                .getInstalledPackages(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_ACTIVITIES
                        | PackageManager.GET_RECEIVERS
                        | PackageManager.GET_INSTRUMENTATION
                        | PackageManager.GET_PROVIDERS
                        | PackageManager.GET_SERVICES)) {
            PackageItemInfo items[] = null;

            switch (type) {
                case ACTIVITIES:
                    items = pi.activities;
                    break;
                case BROADCASTS:
                    items = pi.receivers;
                    break;
                case SERVICES:
                    items = pi.services;
                    break;
                case PROVIDERS:
                    items = pi.providers;
                    break;
                case INSTRUMENTATIONS:
                    items = pi.instrumentation;
            }

            if (items != null)
                for (PackageItemInfo item : items) {

                    if (filterPackage(pi.packageName)) {
                        Log.d(TAG, "getExportedComponents | found "
                            + "{" + pi.packageName + "}"
                            + "{" + item.name + "}");

                        found.add(new ComponentName(pi.packageName, item.name));
                    }
                }

        }

        return found;
    }

    /**
     * Populates the provider authority list: <code>mKnownProviders</code> from the PackageManager
     * of the target device.
     */
    protected void populateProviders()
    {
        PackageManager pm = this.mPackageManager;

        for (PackageInfo pack : pm.getInstalledPackages(PackageManager.GET_PROVIDERS)) {
            ProviderInfo[] providers = pack.providers;
            if (providers != null) {
                for (ProviderInfo provider : providers) {
                    String a = provider.authority;
                    if(a.contains(";"))
                    {
                        String[] aa = a.split(";");
                        for(String x : aa)
                        {
                            mKnownProviders.add(x);
                        }
                    }
                    else
                        mKnownProviders.add(a);
                }
            }
        }

    }

    /* ---------------------------------------------------------------------------
     * Main Methods (Fuzz Actions)
     * Initiate the fuzzing. The following methods are invoked by the Main Activity
     * ---------------------------------------------------------------------------
     */
    public String runSingle(IPCType type, FuzzType fuzzType, String clazz)
    {
        String out = "";
        int index;

        try {

            Log.i(TAG, "Seed: {" + this.seed + "} Version: {" + this.version + "}");

            Log.d(TAG, String.format("runSingle | Now fuzzing: %s %s %s",
                    IPCType.toName(type), clazz, dateFormat.format(new Date())));
            Log.d(TAG, String.format("runSingle | Current Fuzz Type: {%s}", fuzzType));

            switch(fuzzType)
            {
                case NULL:
                    out = fuzzNullSingle(type, clazz);
                    break;
                case RANDOM:
                    out = sendIntent(type, clazz);
                    break;
                case SEMIVALID:
                    out = sendIntent(type, clazz);
                    break;
                case EXPT1:
                    index = getComponentNameIndex(clazz);
                    out = expt1(index, type);
                    break;
                case EXPT2:
                    index = getComponentNameIndex(clazz);
                    out = expt2(index, type);
                    break;
                case EXPT3:
                    index = getComponentNameIndex(clazz);
                    out = expt3(index, type);
                    break;
                case EXPT4:
                    index = getComponentNameIndex(clazz);
                    out = expt4(index, type);
                    break;
                default:
                    break;
            }

            Thread.sleep(1000);
        }
        catch(Exception ex) {
            Log.e(TAG, Log.getStackTraceString(ex));
            out += "An exception occurred: \n"+ex.getMessage();
        }

        return out;
    }

    public String runAllExpts(IPCType type) {
        return runAllExpts(type, 0, mKnownComponents.size());
    }

    public String runAllExpts(IPCType type, int begin, int end) {

        // Check limits
        if (begin < 0 || end > mKnownComponents.size() || begin > end) {
            return String.format("Invalid begin {%d}, end {%d} sequence in runAll." +
                    "Component Size: {%d}", begin, end, mKnownComponents.size());
        }

        FuzzType[] experiments = {
                FuzzType.NULL,
                FuzzType.RANDOM,
                FuzzType.SEMIVALID,
                FuzzType.EXPT1,
                FuzzType.EXPT2,
                FuzzType.EXPT3,
                FuzzType.EXPT4
        };

        // Run experiments for all Fuzz Types
        for (FuzzType fuzz : experiments) {

            Log.i(TAG, "Seed: {" + this.seed + "} Version: {" + this.version + "}");

            Log.d(TAG, String.format("runAllExpts | Begin Fuzz type: {%s} {%s} from {%d} to {%d}",
                    FuzzType.toName(fuzz), type, begin, end));

            runExpt(type, fuzz, begin, end);
        }

        Log.d(TAG, "runAllExpts | End of Experiments");
        return "";
    }

    /**
     *
     * @param fuzzType
     * @param begin the starting Component Name id (index the list)
     * @param end the starting Component Name id (index the list)
     */
    private void runExpt(IPCType type, FuzzType fuzzType, int begin, int end) {


        Log.d(TAG, String.format("runExp | {%s} for {%s} (%d, %d)",
                type, fuzzType, begin, end));

        // For each component in current component type
        // call function based on whichExpt
        for(int i = begin; i < end; i++) {

            String className = mKnownComponents.get(i).getClassName();

            Log.d(TAG, String.format("runExp | Now fuzzing: {%s} at %s (%d out of %d)",
                    className, dateFormat.format(new Date()), i, end));

            if (isBlackListed(className)) {
                Log.d(TAG, String.format("runExp | Skipping Blacklisted Component: %s", className));
                continue;
            }

            switch(fuzzType){
                case NULL:
                    Log.d(TAG, "runExp | In Null (not executed)");
                    break;
                case RANDOM:
                    Log.d(TAG, "runExp | In Random (not executed)");
                    break;
                case SEMIVALID:
                    Log.d(TAG, "runExp | In Semivalid (not executed)");
                    break;
                case EXPT1:
                    // FIC A: Semi-valid Action and Data;
                    expt1(i, type);
                    break;
                case EXPT2:
                    // FIC B: Blank Action or Data
                    expt2(i, type);
                    break;
                case EXPT3:
                    // FIC C: Random Action or Data
                    expt3(i, type);
                    break;
                case EXPT4:
                    // FIC D: Random Extras
                    expt4(i, type);
                    break;
                default:
                    break;
            }
        }

        Log.d(TAG, String.format("runExp | Finished running Expt at: %s",
                dateFormat.format(new Date())));
    }


    /* ---------------------------------------------------------------------------
     * Experiments
     * ---------------------------------------------------------------------------
     */

    /**
     * FIC D: Random Extras
     * For each Action defined, we create a valid pair {Action, Data} with a set 1-5 Extra
     * fields with random values.
     *
     * @param index
     * @param type
     * @return
     */
    public String expt4 (int index, IPCType type) {
        int numIntent = 0;
        HashMap<String, Integer> map;

        Log.d(TAG, "expt4 | Fuzzing " +
                "actions {"  + LIST_ACTION.length  + "} " +
                "k {"        + NUM_RANDOM          + "} " +
                "total {"    + LIST_ACTION.length * NUM_RANDOM + "}");

        /* reset map */
        map = new HashMap<>();

        for (int k = 0; k < NUM_RANDOM; k++) {
            ActionDataPairs ad = new ActionDataPairs(my.getApplicationContext());

            // for each action/data pair
            for(String action : LIST_ACTION) {
                Intent intent = new Intent();
                Uri u = ad.get(action);

                /* add component, add action, add data */
                intent.setAction(action);
                if (u != null)
                    intent.setData(u);
                intent.setComponent(mKnownComponents.get(index));

                /* set Extras */
                int num_extras = rnd.nextInt(5);
                for (int c=0; c < num_extras; c++) {
                    intent.putExtra(new String(getRandomData(64, false)), getRandomData(256, false));
                }

                Log.d(TAG, "expt4 | " +
                        "n {"   + numIntent     + "} " +
                        "("     + index         + " of " +
                        ""      + mKnownComponents.size()   + ") " +
                        "{"         + mKnownComponents.get(index)  + "} " +
                        "type {"    + IPCType.toName(type)      + "} " +
                        "intent {"  + intent.getPackage()       + "}" +
                        "action {"  + action                + "}"
                );

                /* Send Intent */
                String ex = sendIntentByType(intent, type, index, null);
                if (ex != null) {
                    int c = map.containsKey(ex) ? map.get(ex): 0;

                    Log.d(TAG, "expt4 | " +
                            "n {"   + numIntent     + "} " +
                            "("     + index         + " of " +
                            ""      + mKnownComponents.size()   + ") " +
                            ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                    if (c > NUM_RUN_SKIP_EXCEPTION) {
                        Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                        return "Intents sent: " + numIntent;
                    }

                    map.put(ex, ++c);
                }

                numIntent++;

                if (numIntent % NUM_FREQ_GC == 0) {
                    try {
                        Log.d(TAG, "expt4 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IV");
                        System.gc();
                        Thread.sleep(NUM_DELAY_GC);
                    }
                    catch (InterruptedException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }

            }
        }
        Log.d(TAG, "expt4 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IV");
        System.gc();

        System.err.println("expt4 | Sent "+numIntent+" Intents\n");
        return "Intents sent: "+numIntent;
    }

    /**
     * FIC C: Random Action or Data
     * Either the Action or the Data is valid, and the other is set randomly.
     *
     * @param index
     * @param type
     * @return
     */
    public String expt3 (int index, IPCType type) {
        int numIntent = 0;
        Map<String, Integer> map;

        Log.d(TAG, "expt3 | random-action-data | Part 1 -- Fuzzing " +
                "actions: {"   + LIST_ACTION.length  + "} " +
                "k: {"         + NUM_RANDOM          + "} " +
                "total {"      + LIST_ACTION.length * NUM_RANDOM  + "}");

        // Experiment 3(a):
        // select an Action, send random Data

        /* reset map */
        map = new HashMap<>();

        for (String action : LIST_ACTION) {
            for(int count = 0; count < NUM_RANDOM; ) {
                Intent intent = new Intent();
                Uri u = getRandomUri("dumb");

                /* a URI could not be generated */
                if (u == null) {
                    Log.e(TAG, "expt3 {1} | Could not create random URI");
                    continue;
                }

                /* set component, set action, set data */
                intent.setAction(action);
                intent.setData(u);
                intent.setComponent(mKnownComponents.get(index));

                Log.d(TAG, "expt3 | Part 1 | " +
                        "n {"   + numIntent     + "} " +
                        "("     + index         + " of " +
                        ""      + mKnownComponents.size()   + ") " +
                        "{"         + mKnownComponents.get(index)  + "} " +
                        "type {"    + IPCType.toName(type)      + "} " +
                        "intent {"  + intent.getPackage()       + "}" +
                        "action {"  + action                + "}"
                );


                /* Send Intent */
                String ex = sendIntentByType(intent, type, index, null);
                if (ex != null) {
                    int c = map.containsKey(ex) ? map.get(ex): 0;

                    Log.d(TAG, "expt3 | Part 1 | " +
                            "n {"   + numIntent     + "} " +
                            "("     + index         + " of " +
                            ""      + mKnownComponents.size()   + ") " +
                            ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                    if (c > NUM_RUN_SKIP_EXCEPTION) {
                        Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                        return "Intents sent: " + numIntent;
                    }

                    map.put(ex, ++c);
                }

                numIntent++;
                count++;
            }

            if (numIntent % NUM_FREQ_GC == 0) {
                Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IIIA");
                System.gc();
            }
        }
        Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IIIA");
        System.gc();

        // Experiment 3(b):
        // select a semi-valid Data and set Action random
        // all uri types except "content:"

        /* reset map */
        map = new HashMap<>();

        Log.d(TAG, "expt3 | random-action-data | Part 2 -- Fuzzing " +
                "uri types: {"   + iUriTypes.length    + "} " +
                "k: {"           + NUM_RANDOM          + "} " +
                "total {"        + iUriTypes.length * NUM_RANDOM  + "}");

        for(int i=1; i < iUriTypes.length; i++) {
            Uri u = ug.getUri(iUriTypes[i], 0);
            for(int j=0; j < NUM_RANDOM; j++) {

                String action = new String(getRandomData(128, false));
                Intent intent = new Intent();

                /* set component, set data */
                intent.setAction(action);
                intent.setData(u);
                intent.setComponent(mKnownComponents.get(index));

                Log.d(TAG, "expt3 | Part 2 | " +
                        "n {"       + numIntent                   + "} " +
                        "("         + index                       + " of " +
                        ""          + mKnownComponents.size()     + ") " +
                        "{"         + mKnownComponents.get(index) + "} " +
                        "type {"    + IPCType.toName(type)        + "} " +
                        "intent {"  + intent.getPackage()         + "}" +
                        "uri {"     + u                           + "}"
                );

                /* Send Intent */
                String ex = sendIntentByType(intent, type, index, null);
                if (ex != null) {
                    int c = map.containsKey(ex) ? map.get(ex): 0;

                    Log.d(TAG, "expt3 | Part 2 | " +
                            "n {"   + numIntent     + "} " +
                            "("     + index         + " of " +
                            ""      + mKnownComponents.size()   + ") " +
                            ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                    if (c > NUM_RUN_SKIP_EXCEPTION) {
                        Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                        return "Intents sent: " + numIntent;
                    }

                    map.put(ex, ++c);
                }

                numIntent++;

                if(numIntent % NUM_FREQ_GC == 0) {
                    Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IIIB");
                    System.gc();
                }
            }
        }
        Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IIIB");
        System.gc();

        // uri type is "content:"
        // mKnownProviders.size() is replaced with a fixed count, we cannot test for all
        // content-providers, no time

        /* reset map */
        map = new HashMap<>();

        Log.d(TAG, "expt3 | random-action-data | Part 3 -- Fuzzing " +
                "content: {"   + NUM_CONTENT         + "} " +
                "k: {"         + NUM_RANDOM          + "} " +
                "total {"      + NUM_CONTENT * NUM_RANDOM  + "}");

        for(int i=0; i< NUM_CONTENT; i++) {

            Uri u = ug.getUri(iUriTypes[0],  rnd.nextInt(mKnownProviders.size())); // used to be i
            for(int j=0; j < NUM_RANDOM; j++) {

                String action = new String(getRandomData(128, false));
                Intent intent = new Intent();

                /* set component, set data */
                intent.setAction(action);
                intent.setData(u);
                intent.setComponent(mKnownComponents.get(index));

                Log.d(TAG, "expt3 | Part 3 | " +
                        "n {"       + numIntent                   + "} " +
                        "("         + index                       + " of " +
                        ""          + mKnownComponents.size()     + ") " +
                        "{"         + mKnownComponents.get(index) + "} " +
                        "type {"    + IPCType.toName(type)        + "} " +
                        "intent {"  + intent.getPackage()         + "}" +
                        "uri {"     + u                           + "}"
                );

                /* Send Intent */
                String ex = sendIntentByType(intent, type, index, null);
                if (ex != null) {
                    int c = map.containsKey(ex) ? map.get(ex): 0;

                    Log.d(TAG, "expt3 | Part 3 | " +
                            "n {"   + numIntent     + "} " +
                            "("     + index         + " of " +
                            ""      + mKnownComponents.size()   + ") " +
                            ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                    if (c > NUM_RUN_SKIP_EXCEPTION) {
                        Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                        return "Intents sent: " + numIntent;
                    }

                    map.put(ex, ++c);
                }

                numIntent++;

                if(numIntent % NUM_FREQ_GC == 0) {
                    Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IIIC");
                    System.gc();
                }
            }

        }
        Log.d(TAG, "expt3 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IIIC");
        System.gc();

        Log.d(TAG, "Sent " + numIntent + " Intents");
        return "Intents sent: " + numIntent;
    }

    /**
     * FIC B: Blank Action or Data
     * Either the Action OR Data is specified, but not both. All the other fields are left blank.
     *
     * @param index
     * @param type
     * @return
     */
    public String expt2 (int index, IPCType type) {
        int numIntent = 0;
        HashMap<String, Integer> map;

        Log.d(TAG, "expt2 | Part 1 -- Fuzzing " +
                "actions: {"   + iActions.length  + "} " +
                "uri types: {" + iUriTypes.length + "} " +
                "content: {"   + NUM_CONTENT      + "} " +
                "total {"      + iActions.length  + "}");

        // Experiment 2(a):
        // set action, keep data blank

        /* reset map */
        map = new HashMap<>();

        for (String act : LIST_ACTION) {

            Intent intent = new Intent();
            //set component, set action
            intent.setAction(act);
            intent.setComponent(mKnownComponents.get(index));
            //System.err.println("Sending Intent:"+in);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //System.out.println(in);

            //if (numIntent % 100 == 0)
            Log.d(TAG, "expt2 | " +
                    "n {"       + numIntent                    + "} " +
                    "("         + index                        + " of " +
                    ""          + mKnownComponents.size()      + ") " +
                    "{"         + mKnownComponents.get(index)  + "} " +
                    "type {"    + IPCType.toName(type)         + "} " +
                    "intent {"  + intent.getPackage()          + "}" +
                    "action {"  + act                          + "}"
            );


            /* Send Intent */
            String ex = sendIntentByType(intent, type, index, null);
            if (ex != null) {
                int c = map.containsKey(ex) ? map.get(ex): 0;

                Log.d(TAG, "expt2 | Part 1 " +
                        "n {"   + numIntent                 + "} " +
                        "("     + index                     + " of " +
                        ""      + mKnownComponents.size()   + ") " +
                        ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                if (c > NUM_RUN_SKIP_EXCEPTION) {
                    Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                    return "Intents sent: " + numIntent;
                }

                map.put(ex, ++c);
            }

            numIntent++;
        }
        Log.d(TAG, "expt2 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IIA");
        System.gc();

        // Experiment 2(b):
        // Set data, keep action blank
        // all uri types except "content:"

        /* reset map */
        map = new HashMap<>();

        for(int i=1; i < iUriTypes.length; i++) {
            Uri u = ug.getUri(iUriTypes[i], 0);
            Intent intent = new Intent();
            //set component, set data
            intent.setData(u);
            intent.setComponent(mKnownComponents.get(index));
            //System.err.println("Sending Intent:"+in);

            Log.d(TAG, String.format("expt2 | n {%d} (%d of %d) {%s} type {%s} intent {%s} uri {%s}",
                    numIntent,
                    index, mKnownComponents.size(),
                    mKnownComponents.get(index),
                    IPCType.toName(type),
                    intent.getPackage(),
                    iUriTypes[i]
            ));

            Log.d(TAG, "expt2 | " +
                    "n {"       + numIntent                    + "} " +
                    "("         + index                        + " of " +
                    ""          + mKnownComponents.size()      + ") " +
                    "{"         + mKnownComponents.get(index)  + "} " +
                    "type {"    + IPCType.toName(type)         + "} " +
                    "intent {"  + intent.getPackage()          + "}" +
                    "uri {"     + iUriTypes[i]                 + "}"
            );

            /* Send Intent */
            String ex = sendIntentByType(intent, type, index, map);
            if (ex != null) {
                int c = map.containsKey(ex) ? map.get(ex): 0;

                Log.d(TAG, "expt2 | Part 2 " +
                        "n {"   + numIntent                 + "} " +
                        "("     + index                     + " of " +
                        ""      + mKnownComponents.size()   + ") " +
                        ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                if (c > NUM_RUN_SKIP_EXCEPTION) {
                    Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                    return "Intents sent: " + numIntent;
                }

                map.put(ex, ++c);
            }


            numIntent++;
        }

        // uri type is "content:"

        /* reset map */
        map = new HashMap<>();

        for(int i=0; i < NUM_CONTENT; i++) {

            Uri u = ug.getUri(iUriTypes[0],  rnd.nextInt(mKnownProviders.size())); //used to be i
            Intent intent = new Intent();
            //set component, set data
            intent.setData(u);
            intent.setComponent(mKnownComponents.get(index));
            //System.err.println("Sending Intent:"+in);

            Log.d(TAG, String.format("expt2 | n {%d} (%d of %d) {%s} type {%s} intent {%s} uri {%s}",
                    numIntent,
                    index, mKnownComponents.size(),
                    mKnownComponents.get(index),
                    IPCType.toName(type),
                    intent.getPackage(),
                    iUriTypes[i]
            ));

            /* Send Intent */
            String ex = sendIntentByType(intent, type, index, map);
            if (ex != null) {
                int c = map.containsKey(ex) ? map.get(ex): 0;

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                if (c > NUM_RUN_SKIP_EXCEPTION) {
                    Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                    return "Intents sent: " + numIntent;
                }

                map.put(ex, c++);
            }


            numIntent++;
        }
        Log.d(TAG, "expt2 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IIB");
        System.gc();

        Log.d(TAG, "expt2 | Sent "+numIntent+" Intents\n");
        return "Intents sent: "+numIntent;
    }

    /**
     * FIC A: Semi valid Action and Data
     * Valid Action and Valid Data URI are generated separately, but the combination of them
     * may be invalid.
     *
     * @param index the index of a Component Name in mKnownComponents.
     * @param type the type of the Component.
     * @return
     */
    public String expt1 (int index, IPCType type) {
        int numIntent = 0;
        int gcCount = 0;

        HashMap<String, Integer> map;

        Log.d(TAG, "expt1 | Part 1 -- Fuzzing " +
                "uri types: {"  + iUriTypes.length  + "} " +
                "actions {"     + iActions.length   + "} " +
                "total {"       + iUriTypes.length * iActions.length+ "}");

        // This is for all uri types EXCEPT "content"
        // if needed repeat k times

        /* reset map */
        map = new HashMap<>();

        for(int i=1; i < iUriTypes.length; i++) {

            Uri u = ug.getUri(iUriTypes[i], 0);
            for(String action : LIST_ACTION) {

                Intent intent = new Intent();

                /* EXP */
                // action = "ACTION_DREAMING_STARTED";

                /* set component, set action, set data */
                intent.setAction(action);
                intent.setData(u);
                intent.setComponent(mKnownComponents.get(index));

                // eba|tmp
                // intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Log.d(TAG, "expt1 | " +
                        "n {"   + numIntent     + "} " +
                        "("     + index         + " of " +
                        ""      + mKnownComponents.size()   + ") " +
                        "{"         + mKnownComponents.get(index)  + "} " +
                        "type {"    + IPCType.toName(type)      + "} " +
                        "intent {"  + intent.getPackage()       + "}" +
                        "uri {"     + iUriTypes[i]          + "} " +
                        "action {"  + action                + "}"
                );

                /* Send Intent */
                String ex = sendIntentByType(intent, type, index, null);
                if (ex != null) {
                    int c = map.containsKey(ex) ? map.get(ex): 0;

                    Log.d(TAG, "expt1 | " +
                            "n {"   + numIntent     + "} " +
                            "("     + index         + " of " +
                            ""      + mKnownComponents.size()   + ") " +
                            ex + " {" + c + "/" + NUM_RUN_SKIP_EXCEPTION + "}");

                    /* skip fuzzing if we pass the limit of repeated case allowed for that exception */
                    if (c > NUM_RUN_SKIP_EXCEPTION) {
                        Log.d(TAG, "Skipping after " + numIntent++ + " Intents");
                        return "Intents sent: " + numIntent;
                    }

                    map.put(ex, ++c);
                }


                numIntent++;

                if (numIntent % NUM_FREQ_GC == 0) {
                    try {
                        Log.d(TAG, "expt1 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IA {" + ++gcCount + "}");
                        System.gc();
                        Thread.sleep(NUM_DELAY_GC);
                    }
                    catch (InterruptedException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            }
        }

        Log.d(TAG, "expt1 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IA");
        System.gc();

//        Log.d(TAG, String.format("expt1 | Part 2 -- Fuzzing content types {%d}", NUM_CONTENT));
//
//        // This is when uri type is "content"
//        // if needed repeat k times
//        for(int i=0; i < NUM_CONTENT; i++)
//        {
//            Uri u = ug.getUri(iUriTypes[0], rnd.nextInt(mKnownProviders.size())); //index used to be i //20111203
//            for(String act : iActions)
//            {
//                Intent in = new Intent();
//                //set component, set action, set data
//                in.setAction(act);
//                in.setData(u);
//                in.setComponent(mKnownComponents.get(index));
//                in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                //System.out.println(in);
//
//                Log.d(TAG, String.format("expt1 | n {%d} (%d/%d) (%d of %d) {%s} type {%s} intent {%s} uri {%s} action {%s}",
//                        numIntent,
//                        i, NUM_CONTENT,
//                        index, mKnownComponents.size(),
//                        mKnownComponents.get(index),
//                        IPCType.toName(type),
//                        in.getPackage(),
//                        u,
//                        act
//                        ));
//
//                sendIntentByType(in, type);
//                numIntent++;
//
//                if(numIntent % 300 == 0)
//                {
//                    Log.d(TAG, "expt1 | ******** FORCING GARBAGE COLLECTION WITHIN EXPT IB");
//                    System.gc();
//                }
//            }
//        }
//        Log.d(TAG, "expt1 | ******** FORCING GARBAGE COLLECTION AFTER EXPT IB");
//        System.gc();

        Log.d(TAG, "expt1 | Sent "+numIntent+" Intents\n");
        return "Intents sent: "+numIntent;
    }


    /**
     * Fuzz all components of a given type of IPC. The type must be matched to the currently
     * populated known components as this code is tightly coupled to the UIs implementation.
     *
     * @param type the type of IPC
     * @return String that gives a summary of what was done
     */
    public String fuzzAll(IPCType type, FuzzType fuzzType) throws Exception {
        Log.d(TAG, String.format("fuzzAll | {%s} {%s} init",
                IPCType.toName(type), FuzzType.toName(fuzzType)));

        int count;
        String out="";

        switch (fuzzType) {
            case NULL:
                switch (type) {
                    case ACTIVITIES:
                    {
                        count = nullFuzzAllActivities(mKnownComponents);
                        out = "Started: " + count + " Activities";
                        break;
                    }

                    case BROADCASTS:
                    {
                        count = nullFuzzAllBroadcasts(mKnownComponents);
                        out = "Sent: " + count + " broadcasts";
                        break;
                    }

                    case SERVICES:
                    {
                        count = nullFuzzAllServices(mKnownComponents);
                        out = "Started: " + count + " services";
                        break;
                    }

                    default:
                        out = "Not Implemented";
                }
            case RANDOM:
            {
                sendIntentToAll(type);
                out = "Sent random Intent to all";
                break;
            }

            case SEMIVALID:
            {
                sendIntentToAll(type);
                out = "Sent semi-valid Intent to all";
                break;
            }

            default:
                out = "Not implemented.";
        }

        Log.d(TAG, String.format("fuzzAll | {%s} {%s} done",
                IPCType.toName(type), FuzzType.toName(fuzzType)));
        return out;
    }

    /**
     * Fuzz a single Component Name of a given type of IPC. The type must be matched to the
     * currently populated known components as this code is tightly couple to the UIs
     * implementation.
     *
     * @param type the type of IPC
     * @param clazz the classname of the Component Name
     * @return String that gives a summary of what was done
     */
    public String fuzzNullSingle(IPCType type, String clazz) {
        ComponentName toTest = null;
        Intent intent = new Intent();

        for (ComponentName c : mKnownComponents) {
            if (c.getClassName().equals(clazz)) {
                toTest = c;
                break;
            }
        }
        intent.setComponent(toTest);

        Log.d(TAG, String.format("fuzzNullSingle {%s} {%s} {%d}",
                clazz, toTest, mKnownComponents.size()));

        try {
            return sendIntentByType(intent, type);
        } catch (Exception ex) {

        }

        return "";
    }

    /**
     *
     * @param comps
     * @return
     * @throws Exception
     */
    protected int nullFuzzAllActivities(List<ComponentName> comps) throws Exception {
        int count = 0;
        int begin = 0;
        int limit = comps.size();

        for (int i = begin; i < limit; i++) {
            Intent in = new Intent();
            in.setComponent(comps.get(i));

            // Check if the classname is not in the black list
            if (isBlackListed(comps.get(i).getClassName())) {
                Log.d(TAG, "nullFuzzAllActivities | skipping activity: " + i);
                continue;
            } else {
                try {

                    Log.d(TAG, String.format("nullFuzzAllActivities | Null fuzzing activity: " +
                            "(%d of %d) %s", i, limit, comps.get(i).toString()));
                    Log.d(TAG, "nullFuzzAllActivities | I have current focus:  "
                            + my.hasWindowFocus());

                    my.startActivityForResult(in, 1999+i);
                    Thread.sleep(750);

                    // Dismiss any alert that popped up
                    Intent dismiss = new Intent();
                    dismiss.setAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    my.sendBroadcast(dismiss);

                    my.finishActivity(1999+i);
                }
                catch (Exception ex) {
                    Log.e(TAG, Log.getStackTraceString(ex));
                    new Exception("Cannot launch: " + comps.get(i) + "\n" + ex.getMessage(), ex);
                }
            }
            count++;
        }
        return count;
    }

    /**
     *
     * @param comps
     * @return
     */
    int nullFuzzAllBroadcasts(List<ComponentName> comps) throws Exception {
        int count = 0;
        int begin = 0;
        int limit = comps.size();

        for (int i = begin; i < limit; i++) {
            Log.d(TAG, String.format("nullFuzzAllBroadcasts | Null fuzzing broadcast: " +
                    "(%d of %d) %s", i, limit, comps.get(i).toString()));

            Intent in = new Intent();
            in.setComponent(comps.get(i));
            try {
                my.sendBroadcast(in);
                Thread.sleep(500);
            } catch (Exception ex) {
                Log.e(TAG, Log.getStackTraceString(ex));
                new Exception("Cannot launch: " + comps.get(i) + "\n" + ex.getMessage(), ex);
            }
            count++;
        }
        return count;
    }

    /**
     *
     * @param comps
     * @return
     * @throws Exception
     */
    int nullFuzzAllServices(List<ComponentName> comps) throws Exception {
        int count = 0;
        int begin = 0;
        int limit = comps.size();

        for (int i = begin; i < limit; i++) {
            Log.d(TAG, String.format("nullFuzzAllServices | Null fuzzing services: " +
                    "(%d of %d) %s", i, limit, comps.get(i).toString()));

            Intent in = new Intent();
            in.setComponent(comps.get(i));
            try {
                my.startService(in);
                Thread.sleep(1000);
                my.stopService(in);
            } catch (Exception ex) {
                Log.e(TAG, Log.getStackTraceString(ex));
                new Exception("Cannot launch: " + comps.get(i), ex);
            }

            count++;
        }
        return count;
    }



    /* ---------------------------------------------------------------------------
     * Intent helper
     * ---------------------------------------------------------------------------
     */

    public Activity my;

    private Intent buildRandomIntent(ComponentName toTest) {
        Intent i = new Intent();
        i.setComponent(toTest);

        /**************Fuzzing action**********/
        //i.setAction(getTypeString(iActions));
        i.setAction(new String(getRandomData(128, false)));
        /**************Fuzzing Uri*************/
        i.setData(getRandomUri("dumb"));
        /**************Fuzzing Extras**********/
        //i.putExtra(getTypeString(iExtras), getRandomData(512, false));
        int num_extras = rnd.nextInt(5);
        for(int c=0; c < num_extras; c++) {
            i.putExtra(new String(getRandomData(64, false)), getRandomData(256, false));
        }

        //	i.putExtra("EXTRA_KEY_EVENT", new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        //	i.putExtra("EXTRA_KEY_EVENT", new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        return i;
    }

    /**
     *
     * The function support fuzzing components other than activities.
     *
     * @param type
     * @return
     */
    public String sendIntent(IPCType type, String clazz) {

        ComponentName toTest = null;

        for (ComponentName c : mKnownComponents) {
            if (c.getClassName().equals(clazz)) {
                toTest = c;
                break;
            }
        }

        String outStr = "\nJust testing!\nPackage name: ";
        if(toTest != null)
        {
            outStr += toTest.getPackageName() + "\n" + clazz + "\n";

            for(int k=0; k<5; k++) {
                Intent intent = buildRandomIntent(toTest);
            }
        }
        return outStr;
    }

    public void sendIntentToAll(IPCType type) {

        for (String clazz : mComponentNames) {

            try {
                Log.d(TAG, String.format("sendItentToAll | Now fuzzing: %s", clazz));
                if (isBlackListed(clazz)) {
                    Log.d(TAG, String.format("sendIntentToAll | Skipping component {%s}", clazz));
                    continue;
                } else {
                    String out = sendIntent(type, clazz);
                    //mOut.append(out);
                }
            }
            catch (Exception ex) {
                Log.e(TAG, "An exception ocurred");
                Log.e(TAG, Log.getStackTraceString(ex));
            }
        }

    }


    /**
     *
     * @param intent
     * @param type
     * @return
     * @throws Exception
     */
    private String sendIntentByType(Intent intent, IPCType type) throws Exception {
        return sendIntentByType(intent, type, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: {" + data + "}");
    }

    /**
     * Initiate an IPC of a given type from the Activity by sending an Intent.
     * Invoked internally by Null Fuzz Single.
     *
     * @param intent the Intent that is going to be sent.
     * @param type the type of IPC.
     * @param index the index of a ComponentName in the mKnownComponents array.
     * @return {@link String} that gives a summary the action.
     */
    private String sendIntentByType(Intent intent, IPCType type, int index) throws Exception {

        Log.d(TAG, "SendIntentbyType | " + intent);

        try {

            // Initiate IPC according component type
            switch (type) {
                case ACTIVITIES:

                    my.startActivityForResult(intent, 1999+index);
                    Thread.sleep(NUM_DELAY_START_ACT);
                    my.finishActivity(1999+index);

                    Log.d(TAG, "SendIntentbyType | Started Activity: {" + intent.getComponent() + "}");
                    return "Started: " + intent.getPackage();

                case BROADCASTS:
                    my.sendBroadcast(intent);
                    Thread.sleep(NUM_DELAY_START_SRV);

                    Log.d(TAG, "SendIntentbyType | Sent broadcast: {" + intent.getPackage() + "}");
                    return "Sent broadcast: " + intent.getPackage();

                case SERVICES:
                    my.startService(intent);
                    Thread.sleep(NUM_DELAY_START_SRV);
                    // Stopping service
                    my.stopService(intent);

                    Log.d(TAG, "SendIntentbyType | Started service: {" + intent.getPackage() + "}");
                    return "Started: " + intent.getPackage();

                case PROVIDERS:
                    // uh - providers don't use Intents...what am I doing...
                    Log.d(TAG, "Not implemeted");
                    return "Not Implemented";

                case INSTRUMENTATIONS:
                    my.startInstrumentation(intent.getComponent(), null, null);
                    // not intent based you could fuzz these params, if anyone cared.
                    Log.d(TAG, "Not implemeted");
                    return "Not Implemented";
            }
        }
        catch (Exception ex) {
            throw ex;
        }
        catch (Error e) {
            throw e;
        }

        return "";
    }

    /**
     * Initiate an IPC of a given type from the Activity by sending an Intent.
     * Invoked internally by fuzzer methods.
     *
     * @param intent the Intent that is going to be sent.
     * @param type the type of IPC.
     * @param index the index of a ComponentName in the mKnownComponents array.
     * @param count the map to keep track of each Exception count during the experiment.
     *
     * @return {@link String} that gives a summary the action.
     */
    private String sendIntentByType(Intent intent, IPCType type, int index, Map<String, Integer> count) {

        try {
            sendIntentByType(intent, type, index);
        } catch (SecurityException ex) {
            Log.e(TAG, "sendIntent | #SecurityException A security exception occurred while fuzzing", ex);
            return ex.getClass().getSimpleName();
        } catch (InterruptedException ex) {
            Log.e(TAG, "sendIntent | #InterruptedException An interrupted exception occurred while fuzzing", ex);
            return ex.getClass().getSimpleName();
        } catch (Exception ex) {
            Log.e(TAG, "sendIntent | #Exception An exception occurred while fuzzing", ex);
            return ex.getClass().getSimpleName();
        } catch (Error e) {
            Log.e(TAG, "sendIntent | #Error An error occurred while fuzzing", e);
            return e.getClass().getSimpleName();
        } catch (Throwable t) {
            Log.e(TAG, "SendIntent | #Throwable An unexpected error ocurred while fuzzing", t);
            return t.getClass().getSimpleName();
        }

        return null;
    }


    protected Intent fuzzBroadcast(ComponentName toTest) {
        Intent i = new Intent();
        i.setComponent(toTest);
        sendBroadcast(i);
        return i;
    }


}
