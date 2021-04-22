package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.UserManager;
import android.provider.Settings;
import com.android.keyguard.KeyguardUpdateMonitor;

public class LockscreenStatisticsStickerReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final Context mContext;
    private final LockscreenStatisticsUserClassifier mLockscreenStatisticsUserClassifier;
    private final UserManager mUserManager;

    public LockscreenStatisticsStickerReporter(Context context, LockscreenStatisticsUserClassifier lockscreenStatisticsUserClassifier, UserManager userManager) {
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
        reportStickerUsage();
        return null;
    }

    private void reportStickerUsage() {
        for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
            sendIddReportAsUser(getStickerSettings(userInfo.id), this.mLockscreenStatisticsUserClassifier.getUserType(userInfo), KeyguardUpdateMonitor.getCurrentUser() == userInfo.id);
        }
    }

    private String getStickerSettings(int i) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "somc.doze_sticker", i);
        if (stringForUser != null && !stringForUser.equals("")) {
            Uri parse = Uri.parse(stringForUser);
            if (parse.getScheme().equals("resource")) {
                return parse.getPath().substring(1);
            }
            if (parse.getScheme().equals("file")) {
                return "Album";
            }
        }
        return "None";
    }

    private void sendIddReportAsUser(String str, String str2, boolean z) {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_STICKER_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("sticker_settings", str);
        intent.putExtra("user_type", str2);
        intent.putExtra("is_active", z);
        this.mContext.sendBroadcast(intent);
    }
}
