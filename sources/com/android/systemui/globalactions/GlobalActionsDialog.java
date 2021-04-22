package com.android.systemui.globalactions;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.ScrimDrawable;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.util.ScreenRecordHelper;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.view.RotationPolicy;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0015R$style;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.MultiListLayout;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.globalactions.GlobalActionsDialog;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.GlobalActions;
import com.android.systemui.plugins.GlobalActionsPanelPlugin;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.util.leak.RotationUtils;
import com.android.systemui.volume.SystemUIInterpolators$LogAccelerateInterpolator;
import com.sonymobile.systemui.globalactions.EnableAccessibilityController;
import java.util.ArrayList;
import java.util.List;

public class GlobalActionsDialog implements DialogInterface.OnDismissListener, DialogInterface.OnShowListener, ConfigurationController.ConfigurationListener {
    private final ActivityStarter mActivityStarter;
    private MyAdapter mAdapter;
    private ContentObserver mAirplaneModeObserver;
    private ToggleAction mAirplaneModeOn;
    private ToggleAction.State mAirplaneState;
    private final AudioManager mAudioManager;
    private BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private boolean mDeviceProvisioned = false;
    private ActionsDialog mDialog;
    private final IDreamManager mDreamManager;
    private final EmergencyAffordanceManager mEmergencyAffordanceManager;
    private ToggleAction mEmergencyModeOn;
    private ToggleAction.State mEmergencyState;
    private Handler mHandler;
    private boolean mHasLockdownButton;
    private boolean mHasLogoutButton;
    private boolean mHasTelephony;
    private boolean mHasVibrator;
    private boolean mIsWaitingForEcmExit;
    private ArrayList<Action> mItems;
    private final KeyguardManager mKeyguardManager;
    private boolean mKeyguardShowing = false;
    private final LockPatternUtils mLockPatternUtils;
    private GlobalActionsPanelPlugin mPanelPlugin;
    PhoneStateListener mPhoneStateListener;
    private BroadcastReceiver mRingerModeReceiver;
    private final ScreenRecordHelper mScreenRecordHelper;
    private final ScreenshotHelper mScreenshotHelper;
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    private final GlobalActions.GlobalActionsManager mWindowManagerFuncs;

    public interface Action {
        View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater);

        boolean isEnabled();

        void onPress();

        default boolean shouldBeSeparated() {
            return false;
        }

        boolean showBeforeProvisioning();

