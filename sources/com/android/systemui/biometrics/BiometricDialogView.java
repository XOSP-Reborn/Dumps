package com.android.systemui.biometrics;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.Interpolators;
import com.android.systemui.biometrics.BiometricDialogView;
import com.android.systemui.util.leak.RotationUtils;

public abstract class BiometricDialogView extends LinearLayout {
    private boolean mAnimatingAway;
    private final float mAnimationTranslationOffset;
    protected final ImageView mBiometricIcon;
    private Bundle mBundle;
    protected final DialogViewCallback mCallback;
    protected final TextView mDescriptionText;
    private final DevicePolicyManager mDevicePolicyManager;
    protected final LinearLayout mDialog;
    private final float mDialogWidth;
    private final int mErrorColor;
    protected final TextView mErrorText;
    protected Handler mHandler = new Handler() {
        /* class com.android.systemui.biometrics.BiometricDialogView.AnonymousClass2 */

        public void handleMessage(Message message) {
            if (message.what != 1) {
                Log.e("BiometricDialogView", "Unhandled message: " + message.what);
                return;
            }
            BiometricDialogView.this.handleResetMessage();
        }
    };
    protected final ViewGroup mLayout;
    private final Interpolator mLinearOutSlowIn;
    protected final Button mNegativeButton;
    protected final Button mPositiveButton;
    protected boolean mRequireConfirmation;
    private Bundle mRestoredState;
    private final Runnable mShowAnimationRunnable = new Runnable() {
        /* class com.android.systemui.biometrics.BiometricDialogView.AnonymousClass1 */

        public void run() {
            BiometricDialogView.this.mLayout.animate().alpha(1.0f).setDuration(250).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().start();
            BiometricDialogView.this.mDialog.animate().translationY(0.0f).setDuration(250).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(new Runnable() {
                /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$1$qmKE8gu8fBxrvu_aT4hBUtdMU */

                public final void run() {
                    BiometricDialogView.AnonymousClass1.this.lambda$run$0$BiometricDialogView$1();
                }
            }).start();
        }

        public /* synthetic */ void lambda$run$0$BiometricDialogView$1() {
            BiometricDialogView.this.onDialogAnimatedIn();
        }
    };
    private boolean mSkipIntro;
    private int mState = 0;
    protected final TextView mSubtitleText;
    protected final int mTextColor;
    protected final TextView mTitleText;
    protected final Button mTryAgainButton;
    private int mUserId;
    private final UserManager mUserManager;
    private boolean mWasForceRemoved;
    private final WindowManager mWindowManager;
    private final IBinder mWindowToken = new Binder();

    /* access modifiers changed from: protected */
    public abstract int getAuthenticatedAccessibilityResourceId();

    /* access modifiers changed from: protected */
    public abstract int getDelayAfterAuthenticatedDurationMs();

    /* access modifiers changed from: protected */
    public abstract int getHintStringResourceId();

    /* access modifiers changed from: protected */
    public abstract int getIconDescriptionResourceId();

    /* access modifiers changed from: protected */
    public abstract void handleResetMessage();

    public void onDialogAnimatedIn() {
    }

    /* access modifiers changed from: protected */
    public abstract boolean shouldGrayAreaDismissDialog();

    public void showTryAgainButton(boolean z) {
    }

    /* access modifiers changed from: protected */
    public abstract void updateIcon(int i, int i2);

