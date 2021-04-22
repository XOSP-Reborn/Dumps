package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.util.MathUtils;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Interpolators;
import com.android.systemui.doze.util.BurnInHelperKt;
import com.android.systemui.statusbar.notification.NotificationUtils;

public class KeyguardClockPositionAlgorithm {
    private static float CLOCK_HEIGHT_WEIGHT = 0.7f;
    private int mBurnInPreventionOffsetX;
    private int mBurnInPreventionOffsetY;
    private int mClockHeightAdjustment;
    private int mClockHeightAdjustmentByLayout;
    private int mClockNotificationsMargin;
    private int mClockPreferredY;
    private int mContainerTopPadding;
    private float mDarkAmount;
    private float mEmptyDragAmount;
    private boolean mHasCustomClock;
    private boolean mHasVisibleNotifs;
    private int mHeight;
    private int mKeyguardStatusHeight;
    private int mMaxShadeBottom;
    private int mMaxTopPaddingTemporaryStates;
    private int mMinTopMargin;
    private int mNotificationStackHeight;
    private float mPanelExpansion;
    private int mStackScrollerPaddingAdjustment;
    private int mYDistributedClockBottomMargin;

    public static class Result {
        public float clockAlpha;
        public int clockHeightAdjustment;
        public int clockX;
        public int clockY;
        public int stackScrollerPadding;
    }

    public void loadDimens(Resources resources) {
        this.mClockNotificationsMargin = resources.getDimensionPixelSize(C0005R$dimen.keyguard_clock_notifications_margin);
        this.mContainerTopPadding = Math.max(resources.getDimensionPixelSize(C0005R$dimen.keyguard_clock_top_margin), resources.getDimensionPixelSize(C0005R$dimen.keyguard_lock_height) + resources.getDimensionPixelSize(C0005R$dimen.keyguard_lock_padding) + resources.getDimensionPixelSize(C0005R$dimen.keyguard_clock_lock_margin));
        this.mBurnInPreventionOffsetX = resources.getDimensionPixelSize(C0005R$dimen.burn_in_prevention_offset_x);
        this.mBurnInPreventionOffsetY = resources.getDimensionPixelSize(C0005R$dimen.burn_in_prevention_offset_y);
        this.mClockHeightAdjustmentByLayout = resources.getDimensionPixelSize(C0005R$dimen.keyguard_clock_height_adjustment);
    }

    public void setup(int i, int i2, int i3, float f, int i4, int i5, int i6, boolean z, boolean z2, float f2, float f3) {
        this.mMinTopMargin = i + this.mContainerTopPadding;
        this.mMaxShadeBottom = i2;
        this.mNotificationStackHeight = i3;
        this.mPanelExpansion = f;
        this.mHeight = i4;
        this.mKeyguardStatusHeight = i5;
        this.mClockPreferredY = i6;
        this.mHasCustomClock = z;
        this.mHasVisibleNotifs = z2;
        this.mDarkAmount = f2;
        this.mEmptyDragAmount = f3;
    }

    public final void setupSomcClockPositionAdjustments(int i, int i2, int i3) {
        this.mMaxTopPaddingTemporaryStates = i3;
        int i4 = 0;
        this.mClockHeightAdjustment = 0;
        this.mYDistributedClockBottomMargin = 0;
        this.mStackScrollerPaddingAdjustment = 0;
        int expandedClockPosition = getExpandedClockPosition();
        this.mClockHeightAdjustment = Math.min(0, i2 - (this.mKeyguardStatusHeight + expandedClockPosition));
        int i5 = this.mClockHeightAdjustment;
        if (i5 < i) {
            i4 = i5 - i;
            this.mClockHeightAdjustment = i;
        }
        this.mYDistributedClockBottomMargin = getYDistributedClockBottomMargin((float) expandedClockPosition);
        this.mStackScrollerPaddingAdjustment = this.mYDistributedClockBottomMargin + i4;
    }

