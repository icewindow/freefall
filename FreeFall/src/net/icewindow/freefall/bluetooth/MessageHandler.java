package net.icewindow.freefall.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * Handles messages sent by the BTM
 * 
 * @author icewindow
 * 
 */
public class MessageHandler extends Handler {

	/**
	 * Intent for when the BTM found a new device in reach
	 */
	public static final String FOUND_INTENT = "net.icewindow.bluetoothmanager.action.DEVICE_FOUND";

	public static final String DISCONNECT_INTENT = "net.icewindow.bluetoothmanager.action.DISCONNECT";

	public static final String DISCONNECTED_DEVICE = "bluetoothmanager.name.DISCONNECTED_DEVICE";

	private final Context context;

	/**
	 * Creates the handler
	 * 
	 * @param context
	 *            The context on which to operate (usually the main activity)
	 */
	public MessageHandler(Context context) {
		this.context = context;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
			case BluetoothManager.BT_MESSAGE:
				// Got a message via bluetooth. Just show it to the user for now
				// TODO handle message in program
				Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
				break;
			case BluetoothManager.BT_FOUND:
				// BTM found a new device in reach
				context.sendBroadcast(new Intent(FOUND_INTENT));
				break;
			case BluetoothManager.BT_CLIENT_CONNECT_CHANGE:
				context.sendBroadcast(new Intent(BluetoothManager.ACTION_CLIENT_CONNECT_CHANGE));
				context.sendBroadcast(new Intent(BluetoothManager.ACTION_CLIENT_COUNT_CHANGE));
				String device = ((android.bluetooth.BluetoothSocket) msg.obj).getRemoteDevice()
						.getName();
				switch (msg.arg1) {
					case BluetoothManager.BT_CLIENT_CONNECT:
						Toast.makeText(context, "Client " + device + " connected",
								Toast.LENGTH_SHORT).show();
						break;
					case BluetoothManager.BT_CLIENT_DISCONNECT:
						Toast.makeText(context, "Client " + device + " disconnected",
								Toast.LENGTH_SHORT).show();
						break;
				}
			case BluetoothManager.BT_CLIENT_COUNT_CHANGE:
				context.sendBroadcast(new Intent(BluetoothManager.ACTION_CLIENT_COUNT_CHANGE));
				break;
			default:
				break;
		}
	}

}
