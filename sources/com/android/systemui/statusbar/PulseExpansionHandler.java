package com.android.systemui.statusbar;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Gefingerpoken;
import com.android.systemui.Interpolators;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableView;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.phone.ShadeController;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: PulseExpansionHandler.kt */
public final class PulseExpansionHandler implements Gefingerpoken {
    public static final Companion Companion = new Companion(null);
    private static final float RUBBERBAND_FACTOR_STATIC = RUBBERBAND_FACTOR_STATIC;
    private static final int SPRING_BACK_ANIMATION_LENGTH_MS = SPRING_BACK_ANIMATION_LENGTH_MS;
    private boolean isExpanding;
    private boolean isWakingToShadeLocked;
    private boolean mDraggedFarEnough;
    private float mEmptyDragAmount;
    private ExpansionCallback mExpansionCallback;
    private final FalsingManager mFalsingManager;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private final int mMinDragDistance;
    private final PowerManager mPowerManager;
    private boolean mPulsing;
    private boolean mReachedWakeUpHeight;
    private ShadeController mShadeController;
    private NotificationStackScrollLayout mStackScroller;
    private ExpandableView mStartingChild;
    private final int[] mTemp2 = new int[2];
    private final float mTouchSlop;
    private final NotificationWakeUpCoordinator mWakeUpCoordinator;
    private float mWakeUpHeight;

    /* compiled from: PulseExpansionHandler.kt */
    public interface ExpansionCallback {
        void setEmptyDragAmount(float f);
    }

    public PulseExpansionHandler(Context context, NotificationWakeUpCoordinator notificationWakeUpCoordinator) {
        Intrinsics.checkParameterIsNotNull(context, "context");
        Intrinsics.checkParameterIsNotNull(notificationWakeUpCoordinator, "mWakeUpCoordinator");
        this.mWakeUpCoordinator = notificationWakeUpCoordinator;
        this.mMinDragDistance = context.getResources().getDimensionPixelSize(C0005R$dimen.keyguard_drag_down_min_distance);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        Intrinsics.checkExpressionValueIsNotNull(viewConfiguration, "ViewConfiguration.get(context)");
        this.mTouchSlop = (float) viewConfiguration.getScaledTouchSlop();
        FalsingManager instance = FalsingManagerFactory.getInstance(context);
        Intrinsics.checkExpressionValueIsNotNull(instance, "FalsingManagerFactory.getInstance(context)");
        this.mFalsingManager = instance;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
    }

    /* compiled from: PulseExpansionHandler.kt */
    public static final class Companion {
        private Companion() {
        }

        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }

    public final boolean isWakingToShadeLocked() {
        return this.isWakingToShadeLocked;
    }

    private final boolean isFalseTouch() {
        return this.mFalsingManager.isFalseTouch();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        Intrinsics.checkParameterIsNotNull(motionEvent, "event");
        return maybeStartExpansion(motionEvent);
    }

    private final boolean maybeStartExpansion(MotionEvent motionEvent) {
        if (!this.mPulsing) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mDraggedFarEnough = false;
            this.isExpanding = false;
            this.mStartingChild = null;
            this.mInitialTouchY = y;
            this.mInitialTouchX = x;
        } else if (actionMasked == 2) {
            float f = y - this.mInitialTouchY;
            if (f > this.mTouchSlop && f > Math.abs(x - this.mInitialTouchX)) {
                this.mFalsingManager.onStartExpandingFromPulse();
                this.isExpanding = true;
                captureStartingChild(this.mInitialTouchX, this.mInitialTouchY);
                this.mInitialTouchY = y;
                this.mInitialTouchX = x;
                this.mWakeUpHeight = this.mWakeUpCoordinator.getWakeUpHeight();
                this.mReachedWakeUpHeight = false;
                return true;
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        Intrinsics.checkParameterIsNotNull(motionEvent, "event");
        if (!this.isExpanding) {
            return maybeStartExpansion(motionEvent);
        }
        float y = motionEvent.getY();
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 1) {
            if (actionMasked == 2) {
                updateExpansionHeight(y - this.mInitialTouchY);
            } else if (actionMasked == 3) {
                cancelExpansion();
            }
        } else if (this.mFalsingManager.isUnlockingDisabled() || isFalseTouch()) {
            cancelExpansion();
        } else {
            finishExpansion();
        }
        return this.isExpanding;
    }

