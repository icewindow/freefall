package net.icewindow.freefall.activity;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.fragment.BasicSettingsFragment;
import net.icewindow.freefall.service.FreefallService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction().replace(android.R.id.content, new BasicSettingsFragment()).commit();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangedListener);
	}

	OnSharedPreferenceChangeListener preferenceChangedListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(getString(R.string.DISPLAY_NOTIFICATION))) {
				Intent intent = new Intent(FreefallService.INTENT_NAME);
				intent.putExtra(FreefallService.EXTRA_ACTION_DESCRIPTOR, FreefallService.ACTION_CHANGE_NOTIFICATION_DISPLAY);
				intent.putExtra(FreefallService.EXTRA_DISPLAY_NOTIFICATION, sharedPreferences.getBoolean(key, true));
				startService(intent);
			} else if (key.equals(getString(R.string.SENSOR_ACTIVE))) {
				Intent intent = new Intent(FreefallService.INTENT_NAME);
				intent.putExtra(FreefallService.EXTRA_ACTION_DESCRIPTOR, FreefallService.ACTION_SENSOR_TYPE_CHANGE);
				intent.putExtra(FreefallService.EXTRA_SENSOR_TYPE, sharedPreferences.getBoolean(key, false));
				startService(intent);
			}
		}
	};

}
