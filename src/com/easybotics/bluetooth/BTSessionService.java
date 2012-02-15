package com.easybotics.bluetooth;

import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;



public class BTSessionService 
implements  BTConnectThreadDelegate, BTSessionThreadDelegate 
	{
    private static final String TAG = "BTSessionService";
    private static final boolean D = true;
    
    // Constants that indicate the current connection state
    public static final int STATE_IDLE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    // Handler codes: mConnectionStateHandler
    public static final int SESSION_CONNECTED = 1;
    public static final int SESSION_COULD_NOT_CONNECT = 777;
    public static final int SESSION_LOST = 999;

    //Handler codes: io stream (mStreamHandler)
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 4;
    public static final int MESSAGE_EOF = 8; /// stream closed

    
    //TODO update these values
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    public Handler mConnectionStateHandler; /// Used for connected/connection lost etc
    public Handler mStreamHandler; /// Used for read/write stream
    
    protected Context mParentContext;
    public BluetoothAdapter mAdapter;
    protected BTConnectThread mConnectorThread;
    public BTSessionThread mSessionThread;
    public int mState;
    
    /**
     * Constructor. Prepares a new session.
     * @param context  The UI Activity Context
     * @param handler A handler for sending back connection state messages
     */
    public BTSessionService(Context context, Handler handler) {
    	mParentContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_IDLE;
        mConnectionStateHandler = handler;
    }
    
    public void connectDevice(String address, boolean secure) {
        // Get the BLuetoothDevice object
        BluetoothDevice device = mAdapter.getRemoteDevice(address);
        // TODO Attempt to connect to the device
        mConnectorThread = new BTConnectThread(device, secure);
        mConnectorThread.mInsecureUUID = MY_UUID_SECURE;
        mConnectorThread.mSecureUUID = MY_UUID_INSECURE;
        mConnectorThread.mDelegate = this;
        mConnectorThread.start();
        mState = STATE_CONNECTING;

    }
    
    public void setStreamHandler(Handler streamHandler) {
    	this.mStreamHandler = streamHandler;
    	this.mSessionThread.mStreamHandler = streamHandler;
    }
    
    //================================
    // BTConnectThreadDelegate 
    //================================
    
	public void btConnected(BTConnectThread connector, BluetoothDevice device, BluetoothSocket socket, String socketType) {
		mState = STATE_CONNECTED;
        String deviceName = device.getName();
        if(D) Log.i(TAG,"Connected to: " + deviceName);
    	connector.mDelegate = null;
    	mConnectorThread = null;
    	
        // Start the thread to manage the connection and perform transmissions
		mSessionThread = new BTSessionThread(socket, socketType);
		mSessionThread.start();
    	
		if (null != this.mConnectionStateHandler) {
			mConnectionStateHandler.obtainMessage(SESSION_CONNECTED).sendToTarget();
		}
	}
	
    public void btConnectionFailed(BTConnectThread connector)  {
        Log.e(TAG, "btConnectionFailed");
    	connector.mDelegate = null;
    	mConnectorThread = null;
        mState = STATE_IDLE;
    	//tell UI to handle "could not connect"
		if (null != this.mConnectionStateHandler) {
			mConnectionStateHandler.obtainMessage(SESSION_COULD_NOT_CONNECT).sendToTarget();
		}    	
    }
    
    
    //================================
    // BTSessionThreadDelegate 
    //================================

    public void btConnectionLost(BTSessionThread dead) {
    	dead.mSessionStateDelegate = null;
        Log.e(TAG, "btConnectionLost");
    	mSessionThread = null;
        mState = STATE_IDLE;
    	//post notification to cause UI to do something to handle lost connection
		if (null != this.mConnectionStateHandler) {
			mConnectionStateHandler.obtainMessage(SESSION_LOST).sendToTarget();
		}  
    }
    
}
