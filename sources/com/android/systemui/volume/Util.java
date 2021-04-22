package com.android.systemui.volume;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

/* access modifiers changed from: package-private */
public class Util extends com.android.settingslib.volume.Util {
    public static String logTag(Class<?> cls) {
        String str = "vol." + cls.getSimpleName();
        return str.length() < 23 ? str : str.substring(0, 23);
    }

    public static String ringerModeToString(int i) {
        if (i == 0) {
            return "RINGER_MODE_SILENT";
        }
        if (i == 1) {
            return "RINGER_MODE_VIBRATE";
        }
        if (i == 2) {
            return "RINGER_MODE_NORMAL";
        }
        return "RINGER_MODE_UNKNOWN_" + i;
    }

    public static final void setVisOrGone(View view, boolean z) {
        if (view != null) {
            int i = 0;
            if ((view.getVisibility() == 0) != z) {
                if (!z) {
                    i = 8;
                }
                view.setVisibility(i);
            }
        }
    }

    public static boolean isMediaVibrationActivated(AudioManager audioManager) {
        int i;
        String parameters = audioManager.getParameters("somc.media_vibration_path");
        if (parameters != null) {
            String[] split = parameters.split("=");
            if (split.length > 1) {
                i = Integer.parseInt(split[1]);
                return i == 1 && audioManager.isMusicActive();
            }
        }
        i = 0;
        if (i == 1) {
            return false;
        }
    }

    public static String getAppName(Context context) {
        String string = Settings.System.getString(context.getContentResolver(), "somc.media_vibration_package_name");
        if (string == null) {
            return "";
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = context.createPackageContextAsUser(string, 0, UserHandle.of(ActivityManager.getCurrentUser())).getApplicationInfo();
            if (applicationInfo != null) {
                return packageManager.getApplicationLabel(applicationInfo).toString();
            }
            return "";
        } catch (PackageManager.NameNotFoundException unused) {
            return "";
        }
    }
}
