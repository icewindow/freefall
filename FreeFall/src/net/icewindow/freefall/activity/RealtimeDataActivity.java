package net.icewindow.freefall.activity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.activity.model.ValueSet;
import net.icewindow.freefall.activity.view.RealtimeGraph;
import net.icewindow.freefall.service.FreefallService;
import net.icewindow.freefall.service.FreefallService.ServiceBinder;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class RealtimeDataActivity extends Activity {

	private final static String TAG = "Realtime Graph Activity";

	private RealtimeGraph graph;

	private SharedPreferences preferences;

	private ServiceBinder binder;

	public static final String graphX = "X", graphY = "Y", graphZ = "Z", graphVector = "Vector";

	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = (ServiceBinder) service;
			binder.attachModel(graph.getModel());
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_realtime_data);

		graph = (RealtimeGraph) findViewById(R.id.graph);

		// preferences = getSharedPreferences(getString(R.string.PREFERENCE_FILE_KEY), MODE_PRIVATE);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		graph.getModel().drawGridX(preferences.getBoolean(getString(R.string.GRAPH_DRAW_X_GRID), false));
		graph.getModel().drawGridY(preferences.getBoolean(getString(R.string.GRAPH_DRAW_Y_GRID), true));

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Paint paint = new Paint();
		paint.setColor(0xffff0000);
		graph.getModel().addValueSet(paint, graphX);
		paint.setColor(0xff00ff00);
		graph.getModel().addValueSet(paint, graphY);
		paint.setColor(0xff0000ff);
		graph.getModel().addValueSet(paint, graphZ);
		paint.setColor(0xff1ce1ce);
		graph.getModel().addValueSet(paint, graphVector);

		Intent intent = new Intent(FreefallService.INTENT_NAME);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.realtime_data, menu);
		menu.findItem(R.id.menu_toggle_x).setChecked(preferences.getBoolean(getString(R.string.GRAPH_DRAW_X_GRID), false));
		menu.findItem(R.id.menu_toggle_y).setChecked(preferences.getBoolean(getString(R.string.GRAPH_DRAW_Y_GRID), true));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		RealtimeGraphModel model = graph.getModel();
		SharedPreferences.Editor edit = preferences.edit();
		switch (item.getItemId()) {
			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.menu_save:
				saveModelToFile();
				return true;
			case R.id.menu_clear:
				model.clearValues(graphX);
				model.clearValues(graphY);
				model.clearValues(graphZ);
				model.clearValues(graphVector);
				model.setScaleNegative(1);
				model.setScalePositive(1);
				model.commit();
				return true;
			case R.id.menu_toggle_x:
				model.drawGridX(!model.drawGridX());
				item.setChecked(model.drawGridX());
				model.commit();
				edit.putBoolean(getString(R.string.GRAPH_DRAW_X_GRID), model.drawGridX());
				edit.commit();
				return true;
			case R.id.menu_toggle_y:
				model.drawGridY(!model.drawGridY());
				item.setChecked(model.drawGridY());
				model.commit();
				edit.putBoolean(getString(R.string.GRAPH_DRAW_Y_GRID), model.drawGridY());
				edit.commit();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
	}

	public void saveModelToFile() {
		File dataDir = new File(Environment.getExternalStorageDirectory(), "FreeFall");
		if (!dataDir.exists()) {
			dataDir.mkdir();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss", Locale.getDefault());
		Date date = new Date();
		File file = new File(dataDir, sdf.format(date) + ".json");

		Map<String, ValueSet> valueSets = graph.getModel().getValueSets();
		FileWriter fileWriter = null;
		BufferedWriter bufferedWriter = null;
		JsonWriter writer = null;
		Toast.makeText(getApplicationContext(), R.string.text_saving, Toast.LENGTH_SHORT).show();
		try {
			fileWriter = new FileWriter(file);
			bufferedWriter = new BufferedWriter(fileWriter);
			writer = new JsonWriter(bufferedWriter);
			writer.setIndent("  ");
			writer.beginObject();
			for (String key : valueSets.keySet()) {
				ValueSet set = valueSets.get(key);
				writer.name(key);
				writer.beginArray();
				for (double value : set.getValues()) {
					writer.value(value);
				}
				writer.endArray();
			}
			writer.endObject();
			Toast.makeText(getApplicationContext(), getString(R.string.text_saved, file.getName()), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), R.string.text_saving_error, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error writing to file!!", e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
				}
			}
			if (fileWriter != null) {
				try {
					fileWriter.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
