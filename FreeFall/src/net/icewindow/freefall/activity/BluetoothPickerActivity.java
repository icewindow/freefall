package net.icewindow.freefall.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.icewindow.freefall.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class BluetoothPickerActivity extends Activity {

	private List<Map<String, String>> listData;
	private ListAdapter listAdapter;
	private final String[] keys = new String[] { "name", "adress" };

	private SharedPreferences preferences;

	private BluetoothAdapter bluetoothAdapter;

	private final OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String deviceName = listData.get(position).get(keys[0]);
			String address = listData.get(position).get(keys[1]);
			preferences.edit().putString(getString(R.string.SELECTED_BLUETOOTH_ADDRESS), address).commit();
			bluetoothAdapter.cancelDiscovery();
			Toast.makeText(BluetoothPickerActivity.this,
					getString(R.string.text_bluetooth_selectedDevice, deviceName, address),
					Toast.LENGTH_SHORT).show();
			BluetoothPickerActivity.this.finish();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_bluetooth_picker);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter != null) {

			String[] keys = new String[] { "name", "adress" };
			listData = new ArrayList<Map<String, String>>();

			Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
			for (BluetoothDevice device : devices) {
				Map<String, String> map = new HashMap<String, String>(2);
				map.put(keys[0], device.getName());
				map.put(keys[1], device.getAddress());
				listData.add(map);
			}
			listAdapter = new SimpleAdapter(this, listData, android.R.layout.simple_list_item_2, keys, new int[] {
					android.R.id.text1, android.R.id.text2 });
		} else {
			listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
					new String[] { getString(R.string.text_bluetooth_noAdapter) });
		}

		ListView listView = (ListView) findViewById(R.id.bluetoothDeviceListView);
		listView.setAdapter(listAdapter);
		if (bluetoothAdapter != null) {
			listView.setOnItemClickListener(itemClickListener);
		}
	}

}
