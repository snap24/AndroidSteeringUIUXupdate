package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SteeringWheelView extends View {

    private Paint rimPaint;
    private Paint hubPaint;
    private Paint spokePaint;
    private Paint gripPaint;
    private Paint indicatorPaint;

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
        rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setColor(Color.parseColor("#71797E"));
        rimPaint.setStrokeWidth(18f);

        hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setStyle(Paint.Style.FILL);
        hubPaint.setColor(Color.parseColor("#1A1A1A"));

        spokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spokePaint.setStyle(Paint.Style.STROKE);
        spokePaint.setColor(Color.parseColor("#71797E"));
        spokePaint.setStrokeWidth(8f);
        spokePaint.setStrokeCap(Paint.Cap.ROUND);

        gripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gripPaint.setStyle(Paint.Style.STROKE);
        gripPaint.setColor(Color.parseColor("#71797E"));
        gripPaint.setStrokeWidth(22f);
        gripPaint.setStrokeCap(Paint.Cap.ROUND);

        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setStyle(Paint.Style.FILL);
        indicatorPaint.setColor(Color.WHITE);
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
        float radius = Math.min(w, h) / 2f - 30f;
        float hubRadius = radius * 0.25f;

        canvas.save();
        canvas.rotate(currentAngleDeg, cx, cy);

        // 1. THICK CIRCULAR RIM (Increased pipe size)
        rimPaint.setStrokeWidth(35f);
        canvas.drawCircle(cx, cy, radius, rimPaint);

        // 2. CLASSIC 3-SPOKE DESIGN
        spokePaint.setStrokeWidth(18f);
        float[] spokeAngles = {90, 210, 330}; // 6 o'clock, 10 o'clock, 2 o'clock
        for (float angle : spokeAngles) {
            double angleRad = Math.toRadians(angle);
            float sx = cx + (float)(hubRadius * Math.cos(angleRad));
            float sy = cy + (float)(hubRadius * Math.sin(angleRad));
            float ex = cx + (float)(radius * 0.98f * Math.cos(angleRad));
            float ey = cy + (float)(radius * 0.98f * Math.sin(angleRad));
            canvas.drawLine(sx, sy, ex, ey, spokePaint);
        }

        // 3. SMOOTH CENTRAL HUB
        canvas.drawCircle(cx, cy, hubRadius, hubPaint);
        Paint hubRim = new Paint(rimPaint);
        hubRim.setStrokeWidth(3f);
        canvas.drawCircle(cx, cy, hubRadius, hubRim); // Subtle border

        // 4. PRECISION CENTER DOT (Indicator)
        canvas.drawCircle(cx, cy, 6f, indicatorPaint);

        // 5. TOP CENTER MARKER (Kept for steering alignment)
        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(Color.parseColor("#E0E0E0"));
        markerPaint.setStrokeWidth(22f);
        RectF rimRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(rimRect, 268f, 4f, false, markerPaint);

        canvas.restore();

        // 6. GUIDELINE
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