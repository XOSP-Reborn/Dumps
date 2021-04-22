package com.android.systemui.biometrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;

public class FaceDialogView extends BiometricDialogView {
    private boolean mDialogAnimatedIn;
    private final Runnable mErrorToIdleAnimationRunnable = new Runnable() {
        /* class com.android.systemui.biometrics.$$Lambda$FaceDialogView$czDcP2iyglsmecT6GyDucy4syc */

        public final void run() {
            FaceDialogView.this.lambda$new$0$FaceDialogView();
        }
    };
    private IconController mIconController = new IconController();
    private float mIconOriginalY;
    private DialogOutlineProvider mOutlineProvider = new DialogOutlineProvider();
    private int mSize;

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getDelayAfterAuthenticatedDurationMs() {
        return 500;
    }

    private final class IconController extends Animatable2.AnimationCallback {
        private boolean mLastPulseDirection;
        int mState = 0;

        IconController() {
        }

        public void animateOnce(int i) {
            animateIcon(i, false);
        }

        public void showStatic(int i) {
            FaceDialogView faceDialogView = FaceDialogView.this;
            faceDialogView.mBiometricIcon.setImageDrawable(((LinearLayout) faceDialogView).mContext.getDrawable(i));
        }

        public void startPulsing() {
            this.mLastPulseDirection = false;
            animateIcon(C0006R$drawable.face_dialog_pulse_dark_to_light, true);
        }

        public void showIcon(int i) {
            FaceDialogView.this.mBiometricIcon.setImageDrawable(((LinearLayout) FaceDialogView.this).mContext.getDrawable(i));
        }

        private void animateIcon(int i, boolean z) {
            AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) ((LinearLayout) FaceDialogView.this).mContext.getDrawable(i);
            FaceDialogView.this.mBiometricIcon.setImageDrawable(animatedVectorDrawable);
            animatedVectorDrawable.forceAnimationOnUI();
            if (z) {
                animatedVectorDrawable.registerAnimationCallback(this);
            }
            animatedVectorDrawable.start();
        }

        private void pulseInNextDirection() {
            int i;
            if (this.mLastPulseDirection) {
                i = C0006R$drawable.face_dialog_pulse_dark_to_light;
            } else {
                i = C0006R$drawable.face_dialog_pulse_light_to_dark;
            }
            animateIcon(i, true);
            this.mLastPulseDirection = !this.mLastPulseDirection;
        }

