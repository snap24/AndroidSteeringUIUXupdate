package com.teamclouday.androidsteering;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class HitTestHelper {
    public static boolean isTouchInside(View child, MotionEvent ev, ViewGroup root) {
        float[] point = new float[] { ev.getRawX(), ev.getRawY() };
        
        // Transform screen coordinates to root coordinates
        int[] rootLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        point[0] -= rootLoc[0];
        point[1] -= rootLoc[1];
        
        // Traverse down from root to child to apply inverse matrices
        java.util.List<View> path = new java.util.ArrayList<>();
        View current = child;
        while (current != root && current != null) {
            path.add(0, current);
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
        
        Matrix inverse = new Matrix();
        for (View v : path) {
            point[0] -= v.getLeft() - v.getScrollX();
            point[1] -= v.getTop() - v.getScrollY();
            v.getMatrix().invert(inverse);
            inverse.mapPoints(point);
        }
        
        // 20px slop for easier grabbing
        return point[0] >= -20 && point[0] <= child.getWidth() + 20 &&
               point[1] >= -20 && point[1] <= child.getHeight() + 20;
    }
}
