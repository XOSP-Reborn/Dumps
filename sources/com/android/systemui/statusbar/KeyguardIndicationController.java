package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.ViewClippingUtil;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.IllegalFormatConversionException;

public class KeyguardIndicationController implements StatusBarStateController.StateListener, UnlockMethodCache.OnUnlockMethodChangedListener {
    private final AccessibilityController mAccessibilityController;
    private final IBatteryStats mBatteryInfo;
    private int mBatteryLevel;
    private int mChargingSpeed;
    private int mChargingWattage;
    private final ViewClippingUtil.ClippingParameters mClippingParams;
    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private KeyguardIndicationTextView mDisclosure;
    private boolean mDozing;
    private ColorStateList mErrorColorState;
    private final int mFastThreshold;
    private final Handler mHandler;
    private ViewGroup mIndicationArea;
    private ColorStateList mInitialTextColorState;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final LockIcon mLockIcon;
    private final LockPatternUtils mLockPatternUtils;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    private String mMessageToShowOnScreenOn;
    private boolean mPowerCharged;
    private boolean mPowerPluggedIn;
    private boolean mPowerPluggedInWired;
    private String mRestingIndication;
    private final ShadeController mShadeController;
    private final int mSlowThreshold;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private final StatusBarStateController mStatusBarStateController;
    private KeyguardIndicationTextView mTextView;
    private final KeyguardUpdateMonitorCallback mTickReceiver;
    private CharSequence mTransientIndication;
    private ColorStateList mTransientTextColorState;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private final UserManager mUserManager;
    private boolean mVisible;
    private final SettableWakeLock mWakeLock;

    private String getTrustManagedIndication() {
        return null;
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
    }

