package edu.purdue.android.fuzzer.squibble.common;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public enum FuzzType {
    NULL,
    RANDOM,
    SEMIVALID,
    EXPT1,
    EXPT2,
    EXPT3,
    EXPT4,
    EXPT5,
    EXPT6,
    EXPT7;

    /**
     * Mapping from ipcTypes to Strings for display. Overhead because you can't
     * switch on strings.
     */
    static Map<String, FuzzType> fuzzNamesToTypes = new HashMap<String, FuzzType>();
    static Map<FuzzType, String> fuzzTypesToNames = new TreeMap<FuzzType, String>();

    static {
        fuzzNamesToTypes.put("Null-old", FuzzType.NULL);
        fuzzNamesToTypes.put("Random-old", FuzzType.RANDOM);
        fuzzNamesToTypes.put("Semivalid-old", FuzzType.SEMIVALID);
        fuzzNamesToTypes.put("Semivalid-act-or-data(1)", FuzzType.EXPT1);
        fuzzNamesToTypes.put("Blank-act-or-data(2)", FuzzType.EXPT2);
        fuzzNamesToTypes.put("Random-act-or-data(3)", FuzzType.EXPT3);
        fuzzNamesToTypes.put("Random-extras(4)", FuzzType.EXPT4);
        fuzzNamesToTypes.put("Implicit-rand-data(5)", FuzzType.EXPT5);

        for (Map.Entry<String, FuzzType> e : fuzzNamesToTypes.entrySet()) {
            fuzzTypesToNames.put(e.getValue(), e.getKey());
        }
    }

    public static String toName(FuzzType type) {
        return fuzzTypesToNames.get(type);
    }

    public static FuzzType fromName(String name) {
        return fuzzNamesToTypes.get(name);
    }

}