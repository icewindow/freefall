package net.icewindow.freefall.service.bluetooth;

import java.io.IOException;
import java.util.UUID;

import net.icewindow.freefall.service.FreefallService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothServer extends Thread implements IBluetoothConnection {

	public static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static final String TAG = "BluetoothServer";

	private BluetoothServerSocket socket;
	protected final Handler handler;
	private final BluetoothAdapter adapter;

	protected ConnectedDevice client;
	private boolean running = false;

	public BluetoothServer(BluetoothAdapter adapter, Handler handler) {
		this.handler = handler;
		this.adapter = adapter;
	}

	@Override
	public void run() {
		running = true;
		BluetoothSocket clientSocket = null;
		{
			Message msg = handler.obtainMessage(FreefallService.MSG_SERVER_STATE_CHANGE);
			msg.arg1 = FreefallService.ARG_SERVER_STARTED;
			handler.sendMessage(msg);
		}
		Log.d(TAG, "Server starting up");
		while (running) {
			if (socket == null) {
				break;
			}
			try {
				clientSocket = socket.accept();
				Log.d(TAG, "Accepted connection");
				synchronized (this) {
					BluetoothServer.this.client = new ConnectedDevice(clientSocket, BluetoothServer.this.handler);
					BluetoothServer.this.client.start();
				}
			} catch (IOException e) {
				break;
			}
		}
		running = false;
		{
			Message msg = handler.obtainMessage(FreefallService.MSG_SERVER_STATE_CHANGE);
			msg.arg1 = FreefallService.ARG_SERVER_STOPPED;
			handler.sendMessage(msg);
		}
		Log.d(TAG, "Server shut down");
	}

	public void startup() {
		Log.d(TAG, "Setting up BluetoothServerSocket");
		BluetoothServerSocket tmpSock = null;
		try {
			tmpSock = adapter.listenUsingRfcommWithServiceRecord("SSP", SERVICE_UUID);
		} catch (IOException e) {
			Log.e(TAG, "Error Creating ServerSocket in dedicated method!");
			e.printStackTrace();
		}
		socket = tmpSock;
	}

	public void shutdown() {
		Log.d(TAG, "Shutting server down...");
		running = false;
		try {
			socket.close();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
		socket = null;
	}

	@Override
	public void write(String data) {
		if (client != null) {
			client.write(data);
		}
	}

}