        public void onAnimationEnd(Drawable drawable) {
            super.onAnimationEnd(drawable);
            if (this.mState == 1) {
                pulseInNextDirection();
            }
        }
    }

    /* access modifiers changed from: private */
    public final class DialogOutlineProvider extends ViewOutlineProvider {
        float mY;

        private DialogOutlineProvider() {
        }

        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, (int) this.mY, FaceDialogView.this.mDialog.getWidth(), FaceDialogView.this.mDialog.getBottom(), FaceDialogView.this.getResources().getDimension(C0005R$dimen.biometric_dialog_corner_size));
        }

        /* access modifiers changed from: package-private */
        public int calculateSmall() {
            return (FaceDialogView.this.mDialog.getHeight() - FaceDialogView.this.mBiometricIcon.getHeight()) - (((int) FaceDialogView.this.dpToPixels(16.0f)) * 2);
        }

        /* access modifiers changed from: package-private */
        public void setOutlineY(float f) {
            this.mY = f;
        }
    }

    public /* synthetic */ void lambda$new$0$FaceDialogView() {
        updateState(0);
        this.mErrorText.setVisibility(4);
    }

    public FaceDialogView(Context context, DialogViewCallback dialogViewCallback) {
        super(context, dialogViewCallback);
    }

    private void updateSize(int i) {
        float height = ((float) (this.mDialog.getHeight() - this.mBiometricIcon.getHeight())) - dpToPixels(16.0f);
        if (i == 1) {
            this.mTitleText.setVisibility(4);
            this.mErrorText.setVisibility(4);
            this.mNegativeButton.setVisibility(4);
            if (!TextUtils.isEmpty(this.mSubtitleText.getText())) {
                this.mSubtitleText.setVisibility(4);
            }
            if (!TextUtils.isEmpty(this.mDescriptionText.getText())) {
                this.mDescriptionText.setVisibility(4);
            }
            this.mBiometricIcon.setY(height);
            this.mDialog.setOutlineProvider(this.mOutlineProvider);
            DialogOutlineProvider dialogOutlineProvider = this.mOutlineProvider;
            dialogOutlineProvider.setOutlineY((float) dialogOutlineProvider.calculateSmall());
            this.mDialog.setClipToOutline(true);
            this.mDialog.invalidateOutline();
            this.mSize = i;
        } else if (this.mSize == 1 && i == 3) {
            this.mSize = 2;
            ValueAnimator ofFloat = ValueAnimator.ofFloat((float) this.mOutlineProvider.calculateSmall(), 0.0f);
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.biometrics.$$Lambda$FaceDialogView$MYsjnJHs10NhJPXX4FLFafo9YY8 */

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FaceDialogView.this.lambda$updateSize$1$FaceDialogView(valueAnimator);
                }
            });
            ValueAnimator ofFloat2 = ValueAnimator.ofFloat(height, this.mIconOriginalY);
            ofFloat2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.biometrics.$$Lambda$FaceDialogView$sSRypCm7hC9Of8MaBum8gJxI9Q */

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FaceDialogView.this.lambda$updateSize$2$FaceDialogView(valueAnimator);
                }
            });
            ValueAnimator ofFloat3 = ValueAnimator.ofFloat(dpToPixels(32.0f), 0.0f);
            ofFloat3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.biometrics.$$Lambda$FaceDialogView$6DWEWGhnaIhNrLSCCr7Op0b0jD4 */

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FaceDialogView.this.lambda$updateSize$3$FaceDialogView(valueAnimator);
                }
            });
            ValueAnimator ofFloat4 = ValueAnimator.ofFloat(0.0f, 1.0f);
            ofFloat4.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                /* class com.android.systemui.biometrics.$$Lambda$FaceDialogView$y85DatSeGK11aptJj_FqyvqURDw */

                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    FaceDialogView.this.lambda$updateSize$4$FaceDialogView(valueAnimator);
                }
            });
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setDuration(150L);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.biometrics.FaceDialogView.AnonymousClass1 */

                public void onAnimationStart(Animator animator) {
                    super.onAnimationStart(animator);
                    FaceDialogView.this.mTitleText.setVisibility(0);
                    FaceDialogView.this.mErrorText.setVisibility(0);
                    FaceDialogView.this.mNegativeButton.setVisibility(0);
                    FaceDialogView.this.mTryAgainButton.setVisibility(0);
                    if (!TextUtils.isEmpty(FaceDialogView.this.mSubtitleText.getText())) {
                        FaceDialogView.this.mSubtitleText.setVisibility(0);
                    }
                    if (!TextUtils.isEmpty(FaceDialogView.this.mDescriptionText.getText())) {
                        FaceDialogView.this.mDescriptionText.setVisibility(0);
                    }
                }

                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    FaceDialogView.this.mSize = 3;
                }
            });
            animatorSet.play(ofFloat).with(ofFloat2).with(ofFloat4).with(ofFloat3);
            animatorSet.start();
        } else if (this.mSize == 3) {
            this.mDialog.setClipToOutline(false);
            this.mDialog.invalidateOutline();
            this.mBiometricIcon.setY(this.mIconOriginalY);
            this.mSize = i;
        }
    }

    public /* synthetic */ void lambda$updateSize$1$FaceDialogView(ValueAnimator valueAnimator) {
        this.mOutlineProvider.setOutlineY(((Float) valueAnimator.getAnimatedValue()).floatValue());
        this.mDialog.invalidateOutline();
    }

    public /* synthetic */ void lambda$updateSize$2$FaceDialogView(ValueAnimator valueAnimator) {
        this.mBiometricIcon.setY(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    public /* synthetic */ void lambda$updateSize$3$FaceDialogView(ValueAnimator valueAnimator) {
        this.mErrorText.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    public /* synthetic */ void lambda$updateSize$4$FaceDialogView(ValueAnimator valueAnimator) {
        float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.mTitleText.setAlpha(floatValue);
        this.mErrorText.setAlpha(floatValue);
        this.mNegativeButton.setAlpha(floatValue);
        this.mTryAgainButton.setAlpha(floatValue);
        if (!TextUtils.isEmpty(this.mSubtitleText.getText())) {
            this.mSubtitleText.setAlpha(floatValue);
        }
        if (!TextUtils.isEmpty(this.mDescriptionText.getText())) {
            this.mDescriptionText.setAlpha(floatValue);
        }
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void onSaveState(Bundle bundle) {
        super.onSaveState(bundle);
        bundle.putInt("key_dialog_size", this.mSize);
        bundle.putBoolean("key_dialog_animated_in", this.mDialogAnimatedIn);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void handleResetMessage() {
        this.mErrorText.setText(getHintStringResourceId());
        this.mErrorText.setContentDescription(((LinearLayout) this).mContext.getString(getHintStringResourceId()));
        this.mErrorText.setTextColor(this.mTextColor);
        if (getState() == 1) {
            this.mErrorText.setVisibility(0);
        } else {
            this.mErrorText.setVisibility(4);
        }
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void restoreState(Bundle bundle) {
        super.restoreState(bundle);
        this.mSize = bundle.getInt("key_dialog_size");
        this.mDialogAnimatedIn = bundle.getBoolean("key_dialog_animated_in");
    }

    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mIconOriginalY == 0.0f) {
            this.mIconOriginalY = this.mBiometricIcon.getY();
        }
        int i5 = this.mSize;
        if (i5 != 0) {
            if (i5 == 1) {
                updateSize(1);
            }
        } else if (!requiresConfirmation()) {
            updateSize(1);
        } else {
            updateSize(3);
        }
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void onErrorReceived(String str) {
        super.onErrorReceived(str);
        if (this.mSize == 1) {
            updateSize(3);
        }
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void onAuthenticationFailed(String str) {
        super.onAuthenticationFailed(str);
        showTryAgainButton(true);
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void showTryAgainButton(boolean z) {
        if (z && this.mSize == 1) {
            updateSize(3);
        } else if (z) {
            this.mTryAgainButton.setVisibility(0);
        } else {
            this.mTryAgainButton.setVisibility(8);
        }
        if (z) {
            this.mPositiveButton.setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getHintStringResourceId() {
        return C0014R$string.face_dialog_looking_for_face;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getAuthenticatedAccessibilityResourceId() {
        return this.mRequireConfirmation ? 17040056 : 17040057;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getIconDescriptionResourceId() {
        return C0014R$string.accessibility_face_dialog_face_icon;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void updateIcon(int i, int i2) {
        IconController iconController = this.mIconController;
        iconController.mState = i2;
        if (i2 == 1) {
            this.mHandler.removeCallbacks(this.mErrorToIdleAnimationRunnable);
            if (this.mDialogAnimatedIn) {
                this.mIconController.startPulsing();
                this.mErrorText.setVisibility(0);
            } else {
                this.mIconController.showIcon(C0006R$drawable.face_dialog_pulse_dark_to_light);
            }
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_authenticating));
        } else if (i == 3 && i2 == 4) {
            iconController.animateOnce(C0006R$drawable.face_dialog_dark_to_checkmark);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_confirmed));
        } else if (i == 2 && i2 == 0) {
            this.mIconController.animateOnce(C0006R$drawable.face_dialog_error_to_idle);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_idle));
        } else if (i == 2 && i2 == 4) {
            this.mHandler.removeCallbacks(this.mErrorToIdleAnimationRunnable);
            this.mIconController.animateOnce(C0006R$drawable.face_dialog_dark_to_checkmark);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_authenticated));
        } else if (i2 == 2) {
            if (!this.mHandler.hasCallbacks(this.mErrorToIdleAnimationRunnable)) {
                this.mIconController.animateOnce(C0006R$drawable.face_dialog_dark_to_error);
                this.mHandler.postDelayed(this.mErrorToIdleAnimationRunnable, 2000);
            }
        } else if (i == 1 && i2 == 4) {
            this.mIconController.animateOnce(C0006R$drawable.face_dialog_dark_to_checkmark);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_authenticated));
        } else if (i2 == 3) {
            this.mHandler.removeCallbacks(this.mErrorToIdleAnimationRunnable);
            this.mIconController.animateOnce(C0006R$drawable.face_dialog_wink_from_dark);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_authenticated));
        } else if (i2 == 0) {
            this.mIconController.showStatic(C0006R$drawable.face_dialog_idle_static);
            this.mBiometricIcon.setContentDescription(((LinearLayout) this).mContext.getString(C0014R$string.biometric_dialog_face_icon_description_idle));
        } else {
            Log.w("FaceDialogView", "Unknown animation from " + i + " -> " + i2);
        }
        if (i == 2 && i2 == 2) {
            this.mHandler.removeCallbacks(this.mErrorToIdleAnimationRunnable);
            this.mHandler.postDelayed(this.mErrorToIdleAnimationRunnable, 2000);
        }
    }

    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void onDialogAnimatedIn() {
        this.mDialogAnimatedIn = true;
        this.mIconController.startPulsing();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public boolean shouldGrayAreaDismissDialog() {
        return this.mSize != 1;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private float dpToPixels(float f) {
        return f * (((float) ((LinearLayout) this).mContext.getResources().getDisplayMetrics().densityDpi) / 160.0f);
    }
}
