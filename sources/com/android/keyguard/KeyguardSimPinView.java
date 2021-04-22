package com.android.keyguard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
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

public class KeyguardSimPinView extends KeyguardPinBasedInputView {
    private CheckSimPin mCheckSimPinThread;
    private boolean mIsSkinningEnabled;
    private KeyguardUpdateMonitor mKgUpdateMonitor;
    private int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    private boolean mShowDefaultMessage;
    private int mSimImageColor;
    private ImageView mSimImageView;
    private ProgressDialog mSimUnlockProgressDialog;
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

    /* renamed from: com.android.keyguard.KeyguardSimPinView$4  reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
        }
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mSubId = -1;
        this.mSubDisplayName = null;
        this.mSimImageColor = -1;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardSimPinView.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onSimStateChanged(int i, int i2, IccCardConstants.State state) {
                if (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()] != 1) {
                    KeyguardSimPinView.this.resetState();
                    return;
                }
                KeyguardSimPinView.this.mRemainingAttempts = -1;
                KeyguardSimPinView.this.resetState();
            }
        };
        this.mKgUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mIsSkinningEnabled = context.getResources().getBoolean(R$bool.somc_keyguard_theme_enabled);
    }

    public final void updateSimImageColor(int i) {
        this.mSimImageColor = i;
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(i));
    }

    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void resetState() {
        super.resetState();
        handleSubInfoChangeIfNeeded();
        if (this.mShowDefaultMessage) {
            showDefaultMessage();
        }
        ((KeyguardEsimArea) findViewById(R$id.keyguard_esim_area)).setVisibility(KeyguardEsimArea.isEsimLocked(((LinearLayout) this).mContext, this.mSubId) ? 0 : 8);
        this.mPasswordEntry.setEnabled(true);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setLockedSimMessage() {
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage(getPinPasswordErrorMessage(this.mRemainingAttempts, true));
        }
    }

    private void showDefaultMessage() {
        setLockedSimMessage();
        if (this.mRemainingAttempts < 0) {
            new CheckSimPin("", this.mSubId) {
                /* class com.android.keyguard.KeyguardSimPinView.AnonymousClass2 */

