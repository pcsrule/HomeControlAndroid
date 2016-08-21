package net.nolanwires.HomeControlAndroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.actions.NoteIntents;

import net.nolanwires.HomeControlAndroid.R;
import net.nolanwires.HomeControlAndroid.fragments.DeviceAdapterFragment;

/**
 * An activity representing a single Device detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link DeviceListActivity}.
 */
public class DeviceDetailActivity extends AppCompatActivity {

    public static final String ARG_ITEM_ID = "device_adapter";
    public static final String ARG_COMMAND = "command";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        DeviceAdapterFragment adapterFragment = null;

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.compareTo(NoteIntents.ACTION_CREATE_NOTE) == 0) {

            String command = intent.getStringExtra(Intent.EXTRA_TEXT).toLowerCase();

            for (Class<? extends DeviceAdapterFragment> adapter : DeviceAdapterFragment.getAdapters()) {
                for (String s : DeviceAdapterFragment.getKeywords(adapter)) {
                    if (command.contains(s.toLowerCase())) {
                        adapterFragment = DeviceAdapterFragment.newInstance(adapter, command);
                    }
                }
            }

            /* no keyword match found */
            if (adapterFragment == null) {
                Toast.makeText(this, "I don't know how to do that", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

        } else {
            adapterFragment = DeviceAdapterFragment.constructFragmentForIndex(intent.getIntExtra(ARG_ITEM_ID, 0), null);
        }

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.device_detail_container, adapterFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.device_detail_container, adapterFragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, DeviceListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
