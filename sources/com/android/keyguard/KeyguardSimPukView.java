package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.settingslib.Utils;
import com.sonymobile.keyguard.SomcKeyguardRuntimeResources;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    private CheckSimPuk mCheckSimPukThread;
    private boolean mIsSkinningEnabled;
    private KeyguardUpdateMonitor mKgUpdateMonitor;
    private String mPinText;
    private String mPukText;
    private int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    private boolean mShowDefaultMessage;
    private int mSimImageColor;
    private ImageView mSimImageView;
    private ProgressDialog mSimUnlockProgressDialog;
    private StateMachine mStateMachine;
    private TextView mSubDisplayName;
    private int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public int getPromptReasonStringRes(int i) {
        return 0;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public boolean shouldLockout(long j) {
        return false;
    }

    @Override // com.android.keyguard.KeyguardSecurityView
    public void startAppearAnimation() {
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    /* renamed from: com.android.keyguard.KeyguardSimPukView$4  reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
        }
    }

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mStateMachine = new StateMachine();
        this.mSubId = -1;
        this.mSubDisplayName = null;
        this.mSimImageColor = -1;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardSimPukView.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onSimStateChanged(int i, int i2, IccCardConstants.State state) {
                if (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()] != 1) {
                    KeyguardSimPukView.this.resetState();
                    return;
                }
                KeyguardSimPukView.this.mRemainingAttempts = -1;
                KeyguardSimPukView.this.mShowDefaultMessage = true;
                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPukView.this.mCallback;
                if (keyguardSecurityCallback != null) {
                    keyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                }
                KeyguardSimPukView.this.resetState();
            }
        };
        this.mKgUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mIsSkinningEnabled = context.getResources().getBoolean(R$bool.somc_keyguard_theme_enabled);
    }

    /* access modifiers changed from: private */
    public class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

        private StateMachine() {
            this.ENTER_PUK = 0;
            this.ENTER_PIN = 1;
            this.CONFIRM_PIN = 2;
            this.DONE = 3;
            this.state = 0;
        }

        public void next() {
            int i;
            int i2 = this.state;
            if (i2 == 0) {
                if (KeyguardSimPukView.this.checkPuk()) {
                    this.state = 1;
                    i = R$string.kg_puk_enter_pin_hint;
                } else {
                    i = R$string.kg_invalid_sim_puk_hint;
                }
            } else if (i2 == 1) {
                if (KeyguardSimPukView.this.checkPin()) {
                    this.state = 2;
                    i = R$string.kg_enter_confirm_pin_hint;
                } else {
                    i = R$string.kg_invalid_sim_pin_hint;
                }
            } else if (i2 != 2) {
                i = 0;
            } else if (KeyguardSimPukView.this.confirmPin()) {
                this.state = 3;
                i = R$string.keyguard_sim_unlock_progress_dialog_message;
                KeyguardSimPukView.this.updateSim();
            } else {
                this.state = 1;
                i = R$string.kg_invalid_confirm_pin_hint;
            }
            KeyguardSimPukView.this.resetPasswordText(true, true);
            if (i != 0) {
                KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(i);
            }
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            KeyguardSimPukView.this.mPinText = "";
            KeyguardSimPukView.this.mPukText = "";
            int i = 0;
            this.state = 0;
            KeyguardSimPukView.this.handleSubInfoChangeIfNeeded();
            if (KeyguardSimPukView.this.mShowDefaultMessage) {
                KeyguardSimPukView.this.showDefaultMessage();
            }
            boolean isEsimLocked = KeyguardEsimArea.isEsimLocked(((LinearLayout) KeyguardSimPukView.this).mContext, KeyguardSimPukView.this.mSubId);
            KeyguardEsimArea keyguardEsimArea = (KeyguardEsimArea) KeyguardSimPukView.this.findViewById(R$id.keyguard_esim_area);
            if (!isEsimLocked) {
                i = 8;
            }
            keyguardEsimArea.setVisibility(i);
            KeyguardSimPukView.this.mPasswordEntry.requestFocus();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showDefaultMessage() {
        int i = this.mRemainingAttempts;
        if (i >= 0) {
            this.mSecurityMessageDisplay.setMessage(getPukPasswordErrorMessage(i, true));
        } else {
            new CheckSimPuk("", "", this.mSubId) {
                /* class com.android.keyguard.KeyguardSimPukView.AnonymousClass2 */

                /* access modifiers changed from: package-private */
                @Override // com.android.keyguard.KeyguardSimPukView.CheckSimPuk
                public void onSimLockChangedResponse(int i, int i2) {
                    Log.d("KeyguardSimPukView", "onSimCheckResponse  dummy One result" + i + " attemptsRemaining=" + i2);
                    if (i2 >= 0) {
                        KeyguardSimPukView.this.mRemainingAttempts = i2;
                        KeyguardSimPukView keyguardSimPukView = KeyguardSimPukView.this;
                        keyguardSimPukView.mSecurityMessageDisplay.setMessage(keyguardSimPukView.getPukPasswordErrorMessage(i2, true));
                    }
                }
            }.start();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSubInfoChangeIfNeeded() {
        int nextSubIdForState = this.mKgUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);
        if (nextSubIdForState != this.mSubId && SubscriptionManager.isValidSubscriptionId(nextSubIdForState)) {
            this.mSubId = nextSubIdForState;
            handleSubInfoChange();
            this.mShowDefaultMessage = true;
            this.mRemainingAttempts = -1;
        }
    }

    public final void updateSimImageColor(int i) {
        this.mSimImageColor = i;
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(i));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private String getPukPasswordErrorMessage(int i, boolean z) {
        String str;
        int i2;
        int i3;
        if (i == 0) {
            str = getContext().getString(R$string.kg_password_wrong_puk_code_dead);
        } else if (i > 0) {
            String string = getContext().getString(R$string.kg_customized_zero_pin_attempts_message);
            if (i != 10 || !z || TextUtils.isEmpty(string)) {
                if (z) {
                    i3 = R$plurals.kg_password_default_puk_message;
                } else {
                    i3 = R$plurals.kg_password_wrong_puk_code;
                }
                str = getContext().getResources().getQuantityString(i3, i, Integer.valueOf(i));
            } else {
                str = string;
            }
        } else {
            if (z) {
                i2 = R$string.kg_puk_enter_puk_hint;
            } else {
                i2 = R$string.kg_password_puk_failed;
            }
            str = getContext().getString(i2);
        }
        if (!KeyguardEsimArea.isEsimLocked(((LinearLayout) this).mContext, this.mSubId)) {
            return str;
        }
        return getResources().getString(R$string.kg_sim_lock_esim_instructions, str);
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void resetState() {
        super.resetState();
        this.mStateMachine.reset();
        this.mPasswordEntry.setEnabled(true);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onLocaleChanged() {
        this.mRemainingAttempts = -1;
        this.mShowDefaultMessage = true;
        resetState();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getPasswordTextViewId() {
        return R$id.pukEntry;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSubDisplayName = (TextView) findViewById(R$id.sub_display_name);
        this.mSubId = this.mKgUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);
        this.mSimImageView = (ImageView) findViewById(R$id.keyguard_sim);
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            View findViewById = findViewById(R$id.sim_info_message);
            if (findViewById != null) {
                SomcKeyguardRuntimeResources.updateVisibility(findViewById, ((LinearLayout) this).mContext.getResources(), R$string.somc_keyguard_visible_tablet_or_portrait);
            }
            handleSubInfoChange();
        }
        View view = this.mEcaView;
        if (view instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) view).setCarrierTextVisible(true);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mKgUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        resetState();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mKgUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public void onPause() {
        ProgressDialog progressDialog = this.mSimUnlockProgressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    /* access modifiers changed from: private */
    public abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;
        private final int mSubId;

        /* access modifiers changed from: package-private */
        public abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String str, String str2, int i) {
            this.mPuk = str;
            this.mPin = str2;
            this.mSubId = i;
        }

        public void run() {
            try {
                final int[] supplyPukReportResultForSubscriber = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(this.mSubId, this.mPuk, this.mPin);
                KeyguardSimPukView.this.post(new Runnable() {
                    /* class com.android.keyguard.KeyguardSimPukView.CheckSimPuk.AnonymousClass1 */

                    public void run() {
                        CheckSimPuk checkSimPuk = CheckSimPuk.this;
                        int[] iArr = supplyPukReportResultForSubscriber;
                        checkSimPuk.onSimLockChangedResponse(iArr[0], iArr[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPukView", "RemoteException for supplyPukReportResult:", e);
                KeyguardSimPukView.this.post(new Runnable() {
                    /* class com.android.keyguard.KeyguardSimPukView.CheckSimPuk.AnonymousClass2 */

                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(2, -1);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(((LinearLayout) this).mContext);
            this.mSimUnlockProgressDialog.setMessage(((LinearLayout) this).mContext.getString(R$string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(((LinearLayout) this).mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Dialog getPukRemainingAttemptsDialog(int i) {
        String pukPasswordErrorMessage = getPukPasswordErrorMessage(i, false);
        AlertDialog alertDialog = this.mRemainingAttemptsDialog;
        if (alertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(((LinearLayout) this).mContext);
            builder.setMessage(pukPasswordErrorMessage);
            builder.setCancelable(false);
            if (getResources().getBoolean(R$bool.somc_show_title_in_sim_dialog)) {
                builder.setTitle(R$string.kg_somc_note);
            }
            builder.setNeutralButton(R$string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            alertDialog.setMessage(pukPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() != 8) {
            return false;
        }
        this.mPukText = this.mPasswordEntry.getText();
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length < 4 || length > 8) {
            return false;
        }
        this.mPinText = this.mPasswordEntry.getText();
        return true;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread == null) {
            this.mCheckSimPukThread = new CheckSimPuk(this.mPukText, this.mPinText, this.mSubId) {
                /* class com.android.keyguard.KeyguardSimPukView.AnonymousClass3 */

                /* access modifiers changed from: package-private */
                @Override // com.android.keyguard.KeyguardSimPukView.CheckSimPuk
                public void onSimLockChangedResponse(final int i, final int i2) {
                    KeyguardSimPukView.this.post(new Runnable() {
                        /* class com.android.keyguard.KeyguardSimPukView.AnonymousClass3.AnonymousClass1 */

                        public void run() {
                            KeyguardSimPukView.this.mRemainingAttempts = i2;
                            if (KeyguardSimPukView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPukView.this.mSimUnlockProgressDialog.hide();
                            }
                            if (i == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mSubId);
                                KeyguardSimPukView.this.mRemainingAttempts = -1;
                                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPukView.this.mCallback;
                                if (keyguardSecurityCallback != null) {
                                    keyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else {
                                KeyguardSimPukView.this.mShowDefaultMessage = false;
                                if (i == 1) {
                                    KeyguardSimPukView keyguardSimPukView = KeyguardSimPukView.this;
                                    keyguardSimPukView.mSecurityMessageDisplay.setMessage(keyguardSimPukView.getPukPasswordErrorMessageDisplay(i2));
                                    int integer = KeyguardSimPukView.this.getResources().getInteger(R$integer.somc_sim_puk_attempts_remaining_before_dialog);
                                    int i = i2;
                                    if (i <= integer) {
                                        KeyguardSimPukView.this.getPukRemainingAttemptsDialog(i).show();
                                    } else {
                                        KeyguardSimPukView keyguardSimPukView2 = KeyguardSimPukView.this;
                                        keyguardSimPukView2.mSecurityMessageDisplay.setMessage(keyguardSimPukView2.getPukPasswordErrorMessageDisplay(i));
                                    }
                                } else {
                                    KeyguardSimPukView keyguardSimPukView3 = KeyguardSimPukView.this;
                                    keyguardSimPukView3.mSecurityMessageDisplay.setMessage(keyguardSimPukView3.getContext().getString(R$string.kg_password_puk_failed));
                                }
                                KeyguardSimPukView.this.mStateMachine.reset();
                            }
                            KeyguardSimPukView.this.mCheckSimPukThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPukThread.start();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private String getPukPasswordErrorMessageDisplay(int i) {
        boolean z = getResources().getBoolean(R$bool.somc_sim_puk_view_show_attempts_remaining_in_security_message);
        if (!z) {
            i = -1;
        }
        return getPukPasswordErrorMessage(i, !z);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void verifyPasswordAndUnlock() {
        this.mStateMachine.next();
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardPinBasedInputView
    public CharSequence getTitle() {
        return getContext().getString(17040278);
    }

    private void handleSubInfoChange() {
        int i;
        int i2;
        if (!this.mIsSkinningEnabled) {
            i = Utils.getColorAttrDefaultColor(((LinearLayout) this).mContext, R$attr.wallpaperTextColor);
        } else {
            i = this.mSimImageColor;
        }
        String str = null;
        SubscriptionInfo subscriptionInfoForSubId = this.mKgUpdateMonitor.getSubscriptionInfoForSubId(this.mSubId);
        if (subscriptionInfoForSubId != null) {
            str = subscriptionInfoForSubId.getDisplayName().toString();
            i2 = subscriptionInfoForSubId.getNameSource();
        } else {
            i2 = 0;
        }
        TextView textView = (TextView) findViewById(R$id.slot_id_name);
        if (TextUtils.isEmpty(str) || i2 == 0) {
            textView.setText(((LinearLayout) this).mContext.getString(R$string.somc_dsds_sim_number_format, Integer.valueOf(SubscriptionManager.getSlotIndex(this.mSubId) + 1)));
            textView.setVisibility(0);
            this.mSubDisplayName.setVisibility(8);
        } else {
            this.mSubDisplayName.setText(((LinearLayout) this).mContext.getString(R$string.somc_dsds_sim_number_colon_name_format, Integer.valueOf(SubscriptionManager.getSlotIndex(this.mSubId) + 1), str));
            this.mSubDisplayName.setVisibility(0);
            textView.setVisibility(8);
        }
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(i));
    }
}
