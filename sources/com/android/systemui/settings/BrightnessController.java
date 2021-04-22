package com.android.systemui.settings;

import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.systemui.Dependency;
import com.android.systemui.settings.ToggleSlider;
import com.sonymobile.settingslib.display.BrightnessUtils;
import java.util.ArrayList;
import java.util.Iterator;

public class BrightnessController implements ToggleSlider.Listener {
    private volatile boolean mAutomatic;
    private final boolean mAutomaticAvailable;
    private final Handler mBackgroundHandler;
    private final BrightnessObserver mBrightnessObserver;
    private final BrightnessUtils mBrightnessUtils;
    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final Context mContext;
    private final ToggleSlider mControl;
    private boolean mControlValueInitialized;
    private final int mDefaultBacklight;
    private final int mDefaultBacklightForVr;
    private final DisplayManager mDisplayManager;
    private boolean mExternalChange;
    private final Handler mHandler = new Handler() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass6 */

        public void handleMessage(Message message) {
            boolean z = true;
            BrightnessController.this.mExternalChange = true;
            try {
                int i = message.what;
                if (i == 1) {
                    BrightnessController brightnessController = BrightnessController.this;
                    int i2 = message.arg1;
                    if (message.arg2 == 0) {
                        z = false;
                    }
                    brightnessController.updateSlider(i2, z);
                } else if (i == 2) {
                    ToggleSlider toggleSlider = BrightnessController.this.mControl;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    toggleSlider.setChecked(z);
                } else if (i == 3) {
                    BrightnessController.this.mControl.setOnChangedListener(BrightnessController.this);
                } else if (i == 4) {
                    BrightnessController.this.mControl.setOnChangedListener(null);
                } else if (i != 5) {
                    super.handleMessage(message);
                } else {
                    BrightnessController brightnessController2 = BrightnessController.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    brightnessController2.updateVrMode(z);
                }
            } finally {
                BrightnessController.this.mExternalChange = false;
            }
        }
    };
    private volatile boolean mIsVrModeEnabled;
    private boolean mListening;
    private final int mMaximumBacklight;
    private final int mMaximumBacklightForVr;
    private final int mMinimumBacklight;
    private final int mMinimumBacklightForVr;
    private ValueAnimator mSliderAnimator;
    private final Runnable mStartListeningRunnable = new Runnable() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass1 */

        public void run() {
            BrightnessController.this.mBrightnessObserver.startObserving();
            BrightnessController.this.mUserTracker.startTracking();
            BrightnessController.this.mUpdateModeRunnable.run();
            BrightnessController.this.mUpdateSliderRunnable.run();
            BrightnessController.this.mHandler.sendEmptyMessage(3);
        }
    };
    private final Runnable mStopListeningRunnable = new Runnable() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass2 */

        public void run() {
            BrightnessController.this.mBrightnessObserver.stopObserving();
            BrightnessController.this.mUserTracker.stopTracking();
            BrightnessController.this.mHandler.sendEmptyMessage(4);
        }
    };
    private final Runnable mUpdateModeRunnable = new Runnable() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass3 */

        public void run() {
            boolean z = false;
            if (BrightnessController.this.mAutomaticAvailable) {
                int intForUser = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
                BrightnessController brightnessController = BrightnessController.this;
                if (intForUser != 0) {
                    z = true;
                }
                brightnessController.mAutomatic = z;
                return;
            }
            BrightnessController.this.mHandler.obtainMessage(2, 0).sendToTarget();
        }
    };
    private final Runnable mUpdateSliderRunnable = new Runnable() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass4 */

        public void run() {
            int i;
            boolean z = BrightnessController.this.mIsVrModeEnabled;
            if (z) {
                i = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_for_vr", BrightnessController.this.mDefaultBacklightForVr, -2);
            } else {
                i = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", BrightnessController.this.mDefaultBacklight, -2);
            }
            BrightnessController.this.mHandler.obtainMessage(1, i, z ? 1 : 0).sendToTarget();
        }
    };
    private final CurrentUserTracker mUserTracker;
    private final IVrManager mVrManager;
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        /* class com.android.systemui.settings.BrightnessController.AnonymousClass5 */

        public void onVrStateChanged(boolean z) {
            BrightnessController.this.mHandler.obtainMessage(5, z ? 1 : 0, 0).sendToTarget();
        }
    };

    public interface BrightnessStateChangeCallback {
        void onBrightnessLevelChanged();
    }

    @Override // com.android.systemui.settings.ToggleSlider.Listener
    public void onInit(ToggleSlider toggleSlider) {
    }

    /* access modifiers changed from: private */
    public class BrightnessObserver extends ContentObserver {
        private final Uri BRIGHTNESS_FOR_VR_URI = Settings.System.getUriFor("screen_brightness_for_vr");
        private final Uri BRIGHTNESS_MODE_URI = Settings.System.getUriFor("screen_brightness_mode");
        private final Uri BRIGHTNESS_URI = Settings.System.getUriFor("screen_brightness");

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z) {
            onChange(z, null);
        }

        public void onChange(boolean z, Uri uri) {
            if (!z) {
                if (this.BRIGHTNESS_MODE_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_FOR_VR_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                }
                Iterator it = BrightnessController.this.mChangeCallbacks.iterator();
                while (it.hasNext()) {
                    ((BrightnessStateChangeCallback) it.next()).onBrightnessLevelChanged();
                }
            }
        }

        public void startObserving() {
            ContentResolver contentResolver = BrightnessController.this.mContext.getContentResolver();
            contentResolver.unregisterContentObserver(this);
            contentResolver.registerContentObserver(this.BRIGHTNESS_MODE_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_URI, false, this, -1);
            contentResolver.registerContentObserver(this.BRIGHTNESS_FOR_VR_URI, false, this, -1);
        }

        public void stopObserving() {
            BrightnessController.this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public BrightnessController(Context context, ToggleSlider toggleSlider) {
        this.mContext = context;
        this.mControl = toggleSlider;
        this.mControl.setMax(1023);
        this.mBackgroundHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            /* class com.android.systemui.settings.BrightnessController.AnonymousClass7 */

            @Override // com.android.systemui.settings.CurrentUserTracker
            public void onUserSwitched(int i) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            }
        };
        this.mBrightnessObserver = new BrightnessObserver(this.mHandler);
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMinimumBacklight = powerManager.getMinimumScreenBrightnessSetting();
        this.mMaximumBacklight = powerManager.getMaximumScreenBrightnessSetting();
        this.mDefaultBacklight = powerManager.getDefaultScreenBrightnessSetting();
        this.mMinimumBacklightForVr = powerManager.getMinimumScreenBrightnessForVrSetting();
        this.mMaximumBacklightForVr = powerManager.getMaximumScreenBrightnessForVrSetting();
        this.mDefaultBacklightForVr = powerManager.getDefaultScreenBrightnessForVrSetting();
        this.mAutomaticAvailable = context.getResources().getBoolean(17891368);
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mVrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        this.mBrightnessUtils = new BrightnessUtils(context.getResources());
    }

    public void registerCallbacks() {
        if (!this.mListening) {
            IVrManager iVrManager = this.mVrManager;
            if (iVrManager != null) {
                try {
                    iVrManager.registerListener(this.mVrStateCallbacks);
                    this.mIsVrModeEnabled = this.mVrManager.getVrModeState();
                } catch (RemoteException e) {
                    Log.e("StatusBar.BrightnessController", "Failed to register VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStartListeningRunnable);
            this.mListening = true;
        }
    }

    public void unregisterCallbacks() {
        if (this.mListening) {
            IVrManager iVrManager = this.mVrManager;
            if (iVrManager != null) {
                try {
                    iVrManager.unregisterListener(this.mVrStateCallbacks);
                } catch (RemoteException e) {
                    Log.e("StatusBar.BrightnessController", "Failed to unregister VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStopListeningRunnable);
            this.mListening = false;
            this.mControlValueInitialized = false;
        }
    }

    @Override // com.android.systemui.settings.ToggleSlider.Listener
    public void onChanged(ToggleSlider toggleSlider, boolean z, boolean z2, int i, boolean z3) {
        int i2;
        int i3;
        final String str;
        int i4;
        if (!this.mExternalChange) {
            ValueAnimator valueAnimator = this.mSliderAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            if (this.mIsVrModeEnabled) {
                i3 = 498;
                i2 = this.mMinimumBacklightForVr;
                i4 = this.mMaximumBacklightForVr;
                str = "screen_brightness_for_vr";
            } else {
                i3 = this.mAutomatic ? 219 : 218;
                i2 = this.mMinimumBacklight;
                i4 = this.mMaximumBacklight;
                str = "screen_brightness";
            }
            final int convertGammaToLinear = this.mBrightnessUtils.convertGammaToLinear(i, i2, i4);
            if (z3) {
                MetricsLogger.action(this.mContext, i3, convertGammaToLinear);
            }
            setTemporaryBrightness(convertGammaToLinear);
            if (!z) {
                AsyncTask.execute(new Runnable() {
                    /* class com.android.systemui.settings.BrightnessController.AnonymousClass8 */

                    public void run() {
                        Settings.System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), str, convertGammaToLinear, -2);
                        BrightnessController.this.unsetTemporaryBrightness();
                    }
                });
            }
            Iterator<BrightnessStateChangeCallback> it = this.mChangeCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onBrightnessLevelChanged();
            }
        }
    }

    public void checkRestrictionAndSetEnabled() {
        this.mBackgroundHandler.post(new Runnable() {
            /* class com.android.systemui.settings.BrightnessController.AnonymousClass9 */

            public void run() {
                ((ToggleSliderView) BrightnessController.this.mControl).setEnforcedAdmin(RestrictedLockUtilsInternal.checkIfRestrictionEnforced(BrightnessController.this.mContext, "no_config_brightness", BrightnessController.this.mUserTracker.getCurrentUserId()));
            }
        });
    }

    private void setTemporaryBrightness(int i) {
        this.mDisplayManager.setTemporaryBrightness(i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void unsetTemporaryBrightness() {
        this.mDisplayManager.unsetTemporaryBrightness();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateVrMode(boolean z) {
        if (this.mIsVrModeEnabled != z) {
            this.mIsVrModeEnabled = z;
            this.mBackgroundHandler.post(this.mUpdateSliderRunnable);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSlider(int i, boolean z) {
        int i2;
        int i3;
        if (z) {
            i2 = this.mMinimumBacklightForVr;
            i3 = this.mMaximumBacklightForVr;
        } else {
            i2 = this.mMinimumBacklight;
            i3 = this.mMaximumBacklight;
        }
        if (i != this.mBrightnessUtils.convertGammaToLinear(this.mControl.getValue(), i2, i3)) {
            animateSliderTo(this.mBrightnessUtils.convertLinearToGamma(i, i2, i3));
        }
    }

    private void animateSliderTo(int i) {
        if (!this.mControlValueInitialized) {
            this.mControl.setValue(i);
            this.mControlValueInitialized = true;
        }
        ValueAnimator valueAnimator = this.mSliderAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mSliderAnimator.cancel();
        }
        this.mSliderAnimator = ValueAnimator.ofInt(this.mControl.getValue(), i);
        this.mSliderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.settings.$$Lambda$BrightnessController$T5g_am3jKit6CD1eLLpr05aFxc */

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                BrightnessController.this.lambda$animateSliderTo$0$BrightnessController(valueAnimator);
            }
        });
        this.mSliderAnimator.setDuration((long) ((Math.abs(this.mControl.getValue() - i) * 3000) / 1023));
        this.mSliderAnimator.start();
    }

    public /* synthetic */ void lambda$animateSliderTo$0$BrightnessController(ValueAnimator valueAnimator) {
        this.mExternalChange = true;
        this.mControl.setValue(((Integer) valueAnimator.getAnimatedValue()).intValue());
        this.mExternalChange = false;
    }
}
