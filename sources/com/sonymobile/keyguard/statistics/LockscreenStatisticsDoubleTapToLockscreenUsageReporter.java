package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;

public class LockscreenStatisticsDoubleTapToLockscreenUsageReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final Context mContext;
    private boolean mDoubleTapToLockscreenOnceActivated = false;
    private boolean mIsDoubleTapToLockscreenON = false;

    public LockscreenStatisticsDoubleTapToLockscreenUsageReporter(Context context) {
        this.mContext = context;
    }

    @Override // com.sonymobile.keyguard.statistics.LockscreenStatisticsReporter
    public void sendIddReport() {
        execute(new Void[0]);
    }

    /* access modifiers changed from: protected */
    public Void doInBackground(Void... voidArr) {
        isDoubleTapToLockscreenON();
        reportDoubleTapToLockscreenUsage();
        return null;
    }

    private void reportDoubleTapToLockscreenUsage() {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_DOUBLE_TAP_TO_LOCKSCREEN_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("double_tap_to_lockscreen_switch", this.mIsDoubleTapToLockscreenON);
        intent.putExtra("double_tap_to_lockscreen_activated", this.mDoubleTapToLockscreenOnceActivated);
        this.mContext.sendBroadcast(intent);
    }

    private void isDoubleTapToLockscreenON() {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "double_tap_to_lockscreen_setting", -1, -2);
        if (intForUser != -1) {
            this.mDoubleTapToLockscreenOnceActivated = true;
            if (intForUser == 1) {
                this.mIsDoubleTapToLockscreenON = true;
            } else {
                this.mIsDoubleTapToLockscreenON = false;
            }
        }
    }
}
