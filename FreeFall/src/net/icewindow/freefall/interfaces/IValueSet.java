package net.icewindow.freefall.interfaces;

import android.graphics.Paint;

public interface IValueSet {

	public void addValue(double value);
	
	public void deleteValueAtPosition(int position);
	
	public void deleteValue(double value);
	
	public void deleteValue(double value, boolean deleteAll);
	
	public void clearValues();
	
	public Object getValues();
	
	public double getScale();
	
	public Paint getPaint();
}
