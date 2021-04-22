package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0014R$string;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.TaskStackAnimationHelper;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.io.PrintWriter;
import java.util.ArrayList;

public class TaskView extends FixedSizeFrameLayout implements Task.TaskCallbacks, TaskStackAnimationHelper.Callbacks, View.OnClickListener, View.OnLongClickListener {
    public static final Property<TaskView, Float> DIM_ALPHA = new FloatProperty<TaskView>("dimAlpha") {
        /* class com.android.systemui.recents.views.TaskView.AnonymousClass2 */

        public void setValue(TaskView taskView, float f) {
            taskView.setDimAlpha(f);
        }

        public Float get(TaskView taskView) {
            return Float.valueOf(taskView.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> DIM_ALPHA_WITHOUT_HEADER = new FloatProperty<TaskView>("dimAlphaWithoutHeader") {
        /* class com.android.systemui.recents.views.TaskView.AnonymousClass1 */

        public void setValue(TaskView taskView, float f) {
            taskView.setDimAlphaWithoutHeader(f);
        }

        public Float get(TaskView taskView) {
            return Float.valueOf(taskView.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> VIEW_OUTLINE_ALPHA = new FloatProperty<TaskView>("viewOutlineAlpha") {
        /* class com.android.systemui.recents.views.TaskView.AnonymousClass3 */

        public void setValue(TaskView taskView, float f) {
            taskView.getViewBounds().setAlpha(f);
        }

        public Float get(TaskView taskView) {
            return Float.valueOf(taskView.getViewBounds().getAlpha());
        }
    };
    private float mActionButtonTranslationZ;
    private View mActionButtonView;
    private TaskViewCallbacks mCb;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mClipViewInStack;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;
    private ObjectAnimator mDimAnimator;
    private Toast mDisabledAppToast;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownTouchPos;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "header_")
    protected TaskViewHeader mHeaderView;
    private View mIncompatibleAppToastView;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDisabledInSafeMode;
    private ObjectAnimator mOutlineAnimator;
    private final TaskViewTransform mTargetAnimationTransform;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "task_")
    private Task mTask;
    private boolean mTaskBound;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "thumbnail_")
    protected TaskViewThumbnail mThumbnailView;
    private ArrayList<Animator> mTmpAnimators;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mTouchExplorationEnabled;
    private AnimatorSet mTransformAnimation;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "view_bounds_")
    private AnimateableViewBounds mViewBounds;

    /* access modifiers changed from: package-private */
    public interface TaskViewCallbacks {
        void onTaskViewClipStateChanged(TaskView taskView);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TaskView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TaskView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mClipViewInStack = true;
        this.mTargetAnimationTransform = new TaskViewTransform();
        this.mTmpAnimators = new ArrayList<>();
        this.mDownTouchPos = new Point();
        RecentsConfiguration configuration = LegacyRecentsImpl.getConfiguration();
        Resources resources = context.getResources();
        this.mViewBounds = createOutlineProvider();
        if (configuration.fakeShadows) {
            setBackground(new FakeShadowDrawable(resources, configuration));
        }
        setOutlineProvider(this.mViewBounds);
        setOnLongClickListener(this);
        setAccessibilityDelegate(new TaskViewAccessibilityDelegate(this));
    }

    /* access modifiers changed from: package-private */
    public void setCallbacks(TaskViewCallbacks taskViewCallbacks) {
        this.mCb = taskViewCallbacks;
    }

    /* access modifiers changed from: package-private */
    public void onReload(boolean z) {
        resetNoUserInteractionState();
        if (!z) {
            resetViewProperties();
        }
    }

    public Task getTask() {
        return this.mTask;
    }

    /* access modifiers changed from: protected */
    public AnimateableViewBounds createOutlineProvider() {
        return new AnimateableViewBounds(this, ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(2131166262));
    }

    /* access modifiers changed from: package-private */
    public AnimateableViewBounds getViewBounds() {
        return this.mViewBounds;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mHeaderView = (TaskViewHeader) findViewById(2131362886);
        this.mThumbnailView = (TaskViewThumbnail) findViewById(2131362887);
        this.mThumbnailView.updateClipToTaskBar(this.mHeaderView);
        this.mActionButtonView = findViewById(2131362409);
        this.mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            /* class com.android.systemui.recents.views.TaskView.AnonymousClass4 */

            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, TaskView.this.mActionButtonView.getWidth(), TaskView.this.mActionButtonView.getHeight());
                outline.setAlpha(0.35f);
            }
        });
        this.mActionButtonView.setOnClickListener(this);
        this.mActionButtonTranslationZ = this.mActionButtonView.getTranslationZ();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged() {
        this.mHeaderView.onConfigurationChanged();
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i > 0 && i2 > 0) {
            this.mHeaderView.onTaskViewSizeChanged(i, i2);
            this.mThumbnailView.onTaskViewSizeChanged(i, i2);
            this.mActionButtonView.setTranslationX((float) (i - getMeasuredWidth()));
            this.mActionButtonView.setTranslationY((float) (i2 - getMeasuredHeight()));
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mDownTouchPos.set((int) (motionEvent.getX() * getScaleX()), (int) (motionEvent.getY() * getScaleY()));
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.recents.views.FixedSizeFrameLayout
    public void measureContents(int i, int i2) {
        measureChildren(View.MeasureSpec.makeMeasureSpec((i - ((FrameLayout) this).mPaddingLeft) - ((FrameLayout) this).mPaddingRight, 1073741824), View.MeasureSpec.makeMeasureSpec((i2 - ((FrameLayout) this).mPaddingTop) - ((FrameLayout) this).mPaddingBottom, 1073741824));
        setMeasuredDimension(i, i2);
    }

    /* access modifiers changed from: package-private */
    public void updateViewPropertiesToTaskTransform(TaskViewTransform taskViewTransform, AnimationProps animationProps, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        RecentsConfiguration configuration = LegacyRecentsImpl.getConfiguration();
        cancelTransformAnimation();
        this.mTmpAnimators.clear();
        taskViewTransform.applyToTaskView(this, this.mTmpAnimators, animationProps, !configuration.fakeShadows);
        if (animationProps.isImmediate()) {
            if (Float.compare(getDimAlpha(), taskViewTransform.dimAlpha) != 0) {
                setDimAlpha(taskViewTransform.dimAlpha);
            }
            if (Float.compare(this.mViewBounds.getAlpha(), taskViewTransform.viewOutlineAlpha) != 0) {
                this.mViewBounds.setAlpha(taskViewTransform.viewOutlineAlpha);
            }
            if (animationProps.getListener() != null) {
                animationProps.getListener().onAnimationEnd(null);
            }
            if (animatorUpdateListener != null) {
                animatorUpdateListener.onAnimationUpdate(null);
                return;
            }
            return;
        }
        if (Float.compare(getDimAlpha(), taskViewTransform.dimAlpha) != 0) {
            this.mDimAnimator = ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(), taskViewTransform.dimAlpha);
            ArrayList<Animator> arrayList = this.mTmpAnimators;
            ObjectAnimator objectAnimator = this.mDimAnimator;
            animationProps.apply(6, objectAnimator);
            arrayList.add(objectAnimator);
        }
        if (Float.compare(this.mViewBounds.getAlpha(), taskViewTransform.viewOutlineAlpha) != 0) {
            this.mOutlineAnimator = ObjectAnimator.ofFloat(this, VIEW_OUTLINE_ALPHA, this.mViewBounds.getAlpha(), taskViewTransform.viewOutlineAlpha);
            ArrayList<Animator> arrayList2 = this.mTmpAnimators;
            ObjectAnimator objectAnimator2 = this.mOutlineAnimator;
            animationProps.apply(6, objectAnimator2);
            arrayList2.add(objectAnimator2);
        }
        if (animatorUpdateListener != null) {
            ValueAnimator ofInt = ValueAnimator.ofInt(0, 1);
            ofInt.addUpdateListener(animatorUpdateListener);
            ArrayList<Animator> arrayList3 = this.mTmpAnimators;
            animationProps.apply(6, ofInt);
            arrayList3.add(ofInt);
        }
        this.mTransformAnimation = animationProps.createAnimator(this.mTmpAnimators);
        this.mTransformAnimation.start();
        this.mTargetAnimationTransform.copyFrom(taskViewTransform);
    }

    /* access modifiers changed from: package-private */
    public void resetViewProperties() {
        cancelTransformAnimation();
        setDimAlpha(0.0f);
        setVisibility(0);
        getViewBounds().reset();
        getHeaderView().reset();
        TaskViewTransform.reset(this);
        this.mActionButtonView.setScaleX(1.0f);
        this.mActionButtonView.setScaleY(1.0f);
        this.mActionButtonView.setAlpha(0.0f);
        this.mActionButtonView.setTranslationX(0.0f);
        this.mActionButtonView.setTranslationY(0.0f);
        this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
        View view = this.mIncompatibleAppToastView;
        if (view != null) {
            view.setVisibility(4);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimatingTo(TaskViewTransform taskViewTransform) {
        AnimatorSet animatorSet = this.mTransformAnimation;
        return animatorSet != null && animatorSet.isStarted() && this.mTargetAnimationTransform.isSame(taskViewTransform);
    }

    public void cancelTransformAnimation() {
        cancelDimAnimationIfExists();
        Utilities.cancelAnimationWithoutCallbacks(this.mTransformAnimation);
        Utilities.cancelAnimationWithoutCallbacks(this.mOutlineAnimator);
    }

    private void cancelDimAnimationIfExists() {
        ObjectAnimator objectAnimator = this.mDimAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
    }

    public void setTouchEnabled(boolean z) {
        setOnClickListener(z ? this : null);
    }

    public void startNoUserInteractionAnimation() {
        this.mHeaderView.startNoUserInteractionAnimation();
    }

    /* access modifiers changed from: package-private */
    public void setNoUserInteractionState() {
        this.mHeaderView.setNoUserInteractionState();
    }

    /* access modifiers changed from: package-private */
    public void resetNoUserInteractionState() {
        this.mHeaderView.resetNoUserInteractionState();
    }

    /* access modifiers changed from: package-private */
    public void dismissTask() {
        DismissTaskViewEvent dismissTaskViewEvent = new DismissTaskViewEvent(this);
        dismissTaskViewEvent.addPostAnimationCallback(new Runnable() {
            /* class com.android.systemui.recents.views.TaskView.AnonymousClass5 */

            public void run() {
                EventBus.getDefault().send(new TaskViewDismissedEvent(TaskView.this.mTask, this, new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN)));
            }
        });
        EventBus.getDefault().send(dismissTaskViewEvent);
    }

    /* access modifiers changed from: package-private */
    public boolean shouldClipViewInStack() {
        if (getVisibility() != 0 || LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return false;
        }
        return this.mClipViewInStack;
    }

    /* access modifiers changed from: package-private */
    public void setClipViewInStack(boolean z) {
        if (z != this.mClipViewInStack) {
            this.mClipViewInStack = z;
            TaskViewCallbacks taskViewCallbacks = this.mCb;
            if (taskViewCallbacks != null) {
                taskViewCallbacks.onTaskViewClipStateChanged(this);
            }
        }
    }

    public TaskViewHeader getHeaderView() {
        return this.mHeaderView;
    }

    public void setDimAlpha(float f) {
        this.mDimAlpha = f;
        this.mThumbnailView.setDimAlpha(f);
        this.mHeaderView.setDimAlpha(f);
    }

    public void setDimAlphaWithoutHeader(float f) {
        this.mDimAlpha = f;
        this.mThumbnailView.setDimAlpha(f);
    }

    public float getDimAlpha() {
        return this.mDimAlpha;
    }

    public void setFocusedState(boolean z, boolean z2) {
        if (z) {
            if (z2 && !isFocused()) {
                requestFocus();
            }
        } else if (isAccessibilityFocused() && this.mTouchExplorationEnabled) {
            clearAccessibilityFocus();
        }
    }

    public void showActionButton(boolean z, int i) {
        this.mActionButtonView.setVisibility(0);
        if (!z || this.mActionButtonView.getAlpha() >= 1.0f) {
            this.mActionButtonView.setScaleX(1.0f);
            this.mActionButtonView.setScaleY(1.0f);
            this.mActionButtonView.setAlpha(1.0f);
            this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
            return;
        }
        this.mActionButtonView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration((long) i).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public void hideActionButton(boolean z, int i, boolean z2, final Animator.AnimatorListener animatorListener) {
        if (!z || this.mActionButtonView.getAlpha() <= 0.0f) {
            this.mActionButtonView.setAlpha(0.0f);
            this.mActionButtonView.setVisibility(4);
            if (animatorListener != null) {
                animatorListener.onAnimationEnd(null);
                return;
            }
            return;
        }
        if (z2) {
            this.mActionButtonView.animate().scaleX(0.9f).scaleY(0.9f);
        }
        this.mActionButtonView.animate().alpha(0.0f).setDuration((long) i).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            /* class com.android.systemui.recents.views.TaskView.AnonymousClass6 */

            public void run() {
                Animator.AnimatorListener animatorListener = animatorListener;
                if (animatorListener != null) {
                    animatorListener.onAnimationEnd(null);
                }
                TaskView.this.mActionButtonView.setVisibility(4);
            }
        }).start();
    }

    public void onPrepareLaunchTargetForEnterAnimation() {
        setDimAlphaWithoutHeader(0.0f);
        this.mActionButtonView.setAlpha(0.0f);
        View view = this.mIncompatibleAppToastView;
        if (view != null && view.getVisibility() == 0) {
            this.mIncompatibleAppToastView.setAlpha(0.0f);
        }
    }

    public void onStartLaunchTargetEnterAnimation(TaskViewTransform taskViewTransform, int i, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        cancelDimAnimationIfExists();
        referenceCountedTrigger.increment();
        AnimationProps animationProps = new AnimationProps(i, Interpolators.ALPHA_OUT);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, DIM_ALPHA_WITHOUT_HEADER, getDimAlpha(), taskViewTransform.dimAlpha);
        animationProps.apply(7, ofFloat);
        this.mDimAnimator = ofFloat;
        this.mDimAnimator.addListener(referenceCountedTrigger.decrementOnAnimationEnd());
        this.mDimAnimator.start();
        if (z) {
            showActionButton(true, i);
        }
        View view = this.mIncompatibleAppToastView;
        if (view != null && view.getVisibility() == 0) {
            this.mIncompatibleAppToastView.animate().alpha(1.0f).setDuration((long) i).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public void onStartLaunchTargetLaunchAnimation(int i, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        AnimationProps animationProps = new AnimationProps(i, Interpolators.ALPHA_OUT);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(), 0.0f);
        animationProps.apply(7, ofFloat);
        this.mDimAnimator = ofFloat;
        this.mDimAnimator.start();
        referenceCountedTrigger.increment();
        hideActionButton(true, i, !z, referenceCountedTrigger.decrementOnAnimationEnd());
    }

    public void onStartFrontTaskEnterAnimation(boolean z) {
        if (z) {
            showActionButton(false, 0);
        }
    }

    public void onTaskBound(Task task, boolean z, int i, Rect rect) {
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        this.mTouchExplorationEnabled = z;
        this.mTask = task;
        boolean z2 = true;
        this.mTaskBound = true;
        this.mTask.addCallback(this);
        if (this.mTask.isSystemApp || !systemServices.isInSafeMode()) {
            z2 = false;
        }
        this.mIsDisabledInSafeMode = z2;
        this.mThumbnailView.bindToTask(this.mTask, this.mIsDisabledInSafeMode, i, rect);
        this.mHeaderView.bindToTask(this.mTask, this.mTouchExplorationEnabled, this.mIsDisabledInSafeMode);
        if (task.isDockable || !systemServices.hasDockedTask()) {
            View view = this.mIncompatibleAppToastView;
            if (view != null) {
                view.setVisibility(4);
                return;
            }
            return;
        }
        if (this.mIncompatibleAppToastView == null) {
            this.mIncompatibleAppToastView = Utilities.findViewStubById(this, 2131362300).inflate();
            ((TextView) findViewById(16908299)).setText(C0014R$string.dock_non_resizeble_failed_to_dock_text);
        }
        this.mIncompatibleAppToastView.setVisibility(0);
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataLoaded(Task task, ThumbnailData thumbnailData) {
        if (this.mTaskBound) {
            this.mThumbnailView.onTaskDataLoaded(thumbnailData);
            this.mHeaderView.onTaskDataLoaded();
        }
    }

    @Override // com.android.systemui.shared.recents.model.Task.TaskCallbacks
    public void onTaskDataUnloaded() {
        this.mTask.removeCallback(this);
        this.mThumbnailView.unbindFromTask();
        this.mHeaderView.unbindFromTask(this.mTouchExplorationEnabled);
        this.mTaskBound = false;
    }

    public void onClick(View view) {
        boolean z = true;
        if (this.mIsDisabledInSafeMode) {
            Context context = getContext();
            String string = context.getString(2131821945, this.mTask.title);
            Toast toast = this.mDisabledAppToast;
            if (toast != null) {
                toast.cancel();
            }
            this.mDisabledAppToast = Toast.makeText(context, string, 0);
            this.mDisabledAppToast.show();
            return;
        }
        View view2 = this.mActionButtonView;
        if (view == view2) {
            view2.setTranslationZ(0.0f);
        } else {
            z = false;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(this, this.mTask, null, z));
        MetricsLogger.action(view.getContext(), 277, this.mTask.key.getComponent().toString());
    }

    public boolean onLongClick(View view) {
        boolean z;
        if (!LegacyRecentsImpl.getConfiguration().dragToSplitEnabled) {
            return false;
        }
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        Rect rect = new Rect(this.mViewBounds.getClipBounds());
        if (!rect.isEmpty()) {
            rect.scale(getScaleX());
            Point point = this.mDownTouchPos;
            z = rect.contains(point.x, point.y);
        } else {
            z = this.mDownTouchPos.x <= getWidth() && this.mDownTouchPos.y <= getHeight();
        }
        if (view != this || !z || systemServices.hasDockedTask()) {
            return false;
        }
        setClipViewInStack(false);
        Point point2 = this.mDownTouchPos;
        point2.x = (int) (((float) point2.x) + (((1.0f - getScaleX()) * ((float) getWidth())) / 2.0f));
        Point point3 = this.mDownTouchPos;
        point3.y = (int) (((float) point3.y) + (((1.0f - getScaleY()) * ((float) getHeight())) / 2.0f));
        EventBus.getDefault().register(this, 3);
        EventBus.getDefault().send(new DragStartEvent(this.mTask, this, this.mDownTouchPos));
        return true;
    }

    public final void onBusEvent(DragEndEvent dragEndEvent) {
        if (!(dragEndEvent.dropTarget instanceof DockState)) {
            dragEndEvent.addPostAnimationCallback(new Runnable() {
                /* class com.android.systemui.recents.views.$$Lambda$TaskView$pg3FhkTHYE_OFyYVD2c3tjmUu4 */

                public final void run() {
                    TaskView.this.lambda$onBusEvent$0$TaskView();
                }
            });
        }
        EventBus.getDefault().unregister(this);
    }

    public /* synthetic */ void lambda$onBusEvent$0$TaskView() {
        setClipViewInStack(true);
    }

    public final void onBusEvent(DragEndCancelledEvent dragEndCancelledEvent) {
        dragEndCancelledEvent.addPostAnimationCallback(new Runnable() {
            /* class com.android.systemui.recents.views.$$Lambda$TaskView$5tk674oyueWivSnbNCm15r4dSjc */

            public final void run() {
                TaskView.this.lambda$onBusEvent$1$TaskView();
            }
        });
    }

    public /* synthetic */ void lambda$onBusEvent$1$TaskView() {
        setClipViewInStack(true);
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("TaskView");
        printWriter.print(" mTask=");
        printWriter.print(this.mTask.key.id);
        printWriter.println();
        this.mThumbnailView.dump(str + "  ", printWriter);
    }
}
