package com.sonymobile.keyguard.plugin.analogclock;

import android.graphics.Paint;

public abstract class ClockPart {
    private final int mColorResourceId;
    private final Paint mPaint = new Paint();
    private float mRadians = -1.0f;

    public ClockPart(int i) {
        this.mColorResourceId = i;
    }

    public final Paint getPaint() {
        return this.mPaint;
    }

    public final boolean setRotation(float f) {
        boolean z = f != this.mRadians;
        this.mRadians = f;
        return z;
    }

    public final float getRotation() {
        return this.mRadians;
    }

    /* access modifiers changed from: protected */
    public final int getColorResourceId() {
        return this.mColorResourceId;
    }
}
