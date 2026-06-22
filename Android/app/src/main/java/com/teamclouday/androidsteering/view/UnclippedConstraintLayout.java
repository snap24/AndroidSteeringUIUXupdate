package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.constraintlayout.widget.ConstraintLayout;

public class UnclippedConstraintLayout extends ConstraintLayout {
    public UnclippedConstraintLayout(Context context) { super(context); }
    public UnclippedConstraintLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public UnclippedConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    public void getHitRect(Rect outRect) {
        // Return a massive rect so touches are never clipped by the grandparent
        outRect.set(-10000, -10000, 10000, 10000);
    }
}
