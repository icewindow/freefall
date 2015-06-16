package net.icewindow.freefall.service;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.MainActivity;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.service.bluetooth.BluetoothServer;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.Toast;

public class DataAcquisitionService extends Service {

	/**
	 * Extra name for action descriptor integer
	 */
	public static final String EXTRA_ACTION_DESCRIPTOR = "net.icewindow.freefall.intent.ACTION_DESCRIPTOR";

	/**
	 * Intent to start the service
	 */
	public static final String ACTION_INTENT = "net.icewindow.freefall.intent.action.DATA_SERVICE";

	/**
	 * Extra for notification visibility
	 */
	public static final String EXTRA_DISPLA_NTIFICATION = "net.icewindow.freefall.extra.DISPLAY_NOTIFICATION";

	/**
	 * Extra for sensor module connection state changed
	 */
	public static final String EXTRA_SENSOR_CONNECT_CHANGED = "net-icewindow.freefall.extra.SENSOR_CONNECT_CHANGED";

	/**
	 * Message WHAT for notification visibility change
	 */
	public static final int MSG_DISPLAY_NOTIFICATION = 1;
	/**
	 * Message WHAT for Sensor dis/connected
	 */
	public static final int MSG_SENSOR_CONNECT_CHANGED = 2;
	/**
	 * 
	 */
	public static final int MSG_BLUETOOTH_MESSAGE = 3;
	/**
	 * Message WHAT to terminate the service gracefully (unused as of yet)
	 */
	public static final int MSG_STOP_SERVICE = 999;

	public static final int ARG_BT_CONNECT = 1;
	public static final int ARG_BT_DISCONNECT = 0;

	private NotificationCompat.Builder notificationBuilder;
	private NotificationManager notificationManager;
	private int notificationID = 0x1CEC0DE;
	private HandlerThread backgroundThread;
	private ServiceHandler serviceHandler;
	private BluetoothServer server;

	private SharedPreferences preferences;

	private RealtimeGraphModel model;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DISPLAY_NOTIFICATION:
				if (msg.arg1 == 0) {
					notificationManager.cancel(notificationID);
				} else {
					postNotification();
				}
				break;
			case MSG_SENSOR_CONNECT_CHANGED:
				switch (msg.arg1) {
				case ARG_BT_DISCONNECT:
					notificationBuilder.setContentText(getString(R.string.notification_service_ready));
					break;
				case ARG_BT_CONNECT:
					notificationBuilder.setContentText(getString(R.string.notification_service_running));
					break;
				}
				postNotification();
				break;
			case MSG_BLUETOOTH_MESSAGE:
				Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		backgroundThread = new HandlerThread("FreefallBackgroundService", Process.THREAD_PRIORITY_BACKGROUND);
		backgroundThread.start();

		Looper serviceLooper = backgroundThread.getLooper();
		serviceHandler = new ServiceHandler(serviceLooper);

		server = new BluetoothServer(serviceHandler);
		server.start();

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(getApplicationContext());

		Intent intent = new Intent(this, MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addNextIntent(intent);
		PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		notificationBuilder.setSmallIcon(R.drawable.ic_freefall_service)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
				.setContentTitle(getString(R.string.notification_service_title)).setOngoing(true)
				.setContentText(getString(R.string.notification_service_ready)).setContentIntent(pendingIntent)
				.setTicker(getString(R.string.notification_service_ticker));
		postNotification();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int action = intent.getIntExtra(EXTRA_ACTION_DESCRIPTOR, 0);
		if (action > 0) {
			Message msg = serviceHandler.obtainMessage();
			switch (action) {
			case MSG_DISPLAY_NOTIFICATION:
				msg.what = MSG_DISPLAY_NOTIFICATION;
				msg.arg1 = intent.getExtras().getBoolean(EXTRA_DISPLA_NTIFICATION) ? 1 : 0;
				break;
			case MSG_SENSOR_CONNECT_CHANGED:
				msg.what = MSG_SENSOR_CONNECT_CHANGED;
				msg.arg1 = intent.getExtras().getBoolean(EXTRA_SENSOR_CONNECT_CHANGED) ? 1 : 0;
				break;
			}
			serviceHandler.sendMessage(msg);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		notificationManager.cancel(notificationID);
	}

	private void postNotification() {
		if (preferences.getBoolean(getString(R.string.DISPLAY_NOTIFICATION), true)) {
			notificationManager.notify(notificationID, notificationBuilder.build());
		}
	}
}
