package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.teamclouday.androidsteering.R;

public class SteeringWheelView extends View {

    private Bitmap wheelBitmap;
    private RectF destRect;

    private float currentAngleDeg = 0f;
    private float maxRotationDeg = 180f;
    private float prevTouchAngle = 0f;
    private boolean isTracking = false;

    public interface OnSteeringChangeListener {
        void onSteeringChanged(float normalizedValue);
    }
    private OnSteeringChangeListener listener;

    public SteeringWheelView(Context context) { super(context); init(); }
    public SteeringWheelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.pngtree_wheel);
        if (original != null) {
            wheelBitmap = original;
        }
        destRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f, cy = h / 2f;
        // Calculate radius to fit nicely inside the view
        float radius = Math.min(w, h) / 2f - 10f;
        
        destRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
    }

    public void setOnSteeringChangeListener(OnSteeringChangeListener l) { this.listener = l; }
    public void setMaxRotationDegrees(float deg) { this.maxRotationDeg = deg; }
    public float getMaxRotationDeg() { return maxRotationDeg; }
    public float getSteeringAngle() { return currentAngleDeg / maxRotationDeg; }

    public void setSensorAngle(float degrees) {
        if (!isTracking) {
            this.currentAngleDeg = degrees;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float radius = Math.min(w, h) / 2f - 10f;

        canvas.save();
        canvas.rotate(currentAngleDeg, cx, cy);

        // Draw the natively loaded wheel bitmap
        if (wheelBitmap != null) {
            canvas.drawBitmap(wheelBitmap, null, destRect, null);
        }

        canvas.restore();

        // BACKGROUND GUIDELINE
        Paint centerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerLinePaint.setColor(Color.parseColor("#3371797E"));
        centerLinePaint.setStrokeWidth(2f);
        canvas.drawLine(cx, cy - radius - 20f, cx, cy - radius + 5f, centerLinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float touchX = event.getX(), touchY = event.getY();
        float touchAngle = (float) Math.toDegrees(Math.atan2(touchY - cy, touchX - cx));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                prevTouchAngle = touchAngle;
                isTracking = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isTracking) {
                    float delta = touchAngle - prevTouchAngle;
                    if (delta > 180f) delta -= 360f;
                    if (delta < -180f) delta += 360f;
                    currentAngleDeg = Math.max(-maxRotationDeg, Math.min(maxRotationDeg, currentAngleDeg + delta));
                    prevTouchAngle = touchAngle;
                    invalidate();
                    if (listener != null) listener.onSteeringChanged(getSteeringAngle());
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTracking = false;
                animateBackToCenter();
                return true;
        }
        return false;
    }

    public void animateBackToCenter() {
        post(new Runnable() {
            @Override
            public void run() {
                if (Math.abs(currentAngleDeg) < 0.5f) {
                    currentAngleDeg = 0f;
                    invalidate();
                    if (listener != null) listener.onSteeringChanged(0f);
                    return;
                }
                currentAngleDeg *= 0.85f;
                invalidate();
                if (listener != null) listener.onSteeringChanged(getSteeringAngle());
                postDelayed(this, 16);
            }
        });
    }
}