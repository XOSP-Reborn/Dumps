package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;

public final class LockscreenStatisticsAssistIconReporter {
    public static void sendReceiveEvent(Context context, String str, int i) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_ASSIST_RECEIVE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("version", str);
        intent.putExtra("res_id", i);
        context.sendBroadcast(intent);
    }

    public static void sendTapEvent(Context context, String str, int i, long j) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_ASSIST_TAP");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("version", str);
        intent.putExtra("res_id", i);
        intent.putExtra("show_count", j);
        context.sendBroadcast(intent);
    }
}
