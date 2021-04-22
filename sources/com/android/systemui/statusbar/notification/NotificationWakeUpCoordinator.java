package com.android.systemui.statusbar.notification;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.phone.DozeParameters;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: NotificationWakeUpCoordinator.kt */
public final class NotificationWakeUpCoordinator implements AmbientPulseManager.OnAmbientChangedListener, StatusBarStateController.StateListener {
    private final AmbientPulseManager mAmbientPulseManager;
    private final Context mContext;
    private ObjectAnimator mDarkAnimator;
    private float mDozeAmount;
    private final DozeParameters mDozeParameters;
    private final Set<NotificationEntry> mEntrySetToClearWhenFinished = new LinkedHashSet();
    private float mLinearDozeAmount;
    private float mLinearVisibilityAmount;
    private final NotificationWakeUpCoordinator$mNotificationVisibility$1 mNotificationVisibility = new NotificationWakeUpCoordinator$mNotificationVisibility$1("notificationVisibility");
    private float mNotificationVisibleAmount;
    private boolean mNotificationsVisible;
    private boolean mNotificationsVisibleForExpansion;
    private NotificationStackScrollLayout mStackScroller;
    private final StatusBarStateController mStatusBarStateController;
    private float mVisibilityAmount;
    private Interpolator mVisibilityInterpolator = Interpolators.FAST_OUT_SLOW_IN_REVERSE;
    private boolean mWakingUp;
    private boolean pulsing;
    private boolean willWakeUp;

    public NotificationWakeUpCoordinator(Context context, AmbientPulseManager ambientPulseManager, StatusBarStateController statusBarStateController) {
        Intrinsics.checkParameterIsNotNull(context, "mContext");
        Intrinsics.checkParameterIsNotNull(ambientPulseManager, "mAmbientPulseManager");
        Intrinsics.checkParameterIsNotNull(statusBarStateController, "mStatusBarStateController");
        this.mContext = context;
        this.mAmbientPulseManager = ambientPulseManager;
        this.mStatusBarStateController = statusBarStateController;
        this.mAmbientPulseManager.addListener(this);
        this.mStatusBarStateController.addCallback(this);
        DozeParameters instance = DozeParameters.getInstance(this.mContext);
        Intrinsics.checkExpressionValueIsNotNull(instance, "DozeParameters.getInstance(mContext)");
        this.mDozeParameters = instance;
    }

    public final void setWillWakeUp(boolean z) {
        if (!z || this.mDozeAmount != 0.0f) {
            this.willWakeUp = z;
        }
    }

    public final void setPulsing(boolean z) {
        this.pulsing = z;
        if (z) {
            updateNotificationVisibility(shouldAnimateVisibility(), false);
        }
    }

    public final void setStackScroller(NotificationStackScrollLayout notificationStackScrollLayout) {
        Intrinsics.checkParameterIsNotNull(notificationStackScrollLayout, "stackScroller");
        this.mStackScroller = notificationStackScrollLayout;
    }

    public final void setNotificationsVisibleForExpansion(boolean z, boolean z2, boolean z3) {
        this.mNotificationsVisibleForExpansion = z;
        updateNotificationVisibility(z2, z3);
        if (!z && this.mNotificationsVisible) {
            this.mAmbientPulseManager.releaseAllImmediately();
        }
    }

    private final void updateNotificationVisibility(boolean z, boolean z2) {
        boolean z3 = (this.mNotificationsVisibleForExpansion || this.mAmbientPulseManager.hasNotifications()) && this.pulsing;
        if (z3 || !this.mNotificationsVisible || ((!this.mWakingUp && !this.willWakeUp) || this.mDozeAmount == 0.0f)) {
            setNotificationsVisible(z3, z, z2);
        }
    }

