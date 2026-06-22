package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.teamclouday.androidsteering.R;

public class DraggableFrameLayout extends FrameLayout {

    private static final String PREFS_NAME = "layout_positions";
    private static final long LONG_PRESS_MS = 400L;

    private SharedPreferences prefs;
    private String layoutKey; // e.g. "gamepad", "steering_wheel"
    private boolean editModeActive = false;

    // Per-child drag state
    private View dragTarget = null;
    private View selectedView = null; // Remembers the last touched view for the +/- buttons
    private float dragOffsetX, dragOffsetY;
    private float touchStartX, touchStartY;
    private long touchStartTime;
    
    private android.view.ScaleGestureDetector scaleGestureDetector;
    private android.widget.LinearLayout scalePanel;

    public DraggableFrameLayout(Context context) { super(context); init(context); }
    public DraggableFrameLayout(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public DraggableFrameLayout(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(context); }

    private void init(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        scaleGestureDetector = new android.view.ScaleGestureDetector(context, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(android.view.ScaleGestureDetector detector) {
                if (dragTarget != null && editModeActive) {
                    float scaleFactor = detector.getScaleFactor();
                    float currentScaleX = dragTarget.getScaleX();
                    float currentScaleY = dragTarget.getScaleY();
                    
                    dragTarget.setScaleX(currentScaleX * scaleFactor);
                    dragTarget.setScaleY(currentScaleY * scaleFactor);
                    return true;
                }
                return false;
            }
        });

        // Initialize the scale panel
        scalePanel = new android.widget.LinearLayout(context);
        scalePanel.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        scalePanel.setBackgroundColor(android.graphics.Color.parseColor("#80000000"));
        scalePanel.setPadding(16, 8, 16, 8);
        scalePanel.setVisibility(View.GONE);
        
        android.widget.TextView label = new android.widget.TextView(context);
        label.setText("↕ Size: ");
        label.setTextColor(android.graphics.Color.WHITE);
        label.setTextSize(14f);
        label.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = scalePanel.getX() - event.getRawX();
                        dY = scalePanel.getY() - event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        scalePanel.setX(event.getRawX() + dX);
                        scalePanel.setY(event.getRawY() + dY);
                        return true;
                }
                return false;
            }
        });
        scalePanel.addView(label);

        android.widget.Button btnMinus = new android.widget.Button(context);
        btnMinus.setText("-");
        btnMinus.setTextSize(20f);
        btnMinus.setMinimumWidth(0);
        btnMinus.setMinimumHeight(0);
        btnMinus.setPadding(32, 0, 32, 0);
        btnMinus.setOnClickListener(v -> scaleSelectedView(-0.1f));
        scalePanel.addView(btnMinus);

        android.widget.Button btnPlus = new android.widget.Button(context);
        btnPlus.setText("+");
        btnPlus.setTextSize(20f);
        btnPlus.setMinimumWidth(0);
        btnPlus.setMinimumHeight(0);
        btnPlus.setPadding(32, 0, 32, 0);
        btnPlus.setOnClickListener(v -> scaleSelectedView(0.1f));
        scalePanel.addView(btnPlus);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, 
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        params.topMargin = 0;
        
        post(() -> addView(scalePanel, params));
    }
    
    private void scaleSelectedView(float delta) {
        if (selectedView != null && editModeActive) {
            float newScaleX = Math.max(0.2f, selectedView.getScaleX() + delta);
            float newScaleY = Math.max(0.2f, selectedView.getScaleY() + delta);
            selectedView.setScaleX(newScaleX);
            selectedView.setScaleY(newScaleY);
            savePositions();
        }
    }

    public void setLayoutKey(String key) {
        this.layoutKey = key;
    }

    private java.util.List<View> getDraggableViews() {
        java.util.List<View> views = new java.util.ArrayList<>();
        collectViews(this, views);
        return views;
    }

    private void collectViews(android.view.ViewGroup parent, java.util.List<View> views) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == scalePanel) continue;
            
            // If the child is a ViewGroup but NOT one of our custom interactive views, recurse into it
            if (child instanceof android.view.ViewGroup && 
                !(child instanceof SteeringWheelView) && 
                !(child instanceof SensorCrosshairView) && 
                !(child.getClass().getSimpleName().equals("JoystickView"))) {
                collectViews((android.view.ViewGroup) child, views);
            } else if (child.getId() != View.NO_ID) {
                // It's a leaf view with an ID (Button, Image, Slider, Joystick, ProgressBar, Crosshair, Switch, etc.)
                views.add(child);
            }
        }
    }

    public void setEditMode(boolean active) {
        this.editModeActive = active;
        for (View child : getDraggableViews()) {
            child.setAlpha(active ? 0.85f : 1.0f);
        }
        if (scalePanel != null) {
            scalePanel.setVisibility(active ? View.VISIBLE : View.GONE);
            if (!active) {
                selectedView = null; // Clear selection when exiting edit mode
                updateScalePanelText();
            }
        }
    }

    private void updateScalePanelText() {
        if (scalePanel != null && scalePanel.getChildCount() > 0) {
            android.widget.TextView label = (android.widget.TextView) scalePanel.getChildAt(0);
            if (selectedView != null) {
                String type = selectedView.getClass().getSimpleName();
                if (selectedView instanceof android.widget.Button) {
                    type = "Btn: " + ((android.widget.Button)selectedView).getText();
                }
                label.setText("↕ Size (" + type + "): ");
            } else {
                label.setText("↕ Size (Tap an item): ");
            }
        }
    }

    public boolean isEditMode() { return editModeActive; }

    public void restoreSavedPositions() {
        if (layoutKey == null) return;
        getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getWidth() == 0 || getHeight() == 0) return;
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                boolean isFirstRun = !prefs.contains("is_initialized");
            if (isFirstRun) {
                // Initialize default preferences from default_layout.json
                try {
                    java.io.InputStream is = getContext().getResources().openRawResource(com.teamclouday.androidsteering.R.raw.default_layout);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    String json = new String(buffer, "UTF-8");
                    org.json.JSONObject rootObject = new org.json.JSONObject(json);
                    SharedPreferences.Editor editor = prefs.edit();
                    java.util.Iterator<String> viewKeys = rootObject.keys();
                    while (viewKeys.hasNext()) {
                        String key = viewKeys.next();
                        Object value = rootObject.get(key);
                        if (value instanceof Number) {
                            editor.putFloat(key, ((Number) value).floatValue());
                        }
                    }
                    editor.putBoolean("is_initialized", true);
                    editor.apply();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            java.util.List<View> views = getDraggableViews();
            for (int i = 0; i < views.size(); i++) {
                View child = views.get(i);
                String xKey = layoutKey + "_" + i + "_x";
                String yKey = layoutKey + "_" + i + "_y";
                String scaleXKey = layoutKey + "_" + i + "_scaleX";
                String scaleYKey = layoutKey + "_" + i + "_scaleY";
                
                // Load Scale FIRST so bounds calculation is accurate
                if (prefs.contains(scaleXKey) && prefs.contains(scaleYKey)) {
                    child.setScaleX(prefs.getFloat(scaleXKey, 1.0f));
                    child.setScaleY(prefs.getFloat(scaleYKey, 1.0f));
                }

                float loadedX = child.getX();
                if (prefs.contains(xKey + "Ratio")) {
                    loadedX = prefs.getFloat(xKey + "Ratio", 0.0f) * getWidth();
                } else if (prefs.contains(xKey)) {
                    loadedX = prefs.getFloat(xKey, loadedX);
                }
                
                float loadedY = child.getY();
                if (prefs.contains(yKey + "Ratio")) {
                    loadedY = prefs.getFloat(yKey + "Ratio", 0.0f) * getHeight();
                } else if (prefs.contains(yKey)) {
                    loadedY = prefs.getFloat(yKey, loadedY);
                }
                
                // Clamp X based on strict visual bounds (accounting for scale)
                float marginX = (child.getWidth() * child.getScaleX() - child.getWidth()) / 2f;
                float minX = marginX;
                float maxX = getWidth() - child.getWidth() - marginX;
                if (minX > maxX) { float temp = minX; minX = maxX; maxX = temp; }
                if (loadedX < minX) loadedX = minX;
                else if (loadedX > maxX) loadedX = maxX;
                
                // Clamp Y based on strict visual bounds (accounting for scale)
                float marginY = (child.getHeight() * child.getScaleY() - child.getHeight()) / 2f;
                float minY = marginY;
                float maxY = getHeight() - child.getHeight() - marginY;
                if (minY > maxY) { float temp = minY; minY = maxY; maxY = temp; }
                if (loadedY < minY) loadedY = minY;
                else if (loadedY > maxY) loadedY = maxY;

                child.setX(loadedX);
                child.setY(loadedY);
            }
        }
    });
}

    public void savePositions() {
        if (layoutKey == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        java.util.List<View> views = getDraggableViews();
        for (int i = 0; i < views.size(); i++) {
            View child = views.get(i);
            editor.putFloat(layoutKey + "_" + i + "_xRatio", child.getX() / getWidth());
            editor.putFloat(layoutKey + "_" + i + "_yRatio", child.getY() / getHeight());
            editor.putFloat(layoutKey + "_" + i + "_x", child.getX());
            editor.putFloat(layoutKey + "_" + i + "_y", child.getY());
            editor.putFloat(layoutKey + "_" + i + "_scaleX", child.getScaleX());
            editor.putFloat(layoutKey + "_" + i + "_scaleY", child.getScaleY());
        }
        editor.apply();
    }

    public void resetToDefault() {
        if (layoutKey == null) return;
        
        // Wipe the persistent storage completely to force re-initialization
        // from default_layout.json upon next load.
        prefs.edit().clear().apply();
        
        // Also instantly reset visually so they don't have to wait for reload
        java.util.List<View> views = getDraggableViews();
        for (int i = 0; i < views.size(); i++) {
            View child = views.get(i);
            child.setScaleX(1.0f);
            child.setScaleY(1.0f);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!editModeActive) return false;
        
        // If they touched the scalePanel, do NOT intercept
        if (scalePanel != null && scalePanel.getVisibility() == View.VISIBLE && isTouchOnChild(scalePanel, ev)) {
            return false;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartX = ev.getRawX();
            touchStartY = ev.getRawY();
            touchStartTime = System.currentTimeMillis();
            java.util.List<View> views = getDraggableViews();
            for (int i = views.size() - 1; i >= 0; i--) {
                View child = views.get(i);
                if (isTouchOnChild(child, ev)) {
                    dragTarget = child;
                    selectedView = child;
                    updateScalePanelText();
                    dragOffsetX = ev.getRawX() - child.getX();
                    dragOffsetY = ev.getRawY() - child.getY();
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!editModeActive || dragTarget == null) return false;
        
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(ev);
        }

        if (ev.getPointerCount() > 1) {
            // Pinch to zoom is happening, update offsets so it doesn't jump after pinch
            dragOffsetX = ev.getRawX() - dragTarget.getX();
            dragOffsetY = ev.getRawY() - dragTarget.getY();
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float newX = ev.getRawX() - dragOffsetX;
                float newY = ev.getRawY() - dragOffsetY;

                // Clamp X based on strict visual bounds (accounting for scale)
                float marginX = (selectedView.getWidth() * selectedView.getScaleX() - selectedView.getWidth()) / 2f;
                float minX = marginX;
                float maxX = getWidth() - selectedView.getWidth() - marginX;
                if (minX > maxX) { float temp = minX; minX = maxX; maxX = temp; }
                if (newX < minX) newX = minX;
                else if (newX > maxX) newX = maxX;

                // Clamp Y based on strict visual bounds (accounting for scale)
                float marginY = (selectedView.getHeight() * selectedView.getScaleY() - selectedView.getHeight()) / 2f;
                float minY = marginY;
                float maxY = getHeight() - selectedView.getHeight() - marginY;
                if (minY > maxY) { float temp = minY; minY = maxY; maxY = temp; }
                if (newY < minY) newY = minY;
                else if (newY > maxY) newY = maxY;

                dragTarget.setX(newX);
                dragTarget.setY(newY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                savePositions(); // Auto-save when dropped
                dragTarget = null;
                break;
        }
        return true;
    }

    private boolean isTouchOnChild(View child, MotionEvent ev) {
        return com.teamclouday.androidsteering.HitTestHelper.isTouchInside(child, ev, this);
    }
}