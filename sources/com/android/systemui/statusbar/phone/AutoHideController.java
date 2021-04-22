package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.MotionEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationRemoteInputManager;

public class AutoHideController implements CommandQueue.Callbacks {
    private final Runnable mAutoHide = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$AutoHideController$sJYAhc6qJ_sO_ZdtlpSd2BPK504 */

        public final void run() {
            AutoHideController.this.lambda$new$0$AutoHideController();
        }
    };
    private boolean mAutoHideSuspended;
    private final CommandQueue mCommandQueue;
    @VisibleForTesting
    int mDisplayId;
    private final Handler mHandler;
    private int mLastDispatchedSystemUiVisibility = -1;
    private NavigationBarFragment mNavigationBar;
    private final NotificationRemoteInputManager mRemoteInputManager;
    private StatusBar mStatusBar;
    @VisibleForTesting
    int mSystemUiVisibility;
    private final IWindowManager mWindowManagerService;

    public /* synthetic */ void lambda$new$0$AutoHideController() {
        int i = this.mSystemUiVisibility & (~getTransientMask());
        if (this.mSystemUiVisibility != i) {
            notifySystemUiVisibilityChanged(i);
        }
    }

    public AutoHideController(Context context, Handler handler) {
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mCommandQueue.addCallback((CommandQueue.Callbacks) this);
        this.mHandler = handler;
        this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        this.mWindowManagerService = (IWindowManager) Dependency.get(IWindowManager.class);
        this.mDisplayId = context.getDisplayId();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onDisplayRemoved(int i) {
        if (i == this.mDisplayId) {
            this.mCommandQueue.removeCallback((CommandQueue.Callbacks) this);
        }
    }

    /* access modifiers changed from: package-private */
    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    /* access modifiers changed from: package-private */
    public void setNavigationBar(NavigationBarFragment navigationBarFragment) {
        this.mNavigationBar = navigationBarFragment;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        if (i == this.mDisplayId) {
            int i6 = this.mSystemUiVisibility;
            int i7 = ((~i5) & i6) | (i2 & i5);
            if ((i6 ^ i7) != 0) {
                this.mSystemUiVisibility = i7;
                if (hasStatusBar() && (268435456 & i2) != 0) {
                    this.mSystemUiVisibility &= -268435457;
                }
                if (hasNavigationBar() && (536870912 & i2) != 0) {
                    this.mSystemUiVisibility &= -536870913;
                }
                int i8 = this.mSystemUiVisibility;
                if (i8 != i7) {
                    this.mCommandQueue.setSystemUiVisibility(this.mDisplayId, i8, i3, i4, i5, rect, rect2, z);
                }
                notifySystemUiVisibilityChanged(this.mSystemUiVisibility);
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void notifySystemUiVisibilityChanged(int i) {
        try {
            if (this.mLastDispatchedSystemUiVisibility != i) {
                this.mWindowManagerService.statusBarVisibilityChanged(this.mDisplayId, i);
                this.mLastDispatchedSystemUiVisibility = i;
            }
        } catch (RemoteException unused) {
            Log.w("AutoHideController", "Cannot get WindowManager");
        }
    }

    /* access modifiers changed from: package-private */
    public void resumeSuspendedAutoHide() {
        if (this.mAutoHideSuspended) {
            scheduleAutoHide();
            Runnable checkBarModesRunnable = getCheckBarModesRunnable();
            if (checkBarModesRunnable != null) {
                this.mHandler.postDelayed(checkBarModesRunnable, 500);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void suspendAutoHide() {
        this.mHandler.removeCallbacks(this.mAutoHide);
        Runnable checkBarModesRunnable = getCheckBarModesRunnable();
        if (checkBarModesRunnable != null) {
            this.mHandler.removeCallbacks(checkBarModesRunnable);
        }
        this.mAutoHideSuspended = (this.mSystemUiVisibility & getTransientMask()) != 0;
    }

    /* access modifiers changed from: package-private */
    public void touchAutoHide() {
        if ((!hasStatusBar() || this.mStatusBar.getStatusBarMode() != 1) && (!hasNavigationBar() || !this.mNavigationBar.isSemiTransparent())) {
            cancelAutoHide();
        } else {
            scheduleAutoHide();
        }
    }

    private Runnable getCheckBarModesRunnable() {
        if (hasStatusBar()) {
            return new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$AutoHideController$Dw54NegELGCFcbvVgChoOa9gkLA */

                public final void run() {
                    AutoHideController.this.lambda$getCheckBarModesRunnable$1$AutoHideController();
                }
            };
        }
        if (hasNavigationBar()) {
            return new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$AutoHideController$FON87SM6b42jIBTAjBTcUbKIM */

                public final void run() {
                    AutoHideController.this.lambda$getCheckBarModesRunnable$2$AutoHideController();
                }
            };
        }
        return null;
    }

    public /* synthetic */ void lambda$getCheckBarModesRunnable$1$AutoHideController() {
        this.mStatusBar.checkBarModes();
    }

    public /* synthetic */ void lambda$getCheckBarModesRunnable$2$AutoHideController() {
        this.mNavigationBar.checkNavBarModes();
    }

    private void cancelAutoHide() {
        this.mAutoHideSuspended = false;
        this.mHandler.removeCallbacks(this.mAutoHide);
    }

    private void scheduleAutoHide() {
        cancelAutoHide();
        this.mHandler.postDelayed(this.mAutoHide, 2250);
    }

    /* access modifiers changed from: package-private */
    public void checkUserAutoHide(MotionEvent motionEvent) {
        boolean z = (this.mSystemUiVisibility & getTransientMask()) != 0 && motionEvent.getAction() == 4 && motionEvent.getX() == 0.0f && motionEvent.getY() == 0.0f;
        if (hasStatusBar()) {
            z &= !this.mRemoteInputManager.getController().isRemoteInputActive();
        }
        if (z) {
            userAutoHide();
        }
    }

    private void userAutoHide() {
        cancelAutoHide();
        this.mHandler.postDelayed(this.mAutoHide, 350);
    }

    private int getTransientMask() {
        int i = hasStatusBar() ? 67108864 : 0;
        return hasNavigationBar() ? i | 134217728 : i;
    }

    /* access modifiers changed from: package-private */
    public boolean hasNavigationBar() {
        return this.mNavigationBar != null;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean hasStatusBar() {
        return this.mStatusBar != null;
    }
}
