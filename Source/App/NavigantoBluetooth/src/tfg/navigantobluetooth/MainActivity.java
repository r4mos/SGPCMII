package tfg.navigantobluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements Const {
	
	private View background;
	private View progressBar;
    private TextView text;
	
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    
    private Boolean mIsInVibration = false;
    private Vibrator mVibrator;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.alert_no_bluetooth, Toast.LENGTH_LONG).show();
            finish();
        }
        
        background = (View)findViewById(R.id.background);
        progressBar = (View)findViewById(R.id.progressBar);
        text = (TextView) findViewById(R.id.text);
        
        mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
	}
    @Override
    public synchronized void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
        	background.setBackgroundColor(Color.argb(255, 255, 76, 76)); //Red
        	text.setText(getText(R.string.alert_disabled_bluetooth));
        } else {
            if (mChatService == null) {
                mChatService = new BluetoothChatService(this, mHandler);
            }
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
            	mChatService.start();
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null) {
        	mChatService.stop();
        } 
    }
    
    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothChatService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                	background.setBackgroundColor(Color.argb(255, 76, 255, 76)); //Green
                	progressBar.setVisibility(View.VISIBLE);
                	text.setText(getString(R.string.alert_waiting_instructions));
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	background.setBackgroundColor(Color.argb(255, 255, 192, 77)); //Orange
                	progressBar.setVisibility(View.VISIBLE);
                	text.setText(getString(R.string.alert_waiting_connexion));
                    break;
                case  BluetoothChatService.STATE_DISCONNECTING:
                	mChatService.stop();
                	mChatService.start();
                	break;
                }
                break;
            case BluetoothChatService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessage = new String(readBuf, 0, msg.arg1);
                
                if (readMessage.equals(STOP)) {
                	stopLocalVibration();
                } else if (readMessage.substring(0,1).equals(START)) {
                	int action = Integer.parseInt(readMessage.substring(1));
                	startLocalVibration(action);
                }
                break;
            case BluetoothChatService.MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(BluetoothChatService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(),
                		"Conectado a " + mConnectedDeviceName,
            			Toast.LENGTH_SHORT).show();
                break;
            case BluetoothChatService.MESSAGE_TOAST:
            	Toast.makeText(getApplicationContext(),
            			msg.getData().getString(BluetoothChatService.TOAST),
            			Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    private void startLocalVibration(int action) {
    	if (!mIsInVibration) {
    		mIsInVibration = true;
    		
    		switch (action) {
			case STRAIGHT:
				stopLocalVibration();
				break;
				
			case LEFT:
			case RIGHT:
				long[] turn = {0, 900, 1000};
				mVibrator.vibrate(turn, 0);
				break;
				
			case WRONG:
			case UTURN:
				long[] worng = {0, 100};
				mVibrator.vibrate(worng, 0);
				break;
			
			case DESTINATION:
				long[] destination = {0, 500, 200, 700, 200, 900, 200, 500, 200, 700, 200, 900, 200, 500, 200, 700, 200, 900, 200};
				mVibrator.vibrate(destination, -1);
				break;
			
			//TODO: Simplificar esto
			case ROUNDABOUT1:
				long[] r1 = {0, 500, 1000};
				mVibrator.vibrate(r1, 0);
				break;
			case ROUNDABOUT2:
				long[] r2 = {0, 500, 200, 500, 1000};
				mVibrator.vibrate(r2, 0);
				break;
			case ROUNDABOUT3:
				long[] r3 = {0, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r3, 0);
				break;
			case ROUNDABOUT4:
				long[] r4 = {0, 500, 200, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r4, 0);
				break;
			case ROUNDABOUT5:
				long[] r5 = {0, 500, 200, 500, 200, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r5, 0);
				break;
			case ROUNDABOUT6:
				long[] r6 = {0, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r6, 0);
				break;
			case ROUNDABOUT7:
				long[] r7 = {0, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r7, 0);
				break;
			case ROUNDABOUT8:
				long[] r8 = {0, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 200, 500, 1000};
				mVibrator.vibrate(r8, 0);
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
}
