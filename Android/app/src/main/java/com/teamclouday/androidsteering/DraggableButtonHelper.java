package com.teamclouday.androidsteering;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class DraggableButtonHelper {

    private static final String PREF_NAME = "ButtonPositions";

    public interface OnDragListener {
        void onDragFinished(View view);
    }

    public static void makeDraggable(View view, boolean isEditMode, OnDragListener listener) {
        if (!isEditMode) {
            view.setOnTouchListener(null);
            view.setAlpha(1.0f);
            return;
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        isDragging = false;
                        v.setAlpha(0.6f);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;
                        
                        v.setX(newX);
                        v.setY(newY);
                        isDragging = true;
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.setAlpha(1.0f);
                        if (isDragging) {
                            savePosition(v.getContext(), v.getId(), v.getX(), v.getY());
                            if (listener != null) listener.onDragFinished(v);
                        } else {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public static void savePosition(Context context, int viewId, float x, float y) {
        if (viewId == View.NO_ID) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat(viewId + "_x", x)
                .putFloat(viewId + "_y", y)
                .apply();
    }

    public static void restorePosition(Context context, View view) {
        int viewId = view.getId();
        if (viewId == View.NO_ID) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.contains(viewId + "_x") && prefs.contains(viewId + "_y")) {
            final float x = prefs.getFloat(viewId + "_x", 0);
            final float y = prefs.getFloat(viewId + "_y", 0);
            view.post(() -> {
                view.setX(x);
                view.setY(y);
            });
        }
    }

    public static void resetPositions(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
