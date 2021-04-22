package com.sonymobile.keyguard.clock.picker;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ClockPickerViewContent extends FrameLayout {
    private float mScaleLevel;

    public ClockPickerViewContent(Context context) {
        this(context, null);
    }

    public ClockPickerViewContent(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ClockPickerViewContent(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mScaleLevel = 1.0f;
    }

    /* access modifiers changed from: protected */
    public final void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int paddingStart = getPaddingStart() + getPaddingEnd();
        if (getChildCount() > 0) {
            paddingStart = (int) (((float) paddingStart) + (((float) getChildAt(0).getMeasuredWidth()) * this.mScaleLevel));
        }
        setMeasuredDimension(paddingStart, getMeasuredHeight());
    }

    public final void setScaleLevel(float f) {
        this.mScaleLevel = f;
    }
}
