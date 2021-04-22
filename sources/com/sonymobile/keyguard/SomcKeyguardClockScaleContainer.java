package com.sonymobile.keyguard;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class SomcKeyguardClockScaleContainer extends FrameLayout implements Animator.AnimatorListener {
    private boolean mAnimateScaling;
    private AnimatorSet mAnimatorSet;
    private int mHeightChange;
    private float mMinScale;
    private float mMinScaleForComputeNotifications;
    private boolean mPivotXViewStart;
    private float mPreviousScale;
    private SomcKeyguardClockScaleContainerCallback mSomcKeyguardClockScaleContainerCallback;

    public final void onAnimationRepeat(Animator animator) {
    }

    public SomcKeyguardClockScaleContainer(Context context) {
        this(context, null, 0, 0);
    }

    public SomcKeyguardClockScaleContainer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public SomcKeyguardClockScaleContainer(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SomcKeyguardClockScaleContainer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mMinScale = 0.5f;
        this.mMinScaleForComputeNotifications = 0.6f;
        this.mPivotXViewStart = false;
        this.mAnimateScaling = false;
        this.mPreviousScale = 1.0f;
    }

    public final int getHeightChange() {
        return this.mHeightChange;
    }

    public final int getMaxHeightChange(boolean z) {
        if (getMeasuredHeight() <= 0) {
            return 0;
        }
        return (int) (((float) getMeasuredHeight()) * -1.0f * (1.0f - (z ? this.mMinScaleForComputeNotifications : this.mMinScale)));
    }

    public final void setMinScale(float f) {
        this.mMinScale = f;
    }

    public final void setMinScaleForComputeNotifications(float f) {
        this.mMinScaleForComputeNotifications = f;
    }

    public final int requestHeightChange(int i, int i2) {
        if (i >= 0 && this.mHeightChange == 0) {
            return 0;
        }
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (measuredWidth > 0 && measuredHeight > 0) {
            updateHeightChange(i);
            resizeClockAnimation(measuredWidth, measuredHeight, i2, this.mHeightChange);
        }
        return this.mHeightChange;
    }

    public float getRequestedScale() {
        return this.mPreviousScale;
    }

    public final void setSomcKeyguardClockScaleContainerCallback(SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback) {
        this.mSomcKeyguardClockScaleContainerCallback = somcKeyguardClockScaleContainerCallback;
    }

    public final void setPivotXViewStart(boolean z) {
        this.mPivotXViewStart = z;
    }

    public final void setAnimateScaling(boolean z) {
        this.mAnimateScaling = z;
    }

    private void resizeClockAnimation(int i, int i2, int i3, int i4) {
        float f = (float) i2;
        float f2 = ((float) (i4 + i2)) / f;
        if (Math.abs(this.mPreviousScale - f2) > 1.0E-7f) {
            this.mPreviousScale = f2;
            AnimatorSet animatorSet = this.mAnimatorSet;
            if (animatorSet != null) {
                animatorSet.cancel();
            }
            if (!this.mPivotXViewStart) {
                setPivotX(((float) i) / 2.0f);
            } else if (getResources().getConfiguration().getLayoutDirection() == 1) {
                setPivotX((float) i);
            } else {
                setPivotX(0.0f);
            }
            setPivotY(f);
            if (this.mAnimateScaling) {
                performAnimatedScaling(f2, i3);
            } else {
                performNonAnimatedScaling(f2);
            }
        }
    }

    private void performNonAnimatedScaling(float f) {
        SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback = this.mSomcKeyguardClockScaleContainerCallback;
        if (somcKeyguardClockScaleContainerCallback != null) {
            somcKeyguardClockScaleContainerCallback.onScalingStarted(f);
        }
        setScaleX(f);
        setScaleY(f);
        SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback2 = this.mSomcKeyguardClockScaleContainerCallback;
        if (somcKeyguardClockScaleContainerCallback2 != null) {
            somcKeyguardClockScaleContainerCallback2.onScalingFinished(f);
        }
    }

    private void performAnimatedScaling(float f, int i) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, View.SCALE_X, f);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this, View.SCALE_Y, f);
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(ofFloat, ofFloat2);
        this.mAnimatorSet.setDuration((long) i);
        this.mAnimatorSet.addListener(this);
        this.mAnimatorSet.start();
    }

    private void updateHeightChange(int i) {
        this.mHeightChange += i;
        if (this.mHeightChange > 0) {
            this.mHeightChange = 0;
        }
    }

    public final void onAnimationCancel(Animator animator) {
        SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback = this.mSomcKeyguardClockScaleContainerCallback;
        if (somcKeyguardClockScaleContainerCallback != null) {
            somcKeyguardClockScaleContainerCallback.onScalingCancelled(this.mPreviousScale);
        }
    }

    public final void onAnimationEnd(Animator animator) {
        SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback = this.mSomcKeyguardClockScaleContainerCallback;
        if (somcKeyguardClockScaleContainerCallback != null) {
            somcKeyguardClockScaleContainerCallback.onScalingFinished(this.mPreviousScale);
        }
    }

    public final void onAnimationStart(Animator animator) {
        SomcKeyguardClockScaleContainerCallback somcKeyguardClockScaleContainerCallback = this.mSomcKeyguardClockScaleContainerCallback;
        if (somcKeyguardClockScaleContainerCallback != null) {
            somcKeyguardClockScaleContainerCallback.onScalingStarted(this.mPreviousScale);
        }
    }
}
