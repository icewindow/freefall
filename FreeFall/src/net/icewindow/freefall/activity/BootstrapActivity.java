package net.icewindow.freefall.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BootstrapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);

		finish();
	}
}
