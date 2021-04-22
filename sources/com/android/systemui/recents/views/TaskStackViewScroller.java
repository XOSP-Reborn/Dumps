package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.FloatProperty;
import android.util.Property;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.widget.OverScroller;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.lowram.TaskStackLowRamLayoutAlgorithm;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;

public class TaskStackViewScroller {
    private static final Property<TaskStackViewScroller, Float> STACK_SCROLL = new FloatProperty<TaskStackViewScroller>("stackScroll") {
        /* class com.android.systemui.recents.views.TaskStackViewScroller.AnonymousClass1 */

        public void setValue(TaskStackViewScroller taskStackViewScroller, float f) {
            taskStackViewScroller.setStackScroll(f);
        }

        public Float get(TaskStackViewScroller taskStackViewScroller) {
            return Float.valueOf(taskStackViewScroller.getStackScroll());
        }
    };
    TaskStackViewScrollerCallbacks mCb;
    Context mContext;
    float mFinalAnimatedScroll;
    final FlingAnimationUtils mFlingAnimationUtils;
    float mFlingDownScrollP;
    int mFlingDownY;
    @ViewDebug.ExportedProperty(category = "recents")
    float mLastDeltaP = 0.0f;
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    ObjectAnimator mScrollAnimator;
    OverScroller mScroller;
    @ViewDebug.ExportedProperty(category = "recents")
    float mStackScrollP;

    public interface TaskStackViewScrollerCallbacks {
        void onStackScrollChanged(float f, float f2, AnimationProps animationProps);
    }

