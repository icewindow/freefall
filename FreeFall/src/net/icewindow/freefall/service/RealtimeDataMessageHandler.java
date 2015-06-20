package net.icewindow.freefall.service;

import net.icewindow.freefall.R;
import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * @deprecated
 * @author icewindow
 *
 */
public class RealtimeDataMessageHandler implements Handler.Callback {

	private final String TAG = "Realtime Data Thread";

	private RealtimeGraphModel model;
	private int x, y, z, v;
	private boolean running;
	private Context context;

	public RealtimeDataMessageHandler(int graphX, int graphY, int graphZ, int graphVector,
			RealtimeGraphModel dataModel, Context context) {
		x = graphX;
		y = graphY;
		z = graphZ;
		v = graphVector;
		model = dataModel;
		running = true;
		this.context = context;
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case BluetoothManager.BT_MESSAGE:
				String message = (String) msg.obj;
				// if (!message.matches("([\\d\\.-]*,{0,1}){4}")) break;
				String[] parts = message.split(",");
				try {
					float vx = Float.parseFloat(parts[0]);
					float vy = Float.parseFloat(parts[1]);
					float vz = Float.parseFloat(parts[2]);
					float vv = Float.parseFloat(parts[3]);
//					model.addValue(x, vx);
//					model.addValue(y, vy);
//					model.addValue(z, vz);
//					model.addValue(v, vv);
					model.commit();
				} catch (Exception e) {
				}
				break;
			case BluetoothManager.BT_CLIENT_CONNECT_CHANGE:
				String device = ((android.bluetooth.BluetoothSocket) msg.obj).getRemoteDevice().getName();
				switch (msg.arg1) {
					case BluetoothManager.BT_CLIENT_CONNECT:
						Toast.makeText(context, context.getString(R.string.text_connecting_connected, device),
								Toast.LENGTH_SHORT).show();
						break;
					case BluetoothManager.BT_CLIENT_DISCONNECT:
						Toast.makeText(context, context.getString(R.string.text_connecting_disconnected, device),
								Toast.LENGTH_SHORT).show();
						break;
				}
				break;

		}
		return true;
	}

	public void setRunning(boolean run) {
		running = run;
		if (!running) {
			Log.d(TAG, "Thread is scheduled to finish");
		}
	}

}
