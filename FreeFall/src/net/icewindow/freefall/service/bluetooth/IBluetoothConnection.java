package net.icewindow.freefall.service.bluetooth;

public interface IBluetoothConnection {

	public void write(String data);
	
	public ConnectedDevice getRemoteDevice();
	
}
