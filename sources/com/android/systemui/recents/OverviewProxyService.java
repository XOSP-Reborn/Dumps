package com.android.systemui.recents;

import android.app.ActivityTaskManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.input.InputManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.android.systemui.Dumpable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowCallback;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.CallbackController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OverviewProxyService implements CallbackController<OverviewProxyListener>, NavigationModeController.ModeChangedListener, Dumpable {
    private Region mActiveNavBarRegion;
    private boolean mBound;
    private int mConnectionBackoffAttempts;
    private final List<OverviewProxyListener> mConnectionCallbacks = new ArrayList();
    private final Runnable mConnectionRunnable = new Runnable() {
        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$2FrwSEVJnaHX9GGsAnD2I96htxU */

        public final void run() {
            OverviewProxyService.this.internalConnectToCurrentUser();
        }
    };
    private final Context mContext;
    private int mCurrentBoundedUserId = -1;
    private final Runnable mDeferredConnectionCallback = new Runnable() {
        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$53s1j2vSUNo_EjM7u2nSTJl32gM */

        public final void run() {
            OverviewProxyService.this.lambda$new$0$OverviewProxyService();
        }
    };
    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedCallback = new DeviceProvisionedController.DeviceProvisionedListener() {
        /* class com.android.systemui.recents.OverviewProxyService.AnonymousClass4 */

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSetupChanged() {
            if (OverviewProxyService.this.mDeviceProvisionedController.isCurrentUserSetup()) {
                OverviewProxyService.this.internalConnectToCurrentUser();
            }
        }

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSwitched() {
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.internalConnectToCurrentUser();
        }
    };
    private final DeviceProvisionedController mDeviceProvisionedController;
    private final Handler mHandler;
    private boolean mIsEnabled;
    private final BroadcastReceiver mLauncherStateChangedReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.recents.OverviewProxyService.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            OverviewProxyService.this.updateEnabledState();
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    private float mNavBarButtonAlpha;
    private final NavigationBarController mNavBarController;
    private int mNavBarMode = 0;
    private IOverviewProxy mOverviewProxy;
    private final ServiceConnection mOverviewServiceConnection = new ServiceConnection() {
        /* class com.android.systemui.recents.OverviewProxyService.AnonymousClass3 */

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.mHandler.removeCallbacks(OverviewProxyService.this.mDeferredConnectionCallback);
            try {
                iBinder.linkToDeath(OverviewProxyService.this.mOverviewServiceDeathRcpt, 0);
                OverviewProxyService overviewProxyService = OverviewProxyService.this;
                overviewProxyService.mCurrentBoundedUserId = overviewProxyService.mDeviceProvisionedController.getCurrentUser();
                OverviewProxyService.this.mOverviewProxy = IOverviewProxy.Stub.asInterface(iBinder);
                Bundle bundle = new Bundle();
                bundle.putBinder("extra_sysui_proxy", OverviewProxyService.this.mSysUiProxy.asBinder());
                bundle.putFloat("extra_window_corner_radius", OverviewProxyService.this.mWindowCornerRadius);
                bundle.putBoolean("extra_supports_window_corners", OverviewProxyService.this.mSupportsRoundedCornersOnWindows);
                try {
                    OverviewProxyService.this.mOverviewProxy.onInitialize(bundle);
                } catch (RemoteException e) {
                    OverviewProxyService.this.mCurrentBoundedUserId = -1;
                    Log.e("OverviewProxyService", "Failed to call onInitialize()", e);
                }
                OverviewProxyService.this.dispatchNavButtonBounds();
                OverviewProxyService.this.updateSystemUiStateFlags();
                OverviewProxyService.this.notifyConnectionChanged();
            } catch (RemoteException e2) {
                Log.e("OverviewProxyService", "Lost connection to launcher service", e2);
                OverviewProxyService.this.disconnectFromLauncherService();
                OverviewProxyService.this.retryConnectionWithBackoff();
            }
        }

        public void onNullBinding(ComponentName componentName) {
            Log.w("OverviewProxyService", "Null binding of '" + componentName + "', try reconnecting");
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
            OverviewProxyService.this.retryConnectionWithBackoff();
        }

        public void onBindingDied(ComponentName componentName) {
            Log.w("OverviewProxyService", "Binding died of '" + componentName + "', try reconnecting");
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
            OverviewProxyService.this.retryConnectionWithBackoff();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
        }
    };
    private final IBinder.DeathRecipient mOverviewServiceDeathRcpt = new IBinder.DeathRecipient() {
        /* class com.android.systemui.recents.$$Lambda$FF1twVzMKp_FAsQO2IsbqUbCbs */

        public final void binderDied() {
            OverviewProxyService.this.cleanupAfterDeath();
        }
    };
    private final Intent mQuickStepIntent;
    private final ComponentName mRecentsComponentName;
    private MotionEvent mStatusBarGestureDownEvent;
    private final StatusBarWindowController mStatusBarWinController;
    private final StatusBarWindowCallback mStatusBarWindowCallback = new StatusBarWindowCallback() {
        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$b7uhSpdl46tRQQQT8ZW7Bieyg6A */

        @Override // com.android.systemui.statusbar.phone.StatusBarWindowCallback
        public final void onStateChanged(boolean z, boolean z2, boolean z3) {
            OverviewProxyService.this.onStatusBarStateChanged(z, z2, z3);
        }
    };
    private boolean mSupportsRoundedCornersOnWindows;
    private ISystemUiProxy mSysUiProxy = new ISystemUiProxy.Stub() {
        /* class com.android.systemui.recents.OverviewProxyService.AnonymousClass1 */

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void startScreenPinning(int i) {
            if (verifyCaller("startScreenPinning")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(i) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$gf7wUE1qIRz6uZ2klN6hqV9xFfM */
                        private final /* synthetic */ int f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$startScreenPinning$0$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$startScreenPinning$0$OverviewProxyService$1(int i) {
            StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, StatusBar.class);
            if (statusBar != null) {
                statusBar.showScreenPinningRequest(i, false);
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void stopScreenPinning() {
            if (verifyCaller("stopScreenPinning")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post($$Lambda$OverviewProxyService$1$WjFAUijOf0iWbjyxz5nDkhLzxA.INSTANCE);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        static /* synthetic */ void lambda$stopScreenPinning$1() {
            try {
                ActivityTaskManager.getService().stopSystemLockTaskMode();
            } catch (RemoteException unused) {
                Log.e("OverviewProxyService", "Failed to stop screen pinning");
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void onStatusBarMotionEvent(MotionEvent motionEvent) {
            if (verifyCaller("onStatusBarMotionEvent")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(motionEvent) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$2LCvhYvor9KUdkD8Lozm_8CbJlE */
                        private final /* synthetic */ MotionEvent f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$onStatusBarMotionEvent$2$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onStatusBarMotionEvent$2$OverviewProxyService$1(MotionEvent motionEvent) {
            StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, StatusBar.class);
            if (statusBar != null) {
                statusBar.dispatchNotificationsPanelTouchEvent(motionEvent);
                int actionMasked = motionEvent.getActionMasked();
                if (actionMasked == 0) {
                    OverviewProxyService.this.mStatusBarGestureDownEvent = MotionEvent.obtain(motionEvent);
                }
                if (actionMasked == 1 || actionMasked == 3) {
                    OverviewProxyService.this.mStatusBarGestureDownEvent.recycle();
                    OverviewProxyService.this.mStatusBarGestureDownEvent = null;
                }
                motionEvent.recycle();
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void onSplitScreenInvoked() {
            if (verifyCaller("onSplitScreenInvoked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Divider divider = (Divider) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, Divider.class);
                    if (divider != null) {
                        divider.onDockedFirstAnimationFrame();
                    }
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void onOverviewShown(boolean z) {
            if (verifyCaller("onOverviewShown")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(z) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$o_Nvl9rNrEnvxnQlEkJ_hCsmmfI */
                        private final /* synthetic */ boolean f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$onOverviewShown$3$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onOverviewShown$3$OverviewProxyService$1(boolean z) {
            for (int size = OverviewProxyService.this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
                ((OverviewProxyListener) OverviewProxyService.this.mConnectionCallbacks.get(size)).onOverviewShown(z);
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public Rect getNonMinimizedSplitScreenSecondaryBounds() {
            if (!verifyCaller("getNonMinimizedSplitScreenSecondaryBounds")) {
                return null;
            }
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Divider divider = (Divider) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, Divider.class);
                if (divider != null) {
                    return divider.getView().getNonMinimizedSplitScreenSecondaryBounds();
                }
                Binder.restoreCallingIdentity(clearCallingIdentity);
                return null;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void setNavBarButtonAlpha(float f, boolean z) {
            if (verifyCaller("setNavBarButtonAlpha")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mNavBarButtonAlpha = f;
                    OverviewProxyService.this.mHandler.post(new Runnable(f, z) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$dMl_i5aLm6UXcbb7W1OvCrMhAM */
                        private final /* synthetic */ float f$1;
                        private final /* synthetic */ boolean f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$setNavBarButtonAlpha$4$OverviewProxyService$1(this.f$1, this.f$2);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$setNavBarButtonAlpha$4$OverviewProxyService$1(float f, boolean z) {
            OverviewProxyService.this.notifyNavBarButtonAlphaChanged(f, z);
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void setBackButtonAlpha(float f, boolean z) {
            setNavBarButtonAlpha(f, z);
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void onAssistantProgress(float f) {
            if (verifyCaller("onAssistantProgress")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(f) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$HkSs0hra3pArt93DbnvuWmlgXvE */
                        private final /* synthetic */ float f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$onAssistantProgress$5$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onAssistantProgress$5$OverviewProxyService$1(float f) {
            OverviewProxyService.this.notifyAssistantProgress(f);
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void onAssistantGestureCompletion(float f) {
            if (verifyCaller("onAssistantGestureCompletion")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(f) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$tM0ScuR2E6sp4f29_cYFeTNIIA */
                        private final /* synthetic */ float f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$onAssistantGestureCompletion$6$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onAssistantGestureCompletion$6$OverviewProxyService$1(float f) {
            OverviewProxyService.this.notifyAssistantGestureCompletion(f);
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void startAssistant(Bundle bundle) {
            if (verifyCaller("startAssistant")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(bundle) {
                        /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$1$lzBDCxnJTcDNIZHX5ISOsfuKOL0 */
                        private final /* synthetic */ Bundle f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            OverviewProxyService.AnonymousClass1.this.lambda$startAssistant$7$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$startAssistant$7$OverviewProxyService$1(Bundle bundle) {
            OverviewProxyService.this.notifyStartAssistant(bundle);
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public Bundle monitorGestureInput(String str, int i) {
            if (!verifyCaller("monitorGestureInput")) {
                return null;
            }
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Parcelable monitorGestureInput = InputManager.getInstance().monitorGestureInput(str, i);
                Bundle bundle = new Bundle();
                bundle.putParcelable("extra_input_monitor", monitorGestureInput);
                return bundle;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void notifyAccessibilityButtonClicked(int i) {
            if (verifyCaller("notifyAccessibilityButtonClicked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    AccessibilityManager.getInstance(OverviewProxyService.this.mContext).notifyAccessibilityButtonClicked(i);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        @Override // com.android.systemui.shared.recents.ISystemUiProxy
        public void notifyAccessibilityButtonLongClicked() {
            if (verifyCaller("notifyAccessibilityButtonLongClicked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
                    intent.addFlags(268468224);
                    OverviewProxyService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        private boolean verifyCaller(String str) {
            int identifier = Binder.getCallingUserHandle().getIdentifier();
            if (identifier == OverviewProxyService.this.mCurrentBoundedUserId) {
                return true;
            }
            Log.w("OverviewProxyService", "Launcher called sysui with invalid user: " + identifier + ", reason: " + str);
            return false;
        }
    };
    private int mSysUiStateFlags;
    private float mWindowCornerRadius;

    public interface OverviewProxyListener {
        default void onAssistantGestureCompletion(float f) {
        }

        default void onAssistantProgress(float f) {
        }

        default void onConnectionChanged(boolean z) {
        }

        default void onNavBarButtonAlphaChanged(float f, boolean z) {
        }

        default void onOverviewShown(boolean z) {
        }

        default void onSystemUiStateChanged(int i) {
        }

        default void startAssistant(Bundle bundle) {
        }
    }

    public /* synthetic */ void lambda$new$0$OverviewProxyService() {
        Log.w("OverviewProxyService", "Binder supposed established connection but actual connection to service timed out, trying again");
        retryConnectionWithBackoff();
    }

    public OverviewProxyService(Context context, DeviceProvisionedController deviceProvisionedController, NavigationBarController navigationBarController, NavigationModeController navigationModeController, StatusBarWindowController statusBarWindowController) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mNavBarController = navigationBarController;
        this.mStatusBarWinController = statusBarWindowController;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mConnectionBackoffAttempts = 0;
        this.mRecentsComponentName = ComponentName.unflattenFromString(context.getString(17039804));
        this.mQuickStepIntent = new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName());
        this.mWindowCornerRadius = ScreenDecorationsUtils.getWindowCornerRadius(this.mContext.getResources());
        this.mSupportsRoundedCornersOnWindows = ScreenDecorationsUtils.supportsRoundedCornersOnWindows(this.mContext.getResources());
        this.mNavBarButtonAlpha = 1.0f;
        this.mNavBarMode = navigationModeController.addListener(this);
        updateEnabledState();
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart(this.mRecentsComponentName.getPackageName(), 0);
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, intentFilter);
        statusBarWindowController.registerCallback(this.mStatusBarWindowCallback);
    }

    public void notifyBackAction(boolean z, int i, int i2, boolean z2, boolean z3) {
        try {
            if (this.mOverviewProxy != null) {
                this.mOverviewProxy.onBackAction(z, i, i2, z2, z3);
            }
        } catch (RemoteException e) {
            Log.e("OverviewProxyService", "Failed to notify back action", e);
        }
    }

    public void setSystemUiStateFlag(int i, boolean z, int i2) {
        if (i2 == 0) {
            int i3 = this.mSysUiStateFlags;
            int i4 = z ? i | i3 : (~i) & i3;
            if (this.mSysUiStateFlags != i4) {
                this.mSysUiStateFlags = i4;
                notifySystemUiStateChanged(this.mSysUiStateFlags);
                notifySystemUiStateFlags(this.mSysUiStateFlags);
            }
        }
    }

    public int getSystemUiStateFlags() {
        return this.mSysUiStateFlags;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSystemUiStateFlags() {
        NavigationBarFragment defaultNavigationBarFragment = this.mNavBarController.getDefaultNavigationBarFragment();
        NavigationBarView navigationBarView = this.mNavBarController.getNavigationBarView(this.mContext.getDisplayId());
        this.mSysUiStateFlags = 0;
        if (defaultNavigationBarFragment != null) {
            defaultNavigationBarFragment.updateSystemUiStateFlags(-1);
        }
        if (navigationBarView != null) {
            navigationBarView.updateSystemUiStateFlags();
        }
        StatusBarWindowController statusBarWindowController = this.mStatusBarWinController;
        if (statusBarWindowController != null) {
            statusBarWindowController.notifyStateChangedCallbacks();
        }
        notifySystemUiStateFlags(this.mSysUiStateFlags);
    }

    private void notifySystemUiStateFlags(int i) {
        try {
            if (this.mOverviewProxy != null) {
                this.mOverviewProxy.onSystemUiStateChanged(i);
            }
        } catch (RemoteException e) {
            Log.e("OverviewProxyService", "Failed to notify sysui state change", e);
        }
    }

    /* access modifiers changed from: private */
    public void onStatusBarStateChanged(boolean z, boolean z2, boolean z3) {
        int displayId = this.mContext.getDisplayId();
        boolean z4 = true;
        setSystemUiStateFlag(64, z && !z2, displayId);
        if (!z || !z2) {
            z4 = false;
        }
        setSystemUiStateFlag(512, z4, displayId);
        setSystemUiStateFlag(8, z3, displayId);
    }

    public void onActiveNavBarRegionChanges(Region region) {
        this.mActiveNavBarRegion = region;
        dispatchNavButtonBounds();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void dispatchNavButtonBounds() {
        Region region;
        IOverviewProxy iOverviewProxy = this.mOverviewProxy;
        if (iOverviewProxy != null && (region = this.mActiveNavBarRegion) != null) {
            try {
                iOverviewProxy.onActiveNavBarRegionChanges(region);
            } catch (RemoteException e) {
                Log.e("OverviewProxyService", "Failed to call onActiveNavBarRegionChanges()", e);
            }
        }
    }

    public void cleanupAfterDeath() {
        if (this.mStatusBarGestureDownEvent != null) {
            this.mHandler.post(new Runnable() {
                /* class com.android.systemui.recents.$$Lambda$OverviewProxyService$eCc1ukzT5yAkIaX2QrSVYaZKqYk */

                public final void run() {
                    OverviewProxyService.this.lambda$cleanupAfterDeath$1$OverviewProxyService();
                }
            });
        }
        startConnectionToCurrentUser();
    }

    public /* synthetic */ void lambda$cleanupAfterDeath$1$OverviewProxyService() {
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        if (statusBar != null) {
            this.mStatusBarGestureDownEvent.setAction(3);
            statusBar.dispatchNotificationsPanelTouchEvent(this.mStatusBarGestureDownEvent);
            this.mStatusBarGestureDownEvent.recycle();
            this.mStatusBarGestureDownEvent = null;
        }
    }

    public void startConnectionToCurrentUser() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mConnectionRunnable);
        } else {
            internalConnectToCurrentUser();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    public void internalConnectToCurrentUser() {
        disconnectFromLauncherService();
        if (!this.mDeviceProvisionedController.isCurrentUserSetup() || !isEnabled()) {
            Log.v("OverviewProxyService", "Cannot attempt connection, is setup " + this.mDeviceProvisionedController.isCurrentUserSetup() + ", is enabled " + isEnabled());
            return;
        }
        this.mHandler.removeCallbacks(this.mConnectionRunnable);
        try {
            this.mBound = this.mContext.bindServiceAsUser(new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName()), this.mOverviewServiceConnection, 33554433, UserHandle.of(this.mDeviceProvisionedController.getCurrentUser()));
        } catch (SecurityException e) {
            Log.e("OverviewProxyService", "Unable to bind because of security error", e);
        }
        if (this.mBound) {
            this.mHandler.postDelayed(this.mDeferredConnectionCallback, 5000);
        } else {
            retryConnectionWithBackoff();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void retryConnectionWithBackoff() {
        if (!this.mHandler.hasCallbacks(this.mConnectionRunnable)) {
            long min = (long) Math.min(Math.scalb(1000.0f, this.mConnectionBackoffAttempts), 600000.0f);
            this.mHandler.postDelayed(this.mConnectionRunnable, min);
            this.mConnectionBackoffAttempts++;
            Log.w("OverviewProxyService", "Failed to connect on attempt " + this.mConnectionBackoffAttempts + " will try again in " + min + "ms");
        }
    }

    public void addCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.add(overviewProxyListener);
        overviewProxyListener.onConnectionChanged(this.mOverviewProxy != null);
        overviewProxyListener.onNavBarButtonAlphaChanged(this.mNavBarButtonAlpha, false);
        overviewProxyListener.onSystemUiStateChanged(this.mSysUiStateFlags);
    }

    public void removeCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.remove(overviewProxyListener);
    }

    public boolean shouldShowSwipeUpUI() {
        return isEnabled() && !QuickStepContract.isLegacyMode(this.mNavBarMode);
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public IOverviewProxy getProxy() {
        return this.mOverviewProxy;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void disconnectFromLauncherService() {
        if (this.mBound) {
            this.mContext.unbindService(this.mOverviewServiceConnection);
            this.mBound = false;
        }
        IOverviewProxy iOverviewProxy = this.mOverviewProxy;
        if (iOverviewProxy != null) {
            iOverviewProxy.asBinder().unlinkToDeath(this.mOverviewServiceDeathRcpt, 0);
            this.mOverviewProxy = null;
            notifyNavBarButtonAlphaChanged(1.0f, false);
            notifyConnectionChanged();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyNavBarButtonAlphaChanged(float f, boolean z) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onNavBarButtonAlphaChanged(f, z);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyConnectionChanged() {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onConnectionChanged(this.mOverviewProxy != null);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyAssistantProgress(float f) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onAssistantProgress(f);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyAssistantGestureCompletion(float f) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onAssistantGestureCompletion(f);
        }
    }

    private void notifySystemUiStateChanged(int i) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).onSystemUiStateChanged(i);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyStartAssistant(Bundle bundle) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            this.mConnectionCallbacks.get(size).startAssistant(bundle);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateEnabledState() {
        this.mIsEnabled = this.mContext.getPackageManager().resolveServiceAsUser(this.mQuickStepIntent, 1048576, ActivityManagerWrapper.getInstance().getCurrentUserId()) != null;
    }

    public void lockSet(boolean z) {
        if (z) {
            disconnectFromLauncherService();
        } else {
            startConnectionToCurrentUser();
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
    public void onNavigationModeChanged(int i) {
        this.mNavBarMode = i;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("OverviewProxyService state:");
        printWriter.print("  recentsComponentName=");
        printWriter.println(this.mRecentsComponentName);
        printWriter.print("  isConnected=");
        printWriter.println(this.mOverviewProxy != null);
        printWriter.print("  isCurrentUserSetup=");
        printWriter.println(this.mDeviceProvisionedController.isCurrentUserSetup());
        printWriter.print("  connectionBackoffAttempts=");
        printWriter.println(this.mConnectionBackoffAttempts);
        printWriter.print("  quickStepIntent=");
        printWriter.println(this.mQuickStepIntent);
        printWriter.print("  quickStepIntentResolved=");
        printWriter.println(isEnabled());
        printWriter.print("  mSysUiStateFlags=");
        printWriter.println(this.mSysUiStateFlags);
        printWriter.println("    " + QuickStepContract.getSystemUiStateString(this.mSysUiStateFlags));
        printWriter.print("    backGestureDisabled=");
        printWriter.println(QuickStepContract.isBackGestureDisabled(this.mSysUiStateFlags));
        printWriter.print("    assistantGestureDisabled=");
        printWriter.println(QuickStepContract.isAssistantGestureDisabled(this.mSysUiStateFlags));
    }
}
