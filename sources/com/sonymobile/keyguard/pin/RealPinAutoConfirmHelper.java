package com.sonymobile.keyguard.pin;

import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.PasswordTextView;
import com.android.keyguard.SecurityMessageDisplay;

public class RealPinAutoConfirmHelper implements PinAutoConfirmHelper, PasswordEntryListener, PinUnlockListener {
    private final View mConfirmButtonView;
    private boolean mFeatureEnabled = true;
    private final Handler mHandler;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private LockPatternUtils mLockPatternUtils;
    private final PasswordTextView mPasswordEntry;
    private final PinAutoUnlockSettingsSecureWrapper mPinAutoUnlockSettingsSecureWrapper;
    private final SecurityMessageDisplay mSecurityMessageDisplay;
    private final Vibrator mVibrator;

    private boolean isEnabledForPinLength(int i) {
        return i == 4;
    }

    public RealPinAutoConfirmHelper(View view, PinAutoUnlockSettingsSecureWrapper pinAutoUnlockSettingsSecureWrapper, KeyguardUpdateMonitor keyguardUpdateMonitor, LockPatternUtils lockPatternUtils, Handler handler, Vibrator vibrator, PasswordTextView passwordTextView, SecurityMessageDisplay securityMessageDisplay) {
        this.mConfirmButtonView = view;
        this.mPinAutoUnlockSettingsSecureWrapper = pinAutoUnlockSettingsSecureWrapper;
        this.mKeyguardUpdateMonitor = keyguardUpdateMonitor;
        this.mLockPatternUtils = lockPatternUtils;
        this.mHandler = handler;
        this.mVibrator = vibrator;
        this.mPasswordEntry = passwordTextView;
        this.mSecurityMessageDisplay = securityMessageDisplay;
    }

    @Override // com.sonymobile.keyguard.pin.PinAutoConfirmHelper
    public final void updateEnterKeyVisibility() {
        boolean isAutoUnlockEnabled = isAutoUnlockEnabled();
        this.mConfirmButtonView.setVisibility(isAutoUnlockEnabled ? 4 : 0);
        this.mConfirmButtonView.setSoundEffectsEnabled(!isAutoUnlockEnabled);
        this.mConfirmButtonView.setClickable(!isAutoUnlockEnabled);
    }

    private boolean isAutoUnlockEnabled() {
        return this.mPinAutoUnlockSettingsSecureWrapper.isAutoUnlockEnabled() && this.mFeatureEnabled;
    }

    @Override // com.sonymobile.keyguard.pin.PinAutoConfirmHelper
    public final void disableAutoUnlockIfAppropriate() {
        this.mFeatureEnabled = this.mLockPatternUtils.getCurrentFailedPasswordAttempts(KeyguardUpdateMonitor.getCurrentUser()) < 15;
    }

    @Override // com.sonymobile.keyguard.pin.PasswordEntryListener
    public final void onPasswordLengthIncreased(int i) {
        if (isEnabledForPinLength(i) && this.mFeatureEnabled && this.mPinAutoUnlockSettingsSecureWrapper.isAutoUnlockEnabled()) {
            this.mPasswordEntry.setContentLocked(true);
            this.mHandler.postDelayed(new Runnable() {
                /* class com.sonymobile.keyguard.pin.RealPinAutoConfirmHelper.AnonymousClass1 */

                public void run() {
                    RealPinAutoConfirmHelper.this.mPasswordEntry.setContentLocked(false);
                    RealPinAutoConfirmHelper.this.mConfirmButtonView.performClick();
                }
            }, 100);
        }
    }

    @Override // com.sonymobile.keyguard.pin.PinUnlockListener
    public final void onUnlockSucceded(int i) {
        if (isEnabledForPinLength(i) && !isAutoUnlockEnabled()) {
            this.mPinAutoUnlockSettingsSecureWrapper.enableAutoUnlock();
            this.mPasswordEntry.setContentLocked(false);
        }
    }

    @Override // com.sonymobile.keyguard.pin.PinUnlockListener
    public final void onUnlockFailed() {
        if (isAutoUnlockEnabled() && this.mConfirmButtonView != null) {
            this.mPasswordEntry.setContentLocked(true);
            this.mVibrator.vibrate(100);
            this.mHandler.postDelayed(new Runnable() {
                /* class com.sonymobile.keyguard.pin.RealPinAutoConfirmHelper.AnonymousClass2 */

                public void run() {
                    if (RealPinAutoConfirmHelper.this.mPasswordEntry.isContentLocked()) {
                        RealPinAutoConfirmHelper.this.mPasswordEntry.setContentLocked(false);
                        RealPinAutoConfirmHelper.this.mPasswordEntry.reset(true, true);
                    }
                }
            }, 1000);
        }
    }
}