        boolean showDuringKeyguard();
    }

    /* access modifiers changed from: private */
    public interface LongPressAction extends Action {
        boolean onLongPress();
    }

    /* access modifiers changed from: private */
    public static boolean shouldUseSeparatedView() {
        return true;
    }

    public GlobalActionsDialog(Context context, GlobalActions.GlobalActionsManager globalActionsManager) {
        boolean z = false;
        ToggleAction.State state = ToggleAction.State.Off;
        this.mAirplaneState = state;
        this.mEmergencyState = state;
        this.mIsWaitingForEcmExit = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass10 */

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                    String stringExtra = intent.getStringExtra("reason");
                    if (!"globalactions".equals(stringExtra)) {
                        GlobalActionsDialog.this.mHandler.sendMessage(GlobalActionsDialog.this.mHandler.obtainMessage(0, stringExtra));
                    }
                } else if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && GlobalActionsDialog.this.mIsWaitingForEcmExit) {
                    GlobalActionsDialog.this.mIsWaitingForEcmExit = false;
                    GlobalActionsDialog.this.changeAirplaneModeSystemSetting(true);
                }
            }
        };
        this.mPhoneStateListener = new PhoneStateListener() {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass11 */

            public void onServiceStateChanged(ServiceState serviceState) {
                if (GlobalActionsDialog.this.mHasTelephony) {
                    boolean z = serviceState.getState() == 3;
                    GlobalActionsDialog.this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
                    GlobalActionsDialog.this.mAirplaneModeOn.updateState(GlobalActionsDialog.this.mAirplaneState);
                    GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
                }
            }
        };
        this.mRingerModeReceiver = new BroadcastReceiver() {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass12 */

            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                    GlobalActionsDialog.this.mHandler.sendEmptyMessage(1);
                }
            }
        };
        this.mAirplaneModeObserver = new ContentObserver(new Handler()) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass13 */

            public void onChange(boolean z) {
                GlobalActionsDialog.this.onAirplaneModeChanged();
            }
        };
        this.mHandler = new Handler() {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass14 */

            public void handleMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    if (i == 1) {
                        GlobalActionsDialog.this.refreshSilentMode();
                        GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
                    } else if (i == 2) {
                        GlobalActionsDialog.this.handleShow();
                    }
                } else if (GlobalActionsDialog.this.mDialog != null) {
                    if ("dream".equals(message.obj)) {
                        GlobalActionsDialog.this.mDialog.dismissImmediately();
                    } else {
                        GlobalActionsDialog.this.mDialog.dismiss();
                    }
                    GlobalActionsDialog.this.mDialog = null;
                }
            }
        };
        this.mContext = new ContextThemeWrapper(context, C0015R$style.qs_theme);
        this.mWindowManagerFuncs = globalActionsManager;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mHasTelephony = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator != null && vibrator.hasVibrator()) {
            z = true;
        }
        this.mHasVibrator = z;
        this.mShowSilentToggle = !this.mContext.getResources().getBoolean(17891576);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
        this.mScreenshotHelper = new ScreenshotHelper(context);
        this.mScreenRecordHelper = new ScreenRecordHelper(context);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        UnlockMethodCache instance = UnlockMethodCache.getInstance(context);
        instance.addListener(new UnlockMethodCache.OnUnlockMethodChangedListener(instance) {
            /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$BHjCg9_WI1H1nuzvvDXExj6TPU */
            private final /* synthetic */ UnlockMethodCache f$1;

            {
                this.f$1 = r2;
            }

            @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
            public final void onUnlockMethodStateChanged() {
                GlobalActionsDialog.this.lambda$new$0$GlobalActionsDialog(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$new$0$GlobalActionsDialog(UnlockMethodCache unlockMethodCache) {
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null && actionsDialog.mPanelController != null) {
            this.mDialog.mPanelController.onDeviceLockStateChanged(!unlockMethodCache.canSkipBouncer());
        }
    }

    public void showDialog(boolean z, boolean z2, GlobalActionsPanelPlugin globalActionsPanelPlugin) {
        this.mKeyguardShowing = z;
        this.mDeviceProvisioned = z2;
        this.mPanelPlugin = globalActionsPanelPlugin;
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null) {
            actionsDialog.dismiss();
            this.mDialog = null;
            this.mHandler.sendEmptyMessage(2);
            return;
        }
        handleShow();
    }

    public void dismissDialog() {
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessage(0);
    }

    private void awakenIfNecessary() {
        IDreamManager iDreamManager = this.mDreamManager;
        if (iDreamManager != null) {
            try {
                if (iDreamManager.isDreaming()) {
                    this.mDreamManager.awaken();
                }
            } catch (RemoteException unused) {
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleShow() {
        awakenIfNecessary();
        this.mDialog = createDialog();
        prepareDialog();
        if (this.mAdapter.getCount() != 1 || !(this.mAdapter.getItem(0) instanceof SinglePressAction) || (this.mAdapter.getItem(0) instanceof LongPressAction)) {
            WindowManager.LayoutParams attributes = this.mDialog.getWindow().getAttributes();
            attributes.setTitle("ActionsDialog");
            attributes.layoutInDisplayCutoutMode = 1;
            this.mDialog.getWindow().setAttributes(attributes);
            this.mDialog.show();
            this.mWindowManagerFuncs.onGlobalActionsShown();
            this.mHandler.sendEmptyMessageDelayed(0, 60000);
            return;
        }
        ((SinglePressAction) this.mAdapter.getItem(0)).onPress();
    }

    private ActionsDialog createDialog() {
        GlobalActionsPanelPlugin.PanelViewController panelViewController;
        int i;
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mAudioManager, this.mHandler);
        }
        this.mAirplaneModeOn = new ToggleAction(17302449, 17302451, 17040152, 17040151, 17040150) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass1 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return false;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return true;
            }

            /* access modifiers changed from: package-private */
            @Override // com.android.systemui.globalactions.GlobalActionsDialog.ToggleAction
            public void onToggle(boolean z) {
                if (!GlobalActionsDialog.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    GlobalActionsDialog.this.changeAirplaneModeSystemSetting(z);
                    return;
                }
                GlobalActionsDialog.this.mIsWaitingForEcmExit = true;
                Intent intent = new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null);
                intent.addFlags(268435456);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.globalactions.GlobalActionsDialog.ToggleAction
            public void changeStateFromPress(boolean z) {
                if (GlobalActionsDialog.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = z ? ToggleAction.State.TurningOn : ToggleAction.State.TurningOff;
                    GlobalActionsDialog.this.mAirplaneState = this.mState;
                }
            }
        };
        onAirplaneModeChanged();
        if (canShowEmergencyModeDialog()) {
            this.mEmergencyModeOn = new ToggleAction(17302202, 17302202, 17039963, 17039962, 17039961) {
                /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass2 */

                /* access modifiers changed from: protected */
                @Override // com.android.systemui.globalactions.GlobalActionsDialog.ToggleAction
                public void changeStateFromPress(boolean z) {
                }

                @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
                public boolean showBeforeProvisioning() {
                    return true;
                }

                @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
                public boolean showDuringKeyguard() {
                    return true;
                }

                /* access modifiers changed from: package-private */
                @Override // com.android.systemui.globalactions.GlobalActionsDialog.ToggleAction
                public void onToggle(boolean z) {
                    Intent intent = new Intent();
                    intent.setClassName("com.sonymobile.emergencymode", "com.sonymobile.emergencymode.home.ui.SetupActivity");
                    intent.addFlags(268435456);
                    GlobalActionsDialog.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                }
            };
        }
        if (this.mEmergencyModeOn != null) {
            onEmergencyModeChanged();
        }
        this.mItems = new ArrayList<>();
        String[] stringArray = this.mContext.getResources().getStringArray(17236036);
        ArraySet arraySet = new ArraySet();
        this.mHasLogoutButton = false;
        this.mHasLockdownButton = false;
        int i2 = 0;
        while (true) {
            panelViewController = null;
            if (i2 >= stringArray.length) {
                break;
            }
            String str = stringArray[i2];
            if (!arraySet.contains(str)) {
                if ("power".equals(str)) {
                    this.mItems.add(new PowerAction());
                } else if ("emergencymode".equals(str)) {
                    if (this.mEmergencyModeOn != null && ((i = Settings.Secure.getInt(this.mContext.getContentResolver(), "somc.emergency_mode", 0)) == 0 || i == 1)) {
                        this.mItems.add(this.mEmergencyModeOn);
                    }
                } else if ("airplane".equals(str)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if ("bugreport".equals(str)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", 0) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(new BugReportAction());
                    }
                } else if ("silent".equals(str)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if ("users".equals(str)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if ("settings".equals(str)) {
                    this.mItems.add(getSettingsAction());
                } else if ("lockdown".equals(str)) {
                    if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lockdown_in_power_menu", 0, getCurrentUser().id) != 0 && shouldDisplayLockdown()) {
                        this.mItems.add(getLockdownAction());
                        this.mHasLockdownButton = true;
                    }
                } else if ("voiceassist".equals(str)) {
                    this.mItems.add(getVoiceAssistAction());
                } else if ("assist".equals(str)) {
                    this.mItems.add(getAssistAction());
                } else if ("restart".equals(str)) {
                    this.mItems.add(new RestartAction());
                } else if ("screenshot".equals(str)) {
                    this.mItems.add(new ScreenshotAction());
                } else if ("logout".equals(str)) {
                    if (this.mDevicePolicyManager.isLogoutEnabled() && getCurrentUser().id != 0) {
                        this.mItems.add(new LogoutAction());
                        this.mHasLogoutButton = true;
                    }
                } else if ("emergency".equals(str)) {
                    if (!this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
                        this.mItems.add(new EmergencyDialerAction());
                    }
                } else if (!"lutmobile".equals(str)) {
                    Log.e("GlobalActionsDialog", "Invalid global action key " + str);
                } else if (Build.TYPE.equals("userdebug") && isPackageInstalled("com.sonymobile.lut.android")) {
                    this.mItems.add(getLutAction());
                }
                arraySet.add(str);
            }
            i2++;
        }
        if (this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
            this.mItems.add(new EmergencyAffordanceAction());
        }
        this.mAdapter = new MyAdapter();
        GlobalActionsPanelPlugin globalActionsPanelPlugin = this.mPanelPlugin;
        if (globalActionsPanelPlugin != null) {
            panelViewController = globalActionsPanelPlugin.onPanelShown(new GlobalActionsPanelPlugin.Callbacks() {
                /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass3 */

                @Override // com.android.systemui.plugins.GlobalActionsPanelPlugin.Callbacks
                public void dismissGlobalActionsMenu() {
                    if (GlobalActionsDialog.this.mDialog != null) {
                        GlobalActionsDialog.this.mDialog.dismiss();
                    }
                }

                @Override // com.android.systemui.plugins.GlobalActionsPanelPlugin.Callbacks
                public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent) {
                    GlobalActionsDialog.this.mActivityStarter.startPendingIntentDismissingKeyguard(pendingIntent);
                }
            }, this.mKeyguardManager.isDeviceLocked());
        }
        ActionsDialog actionsDialog = new ActionsDialog(this.mContext, this.mAdapter, panelViewController);
        actionsDialog.setCanceledOnTouchOutside(false);
        actionsDialog.setKeyguardShowing(this.mKeyguardShowing);
        actionsDialog.setOnDismissListener(this);
        actionsDialog.setOnShowListener(this);
        return actionsDialog;
    }

    private boolean shouldDisplayLockdown() {
        int i = getCurrentUser().id;
        if (!this.mKeyguardManager.isDeviceSecure(i)) {
            return false;
        }
        int strongAuthForUser = this.mLockPatternUtils.getStrongAuthForUser(i);
        if (strongAuthForUser == 0 || strongAuthForUser == 4) {
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onUiModeChanged() {
        this.mContext.getTheme().applyStyle(this.mContext.getThemeResId(), true);
        ActionsDialog actionsDialog = this.mDialog;
        if (actionsDialog != null && actionsDialog.isShowing()) {
            this.mDialog.refreshDialog();
        }
    }

    public void destroy() {
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    /* access modifiers changed from: private */
    public final class PowerAction extends SinglePressAction implements LongPressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        private PowerAction() {
            super(17301552, 17040141);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.LongPressAction
        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.shutdown();
        }
    }

    private abstract class EmergencyAction extends SinglePressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        EmergencyAction(int i, int i2) {
            super(i, i2);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean shouldBeSeparated() {
            return GlobalActionsDialog.shouldUseSeparatedView();
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            int i;
            View create = super.create(context, view, viewGroup, layoutInflater);
            if (shouldBeSeparated()) {
                i = create.getResources().getColor(C0004R$color.global_actions_alert_text);
            } else {
                i = create.getResources().getColor(C0004R$color.global_actions_text);
            }
            TextView textView = (TextView) create.findViewById(16908299);
            textView.setTextColor(i);
            textView.setSelected(true);
            ((ImageView) create.findViewById(16908294)).getDrawable().setTint(i);
            return create;
        }
    }

    /* access modifiers changed from: private */
    public class EmergencyAffordanceAction extends EmergencyAction {
        EmergencyAffordanceAction() {
            super(17302201, 17040136);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            GlobalActionsDialog.this.mEmergencyAffordanceManager.performEmergencyCall();
        }
    }

    /* access modifiers changed from: private */
    public class EmergencyDialerAction extends EmergencyAction {
        private EmergencyDialerAction() {
            super(C0006R$drawable.ic_emergency_star, 17040136);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            MetricsLogger.action(GlobalActionsDialog.this.mContext, 1569);
            Intent intent = new Intent("com.android.phone.EmergencyDialer.DIAL");
            intent.addFlags(343932928);
            intent.putExtra("com.android.phone.EmergencyDialer.extra.ENTRY_TYPE", 2);
            GlobalActionsDialog.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    /* access modifiers changed from: private */
    public final class RestartAction extends SinglePressAction implements LongPressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        private RestartAction() {
            super(17302790, 17040142);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.LongPressAction
        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(false);
        }
    }

    /* access modifiers changed from: private */
    public class ScreenshotAction extends SinglePressAction implements LongPressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        public ScreenshotAction() {
            super(17302792, 17040143);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                /* class com.android.systemui.globalactions.GlobalActionsDialog.ScreenshotAction.AnonymousClass1 */

                public void run() {
                    GlobalActionsDialog.this.mScreenshotHelper.takeScreenshot(1, true, true, GlobalActionsDialog.this.mHandler);
                    MetricsLogger.action(GlobalActionsDialog.this.mContext, 1282);
                }
            }, 500);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.LongPressAction
        public boolean onLongPress() {
            if (FeatureFlagUtils.isEnabled(GlobalActionsDialog.this.mContext, "settings_screenrecord_long_press")) {
                GlobalActionsDialog.this.mScreenRecordHelper.launchRecordPrompt();
                return true;
            }
            onPress();
            return true;
        }
    }

    /* access modifiers changed from: private */
    public class BugReportAction extends SinglePressAction implements LongPressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        public BugReportAction() {
            super(17302453, 17039650);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            if (!ActivityManager.isUserAMonkey()) {
                GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                    /* class com.android.systemui.globalactions.GlobalActionsDialog.BugReportAction.AnonymousClass1 */

                    public void run() {
                        try {
                            MetricsLogger.action(GlobalActionsDialog.this.mContext, 292);
                            ActivityManager.getService().requestBugReport(1);
                        } catch (RemoteException unused) {
                        }
                    }
                }, 500);
            }
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.LongPressAction
        public boolean onLongPress() {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            try {
                MetricsLogger.action(GlobalActionsDialog.this.mContext, 293);
                ActivityManager.getService().requestBugReport(0);
            } catch (RemoteException unused) {
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public final class LogoutAction extends SinglePressAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        private LogoutAction() {
            super(17302503, 17040139);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
        public void onPress() {
            GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$LogoutAction$3H17sX2I_BqMu2dZ5Dekk1AEvU */

                public final void run() {
                    GlobalActionsDialog.LogoutAction.this.lambda$onPress$0$GlobalActionsDialog$LogoutAction();
                }
            }, 500);
        }

        public /* synthetic */ void lambda$onPress$0$GlobalActionsDialog$LogoutAction() {
            try {
                int i = GlobalActionsDialog.this.getCurrentUser().id;
                ActivityManager.getService().switchUser(0);
                ActivityManager.getService().stopUser(i, true, (IStopUserCallback) null);
            } catch (RemoteException e) {
                Log.e("GlobalActionsDialog", "Couldn't logout user " + e);
            }
        }
    }

    private Action getSettingsAction() {
        return new SinglePressAction(17302798, 17040144) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass4 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
            public void onPress() {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getAssistAction() {
        return new SinglePressAction(17302283, 17040134) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass5 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
            public void onPress() {
                Intent intent = new Intent("android.intent.action.ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getLutAction() {
        return new SinglePressAction(17303019, 17040140) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass6 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return false;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return false;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
            public void onPress() {
                Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
                intent.setComponent(new ComponentName("com.sonymobile.lut.android", "com.sonymobile.lut.android.DialogsActivity"));
                intent.setAction("android.intent.action.VIEW");
                intent.addCategory("android.intent.category.LAUNCHER");
                intent.addFlags(268468224);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getVoiceAssistAction() {
        return new SinglePressAction(17302839, 17040148) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass7 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
            public void onPress() {
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }
        };
    }

    private Action getLockdownAction() {
        return new SinglePressAction(17302456, 17040138) {
            /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass8 */

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showBeforeProvisioning() {
                return false;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
            public boolean showDuringKeyguard() {
                return true;
            }

            @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
            public void onPress() {
                new LockPatternUtils(GlobalActionsDialog.this.mContext).requireStrongAuth(32, -1);
                try {
                    WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
                    new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).post(new Runnable() {
                        /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$8$e_lPHSPze6aDu1H2zrV7VGwT6lM */

                        public final void run() {
                            GlobalActionsDialog.AnonymousClass8.this.lambda$onPress$0$GlobalActionsDialog$8();
                        }
                    });
                } catch (RemoteException e) {
                    Log.e("GlobalActionsDialog", "Error while trying to lock device.", e);
                }
            }

            public /* synthetic */ void lambda$onPress$0$GlobalActionsDialog$8() {
                GlobalActionsDialog.this.lockProfiles();
            }
        };
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void lockProfiles() {
        TrustManager trustManager = (TrustManager) this.mContext.getSystemService("trust");
        int i = getCurrentUser().id;
        int[] enabledProfileIds = ((UserManager) this.mContext.getSystemService("user")).getEnabledProfileIds(i);
        for (int i2 : enabledProfileIds) {
            if (i2 != i) {
                trustManager.setDeviceLockedForUser(i2, true);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private UserInfo getCurrentUser() {
        try {
            return ActivityManager.getService().getCurrentUser();
        } catch (RemoteException unused) {
            return null;
        }
    }

    private boolean isCurrentUserOwner() {
        UserInfo currentUser = getCurrentUser();
        return currentUser == null || currentUser.isPrimary();
    }

    private void addUsersToMenu(ArrayList<Action> arrayList) {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager.isUserSwitcherEnabled()) {
            List<UserInfo> users = userManager.getUsers();
            UserInfo currentUser = getCurrentUser();
            for (final UserInfo userInfo : users) {
                if (userInfo.supportsSwitchToByUser()) {
                    boolean z = true;
                    if (currentUser != null ? currentUser.id != userInfo.id : userInfo.id != 0) {
                        z = false;
                    }
                    String str = userInfo.iconPath;
                    Drawable createFromPath = str != null ? Drawable.createFromPath(str) : null;
                    StringBuilder sb = new StringBuilder();
                    String str2 = userInfo.name;
                    if (str2 == null) {
                        str2 = "Primary";
                    }
                    sb.append(str2);
                    sb.append(z ? " ✔" : "");
                    arrayList.add(new SinglePressAction(17302672, createFromPath, sb.toString()) {
                        /* class com.android.systemui.globalactions.GlobalActionsDialog.AnonymousClass9 */

                        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
                        public boolean showBeforeProvisioning() {
                            return false;
                        }

                        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
                        public boolean showDuringKeyguard() {
                            return true;
                        }

                        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action, com.android.systemui.globalactions.GlobalActionsDialog.SinglePressAction
                        public void onPress() {
                            try {
                                ActivityManager.getService().switchUser(userInfo.id);
                            } catch (RemoteException e) {
                                Log.e("GlobalActionsDialog", "Couldn't switch user " + e);
                            }
                        }
                    });
                }
            }
        }
    }

    private void prepareDialog() {
        refreshSilentMode();
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
        ToggleAction toggleAction = this.mEmergencyModeOn;
        if (toggleAction != null) {
            toggleAction.updateState(this.mEmergencyState);
        }
        this.mAdapter.notifyDataSetChanged();
        if (this.mShowSilentToggle) {
            this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void refreshSilentMode() {
        if (!this.mHasVibrator) {
            ((ToggleAction) this.mSilentModeAction).updateState(this.mAudioManager.getRingerMode() != 2 ? ToggleAction.State.On : ToggleAction.State.Off);
        }
    }

    public void onDismiss(DialogInterface dialogInterface) {
        this.mWindowManagerFuncs.onGlobalActionsHidden();
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException e) {
                Log.w("GlobalActionsDialog", e);
            }
        }
    }

    public void onShow(DialogInterface dialogInterface) {
        MetricsLogger.visible(this.mContext, 1568);
    }

    public class MyAdapter extends MultiListLayout.MultiListAdapter {
        public boolean areAllItemsEnabled() {
            return false;
        }

        public long getItemId(int i) {
            return (long) i;
        }

        public MyAdapter() {
        }

        private int countItems(boolean z) {
            int i = 0;
            for (int i2 = 0; i2 < GlobalActionsDialog.this.mItems.size(); i2++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i2);
                if (shouldBeShown(action) && action.shouldBeSeparated() == z) {
                    i++;
                }
            }
            return i;
        }

        private boolean shouldBeShown(Action action) {
            if (GlobalActionsDialog.this.mKeyguardShowing && !action.showDuringKeyguard()) {
                return false;
            }
            if (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning()) {
                return true;
            }
            return false;
        }

        @Override // com.android.systemui.MultiListLayout.MultiListAdapter
        public int countSeparatedItems() {
            return countItems(true);
        }

        @Override // com.android.systemui.MultiListLayout.MultiListAdapter
        public int countListItems() {
            return countItems(false);
        }

        public int getCount() {
            return countSeparatedItems() + countListItems();
        }

        public boolean isEnabled(int i) {
            return getItem(i).isEnabled();
        }

        public Action getItem(int i) {
            int i2 = 0;
            for (int i3 = 0; i3 < GlobalActionsDialog.this.mItems.size(); i3++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i3);
                if (shouldBeShown(action)) {
                    if (i2 == i) {
                        return action;
                    }
                    i2++;
                }
            }
            throw new IllegalArgumentException("position " + i + " out of range of showable actions, filtered count=" + getCount() + ", keyguardshowing=" + GlobalActionsDialog.this.mKeyguardShowing + ", provisioned=" + GlobalActionsDialog.this.mDeviceProvisioned);
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            View create = getItem(i).create(GlobalActionsDialog.this.mContext, view, viewGroup, LayoutInflater.from(GlobalActionsDialog.this.mContext));
            create.setOnClickListener(new View.OnClickListener(i) {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$MyAdapter$mHwNDdvU6gX4bdQUg9ucB10QA0w */
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    GlobalActionsDialog.MyAdapter.this.lambda$getView$0$GlobalActionsDialog$MyAdapter(this.f$1, view);
                }
            });
            create.setOnLongClickListener(new View.OnLongClickListener(i) {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$MyAdapter$VSUDyewgk86XHamZik1hS11jzxk */
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final boolean onLongClick(View view) {
                    return GlobalActionsDialog.MyAdapter.this.lambda$getView$1$GlobalActionsDialog$MyAdapter(this.f$1, view);
                }
            });
            return create;
        }

        public /* synthetic */ void lambda$getView$0$GlobalActionsDialog$MyAdapter(int i, View view) {
            onClickItem(i);
        }

        public /* synthetic */ boolean lambda$getView$1$GlobalActionsDialog$MyAdapter(int i, View view) {
            return onLongClickItem(i);
        }

        public boolean onLongClickItem(int i) {
            Action item = GlobalActionsDialog.this.mAdapter.getItem(i);
            if (!(item instanceof LongPressAction)) {
                return false;
            }
            if (GlobalActionsDialog.this.mDialog != null) {
                GlobalActionsDialog.this.mDialog.dismiss();
            } else {
                Log.w("GlobalActionsDialog", "Action long-clicked while mDialog is null.");
            }
            return ((LongPressAction) item).onLongPress();
        }

        public void onClickItem(int i) {
            Action item = GlobalActionsDialog.this.mAdapter.getItem(i);
            if (!(item instanceof SilentModeTriStateAction)) {
                if (GlobalActionsDialog.this.mDialog != null) {
                    GlobalActionsDialog.this.mDialog.dismiss();
                } else {
                    Log.w("GlobalActionsDialog", "Action clicked while mDialog is null.");
                }
                item.onPress();
            }
        }

        @Override // com.android.systemui.MultiListLayout.MultiListAdapter
        public boolean shouldBeSeparated(int i) {
            return getItem(i).shouldBeSeparated();
        }
    }

    /* access modifiers changed from: private */
    public static abstract class SinglePressAction implements Action {
        private final Drawable mIcon;
        private final int mIconResId;
        private final CharSequence mMessage;
        private final int mMessageResId;

        public String getStatus() {
            return null;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean isEnabled() {
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public abstract void onPress();

        protected SinglePressAction(int i, int i2) {
            this.mIconResId = i;
            this.mMessageResId = i2;
            this.mMessage = null;
            this.mIcon = null;
        }

        protected SinglePressAction(int i, Drawable drawable, CharSequence charSequence) {
            this.mIconResId = i;
            this.mMessageResId = 0;
            this.mMessage = charSequence;
            this.mIcon = drawable;
        }

        /* access modifiers changed from: protected */
        public int getActionLayoutId(Context context) {
            return C0010R$layout.global_actions_grid_item;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View inflate = layoutInflater.inflate(getActionLayoutId(context), viewGroup, false);
            ImageView imageView = (ImageView) inflate.findViewById(16908294);
            TextView textView = (TextView) inflate.findViewById(16908299);
            textView.setSelected(true);
            TextView textView2 = (TextView) inflate.findViewById(16909411);
            String status = getStatus();
            if (!TextUtils.isEmpty(status)) {
                textView2.setText(status);
            } else {
                textView2.setVisibility(8);
            }
            Drawable drawable = this.mIcon;
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                int i = this.mIconResId;
                if (i != 0) {
                    imageView.setImageDrawable(context.getDrawable(i));
                }
            }
            CharSequence charSequence = this.mMessage;
            if (charSequence != null) {
                textView.setText(charSequence);
            } else {
                textView.setText(this.mMessageResId);
            }
            return inflate;
        }
    }

    /* access modifiers changed from: private */
    public static abstract class ToggleAction implements Action {
        protected int mDisabledIconResid;
        protected int mDisabledStatusMessageResId;
        protected int mEnabledIconResId;
        protected int mEnabledStatusMessageResId;
        protected int mMessageResId;
        protected State mState = State.Off;

        /* access modifiers changed from: package-private */
        public abstract void onToggle(boolean z);

        /* access modifiers changed from: package-private */
        public void willCreate() {
        }

        /* access modifiers changed from: package-private */
        public enum State {
            Off(false),
            TurningOn(true),
            TurningOff(true),
            On(false);
            
            private final boolean inTransition;

            private State(boolean z) {
                this.inTransition = z;
            }

            public boolean inTransition() {
                return this.inTransition;
            }
        }

        public ToggleAction(int i, int i2, int i3, int i4, int i5) {
            this.mEnabledIconResId = i;
            this.mDisabledIconResid = i2;
            this.mMessageResId = i3;
            this.mEnabledStatusMessageResId = i4;
            this.mDisabledStatusMessageResId = i5;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            willCreate();
            View inflate = layoutInflater.inflate(C0010R$layout.global_actions_item, viewGroup, false);
            ImageView imageView = (ImageView) inflate.findViewById(16908294);
            TextView textView = (TextView) inflate.findViewById(16908299);
            TextView textView2 = (TextView) inflate.findViewById(16909411);
            boolean isEnabled = isEnabled();
            boolean z = true;
            if (textView != null) {
                textView.setText(this.mMessageResId);
                textView.setEnabled(isEnabled);
                textView.setSelected(true);
            }
            State state = this.mState;
            if (!(state == State.On || state == State.TurningOn)) {
                z = false;
            }
            if (imageView != null) {
                imageView.setImageDrawable(context.getDrawable(z ? this.mEnabledIconResId : this.mDisabledIconResid));
                imageView.setEnabled(isEnabled);
            }
            if (textView2 != null) {
                textView2.setText(z ? this.mEnabledStatusMessageResId : this.mDisabledStatusMessageResId);
                textView2.setVisibility(0);
                textView2.setEnabled(isEnabled);
            }
            inflate.setEnabled(isEnabled);
            return inflate;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public final void onPress() {
            if (this.mState.inTransition()) {
                Log.w("GlobalActionsDialog", "shouldn't be able to toggle when in transition");
                return;
            }
            boolean z = this.mState != State.On;
            onToggle(z);
            changeStateFromPress(z);
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean isEnabled() {
            return !this.mState.inTransition();
        }

        /* access modifiers changed from: protected */
        public void changeStateFromPress(boolean z) {
            this.mState = z ? State.On : State.Off;
        }

        public void updateState(State state) {
            this.mState = state;
        }
    }

    /* access modifiers changed from: private */
    public class SilentModeToggleAction extends ToggleAction {
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        public SilentModeToggleAction() {
            super(17302301, 17302300, 17040147, 17040146, 17040145);
        }

        /* access modifiers changed from: package-private */
        @Override // com.android.systemui.globalactions.GlobalActionsDialog.ToggleAction
        public void onToggle(boolean z) {
            if (z) {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(0);
            } else {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(2);
            }
        }
    }

    /* access modifiers changed from: private */
    public static class SilentModeTriStateAction implements Action, View.OnClickListener {
        private final int[] ITEM_IDS = {16909202, 16909203, 16909204};
        private final AudioManager mAudioManager;
        private final Handler mHandler;

        private int indexToRingerMode(int i) {
            return i;
        }

        private int ringerModeToIndex(int i) {
            return i;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean isEnabled() {
            return true;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public void onPress() {
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showBeforeProvisioning() {
            return false;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public boolean showDuringKeyguard() {
            return true;
        }

        SilentModeTriStateAction(AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandler = handler;
        }

        @Override // com.android.systemui.globalactions.GlobalActionsDialog.Action
        public View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater) {
            View inflate = layoutInflater.inflate(17367159, viewGroup, false);
            int ringerMode = this.mAudioManager.getRingerMode();
            ringerModeToIndex(ringerMode);
            int i = 0;
            while (i < 3) {
                View findViewById = inflate.findViewById(this.ITEM_IDS[i]);
                findViewById.setSelected(ringerMode == i);
                findViewById.setTag(Integer.valueOf(i));
                findViewById.setOnClickListener(this);
                i++;
            }
            return inflate;
        }

        public void onClick(View view) {
            if (view.getTag() instanceof Integer) {
                int intValue = ((Integer) view.getTag()).intValue();
                AudioManager audioManager = this.mAudioManager;
                indexToRingerMode(intValue);
                audioManager.setRingerMode(intValue);
                this.mHandler.sendEmptyMessageDelayed(0, 300);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onAirplaneModeChanged() {
        if (!this.mHasTelephony) {
            boolean z = false;
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
                z = true;
            }
            this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
            this.mAirplaneModeOn.updateState(this.mAirplaneState);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void changeAirplaneModeSystemSetting(boolean z) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", z ? 1 : 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra("state", z);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = z ? ToggleAction.State.On : ToggleAction.State.Off;
        }
    }

    /* access modifiers changed from: private */
    public static final class ActionsDialog extends Dialog implements DialogInterface, ColorExtractor.OnColorsChangedListener {
        private final MyAdapter mAdapter;
        private Drawable mBackgroundDrawable;
        private boolean mCancelOnUp;
        private final SysuiColorExtractor mColorExtractor;
        private final Context mContext;
        private EnableAccessibilityController mEnableAccessibilityController;
        private MultiListLayout mGlobalActionsLayout;
        private boolean mIntercepted;
        private boolean mKeyguardShowing;
        private final GlobalActionsPanelPlugin.PanelViewController mPanelController;
        private ResetOrientationData mResetOrientationData;
        private float mScrimAlpha;
        private boolean mShowing;
        private final IStatusBarService mStatusBarService;
        private final IBinder mToken = new Binder();
        private final int mWindowTouchSlop;

        ActionsDialog(Context context, MyAdapter myAdapter, GlobalActionsPanelPlugin.PanelViewController panelViewController) {
            super(context, C0015R$style.Theme_SystemUI_Dialog_GlobalActions);
            this.mContext = context;
            this.mAdapter = myAdapter;
            this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
            this.mStatusBarService = (IStatusBarService) Dependency.get(IStatusBarService.class);
            Window window = getWindow();
            window.requestFeature(1);
            window.getDecorView();
            window.getAttributes().systemUiVisibility |= 1792;
            window.setLayout(-1, -1);
            window.clearFlags(2);
            window.addFlags(17629472);
            window.setType(2020);
            setTitle(17040149);
            this.mPanelController = panelViewController;
            this.mWindowTouchSlop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
            initializeLayout();
        }

        private boolean shouldUsePanel() {
            GlobalActionsPanelPlugin.PanelViewController panelViewController = this.mPanelController;
            return (panelViewController == null || panelViewController.getPanelContent() == null) ? false : true;
        }

        private void initializePanel() {
            int rotation = RotationUtils.getRotation(this.mContext);
            boolean isRotationLocked = RotationPolicy.isRotationLocked(this.mContext);
            if (rotation == 0) {
                if (!isRotationLocked) {
                    if (this.mResetOrientationData == null) {
                        this.mResetOrientationData = new ResetOrientationData();
                        this.mResetOrientationData.locked = false;
                    }
                    this.mGlobalActionsLayout.post(new Runnable() {
                        /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$RJgtbpfP8gfKx4bDDYXf9gg3qxs */

                        public final void run() {
                            GlobalActionsDialog.ActionsDialog.this.lambda$initializePanel$1$GlobalActionsDialog$ActionsDialog();
                        }
                    });
                }
                setRotationSuggestionsEnabled(false);
                ((FrameLayout) findViewById(C0007R$id.global_actions_panel_container)).addView(this.mPanelController.getPanelContent(), new FrameLayout.LayoutParams(-1, -1));
                this.mBackgroundDrawable = this.mPanelController.getBackgroundDrawable();
                this.mScrimAlpha = 1.0f;
            } else if (isRotationLocked) {
                if (this.mResetOrientationData == null) {
                    this.mResetOrientationData = new ResetOrientationData();
                    ResetOrientationData resetOrientationData = this.mResetOrientationData;
                    resetOrientationData.locked = true;
                    resetOrientationData.rotation = rotation;
                }
                this.mGlobalActionsLayout.post(new Runnable() {
                    /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$KOOsXb68KZ6uVivL8nC_5NKKiBk */

                    public final void run() {
                        GlobalActionsDialog.ActionsDialog.this.lambda$initializePanel$0$GlobalActionsDialog$ActionsDialog();
                    }
                });
            }
        }

        public /* synthetic */ void lambda$initializePanel$0$GlobalActionsDialog$ActionsDialog() {
            RotationPolicy.setRotationLockAtAngle(this.mContext, false, 0);
        }

        public /* synthetic */ void lambda$initializePanel$1$GlobalActionsDialog$ActionsDialog() {
            RotationPolicy.setRotationLockAtAngle(this.mContext, true, 0);
        }

        private void initializeLayout() {
            setContentView(getGlobalActionsLayoutId(this.mContext));
            fixNavBarClipping();
            this.mGlobalActionsLayout = (MultiListLayout) findViewById(C0007R$id.global_actions_view);
            this.mGlobalActionsLayout.setOutsideTouchListener(new View.OnClickListener() {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$dNZefhFQEiKyxgSvmP1LBM0gtx4 */

                public final void onClick(View view) {
                    GlobalActionsDialog.ActionsDialog.this.lambda$initializeLayout$2$GlobalActionsDialog$ActionsDialog(view);
                }
            });
            ((View) this.mGlobalActionsLayout.getParent()).setOnClickListener(new View.OnClickListener() {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$qLnbwfmuMwGJ7JUyo3Qt6_cEh4 */

                public final void onClick(View view) {
                    GlobalActionsDialog.ActionsDialog.this.lambda$initializeLayout$3$GlobalActionsDialog$ActionsDialog(view);
                }
            });
            this.mGlobalActionsLayout.setListViewAccessibilityDelegate(new View.AccessibilityDelegate() {
                /* class com.android.systemui.globalactions.GlobalActionsDialog.ActionsDialog.AnonymousClass1 */

                public boolean dispatchPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
                    accessibilityEvent.getText().add(ActionsDialog.this.mContext.getString(17040149));
                    return true;
                }
            });
            this.mGlobalActionsLayout.setRotationListener(new MultiListLayout.RotationListener() {
                /* class com.android.systemui.globalactions.$$Lambda$yTIuIImgAFK3eAYSmNsa3QUABJI */

                @Override // com.android.systemui.MultiListLayout.RotationListener
                public final void onRotate(int i, int i2) {
                    GlobalActionsDialog.ActionsDialog.this.onRotate(i, i2);
                }
            });
            this.mGlobalActionsLayout.setAdapter(this.mAdapter);
            if (shouldUsePanel()) {
                initializePanel();
            }
            if (this.mBackgroundDrawable == null) {
                this.mBackgroundDrawable = new ScrimDrawable();
                this.mScrimAlpha = 0.2f;
            }
            getWindow().setBackgroundDrawable(this.mBackgroundDrawable);
        }

        public /* synthetic */ void lambda$initializeLayout$2$GlobalActionsDialog$ActionsDialog(View view) {
            dismiss();
        }

        public /* synthetic */ void lambda$initializeLayout$3$GlobalActionsDialog$ActionsDialog(View view) {
            dismiss();
        }

        private void fixNavBarClipping() {
            ViewGroup viewGroup = (ViewGroup) findViewById(16908290);
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
            ViewGroup viewGroup2 = (ViewGroup) viewGroup.getParent();
            viewGroup2.setClipChildren(false);
            viewGroup2.setClipToPadding(false);
        }

        private int getGlobalActionsLayoutId(Context context) {
            int rotation = RotationUtils.getRotation(context);
            boolean z = GlobalActionsDialog.isForceGridEnabled(context) || (shouldUsePanel() && rotation == 0);
            if (rotation == 2) {
                if (z) {
                    return C0010R$layout.global_actions_grid_seascape;
                }
                return C0010R$layout.global_actions_column_seascape;
            } else if (z) {
                return C0010R$layout.global_actions_grid;
            } else {
                return C0010R$layout.global_actions_column;
            }
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            if (EnableAccessibilityController.canEnableAccessibilityViaGesture(this.mContext)) {
                this.mEnableAccessibilityController = new EnableAccessibilityController(this.mContext, new Runnable() {
                    /* class com.android.systemui.globalactions.$$Lambda$56YJlzqUKSt4VudfSZci6be4LA */

                    public final void run() {
                        GlobalActionsDialog.ActionsDialog.this.dismiss();
                    }
                });
                super.setCanceledOnTouchOutside(false);
            } else {
                this.mEnableAccessibilityController = null;
                super.setCanceledOnTouchOutside(true);
            }
            super.onStart();
            this.mGlobalActionsLayout.updateList();
            if (this.mBackgroundDrawable instanceof ScrimDrawable) {
                this.mColorExtractor.addOnColorsChangedListener(this);
                updateColors(this.mColorExtractor.getNeutralColors(), false);
            }
        }

        private void updateColors(ColorExtractor.GradientColors gradientColors, boolean z) {
            ScrimDrawable scrimDrawable = this.mBackgroundDrawable;
            if (scrimDrawable instanceof ScrimDrawable) {
                scrimDrawable.setColor(gradientColors.getMainColor(), z);
                View decorView = getWindow().getDecorView();
                if (gradientColors.supportsDarkText()) {
                    decorView.setSystemUiVisibility(8208);
                } else {
                    decorView.setSystemUiVisibility(0);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            EnableAccessibilityController enableAccessibilityController = this.mEnableAccessibilityController;
            if (enableAccessibilityController != null) {
                enableAccessibilityController.onDestroy();
            }
            super.onStop();
            this.mColorExtractor.removeOnColorsChangedListener(this);
        }

        public void show() {
            super.show();
            this.mShowing = true;
            this.mBackgroundDrawable.setAlpha(0);
            MultiListLayout multiListLayout = this.mGlobalActionsLayout;
            multiListLayout.setTranslationX(multiListLayout.getAnimationOffsetX());
            MultiListLayout multiListLayout2 = this.mGlobalActionsLayout;
            multiListLayout2.setTranslationY(multiListLayout2.getAnimationOffsetY());
            this.mGlobalActionsLayout.setAlpha(0.0f);
            this.mGlobalActionsLayout.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(300).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$5VTsKfzFediL_BcyTcZsABCvLU0 */

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    GlobalActionsDialog.ActionsDialog.this.lambda$show$4$GlobalActionsDialog$ActionsDialog(valueAnimator);
                }
            }).start();
        }

        public /* synthetic */ void lambda$show$4$GlobalActionsDialog$ActionsDialog(ValueAnimator valueAnimator) {
            this.mBackgroundDrawable.setAlpha((int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * this.mScrimAlpha * 255.0f));
        }

        public void dismiss() {
            if (this.mShowing) {
                this.mShowing = false;
                this.mGlobalActionsLayout.setTranslationX(0.0f);
                this.mGlobalActionsLayout.setTranslationY(0.0f);
                this.mGlobalActionsLayout.setAlpha(1.0f);
                this.mGlobalActionsLayout.animate().alpha(0.0f).translationX(this.mGlobalActionsLayout.getAnimationOffsetX()).translationY(this.mGlobalActionsLayout.getAnimationOffsetY()).setDuration(300).withEndAction(new Runnable() {
                    /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$2QriKivfZQGysQ0teAWx7uBxqg */

                    public final void run() {
                        GlobalActionsDialog.ActionsDialog.this.lambda$dismiss$5$GlobalActionsDialog$ActionsDialog();
                    }
                }).setInterpolator(new SystemUIInterpolators$LogAccelerateInterpolator()).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    /* class com.android.systemui.globalactions.$$Lambda$GlobalActionsDialog$ActionsDialog$_0WJKduv0QvmLhPuj3fXKKiMDpo */

                    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                        GlobalActionsDialog.ActionsDialog.this.lambda$dismiss$6$GlobalActionsDialog$ActionsDialog(valueAnimator);
                    }
                }).start();
                dismissPanel();
                resetOrientation();
            }
        }

        public /* synthetic */ void lambda$dismiss$5$GlobalActionsDialog$ActionsDialog() {
            super.dismiss();
        }

        public /* synthetic */ void lambda$dismiss$6$GlobalActionsDialog$ActionsDialog(ValueAnimator valueAnimator) {
            this.mBackgroundDrawable.setAlpha((int) ((1.0f - ((Float) valueAnimator.getAnimatedValue()).floatValue()) * this.mScrimAlpha * 255.0f));
        }

        /* access modifiers changed from: package-private */
        public void dismissImmediately() {
            super.dismiss();
            this.mShowing = false;
            dismissPanel();
            resetOrientation();
        }

        private void dismissPanel() {
            GlobalActionsPanelPlugin.PanelViewController panelViewController = this.mPanelController;
            if (panelViewController != null) {
                panelViewController.onDismissed();
            }
        }

        private void setRotationSuggestionsEnabled(boolean z) {
            try {
                this.mStatusBarService.disable2ForUser(z ? 0 : 16, this.mToken, this.mContext.getPackageName(), Binder.getCallingUserHandle().getIdentifier());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        private void resetOrientation() {
            ResetOrientationData resetOrientationData = this.mResetOrientationData;
            if (resetOrientationData != null) {
                RotationPolicy.setRotationLockAtAngle(this.mContext, resetOrientationData.locked, resetOrientationData.rotation);
            }
            setRotationSuggestionsEnabled(true);
        }

        public void onColorsChanged(ColorExtractor colorExtractor, int i) {
            if (this.mKeyguardShowing) {
                if ((i & 2) != 0) {
                    updateColors(colorExtractor.getColors(2), true);
                }
            } else if ((i & 1) != 0) {
                updateColors(colorExtractor.getColors(1), true);
            }
        }

        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (this.mEnableAccessibilityController != null) {
                int actionMasked = motionEvent.getActionMasked();
                if (actionMasked == 0) {
                    View decorView = getWindow().getDecorView();
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    int i = this.mWindowTouchSlop;
                    if (x < (-i) || y < (-i) || x >= decorView.getWidth() + this.mWindowTouchSlop || y >= decorView.getHeight() + this.mWindowTouchSlop) {
                        this.mCancelOnUp = true;
                    }
                }
                try {
                    if (!this.mIntercepted) {
                        this.mIntercepted = this.mEnableAccessibilityController.onInterceptTouchEvent(motionEvent);
                        if (this.mIntercepted) {
                            long uptimeMillis = SystemClock.uptimeMillis();
                            motionEvent = MotionEvent.obtain(uptimeMillis, uptimeMillis, 3, 0.0f, 0.0f, 0);
                            motionEvent.setSource(4098);
                            this.mCancelOnUp = true;
                        }
                    } else {
                        boolean onTouchEvent = this.mEnableAccessibilityController.onTouchEvent(motionEvent);
                        if (actionMasked == 1) {
                            if (this.mCancelOnUp) {
                                cancel();
                            }
                            this.mCancelOnUp = false;
                            this.mIntercepted = false;
                        }
                        return onTouchEvent;
                    }
                } finally {
                    if (actionMasked == 1) {
                        if (this.mCancelOnUp) {
                            cancel();
                        }
                        this.mCancelOnUp = false;
                        this.mIntercepted = false;
                    }
                }
            }
            return super.dispatchTouchEvent(motionEvent);
        }

        public void setKeyguardShowing(boolean z) {
            this.mKeyguardShowing = z;
        }

        public void refreshDialog() {
            initializeLayout();
            this.mGlobalActionsLayout.updateList();
        }

        public void onRotate(int i, int i2) {
            if (this.mShowing) {
                refreshDialog();
            }
        }

        /* access modifiers changed from: private */
        public static class ResetOrientationData {
            public boolean locked;
            public int rotation;

            private ResetOrientationData() {
            }
        }
    }

    private static boolean isPanelDebugModeEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "global_actions_panel_debug_enabled", 0) == 1;
    }

    /* access modifiers changed from: private */
    public static boolean isForceGridEnabled(Context context) {
        return isPanelDebugModeEnabled(context);
    }

    private boolean isPackageInstalled(String str) {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (!(str == null || packageManager == null)) {
            try {
                if (Integer.valueOf(packageManager.getPackageInfo(str, 0).versionName.split("\\.")[0]).intValue() >= 8) {
                    return true;
                }
            } catch (NumberFormatException unused) {
                Log.e("GlobalActionsDialog", "Invalid version Number");
            } catch (PackageManager.NameNotFoundException unused2) {
                Log.e("GlobalActionsDialog", "LUT Application not installed");
            } catch (Exception e) {
                Log.e("GlobalActionsDialog", "Package Manager Exception: ", e);
            }
        }
        return false;
    }

    private void onEmergencyModeChanged() {
        boolean z = false;
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "somc.emergency_mode", 0) != 0) {
            z = true;
        }
        this.mEmergencyState = z ? ToggleAction.State.On : ToggleAction.State.Off;
        this.mEmergencyModeOn.updateState(this.mEmergencyState);
    }

    private boolean canShowEmergencyModeDialog() {
        try {
            this.mContext.getPackageManager().getApplicationInfo("com.sonymobile.emergencymode", 0);
            return ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class)).isCurrentUserSetup();
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }
}
