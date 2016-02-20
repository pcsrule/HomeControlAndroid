package net.nolanwires.HomeControlAndroid.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.deviceadapters.LgSmartTvClient;
import net.nolanwires.HomeControlAndroid.deviceadapters.LgSmartTvClient.LG_KEYCODES;

/**
 * Created by nolan on 2/19/16.
 */
public class LgSmartTvAdapterFragment extends DeviceAdapterFragment implements View.OnClickListener {
    private static final String ADAPTER_NAME = "Projector";
    private static final String ADAPTER_DETAILS = "LG PA75U Projector";

    private LgSmartTvClient mClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            appBarLayout.setTitle(ADAPTER_DETAILS);
        }

        mClient = new LgSmartTvClient(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lgsmarttv_detail_fragment, container, false);

        v.findViewById(R.id.offButton).setOnClickListener(this);
        v.findViewById(R.id.powerSaveButton).setOnClickListener(this);

        return v;
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
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.offButton:
                mClient.sendKeyCode(LG_KEYCODES.POWER);
                break;
            case R.id.powerSaveButton:
                mClient.sendChangePowerSave();
        }
    }
}
