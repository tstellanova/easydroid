package com.easybotics.easybiped;

import com.easybotics.bluetooth.R;

import com.easybotics.bluetooth.BTSessionService;
import com.easybotics.bluetooth.BTDeviceListActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class EasyBipedActivity extends Activity {

	// Debugging
	private static final String TAG = "EasyBiped";
	private static final boolean D = true;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;

	protected MainApplication mSharedApp = null;

	protected String getSavedDeviceAddress() {
		String address = null;
		SharedPreferences prefs = mSharedApp.getSharedPreferences("btConfiguration", Context.MODE_PRIVATE);
		if (null != prefs) {
			String info = prefs.getString("bt_last_selected_device",null);
	        if (null != info)
	        	address = info.substring(info.length() - 17);//get just the MAC address
		}
        return address;
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mSharedApp = (MainApplication) getApplicationContext();

		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (null == btAdapter) {
			Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG)
					.show();
			//TODO finish();
			Intent chatIntent = new Intent(mSharedApp,
					CommandBotActivity.class);
			startActivity(chatIntent);
			return;
		}

		String address = this.getSavedDeviceAddress();
		if (null == address) {
			this.pickDevice();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		String address = this.getSavedDeviceAddress();
		if (null == address) {
			menu.removeItem(R.id.connect_recent);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		case R.id.connect_recent:
			{
				// load the device info
				String address = this.getSavedDeviceAddress();
				if (null != address) {
					doConnectDevice(address,false);
				}
			}
			return true;
			
		case R.id.connect_new:
			this.pickDevice();
			return true;
			
//		case R.id.secure_connect_scan:
//			// Launch the DeviceListActivity to see devices and do scan
//			scanIntent = new Intent(this, BTDeviceListActivity.class);
//			startActivityForResult(scanIntent, REQUEST_CONNECT_DEVICE_SECURE);
//			return true;
//		case R.id.insecure_connect_scan:
//			// Launch the DeviceListActivity to see devices and do scan
//			scanIntent = new Intent(this, BTDeviceListActivity.class);
//			startActivityForResult(scanIntent, REQUEST_CONNECT_DEVICE_INSECURE);
//			return true;
		}
		return false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult req:" + requestCode + " result: "
					+ resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		}
	}
	
	protected void pickDevice() {
		// Scan for devies and allow user to select
		Intent scanIntent  = new Intent(this, BTDeviceListActivity.class);
		startActivityForResult(scanIntent, REQUEST_CONNECT_DEVICE_INSECURE);
	}

	private void connectDevice(Intent data, boolean secure) {
		Bundle extraBundle = data.getExtras();
		// Get the device MAC address
		String address = extraBundle.getString(BTDeviceListActivity.EXTRA_DEVICE_ADDRESS);
		String deviceInfo = extraBundle.getString(BTDeviceListActivity.EXTRA_DEVICE_INFO);
			
		// save the device info
		SharedPreferences prefs = mSharedApp.getSharedPreferences("btConfiguration", Context.MODE_PRIVATE);
		SharedPreferences.Editor edits = prefs.edit();
		edits.putString("bt_last_selected_device", deviceInfo);
		edits.commit();

		doConnectDevice(address,secure);
	}
	
	private void doConnectDevice(String address, boolean secure) {
        if(D) Log.v(TAG,"Connect to: " + address);

		// clear out any old session
		if (null != mSharedApp.mBTSession) {
			mSharedApp.mBTSession.mConnectionStateHandler = null;
			mSharedApp.mBTSession = null;
		}
		mSharedApp.mBTSession = new BTSessionService(mSharedApp,
				this.mConnectionStateHandler);
		mSharedApp.mBTSession.connectDevice(address, secure);
	}

	// The Handler that gets information back from the BTSessionService
	private final Handler mConnectionStateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BTSessionService.SESSION_CONNECTED: {
				// Intent chatIntent = new Intent(sharedApp,
				// BTChatActivity.class);
				Intent chatIntent = new Intent(mSharedApp,
						CommandBotActivity.class);
				startActivity(chatIntent);
			}
				break;

			case BTSessionService.SESSION_LOST: {
				// TODO
			}
				break;
			}
		}
	};
}