package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.C0008R$integer;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.LegacyRecentsImpl;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.utilities.AnimationProps;
import com.android.systemui.shared.recents.model.Task;
import java.util.ArrayList;
import java.util.List;

public class TaskStackAnimationHelper {
    private static final Interpolator DISMISS_ALL_TRANSLATION_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    private static final Interpolator ENTER_FROM_HOME_ALPHA_INTERPOLATOR = Interpolators.LINEAR;
    private static final Interpolator ENTER_WHILE_DOCKING_INTERPOLATOR;
    private static final Interpolator EXIT_TO_HOME_TRANSLATION_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    private static final Interpolator FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
    private static final Interpolator FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator FOCUS_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
    private final int mEnterAndExitFromHomeTranslationOffset;
    private TaskStackView mStackView;
    private ArrayList<TaskViewTransform> mTmpCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<TaskViewTransform> mTmpFinalTaskTransforms = new ArrayList<>();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    public interface Callbacks {
    }

    static {
        Interpolator interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
        FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR = interpolator;
        ENTER_WHILE_DOCKING_INTERPOLATOR = interpolator;
    }

    public TaskStackAnimationHelper(Context context, TaskStackView taskStackView) {
        this.mStackView = taskStackView;
        this.mEnterAndExitFromHomeTranslationOffset = LegacyRecentsImpl.getConfiguration().isGridEnabled ? 0 : 33;
    }