    public KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon) {
        this(context, viewGroup, lockIcon, new LockPatternUtils(context), WakeLock.createPartial(context, "Doze:KeyguardIndication"), (ShadeController) Dependency.get(ShadeController.class), (AccessibilityController) Dependency.get(AccessibilityController.class), UnlockMethodCache.getInstance(context), (StatusBarStateController) Dependency.get(StatusBarStateController.class), KeyguardUpdateMonitor.getInstance(context));
    }

    @VisibleForTesting
    KeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon, LockPatternUtils lockPatternUtils, WakeLock wakeLock, ShadeController shadeController, AccessibilityController accessibilityController, UnlockMethodCache unlockMethodCache, StatusBarStateController statusBarStateController, KeyguardUpdateMonitor keyguardUpdateMonitor) {
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mClippingParams = new ViewClippingUtil.ClippingParameters() {
            /* class com.android.systemui.statusbar.KeyguardIndicationController.AnonymousClass1 */

            public boolean shouldFinish(View view) {
                return view == KeyguardIndicationController.this.mIndicationArea;
            }
        };
        this.mTickReceiver = new KeyguardUpdateMonitorCallback() {
            /* class com.android.systemui.statusbar.KeyguardIndicationController.AnonymousClass3 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onTimeChanged() {
                if (KeyguardIndicationController.this.mVisible) {
                    KeyguardIndicationController.this.updateIndication(false);
                }
            }
        };
        this.mHandler = new Handler() {
            /* class com.android.systemui.statusbar.KeyguardIndicationController.AnonymousClass4 */

            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 1) {
                    KeyguardIndicationController.this.hideTransientIndication();
                } else if (i == 2) {
                    KeyguardIndicationController.this.mLockIcon.setTransientBiometricsError(false);
                }
            }
        };
        this.mContext = context;
        this.mLockIcon = lockIcon;
        this.mShadeController = shadeController;
        this.mAccessibilityController = accessibilityController;
        this.mUnlockMethodCache = unlockMethodCache;
        this.mStatusBarStateController = statusBarStateController;
        this.mKeyguardUpdateMonitor = keyguardUpdateMonitor;
        LockIcon lockIcon2 = this.mLockIcon;
        if (lockIcon2 != null) {
            lockIcon2.setOnLongClickListener(new View.OnLongClickListener() {
                /* class com.android.systemui.statusbar.$$Lambda$KeyguardIndicationController$bqGTofRbajWF7T9LSeA5X_gxSW8 */

                public final boolean onLongClick(View view) {
                    return KeyguardIndicationController.this.handleLockLongClick(view);
                }
            });
            this.mLockIcon.setOnClickListener(new View.OnClickListener() {
                /* class com.android.systemui.statusbar.$$Lambda$KeyguardIndicationController$KgoVbt1hJQysK1ds1xLhviRDjE */

                public final void onClick(View view) {
                    KeyguardIndicationController.this.handleLockClick(view);
                }
            });
        }
        this.mWakeLock = new SettableWakeLock(wakeLock, "KeyguardIndication");
        this.mLockPatternUtils = lockPatternUtils;
        Resources resources = context.getResources();
        this.mSlowThreshold = resources.getInteger(C0008R$integer.config_chargingSlowlyThreshold);
        this.mFastThreshold = resources.getInteger(C0008R$integer.config_chargingFastThreshold);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        setIndicationArea(viewGroup);
        updateDisclosure();
        this.mKeyguardUpdateMonitor.registerCallback(getKeyguardCallback());
        this.mKeyguardUpdateMonitor.registerCallback(this.mTickReceiver);
        this.mStatusBarStateController.addCallback(this);
        this.mUnlockMethodCache.addListener(this);
    }

    public void setIndicationArea(ViewGroup viewGroup) {
        this.mIndicationArea = viewGroup;
        this.mTextView = (KeyguardIndicationTextView) viewGroup.findViewById(C0007R$id.keyguard_indication_text);
        KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
        this.mInitialTextColorState = keyguardIndicationTextView != null ? keyguardIndicationTextView.getTextColors() : ColorStateList.valueOf(-1);
        this.mErrorColorState = Utils.getColorError(this.mContext);
        this.mDisclosure = (KeyguardIndicationTextView) viewGroup.findViewById(C0007R$id.keyguard_indication_enterprise_disclosure);
        updateIndication(false);
    }

    /* access modifiers changed from: private */
    public boolean handleLockLongClick(View view) {
        this.mLockscreenGestureLogger.write(191, 0, 0);
        showTransientIndication(C0014R$string.keyguard_indication_trust_disabled);
        this.mKeyguardUpdateMonitor.onLockIconPressed();
        this.mLockPatternUtils.requireCredentialEntry(KeyguardUpdateMonitor.getCurrentUser());
        return true;
    }

    /* access modifiers changed from: private */
    public void handleLockClick(View view) {
        if (this.mAccessibilityController.isAccessibilityEnabled()) {
            this.mShadeController.animateCollapsePanels(0, true);
        }
    }

    /* access modifiers changed from: protected */
    public KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (this.mUpdateMonitorCallback == null) {
            this.mUpdateMonitorCallback = new BaseKeyguardCallback();
        }
        return this.mUpdateMonitorCallback;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDisclosure() {
        DevicePolicyManager devicePolicyManager = this.mDevicePolicyManager;
        if (devicePolicyManager != null) {
            if (this.mDozing || !devicePolicyManager.isDeviceManaged()) {
                this.mDisclosure.setVisibility(8);
                return;
            }
            CharSequence deviceOwnerOrganizationName = this.mDevicePolicyManager.getDeviceOwnerOrganizationName();
            if (deviceOwnerOrganizationName != null) {
                this.mDisclosure.switchIndication(this.mContext.getResources().getString(C0014R$string.do_disclosure_with_name, deviceOwnerOrganizationName));
            } else {
                this.mDisclosure.switchIndication(C0014R$string.do_disclosure_generic);
            }
            this.mDisclosure.setVisibility(0);
        }
    }

    public void setVisible(boolean z) {
        this.mVisible = z;
        this.mIndicationArea.setVisibility(z ? 0 : 8);
        if (z) {
            if (!this.mHandler.hasMessages(1)) {
                hideTransientIndication();
            }
            updateIndication(false);
        } else if (!z) {
            hideTransientIndication();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public String getTrustGrantedIndication() {
        return this.mContext.getString(C0014R$string.keyguard_indication_trust_unlocked);
    }

    public void hideTransientIndicationDelayed(long j) {
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), j);
    }

    public void showTransientIndication(int i) {
        showTransientIndication(this.mContext.getResources().getString(i));
    }

    public void showTransientIndication(CharSequence charSequence) {
        showTransientIndication(charSequence, this.mInitialTextColorState);
    }

    public void showTransientIndication(CharSequence charSequence, ColorStateList colorStateList) {
        this.mTransientIndication = charSequence;
        this.mTransientTextColorState = colorStateList;
        this.mHandler.removeMessages(1);
        if (this.mDozing && !TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(true);
            hideTransientIndicationDelayed(5000);
        }
        updateIndication(false);
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication(false);
        }
    }

    public final void updateThemeColors(int i) {
        this.mInitialTextColorState = ColorStateList.valueOf(i);
        this.mErrorColorState = this.mInitialTextColorState;
    }

    /* access modifiers changed from: protected */
    public final void updateIndication(boolean z) {
        if (TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(false);
        }
        if (!this.mVisible) {
            return;
        }
        if (this.mDozing) {
            this.mTextView.setTextColor(-1);
            if (!TextUtils.isEmpty(this.mTransientIndication)) {
                this.mTextView.switchIndication(this.mTransientIndication);
            } else if (this.mPowerPluggedIn) {
                String computePowerIndication = computePowerIndication();
                if (z) {
                    animateText(this.mTextView, computePowerIndication);
                } else {
                    this.mTextView.switchIndication(computePowerIndication);
                }
            } else {
                this.mTextView.switchIndication(NumberFormat.getPercentInstance().format((double) (((float) this.mBatteryLevel) / 100.0f)));
            }
        } else {
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            String trustGrantedIndication = getTrustGrantedIndication();
            String trustManagedIndication = getTrustManagedIndication();
            if (!this.mUserManager.isUserUnlocked(currentUser)) {
                this.mTextView.switchIndication(17040400);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else if (!TextUtils.isEmpty(this.mTransientIndication)) {
                this.mTextView.switchIndication(this.mTransientIndication);
                this.mTextView.setTextColor(this.mTransientTextColorState);
            } else if (!TextUtils.isEmpty(trustGrantedIndication) && this.mKeyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication(trustGrantedIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else if (this.mPowerPluggedIn) {
                String computePowerIndication2 = computePowerIndication();
                this.mTextView.setTextColor(this.mInitialTextColorState);
                if (z) {
                    animateText(this.mTextView, computePowerIndication2);
                } else {
                    this.mTextView.switchIndication(computePowerIndication2);
                }
            } else if (TextUtils.isEmpty(trustManagedIndication) || !this.mKeyguardUpdateMonitor.getUserTrustIsManaged(currentUser) || this.mKeyguardUpdateMonitor.getUserHasTrust(currentUser)) {
                this.mTextView.switchIndication(this.mRestingIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            } else {
                this.mTextView.switchIndication(trustManagedIndication);
                this.mTextView.setTextColor(this.mInitialTextColorState);
            }
        }
    }

    private void animateText(final KeyguardIndicationTextView keyguardIndicationTextView, final String str) {
        int integer = this.mContext.getResources().getInteger(C0008R$integer.wired_charging_keyguard_text_animation_distance);
        int integer2 = this.mContext.getResources().getInteger(C0008R$integer.wired_charging_keyguard_text_animation_duration_up);
        final int integer3 = this.mContext.getResources().getInteger(C0008R$integer.wired_charging_keyguard_text_animation_duration_down);
        keyguardIndicationTextView.animate().cancel();
        final float translationY = keyguardIndicationTextView.getTranslationY();
        ViewClippingUtil.setClippingDeactivated(keyguardIndicationTextView, true, this.mClippingParams);
        keyguardIndicationTextView.animate().translationYBy((float) integer).setInterpolator(Interpolators.LINEAR).setDuration((long) integer2).setListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.statusbar.KeyguardIndicationController.AnonymousClass2 */
            private boolean mCancelled;

            public void onAnimationStart(Animator animator) {
                keyguardIndicationTextView.switchIndication(str);
            }

            public void onAnimationCancel(Animator animator) {
                keyguardIndicationTextView.setTranslationY(translationY);
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animator) {
                if (this.mCancelled) {
                    ViewClippingUtil.setClippingDeactivated(keyguardIndicationTextView, false, KeyguardIndicationController.this.mClippingParams);
                } else {
                    keyguardIndicationTextView.animate().setDuration((long) integer3).setInterpolator(Interpolators.BOUNCE).translationY(translationY).setListener(new AnimatorListenerAdapter() {
                        /* class com.android.systemui.statusbar.KeyguardIndicationController.AnonymousClass2.AnonymousClass1 */

                        public void onAnimationCancel(Animator animator) {
                            AnonymousClass2 r0 = AnonymousClass2.this;
                            keyguardIndicationTextView.setTranslationY(translationY);
                        }

                        public void onAnimationEnd(Animator animator) {
                            AnonymousClass2 r1 = AnonymousClass2.this;
                            ViewClippingUtil.setClippingDeactivated(keyguardIndicationTextView, false, KeyguardIndicationController.this.mClippingParams);
                        }
                    });
                }
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private String computePowerIndication() {
        int i;
        if (this.mPowerCharged) {
            return this.mContext.getResources().getString(C0014R$string.keyguard_charged);
        }
        try {
            this.mBatteryInfo.computeChargeTimeRemaining();
        } catch (RemoteException e) {
            Log.e("KeyguardIndication", "Error calling IBatteryStats: ", e);
        }
        this.mChargingSpeed = 1;
        if (this.mPowerPluggedInWired) {
            int i2 = this.mChargingSpeed;
            if (i2 == 0) {
                i = C0014R$string.keyguard_plugged_in_charging_slowly;
            } else if (i2 != 2) {
                i = C0014R$string.keyguard_plugged_in;
            } else {
                i = C0014R$string.keyguard_plugged_in_charging_fast;
            }
        } else {
            i = C0014R$string.keyguard_plugged_in_wireless;
        }
        String format = NumberFormat.getPercentInstance().format((double) (((float) this.mBatteryLevel) / 100.0f));
        try {
            return this.mContext.getResources().getString(i, format);
        } catch (IllegalFormatConversionException unused) {
            return this.mContext.getResources().getString(i);
        }
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void setDozing(boolean z) {
        if (this.mDozing != z) {
            this.mDozing = z;
            updateIndication(false);
            updateDisclosure();
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("KeyguardIndicationController:");
        printWriter.println("  mTransientTextColorState: " + this.mTransientTextColorState);
        printWriter.println("  mInitialTextColorState: " + this.mInitialTextColorState);
        printWriter.println("  mPowerPluggedInWired: " + this.mPowerPluggedInWired);
        printWriter.println("  mPowerPluggedIn: " + this.mPowerPluggedIn);
        printWriter.println("  mPowerCharged: " + this.mPowerCharged);
        printWriter.println("  mChargingSpeed: " + this.mChargingSpeed);
        printWriter.println("  mChargingWattage: " + this.mChargingWattage);
        printWriter.println("  mMessageToShowOnScreenOn: " + this.mMessageToShowOnScreenOn);
        printWriter.println("  mDozing: " + this.mDozing);
        printWriter.println("  mBatteryLevel: " + this.mBatteryLevel);
        StringBuilder sb = new StringBuilder();
        sb.append("  mTextView.getText(): ");
        KeyguardIndicationTextView keyguardIndicationTextView = this.mTextView;
        sb.append((Object) (keyguardIndicationTextView == null ? null : keyguardIndicationTextView.getText()));
        printWriter.println(sb.toString());
        printWriter.println("  computePowerIndication(): " + computePowerIndication());
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        setDozing(z);
    }

    @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
    public void onUnlockMethodStateChanged() {
        updateIndication(!this.mDozing);
    }

    /* access modifiers changed from: protected */
    public class BaseKeyguardCallback extends KeyguardUpdateMonitorCallback {
        protected BaseKeyguardCallback() {
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
            int i = batteryStatus.status;
            boolean z = false;
            boolean z2 = i == 2 || i == 5;
            boolean z3 = KeyguardIndicationController.this.mPowerPluggedIn;
            KeyguardIndicationController.this.mPowerPluggedInWired = batteryStatus.isPluggedInWired() && z2;
            KeyguardIndicationController.this.mPowerPluggedIn = batteryStatus.isPluggedIn() && z2;
            KeyguardIndicationController.this.mPowerCharged = batteryStatus.isCharged();
            KeyguardIndicationController.this.mChargingWattage = batteryStatus.maxChargingWattage;
            KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
            keyguardIndicationController.mChargingSpeed = batteryStatus.getChargingSpeed(keyguardIndicationController.mSlowThreshold, KeyguardIndicationController.this.mFastThreshold);
            KeyguardIndicationController.this.mBatteryLevel = batteryStatus.level;
            KeyguardIndicationController keyguardIndicationController2 = KeyguardIndicationController.this;
            if (!z3 && keyguardIndicationController2.mPowerPluggedInWired) {
                z = true;
            }
            keyguardIndicationController2.updateIndication(z);
            if (!KeyguardIndicationController.this.mDozing) {
                return;
            }
            if (!z3 && KeyguardIndicationController.this.mPowerPluggedIn) {
                KeyguardIndicationController keyguardIndicationController3 = KeyguardIndicationController.this;
                keyguardIndicationController3.showTransientIndication(keyguardIndicationController3.computePowerIndication());
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
            } else if (z3 && !KeyguardIndicationController.this.mPowerPluggedIn) {
                KeyguardIndicationController.this.hideTransientIndication();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardVisibilityChanged(boolean z) {
            if (z) {
                KeyguardIndicationController.this.updateDisclosure();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (instance.isUnlockingWithBiometricAllowed()) {
                animatePadlockError();
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, KeyguardIndicationController.this.mInitialTextColorState);
                } else if (instance.isScreenOn()) {
                    KeyguardIndicationController.this.showTransientIndication(str);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(1300);
                }
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
            KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(KeyguardIndicationController.this.mContext);
            if (!shouldSuppressBiometricError(i, biometricSourceType, instance)) {
                animatePadlockError();
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(str, KeyguardIndicationController.this.mInitialTextColorState);
                } else if (instance.isScreenOn()) {
                    KeyguardIndicationController.this.showTransientIndication(str);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                } else {
                    KeyguardIndicationController.this.mMessageToShowOnScreenOn = str;
                }
            }
        }

        private void animatePadlockError() {
            KeyguardIndicationController.this.mLockIcon.setTransientBiometricsError(true);
            KeyguardIndicationController.this.mHandler.removeMessages(2);
            KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300);
        }

        private boolean shouldSuppressBiometricError(int i, BiometricSourceType biometricSourceType, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
                return shouldSuppressFingerprintError(i, keyguardUpdateMonitor);
            }
            if (biometricSourceType == BiometricSourceType.FACE) {
                return shouldSuppressFaceError(i, keyguardUpdateMonitor);
            }
            return false;
        }

        private boolean shouldSuppressFingerprintError(int i, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            return (!keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() && i != 9) || i == 5;
        }

        private boolean shouldSuppressFaceError(int i, KeyguardUpdateMonitor keyguardUpdateMonitor) {
            return (!keyguardUpdateMonitor.isUnlockingWithBiometricAllowed() && i != 9) || i == 5;
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onTrustAgentErrorMessage(CharSequence charSequence) {
            KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
            keyguardIndicationController.showTransientIndication(charSequence, keyguardIndicationController.mErrorColorState);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOn() {
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn != null) {
                KeyguardIndicationController keyguardIndicationController = KeyguardIndicationController.this;
                keyguardIndicationController.showTransientIndication(keyguardIndicationController.mMessageToShowOnScreenOn, KeyguardIndicationController.this.mErrorColorState);
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
            if (z) {
                KeyguardIndicationController.this.hideTransientIndication();
                KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
            super.onBiometricAuthenticated(i, biometricSourceType);
            KeyguardIndicationController.this.mHandler.sendEmptyMessage(1);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserUnlocked() {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication(false);
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardBouncerChanged(boolean z) {
            if (KeyguardIndicationController.this.mLockIcon != null) {
                KeyguardIndicationController.this.mLockIcon.setBouncerVisible(z);
            }
        }
    }
}
