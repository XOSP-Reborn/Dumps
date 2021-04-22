package com.android.systemui;

import android.app.INotificationManager;
import android.hardware.SensorPrivacyManager;
import android.hardware.display.NightDisplayListener;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.IWindowManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.keyguard.KeyguardClockSwitch;
import com.android.keyguard.KeyguardMessageArea;
import com.android.keyguard.KeyguardSliceView;
import com.android.keyguard.clock.ClockManager;
import com.android.keyguard.clock.ClockManager_Factory;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.appops.AppOpsControllerImpl;
import com.android.systemui.appops.AppOpsControllerImpl_Factory;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.bubbles.BubbleController_Factory;
import com.android.systemui.bubbles.BubbleData;
import com.android.systemui.bubbles.BubbleData_Factory;
import com.android.systemui.classifier.FalsingManagerProxy;
import com.android.systemui.classifier.FalsingManagerProxy_Factory;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.colorextraction.SysuiColorExtractor_Factory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.fragments.FragmentService_Factory;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle_Factory;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle_Factory;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.PluginDependencyProvider_Factory;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.power.PowerNotificationWarnings;
import com.android.systemui.power.PowerNotificationWarnings_Factory;
import com.android.systemui.qs.AutoAddTracker_Factory;
import com.android.systemui.qs.QSCarrierGroup;
import com.android.systemui.qs.QSFooterImpl;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.QSTileHost_Factory;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.qs.QuickStatusBarHeader;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.qs.tileimpl.QSFactoryImpl_Factory;
import com.android.systemui.qs.tiles.AirplaneModeTile_Factory;
import com.android.systemui.qs.tiles.BatterySaverTile_Factory;
import com.android.systemui.qs.tiles.BluetoothTile_Factory;
import com.android.systemui.qs.tiles.CastTile_Factory;
import com.android.systemui.qs.tiles.CellularTile_Factory;
import com.android.systemui.qs.tiles.ColorInversionTile_Factory;
import com.android.systemui.qs.tiles.DataSaverTile_Factory;
import com.android.systemui.qs.tiles.DndTile_Factory;
import com.android.systemui.qs.tiles.FlashlightTile_Factory;
import com.android.systemui.qs.tiles.HotspotTile_Factory;
import com.android.systemui.qs.tiles.LocationTile_Factory;
import com.android.systemui.qs.tiles.NfcTile_Factory;
import com.android.systemui.qs.tiles.NightDisplayTile_Factory;
import com.android.systemui.qs.tiles.RotationLockTile_Factory;
import com.android.systemui.qs.tiles.UiModeNightTile_Factory;
import com.android.systemui.qs.tiles.UserTile_Factory;
import com.android.systemui.qs.tiles.WifiTile_Factory;
import com.android.systemui.qs.tiles.WorkModeTile_Factory;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService_Factory;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.DevicePolicyManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.AmbientPulseManager_Factory;
import com.android.systemui.statusbar.MediaArtworkProcessor;
import com.android.systemui.statusbar.MediaArtworkProcessor_Factory;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationMediaManager_Factory;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager_Factory;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.NotificationViewHierarchyManager_Factory;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.PulseExpansionHandler_Factory;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.SmartReplyController_Factory;
import com.android.systemui.statusbar.StatusBarStateControllerImpl;
import com.android.systemui.statusbar.StatusBarStateControllerImpl_Factory;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.VibratorHelper_Factory;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController_Factory;
import com.android.systemui.statusbar.notification.NotificationAlertingManager;
import com.android.systemui.statusbar.notification.NotificationAlertingManager_Factory;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationFilter;
import com.android.systemui.statusbar.notification.NotificationFilter_Factory;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator_Factory;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.VisualStabilityManager_Factory;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.logging.NotificationLogger_ExpansionStateLogger_Factory;
import com.android.systemui.statusbar.notification.logging.NotificationLogger_Factory;
import com.android.systemui.statusbar.notification.row.ChannelEditorDialogController;
import com.android.systemui.statusbar.notification.row.ChannelEditorDialogController_Factory;
import com.android.systemui.statusbar.notification.row.NotificationBlockingHelperManager;
import com.android.systemui.statusbar.notification.row.NotificationBlockingHelperManager_Factory;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager_Factory;
import com.android.systemui.statusbar.notification.stack.NotificationRoundnessManager_Factory;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout_Factory;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.AutoTileManager_Factory;
import com.android.systemui.statusbar.phone.DarkIconDispatcherImpl;
import com.android.systemui.statusbar.phone.DarkIconDispatcherImpl_Factory;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil;
import com.android.systemui.statusbar.phone.KeyguardDismissUtil_Factory;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LightBarController_Factory;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger_Factory;
import com.android.systemui.statusbar.phone.ManagedProfileControllerImpl;
import com.android.systemui.statusbar.phone.ManagedProfileControllerImpl_Factory;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarLockControllerImpl;
import com.android.systemui.statusbar.phone.NavigationBarLockControllerImpl_Factory;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NavigationModeController_Factory;
import com.android.systemui.statusbar.phone.NotificationGroupAlertTransferHelper;
import com.android.systemui.statusbar.phone.NotificationGroupAlertTransferHelper_Factory;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager_Factory;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarIconControllerImpl;
import com.android.systemui.statusbar.phone.StatusBarIconControllerImpl_Factory;
import com.android.systemui.statusbar.phone.StatusBarRemoteInputCallback;
import com.android.systemui.statusbar.phone.StatusBarRemoteInputCallback_Factory;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.phone.StatusBarWindowController_Factory;
import com.android.systemui.statusbar.phone.StatusBar_MembersInjector;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.AccessibilityController_Factory;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper_Factory;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.android.systemui.statusbar.policy.BatteryControllerImpl_Factory;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl_Factory;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.CastControllerImpl_Factory;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl_Factory;
import com.android.systemui.statusbar.policy.ExtensionControllerImpl;
import com.android.systemui.statusbar.policy.ExtensionControllerImpl_Factory;
import com.android.systemui.statusbar.policy.FlashlightControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightControllerImpl_Factory;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.HotspotControllerImpl_Factory;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl_Factory;
import com.android.systemui.statusbar.policy.LocationControllerImpl;
import com.android.systemui.statusbar.policy.LocationControllerImpl_Factory;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.NetworkControllerImpl_Factory;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl_Factory;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler_Factory;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl_Factory;
import com.android.systemui.statusbar.policy.SecurityControllerImpl;
import com.android.systemui.statusbar.policy.SecurityControllerImpl_Factory;
import com.android.systemui.statusbar.policy.SensorPrivacyControllerImpl;
import com.android.systemui.statusbar.policy.SensorPrivacyControllerImpl_Factory;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.statusbar.policy.SmartReplyConstants_Factory;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl_Factory;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.UserSwitcherController_Factory;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl_Factory;
import com.android.systemui.tuner.TunablePadding;
import com.android.systemui.tuner.TunablePadding_TunablePaddingService_Factory;
import com.android.systemui.tuner.TunerServiceImpl;
import com.android.systemui.tuner.TunerServiceImpl_Factory;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.AsyncSensorManager_Factory;
import com.android.systemui.util.InjectionInflationController;
import com.android.systemui.util.InjectionInflationController_Factory;
import com.android.systemui.util.InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory;
import com.android.systemui.util.InjectionInflationController_ViewAttributeProvider_ProvideContextFactory;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.util.leak.GarbageMonitor_Factory;
import com.android.systemui.util.leak.GarbageMonitor_MemoryTile_Factory;
import com.android.systemui.util.leak.LeakDetector;
import com.android.systemui.util.leak.LeakReporter;
import com.android.systemui.util.leak.LeakReporter_Factory;
import com.android.systemui.volume.VolumeDialogControllerImpl;
import com.android.systemui.volume.VolumeDialogControllerImpl_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenAlbumArtController;
import com.sonymobile.systemui.lockscreen.LockscreenAlbumArtController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController;
import com.sonymobile.systemui.lockscreen.LockscreenAmbientDisplayController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenClockController;
import com.sonymobile.systemui.lockscreen.LockscreenClockController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController;
import com.sonymobile.systemui.lockscreen.LockscreenLoopsController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController_Factory;
import com.sonymobile.systemui.lockscreen.LockscreenTransparentScrimController;
import com.sonymobile.systemui.lockscreen.LockscreenTransparentScrimController_Factory;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController_Factory;
import dagger.internal.DelegateFactory;
import dagger.internal.DoubleCheck;
import dagger.internal.InstanceFactory;
import dagger.internal.Preconditions;
import javax.inject.Provider;

