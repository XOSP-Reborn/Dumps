package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.IApplicationThread;
import android.app.IWallpaperManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProfilerInfo;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.DateTimeView;
import android.widget.ImageView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.RegisterStatusBarResult;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.ActivityIntentHelper;
import com.android.systemui.ActivityStarterDelegate;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.C0001R$array;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.C0015R$style;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.InitController;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.appops.AppOpsController;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.charging.WirelessChargingAnimation;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.fragments.ExtensionFragmentListener;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.keyguard.KeyguardSliceProvider;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyboardShortcuts;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationActivityStarter;
import com.android.systemui.statusbar.notification.NotificationAlertingManager;
import com.android.systemui.statusbar.notification.NotificationClicker;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationFilter;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.NotificationListController;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinderImpl;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.InjectionInflationController;
import com.android.systemui.volume.VolumeComponent;
import com.sonymobile.keyguard.SomcKeyguardRuntimeResources;
import com.sonymobile.keyguard.aod.AodView;
import com.sonymobile.keyguard.aod.PhotoPlaybackView;
import com.sonymobile.runtimeskinning.SkinningBridge;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StatusBar extends SystemUI implements DemoMode, ActivityStarter, UnlockMethodCache.OnUnlockMethodChangedListener, OnHeadsUpChangedListener, CommandQueue.Callbacks, ZenModeController.Callback, ColorExtractor.OnColorsChangedListener, ConfigurationController.ConfigurationListener, StatusBarStateController.StateListener, ShadeController, ActivityLaunchAnimator.Callback, AmbientPulseManager.OnAmbientChangedListener, AppOpsController.Callback {
    protected static final int[] APP_OPS = {26, 24, 27, 0, 1};
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    public static final boolean ONLY_CORE_APPS;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private final int[] mAbsPos = new int[2];
    protected AccessibilityManager mAccessibilityManager;
    private ActivityIntentHelper mActivityIntentHelper;
    private ActivityLaunchAnimator mActivityLaunchAnimator;
    private View mAmbientIndicationContainer;
    protected AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    private final Runnable mAnimateCollapsePanels = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$hcoUGmHpwgtk12ln4V8HNBe6RFA */

        public final void run() {
            StatusBar.this.animateCollapsePanels();
        }
    };
    AodView mAodView;
    protected AppOpsController mAppOpsController;
    protected AssistManager mAssistManager;
    @VisibleForTesting
    protected AutoHideController mAutoHideController;
    private final BroadcastReceiver mBannerActionBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass14 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || "com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                ((NotificationManager) StatusBar.this.mContext.getSystemService("notification")).cancel(5);
                Settings.Secure.putInt(StatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if ("com.android.systemui.statusbar.banner_action_setup".equals(action)) {
                    StatusBar.this.animateCollapsePanels(2, true);
                    StatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    };
    protected IStatusBarService mBarService;
    private BatteryController mBatteryController;
    protected BiometricUnlockController mBiometricUnlockController;
    protected boolean mBouncerShowing;
    private boolean mBouncerWasShowingWhenHidden;
    private BrightnessMirrorController mBrightnessMirrorController;
    private boolean mBrightnessMirrorVisible;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass8 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int i = 0;
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                KeyboardShortcuts.dismiss();
                if (StatusBar.this.mRemoteInputManager.getController() != null) {
                    StatusBar.this.mRemoteInputManager.getController().closeRemoteInputs();
                }
                if (StatusBar.this.mBubbleController.isStackExpanded()) {
                    StatusBar.this.mBubbleController.collapseStack();
                }
                if (StatusBar.this.mLockscreenUserManager.isCurrentProfile(getSendingUserId())) {
                    String stringExtra = intent.getStringExtra("reason");
                    if (stringExtra != null && stringExtra.equals("recentapps")) {
                        i = 2;
                    }
                    StatusBar.this.animateCollapsePanels(i);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                StatusBarWindowController statusBarWindowController = StatusBar.this.mStatusBarWindowController;
                if (statusBarWindowController != null) {
                    statusBarWindowController.setNotTouchable(false);
                }
                if (StatusBar.this.mBubbleController.isStackExpanded()) {
                    StatusBar.this.mBubbleController.collapseStack();
                }
                StatusBar.this.finishBarAnimations();
                StatusBar.this.resetUserExpandedStates();
            } else if ("android.app.action.SHOW_DEVICE_MONITORING_DIALOG".equals(action)) {
                StatusBar.this.mQSPanel.showDeviceMonitoringDialog();
            }
        }
    };
    protected BubbleController mBubbleController;
    private final BubbleController.BubbleExpandListener mBubbleExpandListener = new BubbleController.BubbleExpandListener() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$be2UvXBqvJVkeR4_MOL5Z579OFk */

        @Override // com.android.systemui.bubbles.BubbleController.BubbleExpandListener
        public final void onBubbleExpandChanged(boolean z, String str) {
            StatusBar.this.lambda$new$1$StatusBar(z, str);
        }
    };
    private String mCameraKeyLaunchPackage;
    private long[] mCameraLaunchGestureVibePattern;
    private String mCameraLiftTriggerLaunchPackage;
    private long[] mCameraLiftTriggerVibePattern;
    private final Runnable mCheckBarModes = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$KBnY14rlKZ6x8gvk_goBuFrr5eE */

        public final void run() {
            StatusBar.this.checkBarModes();
        }
    };
    private SysuiColorExtractor mColorExtractor;
    protected CommandQueue mCommandQueue;
    private final Point mCurrentDisplaySize = new Point();
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    private final BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass9 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String lowerCase = extras.getString("command", "").trim().toLowerCase();
                    if (lowerCase.length() > 0) {
                        try {
                            StatusBar.this.dispatchDemoCommand(lowerCase, extras);
                        } catch (Throwable th) {
                            Log.w("StatusBar", "Error running demo command, intent=" + intent, th);
                        }
                    }
                }
            } else {
                "fake_artwork".equals(action);
            }
        }
    };
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    private DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private int mDisabled1 = 0;
    private int mDisabled2 = 0;
    protected Display mDisplay;
    private int mDisplayId;
    private final DisplayMetrics mDisplayMetrics = ((DisplayMetrics) Dependency.get(DisplayMetrics.class));
    protected DozeScrimController mDozeScrimController;
    @VisibleForTesting
    DozeServiceHost mDozeServiceHost = new DozeServiceHost();
    protected boolean mDozing;
    private boolean mDozingRequested;
    private NotificationEntry mDraggedDownEntry;
    private IDreamManager mDreamManager;
    protected NotificationEntryManager mEntryManager;
    private boolean mExpandedVisible;
    protected FalsingManager mFalsingManager;
    protected ForegroundServiceController mForegroundServiceController;
    private final GestureRecorder mGestureRec = null;
    protected PowerManager.WakeLock mGestureWakeLock;
    private final View.OnClickListener mGoToLockedShadeListener = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$yGW3LliHoPrdVSisJBkD7OsnTE */

        public final void onClick(View view) {
            StatusBar.this.lambda$new$0$StatusBar(view);
        }
    };
    protected NotificationGroupAlertTransferHelper mGroupAlertTransferHelper;
    protected NotificationGroupManager mGroupManager;
    private NotificationGutsManager mGutsManager;
    protected final H mHandler = createHandler();
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    protected HeadsUpManagerPhone mHeadsUpManager;
    private boolean mHideIconsForBouncer;
    protected StatusBarIconController mIconController;
    private PhoneStatusBarPolicy mIconPolicy;
    InjectionInflationController mInjectionInflater;
    private int mInteractingWindows;
    protected boolean mIsKeyguard;
    private boolean mIsMoveSystemBars;
    private boolean mIsOccluded;
    private boolean mIsSkinningEnabled;
    KeyguardIndicationController mKeyguardIndicationController;
    protected KeyguardManager mKeyguardManager;
    private KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    @VisibleForTesting
    KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    protected KeyguardViewMediator mKeyguardViewMediator;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private int mLastCameraLaunchSource;
    private final Rect mLastDockedStackBounds = new Rect();
    private final Rect mLastFullscreenStackBounds = new Rect();
    private int mLastLoggedStateFingerprint;
    private boolean mLaunchCameraOnFinishedGoingToSleep;
    private boolean mLaunchCameraWhenFinishedWaking;
    private Runnable mLaunchTransitionEndRunnable;
    private boolean mLaunchingCamera;
    private LightBarController mLightBarController;
    protected NotificationLockscreenUserManager mLockscreenUserManager;
    protected LockscreenWallpaper mLockscreenWallpaper;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private NotificationMediaManager mMediaManager;
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private int mNaturalBarHeight = -1;
    protected NavigationBarController mNavigationBarController;
    private NetworkController mNetworkController;
    private boolean mNoAnimationOnNextBarModeChange;
    private NotificationActivityStarter mNotificationActivityStarter;
    private final NotificationAlertingManager mNotificationAlertingManager = ((NotificationAlertingManager) Dependency.get(NotificationAlertingManager.class));
    protected NotificationFilter mNotificationFilter;
    protected NotificationIconAreaController mNotificationIconAreaController;
    private NotificationInterruptionStateProvider mNotificationInterruptionStateProvider;
    private NotificationListController mNotificationListController;
    protected NotificationListener mNotificationListener;
    protected NotificationLogger mNotificationLogger;
    protected NotificationPanelView mNotificationPanel;
    protected NotificationShelf mNotificationShelf;
    protected boolean mPanelExpanded;
    private View mPendingRemoteInputView;
    PhotoPlaybackView mPhotoPlaybackView;
    private final ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    protected PowerManager mPowerManager;
    protected NotificationPresenter mPresenter;
    PulseExpansionHandler mPulseExpansionHandler;
    private boolean mPulsing;
    private QSPanel mQSPanel;
    private final Object mQueueLock = new Object();
    protected Recents mRecents;
    protected NotificationRemoteInputManager mRemoteInputManager;
    private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler = ((RemoteInputQuickSettingsDisabler) Dependency.get(RemoteInputQuickSettingsDisabler.class));
    private View mReportRejectedTouch;
    private ScreenLifecycle mScreenLifecycle;
    final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass13 */

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurningOn() {
            StatusBar.this.mFalsingManager.onScreenTurningOn();
            StatusBar.this.mNotificationPanel.onScreenTurningOn();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            StatusBar.this.mScrimController.onScreenTurnedOn();
        }

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOff() {
            StatusBar.this.updateDozing();
            StatusBar.this.mFalsingManager.onScreenOff();
            StatusBar.this.mScrimController.onScreenTurnedOff();
            StatusBar.this.updateIsKeyguard();
        }
    };
    private ScreenPinningRequest mScreenPinningRequest;
    protected ScrimController mScrimController;
    private ShadeController mShadeController;
    private StatusBarSignalPolicy mSignalPolicy;
    protected ViewGroup mStackScroller;
    final Runnable mStartTracing = new Runnable() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass10 */

        public void run() {
            StatusBar.this.vibrate();
            SystemClock.sleep(250);
            Log.d("StatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            StatusBar statusBar = StatusBar.this;
            statusBar.mHandler.postDelayed(statusBar.mStopTracing, 10000);
        }
    };
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private int mStatusBarMode;
    private final SysuiStatusBarStateController mStatusBarStateController = ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class));
    private LogMaker mStatusBarStateLog;
    protected PhoneStatusBarView mStatusBarView;
    protected StatusBarWindowView mStatusBarWindow;
    protected StatusBarWindowController mStatusBarWindowController;
    private boolean mStatusBarWindowHidden;
    private int mStatusBarWindowState = 0;
    final Runnable mStopTracing = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$RAI9_BB0sxI6fAXVPwmNkObnx6k */

        public final void run() {
            StatusBar.this.lambda$new$22$StatusBar();
        }
    };
    private int mSystemUiVisibility = 0;
    private final int[] mTmpInt2 = new int[2];
    private boolean mTopHidesStatusBar;
    private UiModeManager mUiModeManager;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    protected UnlockMethodCache mUnlockMethodCache;
    private final ScrimController.Callback mUnlockScrimCallback = new ScrimController.Callback() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass3 */

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onFinished() {
            StatusBar statusBar = StatusBar.this;
            if (statusBar.mStatusBarKeyguardViewManager == null) {
                Log.w("StatusBar", "Tried to notify keyguard visibility when mStatusBarKeyguardViewManager was null");
            } else if (statusBar.mKeyguardMonitor.isKeyguardFadingAway()) {
                StatusBar.this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
            }
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onCancelled() {
            onFinished();
        }
    };
    private final KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass4 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onDreamingStateChanged(boolean z) {
            if (z) {
                StatusBar.this.maybeEscalateHeadsUp();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            super.onStrongAuthStateChanged(i);
            StatusBar.this.mEntryManager.updateNotifications();
        }
    };
    @VisibleForTesting
    protected boolean mUserSetup = false;
    private final DeviceProvisionedController.DeviceProvisionedListener mUserSetupObserver = new DeviceProvisionedController.DeviceProvisionedListener() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass1 */

        @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
        public void onUserSetupChanged() {
            boolean isUserSetup = StatusBar.this.mDeviceProvisionedController.isUserSetup(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            Log.d("StatusBar", "mUserSetupObserver - DeviceProvisionedListener called for user " + StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            StatusBar statusBar = StatusBar.this;
            if (isUserSetup != statusBar.mUserSetup) {
                statusBar.mUserSetup = isUserSetup;
                if (!statusBar.mUserSetup && statusBar.mStatusBarView != null) {
                    statusBar.animateCollapseQuickSettings();
                }
                StatusBar statusBar2 = StatusBar.this;
                NotificationPanelView notificationPanelView = statusBar2.mNotificationPanel;
                if (notificationPanelView != null) {
                    notificationPanelView.setUserSetupComplete(statusBar2.mUserSetup);
                }
                StatusBar.this.updateQsExpansionEnabled();
            }
        }
    };
    protected UserSwitcherController mUserSwitcherController;
    private boolean mVibrateOnOpening;
    private Vibrator mVibrator;
    private VibratorHelper mVibratorHelper;
    protected NotificationViewHierarchyManager mViewHierarchyManager;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected VisualStabilityManager mVisualStabilityManager;
    private VolumeComponent mVolumeComponent;
    private boolean mWakeUpComingFromTouch;
    NotificationWakeUpCoordinator mWakeUpCoordinator;
    private PointF mWakeUpTouchLocation;
    @VisibleForTesting
    WakefulnessLifecycle mWakefulnessLifecycle;
    @VisibleForTesting
    final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass12 */
        private long mStartSleepTime;

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedGoingToSleep() {
            StatusBar.this.mNotificationPanel.onAffordanceLaunchEnded();
            StatusBar.this.releaseGestureWakeLock();
            StatusBar.this.mLaunchCameraWhenFinishedWaking = false;
            StatusBar statusBar = StatusBar.this;
            statusBar.mDeviceInteractive = false;
            statusBar.mWakeUpComingFromTouch = false;
            StatusBar.this.mWakeUpTouchLocation = null;
            StatusBar.this.mVisualStabilityManager.setScreenOn(false);
            StatusBar.this.updateVisibleToUser();
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            if (StatusBar.this.mLaunchCameraOnFinishedGoingToSleep) {
                StatusBar.this.mLaunchCameraOnFinishedGoingToSleep = false;
                StatusBar.this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$12$y9_RRyD4rDeCN3cFnbhrxNLuI7g */

                    public final void run() {
                        StatusBar.AnonymousClass12.this.lambda$onFinishedGoingToSleep$0$StatusBar$12();
                    }
                });
            }
            StatusBar.this.updateIsKeyguard();
            this.mStartSleepTime = SystemClock.uptimeMillis();
        }

        public /* synthetic */ void lambda$onFinishedGoingToSleep$0$StatusBar$12() {
            StatusBar statusBar = StatusBar.this;
            statusBar.onCameraLaunchGestureDetected(statusBar.mLastCameraLaunchSource);
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedGoingToSleep() {
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.notifyHeadsUpGoingToSleep();
            StatusBar.this.dismissVolumeDialog();
            StatusBar.this.mNotificationFilter.resetHideNotificationState();
            StatusBarKeyguardViewManager statusBarKeyguardViewManager = StatusBar.this.mStatusBarKeyguardViewManager;
            if (statusBarKeyguardViewManager != null && !statusBarKeyguardViewManager.isShowing()) {
                StatusBar.this.mNotificationFilter.updateNotificationViewedTime();
            }
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onStartedWakingUp() {
            if (StatusBar.this.mIsMoveSystemBars) {
                StatusBar.this.mStatusBarView.moveStatusBar();
                NavigationBarView defaultNavigationBarView = StatusBar.this.mNavigationBarController.getDefaultNavigationBarView();
                if (SystemClock.uptimeMillis() - this.mStartSleepTime >= 10000) {
                    StatusBar.this.mStatusBarView.moveStatusBar();
                    if (defaultNavigationBarView != null) {
                        defaultNavigationBarView.moveNavigationBar();
                    }
                }
            }
            StatusBar statusBar = StatusBar.this;
            statusBar.mDeviceInteractive = true;
            statusBar.mWakeUpCoordinator.setWakingUp(true);
            StatusBar.this.mAmbientPulseManager.releaseAllImmediately();
            StatusBar.this.mVisualStabilityManager.setScreenOn(true);
            StatusBar.this.updateVisibleToUser();
            StatusBar.this.updateIsKeyguard();
            StatusBar.this.mDozeServiceHost.stopDozing();
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.mPulseExpansionHandler.onStartedWakingUp();
        }

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedWakingUp() {
            StatusBar.this.mWakeUpCoordinator.setWakingUp(false);
            if (StatusBar.this.mLaunchCameraWhenFinishedWaking) {
                StatusBar statusBar = StatusBar.this;
                statusBar.mNotificationPanel.launchCamera(false, statusBar.mLastCameraLaunchSource);
                StatusBar.this.mLaunchCameraWhenFinishedWaking = false;
            }
            StatusBar.this.updateScrimController();
        }
    };
    private final BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) {
                Log.w("StatusBar", "WallpaperManager not available");
                return;
            }
            WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo(-2);
            boolean z = StatusBar.this.mContext.getResources().getBoolean(17891424);
            boolean z2 = true;
            boolean z3 = !DozeParameters.getInstance(StatusBar.this.mContext).getDisplayNeedsBlanking();
            if (!z || ((wallpaperInfo != null || !z3) && (wallpaperInfo == null || !wallpaperInfo.supportsAmbientMode()))) {
                z2 = false;
            }
            StatusBar.this.mStatusBarWindowController.setWallpaperSupportsAmbientMode(z2);
            StatusBar.this.mScrimController.setWallpaperSupportsAmbientMode(z2);
        }
    };
    private boolean mWereIconsJustHidden;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    private ZenModeController mZenController;

    public interface StatusBarInjector {
        void createStatusBar(StatusBar statusBar);
    }

    private int barMode(int i) {
        if ((67108864 & i) != 0) {
            return 1;
        }
        if ((1073741824 & i) != 0) {
            return 2;
        }
        if ((i & 9) == 9) {
            return 6;
        }
        if ((i & 8) != 0) {
            return 4;
        }
        return (i & 1) != 0 ? 3 : 0;
    }

    private static int getLoggingFingerprint(int i, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
        return (i & 255) | ((z ? 1 : 0) << 8) | ((z2 ? 1 : 0) << 9) | ((z3 ? 1 : 0) << 10) | ((z4 ? 1 : 0) << 11) | ((z5 ? 1 : 0) << 12);
    }

    public String getCameraLaunchSourceString(int i) {
        return i == 1 ? "power_double_tap" : i == 0 ? "wiggle_gesture" : i == 2 ? "lift_to_launch_ml" : i == 4 ? "camera_long_press" : "lockscreen_affordance";
    }

    static {
        boolean z;
        try {
            z = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException unused) {
            z = false;
        }
        ONLY_CORE_APPS = z;
    }

    public /* synthetic */ void lambda$new$0$StatusBar(View view) {
        if (this.mState == 1) {
            wakeUpIfDozing(SystemClock.uptimeMillis(), view, "SHADE_CLICK");
            goToLockedShade(null);
        }
    }

    public /* synthetic */ void lambda$new$1$StatusBar(boolean z, String str) {
        this.mEntryManager.updateNotifications();
        updateScrimController();
    }

    @Override // com.android.systemui.appops.AppOpsController.Callback
    public void onActiveStateChanged(int i, int i2, String str, boolean z) {
        ((Handler) Dependency.get(Dependency.MAIN_HANDLER)).post(new Runnable(i, i2, str, z) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$1N2jdpaP82HJRT31BJo2G2gJK5c */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ String f$3;
            private final /* synthetic */ boolean f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                StatusBar.this.lambda$onActiveStateChanged$2$StatusBar(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        });
    }

    public /* synthetic */ void lambda$onActiveStateChanged$2$StatusBar(int i, int i2, String str, boolean z) {
        this.mForegroundServiceController.onAppOpChanged(i, i2, str, z);
        this.mNotificationListController.updateNotificationsForAppOp(i, i2, str, z);
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        RegisterStatusBarResult registerStatusBarResult;
        this.mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
        this.mGroupAlertTransferHelper = (NotificationGroupAlertTransferHelper) Dependency.get(NotificationGroupAlertTransferHelper.class);
        this.mVisualStabilityManager = (VisualStabilityManager) Dependency.get(VisualStabilityManager.class);
        this.mNotificationLogger = (NotificationLogger) Dependency.get(NotificationLogger.class);
        this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        this.mNotificationListener = (NotificationListener) Dependency.get(NotificationListener.class);
        this.mNotificationListener.registerAsSystemService();
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
        this.mScreenLifecycle.addObserver(this.mScreenObserver);
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        this.mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        this.mGutsManager = (NotificationGutsManager) Dependency.get(NotificationGutsManager.class);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mNotificationFilter = (NotificationFilter) Dependency.get(NotificationFilter.class);
        this.mNotificationInterruptionStateProvider = (NotificationInterruptionStateProvider) Dependency.get(NotificationInterruptionStateProvider.class);
        this.mViewHierarchyManager = (NotificationViewHierarchyManager) Dependency.get(NotificationViewHierarchyManager.class);
        this.mForegroundServiceController = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
        this.mAppOpsController = (AppOpsController) Dependency.get(AppOpsController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mKeyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mNavigationBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
        this.mBubbleController = (BubbleController) Dependency.get(BubbleController.class);
        this.mBubbleController.setExpandListener(this.mBubbleExpandListener);
        this.mActivityIntentHelper = new ActivityIntentHelper(this.mContext);
        KeyguardSliceProvider attachedInstance = KeyguardSliceProvider.getAttachedInstance();
        if (attachedInstance != null) {
            attachedInstance.initDependencies(this.mMediaManager, this.mStatusBarStateController);
        } else {
            Log.w("StatusBar", "Cannot init KeyguardSliceProvider dependencies");
        }
        this.mColorExtractor.addOnColorsChangedListener(this);
        this.mStatusBarStateController.addCallback(this, 0);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayId = this.mDisplay.getDisplayId();
        updateDisplaySize();
        this.mVibrateOnOpening = this.mContext.getResources().getBoolean(C0003R$bool.config_vibrateOnIconAnimation);
        this.mVibratorHelper = (VibratorHelper) Dependency.get(VibratorHelper.class);
        DateTimeView.setReceiverHandler((Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        putComponent(StatusBar.class, this);
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (Recents) getComponent(Recents.class);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mCommandQueue = (CommandQueue) getComponent(CommandQueue.class);
        this.mCommandQueue.addCallback((CommandQueue.Callbacks) this);
        try {
            registerStatusBarResult = this.mBarService.registerStatusBar(this.mCommandQueue);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            registerStatusBarResult = null;
        }
        createAndAddWindows(registerStatusBarResult);
        this.mContext.registerReceiverAsUser(this.mWallpaperChangedReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"), null, null);
        this.mWallpaperChangedReceiver.onReceive(this.mContext, null);
        setUpPresenter();
        setSystemUiVisibility(this.mDisplayId, registerStatusBarResult.mSystemUiVisibility, registerStatusBarResult.mFullscreenStackSysUiVisibility, registerStatusBarResult.mDockedStackSysUiVisibility, -1, registerStatusBarResult.mFullscreenStackBounds, registerStatusBarResult.mDockedStackBounds, registerStatusBarResult.mNavbarColorManagedByIme);
        setImeWindowStatus(this.mDisplayId, registerStatusBarResult.mImeToken, registerStatusBarResult.mImeWindowVis, registerStatusBarResult.mImeBackDisposition, registerStatusBarResult.mShowImeSwitcher);
        int size = registerStatusBarResult.mIcons.size();
        for (int i = 0; i < size; i++) {
            this.mCommandQueue.setIcon((String) registerStatusBarResult.mIcons.keyAt(i), (StatusBarIcon) registerStatusBarResult.mIcons.valueAt(i));
        }
        SkinningBridge.onUserSwitched(this.mContext, this.mLockscreenUserManager.getCurrentUserId());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBannerActionBroadcastReceiver, intentFilter, "com.android.systemui.permission.SELF", null);
        try {
            IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).setInAmbientMode(false, 0);
        } catch (RemoteException unused) {
        }
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mIconController);
        this.mSignalPolicy = new StatusBarSignalPolicy(this.mContext, this.mIconController);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateCallback);
        putComponent(DozeHost.class, this.mDozeServiceHost);
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
        this.mFalsingManager = FalsingManagerFactory.getInstance(this.mContext);
        ((ActivityStarterDelegate) Dependency.get(ActivityStarterDelegate.class)).setActivityStarterImpl(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        ((InitController) Dependency.get(InitController.class)).addPostInitTask(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$c2AOy3A7uAuedqvDvblQbirmzTM */

            public final void run() {
                StatusBar.this.updateAreThereNotifications();
            }
        });
        ((InitController) Dependency.get(InitController.class)).addPostInitTask(new Runnable(registerStatusBarResult.mDisabledFlags1, registerStatusBarResult.mDisabledFlags2) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$QO7mysPBJLAKP36FTSzhErEZZ8 */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                StatusBar.this.lambda$start$3$StatusBar(this.f$1, this.f$2);
            }
        });
        Resources resources = this.mContext.getResources();
        this.mCameraKeyLaunchPackage = resources.getString(17039713);
        if (this.mCameraKeyLaunchPackage.isEmpty()) {
            this.mCameraKeyLaunchPackage = null;
        }
        this.mCameraLiftTriggerLaunchPackage = resources.getString(17039715);
        if (this.mCameraLiftTriggerLaunchPackage.isEmpty()) {
            this.mCameraLiftTriggerLaunchPackage = null;
        }
        this.mIsMoveSystemBars = this.mContext.getResources().getBoolean(C0003R$bool.config_enableMoveSystemBars);
    }

    /* access modifiers changed from: protected */
    public void makeStatusBarView(RegisterStatusBarResult registerStatusBarResult) {
        Context context = this.mContext;
        updateDisplaySize();
        updateResources();
        updateTheme();
        inflateStatusBarWindow(context);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(getStatusBarWindowTouchListener());
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(C0007R$id.notification_panel);
        this.mStackScroller = (ViewGroup) this.mStatusBarWindow.findViewById(C0007R$id.notification_stack_scroller);
        this.mZenController.addCallback(this);
        this.mNotificationLogger.setUpWithContainer((NotificationListContainer) this.mStackScroller);
        this.mNotificationIconAreaController = SystemUIFactory.getInstance().createNotificationIconAreaController(context, this, this.mStatusBarStateController);
        inflateShelf();
        this.mNotificationIconAreaController.setupShelf(this.mNotificationShelf);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mNotificationIconAreaController);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(DarkIconDispatcher.class);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(StatusBarStateController.class);
        FragmentHostManager fragmentHostManager = FragmentHostManager.get(this.mStatusBarWindow);
        fragmentHostManager.addTagListener("CollapsedStatusBarFragment", new FragmentHostManager.FragmentListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$dy7qcM4vmC01_Sduz1UMseDUmo */

            @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
            public final void onFragmentViewCreated(String str, Fragment fragment) {
                StatusBar.this.lambda$makeStatusBarView$4$StatusBar(str, fragment);
            }
        });
        fragmentHostManager.getFragmentManager().beginTransaction().replace(C0007R$id.status_bar_container, new CollapsedStatusBarFragment(), "CollapsedStatusBarFragment").commit();
        this.mIconController = (StatusBarIconController) Dependency.get(StatusBarIconController.class);
        this.mHeadsUpManager = new HeadsUpManagerPhone(context, this.mStatusBarWindow, this.mGroupManager, this, this.mVisualStabilityManager);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mHeadsUpManager);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mHeadsUpManager.addListener(this.mGroupManager);
        this.mHeadsUpManager.addListener(this.mGroupAlertTransferHelper);
        this.mHeadsUpManager.addListener(this.mVisualStabilityManager);
        this.mAmbientPulseManager.addListener(this);
        this.mAmbientPulseManager.addListener(this.mGroupManager);
        this.mAmbientPulseManager.addListener(this.mGroupAlertTransferHelper);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupAlertTransferHelper.setHeadsUpManager(this.mHeadsUpManager);
        this.mNotificationLogger.setHeadsUpManager(this.mHeadsUpManager);
        putComponent(HeadsUpManager.class, this.mHeadsUpManager);
        createNavigationBar(registerStatusBarResult);
        this.mLockscreenWallpaper = new LockscreenWallpaper(this.mContext, this, this.mHandler);
        this.mAodView = (AodView) this.mStatusBarWindow.findViewById(C0007R$id.aod_view);
        this.mAodView.setNotificationData(this.mEntryManager.getNotificationData());
        this.mPhotoPlaybackView = (PhotoPlaybackView) this.mStatusBarWindow.findViewById(C0007R$id.aod_photo_playback_view);
        this.mPhotoPlaybackView.setStatusBar(this);
        this.mAodView.setStatusBar(this);
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, (ViewGroup) this.mStatusBarWindow.findViewById(C0007R$id.keyguard_indication_area), (LockIcon) this.mStatusBarWindow.findViewById(C0007R$id.lock_icon));
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
        this.mAmbientIndicationContainer = this.mStatusBarWindow.findViewById(C0007R$id.ambient_indication_container);
        this.mBatteryController.addCallback(new BatteryController.BatteryStateChangeCallback() {
            /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass5 */

            @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
            public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
            }

            @Override // com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback
            public void onPowerSaveChanged(boolean z) {
                StatusBar statusBar = StatusBar.this;
                statusBar.mHandler.post(statusBar.mCheckBarModes);
                DozeServiceHost dozeServiceHost = StatusBar.this.mDozeServiceHost;
                if (dozeServiceHost != null) {
                    dozeServiceHost.firePowerSaveChanged(z);
                }
            }
        });
        this.mAutoHideController = (AutoHideController) Dependency.get(AutoHideController.class);
        this.mAutoHideController.setStatusBar(this);
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        this.mScrimController = SystemUIFactory.getInstance().createScrimController((ScrimView) this.mStatusBarWindow.findViewById(C0007R$id.scrim_behind), (ScrimView) this.mStatusBarWindow.findViewById(C0007R$id.scrim_in_front), this.mLockscreenWallpaper, new TriConsumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$yOhqIPn374xbhtcciPbikc06Y7E */

            public final void accept(Object obj, Object obj2, Object obj3) {
                StatusBar.this.lambda$makeStatusBarView$5$StatusBar((ScrimState) obj, (Float) obj2, (ColorExtractor.GradientColors) obj3);
            }
        }, new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$D6uZssDIjl3zb9PActa_b2Y0wNo */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                StatusBar.this.lambda$makeStatusBarView$6$StatusBar((Integer) obj);
            }
        }, DozeParameters.getInstance(this.mContext), (AlarmManager) this.mContext.getSystemService(AlarmManager.class));
        this.mNotificationPanel.initDependencies(this, this.mGroupManager, this.mNotificationShelf, this.mHeadsUpManager, this.mNotificationIconAreaController, this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(DozeParameters.getInstance(context));
        BackDropView backDropView = (BackDropView) this.mStatusBarWindow.findViewById(C0007R$id.backdrop);
        this.mMediaManager.setup(backDropView, (ImageView) backDropView.findViewById(C0007R$id.backdrop_front), (ImageView) backDropView.findViewById(C0007R$id.backdrop_back), this.mScrimController, this.mLockscreenWallpaper, this.mAodView);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        this.mNotificationPanel.setUserSetupComplete(this.mUserSetup);
        if (UserManager.get(this.mContext).isUserSwitcherEnabled()) {
            createUserSwitcher();
        }
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        Objects.requireNonNull(statusBarWindowView);
        notificationPanelView.setLaunchAffordanceListener(new Consumer() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$HshNPAFauaSwgr5N8iT9CKLXoqs */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                StatusBarWindowView.this.onShowingLaunchAffordanceChanged(((Boolean) obj).booleanValue());
            }
        });
        View findViewById = this.mStatusBarWindow.findViewById(C0007R$id.qs_frame);
        if (findViewById != null) {
            FragmentHostManager fragmentHostManager2 = FragmentHostManager.get(findViewById);
            int i = C0007R$id.qs_frame;
            ExtensionController.ExtensionBuilder newExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(QS.class);
            newExtension.withPlugin(QS.class);
            newExtension.withDefault(new Supplier() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$Zqmz5npIKuMPJHZWVxICwxzCPwk */

                @Override // java.util.function.Supplier
                public final Object get() {
                    return StatusBar.this.createDefaultQSFragment();
                }
            });
            ExtensionFragmentListener.attachExtensonToFragment(findViewById, QS.TAG, i, newExtension.build());
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow, new Consumer() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$0m7F6e2QtJDG3hy0Y3EVPv_U6WQ */

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    StatusBar.this.lambda$makeStatusBarView$7$StatusBar((Boolean) obj);
                }
            });
            fragmentHostManager2.addTagListener(QS.TAG, new FragmentHostManager.FragmentListener() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$PK92anhRWLDXkprajoojY6dzepA */

                @Override // com.android.systemui.fragments.FragmentHostManager.FragmentListener
                public final void onFragmentViewCreated(String str, Fragment fragment) {
                    StatusBar.this.lambda$makeStatusBarView$8$StatusBar(str, fragment);
                }
            });
        }
        this.mReportRejectedTouch = this.mStatusBarWindow.findViewById(C0007R$id.report_rejected_touch);
        if (this.mReportRejectedTouch != null) {
            updateReportRejectedTouchVisibility();
            this.mReportRejectedTouch.setOnClickListener(new View.OnClickListener() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$ggtzWYldpP6XbhwYmX0SNphBaak */

                public final void onClick(View view) {
                    StatusBar.this.lambda$makeStatusBarView$9$StatusBar(view);
                }
            });
        }
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (!powerManager.isScreenOn()) {
            this.mBroadcastReceiver.onReceive(this.mContext, new Intent("android.intent.action.SCREEN_OFF"));
        }
        this.mGestureWakeLock = powerManager.newWakeLock(10, "GestureWakeLock");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        int[] intArray = this.mContext.getResources().getIntArray(C0001R$array.config_cameraLaunchGestureVibePattern);
        this.mCameraLaunchGestureVibePattern = new long[intArray.length];
        for (int i2 = 0; i2 < intArray.length; i2++) {
            this.mCameraLaunchGestureVibePattern[i2] = (long) intArray[i2];
        }
        int[] intArray2 = this.mContext.getResources().getIntArray(17235997);
        this.mCameraLiftTriggerVibePattern = new long[intArray2.length];
        for (int i3 = 0; i3 < intArray2.length; i3++) {
            this.mCameraLiftTriggerVibePattern[i3] = (long) intArray2[i3];
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, intentFilter2, "android.permission.DUMP", null);
        this.mDeviceProvisionedController.addCallback(this.mUserSetupObserver);
        this.mUserSetupObserver.onUserSetupChanged();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
    }

    public /* synthetic */ void lambda$makeStatusBarView$4$StatusBar(String str, Fragment fragment) {
        ((CollapsedStatusBarFragment) fragment).initNotificationIconArea(this.mNotificationIconAreaController);
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        this.mStatusBarView = (PhoneStatusBarView) fragment.getView();
        this.mStatusBarView.setBar(this);
        this.mStatusBarView.setPanel(this.mNotificationPanel);
        this.mStatusBarView.setScrimController(this.mScrimController);
        if (this.mHeadsUpManager.hasPinnedHeadsUp()) {
            this.mNotificationPanel.notifyBarPanelExpansionChanged();
        }
        this.mStatusBarView.setBouncerShowing(this.mBouncerShowing);
        if (phoneStatusBarView != null) {
            this.mStatusBarView.panelExpansionChanged(phoneStatusBarView.getExpansionFraction(), phoneStatusBarView.isExpanded());
        }
        HeadsUpAppearanceController headsUpAppearanceController = this.mHeadsUpAppearanceController;
        if (headsUpAppearanceController != null) {
            headsUpAppearanceController.destroy();
        }
        this.mHeadsUpAppearanceController = new HeadsUpAppearanceController(this.mNotificationIconAreaController, this.mHeadsUpManager, this.mStatusBarWindow);
        this.mHeadsUpAppearanceController.readFrom(headsUpAppearanceController);
        this.mStatusBarWindow.setStatusBarView(this.mStatusBarView);
        updateAreThereNotifications();
        checkBarModes();
        SkinningBridge.onStatusBarCreated(this.mStatusBarView);
    }

    public /* synthetic */ void lambda$makeStatusBarView$5$StatusBar(ScrimState scrimState, Float f, ColorExtractor.GradientColors gradientColors) {
        this.mLightBarController.setScrimState(scrimState, f.floatValue(), gradientColors);
    }

    public /* synthetic */ void lambda$makeStatusBarView$6$StatusBar(Integer num) {
        StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
        if (statusBarWindowController != null) {
            statusBarWindowController.setScrimsVisibility(num.intValue());
        }
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        if (statusBarWindowView != null) {
            statusBarWindowView.onScrimVisibilityChanged(num.intValue());
        }
    }

    public /* synthetic */ void lambda$makeStatusBarView$7$StatusBar(Boolean bool) {
        this.mBrightnessMirrorVisible = bool.booleanValue();
        updateScrimController();
    }

    public /* synthetic */ void lambda$makeStatusBarView$8$StatusBar(String str, Fragment fragment) {
        QS qs = (QS) fragment;
        if (qs instanceof QSFragment) {
            this.mQSPanel = ((QSFragment) qs).getQsPanel();
            this.mQSPanel.setBrightnessMirror(this.mBrightnessMirrorController);
        }
    }

    public /* synthetic */ void lambda$makeStatusBarView$9$StatusBar(View view) {
        Uri reportRejectedTouch = this.mFalsingManager.reportRejectedTouch();
        if (reportRejectedTouch != null) {
            StringWriter stringWriter = new StringWriter();
            stringWriter.write("Build info: ");
            stringWriter.write(SystemProperties.get("ro.build.description"));
            stringWriter.write("\nSerial number: ");
            stringWriter.write(SystemProperties.get("ro.serialno"));
            stringWriter.write("\n");
            PrintWriter printWriter = new PrintWriter(stringWriter);
            FalsingLog.dump(printWriter);
            printWriter.flush();
            startActivityDismissingKeyguard(Intent.createChooser(new Intent("android.intent.action.SEND").setType("*/*").putExtra("android.intent.extra.SUBJECT", "Rejected touch report").putExtra("android.intent.extra.STREAM", reportRejectedTouch).putExtra("android.intent.extra.TEXT", stringWriter.toString()), "Share rejected touch report").addFlags(268435456), true, true);
        }
    }

    /* access modifiers changed from: protected */
    public QS createDefaultQSFragment() {
        return (QS) FragmentHostManager.get(this.mStatusBarWindow).create(QSFragment.class);
    }

    private void setUpPresenter() {
        this.mActivityLaunchAnimator = new ActivityLaunchAnimator(this.mStatusBarWindow, this, this.mNotificationPanel, (NotificationListContainer) this.mStackScroller);
        NotificationRowBinderImpl notificationRowBinderImpl = new NotificationRowBinderImpl(this.mContext, SystemUIFactory.getInstance().provideAllowNotificationLongPress());
        this.mPresenter = new StatusBarNotificationPresenter(this.mContext, this.mNotificationPanel, this.mHeadsUpManager, this.mStatusBarWindow, this.mStackScroller, this.mDozeScrimController, this.mScrimController, this.mActivityLaunchAnimator, this.mStatusBarKeyguardViewManager, this.mNotificationAlertingManager, notificationRowBinderImpl, this.mKeyguardIndicationController);
        this.mNotificationListController = new NotificationListController(this.mEntryManager, (NotificationListContainer) this.mStackScroller, this.mForegroundServiceController, this.mDeviceProvisionedController);
        this.mAppOpsController.addCallback(APP_OPS, this);
        this.mNotificationShelf.setOnActivatedListener(this.mPresenter);
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarWindowController);
        StatusBarRemoteInputCallback statusBarRemoteInputCallback = (StatusBarRemoteInputCallback) Dependency.get(NotificationRemoteInputManager.Callback.class);
        this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        Context context = this.mContext;
        this.mNotificationActivityStarter = new StatusBarNotificationActivityStarter(context, this.mCommandQueue, this.mAssistManager, this.mNotificationPanel, this.mPresenter, this.mEntryManager, this.mHeadsUpManager, (ActivityStarter) Dependency.get(ActivityStarter.class), this.mActivityLaunchAnimator, this.mBarService, this.mStatusBarStateController, this.mKeyguardManager, this.mDreamManager, this.mRemoteInputManager, statusBarRemoteInputCallback, this.mGroupManager, this.mLockscreenUserManager, this.mShadeController, this.mKeyguardMonitor, this.mNotificationInterruptionStateProvider, this.mMetricsLogger, new LockPatternUtils(context), (Handler) Dependency.get(Dependency.MAIN_HANDLER), (Handler) Dependency.get(Dependency.BG_HANDLER), this.mActivityIntentHelper, this.mBubbleController);
        this.mGutsManager.setNotificationActivityStarter(this.mNotificationActivityStarter);
        this.mEntryManager.setRowBinder(notificationRowBinderImpl);
        notificationRowBinderImpl.setNotificationClicker(new NotificationClicker(this, (BubbleController) Dependency.get(BubbleController.class), this.mNotificationActivityStarter));
        this.mGroupAlertTransferHelper.bind(this.mEntryManager, this.mGroupManager);
        this.mNotificationListController.bind();
    }

    /* access modifiers changed from: protected */
    /* renamed from: setUpDisableFlags */
    public void lambda$start$3$StatusBar(int i, int i2) {
        this.mCommandQueue.disable(this.mDisplayId, i, i2, false);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void addAfterKeyguardGoneRunnable(Runnable runnable) {
        this.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public boolean isDozing() {
        return this.mDozing;
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void wakeUpIfDozing(long j, View view, String str) {
        if (this.mDozing) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(j, 4, "com.android.systemui:" + str);
            this.mWakeUpComingFromTouch = true;
            view.getLocationInWindow(this.mTmpInt2);
            this.mWakeUpTouchLocation = new PointF((float) (this.mTmpInt2[0] + (view.getWidth() / 2)), (float) (this.mTmpInt2[1] + (view.getHeight() / 2)));
            this.mFalsingManager.onScreenOnFromTouch();
        }
    }

    /* access modifiers changed from: protected */
    public void createNavigationBar(RegisterStatusBarResult registerStatusBarResult) {
        this.mNavigationBarController.createNavigationBars(true, registerStatusBarResult);
    }

    /* access modifiers changed from: protected */
    public View.OnTouchListener getStatusBarWindowTouchListener() {
        return new View.OnTouchListener() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$n71p2lA3I37oyoKRz8xFfo1UnRo */

            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return StatusBar.this.lambda$getStatusBarWindowTouchListener$10$StatusBar(view, motionEvent);
            }
        };
    }

    public /* synthetic */ boolean lambda$getStatusBarWindowTouchListener$10$StatusBar(View view, MotionEvent motionEvent) {
        this.mAutoHideController.checkUserAutoHide(motionEvent);
        this.mRemoteInputManager.checkRemoteInputOutside(motionEvent);
        if (motionEvent.getAction() == 0 && this.mExpandedVisible) {
            animateCollapsePanels();
        }
        return this.mStatusBarWindow.onTouchEvent(motionEvent);
    }

    private void inflateShelf() {
        this.mNotificationShelf = (NotificationShelf) LayoutInflater.from(this.mContext).inflate(C0010R$layout.status_bar_notification_shelf, this.mStackScroller, false);
        this.mNotificationShelf.setOnClickListener(this.mGoToLockedShadeListener);
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onDensityOrFontScaleChanged();
        }
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
        ((UserSwitcherController) Dependency.get(UserSwitcherController.class)).onDensityOrFontScaleChanged();
        KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
        if (keyguardUserSwitcher != null) {
            keyguardUserSwitcher.onDensityOrFontScaleChanged();
        }
        this.mNotificationIconAreaController.onDensityOrFontScaleChanged(this.mContext);
        this.mHeadsUpManager.onDensityOrFontScaleChanged();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            statusBarKeyguardViewManager.onThemeChanged();
        }
        View view = this.mAmbientIndicationContainer;
        if (view instanceof AutoReinflateContainer) {
            ((AutoReinflateContainer) view).inflateLayout();
        }
        ((LockscreenLoopsController) Dependency.get(LockscreenLoopsController.class)).onThemeChanged();
        ((LockscreenAssistIconController) Dependency.get(LockscreenAssistIconController.class)).onThemeChanged(this.mNotificationPanel);
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onOverlayChanged();
        }
        this.mNotificationPanel.onThemeChanged();
        onThemeChanged();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onUiModeChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onUiModeChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void createUserSwitcher() {
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(C0007R$id.keyguard_user_switcher), (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(C0007R$id.keyguard_header), this.mNotificationPanel);
    }

    /* access modifiers changed from: protected */
    public void inflateStatusBarWindow(Context context) {
        this.mStatusBarWindow = (StatusBarWindowView) this.mInjectionInflater.injectable(LayoutInflater.from(context)).inflate(C0010R$layout.super_status_bar, (ViewGroup) null);
    }

    /* access modifiers changed from: protected */
    public void startKeyguard() {
        Trace.beginSection("StatusBar#startKeyguard");
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        Context context = this.mContext;
        this.mBiometricUnlockController = new BiometricUnlockController(context, this.mDozeScrimController, keyguardViewMediator, this.mScrimController, this, UnlockMethodCache.getInstance(context), new Handler(), this.mKeyguardUpdateMonitor, (TunerService) Dependency.get(TunerService.class));
        putComponent(BiometricUnlockController.class, this.mBiometricUnlockController);
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, getBouncerContainer(), this.mNotificationPanel, this.mBiometricUnlockController, (ViewGroup) this.mStatusBarWindow.findViewById(C0007R$id.lock_icon_container));
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mBiometricUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarKeyguardViewManager);
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mLightBarController.setBiometricUnlockController(this.mBiometricUnlockController);
        this.mMediaManager.setBiometricUnlockController(this.mBiometricUnlockController);
        ((KeyguardDismissUtil) Dependency.get(KeyguardDismissUtil.class)).setDismissHandler(new KeyguardDismissHandler() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$rvCsIQdHonxFrWe7zDPDF5dVrCc */

            @Override // com.android.systemui.statusbar.phone.KeyguardDismissHandler
            public final void executeWhenUnlocked(ActivityStarter.OnDismissAction onDismissAction) {
                StatusBar.this.executeWhenUnlocked(onDismissAction);
            }
        });
        this.mIsSkinningEnabled = this.mContext.getResources().getBoolean(C0003R$bool.somc_keyguard_theme_enabled);
        ((LockscreenAmbientDisplayController) Dependency.get(LockscreenAmbientDisplayController.class)).initObserver();
        ((LockscreenLoopsController) Dependency.get(LockscreenLoopsController.class)).init(this.mNotificationPanel, this.mAodView);
        ((LockscreenAssistIconController) Dependency.get(LockscreenAssistIconController.class)).init(this.mNotificationPanel, this.mKeyguardIndicationController);
        ((LockscreenStyleCoverController) Dependency.get(LockscreenStyleCoverController.class)).init(this.mScrimController, this.mStatusBarKeyguardViewManager);
        Trace.endSection();
    }

    /* access modifiers changed from: protected */
    public View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    /* access modifiers changed from: protected */
    public ViewGroup getBouncerContainer() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(17105427);
        }
        return this.mNaturalBarHeight;
    }

    /* access modifiers changed from: protected */
    public boolean toggleSplitScreenMode(int i, int i2) {
        int i3 = 0;
        if (this.mRecents == null) {
            return false;
        }
        if (WindowManagerProxy.getInstance().getDockSide() == -1) {
            int navBarPosition = WindowManagerWrapper.getInstance().getNavBarPosition(this.mDisplayId);
            if (navBarPosition == -1) {
                return false;
            }
            if (navBarPosition == 1) {
                i3 = 1;
            }
            return this.mRecents.splitPrimaryTask(i3, null, i);
        }
        Divider divider = (Divider) getComponent(Divider.class);
        if (divider != null) {
            if (divider.isMinimized() && !divider.isHomeStackResizable()) {
                return false;
            }
            divider.onUndockingTask();
            if (i2 != -1) {
                this.mMetricsLogger.action(i2);
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0026, code lost:
        if (com.android.systemui.statusbar.phone.StatusBar.ONLY_CORE_APPS == false) goto L_0x002a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateQsExpansionEnabled() {
        /*
            r3 = this;
            com.android.systemui.statusbar.policy.DeviceProvisionedController r0 = r3.mDeviceProvisionedController
            boolean r0 = r0.isDeviceProvisioned()
            r1 = 1
            if (r0 == 0) goto L_0x0029
            boolean r0 = r3.mUserSetup
            if (r0 != 0) goto L_0x0017
            com.android.systemui.statusbar.policy.UserSwitcherController r0 = r3.mUserSwitcherController
            if (r0 == 0) goto L_0x0017
            boolean r0 = r0.isSimpleUserSwitcher()
            if (r0 != 0) goto L_0x0029
        L_0x0017:
            int r0 = r3.mDisabled2
            r2 = r0 & 4
            if (r2 != 0) goto L_0x0029
            r0 = r0 & r1
            if (r0 != 0) goto L_0x0029
            boolean r0 = r3.mDozing
            if (r0 != 0) goto L_0x0029
            boolean r0 = com.android.systemui.statusbar.phone.StatusBar.ONLY_CORE_APPS
            if (r0 != 0) goto L_0x0029
            goto L_0x002a
        L_0x0029:
            r1 = 0
        L_0x002a:
            com.android.systemui.statusbar.phone.NotificationPanelView r3 = r3.mNotificationPanel
            r3.setQsExpansionEnabled(r1)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r0 = "updateQsExpansionEnabled - QS Expand enabled: "
            r3.append(r0)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            java.lang.String r0 = "StatusBar"
            android.util.Log.d(r0, r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBar.updateQsExpansionEnabled():void");
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void addQsTile(ComponentName componentName) {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null && qSPanel.getHost() != null) {
            this.mQSPanel.getHost().addTile(componentName);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void remQsTile(ComponentName componentName) {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null && qSPanel.getHost() != null) {
            this.mQSPanel.getHost().removeTile(componentName);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void clickTile(ComponentName componentName) {
        this.mQSPanel.clickTile(componentName);
    }

    public boolean areNotificationsHidden() {
        return this.mZenController.areNotificationsHiddenInShade();
    }

    public void requestNotificationUpdate() {
        this.mEntryManager.updateNotifications();
    }

    public void requestFaceAuth() {
        if (!this.mUnlockMethodCache.canSkipBouncer()) {
            this.mKeyguardUpdateMonitor.requestFaceAuth();
        }
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void updateAreThereNotifications() {
        AnonymousClass6 r0;
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            final View findViewById = phoneStatusBarView.findViewById(C0007R$id.notification_lights_out);
            boolean z = true;
            boolean z2 = hasActiveNotifications() && !areLightsOn();
            if (findViewById.getAlpha() != 1.0f) {
                z = false;
            }
            if (z2 != z) {
                float f = 0.0f;
                if (z2) {
                    findViewById.setAlpha(0.0f);
                    findViewById.setVisibility(0);
                }
                ViewPropertyAnimator animate = findViewById.animate();
                if (z2) {
                    f = 1.0f;
                }
                ViewPropertyAnimator interpolator = animate.alpha(f).setDuration(z2 ? 750 : 250).setInterpolator(new AccelerateInterpolator(2.0f));
                if (z2) {
                    r0 = null;
                } else {
                    r0 = new AnimatorListenerAdapter() {
                        /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass6 */

                        public void onAnimationEnd(Animator animator) {
                            findViewById.setVisibility(8);
                        }
                    };
                }
                interpolator.setListener(r0).start();
            }
        }
        this.mMediaManager.findAndUpdateMediaNotifications();
        AodView aodView = this.mAodView;
        if (aodView != null) {
            aodView.onUpdateNotifications();
        }
    }

    private void updateReportRejectedTouchVisibility() {
        View view = this.mReportRejectedTouch;
        if (view != null) {
            view.setVisibility((this.mState != 1 || this.mDozing || !this.mFalsingManager.isReportingEnabled()) ? 4 : 0);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void disable(int i, int i2, int i3, boolean z) {
        if (i == this.mDisplayId) {
            int adjustDisableFlags = this.mRemoteInputQuickSettingsDisabler.adjustDisableFlags(i3);
            int i4 = this.mStatusBarWindowState;
            int i5 = this.mDisabled1 ^ i2;
            this.mDisabled1 = i2;
            int i6 = this.mDisabled2 ^ adjustDisableFlags;
            this.mDisabled2 = adjustDisableFlags;
            StringBuilder sb = new StringBuilder();
            sb.append("disable<");
            int i7 = i2 & 65536;
            sb.append(i7 != 0 ? 'E' : 'e');
            int i8 = 65536 & i5;
            char c = ' ';
            sb.append(i8 != 0 ? '!' : ' ');
            char c2 = 'I';
            sb.append((i2 & 131072) != 0 ? 'I' : 'i');
            sb.append((131072 & i5) != 0 ? '!' : ' ');
            int i9 = i2 & 262144;
            sb.append(i9 != 0 ? 'A' : 'a');
            int i10 = 262144 & i5;
            sb.append(i10 != 0 ? '!' : ' ');
            sb.append((i2 & 1048576) != 0 ? 'S' : 's');
            sb.append((1048576 & i5) != 0 ? '!' : ' ');
            sb.append((i2 & 4194304) != 0 ? 'B' : 'b');
            sb.append((4194304 & i5) != 0 ? '!' : ' ');
            sb.append((i2 & 2097152) != 0 ? 'H' : 'h');
            sb.append((2097152 & i5) != 0 ? '!' : ' ');
            int i11 = 16777216 & i2;
            sb.append(i11 != 0 ? 'R' : 'r');
            int i12 = 16777216 & i5;
            sb.append(i12 != 0 ? '!' : ' ');
            sb.append((8388608 & i2) != 0 ? 'C' : 'c');
            sb.append((8388608 & i5) != 0 ? '!' : ' ');
            sb.append((33554432 & i2) != 0 ? 'S' : 's');
            sb.append((i5 & 33554432) != 0 ? '!' : ' ');
            sb.append("> disable2<");
            sb.append((adjustDisableFlags & 1) != 0 ? 'Q' : 'q');
            int i13 = i6 & 1;
            sb.append(i13 != 0 ? '!' : ' ');
            if ((adjustDisableFlags & 2) == 0) {
                c2 = 'i';
            }
            sb.append(c2);
            sb.append((i6 & 2) != 0 ? '!' : ' ');
            sb.append((adjustDisableFlags & 4) != 0 ? 'N' : 'n');
            int i14 = i6 & 4;
            if (i14 != 0) {
                c = '!';
            }
            sb.append(c);
            sb.append('>');
            Log.d("StatusBar", sb.toString());
            if (!(i8 == 0 || i7 == 0)) {
                animateCollapsePanels();
            }
            if (!(i12 == 0 || i11 == 0)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            if (i10 != 0) {
                this.mNotificationInterruptionStateProvider.setDisableNotificationAlerts(i9 != 0);
            }
            if (i13 != 0) {
                updateQsExpansionEnabled();
            }
            if (i14 != 0) {
                updateQsExpansionEnabled();
                if ((i2 & 4) != 0) {
                    animateCollapsePanels();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public H createHandler() {
        return new H();
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, i);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, false, z);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, z, false, callback, 0);
    }

    public void setQsExpanded(boolean z) {
        this.mStatusBarWindowController.setQsExpanded(z);
        this.mNotificationPanel.setStatusAccessibilityImportance(z ? 4 : 0);
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return this.mStatusBarStateController.getState() == 1;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
    public void onUnlockMethodStateChanged() {
        updateKeyguardState();
        logStateToEventlog();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinnedModeChanged(boolean z) {
        if (z) {
            this.mStatusBarWindowController.setHeadsUpShowing(true);
            this.mStatusBarWindowController.setForceStatusBarVisible(true);
            if (this.mNotificationPanel.isFullyCollapsed()) {
                this.mNotificationPanel.requestLayout();
                this.mStatusBarWindowController.setForceWindowCollapsed(true);
                this.mNotificationPanel.post(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$a1PwGueSv8bkjX5GxiVzM2PDffE */

                    public final void run() {
                        StatusBar.this.lambda$onHeadsUpPinnedModeChanged$11$StatusBar();
                    }
                });
            }
        } else if (!this.mNotificationPanel.isFullyCollapsed() || this.mNotificationPanel.isTracking()) {
            this.mStatusBarWindowController.setHeadsUpShowing(false);
        } else {
            this.mHeadsUpManager.setHeadsUpGoingAway(true);
            this.mNotificationPanel.runAfterAnimationFinished(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$vQbe7Nr2PT8R2UTHbkZ0b3R4w */

                public final void run() {
                    StatusBar.this.lambda$onHeadsUpPinnedModeChanged$12$StatusBar();
                }
            });
        }
    }

    public /* synthetic */ void lambda$onHeadsUpPinnedModeChanged$11$StatusBar() {
        this.mStatusBarWindowController.setForceWindowCollapsed(false);
    }

    public /* synthetic */ void lambda$onHeadsUpPinnedModeChanged$12$StatusBar() {
        if (!this.mHeadsUpManager.hasPinnedHeadsUp()) {
            this.mStatusBarWindowController.setHeadsUpShowing(false);
            this.mHeadsUpManager.setHeadsUpGoingAway(false);
        }
        this.mRemoteInputManager.onPanelCollapsed();
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
        dismissVolumeDialog();
        ((NotificationListContainer) this.mStackScroller).setHeadsUpEntry(notificationEntry, true);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
        ((NotificationListContainer) this.mStackScroller).setHeadsUpEntry(notificationEntry, false);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mEntryManager.updateNotifications();
    }

    @Override // com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener
    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mEntryManager.updateNotifications();
        if (z) {
            this.mDozeServiceHost.fireNotificationPulse();
        } else if (!this.mAmbientPulseManager.hasNotifications()) {
            this.mDozeScrimController.pulseOutNow();
        }
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean z) {
        this.mPanelExpanded = z;
        updateHideIconsForBouncer(false);
        this.mStatusBarWindowController.setPanelExpanded(z);
        this.mVisualStabilityManager.setPanelExpanded(z);
        if (z && this.mStatusBarStateController.getState() != 1) {
            clearNotificationEffects();
        }
        if (!z) {
            this.mRemoteInputManager.onPanelCollapsed();
        }
    }

    public ViewGroup getNotificationScrollLayout() {
        return this.mStackScroller;
    }

    public boolean isPulsing() {
        return this.mPulsing;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        return this.mNotificationPanel.hideStatusBarIconsWhenExpanded();
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        updateTheme();
    }

    public View getAmbientIndicationContainer() {
        return this.mAmbientIndicationContainer;
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public boolean isOccluded() {
        return this.mIsOccluded;
    }

    public void setOccluded(boolean z) {
        this.mIsOccluded = z;
        this.mScrimController.setKeyguardOccluded(z);
        updateHideIconsForBouncer(false);
    }

    public boolean hideStatusBarIconsForBouncer() {
        return this.mHideIconsForBouncer || this.mWereIconsJustHidden;
    }

    private void updateHideIconsForBouncer(boolean z) {
        boolean z2 = false;
        boolean z3 = this.mTopHidesStatusBar && this.mIsOccluded && (this.mStatusBarWindowHidden || this.mBouncerShowing);
        boolean z4 = !this.mPanelExpanded && !this.mIsOccluded && this.mBouncerShowing;
        if (z3 || z4) {
            z2 = true;
        }
        if (this.mHideIconsForBouncer != z2) {
            this.mHideIconsForBouncer = z2;
            if (z2 || !this.mBouncerWasShowingWhenHidden) {
                this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, z);
            } else {
                this.mWereIconsJustHidden = true;
                this.mHandler.postDelayed(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$a1IsrkRZhqgkId0jst0xYX6PoT4 */

                    public final void run() {
                        StatusBar.this.lambda$updateHideIconsForBouncer$13$StatusBar();
                    }
                }, 500);
            }
        }
        if (z2) {
            this.mBouncerWasShowingWhenHidden = this.mBouncerShowing;
        }
    }

    public /* synthetic */ void lambda$updateHideIconsForBouncer$13$StatusBar() {
        this.mWereIconsJustHidden = false;
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
    }

    public boolean isHeadsUpShouldBeVisible() {
        return this.mHeadsUpAppearanceController.shouldBeVisible();
    }

    @Override // com.android.systemui.statusbar.notification.ActivityLaunchAnimator.Callback
    public void onLaunchAnimationCancelled() {
        if (!this.mPresenter.isCollapsing()) {
            onClosingFinished();
        }
    }

    @Override // com.android.systemui.statusbar.notification.ActivityLaunchAnimator.Callback
    public void onExpandAnimationFinished(boolean z) {
        if (!this.mPresenter.isCollapsing()) {
            onClosingFinished();
        }
        if (z) {
            instantCollapseNotificationPanel();
        }
    }

    @Override // com.android.systemui.statusbar.notification.ActivityLaunchAnimator.Callback
    public void onExpandAnimationTimedOut() {
        ActivityLaunchAnimator activityLaunchAnimator;
        if (!this.mPresenter.isPresenterFullyCollapsed() || this.mPresenter.isCollapsing() || (activityLaunchAnimator = this.mActivityLaunchAnimator) == null || activityLaunchAnimator.isLaunchForActivity()) {
            collapsePanel(true);
        } else {
            onClosingFinished();
        }
    }

    @Override // com.android.systemui.statusbar.notification.ActivityLaunchAnimator.Callback
    public boolean areLaunchAnimationsEnabled() {
        return this.mState == 0;
    }

    public boolean isDeviceInVrMode() {
        return this.mPresenter.isDeviceInVrMode();
    }

    /* access modifiers changed from: protected */
    public class H extends Handler {
        protected H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1026) {
                StatusBar.this.toggleKeyboardShortcuts(message.arg1);
            } else if (i != 1027) {
                switch (i) {
                    case 1000:
                        StatusBar.this.animateExpandNotificationsPanel();
                        return;
                    case 1001:
                        StatusBar.this.animateCollapsePanels();
                        return;
                    case 1002:
                        StatusBar.this.animateExpandSettingsPanel((String) message.obj);
                        return;
                    case 1003:
                        StatusBar.this.onLaunchTransitionTimeout();
                        return;
                    default:
                        return;
                }
            } else {
                StatusBar.this.dismissKeyboardShortcuts();
            }
        }
    }

    public void maybeEscalateHeadsUp() {
        this.mHeadsUpManager.getAllEntries().forEach($$Lambda$StatusBar$Qz8oyL0qAMzuJuwPLHs4cVCa7kg.INSTANCE);
        this.mHeadsUpManager.releaseAllImmediately();
    }

    static /* synthetic */ void lambda$maybeEscalateHeadsUp$14(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        Notification notification = statusBarNotification.getNotification();
        if (notification.fullScreenIntent != null) {
            try {
                EventLog.writeEvent(36003, statusBarNotification.getKey());
                notification.fullScreenIntent.send();
                notificationEntry.notifyFullScreenIntentLaunched();
            } catch (PendingIntent.CanceledException unused) {
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void handleSystemKey(int i) {
        if (this.mCommandQueue.panelsEnabled() && this.mKeyguardMonitor.isDeviceInteractive()) {
            if ((this.mKeyguardMonitor.isShowing() && !this.mKeyguardMonitor.isOccluded()) || !this.mUserSetup) {
                return;
            }
            if (280 == i) {
                this.mMetricsLogger.action(493);
                this.mNotificationPanel.collapse(false, 1.0f);
            } else if (281 == i) {
                this.mMetricsLogger.action(494);
                if (this.mNotificationPanel.isFullyCollapsed()) {
                    if (this.mVibrateOnOpening) {
                        this.mVibratorHelper.vibrate(2);
                    }
                    this.mNotificationPanel.expand(true);
                    this.mMetricsLogger.count("panel_open", 1);
                } else if (!this.mNotificationPanel.isInSettings() && !this.mNotificationPanel.isExpanding()) {
                    this.mNotificationPanel.flingSettings(0.0f, 0);
                    this.mMetricsLogger.count("panel_open_qs", 1);
                }
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showPinningEnterExitToast(boolean z) {
        if (getNavigationBarView() != null) {
            getNavigationBarView().showPinningEnterExitToast(z);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showPinningEscapeToast() {
        if (getNavigationBarView() != null) {
            getNavigationBarView().showPinningEscapeToast();
        }
    }

    /* access modifiers changed from: package-private */
    public void makeExpandedVisible(boolean z) {
        if (z || (!this.mExpandedVisible && this.mCommandQueue.panelsEnabled())) {
            this.mExpandedVisible = true;
            this.mStatusBarWindowController.setPanelVisible(true);
            visibilityChanged(true);
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, !z);
            setInteracting(1, true);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void postAnimateForceCollapsePanels() {
        this.mHandler.post(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$vsXwLw7AvX4yDOof5dgbuWdLbIs */

            public final void run() {
                StatusBar.this.lambda$postAnimateForceCollapsePanels$15$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$postAnimateForceCollapsePanels$15$StatusBar() {
        animateCollapsePanels(0, true);
    }

    public void postAnimateOpenPanels() {
        this.mHandler.sendEmptyMessage(1002);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void togglePanel() {
        if (this.mPanelExpanded) {
            animateCollapsePanels();
        } else {
            animateExpandNotificationsPanel();
        }
    }

    public void animateCollapsePanels(int i) {
        animateCollapsePanels(i, false, false, 1.0f);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController, com.android.systemui.statusbar.CommandQueue.Callbacks
    public void animateCollapsePanels(int i, boolean z) {
        animateCollapsePanels(i, z, false, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2) {
        animateCollapsePanels(i, z, z2, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2, float f) {
        if (z || this.mState == 0) {
            if ((i & 2) == 0 && !this.mHandler.hasMessages(1020)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            Log.v("StatusBar", "mStatusBarWindow: " + this.mStatusBarWindow + " canPanelBeCollapsed(): " + this.mNotificationPanel.canPanelBeCollapsed());
            if (this.mStatusBarWindow == null || !this.mNotificationPanel.canPanelBeCollapsed()) {
                this.mBubbleController.collapseStack();
                return;
            }
            this.mStatusBarWindowController.setStatusBarFocusable(false);
            this.mStatusBarWindow.cancelExpandHelper();
            if (this.mLaunchingCamera) {
                this.mStatusBarView.collapsePanel(false, z2, f);
                Log.v("StatusBar", "Collapse notification panel without animation");
            } else {
                this.mStatusBarView.collapsePanel(true, z2, f);
            }
            this.mLaunchingCamera = false;
            return;
        }
        runPostCollapseRunnables();
    }

    /* access modifiers changed from: private */
    public void runPostCollapseRunnables() {
        ArrayList arrayList = new ArrayList(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ((Runnable) arrayList.get(i)).run();
        }
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void dispatchNotificationsPanelTouchEvent(MotionEvent motionEvent) {
        if (this.mCommandQueue.panelsEnabled()) {
            this.mNotificationPanel.dispatchTouchEvent(motionEvent);
            int action = motionEvent.getAction();
            if (action == 0) {
                this.mStatusBarWindowController.setNotTouchable(true);
            } else if (action == 1 || action == 3) {
                this.mStatusBarWindowController.setNotTouchable(false);
            }
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void animateExpandNotificationsPanel() {
        if (this.mCommandQueue.panelsEnabled()) {
            this.mNotificationPanel.expandWithoutQs();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void animateExpandSettingsPanel(String str) {
        if (this.mCommandQueue.panelsEnabled() && this.mUserSetup) {
            if (str != null) {
                this.mQSPanel.openDetails(str);
            }
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapsePanel(true, false, 1.0f);
        }
    }

    /* access modifiers changed from: package-private */
    public void makeExpandedInvisible() {
        if (this.mExpandedVisible && this.mStatusBarWindow != null) {
            this.mStatusBarView.collapsePanel(false, false, 1.0f);
            this.mNotificationPanel.closeQs();
            this.mExpandedVisible = false;
            visibilityChanged(false);
            this.mStatusBarWindowController.setPanelVisible(false);
            this.mStatusBarWindowController.setForceStatusBarVisible(false);
            this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
            runPostCollapseRunnables();
            setInteracting(1, false);
            if (!this.mNotificationActivityStarter.isCollapsingToShowActivityOverLockscreen()) {
                showBouncerIfKeyguard();
            }
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, this.mNotificationPanel.hideStatusBarIconsWhenExpanded());
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                WindowManagerGlobal.getInstance().trimMemory(20);
            }
        }
    }

    public boolean interceptTouchEvent(MotionEvent motionEvent) {
        if (this.mStatusBarWindowState == 0) {
            if (!(motionEvent.getAction() == 1 || motionEvent.getAction() == 3) || this.mExpandedVisible) {
                setInteracting(1, true);
            } else {
                setInteracting(1, false);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    public BiometricUnlockController getBiometricUnlockController() {
        return this.mBiometricUnlockController;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setWindowState(int i, int i2, int i3) {
        if (i == this.mDisplayId) {
            boolean z = true;
            boolean z2 = i3 == 0;
            if (!(this.mStatusBarWindow == null || i2 != 1 || this.mStatusBarWindowState == i3)) {
                this.mStatusBarWindowState = i3;
                if (!z2 && this.mState == 0) {
                    this.mStatusBarView.collapsePanel(false, false, 1.0f);
                }
                if (this.mStatusBarView != null) {
                    if (i3 != 2) {
                        z = false;
                    }
                    this.mStatusBarWindowHidden = z;
                    updateHideIconsForBouncer(false);
                }
            }
            SkinningBridge.onWindowStateChanged(i2, i3);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        boolean z2;
        int massageSystemUiVisibility = SkinningBridge.massageSystemUiVisibility(i2);
        if (i == this.mDisplayId) {
            int i6 = this.mSystemUiVisibility;
            int i7 = ((~i5) & i6) | (massageSystemUiVisibility & i5);
            int i8 = i7 ^ i6;
            boolean z3 = false;
            if (i8 != 0) {
                this.mSystemUiVisibility = i7;
                if ((i8 & 1) != 0) {
                    updateAreThereNotifications();
                }
                if ((massageSystemUiVisibility & 268435456) != 0) {
                    this.mNoAnimationOnNextBarModeChange = true;
                }
                int computeStatusBarMode = computeStatusBarMode(i6, i7);
                boolean z4 = computeStatusBarMode != -1;
                if (z4 && computeStatusBarMode != this.mStatusBarMode) {
                    this.mStatusBarMode = computeStatusBarMode;
                    checkBarModes();
                    this.mAutoHideController.touchAutoHide();
                }
                SkinningBridge.onSystemUiVisibilityChanged(this.mSystemUiVisibility);
                z2 = z4;
            } else {
                z2 = false;
            }
            int i9 = this.mStatusBarMode;
            if ((i3 & 8192) == 8192) {
                z3 = true;
            }
            SkinningBridge.onStatusBarModeChanged(i9, z3);
            this.mLightBarController.onSystemUiVisibilityChanged(i3, i4, i5, rect, rect2, z2, this.mStatusBarMode, z);
            SkinningBridge.onDockedStackVisibilityChanged(!rect2.isEmpty());
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showWirelessChargingAnimation(int i) {
        if (this.mDozing || this.mKeyguardManager.isKeyguardLocked()) {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, new WirelessChargingAnimation.Callback() {
                /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass7 */

                @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
                public void onAnimationStarting() {
                    CrossFadeHelper.fadeOut(StatusBar.this.mNotificationPanel, 1.0f);
                }

                @Override // com.android.systemui.charging.WirelessChargingAnimation.Callback
                public void onAnimationEnded() {
                    CrossFadeHelper.fadeIn(StatusBar.this.mNotificationPanel);
                }
            }, this.mDozing).show();
        } else {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, null, false).show();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onRecentsAnimationStateChanged(boolean z) {
        setInteracting(2, z);
    }

    /* access modifiers changed from: protected */
    public int computeStatusBarMode(int i, int i2) {
        return computeBarMode(i, i2);
    }

    /* access modifiers changed from: protected */
    public BarTransitions getStatusBarTransitions() {
        return this.mStatusBarView.getBarTransitions();
    }

    /* access modifiers changed from: protected */
    public int computeBarMode(int i, int i2) {
        int barMode = barMode(i);
        int barMode2 = barMode(i2);
        if (barMode == barMode2) {
            return -1;
        }
        return barMode2;
    }

    /* access modifiers changed from: package-private */
    public void checkBarModes() {
        if (!this.mDemoMode) {
            if (this.mStatusBarView != null) {
                checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, getStatusBarTransitions());
            }
            this.mNavigationBarController.checkNavBarModes(this.mDisplayId);
            this.mNoAnimationOnNextBarModeChange = false;
        }
    }

    /* access modifiers changed from: package-private */
    public void setQsScrimEnabled(boolean z) {
        this.mNotificationPanel.setQsScrimEnabled(z);
    }

    /* access modifiers changed from: package-private */
    public void checkBarMode(int i, int i2, BarTransitions barTransitions) {
        barTransitions.transitionTo(i, !this.mNoAnimationOnNextBarModeChange && this.mDeviceInteractive && i2 != 2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void finishBarAnimations() {
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.getBarTransitions().finishAnimations();
        }
        this.mNavigationBarController.finishBarAnimations(this.mDisplayId);
    }

    public void setInteracting(int i, boolean z) {
        int i2;
        boolean z2 = true;
        if (((this.mInteractingWindows & i) != 0) == z) {
            z2 = false;
        }
        if (z) {
            i2 = this.mInteractingWindows | i;
        } else {
            i2 = this.mInteractingWindows & (~i);
        }
        this.mInteractingWindows = i2;
        if (this.mInteractingWindows != 0) {
            this.mAutoHideController.suspendAutoHide();
        } else {
            this.mAutoHideController.resumeSuspendedAutoHide();
        }
        if (z2 && z && i == 2) {
            this.mNavigationBarController.touchAutoDim(this.mDisplayId);
            dismissVolumeDialog();
        }
        checkBarModes();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void dismissVolumeDialog() {
        VolumeComponent volumeComponent = this.mVolumeComponent;
        if (volumeComponent != null) {
            volumeComponent.dismissNow();
        }
    }

    public boolean inFullscreenMode() {
        return (this.mSystemUiVisibility & 6) != 0;
    }

    public boolean inImmersiveMode() {
        return (this.mSystemUiVisibility & 6144) != 0;
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public static String viewInfo(View view) {
        return "[(" + view.getLeft() + "," + view.getTop() + ")(" + view.getRight() + "," + view.getBottom() + ") " + view.getWidth() + "x" + view.getHeight() + "]";
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mQueueLock) {
            printWriter.println("Current Status Bar state:");
            printWriter.println("  mExpandedVisible=" + this.mExpandedVisible);
            printWriter.println("  mDisplayMetrics=" + this.mDisplayMetrics);
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller));
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        printWriter.print("  mInteractingWindows=");
        printWriter.println(this.mInteractingWindows);
        printWriter.print("  mStatusBarWindowState=");
        printWriter.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        printWriter.print("  mStatusBarMode=");
        printWriter.println(BarTransitions.modeToString(this.mStatusBarMode));
        printWriter.print("  mDozing=");
        printWriter.println(this.mDozing);
        printWriter.print("  mZenMode=");
        printWriter.println(Settings.Global.zenModeToString(Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0)));
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            dumpBarTransitions(printWriter, "mStatusBarView", phoneStatusBarView.getBarTransitions());
        }
        printWriter.println("  StatusBarWindowView: ");
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        if (statusBarWindowView != null) {
            statusBarWindowView.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mMediaManager: ");
        NotificationMediaManager notificationMediaManager = this.mMediaManager;
        if (notificationMediaManager != null) {
            notificationMediaManager.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            printWriter.println("    mNotificationPanel=" + this.mNotificationPanel + " params=" + this.mNotificationPanel.getLayoutParams().debug(""));
            printWriter.print("      ");
            this.mNotificationPanel.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mStackScroller: ");
        if (this.mStackScroller instanceof Dumpable) {
            printWriter.print("      ");
            ((Dumpable) this.mStackScroller).dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Theme:");
        String str = this.mUiModeManager == null ? "null" : this.mUiModeManager.getNightMode() + "";
        StringBuilder sb = new StringBuilder();
        sb.append("    dark theme: ");
        sb.append(str);
        sb.append(" (auto: ");
        sb.append(0);
        sb.append(", yes: ");
        sb.append(2);
        sb.append(", no: ");
        boolean z = true;
        sb.append(1);
        sb.append(")");
        printWriter.println(sb.toString());
        if (this.mContext.getThemeResId() != C0015R$style.Theme_SystemUI_Light) {
            z = false;
        }
        printWriter.println("    light wallpaper theme: " + z);
        DozeLog.dump(printWriter);
        BiometricUnlockController biometricUnlockController = this.mBiometricUnlockController;
        if (biometricUnlockController != null) {
            biometricUnlockController.dump(printWriter);
        }
        KeyguardIndicationController keyguardIndicationController = this.mKeyguardIndicationController;
        if (keyguardIndicationController != null) {
            keyguardIndicationController.dump(fileDescriptor, printWriter, strArr);
        }
        ScrimController scrimController = this.mScrimController;
        if (scrimController != null) {
            scrimController.dump(fileDescriptor, printWriter, strArr);
        }
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            statusBarKeyguardViewManager.dump(printWriter);
        }
        synchronized (this.mEntryManager.getNotificationData()) {
            this.mEntryManager.getNotificationData().dump(printWriter, "  ");
        }
        HeadsUpManagerPhone headsUpManagerPhone = this.mHeadsUpManager;
        if (headsUpManagerPhone != null) {
            headsUpManagerPhone.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mHeadsUpManager: null");
        }
        NotificationGroupManager notificationGroupManager = this.mGroupManager;
        if (notificationGroupManager != null) {
            notificationGroupManager.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mGroupManager: null");
        }
        LightBarController lightBarController = this.mLightBarController;
        if (lightBarController != null) {
            lightBarController.dump(fileDescriptor, printWriter, strArr);
        }
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.dump(fileDescriptor, printWriter, strArr);
        }
        FalsingManagerFactory.getInstance(this.mContext).dump(printWriter);
        FalsingLog.dump(printWriter);
        printWriter.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : Prefs.getAll(this.mContext).entrySet()) {
            printWriter.print("  ");
            printWriter.print(entry.getKey());
            printWriter.print("=");
            printWriter.println(entry.getValue());
        }
    }

    static void dumpBarTransitions(PrintWriter printWriter, String str, BarTransitions barTransitions) {
        printWriter.print("  ");
        printWriter.print(str);
        printWriter.print(".BarTransitions.mMode=");
        printWriter.println(BarTransitions.modeToString(barTransitions.getMode()));
    }

    public void createAndAddWindows(RegisterStatusBarResult registerStatusBarResult) {
        makeStatusBarView(registerStatusBarResult);
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mStatusBarWindowController.add(this.mStatusBarWindow, getStatusBarHeight());
    }

    /* access modifiers changed from: package-private */
    public void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    /* access modifiers changed from: package-private */
    public float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    /* access modifiers changed from: package-private */
    public float getDisplayWidth() {
        return (float) this.mDisplayMetrics.widthPixels;
    }

    /* access modifiers changed from: package-private */
    public float getDisplayHeight() {
        return (float) this.mDisplayMetrics.heightPixels;
    }

    /* access modifiers changed from: package-private */
    public int getRotation() {
        return this.mDisplay.getRotation();
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, false, null, i);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2, 0);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2, boolean z3, ActivityStarter.Callback callback, int i) {
        if (!z || this.mDeviceProvisionedController.isDeviceProvisioned()) {
            executeRunnableDismissingKeyguard(new Runnable(intent, i, z3, callback) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$cYI_U_ShQVlsmm6P5qEeF15rkKQ */
                private final /* synthetic */ Intent f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ boolean f$3;
                private final /* synthetic */ ActivityStarter.Callback f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void run() {
                    StatusBar.this.lambda$startActivityDismissingKeyguard$17$StatusBar(this.f$1, this.f$2, this.f$3, this.f$4);
                }
            }, new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$GXuArppP3Gxe5JvIROZsOAy5v74 */

                public final void run() {
                    StatusBar.lambda$startActivityDismissingKeyguard$18(ActivityStarter.Callback.this);
                }
            }, z2, this.mActivityIntentHelper.wouldLaunchResolverActivity(intent, this.mLockscreenUserManager.getCurrentUserId()), true);
        }
    }

    public /* synthetic */ void lambda$startActivityDismissingKeyguard$17$StatusBar(Intent intent, int i, boolean z, ActivityStarter.Callback callback) {
        int i2;
        this.mAssistManager.hideAssist();
        intent.setFlags(335544320);
        intent.addFlags(i);
        ActivityOptions activityOptions = new ActivityOptions(getActivityOptions(null));
        activityOptions.setDisallowEnterPictureInPictureWhileLaunching(z);
        if (intent == KeyguardBottomAreaView.INSECURE_CAMERA_INTENT) {
            activityOptions.setRotationAnimationHint(3);
        }
        if (intent.getAction() == "android.settings.panel.action.VOLUME") {
            activityOptions.setDisallowEnterPictureInPictureWhileLaunching(true);
        }
        try {
            i2 = ActivityTaskManager.getService().startActivityAsUser((IApplicationThread) null, this.mContext.getBasePackageName(), intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), (IBinder) null, (String) null, 0, 268435456, (ProfilerInfo) null, activityOptions.toBundle(), UserHandle.CURRENT.getIdentifier());
        } catch (RemoteException e) {
            Log.w("StatusBar", "Unable to start activity", e);
            i2 = -96;
        }
        if (callback != null) {
            callback.onActivityStarted(i2);
        }
    }

    static /* synthetic */ void lambda$startActivityDismissingKeyguard$18(ActivityStarter.Callback callback) {
        if (callback != null) {
            callback.onActivityStarted(-96);
        }
    }

    public void readyForKeyguardDone() {
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void executeRunnableDismissingKeyguard(Runnable runnable, Runnable runnable2, boolean z, boolean z2, boolean z3) {
        dismissKeyguardThenExecute(new ActivityStarter.OnDismissAction(runnable, z, z3) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$L4kE_3rylr6H_pNi7mB0rm5zMes */
            private final /* synthetic */ Runnable f$1;
            private final /* synthetic */ boolean f$2;
            private final /* synthetic */ boolean f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            @Override // com.android.systemui.plugins.ActivityStarter.OnDismissAction
            public final boolean onDismiss() {
                return StatusBar.this.lambda$executeRunnableDismissingKeyguard$19$StatusBar(this.f$1, this.f$2, this.f$3);
            }
        }, runnable2, z2);
    }

    public /* synthetic */ boolean lambda$executeRunnableDismissingKeyguard$19$StatusBar(Runnable runnable, boolean z, boolean z2) {
        if (runnable != null) {
            if (!this.mStatusBarKeyguardViewManager.isShowing() || !this.mStatusBarKeyguardViewManager.isOccluded()) {
                AsyncTask.execute(runnable);
            } else {
                this.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
            }
        }
        if (z) {
            if (!this.mExpandedVisible || this.mBouncerShowing) {
                this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$h1YVkfulr3o8WBsc2YTikmPmYI */

                    public final void run() {
                        StatusBar.this.runPostCollapseRunnables();
                    }
                });
            } else {
                animateCollapsePanels(2, true, true);
            }
        } else if (isInLaunchTransition() && this.mNotificationPanel.isLaunchTransitionFinished()) {
            H h = this.mHandler;
            StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
            Objects.requireNonNull(statusBarKeyguardViewManager);
            h.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$JQMd1r5WuAA5n3kv4yv5u3MFjI8 */

                public final void run() {
                    StatusBarKeyguardViewManager.this.readyForKeyguardDone();
                }
            });
        }
        return z2;
    }

    public void resetUserExpandedStates() {
        ArrayList<NotificationEntry> activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
        int size = activeNotifications.size();
        for (int i = 0; i < size; i++) {
            activeNotifications.get(i).resetUserExpansion();
        }
    }

    /* access modifiers changed from: private */
    public void executeWhenUnlocked(ActivityStarter.OnDismissAction onDismissAction) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        }
        dismissKeyguardThenExecute(onDismissAction, null, false);
    }

    /* access modifiers changed from: protected */
    public void dismissKeyguardThenExecute(ActivityStarter.OnDismissAction onDismissAction, boolean z) {
        dismissKeyguardThenExecute(onDismissAction, null, z);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void dismissKeyguardThenExecute(ActivityStarter.OnDismissAction onDismissAction, Runnable runnable, boolean z) {
        if (this.mWakefulnessLifecycle.getWakefulness() == 0 && this.mUnlockMethodCache.canSkipBouncer() && !this.mStatusBarStateController.leaveOpenOnKeyguardHide() && isPulsing()) {
            this.mBiometricUnlockController.startWakeAndUnlock(2);
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(onDismissAction, runnable, z);
        } else {
            onDismissAction.onDismiss();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onConfigChanged(Configuration configuration) {
        updateResources();
        updateDisplaySize();
        this.mViewHierarchyManager.updateRowStates();
        this.mScreenPinningRequest.onConfigurationChanged();
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void setLockscreenUser(int i) {
        this.mLockscreenWallpaper.setCurrentUser(i);
        this.mScrimController.setCurrentUser(i);
        this.mWallpaperChangedReceiver.onReceive(this.mContext, null);
    }

    /* access modifiers changed from: package-private */
    public void updateResources() {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null) {
            qSPanel.updateResources();
        }
        loadDimens();
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.updateResources();
        }
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (notificationPanelView != null) {
            notificationPanelView.updateResources();
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.updateResources();
        }
    }

    /* access modifiers changed from: protected */
    public void loadDimens() {
        int i;
        Resources resources = this.mContext.getResources();
        SomcKeyguardRuntimeResources.reload(this.mStatusBarWindow, resources);
        int i2 = this.mNaturalBarHeight;
        this.mNaturalBarHeight = resources.getDimensionPixelSize(17105427);
        StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
        if (statusBarWindowController != null && (i = this.mNaturalBarHeight) != i2) {
            statusBarWindowController.setBarHeight(i);
        }
    }

    /* access modifiers changed from: protected */
    public void handleVisibleToUserChanged(boolean z) {
        if (z) {
            handleVisibleToUserChangedImpl(z);
            this.mNotificationLogger.startNotificationLogging();
            return;
        }
        this.mNotificationLogger.stopNotificationLogging();
        handleVisibleToUserChangedImpl(z);
    }

    /* access modifiers changed from: package-private */
    public void handlePeekToExpandTransistion() {
        try {
            this.mBarService.onPanelRevealed(false, this.mEntryManager.getNotificationData().getActiveNotifications().size());
        } catch (RemoteException unused) {
        }
    }

    private void handleVisibleToUserChangedImpl(boolean z) {
        int i;
        if (z) {
            boolean hasPinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
            int i2 = 1;
            boolean z2 = !this.mPresenter.isPresenterFullyCollapsed() && ((i = this.mState) == 0 || i == 2);
            int size = this.mEntryManager.getNotificationData().getActiveNotifications().size();
            if (!hasPinnedHeadsUp || !this.mPresenter.isPresenterFullyCollapsed()) {
                i2 = size;
            }
            this.mUiOffloadThread.submit(new Runnable(z2, i2) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$HmJQbKES5h2Nfz54WrIvhU_YRh4 */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    StatusBar.this.lambda$handleVisibleToUserChangedImpl$20$StatusBar(this.f$1, this.f$2);
                }
            });
            return;
        }
        this.mUiOffloadThread.submit(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$c9qjiwrIU9RXDCI3JWlVp8xvdoU */

            public final void run() {
                StatusBar.this.lambda$handleVisibleToUserChangedImpl$21$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$handleVisibleToUserChangedImpl$20$StatusBar(boolean z, int i) {
        try {
            this.mBarService.onPanelRevealed(z, i);
        } catch (RemoteException unused) {
        }
    }

    public /* synthetic */ void lambda$handleVisibleToUserChangedImpl$21$StatusBar() {
        try {
            this.mBarService.onPanelHidden();
        } catch (RemoteException unused) {
        }
    }

    /* JADX WARN: Type inference failed for: r9v0, types: [boolean, int] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void logStateToEventlog() {
        /*
        // Method dump skipped, instructions count: 101
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBar.logStateToEventlog():void");
    }

    /* access modifiers changed from: package-private */
    public void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250, VIBRATION_ATTRIBUTES);
    }

    public /* synthetic */ void lambda$new$22$StatusBar() {
        Debug.stopMethodTracing();
        Log.d("StatusBar", "stopTracing");
        vibrate();
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postQSRunnableDismissingKeyguard(Runnable runnable) {
        this.mHandler.post(new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$GjkAle6Yh2ihV21EScdNFN2cPY */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postQSRunnableDismissingKeyguard$24$StatusBar(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$postQSRunnableDismissingKeyguard$24$StatusBar(Runnable runnable) {
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        executeRunnableDismissingKeyguard(new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$AWaoQDCpm4WLbje2ihIy1hyU7w */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postQSRunnableDismissingKeyguard$23$StatusBar(this.f$1);
            }
        }, null, false, false, false);
    }

    public /* synthetic */ void lambda$postQSRunnableDismissingKeyguard$23$StatusBar(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postRunnableDismissingKeyguard(Runnable runnable) {
        this.mHandler.post(new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$Qg62zaTFZgEgxic4yj9KoTQSdv8 */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postRunnableDismissingKeyguard$26$StatusBar(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$postRunnableDismissingKeyguard$26$StatusBar(Runnable runnable) {
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        executeRunnableDismissingKeyguard(new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$gnIlF9rwp4R2rlyXnw0hvU6cDw8 */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postRunnableDismissingKeyguard$25$StatusBar(this.f$1);
            }
        }, null, true, true, true);
    }

    public /* synthetic */ void lambda$postRunnableDismissingKeyguard$25$StatusBar(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(PendingIntent pendingIntent) {
        this.mHandler.post(new Runnable(pendingIntent) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$r5_blb0mImZBzspRqqf6xf1HZbY */
            private final /* synthetic */ PendingIntent f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postStartActivityDismissingKeyguard$27$StatusBar(this.f$1);
            }
        });
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(Intent intent, int i) {
        this.mHandler.postDelayed(new Runnable(intent) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$swMevwBD7gZzyvLphvmM2iTSGzE */
            private final /* synthetic */ Intent f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postStartActivityDismissingKeyguard$28$StatusBar(this.f$1);
            }
        }, (long) i);
    }

    public /* synthetic */ void lambda$postStartActivityDismissingKeyguard$28$StatusBar(Intent intent) {
        handleStartActivityDismissingKeyguard(intent, true);
    }

    private void handleStartActivityDismissingKeyguard(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, z, true);
    }

    @Override // com.android.systemui.DemoMode
    public void dispatchDemoCommand(String str, Bundle bundle) {
        View view;
        VolumeComponent volumeComponent;
        int i = 0;
        if (!this.mDemoModeAllowed) {
            this.mDemoModeAllowed = Settings.Global.getInt(this.mContext.getContentResolver(), "sysui_demo_allowed", 0) != 0;
        }
        if (this.mDemoModeAllowed) {
            if (str.equals("enter")) {
                this.mDemoMode = true;
            } else if (str.equals("exit")) {
                this.mDemoMode = false;
                checkBarModes();
            } else if (!this.mDemoMode) {
                dispatchDemoCommand("enter", new Bundle());
            }
            boolean z = str.equals("enter") || str.equals("exit");
            if ((z || str.equals("volume")) && (volumeComponent = this.mVolumeComponent) != null) {
                volumeComponent.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("clock")) {
                dispatchDemoCommandToView(str, bundle, C0007R$id.clock);
            }
            if (z || str.equals("battery")) {
                this.mBatteryController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("status")) {
                ((StatusBarIconControllerImpl) this.mIconController).dispatchDemoCommand(str, bundle);
            }
            if (this.mNetworkController != null && (z || str.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("notifications")) {
                PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
                if (phoneStatusBarView == null) {
                    view = null;
                } else {
                    view = phoneStatusBarView.findViewById(C0007R$id.notification_icon_area);
                }
                if (view != null) {
                    view.setVisibility((!this.mDemoMode || !"false".equals(bundle.getString("visible"))) ? 0 : 4);
                }
            }
            if (str.equals("bars")) {
                String string = bundle.getString("mode");
                if (!"opaque".equals(string)) {
                    if ("translucent".equals(string)) {
                        i = 2;
                    } else if ("semi-transparent".equals(string)) {
                        i = 1;
                    } else if ("transparent".equals(string)) {
                        i = 4;
                    } else {
                        i = "warning".equals(string) ? 5 : -1;
                    }
                }
                if (i != -1) {
                    PhoneStatusBarView phoneStatusBarView2 = this.mStatusBarView;
                    if (phoneStatusBarView2 != null) {
                        phoneStatusBarView2.getBarTransitions().transitionTo(i, true);
                    }
                    this.mNavigationBarController.transitionTo(this.mDisplayId, i, true);
                }
            }
            if (z || str.equals("operator")) {
                dispatchDemoCommandToView(str, bundle, C0007R$id.operator_name);
            }
        }
    }

    private void dispatchDemoCommandToView(String str, Bundle bundle, int i) {
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            View findViewById = phoneStatusBarView.findViewById(i);
            if (findViewById instanceof DemoMode) {
                ((DemoMode) findViewById).dispatchDemoCommand(str, bundle);
            }
        }
    }

    public void showKeyguard() {
        this.mStatusBarStateController.setKeyguardRequested(true);
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(false);
        this.mPendingRemoteInputView = null;
        updateIsKeyguard();
        this.mAssistManager.onLockscreenShown();
    }

    public boolean hideKeyguard() {
        this.mStatusBarStateController.setKeyguardRequested(false);
        return updateIsKeyguard();
    }

    public boolean isFullScreenUserSwitcherState() {
        return this.mState == 3;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean updateIsKeyguard() {
        boolean z = true;
        boolean z2 = this.mBiometricUnlockController.getMode() == 1;
        boolean z3 = this.mDozingRequested && (!this.mDeviceInteractive || (isGoingToSleep() && (isScreenFullyOff() || this.mIsKeyguard)));
        if ((!this.mStatusBarStateController.isKeyguardRequested() && !z3) || z2) {
            z = false;
        }
        if (z3) {
            updatePanelExpansionForKeyguard();
        }
        if (!z) {
            return hideKeyguardImpl();
        }
        if (!isGoingToSleep() || this.mScreenLifecycle.getScreenState() != 3) {
            showKeyguardImpl();
        }
        return false;
    }

    public void showKeyguardImpl() {
        this.mIsKeyguard = true;
        if (this.mKeyguardMonitor.isLaunchTransitionFadingAway()) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        this.mHandler.removeMessages(1003);
        UserSwitcherController userSwitcherController = this.mUserSwitcherController;
        if (userSwitcherController != null && userSwitcherController.useFullscreenUserSwitcher()) {
            this.mStatusBarStateController.setState(3);
        } else if (!this.mPulseExpansionHandler.isWakingToShadeLocked()) {
            this.mStatusBarStateController.setState(1);
        }
        updatePanelExpansionForKeyguard();
        NotificationEntry notificationEntry = this.mDraggedDownEntry;
        if (notificationEntry != null) {
            notificationEntry.setUserLocked(false);
            this.mDraggedDownEntry.notifyHeightChanged(false);
            this.mDraggedDownEntry = null;
        }
    }

    private void updatePanelExpansionForKeyguard() {
        if (this.mState == 1 && this.mBiometricUnlockController.getMode() != 1 && !this.mBouncerShowing) {
            instantExpandNotificationsPanel();
        } else if (this.mState == 3) {
            instantCollapseNotificationPanel();
        }
    }

    /* access modifiers changed from: private */
    public void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        runLaunchTransitionEndRunnable();
        this.mKeyguardMonitor.setLaunchTransitionFadingAway(false);
        this.mPresenter.updateMediaMetaData(true, true);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void addPostCollapseAction(Runnable runnable) {
        this.mPostCollapseRunnables.add(runnable);
    }

    public boolean isInLaunchTransition() {
        return this.mNotificationPanel.isLaunchTransitionRunning() || this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(Runnable runnable, Runnable runnable2) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = runnable2;
        $$Lambda$StatusBar$Y3fMrUHySZxiJoTF8C7vKsQWUE r4 = new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$Y3fMrUHySZxiJoTF8C7vKsQWUE */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$fadeKeyguardAfterLaunchTransition$29$StatusBar(this.f$1);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(r4);
        } else {
            r4.run();
        }
    }

    public /* synthetic */ void lambda$fadeKeyguardAfterLaunchTransition$29$StatusBar(Runnable runnable) {
        this.mKeyguardMonitor.setLaunchTransitionFadingAway(true);
        if (runnable != null) {
            runnable.run();
        }
        updateScrimController();
        this.mPresenter.updateMediaMetaData(false, true);
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0).setDuration(0).withLayer().withEndAction(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$GDSEpzokV1v2uNGuP8V5K9Jrjw */

            public final void run() {
                StatusBar.this.onLaunchTransitionFadingEnded();
            }
        });
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, SystemClock.uptimeMillis(), 120, true);
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0).setDuration(96).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$61RWUFHT3DUOUKO1dL6l4XWnMc */

            public final void run() {
                StatusBar.this.lambda$fadeKeyguardWhilePulsing$30$StatusBar();
            }
        }).start();
    }

    public /* synthetic */ void lambda$fadeKeyguardWhilePulsing$30$StatusBar() {
        hideKeyguard();
        this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
    }

    public void animateKeyguardUnoccluding() {
        this.mNotificationPanel.setExpandedFraction(0.0f);
        animateExpandNotificationsPanel();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onLaunchTransitionTimeout() {
        Log.w("StatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mNotificationPanel.resetViews(false);
    }

    private void runLaunchTransitionEndRunnable() {
        Runnable runnable = this.mLaunchTransitionEndRunnable;
        if (runnable != null) {
            this.mLaunchTransitionEndRunnable = null;
            runnable.run();
        }
    }

    public boolean hideKeyguardImpl() {
        this.mIsKeyguard = false;
        Trace.beginSection("StatusBar#hideKeyguard");
        boolean leaveOpenOnKeyguardHide = this.mStatusBarStateController.leaveOpenOnKeyguardHide();
        if (!this.mStatusBarStateController.setState(0)) {
            this.mLockscreenUserManager.updatePublicMode();
        }
        if (this.mStatusBarStateController.leaveOpenOnKeyguardHide()) {
            if (!this.mStatusBarStateController.isKeyguardRequested()) {
                this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(false);
            }
            long calculateGoingToFullShadeDelay = this.mKeyguardMonitor.calculateGoingToFullShadeDelay();
            this.mNotificationPanel.animateToFullShade(calculateGoingToFullShadeDelay);
            NotificationEntry notificationEntry = this.mDraggedDownEntry;
            if (notificationEntry != null) {
                notificationEntry.setUserLocked(false);
                this.mDraggedDownEntry = null;
            }
            this.mNavigationBarController.disableAnimationsDuringHide(this.mDisplayId, calculateGoingToFullShadeDelay);
        } else if (!this.mNotificationPanel.isCollapsing()) {
            instantCollapseNotificationPanel();
        }
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null) {
            qSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
        this.mNotificationPanel.animate().cancel();
        this.mNotificationPanel.setAlpha(1.0f);
        updateScrimController();
        Trace.endSection();
        return leaveOpenOnKeyguardHide;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void releaseGestureWakeLock() {
        if (this.mGestureWakeLock.isHeld()) {
            this.mGestureWakeLock.release();
        }
    }

    public void keyguardGoingAway() {
        this.mKeyguardMonitor.notifyKeyguardGoingAway(true);
        this.mCommandQueue.appTransitionPending(this.mDisplayId, true);
    }

    public void setKeyguardFadingAway(long j, long j2, long j3) {
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, (j + j3) - 120, 120, true);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, j3 > 0);
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, j - 120, 120, true);
        this.mKeyguardMonitor.notifyKeyguardFadingAway(j2, j3);
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardMonitor.notifyKeyguardDoneFading();
        this.mScrimController.setExpansionAffectsAlpha(true);
    }

    /* access modifiers changed from: protected */
    public void updateTheme() {
        int i;
        if (!this.mColorExtractor.getNeutralColors().supportsDarkText() || this.mIsSkinningEnabled) {
            i = C0015R$style.Theme_SystemUI;
        } else {
            i = C0015R$style.Theme_SystemUI_Light;
        }
        if (this.mContext.getThemeResId() != i) {
            this.mContext.setTheme(i);
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).notifyThemeChanged();
        }
    }

    private void updateDozingState() {
        Trace.traceCounter(4096, "dozing", this.mDozing ? 1 : 0);
        Trace.beginSection("StatusBar#updateDozingState");
        boolean isGoingToSleepVisibleNotOccluded = this.mStatusBarKeyguardViewManager.isGoingToSleepVisibleNotOccluded();
        boolean z = false;
        Object[] objArr = this.mBiometricUnlockController.getMode() == 1 ? 1 : null;
        ((LockscreenLoopsController) Dependency.get(LockscreenLoopsController.class)).setDozing(this.mDozing);
        AodView aodView = this.mAodView;
        if (aodView != null) {
            aodView.setDozing(this.mDozing);
        }
        if ((!this.mDozing && this.mDozeServiceHost.shouldAnimateWakeup() && objArr == null) || (this.mDozing && this.mDozeServiceHost.shouldAnimateScreenOff() && isGoingToSleepVisibleNotOccluded)) {
            z = true;
        }
        this.mNotificationPanel.setDozing(this.mDozing, z, this.mWakeUpTouchLocation);
        updateQsExpansionEnabled();
        Trace.endSection();
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        if (this.mState != 1 || !this.mStatusBarKeyguardViewManager.interceptMediaKey(keyEvent)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUnlockOnMenuPressed() {
        return this.mDeviceInteractive && this.mState != 0 && this.mStatusBarKeyguardViewManager.shouldDismissOnMenuPressed();
    }

    public boolean onMenuPressed() {
        if (!shouldUnlockOnMenuPressed()) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    public void endAffordanceLaunch() {
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
    }

    public boolean onBackPressed() {
        if (isInLaunchTransition()) {
            this.mHandler.removeMessages(1003);
            onLaunchTransitionTimeout();
        }
        boolean z = this.mScrimController.getState() == ScrimState.BOUNCER_SCRIMMED;
        if (this.mStatusBarKeyguardViewManager.onBackPressed(z)) {
            if (!z) {
                this.mNotificationPanel.expandWithoutQs();
            }
            return true;
        } else if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs(false);
            }
            return true;
        } else {
            int i = this.mState;
            if (i == 1 || i == 2) {
                KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
                return keyguardUserSwitcher != null && keyguardUserSwitcher.hideIfNotSimple(true);
            }
            if (this.mNotificationPanel.canPanelBeCollapsed()) {
                animateCollapsePanels();
            } else {
                this.mBubbleController.performBackPressIfNeeded();
            }
            return true;
        }
    }

    public boolean onSpacePressed() {
        if (!this.mDeviceInteractive || this.mState == 0) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncerIfKeyguard() {
        int i = this.mState;
        if ((i == 1 || i == 2) && !this.mKeyguardViewMediator.isHiding()) {
            showBouncer(true);
        }
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void showBouncer(boolean z) {
        this.mStatusBarKeyguardViewManager.showBouncer(z);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.expand(false);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, false);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public boolean closeShadeIfOpen() {
        if (!this.mNotificationPanel.isFullyCollapsed()) {
            this.mCommandQueue.animateCollapsePanels(2, true);
            visibilityChanged(false);
            this.mAssistManager.hideAssist();
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void postOnShadeExpanded(final Runnable runnable) {
        this.mNotificationPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            /* class com.android.systemui.statusbar.phone.StatusBar.AnonymousClass11 */

            public void onGlobalLayout() {
                if (StatusBar.this.getStatusBarWindow().getHeight() != StatusBar.this.getStatusBarHeight()) {
                    StatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    StatusBar.this.mNotificationPanel.post(runnable);
                }
            }
        });
    }

    private void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
        runPostCollapseRunnables();
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStatePreChange(int i, int i2) {
        if (this.mVisible && (i2 == 2 || ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).goingToFullShade())) {
            clearNotificationEffects();
        }
        if (i2 == 1) {
            this.mRemoteInputManager.onPanelCollapsed();
            maybeEscalateHeadsUp();
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        this.mState = i;
        updateReportRejectedTouchVisibility();
        updateDozing();
        updateTheme();
        this.mNavigationBarController.touchAutoDim(this.mDisplayId);
        Trace.beginSection("StatusBar#updateKeyguardState");
        boolean z = true;
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
            if (keyguardUserSwitcher != null) {
                keyguardUserSwitcher.setKeyguard(true, this.mStatusBarStateController.fromShadeLocked());
            }
            PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
            if (phoneStatusBarView != null) {
                phoneStatusBarView.removePendingHideExpandedRunnables();
            }
            View view = this.mAmbientIndicationContainer;
            if (view != null) {
                view.setVisibility(0);
            }
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            KeyguardUserSwitcher keyguardUserSwitcher2 = this.mKeyguardUserSwitcher;
            if (keyguardUserSwitcher2 != null) {
                keyguardUserSwitcher2.setKeyguard(false, this.mStatusBarStateController.goingToFullShade() || this.mState == 2 || this.mStatusBarStateController.fromShadeLocked());
            }
            View view2 = this.mAmbientIndicationContainer;
            if (view2 != null) {
                view2.setVisibility(4);
            }
        }
        updateDozingState();
        checkBarModes();
        updateScrimController();
        NotificationPresenter notificationPresenter = this.mPresenter;
        if (this.mState == 1) {
            z = false;
        }
        notificationPresenter.updateMediaMetaData(false, z);
        updateKeyguardState();
        Trace.endSection();
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        Trace.beginSection("StatusBar#updateDozing");
        this.mDozing = z;
        this.mNotificationPanel.resetViews(this.mDozingRequested && DozeParameters.getInstance(this.mContext).shouldControlScreenOff());
        updateQsExpansionEnabled();
        this.mKeyguardViewMediator.setAodShowing(this.mDozing);
        this.mEntryManager.updateNotifications();
        updateDozingState();
        updateScrimController();
        updateReportRejectedTouchVisibility();
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDozing() {
        boolean z = (this.mDozingRequested && this.mState == 1) || this.mBiometricUnlockController.getMode() == 2;
        if (this.mBiometricUnlockController.getMode() == 1) {
            z = false;
        }
        this.mStatusBarStateController.setIsDozing(z);
    }

    private void updateKeyguardState() {
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mUnlockMethodCache.isMethodSecure(), this.mStatusBarKeyguardViewManager.isOccluded());
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void onActivationReset() {
        this.mKeyguardIndicationController.hideTransientIndication();
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
        if (!this.mPresenter.isPresenterFullyCollapsed()) {
            this.mStatusBarWindowController.setStatusBarFocusable(true);
        }
    }

    public void onUnlockHintStarted() {
        this.mFalsingManager.onUnlockHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(C0014R$string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200);
    }

    public void onCameraHintStarted() {
        this.mFalsingManager.onCameraHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(C0014R$string.camera_hint);
    }

    public void onVoiceAssistHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(C0014R$string.voice_hint);
    }

    public void onPhoneHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(C0014R$string.phone_hint);
    }

    public void onTrackingStopped(boolean z) {
        int i = this.mState;
        if ((i == 1 || i == 2) && !z && !this.mUnlockMethodCache.canSkipBouncer()) {
            showBouncer(false);
        }
    }

    public NavigationBarView getNavigationBarView() {
        return this.mNavigationBarController.getNavigationBarView(this.mDisplayId);
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void goToLockedShade(View view) {
        NotificationEntry notificationEntry;
        if ((this.mDisabled2 & 4) == 0) {
            int currentUserId = this.mLockscreenUserManager.getCurrentUserId();
            if (view instanceof ExpandableNotificationRow) {
                notificationEntry = ((ExpandableNotificationRow) view).getEntry();
                notificationEntry.setUserExpanded(true, true);
                notificationEntry.setGroupExpansionChanging(true);
                StatusBarNotification statusBarNotification = notificationEntry.notification;
                if (statusBarNotification != null) {
                    currentUserId = statusBarNotification.getUserId();
                }
            } else {
                notificationEntry = null;
            }
            NotificationLockscreenUserManager notificationLockscreenUserManager = this.mLockscreenUserManager;
            boolean z = !notificationLockscreenUserManager.userAllowsPrivateNotificationsInPublic(notificationLockscreenUserManager.getCurrentUserId()) || !this.mLockscreenUserManager.shouldShowLockscreenNotifications() || this.mFalsingManager.shouldEnforceBouncer();
            if (!this.mLockscreenUserManager.isLockscreenPublicMode(currentUserId) || !z) {
                this.mNotificationPanel.animateToFullShade(0);
                this.mStatusBarStateController.setState(2);
                return;
            }
            this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
            showBouncerIfKeyguard();
            this.mDraggedDownEntry = notificationEntry;
            this.mPendingRemoteInputView = null;
        }
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStatusBarStateController.setState(1);
        }
    }

    public void setBouncerShowing(boolean z) {
        this.mBouncerShowing = z;
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.setBouncerShowing(z);
        }
        updateHideIconsForBouncer(true);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
        updateScrimController();
        if (!this.mBouncerShowing) {
            updatePanelExpansionForKeyguard();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateNotificationPanelTouchState() {
        boolean z = true;
        boolean z2 = isGoingToSleep() && !DozeParameters.getInstance(this.mContext).shouldControlScreenOff();
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if ((this.mDeviceInteractive || this.mPulsing) && !z2) {
            z = false;
        }
        notificationPanelView.setTouchAndAnimationDisabled(z);
    }

    public int getWakefulnessState() {
        return this.mWakefulnessLifecycle.getWakefulness();
    }

    private void vibrateForCameraGesture(int i) {
        if (i == 2) {
            this.mVibrator.vibrate(this.mCameraLiftTriggerVibePattern, -1);
        } else if (i == 4) {
            this.mVibratorHelper.vibrate(1000);
        } else {
            this.mVibrator.vibrate(this.mCameraLaunchGestureVibePattern, -1);
        }
    }

    public boolean isScreenFullyOff() {
        return this.mScreenLifecycle.getScreenState() == 0;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showScreenPinningRequest(int i) {
        if (!this.mKeyguardMonitor.isShowing()) {
            showScreenPinningRequest(i, true);
        }
    }

    public void showScreenPinningRequest(int i, boolean z) {
        this.mScreenPinningRequest.showPrompt(i, z);
    }

    public boolean hasActiveNotifications() {
        return !this.mEntryManager.getNotificationData().getActiveNotifications().isEmpty() || this.mNotificationFilter.hasHiddenNotifications();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionCancelled(int i) {
        if (i == this.mDisplayId) {
            ((Divider) getComponent(Divider.class)).onAppTransitionFinished();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionFinished(int i) {
        if (i == this.mDisplayId) {
            ((Divider) getComponent(Divider.class)).onAppTransitionFinished();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void onCameraLaunchGestureDetected(int i) {
        Log.i("StatusBar", "onCameraLaunchGestureDetected" + i);
        this.mLastCameraLaunchSource = i;
        if (isGoingToSleep()) {
            Slog.d("StatusBar", "Finish going to sleep before launching camera");
            this.mLaunchCameraOnFinishedGoingToSleep = true;
            return;
        }
        if (!this.mNotificationPanel.canCameraGestureBeLaunched(this.mStatusBarKeyguardViewManager.isShowing() && this.mExpandedVisible, i)) {
            Slog.d("StatusBar", "Can't launch camera right now, mExpandedVisible: " + this.mExpandedVisible);
            return;
        }
        if (!this.mDeviceInteractive) {
            ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), 5, "com.android.systemui:CAMERA_GESTURE");
            Slog.d("StatusBar", "!mDeviceInteractive");
        }
        vibrateForCameraGesture(i);
        if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            Intent intent = new Intent(KeyguardBottomAreaView.INSECURE_CAMERA_INTENT);
            intent.putExtra("com.android.systemui.camera_launch_source", getCameraLaunchSourceString(i));
            intent.setPackage(getCameraLaunchPackage(i));
            startActivityDismissingKeyguard(intent, false, i != 2, true, null, 0);
            return;
        }
        if (!this.mDeviceInteractive) {
            this.mGestureWakeLock.acquire(6000);
        }
        if (isWakingUpOrAwake()) {
            Slog.d("StatusBar", "Launching camera");
            if (this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                this.mStatusBarKeyguardViewManager.reset(true);
            }
            if (i == 1) {
                this.mLaunchingCamera = true;
            }
            this.mNotificationPanel.launchCamera(this.mDeviceInteractive, i);
            updateScrimController();
            return;
        }
        Slog.d("StatusBar", "Deferring until screen turns on");
        this.mLaunchCameraWhenFinishedWaking = true;
    }

    public String getCameraLaunchPackage(int i) {
        if (i == 2) {
            return this.mCameraLiftTriggerLaunchPackage;
        }
        if (i == 4) {
            return this.mCameraKeyLaunchPackage;
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public boolean isCameraAllowedByAdmin() {
        if (this.mDevicePolicyManager.getCameraDisabled(null, this.mLockscreenUserManager.getCurrentUserId())) {
            return false;
        }
        if (this.mStatusBarKeyguardViewManager != null && (!isKeyguardShowing() || !isKeyguardSecure())) {
            return true;
        }
        if ((this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mLockscreenUserManager.getCurrentUserId()) & 2) == 0) {
            return true;
        }
        return false;
    }

    private boolean isGoingToSleep() {
        return this.mWakefulnessLifecycle.getWakefulness() == 3;
    }

    private boolean isWakingUpOrAwake() {
        if (this.mWakefulnessLifecycle.getWakefulness() == 2 || this.mWakefulnessLifecycle.getWakefulness() == 1) {
            return true;
        }
        return false;
    }

    public void notifyBiometricAuthModeChanged() {
        updateDozing();
        updateScrimController();
        this.mStatusBarWindow.onBiometricAuthModeChanged(this.mBiometricUnlockController.isWakeAndUnlock());
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void updateScrimController() {
        Trace.beginSection("StatusBar#updateScrimController");
        boolean isWakeAndUnlock = this.mBiometricUnlockController.isWakeAndUnlock();
        this.mScrimController.setExpansionAffectsAlpha(!this.mBiometricUnlockController.isBiometricUnlock());
        boolean isLaunchingAffordanceWithPreview = this.mNotificationPanel.isLaunchingAffordanceWithPreview();
        this.mScrimController.setLaunchingAffordanceWithPreview(isLaunchingAffordanceWithPreview);
        if (this.mBouncerShowing) {
            this.mScrimController.transitionTo(this.mStatusBarKeyguardViewManager.bouncerNeedsScrimming() ? ScrimState.BOUNCER_SCRIMMED : ScrimState.BOUNCER);
        } else if (isInLaunchTransition() || this.mLaunchCameraWhenFinishedWaking || isLaunchingAffordanceWithPreview) {
            this.mScrimController.transitionTo(ScrimState.UNLOCKED, this.mUnlockScrimCallback);
        } else if (this.mBrightnessMirrorVisible) {
            this.mScrimController.transitionTo(ScrimState.BRIGHTNESS_MIRROR);
        } else if (isPulsing()) {
            this.mScrimController.transitionTo(ScrimState.PULSING, this.mDozeScrimController.getScrimCallback());
        } else if (this.mDozing && !isWakeAndUnlock) {
            this.mScrimController.transitionTo(ScrimState.AOD);
        } else if (this.mIsKeyguard && !isWakeAndUnlock) {
            this.mScrimController.transitionTo(ScrimState.KEYGUARD);
        } else if (this.mBubbleController.isStackExpanded()) {
            this.mScrimController.transitionTo(ScrimState.BUBBLE_EXPANDED);
        } else {
            this.mScrimController.transitionTo(ScrimState.UNLOCKED, this.mUnlockScrimCallback);
        }
        Trace.endSection();
    }

    public boolean isKeyguardShowing() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            return statusBarKeyguardViewManager.isShowing();
        }
        Slog.i("StatusBar", "isKeyguardShowing() called before startKeyguard(), returning true");
        return true;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public final class DozeServiceHost implements DozeHost {
        private boolean mAnimateScreenOff;
        private boolean mAnimateWakeup;
        private final ArrayList<DozeHost.Callback> mCallbacks = new ArrayList<>();
        private boolean mIgnoreTouchWhilePulsing;

        DozeServiceHost() {
        }

        public String toString() {
            return "PSB.DozeServiceHost[mCallbacks=" + this.mCallbacks.size() + "]";
        }

        public void firePowerSaveChanged(boolean z) {
            Iterator<DozeHost.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onPowerSaveChanged(z);
            }
        }

        public void fireNotificationPulse() {
            Iterator<DozeHost.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onNotificationAlerted();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void startDozing() {
            if (!StatusBar.this.mDozingRequested) {
                StatusBar.this.mDozingRequested = true;
                StatusBar statusBar = StatusBar.this;
                DozeLog.traceDozing(statusBar.mContext, statusBar.mDozing);
                StatusBar.this.updateDozing();
                StatusBar.this.updateIsKeyguard();
                return;
            }
            StatusBar.this.mDozingRequested = true;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void pulseWhileDozing(final DozeHost.PulseCallback pulseCallback, int i) {
            StatusBarWindowView statusBarWindowView;
            if (i == 5) {
                StatusBar.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 4, "com.android.systemui:LONG_PRESS");
                StatusBar.this.startAssist(new Bundle());
                return;
            }
            if (i == 8) {
                StatusBar.this.mScrimController.setWakeLockScreenSensorActive(true);
            }
            if (i == 6 && (statusBarWindowView = StatusBar.this.mStatusBarWindow) != null) {
                statusBarWindowView.suppressWakeUpGesture(true);
            }
            final boolean z = i == 1;
            StatusBar.this.mPulsing = true;
            StatusBar.this.mDozeScrimController.pulse(new DozeHost.PulseCallback() {
                /* class com.android.systemui.statusbar.phone.StatusBar.DozeServiceHost.AnonymousClass1 */

                @Override // com.android.systemui.doze.DozeHost.PulseCallback
                public void onPulseStarted() {
                    pulseCallback.onPulseStarted();
                    StatusBar.this.updateNotificationPanelTouchState();
                    setPulsing(true);
                }

                @Override // com.android.systemui.doze.DozeHost.PulseCallback
                public void onPulseFinished() {
                    StatusBar.this.mPulsing = false;
                    pulseCallback.onPulseFinished();
                    StatusBar.this.updateNotificationPanelTouchState();
                    StatusBar.this.mScrimController.setWakeLockScreenSensorActive(false);
                    StatusBarWindowView statusBarWindowView = StatusBar.this.mStatusBarWindow;
                    if (statusBarWindowView != null) {
                        statusBarWindowView.suppressWakeUpGesture(false);
                    }
                    setPulsing(false);
                }

                private void setPulsing(boolean z) {
                    StatusBar.this.mStatusBarKeyguardViewManager.setPulsing(z);
                    StatusBar.this.mKeyguardViewMediator.setPulsing(z);
                    StatusBar.this.mNotificationPanel.setPulsing(z);
                    StatusBar.this.mVisualStabilityManager.setPulsing(z);
                    StatusBar.this.mStatusBarWindow.setPulsing(z);
                    DozeServiceHost.this.mIgnoreTouchWhilePulsing = false;
                    KeyguardUpdateMonitor keyguardUpdateMonitor = StatusBar.this.mKeyguardUpdateMonitor;
                    if (keyguardUpdateMonitor != null && z) {
                        keyguardUpdateMonitor.onAuthInterruptDetected(z);
                    }
                    StatusBar.this.updateScrimController();
                    StatusBar.this.mPulseExpansionHandler.setPulsing(z);
                    StatusBar.this.mWakeUpCoordinator.setPulsing(z);
                }
            }, i);
            StatusBar.this.updateScrimController();
        }

        @Override // com.android.systemui.doze.DozeHost
        public void stopDozing() {
            if (StatusBar.this.mDozingRequested) {
                StatusBar.this.mDozingRequested = false;
                StatusBar statusBar = StatusBar.this;
                DozeLog.traceDozing(statusBar.mContext, statusBar.mDozing);
                StatusBar.this.updateDozing();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void onIgnoreTouchWhilePulsing(boolean z) {
            if (z != this.mIgnoreTouchWhilePulsing) {
                DozeLog.tracePulseTouchDisabledByProx(StatusBar.this.mContext, z);
            }
            this.mIgnoreTouchWhilePulsing = z;
            if (StatusBar.this.isDozing() && z) {
                StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void dozeTimeTick() {
            StatusBar.this.mNotificationPanel.dozeTimeTick();
            if (StatusBar.this.mAmbientIndicationContainer instanceof DozeReceiver) {
                ((DozeReceiver) StatusBar.this.mAmbientIndicationContainer).dozeTimeTick();
            }
            StatusBar.this.mAodView.dozeTimeTick();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isPowerSaveActive() {
            return StatusBar.this.mBatteryController.isAodPowerSave();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isPulsingBlocked() {
            return StatusBar.this.mBiometricUnlockController.getMode() == 1;
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isProvisioned() {
            return StatusBar.this.mDeviceProvisionedController.isDeviceProvisioned() && StatusBar.this.mDeviceProvisionedController.isCurrentUserSetup();
        }

        @Override // com.android.systemui.doze.DozeHost
        public boolean isBlockingDoze() {
            if (!StatusBar.this.mBiometricUnlockController.hasPendingAuthentication()) {
                return false;
            }
            Log.i("StatusBar", "Blocking AOD because fingerprint has authenticated");
            return true;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void extendPulse(int i) {
            if (i == 8) {
                StatusBar.this.mScrimController.setWakeLockScreenSensorActive(true);
            }
            if (!StatusBar.this.mDozeScrimController.isPulsing() || !StatusBar.this.mAmbientPulseManager.hasNotifications()) {
                StatusBar.this.mDozeScrimController.extendPulse();
            } else {
                StatusBar.this.mAmbientPulseManager.extendPulse();
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAnimateWakeup(boolean z) {
            if (StatusBar.this.mWakefulnessLifecycle.getWakefulness() != 2 && StatusBar.this.mWakefulnessLifecycle.getWakefulness() != 1) {
                this.mAnimateWakeup = z;
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAnimateScreenOff(boolean z) {
            this.mAnimateScreenOff = z;
        }

        @Override // com.android.systemui.doze.DozeHost
        public void onSlpiTap(float f, float f2) {
            if (f > 0.0f && f2 > 0.0f && StatusBar.this.mAmbientIndicationContainer != null && StatusBar.this.mAmbientIndicationContainer.getVisibility() == 0) {
                StatusBar.this.mAmbientIndicationContainer.getLocationOnScreen(StatusBar.this.mTmpInt2);
                float f3 = f - ((float) StatusBar.this.mTmpInt2[0]);
                float f4 = f2 - ((float) StatusBar.this.mTmpInt2[1]);
                if (0.0f <= f3 && f3 <= ((float) StatusBar.this.mAmbientIndicationContainer.getWidth()) && 0.0f <= f4 && f4 <= ((float) StatusBar.this.mAmbientIndicationContainer.getHeight())) {
                    dispatchTap(StatusBar.this.mAmbientIndicationContainer, f3, f4);
                }
            }
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setDozeScreenBrightness(int i) {
            StatusBar.this.mStatusBarWindowController.setDozeScreenBrightness(i);
        }

        @Override // com.android.systemui.doze.DozeHost
        public void setAodDimmingScrim(float f) {
            StatusBar.this.mScrimController.setAodFrontScrimAlpha(f);
        }

        private void dispatchTap(View view, float f, float f2) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            dispatchTouchEvent(view, f, f2, elapsedRealtime, 0);
            dispatchTouchEvent(view, f, f2, elapsedRealtime, 1);
        }

        private void dispatchTouchEvent(View view, float f, float f2, long j, int i) {
            MotionEvent obtain = MotionEvent.obtain(j, j, i, f, f2, 0);
            view.dispatchTouchEvent(obtain);
            obtain.recycle();
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean shouldAnimateWakeup() {
            return this.mAnimateWakeup;
        }

        public boolean shouldAnimateScreenOff() {
            return this.mAnimateScreenOff;
        }
    }

    public boolean shouldIgnoreTouch() {
        return isDozing() && this.mDozeServiceHost.mIgnoreTouchWhilePulsing;
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public void collapsePanel(boolean z) {
        if (z) {
            if (!collapsePanel()) {
                runPostCollapseRunnables();
            }
        } else if (!this.mPresenter.isPresenterFullyCollapsed()) {
            instantCollapseNotificationPanel();
            visibilityChanged(false);
        } else {
            runPostCollapseRunnables();
        }
    }

    @Override // com.android.systemui.statusbar.phone.ShadeController
    public boolean collapsePanel() {
        if (this.mNotificationPanel.isFullyCollapsed()) {
            return false;
        }
        animateCollapsePanels(2, true, true);
        visibilityChanged(false);
        return true;
    }

    public void setNotificationSnoozed(StatusBarNotification statusBarNotification, NotificationSwipeActionHelper.SnoozeOption snoozeOption) {
        if (snoozeOption.getSnoozeCriterion() != null) {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), snoozeOption.getSnoozeCriterion().getId());
        } else {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), (long) (snoozeOption.getMinutesToSnoozeFor() * 60 * 1000));
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void toggleSplitScreen() {
        toggleSplitScreenMode(-1, -1);
    }

    /* access modifiers changed from: package-private */
    public void awakenDreams() {
        ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$uVSnBgW5bpIDYbVSsVJZcuCIXb4 */

            public final void run() {
                StatusBar.this.lambda$awakenDreams$31$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$awakenDreams$31$StatusBar() {
        try {
            this.mDreamManager.awaken();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void dismissKeyboardShortcutsMenu() {
        this.mHandler.removeMessages(1027);
        this.mHandler.sendEmptyMessage(1027);
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void toggleKeyboardShortcutsMenu(int i) {
        this.mHandler.removeMessages(1026);
        this.mHandler.obtainMessage(1026, i, 0).sendToTarget();
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void setTopAppHidesStatusBar(boolean z) {
        this.mTopHidesStatusBar = z;
        if (!z && this.mWereIconsJustHidden) {
            this.mWereIconsJustHidden = false;
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
        }
        updateHideIconsForBouncer(true);
    }

    /* access modifiers changed from: protected */
    public void toggleKeyboardShortcuts(int i) {
        KeyboardShortcuts.toggle(this.mContext, i);
    }

    /* access modifiers changed from: protected */
    public void dismissKeyboardShortcuts() {
        KeyboardShortcuts.dismiss();
    }

    public void onPanelLaidOut() {
        updateKeyguardMaxNotifications();
    }

    public void updateKeyguardMaxNotifications() {
        if (this.mState == 1 && this.mPresenter.getMaxNotificationsWhileLocked(false) != this.mPresenter.getMaxNotificationsWhileLocked(true)) {
            this.mViewHierarchyManager.updateRowStates();
        }
    }

    public void executeActionDismissingKeyguard(Runnable runnable, boolean z) {
        if (this.mDeviceProvisionedController.isDeviceProvisioned()) {
            dismissKeyguardThenExecute(new ActivityStarter.OnDismissAction(runnable) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$azP2e3yurdr5J8YKihnebZ5HV0 */
                private final /* synthetic */ Runnable f$1;

                {
                    this.f$1 = r2;
                }

                @Override // com.android.systemui.plugins.ActivityStarter.OnDismissAction
                public final boolean onDismiss() {
                    return StatusBar.this.lambda$executeActionDismissingKeyguard$33$StatusBar(this.f$1);
                }
            }, z);
        }
    }

    public /* synthetic */ boolean lambda$executeActionDismissingKeyguard$33$StatusBar(Runnable runnable) {
        new Thread(new Runnable(runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$N0soCd5RBgNyAcmYj5rYlAQiyQ */
            private final /* synthetic */ Runnable f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                StatusBar.lambda$executeActionDismissingKeyguard$32(this.f$0);
            }
        }).start();
        return collapsePanel();
    }

    static /* synthetic */ void lambda$executeActionDismissingKeyguard$32(Runnable runnable) {
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException unused) {
        }
        runnable.run();
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    /* renamed from: startPendingIntentDismissingKeyguard */
    public void lambda$postStartActivityDismissingKeyguard$27$StatusBar(PendingIntent pendingIntent) {
        startPendingIntentDismissingKeyguard(pendingIntent, null);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent, Runnable runnable) {
        startPendingIntentDismissingKeyguard(pendingIntent, runnable, null);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent, Runnable runnable, View view) {
        executeActionDismissingKeyguard(new Runnable(pendingIntent, view, runnable) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBar$wuklJrCUlK7DbWeo55YyS_9Cv4o */
            private final /* synthetic */ PendingIntent f$1;
            private final /* synthetic */ View f$2;
            private final /* synthetic */ Runnable f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                StatusBar.this.lambda$startPendingIntentDismissingKeyguard$34$StatusBar(this.f$1, this.f$2, this.f$3);
            }
        }, pendingIntent.isActivity() && this.mActivityIntentHelper.wouldLaunchResolverActivity(pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId()));
    }

    public /* synthetic */ void lambda$startPendingIntentDismissingKeyguard$34$StatusBar(PendingIntent pendingIntent, View view, Runnable runnable) {
        try {
            pendingIntent.send(null, 0, null, null, null, null, getActivityOptions(this.mActivityLaunchAnimator.getLaunchAnimation(view, this.mShadeController.isOccluded())));
        } catch (PendingIntent.CanceledException e) {
            Log.w("StatusBar", "Sending intent failed: " + e);
        }
        if (pendingIntent.isActivity()) {
            this.mAssistManager.hideAssist();
        }
        if (runnable != null) {
            postOnUiThread(runnable);
        }
    }

    private void postOnUiThread(Runnable runnable) {
        this.mMainThreadHandler.post(runnable);
    }

    public static Bundle getActivityOptions(RemoteAnimationAdapter remoteAnimationAdapter) {
        ActivityOptions activityOptions;
        if (remoteAnimationAdapter != null) {
            activityOptions = ActivityOptions.makeRemoteAnimation(remoteAnimationAdapter);
        } else {
            activityOptions = ActivityOptions.makeBasic();
        }
        activityOptions.setLaunchWindowingMode(4);
        return activityOptions.toBundle();
    }

    /* access modifiers changed from: protected */
    public void visibilityChanged(boolean z) {
        if (this.mVisible != z) {
            this.mVisible = z;
            if (!z) {
                this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
            }
        }
        updateVisibleToUser();
    }

    /* access modifiers changed from: protected */
    public void updateVisibleToUser() {
        boolean z = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible && this.mDeviceInteractive;
        boolean z2 = this.mVisibleToUser;
        if (z != z2) {
            handleVisibleToUserChanged(z2);
        }
    }

    public void clearNotificationEffects() {
        try {
            this.mBarService.clearNotificationEffects();
        } catch (RemoteException unused) {
        }
    }

    /* access modifiers changed from: protected */
    public void notifyHeadsUpGoingToSleep() {
        maybeEscalateHeadsUp();
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public boolean isBouncerShowingScrimmed() {
        return isBouncerShowing() && this.mStatusBarKeyguardViewManager.bouncerNeedsScrimming();
    }

    public static PackageManager getPackageManagerForUser(Context context, int i) {
        if (i >= 0) {
            try {
                context = context.createPackageContextAsUser(context.getPackageName(), 4, new UserHandle(i));
            } catch (PackageManager.NameNotFoundException unused) {
            }
        }
        return context.getPackageManager();
    }

    public boolean isKeyguardSecure() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            return statusBarKeyguardViewManager.isSecure();
        }
        Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
        return false;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void showAssistDisclosure() {
        AssistManager assistManager = this.mAssistManager;
        if (assistManager != null) {
            assistManager.showDisclosure();
        }
    }

    public NotificationPanelView getPanel() {
        return this.mNotificationPanel;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void startAssist(Bundle bundle) {
        AssistManager assistManager = this.mAssistManager;
        if (assistManager != null) {
            assistManager.startAssist(bundle);
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void startAssistByDoubleTap(Bundle bundle) {
        AssistManager assistManager = this.mAssistManager;
        if (assistManager != null) {
            assistManager.startAssistByDoubleTap(bundle);
        }
    }

    public NotificationGutsManager getGutsManager() {
        return this.mGutsManager;
    }

    public int getStatusBarMode() {
        return this.mStatusBarMode;
    }
}
