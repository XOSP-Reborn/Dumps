package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.AsyncTask;
import android.os.UserManager;
import android.provider.Settings;
import com.android.keyguard.KeyguardUpdateMonitor;

public class LockscreenStatisticsAmbientDisplayReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private final Context mContext;
    private final String[] mKeys = {"doze_always_on", "doze_take_on", "doze_pulse_on_pick_up", "doze_off"};
    private final LockscreenStatisticsUserClassifier mLockscreenStatisticsUserClassifier;
    private final UserManager mUserManager;

    public LockscreenStatisticsAmbientDisplayReporter(Context context, LockscreenStatisticsUserClassifier lockscreenStatisticsUserClassifier, UserManager userManager) {
        this.mContext = context;
        this.mLockscreenStatisticsUserClassifier = lockscreenStatisticsUserClassifier;
        this.mUserManager = userManager;
        this.mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(context);
    }

    @Override // com.sonymobile.keyguard.statistics.LockscreenStatisticsReporter
    public void sendIddReport() {
        execute(new Void[0]);
    }

    /* access modifiers changed from: protected */
    public Void doInBackground(Void... voidArr) {
        reportAmbientDisplayUsage();
        return null;
    }

    private void reportAmbientDisplayUsage() {
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            sendIddReportAsUser(getAmbientSettings(userInfo.id), this.mLockscreenStatisticsUserClassifier.getUserType(userInfo), KeyguardUpdateMonitor.getCurrentUser() == userInfo.id);
        }
    }

    private int getAmbientSettings(int i) {
        for (int i2 = 0; i2 < this.mKeys.length; i2++) {
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), this.mKeys[i2], 0, i) != 0) {
                return i2;
            }
        }
        return this.mAmbientDisplayConfiguration.alwaysOnAvailable() ? 1 : 2;
    }

    private void sendIddReportAsUser(int i, String str, boolean z) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_AMBIENTDISPLAY_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("ambient_settings", i);
        intent.putExtra("user_type", str);
        intent.putExtra("is_active", z);
        intent.putExtra("is_active", z);
        this.mContext.sendBroadcast(intent);
    }
}
