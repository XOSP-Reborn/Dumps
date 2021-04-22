package com.sonymobile.settingslib;

import android.content.res.Resources;
import java.util.HashSet;

public final class SomcConfig {
    public static boolean isBlockableNotificationPackages(Resources resources, String str) {
        HashSet hashSet = new HashSet();
        for (String str2 : resources.getStringArray(17235994)) {
            hashSet.add(str2);
        }
        return hashSet.contains(str);
    }
}
