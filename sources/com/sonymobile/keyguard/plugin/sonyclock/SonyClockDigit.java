package com.sonymobile.keyguard.plugin.sonyclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0009R$interpolator;

public class SonyClockDigit extends FrameLayout {
    private char mCurrentDigit;
    private TextView mCurrentDisplay;
    private int mDigitAnimationDuration;
    private Interpolator mInInterpolator;
    private TextView mOldDisplay;
    private TextView mPlaceHolderDisplay;

    public SonyClockDigit(Context context) {
        this(context, null, 0, 0);
    }

    public SonyClockDigit(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public SonyClockDigit(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SonyClockDigit(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mCurrentDigit = '0';
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        super.onFinishInflate();
        this.mDigitAnimationDuration = getResources().getInteger(C0008R$integer.somc_sony_clock_digit_animation_duration);
        this.mInInterpolator = AnimationUtils.loadInterpolator(getContext(), C0009R$interpolator.somc_sony_clock_digit_animation_interpolator);
    }

    public final void setDigitViews(View view, View view2, View view3) {
        if (view != null && (view instanceof TextView)) {
            this.mCurrentDisplay = (TextView) view;
        }
        if (view2 != null && (view2 instanceof TextView)) {
            this.mOldDisplay = (TextView) view2;
        }
        if (view3 != null && (view3 instanceof TextView)) {
            this.mPlaceHolderDisplay = (TextView) view3;
        }
    }

    public final void updateDigit(char c, boolean z) {
        if (hasDigitViews()) {
            if (!z || c == this.mCurrentDigit) {
                this.mCurrentDisplay.setText(String.valueOf(c));
                this.mOldDisplay.setText(String.valueOf(c));
                this.mOldDisplay.setVisibility(4);
            } else {
                this.mCurrentDisplay.setText(String.valueOf(c));
                this.mOldDisplay.setText(String.valueOf(this.mCurrentDigit));
                this.mOldDisplay.setVisibility(0);
                int width = this.mPlaceHolderDisplay.getWidth();
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mOldDisplay, "translationX", 0.0f, (float) (-width));
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mCurrentDisplay, "translationX", (float) width, 0.0f);
                ofFloat.addListener(new AnimatorListenerAdapter() {
                    /* class com.sonymobile.keyguard.plugin.sonyclock.SonyClockDigit.AnonymousClass1 */

                    public void onAnimationEnd(Animator animator) {
                        SonyClockDigit.this.mOldDisplay.setVisibility(4);
                    }
                });
                ofFloat.setInterpolator(this.mInInterpolator);
                ofFloat2.setInterpolator(this.mInInterpolator);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(ofFloat, ofFloat2);
                animatorSet.setDuration((long) this.mDigitAnimationDuration).start();
            }
            this.mCurrentDigit = c;
        }
    }

    private boolean hasDigitViews() {
        return (this.mCurrentDisplay == null || this.mOldDisplay == null || this.mPlaceHolderDisplay == null) ? false : true;
    }

    public void updateThemeColor(int i) {
        this.mCurrentDisplay.setTextColor(i);
        this.mOldDisplay.setTextColor(i);
    }
}
