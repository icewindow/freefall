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
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
				preferenceChangedListener);
	}

	OnSharedPreferenceChangeListener preferenceChangedListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(getString(R.string.DISPLAY_NOTIFICATION))) {
				Intent intent = new Intent(FreefallService.ACTION_INTENT);
				intent.putExtra(FreefallService.EXTRA_ACTION_DESCRIPTOR,
						FreefallService.MSG_DISPLAY_NOTIFICATION);
				intent.putExtra(FreefallService.EXTRA_DISPLA_NOTIFICATION,
						sharedPreferences.getBoolean(key, true));
				startService(intent);
			}
		}
	};

}
