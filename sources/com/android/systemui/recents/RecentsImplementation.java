package com.android.systemui.recents;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import com.android.systemui.SysUiServiceProvider;
import java.io.PrintWriter;

/* access modifiers changed from: package-private */
public interface RecentsImplementation {
    default void cancelPreloadRecentApps() {
    }

    default void dump(PrintWriter printWriter) {
    }

    default void growRecents() {
    }

    default void hideRecentApps(boolean z, boolean z2) {
    }

    default void onAppTransitionFinished() {
    }

    default void onBootCompleted() {
    }

    default void onConfigurationChanged(Configuration configuration) {
    }

    default void onStart(Context context, SysUiServiceProvider sysUiServiceProvider) {
    }

    default void preloadRecentApps() {
    }

    default void showRecentApps(boolean z) {
    }

    default boolean splitPrimaryTask(int i, Rect rect, int i2) {
        return false;
    }

    default void toggleRecentApps() {
    }
}
