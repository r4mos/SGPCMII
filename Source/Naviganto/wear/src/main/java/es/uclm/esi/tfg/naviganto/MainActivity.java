package es.uclm.esi.tfg.naviganto;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements Const,  MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private GoogleApiClient mApiClient;

    private Boolean mIsInVibration = false;
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initGoogleApiClient();
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if (mApiClient != null) {
            if (mApiClient.isConnected()) {
                setTextOnTextView(R.string.alert_waiting_instructions);
            } else if (!mApiClient.isConnecting()) {
                setTextOnTextView(R.string.alert_waiting_connexion);
                mApiClient.connect();
            }
        }
    }

    private void setTextOnTextView (int resId) {
        setTextOnTextView(getString(resId));
    }

    private void setTextOnTextView(final String text) {
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                TextView txt = (TextView) stub.findViewById(R.id.text);
                txt.setText(text);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                    startAction(new String(messageEvent.getData()));
                } else if (messageEvent.getPath().equalsIgnoreCase( END_ACTIVITY ) ) {
                    if ( mApiClient.isConnected() ) {
                        mApiClient.disconnect();
                    }
                    if (mVibrator != null) {
                        mVibrator.cancel();
                    }
                }
            }
        });
    }

    private void startAction (String readMessage) {
        if (readMessage.equals(STOP)) {
            stopLocalVibration();
        } else if (readMessage.length() > 1) {
            stopLocalVibration();
            int action = Integer.parseInt(readMessage.substring(1));
            startLocalVibration(action);
        }
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

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
        setTextOnTextView(R.string.alert_waiting_instructions);
    }

    @Override
    protected void onStop() {
        if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );
        super.onDestroy();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mVibrator != null) {
            mVibrator.cancel();
        }
    }
}