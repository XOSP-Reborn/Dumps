package com.android.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;
import com.sonymobile.keyguard.pin.PinUnlockListener;
import java.util.Arrays;

public abstract class KeyguardAbsKeyInputView extends LinearLayout implements KeyguardSecurityView, EmergencyButton.EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    private final Configuration mConfiguration;
    private CountDownTimer mCountdownTimer;
    private boolean mDismissing;
    protected View mEcaView;
    protected boolean mEnableHaptics;
    protected LockPatternUtils mLockPatternUtils;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    private PinUnlockListener mPinUnlockListener;
    protected boolean mResumed;
    protected SecurityMessageDisplay mSecurityMessageDisplay;

    /* access modifiers changed from: protected */
    public abstract byte[] getPasswordText();

    /* access modifiers changed from: protected */
    public abstract int getPasswordTextViewId();

    /* access modifiers changed from: protected */
    public abstract int getPromptReasonStringRes(int i);

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean needsInput() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onLocaleChanged() {
    }

    /* access modifiers changed from: protected */
    public abstract void resetPasswordText(boolean z, boolean z2);

    /* access modifiers changed from: protected */
    public abstract void resetState();

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryEnabled(boolean z);

    /* access modifiers changed from: protected */
    public abstract void setPasswordEntryInputEnabled(boolean z);

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long j) {
        return j != 0;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCountdownTimer = null;
        this.mConfiguration = new Configuration();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        this.mCallback = keyguardSecurityCallback;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mEnableHaptics = this.mLockPatternUtils.isTactileFeedbackEnabled();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void reset() {
        this.mDismissing = false;
        resetPasswordText(false, false);
        long lockoutAttemptDeadline = this.mLockPatternUtils.getLockoutAttemptDeadline(KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(lockoutAttemptDeadline)) {
            handleAttemptLockout(lockoutAttemptDeadline);
        } else {
            resetState();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if ((this.mConfiguration.updateFrom(configuration) & 4) != 0) {
            onLocaleChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mLockPatternUtils = new LockPatternUtils(((LinearLayout) this).mContext);
        this.mEcaView = findViewById(R$id.keyguard_selector_fade_container);
        EmergencyButton emergencyButton = (EmergencyButton) findViewById(R$id.emergency_call_button);
        if (emergencyButton != null) {
            emergencyButton.setCallback(this);
        }
        this.mConfiguration.setTo(((LinearLayout) this).mContext.getResources().getConfiguration());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
    }

    @Override // com.android.keyguard.EmergencyButton.EmergencyButtonCallback
    public void onEmergencyButtonClickedWhenInCall() {
        this.mCallback.reset();
    }

    /* access modifiers changed from: protected */
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        if (!this.mDismissing) {
            final byte[] passwordText = getPasswordText();
            setPasswordEntryInputEnabled(false);
            AsyncTask<?, ?, ?> asyncTask = this.mPendingLockCheck;
            if (asyncTask != null) {
                asyncTask.cancel(false);
            }
            final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (passwordText.length <= 3) {
                setPasswordEntryInputEnabled(true);
                onPasswordChecked(currentUser, false, 0, false);
                Arrays.fill(passwordText, (byte) 0);
                return;
            }
            if (LatencyTracker.isEnabled(((LinearLayout) this).mContext)) {
                LatencyTracker.getInstance(((LinearLayout) this).mContext).onActionStart(3);
                LatencyTracker.getInstance(((LinearLayout) this).mContext).onActionStart(4);
            }
            this.mPendingLockCheck = LockPatternChecker.checkPassword(this.mLockPatternUtils, passwordText, currentUser, new LockPatternChecker.OnCheckCallback() {
                /* class com.android.keyguard.KeyguardAbsKeyInputView.AnonymousClass1 */

                public void onEarlyMatched() {
                    if (LatencyTracker.isEnabled(((LinearLayout) KeyguardAbsKeyInputView.this).mContext)) {
                        LatencyTracker.getInstance(((LinearLayout) KeyguardAbsKeyInputView.this).mContext).onActionEnd(3);
                    }
                    KeyguardAbsKeyInputView.this.onPasswordChecked(currentUser, true, 0, true);
                    Arrays.fill(passwordText, (byte) 0);
                }

                public void onChecked(boolean z, int i) {
                    if (LatencyTracker.isEnabled(((LinearLayout) KeyguardAbsKeyInputView.this).mContext)) {
                        LatencyTracker.getInstance(((LinearLayout) KeyguardAbsKeyInputView.this).mContext).onActionEnd(4);
                    }
                    KeyguardAbsKeyInputView.this.setPasswordEntryInputEnabled(true);
                    KeyguardAbsKeyInputView keyguardAbsKeyInputView = KeyguardAbsKeyInputView.this;
                    keyguardAbsKeyInputView.mPendingLockCheck = null;
                    if (!z) {
                        keyguardAbsKeyInputView.onPasswordChecked(currentUser, false, i, true);
                    }
                    Arrays.fill(passwordText, (byte) 0);
                }

                public void onCancelled() {
                    if (LatencyTracker.isEnabled(((LinearLayout) KeyguardAbsKeyInputView.this).mContext)) {
                        LatencyTracker.getInstance(((LinearLayout) KeyguardAbsKeyInputView.this).mContext).onActionEnd(4);
                    }
                    Arrays.fill(passwordText, (byte) 0);
                }
            });
        }
    }

    /* access modifiers changed from: protected */
    public void setPinUnlockListener(PinUnlockListener pinUnlockListener) {
        this.mPinUnlockListener = pinUnlockListener;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onPasswordChecked(int i, boolean z, int i2, boolean z2) {
        boolean z3 = KeyguardUpdateMonitor.getCurrentUser() == i;
        byte[] passwordText = getPasswordText();
        if (z) {
            this.mLockPatternUtils.sanitizePassword();
            PinUnlockListener pinUnlockListener = this.mPinUnlockListener;
            if (pinUnlockListener != null) {
                pinUnlockListener.onUnlockSucceded(passwordText.length);
            }
            this.mCallback.reportUnlockAttempt(i, true, 0);
            if (z3) {
                this.mDismissing = true;
                this.mCallback.dismiss(true, i);
            }
        } else {
            if (z2) {
                PinUnlockListener pinUnlockListener2 = this.mPinUnlockListener;
                if (pinUnlockListener2 != null) {
                    pinUnlockListener2.onUnlockFailed();
                }
                this.mCallback.reportUnlockAttempt(i, false, i2);
                if (i2 > 0) {
                    handleAttemptLockout(this.mLockPatternUtils.setLockoutAttemptDeadline(i, i2));
                }
            }
            if (i2 == 0) {
                this.mSecurityMessageDisplay.setMessage(getWrongPasswordStringId());
                new Handler().postDelayed(new Runnable() {
                    /* class com.android.keyguard.KeyguardAbsKeyInputView.AnonymousClass2 */

                    public void run() {
                        KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage("");
                    }
                }, 1000);
            }
        }
        resetPasswordText(true, !z);
    }

    /* access modifiers changed from: protected */
    public void handleAttemptLockout(long j) {
        setPasswordEntryEnabled(false);
        this.mCountdownTimer = new CountDownTimer(((long) Math.ceil(((double) (j - SystemClock.elapsedRealtime())) / 1000.0d)) * 1000, 1000) {
            /* class com.android.keyguard.KeyguardAbsKeyInputView.AnonymousClass3 */

            public void onTick(long j) {
                int round = (int) Math.round(((double) j) / 1000.0d);
                KeyguardAbsKeyInputView keyguardAbsKeyInputView = KeyguardAbsKeyInputView.this;
                keyguardAbsKeyInputView.mSecurityMessageDisplay.setMessage(((LinearLayout) keyguardAbsKeyInputView).mContext.getResources().getQuantityString(R$plurals.kg_too_many_failed_attempts_countdown, round, Integer.valueOf(round)));
            }

            public void onFinish() {
                KeyguardAbsKeyInputView.this.mSecurityMessageDisplay.setMessage("");
                KeyguardAbsKeyInputView.this.resetState();
            }
        }.start();
    }

    /* access modifiers changed from: protected */
    public void onUserInput() {
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.userActivity();
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 0) {
            return false;
        }
        onUserInput();
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onPause() {
        this.mResumed = false;
        CountDownTimer countDownTimer = this.mCountdownTimer;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            this.mCountdownTimer = null;
        }
        AsyncTask<?, ?, ?> asyncTask = this.mPendingLockCheck;
        if (asyncTask != null) {
            asyncTask.cancel(false);
            this.mPendingLockCheck = null;
        }
        reset();
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void onResume(int i) {
        this.mResumed = true;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showPromptReason(int i) {
        int promptReasonStringRes;
        if (i != 0 && (promptReasonStringRes = getPromptReasonStringRes(i)) != 0) {
            this.mSecurityMessageDisplay.setMessage(promptReasonStringRes);
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void showMessage(CharSequence charSequence, ColorStateList colorStateList) {
        this.mSecurityMessageDisplay.setNextMessageColor(colorStateList);
        this.mSecurityMessageDisplay.setMessage(charSequence);
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }
}
