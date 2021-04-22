package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.sonymobile.keyguard.aod.PhotoPlaybackSharedPreferences;

public class LockscreenStatisticsPhotoPlaybackReporter extends AsyncTask<Void, Void, Void> implements LockscreenStatisticsReporter {
    private final Context mContext;

    public LockscreenStatisticsPhotoPlaybackReporter(Context context) {
        this.mContext = context;
    }

    @Override // com.sonymobile.keyguard.statistics.LockscreenStatisticsReporter
    public void sendIddReport() {
        execute(new Void[0]);
    }

    /* access modifiers changed from: protected */
    public Void doInBackground(Void... voidArr) {
        reportPhotoPkaybackUsage();
        return null;
    }

    private void reportPhotoPkaybackUsage() {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_PHOTOPLAYBACK_USAGE");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("oobe_shown", PhotoPlaybackSharedPreferences.getPhotoplaybackSharedPrefOobeShown(this.mContext));
        intent.putExtra("intro_shown", PhotoPlaybackSharedPreferences.getPhotoplaybackSharedPrefIntroShown(this.mContext));
        intent.putExtra("album_shown", PhotoPlaybackSharedPreferences.getPhotoplaybackSharedPrefAlbumShown(this.mContext));
        this.mContext.sendBroadcast(intent);
    }
}
