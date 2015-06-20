package net.icewindow.freefall.activity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.activity.model.ValueSet;
import net.icewindow.freefall.activity.view.RealtimeGraph;
import net.icewindow.freefall.bluetooth.BluetoothManager;
import net.icewindow.freefall.bluetooth.ConnectedDevice;
import net.icewindow.freefall.service.RealtimeDataMessageHandler;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class RealtimeDataActivity extends Activity {

	private final static String TAG = "Realtime Graph Activity";

	private RealtimeGraph graph;

	private BluetoothManager btmanager;

	private SharedPreferences preferences;

	private RealtimeDataMessageHandler rtdvt;

	private int graphX, graphY, graphZ, graphVector;

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
		graphX = graph.getModel().addValueSet(paint, "X");
		paint.setColor(0xff00ff00);
		graphY = graph.getModel().addValueSet(paint, "Y");
		paint.setColor(0xff0000ff);
		graphZ = graph.getModel().addValueSet(paint, "Z");
		paint.setColor(0xff1ce1ce);
		graphVector = graph.getModel().addValueSet(paint, "Vector");

		rtdvt = new RealtimeDataMessageHandler(graphX, graphY, graphZ, graphVector, graph.getModel(), this);
		Handler.Callback handlerCallback = rtdvt;
		Handler handler = new Handler(handlerCallback);
		btmanager = new BluetoothManager(handler);

		{
			Button b = (Button) findViewById(R.id.btn_connect);
			b.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(RealtimeDataActivity.this, R.string.text_connecting, Toast.LENGTH_SHORT).show();
					String address = preferences.getString(getString(R.string.SENSOR_ADDRESS), "");
					if (address.equals("")) {
						Toast.makeText(RealtimeDataActivity.this, R.string.text_connecting_failed, Toast.LENGTH_SHORT)
								.show();
						return;
					}
					BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
					btmanager.establishConnection(device);

				}
			});
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
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
		menu.findItem(R.id.menu_toggle_x).setChecked(
				preferences.getBoolean(getString(R.string.GRAPH_DRAW_X_GRID), false));
		menu.findItem(R.id.menu_toggle_y).setChecked(
				preferences.getBoolean(getString(R.string.GRAPH_DRAW_Y_GRID), true));
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
		if (isFinishing()) {
			rtdvt.setRunning(false);
			ArrayList<ConnectedDevice> devices = btmanager.getConnectedDevices();
			for (ConnectedDevice device : devices) {
				device.disconnect();
			}
		}
	}

	public void saveModelToFile() {
		File dataDir = new File(Environment.getExternalStorageDirectory(), "FreeFall");
		if (!dataDir.exists()) {
			dataDir.mkdir();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss", Locale.getDefault());
		Date date = new Date();
		File file = new File(dataDir, sdf.format(date) + ".json");

		ArrayList<ValueSet> valueSets = graph.getModel().getValueSets();
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
			for (ValueSet set : valueSets) {
				ArrayList<Double> values = set.getValues();
				if (set.isNamed()) {
					writer.name(set.getName());
				} else {
					writer.name("valueSet");
				}
				writer.beginArray();
				for (double value : values) {
					writer.value(value);
				}
				writer.endArray();
			}
			writer.endObject();
			Toast.makeText(getApplicationContext(), getString(R.string.text_saved, file.getName()), Toast.LENGTH_SHORT)
					.show();
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
