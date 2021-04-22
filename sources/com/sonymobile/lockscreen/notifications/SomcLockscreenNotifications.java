package com.sonymobile.lockscreen.notifications;

import android.content.Context;
import android.provider.Settings;
import com.android.systemui.C0003R$bool;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;

public class SomcLockscreenNotifications {
    private static final SysuiStatusBarStateController mStatusBarStateController = ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class));

    public static boolean shouldFilterOutOldNotifications(Context context) {
        if ((mStatusBarStateController.getState() == 1 ? 1 : null) == null) {
            return false;
        }
        return Settings.Secure.getIntForUser(context.getContentResolver(), "somc.lockscreen.keep_notifications", context.getResources().getBoolean(C0003R$bool.config_keepLockscreenNotifications) ? 1 : 0, -2) == 0;
    }
}
