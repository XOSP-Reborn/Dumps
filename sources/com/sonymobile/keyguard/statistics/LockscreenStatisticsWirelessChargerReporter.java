package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;

public final class LockscreenStatisticsWirelessChargerReporter {
    public static void sendEvent(Context context, boolean z) {
        String str;
        Configuration configuration;
        Resources resources = context.getResources();
        if (!(resources == null || (configuration = resources.getConfiguration()) == null || z)) {
            int i = configuration.orientation;
            if (i == 1) {
                str = "Portrait";
            } else if (i == 2) {
                str = "Landscape";
            }
            Intent intent = new Intent();
            intent.setAction("com.sonymobile.lockscreen.idd.ACTION_WIRELESSCHARGER_EVENT");
            intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
            intent.putExtra("is_connect", z);
            intent.putExtra("orientation", str);
            context.sendBroadcast(intent);
        }
        str = "Undefined";
        Intent intent2 = new Intent();
        intent2.setAction("com.sonymobile.lockscreen.idd.ACTION_WIRELESSCHARGER_EVENT");
        intent2.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent2.putExtra("is_connect", z);
        intent2.putExtra("orientation", str);
        context.sendBroadcast(intent2);
    }
}
