package net.nolanwires.HomeControlAndroid.fragments;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.nolanwires.HomeControlAndroid.DeviceDetailActivity;
import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.ChromecastClient;
import net.nolanwires.HomeControlAndroid.deviceadapters.LgSmartTvClient;
import net.nolanwires.HomeControlAndroid.deviceadapters.LgSmartTvClient.LG_KEYCODES;

/**
 * Created by nolan on 2/19/16.
 */
public class LgSmartTvAdapterFragment extends DeviceAdapterFragment implements View.OnClickListener {
    private static final String ADAPTER_NAME = "Projector";
    private static final String ADAPTER_DETAILS = "LG PA75U Projector";

    private LgSmartTvClient mClient;

    // Add this class to the list of devices and the voice command keyword list.
    static void init() {
        ADAPTERS.add(LgSmartTvAdapterFragment.class);
        ADAPTER_NAMES.put(LgSmartTvAdapterFragment.class, ADAPTER_NAME);
        ADAPTER_KEYWORDS.put(LgSmartTvAdapterFragment.class, new String[]{"projector"});
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(ADAPTER_DETAILS);
        }

        mClient = new LgSmartTvClient(getContext());

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String command = bundle.getString(DeviceDetailActivity.ARG_COMMAND);

            if (command != null) {
                if (command.contains("off"))
                    mClient.sendKeyCode(LG_KEYCODES.POWER);
                else if (command.contains("brightness"))
                    mClient.sendChangePowerSave();
                else if (command.contains("input"))
                    mClient.sendKeyCode(LG_KEYCODES.INPUT);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lgsmarttv_detail_fragment, container, false);

        v.findViewById(R.id.offButton).setOnClickListener(this);
        v.findViewById(R.id.powerSaveButton).setOnClickListener(this);
        v.findViewById(R.id.inputButton).setOnClickListener(this);
        v.findViewById(R.id.upButton).setOnClickListener(this);
        v.findViewById(R.id.downButton).setOnClickListener(this);
        v.findViewById(R.id.leftButton).setOnClickListener(this);
        v.findViewById(R.id.rightButton).setOnClickListener(this);
        v.findViewById(R.id.okButton).setOnClickListener(this);
        v.findViewById(R.id.backButton).setOnClickListener(this);
        v.findViewById(R.id.menuButton).setOnClickListener(this);

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.offButton:
                mClient.sendKeyCode(LG_KEYCODES.POWER);
                break;
            case R.id.inputButton:
                mClient.sendKeyCode(LG_KEYCODES.INPUT);
                break;
            case R.id.powerSaveButton:
                mClient.sendChangePowerSave();
                break;
            case R.id.upButton:
                mClient.sendKeyCode(LG_KEYCODES.UP);
                break;
            case R.id.downButton:
                mClient.sendKeyCode(LG_KEYCODES.DOWN);
                break;
            case R.id.leftButton:
                mClient.sendKeyCode(LG_KEYCODES.LEFT);
                break;
            case R.id.rightButton:
                mClient.sendKeyCode(LG_KEYCODES.RIGHT);
                break;
            case R.id.okButton:
                mClient.sendKeyCode(LG_KEYCODES.OK);
                break;
            case R.id.backButton:
                mClient.sendKeyCode(LG_KEYCODES.BACK);
                break;
            case R.id.menuButton:
                mClient.sendKeyCode(LG_KEYCODES.MENU);
                break;
        }
    }
}
