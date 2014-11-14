package tfg.naviganto;

import java.util.ArrayList;
import java.util.Locale;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.DrawerLayout;

public class MainActivity 
		extends ActionBarActivity
		implements Const, NavigationDrawerFragment.NavigationDrawerCallbacks, 
			LocationListener, SensorEventListener, TextToSpeech.OnInitListener {
	
	private NavigationDrawerFragment navigationDrawerFragment;
	private View container;
	private View loading;
    private View navPanel;
    private ImageView navImage;
    private TextView navText;
    private TextView navKm;
    private MapView map;
    
    private TextToSpeech textToSpeech;
    private SharedPreferences lastLocation;
    private SharedPreferences settings;
    private Boolean mapOrientation;
    private Boolean alertSounds;
    
    protected LocationManager locationManager;  
    protected LocationOverlay locationOverlay;
    private SensorManager sensorManager;
    private Sensor orientation;
    
    private Boolean navMode = false;
    private Boolean newAction = true;
    private int step = 0;
    private int roadLost = 0;
    private float metersToGoal = Float.MAX_VALUE;

    private Road road = null;
    private String transport;
    private Polyline roadOverlay = null;
    private FolderOverlay roadMarkers = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
        container = (View)findViewById(R.id.container);
        loading = (View)findViewById(R.id.loading);
        
        map = (MapView)findViewById(R.id.map);
		map.setMultiTouchControls(true);
		map.getController().setZoom(18);
        
		navPanel = (View)findViewById(R.id.navPanel);
		navPanel.setVisibility(View.GONE);
		navImage = (ImageView) findViewById(R.id.navPanelImage);
		navText = (TextView) findViewById(R.id.navPanelText);
		navKm = (TextView) findViewById(R.id.navPanelKm);
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		
        lastLocation = getSharedPreferences("LastLocation",Context.MODE_PRIVATE);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        mapOrientation = settings.getBoolean("settingsAlertsSound", false);
        alertSounds = settings.getBoolean("settingsDisplayOrientation", false);
        
        textToSpeech = new TextToSpeech(this, this);
        
        locationOverlay = new LocationOverlay(this);
		map.getOverlays().add(locationOverlay);
        
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			onLocationChanged(location);
		}
		
		setTitle(R.string.navigation_drawer_explore);
    }
    
    
    private void cleanMap() {
    	step=0;
		metersToGoal = Float.MAX_VALUE;
    	roadLost = 0;
    	road = null;
    	if (roadOverlay != null) {
    		map.getOverlays().remove(roadOverlay);
    		roadOverlay = null;
    	}
    	if (roadMarkers != null) {
    		map.getOverlays().remove(roadMarkers);
    		roadMarkers = null;
    	}
    	navPanel.setVisibility(View.GONE);
    	map.invalidate();
    }
    private void centerMap(final GeoPoint loc) {
    	map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	map.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		    	map.getController().setCenter(loc);
		    }
		});
    }
    private GeoPoint getLastLocation() {
    	if (!locationOverlay.isEnabled() || locationOverlay.getLocation() == null) {
    		Double lat = Double.parseDouble(lastLocation.getString("latitude",  "39.40540171"));
        	Double lon = Double.parseDouble(lastLocation.getString("longitude", "-3.12204771"));
        	return new GeoPoint(lat, lon);
    	} else {
    		return locationOverlay.getLocation();
    	}
    }
    
    
    private float getDistance(GeoPoint p1, GeoPoint p2) {
        double lat1 = ((double)p1.getLatitudeE6()) / 1e6;
        double lng1 = ((double)p1.getLongitudeE6()) / 1e6;
        double lat2 = ((double)p2.getLatitudeE6()) / 1e6;
        double lng2 = ((double)p2.getLongitudeE6()) / 1e6;
        float [] dist = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, dist);
        return dist[0];
    }
    private float kmhToMs(int k){
    	return (float)(k/3.6);
    }
    
    private boolean isOnline() {
    	ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();

    	if (netInfo != null && netInfo.isConnectedOrConnecting()) {
    		return true;
    	}

    	return false;
    }
    
    private void setPreferences(){
    	//Last location
    	if (locationOverlay.getLocation() != null) {
    		SharedPreferences.Editor editor = lastLocation.edit();
    		editor.putString("latitude",  String.valueOf(locationOverlay.getLocation().getLatitude()) );
    		editor.putString("longitude", String.valueOf(locationOverlay.getLocation().getLongitude()) );
    		editor.commit();
    	}
    }
    
    
    @SuppressWarnings("unchecked")
	private void gotoGeoPoint(GeoPoint endPoint) {
		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
		waypoints.add(getLastLocation());
		waypoints.add(endPoint);
		
		if (isOnline()) {
			new GetRoad().execute(waypoints);
		} else {
			Toast.makeText(getBaseContext(), R.string.alert_no_internet,Toast.LENGTH_SHORT).show();
		}
    }
    private class GetRoad extends AsyncTask<ArrayList<GeoPoint>, Float, Boolean>{
		@Override
		protected void onPreExecute() {
			container.setVisibility(View.GONE);
			loading.setVisibility(View.VISIBLE);
			navigationDrawerFragment.setMenuVisibility(false);
		}
		@Override
		protected Boolean doInBackground(ArrayList<GeoPoint>... params) {
			RoadManager roadManager = new MapQuestRoadManager(MAPQUESTAPIKEY);
			roadManager.addRequestOption("units=k");
			
			if (transport != null && transport != "") {
				roadManager.addRequestOption("routeType=" + transport);
			}
			
			road = roadManager.getRoad(params[0]);
			
			if (road == null) {
				Toast.makeText(getBaseContext(),
						R.string.alert_route_internet,
						Toast.LENGTH_SHORT).show();
				return false;
			} else if (road.mStatus != Road.STATUS_OK) {
				Toast.makeText(getBaseContext(),
						R.string.alert_route_status+road.mStatus,
						Toast.LENGTH_SHORT).show();
				return false;
			}
			
			return true;
		}
		protected void onPostExecute(Boolean result) {
			if (result) gotoGeoPointPosThread();
			container.setVisibility(View.VISIBLE);
			loading.setVisibility(View.GONE);
			navigationDrawerFragment.setMenuVisibility(true);
        }
	}
    private void gotoGeoPointPosThread() {
    	roadOverlay = RoadManager.buildRoadOverlay(road, getBaseContext());
		roadOverlay.setWidth(10);
		map.getOverlays().add(roadOverlay);
		
		roadMarkers = new FolderOverlay(this);
		addMarkerToRoadMarkers(road.mNodes.get(0).mLocation);
		addMarkerToRoadMarkers(road.mNodes.get(road.mNodes.size()-1).mLocation);	
		map.getOverlays().add(roadMarkers);
						
		map.invalidate();
		
		navPanel.setVisibility(View.VISIBLE);
		
		if (road.mNodes.get(step).mManeuverType < 3) {
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
			step=1;
		}
    }
    private void addMarkerToRoadMarkers(GeoPoint point) {
    	Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
    	Marker marker = new Marker(map);
    	marker.setPosition(point);
    	marker.setIcon(nodeIcon);
		roadMarkers.add(marker);
    }
    
    
    private int getAction(int n) {
    	//http://open.mapquestapi.com/guidance/#maneuvertypes
    	switch (n) {
		case 3: case 4: case 5: case 9: case 13: case 15: case 17: case 20:
			return LEFT;
		case 6: case 7: case 8: case 10: case 14: case 16: case 18: case 21:
			return RIGHT;	
		case 12:
			return UTURN;
		case 27:
			return ROUNDABOUT1;
		case 28:
			return ROUNDABOUT2;
		case 29:
			return ROUNDABOUT3;
		case 30:
			return ROUNDABOUT4;
		case 31:
			return ROUNDABOUT5;
		case 32:
			return ROUNDABOUT6;
		case 33:
			return ROUNDABOUT7;
		case 34:
			return ROUNDABOUT8;
		case 24:  case 25: case 26:
			return DESTINATION;
		default:
	    	return STRAIGHT;
		}
    }
    private void startAction(int action, float distance) {    	
    	switch (action) {
		case STRAIGHT:
			//Fin de la vibración
			navText.setText(R.string.action_straight_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
			if (alertSounds) {
				if (distance > 100) {
					convertTextToSpeech(R.string.action_straight_sound_pre
							+ String.format("%.0f", distance) 
							+ " " + R.string.distance_meters
							+ R.string.action_straight_sound_pos);
				} else if (distance > 1000) {
					convertTextToSpeech(R.string.action_straight_sound_pre
							+ String.format("%.0f", distance/1000) 
							+ " " + R.string.distance_kilometers
							+ R.string.action_straight_sound_pos);
				}
			}
			break;
		case RIGHT:
			//Inicio vibracion
			navText.setText(R.string.action_right_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_right));
			if (alertSounds) {
				convertTextToSpeech(R.string.action_right_sound_pre 
						+ String.format("%.0f", distance) 
						+ R.string.action_right_sound_pos);
			}
			break;
		case LEFT:
			//Inicio vibracion
			navText.setText(R.string.action_left_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_left));
			if (alertSounds) {
				convertTextToSpeech(R.string.action_left_sound_pre
						+ String.format("%.0f", distance) 
						+ R.string.action_left_sound_pos);
			}
			break;
		case UTURN:
			//Inicio vibracion
			navText.setText(R.string.action_uturn_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
			if (alertSounds) {
				convertTextToSpeech(getString(R.string.action_uturn_sound));
			}
			break;
		case ROUNDABOUT1:
		case ROUNDABOUT2:
		case ROUNDABOUT3:
		case ROUNDABOUT4:
		case ROUNDABOUT5:
		case ROUNDABOUT6:
		case ROUNDABOUT7:
		case ROUNDABOUT8:
			//Inicio vibracion
			int exit = action-20; //roundabout=2X --> x=exit number
			navText.setText(R.string.action_roundabout_text + exit);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_roundabout));
			if (alertSounds) {
				convertTextToSpeech(R.string.action_roundabout_sound_pre 
						+ String.format("%.0f", distance) 
						+ R.string.action_roundabout_sound_pos
						+ exit);
			}
			break;
		case DESTINATION:
			//Inicio vibracion
			navText.setText(R.string.action_destination_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_arrived));
			if (alertSounds) {
				convertTextToSpeech(R.string.action_destination_sound_pre 
						+ String.format("%.0f", distance) 
						+ R.string.action_destination_sound_pos);
			}
			break;
		case WRONG:
			//Inicio vibracion
			navText.setText(R.string.action_wrong_text);
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
			if (alertSounds) {
				convertTextToSpeech(getString(R.string.action_wrong_sound));
			}
    		break;
		}
    }
    private void stopAction() {
    	//Fin de la vibración
    }
    
    
	private void convertTextToSpeech(String text) {
		if (!text.equals("") && textToSpeech != null) {
			textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}
    
	
	/* Override from LocationListener */
    @Override
    public void onLocationChanged(Location loc) {
      	GeoPoint myLocation = new GeoPoint(loc);
      	
      	//Change zoom according to speed
      	// and meters to first warn based on speed
      	float speed = loc.getSpeed();
      	int metersToFirstWarning;
      	
      	if (speed > kmhToMs(100)) {
      		map.getController().setZoom(15);
      		metersToFirstWarning = 1000;
      	} else if (speed > kmhToMs(80)) {
      		map.getController().setZoom(16);
      		metersToFirstWarning = 500;
      	} else if (speed > kmhToMs(50)) {
      		map.getController().setZoom(17);
      		metersToFirstWarning = 100;
      	} else {
      		map.getController().setZoom(18);
      		metersToFirstWarning = 30;
      	}
      	
      	//Update locationOverlay & map
       	if (!locationOverlay.isEnabled()) {
			locationOverlay.setEnabled(true);
			map.getController().animateTo(myLocation);
		}
		locationOverlay.setLocation(myLocation);
		locationOverlay.setAccuracy((int)loc.getAccuracy());
       	
		if (navMode) {
			map.getController().animateTo(myLocation);
		} else {
			map.invalidate();
		}
				
		//Navigation control
		if (road != null) {
			float distance = getDistance(myLocation, road.mNodes.get(step).mLocation);
			
			if (distance < metersToFirstWarning) {
				if (distance < metersToGoal) {
					if (newAction || metersToGoal == Float.MAX_VALUE) {
						newAction = false;
						startAction(getAction(road.mNodes.get(step).mManeuverType), distance);
					}
					metersToGoal = distance;
				} else {
					newAction = true;
					stopAction();
					metersToGoal = Float.MAX_VALUE;
					
					if (step == road.mNodes.size()-1) {
						cleanMap();
					} else {
						step++;
					}
				}
			} else {
				if (newAction) {
					newAction = false;
					startAction(STRAIGHT, distance);
				}
			}
			
			if (distance > 1000) {
				navKm.setText(String.format("%.1f", distance/1000) + "km");
			} else {
				navKm.setText(String.format("%.0f", distance) + "m");
			}
		}
		
		//Road lost control
		if (roadOverlay != null && !roadOverlay.isCloseTo(myLocation, 24, map)) {
			if (roadLost < 3) {
				roadLost++;
			} else {
				startAction(WRONG, 0);
				GeoPoint endPoint = road.mNodes.get(road.mNodes.size()-1).mLocation;
				cleanMap();
				gotoGeoPoint(endPoint);
			}
		}
    }
    @Override
    public void onProviderDisabled(String provider) {}
    @Override
    public void onProviderEnabled(String provider) {}
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}  

    
    /* Override from SensorEventListener */
    static float azimuthOrientation = 0.0f;
    @Override
    public void onSensorChanged(SensorEvent event) {
    	float azimuth = event.values[0];
		if (Math.abs(azimuth-azimuthOrientation)>2.0f){
			azimuthOrientation = azimuth;
		}
    	map.setMapOrientation(-azimuth);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    
    /* Override from TextToSpeech */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = textToSpeech.setLanguage(new Locale(getString(R.string.locale)));
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(getBaseContext(),
						R.string.alert_lang_not_supported,
						Toast.LENGTH_SHORT).show();
			} else {
				convertTextToSpeech("");
			}
		} 
	}
    
	
    /* Override from Activity */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      centerMap(getLastLocation());
    }
    @Override
    public void onPause() {
        super.onPause();
        //Stop Updates
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
        setPreferences();
    }
    @Override
    public void onResume() {
        super.onResume();
        //Start Location Updates
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
			locationOverlay.setEnabled(true);
		} else {
			Toast.makeText(getBaseContext(), R.string.alert_no_gps, Toast.LENGTH_SHORT).show();
			locationOverlay.setEnabled(false);
			
		}
        //Start Orientation Updates
        if (mapOrientation) {
        	sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        
        if (!isOnline()) {
			Toast.makeText(getBaseContext(), R.string.alert_no_internet,Toast.LENGTH_SHORT).show();
		}
        
        centerMap(getLastLocation());
    }
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	setPreferences();
    	if (textToSpeech != null) {
    		textToSpeech.shutdown();
    	}
    }
    @Override
    public void onBackPressed() {
    	//Exit dialog
    	if (navMode) {
    		AlertDialog.Builder d = new AlertDialog.Builder(this);  
            d.setMessage(R.string.alert_exit_on_nav);            
            d.setCancelable(false);  
            d.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {  
                public void onClick(DialogInterface d, int id) {  
                	finish();
                }  
            });  
            d.setNegativeButton(R.string.no, null);            
            d.show();
    	} else {
    		finish();
    	}
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case GO:
        	if(resultCode == RESULT_OK){
        		setTitle(R.string.navigation_drawer_going);
    			navMode = true;
    			cleanMap();
    			centerMap(getLastLocation());

    			transport = data.getStringExtra("transport");	
    			gotoGeoPoint( new GeoPoint( data.getDoubleExtra("lat", 39.40642),
    										data.getDoubleExtra("lon", -3.11702477) ) );
            }
            if (resultCode == RESULT_CANCELED) {
            	Toast.makeText(getBaseContext(),R.string.alert_no_destination,Toast.LENGTH_SHORT).show();
    			centerMap(getLastLocation());
            }
        case SETTINGS:
        	mapOrientation = settings.getBoolean("settingsDisplayOrientation", false);
        	if (!mapOrientation) map.setMapOrientation(0.0f);
            alertSounds = settings.getBoolean("settingsAlertsSound", false);
            break;
        }
 
    }
    
    
    /* Override from ActionBarActivity */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
    	Intent intent;
    	
    	switch (position) {

		case EXPLORE:
			setTitle(R.string.navigation_drawer_explore);
			navMode = false;
			cleanMap();
			centerMap(getLastLocation());
			break; 
			
		case GO:
			intent = new Intent(getBaseContext(), SelectEndActivity.class);
			startActivityForResult(intent, GO);
			break;
			
		case SETTINGS:
			intent = new Intent(getBaseContext(), SettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
			break;
			
		case ABOUT:
			intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("https://bitbucket.org/cr4mos/tfg-sgpcmii"));
			startActivity(intent);
			break;
		}
    }   
}
