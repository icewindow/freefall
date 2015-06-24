package net.icewindow.freefall.activity;

import net.icewindow.freefall.R;
import net.icewindow.freefall.service.FreefallService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	private Button connectBtn;

	private final BroadcastReceiver connectStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getIntExtra(FreefallService.BROADCAST_EXTRA_SENSOR_CONNECTION_STATE, FreefallService.STATE_OFFLINE)) {
				case FreefallService.STATE_CONNECTED:
					connectBtn.setText(R.string.button_main_connected);
					connectBtn.setOnClickListener(null);
					break;
				case FreefallService.STATE_CONNECTING:
					connectBtn.setText(R.string.button_main_connecting);
					connectBtn.setOnClickListener(null);
					break;
				default:
					connectBtn.setText(R.string.button_main_connect);
					connectBtn.setOnClickListener(connectButtonOnClickListener);
					break;
			}
		}
	};

	private final View.OnClickListener connectButtonOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent service = new Intent(FreefallService.INTENT_NAME);
			service.putExtra(FreefallService.EXTRA_ACTION_DESCRIPTOR, FreefallService.ACTION_CONNECT_SENSOR);
			startService(service);
			connectBtn.setText(R.string.button_main_connecting);
		}
	};

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

		connectBtn = (Button) findViewById(R.id.btn_send_ping);
		connectBtn.setOnClickListener(connectButtonOnClickListener);

		IntentFilter filter = new IntentFilter(FreefallService.BROADCAST_SENSOR_CONNECTION_STATE);
		registerReceiver(connectStateReceiver, filter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
			case R.id.action_settings:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_start_graph:
				intent = new Intent(MainActivity.this, RealtimeDataActivity.class);
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(connectStateReceiver);
	}

}
