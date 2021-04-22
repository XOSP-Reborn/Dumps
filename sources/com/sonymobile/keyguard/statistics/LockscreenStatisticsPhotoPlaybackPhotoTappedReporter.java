package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;

public final class LockscreenStatisticsPhotoPlaybackPhotoTappedReporter {
    public static void sendEvent(Context context, int i) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_PHOTOPLAYBACK_PHOTO_TAPPED");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("photo_type", i);
        context.sendBroadcast(intent);
    }
}
