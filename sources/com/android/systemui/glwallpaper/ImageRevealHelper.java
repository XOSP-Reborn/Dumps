package com.android.systemui.glwallpaper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import com.android.systemui.Interpolators;

/* access modifiers changed from: package-private */
public class ImageRevealHelper {
    private final ValueAnimator mAnimator;
    private boolean mAwake = false;
    private float mReveal = 0.0f;
    private final RevealStateListener mRevealListener;

    public interface RevealStateListener {
        void onRevealEnd();

        void onRevealStart(boolean z);

        void onRevealStateChanged();
    }

    ImageRevealHelper(RevealStateListener revealStateListener) {
        this.mRevealListener = revealStateListener;
        this.mAnimator = ValueAnimator.ofFloat(new float[0]);
        this.mAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.glwallpaper.$$Lambda$ImageRevealHelper$F24215Snv58_ZInLQsaNs5JLH9M */

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ImageRevealHelper.this.lambda$new$0$ImageRevealHelper(valueAnimator);
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.glwallpaper.ImageRevealHelper.AnonymousClass1 */
            private boolean mIsCanceled;

            public void onAnimationCancel(Animator animator) {
                this.mIsCanceled = true;
            }

            public void onAnimationEnd(Animator animator) {
                if (!this.mIsCanceled && ImageRevealHelper.this.mRevealListener != null) {
                    ImageRevealHelper.this.mRevealListener.onRevealEnd();
                }
                this.mIsCanceled = false;
            }

            public void onAnimationStart(Animator animator) {
                if (ImageRevealHelper.this.mRevealListener != null) {
                    ImageRevealHelper.this.mRevealListener.onRevealStart(true);
                }
            }
        });
    }

    public /* synthetic */ void lambda$new$0$ImageRevealHelper(ValueAnimator valueAnimator) {
        this.mReveal = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        RevealStateListener revealStateListener = this.mRevealListener;
        if (revealStateListener != null) {
            revealStateListener.onRevealStateChanged();
        }
    }

    private void animate() {
        this.mAnimator.cancel();
        ValueAnimator valueAnimator = this.mAnimator;
        float[] fArr = new float[2];
        fArr[0] = this.mReveal;
        fArr[1] = this.mAwake ? 0.0f : 1.0f;
        valueAnimator.setFloatValues(fArr);
        this.mAnimator.start();
    }

    public float getReveal() {
        return this.mReveal;
    }

    /* access modifiers changed from: package-private */
    public void updateAwake(boolean z, long j) {
        this.mAwake = z;
        this.mAnimator.setDuration(j);
        if (j == 0) {
            this.mReveal = this.mAwake ? 0.0f : 1.0f;
            this.mRevealListener.onRevealStart(false);
            this.mRevealListener.onRevealStateChanged();
            this.mRevealListener.onRevealEnd();
            return;
        }
        animate();
    }
}
