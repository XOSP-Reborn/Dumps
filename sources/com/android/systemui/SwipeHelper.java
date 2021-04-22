package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;

public class SwipeHelper implements Gefingerpoken {
    private final Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private final Context mContext;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDisableHwLayers;
    private final ArrayMap<View, Animator> mDismissPendingMap = new ArrayMap<>();
    private boolean mDragging;
    private final boolean mFadeDependingOnAmountSwiped;
    private final FalsingManager mFalsingManager;
    private final int mFalsingThreshold;
    private final FlingAnimationUtils mFlingAnimationUtils;
    protected final Handler mHandler;
    private float mInitialTouchPos;
    private boolean mLongPressSent;
    private final long mLongPressTimeout;
    private float mMaxSwipeProgress = 1.0f;
    private boolean mMenuRowIntercepting;
    private float mMinSwipeProgress = 0.0f;
    private float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    private boolean mSnappingChild;
    private final int mSwipeDirection;
    private final int[] mTmpPos = new int[2];
    private boolean mTouchAboveFalsingThreshold;
    private float mTranslation = 0.0f;
    private final VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    /* access modifiers changed from: protected */
    public long getMaxEscapeAnimDuration() {
        return 400;
    }

    /* access modifiers changed from: protected */
    public float getUnscaledEscapeVelocity() {
        return 500.0f;
    }

    /* access modifiers changed from: protected */
    public boolean handleUpEvent(MotionEvent motionEvent, View view, float f, float f2) {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onChildSnappedBack(View view, float f) {
    }

    public void onDownUpdate(View view, MotionEvent motionEvent) {
    }

    /* access modifiers changed from: protected */
    public void onMoveUpdate(View view, MotionEvent motionEvent, float f, float f2) {
    }

    /* access modifiers changed from: protected */
    public void prepareDismissAnimation(View view, Animator animator) {
    }

    /* access modifiers changed from: protected */
    public void prepareSnapBackAnimation(View view, Animator animator) {
    }

    public SwipeHelper(int i, Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = i;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mPagingTouchSlop = (float) ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mLongPressTimeout = (long) (((float) ViewConfiguration.getLongPressTimeout()) * 1.5f);
        Resources resources = context.getResources();
        this.mDensityScale = resources.getDisplayMetrics().density;
        this.mFalsingThreshold = resources.getDimensionPixelSize(C0005R$dimen.swipe_helper_falsing_threshold);
        this.mFadeDependingOnAmountSwiped = resources.getBoolean(C0003R$bool.config_fadeDependingOnAmountSwiped);
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, ((float) getMaxEscapeAnimDuration()) / 1000.0f);
    }

    public void setDensityScale(float f) {
        this.mDensityScale = f;
    }

    public void setPagingTouchSlop(float f) {
        this.mPagingTouchSlop = f;
    }

    public void setDisableHardwareLayers(boolean z) {
        this.mDisableHwLayers = z;
    }

    private float getPos(MotionEvent motionEvent) {
        return this.mSwipeDirection == 0 ? motionEvent.getX() : motionEvent.getY();
    }

    private float getPerpendicularPos(MotionEvent motionEvent) {
        return this.mSwipeDirection == 0 ? motionEvent.getY() : motionEvent.getX();
    }

    /* access modifiers changed from: protected */
    public float getTranslation(View view) {
        return this.mSwipeDirection == 0 ? view.getTranslationX() : view.getTranslationY();
    }

    private float getVelocity(VelocityTracker velocityTracker) {
        if (this.mSwipeDirection == 0) {
            return velocityTracker.getXVelocity();
        }
        return velocityTracker.getYVelocity();
    }

    /* access modifiers changed from: protected */
    public ObjectAnimator createTranslationAnimation(View view, float f) {
        return ObjectAnimator.ofFloat(view, this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y, f);
    }

    /* access modifiers changed from: protected */
    public Animator getViewTranslationAnimator(View view, float f, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        ObjectAnimator createTranslationAnimation = createTranslationAnimation(view, f);
        if (animatorUpdateListener != null) {
            createTranslationAnimation.addUpdateListener(animatorUpdateListener);
        }
        return createTranslationAnimation;
    }

    /* access modifiers changed from: protected */
    public void setTranslation(View view, float f) {
        if (view != null) {
            if (this.mSwipeDirection == 0) {
                view.setTranslationX(f);
            } else {
                view.setTranslationY(f);
            }
        }
    }

    /* access modifiers changed from: protected */
    public float getSize(View view) {
        return (float) (this.mSwipeDirection == 0 ? view.getMeasuredWidth() : view.getMeasuredHeight());
    }

