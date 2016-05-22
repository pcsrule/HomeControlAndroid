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

public abstract class DeviceAdapterFragment extends Fragment {
    private static final String ARG_COMMAND = "arg_command";
    static Map<Class, String[]> ADAPTER_KEYWORDS;
    static Map<Class, String> ADAPTER_NAMES;
    static ArrayList<Class<? extends DeviceAdapterFragment>> ADAPTERS;

    static {
        ADAPTERS = new ArrayList<>();
        ADAPTER_KEYWORDS = new HashMap<>();
        ADAPTER_NAMES = new HashMap<>();

        TcpHubAdapterFragment.init();
        CoapDeviceAdapterFragment.init();
        LgSmartTvAdapterFragment.init();
        ChromecastAdapterFragment.init();

    }

    public static DeviceAdapterFragment newInstance(Class adapterClass, String command) {

        Bundle args = new Bundle();

        if (command != null)
            args.putString(ARG_COMMAND, command);

        try {
            DeviceAdapterFragment fragment = null;

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
