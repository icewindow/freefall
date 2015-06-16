package net.icewindow.freefall.service.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.icewindow.freefall.service.DataAcquisitionService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * A device connected to the host device
 * 
 * @author icewindow
 * 
 */
public class ConnectedDevice extends Thread {

	/**
	 * LogCat Tag
	 */
	public static final String TAG = "ConnectedDevice";

	private final BluetoothSocket socket;
	private final Handler handler;
	private final InputStream in;
	private final OutputStream out;

	private boolean running = false;

	/**
	 * Creates a new ConnectedDevice, which is connected to the socket specified.
	 * 
	 * @param socket
	 *            The socket this device is connected to
	 * @param handler
	 *            The handler which handles Bluetooth messages
	 */
	public ConnectedDevice(BluetoothSocket socket, Handler handler) {
		this.socket = socket;
		this.handler = handler;

		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "Error during constructor: couldn't get input/output stream from socket!", e);
		}
		this.in = tmpIn;
		this.out = tmpOut;
	}

	@Override
	public void run() {
		Log.d(TAG, "ConnectedDevice.run() invoked");
		byte[] buffer = new byte[512];
		int read;
		running = true;

		Log.d(TAG, "Listening for incoming data...");
		while (running) {
			StringBuilder builder = new StringBuilder();
			try {
				outer: while ((read = in.read(buffer)) > 0) {
					for (int i = 0; i < read; i++) {
						if (buffer[i] == (byte) 0x0A) {
							break outer;
						}
						builder.append((char) buffer[i]);
					}
				}
			} catch (IOException e) {
				break;
			}
			Message message = handler.obtainMessage(DataAcquisitionService.MSG_BLUETOOTH_MESSAGE);
			message.obj = builder.toString();
			handler.sendMessage(message);
		}
		Log.d(TAG, "Connection closed");
		Message msg = handler.obtainMessage(DataAcquisitionService.MSG_SENSOR_CONNECT_CHANGED);
		msg.arg1 = DataAcquisitionService.ARG_BT_DISCONNECT;
		handler.sendMessage(msg);
		running = false;
	}

	/**
	 * Writes data to the output stream
	 * 
	 * @param data
	 *            The data to be written
	 */
	public void write(String data) {
		byte[] buffer = data.getBytes();
		Log.d(TAG, "Writing " + buffer.length + " bytes to " + socket.getRemoteDevice().getName());
		try {
			out.write(buffer);
			// out.write(new byte[] { (byte) 0x00, (byte) 0xFF });
		} catch (IOException e) {
			Log.e(TAG, "Error writing to output stream!", e);
		}
	}

	/**
	 * Closes the socket, disconnecting this device from the remote device
	 */
	public void disconnect() {
		try {
			socket.close();
			running = false;
		} catch (IOException e) {
			Log.e(TAG, "Error closing socket!", e);
		}
	}

	// Getters/Setters

	/**
	 * Gets the remote device this device is connected to
	 * 
	 * @return The remote device or <code>null</code> if the socket is <code>null</code>
	 */
	public BluetoothDevice getRemoteDevice() {
		if (null == socket) {
			return null;
		}
		return socket.getRemoteDevice();
	}

	/**
	 * Checks whether or not the thread is running, and thus if the connection is alive
	 * 
	 * @return <code>true</code> if the connection is alive, <code>false</code> otherwise
	 */
	public boolean isConnected() {
		return running;
	}
}
