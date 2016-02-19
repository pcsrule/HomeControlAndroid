package net.nolanwires.HomeControlAndroid;

import net.nolanwires.HomeControlAndroid.fragments.CoapDeviceAdapterFragment;
import net.nolanwires.HomeControlAndroid.fragments.DeviceAdapterFragment;
import net.nolanwires.HomeControlAndroid.fragments.TcpHubAdapterFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nolan on 2/17/16.
 * Describes a device or group of devices that can be controlled to be displayed
 * on the left pane of the app in a list. This should contain all information needed
 * to construct a controller class for the device.
 */
public class DeviceContent {

    public static final List<DeviceAdapterFragment> ITEMS = new ArrayList<>();

    static {
        addDeviceAdapter(new CoapDeviceAdapterFragment());
        addDeviceAdapter(new TcpHubAdapterFragment());
    }

    private static void addDeviceAdapter(DeviceAdapterFragment deviceAdapter) {
        ITEMS.add(deviceAdapter);
    }
}
