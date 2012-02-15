package com.easybotics.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public interface BTConnectThreadDelegate {
	public void btConnected(BTConnectThread connector, BluetoothDevice device, BluetoothSocket socket, String socketType);
    public void btConnectionFailed(BTConnectThread connector) ;
}
