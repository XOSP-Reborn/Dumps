package com.sonymobile.systemui.lockscreen;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewGroup;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.sonymobile.keyguard.clock.picker.ClockPickerController;
import com.sonymobile.keyguard.clock.picker.ClockPickerStarter;
import com.sonymobile.keyguard.clock.picker.StartClockPickerTouchListener;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginFactoryLoader;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginMetaDataLoader;
import com.sonymobile.keyguard.plugininfrastructure.RealClockPluginUserSelectionHandler;
import com.sonymobile.keyguard.plugininfrastructure.RealCustomizationResourceLoader;
import com.sonymobile.keyguard.plugininfrastructure.RealDefaultKeyguardFactoryProvider;
import com.sonymobile.keyguard.plugininfrastructure.RealKeyguardPluginSecureSettingsAbstraction;

public class LockscreenClockController implements ClockPickerStarter, ConfigurationChangedReceiver {
    private ClockPickerController mClockPickerController;
    private Context mContext;
    private int mCurrentUserId;
    private ViewGroup mKeyguardStatusView;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenClockController.AnonymousClass2 */

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardBouncerChanged(boolean z) {
            if (z) {
                LockscreenClockController.this.exitClockPicker();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitching(int i) {
            LockscreenClockController.this.clearClockPickerView();
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            LockscreenClockController.this.mCurrentUserId = i;
        }
    };
    private final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        /* class com.sonymobile.systemui.lockscreen.LockscreenClockController.AnonymousClass1 */

        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedGoingToSleep() {
            LockscreenClockController.this.exitClockPicker();
        }
    };

    public LockscreenClockController(Context context) {
        this.mContext = context;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        ((WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class)).addObserver(this.mWakefulnessObserver);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    public final void initClockPickerStarter(ViewGroup viewGroup) {
        ViewGroup viewGroup2 = this.mKeyguardStatusView;
        if (viewGroup2 != null) {
            viewGroup2.setOnTouchListener(null);
            this.mKeyguardStatusView = null;
        }
        this.mKeyguardStatusView = viewGroup;
        this.mKeyguardStatusView.setOnTouchListener(new StartClockPickerTouchListener(this.mContext, this));
    }

    private void initClockController() {
        RealDefaultKeyguardFactoryProvider realDefaultKeyguardFactoryProvider = new RealDefaultKeyguardFactoryProvider(new KeyguardPluginMetaDataLoader(this.mContext), new RealCustomizationResourceLoader(this.mContext));
        RealKeyguardPluginSecureSettingsAbstraction realKeyguardPluginSecureSettingsAbstraction = new RealKeyguardPluginSecureSettingsAbstraction(this.mContext.getContentResolver(), this.mCurrentUserId);
        KeyguardPluginMetaDataLoader keyguardPluginMetaDataLoader = new KeyguardPluginMetaDataLoader(this.mContext);
        RealClockPluginUserSelectionHandler realClockPluginUserSelectionHandler = new RealClockPluginUserSelectionHandler(keyguardPluginMetaDataLoader, realKeyguardPluginSecureSettingsAbstraction, realDefaultKeyguardFactoryProvider);
        this.mClockPickerController = new ClockPickerController(this.mContext, keyguardPluginMetaDataLoader, new KeyguardPluginFactoryLoader(this.mContext, realDefaultKeyguardFactoryProvider, realClockPluginUserSelectionHandler), realDefaultKeyguardFactoryProvider, realClockPluginUserSelectionHandler);
    }

    public final void clearClockPickerView() {
        if (this.mClockPickerController != null) {
            exitClockPicker();
            this.mClockPickerController = null;
        }
    }

    public final void exitClockPicker() {
        ClockPickerController clockPickerController = this.mClockPickerController;
        if (clockPickerController != null) {
            clockPickerController.exitClockPicker(null, false);
        }
    }

    @Override // com.sonymobile.keyguard.clock.picker.ClockPickerStarter
    public final void displayClockPluginPicker() {
        if (this.mClockPickerController == null) {
            initClockController();
        }
        this.mClockPickerController.startClockPicker(this.mKeyguardStatusView);
    }

    @Override // com.android.systemui.ConfigurationChangedReceiver
    public final void onConfigurationChanged(Configuration configuration) {
        clearClockPickerView();
    }
}
