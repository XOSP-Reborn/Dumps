package com.android.systemui.statusbar.policy;

public interface KeyguardMonitor extends CallbackController<Callback> {

    public interface Callback {
        void onKeyguardShowingChanged();
    }

    long calculateGoingToFullShadeDelay();

    long getKeyguardFadingAwayDelay();

    long getKeyguardFadingAwayDuration();

    default boolean isDeviceInteractive() {
        return false;
    }

    boolean isKeyguardFadingAway();

    boolean isKeyguardGoingAway();

    boolean isLaunchTransitionFadingAway();

    boolean isOccluded();

    boolean isSecure();

    boolean isShowing();

    default void notifyKeyguardDoneFading() {
    }

    default void notifyKeyguardFadingAway(long j, long j2) {
    }

    default void notifyKeyguardGoingAway(boolean z) {
    }

    default void notifyKeyguardState(boolean z, boolean z2, boolean z3) {
    }

    default void setLaunchTransitionFadingAway(boolean z) {
    }
}
