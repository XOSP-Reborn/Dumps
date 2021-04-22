package com.sonymobile.systemui.lockscreen;

import android.app.ActivityManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public class LockscreenAlbumArtController {
    private final Context mContext;
    private int mCurrentUserId;
    private boolean mShowAlbumArt;
    private ContentObserver mShowAlbumArtObserver = new ContentObserver(new Handler()) {
        /* class com.sonymobile.systemui.lockscreen.LockscreenAlbumArtController.AnonymousClass1 */

        public void onChange(boolean z) {
            LockscreenAlbumArtController.this.updateShowAlbumArt();
        }
    };

    public LockscreenAlbumArtController(Context context) {
        this.mContext = context;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
    }

    public void initShowAlbumArtObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mShowAlbumArtObserver);
        this.mShowAlbumArtObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("somc.albumart_enabled"), true, this.mShowAlbumArtObserver, this.mCurrentUserId);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateShowAlbumArt() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "somc.albumart_enabled", 1, this.mCurrentUserId) == 0) {
            z = false;
        }
        this.mShowAlbumArt = z;
    }

    public final void onUserSwitched(int i) {
        this.mCurrentUserId = i;
        initShowAlbumArtObserver();
    }

    public final boolean shouldShowAlbumArt() {
        return this.mShowAlbumArt;
    }
}
