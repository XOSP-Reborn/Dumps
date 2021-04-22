package com.android.keyguard;

import android.content.Context;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;
import com.sonymobile.keyguard.pin.PinAutoConfirmHelper;
import com.sonymobile.keyguard.pin.RealPinAutoConfirmHelper;
import com.sonymobile.keyguard.pin.RealPinAutoUnlockSettingsSecureWrapper;

public class KeyguardPINView extends KeyguardPinBasedInputView {
    private final AppearAnimationUtils mAppearAnimationUtils;
    private PinAutoConfirmHelper mAutoConfirmHelper;
    private ViewGroup mContainer;
    private final DisappearAnimationUtils mDisappearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtilsLocked;
    private int mDisappearYTranslation;
    private View mDivider;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private ViewGroup mRow0;
    private ViewGroup mRow1;
    private ViewGroup mRow2;
    private ViewGroup mRow3;
    private KeyguardUpdateMonitorCallback mUpdateCallback;
    private View[][] mViews;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public KeyguardPINView(Context context) {
        this(context, null);
    }

    public KeyguardPINView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardPINView.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
                KeyguardPINView.this.updateDefaultMessage();
            }
        };
        this.mAppearAnimationUtils = new AppearAnimationUtils(context, 220, 0.4f, 0.4f, AnimationUtils.loadInterpolator(((LinearLayout) this).mContext, 17563662));
        this.mDisappearAnimationUtils = new DisappearAnimationUtils(context, 125, 0.6f, 0.45f, AnimationUtils.loadInterpolator(((LinearLayout) this).mContext, 17563663));
        this.mDisappearAnimationUtilsLocked = new DisappearAnimationUtils(context, 187, 0.6f, 0.45f, AnimationUtils.loadInterpolator(((LinearLayout) this).mContext, 17563663));
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void resetState() {
        super.resetState();
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage("");
        }
        updateDefaultMessage();
        PinAutoConfirmHelper pinAutoConfirmHelper = this.mAutoConfirmHelper;
        if (pinAutoConfirmHelper != null) {
            pinAutoConfirmHelper.disableAutoUnlockIfAppropriate();
            this.mAutoConfirmHelper.updateEnterKeyVisibility();
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getPasswordTextViewId() {
        return R$id.pinEntry;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mContainer = (ViewGroup) findViewById(R$id.container);
        this.mRow0 = (ViewGroup) findViewById(R$id.row0);
        this.mRow1 = (ViewGroup) findViewById(R$id.row1);
        this.mRow2 = (ViewGroup) findViewById(R$id.row2);
        this.mRow3 = (ViewGroup) findViewById(R$id.row3);
        this.mDivider = findViewById(R$id.divider);
        this.mViews = new View[][]{new View[]{this.mRow0, null, null}, new View[]{findViewById(R$id.key1), findViewById(R$id.key2), findViewById(R$id.key3)}, new View[]{findViewById(R$id.key4), findViewById(R$id.key5), findViewById(R$id.key6)}, new View[]{findViewById(R$id.key7), findViewById(R$id.key8), findViewById(R$id.key9)}, new View[]{findViewById(R$id.delete_button), findViewById(R$id.key0), findViewById(R$id.key_enter)}, new View[]{null, this.mEcaView, null}};
        View findViewById = findViewById(R$id.cancel_button);
        if (findViewById != null) {
            findViewById.setOnClickListener(new View.OnClickListener() {
                /* class com.android.keyguard.$$Lambda$KeyguardPINView$32q9EwjCzWlJ6lNiw9pw0PSsPxs */

                public final void onClick(View view) {
                    KeyguardPINView.this.lambda$onFinishInflate$0$KeyguardPINView(view);
                }
            });
        }
        Context context = getContext();
        RealPinAutoConfirmHelper realPinAutoConfirmHelper = new RealPinAutoConfirmHelper(findViewById(R$id.key_enter), new RealPinAutoUnlockSettingsSecureWrapper(context.getContentResolver()), KeyguardUpdateMonitor.getInstance(context), new LockPatternUtils(context), new Handler(Looper.getMainLooper()), (Vibrator) context.getSystemService("vibrator"), this.mPasswordEntry, this.mSecurityMessageDisplay);
        this.mAutoConfirmHelper = realPinAutoConfirmHelper;
        this.mPasswordEntry.setPinEntryListener(realPinAutoConfirmHelper);
        this.mPasswordEntry.setContentLocked(false);
        setPinUnlockListener(realPinAutoConfirmHelper);
        PinAutoConfirmHelper pinAutoConfirmHelper = this.mAutoConfirmHelper;
        if (pinAutoConfirmHelper != null) {
            pinAutoConfirmHelper.disableAutoUnlockIfAppropriate();
            this.mAutoConfirmHelper.updateEnterKeyVisibility();
        }
    }

    public /* synthetic */ void lambda$onFinishInflate$0$KeyguardPINView(View view) {
        this.mCallback.reset();
        this.mCallback.onCancelClicked();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateCallback);
        updateDefaultMessage();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mKeyguardUpdateMonitor.removeCallback(this.mUpdateCallback);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_pin;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        enableClipping(false);
        setAlpha(1.0f);
        setTranslationY(this.mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 160, 0.0f, this.mAppearAnimationUtils.getInterpolator());
        this.mAppearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            /* class com.android.keyguard.KeyguardPINView.AnonymousClass2 */

            public void run() {
                KeyguardPINView.this.enableClipping(true);
            }
        });
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public boolean startDisappearAnimation(final Runnable runnable) {
        DisappearAnimationUtils disappearAnimationUtils;
        enableClipping(false);
        setTranslationY(0.0f);
        AppearAnimationUtils.startTranslationYAnimation(this, 0, 280, (float) this.mDisappearYTranslation, this.mDisappearAnimationUtils.getInterpolator());
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            disappearAnimationUtils = this.mDisappearAnimationUtilsLocked;
        } else {
            disappearAnimationUtils = this.mDisappearAnimationUtils;
        }
        disappearAnimationUtils.startAnimation2d(this.mViews, new Runnable() {
            /* class com.android.keyguard.KeyguardPINView.AnonymousClass3 */

            public void run() {
                KeyguardPINView.this.enableClipping(true);
                Runnable runnable = runnable;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay == null || TextUtils.isEmpty(((KeyguardMessageArea) securityMessageDisplay).getText())) {
            return true;
        }
        DisappearAnimationUtils disappearAnimationUtils2 = this.mDisappearAnimationUtils;
        disappearAnimationUtils2.createAnimation((View) ((KeyguardMessageArea) this.mSecurityMessageDisplay), 0L, 200L, 3.0f * (-disappearAnimationUtils2.getStartTranslation()), false, this.mDisappearAnimationUtils.getInterpolator(), (Runnable) null);
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void enableClipping(boolean z) {
        this.mContainer.setClipToPadding(z);
        this.mContainer.setClipChildren(z);
        this.mRow1.setClipToPadding(z);
        this.mRow2.setClipToPadding(z);
        this.mRow3.setClipToPadding(z);
        setClipChildren(z);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDefaultMessage() {
        if (this.mSecurityMessageDisplay == null) {
            return;
        }
        if (this.mKeyguardUpdateMonitor.isFingerprintUnlockPossible()) {
            this.mSecurityMessageDisplay.setDefaultMessage(R$string.kg_pin_instructions_with_fingerprint);
        } else {
            this.mSecurityMessageDisplay.setDefaultMessage(R$string.kg_pin_instructions);
        }
    }
}