    private final void setNotificationsVisible(boolean z, boolean z2, boolean z3) {
        if (this.mNotificationsVisible != z) {
            this.mNotificationsVisible = z;
            ObjectAnimator objectAnimator = this.mDarkAnimator;
            if (objectAnimator != null) {
                objectAnimator.cancel();
            }
            if (z2) {
                notifyAnimationStart(z);
                startVisibilityAnimation(z3);
                return;
            }
            setVisibilityAmount(z ? 1.0f : 0.0f);
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozeAmountChanged(float f, float f2) {
        if (!(f == 1.0f || f == 0.0f)) {
            float f3 = this.mLinearDozeAmount;
            if (f3 == 0.0f || f3 == 1.0f) {
                notifyAnimationStart(this.mLinearDozeAmount == 1.0f);
            }
        }
        this.mLinearDozeAmount = f;
        this.mDozeAmount = f2;
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            notificationStackScrollLayout.setDozeAmount(this.mDozeAmount);
            updateDarkAmount();
            if (f == 0.0f) {
                setNotificationsVisible(false, false, false);
                setNotificationsVisibleForExpansion(false, false, false);
                return;
            }
            return;
        }
        Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
        throw null;
    }

    private final void startVisibilityAnimation(boolean z) {
        Interpolator interpolator;
        float f = this.mNotificationVisibleAmount;
        float f2 = 0.0f;
        if (f == 0.0f || f == 1.0f) {
            if (this.mNotificationsVisible) {
                interpolator = Interpolators.TOUCH_RESPONSE;
            } else {
                interpolator = Interpolators.FAST_OUT_SLOW_IN_REVERSE;
            }
            this.mVisibilityInterpolator = interpolator;
        }
        if (this.mNotificationsVisible) {
            f2 = 1.0f;
        }
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, this.mNotificationVisibility, f2);
        ofFloat.setInterpolator(Interpolators.LINEAR);
        long j = (long) 500;
        if (z) {
            j = (long) (((float) j) / 1.5f);
        }
        ofFloat.setDuration(j);
        ofFloat.start();
        this.mDarkAnimator = ofFloat;
    }

    /* access modifiers changed from: private */
    public final void setVisibilityAmount(float f) {
        this.mLinearVisibilityAmount = f;
        this.mVisibilityAmount = this.mVisibilityInterpolator.getInterpolation(f);
        handleAnimationFinished();
        updateDarkAmount();
    }

    private final void handleAnimationFinished() {
        if (this.mLinearDozeAmount == 0.0f || this.mLinearVisibilityAmount == 0.0f) {
            Iterator<T> it = this.mEntrySetToClearWhenFinished.iterator();
            while (it.hasNext()) {
                it.next().setAmbientGoingAway(false);
            }
            this.mEntrySetToClearWhenFinished.clear();
        }
    }

    public final float getWakeUpHeight() {
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            return notificationStackScrollLayout.getPulseHeight();
        }
        Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
        throw null;
    }

    private final void updateDarkAmount() {
        float min = Math.min(1.0f - this.mLinearVisibilityAmount, this.mLinearDozeAmount);
        float min2 = Math.min(1.0f - this.mVisibilityAmount, this.mDozeAmount);
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            notificationStackScrollLayout.setDarkAmount(min, min2);
        } else {
            Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
            throw null;
        }
    }

    private final void notifyAnimationStart(boolean z) {
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            notificationStackScrollLayout.notifyDarkAnimationStart(!z);
        } else {
            Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
            throw null;
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        if (z) {
            setNotificationsVisible(false, false, false);
        }
    }

    public final float setPulseHeight(float f) {
        NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
        if (notificationStackScrollLayout != null) {
            return notificationStackScrollLayout.setPulseHeight(f);
        }
        Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
        throw null;
    }

    public final void setWakingUp(boolean z) {
        setWillWakeUp(false);
        this.mWakingUp = z;
        if (z && this.mNotificationsVisible && !this.mNotificationsVisibleForExpansion) {
            NotificationStackScrollLayout notificationStackScrollLayout = this.mStackScroller;
            if (notificationStackScrollLayout != null) {
                notificationStackScrollLayout.wakeUpFromPulse();
            } else {
                Intrinsics.throwUninitializedPropertyAccessException("mStackScroller");
                throw null;
            }
        }
    }

    @Override // com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener
    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        Intrinsics.checkParameterIsNotNull(notificationEntry, "entry");
        boolean shouldAnimateVisibility = shouldAnimateVisibility();
        if (!z) {
            if (!(this.mLinearDozeAmount == 0.0f || this.mLinearVisibilityAmount == 0.0f)) {
                if (notificationEntry.isRowDismissed()) {
                    shouldAnimateVisibility = false;
                } else if (!this.mWakingUp && !this.willWakeUp) {
                    notificationEntry.setAmbientGoingAway(true);
                    this.mEntrySetToClearWhenFinished.add(notificationEntry);
                }
            }
        } else if (this.mEntrySetToClearWhenFinished.contains(notificationEntry)) {
            this.mEntrySetToClearWhenFinished.remove(notificationEntry);
            notificationEntry.setAmbientGoingAway(false);
        }
        updateNotificationVisibility(shouldAnimateVisibility, false);
    }

    private final boolean shouldAnimateVisibility() {
        return this.mDozeParameters.getAlwaysOn() && !this.mDozeParameters.getDisplayNeedsBlanking();
    }
}
