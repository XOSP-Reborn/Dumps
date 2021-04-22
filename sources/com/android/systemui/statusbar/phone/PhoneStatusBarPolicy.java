package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.service.notification.ZenModeConfig;
import android.telecom.TelecomManager;
import android.text.format.DateFormat;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Locale;

public class PhoneStatusBarPolicy implements BluetoothController.Callback, CommandQueue.Callbacks, RotationLockController.RotationLockControllerCallback, DataSaverController.Listener, ZenModeController.Callback, DeviceProvisionedController.DeviceProvisionedListener, KeyguardMonitor.Callback, LocationController.LocationChangeCallback {
    private static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    private final AlarmManager mAlarmManager;
    private BluetoothController mBluetooth;
    private final CastController mCast;
    private final CastController.Callback mCastCallback = new CastController.Callback() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass3 */

        @Override // com.android.systemui.statusbar.policy.CastController.Callback
        public void onCastDevicesChanged() {
            PhoneStatusBarPolicy.this.updateCast();
        }
    };
    private final Context mContext;
    private boolean mCurrentUserSetup;
    private final DataSaverController mDataSaver;
    private final Handler mHandler = new Handler();
    private final HotspotController mHotspot;
    private final HotspotController.Callback mHotspotCallback = new HotspotController.Callback() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass2 */

        @Override // com.android.systemui.statusbar.policy.HotspotController.Callback
        public void onHotspotChanged(boolean z, int i) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotHotspot, z);
        }
    };
    private final StatusBarIconController mIconController;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass6 */

        /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1676458352:
                    if (action.equals("android.intent.action.HEADSET_PLUG")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case -1238404651:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -864107122:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case -229777127:
                    if (action.equals("android.intent.action.SIM_STATE_CHANGED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 100931828:
                    if (action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1051344550:
                    if (action.equals("android.telecom.action.CURRENT_TTY_MODE_CHANGED")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 1051477093:
                    if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 2070024785:
                    if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    PhoneStatusBarPolicy.this.updateVolumeZen();
                    return;
                case 2:
                    if (!intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                        PhoneStatusBarPolicy.this.updateSimState(intent);
                        return;
                    }
                    return;
                case 3:
                    PhoneStatusBarPolicy.this.updateTTY(intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0));
                    return;
                case 4:
                case 5:
                case 6:
                    PhoneStatusBarPolicy.this.updateManagedProfile();
                    return;
                case 7:
                    PhoneStatusBarPolicy.this.updateHeadsetPlug(intent);
                    return;
                default:
                    return;
            }
        }
    };
    private final KeyguardMonitor mKeyguardMonitor;
    private final LocationController mLocationController;
    private boolean mManagedProfileIconVisible = false;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private final NextAlarmController.NextAlarmChangeCallback mNextAlarmCallback = new NextAlarmController.NextAlarmChangeCallback() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass4 */

        @Override // com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback
        public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
            PhoneStatusBarPolicy.this.mNextAlarm = alarmClockInfo;
            PhoneStatusBarPolicy.this.updateAlarm();
        }
    };
    private final NextAlarmController mNextAlarmController;
    private final DeviceProvisionedController mProvisionedController;
    private Runnable mRemoveCastIconRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass7 */

        public void run() {
            if (PhoneStatusBarPolicy.DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
            }
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotCast, false);
        }
    };
    private final RotationLockController mRotationLockController;
    private final SensorPrivacyController mSensorPrivacyController;
    private final SensorPrivacyController.OnSensorPrivacyChangedListener mSensorPrivacyListener = new SensorPrivacyController.OnSensorPrivacyChangedListener() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass5 */

        @Override // com.android.systemui.statusbar.policy.SensorPrivacyController.OnSensorPrivacyChangedListener
        public void onSensorPrivacyChanged(boolean z) {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable(z) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$PhoneStatusBarPolicy$5$UApHxsPG0BIvDnX5FCFYX6op1Fs */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PhoneStatusBarPolicy.AnonymousClass5.this.lambda$onSensorPrivacyChanged$0$PhoneStatusBarPolicy$5(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$onSensorPrivacyChanged$0$PhoneStatusBarPolicy$5(boolean z) {
            PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotSensorsOff, z);
        }
    };
    IccCardConstants.State mSimState = IccCardConstants.State.READY;
    private final String mSlotAlarmClock;
    private final String mSlotBluetooth;
    private final String mSlotCast;
    private final String mSlotDataSaver;
    private final String mSlotHeadset;
    private final String mSlotHotspot;
    private final String mSlotLocation;
    private final String mSlotManagedProfile;
    private final String mSlotRotate;
    private final String mSlotSensorsOff;
    private final String mSlotTty;
    private final String mSlotVolume;
    private final String mSlotZen;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    private final UserInfoController mUserInfoController;
    private final UserManager mUserManager;
    private final SynchronousUserSwitchObserver mUserSwitchListener = new SynchronousUserSwitchObserver() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.AnonymousClass1 */

        public /* synthetic */ void lambda$onUserSwitching$0$PhoneStatusBarPolicy$1() {
            PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
        }

        public void onUserSwitching(int i) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$PhoneStatusBarPolicy$1$4_BI5ieR2ylfAj9z5SwNfbqaqk4 */

                public final void run() {
                    PhoneStatusBarPolicy.AnonymousClass1.this.lambda$onUserSwitching$0$PhoneStatusBarPolicy$1();
                }
            });
        }

        public void onUserSwitchComplete(int i) throws RemoteException {
            PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$PhoneStatusBarPolicy$1$lONTSmykfPe64DIHRuLayVCRwlI */

                public final void run() {
                    PhoneStatusBarPolicy.AnonymousClass1.this.lambda$onUserSwitchComplete$1$PhoneStatusBarPolicy$1();
                }
            });
        }

        public /* synthetic */ void lambda$onUserSwitchComplete$1$PhoneStatusBarPolicy$1() {
            PhoneStatusBarPolicy.this.updateAlarm();
            PhoneStatusBarPolicy.this.updateManagedProfile();
        }
    };
    private boolean mVolumeVisible;
    private final ZenModeController mZenController;
    private boolean mZenVisible;

    public PhoneStatusBarPolicy(Context context, StatusBarIconController statusBarIconController) {
        this.mContext = context;
        this.mIconController = statusBarIconController;
        this.mCast = (CastController) Dependency.get(CastController.class);
        this.mHotspot = (HotspotController) Dependency.get(HotspotController.class);
        this.mBluetooth = (BluetoothController) Dependency.get(BluetoothController.class);
        this.mNextAlarmController = (NextAlarmController) Dependency.get(NextAlarmController.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mRotationLockController = (RotationLockController) Dependency.get(RotationLockController.class);
        this.mDataSaver = (DataSaverController) Dependency.get(DataSaverController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mLocationController = (LocationController) Dependency.get(LocationController.class);
        this.mSensorPrivacyController = (SensorPrivacyController) Dependency.get(SensorPrivacyController.class);
        this.mSlotCast = context.getString(17041398);
        this.mSlotHotspot = context.getString(17041405);
        this.mSlotBluetooth = context.getString(17041396);
        this.mSlotTty = context.getString(17041421);
        this.mSlotZen = context.getString(17041425);
        this.mSlotVolume = context.getString(17041422);
        this.mSlotAlarmClock = context.getString(17041394);
        this.mSlotManagedProfile = context.getString(17041408);
        this.mSlotRotate = context.getString(17041415);
        this.mSlotHeadset = context.getString(17041404);
        this.mSlotDataSaver = context.getString(17041402);
        this.mSlotLocation = context.getString(17041407);
        this.mSlotSensorsOff = context.getString(17041417);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        intentFilter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
        try {
            ActivityManager.getService().registerUserSwitchObserver(this.mUserSwitchListener, "PhoneStatusBarPolicy");
        } catch (RemoteException unused) {
        }
        updateTTY();
        updateBluetooth();
        this.mIconController.setIcon(this.mSlotAlarmClock, C0006R$drawable.stat_sys_alarm, null);
        this.mIconController.setIconVisibility(this.mSlotAlarmClock, false);
        this.mIconController.setIcon(this.mSlotZen, C0006R$drawable.stat_sys_dnd, null);
        this.mIconController.setIconVisibility(this.mSlotZen, false);
        this.mIconController.setIcon(this.mSlotVolume, C0006R$drawable.stat_sys_ringer_vibrate, null);
        this.mIconController.setIconVisibility(this.mSlotVolume, false);
        updateVolumeZen();
        this.mIconController.setIcon(this.mSlotCast, C0006R$drawable.stat_sys_cast, null);
        this.mIconController.setIconVisibility(this.mSlotCast, false);
        this.mIconController.setIcon(this.mSlotHotspot, C0006R$drawable.stat_sys_hotspot, this.mContext.getString(C0014R$string.accessibility_status_bar_hotspot));
        this.mIconController.setIconVisibility(this.mSlotHotspot, this.mHotspot.isHotspotEnabled());
        this.mIconController.setIcon(this.mSlotManagedProfile, C0006R$drawable.stat_sys_managed_profile_status, this.mContext.getString(C0014R$string.accessibility_managed_profile));
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
        this.mIconController.setIcon(this.mSlotDataSaver, C0006R$drawable.stat_sys_data_saver, context.getString(C0014R$string.accessibility_data_saver_on));
        this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
        this.mIconController.setIcon(this.mSlotLocation, 17303131, this.mContext.getString(C0014R$string.accessibility_location_active));
        this.mIconController.setIconVisibility(this.mSlotLocation, false);
        this.mIconController.setIcon(this.mSlotSensorsOff, C0006R$drawable.stat_sys_sensors_off, this.mContext.getString(C0014R$string.accessibility_sensors_off_active));
        this.mIconController.setIconVisibility(this.mSlotSensorsOff, this.mSensorPrivacyController.isSensorPrivacyEnabled());
        this.mRotationLockController.addCallback(this);
        this.mBluetooth.addCallback(this);
        this.mProvisionedController.addCallback(this);
        this.mZenController.addCallback(this);
        this.mCast.addCallback(this.mCastCallback);
        this.mHotspot.addCallback(this.mHotspotCallback);
        this.mNextAlarmController.addCallback(this.mNextAlarmCallback);
        this.mDataSaver.addCallback(this);
        this.mKeyguardMonitor.addCallback(this);
        this.mSensorPrivacyController.addCallback(this.mSensorPrivacyListener);
        this.mLocationController.addCallback(this);
        ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).addCallback((CommandQueue.Callbacks) this);
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onZenChanged(int i) {
        updateVolumeZen();
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        updateVolumeZen();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateAlarm() {
        int i;
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(-2);
        boolean z = true;
        boolean z2 = nextAlarmClock != null && nextAlarmClock.getTriggerTime() > 0;
        boolean z3 = this.mZenController.getZen() == 2;
        StatusBarIconController statusBarIconController = this.mIconController;
        String str = this.mSlotAlarmClock;
        if (z3) {
            i = C0006R$drawable.stat_sys_alarm_dim;
        } else {
            i = C0006R$drawable.stat_sys_alarm;
        }
        statusBarIconController.setIcon(str, i, buildAlarmContentDescription());
        StatusBarIconController statusBarIconController2 = this.mIconController;
        String str2 = this.mSlotAlarmClock;
        if (!this.mCurrentUserSetup || !z2) {
            z = false;
        }
        statusBarIconController2.setIconVisibility(str2, z);
    }

    private String buildAlarmContentDescription() {
        AlarmManager.AlarmClockInfo alarmClockInfo = this.mNextAlarm;
        if (alarmClockInfo == null) {
            return this.mContext.getString(C0014R$string.status_bar_alarm);
        }
        return formatNextAlarm(alarmClockInfo, this.mContext);
    }

    private static String formatNextAlarm(AlarmManager.AlarmClockInfo alarmClockInfo, Context context) {
        if (alarmClockInfo == null) {
            return "";
        }
        return context.getString(C0014R$string.accessibility_quick_settings_alarm, DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private final void updateSimState(Intent intent) {
        String stringExtra = intent.getStringExtra("ss");
        if ("ABSENT".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.ABSENT;
        } else if ("CARD_IO_ERROR".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.CARD_IO_ERROR;
        } else if ("CARD_RESTRICTED".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.CARD_RESTRICTED;
        } else if ("READY".equals(stringExtra)) {
            this.mSimState = IccCardConstants.State.READY;
        } else if ("LOCKED".equals(stringExtra)) {
            String stringExtra2 = intent.getStringExtra("reason");
            if ("PIN".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PIN_REQUIRED;
            } else if ("PUK".equals(stringExtra2)) {
                this.mSimState = IccCardConstants.State.PUK_REQUIRED;
            } else {
                this.mSimState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            this.mSimState = IccCardConstants.State.UNKNOWN;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0088  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x009e  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00a9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private final void updateVolumeZen() {
        /*
        // Method dump skipped, instructions count: 182
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.updateVolumeZen():void");
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
    public void onBluetoothDevicesChanged() {
        updateBluetooth();
    }

    @Override // com.android.systemui.statusbar.policy.BluetoothController.Callback
    public void onBluetoothStateChange(boolean z) {
        updateBluetooth();
    }

    private final void updateBluetooth() {
        boolean z;
        int i = C0006R$drawable.stat_sys_data_bluetooth_connected;
        String string = this.mContext.getString(C0014R$string.accessibility_quick_settings_bluetooth_on);
        BluetoothController bluetoothController = this.mBluetooth;
        if (bluetoothController == null || !bluetoothController.isBluetoothConnected()) {
            z = false;
        } else {
            string = this.mContext.getString(C0014R$string.accessibility_bluetooth_connected);
            z = this.mBluetooth.isBluetoothEnabled();
        }
        this.mIconController.setIcon(this.mSlotBluetooth, i, string);
        this.mIconController.setIconVisibility(this.mSlotBluetooth, z);
    }

    private final void updateTTY() {
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        if (telecomManager == null) {
            updateTTY(0);
        } else {
            updateTTY(telecomManager.getCurrentTtyMode());
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private final void updateTTY(int i) {
        boolean z = i != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + z);
        }
        if (z) {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY on");
            }
            this.mIconController.setIcon(this.mSlotTty, C0006R$drawable.stat_sys_tty_mode, this.mContext.getString(C0014R$string.accessibility_tty_enabled));
            this.mIconController.setIconVisibility(this.mSlotTty, true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY off");
        }
        this.mIconController.setIconVisibility(this.mSlotTty, false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0020 A[SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x0011  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateCast() {
        /*
        // Method dump skipped, instructions count: 112
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.PhoneStatusBarPolicy.updateCast():void");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateManagedProfile() {
        this.mUiOffloadThread.submit(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$PhoneStatusBarPolicy$0YjhmxnSstzZ2dpboZJyd_6m3ZY */

            public final void run() {
                PhoneStatusBarPolicy.this.lambda$updateManagedProfile$1$PhoneStatusBarPolicy();
            }
        });
    }

    public /* synthetic */ void lambda$updateManagedProfile$1$PhoneStatusBarPolicy() {
        try {
            this.mHandler.post(new Runnable(this.mUserManager.isManagedProfile(ActivityTaskManager.getService().getLastResumedActivityUserId())) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$PhoneStatusBarPolicy$XFUG8Il5dqSkLb5SFSNbiYcXg */
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PhoneStatusBarPolicy.this.lambda$updateManagedProfile$0$PhoneStatusBarPolicy(this.f$1);
                }
            });
        } catch (RemoteException e) {
            Log.w("PhoneStatusBarPolicy", "updateManagedProfile: ", e);
        }
    }

    public /* synthetic */ void lambda$updateManagedProfile$0$PhoneStatusBarPolicy(boolean z) {
        boolean z2;
        if (!z || (this.mKeyguardMonitor.isShowing() && !this.mKeyguardMonitor.isOccluded())) {
            z2 = false;
        } else {
            z2 = true;
            this.mIconController.setIcon(this.mSlotManagedProfile, C0006R$drawable.stat_sys_managed_profile_status, this.mContext.getString(C0014R$string.accessibility_managed_profile));
        }
        if (this.mManagedProfileIconVisible != z2) {
            this.mIconController.setIconVisibility(this.mSlotManagedProfile, z2);
            this.mManagedProfileIconVisible = z2;
        }
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionStarting(int i, long j, long j2, boolean z) {
        if (this.mContext.getDisplayId() == i) {
            updateManagedProfile();
        }
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
    public void onKeyguardShowingChanged() {
        updateManagedProfile();
    }

    @Override // com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener
    public void onUserSetupChanged() {
        DeviceProvisionedController deviceProvisionedController = this.mProvisionedController;
        boolean isUserSetup = deviceProvisionedController.isUserSetup(deviceProvisionedController.getCurrentUser());
        if (this.mCurrentUserSetup != isUserSetup) {
            this.mCurrentUserSetup = isUserSetup;
            updateAlarm();
        }
    }

    @Override // com.android.systemui.statusbar.policy.RotationLockController.RotationLockControllerCallback
    public void onRotationLockStateChanged(boolean z, boolean z2) {
        boolean isCurrentOrientationLockPortrait = RotationLockTile.isCurrentOrientationLockPortrait(this.mRotationLockController, this.mContext);
        if (z) {
            if (isCurrentOrientationLockPortrait) {
                this.mIconController.setIcon(this.mSlotRotate, C0006R$drawable.stat_sys_rotate_portrait, this.mContext.getString(C0014R$string.accessibility_rotation_lock_on_portrait));
            } else {
                this.mIconController.setIcon(this.mSlotRotate, C0006R$drawable.stat_sys_rotate_landscape, this.mContext.getString(C0014R$string.accessibility_rotation_lock_on_landscape));
            }
            this.mIconController.setIconVisibility(this.mSlotRotate, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotRotate, false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateHeadsetPlug(Intent intent) {
        int i;
        int i2;
        boolean z = intent.getIntExtra("state", 0) != 0;
        boolean z2 = intent.getIntExtra("microphone", 0) != 0;
        if (z) {
            Context context = this.mContext;
            if (z2) {
                i = C0014R$string.accessibility_status_bar_headset;
            } else {
                i = C0014R$string.accessibility_status_bar_headphones;
            }
            String string = context.getString(i);
            StatusBarIconController statusBarIconController = this.mIconController;
            String str = this.mSlotHeadset;
            if (z2) {
                i2 = C0006R$drawable.stat_sys_headset_mic;
            } else {
                i2 = C0006R$drawable.stat_sys_headset;
            }
            statusBarIconController.setIcon(str, i2, string);
            this.mIconController.setIconVisibility(this.mSlotHeadset, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotHeadset, false);
    }

    @Override // com.android.systemui.statusbar.policy.DataSaverController.Listener
    public void onDataSaverChanged(boolean z) {
        this.mIconController.setIconVisibility(this.mSlotDataSaver, z);
    }

    @Override // com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback
    public void onLocationActiveChanged(boolean z) {
        updateLocation();
    }

    private void updateLocation() {
        if (this.mLocationController.isLocationActive()) {
            this.mIconController.setIconVisibility(this.mSlotLocation, true);
        } else {
            this.mIconController.setIconVisibility(this.mSlotLocation, false);
        }
    }
}
