package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IWallpaperManager;
import android.app.IWallpaperManagerCallback;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationMediaManager;
import libcore.io.IoUtils;

public class LockscreenWallpaper extends IWallpaperManagerCallback.Stub implements Runnable {
    private final StatusBar mBar;
    private Bitmap mCache;
    private boolean mCached;
    private int mCurrentUserId;
    private final Handler mH;
    private AsyncTask<Void, Void, LoaderResult> mLoader;
    private final NotificationMediaManager mMediaManager = ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class));
    private UserHandle mSelectedUser;
    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final WallpaperManager mWallpaperManager;

    public void onWallpaperColorsChanged(WallpaperColors wallpaperColors, int i, int i2) {
    }

    public LockscreenWallpaper(Context context, StatusBar statusBar, Handler handler) {
        this.mBar = statusBar;
        this.mH = handler;
        this.mWallpaperManager = (WallpaperManager) context.getSystemService("wallpaper");
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        try {
            IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).setLockWallpaperCallback(this);
        } catch (RemoteException e) {
            Log.e("LockscreenWallpaper", "System dead?" + e);
        }
    }

    public Bitmap getBitmap() {
        if (this.mCached) {
            return this.mCache;
        }
        boolean z = true;
        if (!this.mWallpaperManager.isWallpaperSupported()) {
            this.mCached = true;
            this.mCache = null;
            return null;
        }
        LoaderResult loadBitmap = loadBitmap(this.mCurrentUserId, this.mSelectedUser);
        if (loadBitmap.success) {
            this.mCached = true;
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
            if (loadBitmap.bitmap == null) {
                z = false;
            }
            keyguardUpdateMonitor.setHasLockscreenWallpaper(z);
            this.mCache = loadBitmap.bitmap;
        }
        return this.mCache;
    }

    public LoaderResult loadBitmap(int i, UserHandle userHandle) {
        if (userHandle != null) {
            i = userHandle.getIdentifier();
        }
        ParcelFileDescriptor wallpaperFile = this.mWallpaperManager.getWallpaperFile(2, i);
        if (wallpaperFile != null) {
            try {
                return LoaderResult.success(BitmapFactory.decodeFileDescriptor(wallpaperFile.getFileDescriptor(), null, new BitmapFactory.Options()));
            } catch (OutOfMemoryError e) {
                Log.w("LockscreenWallpaper", "Can't decode file", e);
                return LoaderResult.fail();
            } finally {
                IoUtils.closeQuietly(wallpaperFile);
            }
        } else if (userHandle != null) {
            return LoaderResult.success(this.mWallpaperManager.getBitmapAsUser(userHandle.getIdentifier(), true));
        } else {
            return LoaderResult.success(null);
        }
    }

    public void setCurrentUser(int i) {
        if (i != this.mCurrentUserId) {
            UserHandle userHandle = this.mSelectedUser;
            if (userHandle == null || i != userHandle.getIdentifier()) {
                this.mCached = false;
            }
            this.mCurrentUserId = i;
        }
    }

    public void onWallpaperChanged() {
        postUpdateWallpaper();
    }

    private void postUpdateWallpaper() {
        this.mH.removeCallbacks(this);
        this.mH.post(this);
    }

    public void run() {
        AsyncTask<Void, Void, LoaderResult> asyncTask = this.mLoader;
        if (asyncTask != null) {
            asyncTask.cancel(false);
        }
        final int i = this.mCurrentUserId;
        final UserHandle userHandle = this.mSelectedUser;
        this.mLoader = new AsyncTask<Void, Void, LoaderResult>() {
            /* class com.android.systemui.statusbar.phone.LockscreenWallpaper.AnonymousClass1 */

            /* access modifiers changed from: protected */
            public LoaderResult doInBackground(Void... voidArr) {
                return LockscreenWallpaper.this.loadBitmap(i, userHandle);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(LoaderResult loaderResult) {
                super.onPostExecute((Object) loaderResult);
                if (!isCancelled()) {
                    if (loaderResult.success) {
                        LockscreenWallpaper.this.mCached = true;
                        LockscreenWallpaper.this.mCache = loaderResult.bitmap;
                        LockscreenWallpaper.this.mUpdateMonitor.setHasLockscreenWallpaper(loaderResult.bitmap != null);
                        LockscreenWallpaper.this.mMediaManager.updateMediaMetaData(true, true);
                    }
                    LockscreenWallpaper.this.mLoader = null;
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public static class LoaderResult {
        public final Bitmap bitmap;
        public final boolean success;

        LoaderResult(boolean z, Bitmap bitmap2) {
            this.success = z;
            if (bitmap2 == null || bitmap2.getByteCount() > 104857600) {
                this.bitmap = null;
            } else {
                this.bitmap = bitmap2;
            }
        }

        static LoaderResult success(Bitmap bitmap2) {
            return new LoaderResult(true, bitmap2);
        }

        static LoaderResult fail() {
            return new LoaderResult(false, null);
        }
    }
}
