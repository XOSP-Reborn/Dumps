package com.sonymobile.systemui.emergencymode;

import android.content.Context;
import android.provider.Settings;

public class EmergencyModeStatus {
    public static boolean isEmergencyModeOn(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(), "somc.emergency_mode", 0) != 0) {
            return true;
        }
        return false;
    }
}
