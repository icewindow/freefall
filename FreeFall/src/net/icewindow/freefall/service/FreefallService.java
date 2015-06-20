package net.icewindow.freefall.service;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.MainActivity;
import net.icewindow.freefall.activity.RealtimeDataActivity;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.service.bluetooth.BluetoothClient;
import net.icewindow.freefall.service.bluetooth.BluetoothServer;
import net.icewindow.freefall.service.bluetooth.ConnectedDevice;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

public class FreefallService extends Service {

	/**
	 * Extra name for action descriptor integer
	 */
	public static final String EXTRA_ACTION_DESCRIPTOR = "net.icewindow.freefall.intent.ACTION_DESCRIPTOR";

	/**
	 * Intent to start the service
	 */
	public static final String INTENT_NAME = "net.icewindow.freefall.intent.action.DATA_SERVICE";

	/**
	 * Boolean extra for {@link FreefallService#ACTION_CHANGE_NOTIFICATION_DISPLAY}
	 */
	public static final String EXTRA_DISPLAY_NOTIFICATION = "net.icewindow.freefall.extra.DISPLAY_NOTIFICATION";
	/**
	 * Boolean extra for {@link FreefallService#ACTION_SENSOR_TYPE_CHANGE}
	 */
	public static final String EXTRA_SENSOR_TYPE = "net.icewindow.freefall.extra.SENSOR_TYPE";
	/**
	 * String extra for {@link FreefallService#ACTION_SENSOR_WRITE}
	 */
	public static final String EXTRA_WRITE_DATA = "net.icewindow.freefall.extra.WRITE_DATA";
	/**
	 * Class extra to denote who bound to the service
	 */
	public static final String EXTRA_SERVICE_BINDER = "net.icewindow.freefall.extra.SERVICE_BINDER";

	/**
	 * Action to signify the notification should be displayed or not<br/>
	 * Has {@link FreefallService#EXTRA_DISPLAY_NOTIFICATION}
	 */
	public static final int ACTION_CHANGE_NOTIFICATION_DISPLAY = 1;
	/**
	 * Action to indicate the service should try to connect to the sensor
	 */
	public static final int ACTION_CONNECT_SENSOR = 2;
	/**
	 * Action to signify the sensor type has changed (active or passive)<br/>
	 * Has {@link FreefallService#EXTRA_SENSOR_TYPE}
	 */
	public static final int ACTION_SENSOR_TYPE_CHANGE = 3;
	/**
	 * Action indicating we want to write to the remote device<br/>
	 * Has {@link FreefallService#EXTRA_WRITE_DATA}
	 */
	public static final int ACTION_SENSOR_WRITE = 4;

	/**
	 * Message WHAT for notification visibility change
	 */
	public static final int MSG_DISPLAY_NOTIFICATION = 1;
	/**
	 * Message WHAT for Sensor dis/connected
	 */
	public static final int MSG_SENSOR_CONNECT_CHANGED = 2;
	/**
	 * Message WHAT for bluetooth messages
	 */
	public static final int MSG_BLUETOOTH_MESSAGE = 3;
	/**
	 * Message WHAT for server state change
	 */
	public static final int MSG_SERVER_STATE_CHANGE = 4;
	/**
	 * Make a Toast
	 */
	public static final int MSG_TOAST = 100;
	/**
	 * Message WHAT to terminate the service gracefully (unused as of yet)
	 */
	public static final int MSG_STOP_SERVICE = 999;

	public static final int ARG_BT_CONNECT = 1;
	public static final int ARG_BT_DISCONNECT = 0;

