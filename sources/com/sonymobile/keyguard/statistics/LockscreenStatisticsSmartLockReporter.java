package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.util.List;

public class LockscreenStatisticsSmartLockReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final Context mContext;
    private final LockscreenStatisticsUserClassifier mLockscreenStatisticsUserClassifier;
    private final UserManager mUserManager;

    public LockscreenStatisticsSmartLockReporter(Context context, LockscreenStatisticsUserClassifier lockscreenStatisticsUserClassifier, UserManager userManager) {
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
        reportSmartLockUsage();
        return null;
    }

    private void reportSmartLockUsage() {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        for (UserInfo userInfo : users) {
            sendIddReportAsUser(instance.getUserTrustIsManaged(userInfo.id), this.mLockscreenStatisticsUserClassifier.getUserType(userInfo), KeyguardUpdateMonitor.getCurrentUser() == userInfo.id);
        }
    }

    private void sendIddReportAsUser(boolean z, String str, boolean z2) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_SMARTLOCK_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("smartlock_settings", z);
        intent.putExtra("user_type", str);
        intent.putExtra("is_active", z2);
        this.mContext.sendBroadcast(intent);
    }
}
