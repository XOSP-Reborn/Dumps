package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0014R$string;
import com.android.systemui.dock.DockManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.UserInfoController;

public class LockIcon extends KeyguardAffordanceView implements UserInfoController.OnUserInfoChangedListener, StatusBarStateController.StateListener, ConfigurationController.ConfigurationListener, UnlockMethodCache.OnUnlockMethodChangedListener {
    private final AccessibilityController mAccessibilityController;
    private boolean mBouncerVisible;
    private final ConfigurationController mConfigurationController;
    private int mDensity;
    private final DockManager.DockEventListener mDockEventListener;
    private final DockManager mDockManager;
    private boolean mDocked;
    private float mDozeAmount;
    private boolean mDozing;
    private int mIconColor;
    private int mIconRes;
    private boolean mIsFaceUnlockState;
    private boolean mIsSkinningEnabled;
    private boolean mIsUnlockAnimation;
    private final KeyguardMonitor mKeyguardMonitor;
    private final KeyguardMonitor.Callback mKeyguardMonitorCallback;
    private boolean mKeyguardShowing;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mLastBouncerVisible;
    private boolean mLastDozing;
    private boolean mLastPulsing;
    private int mLastState;
    private final Handler mMainHandler;
    private boolean mPulsing;
    private boolean mShowingLaunchAffordance;
    private boolean mSimLocked;
    private final StatusBarStateController mStatusBarStateController;
    private boolean mTransientBiometricsError;
    private final UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private boolean mWakeAndUnlockRunning;
    private boolean mWasPulsingOnThisFrame;

