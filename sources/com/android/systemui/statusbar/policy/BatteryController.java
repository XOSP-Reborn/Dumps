package com.android.systemui.statusbar.policy;

import com.android.systemui.DemoMode;
import com.android.systemui.Dumpable;

public interface BatteryController extends DemoMode, Dumpable, CallbackController<BatteryStateChangeCallback> {

    public interface BatteryStateChangeCallback {
        default void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        }

        default void onPowerSaveChanged(boolean z) {
        }
    }

    public interface EstimateFetchCompletion {
        void onBatteryRemainingEstimateRetrieved(String str);
    }

    default void getEstimatedTimeRemainingString(EstimateFetchCompletion estimateFetchCompletion) {
    }

    boolean isPowerSave();

    void setPowerSaveMode(boolean z);

    default boolean isAodPowerSave() {
        return isPowerSave();
    }
}
