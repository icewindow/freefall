package net.icewindow.freefall.activity;

import net.icewindow.freefall.R;
import net.icewindow.freefall.service.DataAcquisitionService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		if (preferences.getString(getString(R.string.SELECTED_BLUETOOTH_ADDRESS), "").equals("")) {
			Intent intent = new Intent(this, BluetoothPickerActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
		}
		
		{
			Intent intent = new Intent(DataAcquisitionService.ACTION_INTENT);
			startService(intent);
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
					Intent intent = new Intent(DataAcquisitionService.ACTION_INTENT);
					stopService(intent);
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