    public BiometricDialogView(Context context, DialogViewCallback dialogViewCallback) {
        super(context);
        this.mCallback = dialogViewCallback;
        this.mLinearOutSlowIn = Interpolators.LINEAR_OUT_SLOW_IN;
        this.mWindowManager = (WindowManager) ((LinearLayout) this).mContext.getSystemService(WindowManager.class);
        this.mUserManager = (UserManager) ((LinearLayout) this).mContext.getSystemService(UserManager.class);
        this.mDevicePolicyManager = (DevicePolicyManager) ((LinearLayout) this).mContext.getSystemService(DevicePolicyManager.class);
        this.mAnimationTranslationOffset = getResources().getDimension(C0005R$dimen.biometric_dialog_animation_translation_offset);
        this.mErrorColor = getResources().getColor(C0004R$color.biometric_dialog_error);
        this.mTextColor = getResources().getColor(C0004R$color.biometric_dialog_gray);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.mDialogWidth = (float) Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(C0010R$layout.biometric_dialog, (ViewGroup) this, false);
        addView(this.mLayout);
        this.mLayout.setOnKeyListener(new View.OnKeyListener() {
            /* class com.android.systemui.biometrics.BiometricDialogView.AnonymousClass3 */
            boolean downPressed = false;

            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i != 4) {
                    return false;
                }
                if (keyEvent.getAction() == 0 && !this.downPressed) {
                    this.downPressed = true;
                } else if (keyEvent.getAction() == 0) {
                    this.downPressed = false;
                } else if (keyEvent.getAction() == 1 && this.downPressed) {
                    this.downPressed = false;
                    BiometricDialogView.this.mCallback.onUserCanceled();
                }
                return true;
            }
        });
        View findViewById = this.mLayout.findViewById(C0007R$id.space);
        View findViewById2 = this.mLayout.findViewById(C0007R$id.left_space);
        View findViewById3 = this.mLayout.findViewById(C0007R$id.right_space);
        this.mDialog = (LinearLayout) this.mLayout.findViewById(C0007R$id.dialog);
        this.mTitleText = (TextView) this.mLayout.findViewById(C0007R$id.title);
        this.mSubtitleText = (TextView) this.mLayout.findViewById(C0007R$id.subtitle);
        this.mDescriptionText = (TextView) this.mLayout.findViewById(C0007R$id.description);
        this.mBiometricIcon = (ImageView) this.mLayout.findViewById(C0007R$id.biometric_icon);
        this.mErrorText = (TextView) this.mLayout.findViewById(C0007R$id.error);
        this.mNegativeButton = (Button) this.mLayout.findViewById(C0007R$id.button2);
        this.mPositiveButton = (Button) this.mLayout.findViewById(C0007R$id.button1);
        this.mTryAgainButton = (Button) this.mLayout.findViewById(C0007R$id.button_try_again);
        this.mBiometricIcon.setContentDescription(getResources().getString(getIconDescriptionResourceId()));
        setDismissesDialog(findViewById);
        setDismissesDialog(findViewById2);
        setDismissesDialog(findViewById3);
        this.mNegativeButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$RYcTkb_tfg9qgMigefaLgT2rmQ */

            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$0$BiometricDialogView(view);
            }
        });
        this.mPositiveButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$2B_4hvtZC5hBNK8tMhbM4pc0Qyc */

            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$2$BiometricDialogView(view);
            }
        });
        this.mTryAgainButton.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$0WbgvKDgE592VyX7dMGcDXbGTQ */

            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$3$BiometricDialogView(view);
            }
        });
        this.mLayout.setFocusableInTouchMode(true);
        this.mLayout.requestFocus();
    }

    public /* synthetic */ void lambda$new$0$BiometricDialogView(View view) {
        int i = this.mState;
        if (i == 3 || i == 4) {
            this.mCallback.onUserCanceled();
        } else {
            this.mCallback.onNegativePressed();
        }
    }

    public /* synthetic */ void lambda$new$2$BiometricDialogView(View view) {
        updateState(4);
        this.mHandler.postDelayed(new Runnable() {
            /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$Qw9PCsGZ_LOQrNNiplnrZAouws */

            public final void run() {
                BiometricDialogView.this.lambda$new$1$BiometricDialogView();
            }
        }, (long) getDelayAfterAuthenticatedDurationMs());
    }

    public /* synthetic */ void lambda$new$1$BiometricDialogView() {
        this.mCallback.onPositivePressed();
    }

    public /* synthetic */ void lambda$new$3$BiometricDialogView(View view) {
        handleResetMessage();
        updateState(1);
        showTryAgainButton(false);
        this.mCallback.onTryAgainPressed();
    }

    public void onSaveState(Bundle bundle) {
        bundle.putInt("key_try_again_visibility", this.mTryAgainButton.getVisibility());
        bundle.putInt("key_confirm_visibility", this.mPositiveButton.getVisibility());
        bundle.putInt("key_state", this.mState);
        bundle.putInt("key_error_text_visibility", this.mErrorText.getVisibility());
        bundle.putCharSequence("key_error_text_string", this.mErrorText.getText());
        bundle.putBoolean("key_error_text_is_temporary", this.mHandler.hasMessages(1));
        bundle.putInt("key_error_text_color", this.mErrorText.getCurrentTextColor());
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ImageView imageView = (ImageView) this.mLayout.findViewById(C0007R$id.background);
        if (this.mUserManager.isManagedProfile(this.mUserId)) {
            Drawable drawable = getResources().getDrawable(C0006R$drawable.work_challenge_background, ((LinearLayout) this).mContext.getTheme());
            drawable.setColorFilter(this.mDevicePolicyManager.getOrganizationColorForUser(this.mUserId), PorterDuff.Mode.DARKEN);
            imageView.setImageDrawable(drawable);
        } else {
            imageView.setImageDrawable(null);
            imageView.setBackgroundColor(C0004R$color.biometric_dialog_dim_color);
        }
        this.mNegativeButton.setVisibility(0);
        if (RotationUtils.getRotation(((LinearLayout) this).mContext) != 0) {
            this.mDialog.getLayoutParams().width = (int) this.mDialogWidth;
        }
        if (this.mRestoredState == null) {
            updateState(1);
            this.mErrorText.setText(getHintStringResourceId());
            this.mErrorText.setContentDescription(((LinearLayout) this).mContext.getString(getHintStringResourceId()));
            this.mErrorText.setVisibility(0);
        } else {
            updateState(this.mState);
        }
        CharSequence charSequence = this.mBundle.getCharSequence("title");
        this.mTitleText.setVisibility(0);
        this.mTitleText.setText(charSequence);
        CharSequence charSequence2 = this.mBundle.getCharSequence("subtitle");
        if (TextUtils.isEmpty(charSequence2)) {
            this.mSubtitleText.setVisibility(8);
        } else {
            this.mSubtitleText.setVisibility(0);
            this.mSubtitleText.setText(charSequence2);
        }
        CharSequence charSequence3 = this.mBundle.getCharSequence("description");
        if (TextUtils.isEmpty(charSequence3)) {
            this.mDescriptionText.setVisibility(8);
        } else {
            this.mDescriptionText.setVisibility(0);
            this.mDescriptionText.setText(charSequence3);
        }
        this.mNegativeButton.setText(this.mBundle.getCharSequence("negative_text"));
        if (requiresConfirmation() && this.mRestoredState == null) {
            this.mPositiveButton.setVisibility(0);
            this.mPositiveButton.setEnabled(false);
        }
        if (this.mWasForceRemoved || this.mSkipIntro) {
            this.mLayout.animate().cancel();
            this.mDialog.animate().cancel();
            this.mDialog.setAlpha(1.0f);
            this.mDialog.setTranslationY(0.0f);
            this.mLayout.setAlpha(1.0f);
        } else {
            this.mDialog.setTranslationY(this.mAnimationTranslationOffset);
            this.mLayout.setAlpha(0.0f);
            postOnAnimation(this.mShowAnimationRunnable);
        }
        this.mWasForceRemoved = false;
        this.mSkipIntro = false;
    }

    private void setDismissesDialog(View view) {
        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.biometrics.$$Lambda$BiometricDialogView$agcwyvTgMSypTMy6oXZQaR3oBGY */

            public final void onClick(View view) {
                BiometricDialogView.this.lambda$setDismissesDialog$4$BiometricDialogView(view);
            }
        });
    }

    public /* synthetic */ void lambda$setDismissesDialog$4$BiometricDialogView(View view) {
        if (this.mState != 4 && shouldGrayAreaDismissDialog()) {
            this.mCallback.onUserCanceled();
        }
    }

    public void startDismiss() {
        this.mAnimatingAway = true;
        final AnonymousClass4 r0 = new Runnable() {
            /* class com.android.systemui.biometrics.BiometricDialogView.AnonymousClass4 */

            public void run() {
                BiometricDialogView.this.mWindowManager.removeView(BiometricDialogView.this);
                BiometricDialogView.this.mAnimatingAway = false;
                BiometricDialogView.this.handleResetMessage();
                BiometricDialogView.this.showTryAgainButton(false);
                BiometricDialogView.this.updateState(0);
            }
        };
        postOnAnimation(new Runnable() {
            /* class com.android.systemui.biometrics.BiometricDialogView.AnonymousClass5 */

            public void run() {
                BiometricDialogView.this.mLayout.animate().alpha(0.0f).setDuration(350).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().start();
                BiometricDialogView.this.mDialog.animate().translationY(BiometricDialogView.this.mAnimationTranslationOffset).setDuration(350).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(r0).start();
            }
        });
    }

    public void forceRemove() {
        this.mLayout.animate().cancel();
        this.mDialog.animate().cancel();
        this.mWindowManager.removeView(this);
        this.mAnimatingAway = false;
        this.mWasForceRemoved = true;
    }

    public void setSkipIntro(boolean z) {
        this.mSkipIntro = z;
    }

    public void setBundle(Bundle bundle) {
        this.mBundle = bundle;
    }

    public void setRequireConfirmation(boolean z) {
        this.mRequireConfirmation = z;
    }

    public boolean requiresConfirmation() {
        return this.mRequireConfirmation;
    }

    public void setUserId(int i) {
        this.mUserId = i;
    }

    /* access modifiers changed from: protected */
    public void showTemporaryMessage(String str) {
        this.mHandler.removeMessages(1);
        this.mErrorText.setText(str);
        this.mErrorText.setTextColor(this.mErrorColor);
        this.mErrorText.setContentDescription(str);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), 2000);
    }

    public void onHelpReceived(String str) {
        updateState(2);
        showTemporaryMessage(str);
    }

    public void onAuthenticationFailed(String str) {
        updateState(2);
        showTemporaryMessage(str);
    }

    public void onErrorReceived(String str) {
        updateState(2);
        showTemporaryMessage(str);
        showTryAgainButton(false);
        this.mCallback.onErrorShown();
    }

    public void updateState(int i) {
        if (i == 3) {
            this.mHandler.removeMessages(1);
            this.mErrorText.setVisibility(4);
            this.mPositiveButton.setVisibility(0);
            this.mPositiveButton.setEnabled(true);
        } else if (i == 4) {
            this.mPositiveButton.setVisibility(8);
            this.mNegativeButton.setVisibility(8);
            this.mErrorText.setVisibility(4);
        }
        if (i == 3 || i == 4) {
            this.mNegativeButton.setText(C0014R$string.cancel);
        }
        updateIcon(this.mState, i);
        this.mState = i;
    }

    public void restoreState(Bundle bundle) {
        this.mRestoredState = bundle;
        this.mTryAgainButton.setVisibility(bundle.getInt("key_try_again_visibility"));
        this.mPositiveButton.setVisibility(bundle.getInt("key_confirm_visibility"));
        this.mState = bundle.getInt("key_state");
        this.mErrorText.setText(bundle.getCharSequence("key_error_text_string"));
        this.mErrorText.setContentDescription(bundle.getCharSequence("key_error_text_string"));
        this.mErrorText.setVisibility(bundle.getInt("key_error_text_visibility"));
        this.mErrorText.setTextColor(bundle.getInt("key_error_text_color"));
        if (bundle.getBoolean("key_error_text_is_temporary")) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1), 2000);
        }
    }

    /* access modifiers changed from: protected */
    public int getState() {
        return this.mState;
    }

    public WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2014, 16777216, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("BiometricDialogView");
        layoutParams.token = this.mWindowToken;
        return layoutParams;
    }
}
