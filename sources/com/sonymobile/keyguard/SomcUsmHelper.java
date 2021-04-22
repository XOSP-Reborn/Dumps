package com.sonymobile.keyguard;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class SomcUsmHelper {
    private static final boolean DEBUG = Log.isLoggable("SomcUsmHelper", 3);

    public static boolean isUsmEnabled(Context context) {
        boolean z = false;
        if (Settings.Secure.getInt(context.getContentResolver(), "somc.ultrastamina_mode", 0) != 0) {
            z = true;
        }
        if (DEBUG) {
            Log.d("SomcUsmHelper", "isUsmEnabled() return " + z);
        }
        return z;
    }
}
