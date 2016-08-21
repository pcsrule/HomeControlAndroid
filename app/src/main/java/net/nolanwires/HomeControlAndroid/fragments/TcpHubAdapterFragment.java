package net.nolanwires.HomeControlAndroid.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.nolanwires.HomeControlAndroid.activities.DeviceDetailActivity;
import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.TcpConnectedLightingClient;

/**
 * Created by nolan on 2/18/16.
 */
public class TcpHubAdapterFragment extends DeviceAdapterFragment implements TcpConnectedLightingClient.OnLightStatusUpdateListener, Runnable {

    // Public adapter attributes
    public static final String ADAPTER_DETAILS = "TCP Connected lighting";
    private static final int POLL_DELAY_MS = 2000;
    private static final String PREFS_KEY_TOKEN = "TCPHUBTOKEN";

    private TcpConnectedLightingClient mLightingClient;
    private LightListAdapter mAdapter;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private LinearLayout progressLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(ADAPTER_DETAILS);
        }

        mHandler = new Handler();

    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // See if we have an auth token and start client
        mPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        String token = mPrefs.getString(PREFS_KEY_TOKEN, null);
        mLightingClient = new TcpConnectedLightingClient(getContext(), this, token);

        // Construct views
        View v = inflater.inflate(R.layout.tcphub_detail_fragment, container, false);
        ListView lv = (ListView) v.findViewById(R.id.listView);
        mAdapter = new LightListAdapter();
        lv.setAdapter(mAdapter);

        // Display progress indicator if we don't have a saved auth token
        progressLayout = (LinearLayout) v.findViewById(R.id.progressLayout);
        progressLayout.setVisibility(token == null ? View.VISIBLE : View.GONE);

        // Start polling
        run();

        return v;
    }

    @Override
    public void run() {
        mLightingClient.getLights();
        mHandler.postDelayed(this, POLL_DELAY_MS);
    }

    @Override
    public void OnLightStatusUpdate(String token) {
        if (token != null && progressLayout.getVisibility() == View.VISIBLE) {
            mPrefs.edit().putString(PREFS_KEY_TOKEN, token).apply();
            progressLayout.setVisibility(View.GONE);
            return;
        }

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String command = bundle.getString(DeviceDetailActivity.ARG_COMMAND);

            if (command != null) {
                TcpConnectedLightingClient.Light l = mLightingClient.getLightForName(command);

                boolean isOn = false;
                if ((command.contains("off") || (isOn = command.contains("on"))) && l != null) {
                    mLightingClient.setIsLightOn(l.getId(), isOn);
                } else {
                    Toast.makeText(getContext(), "What you say?", Toast.LENGTH_LONG).show();
                }
            }
            bundle.clear();
        }
        mAdapter.notifyDataSetChanged();
    }

    private class LightListAdapter extends BaseAdapter {

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return !mLightingClient.getLightForIndex(position).isOffline();
        }

        @Override
        public int getCount() {
            return mLightingClient.getNumLights();
        }

        @Override
        public Object getItem(int position) {
            return mLightingClient.getLightForIndex(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.light_list_item, parent, false);
            }

            TcpConnectedLightingClient.Light light = mLightingClient.getLightForIndex(position);
            Switch lightSwitch = (Switch) convertView.findViewById(R.id.lightSwitch);
            TextView textView = (TextView) convertView.findViewById(R.id.textView);
            SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.seekBar);

            textView.setText(light.getName());
            seekBar.setProgress(light.getBrightness());
            lightSwitch.setChecked(light.isOn());
            seekBar.setEnabled(!light.isOffline());
            lightSwitch.setEnabled(!light.isOffline());

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mLightingClient.setLightBrightness(mLightingClient.getLightForIndex(position).getId(), seekBar.getProgress());
                }
            });

            lightSwitch.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLightingClient.setIsLightOn(mLightingClient.getLightForIndex(position).getId(), ((Switch) v).isChecked());
                }
            });

            return convertView;
        }

        @Override
        public boolean isEmpty() {
            return mLightingClient.getNumLights() == 0;
        }
    }
}
