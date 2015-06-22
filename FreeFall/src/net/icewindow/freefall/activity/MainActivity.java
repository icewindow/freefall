package net.icewindow.freefall.activity;

import net.icewindow.freefall.R;
import net.icewindow.freefall.service.FreefallService;
import net.icewindow.freefall.service.bluetooth.BluetoothClient;
import net.icewindow.freefall.service.bluetooth.ConnectedDevice;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	private final static class ConnectHandler extends Handler {
		private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == FreefallService.MSG_BT_CLIENT_READY) {
				ConnectedDevice server = (ConnectedDevice) msg.obj;
				server.write(adapter.getAddress());
				server.disconnect();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		{
			Intent intent = new Intent(FreefallService.INTENT_NAME);
			startService(intent);
		}

		// if (preferences.getString(getString(R.string.SETUP_COMPLETE), "").equals("")) {
		// Intent intent = new Intent(this, FirstSetupActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
		// startActivity(intent);
		// }

		if (preferences.getString(getString(R.string.SENSOR_ADDRESS), "").equals("")) {
			Intent intent = new Intent(this, BluetoothPickerActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		}

		{
			Button btn = (Button) findViewById(R.id.btn_main_rtdv);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainActivity.this, RealtimeDataActivity.class);
					startActivity(intent);
				}
			});
		}
		{
			Button btn = (Button) findViewById(R.id.btn_clear);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					preferences.edit().clear().commit();
					finish();
				}
			});
		}
		{
			Button btn = (Button) findViewById(R.id.btn_stop_service);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(FreefallService.INTENT_NAME);
					stopService(intent);
				}
			});
		}
		{
			Button btn = (Button) findViewById(R.id.btn_mailtest);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(FreefallService.INTENT_NAME);
					intent.putExtra(FreefallService.EXTRA_ACTION_DESCRIPTOR, FreefallService.ACTION_SEND_MAIL);
					intent.putExtra(FreefallService.EXTRA_EMAIL_BODY, "Freefall test mail");
					intent.putExtra(FreefallService.EXTRA_EMAIL_SUBJECT, "Freefall Test message");
					startService(intent);
				}
			});
		}
		{
			Button btn = (Button) findViewById(R.id.btn_send_ping);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final Handler handler = new ConnectHandler();
					BluetoothClient client = new BluetoothClient(handler);
					try {
						BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
								preferences.getString(getString(R.string.SENSOR_ADDRESS), ""));
						client.connectToServer(device);
					} catch (IllegalArgumentException e) {
					}
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
	}

}
