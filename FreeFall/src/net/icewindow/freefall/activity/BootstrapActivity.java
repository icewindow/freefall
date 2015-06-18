package net.icewindow.freefall.activity;

import net.icewindow.freefall.R;
import net.icewindow.freefall.service.FreefallService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class BootstrapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);

		finish();
	}
}
