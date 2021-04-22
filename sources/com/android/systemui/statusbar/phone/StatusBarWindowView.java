package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.DisplayCutout;
import android.view.GestureDetector;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.FloatingToolbar;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.R$styleable;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.tuner.TunerService;
import com.sonymobile.keyguard.SomcKeepScreenOnHelper;
import com.sonymobile.keyguard.SomcUserActivityPoker;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class StatusBarWindowView extends FrameLayout {
    private View mBrightnessMirror;
    private boolean mDoubleTapEnabled;
    private DragDownHelper mDragDownHelper;
    private boolean mExpandAnimationPending;
    private boolean mExpandAnimationRunning;
    private boolean mExpandingBelowNotch;
    private Window mFakeWindow = new Window(((FrameLayout) this).mContext) {
        /* class com.android.systemui.statusbar.phone.StatusBarWindowView.AnonymousClass4 */

        public void addContentView(View view, ViewGroup.LayoutParams layoutParams) {
        }

        public void alwaysReadCloseOnTouchAttr() {
        }

        public void clearContentView() {
        }

        public void closeAllPanels() {
        }

        public void closePanel(int i) {
        }

        public View getCurrentFocus() {
            return null;
        }

        public WindowInsetsController getInsetsController() {
            return null;
        }

        public LayoutInflater getLayoutInflater() {
            return null;
        }

        public int getNavigationBarColor() {
            return 0;
        }

        public int getStatusBarColor() {
            return 0;
        }

        public int getVolumeControlStream() {
            return 0;
        }

        public void invalidatePanelMenu(int i) {
        }

        public boolean isFloating() {
            return false;
        }

        public boolean isShortcutKey(int i, KeyEvent keyEvent) {
            return false;
        }

        /* access modifiers changed from: protected */
        public void onActive() {
        }

        public void onConfigurationChanged(Configuration configuration) {
        }

        public void onMultiWindowModeChanged() {
        }

        public void onPictureInPictureModeChanged(boolean z) {
        }

        public void openPanel(int i, KeyEvent keyEvent) {
        }

        public View peekDecorView() {
            return null;
        }

        public boolean performContextMenuIdentifierAction(int i, int i2) {
            return false;
        }

        public boolean performPanelIdentifierAction(int i, int i2, int i3) {
            return false;
        }

        public boolean performPanelShortcut(int i, int i2, KeyEvent keyEvent, int i3) {
            return false;
        }

        public void reportActivityRelaunched() {
        }

        public void restoreHierarchyState(Bundle bundle) {
        }

        public Bundle saveHierarchyState() {
            return null;
        }

        public void setBackgroundDrawable(Drawable drawable) {
        }

        public void setChildDrawable(int i, Drawable drawable) {
        }

        public void setChildInt(int i, int i2) {
        }

        @Override // android.view.Window
        public void setContentView(int i) {
        }

        @Override // android.view.Window
        public void setContentView(View view) {
        }

        public void setContentView(View view, ViewGroup.LayoutParams layoutParams) {
        }

        public void setDecorCaptionShade(int i) {
        }

        public void setFeatureDrawable(int i, Drawable drawable) {
        }

        public void setFeatureDrawableAlpha(int i, int i2) {
        }

        public void setFeatureDrawableResource(int i, int i2) {
        }

        public void setFeatureDrawableUri(int i, Uri uri) {
        }

        public void setFeatureInt(int i, int i2) {
        }

        public void setNavigationBarColor(int i) {
        }

        public void setResizingCaptionDrawable(Drawable drawable) {
        }

        public void setStatusBarColor(int i) {
        }

        public void setTitle(CharSequence charSequence) {
        }

        public void setTitleColor(int i) {
        }

        public void setVolumeControlStream(int i) {
        }

        public boolean superDispatchGenericMotionEvent(MotionEvent motionEvent) {
            return false;
        }

        public boolean superDispatchKeyEvent(KeyEvent keyEvent) {
            return false;
        }

        public boolean superDispatchKeyShortcutEvent(KeyEvent keyEvent) {
            return false;
        }

        public boolean superDispatchTouchEvent(MotionEvent motionEvent) {
            return false;
        }

        public boolean superDispatchTrackballEvent(MotionEvent motionEvent) {
            return false;
        }

        public void takeInputQueue(InputQueue.Callback callback) {
        }

        public void takeKeyEvents(boolean z) {
        }

        public void takeSurface(SurfaceHolder.Callback2 callback2) {
        }

        public void togglePanel(int i, KeyEvent keyEvent) {
        }

        public View getDecorView() {
            return StatusBarWindowView.this;
        }
    };
    private FalsingManager mFalsingManager;
    private ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    private final GestureDetector mGestureDetector;
    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        /* class com.android.systemui.statusbar.phone.StatusBarWindowView.AnonymousClass1 */

        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (!StatusBarWindowView.this.mSingleTapEnabled || StatusBarWindowView.this.mSuppressingWakeUpGesture) {
                return false;
            }
            StatusBarWindowView.this.mService.wakeUpIfDozing(SystemClock.uptimeMillis(), StatusBarWindowView.this, "SINGLE_TAP");
            return true;
        }

        public boolean onDoubleTap(MotionEvent motionEvent) {
            if (!StatusBarWindowView.this.mDoubleTapEnabled && !StatusBarWindowView.this.mSingleTapEnabled) {
                return false;
            }
            StatusBarWindowView.this.mService.wakeUpIfDozing(SystemClock.uptimeMillis(), StatusBarWindowView.this, "DOUBLE_TAP");
            return true;
        }
    };
    private int mLeftInset = 0;
    private LockIcon mLockIcon;
    private NotificationPanelView mNotificationPanel;
    private int mRightInset = 0;
    private StatusBar mService;
    private boolean mSingleTapEnabled;
    private final SomcKeepScreenOnHelper mSomcKeepScreenOnHelper;
    private NotificationStackScrollLayout mStackScrollLayout;
    private final StatusBarStateController mStatusBarStateController;
    private PhoneStatusBarView mStatusBarView;
    private boolean mSuppressingWakeUpGesture;
    private boolean mTouchActive;
    private boolean mTouchCancelled;
    private final Paint mTransparentSrcPaint = new Paint();
    private final TunerService.Tunable mTunable = new TunerService.Tunable() {
        /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarWindowView$pvQm7Ibp9l08TX9BBh0oKlSeE */

        @Override // com.android.systemui.tuner.TunerService.Tunable
        public final void onTuningChanged(String str, String str2) {
            StatusBarWindowView.this.lambda$new$0$StatusBarWindowView(str, str2);
        }
    };

    /* JADX WARNING: Removed duplicated region for block: B:13:0x002f  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0039  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public /* synthetic */ void lambda$new$0$StatusBarWindowView(java.lang.String r4, java.lang.String r5) {
        /*
            r3 = this;
            android.hardware.display.AmbientDisplayConfiguration r5 = new android.hardware.display.AmbientDisplayConfiguration
            android.content.Context r0 = r3.mContext
            r5.<init>(r0)
            int r0 = r4.hashCode()
            r1 = 417936100(0x18e932e4, float:6.0280475E-24)
            r2 = 1
            if (r0 == r1) goto L_0x0021
            r1 = 1073289638(0x3ff919a6, float:1.9460952)
            if (r0 == r1) goto L_0x0017
            goto L_0x002b
        L_0x0017:
            java.lang.String r0 = "doze_pulse_on_double_tap"
            boolean r4 = r4.equals(r0)
            if (r4 == 0) goto L_0x002b
            r4 = 0
            goto L_0x002c
        L_0x0021:
            java.lang.String r0 = "doze_tap_gesture"
            boolean r4 = r4.equals(r0)
            if (r4 == 0) goto L_0x002b
            r4 = r2
            goto L_0x002c
        L_0x002b:
            r4 = -1
        L_0x002c:
            r0 = -2
            if (r4 == 0) goto L_0x0039
            if (r4 == r2) goto L_0x0032
            goto L_0x003f
        L_0x0032:
            boolean r4 = r5.tapGestureEnabled(r0)
            r3.mSingleTapEnabled = r4
            goto L_0x003f
        L_0x0039:
            boolean r4 = r5.doubleTapGestureEnabled(r0)
            r3.mDoubleTapEnabled = r4
        L_0x003f:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBarWindowView.lambda$new$0$StatusBarWindowView(java.lang.String, java.lang.String):void");
    }

    public StatusBarWindowView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setMotionEventSplittingEnabled(false);
        this.mTransparentSrcPaint.setColor(0);
        this.mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mGestureDetector = new GestureDetector(context, this.mGestureListener);
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this.mTunable, "doze_pulse_on_double_tap", "doze_tap_gesture");
        this.mSomcKeepScreenOnHelper = new SomcKeepScreenOnHelper(new SomcUserActivityPoker() {
            /* class com.android.systemui.statusbar.phone.StatusBarWindowView.AnonymousClass2 */

            @Override // com.sonymobile.keyguard.SomcUserActivityPoker
            public void poke() {
                if (StatusBarWindowView.this.mService != null) {
                    StatusBarWindowView.this.mService.userActivity();
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    public boolean fitSystemWindows(Rect rect) {
        int i;
        int i2;
        boolean z = true;
        if (getFitsSystemWindows()) {
            if (rect.top == getPaddingTop() && rect.bottom == getPaddingBottom()) {
                z = false;
            }
            DisplayCutout displayCutout = getRootWindowInsets().getDisplayCutout();
            if (displayCutout != null) {
                i = displayCutout.getSafeInsetLeft();
                i2 = displayCutout.getSafeInsetRight();
            } else {
                i2 = 0;
                i = 0;
            }
            int max = Math.max(rect.left, i);
            int max2 = Math.max(rect.right, i2);
            if (!(max2 == this.mRightInset && max == this.mLeftInset)) {
                this.mRightInset = max2;
                this.mLeftInset = max;
                applyMargins();
            }
            if (z) {
                setPadding(0, 0, 0, 0);
            }
            rect.left = 0;
            rect.top = 0;
            rect.right = 0;
        } else {
            if (!(this.mRightInset == 0 && this.mLeftInset == 0)) {
                this.mRightInset = 0;
                this.mLeftInset = 0;
                applyMargins();
            }
            if (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0 && getPaddingBottom() == 0) {
                z = false;
            }
            if (z) {
                setPadding(0, 0, 0, 0);
            }
            rect.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getLayoutParams() instanceof LayoutParams) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (!layoutParams.ignoreRightInset && !(((FrameLayout.LayoutParams) layoutParams).rightMargin == this.mRightInset && ((FrameLayout.LayoutParams) layoutParams).leftMargin == this.mLeftInset)) {
                    ((FrameLayout.LayoutParams) layoutParams).rightMargin = this.mRightInset;
                    ((FrameLayout.LayoutParams) layoutParams).leftMargin = this.mLeftInset;
                    childAt.requestLayout();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public NotificationStackScrollLayout getStackScrollLayout() {
        return this.mStackScrollLayout;
    }

    @Override // android.widget.FrameLayout, android.widget.FrameLayout, android.view.ViewGroup
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    /* access modifiers changed from: protected */
    @Override // android.widget.FrameLayout, android.widget.FrameLayout
    public FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mStackScrollLayout = (NotificationStackScrollLayout) findViewById(C0007R$id.notification_stack_scroller);
        this.mNotificationPanel = (NotificationPanelView) findViewById(C0007R$id.notification_panel);
        this.mBrightnessMirror = findViewById(C0007R$id.brightness_mirror);
        this.mLockIcon = (LockIcon) findViewById(C0007R$id.lock_icon);
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        if (view.getId() == C0007R$id.brightness_mirror) {
            this.mBrightnessMirror = view;
        }
    }

    public void setPulsing(boolean z) {
        LockIcon lockIcon = this.mLockIcon;
        if (lockIcon != null) {
            lockIcon.setPulsing(z);
        }
    }

    public void onBiometricAuthModeChanged(boolean z) {
        LockIcon lockIcon = this.mLockIcon;
        if (lockIcon != null) {
            lockIcon.onBiometricAuthModeChanged(z);
        }
    }

    public void setStatusBarView(PhoneStatusBarView phoneStatusBarView) {
        this.mStatusBarView = phoneStatusBarView;
    }

    public void setService(StatusBar statusBar) {
        this.mService = statusBar;
        NotificationStackScrollLayout stackScrollLayout = getStackScrollLayout();
        setDragDownHelper(new DragDownHelper(getContext(), this, stackScrollLayout.getExpandHelperCallback(), stackScrollLayout.getDragDownCallback()));
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setDragDownHelper(DragDownHelper dragDownHelper) {
        this.mDragDownHelper = dragDownHelper;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setWillNotDraw(true);
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mService.interceptMediaKey(keyEvent) || super.dispatchKeyEvent(keyEvent)) {
            return true;
        }
        boolean z = keyEvent.getAction() == 0;
        int keyCode = keyEvent.getKeyCode();
        if (keyCode != 4) {
            if (keyCode != 62) {
                if (keyCode != 82) {
                    if ((keyCode == 24 || keyCode == 25) && this.mService.isDozing()) {
                        MediaSessionLegacyHelper.getHelper(((FrameLayout) this).mContext).sendVolumeKeyEvent(keyEvent, Integer.MIN_VALUE, true);
                        return true;
                    }
                    return false;
                } else if (!z) {
                    return this.mService.onMenuPressed();
                }
            }
            if (!z) {
                return this.mService.onSpacePressed();
            }
            return false;
        }
        if (!z) {
            this.mService.onBackPressed();
        }
        return true;
    }

    public void setTouchActive(boolean z) {
        this.mTouchActive = z;
    }

    /* access modifiers changed from: package-private */
    public void suppressWakeUpGesture(boolean z) {
        this.mSuppressingWakeUpGesture = z;
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        boolean z = motionEvent.getActionMasked() == 0;
        boolean z2 = motionEvent.getActionMasked() == 1;
        boolean z3 = motionEvent.getActionMasked() == 3;
        boolean z4 = this.mExpandingBelowNotch;
        if (z2 || z3) {
            this.mExpandingBelowNotch = false;
        }
        if (!z3 && this.mService.shouldIgnoreTouch()) {
            return false;
        }
        if (z && this.mNotificationPanel.isFullyCollapsed()) {
            this.mNotificationPanel.startExpandLatencyTracking();
        }
        if (z) {
            setTouchActive(true);
            this.mTouchCancelled = false;
        } else if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
            setTouchActive(false);
        }
        if (this.mTouchCancelled || this.mExpandAnimationRunning || this.mExpandAnimationPending) {
            return false;
        }
        this.mFalsingManager.onTouchEvent(motionEvent, getWidth(), getHeight());
        this.mGestureDetector.onTouchEvent(motionEvent);
        View view = this.mBrightnessMirror;
        if (view != null && view.getVisibility() == 0 && motionEvent.getActionMasked() == 5) {
            return false;
        }
        if (z) {
            getStackScrollLayout().closeControlsIfOutsideTouch(motionEvent);
        }
        if (this.mService.isDozing()) {
            this.mService.mDozeScrimController.extendPulse();
        }
        if (z && motionEvent.getY() >= ((float) ((FrameLayout) this).mBottom)) {
            this.mExpandingBelowNotch = true;
            z4 = true;
        }
        if (z4) {
            return this.mStatusBarView.dispatchTouchEvent(motionEvent);
        }
        if (this.mStatusBarStateController.getState() == 1) {
            this.mSomcKeepScreenOnHelper.checkIfMotionEventShouldKeepScreenOn(motionEvent);
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        NotificationStackScrollLayout stackScrollLayout = getStackScrollLayout();
        boolean z = false;
        if (this.mService.isDozing() && !this.mService.isPulsing()) {
            return false;
        }
        if (this.mNotificationPanel.isFullyExpanded() && stackScrollLayout.getVisibility() == 0 && this.mStatusBarStateController.getState() == 1 && !this.mService.isBouncerShowing() && !this.mService.isDozing()) {
            z = this.mDragDownHelper.onInterceptTouchEvent(motionEvent);
        }
        if (!z) {
            super.onInterceptTouchEvent(motionEvent);
        }
        if (z) {
            MotionEvent obtain = MotionEvent.obtain(motionEvent);
            obtain.setAction(3);
            stackScrollLayout.onInterceptTouchEvent(obtain);
            this.mNotificationPanel.onInterceptTouchEvent(obtain);
            obtain.recycle();
        }
        return z;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z = this.mService.isDozing() ? !this.mService.isPulsing() : false;
        if ((this.mStatusBarStateController.getState() == 1 && !z) || this.mDragDownHelper.isDraggingDown()) {
            z = this.mDragDownHelper.onTouchEvent(motionEvent);
        }
        if (!z) {
            z = super.onTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (!z && (action == 1 || action == 3)) {
            this.mService.setInteracting(1, false);
        }
        return z;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void cancelExpandHelper() {
        NotificationStackScrollLayout stackScrollLayout = getStackScrollLayout();
        if (stackScrollLayout != null) {
            stackScrollLayout.cancelExpandHelper();
        }
    }

    public void cancelCurrentTouch() {
        if (this.mTouchActive) {
            long uptimeMillis = SystemClock.uptimeMillis();
            MotionEvent obtain = MotionEvent.obtain(uptimeMillis, uptimeMillis, 3, 0.0f, 0.0f, 0);
            obtain.setSource(4098);
            dispatchTouchEvent(obtain);
            obtain.recycle();
            this.mTouchCancelled = true;
        }
    }

    public void setExpandAnimationRunning(boolean z) {
        this.mExpandAnimationRunning = z;
    }

    public void setExpandAnimationPending(boolean z) {
        this.mExpandAnimationPending = z;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mExpandAnimationPending=");
        printWriter.println(this.mExpandAnimationPending);
        printWriter.print("  mExpandAnimationRunning=");
        printWriter.println(this.mExpandAnimationRunning);
        printWriter.print("  mTouchCancelled=");
        printWriter.println(this.mTouchCancelled);
        printWriter.print("  mTouchActive=");
        printWriter.println(this.mTouchActive);
    }

    public void onScrimVisibilityChanged(int i) {
        LockIcon lockIcon = this.mLockIcon;
        if (lockIcon != null) {
            lockIcon.onScrimVisibilityChanged(i);
        }
    }

    public void onShowingLaunchAffordanceChanged(boolean z) {
        LockIcon lockIcon = this.mLockIcon;
        if (lockIcon != null) {
            lockIcon.onShowingLaunchAffordanceChanged(z);
        }
    }

    public class LayoutParams extends FrameLayout.LayoutParams {
        public boolean ignoreRightInset;

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.StatusBarWindowView_Layout);
            this.ignoreRightInset = obtainStyledAttributes.getBoolean(R$styleable.StatusBarWindowView_Layout_ignoreRightInset, false);
            obtainStyledAttributes.recycle();
        }
    }

    public ActionMode startActionModeForChild(View view, ActionMode.Callback callback, int i) {
        if (i == 1) {
            return startActionMode(view, callback, i);
        }
        return super.startActionModeForChild(view, callback, i);
    }

    private ActionMode createFloatingActionMode(View view, ActionMode.Callback2 callback2) {
        ActionMode actionMode = this.mFloatingActionMode;
        if (actionMode != null) {
            actionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = new FloatingToolbar(this.mFakeWindow);
        final FloatingActionMode floatingActionMode = new FloatingActionMode(((FrameLayout) this).mContext, callback2, view, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = view;
        this.mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            /* class com.android.systemui.statusbar.phone.StatusBarWindowView.AnonymousClass3 */

            public boolean onPreDraw() {
                floatingActionMode.updateViewLocationInWindow();
                return true;
            }
        };
        return floatingActionMode;
    }

    private void setHandledFloatingActionMode(ActionMode actionMode) {
        this.mFloatingActionMode = actionMode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void cleanupFloatingActionModeViews() {
        FloatingToolbar floatingToolbar = this.mFloatingToolbar;
        if (floatingToolbar != null) {
            floatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        View view = this.mFloatingActionModeOriginatingView;
        if (view != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                view.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    private ActionMode startActionMode(View view, ActionMode.Callback callback, int i) {
        ActionModeCallback2Wrapper actionModeCallback2Wrapper = new ActionModeCallback2Wrapper(callback);
        ActionMode createFloatingActionMode = createFloatingActionMode(view, actionModeCallback2Wrapper);
        if (createFloatingActionMode == null || !actionModeCallback2Wrapper.onCreateActionMode(createFloatingActionMode, createFloatingActionMode.getMenu())) {
            return null;
        }
        setHandledFloatingActionMode(createFloatingActionMode);
        return createFloatingActionMode;
    }

    /* access modifiers changed from: private */
    public class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback callback) {
            this.mWrapped = callback;
        }

        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            return this.mWrapped.onCreateActionMode(actionMode, menu);
        }

        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            StatusBarWindowView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(actionMode, menu);
        }

        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            return this.mWrapped.onActionItemClicked(actionMode, menuItem);
        }

        public void onDestroyActionMode(ActionMode actionMode) {
            this.mWrapped.onDestroyActionMode(actionMode);
            if (actionMode == StatusBarWindowView.this.mFloatingActionMode) {
                StatusBarWindowView.this.cleanupFloatingActionModeViews();
                StatusBarWindowView.this.mFloatingActionMode = null;
            }
            StatusBarWindowView.this.requestFitSystemWindows();
        }

        public void onGetContentRect(ActionMode actionMode, View view, Rect rect) {
            ActionMode.Callback callback = this.mWrapped;
            if (callback instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) callback).onGetContentRect(actionMode, view, rect);
            } else {
                super.onGetContentRect(actionMode, view, rect);
            }
        }
    }
}