                /* access modifiers changed from: package-private */
                @Override // com.android.keyguard.KeyguardSimPinView.CheckSimPin
                public void onSimCheckResponse(int i, int i2) {
                    Log.d("KeyguardSimPinView", "onSimCheckResponse  dummy One result" + i + " attemptsRemaining=" + i2);
                    if (i2 >= 0) {
                        KeyguardSimPinView.this.mRemainingAttempts = i2;
                        KeyguardSimPinView.this.setLockedSimMessage();
                    }
                }
            }.start();
        }
    }

    private void handleSubInfoChangeIfNeeded() {
        int nextSubIdForState = this.mKgUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED);
        if (SubscriptionManager.isValidSubscriptionId(nextSubIdForState)) {
            this.mSubId = nextSubIdForState;
            handleSubInfoChange();
            this.mShowDefaultMessage = true;
            this.mRemainingAttempts = -1;
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        resetState();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void onLocaleChanged() {
        this.mRemainingAttempts = -1;
        this.mShowDefaultMessage = true;
        resetState();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private String getPinPasswordErrorMessage(int i, boolean z) {
        String str;
        int i2;
        int i3;
        if (i == 0) {
            str = getContext().getString(R$string.kg_password_wrong_pin_code_pukked);
        } else if (i == 1) {
            if (z) {
                i3 = R$string.kg_password_default_pin_message_one;
            } else {
                i3 = R$string.kg_password_wrong_pin_code_one;
            }
            str = getContext().getResources().getString(i3, Integer.valueOf(i));
        } else if (i > 1) {
            if (z) {
                i2 = R$plurals.kg_password_default_pin_message;
            } else {
                i2 = R$plurals.kg_password_wrong_pin_code;
            }
            str = getContext().getResources().getQuantityString(i2, i, Integer.valueOf(i));
        } else {
            str = getContext().getString(z ? R$string.kg_sim_pin_instructions : R$string.kg_password_pin_failed);
        }
        if (!KeyguardEsimArea.isEsimLocked(((LinearLayout) this).mContext, this.mSubId)) {
            return str;
        }
        return getResources().getString(R$string.kg_sim_lock_esim_instructions, str);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public int getPasswordTextViewId() {
        return R$id.simPinEntry;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSubDisplayName = (TextView) findViewById(R$id.sub_display_name);
        this.mSubId = this.mKgUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED);
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

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView, com.android.keyguard.KeyguardPinBasedInputView
    public void onResume(int i) {
        super.onResume(i);
        this.mKgUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        resetState();
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardAbsKeyInputView
    public void onPause() {
        ProgressDialog progressDialog = this.mSimUnlockProgressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
        KeyguardUpdateMonitor.getInstance(((LinearLayout) this).mContext).removeCallback(this.mUpdateMonitorCallback);
        this.mKgUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
        ProgressDialog progressDialog2 = this.mSimUnlockProgressDialog;
        if (progressDialog2 != null) {
            progressDialog2.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    /* access modifiers changed from: private */
    public abstract class CheckSimPin extends Thread {
        private final String mPin;
        private int mSubId;

        /* access modifiers changed from: package-private */
        public abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String str, int i) {
            this.mPin = str;
            this.mSubId = i;
        }

        public void run() {
            try {
                final int[] supplyPinReportResultForSubscriber = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(this.mSubId, this.mPin);
                KeyguardSimPinView.this.post(new Runnable() {
                    /* class com.android.keyguard.KeyguardSimPinView.CheckSimPin.AnonymousClass1 */

                    public void run() {
                        CheckSimPin checkSimPin = CheckSimPin.this;
                        int[] iArr = supplyPinReportResultForSubscriber;
                        checkSimPin.onSimCheckResponse(iArr[0], iArr[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e("KeyguardSimPinView", "RemoteException for supplyPinReportResult:", e);
                KeyguardSimPinView.this.post(new Runnable() {
                    /* class com.android.keyguard.KeyguardSimPinView.CheckSimPin.AnonymousClass2 */

                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(2, -1);
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
            this.mSimUnlockProgressDialog.getWindow().setType(2009);
        }
        return this.mSimUnlockProgressDialog;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Dialog getSimRemainingAttemptsDialog(int i) {
        String pinPasswordErrorMessage = getPinPasswordErrorMessage(i, false);
        AlertDialog alertDialog = this.mRemainingAttemptsDialog;
        if (alertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(((LinearLayout) this).mContext);
            builder.setMessage(pinPasswordErrorMessage);
            builder.setCancelable(false);
            if (getResources().getBoolean(R$bool.somc_show_title_in_sim_dialog)) {
                builder.setTitle(R$string.kg_somc_note);
            }
            builder.setNeutralButton(getResources().getBoolean(R$bool.somc_sim_show_continue_dialog) ? R$string.kg_somc_continue : R$string.ok, (DialogInterface.OnClickListener) null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            alertDialog.setMessage(pinPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.keyguard.KeyguardAbsKeyInputView
    public void verifyPasswordAndUnlock() {
        String text = this.mPasswordEntry.getText();
        if (text.length() < 4 || text.length() > 8) {
            this.mSecurityMessageDisplay.setMessage(R$string.kg_invalid_sim_pin_hint);
            resetPasswordText(true, true);
            this.mCallback.userActivity();
            return;
        }
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPinThread == null) {
            this.mCheckSimPinThread = new CheckSimPin(this.mPasswordEntry.getText(), this.mSubId) {
                /* class com.android.keyguard.KeyguardSimPinView.AnonymousClass3 */

                /* access modifiers changed from: package-private */
                @Override // com.android.keyguard.KeyguardSimPinView.CheckSimPin
                public void onSimCheckResponse(final int i, final int i2) {
                    KeyguardSimPinView.this.post(new Runnable() {
                        /* class com.android.keyguard.KeyguardSimPinView.AnonymousClass3.AnonymousClass1 */

                        public void run() {
                            KeyguardSimPinView.this.mRemainingAttempts = i2;
                            if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPinView.this.resetPasswordText(true, i != 0);
                            KeyguardSimPinView.this.mShowDefaultMessage = false;
                            int i = i;
                            if (i == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mSubId);
                                KeyguardSimPinView.this.mRemainingAttempts = -1;
                                KeyguardSimPinView.this.mShowDefaultMessage = true;
                                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPinView.this.mCallback;
                                if (keyguardSecurityCallback != null) {
                                    keyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else if (i == 1) {
                                KeyguardSimPinView keyguardSimPinView = KeyguardSimPinView.this;
                                keyguardSimPinView.mSecurityMessageDisplay.setMessage(keyguardSimPinView.getPinPasswordErrorMessage(i2, false));
                                int integer = KeyguardSimPinView.this.getResources().getInteger(R$integer.somc_sim_pin_attempts_remaining_before_dialog);
                                int i2 = i2;
                                if (i2 <= integer) {
                                    KeyguardSimPinView.this.getSimRemainingAttemptsDialog(i2).show();
                                } else {
                                    KeyguardSimPinView keyguardSimPinView2 = KeyguardSimPinView.this;
                                    keyguardSimPinView2.mSecurityMessageDisplay.setMessage(keyguardSimPinView2.getPinPasswordErrorMessage(i2, false));
                                }
                            } else {
                                KeyguardSimPinView keyguardSimPinView3 = KeyguardSimPinView.this;
                                keyguardSimPinView3.mSecurityMessageDisplay.setMessage(keyguardSimPinView3.getContext().getString(R$string.kg_password_pin_failed));
                            }
                            KeyguardSimPinView.this.mCallback.userActivity();
                            KeyguardSimPinView.this.mCheckSimPinThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPinThread.start();
        }
    }

    @Override // com.android.keyguard.KeyguardSecurityView, com.android.keyguard.KeyguardPinBasedInputView
    public CharSequence getTitle() {
        return getContext().getString(17040277);
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
