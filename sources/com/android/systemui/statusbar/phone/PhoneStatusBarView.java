package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.CommandQueue;
import java.util.Objects;

public class PhoneStatusBarView extends PanelBar {
    StatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions = new PhoneStatusBarTransitions(this);
    private DarkIconDispatcher.DarkReceiver mBattery;
    private View mCenterIconSpace;
    private final CommandQueue mCommandQueue;
    private int mCutoutSideNudge = 0;
    private View mCutoutSpace;
    private DisplayCutout mDisplayCutout;
    private Runnable mHideExpandedRunnable = new Runnable() {
        /* class com.android.systemui.statusbar.phone.PhoneStatusBarView.AnonymousClass1 */

        public void run() {
            PhoneStatusBarView phoneStatusBarView = PhoneStatusBarView.this;
            if (phoneStatusBarView.mPanelFraction == 0.0f) {
                phoneStatusBarView.mBar.makeExpandedInvisible();
            }
        }
    };
    boolean mIsFullyOpenedPanel = false;
    private int mLastOrientation;
    private float mMinFraction;
    private ScrimController mScrimController;
    private View mStatusBarContents;
    private final int mStatusBarPaddingX;
    private final int mStatusBarPaddingY;
    private int mStatusbarDefaultPaddingEnd;
    private int mStatusbarDefaultPaddingStart;

