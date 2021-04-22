package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.view.CompositionSamplingListener;
import android.view.View;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.shared.system.QuickStepContract;
import java.io.PrintWriter;

public class NavBarTintController implements View.OnAttachStateChangeListener, View.OnLayoutChangeListener {
    private float mCurrentMedianLuma;
    private final Handler mHandler = new Handler();
    private float mLastMedianLuma;
    private final LightBarTransitionsController mLightBarController;
    private final float mLuminanceChangeThreshold;
    private final float mLuminanceThreshold;
    private final int mNavBarHeight;
    private int mNavBarMode = 0;
    private final int mNavColorSampleMargin;
    private final NavigationBarView mNavigationBarView;
    private final Rect mSamplingBounds = new Rect();
    private boolean mSamplingEnabled = false;
    private final CompositionSamplingListener mSamplingListener;
    private boolean mSamplingListenerRegistered = false;
    private boolean mUpdateOnNextDraw;
    private final Runnable mUpdateSamplingListener = new Runnable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$NavBarTintController$2EUeUMrCltge35I4yojwTXFosWM */

        public final void run() {
            NavBarTintController.lambda$2EUeUMrCltge35I4yojwTXFosWM(NavBarTintController.this);
        }
    };
    private boolean mWindowVisible;

    public NavBarTintController(NavigationBarView navigationBarView, LightBarTransitionsController lightBarTransitionsController) {
        this.mSamplingListener = new CompositionSamplingListener(navigationBarView.getContext().getMainExecutor()) {
            /* class com.android.systemui.statusbar.phone.NavBarTintController.AnonymousClass1 */

            public void onSampleCollected(float f) {
                NavBarTintController.this.updateTint(f);
            }
        };
        this.mNavigationBarView = navigationBarView;
        this.mNavigationBarView.addOnAttachStateChangeListener(this);
        this.mNavigationBarView.addOnLayoutChangeListener(this);
        this.mLightBarController = lightBarTransitionsController;
        Resources resources = navigationBarView.getResources();
        this.mNavBarHeight = resources.getDimensionPixelSize(C0005R$dimen.navigation_bar_height);
        this.mNavColorSampleMargin = resources.getDimensionPixelSize(C0005R$dimen.navigation_handle_sample_horizontal_margin);
        this.mLuminanceThreshold = resources.getFloat(C0005R$dimen.navigation_luminance_threshold);
        this.mLuminanceChangeThreshold = resources.getFloat(C0005R$dimen.navigation_luminance_change_threshold);
    }

    /* access modifiers changed from: package-private */
    public void onDraw() {
        if (this.mUpdateOnNextDraw) {
            this.mUpdateOnNextDraw = false;
            requestUpdateSamplingListener();
        }
    }

    /* access modifiers changed from: package-private */
    public void start() {
        if (isEnabled(this.mNavigationBarView.getContext(), this.mNavBarMode)) {
            this.mSamplingEnabled = true;
            requestUpdateSamplingListener();
        }
    }

    /* access modifiers changed from: package-private */
    public void stop() {
        this.mSamplingEnabled = false;
        requestUpdateSamplingListener();
    }

    public void onViewAttachedToWindow(View view) {
        requestUpdateSamplingListener();
    }

    public void onViewDetachedFromWindow(View view) {
        requestUpdateSamplingListener();
    }

    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        this.mSamplingBounds.setEmpty();
        View currentView = this.mNavigationBarView.getHomeHandle().getCurrentView();
        if (currentView != null) {
            int[] iArr = new int[2];
            currentView.getLocationOnScreen(iArr);
            Point point = new Point();
            currentView.getContext().getDisplay().getRealSize(point);
            Rect rect = new Rect(iArr[0] - this.mNavColorSampleMargin, point.y - this.mNavBarHeight, iArr[0] + currentView.getWidth() + this.mNavColorSampleMargin, point.y);
            if (!rect.equals(this.mSamplingBounds)) {
                this.mSamplingBounds.set(rect);
                requestUpdateSamplingListener();
            }
        }
    }

    private void requestUpdateSamplingListener() {
        this.mHandler.removeCallbacks(this.mUpdateSamplingListener);
        this.mHandler.post(this.mUpdateSamplingListener);
    }

    /* access modifiers changed from: private */
    public void updateSamplingListener() {
        if (this.mSamplingListenerRegistered) {
            this.mSamplingListenerRegistered = false;
            CompositionSamplingListener.unregister(this.mSamplingListener);
        }
        if (this.mSamplingEnabled && this.mWindowVisible && !this.mSamplingBounds.isEmpty() && this.mNavigationBarView.isAttachedToWindow()) {
            if (!this.mNavigationBarView.getViewRootImpl().getSurfaceControl().isValid()) {
                this.mUpdateOnNextDraw = true;
                return;
            }
            this.mSamplingListenerRegistered = true;
            CompositionSamplingListener.register(this.mSamplingListener, 0, this.mNavigationBarView.getViewRootImpl().getSurfaceControl().getHandle(), this.mSamplingBounds);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTint(float f) {
        this.mLastMedianLuma = f;
        if (Math.abs(this.mCurrentMedianLuma - this.mLastMedianLuma) > this.mLuminanceChangeThreshold) {
            if (f > this.mLuminanceThreshold) {
                this.mLightBarController.setIconsDark(true, true);
            } else {
                this.mLightBarController.setIconsDark(false, true);
            }
            this.mCurrentMedianLuma = f;
        }
    }

    public void setWindowVisible(boolean z) {
        this.mWindowVisible = z;
        requestUpdateSamplingListener();
    }

    public void onNavigationModeChanged(int i) {
        this.mNavBarMode = i;
    }

    /* access modifiers changed from: package-private */
    public void dump(PrintWriter printWriter) {
        printWriter.println("NavBarTintController:");
        printWriter.println("  navBar isAttached: " + this.mNavigationBarView.isAttachedToWindow());
        StringBuilder sb = new StringBuilder();
        sb.append("  navBar isScValid: ");
        sb.append(this.mNavigationBarView.isAttachedToWindow() ? Boolean.valueOf(this.mNavigationBarView.getViewRootImpl().getSurfaceControl().isValid()) : "false");
        printWriter.println(sb.toString());
        printWriter.println("  mSamplingListenerRegistered: " + this.mSamplingListenerRegistered);
        printWriter.println("  mSamplingBounds: " + this.mSamplingBounds);
        printWriter.println("  mLastMedianLuma: " + this.mLastMedianLuma);
        printWriter.println("  mCurrentMedianLuma: " + this.mCurrentMedianLuma);
        printWriter.println("  mWindowVisible: " + this.mWindowVisible);
    }

    public static boolean isEnabled(Context context, int i) {
        return context.getDisplayId() == 0 && QuickStepContract.isGesturalMode(i);
    }
}
