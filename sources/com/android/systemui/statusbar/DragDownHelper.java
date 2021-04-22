package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.ExpandHelper;
import com.android.systemui.Gefingerpoken;
import com.android.systemui.Interpolators;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.statusbar.notification.row.ExpandableView;

public class DragDownHelper implements Gefingerpoken {
    private ExpandHelper.Callback mCallback;
    private DragDownCallback mDragDownCallback;
    private boolean mDraggedFarEnough;
    private boolean mDraggingDown;
    private FalsingManager mFalsingManager;
    private View mHost;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private float mLastHeight;
    private int mMinDragDistance;
    private ExpandableView mStartingChild;
    private final int[] mTemp2 = new int[2];
    private float mTouchSlop;

    public interface DragDownCallback {
        boolean isFalsingCheckNeeded();

        void onCrossedThreshold(boolean z);

        void onDragDownReset();

        boolean onDraggedDown(View view, int i);

        void onTouchSlopExceeded();

        void setEmptyDragAmount(float f);
    }

    public DragDownHelper(Context context, View view, ExpandHelper.Callback callback, DragDownCallback dragDownCallback) {
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(C0005R$dimen.keyguard_drag_down_min_distance);
        this.mTouchSlop = (float) ViewConfiguration.get(context).getScaledTouchSlop();
        this.mCallback = callback;
        this.mDragDownCallback = dragDownCallback;
        this.mHost = view;
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mDraggedFarEnough = false;
            this.mDraggingDown = false;
            this.mStartingChild = null;
            this.mInitialTouchY = y;
            this.mInitialTouchX = x;
        } else if (actionMasked == 2) {
            float f = y - this.mInitialTouchY;
            if (f > this.mTouchSlop && f > Math.abs(x - this.mInitialTouchX)) {
                this.mFalsingManager.onNotificatonStartDraggingDown();
                this.mDraggingDown = true;
                captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mDragDownCallback.onTouchSlopExceeded();
                return true;
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mDraggingDown) {
            return false;
        }
        motionEvent.getX();
        float y = motionEvent.getY();
        if (motionEvent.getPointerCount() > 1) {
            stopDragging();
            return false;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 1) {
            if (actionMasked == 2) {
                float f = this.mInitialTouchY;
                this.mLastHeight = y - f;
                captureStartingChild(this.mInitialTouchX, f);
                ExpandableView expandableView = this.mStartingChild;
                if (expandableView != null) {
                    handleExpansion(this.mLastHeight, expandableView);
                } else {
                    this.mDragDownCallback.setEmptyDragAmount(this.mLastHeight);
                }
                if (this.mLastHeight > ((float) this.mMinDragDistance)) {
                    if (!this.mDraggedFarEnough) {
                        this.mDraggedFarEnough = true;
                        this.mDragDownCallback.onCrossedThreshold(true);
                    }
                } else if (this.mDraggedFarEnough) {
                    this.mDraggedFarEnough = false;
                    this.mDragDownCallback.onCrossedThreshold(false);
                }
                return true;
            } else if (actionMasked == 3) {
                stopDragging();
                return false;
            }
        } else if (this.mFalsingManager.isUnlockingDisabled() || isFalseTouch() || !this.mDragDownCallback.onDraggedDown(this.mStartingChild, (int) (y - this.mInitialTouchY))) {
            stopDragging();
            return false;
        } else {
            ExpandableView expandableView2 = this.mStartingChild;
            if (expandableView2 == null) {
                cancelExpansion();
            } else {
                this.mCallback.setUserLockedChild(expandableView2, false);
                this.mStartingChild = null;
            }
            this.mDraggingDown = false;
        }
        return false;
    }

    private boolean isFalseTouch() {
        if (!this.mDragDownCallback.isFalsingCheckNeeded()) {
            return false;
        }
        if (this.mFalsingManager.isFalseTouch() || !this.mDraggedFarEnough) {
            return true;
        }
        return false;
    }

    private void captureStartingChild(float f, float f2) {
        if (this.mStartingChild == null) {
            this.mStartingChild = findView(f, f2);
            ExpandableView expandableView = this.mStartingChild;
            if (expandableView != null) {
                this.mCallback.setUserLockedChild(expandableView, true);
            }
        }
    }

    private void handleExpansion(float f, ExpandableView expandableView) {
        float f2 = 0.0f;
        if (f >= 0.0f) {
            f2 = f;
        }
        boolean isContentExpandable = expandableView.isContentExpandable();
        float f3 = f2 * (isContentExpandable ? 0.5f : 0.15f);
        if (isContentExpandable && ((float) expandableView.getCollapsedHeight()) + f3 > ((float) expandableView.getMaxContentHeight())) {
            f3 -= ((((float) expandableView.getCollapsedHeight()) + f3) - ((float) expandableView.getMaxContentHeight())) * 0.85f;
        }
        expandableView.setActualHeight((int) (((float) expandableView.getCollapsedHeight()) + f3));
    }

    private void cancelExpansion(final ExpandableView expandableView) {
        if (expandableView.getActualHeight() == expandableView.getCollapsedHeight()) {
            this.mCallback.setUserLockedChild(expandableView, false);
            return;
        }
        ObjectAnimator ofInt = ObjectAnimator.ofInt(expandableView, "actualHeight", expandableView.getActualHeight(), expandableView.getCollapsedHeight());
        ofInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        ofInt.setDuration(375L);
        ofInt.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.statusbar.DragDownHelper.AnonymousClass1 */

            public void onAnimationEnd(Animator animator) {
                DragDownHelper.this.mCallback.setUserLockedChild(expandableView, false);
            }
        });
        ofInt.start();
    }

    private void cancelExpansion() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(this.mLastHeight, 0.0f);
        ofFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        ofFloat.setDuration(375L);
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.statusbar.$$Lambda$DragDownHelper$q6x0oNk24uuvhTw3d_iOE5k6pV4 */

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                DragDownHelper.this.lambda$cancelExpansion$0$DragDownHelper(valueAnimator);
            }
        });
        ofFloat.start();
    }

    public /* synthetic */ void lambda$cancelExpansion$0$DragDownHelper(ValueAnimator valueAnimator) {
        this.mDragDownCallback.setEmptyDragAmount(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    private void stopDragging() {
        this.mFalsingManager.onNotificatonStopDraggingDown();
        ExpandableView expandableView = this.mStartingChild;
        if (expandableView != null) {
            cancelExpansion(expandableView);
            this.mStartingChild = null;
        } else {
            cancelExpansion();
        }
        this.mDraggingDown = false;
        this.mDragDownCallback.onDragDownReset();
    }

    private ExpandableView findView(float f, float f2) {
        this.mHost.getLocationOnScreen(this.mTemp2);
        int[] iArr = this.mTemp2;
        return this.mCallback.getChildAtRawPosition(f + ((float) iArr[0]), f2 + ((float) iArr[1]));
    }

    public boolean isDraggingDown() {
        return this.mDraggingDown;
    }
}
