package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.trust.TrustManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.pip.phone.ForegroundThread;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchMostRecentTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.ActivityPinnedEvent;
import com.android.systemui.recents.events.component.ActivityUnpinnedEvent;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.events.ui.TaskSnapshotChangedEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskViewHeader;
import com.android.systemui.recents.views.TaskViewTransform;
import com.android.systemui.recents.views.grid.TaskGridLayoutAlgorithm;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecCompat;
import com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture;
import com.android.systemui.shared.recents.view.RecentsTransition;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.phone.StatusBar;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class RecentsImpl implements ActivityOptions.OnAnimationFinishedListener {
    private static final ArraySet<Task.TaskKey> EMPTY_SET = new ArraySet<>();
    private static boolean mToggleFollowingTransitionStart = true;
    private static boolean mWaitingForTransitionStart = false;
    protected static RecentsTaskLoadPlan sInstanceLoadPlan;
    protected static long sLastPipTime = -1;
    private TaskStackLayoutAlgorithm mBackgroundLayoutAlgorithm;
    protected Context mContext;
    boolean mDraggingInRecents;
    private TaskStackView mDummyStackView;
    private final TaskStack mEmptyTaskStack = new TaskStack();
    DozeTrigger mFastAltTabTrigger = new DozeTrigger(225, new Runnable() {
        /* class com.android.systemui.recents.RecentsImpl.AnonymousClass2 */

        public void run() {
            RecentsImpl recentsImpl = RecentsImpl.this;
            recentsImpl.showRecents(recentsImpl.mTriggeredFromAltTab, false, true, -1);
        }
    });
    protected Handler mHandler;
    TaskViewHeader mHeaderBar;
    final Object mHeaderBarLock = new Object();
    protected long mLastToggleTime;
    boolean mLaunchedWhileDocking;
    private OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
        /* class com.android.systemui.recents.RecentsImpl.AnonymousClass3 */

        @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
        public void onConnectionChanged(boolean z) {
            if (!z) {
                LegacyRecentsImpl.getTaskLoader().onTrimMemory(80);
            }
        }
    };
    private Runnable mResetToggleFlagListener = new Runnable() {
        /* class com.android.systemui.recents.RecentsImpl.AnonymousClass1 */

        public void run() {
            RecentsImpl.this.setWaitingForTransitionStart(false);
        }
    };
    int mTaskBarHeight;
    TaskStackListenerImpl mTaskStackListener;
    Rect mTmpBounds = new Rect();
    TaskViewTransform mTmpTransform = new TaskViewTransform();
    protected boolean mTriggeredFromAltTab;
    private TrustManager mTrustManager;

    public void cancelPreloadingRecents() {
    }

    class TaskStackListenerImpl extends SysUiTaskStackChangeListener {
        private OverviewProxyService mOverviewProxyService = ((OverviewProxyService) Dependency.get(OverviewProxyService.class));

        public TaskStackListenerImpl() {
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskStackChangedBackground() {
            if (!this.mOverviewProxyService.isEnabled() && checkCurrentUserId(RecentsImpl.this.mContext, false) && LegacyRecentsImpl.getConfiguration().svelteLevel == 0) {
                Rect windowRect = RecentsImpl.this.getWindowRect(null);
                if (!windowRect.isEmpty()) {
                    ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
                    RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
                    RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(RecentsImpl.this.mContext);
                    int i = -1;
                    taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
                    TaskStack taskStack = recentsTaskLoadPlan.getTaskStack();
                    RecentsActivityLaunchState recentsActivityLaunchState = new RecentsActivityLaunchState();
                    RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
                    synchronized (RecentsImpl.this.mBackgroundLayoutAlgorithm) {
                        RecentsImpl.this.updateDummyStackViewLayout(RecentsImpl.this.mBackgroundLayoutAlgorithm, taskStack, windowRect);
                        recentsActivityLaunchState.launchedFromApp = true;
                        RecentsImpl.this.mBackgroundLayoutAlgorithm.update(recentsTaskLoadPlan.getTaskStack(), RecentsImpl.EMPTY_SET, recentsActivityLaunchState, -1.0f);
                        TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport = RecentsImpl.this.mBackgroundLayoutAlgorithm.computeStackVisibilityReport(taskStack.getTasks());
                        if (runningTask != null) {
                            i = runningTask.id;
                        }
                        options.runningTaskId = i;
                        options.numVisibleTasks = computeStackVisibilityReport.numVisibleTasks;
                        options.numVisibleTaskThumbnails = computeStackVisibilityReport.numVisibleThumbnails;
                        options.onlyLoadForCache = true;
                        options.onlyLoadPausedActivities = true;
                        options.loadThumbnails = true;
                    }
                    taskLoader.loadTasks(recentsTaskLoadPlan, options);
                }
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityPinned(String str, int i, int i2, int i3) {
            if (checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                LegacyRecentsImpl.getConfiguration().getLaunchState().launchedFromPipApp = true;
                LegacyRecentsImpl.getConfiguration().getLaunchState().launchedWithNextPipApp = false;
                EventBus.getDefault().send(new ActivityPinnedEvent(i2));
                RecentsImpl.consumeInstanceLoadPlan();
                RecentsImpl.sLastPipTime = System.currentTimeMillis();
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityUnpinned() {
            if (checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                EventBus.getDefault().send(new ActivityUnpinnedEvent());
                RecentsImpl.sLastPipTime = -1;
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskSnapshotChanged(int i, ThumbnailData thumbnailData) {
            if (checkCurrentUserId(RecentsImpl.this.mContext, false)) {
                EventBus.getDefault().send(new TaskSnapshotChangedEvent(i, thumbnailData));
            }
        }
    }

    public RecentsImpl(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mBackgroundLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        ForegroundThread.get();
        this.mTaskStackListener = new TaskStackListenerImpl();
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mDummyStackView = new TaskStackView(this.mContext);
        reloadResources();
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
    }

    public void onBootCompleted() {
        if (!((OverviewProxyService) Dependency.get(OverviewProxyService.class)).isEnabled()) {
            RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
            RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this.mContext);
            taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
            RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
            options.numVisibleTasks = taskLoader.getIconCacheSize();
            options.numVisibleTaskThumbnails = taskLoader.getThumbnailCacheSize();
            options.onlyLoadForCache = true;
            taskLoader.loadTasks(recentsTaskLoadPlan, options);
        }
    }

    public void onConfigurationChanged() {
        reloadResources();
        this.mDummyStackView.reloadOnConfigurationChange();
        synchronized (this.mBackgroundLayoutAlgorithm) {
            this.mBackgroundLayoutAlgorithm.reloadOnConfigurationChange(this.mContext);
        }
    }

    public void onVisibilityChanged(Context context, boolean z) {
        LegacyRecentsImpl.getSystemServices().setRecentsVisibility(z);
    }

    public void onStartScreenPinning(Context context, int i) {
        StatusBar statusBar = getStatusBar();
        if (statusBar != null) {
            statusBar.showScreenPinningRequest(i, false);
        }
    }

    public void showRecents(boolean z, boolean z2, boolean z3, int i) {
        LegacyRecentsImpl.getSystemServices();
        boolean z4 = true;
        MutableBoolean mutableBoolean = new MutableBoolean(true);
        boolean isRecentsActivityVisible = LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible(mutableBoolean);
        boolean z5 = mutableBoolean.value;
        boolean z6 = LegacyRecentsImpl.getSystemServices().getSplitScreenPrimaryStack() != null;
        this.mTriggeredFromAltTab = z;
        this.mDraggingInRecents = z2;
        this.mLaunchedWhileDocking = z6;
        if (this.mFastAltTabTrigger.isAsleep()) {
            this.mFastAltTabTrigger.stopDozing();
        } else if (this.mFastAltTabTrigger.isDozing()) {
            if (z) {
                this.mFastAltTabTrigger.stopDozing();
            } else {
                return;
            }
        } else if (z) {
            this.mFastAltTabTrigger.startDozing();
            return;
        }
        if ((z6 || z2) || !isRecentsActivityVisible) {
            try {
                ActivityManager.RunningTaskInfo runningTask = ActivityManagerWrapper.getInstance().getRunningTask();
                if (!mutableBoolean.value) {
                    if (!z5) {
                        z4 = false;
                    }
                }
                startRecentsActivityAndDismissKeyguardIfNeeded(runningTask, z4, z3, i);
            } catch (ActivityNotFoundException e) {
                Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
            }
        }
    }

    public void hideRecents(boolean z, boolean z2) {
        if (!z || !this.mFastAltTabTrigger.isDozing()) {
            EventBus.getDefault().post(new HideRecentsEvent(z, z2));
            return;
        }
        showNextTask();
        this.mFastAltTabTrigger.stopDozing();
    }

    public void toggleRecents(int i) {
        if (ActivityManagerWrapper.getInstance().isScreenPinningActive() || this.mFastAltTabTrigger.isDozing()) {
            return;
        }
        if (mWaitingForTransitionStart) {
            mToggleFollowingTransitionStart = true;
            return;
        }
        boolean z = false;
        this.mDraggingInRecents = false;
        this.mLaunchedWhileDocking = false;
        this.mTriggeredFromAltTab = false;
        try {
            MutableBoolean mutableBoolean = new MutableBoolean(true);
            long elapsedRealtime = SystemClock.elapsedRealtime() - this.mLastToggleTime;
            if (LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible(mutableBoolean)) {
                if (!LegacyRecentsImpl.getConfiguration().getLaunchState().launchedWithAltTab) {
                    if (LegacyRecentsImpl.getConfiguration().isGridEnabled) {
                        if (elapsedRealtime < ((long) ViewConfiguration.getDoubleTapTimeout())) {
                            z = true;
                        }
                        if (z) {
                            EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
                        } else {
                            EventBus.getDefault().post(new LaunchMostRecentTaskRequestEvent());
                        }
                    } else {
                        EventBus.getDefault().post(new LaunchNextTaskRequestEvent());
                    }
                } else if (elapsedRealtime >= 350) {
                    EventBus.getDefault().post(new ToggleRecentsEvent());
                    this.mLastToggleTime = SystemClock.elapsedRealtime();
                }
            } else if (elapsedRealtime >= 350) {
                startRecentsActivityAndDismissKeyguardIfNeeded(ActivityManagerWrapper.getInstance().getRunningTask(), mutableBoolean.value, true, i);
                ActivityManagerWrapper.getInstance().closeSystemWindows("recentapps");
                this.mLastToggleTime = SystemClock.elapsedRealtime();
            }
        } catch (ActivityNotFoundException e) {
            Log.e("RecentsImpl", "Failed to launch RecentsActivity", e);
        }
    }

    public void preloadRecents() {
        if (!ActivityManagerWrapper.getInstance().isScreenPinningActive()) {
            StatusBar statusBar = getStatusBar();
            if (statusBar == null || !statusBar.isKeyguardShowing()) {
                this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.recents.$$Lambda$RecentsImpl$j3kxRIim5t_10M0HDPUojNhJV5I */

                    public final void run() {
                        RecentsImpl.this.lambda$preloadRecents$0$RecentsImpl();
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$preloadRecents$0$RecentsImpl() {
        ActivityManager.RunningTaskInfo runningTask;
        if (!LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible(null) && (runningTask = ActivityManagerWrapper.getInstance().getRunningTask()) != null) {
            RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
            sInstanceLoadPlan = new RecentsTaskLoadPlan(this.mContext);
            taskLoader.preloadTasks(sInstanceLoadPlan, runningTask.id);
            TaskStack taskStack = sInstanceLoadPlan.getTaskStack();
            if (taskStack.getTaskCount() > 0) {
                preloadIcon(runningTask.id);
                updateHeaderBarLayout(taskStack, null);
            }
        }
    }

    public void onDraggingInRecents(float f) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEvent(f));
    }

    public void onDraggingInRecentsEnded(float f) {
        EventBus.getDefault().sendOntoMainThread(new DraggingInRecentsEndedEvent(f));
    }

    public void onShowCurrentUserToast(int i, int i2) {
        Toast.makeText(this.mContext, i, i2).show();
    }

    public void showNextTask() {
        ActivityManager.RunningTaskInfo runningTask;
        Task task;
        ActivityOptions activityOptions;
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
        RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
        TaskStack taskStack = recentsTaskLoadPlan.getTaskStack();
        if (taskStack != null && taskStack.getTaskCount() != 0 && (runningTask = ActivityManagerWrapper.getInstance().getRunningTask()) != null) {
            boolean z = runningTask.configuration.windowConfiguration.getActivityType() == 2;
            ArrayList<Task> tasks = taskStack.getTasks();
            int size = tasks.size() - 1;
            while (true) {
                if (size < 1) {
                    task = null;
                    activityOptions = null;
                    break;
                }
                Task task2 = tasks.get(size);
                if (z) {
                    task = tasks.get(size - 1);
                    activityOptions = ActivityOptions.makeCustomAnimation(this.mContext, 2130772029, 2130772022);
                    break;
                } else if (task2.key.id == runningTask.id) {
                    task = tasks.get(size - 1);
                    activityOptions = ActivityOptions.makeCustomAnimation(this.mContext, 2130772032, 2130772031);
                    break;
                } else {
                    size--;
                }
            }
            if (task == null) {
                systemServices.startInPlaceAnimationOnFrontMostApplication(ActivityOptions.makeCustomInPlaceAnimation(this.mContext, 2130772030));
            } else {
                ActivityManagerWrapper.getInstance().startActivityFromRecentsAsync(task.key, activityOptions, null, null);
            }
        }
    }

    public void splitPrimaryTask(int i, int i2, Rect rect) {
        if (LegacyRecentsImpl.getSystemServices().setTaskWindowingModeSplitScreenPrimary(i, i2, rect)) {
            EventBus.getDefault().send(new DockedTopTaskEvent(rect));
        }
    }

    public void setWaitingForTransitionStart(boolean z) {
        if (mWaitingForTransitionStart != z) {
            mWaitingForTransitionStart = z;
            if (!z && mToggleFollowingTransitionStart) {
                this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.recents.$$Lambda$RecentsImpl$8KagPhOavaRXCFab5YBHz8Lgk54 */

                    public final void run() {
                        RecentsImpl.this.lambda$setWaitingForTransitionStart$1$RecentsImpl();
                    }
                });
            }
            mToggleFollowingTransitionStart = false;
        }
    }

    public /* synthetic */ void lambda$setWaitingForTransitionStart$1$RecentsImpl() {
        toggleRecents(-1);
    }

    public static RecentsTaskLoadPlan consumeInstanceLoadPlan() {
        RecentsTaskLoadPlan recentsTaskLoadPlan = sInstanceLoadPlan;
        sInstanceLoadPlan = null;
        return recentsTaskLoadPlan;
    }

    public static long getLastPipTime() {
        return sLastPipTime;
    }

    private void reloadResources() {
        Resources resources = this.mContext.getResources();
        this.mTaskBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(this.mContext, 2131166256, 2131166256, 2131166256, 2131166257, 2131166256, 2131166257, 2131166224);
        this.mHeaderBar = (TaskViewHeader) LayoutInflater.from(this.mContext).inflate(2131558745, (ViewGroup) null, false);
        this.mHeaderBar.setLayoutDirection(resources.getConfiguration().getLayoutDirection());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDummyStackViewLayout(TaskStackLayoutAlgorithm taskStackLayoutAlgorithm, TaskStack taskStack, Rect rect) {
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        Rect displayRect = systemServices.getDisplayRect();
        Rect rect2 = new Rect();
        systemServices.getStableInsets(rect2);
        if (systemServices.hasDockedTask()) {
            if (rect2.bottom < rect.height()) {
                rect.bottom -= rect2.bottom;
            }
            rect2.bottom = 0;
        }
        calculateWindowStableInsets(rect2, rect, displayRect);
        rect.offsetTo(0, 0);
        taskStackLayoutAlgorithm.setSystemInsets(rect2);
        if (taskStack != null) {
            taskStackLayoutAlgorithm.getTaskStackBounds(displayRect, rect, rect2.top, rect2.left, rect2.right, this.mTmpBounds);
            taskStackLayoutAlgorithm.reset();
            taskStackLayoutAlgorithm.initialize(displayRect, rect, this.mTmpBounds);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Rect getWindowRect(Rect rect) {
        if (rect != null) {
            return new Rect(rect);
        }
        return LegacyRecentsImpl.getSystemServices().getWindowRect();
    }

    private void updateHeaderBarLayout(TaskStack taskStack, Rect rect) {
        int i;
        Rect windowRect = getWindowRect(rect);
        boolean useGridLayout = this.mDummyStackView.useGridLayout();
        updateDummyStackViewLayout(this.mDummyStackView.getStackAlgorithm(), taskStack, windowRect);
        if (taskStack != null) {
            TaskStackLayoutAlgorithm stackAlgorithm = this.mDummyStackView.getStackAlgorithm();
            this.mDummyStackView.getStack().removeAllTasks(false);
            this.mDummyStackView.setTasks(taskStack, false);
            if (useGridLayout) {
                TaskGridLayoutAlgorithm gridAlgorithm = this.mDummyStackView.getGridAlgorithm();
                gridAlgorithm.initialize(windowRect);
                int taskCount = taskStack.getTaskCount();
                TaskViewTransform taskViewTransform = new TaskViewTransform();
                gridAlgorithm.getTransform(0, taskCount, taskViewTransform, stackAlgorithm);
                i = (int) taskViewTransform.rect.width();
            } else {
                Rect untransformedTaskViewBounds = stackAlgorithm.getUntransformedTaskViewBounds();
                if (!untransformedTaskViewBounds.isEmpty()) {
                    i = untransformedTaskViewBounds.width();
                }
            }
            if (taskStack != null && i > 0) {
                synchronized (this.mHeaderBarLock) {
                    if (!(this.mHeaderBar.getMeasuredWidth() == i && this.mHeaderBar.getMeasuredHeight() == this.mTaskBarHeight)) {
                        if (useGridLayout) {
                            this.mHeaderBar.setShouldDarkenBackgroundColor(true);
                            this.mHeaderBar.setNoUserInteractionState();
                        }
                        this.mHeaderBar.forceLayout();
                        this.mHeaderBar.measure(View.MeasureSpec.makeMeasureSpec(i, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTaskBarHeight, 1073741824));
                    }
                    this.mHeaderBar.layout(0, 0, i, this.mTaskBarHeight);
                }
                return;
            }
        }
        i = 0;
        if (taskStack != null) {
        }
    }

    private void calculateWindowStableInsets(Rect rect, Rect rect2, Rect rect3) {
        Rect rect4 = new Rect(rect3);
        rect4.inset(rect);
        Rect rect5 = new Rect(rect2);
        rect5.intersect(rect4);
        rect.left = rect5.left - rect2.left;
        rect.top = rect5.top - rect2.top;
        rect.right = rect2.right - rect5.right;
        rect.bottom = rect2.bottom - rect5.bottom;
    }

    private void preloadIcon(int i) {
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.runningTaskId = i;
        options.loadThumbnails = false;
        options.onlyLoadForCache = true;
        LegacyRecentsImpl.getTaskLoader().loadTasks(sInstanceLoadPlan, options);
    }

    /* access modifiers changed from: protected */
    public ActivityOptions getUnknownTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, 2130772025, 2130772026, this.mHandler, null);
    }

    /* access modifiers changed from: protected */
    public ActivityOptions getHomeTransitionActivityOptions() {
        return ActivityOptions.makeCustomAnimation(this.mContext, 2130772023, 2130772024, this.mHandler, null);
    }

    private Pair<ActivityOptions, AppTransitionAnimationSpecsFuture> getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo runningTaskInfo, Rect rect) {
        Runnable runnable;
        boolean z = LegacyRecentsImpl.getConfiguration().isLowRamDevice;
        final Task task = new Task();
        final TaskViewTransform thumbnailTransitionTransform = getThumbnailTransitionTransform(this.mDummyStackView, task, rect);
        final RectF rectF = thumbnailTransitionTransform.rect;
        AnonymousClass4 r8 = new AppTransitionAnimationSpecsFuture(this.mHandler) {
            /* class com.android.systemui.recents.RecentsImpl.AnonymousClass4 */

            @Override // com.android.systemui.shared.recents.view.AppTransitionAnimationSpecsFuture
            public List<AppTransitionAnimationSpecCompat> composeSpecs() {
                Rect rect = new Rect();
                rectF.round(rect);
                return Lists.newArrayList(new AppTransitionAnimationSpecCompat[]{new AppTransitionAnimationSpecCompat(task.key.id, RecentsImpl.this.drawThumbnailTransitionBitmap(task, thumbnailTransitionTransform), rect)});
            }
        };
        Context context = this.mContext;
        Handler handler = this.mHandler;
        if (z) {
            runnable = null;
        } else {
            runnable = this.mResetToggleFlagListener;
        }
        return new Pair<>(RecentsTransition.createAspectScaleAnimation(context, handler, false, r8, runnable), r8);
    }

    private TaskViewTransform getThumbnailTransitionTransform(TaskStackView taskStackView, Task task, Rect rect) {
        TaskStack stack = taskStackView.getStack();
        Task launchTarget = stack.getLaunchTarget();
        if (launchTarget != null) {
            task.copyFrom(launchTarget);
        } else {
            launchTarget = stack.getFrontMostTask();
            task.copyFrom(launchTarget);
        }
        taskStackView.updateLayoutAlgorithm(true);
        taskStackView.updateToInitialState();
        taskStackView.getStackAlgorithm().getStackTransformScreenCoordinates(launchTarget, taskStackView.getScroller().getStackScroll(), this.mTmpTransform, null, rect);
        return this.mTmpTransform;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private Bitmap drawThumbnailTransitionBitmap(Task task, TaskViewTransform taskViewTransform) {
        Bitmap drawViewIntoHardwareBitmap;
        SystemServicesProxy systemServices = LegacyRecentsImpl.getSystemServices();
        int width = (int) taskViewTransform.rect.width();
        int height = (int) taskViewTransform.rect.height();
        if (taskViewTransform == null || task.key == null || width <= 0 || height <= 0) {
            return null;
        }
        synchronized (this.mHeaderBarLock) {
            boolean z = !task.isSystemApp && systemServices.isInSafeMode();
            this.mHeaderBar.onTaskViewSizeChanged(width, height);
            Drawable drawable = this.mHeaderBar.getIconView().getDrawable();
            if (drawable != null) {
                drawable.setCallback(null);
            }
            this.mHeaderBar.bindToTask(task, false, z);
            this.mHeaderBar.onTaskDataLoaded();
            this.mHeaderBar.setDimAlpha(taskViewTransform.dimAlpha);
            drawViewIntoHardwareBitmap = RecentsTransition.drawViewIntoHardwareBitmap(width, this.mTaskBarHeight, this.mHeaderBar, 1.0f, 0);
        }
        return drawViewIntoHardwareBitmap;
    }

    /* access modifiers changed from: protected */
    public void startRecentsActivityAndDismissKeyguardIfNeeded(ActivityManager.RunningTaskInfo runningTaskInfo, boolean z, boolean z2, int i) {
        StatusBar statusBar = getStatusBar();
        if (statusBar == null || !statusBar.isKeyguardShowing()) {
            lambda$startRecentsActivityAndDismissKeyguardIfNeeded$2$RecentsImpl(runningTaskInfo, z, z2, i);
        } else {
            statusBar.executeRunnableDismissingKeyguard(new Runnable(runningTaskInfo, z, z2, i) {
                /* class com.android.systemui.recents.$$Lambda$RecentsImpl$nl0CQjEHqL97ISAVXWR_ZbUHGg */
                private final /* synthetic */ ActivityManager.RunningTaskInfo f$1;
                private final /* synthetic */ boolean f$2;
                private final /* synthetic */ boolean f$3;
                private final /* synthetic */ int f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void run() {
                    RecentsImpl.this.lambda$startRecentsActivityAndDismissKeyguardIfNeeded$3$RecentsImpl(this.f$1, this.f$2, this.f$3, this.f$4);
                }
            }, null, true, false, true);
        }
    }

    public /* synthetic */ void lambda$startRecentsActivityAndDismissKeyguardIfNeeded$3$RecentsImpl(ActivityManager.RunningTaskInfo runningTaskInfo, boolean z, boolean z2, int i) {
        this.mTrustManager.reportKeyguardShowingChanged();
        this.mHandler.post(new Runnable(runningTaskInfo, z, z2, i) {
            /* class com.android.systemui.recents.$$Lambda$RecentsImpl$p86QbP39AERJwYL1CmDcPooyqY */
            private final /* synthetic */ ActivityManager.RunningTaskInfo f$1;
            private final /* synthetic */ boolean f$2;
            private final /* synthetic */ boolean f$3;
            private final /* synthetic */ int f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                RecentsImpl.this.lambda$startRecentsActivityAndDismissKeyguardIfNeeded$2$RecentsImpl(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: startRecentsActivity */
    public void lambda$startRecentsActivityAndDismissKeyguardIfNeeded$2$RecentsImpl(ActivityManager.RunningTaskInfo runningTaskInfo, boolean z, boolean z2, int i) {
        Pair<ActivityOptions, AppTransitionAnimationSpecsFuture> pair;
        ActivityOptions activityOptions;
        RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        int i2 = (this.mLaunchedWhileDocking || runningTaskInfo == null) ? -1 : runningTaskInfo.id;
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || sInstanceLoadPlan == null) {
            sInstanceLoadPlan = new RecentsTaskLoadPlan(this.mContext);
        }
        if (this.mLaunchedWhileDocking || this.mTriggeredFromAltTab || !sInstanceLoadPlan.hasTasks()) {
            taskLoader.preloadTasks(sInstanceLoadPlan, i2);
        }
        TaskStack taskStack = sInstanceLoadPlan.getTaskStack();
        boolean z3 = true;
        boolean z4 = taskStack.getTaskCount() > 0;
        boolean z5 = runningTaskInfo != null && !z && z4;
        launchState.launchedFromHome = !z5 && !this.mLaunchedWhileDocking;
        if (!z5 && !this.mLaunchedWhileDocking) {
            z3 = false;
        }
        launchState.launchedFromApp = z3;
        launchState.launchedFromPipApp = false;
        launchState.launchedWithNextPipApp = taskStack.isNextLaunchTargetPip(getLastPipTime());
        launchState.launchedViaDockGesture = this.mLaunchedWhileDocking;
        launchState.launchedViaDragGesture = this.mDraggingInRecents;
        launchState.launchedToTaskId = i2;
        launchState.launchedWithAltTab = this.mTriggeredFromAltTab;
        setWaitingForTransitionStart(z5);
        preloadIcon(i2);
        Rect windowRectOverride = getWindowRectOverride(i);
        updateHeaderBarLayout(taskStack, windowRectOverride);
        TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport = this.mDummyStackView.computeStackVisibilityReport();
        launchState.launchedNumVisibleTasks = computeStackVisibilityReport.numVisibleTasks;
        launchState.launchedNumVisibleThumbnails = computeStackVisibilityReport.numVisibleThumbnails;
        if (!z2) {
            startRecentsActivity(ActivityOptions.makeCustomAnimation(this.mContext, -1, -1), null);
            return;
        }
        if (z5) {
            pair = getThumbnailTransitionActivityOptions(runningTaskInfo, windowRectOverride);
        } else {
            if (z4) {
                activityOptions = getHomeTransitionActivityOptions();
            } else {
                activityOptions = getUnknownTransitionActivityOptions();
            }
            pair = new Pair<>(activityOptions, null);
        }
        startRecentsActivity((ActivityOptions) pair.first, (AppTransitionAnimationSpecsFuture) pair.second);
        this.mLastToggleTime = SystemClock.elapsedRealtime();
    }

    private Rect getWindowRectOverride(int i) {
        if (i == -1) {
            return SystemServicesProxy.getInstance(this.mContext).getWindowRect();
        }
        Rect rect = new Rect();
        Rect displayRect = LegacyRecentsImpl.getSystemServices().getDisplayRect();
        DockedDividerUtils.calculateBoundsForPosition(i, 4, rect, displayRect.width(), displayRect.height(), LegacyRecentsImpl.getSystemServices().getDockedDividerSize(this.mContext));
        return rect;
    }

    private StatusBar getStatusBar() {
        return (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
    }

    private void startRecentsActivity(ActivityOptions activityOptions, AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture) {
        Intent intent = new Intent();
        intent.setClassName("com.android.systemui", "com.android.systemui.recents.RecentsActivity");
        intent.setFlags(276840448);
        HidePipMenuEvent hidePipMenuEvent = new HidePipMenuEvent();
        hidePipMenuEvent.addPostAnimationCallback(new Runnable(intent, activityOptions, appTransitionAnimationSpecsFuture) {
            /* class com.android.systemui.recents.$$Lambda$RecentsImpl$G7WjMO7A3RzOtnhk21g2Og8V7I */
            private final /* synthetic */ Intent f$0;
            private final /* synthetic */ ActivityOptions f$1;
            private final /* synthetic */ AppTransitionAnimationSpecsFuture f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                RecentsImpl.lambda$startRecentsActivity$4(this.f$0, this.f$1, this.f$2);
            }
        });
        EventBus.getDefault().send(hidePipMenuEvent);
        this.mDummyStackView.setTasks(this.mEmptyTaskStack, false);
    }

    static /* synthetic */ void lambda$startRecentsActivity$4(Intent intent, ActivityOptions activityOptions, AppTransitionAnimationSpecsFuture appTransitionAnimationSpecsFuture) {
        LegacyRecentsImpl.getSystemServices().startActivityAsUserAsync(intent, activityOptions);
        EventBus.getDefault().send(new RecentsActivityStartingEvent());
        if (appTransitionAnimationSpecsFuture != null) {
            appTransitionAnimationSpecsFuture.composeSpecsSynchronous();
        }
    }

    public void onAnimationFinished() {
        EventBus.getDefault().post(new EnterRecentsWindowLastAnimationFrameEvent());
    }
}
