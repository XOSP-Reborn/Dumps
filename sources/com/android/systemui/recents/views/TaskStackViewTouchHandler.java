package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.MutableBoolean;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.SwipeHelper;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.util.ArrayList;
import java.util.List;

/* access modifiers changed from: package-private */
public class TaskStackViewTouchHandler implements SwipeHelper.Callback {
    private static final Interpolator OVERSCROLL_INTERP;
    int mActivePointerId = -1;
    TaskView mActiveTaskView = null;
    Context mContext;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<Task> mCurrentTasks = new ArrayList<>();
    float mDownScrollP;
    int mDownX;
    int mDownY;
    private ArrayList<TaskViewTransform> mFinalTaskTransforms = new ArrayList<>();
    FlingAnimationUtils mFlingAnimUtils;
    boolean mInterceptedBySwipeHelper;
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mIsScrolling;
    int mLastY;
    int mMaximumVelocity;
    int mMinimumVelocity;
    int mOverscrollSize;
    ValueAnimator mScrollFlingAnimator;
    int mScrollTouchSlop;
    TaskStackViewScroller mScroller;
    private final StackViewScrolledEvent mStackViewScrolledEvent = new StackViewScrolledEvent();
    TaskStackView mSv;
    SwipeHelper mSwipeHelper;
    private ArrayMap<View, Animator> mSwipeHelperAnimations = new ArrayMap<>();
    private float mTargetStackScroll;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    VelocityTracker mVelocityTracker;
    final int mWindowTouchSlop;

