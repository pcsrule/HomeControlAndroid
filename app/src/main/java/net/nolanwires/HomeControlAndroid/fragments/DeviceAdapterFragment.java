package net.nolanwires.HomeControlAndroid.fragments;

import android.support.v4.app.Fragment;

/**
 * Created by nolan on 2/18/16.
 */
public abstract class DeviceAdapterFragment extends Fragment {

    @Override
    public abstract String toString();
    public abstract String getDetails();
    public abstract boolean getEnabled();
}
