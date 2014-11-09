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
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.DrawerLayout;

public class MainActivity extends ActionBarActivity
	implements NavigationDrawerFragment.NavigationDrawerCallbacks, 
		LocationListener, SensorEventListener, TextToSpeech.OnInitListener {
	
	static final String MAPQUESTAPIKEY = "Fmjtd%7Cluurnu0anl%2C2s%3Do5-9wrw94";
	static final Locale locale = new Locale("es", "", "");
	
    private NavigationDrawerFragment navigationDrawerFragment;
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
    private Polyline roadOverlay = null;
    private FolderOverlay roadMarkers = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //TOFIX: Hay que hacer las peticiones en otro Thread
        //NetworkOnMainThreadException: https://code.google.com/p/osmbonuspack/issues/detail?id=9
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	StrictMode.setThreadPolicy(policy);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
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
        settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
        mapOrientation = settings.getBoolean("mapOrientation", false);
        alertSounds = settings.getBoolean("alertSounds", false);
        
        if (alertSounds) {
        	textToSpeech = new TextToSpeech(this, this);
        }
        
        locationOverlay = new LocationOverlay(this);
		map.getOverlays().add(locationOverlay);
        
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			onLocationChanged(location);
		}
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
    }
    private void centerMapLastLocation() {
    	map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	map.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		    	map.getController().setCenter(getLastLocation());
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
    private void setPreferences(){
    	//Last location
    	if (locationOverlay.getLocation() != null) {
    		SharedPreferences.Editor editor = lastLocation.edit();
    		editor.putString("latitude",  String.valueOf(locationOverlay.getLocation().getLatitude()) );
    		editor.putString("longitude", String.valueOf(locationOverlay.getLocation().getLongitude()) );
    		editor.commit();
    	}
    }
    private Boolean gotoGeoPoint(GeoPoint endPoint) {
		RoadManager roadManager = new MapQuestRoadManager(MAPQUESTAPIKEY);
		roadManager.addRequestOption("units=k");
		//roadManager.addRequestOption("routeType=fastest");
		//roadManager.addRequestOption("routeType=shortest");
		//roadManager.addRequestOption("routeType=bicycle");
		//roadManager.addRequestOption("routeType=pedestrian");
		//Y otras opciones http://open.mapquestapi.com/guidance/
		
		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
		waypoints.add(getLastLocation());
		waypoints.add(endPoint);
		
		road = roadManager.getRoad(waypoints);
		if (road == null) {
			Toast.makeText(getBaseContext(),
					"Error cargando la ruta. Sin conexión a internet",
					Toast.LENGTH_SHORT).show();
			return false;
		}else if (road.mStatus != Road.STATUS_OK) {
			Toast.makeText(getBaseContext(),
					"Error cargando la ruta. Estado="+road.mStatus,
					Toast.LENGTH_SHORT).show();
			return false;
		}
						
		roadOverlay = RoadManager.buildRoadOverlay(road, getBaseContext());
		roadOverlay.setWidth(10);
		map.getOverlays().add(roadOverlay);
		
		roadMarkers = new FolderOverlay(this);
		addMarkerToRoadMarkers(road.mNodes.get(0).mLocation);
		addMarkerToRoadMarkers(road.mNodes.get(road.mNodes.size()-1).mLocation);	
		map.getOverlays().add(roadMarkers);
						
		map.invalidate();
		
		if (road.mNodes.get(step).mManeuverType < 3) {
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
			step=1;
		}	
		
		return true;
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
    	//left=10 right=01 both=11 none=00
    	//roundabout=2X --> x=exit number 
    	switch (n) {
		case 3: case 4: case 5: case 9: case 13: case 15: case 17: case 20:
			return 10;
		case 6: case 7: case 8: case 10: case 14: case 16: case 18: case 21:
			return 01;	
		case 12:
			return 11;
		case 27:
			return 21;
		case 28:
			return 22;
		case 29:
			return 23;
		case 30:
			return 24;
		case 31:
			return 25;
		case 32:
			return 26;
		case 33:
			return 27;
		case 34:
			return 28;
		case 24:  case 25: case 26:
			return 30;
		default:
	    	return 00;
		}
    }
    private void startAction(int action, float distance) {    	
    	switch (action) {
		case 0:
			//Fin de la vibración
			navText.setText("Siga recto");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
			if (alertSounds) {
				if (distance > 100) {
					convertTextToSpeech("Siga recto " + String.format("%.0f", distance) + " metros");
				} else if (distance > 1000) {
					convertTextToSpeech("Siga recto " + String.format("%.0f", distance/1000) + " kilometros");
				}
			}
			break;
		case 1:
			//Inicio vibracion
			navText.setText("Gire a la derecha");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_right));
			if (alertSounds) {
				convertTextToSpeech("A " + String.format("%.0f", distance) + " metros, gire a la derecha");
			}
			break;
		case 10:
			//Inicio vibracion
			navText.setText("Gire a la izquierda");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_left));
			if (alertSounds) {
				convertTextToSpeech("A " + String.format("%.0f", distance) + " metros, gire a la izquierda");
			}
			break;
		case 11:
			//Inicio vibracion
			navText.setText("Dé media vuelta");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
			if (alertSounds) {
				convertTextToSpeech("Dé media vuelta");
			}
			break;
		case 21: case 22: case 23: case 24: case 25: case 26: case 27: case 28:
			//Inicio vibracion
			int exit = action-20;
			navText.setText("En la rotonda coja la " + exit + " salida");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_roundabout));
			break;
		case 30:
			//Inicio vibracion
			navText.setText("Ha llegado a su destino");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_arrived));
			if (alertSounds) {
				convertTextToSpeech("En " + String.format("%.0f", distance) + " metros, llegará a su destino");
			}
			break;
		case -1:
			//Inicio vibracion
			navText.setText("Recalculando el recorrido");
			navImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
			if (alertSounds) {
				convertTextToSpeech("Recalculando el recorrido");
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
					startAction(0, distance);
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
				startAction(-1, 0);
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
    	if (navMode) {
    		float azimuth = event.values[0];
			if (Math.abs(azimuth-azimuthOrientation)>2.0f){
				azimuthOrientation = azimuth;
			}
    		map.setMapOrientation(-azimuth);
		} else {
			map.setMapOrientation(0.0f);
		}
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    /* Override from TextToSpeech */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = textToSpeech.setLanguage(locale);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Toast.makeText(getBaseContext(),
						"Lenguaje no soportado",
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
      centerMapLastLocation();
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
			Toast.makeText(getBaseContext(), "GPS deshabilitado", Toast.LENGTH_SHORT).show();
			locationOverlay.setEnabled(false);
			
		}
        //Start Orientation Updates
        if (mapOrientation) {
        	sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        centerMapLastLocation();
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
            d.setMessage("¿Salir sin acabar la navegación?");            
            d.setCancelable(false);  
            d.setPositiveButton("Si", new DialogInterface.OnClickListener() {  
                public void onClick(DialogInterface d, int id) {  
                	finish();
                }  
            });  
            d.setNegativeButton("No", null);            
            d.show();
    	} else {
    		finish();
    	}
    }
    
    /* Override from ActionBarActivity */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
    	switch (position) {
    	//Explore
		case 0:
			cleanMap();
			centerMapLastLocation();
	    	navMode = false;
	    	navPanel.setVisibility(View.GONE);
			break;
			
		//Go
		case 1:
			cleanMap();
			centerMapLastLocation();
			navMode = true;
			navPanel.setVisibility(View.VISIBLE);
			gotoGeoPoint(new GeoPoint(39.40642, -3.11702477));
			break;
			
		//Settings
		case 2:
			//Intent i = new Intent(getBaseContext(), SettingsActivity.class);
			//startActivity(i);
			break;
			
		//About
		case 3:
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("https://bitbucket.org/cr4mos/tfg-sgpcmii"));
			startActivity(intent);
			break;
		}
    }   
}
