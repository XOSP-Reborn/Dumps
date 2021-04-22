package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserManager;
import com.sonymobile.keyguard.aod.PhotoPlaybackProviderUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockscreenStatisticsHelper {
    private static final String[] mDeviceNames = {"H82", "H92", "H81", "H91", "H83", "H93", "H84", "H94", "J81", "J91", "J82", "J92"};

    public static List<LockscreenStatisticsReporter> getReporters(Context context) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(getFingerprintCountReporter(context));
        arrayList.add(getSmartLockReporter(context));
        return Collections.unmodifiableList(arrayList);
    }

    private static LockscreenStatisticsReporter getFingerprintCountReporter(Context context) {
        return new LockscreenStatisticsFingerprintCountReporter(context, new LockscreenStatisticsUserClassifier(), (UserManager) context.getSystemService("user"));
    }

    private static LockscreenStatisticsReporter getSmartLockReporter(Context context) {
        return new LockscreenStatisticsSmartLockReporter(context, new LockscreenStatisticsUserClassifier(), (UserManager) context.getSystemService("user"));
    }

    public static List<LockscreenStatisticsReporter> getReportersWeekly(Context context) {
        ArrayList arrayList = new ArrayList();
        if (PhotoPlaybackProviderUtils.isPhotoPlaybackApplicationInstalled(context)) {
            arrayList.add(getPhotoPlaybackReporter(context));
        }
        if (isSupportedDeviceVersion()) {
            arrayList.add(getAmbientDisplayReporter(context));
            arrayList.add(getStickerReporter(context));
        }
        arrayList.add(getDoubleTapToLockscreenReporter(context));
        return Collections.unmodifiableList(arrayList);
    }

    private static LockscreenStatisticsReporter getPhotoPlaybackReporter(Context context) {
        return new LockscreenStatisticsPhotoPlaybackReporter(context);
    }

    private static LockscreenStatisticsReporter getAmbientDisplayReporter(Context context) {
        return new LockscreenStatisticsAmbientDisplayReporter(context, new LockscreenStatisticsUserClassifier(), (UserManager) context.getSystemService("user"));
    }

    private static boolean isSupportedDeviceVersion() {
        String str = SystemProperties.get("ro.semc.product.device", "default");
        for (String str2 : mDeviceNames) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private static LockscreenStatisticsReporter getStickerReporter(Context context) {
        return new LockscreenStatisticsStickerReporter(context, new LockscreenStatisticsUserClassifier(), (UserManager) context.getSystemService("user"));
    }

    private static LockscreenStatisticsReporter getDoubleTapToLockscreenReporter(Context context) {
        return new LockscreenStatisticsDoubleTapToLockscreenUsageReporter(context);
    }
}
