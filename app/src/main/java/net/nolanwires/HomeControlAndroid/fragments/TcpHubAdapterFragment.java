package net.nolanwires.HomeControlAndroid.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import net.nolanwires.HomeControlAndroid.DeviceDetailActivity;
import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.TcpConnectedLightingClient;

/**
 * Created by nolan on 2/18/16.
 */
public class TcpHubAdapterFragment extends DeviceAdapterFragment implements TcpConnectedLightingClient.OnLightStatusUpdateListener, Runnable {

    private static final String ADAPTER_NAME = "Lighting";
    private static final String ADAPTER_DETAILS = "TCP Connected lighting";
    private static final int POLL_DELAY_MS = 2000;
    private static final int POLL_WHAT = 1337;

    private TcpConnectedLightingClient mLightingClient;
    private LightListAdapter mAdapter;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = ((DeviceDetailActivity)getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(ADAPTER_DETAILS);
        }

        mHandler = new Handler();
        mLightingClient = new TcpConnectedLightingClient(getContext(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ListView lv = new ListView(getContext());

        mAdapter = new LightListAdapter();
        lv.setAdapter(mAdapter);

        mLightingClient.getLights();
        run();

        return lv;
    }

    @Override
    public void run() {
        mLightingClient.getLights();
        mHandler.postDelayed(this, POLL_DELAY_MS);
    }

    @Override
    public String toString() {
        return ADAPTER_NAME;
    }

    @Override
    public String getDetails() {
        return ADAPTER_DETAILS;
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public void OnLightStatusUpdate() {
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
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mLightingClient.setLightBrightness(mLightingClient.getLightForIndex(position).getId(), seekBar.getProgress());
                }
            });

            lightSwitch.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLightingClient.setIsLightOn(mLightingClient.getLightForIndex(position).getId(), ((Switch)v).isChecked());
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
