package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.util.List;

public class LockscreenStatisticsFingerprintCountReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final Context mContext;
    private final LockscreenStatisticsUserClassifier mLockscreenStatisticsUserClassifier;
    private final UserManager mUserManager;

    public LockscreenStatisticsFingerprintCountReporter(Context context, LockscreenStatisticsUserClassifier lockscreenStatisticsUserClassifier, UserManager userManager) {
        this.mContext = context;
        this.mLockscreenStatisticsUserClassifier = lockscreenStatisticsUserClassifier;
        this.mUserManager = userManager;
    }

    @Override // com.sonymobile.keyguard.statistics.LockscreenStatisticsReporter
    public void sendIddReport() {
        execute(new Void[0]);
    }

    /* access modifiers changed from: protected */
    public Void doInBackground(Void... voidArr) {
        reportFingerprintCount();
        return null;
    }

    private void reportFingerprintCount() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        for (UserInfo userInfo : users) {
            long fingerprintCount = (long) instance.getFingerprintCount(userInfo.id);
            boolean z = KeyguardUpdateMonitor.getCurrentUser() == userInfo.id;
            String userType = this.mLockscreenStatisticsUserClassifier.getUserType(userInfo);
            if (fingerprintCount != -1) {
                sendIddReportAsUser(fingerprintCount, userType, z);
            }
        }
    }

    private void sendIddReportAsUser(long j, String str, boolean z) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_FINGERPRINT_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("registration_number", j);
        intent.putExtra("user_type", str);
        intent.putExtra("is_active", z);
        this.mContext.sendBroadcast(intent);
    }
}
