package com.teamclouday.androidsteering.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class UnclippedLinearLayout extends LinearLayout {
    public UnclippedLinearLayout(Context context) { super(context); }
    public UnclippedLinearLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public UnclippedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    public void getHitRect(Rect outRect) {
        outRect.set(-10000, -10000, 10000, 10000);
    }
}
