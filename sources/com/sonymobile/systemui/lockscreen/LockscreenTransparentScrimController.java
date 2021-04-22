package com.sonymobile.systemui.lockscreen;

import android.app.ActivityManager;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.ServiceManager;
import android.util.Log;
import com.android.systemui.statusbar.phone.ScrimController;

public class LockscreenTransparentScrimController {
    private static final String TAG = "LockscreenTransparentScrimController";
    private Context mContext;
    private int mCurrentUserId;
    private boolean mHasArtwork = false;
    private ScrimController mScrimController;
    private boolean mSeeThroughScrim = false;

    public LockscreenTransparentScrimController(Context context) {
        this.mContext = context;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
    }

    public final void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    public final void updateHasArtwork(boolean z) {
        boolean shouldSeeThroughScrim = shouldSeeThroughScrim();
        if (this.mSeeThroughScrim != shouldSeeThroughScrim || this.mHasArtwork != z) {
            this.mHasArtwork = z;
            this.mSeeThroughScrim = shouldSeeThroughScrim;
            updateSeeThroughScrimState();
        }
    }

    private void updateSeeThroughScrimState() {
        ScrimController scrimController = this.mScrimController;
        if (scrimController != null) {
            scrimController.updateSeeThroughScrimState(this.mSeeThroughScrim && !this.mHasArtwork);
        }
    }

    private boolean shouldSeeThroughScrim() {
        WallpaperManager wallpaperManager = (WallpaperManager) this.mContext.getSystemService("wallpaper");
        IWallpaperManager asInterface = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
        try {
            if (asInterface.getWallpaperIdForUser(2, this.mCurrentUserId) < 0) {
                try {
                    WallpaperInfo wallpaperInfo = asInterface.getWallpaperInfo(this.mCurrentUserId);
                    ComponentName defaultWallpaperComponent = WallpaperManager.getDefaultWallpaperComponent(this.mContext);
                    if (wallpaperInfo == null || defaultWallpaperComponent == null || !wallpaperInfo.getPackageName().equals(defaultWallpaperComponent.getPackageName())) {
                        return false;
                    }
                    return true;
                } catch (Exception unused) {
                    Log.e(TAG, "Exception thrown for getWallpaperInfo()");
                }
            }
            return false;
        } catch (Exception unused2) {
            Log.e(TAG, "Exception thrown for getWallpaperIdForUser()");
            return false;
        }
    }
}
