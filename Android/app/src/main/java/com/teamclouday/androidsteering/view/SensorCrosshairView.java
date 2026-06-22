package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SensorCrosshairView extends View {

    private Paint bgPaint;
    private Paint crosshairPaint;
    private Paint dotPaint;

    private float rollPercent = 50f;
    private float pitchPercent = 50f;

    public SensorCrosshairView(Context context) { super(context); init(); }
    public SensorCrosshairView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#1A71797E")); // Faint background circle
        bgPaint.setStyle(Paint.Style.FILL);

        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setColor(Color.parseColor("#4D71797E")); // 30% Steel Grey
        crosshairPaint.setStrokeWidth(4f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#E0E0E0")); // Bright off-white dots
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setRollPitch(float roll, float pitch) {
        this.rollPercent = Math.max(0f, Math.min(100f, roll));
        this.pitchPercent = Math.max(0f, Math.min(100f, pitch));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float padding = 14f;
        
        // Draw background circle gauge
        canvas.drawCircle(cx, cy, Math.min(cx, cy) - 4f, bgPaint);

        // Draw Crosshair (+)
        canvas.drawLine(padding, cy, w - padding, cy, crosshairPaint); // Horizontal Axis
        canvas.drawLine(cx, padding, cx, h - padding, crosshairPaint); // Vertical Axis

        // Calculate Dot Positions
        // Roll (Steering): 0 to 100 -> Left to Right (Horizontal Line)
        float rollX = padding + ((w - padding * 2) * (rollPercent / 100f));
        
        // Pitch (Acceleration): 0 to 100 -> Bottom to Top (Vertical Line)
        float pitchY = (h - padding) - ((h - padding * 2) * (pitchPercent / 100f));

        // Draw the two indicator dots
        canvas.drawCircle(rollX, cy, 10f, dotPaint); // Roll moves horizontally
        canvas.drawCircle(cx, pitchY, 10f, dotPaint); // Pitch moves vertically
    }
}