    private float getSwipeProgressForOffset(View view, float f) {
        return Math.min(Math.max(this.mMinSwipeProgress, Math.abs(f / getSize(view))), this.mMaxSwipeProgress);
    }

    private float getSwipeAlpha(float f) {
        if (this.mFadeDependingOnAmountSwiped) {
            return Math.max(1.0f - f, 0.0f);
        }
        return 1.0f - Math.max(0.0f, Math.min(1.0f, f / 0.5f));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSwipeProgressFromOffset(View view, boolean z) {
        updateSwipeProgressFromOffset(view, z, getTranslation(view));
    }

    private void updateSwipeProgressFromOffset(View view, boolean z, float f) {
        float swipeProgressForOffset = getSwipeProgressForOffset(view, f);
        if (!this.mCallback.updateSwipeProgress(view, z, swipeProgressForOffset) && z) {
            if (!this.mDisableHwLayers) {
                if (swipeProgressForOffset == 0.0f || swipeProgressForOffset == 1.0f) {
                    view.setLayerType(0, null);
                } else {
                    view.setLayerType(2, null);
                }
            }
            view.setAlpha(getSwipeAlpha(swipeProgressForOffset));
        }
        invalidateGlobalRegion(view);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    public static void invalidateGlobalRegion(View view, RectF rectF) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(rectF);
            view.invalidate((int) Math.floor((double) rectF.left), (int) Math.floor((double) rectF.top), (int) Math.ceil((double) rectF.right), (int) Math.ceil((double) rectF.bottom));
        }
    }

