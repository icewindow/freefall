package net.icewindow.freefall.receiver;

import net.icewindow.freefall.service.DataAcquisitionService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(DataAcquisitionService.ACTION_INTENT);
		context.startService(service);
	}

}
