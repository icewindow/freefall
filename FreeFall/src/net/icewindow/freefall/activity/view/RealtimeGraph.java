package net.icewindow.freefall.activity.view;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.apache.http.client.CircularRedirectException;

import net.icewindow.freefall.activity.model.RealtimeGraphModel;
import net.icewindow.freefall.activity.model.ValueSet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.HeterogeneousExpandableList;

@SuppressWarnings("unused")
public class RealtimeGraph extends SurfaceView implements SurfaceHolder.Callback, Observer {

	/**
	 * LogCat tag
	 */
	private final String TAG = "Realtime Graph";

	/**
	 * Parent context
	 */
	private Context context;

	/**
	 * Canvas to draw on
	 */
	private SurfaceHolder surfaceHolder;

	/**
	 * Height of the drawable area
	 * 
	 * @see #surfaceChanged(SurfaceHolder, int, int, int)
	 */
	private int surfaceWidth;

	/**
	 * Width of the drawable area
	 * 
	 * @see #surfaceChanged(SurfaceHolder, int, int, int)
	 */
	private int surfaceHeight;

	/**
	 * Data model
	 */
	private RealtimeGraphModel model;

	/**
	 * Touch points
	 */
	private TouchPoint touch1, touch2;

	private int touchBeginOffset, touchBeginWidth;

	/**
	 * Helper class to store points
	 * 
	 * @see TouchPoint
	 * 
	 * @author icewindow
	 *
	 */
	private class Point {
		public float X, Y;

		public Point(float x, float y) {
			this.X = x;
			this.Y = y;
		}

		public double distance() {
			return Math.sqrt(X * X + Y * Y);
		}

		public double distance(Point other) {
			return distance() - other.distance();
		}

		public double distance(float onePoint, float anotherPoint) {
			return Math.sqrt(onePoint * onePoint + anotherPoint * anotherPoint);
		}

		@Override
		public String toString() {
			return X + " / " + Y;
		}
	}

	/**
	 * Helper class to store touch points
	 * 
	 * @author icewindow
	 *
	 */
	private class TouchPoint extends Point {

		public final int id;
		public Point previous;
		public Point initial;

		public TouchPoint(float x, float y, int pointerID) {
			super(x, y);
			previous = new Point(x, y);
			initial = new Point(x, y);
			id = pointerID;
		}

		public double distanceFromInitial() {
			return distance(initial);
		}

		public double distanceFromPrevious() {
			return distance(previous);
		}

		public double distanceFromPreviousX() {
			return distance(X, previous.X);
		}

		public double distanceFromInitialX() {
			return distance(X, initial.X);
		}

	}

	/**
	 * Creates the View
	 * 
	 * @param context
	 */
	public RealtimeGraph(Context context) {
		this(context, null);
	}

	/**
	 * Creates the View
	 * 
	 * @param context
	 * @param attribs
	 */
	public RealtimeGraph(Context context, AttributeSet attribs) {
		super(context, attribs);
		this.context = context;

		model = new RealtimeGraphModel();
		model.addObserver(this);

		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);

		setFocusable(true);
		setFocusableInTouchMode(true);

