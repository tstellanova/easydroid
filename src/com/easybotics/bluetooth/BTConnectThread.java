package com.easybotics.bluetooth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;




public class BTConnectThread extends Thread {
    private static final String TAG = "BTConnectThread";

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String mSocketType;

    public BTConnectThreadDelegate mDelegate;
    public UUID mSecureUUID;
    public UUID mInsecureUUID;
    
    public BTConnectThread(BluetoothDevice device, boolean secure) {
        mmDevice = device;
        BluetoothSocket tmp = null;
        mSocketType = secure ? "Secure" : "Insecure";

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
        	Method m;
			m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
			if (null != m) {
				tmp = (BluetoothSocket) m.invoke(device, 1);
			} else {
	            if (secure) {
	                tmp = device.createRfcommSocketToServiceRecord(mSecureUUID);
	            } else {
	                tmp = device.createInsecureRfcommSocketToServiceRecord(mInsecureUUID); 
	            }
			}
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
		}  
        mmSocket = tmp;
    }

    public void run() {
        Log.i(TAG, "BEGIN socketType:" + mSocketType);
        setName("BTConnectThread" + mSocketType);

        if (null == mmSocket) {
        	Log.e(TAG,"null socket!!!");
        	return;
        }
        
        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mmSocket.connect();
        } catch (IOException e) {
        	Log.e(TAG, "Connect failed ",e);
            // Close the socket
            try {
                mmSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() " + mSocketType +
                        " socket during connection failure", e2);
            }
            
            if (mDelegate != null) {
            	mDelegate.btConnectionFailed(this);
            }
            return;
        }

        if (mDelegate != null) {
        	mDelegate.btConnected(this, mmDevice, mmSocket, mSocketType);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
        }
    }
    
}