    private final void finishExpansion() {
        resetClock();
        ExpandableView expandableView = this.mStartingChild;
        if (expandableView != null) {
            if (expandableView != null) {
                setUserLocked(expandableView, false);
                this.mStartingChild = null;
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        }
        this.isExpanding = false;
        this.isWakingToShadeLocked = true;
        this.mWakeUpCoordinator.setWillWakeUp(true);
        PowerManager powerManager = this.mPowerManager;
        if (powerManager != null) {
            powerManager.wakeUp(SystemClock.uptimeMillis(), 4, "com.android.systemui:PULSEDRAG");
            ShadeController shadeController = this.mShadeController;
            if (shadeController != null) {
                shadeController.goToLockedShade(this.mStartingChild);
                ExpandableView expandableView2 = this.mStartingChild;
                if (expandableView2 instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView2;
                    if (expandableNotificationRow != null) {
                        expandableNotificationRow.onExpandedByGesture(true);
                    } else {
                        Intrinsics.throwNpe();
                        throw null;
                    }
                }
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
    }

    private final void updateExpansionHeight(float f) {
        float f2;
        float f3 = 0.0f;
        float max = Math.max(f, 0.0f);
        if (!this.mReachedWakeUpHeight && f > this.mWakeUpHeight) {
            this.mReachedWakeUpHeight = true;
        }
        ExpandableView expandableView = this.mStartingChild;
        if (expandableView == null) {
            if (this.mReachedWakeUpHeight) {
                f3 = this.mWakeUpHeight;
            }
            this.mWakeUpCoordinator.setNotificationsVisibleForExpansion(f > f3, true, true);
            f2 = Math.max(this.mWakeUpHeight, max);
        } else if (expandableView != null) {
            int min = Math.min((int) (((float) expandableView.getCollapsedHeight()) + max), expandableView.getMaxContentHeight());
            expandableView.setActualHeight(min);
            f2 = Math.max((float) min, max);
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
        setEmptyDragAmount(this.mWakeUpCoordinator.setPulseHeight(f2) * RUBBERBAND_FACTOR_STATIC);
    }

    private final void captureStartingChild(float f, float f2) {
        if (this.mStartingChild == null) {
            this.mStartingChild = findView(f, f2);
            ExpandableView expandableView = this.mStartingChild;
            if (expandableView == null) {
                return;
            }
            if (expandableView != null) {
                setUserLocked(expandableView, true);
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        }
    }

    /* access modifiers changed from: private */
    public final void setEmptyDragAmount(float f) {
        this.mEmptyDragAmount = f;
        ExpansionCallback expansionCallback = this.mExpansionCallback;
        if (expansionCallback != null) {
            expansionCallback.setEmptyDragAmount(f);
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
    }

    private final void reset(ExpandableView expandableView) {
        if (expandableView.getActualHeight() == expandableView.getCollapsedHeight()) {
            setUserLocked(expandableView, false);
            return;
        }
        ObjectAnimator ofInt = ObjectAnimator.ofInt(expandableView, "actualHeight", expandableView.getActualHeight(), expandableView.getCollapsedHeight());
        Intrinsics.checkExpressionValueIsNotNull(ofInt, "anim");
        ofInt.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        ofInt.setDuration((long) SPRING_BACK_ANIMATION_LENGTH_MS);
        ofInt.addListener(new PulseExpansionHandler$reset$1(this, expandableView));
        ofInt.start();
    }

    /* access modifiers changed from: private */
    public final void setUserLocked(ExpandableView expandableView, boolean z) {
        if (expandableView instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) expandableView).setUserLocked(z);
        }
    }

    private final void resetClock() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(this.mEmptyDragAmount, 0.0f);
        Intrinsics.checkExpressionValueIsNotNull(ofFloat, "anim");
        ofFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        ofFloat.setDuration((long) SPRING_BACK_ANIMATION_LENGTH_MS);
        ofFloat.addUpdateListener(new PulseExpansionHandler$resetClock$1(this));
        ofFloat.start();
    }

    private final void cancelExpansion() {
        this.mFalsingManager.onExpansionFromPulseStopped();
        ExpandableView expandableView = this.mStartingChild;
        if (expandableView == null) {
            resetClock();
        } else if (expandableView != null) {
            reset(expandableView);
            this.mStartingChild = null;
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
        this.mWakeUpCoordinator.setNotificationsVisibleForExpansion(false, true, false);
        this.isExpanding = false;
    }

    private final ExpandableView findView(float f, float f2) {
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            notificationStackScrollLayout.getLocationOnScreen(this.mTemp2);
            int[] iArr = this.mTemp2;
            float f3 = f + ((float) iArr[0]);
            float f4 = f2 + ((float) iArr[1]);
            NotificationStackScrollLayout notificationStackScrollLayout2 = this.mStackScroller;
            if (notificationStackScrollLayout2 != null) {
                ExpandableView childAtRawPosition = notificationStackScrollLayout2.getChildAtRawPosition(f3, f4);
                if (childAtRawPosition == null || !childAtRawPosition.isContentExpandable()) {
                    return null;
                }
                return childAtRawPosition;
            }
            Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
            throw null;
        }
        Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
        throw null;
    }

    public final void setUp(NotificationStackScrollLayout notificationStackScrollLayout, ExpansionCallback expansionCallback, ShadeController shadeController) {
        Intrinsics.checkParameterIsNotNull(notificationStackScrollLayout, "notificationStackScroller");
        Intrinsics.checkParameterIsNotNull(expansionCallback, "expansionCallback");
        Intrinsics.checkParameterIsNotNull(shadeController, "shadeController");
        this.mExpansionCallback = expansionCallback;
        this.mShadeController = shadeController;
        this.mStackScroller = notificationStackScrollLayout;
    }

    public final void setPulsing(boolean z) {
        this.mPulsing = z;
    }

    public final void onStartedWakingUp() {
        this.isWakingToShadeLocked = false;
    }
}
