package tfg.naviganto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SelectEndActivity extends Activity implements Const {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_end);
		
		Double lat = 39.40642;
    	Double lon = -3.11702477;
    	String transport = FASTEST;
    	
		Intent returnIntent = new Intent();
		returnIntent.putExtra("lat",lat);
		returnIntent.putExtra("lon",lon);
		returnIntent.putExtra("transport", transport);
		
		setResult(RESULT_OK,returnIntent);
		finish();
	}
	
	@Override
    public void onBackPressed() {
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);
		finish();
    }
}
