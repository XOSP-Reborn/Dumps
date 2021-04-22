package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.R$bool;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.google.android.collect.Lists;
import com.sonymobile.keyguard.statistics.LockscreenStatisticsWirelessChargerReporter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class StatusBarWindowController implements RemoteInputController.Callback, Dumpable, ConfigurationController.ConfigurationListener {
    private final IActivityManager mActivityManager;
    private int mBarHeight;
    private final ArrayList<WeakReference<StatusBarWindowCallback>> mCallbacks;
    private final SysuiColorExtractor mColorExtractor;
    private final Context mContext;
    private final State mCurrentState;
    private final DozeParameters mDozeParameters;
    private boolean mHasTopUi;
    private boolean mHasTopUiChanged;
    private boolean mKeyguardScreenRotation;
    private OtherwisedCollapsedListener mListener;
    private WindowManager.LayoutParams mLp;
    private final WindowManager.LayoutParams mLpChanged;
    private float mScreenBrightnessDoze;
    private final StatusBarStateController.StateListener mStateListener;
    private ViewGroup mStatusBarView;
    private final WindowManager mWindowManager;

    public interface OtherwisedCollapsedListener {
        void setWouldOtherwiseCollapse(boolean z);
    }

    public StatusBarWindowController(Context context) {
        this(context, (WindowManager) context.getSystemService(WindowManager.class), ActivityManager.getService(), DozeParameters.getInstance(context));
    }

    @VisibleForTesting
    public StatusBarWindowController(Context context, WindowManager windowManager, IActivityManager iActivityManager, DozeParameters dozeParameters) {
        this.mCurrentState = new State();
        this.mCallbacks = Lists.newArrayList();
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mStateListener = new StatusBarStateController.StateListener() {
            /* class com.android.systemui.statusbar.phone.StatusBarWindowController.AnonymousClass1 */

            @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
            public void onStateChanged(int i) {
                StatusBarWindowController.this.setStatusBarState(i);
            }

            @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
            public void onDozingChanged(boolean z) {
                StatusBarWindowController.this.setDozing(z);
            }
        };
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mActivityManager = iActivityManager;
        this.mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
        this.mDozeParameters = dozeParameters;
        this.mScreenBrightnessDoze = this.mDozeParameters.getScreenBrightnessDoze();
        this.mLpChanged = new WindowManager.LayoutParams();
        ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this.mStateListener, 1);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void registerCallback(StatusBarWindowCallback statusBarWindowCallback) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == statusBarWindowCallback) {
                return;
            }
        }
        this.mCallbacks.add(new WeakReference<>(statusBarWindowCallback));
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Resources resources = this.mContext.getResources();
        if (SystemProperties.getBoolean("lockscreen.rot_override", false) || resources.getBoolean(R$bool.config_enableLockScreenRotation)) {
            return true;
        }
        return false;
    }

    public void add(ViewGroup viewGroup, int i) {
        this.mLp = new WindowManager.LayoutParams(-1, i, 2000, -2138832824, -3);
        this.mLp.token = new Binder();
        WindowManager.LayoutParams layoutParams = this.mLp;
        layoutParams.gravity = 48;
        layoutParams.softInputMode = 16;
        layoutParams.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        WindowManager.LayoutParams layoutParams2 = this.mLp;
        layoutParams2.layoutInDisplayCutoutMode = 1;
        this.mStatusBarView = viewGroup;
        this.mBarHeight = i;
        this.mWindowManager.addView(this.mStatusBarView, layoutParams2);
        this.mLpChanged.copyFrom(this.mLp);
        onThemeChanged();
    }

    public ViewGroup getStatusBarView() {
        return this.mStatusBarView;
    }

    public void setDozeScreenBrightness(int i) {
        this.mScreenBrightnessDoze = ((float) i) / 255.0f;
    }

    private void setKeyguardDark(boolean z) {
        int systemUiVisibility = this.mStatusBarView.getSystemUiVisibility();
        this.mStatusBarView.setSystemUiVisibility(z ? systemUiVisibility | 16 | 8192 : systemUiVisibility & -17 & -8193);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            this.mLpChanged.privateFlags |= 1024;
        } else {
            this.mLpChanged.privateFlags &= -1025;
        }
        boolean z = true;
        boolean z2 = state.scrimsVisibility == 2;
        if (!state.keyguardShowing && (!state.dozing || !this.mDozeParameters.getAlwaysOn())) {
            z = false;
        }
        if (!z || state.backdropShowing || z2) {
            this.mLpChanged.flags &= -1048577;
        } else {
            this.mLpChanged.flags |= 1048576;
        }
        if (state.dozing) {
            this.mLpChanged.privateFlags |= 524288;
            return;
        }
        this.mLpChanged.privateFlags &= -524289;
    }

    private void adjustScreenOrientation(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() && !state.dozing) {
            this.mLpChanged.screenOrientation = -1;
        } else if (this.mKeyguardScreenRotation) {
            this.mLpChanged.screenOrientation = 2;
        } else {
            this.mLpChanged.screenOrientation = 5;
        }
    }

    private void applyFocusableFlag(State state) {
        boolean z = state.statusBarFocusable && state.panelExpanded;
        if ((state.bouncerShowing && (state.keyguardOccluded || state.keyguardNeedsInput)) || ((NotificationRemoteInputManager.ENABLE_REMOTE_INPUT && state.remoteInputActive) || state.bubbleExpanded)) {
            WindowManager.LayoutParams layoutParams = this.mLpChanged;
            layoutParams.flags &= -9;
            layoutParams.flags &= -131073;
        } else if (state.isKeyguardShowingAndNotOccluded() || z) {
            WindowManager.LayoutParams layoutParams2 = this.mLpChanged;
            layoutParams2.flags &= -9;
            layoutParams2.flags |= 131072;
        } else {
            WindowManager.LayoutParams layoutParams3 = this.mLpChanged;
            layoutParams3.flags |= 8;
            layoutParams3.flags &= -131073;
        }
        this.mLpChanged.softInputMode = 16;
    }

    private void applyForceShowNavigationFlag(State state) {
        if (state.panelExpanded || state.bouncerShowing || (NotificationRemoteInputManager.ENABLE_REMOTE_INPUT && state.remoteInputActive)) {
            this.mLpChanged.privateFlags |= 8388608;
            return;
        }
        this.mLpChanged.privateFlags &= -8388609;
    }

    private void applyHeight(State state) {
        boolean isExpanded = isExpanded(state);
        if (state.forcePluginOpen) {
            OtherwisedCollapsedListener otherwisedCollapsedListener = this.mListener;
            if (otherwisedCollapsedListener != null) {
                otherwisedCollapsedListener.setWouldOtherwiseCollapse(isExpanded);
            }
            isExpanded = true;
        }
        if (isExpanded) {
            this.mLpChanged.height = -1;
            return;
        }
        this.mLpChanged.height = this.mBarHeight;
    }

    private boolean isExpanded(State state) {
        return !state.forceCollapsed && (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing || state.headsUpShowing || state.bubblesShowing || state.scrimsVisibility != 0);
    }

    private void applyFitsSystemWindows(State state) {
        boolean z = !state.isKeyguardShowingAndNotOccluded();
        ViewGroup viewGroup = this.mStatusBarView;
        if (viewGroup != null && viewGroup.getFitsSystemWindows() != z) {
            this.mStatusBarView.setFitsSystemWindows(z);
            this.mStatusBarView.requestApplyInsets();
        }
    }

    private void applyUserActivityTimeout(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = 15000;
        } else if ((state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && state.bouncerShowing) || (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 2 && state.bouncerShowing)) {
            this.mLpChanged.userActivityTimeout = 15000;
        } else if ((!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || !state.qsExpanded) && (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 2)) {
            this.mLpChanged.userActivityTimeout = -1;
        } else {
            this.mLpChanged.userActivityTimeout = 15000;
        }
    }

    private void applyInputFeatures(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || state.qsExpanded || state.forceUserActivity) {
            this.mLpChanged.inputFeatures &= -5;
            return;
        }
        this.mLpChanged.inputFeatures |= 4;
    }

    private void applyStatusBarColorSpaceAgnosticFlag(State state) {
        if (!isExpanded(state)) {
            this.mLpChanged.privateFlags |= 16777216;
            return;
        }
        this.mLpChanged.privateFlags &= -16777217;
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        applyForceShowNavigationFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        applyHasTopUi(state);
        applyNotTouchable(state);
        applyStatusBarColorSpaceAgnosticFlag(state);
        WindowManager.LayoutParams layoutParams = this.mLp;
        if (!(layoutParams == null || layoutParams.copyFrom(this.mLpChanged) == 0)) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
        boolean z = this.mHasTopUi;
        boolean z2 = this.mHasTopUiChanged;
        if (z != z2) {
            try {
                this.mActivityManager.setHasTopUi(z2);
            } catch (RemoteException e) {
                Log.e("StatusBarWindowController", "Failed to call setHasTopUi", e);
            }
            this.mHasTopUi = this.mHasTopUiChanged;
        }
        notifyStateChangedCallbacks();
    }

    public void notifyStateChangedCallbacks() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            StatusBarWindowCallback statusBarWindowCallback = this.mCallbacks.get(i).get();
            if (statusBarWindowCallback != null) {
                State state = this.mCurrentState;
                statusBarWindowCallback.onStateChanged(state.keyguardShowing, state.keyguardOccluded, state.bouncerShowing);
            }
        }
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        if (state.forceStatusBarVisible) {
            this.mLpChanged.privateFlags |= 4096;
            return;
        }
        this.mLpChanged.privateFlags &= -4097;
    }

    private void applyModalFlag(State state) {
        if (state.headsUpShowing) {
            this.mLpChanged.flags |= 32;
            return;
        }
        this.mLpChanged.flags &= -33;
    }

    private void applyBrightness(State state) {
        if (state.forceDozeBrightness) {
            this.mLpChanged.screenBrightness = this.mScreenBrightnessDoze;
            return;
        }
        this.mLpChanged.screenBrightness = -1.0f;
    }

    private void applyHasTopUi(State state) {
        this.mHasTopUiChanged = isExpanded(state);
    }

    private void applyNotTouchable(State state) {
        if (state.notTouchable) {
            this.mLpChanged.flags |= 16;
            return;
        }
        this.mLpChanged.flags &= -17;
    }

    public void setKeyguardShowing(boolean z) {
        State state = this.mCurrentState;
        state.keyguardShowing = z;
        apply(state);
    }

    public void setKeyguardOccluded(boolean z) {
        State state = this.mCurrentState;
        state.keyguardOccluded = z;
        apply(state);
    }

    public void setKeyguardNeedsInput(boolean z) {
        State state = this.mCurrentState;
        state.keyguardNeedsInput = z;
        apply(state);
    }

    public void setPanelVisible(boolean z) {
        State state = this.mCurrentState;
        state.panelVisible = z;
        state.statusBarFocusable = z;
        apply(state);
    }

    public void setStatusBarFocusable(boolean z) {
        State state = this.mCurrentState;
        state.statusBarFocusable = z;
        apply(state);
    }

    public void setBouncerShowing(boolean z) {
        State state = this.mCurrentState;
        state.bouncerShowing = z;
        apply(state);
    }

    public void setBackdropShowing(boolean z) {
        State state = this.mCurrentState;
        state.backdropShowing = z;
        apply(state);
    }

    public void setKeyguardFadingAway(boolean z) {
        State state = this.mCurrentState;
        state.keyguardFadingAway = z;
        apply(state);
    }

    public void setQsExpanded(boolean z) {
        State state = this.mCurrentState;
        state.qsExpanded = z;
        apply(state);
    }

    public void setScrimsVisibility(int i) {
        State state = this.mCurrentState;
        state.scrimsVisibility = i;
        apply(state);
    }

    public void setHeadsUpShowing(boolean z) {
        State state = this.mCurrentState;
        state.headsUpShowing = z;
        apply(state);
    }

    public void setWallpaperSupportsAmbientMode(boolean z) {
        State state = this.mCurrentState;
        state.wallpaperSupportsAmbientMode = z;
        apply(state);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setStatusBarState(int i) {
        State state = this.mCurrentState;
        state.statusBarState = i;
        apply(state);
    }

    public void setForceStatusBarVisible(boolean z) {
        State state = this.mCurrentState;
        state.forceStatusBarVisible = z;
        apply(state);
    }

    public void setForceWindowCollapsed(boolean z) {
        State state = this.mCurrentState;
        state.forceCollapsed = z;
        apply(state);
    }

    public void setPanelExpanded(boolean z) {
        State state = this.mCurrentState;
        state.panelExpanded = z;
        apply(state);
    }

    @Override // com.android.systemui.statusbar.RemoteInputController.Callback
    public void onRemoteInputActive(boolean z) {
        State state = this.mCurrentState;
        state.remoteInputActive = z;
        apply(state);
    }

    public void setForceDozeBrightness(boolean z) {
        State state = this.mCurrentState;
        state.forceDozeBrightness = z;
        apply(state);
    }

    public void setDozing(boolean z) {
        State state = this.mCurrentState;
        state.dozing = z;
        apply(state);
    }

    public void setBarHeight(int i) {
        this.mBarHeight = i;
        apply(this.mCurrentState);
    }

    public void setForcePluginOpen(boolean z) {
        State state = this.mCurrentState;
        state.forcePluginOpen = z;
        apply(state);
    }

    public void setNotTouchable(boolean z) {
        State state = this.mCurrentState;
        state.notTouchable = z;
        apply(state);
    }

    public void setBubblesShowing(boolean z) {
        State state = this.mCurrentState;
        state.bubblesShowing = z;
        apply(state);
    }

    public boolean getBubblesShowing() {
        return this.mCurrentState.bubblesShowing;
    }

    public void setBubbleExpanded(boolean z) {
        State state = this.mCurrentState;
        state.bubbleExpanded = z;
        apply(state);
    }

    public boolean getPanelExpanded() {
        return this.mCurrentState.panelExpanded;
    }

    public void setStateListener(OtherwisedCollapsedListener otherwisedCollapsedListener) {
        this.mListener = otherwisedCollapsedListener;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        ViewGroup viewGroup;
        printWriter.println("StatusBarWindowController state:");
        printWriter.println(this.mCurrentState);
        if (Build.IS_DEBUGGABLE && (viewGroup = this.mStatusBarView) != null && viewGroup.getViewRootImpl() != null) {
            this.mStatusBarView.getViewRootImpl().dump("StatusBarView:", fileDescriptor, printWriter, strArr);
        }
    }

    public boolean isShowingWallpaper() {
        return !this.mCurrentState.backdropShowing;
    }

    public void setKeyguardScreenRotation(boolean z) {
        if (this.mKeyguardScreenRotation != z && this.mLpChanged != null) {
            this.mKeyguardScreenRotation = z;
            apply(this.mCurrentState);
            LockscreenStatisticsWirelessChargerReporter.sendEvent(this.mContext, this.mKeyguardScreenRotation);
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onThemeChanged() {
        if (this.mStatusBarView != null) {
            setKeyguardDark(this.mColorExtractor.getNeutralColors().supportsDarkText());
        }
    }

    /* access modifiers changed from: private */
    public static class State {
        boolean backdropShowing;
        boolean bouncerShowing;
        boolean bubbleExpanded;
        boolean bubblesShowing;
        boolean dozing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forcePluginOpen;
        boolean forceStatusBarVisible;
        boolean forceUserActivity;
        boolean headsUpShowing;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        boolean notTouchable;
        boolean panelExpanded;
        boolean panelVisible;
        boolean qsExpanded;
        boolean remoteInputActive;
        int scrimsVisibility;
        boolean statusBarFocusable;
        int statusBarState;
        boolean wallpaperSupportsAmbientMode;

        private State() {
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Window State {");
            sb.append("\n");
            Field[] declaredFields = State.class.getDeclaredFields();
            for (Field field : declaredFields) {
                sb.append("  ");
                try {
                    sb.append(field.getName());
                    sb.append(": ");
                    sb.append(field.get(this));
                } catch (IllegalAccessException unused) {
                }
                sb.append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
