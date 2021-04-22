package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.MutableBoolean;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchMostRecentTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.events.activity.ShowEmptyViewEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.component.ActivityPinnedEvent;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.recents.views.ViewPool;
import com.android.systemui.recents.views.grid.GridTaskView;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.recents.views.grid.TaskViewFocusFrame;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStackView extends FrameLayout implements TaskStack.TaskStackCallbacks, TaskView.TaskViewCallbacks, TaskStackViewScroller.TaskStackViewScrollerCallbacks, TaskStackLayoutAlgorithm.TaskStackLayoutAlgorithmCallbacks, ViewPool.ViewPoolConsumer<TaskView, Task> {
    private TaskStackAnimationHelper mAnimationHelper;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<>();
    private AnimationProps mDeferredTaskViewLayoutAnimation = null;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mDisplayOrientation = 0;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mDisplayRect = new Rect();
    private int mDividerSize;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mEnterAnimationComplete = false;
    private final float mFastFlingVelocity;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mFinishedLayoutAfterStackReload = false;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "focused_task_")
    private Task mFocusedTask;
    private ArraySet<Task.TaskKey> mIgnoreTasks = new ArraySet<>();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInMeasureLayout = false;
    private LayoutInflater mInflater;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialState = 1;
    private int mLastHeight;
    private float mLastScrollPPercent = -1.0f;
    private int mLastWidth;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mLaunchNextAfterFirstMeasure = false;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    private Task mPrefetchingTask;
    private ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener = new ValueAnimator.AnimatorUpdateListener() {
        /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass1 */

        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (!TaskStackView.this.mTaskViewsClipDirty) {
                TaskStackView.this.mTaskViewsClipDirty = true;
                TaskStackView.this.invalidate();
            }
        }
    };
    private boolean mResetToInitialStateWhenResized;
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mScreenPinningEnabled;
    private TaskStackLayoutAlgorithm mStableLayoutAlgorithm;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableStackBounds = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableWindowRect = new Rect();
    private TaskStack mStack = new TaskStack();
    private boolean mStackActionButtonVisible;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackBounds = new Rect();
    private DropTarget mStackDropTarget = new DropTarget() {
        /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass2 */

        @Override // com.android.systemui.recents.views.DropTarget
        public boolean acceptsDrop(int i, int i2, int i3, int i4, Rect rect, boolean z) {
            if (!z) {
                return TaskStackView.this.mLayoutAlgorithm.mStackRect.contains(i, i2);
            }
            return false;
        }
    };
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mStackReloaded = false;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "scroller_")
    private TaskStackViewScroller mStackScroller;
    private int mStartTimerIndicatorDuration;
    private int mTaskCornerRadiusPx;
    private TaskViewFocusFrame mTaskViewFocusFrame;
    private ArrayList<TaskView> mTaskViews = new ArrayList<>();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mTaskViewsClipDirty = true;
    private int[] mTmpIntPair = new int[2];
    private Rect mTmpRect = new Rect();
    private ArrayMap<Task.TaskKey, TaskView> mTmpTaskViewMap = new ArrayMap<>();
    private List<TaskView> mTmpTaskViews = new ArrayList();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mTouchExplorationEnabled;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private TaskStackViewTouchHandler mTouchHandler;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "doze_")
    private DozeTrigger mUIDozeTrigger;
    private ViewPool<TaskView, Task> mViewPool;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mWindowRect = new Rect();

    public TaskStackView(Context context) {
        super(context);
        int i;
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        Resources resources = context.getResources();
        this.mStack.setCallbacks(this);
        this.mViewPool = new ViewPool<>(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, this);
        this.mStableLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        this.mStackScroller = new TaskStackViewScroller(context, this, this.mLayoutAlgorithm);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mStackScroller);
        this.mAnimationHelper = new TaskStackAnimationHelper(context, this);
        if (LegacyRecentsImpl.getConfiguration().isGridEnabled) {
            i = resources.getDimensionPixelSize(2131166225);
        } else {
            i = resources.getDimensionPixelSize(2131166261);
        }
        this.mTaskCornerRadiusPx = i;
        this.mFastFlingVelocity = (float) resources.getDimensionPixelSize(2131166214);
        this.mDividerSize = systemServices.getDockedDividerSize(context);
        this.mDisplayOrientation = Utilities.getAppConfiguration(((FrameLayout) this).mContext).orientation;
        this.mDisplayRect = systemServices.getDisplayRect();
        this.mStackActionButtonVisible = false;
        if (LegacyRecentsImpl.getConfiguration().isGridEnabled) {
            this.mTaskViewFocusFrame = new TaskViewFocusFrame(((FrameLayout) this).mContext, this, this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm);
            addView(this.mTaskViewFocusFrame);
            getViewTreeObserver().addOnGlobalFocusChangeListener(this.mTaskViewFocusFrame);
        }
        this.mUIDozeTrigger = new DozeTrigger(getResources().getInteger(2131427449), new Runnable() {
            /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass3 */

            public void run() {
                List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                int size = taskViews.size();
                for (int i = 0; i < size; i++) {
                    taskViews.get(i).startNoUserInteractionAnimation();
                }
            }
        });
        setImportantForAccessibility(1);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        EventBus.getDefault().register(this, 3);
        super.onAttachedToWindow();
        readSystemFlags();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    /* access modifiers changed from: package-private */
    public void onReload(boolean z) {
        if (!z) {
            resetFocusedTask(getFocusedTask());
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(getTaskViews());
        arrayList.addAll(this.mViewPool.getViews());
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ((TaskView) arrayList.get(size)).onReload(z);
        }
        readSystemFlags();
        this.mTaskViewsClipDirty = true;
        this.mUIDozeTrigger.stopDozing();
        if (!z) {
            this.mStackScroller.reset();
            this.mStableLayoutAlgorithm.reset();
            this.mLayoutAlgorithm.reset();
            this.mLastScrollPPercent = -1.0f;
        }
        this.mStackReloaded = true;
        this.mFinishedLayoutAfterStackReload = false;
        this.mLaunchNextAfterFirstMeasure = false;
        this.mInitialState = 1;
        requestLayout();
    }

    public void setTasks(TaskStack taskStack, boolean z) {
        this.mStack.setTasks(taskStack, z && this.mLayoutAlgorithm.isInitialized());
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public void updateToInitialState() {
        this.mStackScroller.setStackScrollToInitialState();
        this.mLayoutAlgorithm.setTaskOverridesForInitialState(this.mStack, false);
    }

    /* access modifiers changed from: package-private */
    public void updateTaskViewsList() {
        this.mTaskViews.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof TaskView) {
                this.mTaskViews.add((TaskView) childAt);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public List<TaskView> getTaskViews() {
        return this.mTaskViews;
    }

    private TaskView getFrontMostTaskView() {
        List<TaskView> taskViews = getTaskViews();
        if (taskViews.isEmpty()) {
            return null;
        }
        return taskViews.get(taskViews.size() - 1);
    }

    public TaskView getChildViewForTask(Task task) {
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            if (taskView.getTask() == task) {
                return taskView;
            }
        }
        return null;
    }

    public TaskStackLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    public TaskGridLayoutAlgorithm getGridAlgorithm() {
        return this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm;
    }

    public TaskStackViewTouchHandler getTouchHandler() {
        return this.mTouchHandler;
    }

    /* access modifiers changed from: package-private */
    public void addIgnoreTask(Task task) {
        this.mIgnoreTasks.add(task.key);
    }

    /* access modifiers changed from: package-private */
    public void removeIgnoreTask(Task task) {
        this.mIgnoreTasks.remove(task.key);
    }

    /* access modifiers changed from: package-private */
    public boolean isIgnoredTask(Task task) {
        return this.mIgnoreTasks.contains(task.key);
    }

    /* access modifiers changed from: package-private */
    public int[] computeVisibleTaskTransforms(ArrayList<TaskViewTransform> arrayList, ArrayList<Task> arrayList2, float f, float f2, ArraySet<Task.TaskKey> arraySet, boolean z) {
        boolean z2;
        int size = arrayList2.size();
        int[] iArr = this.mTmpIntPair;
        iArr[0] = -1;
        iArr[1] = -1;
        boolean z3 = Float.compare(f, f2) != 0;
        matchTaskListSize(arrayList2, arrayList);
        TaskViewTransform taskViewTransform = null;
        TaskViewTransform taskViewTransform2 = null;
        TaskViewTransform taskViewTransform3 = null;
        for (int i = size - 1; i >= 0; i--) {
            Task task = arrayList2.get(i);
            TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
            TaskViewTransform taskViewTransform4 = arrayList.get(i);
            taskStackLayoutAlgorithm.getStackTransform(task, f, taskViewTransform4, taskViewTransform, z);
            if (z3 && !taskViewTransform4.visible) {
                TaskStackLayoutAlgorithm taskStackLayoutAlgorithm2 = this.mLayoutAlgorithm;
                TaskViewTransform taskViewTransform5 = new TaskViewTransform();
                taskStackLayoutAlgorithm2.getStackTransform(task, f2, taskViewTransform5, taskViewTransform2);
                if (taskViewTransform5.visible) {
                    taskViewTransform4.copyFrom(taskViewTransform5);
                }
                taskViewTransform3 = taskViewTransform5;
            }
            if (arraySet.contains(task.key)) {
                z2 = true;
            } else {
                if (taskViewTransform4.visible) {
                    if (iArr[0] < 0) {
                        iArr[0] = i;
                    }
                    z2 = true;
                    iArr[1] = i;
                } else {
                    z2 = true;
                }
                taskViewTransform = taskViewTransform4;
                taskViewTransform2 = taskViewTransform3;
            }
        }
        return iArr;
    }

    /* access modifiers changed from: package-private */
    public void bindVisibleTaskViews(float f) {
        bindVisibleTaskViews(f, false);
    }

    /* access modifiers changed from: package-private */
    public void bindVisibleTaskViews(float f, boolean z) {
        int i;
        ArrayList<Task> tasks = this.mStack.getTasks();
        int[] computeVisibleTaskTransforms = computeVisibleTaskTransforms(this.mCurrentTaskTransforms, tasks, this.mStackScroller.getStackScroll(), f, this.mIgnoreTasks, z);
        this.mTmpTaskViewMap.clear();
        List<TaskView> taskViews = getTaskViews();
        int i2 = -1;
        for (int size = taskViews.size() - 1; size >= 0; size--) {
            TaskView taskView = taskViews.get(size);
            Task task = taskView.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                int indexOfTask = this.mStack.indexOfTask(task);
                TaskViewTransform taskViewTransform = null;
                if (indexOfTask != -1) {
                    taskViewTransform = this.mCurrentTaskTransforms.get(indexOfTask);
                }
                if (taskViewTransform == null || !taskViewTransform.visible) {
                    if (this.mTouchExplorationEnabled && Utilities.isDescendentAccessibilityFocused(taskView)) {
                        resetFocusedTask(task);
                        i2 = indexOfTask;
                    }
                    this.mViewPool.returnViewToPool(taskView);
                } else {
                    this.mTmpTaskViewMap.put(task.key, taskView);
                }
            }
        }
        for (int size2 = tasks.size() - 1; size2 >= 0; size2--) {
            Task task2 = tasks.get(size2);
            TaskViewTransform taskViewTransform2 = this.mCurrentTaskTransforms.get(size2);
            if (!this.mIgnoreTasks.contains(task2.key) && taskViewTransform2.visible) {
                TaskView taskView2 = this.mTmpTaskViewMap.get(task2.key);
                if (taskView2 == null) {
                    TaskView pickUpViewFromPool = this.mViewPool.pickUpViewFromPool(task2, task2);
                    float f2 = taskViewTransform2.rect.top;
                    TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
                    if (f2 <= ((float) taskStackLayoutAlgorithm.mStackRect.top)) {
                        updateTaskViewToTransform(pickUpViewFromPool, taskStackLayoutAlgorithm.getBackOfStackTransform(), AnimationProps.IMMEDIATE);
                    } else {
                        updateTaskViewToTransform(pickUpViewFromPool, taskStackLayoutAlgorithm.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
                    }
                } else {
                    int findTaskViewInsertIndex = findTaskViewInsertIndex(task2, this.mStack.indexOfTask(task2));
                    if (findTaskViewInsertIndex != getTaskViews().indexOf(taskView2)) {
                        detachViewFromParent(taskView2);
                        attachViewToParent(taskView2, findTaskViewInsertIndex, taskView2.getLayoutParams());
                        updateTaskViewsList();
                    }
                }
            }
        }
        updatePrefetchingTask(tasks, computeVisibleTaskTransforms[0], computeVisibleTaskTransforms[1]);
        if (i2 != -1) {
            if (i2 < computeVisibleTaskTransforms[1]) {
                i = computeVisibleTaskTransforms[1];
            } else {
                i = computeVisibleTaskTransforms[0];
            }
            setFocusedTask(i, false, true);
            TaskView childViewForTask = getChildViewForTask(this.mFocusedTask);
            if (childViewForTask != null) {
                childViewForTask.requestAccessibilityFocus();
            }
        }
    }

    public void relayoutTaskViews(AnimationProps animationProps) {
        relayoutTaskViews(animationProps, null, false);
    }

    private void relayoutTaskViews(AnimationProps animationProps, ArrayMap<Task, AnimationProps> arrayMap, boolean z) {
        cancelDeferredTaskViewLayoutAnimation();
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), z);
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                TaskViewTransform taskViewTransform = this.mCurrentTaskTransforms.get(this.mStack.indexOfTask(task));
                if (arrayMap != null && arrayMap.containsKey(task)) {
                    animationProps = arrayMap.get(task);
                }
                updateTaskViewToTransform(taskView, taskViewTransform, animationProps);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void relayoutTaskViewsOnNextFrame(AnimationProps animationProps) {
        this.mDeferredTaskViewLayoutAnimation = animationProps;
        invalidate();
    }

    public void updateTaskViewToTransform(TaskView taskView, TaskViewTransform taskViewTransform, AnimationProps animationProps) {
        if (!taskView.isAnimatingTo(taskViewTransform)) {
            taskView.cancelTransformAnimation();
            taskView.updateViewPropertiesToTaskTransform(taskViewTransform, animationProps, this.mRequestUpdateClippingListener);
        }
    }

    public void getCurrentTaskTransforms(ArrayList<Task> arrayList, ArrayList<TaskViewTransform> arrayList2) {
        matchTaskListSize(arrayList, arrayList2);
        int focusState = this.mLayoutAlgorithm.getFocusState();
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            Task task = arrayList.get(size);
            TaskViewTransform taskViewTransform = arrayList2.get(size);
            TaskView childViewForTask = getChildViewForTask(task);
            if (childViewForTask != null) {
                taskViewTransform.fillIn(childViewForTask);
            } else {
                this.mLayoutAlgorithm.getStackTransform(task, this.mStackScroller.getStackScroll(), focusState, taskViewTransform, null, true, false);
            }
            taskViewTransform.visible = true;
        }
    }

    public void getLayoutTaskTransforms(float f, int i, ArrayList<Task> arrayList, boolean z, ArrayList<TaskViewTransform> arrayList2) {
        matchTaskListSize(arrayList, arrayList2);
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            Task task = arrayList.get(size);
            TaskViewTransform taskViewTransform = arrayList2.get(size);
            this.mLayoutAlgorithm.getStackTransform(task, f, i, taskViewTransform, null, true, z);
            taskViewTransform.visible = true;
        }
    }

    /* access modifiers changed from: package-private */
    public void cancelDeferredTaskViewLayoutAnimation() {
        this.mDeferredTaskViewLayoutAnimation = null;
    }

    /* access modifiers changed from: package-private */
    public void cancelAllTaskViewAnimations() {
        List<TaskView> taskViews = getTaskViews();
        for (int size = taskViews.size() - 1; size >= 0; size--) {
            TaskView taskView = taskViews.get(size);
            if (!this.mIgnoreTasks.contains(taskView.getTask().key)) {
                taskView.cancelTransformAnimation();
            }
        }
    }

    private void clipTaskViews() {
        int i;
        TaskView taskView;
        if (!LegacyRecentsImpl.getConfiguration().isGridEnabled) {
            List<TaskView> taskViews = getTaskViews();
            int size = taskViews.size();
            TaskView taskView2 = null;
            int i2 = 0;
            while (i2 < size) {
                TaskView taskView3 = taskViews.get(i2);
                if (isIgnoredTask(taskView3.getTask()) && taskView2 != null) {
                    taskView3.setTranslationZ(Math.max(taskView3.getTranslationZ(), taskView2.getTranslationZ() + 0.1f));
                }
                if (i2 < size - 1 && taskView3.shouldClipViewInStack()) {
                    int i3 = i2 + 1;
                    while (true) {
                        if (i3 >= size) {
                            taskView = null;
                            break;
                        }
                        taskView = taskViews.get(i3);
                        if (taskView.shouldClipViewInStack()) {
                            break;
                        }
                        i3++;
                    }
                    if (taskView != null) {
                        float bottom = (float) taskView3.getBottom();
                        float top = (float) taskView.getTop();
                        if (top < bottom) {
                            i = ((int) (bottom - top)) - this.mTaskCornerRadiusPx;
                            taskView3.getViewBounds().setClipBottom(i);
                            taskView3.mThumbnailView.updateThumbnailVisibility(i - taskView3.getPaddingBottom());
                            i2++;
                            taskView2 = taskView3;
                        }
                    }
                }
                i = 0;
                taskView3.getViewBounds().setClipBottom(i);
                taskView3.mThumbnailView.updateThumbnailVisibility(i - taskView3.getPaddingBottom());
                i2++;
                taskView2 = taskView3;
            }
            this.mTaskViewsClipDirty = false;
        }
    }

    public void updateLayoutAlgorithm(boolean z) {
        updateLayoutAlgorithm(z, LegacyRecentsImpl.getConfiguration().getLaunchState());
    }

    public void updateLayoutAlgorithm(boolean z, RecentsActivityLaunchState recentsActivityLaunchState) {
        this.mLayoutAlgorithm.update(this.mStack, this.mIgnoreTasks, recentsActivityLaunchState, this.mLastScrollPPercent);
        if (z) {
            this.mStackScroller.boundScroll();
        }
    }

    private void updateLayoutToStableBounds() {
        this.mWindowRect.set(this.mStableWindowRect);
        this.mStackBounds.set(this.mStableStackBounds);
        this.mLayoutAlgorithm.setSystemInsets(this.mStableLayoutAlgorithm.mSystemInsets);
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
        updateLayoutAlgorithm(true);
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    public boolean setFocusedTask(int i, boolean z, boolean z2) {
        return setFocusedTask(i, z, z2, 0);
    }

    public boolean setFocusedTask(int i, boolean z, boolean z2, int i2) {
        TaskView childViewForTask;
        boolean z3 = false;
        int clamp = this.mStack.getTaskCount() > 0 ? Utilities.clamp(i, 0, this.mStack.getTaskCount() - 1) : -1;
        Task task = clamp != -1 ? this.mStack.getTasks().get(clamp) : null;
        Task task2 = this.mFocusedTask;
        if (task2 != null) {
            if (i2 > 0 && (childViewForTask = getChildViewForTask(task2)) != null) {
                childViewForTask.getHeaderView().cancelFocusTimerIndicator();
            }
            resetFocusedTask(this.mFocusedTask);
        }
        this.mFocusedTask = task;
        if (task != null) {
            if (i2 > 0) {
                TaskView childViewForTask2 = getChildViewForTask(this.mFocusedTask);
                if (childViewForTask2 != null) {
                    childViewForTask2.getHeaderView().startFocusTimerIndicator(i2);
                } else {
                    this.mStartTimerIndicatorDuration = i2;
                }
            }
            if (z) {
                if (!this.mEnterAnimationComplete) {
                    cancelAllTaskViewAnimations();
                }
                this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
                boolean startScrollToFocusedTaskAnimation = this.mAnimationHelper.startScrollToFocusedTaskAnimation(task, z2);
                if (startScrollToFocusedTaskAnimation) {
                    sendAccessibilityEvent(4096);
                }
                z3 = startScrollToFocusedTaskAnimation;
            } else {
                TaskView childViewForTask3 = getChildViewForTask(task);
                if (childViewForTask3 != null) {
                    childViewForTask3.setFocusedState(true, z2);
                }
            }
            TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
            if (taskViewFocusFrame != null) {
                taskViewFocusFrame.moveGridTaskViewFocus(getChildViewForTask(task));
            }
        }
        return z3;
    }

    public void setRelativeFocusedTask(boolean z, boolean z2, boolean z3) {
        setRelativeFocusedTask(z, z2, z3, false, 0);
    }

    public void setRelativeFocusedTask(boolean z, boolean z2, boolean z3, boolean z4, int i) {
        int i2;
        Task focusedTask = getFocusedTask();
        int indexOfTask = this.mStack.indexOfTask(focusedTask);
        if (focusedTask == null) {
            float stackScroll = this.mStackScroller.getStackScroll();
            ArrayList<Task> tasks = this.mStack.getTasks();
            int size = tasks.size();
            if (useGridLayout()) {
                i2 = size - 1;
            } else if (z) {
                i2 = size - 1;
                while (i2 >= 0 && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks.get(i2)), stackScroll) > 0) {
                    i2--;
                }
            } else {
                i2 = 0;
                while (i2 < size && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks.get(i2)), stackScroll) < 0) {
                    i2++;
                }
            }
        } else if (z2) {
            ArrayList<Task> tasks2 = this.mStack.getTasks();
            i2 = (z ? -1 : 1) + indexOfTask;
            if (i2 < 0 || i2 >= tasks2.size()) {
                i2 = indexOfTask;
            }
        } else {
            int taskCount = this.mStack.getTaskCount();
            i2 = ((indexOfTask + (z ? -1 : 1)) + taskCount) % taskCount;
        }
        if (i2 != -1 && setFocusedTask(i2, true, true, i) && z4) {
            EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
        }
    }

    public void resetFocusedTask(Task task) {
        TaskView childViewForTask;
        if (!(task == null || (childViewForTask = getChildViewForTask(task)) == null)) {
            childViewForTask.setFocusedState(false, false);
        }
        TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
        if (taskViewFocusFrame != null) {
            taskViewFocusFrame.moveGridTaskViewFocus(null);
        }
        this.mFocusedTask = null;
    }

    public Task getFocusedTask() {
        return this.mFocusedTask;
    }

    /* access modifiers changed from: package-private */
    public Task getAccessibilityFocusedTask() {
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            if (Utilities.isDescendentAccessibilityFocused(taskView)) {
                return taskView.getTask();
            }
        }
        TaskView frontMostTaskView = getFrontMostTaskView();
        if (frontMostTaskView != null) {
            return frontMostTaskView.getTask();
        }
        return null;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        List<TaskView> taskViews = getTaskViews();
        int size = taskViews.size();
        if (size > 0) {
            TaskView taskView = taskViews.get(size - 1);
            accessibilityEvent.setFromIndex(this.mStack.indexOfTask(taskViews.get(0).getTask()));
            accessibilityEvent.setToIndex(this.mStack.indexOfTask(taskView.getTask()));
            accessibilityEvent.setContentDescription(taskView.getTask().title);
        }
        accessibilityEvent.setItemCount(this.mStack.getTaskCount());
        float height = (float) this.mLayoutAlgorithm.mStackRect.height();
        accessibilityEvent.setScrollY((int) (this.mStackScroller.getStackScroll() * height));
        accessibilityEvent.setMaxScrollY((int) (this.mLayoutAlgorithm.mMaxScrollP * height));
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (getTaskViews().size() > 1) {
            Task accessibilityFocusedTask = getAccessibilityFocusedTask();
            accessibilityNodeInfo.setScrollable(true);
            int indexOfTask = this.mStack.indexOfTask(accessibilityFocusedTask);
            if (indexOfTask > 0 || !this.mStackActionButtonVisible) {
                accessibilityNodeInfo.addAction(8192);
            }
            if (indexOfTask >= 0 && indexOfTask < this.mStack.getTaskCount() - 1) {
                accessibilityNodeInfo.addAction(4096);
            }
        }
    }

    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }

    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (super.performAccessibilityAction(i, bundle)) {
            return true;
        }
        int indexOfTask = this.mStack.indexOfTask(getAccessibilityFocusedTask());
        if (indexOfTask >= 0 && indexOfTask < this.mStack.getTaskCount()) {
            if (i == 4096) {
                setFocusedTask(indexOfTask + 1, true, true, 0);
                return true;
            } else if (i == 8192) {
                setFocusedTask(indexOfTask - 1, true, true, 0);
                return true;
            }
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onInterceptTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onTouchEvent(motionEvent);
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        return this.mTouchHandler.onGenericMotionEvent(motionEvent);
    }

    public void computeScroll() {
        if (this.mStackScroller.computeScroll()) {
            sendAccessibilityEvent(4096);
            LegacyRecentsImpl.getTaskLoader().getHighResThumbnailLoader().setFlingingFast(this.mStackScroller.getScrollVelocity() > this.mFastFlingVelocity);
        }
        AnimationProps animationProps = this.mDeferredTaskViewLayoutAnimation;
        if (animationProps != null) {
            relayoutTaskViews(animationProps);
            this.mTaskViewsClipDirty = true;
            this.mDeferredTaskViewLayoutAnimation = null;
        }
        if (this.mTaskViewsClipDirty) {
            clipTaskViews();
        }
        float stackScroll = this.mStackScroller.getStackScroll();
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
        this.mLastScrollPPercent = Utilities.clamp(Utilities.unmapRange(stackScroll, taskStackLayoutAlgorithm.mMinScrollP, taskStackLayoutAlgorithm.mMaxScrollP), 0.0f, 1.0f);
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getTasks());
    }

    public void setSystemInsets(Rect rect) {
        boolean systemInsets;
        if (this.mLayoutAlgorithm.setSystemInsets(rect) || (this.mStableLayoutAlgorithm.setSystemInsets(rect) | false)) {
            requestLayout();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        boolean z = true;
        this.mInMeasureLayout = true;
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mLayoutAlgorithm;
        Rect rect = this.mDisplayRect;
        Rect rect2 = new Rect(0, 0, size, size2);
        Rect rect3 = this.mLayoutAlgorithm.mSystemInsets;
        taskStackLayoutAlgorithm.getTaskStackBounds(rect, rect2, rect3.top, rect3.left, rect3.right, this.mTmpRect);
        if (!this.mTmpRect.equals(this.mStableStackBounds)) {
            this.mStableStackBounds.set(this.mTmpRect);
            this.mStackBounds.set(this.mTmpRect);
            this.mStableWindowRect.set(0, 0, size, size2);
            this.mWindowRect.set(0, 0, size, size2);
        }
        this.mStableLayoutAlgorithm.initialize(this.mDisplayRect, this.mStableWindowRect, this.mStableStackBounds);
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
        updateLayoutAlgorithm(false);
        if ((size == this.mLastWidth && size2 == this.mLastHeight) || !this.mResetToInitialStateWhenResized) {
            z = false;
        }
        if (!this.mFinishedLayoutAfterStackReload || this.mInitialState != 0 || z) {
            if (this.mInitialState != 2 || z) {
                updateToInitialState();
                this.mResetToInitialStateWhenResized = false;
            }
            if (this.mFinishedLayoutAfterStackReload) {
                this.mInitialState = 0;
            }
        }
        if (this.mLaunchNextAfterFirstMeasure) {
            this.mLaunchNextAfterFirstMeasure = false;
            EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
        }
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), false);
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int size3 = this.mTmpTaskViews.size();
        for (int i3 = 0; i3 < size3; i3++) {
            measureTaskView(this.mTmpTaskViews.get(i3));
        }
        TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
        if (taskViewFocusFrame != null) {
            taskViewFocusFrame.measure();
        }
        setMeasuredDimension(size, size2);
        this.mLastWidth = size;
        this.mLastHeight = size2;
        this.mInMeasureLayout = false;
    }

    private void measureTaskView(TaskView taskView) {
        Rect rect = new Rect();
        if (taskView.getBackground() != null) {
            taskView.getBackground().getPadding(rect);
        }
        this.mTmpRect.set(this.mStableLayoutAlgorithm.getTaskRect());
        this.mTmpRect.union(this.mLayoutAlgorithm.getTaskRect());
        taskView.measure(View.MeasureSpec.makeMeasureSpec(this.mTmpRect.width() + rect.left + rect.right, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTmpRect.height() + rect.top + rect.bottom, 1073741824));
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int size = this.mTmpTaskViews.size();
        for (int i5 = 0; i5 < size; i5++) {
            layoutTaskView(z, this.mTmpTaskViews.get(i5));
        }
        TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
        if (taskViewFocusFrame != null) {
            taskViewFocusFrame.layout();
        }
        if (z && this.mStackScroller.isScrollOutOfBounds()) {
            this.mStackScroller.boundScroll();
        }
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        clipTaskViews();
        if (!this.mFinishedLayoutAfterStackReload) {
            this.mInitialState = 0;
            onFirstLayout();
            if (this.mStackReloaded) {
                this.mFinishedLayoutAfterStackReload = true;
                tryStartEnterAnimation();
            }
        }
    }

    private void layoutTaskView(boolean z, TaskView taskView) {
        if (z) {
            Rect rect = new Rect();
            if (taskView.getBackground() != null) {
                taskView.getBackground().getPadding(rect);
            }
            this.mTmpRect.set(this.mStableLayoutAlgorithm.getTaskRect());
            this.mTmpRect.union(this.mLayoutAlgorithm.getTaskRect());
            taskView.cancelTransformAnimation();
            Rect rect2 = this.mTmpRect;
            taskView.layout(rect2.left - rect.left, rect2.top - rect.top, rect2.right + rect.right, rect2.bottom + rect.bottom);
            return;
        }
        taskView.layout(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
    }

    /* access modifiers changed from: package-private */
    public void onFirstLayout() {
        int initialFocusTaskIndex;
        this.mAnimationHelper.prepareForEnterAnimation();
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        if ((!useGridLayout() || launchState.launchedWithAltTab) && (initialFocusTaskIndex = getInitialFocusTaskIndex(launchState, this.mStack.getTaskCount(), useGridLayout())) != -1) {
            setFocusedTask(initialFocusTaskIndex, false, false);
        }
        updateStackActionButtonVisibility();
    }

    public boolean isTouchPointInView(float f, float f2, TaskView taskView) {
        this.mTmpRect.set(taskView.getLeft(), taskView.getTop(), taskView.getRight(), taskView.getBottom());
        this.mTmpRect.offset((int) taskView.getTranslationX(), (int) taskView.getTranslationY());
        return this.mTmpRect.contains((int) f, (int) f2);
    }

    public Task findAnchorTask(List<Task> list, MutableBoolean mutableBoolean) {
        for (int size = list.size() - 1; size >= 0; size--) {
            Task task = list.get(size);
            if (!isIgnoredTask(task)) {
                return task;
            }
            if (size == list.size() - 1) {
                mutableBoolean.value = true;
            }
        }
        return null;
    }

    @Override // com.android.systemui.recents.model.TaskStack.TaskStackCallbacks
    public void onStackTaskAdded(TaskStack taskStack, Task task) {
        AnimationProps animationProps;
        updateLayoutAlgorithm(true);
        if (!this.mFinishedLayoutAfterStackReload) {
            animationProps = AnimationProps.IMMEDIATE;
        } else {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        }
        relayoutTaskViews(animationProps);
    }

    @Override // com.android.systemui.recents.model.TaskStack.TaskStackCallbacks
    public void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z, boolean z2) {
        TaskView childViewForTask;
        if (this.mFocusedTask == task) {
            resetFocusedTask(task);
        }
        TaskView childViewForTask2 = getChildViewForTask(task);
        if (childViewForTask2 != null) {
            this.mViewPool.returnViewToPool(childViewForTask2);
        }
        removeIgnoreTask(task);
        if (animationProps != null) {
            updateLayoutAlgorithm(true);
            relayoutTaskViews(animationProps);
        }
        if (!(!this.mScreenPinningEnabled || task2 == null || (childViewForTask = getChildViewForTask(task2)) == null)) {
            childViewForTask.showActionButton(true, 200);
        }
        if (this.mStack.getTaskCount() != 0) {
            return;
        }
        if (z2) {
            EventBus.getDefault().send(new AllTaskViewsDismissedEvent(z ? 2131821943 : 2131821944));
        } else {
            EventBus.getDefault().send(new ShowEmptyViewEvent());
        }
    }

    @Override // com.android.systemui.recents.model.TaskStack.TaskStackCallbacks
    public void onStackTasksRemoved(TaskStack taskStack) {
        resetFocusedTask(getFocusedTask());
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(getTaskViews());
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            this.mViewPool.returnViewToPool((TaskView) arrayList.get(size));
        }
        this.mIgnoreTasks.clear();
        EventBus.getDefault().send(new AllTaskViewsDismissedEvent(2131821944));
    }

    @Override // com.android.systemui.recents.model.TaskStack.TaskStackCallbacks
    public void onStackTasksUpdated(TaskStack taskStack) {
        if (this.mFinishedLayoutAfterStackReload) {
            updateLayoutAlgorithm(false);
            relayoutTaskViews(AnimationProps.IMMEDIATE);
            List<TaskView> taskViews = getTaskViews();
            int size = taskViews.size();
            for (int i = 0; i < size; i++) {
                TaskView taskView = taskViews.get(i);
                bindTaskView(taskView, taskView.getTask());
            }
        }
    }

    @Override // com.android.systemui.recents.views.ViewPool.ViewPoolConsumer
    public TaskView createView(Context context) {
        if (LegacyRecentsImpl.getConfiguration().isGridEnabled) {
            return (GridTaskView) this.mInflater.inflate(2131558738, (ViewGroup) this, false);
        }
        return (TaskView) this.mInflater.inflate(2131558744, (ViewGroup) this, false);
    }

    public void onReturnViewToPool(TaskView taskView) {
        unbindTaskView(taskView, taskView.getTask());
        taskView.clearAccessibilityFocus();
        taskView.resetViewProperties();
        taskView.setFocusedState(false, false);
        taskView.setClipViewInStack(false);
        if (this.mScreenPinningEnabled) {
            taskView.hideActionButton(false, 0, false, null);
        }
        detachViewFromParent(taskView);
        updateTaskViewsList();
    }

    public void onPickUpViewFromPool(TaskView taskView, Task task, boolean z) {
        int findTaskViewInsertIndex = findTaskViewInsertIndex(task, this.mStack.indexOfTask(task));
        if (!z) {
            attachViewToParent(taskView, findTaskViewInsertIndex, taskView.getLayoutParams());
        } else if (this.mInMeasureLayout) {
            addView(taskView, findTaskViewInsertIndex);
        } else {
            ViewGroup.LayoutParams layoutParams = taskView.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = generateDefaultLayoutParams();
            }
            addViewInLayout(taskView, findTaskViewInsertIndex, layoutParams, true);
            measureTaskView(taskView);
            layoutTaskView(true, taskView);
        }
        updateTaskViewsList();
        bindTaskView(taskView, task);
        taskView.setCallbacks(this);
        taskView.setTouchEnabled(true);
        taskView.setClipViewInStack(true);
        if (this.mFocusedTask == task) {
            taskView.setFocusedState(true, false);
            if (this.mStartTimerIndicatorDuration > 0) {
                taskView.getHeaderView().startFocusTimerIndicator(this.mStartTimerIndicatorDuration);
                this.mStartTimerIndicatorDuration = 0;
            }
        }
        if (this.mScreenPinningEnabled && taskView.getTask() == this.mStack.getFrontMostTask()) {
            taskView.showActionButton(false, 0);
        }
    }

    public boolean hasPreferredData(TaskView taskView, Task task) {
        return taskView.getTask() == task;
    }

    private void bindTaskView(TaskView taskView, Task task) {
        taskView.onTaskBound(task, this.mTouchExplorationEnabled, this.mDisplayOrientation, this.mDisplayRect);
        if (this.mUIDozeTrigger.isAsleep() || useGridLayout() || LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            taskView.setNoUserInteractionState();
        }
        if (task == this.mPrefetchingTask) {
            task.notifyTaskDataLoaded(task.thumbnail, task.icon);
        } else {
            LegacyRecentsImpl.getTaskLoader().loadTaskData(task);
        }
        LegacyRecentsImpl.getTaskLoader().getHighResThumbnailLoader().onTaskVisible(task);
    }

    private void unbindTaskView(TaskView taskView, Task task) {
        if (task != this.mPrefetchingTask) {
            LegacyRecentsImpl.getTaskLoader().unloadTaskData(task);
        }
        LegacyRecentsImpl.getTaskLoader().getHighResThumbnailLoader().onTaskInvisible(task);
    }

    private void updatePrefetchingTask(ArrayList<Task> arrayList, int i, int i2) {
        int indexOf;
        Task task = (!(i != -1 && i2 != -1) || i >= arrayList.size() - 1) ? null : arrayList.get(i + 1);
        Task task2 = this.mPrefetchingTask;
        if (task2 != task) {
            if (task2 != null && ((indexOf = arrayList.indexOf(task2)) < i2 || indexOf > i)) {
                LegacyRecentsImpl.getTaskLoader().unloadTaskData(this.mPrefetchingTask);
            }
            this.mPrefetchingTask = task;
            if (task != null) {
                LegacyRecentsImpl.getTaskLoader().loadTaskData(task);
            }
        }
    }

    private void clearPrefetchingTask() {
        if (this.mPrefetchingTask != null) {
            LegacyRecentsImpl.getTaskLoader().unloadTaskData(this.mPrefetchingTask);
        }
        this.mPrefetchingTask = null;
    }

    @Override // com.android.systemui.recents.views.TaskView.TaskViewCallbacks
    public void onTaskViewClipStateChanged(TaskView taskView) {
        if (!this.mTaskViewsClipDirty) {
            this.mTaskViewsClipDirty = true;
            invalidate();
        }
    }

    @Override // com.android.systemui.recents.views.TaskStackLayoutAlgorithm.TaskStackLayoutAlgorithmCallbacks
    public void onFocusStateChanged(int i, int i2) {
        if (this.mDeferredTaskViewLayoutAnimation == null) {
            this.mUIDozeTrigger.poke();
            relayoutTaskViewsOnNextFrame(AnimationProps.IMMEDIATE);
        }
    }

    @Override // com.android.systemui.recents.views.TaskStackViewScroller.TaskStackViewScrollerCallbacks
    public void onStackScrollChanged(float f, float f2, AnimationProps animationProps) {
        this.mUIDozeTrigger.poke();
        if (animationProps != null) {
            relayoutTaskViewsOnNextFrame(animationProps);
        }
        if (this.mEnterAnimationComplete && !useGridLayout() && LegacyRecentsImpl.getConfiguration().isLowRamDevice && this.mStack.getTaskCount() > 0 && !this.mStackActionButtonVisible && this.mTouchHandler.mIsScrolling && f2 - f < 0.0f) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(true));
        }
    }

    public final void onBusEvent(PackagesChangedEvent packagesChangedEvent) {
        ArraySet<ComponentName> computeComponentsRemoved = this.mStack.computeComponentsRemoved(packagesChangedEvent.packageName, packagesChangedEvent.userId);
        ArrayList<Task> tasks = this.mStack.getTasks();
        for (int size = tasks.size() - 1; size >= 0; size--) {
            Task task = tasks.get(size);
            if (computeComponentsRemoved.contains(task.key.getComponent())) {
                TaskView childViewForTask = getChildViewForTask(task);
                if (childViewForTask != null) {
                    childViewForTask.dismissTask();
                } else {
                    this.mStack.removeTask(task, AnimationProps.IMMEDIATE, false);
                }
            }
        }
    }

    public final void onBusEvent(LaunchTaskEvent launchTaskEvent) {
        this.mUIDozeTrigger.stopDozing();
    }

    public final void onBusEvent(LaunchMostRecentTaskRequestEvent launchMostRecentTaskRequestEvent) {
        if (this.mStack.getTaskCount() > 0) {
            lambda$onBusEvent$0$TaskStackView(this.mStack.getFrontMostTask());
        }
    }

    public final void onBusEvent(ShowStackActionButtonEvent showStackActionButtonEvent) {
        this.mStackActionButtonVisible = true;
    }

    public final void onBusEvent(HideStackActionButtonEvent hideStackActionButtonEvent) {
        this.mStackActionButtonVisible = false;
    }

    public final void onBusEvent(LaunchNextTaskRequestEvent launchNextTaskRequestEvent) {
        if (!this.mFinishedLayoutAfterStackReload) {
            this.mLaunchNextAfterFirstMeasure = true;
        } else if (this.mStack.getTaskCount() == 0) {
            if (RecentsImpl.getLastPipTime() != -1) {
                EventBus.getDefault().send(new ExpandPipEvent());
                MetricsLogger.action(getContext(), 318, "pip");
                return;
            }
            EventBus.getDefault().send(new HideRecentsEvent(false, true));
        } else if (LegacyRecentsImpl.getConfiguration().getLaunchState().launchedFromPipApp || !this.mStack.isNextLaunchTargetPip(RecentsImpl.getLastPipTime())) {
            Task nextLaunchTarget = this.mStack.getNextLaunchTarget();
            if (nextLaunchTarget != null) {
                HidePipMenuEvent hidePipMenuEvent = new HidePipMenuEvent();
                hidePipMenuEvent.addPostAnimationCallback(new Runnable(nextLaunchTarget) {
                    /* class com.android.systemui.recents.views.$$Lambda$TaskStackView$eeuGItB18dVOcE3IB2KYHvY1WRM */
                    private final /* synthetic */ Task f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        TaskStackView.this.lambda$onBusEvent$0$TaskStackView(this.f$1);
                    }
                });
                EventBus.getDefault().send(hidePipMenuEvent);
                MetricsLogger.action(getContext(), 318, nextLaunchTarget.key.getComponent().toString());
            }
        } else {
            EventBus.getDefault().send(new ExpandPipEvent());
            MetricsLogger.action(getContext(), 318, "pip");
        }
    }

    public final void onBusEvent(LaunchTaskStartedEvent launchTaskStartedEvent) {
        this.mAnimationHelper.startLaunchTaskAnimation(launchTaskStartedEvent.taskView, launchTaskStartedEvent.screenPinningRequested, launchTaskStartedEvent.getAnimationTrigger());
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted dismissRecentsToHomeAnimationStarted) {
        this.mTouchHandler.cancelNonDismissTaskAnimations();
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        cancelDeferredTaskViewLayoutAnimation();
        this.mAnimationHelper.startExitToHomeAnimation(dismissRecentsToHomeAnimationStarted.animated, dismissRecentsToHomeAnimationStarted.getAnimationTrigger());
        TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
        if (taskViewFocusFrame != null) {
            taskViewFocusFrame.moveGridTaskViewFocus(null);
        }
    }

    public final void onBusEvent(DismissFocusedTaskViewEvent dismissFocusedTaskViewEvent) {
        if (this.mFocusedTask != null) {
            TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
            if (taskViewFocusFrame != null) {
                taskViewFocusFrame.moveGridTaskViewFocus(null);
            }
            TaskView childViewForTask = getChildViewForTask(this.mFocusedTask);
            if (childViewForTask != null) {
                childViewForTask.dismissTask();
            }
            resetFocusedTask(this.mFocusedTask);
        }
    }

    public final void onBusEvent(DismissTaskViewEvent dismissTaskViewEvent) {
        this.mAnimationHelper.startDeleteTaskAnimation(dismissTaskViewEvent.taskView, useGridLayout(), dismissTaskViewEvent.getAnimationTrigger());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent dismissAllTaskViewsEvent) {
        final ArrayList arrayList = new ArrayList(this.mStack.getTasks());
        this.mAnimationHelper.startDeleteAllTasksAnimation(getTaskViews(), useGridLayout(), dismissAllTaskViewsEvent.getAnimationTrigger());
        dismissAllTaskViewsEvent.addPostAnimationCallback(new Runnable() {
            /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass4 */

            public void run() {
                TaskStackView taskStackView = TaskStackView.this;
                taskStackView.announceForAccessibility(taskStackView.getContext().getString(2131820755));
                TaskStackView.this.mStack.removeAllTasks(true);
                for (int size = arrayList.size() - 1; size >= 0; size--) {
                    EventBus.getDefault().send(new DeleteTaskDataEvent((Task) arrayList.get(size)));
                }
                MetricsLogger.action(TaskStackView.this.getContext(), 357);
            }
        });
    }

    public final void onBusEvent(TaskViewDismissedEvent taskViewDismissedEvent) {
        AnimationProps animationProps;
        announceForAccessibility(getContext().getString(2131820756, taskViewDismissedEvent.task.title));
        if (useGridLayout() && (animationProps = taskViewDismissedEvent.animation) != null) {
            animationProps.setListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass5 */

                public void onAnimationEnd(Animator animator) {
                    if (TaskStackView.this.mTaskViewFocusFrame != null) {
                        TaskStackView.this.mTaskViewFocusFrame.resize();
                    }
                }
            });
        }
        this.mStack.removeTask(taskViewDismissedEvent.task, taskViewDismissedEvent.animation, false);
        EventBus.getDefault().send(new DeleteTaskDataEvent(taskViewDismissedEvent.task));
        if (this.mStack.getTaskCount() > 0 && LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            EventBus.getDefault().send(new ShowStackActionButtonEvent(false));
        }
        MetricsLogger.action(getContext(), 289, taskViewDismissedEvent.task.key.getComponent().toString());
    }

    public final void onBusEvent(FocusNextTaskViewEvent focusNextTaskViewEvent) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(true, false, true, false, 0);
    }

    public final void onBusEvent(FocusPreviousTaskViewEvent focusPreviousTaskViewEvent) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(false, false, true);
    }

    public final void onBusEvent(NavigateTaskViewEvent navigateTaskViewEvent) {
        if (useGridLayout()) {
            setFocusedTask(this.mLayoutAlgorithm.mTaskGridLayoutAlgorithm.navigateFocus(this.mStack.getTaskCount(), this.mStack.indexOfTask(getFocusedTask()), navigateTaskViewEvent.direction), false, true);
            return;
        }
        int i = AnonymousClass8.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction[navigateTaskViewEvent.direction.ordinal()];
        if (i == 1) {
            EventBus.getDefault().send(new FocusPreviousTaskViewEvent());
        } else if (i == 2) {
            EventBus.getDefault().send(new FocusNextTaskViewEvent());
        }
    }

    /* renamed from: com.android.systemui.recents.views.TaskStackView$8  reason: invalid class name */
    static /* synthetic */ class AnonymousClass8 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction = new int[NavigateTaskViewEvent.Direction.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(6:0|1|2|3|4|6) */
        /* JADX WARNING: Code restructure failed: missing block: B:7:?, code lost:
            return;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        static {
            /*
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction[] r0 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                com.android.systemui.recents.views.TaskStackView.AnonymousClass8.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction = r0
                int[] r0 = com.android.systemui.recents.views.TaskStackView.AnonymousClass8.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.UP     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = com.android.systemui.recents.views.TaskStackView.AnonymousClass8.$SwitchMap$com$android$systemui$recents$events$ui$focus$NavigateTaskViewEvent$Direction     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent$Direction r1 = com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent.Direction.DOWN     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.views.TaskStackView.AnonymousClass8.<clinit>():void");
        }
    }

    public final void onBusEvent(UserInteractionEvent userInteractionEvent) {
        TaskView childViewForTask;
        this.mUIDozeTrigger.poke();
        LegacyRecentsImpl.getDebugFlags();
        Task task = this.mFocusedTask;
        if (task != null && (childViewForTask = getChildViewForTask(task)) != null) {
            childViewForTask.getHeaderView().cancelFocusTimerIndicator();
        }
    }

    public final void onBusEvent(DragStartEvent dragStartEvent) {
        addIgnoreTask(dragStartEvent.task);
        this.mLayoutAlgorithm.getStackTransform(dragStartEvent.task, getScroller().getStackScroll(), this.mTmpTransform, null);
        TaskViewTransform taskViewTransform = this.mTmpTransform;
        taskViewTransform.scale = dragStartEvent.taskView.getScaleX() * 1.05f;
        taskViewTransform.translationZ = (float) (this.mLayoutAlgorithm.mMaxTranslationZ + 1);
        taskViewTransform.dimAlpha = 0.0f;
        updateTaskViewToTransform(dragStartEvent.taskView, taskViewTransform, new AnimationProps(175, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DragDropTargetChangedEvent dragDropTargetChangedEvent) {
        AnimationProps animationProps = new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN);
        DropTarget dropTarget = dragDropTargetChangedEvent.dropTarget;
        boolean z = true;
        if (dropTarget instanceof DockState) {
            Rect rect = new Rect(this.mStableLayoutAlgorithm.mSystemInsets);
            int measuredHeight = getMeasuredHeight() - rect.bottom;
            rect.bottom = 0;
            this.mStackBounds.set(((DockState) dropTarget).getDockedTaskStackBounds(this.mDisplayRect, getMeasuredWidth(), measuredHeight, this.mDividerSize, rect, this.mLayoutAlgorithm, getResources(), this.mWindowRect));
            this.mLayoutAlgorithm.setSystemInsets(rect);
            this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds);
            updateLayoutAlgorithm(true);
        } else {
            removeIgnoreTask(dragDropTargetChangedEvent.task);
            updateLayoutToStableBounds();
            addIgnoreTask(dragDropTargetChangedEvent.task);
            z = false;
        }
        relayoutTaskViews(animationProps, null, z);
    }

    public final void onBusEvent(DragEndEvent dragEndEvent) {
        if (dragEndEvent.dropTarget instanceof DockState) {
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            return;
        }
        removeIgnoreTask(dragEndEvent.task);
        Utilities.setViewFrameFromTranslation(dragEndEvent.taskView);
        new ArrayMap().put(dragEndEvent.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, dragEndEvent.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        dragEndEvent.getAnimationTrigger().increment();
    }

    public final void onBusEvent(DragEndCancelledEvent dragEndCancelledEvent) {
        removeIgnoreTask(dragEndCancelledEvent.task);
        updateLayoutToStableBounds();
        Utilities.setViewFrameFromTranslation(dragEndCancelledEvent.taskView);
        new ArrayMap().put(dragEndCancelledEvent.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, dragEndCancelledEvent.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        dragEndCancelledEvent.getAnimationTrigger().increment();
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent enterRecentsWindowAnimationCompletedEvent) {
        this.mEnterAnimationComplete = true;
        tryStartEnterAnimation();
    }

    private void tryStartEnterAnimation() {
        if (this.mStackReloaded && this.mFinishedLayoutAfterStackReload && this.mEnterAnimationComplete) {
            if (this.mStack.getTaskCount() > 0) {
                ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
                this.mAnimationHelper.startEnterAnimation(referenceCountedTrigger);
                referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
                    /* class com.android.systemui.recents.views.$$Lambda$TaskStackView$W6l3huwmJgGI98GCJCCawhoJNm0 */

                    public final void run() {
                        TaskStackView.this.lambda$tryStartEnterAnimation$1$TaskStackView();
                    }
                });
            }
            this.mStackReloaded = false;
        }
    }

    public /* synthetic */ void lambda$tryStartEnterAnimation$1$TaskStackView() {
        this.mUIDozeTrigger.startDozing();
        if (this.mFocusedTask != null) {
            setFocusedTask(this.mStack.indexOfTask(this.mFocusedTask), false, LegacyRecentsImpl.getConfiguration().getLaunchState().launchedWithAltTab);
            TaskView childViewForTask = getChildViewForTask(this.mFocusedTask);
            if (this.mTouchExplorationEnabled && childViewForTask != null) {
                childViewForTask.requestAccessibilityFocus();
            }
        }
    }

    public final void onBusEvent(final MultiWindowStateChangedEvent multiWindowStateChangedEvent) {
        if (multiWindowStateChangedEvent.inMultiWindow || !multiWindowStateChangedEvent.showDeferredAnimation) {
            setTasks(multiWindowStateChangedEvent.stack, true);
            return;
        }
        LegacyRecentsImpl.getConfiguration().getLaunchState().reset();
        multiWindowStateChangedEvent.getAnimationTrigger().increment();
        post(new Runnable() {
            /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass6 */

            public void run() {
                TaskStackAnimationHelper taskStackAnimationHelper = TaskStackView.this.mAnimationHelper;
                MultiWindowStateChangedEvent multiWindowStateChangedEvent = multiWindowStateChangedEvent;
                taskStackAnimationHelper.startNewStackScrollAnimation(multiWindowStateChangedEvent.stack, multiWindowStateChangedEvent.getAnimationTrigger());
                multiWindowStateChangedEvent.getAnimationTrigger().decrement();
            }
        });
    }

    public final void onBusEvent(ConfigurationChangedEvent configurationChangedEvent) {
        if (configurationChangedEvent.fromDeviceOrientationChange) {
            this.mDisplayOrientation = Utilities.getAppConfiguration(((FrameLayout) this).mContext).orientation;
            this.mDisplayRect = LegacyRecentsImpl.getSystemServices().getDisplayRect();
            this.mStackScroller.stopScroller();
        }
        reloadOnConfigurationChange();
        if (!configurationChangedEvent.fromMultiWindow) {
            this.mTmpTaskViews.clear();
            this.mTmpTaskViews.addAll(getTaskViews());
            this.mTmpTaskViews.addAll(this.mViewPool.getViews());
            int size = this.mTmpTaskViews.size();
            for (int i = 0; i < size; i++) {
                this.mTmpTaskViews.get(i).onConfigurationChanged();
            }
        }
        updateStackActionButtonVisibility();
        if (configurationChangedEvent.fromMultiWindow && this.mInitialState == 0) {
            this.mInitialState = 2;
            requestLayout();
        } else if (configurationChangedEvent.fromDeviceOrientationChange) {
            this.mInitialState = 1;
            requestLayout();
        }
    }

    public final void onBusEvent(RecentsGrowingEvent recentsGrowingEvent) {
        this.mResetToInitialStateWhenResized = true;
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent recentsVisibilityChangedEvent) {
        if (!recentsVisibilityChangedEvent.visible) {
            TaskViewFocusFrame taskViewFocusFrame = this.mTaskViewFocusFrame;
            if (taskViewFocusFrame != null) {
                taskViewFocusFrame.moveGridTaskViewFocus(null);
            }
            ArrayList arrayList = new ArrayList(getTaskViews());
            for (int i = 0; i < arrayList.size(); i++) {
                this.mViewPool.returnViewToPool((TaskView) arrayList.get(i));
            }
            clearPrefetchingTask();
            this.mEnterAnimationComplete = false;
        }
    }

    public final void onBusEvent(ActivityPinnedEvent activityPinnedEvent) {
        Task findTaskWithId = this.mStack.findTaskWithId(activityPinnedEvent.taskId);
        if (findTaskWithId != null) {
            this.mStack.removeTask(findTaskWithId, AnimationProps.IMMEDIATE, false, false);
        }
        updateLayoutAlgorithm(false);
        updateToInitialState();
    }

    public void reloadOnConfigurationChange() {
        this.mStableLayoutAlgorithm.reloadOnConfigurationChange(getContext());
        this.mLayoutAlgorithm.reloadOnConfigurationChange(getContext());
    }

    private int findTaskViewInsertIndex(Task task, int i) {
        if (i != -1) {
            List<TaskView> taskViews = getTaskViews();
            int size = taskViews.size();
            boolean z = false;
            for (int i2 = 0; i2 < size; i2++) {
                Task task2 = taskViews.get(i2).getTask();
                if (task2 == task) {
                    z = true;
                } else if (i < this.mStack.indexOfTask(task2)) {
                    return z ? i2 - 1 : i2;
                }
            }
        }
        return -1;
    }

    /* access modifiers changed from: private */
    /* renamed from: launchTask */
    public void lambda$onBusEvent$0$TaskStackView(final Task task) {
        cancelAllTaskViewAnimations();
        float stackScroll = this.mStackScroller.getStackScroll();
        float stackScrollForTaskAtInitialOffset = this.mLayoutAlgorithm.getStackScrollForTaskAtInitialOffset(task);
        float abs = Math.abs(stackScrollForTaskAtInitialOffset - stackScroll);
        if (getChildViewForTask(task) == null || abs > 0.35f) {
            this.mStackScroller.animateScroll(stackScrollForTaskAtInitialOffset, (int) ((abs * 32.0f) + 216.0f), new Runnable() {
                /* class com.android.systemui.recents.views.TaskStackView.AnonymousClass7 */

                public void run() {
                    EventBus.getDefault().send(new LaunchTaskEvent(TaskStackView.this.getChildViewForTask(task), task, null, false));
                }
            });
            return;
        }
        EventBus.getDefault().send(new LaunchTaskEvent(getChildViewForTask(task), task, null, false));
    }

    public boolean useGridLayout() {
        return this.mLayoutAlgorithm.useGridLayout();
    }

    private void readSystemFlags() {
        this.mTouchExplorationEnabled = LegacyRecentsImpl.getSystemServices().isTouchExplorationEnabled();
        this.mScreenPinningEnabled = ActivityManagerWrapper.getInstance().isScreenPinningEnabled() && !ActivityManagerWrapper.getInstance().isLockToAppActive();
    }

    private void updateStackActionButtonVisibility() {
        if (!LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            if (useGridLayout() || this.mStack.getTaskCount() > 0) {
                EventBus.getDefault().send(new ShowStackActionButtonEvent(false));
            } else {
                EventBus.getDefault().send(new HideStackActionButtonEvent());
            }
        }
    }

    private int getInitialFocusTaskIndex(RecentsActivityLaunchState recentsActivityLaunchState, int i, boolean z) {
        if (!recentsActivityLaunchState.launchedFromApp) {
            return i - 1;
        }
        if (z) {
            return i - 1;
        }
        return Math.max(0, i - 2);
    }

    private void matchTaskListSize(List<Task> list, List<TaskViewTransform> list2) {
        int size = list2.size();
        int size2 = list.size();
        if (size < size2) {
            while (size < size2) {
                list2.add(new TaskViewTransform());
                size++;
            }
        } else if (size > size2) {
            list2.subList(size2, size).clear();
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        String str2;
        String str3;
        String str4;
        String str5;
        String str6;
        String str7 = str + "  ";
        String hexString = Integer.toHexString(System.identityHashCode(this));
        printWriter.print(str);
        printWriter.print("TaskStackView");
        printWriter.print(" hasDefRelayout=");
        String str8 = "Y";
        printWriter.print(this.mDeferredTaskViewLayoutAnimation != null ? str8 : "N");
        printWriter.print(" clipDirty=");
        if (this.mTaskViewsClipDirty) {
            str2 = str8;
        } else {
            str2 = "N";
        }
        printWriter.print(str2);
        printWriter.print(" awaitingStackReload=");
        if (this.mFinishedLayoutAfterStackReload) {
            str3 = str8;
        } else {
            str3 = "N";
        }
        printWriter.print(str3);
        printWriter.print(" initialState=");
        printWriter.print(this.mInitialState);
        printWriter.print(" inMeasureLayout=");
        if (this.mInMeasureLayout) {
            str4 = str8;
        } else {
            str4 = "N";
        }
        printWriter.print(str4);
        printWriter.print(" enterAnimCompleted=");
        if (this.mEnterAnimationComplete) {
            str5 = str8;
        } else {
            str5 = "N";
        }
        printWriter.print(str5);
        printWriter.print(" touchExplorationOn=");
        if (this.mTouchExplorationEnabled) {
            str6 = str8;
        } else {
            str6 = "N";
        }
        printWriter.print(str6);
        printWriter.print(" screenPinningOn=");
        if (!this.mScreenPinningEnabled) {
            str8 = "N";
        }
        printWriter.print(str8);
        printWriter.print(" numIgnoreTasks=");
        printWriter.print(this.mIgnoreTasks.size());
        printWriter.print(" numViewPool=");
        printWriter.print(this.mViewPool.getViews().size());
        printWriter.print(" stableStackBounds=");
        printWriter.print(Utilities.dumpRect(this.mStableStackBounds));
        printWriter.print(" stackBounds=");
        printWriter.print(Utilities.dumpRect(this.mStackBounds));
        printWriter.print(" stableWindow=");
        printWriter.print(Utilities.dumpRect(this.mStableWindowRect));
        printWriter.print(" window=");
        printWriter.print(Utilities.dumpRect(this.mWindowRect));
        printWriter.print(" display=");
        printWriter.print(Utilities.dumpRect(this.mDisplayRect));
        printWriter.print(" orientation=");
        printWriter.print(this.mDisplayOrientation);
        printWriter.print(" [0x");
        printWriter.print(hexString);
        printWriter.print("]");
        printWriter.println();
        if (this.mFocusedTask != null) {
            printWriter.print(str7);
            printWriter.print("Focused task: ");
            this.mFocusedTask.dump("", printWriter);
        }
        int size = this.mTaskViews.size();
        for (int i = 0; i < size; i++) {
            this.mTaskViews.get(i).dump(str7, printWriter);
        }
        this.mLayoutAlgorithm.dump(str7, printWriter);
        this.mStackScroller.dump(str7, printWriter);
    }
}