    public void cancelLongPress() {
        Runnable runnable = this.mWatchLongPress;
        if (runnable != null) {
            this.mHandler.removeCallbacks(runnable);
            this.mWatchLongPress = null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0024, code lost:
        if (r0 != 3) goto L_0x00f1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onInterceptTouchEvent(final android.view.MotionEvent r7) {
        /*
        // Method dump skipped, instructions count: 256
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.SwipeHelper.onInterceptTouchEvent(android.view.MotionEvent):boolean");
    }

    public void dismissChild(View view, float f, boolean z) {
        dismissChild(view, f, null, 0, z, 0, false);
    }

    public void dismissChild(final View view, float f, final Runnable runnable, long j, boolean z, long j2, boolean z2) {
        float f2;
        long j3;
        final boolean canChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean z3 = false;
        boolean z4 = view.getLayoutDirection() == 1;
        int i = (f > 0.0f ? 1 : (f == 0.0f ? 0 : -1));
        boolean z5 = i == 0 && (getTranslation(view) == 0.0f || z2) && this.mSwipeDirection == 1;
        boolean z6 = i == 0 && (getTranslation(view) == 0.0f || z2) && z4;
        if ((Math.abs(f) > getEscapeVelocity() && f < 0.0f) || (getTranslation(view) < 0.0f && !z2)) {
            z3 = true;
        }
        if (z3 || z6 || z5) {
            f2 = -getSize(view);
        } else {
            f2 = getSize(view);
        }
        if (j2 == 0) {
            j3 = i != 0 ? Math.min(400L, (long) ((int) ((Math.abs(f2 - getTranslation(view)) * 1000.0f) / Math.abs(f)))) : 200;
        } else {
            j3 = j2;
        }
        if (!this.mDisableHwLayers) {
            view.setLayerType(2, null);
        }
        Animator viewTranslationAnimator = getViewTranslationAnimator(view, f2, new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.SwipeHelper.AnonymousClass2 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) valueAnimator.getAnimatedValue()).floatValue(), canChildBeDismissed);
            }
        });
        if (viewTranslationAnimator != null) {
            if (z) {
                viewTranslationAnimator.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                viewTranslationAnimator.setDuration(j3);
            } else {
                this.mFlingAnimationUtils.applyDismissing(viewTranslationAnimator, getTranslation(view), f2, f, getSize(view));
            }
            if (j > 0) {
                viewTranslationAnimator.setStartDelay(j);
            }
            viewTranslationAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.SwipeHelper.AnonymousClass3 */
                private boolean mCancelled;

                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    SwipeHelper.this.updateSwipeProgressFromOffset(view, canChildBeDismissed);
                    SwipeHelper.this.mDismissPendingMap.remove(view);
                    View view = view;
                    boolean isRemoved = view instanceof ExpandableNotificationRow ? ((ExpandableNotificationRow) view).isRemoved() : false;
                    if (!this.mCancelled || isRemoved) {
                        SwipeHelper.this.mCallback.onChildDismissed(view);
                    }
                    Runnable runnable = runnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (!SwipeHelper.this.mDisableHwLayers) {
                        view.setLayerType(0, null);
                    }
                }
            });
            prepareDismissAnimation(view, viewTranslationAnimator);
            this.mDismissPendingMap.put(view, viewTranslationAnimator);
            viewTranslationAnimator.start();
        }
    }

    public void snapChild(final View view, final float f, float f2) {
        final boolean canChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        Animator viewTranslationAnimator = getViewTranslationAnimator(view, f, new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.SwipeHelper.AnonymousClass4 */

            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) valueAnimator.getAnimatedValue()).floatValue(), canChildBeDismissed);
            }
        });
        if (viewTranslationAnimator != null) {
            viewTranslationAnimator.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.SwipeHelper.AnonymousClass5 */
                boolean wasCancelled = false;

                public void onAnimationCancel(Animator animator) {
                    this.wasCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    SwipeHelper.this.mSnappingChild = false;
                    if (!this.wasCancelled) {
                        SwipeHelper.this.updateSwipeProgressFromOffset(view, canChildBeDismissed);
                        SwipeHelper.this.onChildSnappedBack(view, f);
                        SwipeHelper.this.mCallback.onChildSnappedBack(view, f);
                    }
                }
            });
            prepareSnapBackAnimation(view, viewTranslationAnimator);
            this.mSnappingChild = true;
            this.mFlingAnimationUtils.apply(viewTranslationAnimator, getTranslation(view), f, f2, Math.abs(f - getTranslation(view)));
            viewTranslationAnimator.start();
        }
    }

    public void onTranslationUpdate(View view, float f, boolean z) {
        updateSwipeProgressFromOffset(view, z, f);
    }

    private void snapChildInstantly(View view) {
        boolean canChildBeDismissed = this.mCallback.canChildBeDismissed(view);
        setTranslation(view, 0.0f);
        updateSwipeProgressFromOffset(view, canChildBeDismissed);
    }

    public void snapChildIfNeeded(View view, boolean z, float f) {
        if ((!this.mDragging || this.mCurrView != view) && !this.mSnappingChild) {
            Animator animator = this.mDismissPendingMap.get(view);
            boolean z2 = true;
            if (animator != null) {
                animator.cancel();
            } else if (getTranslation(view) == 0.0f) {
                z2 = false;
            }
            if (!z2) {
                return;
            }
            if (z) {
                snapChild(view, f, 0.0f);
            } else {
                snapChildInstantly(view);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0036, code lost:
        if (r0 != 4) goto L_0x00f6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r11) {
        /*
        // Method dump skipped, instructions count: 247
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.SwipeHelper.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mFalsingThreshold) * this.mCallback.getFalsingThresholdFactor());
    }

    private float getMaxVelocity() {
        return this.mDensityScale * 4000.0f;
    }

    /* access modifiers changed from: protected */
    public float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFarEnough() {
        return Math.abs(getTranslation(this.mCurrView)) > getSize(this.mCurrView) * 0.6f;
    }

    public boolean isDismissGesture(MotionEvent motionEvent) {
        float translation = getTranslation(this.mCurrView);
        if (motionEvent.getActionMasked() != 1 || this.mFalsingManager.isUnlockingDisabled() || isFalseGesture(motionEvent)) {
            return false;
        }
        if (!swipedFastEnough() && !swipedFarEnough()) {
            return false;
        }
        return this.mCallback.canChildBeDismissedInDirection(this.mCurrView, (translation > 0.0f ? 1 : (translation == 0.0f ? 0 : -1)) > 0);
    }

    public boolean isFalseGesture(MotionEvent motionEvent) {
        boolean isAntiFalsingNeeded = this.mCallback.isAntiFalsingNeeded();
        if (this.mFalsingManager.isClassiferEnabled()) {
            if (isAntiFalsingNeeded && this.mFalsingManager.isFalseTouch()) {
                return true;
            }
        } else if (isAntiFalsingNeeded && !this.mTouchAboveFalsingThreshold) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        if (Math.abs(velocity) > getEscapeVelocity()) {
            if ((velocity > 0.0f) == (translation > 0.0f)) {
                return true;
            }
        }
        return false;
    }

    public interface Callback {
        boolean canChildBeDismissed(View view);

        default boolean canChildBeDragged(View view) {
            return true;
        }

        View getChildAtPosition(MotionEvent motionEvent);

        default int getConstrainSwipeStartPosition() {
            return 0;
        }

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view, float f);

        void onDragCancelled(View view);

        boolean updateSwipeProgress(View view, boolean z, float f);

        default boolean canChildBeDismissedInDirection(View view, boolean z) {
            return canChildBeDismissed(view);
        }
    }
}