    public PhoneStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mStatusBarPaddingX = context.getResources().getDimensionPixelSize(C0005R$dimen.statusbar_burn_in_prevention_padding_x_max);
        this.mStatusBarPaddingY = context.getResources().getDimensionPixelSize(C0005R$dimen.statusbar_burn_in_prevention_padding_y_max);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(StatusBar statusBar) {
        this.mBar = statusBar;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onFinishInflate() {
        this.mBarTransitions.init();
        this.mBattery = (DarkIconDispatcher.DarkReceiver) findViewById(C0007R$id.battery);
        this.mCutoutSpace = findViewById(C0007R$id.cutout_space_view);
        this.mCenterIconSpace = findViewById(C0007R$id.centered_icon_area);
        this.mStatusBarContents = findViewById(C0007R$id.status_bar_contents);
        this.mStatusbarDefaultPaddingStart = this.mStatusBarContents.getPaddingStart();
        this.mStatusbarDefaultPaddingEnd = this.mStatusBarContents.getPaddingEnd();
        updateResources();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mBattery);
        if (updateOrientationAndCutout(getResources().getConfiguration().orientation)) {
            updateLayoutForCutout();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mBattery);
        this.mDisplayCutout = null;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (updateOrientationAndCutout(configuration.orientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (updateOrientationAndCutout(this.mLastOrientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    private boolean updateOrientationAndCutout(int i) {
        boolean z;
        if (i == Integer.MIN_VALUE || this.mLastOrientation == i) {
            z = false;
        } else {
            this.mLastOrientation = i;
            z = true;
        }
        if (Objects.equals(getRootWindowInsets().getDisplayCutout(), this.mDisplayCutout)) {
            return z;
        }
        this.mDisplayCutout = getRootWindowInsets().getDisplayCutout();
        return true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public boolean panelEnabled() {
        return this.mCommandQueue.panelsEnabled();
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (!super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            return false;
        }
        AccessibilityEvent obtain = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(obtain);
        dispatchPopulateAccessibilityEvent(obtain);
        accessibilityEvent.appendRecord(obtain);
        return true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelCollapsed() {
        super.onPanelCollapsed();
        post(this.mHideExpandedRunnable);
        this.mIsFullyOpenedPanel = false;
    }

    public void removePendingHideExpandedRunnables() {
        removeCallbacks(this.mHideExpandedRunnable);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onPanelFullyOpened() {
        super.onPanelFullyOpened();
        if (!this.mIsFullyOpenedPanel) {
            this.mPanel.sendAccessibilityEvent(32);
        }
        this.mIsFullyOpenedPanel = true;
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onTrackingStarted() {
        super.onTrackingStarted();
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
        removePendingHideExpandedRunnables();
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onTrackingStopped(boolean z) {
        super.onTrackingStopped(z);
        this.mBar.onTrackingStopped(z);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void panelScrimMinFractionChanged(float f) {
        if (this.mMinFraction != f) {
            this.mMinFraction = f;
            updateScrimFraction();
        }
    }

    @Override // com.android.systemui.statusbar.phone.PanelBar
    public void panelExpansionChanged(float f, boolean z) {
        super.panelExpansionChanged(f, z);
        updateScrimFraction();
        if ((f == 0.0f || f == 1.0f) && this.mBar.getNavigationBarView() != null) {
            this.mBar.getNavigationBarView().onPanelExpandedChange();
        }
    }

    private void updateScrimFraction() {
        float f = this.mPanelFraction;
        float f2 = this.mMinFraction;
        if (f2 < 1.0f) {
            f = Math.max((f - f2) / (1.0f - f2), 0.0f);
        }
        this.mScrimController.setPanelExpansion(f);
    }

    public void updateResources() {
        this.mCutoutSideNudge = getResources().getDimensionPixelSize(C0005R$dimen.display_cutout_margin_consumption);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(C0005R$dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    private void updateLayoutForCutout() {
        Pair<Integer, Integer> cornerCutoutMargins = cornerCutoutMargins(this.mDisplayCutout, getDisplay());
        updateCutoutLocation(cornerCutoutMargins);
        updateSafeInsets(cornerCutoutMargins);
    }

    private void updateCutoutLocation(Pair<Integer, Integer> pair) {
        if (this.mCutoutSpace != null) {
            DisplayCutout displayCutout = this.mDisplayCutout;
            if (displayCutout == null || displayCutout.isEmpty() || this.mLastOrientation != 1 || pair != null) {
                this.mCenterIconSpace.setVisibility(0);
                this.mCutoutSpace.setVisibility(8);
                return;
            }
            this.mCenterIconSpace.setVisibility(8);
            this.mCutoutSpace.setVisibility(0);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mCutoutSpace.getLayoutParams();
            Rect rect = new Rect();
            ScreenDecorations.DisplayCutoutView.boundsFromDirection(this.mDisplayCutout, 48, rect);
            int i = rect.left;
            int i2 = this.mCutoutSideNudge;
            rect.left = i + i2;
            rect.right -= i2;
            layoutParams.width = rect.width();
            layoutParams.height = rect.height();
        }
    }

    private void updateSafeInsets(Pair<Integer, Integer> pair) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        DisplayCutout displayCutout = this.mDisplayCutout;
        if (displayCutout == null || displayCutout.isEmpty() || this.mLastOrientation != 1 || pair == null) {
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            return;
        }
        layoutParams.leftMargin = Math.max(layoutParams.leftMargin, ((Integer) pair.first).intValue());
        layoutParams.rightMargin = Math.max(layoutParams.rightMargin, ((Integer) pair.second).intValue());
        WindowInsets rootWindowInsets = getRootWindowInsets();
        int systemWindowInsetLeft = rootWindowInsets.getSystemWindowInsetLeft();
        int systemWindowInsetRight = rootWindowInsets.getSystemWindowInsetRight();
        if (layoutParams.leftMargin <= systemWindowInsetLeft) {
            layoutParams.leftMargin = 0;
        }
        if (layoutParams.rightMargin <= systemWindowInsetRight) {
            layoutParams.rightMargin = 0;
        }
    }

    public static Pair<Integer, Integer> cornerCutoutMargins(DisplayCutout displayCutout, Display display) {
        if (displayCutout == null) {
            return null;
        }
        Point point = new Point();
        display.getRealSize(point);
        Rect rect = new Rect();
        ScreenDecorations.DisplayCutoutView.boundsFromDirection(displayCutout, 48, rect);
        if (rect.left <= 0) {
            return new Pair<>(Integer.valueOf(rect.right), 0);
        }
        if (rect.right >= point.x) {
            return new Pair<>(0, Integer.valueOf(point.x - rect.left));
        }
        return null;
    }

    public final void moveStatusBar() {
        if (this.mStatusBarContents != null) {
            int random = (int) (Math.random() * ((double) this.mStatusBarPaddingX));
            int random2 = (int) (Math.random() * ((double) this.mStatusBarPaddingY));
            if (System.currentTimeMillis() % 2 > 0) {
                this.mStatusBarContents.setPaddingRelative(this.mStatusbarDefaultPaddingStart + random, random2, this.mStatusbarDefaultPaddingEnd - random, -random2);
            } else {
                this.mStatusBarContents.setPaddingRelative(this.mStatusbarDefaultPaddingStart - random, -random2, this.mStatusbarDefaultPaddingEnd + random, random2);
            }
        }
    }
}
