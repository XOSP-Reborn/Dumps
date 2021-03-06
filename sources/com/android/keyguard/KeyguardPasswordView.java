package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.biometrics.BiometricSourceType;
import android.os.UserHandle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;
import com.android.internal.widget.TextViewInputDisabler;
import java.util.List;

public class KeyguardPasswordView extends KeyguardAbsKeyInputView implements KeyguardSecurityView, TextView.OnEditorActionListener, TextWatcher {
    private final int mDisappearYTranslation;
    private Interpolator mFastOutLinearInInterpolator;
    InputMethodManager mImm;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private Interpolator mLinearOutSlowInInterpolator;
    private TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryDisabler;
    private final boolean mShowImeAtScreenOn;
    private View mSwitchImeButton;
    private KeyguardUpdateMonitorCallback mUpdateCallback;

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public boolean needsInput() {
        return true;
    }

    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public KeyguardPasswordView(Context context) {
        this(context, null);
    }

    public KeyguardPasswordView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardPasswordView.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
                KeyguardPasswordView.this.updateDefaultMessage();
            }
        };
        this.mShowImeAtScreenOn = context.getResources().getBoolean(R$bool.kg_show_ime_at_screen_on);
        this.mDisappearYTranslation = getResources().getDimensionPixelSize(R$dimen.disappear_y_translation);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, 17563663);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void resetState() {
        this.mPasswordEntry.setTextOperationUser(UserHandle.of(KeyguardUpdateMonitor.getCurrentUser()));
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage("");
        }
        updateDefaultMessage();
        boolean isEnabled = this.mPasswordEntry.isEnabled();
        setPasswordEntryEnabled(true);
        setPasswordEntryInputEnabled(true);
        if (this.mResumed && this.mPasswordEntry.isVisibleToUser() && isEnabled) {
            this.mImm.showSoftInput(this.mPasswordEntry, 1);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getPasswordTextViewId() {
        return R$id.passwordEntry;
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public void onResume(final int i) {
        super.onResume(i);
        post(new Runnable() {
            /* class com.android.keyguard.KeyguardPasswordView.AnonymousClass2 */

            public void run() {
                if (KeyguardPasswordView.this.isShown() && KeyguardPasswordView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPasswordView.this.mPasswordEntry.requestFocus();
                    if (i != 1 || KeyguardPasswordView.this.mShowImeAtScreenOn) {
                        KeyguardPasswordView keyguardPasswordView = KeyguardPasswordView.this;
                        keyguardPasswordView.mImm.showSoftInput(keyguardPasswordView.mPasswordEntry, 1);
                    }
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getPromptReasonStringRes(int i) {
        if (i == 0) {
            return 0;
        }
        if (i == 1) {
            return R$string.kg_prompt_reason_restart_password;
        }
        if (i == 2) {
            return R$string.kg_prompt_reason_timeout_password;
        }
        if (i == 3) {
            return R$string.kg_prompt_reason_device_admin;
        }
        if (i != 4) {
            return R$string.kg_prompt_reason_timeout_password;
        }
        return R$string.kg_prompt_reason_user_request;
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public void onPause() {
        super.onPause();
        this.mImm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSwitchImeButton() {
        boolean z = this.mSwitchImeButton.getVisibility() == 0;
        boolean hasMultipleEnabledIMEsOrSubtypes = hasMultipleEnabledIMEsOrSubtypes(this.mImm, false);
        if (z != hasMultipleEnabledIMEsOrSubtypes) {
            this.mSwitchImeButton.setVisibility(hasMultipleEnabledIMEsOrSubtypes ? 0 : 8);
        }
        if (this.mSwitchImeButton.getVisibility() != 0) {
            ViewGroup.LayoutParams layoutParams = this.mPasswordEntry.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMarginStart(0);
                this.mPasswordEntry.setLayoutParams(layoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mImm = (InputMethodManager) getContext().getSystemService("input_method");
        this.mPasswordEntry = (TextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setTextOperationUser(UserHandle.of(KeyguardUpdateMonitor.getCurrentUser()));
        this.mPasswordEntryDisabler = new TextViewInputDisabler(this.mPasswordEntry);
        this.mPasswordEntry.setKeyListener(TextKeyListener.getInstance());
        this.mPasswordEntry.setInputType(129);
        this.mPasswordEntry.setOnEditorActionListener(this);
        this.mPasswordEntry.addTextChangedListener(this);
        this.mPasswordEntry.setOnClickListener(new View.OnClickListener() {
            /* class com.android.keyguard.KeyguardPasswordView.AnonymousClass3 */

            public void onClick(View view) {
                KeyguardPasswordView.this.mCallback.userActivity();
            }
        });
        this.mPasswordEntry.setSelected(true);
        this.mSwitchImeButton = findViewById(R$id.switch_ime_button);
        this.mSwitchImeButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.keyguard.KeyguardPasswordView.AnonymousClass4 */

            public void onClick(View view) {
                KeyguardPasswordView.this.mCallback.userActivity();
                KeyguardPasswordView keyguardPasswordView = KeyguardPasswordView.this;
                keyguardPasswordView.mImm.showInputMethodPickerFromSystem(false, keyguardPasswordView.getContext().getDisplayId());
            }
        });
        View findViewById = findViewById(R$id.cancel_button);
        if (findViewById != null) {
            findViewById.setOnClickListener(new View.OnClickListener() {
                /* class com.android.keyguard.$$Lambda$KeyguardPasswordView$o6rdkANQuxgpLXMWWI2lzhbd_0k */

                public final void onClick(View view) {
                    KeyguardPasswordView.this.lambda$onFinishInflate$0$KeyguardPasswordView(view);
                }
            });
        }
        updateSwitchImeButton();
        postDelayed(new Runnable() {
            /* class com.android.keyguard.KeyguardPasswordView.AnonymousClass5 */

            public void run() {
                KeyguardPasswordView.this.updateSwitchImeButton();
            }
        }, 500);
    }

    public /* synthetic */ void lambda$onFinishInflate$0$KeyguardPasswordView(View view) {
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

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int i, Rect rect) {
        return this.mPasswordEntry.requestFocus(i, rect);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void resetPasswordText(boolean z, boolean z2) {
        this.mPasswordEntry.setText("");
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public byte[] getPasswordText() {
        return charSequenceToByteArray(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void setPasswordEntryEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void setPasswordEntryInputEnabled(boolean z) {
        this.mPasswordEntryDisabler.setInputEnabled(z);
    }

    private boolean hasMultipleEnabledIMEsOrSubtypes(InputMethodManager inputMethodManager, boolean z) {
        int i = 0;
        for (InputMethodInfo inputMethodInfo : inputMethodManager.getEnabledInputMethodListAsUser(KeyguardUpdateMonitor.getCurrentUser())) {
            if (i > 1) {
                return true;
            }
            List<InputMethodSubtype> enabledInputMethodSubtypeList = inputMethodManager.getEnabledInputMethodSubtypeList(inputMethodInfo, true);
            if (!enabledInputMethodSubtypeList.isEmpty()) {
                int i2 = 0;
                for (InputMethodSubtype inputMethodSubtype : enabledInputMethodSubtypeList) {
                    if (inputMethodSubtype.isAuxiliary()) {
                        i2++;
                    }
                }
                if (enabledInputMethodSubtypeList.size() - i2 <= 0) {
                    if (z) {
                        if (i2 <= 1) {
                        }
                    }
                }
            }
            i++;
        }
        return i > 1 || inputMethodManager.getEnabledInputMethodSubtypeList(null, false).size() > 1;
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getWrongPasswordStringId() {
        return R$string.kg_wrong_password;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
        setAlpha(0.0f);
        setTranslationY(0.0f);
        animate().alpha(1.0f).withLayer().setDuration(300).setInterpolator(this.mLinearOutSlowInInterpolator);
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public boolean startDisappearAnimation(Runnable runnable) {
        animate().alpha(0.0f).translationY((float) this.mDisappearYTranslation).setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100).withEndAction(runnable);
        return true;
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        KeyguardSecurityCallback keyguardSecurityCallback = this.mCallback;
        if (keyguardSecurityCallback != null) {
            keyguardSecurityCallback.userActivity();
        }
    }

    public void afterTextChanged(Editable editable) {
        if (!TextUtils.isEmpty(editable)) {
            onUserInput();
        }
    }

    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        boolean z = keyEvent == null && (i == 0 || i == 6 || i == 5);
        boolean z2 = keyEvent != null && KeyEvent.isConfirmKey(keyEvent.getKeyCode()) && keyEvent.getAction() == 0;
        if (!z && !z2) {
            return false;
        }
        verifyPasswordAndUnlock();
        return true;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public CharSequence getTitle() {
        return getContext().getString(17040273);
    }

    private static byte[] charSequenceToByteArray(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        byte[] bArr = new byte[charSequence.length()];
        for (int i = 0; i < charSequence.length(); i++) {
            bArr[i] = (byte) charSequence.charAt(i);
        }
        return bArr;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDefaultMessage() {
        if (this.mSecurityMessageDisplay == null) {
            return;
        }
        if (this.mKeyguardUpdateMonitor.isFingerprintUnlockPossible()) {
            this.mSecurityMessageDisplay.setDefaultMessage(R$string.kg_password_instructions_with_fingerprint);
        } else {
            this.mSecurityMessageDisplay.setDefaultMessage(R$string.kg_password_instructions);
        }
    }
}
