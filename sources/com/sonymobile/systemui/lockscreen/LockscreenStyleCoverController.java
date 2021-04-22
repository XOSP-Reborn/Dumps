package com.sonymobile.systemui.lockscreen;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import com.android.systemui.Dependency;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.google.android.collect.Lists;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LockscreenStyleCoverController {
    private final ArrayList<WeakReference<LockscreenStyleCoverControllerCallback>> mCallbacks = Lists.newArrayList();
    private final Context mContext;
    private final Runnable mObserverRunnable = new Runnable() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController.AnonymousClass2 */

        public void run() {
            for (int i = 0; i < LockscreenStyleCoverController.this.mCallbacks.size(); i++) {
                LockscreenStyleCoverControllerCallback lockscreenStyleCoverControllerCallback = (LockscreenStyleCoverControllerCallback) ((WeakReference) LockscreenStyleCoverController.this.mCallbacks.get(i)).get();
                if (lockscreenStyleCoverControllerCallback != null) {
                    lockscreenStyleCoverControllerCallback.onStyleCoverClosed(LockscreenStyleCoverController.this.isStyleCoverViewSelectedAndClosed());
                }
            }
        }
    };
    private final Handler mObseverHandler = new Handler();
    private final ContentResolver mResolver;
    private ScrimController mScrimController;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private ContentObserver mStyleCoverObserver = new ContentObserver(new Handler()) {
        /* class com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController.AnonymousClass1 */

        public void onChange(boolean z) {
            LockscreenStyleCoverController.this.mObseverHandler.removeCallbacks(LockscreenStyleCoverController.this.mObserverRunnable);
            boolean isStyleCoverViewSelectedAndClosed = LockscreenStyleCoverController.this.isStyleCoverViewSelectedAndClosed();
            if (isStyleCoverViewSelectedAndClosed) {
                LockscreenStyleCoverController.this.mObseverHandler.postDelayed(LockscreenStyleCoverController.this.mObserverRunnable, 1000);
            } else {
                LockscreenStyleCoverController.this.mObseverHandler.post(LockscreenStyleCoverController.this.mObserverRunnable);
            }
            if (LockscreenStyleCoverController.this.mStatusBarKeyguardViewManager != null && !isStyleCoverViewSelectedAndClosed) {
                LockscreenStyleCoverController.this.mStatusBarKeyguardViewManager.onBackPressed(false);
            }
            ((SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class)).updateLockColors(isStyleCoverViewSelectedAndClosed);
            if (LockscreenStyleCoverController.this.mScrimController != null) {
                LockscreenStyleCoverController.this.mScrimController.setStyleCoverViewSelectedAndClosed(isStyleCoverViewSelectedAndClosed);
            }
        }
    };

    public LockscreenStyleCoverController(Context context) {
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
    }

    public void init(ScrimController scrimController, StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mScrimController = scrimController;
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
        this.mResolver.unregisterContentObserver(this.mStyleCoverObserver);
        this.mStyleCoverObserver.onChange(false);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor("somc.cover_closed"), true, this.mStyleCoverObserver);
    }

    public boolean isStyleCoverViewSelected() {
        return Settings.Global.getInt(this.mResolver, "somc.choice_cover_type", 0) == 4;
    }

    public boolean isStyleCoverClosed() {
        return Settings.Global.getInt(this.mResolver, "somc.cover_closed", 0) == 1;
    }

    public boolean isStyleCoverViewSelectedAndClosed() {
        return isStyleCoverViewSelected() && isStyleCoverClosed();
    }

    public void registerCallback(LockscreenStyleCoverControllerCallback lockscreenStyleCoverControllerCallback) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == lockscreenStyleCoverControllerCallback) {
                return;
            }
        }
        this.mCallbacks.add(new WeakReference<>(lockscreenStyleCoverControllerCallback));
        removeCallback(null);
    }

    public void removeCallback(LockscreenStyleCoverControllerCallback lockscreenStyleCoverControllerCallback) {
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            if (this.mCallbacks.get(size).get() == lockscreenStyleCoverControllerCallback) {
                this.mCallbacks.remove(size);
            }
        }
    }
}
