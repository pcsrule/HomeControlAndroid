package net.nolanwires.HomeControlAndroid.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import net.nolanwires.HomeControlAndroid.R;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by nolan on 2/17/16.
 * Allows communication with CoAP based devices
 */
public class CoapDeviceAdapterFragment extends DeviceAdapterFragment {

    private static final String ADAPTER_NAME = "LED Strip";
    private static final String ADAPTER_DETAILS = "LPD6803 LED Strip";
    private static final String URI = "coap://10.11.13.152";
    private Set<WebLink> links;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = this.getActivity();
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.detail_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(ADAPTER_DETAILS);
        }

        discoverControls();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        GridLayout rootView = new GridLayout(getContext());
        rootView.setColumnCount(3);
        rootView.setRowCount(1);

        for (WebLink link : links) {
            Log.d("coap", link.toString());
            if (Integer.parseInt(link.getAttributes().getContentTypes().get(0)) == MediaTypeRegistry.APPLICATION_JSON) {
                final CoapClient client = new CoapClient(URI + link.getURI());
                try {
                    Log.d("coap", link.getURI());
                    JSONObject obj = new JSONObject(client.get().getResponseText());

                    String name = obj.keys().next();

                    LinearLayout endpoint = (LinearLayout)inflater.inflate(R.layout.coap_device_detail, null, false);

                    TextView groupTitleTextView = (TextView)endpoint.findViewById(R.id.textView);
                    groupTitleTextView.setText(name);
                    //rootView.addView(groupTitleTextView);
                    RadioGroup radioGroup = (RadioGroup)endpoint.findViewById(R.id.radioGroup);
                    radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup group, int checkedId) {
                            RadioButton selected = (RadioButton) group.findViewById(checkedId);
                            int index = group.indexOfChild(selected);
                            client.put(String.valueOf(index), 0);
                        }
                    });

                    JSONArray array = obj.getJSONArray(name);
                    for (int i = 0; i < array.length(); ++i) {
                        String option = array.getJSONObject(i).keys().next();
                        RadioButton radioButton = new RadioButton(rootView.getContext());
                        radioButton.setText(option);
                        radioGroup.addView(radioButton, i);
                    }
                    rootView.addView(endpoint);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return rootView;
    }

    private void discoverControls() {
        CoapClient client = new CoapClient(URI);
        links = client.discover();
        ArrayList<String> pathList = null;

        if (links != null) {
            pathList = new ArrayList<>();
            for (WebLink link : links) {
                pathList.add(link.toString());
            }
        }
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
}