    public TaskStackViewScroller(Context context, TaskStackViewScrollerCallbacks taskStackViewScrollerCallbacks, TaskStackLayoutAlgorithm taskStackLayoutAlgorithm) {
        this.mContext = context;
        this.mCb = taskStackViewScrollerCallbacks;
        this.mScroller = new OverScroller(context);
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            this.mScroller.setFriction(0.06f);
        }
        this.mLayoutAlgorithm = taskStackLayoutAlgorithm;
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
    }

    /* access modifiers changed from: package-private */
    public void reset() {
        this.mStackScrollP = 0.0f;
        this.mLastDeltaP = 0.0f;
    }

    /* access modifiers changed from: package-private */
    public void resetDeltaScroll() {
        this.mLastDeltaP = 0.0f;
    }

    public float getStackScroll() {
        return this.mStackScrollP;
    }

    public void setStackScroll(float f) {
        setStackScroll(f, AnimationProps.IMMEDIATE);
    }

    public float setDeltaStackScroll(float f, float f2) {
        float f3 = f + f2;
        float updateFocusStateOnScroll = this.mLayoutAlgorithm.updateFocusStateOnScroll(f + this.mLastDeltaP, f3, this.mStackScrollP);
        setStackScroll(updateFocusStateOnScroll, AnimationProps.IMMEDIATE);
        this.mLastDeltaP = f2;
        return updateFocusStateOnScroll - f3;
    }

    public void setStackScroll(float f, AnimationProps animationProps) {
        float f2 = this.mStackScrollP;
        this.mStackScrollP = f;
        TaskStackViewScrollerCallbacks taskStackViewScrollerCallbacks = this.mCb;
        if (taskStackViewScrollerCallbacks != null) {
            taskStackViewScrollerCallbacks.onStackScrollChanged(f2, this.mStackScrollP, animationProps);
        }
    }

    public boolean setStackScrollToInitialState() {
        float f = this.mStackScrollP;
        setStackScroll(this.mLayoutAlgorithm.mInitialScrollP);
        return Float.compare(f, this.mStackScrollP) != 0;
    }

    public void fling(float f, int i, int i2, int i3, int i4, int i5, int i6) {
        this.mFlingDownScrollP = f;
        this.mFlingDownY = i;
        this.mScroller.fling(0, i2, 0, i3, 0, 0, i4, i5, 0, i6);
    }

    public boolean boundScroll() {
        float stackScroll = getStackScroll();
        float boundedStackScroll = getBoundedStackScroll(stackScroll);
        if (Float.compare(boundedStackScroll, stackScroll) == 0) {
            return false;
        }
        setStackScroll(boundedStackScroll);
        return true;
    }

    /* access modifiers changed from: package-private */
    public float getBoundedStackScroll(float f) {
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
        return Utilities.clamp(f, taskStackLayoutAlgorithm.mMinScrollP, taskStackLayoutAlgorithm.mMaxScrollP);
    }

    /* access modifiers changed from: package-private */
    public float getScrollAmountOutOfBounds(float f) {
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
        float f2 = taskStackLayoutAlgorithm.mMinScrollP;
        if (f < f2) {
            return Math.abs(f - f2);
        }
        float f3 = taskStackLayoutAlgorithm.mMaxScrollP;
        if (f > f3) {
            return Math.abs(f - f3);
        }
        return 0.0f;
    }

    /* access modifiers changed from: package-private */
    public boolean isScrollOutOfBounds() {
        return Float.compare(getScrollAmountOutOfBounds(this.mStackScrollP), 0.0f) != 0;
    }

    /* access modifiers changed from: package-private */
    public void scrollToClosestTask(int i) {
        float stackScroll = getStackScroll();
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
            if (stackScroll >= taskStackLayoutAlgorithm.mMinScrollP && stackScroll <= taskStackLayoutAlgorithm.mMaxScrollP) {
                TaskStackLowRamLayoutAlgorithm taskStackLowRamLayoutAlgorithm = taskStackLayoutAlgorithm.mTaskStackLowRamLayoutAlgorithm;
                if (((float) Math.abs(i)) > ((float) ViewConfiguration.get(this.mContext).getScaledMinimumFlingVelocity())) {
                    fling(0.0f, 0, taskStackLowRamLayoutAlgorithm.percentageToScroll(stackScroll), -i, taskStackLowRamLayoutAlgorithm.percentageToScroll(this.mLayoutAlgorithm.mMinScrollP), taskStackLowRamLayoutAlgorithm.percentageToScroll(this.mLayoutAlgorithm.mMaxScrollP), 0);
                    float closestTaskP = taskStackLowRamLayoutAlgorithm.getClosestTaskP(taskStackLowRamLayoutAlgorithm.scrollToPercentage(this.mScroller.getFinalY()), this.mLayoutAlgorithm.mNumStackTasks, i);
                    ValueAnimator ofFloat = ObjectAnimator.ofFloat(stackScroll, closestTaskP);
                    this.mFlingAnimationUtils.apply(ofFloat, (float) taskStackLowRamLayoutAlgorithm.percentageToScroll(stackScroll), (float) taskStackLowRamLayoutAlgorithm.percentageToScroll(closestTaskP), (float) i);
                    animateScroll(closestTaskP, (int) ofFloat.getDuration(), ofFloat.getInterpolator(), null);
                    return;
                }
                animateScroll(taskStackLowRamLayoutAlgorithm.getClosestTaskP(stackScroll, this.mLayoutAlgorithm.mNumStackTasks, i), 300, Interpolators.ACCELERATE_DECELERATE, null);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public ObjectAnimator animateBoundScroll() {
        float stackScroll = getStackScroll();
        float boundedStackScroll = getBoundedStackScroll(stackScroll);
        if (Float.compare(boundedStackScroll, stackScroll) != 0) {
            animateScroll(boundedStackScroll, null);
        }
        return this.mScrollAnimator;
    }

    /* access modifiers changed from: package-private */
    public void animateScroll(float f, Runnable runnable) {
        animateScroll(f, this.mContext.getResources().getInteger(2131427442), runnable);
    }

    /* access modifiers changed from: package-private */
    public void animateScroll(float f, int i, Runnable runnable) {
        animateScroll(f, i, Interpolators.LINEAR_OUT_SLOW_IN, runnable);
    }

    /* access modifiers changed from: package-private */
    public void animateScroll(float f, int i, TimeInterpolator timeInterpolator, Runnable runnable) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, STACK_SCROLL, getStackScroll(), f);
        ofFloat.setDuration((long) i);
        ofFloat.setInterpolator(timeInterpolator);
        animateScroll(f, ofFloat, runnable);
    }

    private void animateScroll(float f, ObjectAnimator objectAnimator, final Runnable runnable) {
        ObjectAnimator objectAnimator2 = this.mScrollAnimator;
        if (objectAnimator2 != null && objectAnimator2.isRunning()) {
            setStackScroll(this.mFinalAnimatedScroll);
            this.mScroller.forceFinished(true);
        }
        stopScroller();
        stopBoundScrollAnimation();
        if (Float.compare(this.mStackScrollP, f) != 0) {
            this.mFinalAnimatedScroll = f;
            this.mScrollAnimator = objectAnimator;
            this.mScrollAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.recents.views.TaskStackViewScroller.AnonymousClass2 */

                public void onAnimationEnd(Animator animator) {
                    Runnable runnable = runnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                    TaskStackViewScroller.this.mScrollAnimator.removeAllListeners();
                }
            });
            this.mScrollAnimator.start();
        } else if (runnable != null) {
            runnable.run();
        }
    }

    /* access modifiers changed from: package-private */
    public void stopBoundScrollAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollAnimator);
    }

    /* access modifiers changed from: package-private */
    public boolean computeScroll() {
        if (!this.mScroller.computeScrollOffset()) {
            return false;
        }
        float deltaPForY = this.mLayoutAlgorithm.getDeltaPForY(this.mFlingDownY, this.mScroller.getCurrY());
        float f = this.mFlingDownScrollP;
        this.mFlingDownScrollP = f + setDeltaStackScroll(f, deltaPForY);
        return true;
    }

    /* access modifiers changed from: package-private */
    public float getScrollVelocity() {
        return this.mScroller.getCurrVelocity();
    }

    /* access modifiers changed from: package-private */
    public void stopScroller() {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("TaskStackViewScroller");
        printWriter.print(" stackScroll:");
        printWriter.print(this.mStackScrollP);
        printWriter.println();
    }
}
