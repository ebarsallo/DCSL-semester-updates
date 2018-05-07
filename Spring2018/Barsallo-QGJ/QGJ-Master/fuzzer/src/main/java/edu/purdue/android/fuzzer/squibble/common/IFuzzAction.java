package edu.purdue.android.fuzzer.squibble.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ebarsallo
 */

public enum IFuzzAction {
    START_ACTION,
    LOAD_ACTION,
    FUZZ_SINGLE_ACTION,
    FUZZ_ALL_ACTION,
    RUN_ALL_EXPERIMENTS;


    static Map<IFuzzAction, String> iFuzzActionToPath = new HashMap<>();
    static Map<String, IFuzzAction> iFuzzPathToAction = new HashMap<String, IFuzzAction>();


    static {
        iFuzzActionToPath.put(IFuzzAction.START_ACTION, "/start");
        iFuzzActionToPath.put(IFuzzAction.LOAD_ACTION, "/load");
        iFuzzActionToPath.put(IFuzzAction.FUZZ_SINGLE_ACTION, "/fuzzsingle");
        iFuzzActionToPath.put(IFuzzAction.FUZZ_ALL_ACTION, "/fuzzall");
        iFuzzActionToPath.put(IFuzzAction.RUN_ALL_EXPERIMENTS, "/runall");

        for (Map.Entry<IFuzzAction, String> e : iFuzzActionToPath.entrySet()) {
            iFuzzPathToAction.put(e.getValue(), e.getKey());
        }
    }

    public static String toPath(IFuzzAction action) {
        return iFuzzActionToPath.get(action);
    }

    public static IFuzzAction fromPath(String path) {
        return iFuzzPathToAction.get(path);
    }

}


