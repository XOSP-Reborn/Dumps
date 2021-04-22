package com.sonymobile.systemui.lockscreen;

import android.app.ActivityManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public class LockscreenAmbientDisplayController {
    private final Context mContext;
    private int mCurrentUserId;
    private boolean mShowMusicInfo;
    private ContentObserver mShowMusicInfoObserver = new ContentObserver(new Handler()) {
        /* class com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController.AnonymousClass1 */

        public void onChange(boolean z) {
            LockscreenAmbientDisplayController.this.updateShowMusicInfo();
        }
    };
    private ContentObserver mStickerObserver = new ContentObserver(new Handler()) {
        /* class com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController.AnonymousClass2 */

        public void onChange(boolean z) {
            LockscreenAmbientDisplayController.this.updateSticker();
        }
    };
    private String mStickerUri;

    public LockscreenAmbientDisplayController(Context context) {
        this.mContext = context;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
    }

    public void initObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mShowMusicInfoObserver);
        this.mContext.getContentResolver().unregisterContentObserver(this.mStickerObserver);
        this.mShowMusicInfoObserver.onChange(false);
        this.mStickerObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("somc.musicinfo_enabled"), true, this.mShowMusicInfoObserver, this.mCurrentUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("somc.doze_sticker"), true, this.mStickerObserver, this.mCurrentUserId);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateShowMusicInfo() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "somc.musicinfo_enabled", 1, this.mCurrentUserId) == 0) {
            z = false;
        }
        this.mShowMusicInfo = z;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSticker() {
        this.mStickerUri = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "somc.doze_sticker", this.mCurrentUserId);
    }

    public final boolean shouldShowMusicInfo() {
        return this.mShowMusicInfo;
    }

    public final String getStickerUri() {
        return this.mStickerUri;
    }
}
