package net.icewindow.freefall.activity.fragment;

import net.icewindow.freefall.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class BasicSettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.basic_preferences);
	}
}
