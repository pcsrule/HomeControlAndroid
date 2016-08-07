package net.nolanwires.HomeControlAndroid.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by nolan on 2/18/16.
 */

public class DeviceAdapterFragment extends Fragment {
    private static final String ARG_COMMAND = "arg_command";
    static Map<Class, String[]> ADAPTER_KEYWORDS;
    static Map<Class, String> ADAPTER_NAMES;
    static ArrayList<Class<? extends DeviceAdapterFragment>> ADAPTERS;

    static {
        ADAPTERS = new ArrayList<>();
        ADAPTER_KEYWORDS = new HashMap<>();
        ADAPTER_NAMES = new HashMap<>();

        ADAPTERS.add(TcpHubAdapterFragment.class);
        ADAPTER_NAMES.put(TcpHubAdapterFragment.class, "Lighting");
        ADAPTER_KEYWORDS.put(TcpHubAdapterFragment.class, new String[]{"light"});

        ADAPTERS.add(CoapDeviceAdapterFragment.class);
        ADAPTER_NAMES.put(CoapDeviceAdapterFragment.class, "LED Strip");
        ADAPTER_KEYWORDS.put(CoapDeviceAdapterFragment.class, new String[]{"led", "strip"});

        ADAPTERS.add(LgSmartTvAdapterFragment.class);
        ADAPTER_NAMES.put(LgSmartTvAdapterFragment.class, "Projector");
        ADAPTER_KEYWORDS.put(LgSmartTvAdapterFragment.class, new String[]{"projector"});

    }

    public static DeviceAdapterFragment newInstance(Class adapterClass, String command) {

        Bundle args = new Bundle();

        if (command != null)
            args.putString(ARG_COMMAND, command);

        try {
            DeviceAdapterFragment fragment;

            // Call the "first" constructor in the specified adapter with reflection because Java
            fragment = (DeviceAdapterFragment) adapterClass.getDeclaredConstructors()[0].newInstance();
            fragment.setArguments(args);
            return fragment;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getNameForType(Class<? extends DeviceAdapterFragment> type) {
        return ADAPTER_NAMES.get(type);
    }

    //public abstract String getDetails();

    /**
     * Get list of keywords associated with what this device controls.
     *
     * @return List of associated keywords.
     */
    public static String[] getKeywords(Class<? extends DeviceAdapterFragment> adapterType) {
        return ADAPTER_KEYWORDS.get(adapterType);
    }

    public static List<Class<? extends DeviceAdapterFragment>> getAdapters() {
        return ADAPTERS;
    }

    public static DeviceAdapterFragment constructFragmentForIndex(int index, String command) {
        return DeviceAdapterFragment.newInstance(ADAPTERS.get(index), command);
    }

    //public abstract boolean getEnabled();
}
