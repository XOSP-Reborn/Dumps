package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.StatsLog;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManagerGlobal;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.phone.KeyguardBouncer;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.sonymobile.runtimeskinning.SkinningBridge;
import java.io.PrintWriter;
import java.util.ArrayList;

public class StatusBarKeyguardViewManager implements RemoteInputController.Callback, StatusBarStateController.StateListener, ConfigurationController.ConfigurationListener, NotificationPanelView.PanelExpansionListener, NavigationModeController.ModeChangedListener {
    private ActivityStarter.OnDismissAction mAfterKeyguardGoneAction;
    private final ArrayList<Runnable> mAfterKeyguardGoneRunnables = new ArrayList<>();
    private BiometricUnlockController mBiometricUnlockController;
    protected KeyguardBouncer mBouncer;
    private ViewGroup mContainer;
    protected final Context mContext;
    private final DockManager.DockEventListener mDockEventListener = new DockManager.DockEventListener() {
        /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass2 */
    };
    private final DockManager mDockManager;
    private boolean mDozing;
    private final KeyguardBouncer.BouncerExpansionCallback mExpansionCallback = new KeyguardBouncer.BouncerExpansionCallback() {
        /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass1 */

        @Override // com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback
        public void onFullyShown() {
            StatusBarKeyguardViewManager.this.updateStates();
            StatusBarKeyguardViewManager.this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), StatusBarKeyguardViewManager.this.mContainer, "BOUNCER_VISIBLE");
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }

        @Override // com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback
        public void onStartingToHide() {
            StatusBarKeyguardViewManager.this.updateStates();
        }

        @Override // com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback
        public void onStartingToShow() {
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }

        @Override // com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback
        public void onFullyHidden() {
            StatusBarKeyguardViewManager.this.updateStates();
            StatusBarKeyguardViewManager.this.updateLockIcon();
        }
    };
    protected boolean mFirstUpdate = true;
    private boolean mGesturalNav;
    private boolean mGoingToSleepVisibleNotOccluded;
    private boolean mIsDocked;
    private final KeyguardMonitorImpl mKeyguardMonitor = ((KeyguardMonitorImpl) Dependency.get(KeyguardMonitor.class));
    private int mLastBiometricMode;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    private boolean mLastDozing;
    private boolean mLastGesturalNav;
    private boolean mLastIsDocked;
    private boolean mLastLockVisible;
    protected boolean mLastOccluded;
    private boolean mLastPulsing;
    protected boolean mLastRemoteInputActive;
    protected boolean mLastShowing;
    private ViewGroup mLockIconContainer;
    protected LockPatternUtils mLockPatternUtils;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass7 */

        public void run() {
            StatusBarKeyguardViewManager.this.mStatusBar.getNavigationBarView().getRootView().setVisibility(0);
        }
    };
    private final NotificationMediaManager mMediaManager = ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class));
    private NotificationPanelView mNotificationPanelView;
    protected boolean mOccluded;
    private DismissWithActionRequest mPendingWakeupAction;
    private boolean mPulsing;
    protected boolean mRemoteInputActive;
    private boolean mScreenRotation = false;
    protected boolean mShowing;
    protected StatusBar mStatusBar;
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private final StatusBarWindowController mStatusBarWindowController;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass3 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onEmergencyCallAction() {
            StatusBarKeyguardViewManager statusBarKeyguardViewManager = StatusBarKeyguardViewManager.this;
            if (statusBarKeyguardViewManager.mOccluded) {
                statusBarKeyguardViewManager.reset(true);
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
            boolean z = false;
            StatusBarKeyguardViewManager.this.mScreenRotation = false;
            if (batteryStatus.isWirelessPluggedIn() && SystemProperties.getInt("persist.service.wc_manager.chg_type", 0) == 1) {
                StatusBarKeyguardViewManager.this.mScreenRotation = true;
            }
            StatusBarWindowController statusBarWindowController = StatusBarKeyguardViewManager.this.mStatusBarWindowController;
            if (!StatusBarKeyguardViewManager.this.mDozing) {
                z = StatusBarKeyguardViewManager.this.mScreenRotation;
            }
            statusBarWindowController.setKeyguardScreenRotation(z);
        }
    };
    protected ViewMediatorCallback mViewMediatorCallback;

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateLockIcon() {
    }

    public void onCancelClicked() {
    }

    public void onScreenTurnedOn() {
    }

    public void onScreenTurningOn() {
    }

    public void onStartedWakingUp() {
    }

    /* access modifiers changed from: protected */
    public boolean shouldDestroyViewOnReset() {
        return false;
    }

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = viewMediatorCallback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateMonitorCallback);
        this.mStatusBarStateController.addCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mGesturalNav = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this));
        this.mDockManager = (DockManager) Dependency.get(DockManager.class);
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.addListener(this.mDockEventListener);
            this.mIsDocked = this.mDockManager.isDocked();
        }
    }

    public void registerStatusBar(StatusBar statusBar, ViewGroup viewGroup, NotificationPanelView notificationPanelView, BiometricUnlockController biometricUnlockController, DismissCallbackRegistry dismissCallbackRegistry, ViewGroup viewGroup2) {
        this.mStatusBar = statusBar;
        this.mContainer = viewGroup;
        this.mLockIconContainer = viewGroup2;
        ViewGroup viewGroup3 = this.mLockIconContainer;
        if (viewGroup3 != null) {
            this.mLastLockVisible = viewGroup3.getVisibility() == 0;
        }
        this.mBiometricUnlockController = biometricUnlockController;
        this.mBouncer = SystemUIFactory.getInstance().createKeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, viewGroup, dismissCallbackRegistry, this.mExpansionCallback);
        this.mNotificationPanelView = notificationPanelView;
        notificationPanelView.setExpansionListener(this);
    }

    @Override // com.android.systemui.statusbar.phone.NotificationPanelView.PanelExpansionListener
    public void onPanelExpansionChanged(float f, boolean z) {
        if (this.mNotificationPanelView.isUnlockHintRunning()) {
            this.mBouncer.setExpansion(1.0f);
        } else if (bouncerNeedsScrimming()) {
            this.mBouncer.setExpansion(0.0f);
        } else if (this.mShowing) {
            if (!isWakeAndUnlocking() && !this.mStatusBar.isInLaunchTransition()) {
                this.mBouncer.setExpansion(f);
            }
            if (f != 1.0f && z && this.mStatusBar.isKeyguardCurrentlySecure() && !this.mBouncer.isShowing() && !this.mBouncer.isAnimatingAway()) {
                this.mBouncer.show(false, false);
            }
        } else if (this.mPulsing && f == 0.0f) {
            this.mStatusBar.wakeUpIfDozing(SystemClock.uptimeMillis(), this.mContainer, "BOUNCER_VISIBLE");
        }
    }

    @Override // com.android.systemui.statusbar.phone.NotificationPanelView.PanelExpansionListener
    public void onQsExpansionChanged(float f) {
        updateLockIcon();
    }

    public void show(Bundle bundle) {
        this.mShowing = true;
        this.mStatusBarWindowController.setKeyguardShowing(true);
        KeyguardMonitorImpl keyguardMonitorImpl = this.mKeyguardMonitor;
        keyguardMonitorImpl.notifyKeyguardState(this.mShowing, keyguardMonitorImpl.isSecure(), this.mKeyguardMonitor.isOccluded());
        reset(true);
        StatsLog.write(62, 2);
    }

    /* access modifiers changed from: protected */
    public void showBouncerOrKeyguard(boolean z) {
        if (!this.mBouncer.needsFullscreenBouncer() || this.mDozing) {
            this.mStatusBar.showKeyguard();
            if (z) {
                hideBouncer(shouldDestroyViewOnReset());
                this.mBouncer.prepare();
            }
        } else {
            this.mStatusBar.hideKeyguard();
            this.mBouncer.show(true);
        }
        updateStates();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideBouncer(boolean z) {
        KeyguardBouncer keyguardBouncer = this.mBouncer;
        if (keyguardBouncer != null) {
            keyguardBouncer.hide(z);
            cancelPendingWakeupAction();
        }
    }

    public void showBouncer(boolean z) {
        if (this.mShowing && !this.mBouncer.isShowing()) {
            this.mBouncer.show(false, z);
        }
        updateStates();
    }

    public void dismissWithAction(ActivityStarter.OnDismissAction onDismissAction, Runnable runnable, boolean z) {
        dismissWithAction(onDismissAction, runnable, z, null);
    }

    public void dismissWithAction(ActivityStarter.OnDismissAction onDismissAction, Runnable runnable, boolean z, String str) {
        if (this.mShowing) {
            cancelPendingWakeupAction();
            if (this.mDozing && !isWakeAndUnlocking()) {
                this.mPendingWakeupAction = new DismissWithActionRequest(onDismissAction, runnable, z, str);
                return;
            } else if (!z) {
                this.mBouncer.showWithDismissAction(onDismissAction, runnable);
            } else {
                this.mAfterKeyguardGoneAction = onDismissAction;
                this.mBouncer.show(false);
            }
        }
        updateStates();
    }

    private boolean isWakeAndUnlocking() {
        int mode = this.mBiometricUnlockController.getMode();
        return mode == 1 || mode == 2;
    }

    public void addAfterKeyguardGoneRunnable(Runnable runnable) {
        this.mAfterKeyguardGoneRunnables.add(runnable);
    }

    public void reset(boolean z) {
        if (this.mShowing) {
            if (!this.mOccluded || this.mDozing) {
                showBouncerOrKeyguard(z);
            } else {
                this.mStatusBar.hideKeyguard();
                if (z || this.mBouncer.needsFullscreenBouncer()) {
                    hideBouncer(false);
                }
            }
            KeyguardUpdateMonitor.getInstance(this.mContext).sendKeyguardReset();
            updateStates();
        }
    }

    public boolean isGoingToSleepVisibleNotOccluded() {
        return this.mGoingToSleepVisibleNotOccluded;
    }

    public void onStartedGoingToSleep() {
        this.mGoingToSleepVisibleNotOccluded = isShowing() && !isOccluded();
    }

    public void onFinishedGoingToSleep() {
        this.mGoingToSleepVisibleNotOccluded = false;
        this.mBouncer.onScreenTurnedOff();
    }

    @Override // com.android.systemui.statusbar.RemoteInputController.Callback
    public void onRemoteInputActive(boolean z) {
        this.mRemoteInputActive = z;
        updateStates();
    }

    private void setDozing(boolean z) {
        if (this.mDozing != z) {
            this.mDozing = z;
            this.mStatusBarWindowController.setKeyguardScreenRotation(z ? false : this.mScreenRotation);
            if (z || this.mBouncer.needsFullscreenBouncer() || this.mOccluded) {
                reset(z);
            }
            updateStates();
            if (!z) {
                launchPendingWakeupAction();
            }
        }
    }

    public void setPulsing(boolean z) {
        if (this.mPulsing != z) {
            this.mPulsing = z;
            updateStates();
        }
    }

    public void setNeedsInput(boolean z) {
        this.mStatusBarWindowController.setKeyguardNeedsInput(z);
    }

    public boolean isUnlockWithWallpaper() {
        return this.mStatusBarWindowController.isShowingWallpaper();
    }

    public void setOccluded(boolean z, boolean z2) {
        this.mStatusBar.setOccluded(z);
        boolean z3 = true;
        if (z && !this.mOccluded && this.mShowing) {
            StatsLog.write(62, 3);
            if (this.mStatusBar.isInLaunchTransition()) {
                this.mOccluded = true;
                this.mStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
                    /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass4 */

                    public void run() {
                        StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardOccluded(StatusBarKeyguardViewManager.this.mOccluded);
                        StatusBarKeyguardViewManager.this.reset(true);
                    }
                });
                return;
            }
        } else if (!z && this.mOccluded && this.mShowing) {
            StatsLog.write(62, 2);
        }
        boolean z4 = !this.mOccluded && z;
        this.mOccluded = z;
        if (this.mShowing) {
            NotificationMediaManager notificationMediaManager = this.mMediaManager;
            if (!z2 || z) {
                z3 = false;
            }
            notificationMediaManager.updateMediaMetaData(false, z3);
        }
        this.mStatusBarWindowController.setKeyguardOccluded(z);
        if (!this.mDozing) {
            reset(z4);
        } else {
            updateStates();
        }
        if (z2 && !z && this.mShowing && !this.mBouncer.isShowing()) {
            this.mStatusBar.animateKeyguardUnoccluding();
        }
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(runnable);
            this.mNotificationPanelView.onBouncerPreHideAnimation();
        } else if (runnable != null) {
            runnable.run();
        }
        this.mNotificationPanelView.blockExpansionForCurrentTouch();
        updateLockIcon();
    }

    public void hide(long j, long j2) {
        long j3;
        long j4;
        this.mShowing = false;
        KeyguardMonitorImpl keyguardMonitorImpl = this.mKeyguardMonitor;
        keyguardMonitorImpl.notifyKeyguardState(this.mShowing, keyguardMonitorImpl.isSecure(), this.mKeyguardMonitor.isOccluded());
        launchPendingWakeupAction();
        long j5 = KeyguardUpdateMonitor.getInstance(this.mContext).needsSlowUnlockTransition() ? 2000 : j2;
        long max = Math.max(0L, (j - 48) - SystemClock.uptimeMillis());
        if (this.mStatusBar.isInLaunchTransition()) {
            this.mStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass5 */

                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardShowing(false);
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardFadingAway(true);
                    StatusBarKeyguardViewManager.this.hideBouncer(true);
                    StatusBarKeyguardViewManager.this.updateStates();
                }
            }, new Runnable() {
                /* class com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager.AnonymousClass6 */

                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBar.hideKeyguard();
                    StatusBarKeyguardViewManager.this.mStatusBarWindowController.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.keyguardGone();
                    StatusBarKeyguardViewManager.this.executeAfterKeyguardGoneAction();
                }
            });
        } else {
            executeAfterKeyguardGoneAction();
            boolean z = this.mBiometricUnlockController.getMode() == 2;
            if (z) {
                j3 = 240;
                j4 = 0;
            } else {
                j3 = j5;
                j4 = max;
            }
            this.mStatusBar.setKeyguardFadingAway(j, j4, j3);
            this.mBiometricUnlockController.startKeyguardFadingAway();
            hideBouncer(true);
            if (z) {
                this.mStatusBar.fadeKeyguardWhilePulsing();
                wakeAndUnlockDejank();
            } else if (!this.mStatusBar.hideKeyguard()) {
                this.mStatusBarWindowController.setKeyguardFadingAway(true);
                this.mStatusBar.updateScrimController();
                wakeAndUnlockDejank();
            } else {
                this.mStatusBar.finishKeyguardFadingAway();
                this.mBiometricUnlockController.finishKeyguardFadingAway();
            }
            updateStates();
            this.mStatusBarWindowController.setKeyguardShowing(false);
            this.mViewMediatorCallback.keyguardGone();
        }
        StatsLog.write(62, 1);
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        hideBouncer(true);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
    public void onNavigationModeChanged(int i) {
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (isGesturalMode != this.mGesturalNav) {
            this.mGesturalNav = isGesturalMode;
            updateStates();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        hideBouncer(true);
        this.mBouncer.prepare();
    }

    public /* synthetic */ void lambda$onKeyguardFadedAway$0$StatusBarKeyguardViewManager() {
        this.mStatusBarWindowController.setKeyguardFadingAway(false);
    }

    public void onKeyguardFadedAway() {
        this.mContainer.postDelayed(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarKeyguardViewManager$Qxwhwo1uUdyEeH0KZ8eQm9dLJ8 */

            public final void run() {
                StatusBarKeyguardViewManager.this.lambda$onKeyguardFadedAway$0$StatusBarKeyguardViewManager();
            }
        }, 100);
        this.mStatusBar.finishKeyguardFadingAway();
        this.mBiometricUnlockController.finishKeyguardFadingAway();
        WindowManagerGlobal.getInstance().trimMemory(20);
    }

    private void wakeAndUnlockDejank() {
        if (this.mBiometricUnlockController.getMode() == 1 && LatencyTracker.isEnabled(this.mContext)) {
            DejankUtils.postAfterTraversal(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarKeyguardViewManager$MwYtqufbgyJNJ1l_l2mNmQjtUTg */

                public final void run() {
                    StatusBarKeyguardViewManager.this.lambda$wakeAndUnlockDejank$1$StatusBarKeyguardViewManager();
                }
            });
        }
    }

    public /* synthetic */ void lambda$wakeAndUnlockDejank$1$StatusBarKeyguardViewManager() {
        LatencyTracker.getInstance(this.mContext).onActionEnd(2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void executeAfterKeyguardGoneAction() {
        ActivityStarter.OnDismissAction onDismissAction = this.mAfterKeyguardGoneAction;
        if (onDismissAction != null) {
            onDismissAction.onDismiss();
            this.mAfterKeyguardGoneAction = null;
        }
        for (int i = 0; i < this.mAfterKeyguardGoneRunnables.size(); i++) {
            this.mAfterKeyguardGoneRunnables.get(i).run();
        }
        this.mAfterKeyguardGoneRunnables.clear();
    }

    public void dismissAndCollapse() {
        this.mStatusBar.executeRunnableDismissingKeyguard(null, null, true, false, true);
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed(boolean z) {
        if (!this.mBouncer.isShowing()) {
            return false;
        }
        this.mStatusBar.endAffordanceLaunch();
        if (!this.mBouncer.isScrimmed() || this.mBouncer.needsFullscreenBouncer()) {
            reset(z);
            return true;
        }
        hideBouncer(false);
        updateStates();
        return true;
    }

    public boolean isBouncerShowing() {
        return this.mBouncer.isShowing();
    }

    public boolean isBouncerPartiallyVisible() {
        return this.mBouncer.isPartiallyVisible();
    }

    private long getNavBarShowDelay() {
        if (this.mKeyguardMonitor.isKeyguardFadingAway()) {
            return this.mKeyguardMonitor.getKeyguardFadingAwayDelay();
        }
        return this.mBouncer.isShowing() ? 160 : 0;
    }

    /* access modifiers changed from: protected */
    public void updateStates() {
        int systemUiVisibility = this.mContainer.getSystemUiVisibility();
        boolean z = this.mShowing;
        boolean z2 = this.mOccluded;
        boolean isShowing = this.mBouncer.isShowing();
        boolean z3 = true;
        boolean z4 = !this.mBouncer.isFullscreenBouncer();
        boolean z5 = this.mRemoteInputActive;
        if ((z4 || !z || z5) != (this.mLastBouncerDismissible || !this.mLastShowing || this.mLastRemoteInputActive) || this.mFirstUpdate) {
            if (z4 || !z || z5) {
                this.mContainer.setSystemUiVisibility(systemUiVisibility & -4194305);
            } else {
                this.mContainer.setSystemUiVisibility(systemUiVisibility | 4194304);
            }
        }
        boolean isNavBarVisible = isNavBarVisible();
        if (isNavBarVisible != getLastNavBarVisible() || this.mFirstUpdate) {
            updateNavigationBarVisibility(isNavBarVisible);
        }
        if (isShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            this.mStatusBarWindowController.setBouncerShowing(isShowing);
            this.mStatusBar.setBouncerShowing(isShowing);
        }
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((z && !z2) != (this.mLastShowing && !this.mLastOccluded) || this.mFirstUpdate) {
            if (!z || z2) {
                z3 = false;
            }
            instance.onKeyguardVisibilityChanged(z3);
        }
        if (isShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            instance.sendKeyguardBouncerChanged(isShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = z;
        this.mLastOccluded = z2;
        this.mLastBouncerShowing = isShowing;
        this.mLastBouncerDismissible = z4;
        this.mLastRemoteInputActive = z5;
        this.mLastDozing = this.mDozing;
        this.mLastPulsing = this.mPulsing;
        this.mLastBiometricMode = this.mBiometricUnlockController.getMode();
        this.mLastGesturalNav = this.mGesturalNav;
        this.mLastIsDocked = this.mIsDocked;
        this.mStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    /* access modifiers changed from: protected */
    public void updateNavigationBarVisibility(boolean z) {
        if (this.mStatusBar.getNavigationBarView() != null) {
            if (z) {
                long navBarShowDelay = getNavBarShowDelay();
                if (navBarShowDelay == 0) {
                    this.mMakeNavigationBarVisibleRunnable.run();
                } else {
                    this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, navBarShowDelay);
                }
            } else {
                this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
                this.mStatusBar.getNavigationBarView().getRootView().setVisibility(8);
            }
        }
        SkinningBridge.onNavigationBarVisibilityChanged(z);
    }

    /* access modifiers changed from: protected */
    public boolean isNavBarVisible() {
        int mode = this.mBiometricUnlockController.getMode();
        boolean z = this.mShowing && !this.mOccluded;
        return (!z && !(this.mDozing && mode != 2)) || this.mBouncer.isShowing() || this.mRemoteInputActive || (((z && !this.mDozing) || (this.mPulsing && !this.mIsDocked)) && this.mGesturalNav);
    }

    /* access modifiers changed from: protected */
    public boolean getLastNavBarVisible() {
        boolean z = this.mLastShowing && !this.mLastOccluded;
        return (!z && !(this.mLastDozing && this.mLastBiometricMode != 2)) || this.mLastBouncerShowing || this.mLastRemoteInputActive || (((z && !this.mLastDozing) || (this.mLastPulsing && !this.mLastIsDocked)) && this.mLastGesturalNav);
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mBouncer.shouldDismissOnMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        return this.mBouncer.interceptMediaKey(keyEvent);
    }

    public void readyForKeyguardDone() {
        this.mViewMediatorCallback.readyForKeyguardDone();
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        return this.mStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        return ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).leaveOpenOnKeyguardHide();
    }

    public void keyguardGoingAway() {
        this.mStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float f) {
        this.mStatusBar.animateCollapsePanels(0, true, false, f);
    }

    public void notifyKeyguardAuthenticated(boolean z) {
        this.mBouncer.notifyKeyguardAuthenticated(z);
    }

    public void showBouncerMessage(String str, ColorStateList colorStateList) {
        this.mBouncer.showMessage(str, colorStateList);
    }

    public ViewRootImpl getViewRootImpl() {
        return this.mStatusBar.getStatusBarView().getViewRootImpl();
    }

    public void launchPendingWakeupAction() {
        DismissWithActionRequest dismissWithActionRequest = this.mPendingWakeupAction;
        this.mPendingWakeupAction = null;
        if (dismissWithActionRequest == null) {
            return;
        }
        if (this.mShowing) {
            dismissWithAction(dismissWithActionRequest.dismissAction, dismissWithActionRequest.cancelAction, dismissWithActionRequest.afterKeyguardGone, dismissWithActionRequest.message);
            return;
        }
        ActivityStarter.OnDismissAction onDismissAction = dismissWithActionRequest.dismissAction;
        if (onDismissAction != null) {
            onDismissAction.onDismiss();
        }
    }

    public void cancelPendingWakeupAction() {
        Runnable runnable;
        DismissWithActionRequest dismissWithActionRequest = this.mPendingWakeupAction;
        this.mPendingWakeupAction = null;
        if (dismissWithActionRequest != null && (runnable = dismissWithActionRequest.cancelAction) != null) {
            runnable.run();
        }
    }

    public boolean bouncerNeedsScrimming() {
        return this.mOccluded || this.mBouncer.willDismissWithAction() || this.mStatusBar.isFullScreenUserSwitcherState() || (this.mBouncer.isShowing() && this.mBouncer.isScrimmed()) || this.mBouncer.isFullscreenBouncer();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("StatusBarKeyguardViewManager:");
        printWriter.println("  mShowing: " + this.mShowing);
        printWriter.println("  mOccluded: " + this.mOccluded);
        printWriter.println("  mRemoteInputActive: " + this.mRemoteInputActive);
        printWriter.println("  mDozing: " + this.mDozing);
        printWriter.println("  mGoingToSleepVisibleNotOccluded: " + this.mGoingToSleepVisibleNotOccluded);
        printWriter.println("  mAfterKeyguardGoneAction: " + this.mAfterKeyguardGoneAction);
        printWriter.println("  mAfterKeyguardGoneRunnables: " + this.mAfterKeyguardGoneRunnables);
        printWriter.println("  mPendingWakeupAction: " + this.mPendingWakeupAction);
        KeyguardBouncer keyguardBouncer = this.mBouncer;
        if (keyguardBouncer != null) {
            keyguardBouncer.dump(printWriter);
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        updateLockIcon();
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        setDozing(z);
    }

    /* access modifiers changed from: private */
    public static class DismissWithActionRequest {
        final boolean afterKeyguardGone;
        final Runnable cancelAction;
        final ActivityStarter.OnDismissAction dismissAction;
        final String message;

        DismissWithActionRequest(ActivityStarter.OnDismissAction onDismissAction, Runnable runnable, boolean z, String str) {
            this.dismissAction = onDismissAction;
            this.cancelAction = runnable;
            this.afterKeyguardGone = z;
            this.message = str;
        }
    }
}
