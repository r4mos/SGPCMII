package es.uclm.esi.tfg.naviganto;

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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.widget.DrawerLayout;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;


public class MainActivity
        extends ActionBarActivity
        implements Const, NavigationDrawerFragment.NavigationDrawerCallbacks,
        LocationListener, SensorEventListener, TextToSpeech.OnInitListener,
        GoogleApiClient.ConnectionCallbacks{

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private View mContainer;
    private View mLoading;
    private View mNavPanel;
    private ImageView mNavImage;
    private TextView mNavText;
    private TextView mNavKm;
    private MapView mMap;

    private TextToSpeech mTextToSpeech;
    private SharedPreferences mLastLocation;
    private SharedPreferences mSettings;
    private Boolean mMapOrientation;
    private Boolean mAlertSounds;
    private int mAlertVibrate = NONE;
    private Boolean mIsInVibration = false;
    private Vibrator mVibrator;

    protected LocationManager mLocationManager;
    protected LocationOverlay mLocationOverlay;
    private SensorManager mSensorManager;
    private Sensor mOrientation;

    private Boolean mNavMode = false;
    private Boolean mNewAction = true;
    private int mStep = 0;
    private int mRoadLost = 0;
    private float mMetersToGoal = Float.MAX_VALUE;

    private Road mRoad = null;
    private String mTransport;
    private Polyline mRoadOverlay = null;
    private FolderOverlay mRoadMarkers = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatServiceLeft = null;
    private BluetoothChatService mChatServiceRight = null;
    private StringBuffer mOutStringBufferLeft;
    private StringBuffer mOutStringBufferRight;

    private GoogleApiClient mApiClient = null;
    private String mWearLeft = "";
    private String mWearRight = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        mContainer = findViewById(R.id.container);
        mLoading = findViewById(R.id.loading);

        mMap = (MapView)findViewById(R.id.map);
        mMap.setMultiTouchControls(true);
        mMap.getController().setZoom(18);

        mNavPanel = findViewById(R.id.navPanel);
        mNavPanel.setVisibility(View.GONE);
        mNavImage = (ImageView) findViewById(R.id.navPanelImage);
        mNavText = (TextView) findViewById(R.id.navPanelText);
        mNavKm = (TextView) findViewById(R.id.navPanelKm);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        mLastLocation = getSharedPreferences("LastLocation",Context.MODE_PRIVATE);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mMapOrientation = mSettings.getBoolean("settingsDisplayOrientation", false);
        mAlertSounds = mSettings.getBoolean("settingsAlertsSound", false);

        mTextToSpeech = new TextToSpeech(this, this);

        mLocationOverlay = new LocationOverlay(this);
        mMap.getOverlays().add(mLocationOverlay);

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            onLocationChanged(location);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.alert_no_bluetooth, Toast.LENGTH_LONG).show();
        }

        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .build();
        if (mApiClient != null) mApiClient.connect();

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        setTitle(R.string.navigation_drawer_explore);
    }


    private void cleanMap() {
        mStep=0;
        mMetersToGoal = Float.MAX_VALUE;
        mRoadLost = 0;
        mRoad = null;
        if (mRoadOverlay != null) {
            mMap.getOverlays().remove(mRoadOverlay);
            mRoadOverlay = null;
        }
        if (mRoadMarkers != null) {
            mMap.getOverlays().remove(mRoadMarkers);
            mRoadMarkers = null;
        }
        mNavPanel.setVisibility(View.GONE);
        mMap.invalidate();
    }
    private void centerMap(final GeoPoint loc) {
        mMap.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mMap.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mMap.getController().setCenter(loc);
            }
        });
    }
    private GeoPoint getLastLocation() {
        if (!mLocationOverlay.isEnabled() || mLocationOverlay.getLocation() == null) {
            Double lat = Double.parseDouble(mLastLocation.getString("latitude",  "39.40540171"));
            Double lon = Double.parseDouble(mLastLocation.getString("longitude", "-3.12204771"));
            return new GeoPoint(lat, lon);
        } else {
            return mLocationOverlay.getLocation();
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

        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    private void setLocationPreferences(){
        if (mLocationOverlay.getLocation() != null) {
            SharedPreferences.Editor editor = mLastLocation.edit();
            editor.putString("latitude",
                    String.valueOf(mLocationOverlay.getLocation().getLatitude()) );
            editor.putString("longitude",
                    String.valueOf(mLocationOverlay.getLocation().getLongitude()) );
            editor.commit();
        }
    }


    @SuppressWarnings("unchecked")
    private void gotoGeoPoint(GeoPoint endPoint) {
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
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
            mContainer.setVisibility(View.GONE);
            mLoading.setVisibility(View.VISIBLE);
            mNavigationDrawerFragment.setMenuVisibility(false);
        }
        @Override
        protected Boolean doInBackground(ArrayList<GeoPoint>... params) {
            RoadManager roadManager = new MapQuestRoadManager(MAPQUESTAPIKEY);
            roadManager.addRequestOption("units=k");

            if (mTransport != null && !mTransport.equals("")) {
                roadManager.addRequestOption("routeType=" + mTransport);
            }

            mRoad = roadManager.getRoad(params[0]);

            if (mRoad == null) {
                Toast.makeText(getBaseContext(),
                        R.string.alert_route_internet,
                        Toast.LENGTH_SHORT).show();
                return false;
            } else if (mRoad.mStatus != Road.STATUS_OK) {
                Toast.makeText(getBaseContext(),
                        R.string.alert_route_status+mRoad.mStatus,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            return true;
        }
        protected void onPostExecute(Boolean result) {
            if (result) gotoGeoPointPosThread();
            mContainer.setVisibility(View.VISIBLE);
            mLoading.setVisibility(View.GONE);
            mNavigationDrawerFragment.setMenuVisibility(true);
        }
    }
    private void gotoGeoPointPosThread() {
        mRoadOverlay = RoadManager.buildRoadOverlay(mRoad, getBaseContext());
        mRoadOverlay.setWidth(10);
        mMap.getOverlays().add(mRoadOverlay);

        mRoadMarkers = new FolderOverlay(this);
        addMarkerToRoadMarkers(mRoad.mNodes.get(0).mLocation);
        addMarkerToRoadMarkers(mRoad.mNodes.get(mRoad.mNodes.size()-1).mLocation);
        mMap.getOverlays().add(mRoadMarkers);

        mMap.invalidate();

        mNavPanel.setVisibility(View.VISIBLE);

        if (mRoad.mNodes.get(mStep).mManeuverType < 3) {
            mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
            mStep=1;
        }
    }
    private void addMarkerToRoadMarkers(GeoPoint point) {
        Drawable nodeIcon = getResources().getDrawable(R.drawable.marker_node);
        Marker marker = new Marker(mMap);
        marker.setPosition(point);
        marker.setIcon(nodeIcon);
        mRoadMarkers.add(marker);
    }


    private int getAction(int n) {
        //API: http://open.mapquestapi.com/guidance/#maneuvertypes
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
                mNavText.setText(R.string.action_straight_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_continue));
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
                startVibrate(action);
                break;

            case RIGHT:
                mNavText.setText(R.string.action_right_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_right));
                convertTextToSpeech(R.string.action_right_sound_pre
                        + String.format("%.0f", distance)
                        + R.string.action_right_sound_pos);
                startVibrate(action);
                break;

            case LEFT:
                mNavText.setText(R.string.action_left_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_turn_left));
                convertTextToSpeech(R.string.action_left_sound_pre
                        + String.format("%.0f", distance)
                        + R.string.action_left_sound_pos);
                startVibrate(action);
                break;

            case UTURN:
                mNavText.setText(R.string.action_uturn_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
                convertTextToSpeech(getString(R.string.action_uturn_sound));
                startVibrate(action);
                break;

            case ROUNDABOUT1:
            case ROUNDABOUT2:
            case ROUNDABOUT3:
            case ROUNDABOUT4:
            case ROUNDABOUT5:
            case ROUNDABOUT6:
            case ROUNDABOUT7:
            case ROUNDABOUT8:
                int exit = action-20; //roundabout=2X --> x=exit number
                mNavText.setText(R.string.action_roundabout_text + exit);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_roundabout));
                convertTextToSpeech(R.string.action_roundabout_sound_pre
                        + String.format("%.0f", distance)
                        + R.string.action_roundabout_sound_pos + exit);
                startVibrate(action);
                break;

            case DESTINATION:
                mNavText.setText(R.string.action_destination_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_arrived));
                convertTextToSpeech(R.string.action_destination_sound_pre
                        + String.format("%.0f", distance)
                        + R.string.action_destination_sound_pos);
                startVibrate(action);
                break;

            case WRONG:
                mNavText.setText(R.string.action_wrong_text);
                mNavImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_u_turn));
                convertTextToSpeech(getString(R.string.action_wrong_sound));
                startVibrate(action);
                break;
        }
    }
    private void stopAction() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
        }
        stopVibrate();
    }

    private void startVibrate(int action) {
        stopVibrate();
        switch (mAlertVibrate) {
            case LLOCAL_RBLUETOOTH:
                if (isLeftAction(action)) startLocalVibration(action);
                if (isRightAction(action)) sendMessageBluetooth(START + action, RIGHT);
                break;

            case LBLUETOOTH_RLOCAL:
                if (isLeftAction(action)) sendMessageBluetooth(START + action, LEFT);
                if (isRightAction(action)) startLocalVibration(action);
                break;

            case LLOCAL_RWEAR:
                if (isLeftAction(action)) startLocalVibration(action);
                if (isRightAction(action)) sendMessageWear(START + action, RIGHT);
                break;

            case LWEAR_RLOCAL:
                if (isLeftAction(action)) sendMessageWear(START + action, LEFT);
                if (isRightAction(action)) startLocalVibration(action);
                break;

            case LBLUETOOTH_RWEAR:
                if (isLeftAction(action)) sendMessageBluetooth(START + action, LEFT);
                if (isRightAction(action)) sendMessageWear(START + action, RIGHT);
                break;

            case LWEAR_RBLUETOOTH:
                if (isLeftAction(action)) sendMessageWear(START + action, LEFT);
                if (isRightAction(action)) sendMessageBluetooth(START + action, RIGHT);
                break;

            case LBLUETOOTH_RBLUETOOTH:
                if (isLeftAction(action)) sendMessageBluetooth(START + action, LEFT);
                if (isRightAction(action)) sendMessageBluetooth(START + action, RIGHT);
                break;

            case LWEAR_RWEAR:
                if (isLeftAction(action)) sendMessageWear(START + action, LEFT);
                if (isRightAction(action)) sendMessageWear(START + action, RIGHT);
                break;

            default:
                break;
        }
    }
    private Boolean isLeftAction(int action) {
        return action != RIGHT;
    }
    private Boolean isRightAction(int action) {
        return action != LEFT;
    }
    private void startLocalVibration(int action) {
        if (!mIsInVibration) {
            mIsInVibration = true;

            switch (action) {
                case LEFT:
                case RIGHT:
                    long[] turn = {0, 900, 1000};
                    mVibrator.vibrate(turn, 0);
                    break;

                case WRONG:
                    mVibrator.vibrate(5000);
                    break;

                case UTURN:
                    long[] uturn = {0, 100};
                    mVibrator.vibrate(uturn, 0);
                    break;

                case DESTINATION:
                    long[] destination = new long[19];
                    for (int i=0; i<3; i++) {
                        destination[i*6+1] = 500;
                        destination[i*6+2] = 200;
                        destination[i*6+3] = 700;
                        destination[i*6+4] = 200;
                        destination[i*6+5] = 900;
                        destination[i*6+6] = 200;
                    }
                    mVibrator.vibrate(destination, -1);
                    break;

                case ROUNDABOUT1:
                case ROUNDABOUT2:
                case ROUNDABOUT3:
                case ROUNDABOUT4:
                case ROUNDABOUT5:
                case ROUNDABOUT6:
                case ROUNDABOUT7:
                case ROUNDABOUT8:
                    int exit = action-20; //roundabout=2X --> x=exit number
                    long[] roundabout = new long[exit*2+1];
                    for (int i=0; i<exit; i++) {
                        if (i != 0) roundabout[i*2] = 200;
                        roundabout[i*2+1] = 500;
                    }
                    roundabout[exit*2] = 1000;
                    mVibrator.vibrate(roundabout, 0);
                    break;
                case TEST:
                    mVibrator.vibrate(2000);
                    break;
            }
        } else {
            stopLocalVibration();
            startLocalVibration(action);
        }
    }
    private void stopLocalVibration() {
        if (mIsInVibration) {
            mVibrator.cancel();
            mIsInVibration = false;
        }
    }
    private void stopVibrate() {
        stopLocalVibration();
        switch (mAlertVibrate) {
            case LLOCAL_RBLUETOOTH:
                sendMessageBluetooth(STOP, RIGHT);
                break;

            case LBLUETOOTH_RLOCAL:
                sendMessageBluetooth(STOP, LEFT);
                break;

            case LLOCAL_RWEAR:
                sendMessageWear(STOP, RIGHT);
                break;

            case LWEAR_RLOCAL:
                sendMessageWear(STOP, LEFT);
                break;

            case LBLUETOOTH_RWEAR:
                sendMessageBluetooth(STOP, LEFT);
                sendMessageWear(STOP, RIGHT);
                break;

            case LWEAR_RBLUETOOTH:
                sendMessageWear(STOP, LEFT);
                sendMessageBluetooth(STOP, RIGHT);
                break;

            case LBLUETOOTH_RBLUETOOTH:
                sendMessageBluetooth(STOP, LEFT);
                sendMessageBluetooth(STOP, RIGHT);
                break;

            case LWEAR_RWEAR:
                sendMessageWear(STOP, LEFT);
                sendMessageWear(STOP, RIGHT);
                break;

            default:
                break;
        }
    }

    private void testVibrate() {
        if (mAlertVibrate > NONE) {
            Toast.makeText(getBaseContext(), R.string.alert_vibrate_left, Toast.LENGTH_SHORT).show();
            startVibrate(LEFT);
            Handler h1 = new Handler();
            h1.postDelayed(new Runnable() {
                public void run() {
                    stopVibrate();
                    Toast.makeText(getBaseContext(), R.string.alert_vibrate_right, Toast.LENGTH_SHORT).show();
                    startVibrate(RIGHT);
                }
            }, 1900);
            Handler h2 = new Handler();
            h2.postDelayed(new Runnable() {
                public void run() {
                    stopVibrate();
                }
            }, 3800);
        } else {
            Toast.makeText(getBaseContext(), R.string.alert_vibrate_disable, Toast.LENGTH_SHORT).show();
        }
    }

    /* Speech Text */
    private void convertTextToSpeech(String text) {
        if (mAlertSounds && !text.equals("") && mTextToSpeech != null) {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    /* Bluetooth */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothChatService.MESSAGE_STATE_CHANGE) {
                switch (msg.arg1) {
                    case BluetoothChatService.STATE_CONNECTED:
                        mContainer.setVisibility(View.VISIBLE);
                        mLoading.setVisibility(View.GONE);
                        break;
                    case BluetoothChatService.STATE_DISCONNECTING:
                        Toast.makeText(getApplicationContext(),
                                R.string.alert_device_disconnected,
                                Toast.LENGTH_SHORT).show();
                        disconectDevices();
                        break;
                    case BluetoothChatService.STATE_CONNECTION_FAILED:
                        disconectDevices();
                        showMap();
                        Toast.makeText(getApplicationContext(),
                                R.string.alert_no_device_connection,
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };
    private void sendMessageBluetooth(String message, int who) {
        if (who == LEFT) {
            sendMessageBluetooth(message, mChatServiceLeft, mOutStringBufferLeft);
        }else if (who == RIGHT) {
            sendMessageBluetooth(message, mChatServiceRight, mOutStringBufferRight);
        }
    }
    private void sendMessageBluetooth(String message, BluetoothChatService d, StringBuffer b) {
        if (d == null || d.getState() != BluetoothChatService.STATE_CONNECTED) {
            return;
        }

        message = message + SPLIT;
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            d.write(send);
            b.setLength(0);
        }
    }
    private void disconectDevices(){
        mAlertVibrate = NONE;

        if (mChatServiceLeft != null) {
            mChatServiceLeft.stop();
            mChatServiceLeft.start();
        }
        if (mChatServiceRight != null) {
            mChatServiceRight.stop();
            mChatServiceRight.start();
        }

        if (!mWearLeft.equals("")) {
            sendMessageWear(END_ACTIVITY, "", mWearLeft);
            mWearLeft = "";
        }
        if (!mWearRight.equals("")) {
            sendMessageWear(END_ACTIVITY, "", mWearRight);
            mWearRight = "";
        }
    }
    private void showMap () {
        mContainer.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.GONE);
    }
    private void showBluetoothConnectingDialog () {
        mContainer.setVisibility(View.GONE);
        mLoading.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(),
                R.string.alert_connecting,Toast.LENGTH_SHORT).show();
    }

    private void sendMessageWear(String text, int who) {
        if (who == LEFT) {
            sendMessageWear(WEAR_MESSAGE_PATH, text, mWearLeft);
        }else if (who == RIGHT) {
            sendMessageWear(WEAR_MESSAGE_PATH, text, mWearRight);
        }
    }
    private void sendMessageWear( final String path, final String text, final String nodeId) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                Wearable.MessageApi.sendMessage(
                            mApiClient, nodeId, path, text.getBytes() ).await();
            }
        }).start();
    }
    @Override
    public void onConnected(Bundle bundle) {}
    @Override
    public void onConnectionSuspended(int i) {}

    /* Override from LocationListener */
    @Override
    public void onLocationChanged(Location loc) {
        GeoPoint myLocation = new GeoPoint(loc);

        //Change zoom according to speed
        // and meters to first warn based on speed
        float speed = loc.getSpeed();
        int metersToFirstWarning;
        if (speed > kmhToMs(100)) {
            mMap.getController().setZoom(15);
            metersToFirstWarning = 1000;
        } else if (speed > kmhToMs(80)) {
            mMap.getController().setZoom(16);
            metersToFirstWarning = 500;
        } else if (speed > kmhToMs(50)) {
            mMap.getController().setZoom(17);
            metersToFirstWarning = 100;
        } else if (speed > kmhToMs(20)) {
            mMap.getController().setZoom(18);
            metersToFirstWarning = 60;
        } else {
            mMap.getController().setZoom(18);
            metersToFirstWarning = 30;
        }

        //Update locationOverlay & map
        if (!mLocationOverlay.isEnabled()) {
            mLocationOverlay.setEnabled(true);
            mMap.getController().animateTo(myLocation);
        }
        mLocationOverlay.setLocation(myLocation);
        mLocationOverlay.setAccuracy((int)loc.getAccuracy());

        if (mNavMode) {
            mMap.getController().animateTo(myLocation);
        } else {
            mMap.invalidate();
        }

        //Navigation control
        if (mRoad != null) {
            float distance = getDistance(myLocation, mRoad.mNodes.get(mStep).mLocation);

            if (distance < metersToFirstWarning) {
                if (distance < mMetersToGoal) {
                    if (mNewAction || mMetersToGoal == Float.MAX_VALUE) {
                        mNewAction = false;
                        startAction(getAction(mRoad.mNodes.get(mStep).mManeuverType), distance);
                    }
                    mMetersToGoal = distance;
                } else {
                    mNewAction = true;
                    stopAction();
                    mMetersToGoal = Float.MAX_VALUE;

                    if (mStep == mRoad.mNodes.size()-1) {
                        cleanMap();
                    } else {
                        mStep++;
                    }
                }
            } else {
                if (mNewAction) {
                    mNewAction = false;
                    startAction(STRAIGHT, distance);
                }
            }

            if (distance > 1000) {
                mNavKm.setText(String.format("%.1f", distance/1000) + "km");
            } else {
                mNavKm.setText(String.format("%.0f", distance) + "m");
            }
        }

        //Road lost control
        if (mRoadOverlay != null) {
            int allowableError = 24;
            if (mTransport.equals(PEDESTRIAN)) allowableError = 54;
            if (!mRoadOverlay.isCloseTo(myLocation, allowableError, mMap)) {
                if (mRoadLost < 3) {
                    mRoadLost++;
                } else {
                    startAction(WRONG, 0);
                    GeoPoint endPoint = mRoad.mNodes.get(mRoad.mNodes.size()-1).mLocation;
                    cleanMap();
                    gotoGeoPoint(endPoint);
                }
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
        mMap.setMapOrientation(-azimuth);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    /* Override from TextToSpeech */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTextToSpeech.setLanguage(new Locale(getString(R.string.locale)));
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getBaseContext(),
                        R.string.alert_lang_not_supported, Toast.LENGTH_SHORT).show();
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
        mLocationManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
        setLocationPreferences();
    }
    @Override
    public void onResume() {
        super.onResume();
        //Start Location Updates
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            mLocationOverlay.setEnabled(true);
        } else {
            Toast.makeText(getBaseContext(), R.string.alert_no_gps, Toast.LENGTH_SHORT).show();
            mLocationOverlay.setEnabled(false);

        }
        //Start Orientation Updates
        if (mMapOrientation) {
            mSensorManager.registerListener(this, mOrientation,SensorManager.SENSOR_DELAY_NORMAL);
        }
        //Internet control
        if (!isOnline()) {
            Toast.makeText(getBaseContext(),
                    R.string.alert_no_internet,Toast.LENGTH_SHORT).show();
        }
        //Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getBaseContext(),
                    R.string.alert_disabled_bluetooth,Toast.LENGTH_SHORT).show();
        } else {
            if (mChatServiceLeft == null) {
                mChatServiceLeft = new BluetoothChatService(this, mHandler);
                mOutStringBufferLeft = new StringBuffer("");
            }
            if (mChatServiceRight == null) {
                mChatServiceRight = new BluetoothChatService(this, mHandler);
                mOutStringBufferRight = new StringBuffer("");
            }

            if (mChatServiceLeft.getState() == BluetoothChatService.STATE_NONE) {
                mChatServiceLeft.start();
            }
            if (mChatServiceRight.getState() == BluetoothChatService.STATE_NONE) {
                mChatServiceRight.start();
            }
        }
        centerMap(getLastLocation());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        setLocationPreferences();
        if (mTextToSpeech != null) {
            mTextToSpeech.shutdown();
        }
        if (mChatServiceLeft != null) {
            mChatServiceLeft.stop();
        }
        if (mChatServiceRight != null) {
            mChatServiceRight.stop();
        }
        if (mApiClient != null) {
            mApiClient.disconnect();
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }
    }
    @Override
    public void onBackPressed() {
        //Exit dialog
        if (mNavMode) {
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
                    mNavMode = true;
                    cleanMap();
                    centerMap(getLastLocation());

                    mTransport = data.getStringExtra("transport");
                    gotoGeoPoint(new GeoPoint(data.getDoubleExtra("lat", 39.40642),
                            data.getDoubleExtra("lon", -3.11702477)));
                }
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getBaseContext(),
                            R.string.alert_no_destination,Toast.LENGTH_SHORT).show();
                    centerMap(getLastLocation());
                }
                break;

            case SETTINGS:
                mMapOrientation = mSettings.getBoolean("settingsDisplayOrientation", false);
                if (!mMapOrientation) mMap.setMapOrientation(0.0f);

                mAlertSounds = mSettings.getBoolean("settingsAlertsSound", false);

                if (mSettings.getBoolean("settingsAlertsVigrate", false)) {
                    disconectDevices();

                    String[] left = mSettings.getString("settingsAlertsVigrateLeft", "").split("\\" + SPLIT);
                    String[] right = mSettings.getString("settingsAlertsVigrateRight", "").split("\\" + SPLIT);

                    String leftDevice = left[0];
                    String leftPayload = left[1];
                    String rightDevice = right[0];
                    String rightPayload = right[1];

                    if (leftDevice.equals(rightDevice) && leftPayload.equals(rightPayload)) {
                        Toast.makeText(getApplicationContext(),
                                R.string.alert_misconfiguration, Toast.LENGTH_SHORT).show();

                    } else if (leftDevice.equals(LOCAL) && rightDevice.equals(BLUETOOTH)) {
                        mAlertVibrate = LLOCAL_RBLUETOOTH;
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(rightPayload);
                        mChatServiceRight.connect(device);
                        showBluetoothConnectingDialog();

                    } else if (leftDevice.equals(BLUETOOTH) && rightDevice.equals(LOCAL)) {
                        mAlertVibrate = LBLUETOOTH_RLOCAL;
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(leftPayload);
                        mChatServiceLeft.connect(device);
                        showBluetoothConnectingDialog();

                    } else if (leftDevice.equals(LOCAL) && rightDevice.equals(WEAR)) {
                        mAlertVibrate = LLOCAL_RWEAR;
                        mWearRight = rightPayload;
                        sendMessageWear(START_ACTIVITY, "", mWearRight);

                    } else if (leftDevice.equals(WEAR) && rightDevice.equals(LOCAL)) {
                        mAlertVibrate = LWEAR_RLOCAL;
                        mWearLeft = leftPayload;
                        sendMessageWear(START_ACTIVITY, "", mWearLeft);

                    } else if (leftDevice.equals(BLUETOOTH) && rightDevice.equals(WEAR)) {
                        mAlertVibrate = LBLUETOOTH_RWEAR;
                        mWearRight = rightPayload;
                        sendMessageWear(START_ACTIVITY, "", mWearRight);
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(leftPayload);
                        mChatServiceLeft.connect(device);
                        showBluetoothConnectingDialog();

                    } else if (leftDevice.equals(WEAR) && rightDevice.equals(BLUETOOTH)) {
                        mAlertVibrate = LWEAR_RBLUETOOTH;
                        mWearLeft = leftPayload;
                        sendMessageWear(START_ACTIVITY, "", mWearLeft);
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(rightPayload);
                        mChatServiceRight.connect(device);
                        showBluetoothConnectingDialog();

                    } else if (leftDevice.equals(BLUETOOTH) && rightDevice.equals(BLUETOOTH)) {
                        mAlertVibrate = LBLUETOOTH_RBLUETOOTH;
                        BluetoothDevice deviceRight = mBluetoothAdapter.getRemoteDevice(rightPayload);
                        BluetoothDevice deviceLeft = mBluetoothAdapter.getRemoteDevice(leftPayload);
                        mChatServiceRight.connect(deviceRight);
                        mChatServiceLeft.connect(deviceLeft);
                        showBluetoothConnectingDialog();

                    } else if (leftDevice.equals(WEAR) && rightDevice.equals(WEAR)) {
                        mAlertVibrate = LWEAR_RWEAR;
                        mWearLeft = leftPayload;
                        mWearRight = rightPayload;
                        sendMessageWear(START_ACTIVITY, "", mWearLeft);
                        sendMessageWear(START_ACTIVITY, "", mWearRight);
                    }
                }
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
                mNavMode = false;
                stopVibrate();
                cleanMap();
                showMap();
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

            case TEST:
                testVibrate();
                break;

            case ABOUT:
			    intent = new Intent(Intent.ACTION_VIEW);
			    intent.setData(Uri.parse("https://bitbucket.org/cr4mos/tfg-sgpcmii"));
			    startActivity(intent);
                break;
        }
    }
}