	public static final int ARG_SERVER_STARTED = 1;
	public static final int ARG_SERVER_STOPPED = 0;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SENSOR_CONNECT_CHANGED:
					switch (msg.arg1) {
						case ARG_BT_DISCONNECT:
							if (isSensorActive()) {
								notificationBuilder
										.setContentText(getString(R.string.notification_service_sensor_waiting_active));
							} else {
								notificationBuilder
										.setContentText(getString(R.string.notification_service_sensor_waiting_passive));
							}
							sensor = null;
							break;
						case ARG_BT_CONNECT:
							notificationBuilder.setContentText(getString(R.string.notification_service_sensor_connected));
							if (server != null) {
								sensor = server.getRemoteDevice();
							} else {
								sensor = client.getRemoteDevice();
							}
							break;
					}
					postNotification();
					break;
				case MSG_BLUETOOTH_MESSAGE:
					String[] parts = ((String) msg.obj).split(":");
					if (parts[0].equals("msg")) {
						Toast.makeText(FreefallService.this, parts[1], Toast.LENGTH_SHORT).show();
					} else if (parts[0].equals("data")) {
						if (model != null) {

							try {
								String[] data = parts[1].split(",");
								float vx = Float.parseFloat(data[0]);
								float vy = Float.parseFloat(data[1]);
								float vz = Float.parseFloat(data[2]);
								float vv = Float.parseFloat(data[3]);
								model.addValue(RealtimeDataActivity.graphX, vx);
								model.addValue(RealtimeDataActivity.graphY, vy);
								model.addValue(RealtimeDataActivity.graphZ, vz);
								model.addValue(RealtimeDataActivity.graphVector, vv);
								model.commit();
							} catch (Exception e) {
								// Messages sometimes get corrupted. In that case, ignore that
							}
						}
					}
					break;
				case MSG_SERVER_STATE_CHANGE:
					switch (msg.arg1) {
						case ARG_SERVER_STARTED:
							notificationBuilder.setContentText(getString(R.string.notification_service_sensor_waiting_active));
							break;
						case ARG_SERVER_STOPPED:
							notificationBuilder.setContentText(getString(R.string.notification_service_server_stopped));
							break;
					}
					postNotification();
					break;
				case MSG_TOAST:
					Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
					break;
			}
		}
	}

	/**
	 * Binder implementation
	 * 
	 * @author icewindow
	 */
	public class ServiceBinder extends Binder {
		public static final int STATE_OFFLINE = 0;
		public static final int STATE_ONLINE = 1;
		public static final int STATE_CONNECTED = 2;

		public int getServerStatus() {
			return 0;
		}

		public void attachModel(RealtimeGraphModel model) {
			FreefallService.this.model = model;
			if (sensor != null) {
				sensor.write("1");
			}
		}
	}

	private final IBinder serviceBinder = new ServiceBinder();

	private final BroadcastReceiver bluetoothStateChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
				case BluetoothAdapter.STATE_TURNING_OFF:
					server.shutdown();
					break;
				case BluetoothAdapter.STATE_ON:
					server = new BluetoothServer(adapter, serviceHandler);
					server.startup();
					server.start();
			}
		}
	};

	private NotificationCompat.Builder notificationBuilder;
	private NotificationManager notificationManager;
	private int notificationID = 0x1CEC0DE;
	private HandlerThread backgroundThread;
	private ServiceHandler serviceHandler;
	private BluetoothAdapter adapter;
	private BluetoothServer server;
	private BluetoothClient client;
	private ConnectedDevice sensor;
	private boolean isAlive;

	private SharedPreferences preferences;

	private RealtimeGraphModel model;

	private static final String TAG = "DataAcquisitionService";

	@Override
	public IBinder onBind(Intent intent) {
		return serviceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (sensor != null && model != null) {
			sensor.write("0");
		}
		model = null;
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "Service created");
		isAlive = true;

		backgroundThread = new HandlerThread("FreefallBackgroundService", Process.THREAD_PRIORITY_BACKGROUND);
		backgroundThread.start();

		Looper serviceLooper = backgroundThread.getLooper();
		serviceHandler = new ServiceHandler(serviceLooper);

		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(bluetoothStateChangeReceiver, filter);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(getApplicationContext());

		PendingIntent pendingIntent = buildNotificationIntent();
		notificationBuilder.setSmallIcon(R.drawable.ic_freefall_service)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)).setOngoing(true)
				.setContentIntent(pendingIntent).setTicker(getString(R.string.notification_service_ticker))
				.setContentTitle(getString(R.string.notification_service_title));

		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null) {
			if (isSensorActive()) {
				server = new BluetoothServer(adapter, serviceHandler);
				if (adapter.isEnabled()) {
					server.startup();
					server.start();
				}
			} else {
				notificationBuilder.setContentText(getString(R.string.notification_service_sensor_waiting_passive));
				postNotification();
			}
		}
		client = new BluetoothClient(serviceHandler);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			int action = intent.getIntExtra(EXTRA_ACTION_DESCRIPTOR, 0);
			if (action > 0) {
				switch (action) {
					case ACTION_CHANGE_NOTIFICATION_DISPLAY:
						if (intent.getExtras().getBoolean(EXTRA_DISPLAY_NOTIFICATION)) {
							postNotification();
						} else {
							notificationManager.cancel(notificationID);
						}
						break;
					case ACTION_CONNECT_SENSOR:
						String address = preferences.getString(getString(R.string.SENSOR_ADDRESS), "");
						if (address != "") {
							BluetoothDevice device = adapter.getRemoteDevice(address);
							notificationBuilder
									.setContentText(getString(R.string.notification_service_sensor_connecting_passive));
							postNotification();
							client.connectToServer(device);
						}
						break;
					case ACTION_SENSOR_TYPE_CHANGE:
						boolean sensorActive = intent.getBooleanExtra(EXTRA_SENSOR_TYPE, false);
						PendingIntent pendingIntent = buildNotificationIntent(sensorActive);
						notificationBuilder.setContentIntent(pendingIntent);
						if (sensorActive) {
							notificationBuilder.setContentText(getString(R.string.notification_service_sensor_waiting_active));
						} else {
							notificationBuilder.setContentText(getString(R.string.notification_service_sensor_waiting_passive));
						}
						postNotification();
						break;
					case ACTION_SENSOR_WRITE:
						String data = intent.getStringExtra(EXTRA_WRITE_DATA);
						if (sensor != null) {
							sensor.write(data);
						}
						break;
				}
			}
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		isAlive = false;
		unregisterReceiver(bluetoothStateChangeReceiver);
		server.shutdown();
		notificationManager.cancel(notificationID);
		Log.d(TAG, "Service destroyed");
	}

	private void postNotification() {
		if (preferences.getBoolean(getString(R.string.DISPLAY_NOTIFICATION), true) && isAlive) {
			notificationManager.notify(notificationID, notificationBuilder.build());
		}
	}

	private boolean isSensorActive() {
		return preferences.getBoolean(getString(R.string.SENSOR_ACTIVE), false);
	}

	private PendingIntent buildNotificationIntent() {
		return buildNotificationIntent(isSensorActive());
	}

	private PendingIntent buildNotificationIntent(boolean sensorActive) {
		PendingIntent pendingIntent = null;
		if (sensorActive) {
			Intent intent = new Intent(this, MainActivity.class);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(FreefallService.this);
			stackBuilder.addNextIntent(intent);
			pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		} else {
			Intent intent = new Intent(INTENT_NAME);
			intent.putExtra(EXTRA_ACTION_DESCRIPTOR, ACTION_CONNECT_SENSOR);
			pendingIntent = PendingIntent.getService(FreefallService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		return pendingIntent;
	}
}