    public LockIcon(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, null, null, null, null, null, null);
    }

    public LockIcon(Context context, AttributeSet attributeSet, StatusBarStateController statusBarStateController, ConfigurationController configurationController, AccessibilityController accessibilityController, KeyguardMonitor keyguardMonitor, DockManager dockManager, Handler handler) {
        super(context, attributeSet);
        this.mLastState = 0;
        this.mKeyguardMonitorCallback = new KeyguardMonitor.Callback() {
            /* class com.android.systemui.statusbar.phone.LockIcon.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.policy.KeyguardMonitor.Callback
            public void onKeyguardShowingChanged() {
                LockIcon lockIcon = LockIcon.this;
                lockIcon.mKeyguardShowing = lockIcon.mKeyguardMonitor.isShowing();
                LockIcon.this.update(false);
            }
        };
        this.mDockEventListener = new DockManager.DockEventListener() {
            /* class com.android.systemui.statusbar.phone.LockIcon.AnonymousClass2 */
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.systemui.statusbar.phone.LockIcon.AnonymousClass3 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onSimStateChanged(int i, int i2, IccCardConstants.State state) {
                boolean z = LockIcon.this.mSimLocked;
                LockIcon lockIcon = LockIcon.this;
                lockIcon.mSimLocked = lockIcon.mKeyguardUpdateMonitor.isSimPinSecure();
                LockIcon lockIcon2 = LockIcon.this;
                lockIcon2.update(z != lockIcon2.mSimLocked);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onKeyguardVisibilityChanged(boolean z) {
                LockIcon.this.update();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
                LockIcon.this.update();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onStrongAuthStateChanged(int i) {
                LockIcon.this.update();
            }
        };
        ((ImageView) this).mContext = context;
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(((ImageView) this).mContext);
        this.mAccessibilityController = accessibilityController;
        this.mConfigurationController = configurationController;
        this.mStatusBarStateController = statusBarStateController;
        this.mKeyguardMonitor = keyguardMonitor;
        this.mDockManager = dockManager;
        this.mMainHandler = handler;
        this.mIsSkinningEnabled = context.getResources().getBoolean(C0003R$bool.somc_keyguard_theme_enabled);
        this.mIsUnlockAnimation = false;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mStatusBarStateController.addCallback(this);
        this.mConfigurationController.addCallback(this);
        this.mKeyguardMonitor.addCallback(this.mKeyguardMonitorCallback);
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        this.mUnlockMethodCache.addListener(this);
        this.mSimLocked = this.mKeyguardUpdateMonitor.isSimPinSecure();
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.addListener(this.mDockEventListener);
        }
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mStatusBarStateController.removeCallback(this);
        this.mConfigurationController.removeCallback(this);
        this.mKeyguardUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
        this.mKeyguardMonitor.removeCallback(this.mKeyguardMonitorCallback);
        this.mUnlockMethodCache.removeListener(this);
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.removeListener(this.mDockEventListener);
        }
        this.mIsUnlockAnimation = false;
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        TypedArray obtainStyledAttributes = ((ImageView) this).mContext.getTheme().obtainStyledAttributes(null, new int[]{C0002R$attr.wallpaperTextColor}, 0, 0);
        this.mIconColor = obtainStyledAttributes.getColor(0, -1);
        obtainStyledAttributes.recycle();
        updateDarkTint();
    }

    @Override // com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        update();
    }

    public void setTransientBiometricsError(boolean z) {
        this.mTransientBiometricsError = z;
        update();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = configuration.densityDpi;
        if (i != this.mDensity) {
            this.mDensity = i;
            update();
        }
    }

    public void update() {
        update(false);
    }

    public void update(boolean z) {
        int state = getState();
        boolean z2 = true;
        int i = 0;
        this.mIsFaceUnlockState = state == 2;
        if (!(state == this.mLastState && this.mLastDozing == this.mDozing && this.mLastPulsing == this.mPulsing && this.mLastBouncerVisible == this.mBouncerVisible && !z)) {
            getAnimationResForTransition(this.mLastState, state, this.mLastPulsing, this.mPulsing, this.mLastDozing, this.mDozing, this.mBouncerVisible);
            int iconForState = getIconForState(state);
            if (iconForState != this.mIconRes || z) {
                this.mIconRes = iconForState;
                Drawable drawable = ((ImageView) this).mContext.getDrawable(iconForState);
                if (drawable instanceof AnimatedVectorDrawable) {
                    AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) drawable;
                }
                setImageDrawable(drawable, this.mIsSkinningEnabled);
                if (this.mIsFaceUnlockState) {
                    announceForAccessibility(getContext().getString(C0014R$string.accessibility_scanning_face));
                }
            }
            updateDarkTint();
            this.mLastState = state;
            this.mLastDozing = this.mDozing;
            this.mLastPulsing = this.mPulsing;
            this.mLastBouncerVisible = this.mBouncerVisible;
        }
        if (!(this.mDozing && (!this.mPulsing || this.mDocked)) && !this.mWakeAndUnlockRunning && !this.mShowingLaunchAffordance) {
            z2 = false;
        }
        if (z2) {
            i = 4;
        }
        setVisibility(i);
        updateClickability();
    }

    private void updateClickability() {
        if (this.mAccessibilityController != null) {
            boolean z = true;
            boolean z2 = this.mUnlockMethodCache.isMethodSecure() && this.mUnlockMethodCache.canSkipBouncer();
            boolean isAccessibilityEnabled = this.mAccessibilityController.isAccessibilityEnabled();
            setClickable(isAccessibilityEnabled);
            if (!z2 || isAccessibilityEnabled) {
                z = false;
            }
            setLongClickable(z);
            setFocusable(this.mAccessibilityController.isAccessibilityEnabled());
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        boolean isFingerprintDetectionRunning = this.mKeyguardUpdateMonitor.isFingerprintDetectionRunning();
        boolean isUnlockingWithBiometricAllowed = this.mKeyguardUpdateMonitor.isUnlockingWithBiometricAllowed();
        if (isFingerprintDetectionRunning && isUnlockingWithBiometricAllowed) {
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(C0014R$string.accessibility_unlock_without_fingerprint)));
            accessibilityNodeInfo.setHintText(getContext().getString(C0014R$string.accessibility_waiting_for_fingerprint));
        } else if (this.mIsFaceUnlockState) {
            accessibilityNodeInfo.setClassName(LockIcon.class.getName());
            accessibilityNodeInfo.setContentDescription(getContext().getString(C0014R$string.accessibility_scanning_face));
        }
    }

    private int getIconForState(int i) {
        if (i != 0) {
            if (i == 1) {
                return 17302457;
            }
            if (!(i == 2 || i == 3)) {
                throw new IllegalArgumentException();
            }
        }
        return 17302448;
    }

    private int getAnimationResForTransition(int i, int i2, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
        if (z4 && !z2 && !this.mWasPulsingOnThisFrame) {
            return -1;
        }
        boolean z6 = false;
        boolean z7 = i != 3 && i2 == 3;
        boolean z8 = i != 1 && i2 == 1;
        boolean z9 = i == 1 && i2 == 0;
        boolean z10 = !z && z2;
        if (z3 && !z4 && !this.mWasPulsingOnThisFrame) {
            z6 = true;
        }
        if (z7) {
            return 17432829;
        }
        if (z8) {
            return 17432830;
        }
        if (z9) {
            return 17432821;
        }
        if (i2 == 2 && z5) {
            return 17432822;
        }
        if ((z10 || z6) && i2 != 1) {
            return 17432820;
        }
        return -1;
    }

    private int getState() {
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(((ImageView) this).mContext);
        if (this.mTransientBiometricsError) {
            return 3;
        }
        if ((this.mUnlockMethodCache.canSkipBouncer() || !this.mKeyguardShowing) && !this.mSimLocked) {
            return 1;
        }
        return instance.isFaceDetectionRunning() ? 2 : 0;
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozeAmountChanged(float f, float f2) {
        this.mDozeAmount = f2;
        updateDarkTint();
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
        if (!this.mPulsing) {
            this.mWasPulsingOnThisFrame = true;
            this.mMainHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$LockIcon$TmjtrutkteGSA9bJnhAgff1P4s8 */

                public final void run() {
                    LockIcon.this.lambda$setPulsing$0$LockIcon();
                }
            });
        }
        update();
    }

    public /* synthetic */ void lambda$setPulsing$0$LockIcon() {
        this.mWasPulsingOnThisFrame = false;
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        this.mDozing = z;
        update();
    }

    private void updateDarkTint() {
        setImageTintList(ColorStateList.valueOf(ColorUtils.blendARGB(this.mIconColor, -1, this.mDozeAmount)));
    }

    public void setBouncerVisible(boolean z) {
        if (this.mBouncerVisible != z) {
            this.mBouncerVisible = z;
            update();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_width);
            layoutParams.height = getResources().getDimensionPixelSize(C0005R$dimen.keyguard_affordance_width);
            setLayoutParams(layoutParams);
            update(true);
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onLocaleListChanged() {
        setContentDescription(getContext().getText(C0014R$string.accessibility_unlock_button));
        update(true);
    }

    @Override // com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener
    public void onUnlockMethodStateChanged() {
        update();
    }

    public void onBiometricAuthModeChanged(boolean z) {
        if (z) {
            this.mWakeAndUnlockRunning = true;
        }
        update();
    }

    public void onShowingLaunchAffordanceChanged(boolean z) {
        this.mShowingLaunchAffordance = z;
        update();
    }

    public void onScrimVisibilityChanged(int i) {
        if (this.mWakeAndUnlockRunning && i == 0) {
            this.mWakeAndUnlockRunning = false;
            update();
        }
    }

    public void setIconAlpha(float f) {
        if (!this.mIsUnlockAnimation) {
            setAlpha(f);
        }
    }
}
