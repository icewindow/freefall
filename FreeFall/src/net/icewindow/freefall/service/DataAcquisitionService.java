package net.icewindow.freefall.service;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DataAcquisitionService extends Service {

	private NotificationCompat.Builder notificationBuilder;
	private NotificationManager notificationManager;
	private int notificationID = 0x1CEC0DE;
	private HandlerThread backgroundThread;
	private ServiceHandler serviceHandler;
	private Looper serviceLooper;
	
	private RealtimeGraphModel model;

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Toast.makeText(DataAcquisitionService.this, "", Toast.LENGTH_SHORT).show();
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

		serviceLooper = backgroundThread.getLooper();
		serviceHandler = new ServiceHandler(serviceLooper);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
				.setSmallIcon(R.drawable.ic_freefall_service)
				.setContentTitle(getString(R.string.notification_service_title)).setOngoing(true)
				.setContentText(getString(R.string.notification_service_ready));
		notificationManager.notify(notificationID, notificationBuilder.build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		notificationBuilder.setContentText(getString(R.string.notification_service_running));
		notificationManager.notify(notificationID, notificationBuilder.build());
		Message msg = serviceHandler.obtainMessage();
		serviceHandler.sendMessage(msg);
		
		
		
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		notificationManager.cancel(notificationID);
	}

}
