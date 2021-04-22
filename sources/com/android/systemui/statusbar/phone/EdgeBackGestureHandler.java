package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.MathUtils;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.ISystemGestureExclusionListener;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.phone.EdgeBackGestureHandler;
import com.android.systemui.statusbar.phone.RegionSamplingHelper;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

public class EdgeBackGestureHandler implements DisplayManager.DisplayListener {
    private boolean mAllowGesture = false;
    private final Context mContext;
    private final int mDisplayId;
    private final Point mDisplaySize = new Point();
    private final PointF mDownPoint = new PointF();
    private NavigationBarEdgePanel mEdgePanel;
    private WindowManager.LayoutParams mEdgePanelLp;
    private int mEdgeWidth;
    private final Region mExcludeRegion = new Region();
    private final int mFingerOffset;
    private ISystemGestureExclusionListener mGestureExclusionListener = new ISystemGestureExclusionListener.Stub() {
        /* class com.android.systemui.statusbar.phone.EdgeBackGestureHandler.AnonymousClass2 */

        public void onSystemGestureExclusionChanged(int i, Region region) {
            if (i == EdgeBackGestureHandler.this.mDisplayId) {
                EdgeBackGestureHandler.this.mMainExecutor.execute(new Runnable(region) {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$EdgeBackGestureHandler$2$F4d7nuVBz_prJQFKFkF_tmjhVc */
                    private final /* synthetic */ Region f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        EdgeBackGestureHandler.AnonymousClass2.this.lambda$onSystemGestureExclusionChanged$0$EdgeBackGestureHandler$2(this.f$1);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$onSystemGestureExclusionChanged$0$EdgeBackGestureHandler$2(Region region) {
            EdgeBackGestureHandler.this.mExcludeRegion.set(region);
        }
    };
    private final IPinnedStackListener.Stub mImeChangedListener = new IPinnedStackListener.Stub() {
        /* class com.android.systemui.statusbar.phone.EdgeBackGestureHandler.AnonymousClass1 */

        public void onActionsChanged(ParceledListSlice parceledListSlice) {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) {
        }

        public void onMinimizedStateChanged(boolean z) {
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
        }

        public void onShelfVisibilityChanged(boolean z, int i) {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            EdgeBackGestureHandler edgeBackGestureHandler = EdgeBackGestureHandler.this;
            if (!z) {
                i = 0;
            }
            edgeBackGestureHandler.mImeHeight = i;
        }
    };
    private int mImeHeight = 0;
    private InputEventReceiver mInputEventReceiver;
    private InputMonitor mInputMonitor;
    private boolean mIsAttached;
    private boolean mIsEnabled;
    private boolean mIsGesturalModeEnabled;
    private boolean mIsOnLeftEdge;
    private int mLeftInset;
    private final int mLongPressTimeout;
    private final Executor mMainExecutor;
    private final int mMinArrowPosition;
    private final int mNavBarHeight;
    private final OverviewProxyService mOverviewProxyService;
    private RegionSamplingHelper mRegionSamplingHelper;
    private int mRightInset;
    private final Rect mSamplingRect = new Rect();
    private boolean mThresholdCrossed = false;
    private final float mTouchSlop;
    private final WindowManager mWm;

    public void onDisplayAdded(int i) {
    }

    public void onDisplayRemoved(int i) {
    }

    public EdgeBackGestureHandler(Context context, OverviewProxyService overviewProxyService) {
        Resources resources = context.getResources();
        this.mContext = context;
        this.mDisplayId = context.getDisplayId();
        this.mMainExecutor = context.getMainExecutor();
        this.mWm = (WindowManager) context.getSystemService(WindowManager.class);
        this.mOverviewProxyService = overviewProxyService;
        this.mTouchSlop = ((float) ViewConfiguration.get(context).getScaledTouchSlop()) * 0.75f;
        this.mLongPressTimeout = Math.min(250, ViewConfiguration.getLongPressTimeout());
        this.mNavBarHeight = resources.getDimensionPixelSize(C0005R$dimen.navigation_bar_frame_height);
        this.mMinArrowPosition = resources.getDimensionPixelSize(C0005R$dimen.navigation_edge_arrow_min_y);
        this.mFingerOffset = resources.getDimensionPixelSize(C0005R$dimen.navigation_edge_finger_offset);
        updateCurrentUserResources(resources);
    }

    public void updateCurrentUserResources(Resources resources) {
        this.mEdgeWidth = resources.getDimensionPixelSize(17105051);
    }

    public void onNavBarAttached() {
        this.mIsAttached = true;
        updateIsEnabled();
    }

    public void onNavBarDetached() {
        this.mIsAttached = false;
        updateIsEnabled();
    }

    public void onNavigationModeChanged(int i, Context context) {
        this.mIsGesturalModeEnabled = QuickStepContract.isGesturalMode(i);
        updateIsEnabled();
        updateCurrentUserResources(context.getResources());
    }

    private void disposeInputChannel() {
        InputEventReceiver inputEventReceiver = this.mInputEventReceiver;
        if (inputEventReceiver != null) {
            inputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        InputMonitor inputMonitor = this.mInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mInputMonitor = null;
        }
    }

    private void updateIsEnabled() {
        boolean z = this.mIsAttached && this.mIsGesturalModeEnabled;
        if (z != this.mIsEnabled) {
            this.mIsEnabled = z;
            disposeInputChannel();
            NavigationBarEdgePanel navigationBarEdgePanel = this.mEdgePanel;
            if (navigationBarEdgePanel != null) {
                this.mWm.removeView(navigationBarEdgePanel);
                this.mEdgePanel = null;
                this.mRegionSamplingHelper.stop();
                this.mRegionSamplingHelper = null;
            }
            if (!this.mIsEnabled) {
                WindowManagerWrapper.getInstance().removePinnedStackListener(this.mImeChangedListener);
                ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
                try {
                    WindowManagerGlobal.getWindowManagerService().unregisterSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                } catch (RemoteException e) {
                    Log.e("EdgeBackGestureHandler", "Failed to unregister window manager callbacks", e);
                }
            } else {
                updateDisplaySize();
                ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).registerDisplayListener(this, this.mContext.getMainThreadHandler());
                try {
                    WindowManagerWrapper.getInstance().addPinnedStackListener(this.mImeChangedListener);
                    WindowManagerGlobal.getWindowManagerService().registerSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                } catch (RemoteException e2) {
                    Log.e("EdgeBackGestureHandler", "Failed to register window manager callbacks", e2);
                }
                this.mInputMonitor = InputManager.getInstance().monitorGestureInput("edge-swipe", this.mDisplayId);
                this.mInputEventReceiver = new SysUiInputEventReceiver(this.mInputMonitor.getInputChannel(), Looper.getMainLooper());
                this.mEdgePanel = new NavigationBarEdgePanel(this.mContext);
                this.mEdgePanelLp = new WindowManager.LayoutParams(this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.navigation_edge_panel_width), this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.navigation_edge_panel_height), 2024, 8388904, -3);
                WindowManager.LayoutParams layoutParams = this.mEdgePanelLp;
                layoutParams.privateFlags |= 16;
                layoutParams.setTitle("EdgeBackGestureHandler" + this.mDisplayId);
                this.mEdgePanelLp.accessibilityTitle = this.mContext.getString(C0014R$string.nav_bar_edge_panel);
                WindowManager.LayoutParams layoutParams2 = this.mEdgePanelLp;
                layoutParams2.windowAnimations = 0;
                this.mEdgePanel.setLayoutParams(layoutParams2);
                this.mWm.addView(this.mEdgePanel, this.mEdgePanelLp);
                this.mRegionSamplingHelper = new RegionSamplingHelper(this.mEdgePanel, new RegionSamplingHelper.SamplingCallback() {
                    /* class com.android.systemui.statusbar.phone.EdgeBackGestureHandler.AnonymousClass3 */

                    @Override // com.android.systemui.statusbar.phone.RegionSamplingHelper.SamplingCallback
                    public void onRegionDarknessChanged(boolean z) {
                        EdgeBackGestureHandler.this.mEdgePanel.setIsDark(!z, true);
                    }

                    @Override // com.android.systemui.statusbar.phone.RegionSamplingHelper.SamplingCallback
                    public Rect getSampledRegion(View view) {
                        return EdgeBackGestureHandler.this.mSamplingRect;
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onInputEvent(InputEvent inputEvent) {
        if (inputEvent instanceof MotionEvent) {
            onMotionEvent((MotionEvent) inputEvent);
        }
    }

    private boolean isWithinTouchRegion(int i, int i2) {
        if (i2 > this.mDisplaySize.y - Math.max(this.mImeHeight, this.mNavBarHeight)) {
            return false;
        }
        int i3 = this.mEdgeWidth;
        if (i > this.mLeftInset + i3 && i < (this.mDisplaySize.x - i3) - this.mRightInset) {
            return false;
        }
        boolean contains = this.mExcludeRegion.contains(i, i2);
        if (contains) {
            this.mOverviewProxyService.notifyBackAction(false, -1, -1, false, !this.mIsOnLeftEdge);
        }
        return !contains;
    }

    private void cancelGesture(MotionEvent motionEvent) {
        this.mAllowGesture = false;
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        obtain.setAction(3);
        this.mEdgePanel.handleTouch(obtain);
        obtain.recycle();
    }

    private void onMotionEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        boolean z = true;
        if (actionMasked == 0) {
            int systemUiStateFlags = this.mOverviewProxyService.getSystemUiStateFlags();
            this.mIsOnLeftEdge = motionEvent.getX() <= ((float) (this.mEdgeWidth + this.mLeftInset));
            if (QuickStepContract.isBackGestureDisabled(systemUiStateFlags) || !isWithinTouchRegion((int) motionEvent.getX(), (int) motionEvent.getY())) {
                z = false;
            }
            this.mAllowGesture = z;
            if (this.mAllowGesture) {
                this.mEdgePanelLp.gravity = this.mIsOnLeftEdge ? 51 : 53;
                this.mEdgePanel.setIsLeftPanel(this.mIsOnLeftEdge);
                this.mEdgePanel.handleTouch(motionEvent);
                updateEdgePanelPosition(motionEvent.getY());
                this.mWm.updateViewLayout(this.mEdgePanel, this.mEdgePanelLp);
                this.mRegionSamplingHelper.start(this.mSamplingRect);
                this.mDownPoint.set(motionEvent.getX(), motionEvent.getY());
                this.mThresholdCrossed = false;
            }
        } else if (this.mAllowGesture) {
            if (!this.mThresholdCrossed) {
                if (actionMasked == 5) {
                    cancelGesture(motionEvent);
                    return;
                } else if (actionMasked == 2) {
                    if (motionEvent.getEventTime() - motionEvent.getDownTime() > ((long) this.mLongPressTimeout)) {
                        cancelGesture(motionEvent);
                        return;
                    }
                    float abs = Math.abs(motionEvent.getX() - this.mDownPoint.x);
                    float abs2 = Math.abs(motionEvent.getY() - this.mDownPoint.y);
                    if (abs2 > abs && abs2 > this.mTouchSlop) {
                        cancelGesture(motionEvent);
                        return;
                    } else if (abs > abs2 && abs > this.mTouchSlop) {
                        this.mThresholdCrossed = true;
                        this.mInputMonitor.pilferPointers();
                    }
                }
            }
            this.mEdgePanel.handleTouch(motionEvent);
            boolean z2 = actionMasked == 1;
            if (z2) {
                boolean shouldTriggerBack = this.mEdgePanel.shouldTriggerBack();
                if (shouldTriggerBack) {
                    sendEvent(0, 4);
                    sendEvent(1, 4);
                }
                OverviewProxyService overviewProxyService = this.mOverviewProxyService;
                PointF pointF = this.mDownPoint;
                overviewProxyService.notifyBackAction(shouldTriggerBack, (int) pointF.x, (int) pointF.y, false, !this.mIsOnLeftEdge);
            }
            if (z2 || actionMasked == 3) {
                this.mRegionSamplingHelper.stop();
                return;
            }
            updateSamplingRect();
            this.mRegionSamplingHelper.updateSamplingRect();
        }
    }

    private void updateEdgePanelPosition(float f) {
        float max = Math.max(f - ((float) this.mFingerOffset), (float) this.mMinArrowPosition) - (((float) this.mEdgePanelLp.height) / 2.0f);
        this.mEdgePanelLp.y = MathUtils.constrain((int) max, 0, this.mDisplaySize.y);
        updateSamplingRect();
    }

    private void updateSamplingRect() {
        WindowManager.LayoutParams layoutParams = this.mEdgePanelLp;
        int i = layoutParams.y;
        int i2 = this.mIsOnLeftEdge ? this.mLeftInset : (this.mDisplaySize.x - this.mRightInset) - layoutParams.width;
        this.mSamplingRect.set(i2, i, this.mEdgePanelLp.width + i2, this.mEdgePanelLp.height + i);
        this.mEdgePanel.adjustRectToBoundingBox(this.mSamplingRect);
    }

    public void onDisplayChanged(int i) {
        if (i == this.mDisplayId) {
            updateDisplaySize();
        }
    }

    private void updateDisplaySize() {
        ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId).getRealSize(this.mDisplaySize);
    }

    private void sendEvent(int i, int i2) {
        long uptimeMillis = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(uptimeMillis, uptimeMillis, i, i2, 0, 0, -1, 0, 72, 257);
        int expandedDisplayId = ((BubbleController) Dependency.get(BubbleController.class)).getExpandedDisplayId(this.mContext);
        if (i2 == 4 && expandedDisplayId != -1) {
            keyEvent.setDisplayId(expandedDisplayId);
        }
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
    }

    public void setInsets(int i, int i2) {
        this.mLeftInset = i;
        this.mRightInset = i2;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("EdgeBackGestureHandler:");
        printWriter.println("  mIsEnabled=" + this.mIsEnabled);
        printWriter.println("  mAllowGesture=" + this.mAllowGesture);
        printWriter.println("  mExcludeRegion=" + this.mExcludeRegion);
        printWriter.println("  mImeHeight=" + this.mImeHeight);
        printWriter.println("  mIsAttached=" + this.mIsAttached);
        printWriter.println("  mEdgeWidth=" + this.mEdgeWidth);
    }

    /* access modifiers changed from: package-private */
    public class SysUiInputEventReceiver extends InputEventReceiver {
        SysUiInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent inputEvent) {
            EdgeBackGestureHandler.this.onInputEvent(inputEvent);
            finishInputEvent(inputEvent, true);
        }
    }
}
