package com.android.settingslib.accessibility;

import android.content.Context;
import android.provider.Settings;
import com.android.settingslib.R$bool;

public class AccessibilityUtils {
    public static String getShortcutTargetServiceComponentNameString(Context context, int i) {
        String stringForUser = Settings.Secure.getStringForUser(context.getContentResolver(), "accessibility_shortcut_target_service", i);
        if (stringForUser != null) {
            return stringForUser;
        }
        return context.getString(17039729);
    }

    public static boolean isShortcutEnabled(Context context, int i) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "accessibility_shortcut_enabled", 1, i) == 1;
    }

    public static boolean isAccessibilityShortcutGestureEnabled(Context context) {
        return context.getResources().getBoolean(R$bool.config_enableAccessibilityShortcutGesture);
    }
}