    @Override // com.android.systemui.SwipeHelper.Callback
    public float getFalsingThresholdFactor() {
        return 0.0f;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onDragCancelled(View view) {
    }

    static {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(0.2f, 0.175f, 0.25f, 0.3f, 1.0f, 0.3f);
        OVERSCROLL_INTERP = new FreePathInterpolator(path);
    }

    public TaskStackViewTouchHandler(Context context, TaskStackView taskStackView, TaskStackViewScroller taskStackViewScroller) {
        Resources resources = context.getResources();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mContext = context;
        this.mSv = taskStackView;
        this.mScroller = taskStackViewScroller;
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mWindowTouchSlop = viewConfiguration.getScaledWindowTouchSlop();
        this.mFlingAnimUtils = new FlingAnimationUtils(context, 0.2f);
        this.mOverscrollSize = resources.getDimensionPixelSize(2131166215);
        this.mSwipeHelper = new SwipeHelper(0, this, context) {
            /* class com.android.systemui.recents.views.TaskStackViewTouchHandler.AnonymousClass1 */

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.SwipeHelper
            public long getMaxEscapeAnimDuration() {
                return 700;
            }

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.SwipeHelper
            public float getUnscaledEscapeVelocity() {
                return 800.0f;
            }

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.SwipeHelper
            public float getSize(View view) {
                return TaskStackViewTouchHandler.this.getScaledDismissSize();
            }

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.SwipeHelper
            public void prepareDismissAnimation(View view, Animator animator) {
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(view, animator);
            }

            /* access modifiers changed from: protected */
            @Override // com.android.systemui.SwipeHelper
            public void prepareSnapBackAnimation(View view, Animator animator) {
                animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(view, animator);
            }
        };
        this.mSwipeHelper.setDisableHardwareLayers(true);
    }

    /* access modifiers changed from: package-private */
    public void initOrResetVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    /* access modifiers changed from: package-private */
    public void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        this.mInterceptedBySwipeHelper = isSwipingEnabled() && this.mSwipeHelper.onInterceptTouchEvent(motionEvent);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        return handleTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(motionEvent)) {
            return true;
        }
        handleTouchEvent(motionEvent);
        return true;
    }

    public void cancelNonDismissTaskAnimations() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollFlingAnimator);
        if (!this.mSwipeHelperAnimations.isEmpty()) {
            List<TaskView> taskViews = this.mSv.getTaskViews();
            for (int size = taskViews.size() - 1; size >= 0; size--) {
                TaskView taskView = taskViews.get(size);
                if (!this.mSv.isIgnoredTask(taskView.getTask())) {
                    taskView.cancelTransformAnimation();
                    this.mSv.getStackAlgorithm().addUnfocusedTaskOverride(taskView, this.mTargetStackScroll);
                }
            }
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
            this.mSwipeHelperAnimations.clear();
        }
        this.mActiveTaskView = null;
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        int i = 0;
        if (this.mSv.getTaskViews().size() == 0) {
            return false;
        }
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = this.mSv.mLayoutAlgorithm;
        int action = motionEvent.getAction() & 255;
        if (action == 0) {
            this.mScroller.stopScroller();
            this.mScroller.stopBoundScrollAnimation();
            this.mScroller.resetDeltaScroll();
            cancelNonDismissTaskAnimations();
            this.mSv.cancelDeferredTaskViewLayoutAnimation();
            this.mDownX = (int) motionEvent.getX();
            this.mDownY = (int) motionEvent.getY();
            this.mLastY = this.mDownY;
            this.mDownScrollP = this.mScroller.getStackScroll();
            this.mActivePointerId = motionEvent.getPointerId(0);
            this.mActiveTaskView = findViewAtPoint(this.mDownX, this.mDownY);
            initOrResetVelocityTracker();
            this.mVelocityTracker.addMovement(motionEvent);
        } else if (action == 1) {
            this.mVelocityTracker.addMovement(motionEvent);
            this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
            int y = (int) motionEvent.getY(motionEvent.findPointerIndex(this.mActivePointerId));
            int yVelocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
            if (this.mIsScrolling) {
                if (this.mScroller.isScrollOutOfBounds()) {
                    this.mScroller.animateBoundScroll();
                } else if (Math.abs(yVelocity) > this.mMinimumVelocity && !LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                    this.mScroller.fling(this.mDownScrollP, this.mDownY, y, yVelocity, (int) ((float) (this.mDownY + taskStackLayoutAlgorithm.getYForDeltaP(this.mDownScrollP, taskStackLayoutAlgorithm.mMaxScrollP))), (int) ((float) (this.mDownY + taskStackLayoutAlgorithm.getYForDeltaP(this.mDownScrollP, taskStackLayoutAlgorithm.mMinScrollP))), this.mOverscrollSize);
                    this.mSv.invalidate();
                }
                TaskStackView taskStackView = this.mSv;
                if (!taskStackView.mTouchExplorationEnabled && !taskStackView.useGridLayout()) {
                    if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                        this.mScroller.scrollToClosestTask(yVelocity);
                    } else {
                        TaskStackView taskStackView2 = this.mSv;
                        taskStackView2.resetFocusedTask(taskStackView2.getFocusedTask());
                    }
                }
            } else if (this.mActiveTaskView == null) {
                maybeHideRecentsFromBackgroundTap((int) motionEvent.getX(), (int) motionEvent.getY());
            }
            this.mActivePointerId = -1;
            this.mIsScrolling = false;
            recycleVelocityTracker();
        } else if (action == 2) {
            int findPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
            if (findPointerIndex != -1) {
                int y2 = (int) motionEvent.getY(findPointerIndex);
                int x = (int) motionEvent.getX(findPointerIndex);
                if (!this.mIsScrolling) {
                    int abs = Math.abs(y2 - this.mDownY);
                    int abs2 = Math.abs(x - this.mDownX);
                    if (Math.abs(y2 - this.mDownY) > this.mScrollTouchSlop && abs > abs2) {
                        this.mIsScrolling = true;
                        float stackScroll = this.mScroller.getStackScroll();
                        List<TaskView> taskViews = this.mSv.getTaskViews();
                        for (int size = taskViews.size() - 1; size >= 0; size--) {
                            taskStackLayoutAlgorithm.addUnfocusedTaskOverride(taskViews.get(size).getTask(), stackScroll);
                        }
                        taskStackLayoutAlgorithm.setFocusState(0);
                        ViewParent parent = this.mSv.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        MetricsLogger.action(this.mSv.getContext(), 287);
                        this.mDownY = y2;
                        this.mLastY = y2;
                    }
                }
                if (this.mIsScrolling) {
                    float deltaPForY = taskStackLayoutAlgorithm.getDeltaPForY(this.mDownY, y2);
                    float f = taskStackLayoutAlgorithm.mMinScrollP;
                    float f2 = taskStackLayoutAlgorithm.mMaxScrollP;
                    float f3 = this.mDownScrollP + deltaPForY;
                    if (f3 < f || f3 > f2) {
                        float clamp = Utilities.clamp(f3, f, f2);
                        float f4 = f3 - clamp;
                        float maxOverscroll = LegacyRecentsImpl.getConfiguration().isLowRamDevice ? taskStackLayoutAlgorithm.mTaskStackLowRamLayoutAlgorithm.getMaxOverscroll() : 1.0f;
                        f3 = clamp + (Math.signum(f4) * OVERSCROLL_INTERP.getInterpolation(Math.abs(f4) / maxOverscroll) * maxOverscroll);
                    }
                    float f5 = this.mDownScrollP;
                    this.mDownScrollP = f5 + this.mScroller.setDeltaStackScroll(f5, f3 - f5);
                    this.mStackViewScrolledEvent.updateY(y2 - this.mLastY);
                    EventBus.getDefault().send(this.mStackViewScrolledEvent);
                }
                this.mLastY = y2;
                this.mVelocityTracker.addMovement(motionEvent);
            }
        } else if (action == 3) {
            this.mActivePointerId = -1;
            this.mIsScrolling = false;
            recycleVelocityTracker();
        } else if (action == 5) {
            int actionIndex = motionEvent.getActionIndex();
            this.mActivePointerId = motionEvent.getPointerId(actionIndex);
            this.mDownX = (int) motionEvent.getX(actionIndex);
            this.mDownY = (int) motionEvent.getY(actionIndex);
            this.mLastY = this.mDownY;
            this.mDownScrollP = this.mScroller.getStackScroll();
            this.mScroller.resetDeltaScroll();
            this.mVelocityTracker.addMovement(motionEvent);
        } else if (action == 6) {
            int actionIndex2 = motionEvent.getActionIndex();
            if (motionEvent.getPointerId(actionIndex2) == this.mActivePointerId) {
                if (actionIndex2 == 0) {
                    i = 1;
                }
                this.mActivePointerId = motionEvent.getPointerId(i);
                this.mDownX = (int) motionEvent.getX(actionIndex2);
                this.mDownY = (int) motionEvent.getY(actionIndex2);
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
            }
            this.mVelocityTracker.addMovement(motionEvent);
        }
        return this.mIsScrolling;
    }

    /* access modifiers changed from: package-private */
    public void maybeHideRecentsFromBackgroundTap(int i, int i2) {
        int i3;
        int abs = Math.abs(this.mDownX - i);
        int abs2 = Math.abs(this.mDownY - i2);
        int i4 = this.mScrollTouchSlop;
        if (abs <= i4 && abs2 <= i4) {
            if (i > (this.mSv.getRight() - this.mSv.getLeft()) / 2) {
                i3 = i - this.mWindowTouchSlop;
            } else {
                i3 = this.mWindowTouchSlop + i;
            }
            if (findViewAtPoint(i3, i2) == null) {
                Rect rect = this.mSv.mLayoutAlgorithm.mStackRect;
                if (i <= rect.left || i >= rect.right) {
                    EventBus.getDefault().send(new HideRecentsEvent(false, true));
                }
            }
        }
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if ((motionEvent.getSource() & 2) != 2 || (motionEvent.getAction() & 255) != 8) {
            return false;
        }
        if (motionEvent.getAxisValue(9) > 0.0f) {
            this.mSv.setRelativeFocusedTask(true, true, false);
        } else {
            this.mSv.setRelativeFocusedTask(false, true, false);
        }
        return true;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public View getChildAtPosition(MotionEvent motionEvent) {
        TaskView findViewAtPoint = findViewAtPoint((int) motionEvent.getX(), (int) motionEvent.getY());
        if (findViewAtPoint == null || !canChildBeDismissed(findViewAtPoint)) {
            return null;
        }
        return findViewAtPoint;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean canChildBeDismissed(View view) {
        return !this.mSwipeHelperAnimations.containsKey(view) && this.mSv.getStack().indexOfTask(((TaskView) view).getTask()) != -1;
    }

    public void onBeginManualDrag(TaskView taskView) {
        this.mActiveTaskView = taskView;
        this.mSwipeHelperAnimations.put(taskView, null);
        onBeginDrag(taskView);
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onBeginDrag(View view) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(false);
        taskView.setTouchEnabled(false);
        ViewParent parent = this.mSv.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        this.mSv.addIgnoreTask(taskView.getTask());
        this.mCurrentTasks = new ArrayList<>(this.mSv.getStack().getTasks());
        MutableBoolean mutableBoolean = new MutableBoolean(false);
        Task findAnchorTask = this.mSv.findAnchorTask(this.mCurrentTasks, mutableBoolean);
        TaskStackLayoutAlgorithm stackAlgorithm = this.mSv.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mSv.getScroller();
        if (findAnchorTask != null) {
            this.mSv.getCurrentTaskTransforms(this.mCurrentTasks, this.mCurrentTaskTransforms);
            float f = 0.0f;
            boolean z = this.mCurrentTasks.size() > 0;
            if (z) {
                if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                    f = this.mSv.getStackAlgorithm().mTaskStackLowRamLayoutAlgorithm.getScrollPForTask((int) stackAlgorithm.getStackScrollForTask(findAnchorTask));
                } else {
                    f = stackAlgorithm.getStackScrollForTask(findAnchorTask);
                }
            }
            this.mSv.updateLayoutAlgorithm(false);
            float stackScroll = scroller.getStackScroll();
            if (mutableBoolean.value) {
                stackScroll = scroller.getBoundedStackScroll(stackScroll);
            } else if (z) {
                float stackScrollForTaskIgnoreOverrides = stackAlgorithm.getStackScrollForTaskIgnoreOverrides(findAnchorTask);
                if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                    stackScrollForTaskIgnoreOverrides = this.mSv.getStackAlgorithm().mTaskStackLowRamLayoutAlgorithm.getScrollPForTask((int) stackAlgorithm.getStackScrollForTask(findAnchorTask));
                }
                float f2 = stackScrollForTaskIgnoreOverrides - f;
                if (stackAlgorithm.getFocusState() != 1 && !LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                    f2 *= 0.75f;
                }
                stackScroll = scroller.getBoundedStackScroll(scroller.getStackScroll() + f2);
            }
            this.mSv.bindVisibleTaskViews(stackScroll, true);
            this.mSv.getLayoutTaskTransforms(stackScroll, 0, this.mCurrentTasks, true, this.mFinalTaskTransforms);
            this.mTargetStackScroll = stackScroll;
        }
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public boolean updateSwipeProgress(View view, boolean z, float f) {
        if ((this.mActiveTaskView != view && !this.mSwipeHelperAnimations.containsKey(view)) || LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            return true;
        }
        updateTaskViewTransforms(Interpolators.FAST_OUT_SLOW_IN.getInterpolation(f));
        return true;
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onChildDismissed(View view) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(true);
        taskView.setTouchEnabled(true);
        AnimationProps animationProps = null;
        if (this.mSwipeHelperAnimations.containsKey(view)) {
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
        }
        EventBus eventBus = EventBus.getDefault();
        Task task = taskView.getTask();
        if (this.mSwipeHelperAnimations.containsKey(view)) {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        }
        eventBus.send(new TaskViewDismissedEvent(task, taskView, animationProps));
        if (this.mSwipeHelperAnimations.containsKey(view)) {
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getStackAlgorithm().clearUnfocusedTaskOverrides();
            this.mSwipeHelperAnimations.remove(view);
        }
        MetricsLogger.histogram(taskView.getContext(), "overview_task_dismissed_source", 1);
    }

    @Override // com.android.systemui.SwipeHelper.Callback
    public void onChildSnappedBack(View view, float f) {
        TaskView taskView = (TaskView) view;
        taskView.setClipViewInStack(true);
        taskView.setTouchEnabled(true);
        this.mSv.removeIgnoreTask(taskView.getTask());
        this.mSv.updateLayoutAlgorithm(false);
        this.mSv.relayoutTaskViews(AnimationProps.IMMEDIATE);
        this.mSwipeHelperAnimations.remove(view);
    }

    private void updateTaskViewTransforms(float f) {
        int indexOf;
        List<TaskView> taskViews = this.mSv.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mSv.isIgnoredTask(task) && (indexOf = this.mCurrentTasks.indexOf(task)) != -1) {
                TaskViewTransform taskViewTransform = this.mCurrentTaskTransforms.get(indexOf);
                TaskViewTransform taskViewTransform2 = this.mFinalTaskTransforms.get(indexOf);
                this.mTmpTransform.copyFrom(taskViewTransform);
                this.mTmpTransform.rect.set(Utilities.RECTF_EVALUATOR.evaluate(f, taskViewTransform.rect, taskViewTransform2.rect));
                TaskViewTransform taskViewTransform3 = this.mTmpTransform;
                float f2 = taskViewTransform.dimAlpha;
                taskViewTransform3.dimAlpha = f2 + ((taskViewTransform2.dimAlpha - f2) * f);
                float f3 = taskViewTransform.viewOutlineAlpha;
                taskViewTransform3.viewOutlineAlpha = f3 + ((taskViewTransform2.viewOutlineAlpha - f3) * f);
                float f4 = taskViewTransform.translationZ;
                taskViewTransform3.translationZ = f4 + ((taskViewTransform2.translationZ - f4) * f);
                this.mSv.updateTaskViewToTransform(taskView, taskViewTransform3, AnimationProps.IMMEDIATE);
            }
        }
    }

    private TaskView findViewAtPoint(int i, int i2) {
        ArrayList<Task> tasks = this.mSv.getStack().getTasks();
        for (int size = tasks.size() - 1; size >= 0; size--) {
            TaskView childViewForTask = this.mSv.getChildViewForTask(tasks.get(size));
            if (childViewForTask != null && childViewForTask.getVisibility() == 0 && this.mSv.isTouchPointInView((float) i, (float) i2, childViewForTask)) {
                return childViewForTask;
            }
        }
        return null;
    }

    public float getScaledDismissSize() {
        return ((float) Math.max(this.mSv.getWidth(), this.mSv.getHeight())) * 1.5f;
    }

    private boolean isSwipingEnabled() {
        return !this.mSv.useGridLayout();
    }
}
