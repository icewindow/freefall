package net.icewindow.freefall.activity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.icewindow.freefall.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothPickerActivity extends Activity {

	public final static int REQUEST_BT_ENABLE = 10;

	private List<Map<String, String>> listData;
	private ListAdapter listAdapter;
	private final String[] keys = new String[] { "name", "adress" };
	private ListView listView;

	private SharedPreferences preferences;

	private BluetoothAdapter bluetoothAdapter;

	private boolean userAsked = false;

	private int discoveryDeviceCount = 0;

	private final OnItemClickListener itemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			bluetoothAdapter.cancelDiscovery();
			String address = listData.get(position).get(keys[1]);
			BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
			String deviceName = device.getName();
			if (device.getBondState() == BluetoothDevice.BOND_NONE) {
				try {
					Method m = BluetoothDevice.class.getMethod("createBond");
					m.invoke(device);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}

			Intent intent = null;
			if (preferences.getString(getString(R.string.SELECTED_BLUETOOTH_ADDRESS), "").equals("")) {
				intent = new Intent(BluetoothPickerActivity.this, MainActivity.class);
			}
			preferences.edit().putString(getString(R.string.SELECTED_BLUETOOTH_ADDRESS), address).commit();
			Toast.makeText(BluetoothPickerActivity.this,
					getString(R.string.text_bluetooth_selectedDevice, deviceName, address), Toast.LENGTH_SHORT).show();
			BluetoothPickerActivity.this.finish();
			if (intent != null) startActivity(intent);
		}
	};

	private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Map<String, String> map = new HashMap<String, String>(2);
				map.put(keys[0], "*" + device.getName());
				map.put(keys[1], device.getAddress());
				if (!listData.contains(map)) listData.add(map);
				discoveryDeviceCount++;
			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
				((Button) findViewById(R.id.button_discovery_start)).setText(R.string.button_discovery_discovering);
				discoveryDeviceCount = 0;
			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				((Button) findViewById(R.id.button_discovery_start)).setText(R.string.button_discovery_start);
				Toast.makeText(BluetoothPickerActivity.this,
						getString(R.string.text_discovery_devicecount, discoveryDeviceCount), Toast.LENGTH_SHORT)
						.show();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_bluetooth_picker);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(discoveryReceiver, filter);

		listView = (ListView) findViewById(R.id.bluetoothDeviceListView);
		{
			Button button = (Button) findViewById(R.id.button_discovery_start);
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					bluetoothAdapter.startDiscovery();
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (bluetoothAdapter != null) {
			buildList();
		} else {
			((TextView) findViewById(R.id.bluetoothDeviceMessageView)).setText(R.string.text_bluetooth_noAdapter);
		}

		// Ask the user to enable bluetooth, if it isn't enabled
		if (!bluetoothAdapter.isEnabled() && !userAsked) {
			userAsked = true;
			Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetoothIntent, REQUEST_BT_ENABLE);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Here is a good place to intercept the user's choice whether or not to enable bluetooth
		switch (requestCode) {
			case REQUEST_BT_ENABLE:
				switch (resultCode) {
					case RESULT_OK:
						buildList();
						break;
					case RESULT_CANCELED:
						((TextView) findViewById(R.id.bluetoothDeviceMessageView))
								.setText(R.string.text_bluetooth_enableBT);
						break;
					case RESULT_FIRST_USER:
						Toast.makeText(this, "I dunno lol ¯\\°.o/¯", Toast.LENGTH_SHORT).show();
				}
		}
	}

	private void buildList() {
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

		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(itemClickListener);
	}

}
