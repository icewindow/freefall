package net.icewindow.freefall.service.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class BluetoothServer extends Thread implements IBluetoothConnection {

	public static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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
		startup();
		running = true;
		BluetoothSocket clientSocket = null;
		while (running) {
			try {
				clientSocket = socket.accept();
				synchronized (this) {
					BluetoothServer.this.client = new ConnectedDevice(clientSocket, BluetoothServer.this.handler);
					BluetoothServer.this.client.start();
				}
			} catch (IOException e) {
			}
		}
	}
	
	private void startup() {
		BluetoothServerSocket tmpSock = null;
		try {
			tmpSock = adapter.listenUsingRfcommWithServiceRecord("SSP", SERVICE_UUID);
		} catch (IOException e) {
			e.printStackTrace();
		}
		socket = tmpSock;
	}
	
	public void shutdown() {
		try {
			socket.close();
		} catch (IOException e) {
		}
		running = false;
	}

	@Override
	public void write(String data) {
		client.write(data);
	}

}
