package net.nolanwires.HomeControlAndroid.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.WakeOnLanClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by nolan on 8/16/16.
 */
public class WakeOnLanAdapterFragment extends DeviceAdapterFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String ADAPTER_DETAILS = "Wake On LAN";
    private static final String PREFS_KEY_HOST_LIST = "WOL_HOST_LIST";
    public static final int MSG_MAC_FOUND = 69;
    private static final int MSG_HOST_FOUND = 420;
    private static final String JSON_FIELD_HOSTNAME = "hostname";
    private static final String JSON_FIELD_MAC = "MAC";
    private static final String JSON_FIELD_IP = "IP";

    private ArrayAdapter<WakeOnLanClient> mArrayAdapter = null;
    private InetAddress mBroadcastAddress;
    private Handler mHandler;
    private SharedPreferences mPrefs;
    private ArrayList<WakeOnLanClient> mHosts;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private String TAG = "WOLFragment";
    private NsdManager mNsdManager;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.wol, menu);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", MODE_PRIVATE);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(ADAPTER_DETAILS);
        }

        setHasOptionsMenu(true);

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_HOST_FOUND:
                        mArrayAdapter.add((WakeOnLanClient) msg.obj);
                        break;
                    case MSG_MAC_FOUND:
                        if (mArrayAdapter != null)
                            mArrayAdapter.notifyDataSetChanged();
                        writeHostList();
                        break;
                }
            }
        };

        mBroadcastAddress = getBroadcastAddress();
        initializeDiscoveryListener();
        mHosts = new ArrayList<>();
        readHostList();


        mArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, mHosts);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_button:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Host to Wake");
                builder.setMessage("Enter a DNS resolvable hostname or IP. Must be on the same L2 network.");

                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String host = input.getText().toString();
                        if (host.length() != 0)
                            mArrayAdapter.add(new WakeOnLanClient(
                                    host, null, null, mBroadcastAddress, mHandler));
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initializeDiscoveryListener() {
        mNsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);
        // Instantiate a new DiscoveryListener
        Log.d(TAG, "init service discovery");
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                for (WakeOnLanClient c : mHosts) {
                    if (service.getServiceName().equals(c.getDNSName()))
                        return;
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_HOST_FOUND, new WakeOnLanClient(
                        service.getServiceName(),
                        null,
                        null,
                        mBroadcastAddress,
                        mHandler)));
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Start Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager.discoverServices("_nvstream._tcp", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void readHostList() {
        try {
            JSONArray hostsJson = new JSONArray(mPrefs.getString(PREFS_KEY_HOST_LIST, null));
            for (int i = 0; i < hostsJson.length(); ++i) {
                JSONObject obj = hostsJson.getJSONObject(i);
                String host = obj.getString(JSON_FIELD_HOSTNAME);
                String mac = null;

                if (obj.has(JSON_FIELD_MAC))
                    mac = obj.getString(JSON_FIELD_MAC);

                mHosts.add(new WakeOnLanClient(
                        host,
                        null,
                        mac,
                        mBroadcastAddress,
                        mHandler));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeHostList() {
        JSONArray hostsJson = new JSONArray();

        for (WakeOnLanClient c : mHosts) {
            JSONObject hostObj = new JSONObject();
            try {
                hostObj.put(JSON_FIELD_HOSTNAME, c.getDNSName());
                hostObj.put(JSON_FIELD_MAC, c.getMACString());
                hostObj.put(JSON_FIELD_IP, c.getIPAddress().getHostAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
            hostsJson.put(hostObj);
        }
        mPrefs.edit().putString(PREFS_KEY_HOST_LIST, hostsJson.toString()).apply();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ListView v = new ListView(getContext());
        v.setOnItemClickListener(this);
        v.setOnItemLongClickListener(this);
        v.setAdapter(mArrayAdapter);
        return v;
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mArrayAdapter.getItem(i).doRequest();
    }

    @Override
    public void onPause() {
        try {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } catch (Exception ignored) {

        }
        for (int i = 0; i < mArrayAdapter.getCount(); ++i) {
            mArrayAdapter.getItem(i).interrupt();
        }
        super.onPause();
    }

    private InetAddress getBroadcastAddress() {
        try {
            WifiManager wifi = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();

            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) (broadcast >> (k * 8));
            return InetAddress.getByAddress(quads);
        } catch (Exception ignored) {

        }
        return null;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        mArrayAdapter.remove(mArrayAdapter.getItem(i));
        writeHostList();
        return true;
    }
}