    public void prepareForEnterAnimation() {
        float f;
        int i;
        float f2;
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        Resources resources = this.mStackView.getResources();
        Resources resources2 = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        Task launchTarget = stack.getLaunchTarget();
        if (stack.getTaskCount() != 0) {
            int height = stackAlgorithm.mStackRect.height();
            resources.getDimensionPixelSize(2131166252);
            int dimensionPixelSize = resources.getDimensionPixelSize(2131166253);
            boolean z = resources2.getConfiguration().orientation == 2;
            boolean z2 = LegacyRecentsImpl.getConfiguration().isLowRamDevice;
            TaskViewTransform taskViewTransform = null;
            if (!z2 || !launchState.launchedFromApp || launchState.launchedViaDockGesture) {
                f = 0.0f;
            } else {
                stackAlgorithm.getStackTransform(launchTarget, scroller.getStackScroll(), this.mTmpTransform, null);
                f = this.mTmpTransform.rect.top;
            }
            List<TaskView> taskViews = this.mStackView.getTaskViews();
            int size = taskViews.size() - 1;
            while (size >= 0) {
                TaskView taskView = taskViews.get(size);
                Task task = taskView.getTask();
                stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, taskViewTransform);
                if (!launchState.launchedFromApp || launchState.launchedViaDockGesture) {
                    i = dimensionPixelSize;
                    if (launchState.launchedFromHome) {
                        if (z2) {
                            f2 = 0.0f;
                            this.mTmpTransform.rect.offset(0.0f, (float) (stackAlgorithm.getTaskRect().height() / 4));
                        } else {
                            f2 = 0.0f;
                            this.mTmpTransform.rect.offset(0.0f, (float) height);
                        }
                        TaskViewTransform taskViewTransform2 = this.mTmpTransform;
                        taskViewTransform2.alpha = f2;
                        this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform2, AnimationProps.IMMEDIATE);
                    } else if (launchState.launchedViaDockGesture) {
                        this.mTmpTransform.rect.offset(0.0f, (float) (z ? i : (int) (((float) height) * 0.9f)));
                        TaskViewTransform taskViewTransform3 = this.mTmpTransform;
                        taskViewTransform3.alpha = 0.0f;
                        this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform3, AnimationProps.IMMEDIATE);
                        size--;
                        dimensionPixelSize = i;
                        taskViewTransform = null;
                    }
                } else {
                    if (task.isLaunchTarget) {
                        taskView.onPrepareLaunchTargetForEnterAnimation();
                    } else if (z2 && size >= taskViews.size() - 10) {
                        stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, taskViewTransform);
                        this.mTmpTransform.rect.offset(0.0f, -f);
                        TaskViewTransform taskViewTransform4 = this.mTmpTransform;
                        taskViewTransform4.alpha = 0.0f;
                        this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform4, AnimationProps.IMMEDIATE);
                        stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, null);
                        TaskViewTransform taskViewTransform5 = this.mTmpTransform;
                        taskViewTransform5.alpha = 1.0f;
                        i = dimensionPixelSize;
                        this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform5, new AnimationProps(336, Interpolators.FAST_OUT_SLOW_IN));
                    }
                    i = dimensionPixelSize;
                }
                size--;
                dimensionPixelSize = i;
                taskViewTransform = null;
            }
        }
    }

    public void startEnterAnimation(ReferenceCountedTrigger referenceCountedTrigger) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        Resources resources = this.mStackView.getResources();
        Resources resources2 = this.mStackView.getContext().getApplicationContext().getResources();
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        stack.getLaunchTarget();
        if (stack.getTaskCount() != 0) {
            boolean z = LegacyRecentsImpl.getConfiguration().isLowRamDevice;
            int integer = resources.getInteger(2131427451);
            resources.getInteger(2131427450);
            int integer2 = resources2.getInteger(C0008R$integer.long_press_dock_anim_duration);
            if (launchState.launchedFromApp && !launchState.launchedViaDockGesture && z) {
                referenceCountedTrigger.addLastDecrementRunnable($$Lambda$TaskStackAnimationHelper$Z1ye5IT0uybrzSDdzPdGEoeTWaY.INSTANCE);
            }
            List<TaskView> taskViews = this.mStackView.getTaskViews();
            int size = taskViews.size();
            int i = size - 1;
            int i2 = i;
            while (i2 >= 0) {
                int i3 = (size - i2) - 1;
                TaskView taskView = taskViews.get(i2);
                Task task = taskView.getTask();
                stackAlgorithm.getStackTransform(task, scroller.getStackScroll(), this.mTmpTransform, null);
                if (!launchState.launchedFromApp || launchState.launchedViaDockGesture) {
                    if (launchState.launchedFromHome) {
                        float min = ((float) (Math.min(5, i3) * this.mEnterAndExitFromHomeTranslationOffset)) / 300.0f;
                        AnimationProps animationProps = new AnimationProps();
                        animationProps.setInterpolator(4, ENTER_FROM_HOME_ALPHA_INTERPOLATOR);
                        animationProps.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                        if (z) {
                            animationProps.setInterpolator(6, Interpolators.FAST_OUT_SLOW_IN);
                            animationProps.setDuration(6, 150);
                            animationProps.setDuration(4, 150);
                        } else {
                            animationProps.setStartDelay(4, Math.min(5, i3) * 16);
                            animationProps.setInterpolator(6, new RecentsEntrancePathInterpolator(0.0f, 0.0f, 0.2f, 1.0f, min));
                            animationProps.setDuration(6, 300);
                            animationProps.setDuration(4, 100);
                        }
                        referenceCountedTrigger.increment();
                        this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, animationProps);
                        if (i2 == i) {
                            taskView.onStartFrontTaskEnterAnimation(this.mStackView.mScreenPinningEnabled);
                        }
                    } else if (launchState.launchedViaDockGesture) {
                        AnimationProps animationProps2 = new AnimationProps();
                        animationProps2.setDuration(6, (i2 * 33) + integer2);
                        animationProps2.setInterpolator(6, ENTER_WHILE_DOCKING_INTERPOLATOR);
                        animationProps2.setStartDelay(6, 48);
                        animationProps2.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                        referenceCountedTrigger.increment();
                        this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, animationProps2);
                    }
                } else if (task.isLaunchTarget) {
                    taskView.onStartLaunchTargetEnterAnimation(this.mTmpTransform, integer, this.mStackView.mScreenPinningEnabled, referenceCountedTrigger);
                }
                i2--;
                taskViews = taskViews;
                scroller = scroller;
            }
        }
    }

    public void startExitToHomeAnimation(boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        AnimationProps animationProps;
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        if (this.mStackView.getStack().getTaskCount() != 0) {
            int height = stackAlgorithm.mStackRect.height();
            List<TaskView> taskViews = this.mStackView.getTaskViews();
            int size = taskViews.size();
            for (int i = 0; i < size; i++) {
                int i2 = (size - i) - 1;
                TaskView taskView = taskViews.get(i);
                if (!this.mStackView.isIgnoredTask(taskView.getTask())) {
                    if (z) {
                        int min = Math.min(5, i2) * this.mEnterAndExitFromHomeTranslationOffset;
                        animationProps = new AnimationProps();
                        animationProps.setDuration(6, 200);
                        animationProps.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                            animationProps.setInterpolator(6, Interpolators.FAST_OUT_SLOW_IN);
                        } else {
                            animationProps.setStartDelay(6, min);
                            animationProps.setInterpolator(6, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR);
                        }
                        referenceCountedTrigger.increment();
                    } else {
                        animationProps = AnimationProps.IMMEDIATE;
                    }
                    this.mTmpTransform.fillIn(taskView);
                    if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
                        animationProps.setInterpolator(4, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR);
                        animationProps.setDuration(4, 200);
                        this.mTmpTransform.rect.offset(0.0f, (float) (stackAlgorithm.mTaskStackLowRamLayoutAlgorithm.getTaskRect().height() / 4));
                        this.mTmpTransform.alpha = 0.0f;
                    } else {
                        this.mTmpTransform.rect.offset(0.0f, (float) height);
                    }
                    this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, animationProps);
                }
            }
        }
    }

    public void startLaunchTaskAnimation(TaskView taskView, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        Resources resources = this.mStackView.getResources();
        int integer = resources.getInteger(2131427452);
        resources.getDimensionPixelSize(2131166252);
        taskView.getTask();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            final TaskView taskView2 = taskViews.get(i);
            taskView2.getTask();
            if (taskView2 == taskView) {
                taskView2.setClipViewInStack(false);
                referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
                    /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass1 */

                    public void run() {
                        taskView2.setClipViewInStack(true);
                    }
                });
                taskView2.onStartLaunchTargetLaunchAnimation(integer, z, referenceCountedTrigger);
            }
        }
    }

    public void startDeleteTaskAnimation(TaskView taskView, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        if (z) {
            startTaskGridDeleteTaskAnimation(taskView, referenceCountedTrigger);
        } else {
            startTaskStackDeleteTaskAnimation(taskView, referenceCountedTrigger);
        }
    }

    public void startDeleteAllTasksAnimation(List<TaskView> list, boolean z, ReferenceCountedTrigger referenceCountedTrigger) {
        if (z) {
            for (int i = 0; i < list.size(); i++) {
                startTaskGridDeleteTaskAnimation(list.get(i), referenceCountedTrigger);
            }
            return;
        }
        startTaskStackDeleteAllTasksAnimation(list, referenceCountedTrigger);
    }

    public boolean startScrollToFocusedTaskAnimation(Task task, boolean z) {
        Interpolator interpolator;
        int i;
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        TaskStack stack = this.mStackView.getStack();
        float stackScroll = scroller.getStackScroll();
        final float boundedStackScroll = scroller.getBoundedStackScroll(stackAlgorithm.getStackScrollForTask(task));
        boolean z2 = boundedStackScroll > stackScroll;
        boolean z3 = Float.compare(boundedStackScroll, stackScroll) != 0;
        int size = this.mStackView.getTaskViews().size();
        ArrayList<Task> tasks = stack.getTasks();
        this.mStackView.getCurrentTaskTransforms(tasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.bindVisibleTaskViews(boundedStackScroll);
        stackAlgorithm.setFocusState(1);
        scroller.setStackScroll(boundedStackScroll, null);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(boundedStackScroll, stackAlgorithm.getFocusState(), tasks, true, this.mTmpFinalTaskTransforms);
        TaskView childViewForTask = this.mStackView.getChildViewForTask(task);
        if (childViewForTask == null) {
            Log.e("TaskStackAnimationHelper", "b/27389156 null-task-view prebind:" + size + " postbind:" + this.mStackView.getTaskViews().size() + " prescroll:" + stackScroll + " postscroll: " + boundedStackScroll);
            return false;
        }
        childViewForTask.setFocusedState(true, z);
        ReferenceCountedTrigger referenceCountedTrigger = new ReferenceCountedTrigger();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
            /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass2 */

            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(boundedStackScroll);
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size2 = taskViews.size();
        int indexOf = taskViews.indexOf(childViewForTask);
        for (int i2 = 0; i2 < size2; i2++) {
            TaskView taskView = taskViews.get(i2);
            Task task2 = taskView.getTask();
            if (!this.mStackView.isIgnoredTask(task2)) {
                int indexOf2 = tasks.indexOf(task2);
                TaskViewTransform taskViewTransform = this.mTmpFinalTaskTransforms.get(indexOf2);
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpCurrentTaskTransforms.get(indexOf2), AnimationProps.IMMEDIATE);
                if (z2) {
                    i = calculateStaggeredAnimDuration(i2);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i2 < indexOf) {
                    i = (((indexOf - i2) - 1) * 50) + 150;
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                } else if (i2 > indexOf) {
                    i = Math.max(100, 150 - (((i2 - indexOf) - 1) * 50));
                    interpolator = FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR;
                } else {
                    i = 200;
                    interpolator = FOCUS_NEXT_TASK_INTERPOLATOR;
                }
                AnimationProps animationProps = new AnimationProps();
                animationProps.setDuration(6, i);
                animationProps.setInterpolator(6, interpolator);
                animationProps.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                referenceCountedTrigger.increment();
                this.mStackView.updateTaskViewToTransform(taskView, taskViewTransform, animationProps);
            }
        }
        return z3;
    }

    public void startNewStackScrollAnimation(TaskStack taskStack, ReferenceCountedTrigger referenceCountedTrigger) {
        TaskStackLayoutAlgorithm stackAlgorithm = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller scroller = this.mStackView.getScroller();
        ArrayList<Task> tasks = taskStack.getTasks();
        this.mStackView.getCurrentTaskTransforms(tasks, this.mTmpCurrentTaskTransforms);
        this.mStackView.setTasks(taskStack, false);
        this.mStackView.updateLayoutAlgorithm(false);
        final float f = stackAlgorithm.mInitialScrollP;
        this.mStackView.bindVisibleTaskViews(f);
        stackAlgorithm.setFocusState(0);
        stackAlgorithm.setTaskOverridesForInitialState(taskStack, true);
        scroller.setStackScroll(f);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(f, stackAlgorithm.getFocusState(), tasks, false, this.mTmpFinalTaskTransforms);
        Task frontMostTask = taskStack.getFrontMostTask();
        final TaskView childViewForTask = this.mStackView.getChildViewForTask(frontMostTask);
        final TaskViewTransform taskViewTransform = this.mTmpFinalTaskTransforms.get(tasks.indexOf(frontMostTask));
        if (childViewForTask != null) {
            this.mStackView.updateTaskViewToTransform(childViewForTask, stackAlgorithm.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
        }
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
            /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass3 */

            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(f);
                if (childViewForTask != null) {
                    TaskStackAnimationHelper.this.mStackView.updateTaskViewToTransform(childViewForTask, taskViewTransform, new AnimationProps(75, 250, TaskStackAnimationHelper.FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR));
                }
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int size = taskViews.size();
        for (int i = 0; i < size; i++) {
            TaskView taskView = taskViews.get(i);
            Task task = taskView.getTask();
            if (!this.mStackView.isIgnoredTask(task) && (task != frontMostTask || childViewForTask == null)) {
                int indexOf = tasks.indexOf(task);
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpCurrentTaskTransforms.get(indexOf), AnimationProps.IMMEDIATE);
                int calculateStaggeredAnimDuration = calculateStaggeredAnimDuration(i);
                Interpolator interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                AnimationProps animationProps = new AnimationProps();
                animationProps.setDuration(6, calculateStaggeredAnimDuration);
                animationProps.setInterpolator(6, interpolator);
                animationProps.setListener(referenceCountedTrigger.decrementOnAnimationEnd());
                referenceCountedTrigger.increment();
                this.mStackView.updateTaskViewToTransform(taskView, this.mTmpFinalTaskTransforms.get(indexOf), animationProps);
            }
        }
    }

    private int calculateStaggeredAnimDuration(int i) {
        return Math.max(100, ((i - 1) * 50) + 100);
    }

    private void startTaskGridDeleteTaskAnimation(TaskView taskView, final ReferenceCountedTrigger referenceCountedTrigger) {
        referenceCountedTrigger.increment();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable(taskView) {
            /* class com.android.systemui.recents.views.$$Lambda$TaskStackAnimationHelper$8n9X8WiqU8WjSQafJipbVZDLA */
            private final /* synthetic */ TaskView f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TaskStackAnimationHelper.this.lambda$startTaskGridDeleteTaskAnimation$1$TaskStackAnimationHelper(this.f$1);
            }
        });
        taskView.animate().setDuration(300).scaleX(0.9f).scaleY(0.9f).alpha(0.0f).setListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass4 */

            public void onAnimationEnd(Animator animator) {
                referenceCountedTrigger.decrement();
            }
        }).start();
    }

    public /* synthetic */ void lambda$startTaskGridDeleteTaskAnimation$1$TaskStackAnimationHelper(TaskView taskView) {
        this.mStackView.getTouchHandler().onChildDismissed(taskView);
    }

    private void startTaskStackDeleteTaskAnimation(TaskView taskView, final ReferenceCountedTrigger referenceCountedTrigger) {
        TaskStackViewTouchHandler touchHandler = this.mStackView.getTouchHandler();
        touchHandler.onBeginManualDrag(taskView);
        referenceCountedTrigger.increment();
        referenceCountedTrigger.addLastDecrementRunnable(new Runnable(taskView) {
            /* class com.android.systemui.recents.views.$$Lambda$TaskStackAnimationHelper$ax6dOg8GHbAwig9kBnwP5_DTcLA */
            private final /* synthetic */ TaskView f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                TaskStackViewTouchHandler.this.onChildDismissed(this.f$1);
            }
        });
        float scaledDismissSize = touchHandler.getScaledDismissSize();
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        ofFloat.setDuration(400L);
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(scaledDismissSize, touchHandler) {
            /* class com.android.systemui.recents.views.$$Lambda$TaskStackAnimationHelper$DBVHlVbyKhFHpm00avfl8nT1DCw */
            private final /* synthetic */ float f$1;
            private final /* synthetic */ TaskStackViewTouchHandler f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                TaskStackAnimationHelper.lambda$startTaskStackDeleteTaskAnimation$3(TaskView.this, this.f$1, this.f$2, valueAnimator);
            }
        });
        ofFloat.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass5 */

            public void onAnimationEnd(Animator animator) {
                referenceCountedTrigger.decrement();
            }
        });
        ofFloat.start();
    }

    static /* synthetic */ void lambda$startTaskStackDeleteTaskAnimation$3(TaskView taskView, float f, TaskStackViewTouchHandler taskStackViewTouchHandler, ValueAnimator valueAnimator) {
        float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        taskView.setTranslationX(f * floatValue);
        taskStackViewTouchHandler.updateSwipeProgress(taskView, true, floatValue);
    }

    private void startTaskStackDeleteAllTasksAnimation(List<TaskView> list, final ReferenceCountedTrigger referenceCountedTrigger) {
        int measuredWidth = this.mStackView.getMeasuredWidth() - this.mStackView.getStackAlgorithm().getTaskRect().left;
        int size = list.size();
        for (int i = size - 1; i >= 0; i--) {
            final TaskView taskView = list.get(i);
            taskView.setClipViewInStack(false);
            AnimationProps animationProps = new AnimationProps(((size - i) - 1) * 33, 200, DISMISS_ALL_TRANSLATION_INTERPOLATOR, new AnimatorListenerAdapter() {
                /* class com.android.systemui.recents.views.TaskStackAnimationHelper.AnonymousClass6 */

                public void onAnimationEnd(Animator animator) {
                    referenceCountedTrigger.decrement();
                    taskView.setClipViewInStack(true);
                }
            });
            referenceCountedTrigger.increment();
            this.mTmpTransform.fillIn(taskView);
            this.mTmpTransform.rect.offset((float) measuredWidth, 0.0f);
            this.mStackView.updateTaskViewToTransform(taskView, this.mTmpTransform, animationProps);
        }
    }
}
