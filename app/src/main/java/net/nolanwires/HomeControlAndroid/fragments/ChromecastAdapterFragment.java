package net.nolanwires.HomeControlAndroid.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.nolanwires.HomeControlAndroid.DeviceDetailActivity;
import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.ChromecastClient;

/**
 * Created by nolan on 2/21/16.
 */
public class ChromecastAdapterFragment extends DeviceAdapterFragment implements View.OnClickListener, Handler.Callback {
    private static final String ADAPTER_NAME = "Chromecast";

    private ChromecastClient mChromecastClient;
    private ActionBar mActionBar;
    private TextView mStatusTextView;
    private TextView mMediaStatusTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = ((DeviceDetailActivity) getActivity()).getSupportActionBar();

        mChromecastClient = new ChromecastClient(new Handler(this));

        if (mActionBar != null) {
            mActionBar.setTitle(mChromecastClient.getDeviceName());
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
                mMediaStatusTextView.setText((String)msg.obj);
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
    public String toString() {
        return ADAPTER_NAME;
    }

    @Override
    public String getDetails() {
        if (mChromecastClient != null)
            return mChromecastClient.getDeviceName();
        else
            return "Chromecast";
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mChromecastClient.quit();
    }

}
