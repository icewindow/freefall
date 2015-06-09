package net.icewindow.freefall.bluetooth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothManager {

	/**
	 * LogCat tag
	 */
	public static final String TAG = "BluetoothManager";

	/**
	 * UUID for this application
	 */
	// public static final UUID SERVICE_UUID = UUID.fromString("01CEC0DE-014B-5E55-2013-00B4171C50F7");
	public static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	/**
	 * Name for this service
	 */
	public static final String NAME = "CardDroid";

	/**
	 * Enable bluetooth intent return value
	 */
	public static final int REQUEST_BT_ENABLE = 10;

	/**
	 * Request discoverable intent return value
	 */
	public static final int REQUEST_BT_VISIBLE = 11;

	/**
	 * The amount of time this device should be visible to other devices, in seconds
	 */
	public static final int DISCOVERABLE_TIME = 300;

	/**
	 * Value for the WHAT-field in messages from <code>ConnectedDevice</code>
	 */
	public static final int BT_MESSAGE = 20;

	/**
	 * Value for the WHAT-filed in messages from <code>BluetoothManager.foundBroadcastReceiver</code> when a bluetooth
	 * device is found during discovery
	 */
	public static final int BT_FOUND = 21;

	/**
	 * Value for the WHAT-filed in messages indicating a client connected or disconnected
	 */
	public static final int BT_CLIENT_CONNECT_CHANGE = 22;

	/**
	 * Indicating a device has connected in BT_CLIENT_CONNECT_CHANGE messages
	 */
	public static final int BT_CLIENT_CONNECT = 221;

	/**
	 * Indicating a device disconnected in BT__CLIENT_CONNECT_CHANGE messages
	 */
	public static final int BT_CLIENT_DISCONNECT = 222;

	/**
	 * WHAT for when devices got marked as dead
	 */
	public static final int BT_CLIENT_COUNT_CHANGE = 23;

	/**
	 * Intent for when device connects or disconnects
	 */
	public static final String ACTION_CLIENT_CONNECT_CHANGE = "net.icewindow.bluetoothmanager.action.CLIENT_CONNECT_CHANGE";

	/**
	 * Intent for when clients get marked as dead
	 */
	public static final String ACTION_CLIENT_COUNT_CHANGE = "net.icewindow.bluetoothmanager.action.CLIENET_COUNT_CHANGE";

	private final BluetoothAdapter adapter;
	private final Handler handler;

	private ArrayList<ConnectedDevice> connectedDevices;
	private HashSet<BluetoothDevice> availableDevices;
	private EstablishDeviceConnection establishDeviceConnection;

	/**
	 * BroadcastReceiver which will receive {@link BluetoothDevice#ACTION_FOUND} messages<br/>
	 * Sends a message to the handler if a matching broadcast is received
	 */
	public final BroadcastReceiver foundBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				availableDevices.add(device);
				Message foundMessage = new Message();
				foundMessage.what = BT_FOUND;
				handler.sendMessage(foundMessage);
			}
		}
	};

	/**
	 * "Garbage collector" broadcast receiver, which will delete all unused (disconnected) devices
	 */
	public final BroadcastReceiver deviceDisconnectReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_CLIENT_CONNECT_CHANGE.equals(action)) {
				ArrayList<ConnectedDevice> deadDevices = new ArrayList<ConnectedDevice>();
				for (ConnectedDevice device : connectedDevices) {
					if (!device.isConnected()) {
						Log.d(TAG, "Device " + device.getRemoteDevice().getName() + " marked as dead");
						deadDevices.add(device);
					}
				}
				Log.d(TAG, "Marked " + deadDevices.size() + " devices as dead");
				if (deadDevices.size() > 0) {
					connectedDevices.removeAll(deadDevices);
					Message msg = new Message();
					msg.what = BT_CLIENT_COUNT_CHANGE;
					handler.sendMessage(msg);
				}
			}
		}
	};

	/**
	 * Creates a new BluetoothManager
	 * 
	 * @param adapter
	 *            The bluetooth adapter
	 * @param handler
	 *            Handler which will handle incoming messages
	 */
	public BluetoothManager(Handler handler) {
		Log.d(TAG, "BluetoothManager initialized");
		this.adapter = BluetoothAdapter.getDefaultAdapter();
		this.handler = handler;
		connectedDevices = new ArrayList<ConnectedDevice>();
	}

	/**
	 * Sends a message to all connected devices
	 * 
	 * @param data
	 *            The message to be send
	 */
	public void broadcast(String data) {
		if (connectedDevices != null) {
			Log.d(TAG,
					"Broadcasting \"" + data.substring(0, (data.length() > 32 ? 32 : data.length()))
							+ (data.length() > 32 ? "(...)" : "") + "\"");
			for (ConnectedDevice device : connectedDevices) {
				if (device != null) {
					device.write(data);
				}
			}
		}
	}

	/**
	 * Send a message to all devices listed in <code>devices</code><br/>
	 * Devices are addressed by their index in the array list.
	 * 
	 * @param devices
	 *            Array of indices of the devices to which to send the message
	 * @param data
	 *            The message to be send
	 */
	public void send(int[] devices, String data) {
		for (int device : devices) {
			connectedDevices.get(device).write(data);
		}
	}

	/**
	 * Establish a connection with a remote device (connect to a remote server)
	 * 
	 * @param device
	 *            The device with which to establish the connection
	 */
	public void establishConnection(BluetoothDevice device) {
		if (null != establishDeviceConnection) {
			establishDeviceConnection.cancel();
			establishDeviceConnection = null;
		}
		establishDeviceConnection = new EstablishDeviceConnection(this, device);
		establishDeviceConnection.start();
	}

	/**
	 * Connect to a remote bluetooth device
	 * 
	 * @param socket
	 *            The socket to which to connect
	 */
	public void connectDevice(BluetoothSocket socket) {
		if (null != socket) {
			Message msg = new Message();
			msg.what = BT_CLIENT_CONNECT_CHANGE;
			msg.arg1 = BT_CLIENT_CONNECT;
			msg.obj = socket;
			handler.sendMessage(msg);
			ConnectedDevice newDevice = new ConnectedDevice(socket, handler);
			newDevice.start();
			connectedDevices.add(newDevice);
		}
	}

	// Getters/Setters

	public ArrayList<ConnectedDevice> getConnectedDevices() {
		return connectedDevices;
	}

	/**
	 * Checks if bluetooth is enabled
	 * 
	 * @return <code>true</code> if bluetooth is enabled, <code>false</code> otherwise
	 */
	public boolean isBluetoothEnabled() {
		return adapter.isEnabled();
	}

	/**
	 * Checks if a bluetooth adapter is present
	 * 
	 * @return <code>true</code> if an adapter is present, <code>false</code> otherwise
	 */
	public boolean isBluetoothPresent() {
		if (null == adapter) {
			return false;
		} else {
			return true;
		}
	}

	// Helper methods

	/**
	 * Wrapper for {@link BluetoothAdapter#startDiscovery()}
	 * 
	 * @see BluetoothAdapter#startDiscovery()
	 */
	public void startDiscovery() {
		adapter.startDiscovery();
	}

	/**
	 * Wrapper for {@link BluetootAdapter#cancelDiscovery()}
	 * 
	 * @see BluetoothAdapter#cancelDiscovery()
	 */
	public void cancelDiscovery() {
		adapter.cancelDiscovery();
	}

	/**
	 * Reverts the name change of the device made by <code>setDeviceGame()</code>
	 * 
	 * @see BluetoothManager#setDeviceGame(gameName)
	 */
	public void revertDeviceName() {
		String[] deviceName = adapter.getName().split(" ");
		if (deviceName.length == 1) {
			return;
		}
		adapter.setName(deviceName[deviceName.length - 1]);
	}

	/**
	 * Sets the device's name to the game's name, specified by <code>gameName</code>, plus the old device name.<br/>
	 * Name can be changed back by <code>revertDeviceName()</code>
	 * 
	 * @param gameName
	 *            The game's name
	 * @see BluetoothManager#revertDeviceName()
	 */
	public void setDeviceGame(String gameName) {
		String deviceName = adapter.getName();
		if (adapter.setName(gameName + " on " + deviceName)) {
			Log.d(TAG, "Successfully changed device name to " + deviceName);
		} else {
			Log.e(TAG, "Could not change device name!");
		}
	}

	/**
	 * Reset the list of available devices
	 */
	public void resetAvailableDeviceList() {
		availableDevices = null;
		availableDevices = new HashSet<BluetoothDevice>();
	}

	/**
	 * List all devices which are paired with this device and are in reach
	 * 
	 * @return All devices which are paired and in reach
	 */
	public Set<BluetoothDevice> getAvailableDevices() {
		Set<BluetoothDevice> bondedDevices = new HashSet<BluetoothDevice>(adapter.getBondedDevices());
		bondedDevices.retainAll(availableDevices);
		return bondedDevices;
	}
}