public final class DaggerSystemUIFactory_SystemUIRootComponent implements SystemUIFactory.SystemUIRootComponent {
    private Provider<AccessibilityController> accessibilityControllerProvider;
    private Provider<AccessibilityManagerWrapper> accessibilityManagerWrapperProvider;
    private Provider<ActivityStarterDelegate> activityStarterDelegateProvider;
    private AirplaneModeTile_Factory airplaneModeTileProvider;
    private Provider<AmbientPulseManager> ambientPulseManagerProvider;
    private Provider<AppOpsControllerImpl> appOpsControllerImplProvider;
    private Provider<AsyncSensorManager> asyncSensorManagerProvider;
    private AutoAddTracker_Factory autoAddTrackerProvider;
    private AutoTileManager_Factory autoTileManagerProvider;
    private Provider<BatteryControllerImpl> batteryControllerImplProvider;
    private BatterySaverTile_Factory batterySaverTileProvider;
    private Provider<BluetoothControllerImpl> bluetoothControllerImplProvider;
    private BluetoothTile_Factory bluetoothTileProvider;
    private Provider<BubbleController> bubbleControllerProvider;
    private Provider<BubbleData> bubbleDataProvider;
    private Provider<CastControllerImpl> castControllerImplProvider;
    private CastTile_Factory castTileProvider;
    private CellularTile_Factory cellularTileProvider;
    private Provider<ChannelEditorDialogController> channelEditorDialogControllerProvider;
    private Provider<ClockManager> clockManagerProvider;
    private ColorInversionTile_Factory colorInversionTileProvider;
    private SystemUIFactory.ContextHolder contextHolder;
    private Provider<DarkIconDispatcherImpl> darkIconDispatcherImplProvider;
    private DataSaverTile_Factory dataSaverTileProvider;
    private Provider<DeviceProvisionedControllerImpl> deviceProvisionedControllerImplProvider;
    private DndTile_Factory dndTileProvider;
    private Provider<DumpController> dumpControllerProvider;
    private Provider<DynamicPrivacyController> dynamicPrivacyControllerProvider;
    private NotificationLogger_ExpansionStateLogger_Factory expansionStateLoggerProvider;
    private Provider<ExtensionControllerImpl> extensionControllerImplProvider;
    private Provider<FalsingManagerProxy> falsingManagerProxyProvider;
    private Provider<FlashlightControllerImpl> flashlightControllerImplProvider;
    private FlashlightTile_Factory flashlightTileProvider;
    private Provider<ForegroundServiceController> foregroundServiceControllerProvider;
    private Provider<ForegroundServiceNotificationListener> foregroundServiceNotificationListenerProvider;
    private Provider<FragmentService> fragmentServiceProvider;
    private Provider<GarbageMonitor> garbageMonitorProvider;
    private Provider<HotspotControllerImpl> hotspotControllerImplProvider;
    private HotspotTile_Factory hotspotTileProvider;
    private Provider<InitController> initControllerProvider;
    private Provider<InjectionInflationController> injectionInflationControllerProvider;
    private Provider<KeyguardDismissUtil> keyguardDismissUtilProvider;
    private Provider<KeyguardMonitorImpl> keyguardMonitorImplProvider;
    private Provider<LeakReporter> leakReporterProvider;
    private Provider<LightBarController> lightBarControllerProvider;
    private Provider<LocationControllerImpl> locationControllerImplProvider;
    private LocationTile_Factory locationTileProvider;
    private Provider<LockscreenAlbumArtController> lockscreenAlbumArtControllerProvider;
    private Provider<LockscreenAmbientDisplayController> lockscreenAmbientDisplayControllerProvider;
    private Provider<LockscreenAssistIconController> lockscreenAssistIconControllerProvider;
    private Provider<LockscreenClockController> lockscreenClockControllerProvider;
    private Provider<LockscreenGestureLogger> lockscreenGestureLoggerProvider;
    private Provider<LockscreenLoopsController> lockscreenLoopsControllerProvider;
    private Provider<LockscreenSkinningController> lockscreenSkinningControllerProvider;
    private Provider<LockscreenStyleCoverController> lockscreenStyleCoverControllerProvider;
    private Provider<LockscreenTransparentScrimController> lockscreenTransparentScrimControllerProvider;
    private Provider<ManagedProfileControllerImpl> managedProfileControllerImplProvider;
    private Provider<MediaArtworkProcessor> mediaArtworkProcessorProvider;
    private GarbageMonitor_MemoryTile_Factory memoryTileProvider;
    private Provider<NavigationBarLockControllerImpl> navigationBarLockControllerImplProvider;
    private Provider<NavigationModeController> navigationModeControllerProvider;
    private Provider<NetworkControllerImpl> networkControllerImplProvider;
    private Provider<NextAlarmControllerImpl> nextAlarmControllerImplProvider;
    private NfcTile_Factory nfcTileProvider;
    private NightDisplayTile_Factory nightDisplayTileProvider;
    private Provider<NotificationAlertingManager> notificationAlertingManagerProvider;
    private Provider<NotificationBlockingHelperManager> notificationBlockingHelperManagerProvider;
    private Provider<NotificationFilter> notificationFilterProvider;
    private Provider<NotificationGroupAlertTransferHelper> notificationGroupAlertTransferHelperProvider;
    private Provider<NotificationGroupManager> notificationGroupManagerProvider;
    private Provider<NotificationGutsManager> notificationGutsManagerProvider;
    private Provider<NotificationLogger> notificationLoggerProvider;
    private Provider<NotificationMediaManager> notificationMediaManagerProvider;
    private Provider<NotificationRemoteInputManager> notificationRemoteInputManagerProvider;
    private Provider notificationRoundnessManagerProvider;
    private Provider<NotificationViewHierarchyManager> notificationViewHierarchyManagerProvider;
    private Provider<NotificationWakeUpCoordinator> notificationWakeUpCoordinatorProvider;
    private Provider<OverviewProxyService> overviewProxyServiceProvider;
    private Provider<PluginDependencyProvider> pluginDependencyProvider;
    private Provider<PowerNotificationWarnings> powerNotificationWarningsProvider;
    private Provider<ActivityManagerWrapper> provideActivityManagerWrapperProvider;
    private Provider<Boolean> provideAllowNotificationLongPressProvider;
    private Provider<AssistManager> provideAssistManagerProvider;
    private Provider<AutoHideController> provideAutoHideControllerProvider;
    private Provider<Handler> provideBgHandlerProvider;
    private Provider<Looper> provideBgLooperProvider;
    private Provider<ConfigurationController> provideConfigurationControllerProvider;
    private SystemUIFactory_ContextHolder_ProvideContextFactory provideContextProvider;
    private Provider<DataSaverController> provideDataSaverControllerProvider;
    private Provider<DevicePolicyManagerWrapper> provideDevicePolicyManagerWrapperProvider;
    private Provider<DisplayMetrics> provideDisplayMetricsProvider;
    private Provider<DockManager> provideDockManagerProvider;
    private Provider<EnhancedEstimates> provideEnhancedEstimatesProvider;
    private Provider<Handler> provideHandlerProvider;
    private Provider<INotificationManager> provideINotificationManagerProvider;
    private Provider<IStatusBarService> provideIStatusBarServiceProvider;
    private Provider<IWindowManager> provideIWindowManagerProvider;
    private Provider<NotificationData.KeyguardEnvironment> provideKeyguardEnvironmentProvider;
    private Provider<LeakDetector> provideLeakDetectorProvider;
    private Provider<String> provideLeakReportEmailProvider;
    private Provider<LocalBluetoothManager> provideLocalBluetoothControllerProvider;
    private Provider<Handler> provideMainHandlerProvider;
    private Provider<MetricsLogger> provideMetricsLoggerProvider;
    private Provider<NavigationBarController> provideNavigationBarControllerProvider;
    private Provider<NightDisplayListener> provideNightDisplayListenerProvider;
    private Provider<NotificationEntryManager> provideNotificationEntryManagerProvider;
    private Provider<NotificationInterruptionStateProvider> provideNotificationInterruptionStateProvider;
    private Provider<NotificationListener> provideNotificationListenerProvider;
    private Provider<NotificationLockscreenUserManager> provideNotificationLockscreenUserManagerProvider;
    private Provider<PackageManagerWrapper> providePackageManagerWrapperProvider;
    private Provider<PluginManager> providePluginManagerProvider;
    private Provider<SensorPrivacyManager> provideSensorPrivacyManagerProvider;
    private Provider<ShadeController> provideShadeControllerProvider;
    private Provider<PulseExpansionHandler> pulseExpansionHandlerProvider;
    private Provider<QSFactoryImpl> qSFactoryImplProvider;
    private Provider<QSTileHost> qSTileHostProvider;
    private Provider<RemoteInputQuickSettingsDisabler> remoteInputQuickSettingsDisablerProvider;
    private Provider<RotationLockControllerImpl> rotationLockControllerImplProvider;
    private RotationLockTile_Factory rotationLockTileProvider;
    private Provider<ScreenLifecycle> screenLifecycleProvider;
    private Provider<SecurityControllerImpl> securityControllerImplProvider;
    private Provider<SensorPrivacyControllerImpl> sensorPrivacyControllerImplProvider;
    private Provider<SmartReplyConstants> smartReplyConstantsProvider;
    private Provider<SmartReplyController> smartReplyControllerProvider;
    private Provider<StatusBarIconControllerImpl> statusBarIconControllerImplProvider;
    private Provider<StatusBarRemoteInputCallback> statusBarRemoteInputCallbackProvider;
    private Provider<StatusBarStateControllerImpl> statusBarStateControllerImplProvider;
    private Provider<StatusBarWindowController> statusBarWindowControllerProvider;
    private Provider<SystemUIFactory.SystemUIRootComponent> systemUIRootComponentProvider;
    private Provider<SysuiColorExtractor> sysuiColorExtractorProvider;
    private Provider<TunablePadding.TunablePaddingService> tunablePaddingServiceProvider;
    private Provider<TunerServiceImpl> tunerServiceImplProvider;
    private UiModeNightTile_Factory uiModeNightTileProvider;
    private Provider<UiOffloadThread> uiOffloadThreadProvider;
    private Provider<UserInfoControllerImpl> userInfoControllerImplProvider;
    private Provider<UserSwitcherController> userSwitcherControllerProvider;
    private UserTile_Factory userTileProvider;
    private Provider<VibratorHelper> vibratorHelperProvider;
    private Provider<VisualStabilityManager> visualStabilityManagerProvider;
    private Provider<VolumeDialogControllerImpl> volumeDialogControllerImplProvider;
    private Provider<WakefulnessLifecycle> wakefulnessLifecycleProvider;
    private WifiTile_Factory wifiTileProvider;
    private WorkModeTile_Factory workModeTileProvider;
    private Provider<ZenModeControllerImpl> zenModeControllerImplProvider;

