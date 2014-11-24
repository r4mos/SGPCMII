package tfg.naviganto;

import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;
 
public class SettingsActivity extends PreferenceActivity {
 
	private CheckBoxPreference mVibrate;
	private ListPreference mLeft;
	private ListPreference mRight;
	private BluetoothAdapter mBtAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, R.string.alert_no_bluetooth, Toast.LENGTH_LONG).show();
            mVibrate.setEnabled(false);
        }
        
        mVibrate = (CheckBoxPreference)findPreference("settingsAlertsVigrate");
        mLeft = (ListPreference)findPreference("settingsAlertsVigrateLeft");
        mRight = (ListPreference)findPreference("settingsAlertsVigrateRight");
        
        mVibrate.setChecked(false);
        mLeft.setEnabled(false);
        mRight.setEnabled(false);
        
        mLeft.setEnabled(mVibrate.isChecked());
        mRight.setEnabled(mVibrate.isChecked());
        
        mVibrate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue){
                boolean isEnabled = ((Boolean) newValue).booleanValue();
                mLeft.setEnabled(isEnabled);
                mRight.setEnabled(isEnabled);
                return true;
            }
        });
        
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        int i = pairedDevices.size() + 1;
        final CharSequence[] entries = new CharSequence[i];
        final CharSequence[] values  = new CharSequence[i];
        
        i = 0;
        entries[i] = getString(R.string.settings_this_device);
        values[i] = "";
        
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
            	i++;
            	entries[i] = device.getName();
                values[i] = device.getAddress();
            }
        } else {
        	Toast.makeText(getApplicationContext(), R.string.alert_no_devices, Toast.LENGTH_SHORT).show();
        	mVibrate.setEnabled(false);
        }
        
		mLeft.setEntries(entries);
		mRight.setEntries(entries);
		mLeft.setEntryValues(values);
		mRight.setEntryValues(values);
		mLeft.setValue("");
		mRight.setValue("");
		
		mLeft.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
            	mLeft.setSummary(getEntryByValue(newValue.toString(), entries, values));
                return true;
            }
        });
        mRight.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
            	mRight.setSummary(getEntryByValue(newValue.toString(), entries, values));
                return true;
            }
        });
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
