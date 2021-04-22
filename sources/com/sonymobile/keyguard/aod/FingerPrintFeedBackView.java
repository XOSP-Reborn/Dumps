package com.sonymobile.keyguard.aod;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback;

public class FingerPrintFeedBackView extends FrameLayout {
    private Context mContext;
    private final Handler mHandler = new Handler() {
        /* class com.sonymobile.keyguard.aod.FingerPrintFeedBackView.AnonymousClass3 */

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                FingerPrintFeedBackView.this.mShouldShowFingerPrintMessage = false;
                FingerPrintFeedBackView.this.setDefautMessage();
            } else if (i == 2) {
                FingerPrintFeedBackView.this.mPhotoPlaybackDemoMode = true;
                FingerPrintFeedBackView.this.setDefautMessage();
            } else if (i == 3) {
                FingerPrintFeedBackView.this.switchIndication("");
            } else if (i == 4) {
                FingerPrintFeedBackView.this.setDefautMessage();
            }
        }
    };
    private final LockscreenStyleCoverController mLockscreenStyleCoverController;
    private boolean mPhotoPlaybackDemoMode = false;
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() {
        /* class com.sonymobile.keyguard.aod.FingerPrintFeedBackView.AnonymousClass1 */

        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            if (!FingerPrintFeedBackView.this.mShouldShowFingerPrintMessage) {
                FingerPrintFeedBackView.this.mHandler.sendMessage(FingerPrintFeedBackView.this.mHandler.obtainMessage(4));
            }
        }
    };
    private boolean mShouldShowAlbumDisabledMessage = false;
    private boolean mShouldShowFingerPrintMessage = false;
    private LockscreenStyleCoverControllerCallback mStyleCoverCallback = new LockscreenStyleCoverControllerCallback() {
        /* class com.sonymobile.keyguard.aod.FingerPrintFeedBackView.AnonymousClass2 */

        @Override // com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback
        public void onStyleCoverClosed(boolean z) {
            if (z) {
                FingerPrintFeedBackView.this.switchIndication("");
            }
        }
    };
    private KeyguardUpdateMonitorCallback mUpdateMonitor;

    private boolean isOverDoubleTapMaxCount(int i) {
        return i >= 3;
    }

    public FingerPrintFeedBackView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        registerCallbacks(KeyguardUpdateMonitor.getInstance(context));
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        this.mLockscreenStyleCoverController = (LockscreenStyleCoverController) Dependency.get(LockscreenStyleCoverController.class);
        setDefautMessage();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLockscreenStyleCoverController.registerCallback(this.mStyleCoverCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mLockscreenStyleCoverController.removeCallback(this.mStyleCoverCallback);
    }

    public void switchIndication(CharSequence charSequence) {
        removeAllViews();
        TextView textView = new TextView(this.mContext);
        if (TextUtils.isEmpty(charSequence) || this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed()) {
            textView.setText("");
        } else {
            textView.setText(charSequence);
        }
        textView.setTextColor(-1);
        textView.setTextSize(14.0f);
        textView.setBackground(getResources().getDrawable(C0006R$drawable.somc_aod_messages_bg));
        if (textView.getText().equals("")) {
            textView.setBackgroundColor(0);
        }
        textView.setGravity(17);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        int applyDimension = (int) TypedValue.applyDimension(1, 16.0f, getResources().getDisplayMetrics());
        layoutParams.setMargins(applyDimension, 0, applyDimension, (int) TypedValue.applyDimension(1, 48.0f, getResources().getDisplayMetrics()));
        layoutParams.gravity = 81;
        addView(textView, layoutParams);
    }

    public void switchIndication(int i) {
        switchIndication(getResources().getText(i));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setDefautMessage() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
        if (this.mPhotoPlaybackDemoMode) {
            switchIndication(C0014R$string.somc_keyguard_guide_photo_playback);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(3), 15000);
        } else if (this.mShouldShowAlbumDisabledMessage) {
            switchIndication(getResources().getString(C0014R$string.lockscreen_ambient_recall_disable_toast_error_txt, PhotoPlaybackProviderUtils.getAlbumApplicationName(getContext())));
            Handler handler2 = this.mHandler;
            handler2.sendMessageDelayed(handler2.obtainMessage(3), 15000);
        } else if (isOverDoubleTapMaxCount(getDoubleTapCount(KeyguardUpdateMonitor.getCurrentUser()))) {
            switchIndication("");
        } else {
            switchIndication(C0014R$string.somc_keyguard_unlock_guide_ambient);
            Handler handler3 = this.mHandler;
            handler3.sendMessageDelayed(handler3.obtainMessage(3), 15000);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setTransientMessage(String str) {
        this.mHandler.removeMessages(1);
        this.mShouldShowFingerPrintMessage = true;
        switchIndication(str);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), 5000);
    }

    private void registerCallbacks(KeyguardUpdateMonitor keyguardUpdateMonitor) {
        keyguardUpdateMonitor.registerCallback(getFBCallback());
    }

    /* access modifiers changed from: protected */
    public KeyguardUpdateMonitorCallback getFBCallback() {
        if (this.mUpdateMonitor == null) {
            this.mUpdateMonitor = new FingerprintFBCallBack();
        }
        return this.mUpdateMonitor;
    }

    /* access modifiers changed from: protected */
    public class FingerprintFBCallBack extends KeyguardUpdateMonitorCallback {
        protected FingerprintFBCallBack() {
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
            FingerPrintFeedBackView.this.setTransientMessage(str);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
            if (i != 5) {
                FingerPrintFeedBackView.this.setTransientMessage(str);
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            FingerPrintFeedBackView.this.setDefautMessage();
        }
    }

    public void onParentDoubleTap() {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        int doubleTapCount = getDoubleTapCount(currentUser);
        if (!isOverDoubleTapMaxCount(doubleTapCount)) {
            int i = doubleTapCount + 1;
            setDoubleTapCount(i, currentUser);
            if (isOverDoubleTapMaxCount(i)) {
                setDefautMessage();
            }
        }
    }

    private int getDoubleTapCount(int i) {
        return this.mContext.getSharedPreferences("AodSharedPref", 0).getInt("aod_double_tap_count_" + String.valueOf(i), 0);
    }

    private void setDoubleTapCount(int i, int i2) {
        SharedPreferences.Editor edit = this.mContext.getSharedPreferences("AodSharedPref", 0).edit();
        edit.putInt("aod_double_tap_count_" + String.valueOf(i2), i);
        edit.commit();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setDefautMessage();
    }

    public void setPhotoPlayBackDemoMode(boolean z, long j) {
        if (z) {
            this.mPhotoPlaybackDemoMode = true;
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(2), j);
            return;
        }
        this.mPhotoPlaybackDemoMode = false;
        Handler handler2 = this.mHandler;
        handler2.sendMessage(handler2.obtainMessage(4));
    }

    public void showAlbumDisabledMessage(boolean z) {
        this.mShouldShowAlbumDisabledMessage = z;
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(4));
    }
}
