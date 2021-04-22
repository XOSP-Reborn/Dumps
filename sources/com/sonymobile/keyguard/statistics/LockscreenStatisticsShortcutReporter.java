package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;

public class LockscreenStatisticsShortcutReporter {

    public enum Types {
        Camera,
        VoiceAssist,
        Phone
    }

    public static void sendEvent(Context context, Types types) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_SHORTCUT_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("shortcut_type", types.toString());
        context.sendBroadcast(intent);
    }
}
