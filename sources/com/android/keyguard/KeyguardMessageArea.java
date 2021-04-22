package com.android.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.lang.ref.WeakReference;

public class KeyguardMessageArea extends TextView implements SecurityMessageDisplay, ConfigurationController.ConfigurationListener {
    private static final Object ANNOUNCE_TOKEN = new Object();
    private boolean mBouncerVisible;
    private final ConfigurationController mConfigurationController;
    private ColorStateList mDefaultColorState;
    private CharSequence mDefaultMessage;
    private final Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private boolean mIsSkinningEnabled;
    private CharSequence mMessage;
    private ColorStateList mNextMessageColorState;
    private ColorStateList mThemeColorState;

    public KeyguardMessageArea(Context context) {
        super(context, null);
        this.mThemeColorState = ColorStateList.valueOf(-1);
        this.mNextMessageColorState = ColorStateList.valueOf(-1);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardMessageArea.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onFinishedGoingToSleep(int i) {
                KeyguardMessageArea.this.setSelected(false);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onKeyguardBouncerChanged(boolean z) {
                KeyguardMessageArea.this.mBouncerVisible = z;
                KeyguardMessageArea.this.update();
            }
        };
        throw new IllegalStateException("This constructor should never be invoked");
    }

    public KeyguardMessageArea(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attributeSet, ConfigurationController configurationController) {
        this(context, attributeSet, KeyguardUpdateMonitor.getInstance(context), configurationController);
    }

    public KeyguardMessageArea(Context context, AttributeSet attributeSet, KeyguardUpdateMonitor keyguardUpdateMonitor, ConfigurationController configurationController) {
        super(context, attributeSet);
        this.mThemeColorState = ColorStateList.valueOf(-1);
        this.mNextMessageColorState = ColorStateList.valueOf(-1);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardMessageArea.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onFinishedGoingToSleep(int i) {
                KeyguardMessageArea.this.setSelected(false);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onKeyguardBouncerChanged(boolean z) {
                KeyguardMessageArea.this.mBouncerVisible = z;
                KeyguardMessageArea.this.update();
            }
        };
        setLayerType(2, null);
        keyguardUpdateMonitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mConfigurationController = configurationController;
        this.mIsSkinningEnabled = context.getResources().getBoolean(R$bool.somc_keyguard_theme_enabled);
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mConfigurationController.addCallback(this);
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mConfigurationController.removeCallback(this);
    }

    @Override // com.android.keyguard.SecurityMessageDisplay
    public void setDefaultMessageColor(int i) {
        this.mThemeColorState = ColorStateList.valueOf(i);
        onThemeChanged();
    }

    @Override // com.android.keyguard.SecurityMessageDisplay
    public void setNextMessageColor(ColorStateList colorStateList) {
        this.mNextMessageColorState = colorStateList;
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        TypedArray obtainStyledAttributes = ((TextView) this).mContext.obtainStyledAttributes(new int[]{R$attr.wallpaperTextColor});
        ColorStateList valueOf = ColorStateList.valueOf(obtainStyledAttributes.getColor(0, -65536));
        obtainStyledAttributes.recycle();
        if (this.mIsSkinningEnabled) {
            valueOf = this.mThemeColorState;
        }
        this.mDefaultColorState = valueOf;
        update();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        TypedArray obtainStyledAttributes = ((TextView) this).mContext.obtainStyledAttributes(R$style.Keyguard_TextView, new int[]{16842901});
        setTextSize(0, (float) obtainStyledAttributes.getDimensionPixelSize(0, 0));
        obtainStyledAttributes.recycle();
    }

    @Override // com.android.keyguard.SecurityMessageDisplay
    public void setMessage(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence) || TextUtils.isEmpty(this.mMessage)) {
            securityMessageChanged(charSequence);
        } else {
            clearMessage();
        }
    }

    @Override // com.android.keyguard.SecurityMessageDisplay
    public void setMessage(int i) {
        setMessage(i != 0 ? getContext().getResources().getText(i) : null);
    }

    @Override // com.android.keyguard.SecurityMessageDisplay
    public void setDefaultMessage(int i) {
        this.mDefaultMessage = getContext().getResources().getText(i);
        this.mHandler.post(new Runnable() {
            /* class com.android.keyguard.KeyguardMessageArea.AnonymousClass2 */

            public void run() {
                KeyguardMessageArea.this.update();
            }
        });
    }

    public static KeyguardMessageArea findSecurityMessageDisplay(View view) {
        KeyguardMessageArea keyguardMessageArea = (KeyguardMessageArea) view.findViewById(R$id.keyguard_message_area);
        if (keyguardMessageArea == null) {
            keyguardMessageArea = (KeyguardMessageArea) view.getRootView().findViewById(R$id.keyguard_message_area);
        }
        if (keyguardMessageArea != null) {
            return keyguardMessageArea;
        }
        throw new RuntimeException("Can't find keyguard_message_area in " + view.getClass());
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        setSelected(KeyguardUpdateMonitor.getInstance(((TextView) this).mContext).isDeviceInteractive());
    }

    private void securityMessageChanged(CharSequence charSequence) {
        this.mMessage = charSequence;
        update();
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    private void clearMessage() {
        this.mMessage = null;
        update();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void update() {
        CharSequence charSequence = TextUtils.isEmpty(this.mMessage) ? this.mDefaultMessage : this.mMessage;
        setVisibility(TextUtils.isEmpty(charSequence) ? 4 : 0);
        setText(charSequence);
        ColorStateList colorStateList = this.mDefaultColorState;
        if (this.mNextMessageColorState.getDefaultColor() != -1) {
            colorStateList = this.mNextMessageColorState;
            this.mNextMessageColorState = ColorStateList.valueOf(-1);
        }
        setTextColor(colorStateList);
    }

    /* access modifiers changed from: private */
    public static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View view, CharSequence charSequence) {
            this.mHost = new WeakReference<>(view);
            this.mTextToAnnounce = charSequence;
        }

        public void run() {
            View view = this.mHost.get();
            if (view != null) {
                view.announceForAccessibility(this.mTextToAnnounce);
            }
        }
    }
}
