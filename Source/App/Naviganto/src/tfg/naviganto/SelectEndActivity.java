package tfg.naviganto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class SelectEndActivity extends Activity implements Const {

	private Spinner transport;
	private EditText search_text;
	private Button search_button;
	private ListView searches;
	private ProgressBar progressBar;
	
	private List<Address> foundAdresses;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_end);
		
		search_text = (EditText)findViewById(R.id.search_form);
		searches = (ListView)findViewById(R.id.list_results);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		
		transport = (Spinner)findViewById(R.id.transport);
		List<Map<String, String>> data = getTransportList();
		SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_1,
                new String[] { "title" },
                new int[] { android.R.id.text1 } );
		transport.setAdapter(adapter);

		search_button = (Button)findViewById(R.id.search_button);
		search_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new GetAddresses().execute();
            }
        });
		
		searches.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("lat",foundAdresses.get(position).getLatitude());
				returnIntent.putExtra("lon",foundAdresses.get(position).getLongitude());
				returnIntent.putExtra("transport", getSelectedTransport());
				
				setResult(RESULT_OK,returnIntent);
				finish();
			}
		});
	}
	
	private List<Map<String, String>> getTransportList() {
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		data.add(getItemByStringId(R.string.transport_fastest));
		data.add(getItemByStringId(R.string.transport_shortest));
		data.add(getItemByStringId(R.string.transport_bicycle));
		data.add(getItemByStringId(R.string.transport_pedestrian));
		return data;
	}
	private Map<String, String> getItemByStringId(int transport) {
		Map<String, String> item = new HashMap<String, String>(2);
		item.put("title", getString(transport));
		return item;
	}
	private String getSelectedTransport() {
		int select = transport.getSelectedItemPosition();
		switch (select) {
		case 0:
			return FASTEST;
		case 1:
			return SHORTEST;
		case 2:
			return BICYCLE;
		case 3:
			return PEDESTRIAN;
		}
		return "";
	}
	
	
	private class GetAddresses extends AsyncTask<Void, Float, Boolean> {
		@Override
		protected void onPreExecute() {
			search_button.setEnabled(false);
			searches.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			
			//Hide keyboard
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(search_text.getWindowToken(), 0);
		}
		@Override
		protected Boolean doInBackground(Void... params) {
			GeocoderNominatim geocoder = new GeocoderNominatim(getApplicationContext());
			String text = search_text.getText().toString();
			
			if (text.equals("")) {
				return false;
			}
			
			try {
				foundAdresses = geocoder.getFromLocationName(text, 20);
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		protected void onPostExecute(Boolean result) {
			if (result) getAddressesPost();
			search_button.setEnabled(true);
			progressBar.setVisibility(View.GONE);
			searches.setVisibility(View.VISIBLE);
        }
	}
	private void getAddressesPost() {
		if (foundAdresses.size() == 0) {
			Toast.makeText(this, R.string.alert_search_empy, Toast.LENGTH_SHORT).show();
		} else {
			List<Map<String, String>> data = new ArrayList<Map<String, String>>();
			for (int i=0; i<foundAdresses.size(); i++) {
				String[] info = foundAdresses.get(i).getExtras().getString("display_name").split(", ");
				
				String subtitle = "";
				for (int j=1; j<info.length; j++) {
					if (!isNumber(info[j])) {
						if (j!=1) subtitle = subtitle + ", ";
						subtitle = subtitle + info[j];
					}
				}
				
				Map<String, String> item = new HashMap<String, String>(2);
				item.put("title", info[0]);
				item.put("subtitle", subtitle);
			    data.add(item);
			}
			
			SimpleAdapter adapter = new SimpleAdapter(this, data,
                    android.R.layout.simple_list_item_2,
                    new String[] { "title", "subtitle" },
                    new int[] { android.R.id.text1, android.R.id.text2 } );
			searches.setAdapter(adapter);
		}
	}
	private static boolean isNumber(String string) {
	    if (string == null || string == "") {
	        return false;
	    }
	    for (int i = 0; i < string.length(); i++) {
	        if (!Character.isDigit(string.charAt(i))) {
	            return false;
	        }
	    }
	    return true;
	}
	
	
	private boolean isOnline() {
    	ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();

    	if (netInfo != null && netInfo.isConnectedOrConnecting()) {
    		return true;
    	}

    	return false;
    }
	
	
	@Override
    public void onBackPressed() {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);
		finish();
    }
	
    @Override
    public void onResume() {
        super.onResume();
        
        if (isOnline()) {
        	search_button.setEnabled(true);
        } else {
			Toast.makeText(getBaseContext(), R.string.alert_no_internet,Toast.LENGTH_SHORT).show();
			search_button.setEnabled(false);
		}
    }
}
