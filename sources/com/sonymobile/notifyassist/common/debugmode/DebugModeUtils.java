package com.sonymobile.notifyassist.common.debugmode;

import android.os.Build;
import android.os.SystemProperties;

public class DebugModeUtils {
    public static long getLatencyTimeForDebug() {
        int i;
        if (!Build.TYPE.equals("userdebug") || (i = SystemProperties.getInt("persist.debug.notify.short_latency_time", 20160)) <= 0) {
            return 1209600000;
        }
        return ((long) i) * 60 * 1000;
    }
}
