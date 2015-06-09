package net.icewindow.freefall.activity.model;

import java.util.ArrayList;
import java.util.Observable;

import net.icewindow.freefall.interfaces.IRealtimeGraphModel;
import android.graphics.Paint;

public class RealtimeGraphModel extends Observable implements IRealtimeGraphModel {

	private ArrayList<ValueSet> valueSets;

	private int scalePositive;
	private int scaleNegative;
	private int scaleX, offsetX, width;
	private boolean drawNumbersX = false, drawNumbersY = true, drawGridX = true, drawGridY = true;

	private int lastValueSetId;

	public RealtimeGraphModel() {
		valueSets = new ArrayList<ValueSet>();
		lastValueSetId = 0;
		scalePositive = 1;
		scaleNegative = 1;
		width = 1000;
		scaleX = 10;
		offsetX = 0;
	}

	@Override
	public int addValueSet(Paint paint) {
		valueSets.add(new ValueSet(paint));
		return lastValueSetId++;
	}

	public int addValueSet(Paint paint, String name) {
		valueSets.add(new ValueSet(paint, name));
		return lastValueSetId++;
	}

	@Override
	public void addValue(int valueSetId, double data) {
		valueSets.get(valueSetId).addValue(data);
		if (data < 0) {
			if (Math.abs(data) > scaleNegative) {
				scaleNegative = (int) Math.ceil(Math.abs(data));
			}
		} else {
			if (data > scalePositive) {
				scalePositive = (int) Math.ceil(data);
			}
		}
	}

	@Override
	public void clearValues(int valueSetId) {
		valueSets.get(valueSetId).clearValues();
	}

	@Override
	public void removeValueSet(int valueSetId) {
		// TODO Remove value set
	}

	@Override
	public void commit() {
		setChanged();
		notifyObservers();
	}

	@Override
	public ArrayList<ValueSet> getValueSets() {
		return valueSets;
	}

	@Override
	public int getScalePositive() {
		return scalePositive;
	}

	public void setScalePositive(int scalePositive) {
		this.scalePositive = scalePositive;
	}

	@Override
	public int getScaleNegative() {
		return scaleNegative;
	}

	public void setScaleNegative(int scaleNegative) {
		this.scaleNegative = scaleNegative;
	}

	@Override
	public int getScaleX() {
		return scaleX;
	}

	public void setWidth(int width) {
		this.width = width;
		if (this.width < 1) this.width = 1;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getOffsetX() {
		return offsetX;
	};

	@Override
	public boolean drawGridX() {
		return drawGridX;
	}

	public void drawGridX(boolean drawGrid) {
		drawGridX = drawGrid;
	}

	@Override
	public boolean drawGridY() {
		return drawGridY;
	}

	public void drawGridY(boolean drawGrid) {
		drawGridY = drawGrid;
	}

	@Override
	public boolean drawNumbersX() {
		return drawNumbersX;
	}

	@Override
	public boolean drawNumbersY() {
		return drawNumbersY;
	}

	public void drawNumbersX(boolean drawNumbersX) {
		this.drawNumbersX = drawNumbersX;
	}

	public void drawNumbersY(boolean drawNumbersY) {
		this.drawNumbersY = drawNumbersY;
	}

	public void setOffsetX(int offsetX) {
		this.offsetX = offsetX;
		if (this.offsetX < 0) this.offsetX = 0;
	}

}
