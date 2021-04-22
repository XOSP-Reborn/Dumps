package com.sonymobile.keyguard.aod;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import com.android.systemui.C0000R$anim;

public class NotificationAnimation {
    private Animator mAnimator;
    private boolean mCanceled;
    private Handler mHandler = new Handler();
    private int mLargeRepeatCount = 0;
    private Runnable mNewNotificationRunnable = new Runnable() {
        /* class com.sonymobile.keyguard.aod.NotificationAnimation.AnonymousClass1 */

        public void run() {
            NotificationAnimation.this.mRepeatCount = 0;
            NotificationAnimation.this.mCanceled = false;
            if (NotificationAnimation.this.mTargetView != null) {
                NotificationAnimation.this.mAnimator.start();
            }
        }
    };
    private int mRepeatCount = 0;
    private View mTargetView;

    static /* synthetic */ int access$008(NotificationAnimation notificationAnimation) {
        int i = notificationAnimation.mRepeatCount;
        notificationAnimation.mRepeatCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$408(NotificationAnimation notificationAnimation) {
        int i = notificationAnimation.mLargeRepeatCount;
        notificationAnimation.mLargeRepeatCount = i + 1;
        return i;
    }

    public NotificationAnimation(Context context) {
        this.mAnimator = AnimatorInflater.loadAnimator(context, C0000R$anim.somc_aod_notification_icon_new);
        this.mAnimator.addListener(new Animator.AnimatorListener() {
            /* class com.sonymobile.keyguard.aod.NotificationAnimation.AnonymousClass2 */

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                NotificationAnimation.access$008(NotificationAnimation.this);
                if (NotificationAnimation.this.mCanceled) {
                    return;
                }
                if (NotificationAnimation.this.mRepeatCount >= 3) {
                    NotificationAnimation.access$408(NotificationAnimation.this);
                    if (NotificationAnimation.this.mLargeRepeatCount < 5) {
                        NotificationAnimation.this.mHandler.postDelayed(NotificationAnimation.this.mNewNotificationRunnable, 1000);
                    }
                } else if (NotificationAnimation.this.mTargetView != null) {
                    animator.start();
                }
            }

            public void onAnimationCancel(Animator animator) {
                NotificationAnimation.this.mCanceled = true;
            }
        });
    }

    public void startNotificationAnimation(View view) {
        cancelAnimator();
        this.mTargetView = view;
        this.mAnimator.setTarget(view);
        this.mHandler.post(this.mNewNotificationRunnable);
    }

    public void stopNotificationAnimation() {
        cancelAnimator();
    }

    private void cancelAnimator() {
        Animator animator = this.mAnimator;
        if (animator != null) {
            animator.cancel();
        }
        this.mHandler.removeCallbacks(this.mNewNotificationRunnable);
        init();
    }

    private void init() {
        this.mRepeatCount = 0;
        this.mLargeRepeatCount = 0;
        this.mCanceled = false;
        this.mTargetView = null;
    }
}
