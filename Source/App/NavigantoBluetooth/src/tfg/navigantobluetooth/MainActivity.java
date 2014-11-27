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
        if (mVibrator != null) {
        	mVibrator.cancel();
        }
    }
    
    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothChatService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTING:
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                	background.setBackgroundColor(Color.argb(255, 255, 192, 77)); //Orange
                	progressBar.setVisibility(View.VISIBLE);
                	text.setText(getString(R.string.alert_waiting_connexion));
                    break;
                case  BluetoothChatService.STATE_DISCONNECTING:
                	mChatService.stop();
                	mVibrator.cancel();
                	mChatService.start();
                	break;
                }
                break;
            case BluetoothChatService.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                String readMessages = new String(readBuf, 0, msg.arg1);
                String readMessage = "";
                
                for (int i=0; i<readMessages.length(); i++) {
                	if (readMessages.charAt(i) == SPLIT) {
                		startAction(readMessage);
                		readMessage = "";
                	} else {
                		readMessage = readMessage + readMessages.charAt(i);
                	}
                }
                
                break;
            case BluetoothChatService.MESSAGE_DEVICE_NAME:
                mConnectedDeviceName = msg.getData().getString(BluetoothChatService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), getString(R.string.alert_connected_to) 
                		+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                background.setBackgroundColor(Color.argb(255, 76, 255, 76)); //Green
            	progressBar.setVisibility(View.VISIBLE);
            	text.setText(getString(R.string.alert_waiting_instructions));
                break;
            case BluetoothChatService.MESSAGE_TOAST:
            	Toast.makeText(getApplicationContext(),
            			msg.getData().getString(BluetoothChatService.TOAST),
            			Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    private void startAction (String readMessage) {
    	if (readMessage.equals(STOP)) {
        	stopLocalVibration();
        } else if (readMessage.length() > 1) {
        	stopLocalVibration();
        	int action = Integer.parseInt(readMessage.substring(1));
        	startLocalVibration(action);
        } else {
        	mChatService.stop();
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
}
