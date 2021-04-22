package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;

public final class LockscreenStatisticsFingerprintLockOutReporter {

    public enum LockOutTrigger {
        lockout_five,
        lockout_twenty,
        lockout_timeout
    }

    public static void sendEvent(Context context, LockOutTrigger lockOutTrigger) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_FINGERPRINT_LOCKOUT");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("lockout_trigger", lockOutTrigger.toString());
        context.sendBroadcast(intent);
    }
}
