package net.icewindow.freefall.service.bluetooth;

import java.io.IOException;
import java.util.UUID;

import net.icewindow.freefall.service.DataAcquisitionService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothServer extends Thread {

	public static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothServerSocket socket;
	protected final Handler handler;

	protected ConnectedDevice client;
	private boolean running = false;

	private class ConnectDevice extends Thread {
		private static final String TAG = "BluetoothServer$ConnectDevice";
		private final BluetoothSocket socket;

		public ConnectDevice(BluetoothSocket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			Log.d(TAG, "Connecting to device...");
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
			Message msg = BluetoothServer.this.handler.obtainMessage();
			msg.what = DataAcquisitionService.MSG_SENSOR_CONNECT_CHANGED;
			msg.arg1 = 1;
			BluetoothServer.this.client = new ConnectedDevice(socket, BluetoothServer.this.handler);
			BluetoothServer.this.client.start();
		}
	}

	public BluetoothServer(Handler handler) {
		BluetoothServerSocket tmpSock = null;
		try {
			tmpSock = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("SSP", SERVICE_UUID);
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket = tmpSock;
		this.handler = handler;
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			try {
				BluetoothSocket clientSocket = socket.accept();
				new ConnectDevice(clientSocket).start();
			} catch (IOException e) {

			}
		}
	}

}
