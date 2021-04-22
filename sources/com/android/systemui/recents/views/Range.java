package com.android.systemui.recents.views;

/* access modifiers changed from: package-private */
/* compiled from: TaskStackLayoutAlgorithm */
public class Range {
    float max;
    float min;
    float origin;
    final float relativeMax;
    final float relativeMin;

    public Range(float f, float f2) {
        this.relativeMin = f;
        this.min = f;
        this.relativeMax = f2;
        this.max = f2;
    }

    public void offset(float f) {
        this.origin = f;
        this.min = this.relativeMin + f;
        this.max = f + this.relativeMax;
    }

    public float getNormalizedX(float f) {
        float f2;
        float f3;
        float f4 = this.origin;
        if (f < f4) {
            f2 = (f - f4) * 0.5f;
            f3 = -this.relativeMin;
        } else {
            f2 = (f - f4) * 0.5f;
            f3 = this.relativeMax;
        }
        return (f2 / f3) + 0.5f;
    }

    public float getAbsoluteX(float f) {
        float f2;
        float f3;
        if (f < 0.5f) {
            f2 = (f - 0.5f) / 0.5f;
            f3 = -this.relativeMin;
        } else {
            f2 = (f - 0.5f) / 0.5f;
            f3 = this.relativeMax;
        }
        return f2 * f3;
    }

    public boolean isInRange(float f) {
        double d = (double) f;
        return d >= Math.floor((double) this.min) && d <= Math.ceil((double) this.max);
    }
}
