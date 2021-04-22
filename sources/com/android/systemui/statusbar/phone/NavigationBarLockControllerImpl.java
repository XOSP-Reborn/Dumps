package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.InputEvent;
import android.view.InputMonitor;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.InputChannelCompat$InputEventListener;
import com.android.systemui.shared.system.InputChannelCompat$InputEventReceiver;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class NavigationBarLockControllerImpl implements NavigationBarLockController, CommandQueue.Callbacks, NavigationModeController.ModeChangedListener {
    static final boolean DEBUG = Log.isLoggable("NavBarLockController", 3);
    private boolean mAccessibilityEnabled;
    private final AccessibilityManager.AccessibilityServicesStateChangeListener mAccessibilityListener = new AccessibilityManager.AccessibilityServicesStateChangeListener() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarLockControllerImpl$STykHzi1EHPb5RGq9Or8zKqQfHU */

        public final void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
            NavigationBarLockControllerImpl.this.lambda$new$0$NavigationBarLockControllerImpl(accessibilityManager);
        }
    };
    private final AutoHideController mAutoHideController;
    private final Context mContext;
    private final int mDisplayId;
    private boolean mGesturalMode;
    private final Handler mHandler;
    private boolean mHomeOrRecentsShowing;
    private boolean mImeShowing;
    private InputChannelCompat$InputEventReceiver mInputEventReceiver;
    private InputMonitor mInputMonitor;
    private final KeyguardMonitor.Callback mKeyguardListener = new KeyguardMonitor.Callback() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarLockControllerImpl$UiCMKGKHT2JGmxdA6CbTuMqnrZo */

        @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
        public final void onKeyguardShowingChanged() {
            NavigationBarLockControllerImpl.this.lambda$new$1$NavigationBarLockControllerImpl();
        }
    };
    private boolean mKeyguardShowing;
    private NavigationBarLockStateMachine mLockStateMachine;
    private boolean mLocked;
    private final NavigationBarController mNavBarController;
    private NavigationBarView mNavigationBarView;
    private boolean mPreRequisitesMet;
    private long mReLockDelay;
    private boolean mTalkBackEnabled;
    private final TaskStackChangeListener mTaskStackListener = new TaskStackChangeListener() {
        /* class com.android.systemui.statusbar.phone.NavigationBarLockControllerImpl.AnonymousClass1 */

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskStackChanged() {
            NavigationBarLockControllerImpl.this.updateFrontTask();
            NavigationBarLockControllerImpl.this.checkPrerequisites(false);
        }
    };
    private int mWindowState;

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setWindowState(int i, int i2, int i3) {
        NavigationBarLockStateMachine navigationBarLockStateMachine = this.mLockStateMachine;
        if (navigationBarLockStateMachine != null && i2 == 2 && this.mWindowState != i3 && i3 == 2) {
            navigationBarLockStateMachine.windowHidden();
        }
        this.mWindowState = i3;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setImeWindowStatus(int i, IBinder iBinder, int i2, int i3, boolean z) {
        this.mImeShowing = (i2 & 2) != 0;
        checkPrerequisites(false);
    }

    public /* synthetic */ void lambda$new$0$NavigationBarLockControllerImpl(AccessibilityManager accessibilityManager) {
        updateAccessibilityState(accessibilityManager);
        checkPrerequisites(false);
    }

    public /* synthetic */ void lambda$new$1$NavigationBarLockControllerImpl() {
        updateKeyguardState();
        checkPrerequisites(false);
    }

    public NavigationBarLockControllerImpl(Context context) {
        this.mContext = context;
        this.mDisplayId = context.getDisplayId();
        this.mNavBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
        this.mAutoHideController = (AutoHideController) Dependency.get(AutoHideController.class);
        this.mReLockDelay = (long) context.getResources().getInteger(C0008R$integer.navbar_lock_relock_timeout);
        this.mHandler = new Handler();
        this.mGesturalMode = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this));
        updateAccessibilityState(AccessibilityManager.getInstance(this.mContext));
        updateKeyguardState();
        updateFrontTask();
        this.mImeShowing = false;
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallback((CommandQueue.Callbacks) this);
        ((AccessibilityManagerWrapper) Dependency.get(AccessibilityManagerWrapper.class)).addCallback(this.mAccessibilityListener);
        ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(this.mKeyguardListener);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void setNavigationBarView(NavigationBarView navigationBarView) {
        this.mNavigationBarView = navigationBarView;
    }

    @Override // com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener
    public void onNavigationModeChanged(int i) {
        this.mGesturalMode = QuickStepContract.isGesturalMode(i);
        checkPrerequisites(false);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void onAccessibilityButtonState(boolean z) {
        this.mAccessibilityEnabled = z;
        checkPrerequisites(false);
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void onTouchEvent(MotionEvent motionEvent) {
        if (this.mLockStateMachine != null) {
            if (motionEvent.getActionMasked() == 0) {
                this.mLockStateMachine.touchPressed();
            }
            if (this.mLockStateMachine.isLocked()) {
                this.mNavBarController.touchAutoDim(this.mDisplayId);
            }
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void setReLockDelay(long j) {
        this.mReLockDelay = j;
        NavigationBarLockStateMachine navigationBarLockStateMachine = this.mLockStateMachine;
        if (navigationBarLockStateMachine != null) {
            navigationBarLockStateMachine.setReLockDelay(this.mReLockDelay);
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void setEnabled(boolean z) {
        if (z && this.mLockStateMachine == null) {
            this.mLockStateMachine = new NavigationBarLockStateMachine(this.mHandler, new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarLockControllerImpl$SZx9Cepfis1Zz2qm9OM9izetXww */

                public final void run() {
                    NavigationBarLockControllerImpl.this.lockSet();
                }
            });
            this.mLockStateMachine.setReLockDelay(this.mReLockDelay);
            this.mLockStateMachine.start();
            checkPrerequisites(true);
            registerInputMonitor();
        } else if (!z && this.mLockStateMachine != null) {
            unregisterInputMonitor();
            this.mLockStateMachine.quit();
            this.mLockStateMachine = null;
        }
        if (DEBUG) {
            Log.d("NavBarLockController", "NavBarLock display: " + this.mDisplayId + " enabled: " + z);
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void unlock() {
        NavigationBarLockStateMachine navigationBarLockStateMachine = this.mLockStateMachine;
        if (navigationBarLockStateMachine != null) {
            navigationBarLockStateMachine.unlock();
        }
    }

    @Override // com.android.systemui.statusbar.phone.NavigationBarLockController
    public void destroy() {
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).removeCallback((CommandQueue.Callbacks) this);
        ((AccessibilityManagerWrapper) Dependency.get(AccessibilityManagerWrapper.class)).removeCallback(this.mAccessibilityListener);
        ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(this.mKeyguardListener);
        ActivityManagerWrapper.getInstance().unregisterTaskStackListener(this.mTaskStackListener);
        setEnabled(false);
    }

    /* access modifiers changed from: private */
    public void onInputEvent(InputEvent inputEvent) {
        if ((inputEvent instanceof MotionEvent) && ((MotionEvent) inputEvent).getActionMasked() == 1) {
            this.mLockStateMachine.touchReleased();
        }
    }

    private void registerInputMonitor() {
        unregisterInputMonitor();
        this.mInputMonitor = InputManager.getInstance().monitorGestureInput("nav_lock", this.mDisplayId);
        this.mInputEventReceiver = new InputChannelCompat$InputEventReceiver(this.mInputMonitor.getInputChannel(), Looper.getMainLooper(), Choreographer.getMainThreadInstance(), new InputChannelCompat$InputEventListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$NavigationBarLockControllerImpl$qb2aWlkkpuV5y2YudWhNfOoFJtY */

            @Override // com.android.systemui.shared.system.InputChannelCompat$InputEventListener
            public final void onInputEvent(InputEvent inputEvent) {
                NavigationBarLockControllerImpl.this.onInputEvent(inputEvent);
            }
        });
    }

    private void unregisterInputMonitor() {
        InputChannelCompat$InputEventReceiver inputChannelCompat$InputEventReceiver = this.mInputEventReceiver;
        if (inputChannelCompat$InputEventReceiver != null) {
            inputChannelCompat$InputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        InputMonitor inputMonitor = this.mInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mInputMonitor = null;
        }
    }

    /* access modifiers changed from: private */
    public void lockSet() {
        NavigationBarLockStateMachine navigationBarLockStateMachine = this.mLockStateMachine;
        boolean z = navigationBarLockStateMachine != null && navigationBarLockStateMachine.isLocked();
        if (z != this.mLocked) {
            this.mNavigationBarView.setLocked(z);
            if (!z) {
                this.mAutoHideController.touchAutoHide();
            }
            this.mLocked = z;
        }
    }

    private void updateAccessibilityState(AccessibilityManager accessibilityManager) {
        this.mTalkBackEnabled = accessibilityManager.isTouchExplorationEnabled();
    }

    private void updateKeyguardState() {
        this.mKeyguardShowing = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing();
    }

    public void updateFrontTask() {
        boolean z = false;
        ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask(0);
        if (runningTask != null) {
            int activityType = runningTask.configuration.windowConfiguration.getActivityType();
            if (activityType == 2 || activityType == 3) {
                z = true;
            }
            this.mHomeOrRecentsShowing = z;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void checkPrerequisites(boolean z) {
        boolean z2 = !this.mGesturalMode && !this.mImeShowing && !this.mAccessibilityEnabled && !this.mTalkBackEnabled && !this.mKeyguardShowing && !this.mHomeOrRecentsShowing;
        if (this.mLockStateMachine != null && (z || z2 != this.mPreRequisitesMet)) {
            if (DEBUG) {
                Log.d("NavBarLockController", "mWindowState=" + this.mWindowState + ", mGesturalMode=" + this.mGesturalMode + ", mImeShowing=" + this.mImeShowing + ", mAccessibilityEnabled=" + this.mAccessibilityEnabled + ", mTalkbackEnabled=" + this.mTalkBackEnabled + ", mKeyguardShowing=" + this.mKeyguardShowing + ", mHomeOrRecentsShowing=" + this.mHomeOrRecentsShowing + ", mReLockDelay=" + this.mReLockDelay);
            }
            this.mLockStateMachine.preRequisitesChanged(z2);
        }
        this.mPreRequisitesMet = z2;
    }
}