		Log.d(TAG, "Graph created");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "Surface changed");
		synchronized (holder) {
			this.surfaceHeight = height;
			this.surfaceWidth = width;
			updateView();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Surface created");
		updateView();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Surface destroyed");
	}

	@Override
	public void update(Observable observable, Object data) {
		updateView();
	}

	@Override
	public boolean performClick() {
		return super.performClick();
	};

	private void drawReticle(Canvas canvas, Point point, Paint paint) {
		drawReticle(canvas, (int) Math.round(point.X), (int) Math.round(point.Y), paint);
	}

	private void drawReticle(Canvas canvas, int offsetX, int offsetY, Paint paint) {
		Rect r = new Rect();

		paint.setStyle(Paint.Style.STROKE);
		canvas.drawCircle(offsetX, offsetY, 5, paint);
		r.set(offsetX - 40, offsetY - 40, offsetX + 40, offsetY + 40);
		canvas.drawArc(new RectF(r), 0.0f, 360.0f, false, paint);

		r.set(offsetX - 25, offsetY - 25, offsetX - 24, offsetY + 25);
		canvas.drawRect(r, paint);
		r.set(offsetX - 25, offsetY - 25, offsetX + 25, offsetY - 24);
		canvas.drawRect(r, paint);
		r.set(offsetX - 25, offsetY + 25, offsetX + 25, offsetY + 24);
		canvas.drawRect(r, paint);
		r.set(offsetX + 25, offsetY - 25, offsetX + 24, offsetY + 25);

		canvas.drawRect(r, paint);
		r.set(offsetX, offsetY - 25, offsetX + 1, offsetY + 25);
		canvas.drawRect(r, paint);
		r.set(offsetX - 25, offsetY, offsetX + 25, offsetY + 1);
		canvas.drawRect(r, paint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		int pointerIndex = MotionEventCompat.getActionIndex(event);
		float x = MotionEventCompat.getX(event, pointerIndex);
		float y = MotionEventCompat.getY(event, pointerIndex);
		StringBuilder d = new StringBuilder();
		switch (MotionEventCompat.getActionMasked(event)) {
			case MotionEvent.ACTION_MOVE:
				// TODO Fix scrolling... That is, get it working again
				// Canvas canvas = null;
				// try {
				// canvas = surfaceHolder.lockCanvas();
				// synchronized (surfaceHolder) {
				// Rect r = new Rect();
				// Paint paint = new Paint();
				// paint.setColor(0xff000000);
				//
				// int directionMultiplier = 0;
				// if (x > previousX) {
				// directionMultiplier = -1;
				// } else if (x < previousX) {
				// directionMultiplier = 1;
				// }
				// model.setOffsetX(model.getOffsetX() + ((int) ((x - beginX)) / 50) * directionMultiplier);
				//
				// doDraw(canvas);
				//
				// drawRecticle(canvas, (int) x, (int) y, paint);
				//
				// paint.setColor(0xffff0000);
				// drawRecticle(canvas, (int) beginX, (int) beginY, paint);
				//
				// paint.setColor(0xff0000ff);
				// drawRecticle(canvas, (int) previousX, (int) previousY, paint);
				// }
				// } finally {
				// if (canvas != null) {
				// surfaceHolder.unlockCanvasAndPost(canvas);
				// }
				// }
				// previousX = event.getX();
				// previousY = event.getY();
				touch1.previous.X = touch1.X;
				touch1.previous.Y = touch1.Y;
				int touch1Pointer = event.findPointerIndex(touch1.id);
				touch1.X = event.getX(touch1Pointer);
				touch1.Y = event.getY(touch1Pointer);

				if (touch2 != null) {
					touch2.previous.X = touch2.X;
					touch2.previous.Y = touch2.Y;
					int touch2Pointer = event.findPointerIndex(touch2.id);
					touch2.X = event.getX(touch2Pointer);
					touch2.Y = event.getY(touch2Pointer);

					double zoomFactor = Math.abs(touch1.distance(touch2));// * model.getWidth() / model.getScaleX();
					d.append("Zoom: ").append(zoomFactor).append("\nWidth: ").append(model.getWidth()).append("\n");
					if (touch1.distance(touch2) > touch1.previous.distance(touch2.previous)) {
						// Zooming in
						model.setWidth(touchBeginWidth - (int) (zoomFactor));
					} else {
						// Zooming out
						model.setWidth(touchBeginWidth + (int) (zoomFactor));
					}
				} else {
					// Set pointer to new origin, if necessary
					if (touch1.X < touch1.initial.X && touch1.X > touch1.previous.X) {
						// Left side of pointer and going towards initial point
						touch1.initial.X = touch1.X;
						touch1.initial.Y = touch1.Y;
					} else if (touch1.X > touch1.initial.X && touch1.X < touch1.previous.X) {
						// Right side of pointer and going towards initial point
						touch1.initial.X = touch1.X;
						touch1.initial.Y = touch1.Y;

					}
					int offsetAdjust = (int) (touch1.initial.X - touch1.X) / 10; // * model.getWidth() /
																					// model.getScaleX();
					model.setOffsetX(touchBeginOffset + offsetAdjust);
					d.append("Offset: ").append(model.getOffsetX()).append("\n");
				}
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				touch2 = new TouchPoint(event.getX(1), event.getY(1), event.getPointerId(1));
				touchBeginWidth = model.getWidth();
				break;
			case MotionEvent.ACTION_POINTER_UP:
				int id = event.findPointerIndex(pointerIndex);
				Log.d(TAG, "Action up on " + pointerIndex + " (ID: " + id + ")");
				if (id == touch1.id) {
					touch1 = touch2;
				}
				touch2 = null;
				break;
			case MotionEvent.ACTION_DOWN:
				// TODO Implement multi-touch (pinch to zoom in/out of diagram (change model scale))
				touch1 = new TouchPoint(event.getX(), event.getY(), event.getPointerId(0));
				model.drawNumbersX(true);
				touchBeginOffset = model.getOffsetX();
				break;
			case MotionEvent.ACTION_UP:
				if (Math.abs(touch1.distanceFromInitial()) > 5) {
					performClick();
				}
				touch1 = null;
				model.drawNumbersX(false);
				updateView();
				break;
		}
		// Draw reticles (if any)
		Canvas canvas = null;
		try {
			canvas = surfaceHolder.lockCanvas();
			synchronized (surfaceHolder) {
				doDraw(canvas);
				Paint paint = new Paint();
				if (touch1 != null) {
					d.append("Pointer 1: ").append(touch1).append("\n");
					paint.setColor(0xffff0000);
					drawReticle(canvas, touch1.initial, paint);

					paint.setColor(0xff00ff00);
					drawReticle(canvas, touch1.previous, paint);

					paint.setColor(0xff000000);
					drawReticle(canvas, touch1, paint);
				}
				if (touch2 != null) {
					d.append("Pointer 2: ").append(touch2).append("\n");
					paint.setColor(0xffff00ff);
					drawReticle(canvas, touch2.initial, paint);

					paint.setColor(0xff00ffff);
					drawReticle(canvas, touch2.previous, paint);

					paint.setColor(0xff0000ff);
					drawReticle(canvas, touch2, paint);
				}
				String[] lines = d.toString().split("\n");
				for (int i = 0; i < lines.length; i++) {
					canvas.drawText(lines[i], 30, 30 + i * 10, paint);
				}
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
		return true;
	}

	/**
	 * Wrapper call to {@code doDraw()}
	 * 
	 * @see #doDraw(Canvas)
	 */
	private void updateView() {
		Canvas canvas = null;
		try {
			canvas = surfaceHolder.lockCanvas();
			synchronized (surfaceHolder) {
				doDraw(canvas);
			}
		} finally {
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	/**
	 * Draws the graph to the surface
	 * 
	 * @param canvas
	 */
	private void doDraw(Canvas c) {
		if (c == null) {
			Log.e(TAG, "Canvas not yet initialized!");
			return;
		}
		Rect r = new Rect();
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);

		// Basic information
		int diagramStartX = 10;
		int diagramStartY = 10;
		int diagramEndX = surfaceWidth - 10;
		int diagramEndY = surfaceHeight - 10;
		int diagramHeight = diagramEndY - diagramStartY;
		int diagramWidth = diagramEndX - diagramStartX;

		// Clear canvas
		c.drawPaint(paint);

		// Draw diagram backdrop
		paint.setColor(0xffdddddd);
		r.set(diagramStartX, diagramStartY, diagramEndX, diagramEndY);
		c.drawRect(r, paint);

		// Scale
		int scalePositive = model.getScalePositive();
		int scaleNegative = model.getScaleNegative();
		int scaleY = scalePositive + scaleNegative;
		int majorCount = model.getWidth() / model.getScaleX();
		double unitWidth = (double) diagramWidth / model.getWidth();
		double macroUnitWidth = (double) diagramWidth / majorCount;
		double unitHeight = (double) diagramHeight / scaleY;

		paint.setColor(0xffaaaaaa);

		c.save();
		r.set(diagramStartX, diagramStartY, diagramEndX, diagramEndY);

		// Draw X markers
		if (model.drawGridX()) {
			for (int i = 1; i < majorCount + 1; i++) {
				int line = (int) Math
						.round(diagramStartX + i * macroUnitWidth - model.getOffsetX() % model.getScaleX());
				r.set(line, diagramStartY, line + 1, diagramEndY);
				c.drawRect(r, paint);
			}
		}

		// Draw Y markers
		if (model.drawGridY()) {
			for (int i = 0; i < scaleY + 1; i++) {
				int line = (int) Math.round(diagramStartY + i * unitHeight);
				r.set(diagramStartX - 5, line, diagramEndX, line + 1);
				c.drawRect(r, paint);
			}
		}

		// Draw Y axis
		paint.setColor(0xff777777);
		r.set(diagramStartX, diagramStartY, diagramStartX + 1, diagramEndY);
		c.drawRect(r, paint);

		// Draw baseline (X axis)
		int baseline = (int) Math.round(diagramStartY + unitHeight * scalePositive);
		r.set(diagramStartX - 5, baseline, diagramEndX, baseline + 1);
		c.drawRect(r, paint);

		if (model.drawNumbersX()) {
			c.clipRect(r);
			for (int i = 0; i < majorCount - 1; i++) {
				c.drawText("" + i,
						(float) (diagramStartX + i * macroUnitWidth - model.getOffsetX() % model.getScaleX()) + 2.0f,
						baseline + diagramStartX + 2, paint);
			}
		}

		if (model.drawNumbersY()) {
			for (int i = 1, j = scalePositive - 1; i < scaleY + 1; i++, j--) {
				c.drawText("" + j, diagramStartY + 2, (float) (diagramStartY + i * unitHeight) - 2.0f, paint);
			}
		}

		c.restore();

		ArrayList<ValueSet> valueSets = model.getValueSets();

		for (ValueSet set : valueSets) {
			double scale = set.getScale();
			paint.set(set.getPaint());
			ArrayList<Double> values = set.getValues();
			for (int i = model.getOffsetX(), j = 0; i < values.size(); i++, j++) {
				if (i > model.getOffsetX() + model.getWidth()) break;
				float startX, startY;
				float endX = diagramStartX + j * (float) unitWidth;
				float endY = baseline - (float) (values.get(i) * scale * unitHeight);
				if (j == 0) {
					startX = endX;
					startY = endY;
				} else {
					startX = diagramStartX + (j - 1) * (float) unitWidth;
					startY = baseline - (float) (values.get(i - 1) * scale * unitHeight);
				}
				c.drawLine(startX, startY, endX, endY, paint);
			}
		}
	}

	public RealtimeGraphModel getModel() {
		return model;
	}

}
