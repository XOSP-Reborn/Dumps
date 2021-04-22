package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.ScrimDrawable;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ShowEmptyViewEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.DockState;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.ActivityOptionsCompat;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RecentsView extends FrameLayout {
    private boolean mAwaitingFirstLayout;
    private ScrimDrawable mBackgroundScrim;
    private ValueAnimator mBackgroundScrimAnimator;
    private float mBusynessFactor;
    private int mDividerSize;
    private TextView mEmptyView;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private Handler mHandler;
    private ColorDrawable mMultiWindowBackgroundScrim;
    private TextView mStackActionButton;
    private final int mStackButtonShadowColor;
    private final PointF mStackButtonShadowDistance;
    private final float mStackButtonShadowRadius;
    @ViewDebug.ExportedProperty(category = "recents")
    Rect mSystemInsets;
    private TaskStackView mTaskStackView;
    private Point mTmpDisplaySize;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private RecentsViewTouchHandler mTouchHandler;
    private RecentsTransitionComposer mTransitionHelper;
    private final ValueAnimator.AnimatorUpdateListener mUpdateBackgroundScrimAlpha;

    public /* synthetic */ void lambda$new$0$RecentsView(ValueAnimator valueAnimator) {
        int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
        this.mBackgroundScrim.setAlpha(intValue);
        this.mMultiWindowBackgroundScrim.setAlpha(intValue);
    }

    public RecentsView(Context context) {
        this(context, null);
    }

    public RecentsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public RecentsView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RecentsView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mAwaitingFirstLayout = true;
        this.mSystemInsets = new Rect();
        this.mTmpDisplaySize = new Point();
        this.mUpdateBackgroundScrimAlpha = new ValueAnimator.AnimatorUpdateListener() {
            /* class com.android.systemui.recents.views.$$Lambda$RecentsView$6rfoH9yP_J2fW6JDlOW4RINdzy4 */

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                RecentsView.this.lambda$new$0$RecentsView(valueAnimator);
            }
        };
        setWillNotDraw(false);
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        this.mHandler = new Handler();
        this.mTransitionHelper = new RecentsTransitionComposer(getContext());
        this.mDividerSize = systemServices.getDockedDividerSize(context);
        this.mTouchHandler = new RecentsViewTouchHandler(this);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
        this.mBackgroundScrim = new ScrimDrawable();
        this.mMultiWindowBackgroundScrim = new ColorDrawable();
        LayoutInflater from = LayoutInflater.from(context);
        this.mEmptyView = (TextView) from.inflate(2131558737, (ViewGroup) this, false);
        addView(this.mEmptyView);
        TextView textView = this.mStackActionButton;
        if (textView != null) {
            removeView(textView);
        }
        this.mStackActionButton = (TextView) from.inflate(LegacyRecentsImpl.getConfiguration().isLowRamDevice ? 2131558740 : 2131558743, (ViewGroup) this, false);
        this.mStackButtonShadowRadius = this.mStackActionButton.getShadowRadius();
        this.mStackButtonShadowDistance = new PointF(this.mStackActionButton.getShadowDx(), this.mStackActionButton.getShadowDy());
        this.mStackButtonShadowColor = this.mStackActionButton.getShadowColor();
        addView(this.mStackActionButton);
        reevaluateStyles();
    }

    public void reevaluateStyles() {
        int colorAttrDefaultColor = Utils.getColorAttrDefaultColor(((FrameLayout) this).mContext, 2130969596);
        int i = 0;
        boolean z = Color.luminance(colorAttrDefaultColor) < 0.5f;
        this.mEmptyView.setTextColor(colorAttrDefaultColor);
        this.mEmptyView.setCompoundDrawableTintList(new ColorStateList(new int[][]{new int[]{16842910}}, new int[]{colorAttrDefaultColor}));
        TextView textView = this.mStackActionButton;
        if (textView != null) {
            textView.setTextColor(colorAttrDefaultColor);
            if (z) {
                this.mStackActionButton.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            } else {
                TextView textView2 = this.mStackActionButton;
                float f = this.mStackButtonShadowRadius;
                PointF pointF = this.mStackButtonShadowDistance;
                textView2.setShadowLayer(f, pointF.x, pointF.y, this.mStackButtonShadowColor);
            }
        }
        if (z) {
            i = 8208;
        }
        setSystemUiVisibility(i | 1792);
    }

    public void onReload(TaskStack taskStack, boolean z) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        boolean z2 = taskStack.getTaskCount() == 0;
        if (this.mTaskStackView == null) {
            this.mTaskStackView = new TaskStackView(getContext());
            this.mTaskStackView.setSystemInsets(this.mSystemInsets);
            addView(this.mTaskStackView);
            z = false;
        }
        this.mAwaitingFirstLayout = !z;
        this.mTaskStackView.onReload(z);
        updateStack(taskStack, true);
        updateBusyness();
        if (z) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 200);
            return;
        }
        if (launchState.launchedViaDockGesture || launchState.launchedFromApp || z2) {
            this.mBackgroundScrim.setAlpha((int) (getOpaqueScrimAlpha() * 255.0f));
        } else {
            this.mBackgroundScrim.setAlpha(0);
        }
        this.mMultiWindowBackgroundScrim.setAlpha(this.mBackgroundScrim.getAlpha());
    }

    public void updateStack(TaskStack taskStack, boolean z) {
        if (z) {
            this.mTaskStackView.setTasks(taskStack, true);
        }
        if (taskStack.getTaskCount() > 0) {
            hideEmptyView();
        } else {
            showEmptyView(2131821943);
        }
    }

    public void updateScrimOpacity() {
        if (updateBusyness()) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 200);
        }
    }

    private boolean updateBusyness() {
        float min = ((float) Math.min(this.mTaskStackView.getStack().getTaskCount(), 3)) / 3.0f;
        if (this.mBusynessFactor == min) {
            return false;
        }
        this.mBusynessFactor = min;
        return true;
    }

    public TaskStack getStack() {
        return this.mTaskStackView.getStack();
    }

    public void updateBackgroundScrim(Window window, boolean z) {
        if (z) {
            this.mBackgroundScrim.setCallback((Drawable.Callback) null);
            window.setBackgroundDrawable(this.mMultiWindowBackgroundScrim);
            return;
        }
        this.mMultiWindowBackgroundScrim.setCallback(null);
        window.setBackgroundDrawable(this.mBackgroundScrim);
    }

    public boolean launchFocusedTask(int i) {
        Task focusedTask;
        TaskStackView taskStackView = this.mTaskStackView;
        if (taskStackView == null || (focusedTask = taskStackView.getFocusedTask()) == null) {
            return false;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this.mTaskStackView.getChildViewForTask(focusedTask), focusedTask, null, false));
        if (i == 0) {
            return true;
        }
        MetricsLogger.action(getContext(), i, focusedTask.key.getComponent().toString());
        return true;
    }

    public boolean launchPreviousTask() {
        Task launchTarget;
        if (LegacyRecentsImpl.getConfiguration().getLaunchState().launchedFromPipApp) {
            EventBus.getDefault().send(new ExpandPipEvent());
            return true;
        } else if (this.mTaskStackView == null || (launchTarget = getStack().getLaunchTarget()) == null) {
            return false;
        } else {
            EventBus.getDefault().send(new LaunchTaskEvent(this.mTaskStackView.getChildViewForTask(launchTarget), launchTarget, null, false));
            return true;
        }
    }

    public void showEmptyView(int i) {
        this.mTaskStackView.setVisibility(4);
        this.mEmptyView.setText(i);
        this.mEmptyView.setVisibility(0);
        this.mEmptyView.bringToFront();
        this.mStackActionButton.bringToFront();
    }

    public void hideEmptyView() {
        this.mEmptyView.setVisibility(4);
        this.mTaskStackView.setVisibility(0);
        this.mTaskStackView.bringToFront();
        this.mStackActionButton.bringToFront();
    }

    public void setScrimColors(ColorExtractor.GradientColors gradientColors, boolean z) {
        this.mBackgroundScrim.setColor(gradientColors.getMainColor(), z);
        int alpha = this.mMultiWindowBackgroundScrim.getAlpha();
        this.mMultiWindowBackgroundScrim.setColor(gradientColors.getMainColor());
        this.mMultiWindowBackgroundScrim.setAlpha(alpha);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        EventBus.getDefault().register(this.mTouchHandler, 4);
        super.onAttachedToWindow();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(this.mTouchHandler);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.measure(i, i2);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            measureChild(this.mEmptyView, View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE));
        }
        Rect stackActionButtonRect = this.mTaskStackView.mLayoutAlgorithm.getStackActionButtonRect();
        measureChild(this.mStackActionButton, View.MeasureSpec.makeMeasureSpec(stackActionButtonRect.width(), Integer.MIN_VALUE), View.MeasureSpec.makeMeasureSpec(stackActionButtonRect.height(), Integer.MIN_VALUE));
        setMeasuredDimension(size, size2);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.layout(i, i2, getMeasuredWidth() + i, getMeasuredHeight() + i2);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            Rect rect = this.mSystemInsets;
            int i5 = rect.left + rect.right;
            int i6 = rect.top + rect.bottom;
            int measuredWidth = this.mEmptyView.getMeasuredWidth();
            int measuredHeight = this.mEmptyView.getMeasuredHeight();
            int max = this.mSystemInsets.left + i + (Math.max(0, ((i3 - i) - i5) - measuredWidth) / 2);
            int max2 = this.mSystemInsets.top + i2 + (Math.max(0, ((i4 - i2) - i6) - measuredHeight) / 2);
            this.mEmptyView.layout(max, max2, measuredWidth + max, measuredHeight + max2);
        }
        ((FrameLayout) this).mContext.getDisplay().getRealSize(this.mTmpDisplaySize);
        this.mBackgroundScrim.setBounds(i, i2, i3, i4);
        ColorDrawable colorDrawable = this.mMultiWindowBackgroundScrim;
        Point point = this.mTmpDisplaySize;
        colorDrawable.setBounds(0, 0, point.x, point.y);
        Rect stackActionButtonBoundsFromStackLayout = getStackActionButtonBoundsFromStackLayout();
        this.mStackActionButton.layout(stackActionButtonBoundsFromStackLayout.left, stackActionButtonBoundsFromStackLayout.top, stackActionButtonBoundsFromStackLayout.right, stackActionButtonBoundsFromStackLayout.bottom);
        if (this.mAwaitingFirstLayout) {
            this.mAwaitingFirstLayout = false;
            if (LegacyRecentsImpl.getConfiguration().getLaunchState().launchedViaDragGesture) {
                setTranslationY((float) getMeasuredHeight());
            } else {
                setTranslationY(0.0f);
            }
            if (LegacyRecentsImpl.getConfiguration().isLowRamDevice && this.mEmptyView.getVisibility() == 0) {
                animateEmptyView(true, null);
            }
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mSystemInsets.set(windowInsets.getSystemWindowInsetsAsRect());
        this.mTaskStackView.setSystemInsets(this.mSystemInsets);
        requestLayout();
        return windowInsets;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onInterceptTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onTouchEvent(motionEvent);
    }

    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            visibleDockStates.get(size).viewState.draw(canvas);
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable drawable) {
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            if (visibleDockStates.get(size).viewState.dockAreaOverlay == drawable) {
                return true;
            }
        }
        return super.verifyDrawable(drawable);
    }

    public final void onBusEvent(LaunchTaskEvent launchTaskEvent) {
        launchTaskFromRecents(getStack(), launchTaskEvent.task, this.mTaskStackView, launchTaskEvent.taskView, launchTaskEvent.screenPinningRequested, launchTaskEvent.targetWindowingMode, launchTaskEvent.targetActivityType);
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new HideStackActionButtonEvent(false));
        }
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted dismissRecentsToHomeAnimationStarted) {
        EventBus.getDefault().send(new HideStackActionButtonEvent());
        animateBackgroundScrim(0.0f, 200);
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            animateEmptyView(false, dismissRecentsToHomeAnimationStarted.getAnimationTrigger());
        }
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        DockState[] dockStatesForCurrentOrientation = LegacyRecentsImpl.getConfiguration().getDockStatesForCurrentOrientation();
        DockState.ViewState viewState = DockState.NONE.viewState;
        updateVisibleDockRegions(dockStatesForCurrentOrientation, true, viewState.dockAreaAlpha, viewState.hintTextAlpha, true, false);
        TextView textView = this.mStackActionButton;
        if (textView != null) {
            textView.animate().alpha(0.0f).setDuration(100).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DragDropTargetChangedEvent dragDropTargetChangedEvent) {
        DropTarget dropTarget = dragDropTargetChangedEvent.dropTarget;
        if (dropTarget == null || !(dropTarget instanceof DockState)) {
            DockState[] dockStatesForCurrentOrientation = LegacyRecentsImpl.getConfiguration().getDockStatesForCurrentOrientation();
            DockState.ViewState viewState = DockState.NONE.viewState;
            updateVisibleDockRegions(dockStatesForCurrentOrientation, true, viewState.dockAreaAlpha, viewState.hintTextAlpha, true, true);
        } else {
            updateVisibleDockRegions(new DockState[]{(DockState) dropTarget}, false, -1, -1, true, true);
        }
        if (this.mStackActionButton != null) {
            dragDropTargetChangedEvent.addPostAnimationCallback(new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass1 */

                public void run() {
                    Rect stackActionButtonBoundsFromStackLayout = RecentsView.this.getStackActionButtonBoundsFromStackLayout();
                    RecentsView.this.mStackActionButton.setLeftTopRightBottom(stackActionButtonBoundsFromStackLayout.left, stackActionButtonBoundsFromStackLayout.top, stackActionButtonBoundsFromStackLayout.right, stackActionButtonBoundsFromStackLayout.bottom);
                }
            });
        }
    }

    public final void onBusEvent(final DragEndEvent dragEndEvent) {
        DropTarget dropTarget = dragEndEvent.dropTarget;
        if (dropTarget instanceof DockState) {
            updateVisibleDockRegions(null, false, -1, -1, false, false);
            Utilities.setViewFrameFromTranslation(dragEndEvent.taskView);
            if (ActivityManagerWrapper.getInstance().startActivityFromRecents(dragEndEvent.task.key.id, ActivityOptionsCompat.makeSplitScreenOptions(((DockState) dropTarget).createMode == 0))) {
                $$Lambda$RecentsView$RRL6yVNHxRLA7npjCgaGmNF62Mc r3 = new Runnable(dragEndEvent) {
                    /* class com.android.systemui.recents.views.$$Lambda$RecentsView$RRL6yVNHxRLA7npjCgaGmNF62Mc */
                    private final /* synthetic */ DragEndEvent f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        RecentsView.this.lambda$onBusEvent$1$RecentsView(this.f$1);
                    }
                };
                final Rect taskRect = getTaskRect(dragEndEvent.taskView);
                WindowManagerWrapper.getInstance().overridePendingAppTransitionMultiThumbFuture(new AppTransitionAnimationSpecsFuture(getHandler()) {
                    /* class com.android.systemui.recents.views.RecentsView.AnonymousClass2 */

                    @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
                    public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                        return RecentsView.this.mTransitionHelper.composeDockAnimationSpec(dragEndEvent.taskView, taskRect);
                    }
                }, r3, getHandler(), true, getContext().getDisplayId());
                MetricsLogger.action(((FrameLayout) this).mContext, 270, dragEndEvent.task.getTopComponent().flattenToShortString());
            } else {
                EventBus.getDefault().send(new DragEndCancelledEvent(getStack(), dragEndEvent.task, dragEndEvent.taskView));
            }
        } else {
            updateVisibleDockRegions(null, true, -1, -1, true, false);
        }
        TextView textView = this.mStackActionButton;
        if (textView != null) {
            textView.animate().alpha(1.0f).setDuration(134).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public /* synthetic */ void lambda$onBusEvent$1$RecentsView(DragEndEvent dragEndEvent) {
        EventBus.getDefault().send(new DockedFirstAnimationFrameEvent());
        getStack().removeTask(dragEndEvent.task, null, true);
    }

    public final void onBusEvent(DragEndCancelledEvent dragEndCancelledEvent) {
        updateVisibleDockRegions(null, true, -1, -1, true, false);
    }

    private Rect getTaskRect(TaskView taskView) {
        int[] locationOnScreen = taskView.getLocationOnScreen();
        int i = locationOnScreen[0];
        int i2 = locationOnScreen[1];
        return new Rect(i, i2, (int) (((float) i) + (((float) taskView.getWidth()) * taskView.getScaleX())), (int) (((float) i2) + (((float) taskView.getHeight()) * taskView.getScaleY())));
    }

    public final void onBusEvent(DraggingInRecentsEvent draggingInRecentsEvent) {
        if (this.mTaskStackView.getTaskViews().size() > 0) {
            setTranslationY(draggingInRecentsEvent.distanceFromTop - this.mTaskStackView.getTaskViews().get(0).getY());
        }
    }

    public final void onBusEvent(DraggingInRecentsEndedEvent draggingInRecentsEndedEvent) {
        ViewPropertyAnimator animate = animate();
        if (draggingInRecentsEndedEvent.velocity > this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            animate.translationY((float) getHeight());
            animate.withEndAction(new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass3 */

                public void run() {
                    WindowManagerProxy.getInstance().maximizeDockedStack();
                }
            });
            this.mFlingAnimationUtils.apply(animate, getTranslationY(), (float) getHeight(), draggingInRecentsEndedEvent.velocity);
        } else {
            animate.translationY(0.0f);
            animate.setListener(null);
            this.mFlingAnimationUtils.apply(animate, getTranslationY(), 0.0f, draggingInRecentsEndedEvent.velocity);
        }
        animate.start();
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent enterRecentsWindowAnimationCompletedEvent) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        if (!launchState.launchedViaDockGesture && !launchState.launchedFromApp && getStack().getTaskCount() > 0) {
            animateBackgroundScrim(getOpaqueScrimAlpha(), 300);
        }
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent allTaskViewsDismissedEvent) {
        EventBus.getDefault().send(new HideStackActionButtonEvent());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent dismissAllTaskViewsEvent) {
        if (!LegacyRecentsImpl.getSystemServices().hasDockedTask()) {
            animateBackgroundScrim(0.0f, 200);
        }
    }

    public final void onBusEvent(ShowStackActionButtonEvent showStackActionButtonEvent) {
        showStackActionButton(134, showStackActionButtonEvent.translate);
    }

    public final void onBusEvent(HideStackActionButtonEvent hideStackActionButtonEvent) {
        hideStackActionButton(100, true);
    }

    public final void onBusEvent(MultiWindowStateChangedEvent multiWindowStateChangedEvent) {
        updateStack(multiWindowStateChangedEvent.stack, false);
    }

    public final void onBusEvent(ShowEmptyViewEvent showEmptyViewEvent) {
        showEmptyView(2131821943);
    }

    private void showStackActionButton(final int i, final boolean z) {
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        if (this.mStackActionButton.getVisibility() == 4) {
            this.mStackActionButton.setVisibility(0);
            this.mStackActionButton.setAlpha(0.0f);
            if (z) {
                TextView textView = this.mStackActionButton;
                textView.setTranslationY(((float) textView.getMeasuredHeight()) * (LegacyRecentsImpl.getConfiguration().isLowRamDevice ? 1.0f : -0.25f));
            } else {
                this.mStackActionButton.setTranslationY(0.0f);
            }
            referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass4 */

                public void run() {
                    if (z) {
                        RecentsView.this.mStackActionButton.animate().translationY(0.0f);
                    }
                    RecentsView.this.mStackActionButton.animate().alpha(1.0f).setDuration((long) i).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).start();
                }
            });
        }
        referenceCountedTrigger.flushLastDecrementRunnables();
    }

    private void hideStackActionButton(int i, boolean z) {
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        hideStackActionButton(i, z, referenceCountedTrigger);
        referenceCountedTrigger.flushLastDecrementRunnables();
    }

    private void hideStackActionButton(int i, boolean z, final ReferenceCountedTrigger referenceCountedTrigger) {
        if (this.mStackActionButton.getVisibility() == 0) {
            if (z) {
                this.mStackActionButton.animate().translationY(((float) this.mStackActionButton.getMeasuredHeight()) * (LegacyRecentsImpl.getConfiguration().isLowRamDevice ? 1.0f : -0.25f));
            }
            this.mStackActionButton.animate().alpha(0.0f).setDuration((long) i).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass5 */

                public void run() {
                    RecentsView.this.mStackActionButton.setVisibility(4);
                    referenceCountedTrigger.decrement();
                }
            }).start();
            referenceCountedTrigger.increment();
        }
    }

    private void animateEmptyView(boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        float height = (float) (this.mTaskStackView.getStackAlgorithm().getTaskRect().height() / 4);
        float f = 0.0f;
        this.mEmptyView.setTranslationY(z ? height : 0.0f);
        this.mEmptyView.setAlpha(z ? 0.0f : 1.0f);
        ViewPropertyAnimator interpolator = this.mEmptyView.animate().setDuration(150).setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        if (z) {
            height = 0.0f;
        }
        ViewPropertyAnimator translationY = interpolator.translationY(height);
        if (z) {
            f = 1.0f;
        }
        ViewPropertyAnimator alpha = translationY.alpha(f);
        if (referenceCountedTrigger != null) {
            alpha.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
            referenceCountedTrigger.increment();
        }
        alpha.start();
    }

    private void updateVisibleDockRegions(DockState[] dockStateArr, boolean z, int i, int i2, boolean z2, boolean z3) {
        int i3;
        int i4;
        int i5;
        Rect rect;
        ArraySet arraySet = new ArraySet();
        Utilities.arrayToSet(dockStateArr, arraySet);
        ArrayList<DockState> visibleDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int size = visibleDockStates.size() - 1; size >= 0; size--) {
            DockState dockState = visibleDockStates.get(size);
            DockState.ViewState viewState = dockState.viewState;
            if (dockStateArr == null || !arraySet.contains(dockState)) {
                viewState.startAnimation(null, 0, 0, 250, Interpolators.FAST_OUT_SLOW_IN, z2, z3);
            } else {
                if (i != -1) {
                    i3 = i2;
                    i4 = i;
                } else {
                    i3 = i2;
                    i4 = viewState.dockAreaAlpha;
                }
                if (i3 != -1) {
                    i5 = i3;
                } else {
                    i5 = viewState.hintTextAlpha;
                }
                if (z) {
                    rect = dockState.getPreDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mSystemInsets);
                } else {
                    rect = dockState.getDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mDividerSize, this.mSystemInsets, getResources());
                }
                if (viewState.dockAreaOverlay.getCallback() != this) {
                    viewState.dockAreaOverlay.setCallback(this);
                    viewState.dockAreaOverlay.setBounds(rect);
                }
                viewState.startAnimation(rect, i4, i5, 250, Interpolators.FAST_OUT_SLOW_IN, z2, z3);
            }
        }
    }

    private float getOpaqueScrimAlpha() {
        return MathUtils.map(0.0f, 1.0f, 0.2f, 0.7f, this.mBusynessFactor);
    }

    private void animateBackgroundScrim(float f, int i) {
        Interpolator interpolator;
        Utilities.cancelAnimationWithoutCallbacks(this.mBackgroundScrimAnimator);
        int alpha = this.mBackgroundScrim.getAlpha();
        int i2 = (int) (f * 255.0f);
        this.mBackgroundScrimAnimator = ValueAnimator.ofInt(alpha, i2);
        this.mBackgroundScrimAnimator.setDuration((long) i);
        ValueAnimator valueAnimator = this.mBackgroundScrimAnimator;
        if (i2 > alpha) {
            interpolator = Interpolators.ALPHA_IN;
        } else {
            interpolator = Interpolators.ALPHA_OUT;
        }
        valueAnimator.setInterpolator(interpolator);
        this.mBackgroundScrimAnimator.addUpdateListener(this.mUpdateBackgroundScrimAlpha);
        this.mBackgroundScrimAnimator.start();
    }

    /* access modifiers changed from: package-private */
    public Rect getStackActionButtonBoundsFromStackLayout() {
        int i;
        int i2;
        int i3;
        int i4;
        Rect rect = new Rect(this.mTaskStackView.mLayoutAlgorithm.getStackActionButtonRect());
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            Rect windowRect = LegacyRecentsImpl.getSystemServices().getWindowRect();
            int width = windowRect.width();
            Rect rect2 = this.mSystemInsets;
            i = ((((width - rect2.left) - rect2.right) - this.mStackActionButton.getMeasuredWidth()) / 2) + this.mSystemInsets.left;
            i2 = windowRect.height() - ((this.mStackActionButton.getMeasuredHeight() + this.mSystemInsets.bottom) + (this.mStackActionButton.getPaddingBottom() / 2));
        } else {
            if (isLayoutRtl()) {
                i4 = rect.left;
                i3 = this.mStackActionButton.getPaddingLeft();
            } else {
                i4 = rect.right + this.mStackActionButton.getPaddingRight();
                i3 = this.mStackActionButton.getMeasuredWidth();
            }
            i = i4 - i3;
            i2 = rect.top + ((rect.height() - this.mStackActionButton.getMeasuredHeight()) / 2);
        }
        rect.set(i, i2, this.mStackActionButton.getMeasuredWidth() + i, this.mStackActionButton.getMeasuredHeight() + i2);
        return rect;
    }

    /* access modifiers changed from: package-private */
    public View getStackActionButton() {
        return this.mStackActionButton;
    }

    public void launchTaskFromRecents(TaskStack taskStack, final Task task, final TaskStackView taskStackView, TaskView taskView, final boolean z, final int i, final int i2) {
        AnonymousClass6 r5;
        Runnable runnable;
        AnonymousClass6 r13 = null;
        if (taskView != null) {
            final Rect windowRect = LegacyRecentsImpl.getSystemServices().getWindowRect();
            AnonymousClass6 r14 = new AppTransitionAnimationSpecsFuture(taskStackView.getHandler()) {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass6 */

                @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
                public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                    return RecentsView.this.mTransitionHelper.composeAnimationSpecs(task, taskStackView, i, i2, windowRect);
                }
            };
            runnable = new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass7 */
                private boolean mHandled;

                public void run() {
                    if (!this.mHandled) {
                        this.mHandled = true;
                        EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                        EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                        taskStackView.cancelAllTaskViewAnimations();
                        if (z) {
                            RecentsView.this.mHandler.postDelayed(new Runnable(task) {
                                /* class com.android.systemui.recents.views.$$Lambda$RecentsView$7$6nm4p5eX5UvPb6uaSaO8OVjwZdU */
                                private final /* synthetic */ Task f$1;

                                {
                                    this.f$1 = r2;
                                }

                                public final void run() {
                                    RecentsView.AnonymousClass7.this.lambda$run$0$RecentsView$7(this.f$1);
                                }
                            }, 350);
                        }
                        if (!LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                            EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
                        }
                    }
                }

                public /* synthetic */ void lambda$run$0$RecentsView$7(Task task) {
                    EventBus.getDefault().send(new ScreenPinningRequestEvent(((FrameLayout) RecentsView.this).mContext, task.key.id));
                }
            };
            r5 = r14;
        } else {
            runnable = new Runnable() {
                /* class com.android.systemui.recents.views.RecentsView.AnonymousClass8 */
                private boolean mHandled;

                public void run() {
                    if (!this.mHandled) {
                        this.mHandled = true;
                        EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                        EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                        taskStackView.cancelAllTaskViewAnimations();
                        if (!LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                            EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
                        }
                    }
                }
            };
            r5 = null;
        }
        EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(true));
        Context context = ((FrameLayout) this).mContext;
        Handler handler = this.mHandler;
        if (r5 != null) {
            r13 = r5;
        }
        ActivityOptions createAspectScaleAnimation = RecentsTransition.createAspectScaleAnimation(context, handler, true, r13, runnable);
        if (taskView == null) {
            startTaskActivity(taskStack, task, taskView, createAspectScaleAnimation, r5, i, i2);
        } else {
            EventBus.getDefault().send(new LaunchTaskStartedEvent(taskView, z));
            startTaskActivity(taskStack, task, taskView, createAspectScaleAnimation, r5, i, i2);
        }
        ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
    }

    private void startTaskActivity(TaskStack taskStack, Task task, TaskView taskView, ActivityOptions activityOptions, AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture, int i, int i2) {
        ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(task.key, activityOptions, i, i2, new Consumer(taskStack, task, taskView) {
            /* class com.android.systemui.recents.views.$$Lambda$RecentsView$WRtgGycc3yq7mZO7tMOI7w0a60 */
            private final /* synthetic */ TaskStack f$1;
            private final /* synthetic */ Task f$2;
            private final /* synthetic */ TaskView f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RecentsView.this.lambda$startTaskActivity$2$RecentsView(this.f$1, this.f$2, this.f$3, (Boolean) obj);
            }
        }, getHandler());
        if (appTransitionAnimationSpecsFuture != null) {
            Handler handler = this.mHandler;
            Objects.requireNonNull(appTransitionAnimationSpecsFuture);
            handler.post(new Runnable() {
                /* class com.android.systemui.recents.views.$$Lambda$2_yYbS189Yb53TwKAnkQBhUWOR4 */

                public final void run() {
                    AppTransitionAnimationSpecsFuture.this.composeSpecsSynchronous();
                }
            });
        }
    }

    public /* synthetic */ void lambda$startTaskActivity$2$RecentsView(TaskStack taskStack, Task task, TaskView taskView, Boolean bool) {
        int i = 0;
        if (bool.booleanValue()) {
            int indexOfTask = taskStack.indexOfTask(task);
            if (indexOfTask > -1) {
                i = (taskStack.getTaskCount() - indexOfTask) - 1;
            }
            EventBus.getDefault().send(new LaunchTaskSucceededEvent(i));
            return;
        }
        Log.e("RecentsView", ((FrameLayout) this).mContext.getString(2131821946, task.title));
        if (taskView != null) {
            taskView.dismissTask();
        }
        EventBus.getDefault().send(new LaunchTaskFailedEvent());
    }

    public void requestDisallowInterceptTouchEvent(boolean z) {
        super.requestDisallowInterceptTouchEvent(z);
        this.mTouchHandler.cancelStackActionButtonClick();
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        String hexString = Integer.toHexString(System.identityHashCode(this));
        printWriter.print(str);
        printWriter.print("RecentsView");
        printWriter.print(" awaitingFirstLayout=");
        printWriter.print(this.mAwaitingFirstLayout ? "Y" : "N");
        printWriter.print(" insets=");
        printWriter.print(Utilities.dumpRect(this.mSystemInsets));
        printWriter.print(" [0x");
        printWriter.print(hexString);
        printWriter.print("]");
        printWriter.println();
        if (getStack() != null) {
            getStack().dump(str2, printWriter);
        }
        TaskStackView taskStackView = this.mTaskStackView;
        if (taskStackView != null) {
            taskStackView.dump(str2, printWriter);
        }
    }
}