    private DaggerSystemUIFactory_SystemUIRootComponent(Builder builder) {
        initialize(builder);
        initialize2(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    private void initialize(Builder builder) {
        this.provideContextProvider = SystemUIFactory_ContextHolder_ProvideContextFactory.create(builder.contextHolder);
        this.provideBgLooperProvider = DoubleCheck.provider(DependencyProvider_ProvideBgLooperFactory.create(builder.dependencyProvider));
        this.provideLeakDetectorProvider = DoubleCheck.provider(DependencyProvider_ProvideLeakDetectorFactory.create(builder.dependencyProvider));
        this.provideLeakReportEmailProvider = DoubleCheck.provider(SystemUIFactory_ProvideLeakReportEmailFactory.create(builder.systemUIFactory));
        this.leakReporterProvider = DoubleCheck.provider(LeakReporter_Factory.create(this.provideContextProvider, this.provideLeakDetectorProvider, this.provideLeakReportEmailProvider));
        this.garbageMonitorProvider = DoubleCheck.provider(GarbageMonitor_Factory.create(this.provideContextProvider, this.provideBgLooperProvider, this.provideLeakDetectorProvider, this.leakReporterProvider));
        this.activityStarterDelegateProvider = DoubleCheck.provider(ActivityStarterDelegate_Factory.create());
        this.providePluginManagerProvider = DoubleCheck.provider(DependencyProvider_ProvidePluginManagerFactory.create(builder.dependencyProvider, this.provideContextProvider));
        this.asyncSensorManagerProvider = DoubleCheck.provider(AsyncSensorManager_Factory.create(this.provideContextProvider, this.providePluginManagerProvider));
        this.provideBgHandlerProvider = DoubleCheck.provider(DependencyProvider_ProvideBgHandlerFactory.create(builder.dependencyProvider, this.provideBgLooperProvider));
        this.provideLocalBluetoothControllerProvider = DoubleCheck.provider(DependencyProvider_ProvideLocalBluetoothControllerFactory.create(builder.dependencyProvider, this.provideContextProvider, this.provideBgHandlerProvider));
        this.bluetoothControllerImplProvider = DoubleCheck.provider(BluetoothControllerImpl_Factory.create(this.provideContextProvider, this.provideBgLooperProvider, this.provideLocalBluetoothControllerProvider));
        this.locationControllerImplProvider = DoubleCheck.provider(LocationControllerImpl_Factory.create(this.provideContextProvider, this.provideBgLooperProvider));
        this.rotationLockControllerImplProvider = DoubleCheck.provider(RotationLockControllerImpl_Factory.create(this.provideContextProvider));
        this.provideMainHandlerProvider = DoubleCheck.provider(DependencyProvider_ProvideMainHandlerFactory.create(builder.dependencyProvider));
        this.deviceProvisionedControllerImplProvider = DoubleCheck.provider(DeviceProvisionedControllerImpl_Factory.create(this.provideContextProvider, this.provideMainHandlerProvider));
        this.networkControllerImplProvider = DoubleCheck.provider(NetworkControllerImpl_Factory.create(this.provideContextProvider, this.provideBgLooperProvider, this.deviceProvisionedControllerImplProvider));
        this.zenModeControllerImplProvider = DoubleCheck.provider(ZenModeControllerImpl_Factory.create(this.provideContextProvider, this.provideMainHandlerProvider));
        this.hotspotControllerImplProvider = DoubleCheck.provider(HotspotControllerImpl_Factory.create(this.provideContextProvider, this.provideMainHandlerProvider));
        this.castControllerImplProvider = DoubleCheck.provider(CastControllerImpl_Factory.create(this.provideContextProvider));
        this.flashlightControllerImplProvider = DoubleCheck.provider(FlashlightControllerImpl_Factory.create(this.provideContextProvider));
        this.keyguardMonitorImplProvider = DoubleCheck.provider(KeyguardMonitorImpl_Factory.create(this.provideContextProvider));
        this.userSwitcherControllerProvider = DoubleCheck.provider(UserSwitcherController_Factory.create(this.provideContextProvider, this.keyguardMonitorImplProvider, this.provideMainHandlerProvider, this.activityStarterDelegateProvider));
        this.userInfoControllerImplProvider = DoubleCheck.provider(UserInfoControllerImpl_Factory.create(this.provideContextProvider));
        this.provideEnhancedEstimatesProvider = DoubleCheck.provider(SystemUIFactory_ProvideEnhancedEstimatesFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.batteryControllerImplProvider = DoubleCheck.provider(BatteryControllerImpl_Factory.create(this.provideContextProvider, this.provideEnhancedEstimatesProvider));
        this.provideNightDisplayListenerProvider = DoubleCheck.provider(DependencyProvider_ProvideNightDisplayListenerFactory.create(builder.dependencyProvider, this.provideContextProvider, this.provideBgHandlerProvider));
        this.managedProfileControllerImplProvider = DoubleCheck.provider(ManagedProfileControllerImpl_Factory.create(this.provideContextProvider));
        this.nextAlarmControllerImplProvider = DoubleCheck.provider(NextAlarmControllerImpl_Factory.create(this.provideContextProvider));
        this.provideDataSaverControllerProvider = DoubleCheck.provider(DependencyProvider_ProvideDataSaverControllerFactory.create(builder.dependencyProvider, this.networkControllerImplProvider));
        this.accessibilityControllerProvider = DoubleCheck.provider(AccessibilityController_Factory.create(this.provideContextProvider));
        this.provideAssistManagerProvider = DoubleCheck.provider(SystemUIFactory_ProvideAssistManagerFactory.create(builder.systemUIFactory, this.deviceProvisionedControllerImplProvider, this.provideContextProvider));
        this.securityControllerImplProvider = DoubleCheck.provider(SecurityControllerImpl_Factory.create(this.provideContextProvider, this.provideBgHandlerProvider));
        this.tunerServiceImplProvider = DoubleCheck.provider(TunerServiceImpl_Factory.create(this.provideContextProvider, this.provideBgHandlerProvider, this.provideLeakDetectorProvider));
        this.statusBarWindowControllerProvider = DoubleCheck.provider(StatusBarWindowController_Factory.create(this.provideContextProvider));
        this.darkIconDispatcherImplProvider = DoubleCheck.provider(DarkIconDispatcherImpl_Factory.create(this.provideContextProvider));
        this.provideConfigurationControllerProvider = DoubleCheck.provider(DependencyProvider_ProvideConfigurationControllerFactory.create(builder.dependencyProvider, this.provideContextProvider));
        this.statusBarIconControllerImplProvider = DoubleCheck.provider(StatusBarIconControllerImpl_Factory.create(this.provideContextProvider));
        this.navigationBarLockControllerImplProvider = DoubleCheck.provider(NavigationBarLockControllerImpl_Factory.create(this.provideContextProvider));
        this.screenLifecycleProvider = DoubleCheck.provider(ScreenLifecycle_Factory.create());
        this.wakefulnessLifecycleProvider = DoubleCheck.provider(WakefulnessLifecycle_Factory.create());
        this.systemUIRootComponentProvider = InstanceFactory.create(this);
        this.fragmentServiceProvider = DoubleCheck.provider(FragmentService_Factory.create(this.systemUIRootComponentProvider));
        this.extensionControllerImplProvider = DoubleCheck.provider(ExtensionControllerImpl_Factory.create(this.provideContextProvider, this.provideLeakDetectorProvider, this.providePluginManagerProvider, this.tunerServiceImplProvider, this.provideConfigurationControllerProvider));
        this.pluginDependencyProvider = DoubleCheck.provider(PluginDependencyProvider_Factory.create(this.providePluginManagerProvider));
        this.volumeDialogControllerImplProvider = DoubleCheck.provider(VolumeDialogControllerImpl_Factory.create(this.provideContextProvider));
        this.provideMetricsLoggerProvider = DoubleCheck.provider(DependencyProvider_ProvideMetricsLoggerFactory.create(builder.dependencyProvider));
        this.accessibilityManagerWrapperProvider = DoubleCheck.provider(AccessibilityManagerWrapper_Factory.create(this.provideContextProvider));
        this.sysuiColorExtractorProvider = DoubleCheck.provider(SysuiColorExtractor_Factory.create(this.provideContextProvider, this.provideConfigurationControllerProvider));
        this.tunablePaddingServiceProvider = DoubleCheck.provider(TunablePadding_TunablePaddingService_Factory.create(this.tunerServiceImplProvider));
        this.foregroundServiceControllerProvider = DoubleCheck.provider(ForegroundServiceController_Factory.create());
        this.uiOffloadThreadProvider = DoubleCheck.provider(UiOffloadThread_Factory.create());
        this.powerNotificationWarningsProvider = DoubleCheck.provider(PowerNotificationWarnings_Factory.create(this.provideContextProvider, this.activityStarterDelegateProvider));
        this.lightBarControllerProvider = DoubleCheck.provider(LightBarController_Factory.create(this.provideContextProvider, this.darkIconDispatcherImplProvider, this.batteryControllerImplProvider));
        this.provideIWindowManagerProvider = DoubleCheck.provider(DependencyProvider_ProvideIWindowManagerFactory.create(builder.dependencyProvider));
        this.provideNavigationBarControllerProvider = DoubleCheck.provider(DependencyProvider_ProvideNavigationBarControllerFactory.create(builder.dependencyProvider, this.provideContextProvider, this.provideMainHandlerProvider));
        this.navigationModeControllerProvider = DoubleCheck.provider(NavigationModeController_Factory.create(this.provideContextProvider, this.deviceProvisionedControllerImplProvider, this.uiOffloadThreadProvider));
        this.overviewProxyServiceProvider = DoubleCheck.provider(OverviewProxyService_Factory.create(this.provideContextProvider, this.deviceProvisionedControllerImplProvider, this.provideNavigationBarControllerProvider, this.navigationModeControllerProvider, this.statusBarWindowControllerProvider));
        this.vibratorHelperProvider = DoubleCheck.provider(VibratorHelper_Factory.create(this.provideContextProvider));
        this.provideIStatusBarServiceProvider = DoubleCheck.provider(DependencyProvider_ProvideIStatusBarServiceFactory.create(builder.dependencyProvider));
        this.provideDisplayMetricsProvider = DoubleCheck.provider(DependencyProvider_ProvideDisplayMetricsFactory.create(builder.dependencyProvider));
        this.lockscreenGestureLoggerProvider = DoubleCheck.provider(LockscreenGestureLogger_Factory.create());
        this.provideKeyguardEnvironmentProvider = DoubleCheck.provider(SystemUIFactory_ProvideKeyguardEnvironmentFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.provideShadeControllerProvider = DoubleCheck.provider(SystemUIFactory_ProvideShadeControllerFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.notificationGroupManagerProvider = DoubleCheck.provider(NotificationGroupManager_Factory.create());
        this.statusBarRemoteInputCallbackProvider = DoubleCheck.provider(StatusBarRemoteInputCallback_Factory.create(this.provideContextProvider, this.notificationGroupManagerProvider));
        this.initControllerProvider = DoubleCheck.provider(InitController_Factory.create());
        this.appOpsControllerImplProvider = DoubleCheck.provider(AppOpsControllerImpl_Factory.create(this.provideContextProvider, this.provideBgLooperProvider));
        this.statusBarStateControllerImplProvider = DoubleCheck.provider(StatusBarStateControllerImpl_Factory.create());
        this.provideNotificationLockscreenUserManagerProvider = DoubleCheck.provider(SystemUIFactory_ProvideNotificationLockscreenUserManagerFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.notificationGroupAlertTransferHelperProvider = DoubleCheck.provider(NotificationGroupAlertTransferHelper_Factory.create());
        this.provideNotificationEntryManagerProvider = DoubleCheck.provider(SystemUIFactory_ProvideNotificationEntryManagerFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.visualStabilityManagerProvider = DoubleCheck.provider(VisualStabilityManager_Factory.create(this.provideNotificationEntryManagerProvider, this.provideMainHandlerProvider));
        this.notificationGutsManagerProvider = DoubleCheck.provider(NotificationGutsManager_Factory.create(this.provideContextProvider, this.visualStabilityManagerProvider));
        this.mediaArtworkProcessorProvider = DoubleCheck.provider(MediaArtworkProcessor_Factory.create());
        this.notificationMediaManagerProvider = DoubleCheck.provider(NotificationMediaManager_Factory.create(this.provideContextProvider, this.provideShadeControllerProvider, this.statusBarWindowControllerProvider, this.provideNotificationEntryManagerProvider, this.mediaArtworkProcessorProvider));
        this.ambientPulseManagerProvider = DoubleCheck.provider(AmbientPulseManager_Factory.create(this.provideContextProvider));
        this.notificationBlockingHelperManagerProvider = DoubleCheck.provider(NotificationBlockingHelperManager_Factory.create(this.provideContextProvider));
        this.smartReplyControllerProvider = DoubleCheck.provider(SmartReplyController_Factory.create(this.provideNotificationEntryManagerProvider, this.provideIStatusBarServiceProvider));
        this.notificationRemoteInputManagerProvider = DoubleCheck.provider(NotificationRemoteInputManager_Factory.create(this.provideContextProvider, this.provideNotificationLockscreenUserManagerProvider, this.smartReplyControllerProvider, this.provideNotificationEntryManagerProvider, this.provideShadeControllerProvider, this.provideMainHandlerProvider));
        this.smartReplyConstantsProvider = DoubleCheck.provider(SmartReplyConstants_Factory.create(this.provideMainHandlerProvider, this.provideContextProvider));
        this.provideNotificationListenerProvider = DoubleCheck.provider(SystemUIFactory_ProvideNotificationListenerFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.expansionStateLoggerProvider = NotificationLogger_ExpansionStateLogger_Factory.create(this.uiOffloadThreadProvider);
        this.notificationLoggerProvider = DoubleCheck.provider(NotificationLogger_Factory.create(this.provideNotificationListenerProvider, this.uiOffloadThreadProvider, this.provideNotificationEntryManagerProvider, this.statusBarStateControllerImplProvider, this.expansionStateLoggerProvider));
        this.bubbleDataProvider = DoubleCheck.provider(BubbleData_Factory.create(this.provideContextProvider));
        this.dynamicPrivacyControllerProvider = DoubleCheck.provider(DynamicPrivacyController_Factory.create(this.provideContextProvider, this.provideNotificationLockscreenUserManagerProvider));
        this.notificationViewHierarchyManagerProvider = DoubleCheck.provider(NotificationViewHierarchyManager_Factory.create(this.provideContextProvider, this.provideMainHandlerProvider, this.provideNotificationLockscreenUserManagerProvider, this.notificationGroupManagerProvider, this.visualStabilityManagerProvider, this.statusBarStateControllerImplProvider, this.provideNotificationEntryManagerProvider, this.provideShadeControllerProvider, this.bubbleDataProvider, this.dynamicPrivacyControllerProvider));
        this.notificationFilterProvider = DoubleCheck.provider(NotificationFilter_Factory.create());
        this.provideNotificationInterruptionStateProvider = DoubleCheck.provider(SystemUIFactory_ProvideNotificationInterruptionStateProviderFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.keyguardDismissUtilProvider = DoubleCheck.provider(KeyguardDismissUtil_Factory.create());
        this.remoteInputQuickSettingsDisablerProvider = DoubleCheck.provider(RemoteInputQuickSettingsDisabler_Factory.create(this.provideContextProvider, this.provideConfigurationControllerProvider));
        this.bubbleControllerProvider = DoubleCheck.provider(BubbleController_Factory.create(this.provideContextProvider, this.statusBarWindowControllerProvider, this.bubbleDataProvider, this.provideConfigurationControllerProvider, this.provideNotificationInterruptionStateProvider, this.zenModeControllerImplProvider));
        this.notificationAlertingManagerProvider = DoubleCheck.provider(NotificationAlertingManager_Factory.create(this.provideNotificationEntryManagerProvider, this.ambientPulseManagerProvider, this.notificationRemoteInputManagerProvider, this.visualStabilityManagerProvider, this.provideShadeControllerProvider, this.provideNotificationInterruptionStateProvider, this.provideNotificationListenerProvider));
        this.provideSensorPrivacyManagerProvider = DoubleCheck.provider(DependencyProvider_ProvideSensorPrivacyManagerFactory.create(builder.dependencyProvider, this.provideContextProvider));
        this.provideAutoHideControllerProvider = DoubleCheck.provider(DependencyProvider_ProvideAutoHideControllerFactory.create(builder.dependencyProvider, this.provideContextProvider, this.provideMainHandlerProvider));
        this.foregroundServiceNotificationListenerProvider = DoubleCheck.provider(ForegroundServiceNotificationListener_Factory.create(this.provideContextProvider, this.foregroundServiceControllerProvider, this.provideNotificationEntryManagerProvider));
        this.provideHandlerProvider = DoubleCheck.provider(DependencyProvider_ProvideHandlerFactory.create(builder.dependencyProvider));
        this.injectionInflationControllerProvider = DoubleCheck.provider(InjectionInflationController_Factory.create(this.systemUIRootComponentProvider));
        this.provideDockManagerProvider = DoubleCheck.provider(SystemUIFactory_ProvideDockManagerFactory.create(builder.systemUIFactory, this.provideContextProvider));
        this.clockManagerProvider = DoubleCheck.provider(ClockManager_Factory.create(this.provideContextProvider, this.injectionInflationControllerProvider, this.providePluginManagerProvider, this.sysuiColorExtractorProvider, this.provideDockManagerProvider));
    }

    private void initialize2(Builder builder) {
        this.provideActivityManagerWrapperProvider = DoubleCheck.provider(DependencyProvider_ProvideActivityManagerWrapperFactory.create(builder.dependencyProvider));
        this.provideDevicePolicyManagerWrapperProvider = DoubleCheck.provider(DependencyProvider_ProvideDevicePolicyManagerWrapperFactory.create(builder.dependencyProvider));
        this.providePackageManagerWrapperProvider = DoubleCheck.provider(DependencyProvider_ProvidePackageManagerWrapperFactory.create(builder.dependencyProvider));
        this.sensorPrivacyControllerImplProvider = DoubleCheck.provider(SensorPrivacyControllerImpl_Factory.create(this.provideContextProvider));
        this.dumpControllerProvider = DoubleCheck.provider(DumpController_Factory.create());
        this.provideINotificationManagerProvider = DoubleCheck.provider(DependencyProvider_ProvideINotificationManagerFactory.create(builder.dependencyProvider));
        this.channelEditorDialogControllerProvider = DoubleCheck.provider(ChannelEditorDialogController_Factory.create(this.provideContextProvider, this.provideINotificationManagerProvider));
        this.falsingManagerProxyProvider = DoubleCheck.provider(FalsingManagerProxy_Factory.create(this.provideContextProvider, this.providePluginManagerProvider, this.provideMainHandlerProvider));
        this.lockscreenAlbumArtControllerProvider = DoubleCheck.provider(LockscreenAlbumArtController_Factory.create(this.provideContextProvider));
        this.lockscreenTransparentScrimControllerProvider = DoubleCheck.provider(LockscreenTransparentScrimController_Factory.create(this.provideContextProvider));
        this.lockscreenClockControllerProvider = DoubleCheck.provider(LockscreenClockController_Factory.create(this.provideContextProvider));
        this.lockscreenLoopsControllerProvider = DoubleCheck.provider(LockscreenLoopsController_Factory.create(this.provideContextProvider));
        this.lockscreenAmbientDisplayControllerProvider = DoubleCheck.provider(LockscreenAmbientDisplayController_Factory.create(this.provideContextProvider));
        this.lockscreenAssistIconControllerProvider = DoubleCheck.provider(LockscreenAssistIconController_Factory.create(this.provideContextProvider, this.lockscreenLoopsControllerProvider));
        this.lockscreenSkinningControllerProvider = DoubleCheck.provider(LockscreenSkinningController_Factory.create(this.provideContextProvider));
        this.lockscreenStyleCoverControllerProvider = DoubleCheck.provider(LockscreenStyleCoverController_Factory.create(this.provideContextProvider));
        this.notificationWakeUpCoordinatorProvider = DoubleCheck.provider(NotificationWakeUpCoordinator_Factory.create(this.provideContextProvider, this.ambientPulseManagerProvider, this.statusBarStateControllerImplProvider));
        this.pulseExpansionHandlerProvider = DoubleCheck.provider(PulseExpansionHandler_Factory.create(this.provideContextProvider, this.notificationWakeUpCoordinatorProvider));
        this.contextHolder = builder.contextHolder;
        this.qSTileHostProvider = new DelegateFactory();
        this.wifiTileProvider = WifiTile_Factory.create(this.qSTileHostProvider, this.networkControllerImplProvider, this.activityStarterDelegateProvider);
        this.bluetoothTileProvider = BluetoothTile_Factory.create(this.qSTileHostProvider, this.bluetoothControllerImplProvider, this.activityStarterDelegateProvider);
        this.cellularTileProvider = CellularTile_Factory.create(this.qSTileHostProvider, this.networkControllerImplProvider, this.activityStarterDelegateProvider);
        this.dndTileProvider = DndTile_Factory.create(this.qSTileHostProvider, this.zenModeControllerImplProvider, this.activityStarterDelegateProvider);
        this.colorInversionTileProvider = ColorInversionTile_Factory.create(this.qSTileHostProvider);
        this.airplaneModeTileProvider = AirplaneModeTile_Factory.create(this.qSTileHostProvider, this.activityStarterDelegateProvider);
        this.workModeTileProvider = WorkModeTile_Factory.create(this.qSTileHostProvider, this.managedProfileControllerImplProvider);
        this.rotationLockTileProvider = RotationLockTile_Factory.create(this.qSTileHostProvider, this.rotationLockControllerImplProvider);
        this.flashlightTileProvider = FlashlightTile_Factory.create(this.qSTileHostProvider, this.flashlightControllerImplProvider);
        this.locationTileProvider = LocationTile_Factory.create(this.qSTileHostProvider, this.locationControllerImplProvider, this.keyguardMonitorImplProvider, this.activityStarterDelegateProvider);
        this.castTileProvider = CastTile_Factory.create(this.qSTileHostProvider, this.castControllerImplProvider, this.keyguardMonitorImplProvider, this.networkControllerImplProvider, this.activityStarterDelegateProvider);
        this.hotspotTileProvider = HotspotTile_Factory.create(this.qSTileHostProvider, this.hotspotControllerImplProvider, this.provideDataSaverControllerProvider);
        this.userTileProvider = UserTile_Factory.create(this.qSTileHostProvider, this.userSwitcherControllerProvider, this.userInfoControllerImplProvider);
        this.batterySaverTileProvider = BatterySaverTile_Factory.create(this.qSTileHostProvider, this.batteryControllerImplProvider);
        this.dataSaverTileProvider = DataSaverTile_Factory.create(this.qSTileHostProvider, this.networkControllerImplProvider);
        this.nightDisplayTileProvider = NightDisplayTile_Factory.create(this.qSTileHostProvider);
        this.nfcTileProvider = NfcTile_Factory.create(this.qSTileHostProvider);
        this.memoryTileProvider = GarbageMonitor_MemoryTile_Factory.create(this.qSTileHostProvider);
        this.uiModeNightTileProvider = UiModeNightTile_Factory.create(this.qSTileHostProvider, this.provideConfigurationControllerProvider, this.batteryControllerImplProvider);
        this.qSFactoryImplProvider = DoubleCheck.provider(QSFactoryImpl_Factory.create(this.wifiTileProvider, this.bluetoothTileProvider, this.cellularTileProvider, this.dndTileProvider, this.colorInversionTileProvider, this.airplaneModeTileProvider, this.workModeTileProvider, this.rotationLockTileProvider, this.flashlightTileProvider, this.locationTileProvider, this.castTileProvider, this.hotspotTileProvider, this.userTileProvider, this.batterySaverTileProvider, this.dataSaverTileProvider, this.nightDisplayTileProvider, this.nfcTileProvider, this.memoryTileProvider, this.uiModeNightTileProvider));
        this.autoAddTrackerProvider = AutoAddTracker_Factory.create(this.provideContextProvider);
        this.autoTileManagerProvider = AutoTileManager_Factory.create(this.provideContextProvider, this.autoAddTrackerProvider, this.qSTileHostProvider, this.provideBgHandlerProvider, this.hotspotControllerImplProvider, this.provideDataSaverControllerProvider, this.managedProfileControllerImplProvider, this.provideNightDisplayListenerProvider, this.castControllerImplProvider);
        this.qSTileHostProvider = DoubleCheck.provider(QSTileHost_Factory.create(this.provideContextProvider, this.statusBarIconControllerImplProvider, this.qSFactoryImplProvider, this.provideMainHandlerProvider, this.provideBgLooperProvider, this.providePluginManagerProvider, this.tunerServiceImplProvider, this.autoTileManagerProvider, this.dumpControllerProvider));
        ((DelegateFactory) this.qSTileHostProvider).setDelegatedProvider(this.qSTileHostProvider);
        this.provideAllowNotificationLongPressProvider = DoubleCheck.provider(SystemUIFactory_ProvideAllowNotificationLongPressFactory.create(builder.systemUIFactory));
        this.notificationRoundnessManagerProvider = DoubleCheck.provider(NotificationRoundnessManager_Factory.create(this.ambientPulseManagerProvider));
    }

    @Override // com.android.systemui.SystemUIFactory.SystemUIRootComponent
    public GarbageMonitor createGarbageMonitor() {
        return this.garbageMonitorProvider.get();
    }

    @Override // com.android.systemui.SystemUIFactory.SystemUIRootComponent
    public Dependency.DependencyInjector createDependency() {
        return new DependencyInjectorImpl();
    }

    @Override // com.android.systemui.SystemUIFactory.SystemUIRootComponent
    public StatusBar.StatusBarInjector getStatusBarInjector() {
        return new StatusBarInjectorImpl();
    }

    @Override // com.android.systemui.SystemUIFactory.SystemUIRootComponent
    public FragmentService.FragmentCreator createFragmentCreator() {
        return new FragmentCreatorImpl();
    }

    @Override // com.android.systemui.SystemUIFactory.SystemUIRootComponent
    public InjectionInflationController.ViewCreator createViewCreator() {
        return new ViewCreatorImpl();
    }

    public static final class Builder {
        private SystemUIFactory.ContextHolder contextHolder;
        private DependencyProvider dependencyProvider;
        private SystemUIFactory systemUIFactory;

        private Builder() {
        }

        public SystemUIFactory.SystemUIRootComponent build() {
            if (this.contextHolder != null) {
                if (this.dependencyProvider == null) {
                    this.dependencyProvider = new DependencyProvider();
                }
                if (this.systemUIFactory == null) {
                    this.systemUIFactory = new SystemUIFactory();
                }
                return new DaggerSystemUIFactory_SystemUIRootComponent(this);
            }
            throw new IllegalStateException(SystemUIFactory.ContextHolder.class.getCanonicalName() + " must be set");
        }

        public Builder systemUIFactory(SystemUIFactory systemUIFactory2) {
            Preconditions.checkNotNull(systemUIFactory2);
            this.systemUIFactory = systemUIFactory2;
            return this;
        }

        public Builder dependencyProvider(DependencyProvider dependencyProvider2) {
            Preconditions.checkNotNull(dependencyProvider2);
            this.dependencyProvider = dependencyProvider2;
            return this;
        }

        public Builder contextHolder(SystemUIFactory.ContextHolder contextHolder2) {
            Preconditions.checkNotNull(contextHolder2);
            this.contextHolder = contextHolder2;
            return this;
        }
    }

    private final class DependencyInjectorImpl implements Dependency.DependencyInjector {
        private DependencyInjectorImpl() {
        }

        @Override // com.android.systemui.Dependency.DependencyInjector
        public void createSystemUI(Dependency dependency) {
            injectDependency(dependency);
        }

        private Dependency injectDependency(Dependency dependency) {
            Dependency_MembersInjector.injectMActivityStarter(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider));
            Dependency_MembersInjector.injectMActivityStarterDelegate(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider));
            Dependency_MembersInjector.injectMAsyncSensorManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.asyncSensorManagerProvider));
            Dependency_MembersInjector.injectMBluetoothController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.bluetoothControllerImplProvider));
            Dependency_MembersInjector.injectMLocationController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.locationControllerImplProvider));
            Dependency_MembersInjector.injectMRotationLockController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.rotationLockControllerImplProvider));
            Dependency_MembersInjector.injectMNetworkController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.networkControllerImplProvider));
            Dependency_MembersInjector.injectMZenModeController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.zenModeControllerImplProvider));
            Dependency_MembersInjector.injectMHotspotController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.hotspotControllerImplProvider));
            Dependency_MembersInjector.injectMCastController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.castControllerImplProvider));
            Dependency_MembersInjector.injectMFlashlightController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.flashlightControllerImplProvider));
            Dependency_MembersInjector.injectMUserSwitcherController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.userSwitcherControllerProvider));
            Dependency_MembersInjector.injectMUserInfoController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.userInfoControllerImplProvider));
            Dependency_MembersInjector.injectMKeyguardMonitor(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.keyguardMonitorImplProvider));
            Dependency_MembersInjector.injectMBatteryController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.batteryControllerImplProvider));
            Dependency_MembersInjector.injectMNightDisplayListener(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNightDisplayListenerProvider));
            Dependency_MembersInjector.injectMManagedProfileController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.managedProfileControllerImplProvider));
            Dependency_MembersInjector.injectMNextAlarmController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.nextAlarmControllerImplProvider));
            Dependency_MembersInjector.injectMDataSaverController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideDataSaverControllerProvider));
            Dependency_MembersInjector.injectMAccessibilityController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.accessibilityControllerProvider));
            Dependency_MembersInjector.injectMDeviceProvisionedController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.deviceProvisionedControllerImplProvider));
            Dependency_MembersInjector.injectMPluginManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.providePluginManagerProvider));
            Dependency_MembersInjector.injectMAssistManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideAssistManagerProvider));
            Dependency_MembersInjector.injectMSecurityController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.securityControllerImplProvider));
            Dependency_MembersInjector.injectMLeakDetector(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideLeakDetectorProvider));
            Dependency_MembersInjector.injectMLeakReporter(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.leakReporterProvider));
            Dependency_MembersInjector.injectMGarbageMonitor(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.garbageMonitorProvider));
            Dependency_MembersInjector.injectMTunerService(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.tunerServiceImplProvider));
            Dependency_MembersInjector.injectMStatusBarWindowController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarWindowControllerProvider));
            Dependency_MembersInjector.injectMDarkIconDispatcher(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.darkIconDispatcherImplProvider));
            Dependency_MembersInjector.injectMConfigurationController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideConfigurationControllerProvider));
            Dependency_MembersInjector.injectMStatusBarIconController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarIconControllerImplProvider));
            Dependency_MembersInjector.injectMNavigationBarLockController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.navigationBarLockControllerImplProvider));
            Dependency_MembersInjector.injectMScreenLifecycle(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.screenLifecycleProvider));
            Dependency_MembersInjector.injectMWakefulnessLifecycle(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.wakefulnessLifecycleProvider));
            Dependency_MembersInjector.injectMFragmentService(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.fragmentServiceProvider));
            Dependency_MembersInjector.injectMExtensionController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.extensionControllerImplProvider));
            Dependency_MembersInjector.injectMPluginDependencyProvider(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.pluginDependencyProvider));
            Dependency_MembersInjector.injectMLocalBluetoothManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideLocalBluetoothControllerProvider));
            Dependency_MembersInjector.injectMVolumeDialogController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.volumeDialogControllerImplProvider));
            Dependency_MembersInjector.injectMMetricsLogger(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideMetricsLoggerProvider));
            Dependency_MembersInjector.injectMAccessibilityManagerWrapper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.accessibilityManagerWrapperProvider));
            Dependency_MembersInjector.injectMSysuiColorExtractor(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.sysuiColorExtractorProvider));
            Dependency_MembersInjector.injectMTunablePaddingService(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.tunablePaddingServiceProvider));
            Dependency_MembersInjector.injectMForegroundServiceController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.foregroundServiceControllerProvider));
            Dependency_MembersInjector.injectMUiOffloadThread(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.uiOffloadThreadProvider));
            Dependency_MembersInjector.injectMWarningsUI(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.powerNotificationWarningsProvider));
            Dependency_MembersInjector.injectMLightBarController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lightBarControllerProvider));
            Dependency_MembersInjector.injectMIWindowManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideIWindowManagerProvider));
            Dependency_MembersInjector.injectMOverviewProxyService(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.overviewProxyServiceProvider));
            Dependency_MembersInjector.injectMNavBarModeController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.navigationModeControllerProvider));
            Dependency_MembersInjector.injectMEnhancedEstimates(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideEnhancedEstimatesProvider));
            Dependency_MembersInjector.injectMVibratorHelper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.vibratorHelperProvider));
            Dependency_MembersInjector.injectMIStatusBarService(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideIStatusBarServiceProvider));
            Dependency_MembersInjector.injectMDisplayMetrics(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideDisplayMetricsProvider));
            Dependency_MembersInjector.injectMLockscreenGestureLogger(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenGestureLoggerProvider));
            Dependency_MembersInjector.injectMKeyguardEnvironment(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideKeyguardEnvironmentProvider));
            Dependency_MembersInjector.injectMShadeController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideShadeControllerProvider));
            Dependency_MembersInjector.injectMNotificationRemoteInputManagerCallback(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarRemoteInputCallbackProvider));
            Dependency_MembersInjector.injectMInitController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.initControllerProvider));
            Dependency_MembersInjector.injectMAppOpsController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.appOpsControllerImplProvider));
            Dependency_MembersInjector.injectMNavigationBarController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNavigationBarControllerProvider));
            Dependency_MembersInjector.injectMStatusBarStateController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarStateControllerImplProvider));
            Dependency_MembersInjector.injectMNotificationLockscreenUserManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNotificationLockscreenUserManagerProvider));
            Dependency_MembersInjector.injectMNotificationGroupAlertTransferHelper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationGroupAlertTransferHelperProvider));
            Dependency_MembersInjector.injectMNotificationGroupManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationGroupManagerProvider));
            Dependency_MembersInjector.injectMVisualStabilityManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.visualStabilityManagerProvider));
            Dependency_MembersInjector.injectMNotificationGutsManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationGutsManagerProvider));
            Dependency_MembersInjector.injectMNotificationMediaManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationMediaManagerProvider));
            Dependency_MembersInjector.injectMAmbientPulseManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.ambientPulseManagerProvider));
            Dependency_MembersInjector.injectMNotificationBlockingHelperManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationBlockingHelperManagerProvider));
            Dependency_MembersInjector.injectMNotificationRemoteInputManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationRemoteInputManagerProvider));
            Dependency_MembersInjector.injectMSmartReplyConstants(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.smartReplyConstantsProvider));
            Dependency_MembersInjector.injectMNotificationListener(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNotificationListenerProvider));
            Dependency_MembersInjector.injectMNotificationLogger(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationLoggerProvider));
            Dependency_MembersInjector.injectMNotificationViewHierarchyManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationViewHierarchyManagerProvider));
            Dependency_MembersInjector.injectMNotificationFilter(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationFilterProvider));
            Dependency_MembersInjector.injectMNotificationInterruptionStateProvider(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNotificationInterruptionStateProvider));
            Dependency_MembersInjector.injectMKeyguardDismissUtil(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.keyguardDismissUtilProvider));
            Dependency_MembersInjector.injectMSmartReplyController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.smartReplyControllerProvider));
            Dependency_MembersInjector.injectMRemoteInputQuickSettingsDisabler(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.remoteInputQuickSettingsDisablerProvider));
            Dependency_MembersInjector.injectMBubbleController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.bubbleControllerProvider));
            Dependency_MembersInjector.injectMNotificationEntryManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideNotificationEntryManagerProvider));
            Dependency_MembersInjector.injectMNotificationAlertingManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.notificationAlertingManagerProvider));
            Dependency_MembersInjector.injectMSensorPrivacyManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideSensorPrivacyManagerProvider));
            Dependency_MembersInjector.injectMAutoHideController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideAutoHideControllerProvider));
            Dependency_MembersInjector.injectMForegroundServiceNotificationListener(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.foregroundServiceNotificationListenerProvider));
            Dependency_MembersInjector.injectMBgLooper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideBgLooperProvider));
            Dependency_MembersInjector.injectMBgHandler(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideBgHandlerProvider));
            Dependency_MembersInjector.injectMMainHandler(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideMainHandlerProvider));
            Dependency_MembersInjector.injectMTimeTickHandler(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideHandlerProvider));
            Dependency_MembersInjector.injectMLeakReportEmail(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideLeakReportEmailProvider));
            Dependency_MembersInjector.injectMClockManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.clockManagerProvider));
            Dependency_MembersInjector.injectMActivityManagerWrapper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideActivityManagerWrapperProvider));
            Dependency_MembersInjector.injectMDevicePolicyManagerWrapper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideDevicePolicyManagerWrapperProvider));
            Dependency_MembersInjector.injectMPackageManagerWrapper(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.providePackageManagerWrapperProvider));
            Dependency_MembersInjector.injectMSensorPrivacyController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.sensorPrivacyControllerImplProvider));
            Dependency_MembersInjector.injectMDumpController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.dumpControllerProvider));
            Dependency_MembersInjector.injectMDockManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideDockManagerProvider));
            Dependency_MembersInjector.injectMChannelEditorDialogController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.channelEditorDialogControllerProvider));
            Dependency_MembersInjector.injectMINotificationManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.provideINotificationManagerProvider));
            Dependency_MembersInjector.injectMFalsingManager(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.falsingManagerProxyProvider));
            Dependency_MembersInjector.injectMLockscreenAlbumArtController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenAlbumArtControllerProvider));
            Dependency_MembersInjector.injectMLockscreenTransparentScrimController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenTransparentScrimControllerProvider));
            Dependency_MembersInjector.injectMLockscreenClockController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenClockControllerProvider));
            Dependency_MembersInjector.injectMLockscreenLoopsController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenLoopsControllerProvider));
            Dependency_MembersInjector.injectMLockscreenAmbientDisplayController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenAmbientDisplayControllerProvider));
            Dependency_MembersInjector.injectMLockscreenAssistIconController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenAssistIconControllerProvider));
            Dependency_MembersInjector.injectMLockscreenSkinningController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenSkinningControllerProvider));
            Dependency_MembersInjector.injectMLockscreenStyleCoverController(dependency, DoubleCheck.lazy(DaggerSystemUIFactory_SystemUIRootComponent.this.lockscreenStyleCoverControllerProvider));
            return dependency;
        }
    }

    private final class StatusBarInjectorImpl implements StatusBar.StatusBarInjector {
        private StatusBarInjectorImpl() {
        }

        @Override // com.android.systemui.statusbar.phone.StatusBar.StatusBarInjector
        public void createStatusBar(StatusBar statusBar) {
            injectStatusBar(statusBar);
        }

        private StatusBar injectStatusBar(StatusBar statusBar) {
            StatusBar_MembersInjector.injectMInjectionInflater(statusBar, (InjectionInflationController) DaggerSystemUIFactory_SystemUIRootComponent.this.injectionInflationControllerProvider.get());
            StatusBar_MembersInjector.injectMPulseExpansionHandler(statusBar, (PulseExpansionHandler) DaggerSystemUIFactory_SystemUIRootComponent.this.pulseExpansionHandlerProvider.get());
            StatusBar_MembersInjector.injectMWakeUpCoordinator(statusBar, (NotificationWakeUpCoordinator) DaggerSystemUIFactory_SystemUIRootComponent.this.notificationWakeUpCoordinatorProvider.get());
            return statusBar;
        }
    }

    private final class FragmentCreatorImpl implements FragmentService.FragmentCreator {
        private FragmentCreatorImpl() {
        }

        @Override // com.android.systemui.fragments.FragmentService.FragmentCreator
        public NavigationBarFragment createNavigationBarFragment() {
            return new NavigationBarFragment((AccessibilityManagerWrapper) DaggerSystemUIFactory_SystemUIRootComponent.this.accessibilityManagerWrapperProvider.get(), (DeviceProvisionedController) DaggerSystemUIFactory_SystemUIRootComponent.this.deviceProvisionedControllerImplProvider.get(), (MetricsLogger) DaggerSystemUIFactory_SystemUIRootComponent.this.provideMetricsLoggerProvider.get(), (AssistManager) DaggerSystemUIFactory_SystemUIRootComponent.this.provideAssistManagerProvider.get(), (OverviewProxyService) DaggerSystemUIFactory_SystemUIRootComponent.this.overviewProxyServiceProvider.get(), (NavigationModeController) DaggerSystemUIFactory_SystemUIRootComponent.this.navigationModeControllerProvider.get(), (StatusBarStateController) DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarStateControllerImplProvider.get());
        }

        @Override // com.android.systemui.fragments.FragmentService.FragmentCreator
        public QSFragment createQSFragment() {
            return new QSFragment((RemoteInputQuickSettingsDisabler) DaggerSystemUIFactory_SystemUIRootComponent.this.remoteInputQuickSettingsDisablerProvider.get(), (InjectionInflationController) DaggerSystemUIFactory_SystemUIRootComponent.this.injectionInflationControllerProvider.get(), SystemUIFactory_ContextHolder_ProvideContextFactory.proxyProvideContext(DaggerSystemUIFactory_SystemUIRootComponent.this.contextHolder), (QSTileHost) DaggerSystemUIFactory_SystemUIRootComponent.this.qSTileHostProvider.get());
        }
    }

    private final class ViewCreatorImpl implements InjectionInflationController.ViewCreator {
        private ViewCreatorImpl() {
        }

        @Override // com.android.systemui.util.InjectionInflationController.ViewCreator
        public InjectionInflationController.ViewInstanceCreator createInstanceCreator(InjectionInflationController.ViewAttributeProvider viewAttributeProvider) {
            return new ViewInstanceCreatorImpl(viewAttributeProvider);
        }

        private final class ViewInstanceCreatorImpl implements InjectionInflationController.ViewInstanceCreator {
            private InjectionInflationController.ViewAttributeProvider viewAttributeProvider;

            private ViewInstanceCreatorImpl(InjectionInflationController.ViewAttributeProvider viewAttributeProvider2) {
                initialize(viewAttributeProvider2);
            }

            private void initialize(InjectionInflationController.ViewAttributeProvider viewAttributeProvider2) {
                Preconditions.checkNotNull(viewAttributeProvider2);
                this.viewAttributeProvider = viewAttributeProvider2;
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public QuickStatusBarHeader createQsHeader() {
                return new QuickStatusBarHeader(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (NextAlarmController) DaggerSystemUIFactory_SystemUIRootComponent.this.nextAlarmControllerImplProvider.get(), (ZenModeController) DaggerSystemUIFactory_SystemUIRootComponent.this.zenModeControllerImplProvider.get(), (StatusBarIconController) DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarIconControllerImplProvider.get(), (ActivityStarter) DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public QSFooterImpl createQsFooter() {
                return new QSFooterImpl(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (ActivityStarter) DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider.get(), (UserInfoController) DaggerSystemUIFactory_SystemUIRootComponent.this.userInfoControllerImplProvider.get(), (DeviceProvisionedController) DaggerSystemUIFactory_SystemUIRootComponent.this.deviceProvisionedControllerImplProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public NotificationStackScrollLayout createNotificationStackScrollLayout() {
                return NotificationStackScrollLayout_Factory.newNotificationStackScrollLayout(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), ((Boolean) DaggerSystemUIFactory_SystemUIRootComponent.this.provideAllowNotificationLongPressProvider.get()).booleanValue(), DaggerSystemUIFactory_SystemUIRootComponent.this.notificationRoundnessManagerProvider.get(), (AmbientPulseManager) DaggerSystemUIFactory_SystemUIRootComponent.this.ambientPulseManagerProvider.get(), (DynamicPrivacyController) DaggerSystemUIFactory_SystemUIRootComponent.this.dynamicPrivacyControllerProvider.get(), (ConfigurationController) DaggerSystemUIFactory_SystemUIRootComponent.this.provideConfigurationControllerProvider.get(), (ActivityStarter) DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider.get(), (StatusBarStateController) DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarStateControllerImplProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public NotificationPanelView createPanelView() {
                return new NotificationPanelView(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (InjectionInflationController) DaggerSystemUIFactory_SystemUIRootComponent.this.injectionInflationControllerProvider.get(), (NotificationWakeUpCoordinator) DaggerSystemUIFactory_SystemUIRootComponent.this.notificationWakeUpCoordinatorProvider.get(), (PulseExpansionHandler) DaggerSystemUIFactory_SystemUIRootComponent.this.pulseExpansionHandlerProvider.get(), (DynamicPrivacyController) DaggerSystemUIFactory_SystemUIRootComponent.this.dynamicPrivacyControllerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public QSCarrierGroup createQSCarrierGroup() {
                return new QSCarrierGroup(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (NetworkController) DaggerSystemUIFactory_SystemUIRootComponent.this.networkControllerImplProvider.get(), (ActivityStarter) DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public KeyguardClockSwitch createKeyguardClockSwitch() {
                return new KeyguardClockSwitch(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (StatusBarStateController) DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarStateControllerImplProvider.get(), (SysuiColorExtractor) DaggerSystemUIFactory_SystemUIRootComponent.this.sysuiColorExtractorProvider.get(), (ClockManager) DaggerSystemUIFactory_SystemUIRootComponent.this.clockManagerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public KeyguardSliceView createKeyguardSliceView() {
                return new KeyguardSliceView(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (ActivityStarter) DaggerSystemUIFactory_SystemUIRootComponent.this.activityStarterDelegateProvider.get(), (ConfigurationController) DaggerSystemUIFactory_SystemUIRootComponent.this.provideConfigurationControllerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public KeyguardMessageArea createKeyguardMessageArea() {
                return new KeyguardMessageArea(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (ConfigurationController) DaggerSystemUIFactory_SystemUIRootComponent.this.provideConfigurationControllerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public LockIcon createLockIcon() {
                return new LockIcon(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (StatusBarStateController) DaggerSystemUIFactory_SystemUIRootComponent.this.statusBarStateControllerImplProvider.get(), (ConfigurationController) DaggerSystemUIFactory_SystemUIRootComponent.this.provideConfigurationControllerProvider.get(), (AccessibilityController) DaggerSystemUIFactory_SystemUIRootComponent.this.accessibilityControllerProvider.get(), (KeyguardMonitor) DaggerSystemUIFactory_SystemUIRootComponent.this.keyguardMonitorImplProvider.get(), (DockManager) DaggerSystemUIFactory_SystemUIRootComponent.this.provideDockManagerProvider.get(), (Handler) DaggerSystemUIFactory_SystemUIRootComponent.this.provideMainHandlerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public QSPanel createQSPanel() {
                return new QSPanel(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (DumpController) DaggerSystemUIFactory_SystemUIRootComponent.this.dumpControllerProvider.get());
            }

            @Override // com.android.systemui.util.InjectionInflationController.ViewInstanceCreator
            public QuickQSPanel createQuickQSPanel() {
                return new QuickQSPanel(InjectionInflationController_ViewAttributeProvider_ProvideContextFactory.proxyProvideContext(this.viewAttributeProvider), InjectionInflationController_ViewAttributeProvider_ProvideAttributeSetFactory.proxyProvideAttributeSet(this.viewAttributeProvider), (DumpController) DaggerSystemUIFactory_SystemUIRootComponent.this.dumpControllerProvider.get());
            }
        }
    }
}
