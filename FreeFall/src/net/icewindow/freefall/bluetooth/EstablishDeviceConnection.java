package net.icewindow.freefall.bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * Thread to try and establish a connection with a remote bluetooth device
 * 
 * @author icewindow
 * 
 */
public class EstablishDeviceConnection extends Thread {

	public static final String TAG = "EstablishDeviceConnection";

	private final BluetoothManager bluetoothManager;
	private final BluetoothSocket socket;

	/**
	 * Creates a new thread to establish a connection wit a remote device
	 * 
	 * @param bluetoothManager
	 *            Reference to the creating BluetoothManager
	 * @param device
	 *            The remote device
	 */
	public EstablishDeviceConnection(BluetoothManager bluetoothManager, BluetoothDevice device) {
		this.bluetoothManager = bluetoothManager;
		BluetoothSocket tmp = null;
		try {
			tmp = device.createRfcommSocketToServiceRecord(BluetoothManager.SERVICE_UUID);
		} catch (IOException e) {
			Log.e(TAG, "Error trying to setup connection", e);
		}
		socket = tmp;
	}

	@Override
	public void run() {
		Log.d(TAG, "Establishing connection...");
		bluetoothManager.cancelDiscovery();
		try {
			socket.connect();
		} catch (IOException e) {
			Log.e(TAG, "Error establishing connection!", e);
			try {
				socket.close();
			} catch (IOException ex) {
				Log.e(TAG, "Error closing socket", ex);
			}
		}
		bluetoothManager.connectDevice(socket);
	}

	/**
	 * Aborts the connection
	 */
	public void cancel() {
		try {
			socket.close();
		} catch (IOException e) {
			Log.e(TAG, "Error closing socket!", e);
		}
	}
}
