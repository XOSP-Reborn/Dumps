package com.sonymobile.systemui.lockscreen;

public class UnlockAnimationAlgorithm {
    private float mDefaultClockPivotY;
    private boolean mFlinging = false;
    private boolean mKeyguardShowing = false;
    private boolean mNeedsAnimationWhenLastEvent = false;
    private boolean mQsExpanded = false;
    private float mResultAlpha;
    private float mResultScale;
    private boolean mTracking = false;

    private static float computeScale(float f) {
        return (f * 0.25f) + 0.75f;
    }

    public void setKeyguardShowing(boolean z) {
        this.mKeyguardShowing = z;
    }

    public void setQsExpanded(boolean z) {
        this.mQsExpanded = z;
    }

    public void setTracking(boolean z) {
        this.mTracking = z;
        updateNeedsAnimationState();
    }

    public void setFlinging(boolean z) {
        this.mFlinging = z;
        updateNeedsAnimationState();
    }

    public void setDefaultClockPivotY(float f) {
        this.mDefaultClockPivotY = f;
    }

    public float getDefaultClockPivotY() {
        return this.mDefaultClockPivotY;
    }

    public boolean computeParamsIfNeeded(float f, float f2) {
        if (!needsAnimation() || f >= f2) {
            return false;
        }
        float f3 = f / f2;
        this.mResultAlpha = computeAlpha(f3);
        this.mResultScale = computeScale(f3);
        return true;
    }

    public float getResultAlpha() {
        return this.mResultAlpha;
    }

    public float getResultScale() {
        return this.mResultScale;
    }

    private static float computeAlpha(float f) {
        float max = Math.max(0.0f, (f * 1.1f) - 0.1f);
        return max * max;
    }

    public float getAdjustedExpandedHeight(float f, float f2) {
        return needsAnimation() ? f2 : f;
    }

    public float getAdjustedExpandedFraction(float f, float f2) {
        return needsAnimation() ? f2 : f;
    }

    private boolean needsAnimation() {
        return (this.mTracking || this.mFlinging) && this.mNeedsAnimationWhenLastEvent;
    }

    private void updateNeedsAnimationState() {
        boolean z = false;
        if (this.mTracking || this.mFlinging) {
            if (this.mKeyguardShowing && !this.mQsExpanded) {
                z = true;
            }
            this.mNeedsAnimationWhenLastEvent = z;
            return;
        }
        this.mNeedsAnimationWhenLastEvent = false;
    }
}
