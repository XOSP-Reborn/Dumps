package com.android.keyguard.clock;

import android.content.res.Resources;
import android.util.MathUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.R$dimen;

class SmallClockPosition {
    private final int mBurnInOffsetY;
    private float mDarkAmount;
    private final int mKeyguardLockHeight;
    private final int mKeyguardLockPadding;
    private final int mStatusBarHeight;

    SmallClockPosition(Resources resources) {
        this(resources.getDimensionPixelSize(R$dimen.status_bar_height), resources.getDimensionPixelSize(R$dimen.keyguard_lock_padding), resources.getDimensionPixelSize(R$dimen.keyguard_lock_height), resources.getDimensionPixelSize(R$dimen.burn_in_prevention_offset_y));
    }

    @VisibleForTesting
    SmallClockPosition(int i, int i2, int i3, int i4) {
        this.mStatusBarHeight = i;
        this.mKeyguardLockPadding = i2;
        this.mKeyguardLockHeight = i3;
        this.mBurnInOffsetY = i4;
    }

    /* access modifiers changed from: package-private */
    public void setDarkAmount(float f) {
        this.mDarkAmount = f;
    }

    /* access modifiers changed from: package-private */
    public int getPreferredY() {
        int i = this.mStatusBarHeight;
        int i2 = this.mKeyguardLockPadding;
        return (int) MathUtils.lerp((float) (i + this.mKeyguardLockHeight + (i2 * 2)), (float) (i + i2 + this.mBurnInOffsetY), this.mDarkAmount);
    }
}
