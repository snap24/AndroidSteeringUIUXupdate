package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PedalSliderView extends View {

    private Paint trackPaint;
    private Paint fillPaint;
    private Paint thumbPaint;
    private Paint labelPaint;
    private Paint valuePaint;

    private float trackLeft, trackRight, trackTop, trackBottom;
    private float value = 0.0f; // 0.0 to 1.0
    private String label = "";
    private boolean isPressed = false;
    private Runnable releaseRunnable;

    public interface OnPedalChangeListener {
        void onPedalChanged(float value);
    }
    private OnPedalChangeListener listener;

    public PedalSliderView(Context context) { super(context); init(); }
    public PedalSliderView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        trackPaint.setColor(Color.parseColor("#4D71797E")); // 30% Steel Grey
        trackPaint.setStrokeWidth(2f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#B371797E")); // 70% Steel Grey

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.parseColor("#E0E0E0")); // Off-white thumb

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(22f); 
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTextSize(26f); 
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
    }

    public void setLabel(String label) { this.label = label; invalidate(); }
    public void setAccentColor(int color) { invalidate(); }
    public void setOnPedalChangeListener(OnPedalChangeListener l) { this.listener = l; }
    public float getValue() { return value; }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        float cx = w / 2f;
        float trackW = 44f * getResources().getDisplayMetrics().density;
        trackLeft = cx - trackW / 2f;
        trackRight = cx + trackW / 2f;
        trackTop = 16f;
        trackBottom = h - 45f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int adaptiveColor = Color.parseColor("#71797E");
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.textColor, tv, true);
            adaptiveColor = tv.data;
        } catch (Exception ignored) {}

        labelPaint.setColor(adaptiveColor);
        valuePaint.setColor(adaptiveColor);

        float cx = getWidth() / 2f;
        float trackH = trackBottom - trackTop;

        RectF trackRect = new RectF(trackLeft, trackTop, trackRight, trackBottom);
        canvas.drawRoundRect(trackRect, 8f, 8f, trackPaint);

        float fillTop = trackBottom - (value * trackH);
        RectF fillRect = new RectF(trackLeft, fillTop, trackRight, trackBottom);
        Paint alphaFill = new Paint(fillPaint);
        alphaFill.setAlpha(180);
        canvas.drawRoundRect(fillRect, 8f, 8f, alphaFill);

        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.parseColor("#333333"));
        tickPaint.setStrokeWidth(1.5f);
        for (int i = 1; i < 5; i++) {
            float ty = trackBottom - (trackH * i / 5f);
            canvas.drawLine(trackLeft + 4f, ty, trackRight - 4f, ty, tickPaint);
        }

        float thumbY = fillTop;
        RectF thumbRect = new RectF(trackLeft - 4f, thumbY - 8f, trackRight + 4f, thumbY + 8f);
        thumbPaint.setColor(Color.parseColor("#E0E0E0"));
        canvas.drawRoundRect(thumbRect, 4f, 4f, thumbPaint);

        int pct = (int)(value * 100);
        canvas.drawText(pct + "%", cx, fillTop - 15f, valuePaint);
        canvas.drawText(label, cx, trackBottom + 30f, labelPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float trackH = trackBottom - trackTop;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                if (releaseRunnable != null) removeCallbacks(releaseRunnable);
                // Fall-through
            case MotionEvent.ACTION_MOVE:
                float touchY = event.getY();
                float newValue = 1f - ((touchY - trackTop) / trackH);
                value = Math.max(0f, Math.min(1f, newValue));
                invalidate();
                if (listener != null) listener.onPedalChanged(value);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                animateRelease();
                return true;
        }
        return false;
    }

    private void animateRelease() {
        if (releaseRunnable != null) removeCallbacks(releaseRunnable);

        releaseRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPressed) return; // Safety check

                // Increased snap threshold to 10% to prevent lingering single-digit values
                if (value <= 0.10f) {
                    value = 0f;
                    invalidate();
                    if (listener != null) listener.onPedalChanged(value);
                    return;
                }
                value *= 0.65f; // Faster release
                invalidate();
                if (listener != null) listener.onPedalChanged(value);
                postDelayed(this, 16);
            }
        };
        post(releaseRunnable);
    }
}
