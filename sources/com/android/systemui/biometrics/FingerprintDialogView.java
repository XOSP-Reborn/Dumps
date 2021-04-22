package com.android.systemui.biometrics;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.LinearLayout;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;

public class FingerprintDialogView extends BiometricDialogView {
    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getAuthenticatedAccessibilityResourceId() {
        return 17040096;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getDelayAfterAuthenticatedDurationMs() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnimateForTransition(int i, int i2) {
        if (i2 == 2) {
            return true;
        }
        if (i == 2 && i2 == 1) {
            return true;
        }
        if (i == 1 && i2 == 4) {
            return false;
        }
        if (!(i == 2 && i2 == 4) && i2 == 1) {
        }
        return false;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public boolean shouldGrayAreaDismissDialog() {
        return true;
    }

    public FingerprintDialogView(Context context, DialogViewCallback dialogViewCallback) {
        super(context, dialogViewCallback);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void handleResetMessage() {
        updateState(1);
        this.mErrorText.setText(getHintStringResourceId());
        this.mErrorText.setTextColor(this.mTextColor);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getHintStringResourceId() {
        return C0014R$string.fingerprint_dialog_touch_sensor;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public int getIconDescriptionResourceId() {
        return C0014R$string.accessibility_fingerprint_dialog_fingerprint_icon;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.biometrics.BiometricDialogView
    public void updateIcon(int i, int i2) {
        Drawable animationForTransition = getAnimationForTransition(i, i2);
        if (animationForTransition == null) {
            Log.e("FingerprintDialogView", "Animation not found, " + i + " -> " + i2);
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable = animationForTransition instanceof AnimatedVectorDrawable ? (AnimatedVectorDrawable) animationForTransition : null;
        this.mBiometricIcon.setImageDrawable(animationForTransition);
        if (animatedVectorDrawable != null && shouldAnimateForTransition(i, i2)) {
            animatedVectorDrawable.forceAnimationOnUI();
            animatedVectorDrawable.start();
        }
    }

    /* access modifiers changed from: protected */
    public Drawable getAnimationForTransition(int i, int i2) {
        int i3;
        if (i2 == 2) {
            i3 = C0006R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i == 2 && i2 == 1) {
            i3 = C0006R$drawable.fingerprint_dialog_error_to_fp;
        } else if (i == 1 && i2 == 4) {
            i3 = C0006R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i == 2 && i2 == 4) {
            i3 = C0006R$drawable.fingerprint_dialog_fp_to_error;
        } else if (i2 != 1) {
            return null;
        } else {
            i3 = C0006R$drawable.fingerprint_dialog_fp_to_error;
        }
        return ((LinearLayout) this).mContext.getDrawable(i3);
    }
}