    public void run(Result result) {
        int clockY = getClockY();
        result.clockY = clockY;
        result.clockAlpha = getClockAlpha(clockY);
        result.stackScrollerPadding = clockY + this.mKeyguardStatusHeight;
        result.clockX = (int) NotificationUtils.interpolate(0.0f, burnInPreventionOffsetX(), this.mDarkAmount);
        result.clockHeightAdjustment = this.mClockHeightAdjustment;
    }

    public void adjustStackScrollerPadding(Result result) {
        int i = result.stackScrollerPadding;
        int i2 = this.mStackScrollerPaddingAdjustment;
        int i3 = i + i2;
        int i4 = this.mMaxTopPaddingTemporaryStates;
        if (i4 < i3) {
            this.mStackScrollerPaddingAdjustment = i2 - (i3 - i4);
        }
        result.stackScrollerPadding += this.mStackScrollerPaddingAdjustment;
    }

    private int getYDistributedClockBottomMargin(float f) {
        return Math.max(0, (int) (((f - ((float) this.mMinTopMargin)) - ((float) this.mContainerTopPadding)) * 0.33333334f));
    }

    public final float getMinStackScrollerPadding(int i) {
        return (float) (this.mMinTopMargin + i + this.mClockNotificationsMargin);
    }

    private int getMaxClockY() {
        return Math.max(((this.mHeight / 2) - this.mKeyguardStatusHeight) - this.mClockNotificationsMargin, this.mMinTopMargin + this.mContainerTopPadding + this.mClockHeightAdjustment);
    }

    private int getPreferredClockY() {
        return this.mClockPreferredY;
    }

    private int getExpandedPreferredClockY() {
        if (!this.mHasCustomClock || this.mHasVisibleNotifs) {
            return getExpandedClockPosition(true);
        }
        return getPreferredClockY();
    }

    public int getExpandedClockPosition() {
        return getExpandedClockPosition(false);
    }

    public int getExpandedClockPosition(boolean z) {
        int i = this.mMaxShadeBottom;
        int i2 = this.mMinTopMargin;
        float f = (((((float) (((i - i2) / 2) + i2)) - (((float) (this.mKeyguardStatusHeight + this.mClockHeightAdjustment)) * CLOCK_HEIGHT_WEIGHT)) - ((float) this.mClockNotificationsMargin)) - ((float) (this.mNotificationStackHeight / 2))) - ((float) this.mClockHeightAdjustmentByLayout);
        if (f < ((float) i2)) {
            f = (float) i2;
        }
        float f2 = 0.0f;
        float f3 = f + (z ? (float) this.mClockHeightAdjustment : 0.0f);
        float maxClockY = (float) getMaxClockY();
        if (f3 > maxClockY) {
            f3 = maxClockY;
        }
        if (z) {
            f2 = (float) this.mYDistributedClockBottomMargin;
        }
        return (int) (f3 - f2);
    }

    private int getClockY() {
        float max = MathUtils.max(0.0f, ((float) (this.mHasCustomClock ? getPreferredClockY() : getMaxClockY())) + burnInPreventionOffsetY());
        float f = (float) (-this.mKeyguardStatusHeight);
        float interpolation = Interpolators.FAST_OUT_LINEAR_IN.getInterpolation(this.mPanelExpansion);
        return (int) (MathUtils.lerp(MathUtils.lerp(f, (float) getExpandedPreferredClockY(), interpolation), MathUtils.lerp(f, max, interpolation), this.mDarkAmount) + this.mEmptyDragAmount);
    }

    private float getClockAlpha(int i) {
        return MathUtils.lerp(Interpolators.ACCELERATE.getInterpolation(Math.max(0.0f, ((float) i) / Math.max(1.0f, (float) getExpandedPreferredClockY()))), 1.0f, this.mDarkAmount);
    }

    private float burnInPreventionOffsetY() {
        return (float) (BurnInHelperKt.getBurnInOffset(this.mBurnInPreventionOffsetY * 2, false) - this.mBurnInPreventionOffsetY);
    }

    private float burnInPreventionOffsetX() {
        return (float) (BurnInHelperKt.getBurnInOffset(this.mBurnInPreventionOffsetX * 2, true) - this.mBurnInPreventionOffsetX);
    }
}
