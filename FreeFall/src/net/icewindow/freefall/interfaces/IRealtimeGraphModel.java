package net.icewindow.freefall.interfaces;

import android.graphics.Paint;

public interface IRealtimeGraphModel {

	/**
	 * Adds a value set to the graph model
	 * 
	 * @param paint
	 *            Paint to use for drawing
	 * @return Value set identifier (used for {@link #addValue(int, int)}
	 * @see #addValue(int, int)
	 */
	public void addValueSet(Paint paint, String name);

	/**
	 * Adds one data point to a value set
	 * 
	 * @param valueSetId
	 *            Value set identifier
	 * @param data
	 *            Data to be added to the value set
	 * @see #addValueSet(Class, Paint)
	 */
	public void addValue(String valueSetName, double data);

	/**
	 * Delete values in a given value set
	 * 
	 * @param valueSetId
	 */
	public void clearValues(String valueSetName);

	/**
	 * Removes a value set
	 * 
	 * @param valueSetId
	 */
	public void removeValueSet(String valueSetName);

	/**
	 * Commits the changes to the model (notifies observers)
	 */
	public void commit();

	/**
	 * @return The object holding the value sets
	 */
	public Object getValueSets();

	/**
	 * @return Maximal positive value saved by model
	 */
	public int getScalePositive();

	/**
	 * @return Maximal negative value saved by model
	 */
	public int getScaleNegative();

	/**
	 * @return X-axis scale
	 */
	public int getScaleX();
	
	/**
	 * @return X-axis width
	 */
	public int getWidth(); 

	/**Should not be 0 (zero)
	 * @return Where to start on the X axis, with 0 being the first data point
	 */
	public int getOffsetX();

	/**
	 * @return Whether or not to draw a grid marks in X-direction
	 */
	public boolean drawGridX();

	/**
	 * @return Whether or not to draw grid marks in Y-direction
	 */
	public boolean drawGridY();

	/**
	 * @return Whether or not to draw numbers along the X axis
	 */
	public boolean drawNumbersX();

	/**
	 * @return Whether or not to draw numbers along the Y axis
	 */
	public boolean drawNumbersY();

}
