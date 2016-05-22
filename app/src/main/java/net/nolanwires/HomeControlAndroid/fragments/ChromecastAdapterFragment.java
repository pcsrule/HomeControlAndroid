package net.nolanwires.HomeControlAndroid.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.nolanwires.HomeControlAndroid.DeviceDetailActivity;
import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.ChromecastClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nolan on 2/21/16.
 */
public class ChromecastAdapterFragment extends DeviceAdapterFragment implements View.OnClickListener, Handler.Callback {
    private static final String ADAPTER_NAME = "Chromecast";

    private ChromecastClient mChromecastClient;
    private ActionBar mActionBar;
    private TextView mStatusTextView;
    private TextView mMediaStatusTextView;

    // Add this class to the list of devices and the voice command keyword list.
    static void init() {
        ADAPTERS.add(ChromecastAdapterFragment.class);
        ADAPTER_NAMES.put(ChromecastAdapterFragment.class, ADAPTER_NAME);
        ADAPTER_KEYWORDS.put(ChromecastAdapterFragment.class, new String[]{"chromecast"});
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        mChromecastClient = new ChromecastClient(new Handler(this));

        if (mActionBar != null) {
            mActionBar.setTitle(mChromecastClient.getDeviceName());
        }

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String command = bundle.getString(DeviceDetailActivity.ARG_COMMAND);

            if (command != null) {
                if (command.contains("exit"))
                    mChromecastClient.sendAction(ChromecastClient.ACTION_STOPAPP);
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chromecast_detail_fragment, container, false);

        v.findViewById(R.id.exitAppButton).setOnClickListener(this);
        v.findViewById(R.id.volDownButton).setOnClickListener(this);
        v.findViewById(R.id.volUpButton).setOnClickListener(this);
        mStatusTextView = (TextView) v.findViewById(R.id.chromecastStatusTextView);
        mMediaStatusTextView = (TextView) v.findViewById(R.id.mediaStatusTextView);

        return v;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case ChromecastClient.MSG_NEWSTATUS:
                mStatusTextView.setText((String) msg.obj);
                return true;
            case ChromecastClient.MSG_NEWMEDIASTATUS:
                mMediaStatusTextView.setText((String) msg.obj);
                return true;
            case ChromecastClient.MSG_NEWNAME:
                mActionBar.setTitle((String) msg.obj);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exitAppButton:
                mChromecastClient.sendAction(ChromecastClient.ACTION_STOPAPP);
                break;
            case R.id.volDownButton:
                mChromecastClient.sendAction(ChromecastClient.ACTION_VOLUMEDOWN);
                break;
            case R.id.volUpButton:
                mChromecastClient.sendAction(ChromecastClient.ACTION_VOLUMEUP);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mChromecastClient.quit();
    }

}
