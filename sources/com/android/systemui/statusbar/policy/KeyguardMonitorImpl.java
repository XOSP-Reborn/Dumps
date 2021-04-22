package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.internal.util.Preconditions;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.ArrayList;

public class KeyguardMonitorImpl extends KeyguardUpdateMonitorCallback implements KeyguardMonitor {
    private final ArrayList<KeyguardMonitor.Callback> mCallbacks = new ArrayList<>();
    private final Context mContext;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    private boolean mKeyguardGoingAway;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mLaunchTransitionFadingAway;
    private boolean mListening;
    private boolean mOccluded;
    private boolean mSecure;
    private boolean mShowing;

    public KeyguardMonitorImpl(Context context) {
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void addCallback(KeyguardMonitor.Callback callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null. b/128895449");
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() != 0 && !this.mListening) {
            this.mListening = true;
            this.mKeyguardUpdateMonitor.registerCallback(this);
        }
    }

    public void removeCallback(KeyguardMonitor.Callback callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null. b/128895449");
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0 && this.mListening) {
            this.mListening = false;
            this.mKeyguardUpdateMonitor.removeCallback(this);
        }
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isShowing() {
        return this.mShowing;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isSecure() {
        return this.mSecure;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isOccluded() {
        return this.mOccluded;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public void notifyKeyguardState(boolean z, boolean z2, boolean z3) {
        if (this.mShowing != z || this.mSecure != z2 || this.mOccluded != z3) {
            this.mShowing = z;
            this.mSecure = z2;
            this.mOccluded = z3;
            notifyKeyguardChanged();
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onTrustChanged(int i) {
        notifyKeyguardChanged();
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isDeviceInteractive() {
        return this.mKeyguardUpdateMonitor.isDeviceInteractive();
    }

    private void notifyKeyguardChanged() {
        new ArrayList(this.mCallbacks).forEach($$Lambda$CusFj6pVztwBZlitsnMLA9Hx95I.INSTANCE);
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public void notifyKeyguardFadingAway(long j, long j2) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = j;
        this.mKeyguardFadingAwayDuration = j2;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public void notifyKeyguardDoneFading() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardGoingAway = false;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public void notifyKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public void setLaunchTransitionFadingAway(boolean z) {
        this.mLaunchTransitionFadingAway = z;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isLaunchTransitionFadingAway() {
        return this.mLaunchTransitionFadingAway;
    }
}
