package com.android.keyguard;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.text.method.SingleLineTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.CarrierTextController;
import java.util.Locale;

public class CarrierText extends TextView {
    private static CharSequence mSeparator;
    private CarrierTextController.CarrierTextCallback mCarrierTextCallback;
    private CarrierTextController mCarrierTextController;
    private boolean mShouldMarquee;
    private boolean mShowAirplaneMode;
    private boolean mShowMissingSim;

    public CarrierText(Context context) {
        this(context, null);
    }

    /* JADX INFO: finally extract failed */
    public CarrierText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCarrierTextCallback = new CarrierTextController.CarrierTextCallback() {
            /* class com.android.keyguard.CarrierText.AnonymousClass1 */

            @Override // com.android.keyguard.CarrierTextController.CarrierTextCallback
            public void updateCarrierInfo(CarrierTextController.CarrierTextCallbackInfo carrierTextCallbackInfo) {
                CarrierText.this.setText(carrierTextCallbackInfo.carrierText);
            }

            @Override // com.android.keyguard.CarrierTextController.CarrierTextCallback
            public void startedGoingToSleep() {
                CarrierText.this.setSelected(false);
            }

            @Override // com.android.keyguard.CarrierTextController.CarrierTextCallback
            public void finishedWakingUp() {
                CarrierText.this.setSelected(true);
            }

            @Override // com.android.keyguard.CarrierTextController.CarrierTextCallback
            public boolean isInsideEmergencyCarrierArea() {
                return "insideEmergencyCarrierArea".equals(CarrierText.this.getTag());
            }
        };
        TypedArray obtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R$styleable.CarrierText, 0, 0);
        try {
            boolean z = obtainStyledAttributes.getBoolean(R$styleable.CarrierText_allCaps, false);
            this.mShowAirplaneMode = obtainStyledAttributes.getBoolean(R$styleable.CarrierText_showAirplaneMode, false);
            this.mShowMissingSim = obtainStyledAttributes.getBoolean(R$styleable.CarrierText_showMissingSim, false);
            obtainStyledAttributes.recycle();
            if (!this.mCarrierTextCallback.isInsideEmergencyCarrierArea()) {
                setTransformationMethod(new CarrierTextTransformationMethod(((TextView) this).mContext, z));
            }
        } catch (Throwable th) {
            obtainStyledAttributes.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        mSeparator = getResources().getString(17040328);
        this.mCarrierTextController = new CarrierTextController(((TextView) this).mContext, mSeparator, this.mShowAirplaneMode, this.mShowMissingSim);
        this.mShouldMarquee = KeyguardUpdateMonitor.getInstance(((TextView) this).mContext).isDeviceInteractive();
        setSelected(this.mShouldMarquee);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mCarrierTextController.setListening(this.mCarrierTextCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mCarrierTextController.setListening(null);
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (i == 0) {
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    private class CarrierTextTransformationMethod extends SingleLineTransformationMethod {
        private final boolean mAllCaps;
        private final Locale mLocale;

        public CarrierTextTransformationMethod(Context context, boolean z) {
            this.mLocale = context.getResources().getConfiguration().locale;
            this.mAllCaps = z;
        }

        public CharSequence getTransformation(CharSequence charSequence, View view) {
            CharSequence transformation = super.getTransformation(charSequence, view);
            return (!this.mAllCaps || transformation == null) ? transformation : transformation.toString().toUpperCase(this.mLocale);
        }
    }
}
