package es.uclm.esi.tfg.naviganto;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class SettingsActivity extends PreferenceActivity implements Const {

    private CheckBoxPreference mVibrate;
    private TwoLinesListPreference mLeft;
    private TwoLinesListPreference mRight;

    private BluetoothAdapter mBtAdapter;
    private GoogleApiClient mApiClient = null;

    private CharSequence[] mEntries;
    private CharSequence[] mEntriesSubtitles;
    private CharSequence[] mValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, R.string.alert_no_bluetooth, Toast.LENGTH_LONG).show();
            mVibrate.setEnabled(false);
        }
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();
        if (mApiClient != null) mApiClient.connect();


        mVibrate = (CheckBoxPreference)findPreference("settingsAlertsVigrate");
        mLeft = (TwoLinesListPreference)findPreference("settingsAlertsVigrateLeft");
        mRight = (TwoLinesListPreference)findPreference("settingsAlertsVigrateRight");

        mVibrate.setChecked(false);
        mLeft.setEnabled(false);
        mRight.setEnabled(false);

        mVibrate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue){
                boolean isEnabled = ((Boolean) newValue).booleanValue();
                mLeft.setEnabled(isEnabled);
                mRight.setEnabled(isEnabled);
                return true;
            }
        });

        initListPreferenceValues();
        if (isPlayServicesAvailable()) addWearListPreferenceValues();
    }

    private void initListPreferenceValues() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        int i = pairedDevices.size() +  1;

        mEntries = new CharSequence[i];
        mEntriesSubtitles = new CharSequence[i];
        mValues = new CharSequence[i];

        i = 0;

        mEntries[i] = getString(R.string.settings_this_device);
        mEntriesSubtitles[i] = getString(R.string.settings_local);
        mValues[i] = LOCAL + SPLIT + "this";

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                i++;
                mEntries[i] = device.getName();
                mEntriesSubtitles[i] = getString(R.string.settings_bluetooth);
                mValues[i] = BLUETOOTH + SPLIT + device.getAddress();
            }
        } else {
            if (mBtAdapter.isEnabled())
               Toast.makeText(getApplicationContext(), R.string.alert_no_devices, Toast.LENGTH_SHORT).show();
            mVibrate.setEnabled(false);
        }

        putListPreferenceValues(mEntries, mEntriesSubtitles, mValues);
    }

    public void putListPreferenceValues(final CharSequence[] e, CharSequence[] es, final CharSequence[] v){
        mLeft.setEntries(e);
        mRight.setEntries(e);

        mLeft.setEntryValues(v);
        mRight.setEntryValues(v);

        mLeft.setEntriesSubtitles(es);
        mRight.setEntriesSubtitles(es);

        mLeft.setValue(LOCAL + SPLIT + "this");
        mRight.setValue(LOCAL + SPLIT + "this");

        mLeft.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mLeft.setSummary(getEntryByValue(newValue.toString(), e, v));
                return true;
            }
        });
        mRight.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mRight.setSummary(getEntryByValue(newValue.toString(), e, v));
                return true;
            }
        });
    }

    private void addWearListPreferenceValues(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult wearNodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();

                int i = mEntries.length + wearNodes.getNodes().size();

                final CharSequence[] entries = new CharSequence[i];
                final CharSequence[] entriesSubtitles = new CharSequence[i];
                final CharSequence[] values  = new CharSequence[i];

                for (int j=0; j<mEntries.length; j++) {
                    entries[j] = mEntries[j];
                    entriesSubtitles[j] = mEntriesSubtitles[j];
                    values[j] = mValues[j];
                }

                i = mEntries.length - 1;
                if (wearNodes.getNodes().size() > 0) {
                    for (Node node : wearNodes.getNodes()) {
                        i++;
                        entries[i] = node.getDisplayName();
                        entriesSubtitles[i] = "Wear";
                        values[i] = WEAR + SPLIT + node.getId();
                    }
                }

                putListPreferenceValues(entries, entriesSubtitles, values);
            }
}       ).start();
    }

    public boolean isPlayServicesAvailable() {
        int val = GooglePlayServicesUtil.isGooglePlayServicesAvailable(SettingsActivity.this);
        if (val == ConnectionResult.SUCCESS) return true;
        return false;
    }

    private CharSequence getEntryByValue(String value, CharSequence[] entries, CharSequence[] values){
        for (int i=0; i < values.length; i++) {
            if (value.equals(values[i])) {
                return entries[i];
            }
        }
        return entries[0];
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if (!mBtAdapter.isEnabled()) {
            Toast.makeText(this, R.string.alert_disabled_bluetooth, Toast.LENGTH_SHORT).show();
            mVibrate.setEnabled(false);
            mVibrate.setChecked(false);
            mLeft.setEnabled(false);
            mRight.setEnabled(false);
        } else {
            mVibrate.setEnabled(true);
        }
    }
}
