package com.easybotics.bluetooth;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Arrays;



import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.os.Handler;



public class BTSessionThread extends Thread {

    private static final String TAG = "BTSessionThread";
    private static final boolean D = true;
    
   
    protected BluetoothSocket mmSocket;
    protected BufferedInputStream mmInStream;
    protected OutputStream mmOutStream;
    
    public BTSessionThreadDelegate mSessionStateDelegate;
    public Handler mStreamHandler;
    public Boolean keepReading = new Boolean(true);
        
    public BTSessionThread(BluetoothSocket socket, String socketType) {
        if(D) Log.d(TAG, "create socketType: " + socketType);
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
            mmInStream = new BufferedInputStream(tmpIn,512);
            mmOutStream = tmpOut;
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
            this.notifyListenersOfDisconnect();
        }
    }

    public void run() {
        if(D) Log.i(TAG, "BEGIN BTSessionThread");
        setName("BTSessionThread");

        byte[] buffer = new byte[1024];
        int nRead, nAvail;

        // Keep listening to the InputStream while connected
        while (keepReading) {
            try {
                // Read from the InputStream
            	nAvail = mmInStream.available();
            	Log.i(TAG,"nAvail : " + nAvail);
            	if (nAvail > 0) {
            		nRead = mmInStream.read(buffer);
            		Log.i(TAG,"nRead: " + nRead);
            		byte[] subBuf = Arrays.copyOfRange(buffer,0,nRead);
            		if (null != this.mStreamHandler) {
            			mStreamHandler.obtainMessage(BTSessionService.MESSAGE_READ, nRead, -1, subBuf).sendToTarget();
            		}
            	} else {
            		if (mmInStream.markSupported()) {
            			mmInStream.mark(256);
            			nRead = mmInStream.read(buffer);
            			mmInStream.reset();
            		}
            	}
            } catch (IOException e) {
                Log.e(TAG, "disconnected exception: ", e);
                this.notifyListenersOfDisconnect();
                break;
            }
        }
    }

    protected void notifyListenersOfDisconnect() {
        if (null != mSessionStateDelegate) {
        	mSessionStateDelegate.btConnectionLost(this);
        }
		if (null != this.mStreamHandler) {
			mStreamHandler.obtainMessage(BTSessionService.MESSAGE_EOF).sendToTarget();
		}
    }
    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);
            if (null != this.mStreamHandler) {
            	mStreamHandler.obtainMessage(BTSessionService.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
            //TODO write failed notification?
        }
    }

        /**
         * Close the session
         * 
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of session socket failed", e);
            }
        }
        
        
}
