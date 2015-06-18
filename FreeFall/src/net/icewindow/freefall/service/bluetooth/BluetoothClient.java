package net.icewindow.freefall.service.bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class BluetoothClient implements IBluetoothConnection {

	private ConnectedDevice server;
	private Handler handler;

	private class ConnectDevice extends Thread {
		private BluetoothSocket socket;

		public ConnectDevice(BluetoothDevice device) {
			try {
				socket = device.createRfcommSocketToServiceRecord(BluetoothServer.SERVICE_UUID);
			} catch (IOException e) {
			}
		}

		@Override
		public void run() {
			try {
				socket.connect();
			} catch (IOException e) {
			}
			BluetoothClient.this.connectDevice(socket);
		}
	}

	public BluetoothClient(Handler handler) {
		this.handler = handler;
	}

	public void connectToServer(BluetoothDevice device) {
		new ConnectDevice(device).start();
	}

	protected void connectDevice(BluetoothSocket socket) {
		server = new ConnectedDevice(socket, handler);
	}

	@Override
	public void write(String data) {
		if (server != null) {
			server.write(data);
		}
	}

}
