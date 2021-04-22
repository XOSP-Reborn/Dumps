package com.android.systemui;

import android.app.INotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorPrivacyManager;
import android.hardware.display.NightDisplayListener;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.view.IWindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.Preconditions;
import com.android.keyguard.clock.ClockManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.SystemUI;
import com.android.systemui.appops.AppOpsController;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.dock.DockManager;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.power.PowerUI;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.DevicePolicyManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.notification.NotificationAlertingManager;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationFilter;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.ChannelEditorDialogController;
import com.android.systemui.statusbar.notification.row.NotificationBlockingHelperManager;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.phone.NavigationBarLockController;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NotificationGroupAlertTransferHelper;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunablePadding;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.util.leak.LeakDetector;
import com.android.systemui.util.leak.LeakReporter;
import com.sonymobile.systemui.lockscreen.LockscreenAlbumArtController;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController;
import com.sonymobile.systemui.lockscreen.LockscreenClockController;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import com.sonymobile.systemui.lockscreen.LockscreenTransparentScrimController;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import dagger.Lazy;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Dependency extends SystemUI {
    public static final DependencyKey<Handler> BG_HANDLER = new DependencyKey<>("background_handler");
    public static final DependencyKey<Looper> BG_LOOPER = new DependencyKey<>("background_looper");
    public static final DependencyKey<String> LEAK_REPORT_EMAIL = new DependencyKey<>("leak_report_email");
    public static final DependencyKey<Handler> MAIN_HANDLER = new DependencyKey<>("main_handler");
    public static final DependencyKey<Handler> TIME_TICK_HANDLER = new DependencyKey<>("time_tick_handler");
    private static Dependency sDependency;
    Lazy<AccessibilityController> mAccessibilityController;
    Lazy<AccessibilityManagerWrapper> mAccessibilityManagerWrapper;
    Lazy<ActivityManagerWrapper> mActivityManagerWrapper;
    Lazy<ActivityStarter> mActivityStarter;
    Lazy<ActivityStarterDelegate> mActivityStarterDelegate;
    Lazy<AmbientPulseManager> mAmbientPulseManager;
    Lazy<AppOpsController> mAppOpsController;
    Lazy<AssistManager> mAssistManager;
    Lazy<AsyncSensorManager> mAsyncSensorManager;
    Lazy<AutoHideController> mAutoHideController;
    Lazy<BatteryController> mBatteryController;
    Lazy<Handler> mBgHandler;
    Lazy<Looper> mBgLooper;
    Lazy<BluetoothController> mBluetoothController;
    Lazy<BubbleController> mBubbleController;
    Lazy<CastController> mCastController;
    Lazy<ChannelEditorDialogController> mChannelEditorDialogController;
    Lazy<ClockManager> mClockManager;
    Lazy<ConfigurationController> mConfigurationController;
    Lazy<DarkIconDispatcher> mDarkIconDispatcher;
    Lazy<DataSaverController> mDataSaverController;
    private final ArrayMap<Object, Object> mDependencies = new ArrayMap<>();
    Lazy<DevicePolicyManagerWrapper> mDevicePolicyManagerWrapper;
    Lazy<DeviceProvisionedController> mDeviceProvisionedController;
    Lazy<DisplayMetrics> mDisplayMetrics;
    Lazy<DockManager> mDockManager;
    Lazy<DumpController> mDumpController;
    Lazy<EnhancedEstimates> mEnhancedEstimates;
    Lazy<ExtensionController> mExtensionController;
    Lazy<FalsingManager> mFalsingManager;
    Lazy<FlashlightController> mFlashlightController;
    Lazy<ForegroundServiceController> mForegroundServiceController;
    Lazy<ForegroundServiceNotificationListener> mForegroundServiceNotificationListener;
    Lazy<FragmentService> mFragmentService;
    Lazy<GarbageMonitor> mGarbageMonitor;
    Lazy<HotspotController> mHotspotController;
    Lazy<INotificationManager> mINotificationManager;
    Lazy<IStatusBarService> mIStatusBarService;
    Lazy<IWindowManager> mIWindowManager;
    Lazy<InitController> mInitController;
    Lazy<KeyguardDismissUtil> mKeyguardDismissUtil;
    Lazy<NotificationData.KeyguardEnvironment> mKeyguardEnvironment;
    Lazy<KeyguardMonitor> mKeyguardMonitor;
    Lazy<LeakDetector> mLeakDetector;
    Lazy<String> mLeakReportEmail;
    Lazy<LeakReporter> mLeakReporter;
    Lazy<LightBarController> mLightBarController;
    Lazy<LocalBluetoothManager> mLocalBluetoothManager;
    Lazy<LocationController> mLocationController;
    Lazy<LockscreenAlbumArtController> mLockscreenAlbumArtController;
    Lazy<LockscreenAmbientDisplayController> mLockscreenAmbientDisplayController;
    Lazy<LockscreenAssistIconController> mLockscreenAssistIconController;
    Lazy<LockscreenClockController> mLockscreenClockController;
    Lazy<LockscreenGestureLogger> mLockscreenGestureLogger;
    Lazy<LockscreenLoopsController> mLockscreenLoopsController;
    Lazy<LockscreenSkinningController> mLockscreenSkinningController;
    Lazy<LockscreenStyleCoverController> mLockscreenStyleCoverController;
    Lazy<LockscreenTransparentScrimController> mLockscreenTransparentScrimController;
    Lazy<Handler> mMainHandler;
    Lazy<ManagedProfileController> mManagedProfileController;
    Lazy<MetricsLogger> mMetricsLogger;
    Lazy<NavigationModeController> mNavBarModeController;
    Lazy<NavigationBarController> mNavigationBarController;
    Lazy<NavigationBarLockController> mNavigationBarLockController;
    Lazy<NetworkController> mNetworkController;
    Lazy<NextAlarmController> mNextAlarmController;
    Lazy<NightDisplayListener> mNightDisplayListener;
    Lazy<NotificationAlertingManager> mNotificationAlertingManager;
    Lazy<NotificationBlockingHelperManager> mNotificationBlockingHelperManager;
    Lazy<NotificationEntryManager> mNotificationEntryManager;
    Lazy<NotificationFilter> mNotificationFilter;
    Lazy<NotificationGroupAlertTransferHelper> mNotificationGroupAlertTransferHelper;
    Lazy<NotificationGroupManager> mNotificationGroupManager;
    Lazy<NotificationGutsManager> mNotificationGutsManager;
    Lazy<NotificationInterruptionStateProvider> mNotificationInterruptionStateProvider;
    Lazy<NotificationListener> mNotificationListener;
    Lazy<NotificationLockscreenUserManager> mNotificationLockscreenUserManager;
    Lazy<NotificationLogger> mNotificationLogger;
    Lazy<NotificationMediaManager> mNotificationMediaManager;
    Lazy<NotificationRemoteInputManager> mNotificationRemoteInputManager;
    Lazy<NotificationRemoteInputManager.Callback> mNotificationRemoteInputManagerCallback;
    Lazy<NotificationViewHierarchyManager> mNotificationViewHierarchyManager;
    Lazy<OverviewProxyService> mOverviewProxyService;
    Lazy<PackageManagerWrapper> mPackageManagerWrapper;
    Lazy<PluginDependencyProvider> mPluginDependencyProvider;
    Lazy<PluginManager> mPluginManager;
    private final ArrayMap<Object, LazyDependencyCreator> mProviders = new ArrayMap<>();
    Lazy<RemoteInputQuickSettingsDisabler> mRemoteInputQuickSettingsDisabler;
    Lazy<RotationLockController> mRotationLockController;
    Lazy<ScreenLifecycle> mScreenLifecycle;
    Lazy<SecurityController> mSecurityController;
    Lazy<SensorPrivacyController> mSensorPrivacyController;
    Lazy<SensorPrivacyManager> mSensorPrivacyManager;
    Lazy<ShadeController> mShadeController;
    Lazy<SmartReplyConstants> mSmartReplyConstants;
    Lazy<SmartReplyController> mSmartReplyController;
    Lazy<StatusBarIconController> mStatusBarIconController;
    Lazy<StatusBarStateController> mStatusBarStateController;
    Lazy<StatusBarWindowController> mStatusBarWindowController;
    Lazy<SysuiColorExtractor> mSysuiColorExtractor;
    Lazy<Handler> mTimeTickHandler;
    Lazy<TunablePadding.TunablePaddingService> mTunablePaddingService;
    Lazy<TunerService> mTunerService;
    Lazy<UiOffloadThread> mUiOffloadThread;
    Lazy<UserInfoController> mUserInfoController;
    Lazy<UserSwitcherController> mUserSwitcherController;
    Lazy<VibratorHelper> mVibratorHelper;
    Lazy<VisualStabilityManager> mVisualStabilityManager;
    Lazy<VolumeDialogController> mVolumeDialogController;
    Lazy<WakefulnessLifecycle> mWakefulnessLifecycle;
    Lazy<PowerUI.WarningsUI> mWarningsUI;
    Lazy<ZenModeController> mZenModeController;

    public interface DependencyInjector {
        void createSystemUI(Dependency dependency);
    }

    /* access modifiers changed from: private */
    public interface LazyDependencyCreator<T> {
        T createDependency();
    }

    @Override // com.android.systemui.SystemUI
    public void start() {
        ArrayMap<Object, LazyDependencyCreator> arrayMap = this.mProviders;
        DependencyKey<Handler> dependencyKey = TIME_TICK_HANDLER;
        Lazy<Handler> lazy = this.mTimeTickHandler;
        Objects.requireNonNull(lazy);
        arrayMap.put(dependencyKey, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap2 = this.mProviders;
        DependencyKey<Looper> dependencyKey2 = BG_LOOPER;
        Lazy<Looper> lazy2 = this.mBgLooper;
        Objects.requireNonNull(lazy2);
        arrayMap2.put(dependencyKey2, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap3 = this.mProviders;
        DependencyKey<Handler> dependencyKey3 = BG_HANDLER;
        Lazy<Handler> lazy3 = this.mBgHandler;
        Objects.requireNonNull(lazy3);
        arrayMap3.put(dependencyKey3, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap4 = this.mProviders;
        DependencyKey<Handler> dependencyKey4 = MAIN_HANDLER;
        Lazy<Handler> lazy4 = this.mMainHandler;
        Objects.requireNonNull(lazy4);
        arrayMap4.put(dependencyKey4, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap5 = this.mProviders;
        Lazy<ActivityStarter> lazy5 = this.mActivityStarter;
        Objects.requireNonNull(lazy5);
        arrayMap5.put(ActivityStarter.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap6 = this.mProviders;
        Lazy<ActivityStarterDelegate> lazy6 = this.mActivityStarterDelegate;
        Objects.requireNonNull(lazy6);
        arrayMap6.put(ActivityStarterDelegate.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap7 = this.mProviders;
        Lazy<AsyncSensorManager> lazy7 = this.mAsyncSensorManager;
        Objects.requireNonNull(lazy7);
        arrayMap7.put(AsyncSensorManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap8 = this.mProviders;
        Lazy<BluetoothController> lazy8 = this.mBluetoothController;
        Objects.requireNonNull(lazy8);
        arrayMap8.put(BluetoothController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap9 = this.mProviders;
        Lazy<SensorPrivacyManager> lazy9 = this.mSensorPrivacyManager;
        Objects.requireNonNull(lazy9);
        arrayMap9.put(SensorPrivacyManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap10 = this.mProviders;
        Lazy<LocationController> lazy10 = this.mLocationController;
        Objects.requireNonNull(lazy10);
        arrayMap10.put(LocationController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap11 = this.mProviders;
        Lazy<RotationLockController> lazy11 = this.mRotationLockController;
        Objects.requireNonNull(lazy11);
        arrayMap11.put(RotationLockController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap12 = this.mProviders;
        Lazy<NetworkController> lazy12 = this.mNetworkController;
        Objects.requireNonNull(lazy12);
        arrayMap12.put(NetworkController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap13 = this.mProviders;
        Lazy<ZenModeController> lazy13 = this.mZenModeController;
        Objects.requireNonNull(lazy13);
        arrayMap13.put(ZenModeController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap14 = this.mProviders;
        Lazy<HotspotController> lazy14 = this.mHotspotController;
        Objects.requireNonNull(lazy14);
        arrayMap14.put(HotspotController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap15 = this.mProviders;
        Lazy<CastController> lazy15 = this.mCastController;
        Objects.requireNonNull(lazy15);
        arrayMap15.put(CastController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap16 = this.mProviders;
        Lazy<FlashlightController> lazy16 = this.mFlashlightController;
        Objects.requireNonNull(lazy16);
        arrayMap16.put(FlashlightController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap17 = this.mProviders;
        Lazy<KeyguardMonitor> lazy17 = this.mKeyguardMonitor;
        Objects.requireNonNull(lazy17);
        arrayMap17.put(KeyguardMonitor.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap18 = this.mProviders;
        Lazy<UserSwitcherController> lazy18 = this.mUserSwitcherController;
        Objects.requireNonNull(lazy18);
        arrayMap18.put(UserSwitcherController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap19 = this.mProviders;
        Lazy<UserInfoController> lazy19 = this.mUserInfoController;
        Objects.requireNonNull(lazy19);
        arrayMap19.put(UserInfoController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap20 = this.mProviders;
        Lazy<BatteryController> lazy20 = this.mBatteryController;
        Objects.requireNonNull(lazy20);
        arrayMap20.put(BatteryController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap21 = this.mProviders;
        Lazy<NightDisplayListener> lazy21 = this.mNightDisplayListener;
        Objects.requireNonNull(lazy21);
        arrayMap21.put(NightDisplayListener.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap22 = this.mProviders;
        Lazy<ManagedProfileController> lazy22 = this.mManagedProfileController;
        Objects.requireNonNull(lazy22);
        arrayMap22.put(ManagedProfileController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap23 = this.mProviders;
        Lazy<NextAlarmController> lazy23 = this.mNextAlarmController;
        Objects.requireNonNull(lazy23);
        arrayMap23.put(NextAlarmController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap24 = this.mProviders;
        Lazy<DataSaverController> lazy24 = this.mDataSaverController;
        Objects.requireNonNull(lazy24);
        arrayMap24.put(DataSaverController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap25 = this.mProviders;
        Lazy<AccessibilityController> lazy25 = this.mAccessibilityController;
        Objects.requireNonNull(lazy25);
        arrayMap25.put(AccessibilityController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap26 = this.mProviders;
        Lazy<DeviceProvisionedController> lazy26 = this.mDeviceProvisionedController;
        Objects.requireNonNull(lazy26);
        arrayMap26.put(DeviceProvisionedController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap27 = this.mProviders;
        Lazy<PluginManager> lazy27 = this.mPluginManager;
        Objects.requireNonNull(lazy27);
        arrayMap27.put(PluginManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap28 = this.mProviders;
        Lazy<AssistManager> lazy28 = this.mAssistManager;
        Objects.requireNonNull(lazy28);
        arrayMap28.put(AssistManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap29 = this.mProviders;
        Lazy<SecurityController> lazy29 = this.mSecurityController;
        Objects.requireNonNull(lazy29);
        arrayMap29.put(SecurityController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap30 = this.mProviders;
        Lazy<LeakDetector> lazy30 = this.mLeakDetector;
        Objects.requireNonNull(lazy30);
        arrayMap30.put(LeakDetector.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap31 = this.mProviders;
        DependencyKey<String> dependencyKey5 = LEAK_REPORT_EMAIL;
        Lazy<String> lazy31 = this.mLeakReportEmail;
        Objects.requireNonNull(lazy31);
        arrayMap31.put(dependencyKey5, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap32 = this.mProviders;
        Lazy<LeakReporter> lazy32 = this.mLeakReporter;
        Objects.requireNonNull(lazy32);
        arrayMap32.put(LeakReporter.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap33 = this.mProviders;
        Lazy<GarbageMonitor> lazy33 = this.mGarbageMonitor;
        Objects.requireNonNull(lazy33);
        arrayMap33.put(GarbageMonitor.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap34 = this.mProviders;
        Lazy<TunerService> lazy34 = this.mTunerService;
        Objects.requireNonNull(lazy34);
        arrayMap34.put(TunerService.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap35 = this.mProviders;
        Lazy<StatusBarWindowController> lazy35 = this.mStatusBarWindowController;
        Objects.requireNonNull(lazy35);
        arrayMap35.put(StatusBarWindowController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap36 = this.mProviders;
        Lazy<DarkIconDispatcher> lazy36 = this.mDarkIconDispatcher;
        Objects.requireNonNull(lazy36);
        arrayMap36.put(DarkIconDispatcher.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap37 = this.mProviders;
        Lazy<ConfigurationController> lazy37 = this.mConfigurationController;
        Objects.requireNonNull(lazy37);
        arrayMap37.put(ConfigurationController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap38 = this.mProviders;
        Lazy<StatusBarIconController> lazy38 = this.mStatusBarIconController;
        Objects.requireNonNull(lazy38);
        arrayMap38.put(StatusBarIconController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap39 = this.mProviders;
        Lazy<NavigationBarLockController> lazy39 = this.mNavigationBarLockController;
        Objects.requireNonNull(lazy39);
        arrayMap39.put(NavigationBarLockController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap40 = this.mProviders;
        Lazy<ScreenLifecycle> lazy40 = this.mScreenLifecycle;
        Objects.requireNonNull(lazy40);
        arrayMap40.put(ScreenLifecycle.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap41 = this.mProviders;
        Lazy<WakefulnessLifecycle> lazy41 = this.mWakefulnessLifecycle;
        Objects.requireNonNull(lazy41);
        arrayMap41.put(WakefulnessLifecycle.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap42 = this.mProviders;
        Lazy<FragmentService> lazy42 = this.mFragmentService;
        Objects.requireNonNull(lazy42);
        arrayMap42.put(FragmentService.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap43 = this.mProviders;
        Lazy<ExtensionController> lazy43 = this.mExtensionController;
        Objects.requireNonNull(lazy43);
        arrayMap43.put(ExtensionController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap44 = this.mProviders;
        Lazy<PluginDependencyProvider> lazy44 = this.mPluginDependencyProvider;
        Objects.requireNonNull(lazy44);
        arrayMap44.put(PluginDependencyProvider.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap45 = this.mProviders;
        Lazy<LocalBluetoothManager> lazy45 = this.mLocalBluetoothManager;
        Objects.requireNonNull(lazy45);
        arrayMap45.put(LocalBluetoothManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap46 = this.mProviders;
        Lazy<VolumeDialogController> lazy46 = this.mVolumeDialogController;
        Objects.requireNonNull(lazy46);
        arrayMap46.put(VolumeDialogController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap47 = this.mProviders;
        Lazy<MetricsLogger> lazy47 = this.mMetricsLogger;
        Objects.requireNonNull(lazy47);
        arrayMap47.put(MetricsLogger.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap48 = this.mProviders;
        Lazy<AccessibilityManagerWrapper> lazy48 = this.mAccessibilityManagerWrapper;
        Objects.requireNonNull(lazy48);
        arrayMap48.put(AccessibilityManagerWrapper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap49 = this.mProviders;
        Lazy<SysuiColorExtractor> lazy49 = this.mSysuiColorExtractor;
        Objects.requireNonNull(lazy49);
        arrayMap49.put(SysuiColorExtractor.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap50 = this.mProviders;
        Lazy<TunablePadding.TunablePaddingService> lazy50 = this.mTunablePaddingService;
        Objects.requireNonNull(lazy50);
        arrayMap50.put(TunablePadding.TunablePaddingService.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap51 = this.mProviders;
        Lazy<ForegroundServiceController> lazy51 = this.mForegroundServiceController;
        Objects.requireNonNull(lazy51);
        arrayMap51.put(ForegroundServiceController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap52 = this.mProviders;
        Lazy<UiOffloadThread> lazy52 = this.mUiOffloadThread;
        Objects.requireNonNull(lazy52);
        arrayMap52.put(UiOffloadThread.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap53 = this.mProviders;
        Lazy<PowerUI.WarningsUI> lazy53 = this.mWarningsUI;
        Objects.requireNonNull(lazy53);
        arrayMap53.put(PowerUI.WarningsUI.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap54 = this.mProviders;
        Lazy<LightBarController> lazy54 = this.mLightBarController;
        Objects.requireNonNull(lazy54);
        arrayMap54.put(LightBarController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap55 = this.mProviders;
        Lazy<IWindowManager> lazy55 = this.mIWindowManager;
        Objects.requireNonNull(lazy55);
        arrayMap55.put(IWindowManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap56 = this.mProviders;
        Lazy<OverviewProxyService> lazy56 = this.mOverviewProxyService;
        Objects.requireNonNull(lazy56);
        arrayMap56.put(OverviewProxyService.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap57 = this.mProviders;
        Lazy<NavigationModeController> lazy57 = this.mNavBarModeController;
        Objects.requireNonNull(lazy57);
        arrayMap57.put(NavigationModeController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap58 = this.mProviders;
        Lazy<EnhancedEstimates> lazy58 = this.mEnhancedEstimates;
        Objects.requireNonNull(lazy58);
        arrayMap58.put(EnhancedEstimates.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap59 = this.mProviders;
        Lazy<VibratorHelper> lazy59 = this.mVibratorHelper;
        Objects.requireNonNull(lazy59);
        arrayMap59.put(VibratorHelper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap60 = this.mProviders;
        Lazy<IStatusBarService> lazy60 = this.mIStatusBarService;
        Objects.requireNonNull(lazy60);
        arrayMap60.put(IStatusBarService.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap61 = this.mProviders;
        Lazy<DisplayMetrics> lazy61 = this.mDisplayMetrics;
        Objects.requireNonNull(lazy61);
        arrayMap61.put(DisplayMetrics.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap62 = this.mProviders;
        Lazy<LockscreenGestureLogger> lazy62 = this.mLockscreenGestureLogger;
        Objects.requireNonNull(lazy62);
        arrayMap62.put(LockscreenGestureLogger.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap63 = this.mProviders;
        Lazy<NotificationData.KeyguardEnvironment> lazy63 = this.mKeyguardEnvironment;
        Objects.requireNonNull(lazy63);
        arrayMap63.put(NotificationData.KeyguardEnvironment.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap64 = this.mProviders;
        Lazy<ShadeController> lazy64 = this.mShadeController;
        Objects.requireNonNull(lazy64);
        arrayMap64.put(ShadeController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap65 = this.mProviders;
        Lazy<NotificationRemoteInputManager.Callback> lazy65 = this.mNotificationRemoteInputManagerCallback;
        Objects.requireNonNull(lazy65);
        arrayMap65.put(NotificationRemoteInputManager.Callback.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap66 = this.mProviders;
        Lazy<InitController> lazy66 = this.mInitController;
        Objects.requireNonNull(lazy66);
        arrayMap66.put(InitController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap67 = this.mProviders;
        Lazy<AppOpsController> lazy67 = this.mAppOpsController;
        Objects.requireNonNull(lazy67);
        arrayMap67.put(AppOpsController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap68 = this.mProviders;
        Lazy<NavigationBarController> lazy68 = this.mNavigationBarController;
        Objects.requireNonNull(lazy68);
        arrayMap68.put(NavigationBarController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap69 = this.mProviders;
        Lazy<StatusBarStateController> lazy69 = this.mStatusBarStateController;
        Objects.requireNonNull(lazy69);
        arrayMap69.put(StatusBarStateController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap70 = this.mProviders;
        Lazy<NotificationLockscreenUserManager> lazy70 = this.mNotificationLockscreenUserManager;
        Objects.requireNonNull(lazy70);
        arrayMap70.put(NotificationLockscreenUserManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap71 = this.mProviders;
        Lazy<VisualStabilityManager> lazy71 = this.mVisualStabilityManager;
        Objects.requireNonNull(lazy71);
        arrayMap71.put(VisualStabilityManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap72 = this.mProviders;
        Lazy<NotificationGroupManager> lazy72 = this.mNotificationGroupManager;
        Objects.requireNonNull(lazy72);
        arrayMap72.put(NotificationGroupManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap73 = this.mProviders;
        Lazy<NotificationGroupAlertTransferHelper> lazy73 = this.mNotificationGroupAlertTransferHelper;
        Objects.requireNonNull(lazy73);
        arrayMap73.put(NotificationGroupAlertTransferHelper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap74 = this.mProviders;
        Lazy<NotificationMediaManager> lazy74 = this.mNotificationMediaManager;
        Objects.requireNonNull(lazy74);
        arrayMap74.put(NotificationMediaManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap75 = this.mProviders;
        Lazy<NotificationGutsManager> lazy75 = this.mNotificationGutsManager;
        Objects.requireNonNull(lazy75);
        arrayMap75.put(NotificationGutsManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap76 = this.mProviders;
        Lazy<AmbientPulseManager> lazy76 = this.mAmbientPulseManager;
        Objects.requireNonNull(lazy76);
        arrayMap76.put(AmbientPulseManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap77 = this.mProviders;
        Lazy<NotificationBlockingHelperManager> lazy77 = this.mNotificationBlockingHelperManager;
        Objects.requireNonNull(lazy77);
        arrayMap77.put(NotificationBlockingHelperManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap78 = this.mProviders;
        Lazy<NotificationRemoteInputManager> lazy78 = this.mNotificationRemoteInputManager;
        Objects.requireNonNull(lazy78);
        arrayMap78.put(NotificationRemoteInputManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap79 = this.mProviders;
        Lazy<SmartReplyConstants> lazy79 = this.mSmartReplyConstants;
        Objects.requireNonNull(lazy79);
        arrayMap79.put(SmartReplyConstants.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap80 = this.mProviders;
        Lazy<NotificationListener> lazy80 = this.mNotificationListener;
        Objects.requireNonNull(lazy80);
        arrayMap80.put(NotificationListener.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap81 = this.mProviders;
        Lazy<NotificationLogger> lazy81 = this.mNotificationLogger;
        Objects.requireNonNull(lazy81);
        arrayMap81.put(NotificationLogger.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap82 = this.mProviders;
        Lazy<NotificationViewHierarchyManager> lazy82 = this.mNotificationViewHierarchyManager;
        Objects.requireNonNull(lazy82);
        arrayMap82.put(NotificationViewHierarchyManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap83 = this.mProviders;
        Lazy<NotificationFilter> lazy83 = this.mNotificationFilter;
        Objects.requireNonNull(lazy83);
        arrayMap83.put(NotificationFilter.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap84 = this.mProviders;
        Lazy<NotificationInterruptionStateProvider> lazy84 = this.mNotificationInterruptionStateProvider;
        Objects.requireNonNull(lazy84);
        arrayMap84.put(NotificationInterruptionStateProvider.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap85 = this.mProviders;
        Lazy<KeyguardDismissUtil> lazy85 = this.mKeyguardDismissUtil;
        Objects.requireNonNull(lazy85);
        arrayMap85.put(KeyguardDismissUtil.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap86 = this.mProviders;
        Lazy<SmartReplyController> lazy86 = this.mSmartReplyController;
        Objects.requireNonNull(lazy86);
        arrayMap86.put(SmartReplyController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap87 = this.mProviders;
        Lazy<RemoteInputQuickSettingsDisabler> lazy87 = this.mRemoteInputQuickSettingsDisabler;
        Objects.requireNonNull(lazy87);
        arrayMap87.put(RemoteInputQuickSettingsDisabler.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap88 = this.mProviders;
        Lazy<BubbleController> lazy88 = this.mBubbleController;
        Objects.requireNonNull(lazy88);
        arrayMap88.put(BubbleController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap89 = this.mProviders;
        Lazy<NotificationEntryManager> lazy89 = this.mNotificationEntryManager;
        Objects.requireNonNull(lazy89);
        arrayMap89.put(NotificationEntryManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap90 = this.mProviders;
        Lazy<NotificationAlertingManager> lazy90 = this.mNotificationAlertingManager;
        Objects.requireNonNull(lazy90);
        arrayMap90.put(NotificationAlertingManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap91 = this.mProviders;
        Lazy<ForegroundServiceNotificationListener> lazy91 = this.mForegroundServiceNotificationListener;
        Objects.requireNonNull(lazy91);
        arrayMap91.put(ForegroundServiceNotificationListener.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap92 = this.mProviders;
        Lazy<ClockManager> lazy92 = this.mClockManager;
        Objects.requireNonNull(lazy92);
        arrayMap92.put(ClockManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap93 = this.mProviders;
        Lazy<ActivityManagerWrapper> lazy93 = this.mActivityManagerWrapper;
        Objects.requireNonNull(lazy93);
        arrayMap93.put(ActivityManagerWrapper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap94 = this.mProviders;
        Lazy<DevicePolicyManagerWrapper> lazy94 = this.mDevicePolicyManagerWrapper;
        Objects.requireNonNull(lazy94);
        arrayMap94.put(DevicePolicyManagerWrapper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap95 = this.mProviders;
        Lazy<PackageManagerWrapper> lazy95 = this.mPackageManagerWrapper;
        Objects.requireNonNull(lazy95);
        arrayMap95.put(PackageManagerWrapper.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap96 = this.mProviders;
        Lazy<SensorPrivacyController> lazy96 = this.mSensorPrivacyController;
        Objects.requireNonNull(lazy96);
        arrayMap96.put(SensorPrivacyController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap97 = this.mProviders;
        Lazy<DumpController> lazy97 = this.mDumpController;
        Objects.requireNonNull(lazy97);
        arrayMap97.put(DumpController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap98 = this.mProviders;
        Lazy<DockManager> lazy98 = this.mDockManager;
        Objects.requireNonNull(lazy98);
        arrayMap98.put(DockManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap99 = this.mProviders;
        Lazy<ChannelEditorDialogController> lazy99 = this.mChannelEditorDialogController;
        Objects.requireNonNull(lazy99);
        arrayMap99.put(ChannelEditorDialogController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap100 = this.mProviders;
        Lazy<INotificationManager> lazy100 = this.mINotificationManager;
        Objects.requireNonNull(lazy100);
        arrayMap100.put(INotificationManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap101 = this.mProviders;
        Lazy<FalsingManager> lazy101 = this.mFalsingManager;
        Objects.requireNonNull(lazy101);
        arrayMap101.put(FalsingManager.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap102 = this.mProviders;
        Lazy<LockscreenAlbumArtController> lazy102 = this.mLockscreenAlbumArtController;
        Objects.requireNonNull(lazy102);
        arrayMap102.put(LockscreenAlbumArtController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap103 = this.mProviders;
        Lazy<LockscreenTransparentScrimController> lazy103 = this.mLockscreenTransparentScrimController;
        Objects.requireNonNull(lazy103);
        arrayMap103.put(LockscreenTransparentScrimController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap104 = this.mProviders;
        Lazy<LockscreenClockController> lazy104 = this.mLockscreenClockController;
        Objects.requireNonNull(lazy104);
        arrayMap104.put(LockscreenClockController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap105 = this.mProviders;
        Lazy<LockscreenLoopsController> lazy105 = this.mLockscreenLoopsController;
        Objects.requireNonNull(lazy105);
        arrayMap105.put(LockscreenLoopsController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap106 = this.mProviders;
        Lazy<LockscreenAmbientDisplayController> lazy106 = this.mLockscreenAmbientDisplayController;
        Objects.requireNonNull(lazy106);
        arrayMap106.put(LockscreenAmbientDisplayController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap107 = this.mProviders;
        Lazy<LockscreenAssistIconController> lazy107 = this.mLockscreenAssistIconController;
        Objects.requireNonNull(lazy107);
        arrayMap107.put(LockscreenAssistIconController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap108 = this.mProviders;
        Lazy<LockscreenSkinningController> lazy108 = this.mLockscreenSkinningController;
        Objects.requireNonNull(lazy108);
        arrayMap108.put(LockscreenSkinningController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap109 = this.mProviders;
        Lazy<LockscreenStyleCoverController> lazy109 = this.mLockscreenStyleCoverController;
        Objects.requireNonNull(lazy109);
        arrayMap109.put(LockscreenStyleCoverController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        ArrayMap<Object, LazyDependencyCreator> arrayMap110 = this.mProviders;
        Lazy<AutoHideController> lazy110 = this.mAutoHideController;
        Objects.requireNonNull(lazy110);
        arrayMap110.put(AutoHideController.class, new LazyDependencyCreator() {
            /* class com.android.systemui.$$Lambda$VsMsjQwuYhfrxzUr7AqZvcfoH4 */

            @Override // com.android.systemui.Dependency.LazyDependencyCreator
            public final Object createDependency() {
                return Lazy.this.get();
            }
        });
        sDependency = this;
    }

    @Override // com.android.systemui.SystemUI
    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        getDependency(DumpController.class);
        String lowerCase = (strArr == null || strArr.length <= 1) ? null : strArr[1].toLowerCase();
        if (lowerCase != null) {
            printWriter.println("Dumping controller=" + lowerCase + ":");
        } else {
            printWriter.println("Dumping existing controllers:");
        }
        this.mDependencies.values().stream().filter(new Predicate(lowerCase) {
            /* class com.android.systemui.$$Lambda$Dependency$nA5ayadwqBW4bgzyvl5eaXT_aUY */
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Dependency.lambda$dump$0(this.f$0, obj);
            }
        }).forEach(new Consumer(fileDescriptor, printWriter, strArr) {
            /* class com.android.systemui.$$Lambda$Dependency$txwQ8DNTPzffiYtSV5jsVTL0RAU */
            private final /* synthetic */ FileDescriptor f$0;
            private final /* synthetic */ PrintWriter f$1;
            private final /* synthetic */ String[] f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Dumpable) obj).dump(this.f$0, this.f$1, this.f$2);
            }
        });
    }

    static /* synthetic */ boolean lambda$dump$0(String str, Object obj) {
        return (obj instanceof Dumpable) && (str == null || obj.getClass().getName().toLowerCase().endsWith(str));
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.SystemUI
    public synchronized void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mDependencies.values().stream().filter($$Lambda$Dependency$G0eGYzBRIeN1O3Awe1MBmBXPiQ.INSTANCE).forEach(new Consumer(configuration) {
            /* class com.android.systemui.$$Lambda$Dependency$_u0ScUAjkiveEBWvXdf73QkCm8 */
            private final /* synthetic */ Configuration f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ConfigurationChangedReceiver) obj).onConfigurationChanged(this.f$0);
            }
        });
    }

    static /* synthetic */ boolean lambda$onConfigurationChanged$2(Object obj) {
        return obj instanceof ConfigurationChangedReceiver;
    }

    /* access modifiers changed from: protected */
    public final <T> T getDependency(Class<T> cls) {
        return (T) getDependencyInner(cls);
    }

    /* access modifiers changed from: protected */
    public final <T> T getDependency(DependencyKey<T> dependencyKey) {
        return (T) getDependencyInner(dependencyKey);
    }

    private synchronized <T> T getDependencyInner(Object obj) {
        T t;
        t = (T) this.mDependencies.get(obj);
        if (t == null) {
            t = (T) createDependency(obj);
            this.mDependencies.put(obj, t);
        }
        return t;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public <T> T createDependency(Object obj) {
        Preconditions.checkArgument((obj instanceof DependencyKey) || (obj instanceof Class));
        LazyDependencyCreator lazyDependencyCreator = this.mProviders.get(obj);
        if (lazyDependencyCreator != null) {
            return (T) lazyDependencyCreator.createDependency();
        }
        throw new IllegalArgumentException("Unsupported dependency " + obj + ". " + this.mProviders.size() + " providers known.");
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.util.function.Consumer<T> */
    /* JADX WARN: Multi-variable type inference failed */
    private <T> void destroyDependency(Class<T> cls, Consumer<T> consumer) {
        Object remove = this.mDependencies.remove(cls);
        if (remove != null && consumer != 0) {
            consumer.accept(remove);
        }
    }

    public static void initDependencies(Context context) {
        if (sDependency == null) {
            Dependency dependency = new Dependency();
            SystemUIFactory.getInstance().getRootComponent().createDependency().createSystemUI(dependency);
            dependency.mContext = context;
            dependency.mComponents = new HashMap();
            dependency.start();
        }
    }

    public static void clearDependencies() {
        sDependency = null;
    }

    public static <T> void destroy(Class<T> cls, Consumer<T> consumer) {
        sDependency.destroyDependency(cls, consumer);
    }

    @Deprecated
    public static <T> T get(Class<T> cls) {
        return (T) sDependency.getDependency(cls);
    }

    @Deprecated
    public static <T> T get(DependencyKey<T> dependencyKey) {
        return (T) sDependency.getDependency(dependencyKey);
    }

    public static final class DependencyKey<V> {
        private final String mDisplayName;

        public DependencyKey(String str) {
            this.mDisplayName = str;
        }

        public String toString() {
            return this.mDisplayName;
        }
    }

    public static class DependencyCreator implements SystemUI.Injector {
        public SystemUI apply(Context context) {
            Dependency dependency = new Dependency();
            SystemUIFactory.getInstance().getRootComponent().createDependency().createSystemUI(dependency);
            return dependency;
        }
    }
}
