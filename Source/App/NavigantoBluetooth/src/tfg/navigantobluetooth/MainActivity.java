package tfg.navigantobluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private View background;
	private View progressBar;
    private TextView text;
	
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
	
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
                //TODO
                Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                //
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
}
