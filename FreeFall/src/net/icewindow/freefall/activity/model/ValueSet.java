package net.icewindow.freefall.activity.model;

import java.util.ArrayList;

import android.graphics.Paint;
import net.icewindow.freefall.interfaces.IValueSet;

public class ValueSet implements IValueSet {

	private ArrayList<Double> values;

	private double scale;

	private Paint paint;

	public ValueSet(Paint paint) {
		values = new ArrayList<Double>();
		scale = 1.0d;
		this.paint = new Paint(paint);
	}

	@Override
	public void addValue(double value) {
		values.add(value);
	}

	@Override
	public void deleteValueAtPosition(int position) {
		values.remove(position);
	}

	@Override
	public void deleteValue(double value) {
		deleteValue(value, false);
	}

	@Override
	public void deleteValue(double value, boolean deleteAll) {
		if (deleteAll) {
			while (values.remove(value)) {
			}
		} else {
			values.remove(value);
		}
	}

	@Override
	public void clearValues() {
		values.clear();
	}

	@Override
	public ArrayList<Double> getValues() {
		return values;
	}

	@Override
	public double getScale() {
		return scale;
	}

	@Override
	public Paint getPaint() {
		return paint;
	}
	
}
