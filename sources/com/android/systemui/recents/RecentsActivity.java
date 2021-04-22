package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.LatencyTracker;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowLastAnimationFrameEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.ToggleRecentsEvent;
import com.android.systemui.recents.events.component.ActivityUnpinnedEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.events.component.SetWaitingForTransitionStartEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.NavigateTaskViewEvent;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.utilities.Utilities;
import com.android.systemui.recents.views.RecentsView;
import com.android.systemui.recents.views.SystemBarScrimViews;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class RecentsActivity extends Activity implements ViewTreeObserver.OnPreDrawListener, ColorExtractor.OnColorsChangedListener {
    private SysuiColorExtractor mColorExtractor;
    private boolean mFinishedOnStartup;
    private Handler mHandler = new Handler();
    private Intent mHomeIntent;
    private boolean mIgnoreAltTabRelease;
    private View mIncompatibleAppOverlay;
    private boolean mIsVisible;
    private Configuration mLastConfig;
    private long mLastTabKeyEventTime;
    private PackageMonitor mPackageMonitor = new PackageMonitor() {
        /* class com.android.systemui.recents.RecentsActivity.AnonymousClass1 */

        public void onPackageRemoved(String str, int i) {
            RecentsActivity.this.onPackageChanged(str, getChangingUserId());
        }

        public boolean onPackageChanged(String str, int i, String[] strArr) {
            RecentsActivity.this.onPackageChanged(str, getChangingUserId());
            return true;
        }

        public void onPackageModified(String str) {
            RecentsActivity.this.onPackageChanged(str, getChangingUserId());
        }
    };
    private final ViewTreeObserver.OnPreDrawListener mRecentsDrawnEventListener = new ViewTreeObserver.OnPreDrawListener() {
        /* class com.android.systemui.recents.RecentsActivity.AnonymousClass3 */

        public boolean onPreDraw() {
            RecentsActivity.this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
            EventBus.getDefault().post(new RecentsDrawnEvent());
            if (LatencyTracker.isEnabled(RecentsActivity.this.getApplicationContext())) {
                DejankUtils.postAfterTraversal(new Runnable() {
                    /* class com.android.systemui.recents.$$Lambda$RecentsActivity$3$yqqbbfyTHFuJpHT3gETj09GBDFY */

                    public final void run() {
                        RecentsActivity.AnonymousClass3.this.lambda$onPreDraw$0$RecentsActivity$3();
                    }
                });
            }
            DejankUtils.postAfterTraversal(new Runnable() {
                /* class com.android.systemui.recents.$$Lambda$RecentsActivity$3$SXW_26ZB_jvwD3qSXfVNpRowHM */

                public final void run() {
                    RecentsActivity.AnonymousClass3.this.lambda$onPreDraw$1$RecentsActivity$3();
                }
            });
            return true;
        }

        public /* synthetic */ void lambda$onPreDraw$0$RecentsActivity$3() {
            LatencyTracker.getInstance(RecentsActivity.this.getApplicationContext()).onActionEnd(1);
        }

        public /* synthetic */ void lambda$onPreDraw$1$RecentsActivity$3() {
            LegacyRecentsImpl.getTaskLoader().startLoader(RecentsActivity.this);
            LegacyRecentsImpl.getTaskLoader().getHighResThumbnailLoader().setVisible(true);
        }
    };
    private boolean mRecentsStartRequested;
    private RecentsView mRecentsView;
    private SystemBarScrimViews mScrimViews;
    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.recents.RecentsActivity.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                RecentsActivity.this.dismissRecentsToHomeIfVisible(false);
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                RecentsActivity.this.finish();
            }
        }
    };
    private final UserInteractionEvent mUserInteractionEvent = new UserInteractionEvent();
    private boolean mUsingDarkText;

    /* access modifiers changed from: package-private */
    public class LaunchHomeRunnable implements Runnable {
        Intent mLaunchIntent;
        ActivityOptions mOpts;

        public LaunchHomeRunnable(Intent intent, ActivityOptions activityOptions) {
            this.mLaunchIntent = intent;
            this.mOpts = activityOptions;
        }

        public void run() {
            try {
                RecentsActivity.this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.recents.$$Lambda$RecentsActivity$LaunchHomeRunnable$K3jCoKVe41EkTmSKH7i98xFt8k */

                    public final void run() {
                        RecentsActivity.LaunchHomeRunnable.this.lambda$run$0$RecentsActivity$LaunchHomeRunnable();
                    }
                });
            } catch (Exception e) {
                Log.e("RecentsActivity", RecentsActivity.this.getString(2131821946, new Object[]{"Home"}), e);
            }
        }

        public /* synthetic */ void lambda$run$0$RecentsActivity$LaunchHomeRunnable() {
            ActivityOptions activityOptions = this.mOpts;
            if (activityOptions == null) {
                activityOptions = ActivityOptions.makeCustomAnimation(RecentsActivity.this, 2130772033, 2130772034);
            }
            RecentsActivity.this.startActivityAsUser(this.mLaunchIntent, activityOptions.toBundle(), UserHandle.CURRENT);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToLaunchTargetTaskOrHome() {
        if (!LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible()) {
            return false;
        }
        if (this.mRecentsView.launchPreviousTask()) {
            return true;
        }
        dismissRecentsToHome(true);
        return false;
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToFocusedTaskOrHome() {
        boolean z = false;
        if (LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible()) {
            z = true;
            if (this.mRecentsView.launchFocusedTask(0)) {
                return true;
            }
            dismissRecentsToHome(true);
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public void dismissRecentsToHome(boolean z) {
        dismissRecentsToHome(z, null);
    }

    /* access modifiers changed from: package-private */
    public void dismissRecentsToHome(boolean z, ActivityOptions activityOptions) {
        DismissRecentsToHomeAnimationStarted dismissRecentsToHomeAnimationStarted = new DismissRecentsToHomeAnimationStarted(z);
        dismissRecentsToHomeAnimationStarted.addPostAnimationCallback(new LaunchHomeRunnable(this.mHomeIntent, activityOptions));
        ActivityManagerWrapper.getInstance().closeSystemWindows("homekey");
        EventBus.getDefault().send(dismissRecentsToHomeAnimationStarted);
    }

    /* access modifiers changed from: package-private */
    public boolean dismissRecentsToHomeIfVisible(boolean z) {
        if (!LegacyRecentsImpl.getSystemServices().isRecentsActivityVisible()) {
            return false;
        }
        dismissRecentsToHome(z);
        return true;
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFinishedOnStartup = false;
        if (LegacyRecentsImpl.getSystemServices() == null) {
            this.mFinishedOnStartup = true;
            finish();
            return;
        }
        EventBus.getDefault().register(this, 2);
        this.mPackageMonitor.register(this, Looper.getMainLooper(), UserHandle.ALL, true);
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mColorExtractor.addOnColorsChangedListener(this);
        this.mUsingDarkText = this.mColorExtractor.getColors(1, 1).supportsDarkText();
        setTheme(this.mUsingDarkText ? 2131886407 : 2131886406);
        setContentView(2131558736);
        takeKeyEvents(true);
        this.mRecentsView = (RecentsView) findViewById(2131362651);
        this.mScrimViews = new SystemBarScrimViews(this);
        getWindow().getAttributes().privateFlags |= 16384;
        if (LegacyRecentsImpl.getConfiguration().isLowRamDevice) {
            getWindow().addFlags(1024);
        }
        this.mLastConfig = new Configuration(Utilities.getAppConfiguration(this));
        this.mRecentsView.updateBackgroundScrim(getWindow(), isInMultiWindowMode());
        this.mHomeIntent = new Intent("android.intent.action.MAIN", (Uri) null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        registerReceiver(this.mSystemBroadcastReceiver, intentFilter);
        getWindow().addPrivateFlags(64);
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        reloadStackView();
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, true));
        MetricsLogger.visible(this, 224);
        this.mRecentsView.setScrimColors(this.mColorExtractor.getNeutralColors(), false);
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        Object lastNonConfigurationInstance = getLastNonConfigurationInstance();
        if (lastNonConfigurationInstance != null && (lastNonConfigurationInstance instanceof Boolean) && ((Boolean) lastNonConfigurationInstance).booleanValue()) {
            RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
            launchState.launchedViaDockGesture = false;
            launchState.launchedFromApp = false;
            launchState.launchedFromHome = false;
            onEnterAnimationComplete();
        }
        this.mRecentsStartRequested = false;
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        if ((i & 1) != 0) {
            ColorExtractor.GradientColors neutralColors = this.mColorExtractor.getNeutralColors();
            boolean supportsDarkText = neutralColors.supportsDarkText();
            if (supportsDarkText != this.mUsingDarkText) {
                this.mUsingDarkText = supportsDarkText;
                setTheme(this.mUsingDarkText ? 2131886407 : 2131886406);
                this.mRecentsView.reevaluateStyles();
            }
            this.mRecentsView.setScrimColors(neutralColors, true);
        }
    }

    private void reloadStackView() {
        RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
        RecentsTaskLoadPlan consumeInstanceLoadPlan = RecentsImpl.consumeInstanceLoadPlan();
        if (consumeInstanceLoadPlan == null) {
            consumeInstanceLoadPlan = new RecentsTaskLoadPlan(this);
        }
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        if (!consumeInstanceLoadPlan.hasTasks()) {
            taskLoader.preloadTasks(consumeInstanceLoadPlan, launchState.launchedToTaskId);
        }
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.runningTaskId = launchState.launchedToTaskId;
        options.numVisibleTasks = launchState.launchedNumVisibleTasks;
        options.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
        taskLoader.loadTasks(consumeInstanceLoadPlan, options);
        TaskStack taskStack = consumeInstanceLoadPlan.getTaskStack();
        this.mRecentsView.onReload(taskStack, this.mIsVisible);
        int i = 0;
        this.mScrimViews.updateNavBarScrim(!launchState.launchedViaDockGesture, taskStack.getTaskCount() > 0, null);
        if (!launchState.launchedFromHome && !launchState.launchedFromApp) {
            EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
        }
        if (launchState.launchedWithAltTab) {
            MetricsLogger.count(this, "overview_trigger_alttab", 1);
        } else {
            MetricsLogger.count(this, "overview_trigger_nav_btn", 1);
        }
        if (launchState.launchedFromApp) {
            Task launchTarget = taskStack.getLaunchTarget();
            if (launchTarget != null) {
                i = taskStack.indexOfTask(launchTarget);
            }
            MetricsLogger.count(this, "overview_source_app", 1);
            MetricsLogger.histogram(this, "overview_source_app_index", i);
        } else {
            MetricsLogger.count(this, "overview_source_home", 1);
        }
        MetricsLogger.histogram(this, "overview_task_count", this.mRecentsView.getStack().getTaskCount());
        this.mIsVisible = true;
    }

    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        EventBus.getDefault().send(new EnterRecentsWindowAnimationCompletedEvent());
        EventBus.getDefault().send(new SetWaitingForTransitionStartEvent(false));
    }

    public Object onRetainNonConfigurationInstance() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        this.mIgnoreAltTabRelease = false;
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        Configuration appConfiguration = Utilities.getAppConfiguration(this);
        int taskCount = this.mRecentsView.getStack().getTaskCount();
        EventBus eventBus = EventBus.getDefault();
        boolean z = true;
        boolean z2 = this.mLastConfig.orientation != appConfiguration.orientation;
        boolean z3 = this.mLastConfig.densityDpi != appConfiguration.densityDpi;
        if (taskCount <= 0) {
            z = false;
        }
        eventBus.send(new ConfigurationChangedEvent(false, z2, z3, z));
        this.mLastConfig.updateFrom(appConfiguration);
    }

    public void onMultiWindowModeChanged(boolean z) {
        super.onMultiWindowModeChanged(z);
        this.mRecentsView.updateBackgroundScrim(getWindow(), z);
        if (this.mIsVisible) {
            reloadTaskStack(z, true);
        }
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        this.mIsVisible = false;
        EventBus.getDefault().send(new RecentsVisibilityChangedEvent(this, false));
        MetricsLogger.hidden(this, 224);
        LegacyRecentsImpl.getTaskLoader().getHighResThumbnailLoader().setVisible(false);
        if (!isChangingConfigurations() && !this.mRecentsStartRequested) {
            LegacyRecentsImpl.getConfiguration().getLaunchState().reset();
        }
        LegacyRecentsImpl.getSystemServices().gc();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        if (!this.mFinishedOnStartup) {
            unregisterReceiver(this.mSystemBroadcastReceiver);
            this.mPackageMonitor.unregister();
            EventBus.getDefault().unregister(this);
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this.mScrimViews, 2);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this.mScrimViews);
    }

    public void onTrimMemory(int i) {
        RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
        if (taskLoader != null) {
            taskLoader.onTrimMemory(i);
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i != 61) {
            if (i != 67 && i != 112) {
                switch (i) {
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                        EventBus.getDefault().send(new NavigateTaskViewEvent(NavigateTaskViewEvent.getDirectionFromKeyCode(i)));
                        return true;
                }
            } else if (keyEvent.getRepeatCount() <= 0) {
                EventBus.getDefault().send(new DismissFocusedTaskViewEvent());
                MetricsLogger.histogram(this, "overview_task_dismissed_source", 0);
                return true;
            }
            return super.onKeyDown(i, keyEvent);
        }
        boolean z = SystemClock.elapsedRealtime() - this.mLastTabKeyEventTime > ((long) getResources().getInteger(2131427441));
        if (keyEvent.getRepeatCount() <= 0 || z) {
            if (keyEvent.isShiftPressed()) {
                EventBus.getDefault().send(new FocusPreviousTaskViewEvent());
            } else {
                EventBus.getDefault().send(new FocusNextTaskViewEvent());
            }
            this.mLastTabKeyEventTime = SystemClock.elapsedRealtime();
            if (keyEvent.isAltPressed()) {
                this.mIgnoreAltTabRelease = false;
            }
        }
        return true;
    }

    public void onUserInteraction() {
        EventBus.getDefault().send(this.mUserInteractionEvent);
    }

    public void onBackPressed() {
        EventBus.getDefault().send(new ToggleRecentsEvent());
    }

    public final void onBusEvent(ToggleRecentsEvent toggleRecentsEvent) {
        if (LegacyRecentsImpl.getConfiguration().getLaunchState().launchedFromHome) {
            dismissRecentsToHome(true);
        } else {
            dismissRecentsToLaunchTargetTaskOrHome();
        }
    }

    public final void onBusEvent(RecentsActivityStartingEvent recentsActivityStartingEvent) {
        this.mRecentsStartRequested = true;
    }

    public final void onBusEvent(HideRecentsEvent hideRecentsEvent) {
        if (hideRecentsEvent.triggeredFromAltTab) {
            if (!this.mIgnoreAltTabRelease) {
                dismissRecentsToFocusedTaskOrHome();
            }
        } else if (hideRecentsEvent.triggeredFromHomeKey) {
            dismissRecentsToHome(true);
            EventBus.getDefault().send(this.mUserInteractionEvent);
        }
    }

    public final void onBusEvent(EnterRecentsWindowLastAnimationFrameEvent enterRecentsWindowLastAnimationFrameEvent) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(ExitRecentsWindowFirstAnimationFrameEvent exitRecentsWindowFirstAnimationFrameEvent) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(DockedFirstAnimationFrameEvent dockedFirstAnimationFrameEvent) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(CancelEnterRecentsWindowAnimationEvent cancelEnterRecentsWindowAnimationEvent) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        int i = launchState.launchedToTaskId;
        if (i != -1) {
            Task task = cancelEnterRecentsWindowAnimationEvent.launchTask;
            if (task == null || i != task.key.id) {
                ActivityManagerWrapper.getInstance().cancelWindowTransition(launchState.launchedToTaskId);
            }
        }
    }

    public final void onBusEvent(ShowApplicationInfoEvent showApplicationInfoEvent) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", showApplicationInfoEvent.task.key.getComponent().getPackageName(), null));
        intent.setComponent(intent.resolveActivity(getPackageManager()));
        TaskStackBuilder.create(this).addNextIntentWithParentStack(intent).startActivities(null, new UserHandle(showApplicationInfoEvent.task.key.userId));
        MetricsLogger.count(this, "overview_app_info", 1);
    }

    public final void onBusEvent(ShowIncompatibleAppOverlayEvent showIncompatibleAppOverlayEvent) {
        if (this.mIncompatibleAppOverlay == null) {
            this.mIncompatibleAppOverlay = Utilities.findViewStubById(this, 2131362298).inflate();
            this.mIncompatibleAppOverlay.setWillNotDraw(false);
            this.mIncompatibleAppOverlay.setVisibility(0);
        }
        this.mIncompatibleAppOverlay.animate().alpha(1.0f).setDuration(150).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public final void onBusEvent(HideIncompatibleAppOverlayEvent hideIncompatibleAppOverlayEvent) {
        View view = this.mIncompatibleAppOverlay;
        if (view != null) {
            view.animate().alpha(0.0f).setDuration(150).setInterpolator(Interpolators.ALPHA_OUT).start();
        }
    }

    public final void onBusEvent(DeleteTaskDataEvent deleteTaskDataEvent) {
        LegacyRecentsImpl.getTaskLoader().deleteTaskData(deleteTaskDataEvent.task, false);
        ActivityManagerWrapper.getInstance().removeTask(deleteTaskDataEvent.task.key.id);
    }

    public final void onBusEvent(TaskViewDismissedEvent taskViewDismissedEvent) {
        this.mRecentsView.updateScrimOpacity();
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent allTaskViewsDismissedEvent) {
        if (LegacyRecentsImpl.getSystemServices().hasDockedTask()) {
            this.mRecentsView.showEmptyView(allTaskViewsDismissedEvent.msgResId);
        } else {
            dismissRecentsToHome(false);
        }
        MetricsLogger.count(this, "overview_task_all_dismissed", 1);
    }

    public final void onBusEvent(LaunchTaskSucceededEvent launchTaskSucceededEvent) {
        MetricsLogger.histogram(this, "overview_task_launch_index", launchTaskSucceededEvent.taskIndexFromStackFront);
    }

    public final void onBusEvent(LaunchTaskFailedEvent launchTaskFailedEvent) {
        dismissRecentsToHome(true);
        MetricsLogger.count(this, "overview_task_launch_failed", 1);
    }

    public final void onBusEvent(ScreenPinningRequestEvent screenPinningRequestEvent) {
        MetricsLogger.count(this, "overview_screen_pinned", 1);
    }

    public final void onBusEvent(StackViewScrolledEvent stackViewScrolledEvent) {
        this.mIgnoreAltTabRelease = true;
    }

    public final void onBusEvent(DockedTopTaskEvent dockedTopTaskEvent) {
        this.mRecentsView.getViewTreeObserver().addOnPreDrawListener(this.mRecentsDrawnEventListener);
        this.mRecentsView.invalidate();
    }

    public final void onBusEvent(ActivityUnpinnedEvent activityUnpinnedEvent) {
        if (this.mIsVisible) {
            reloadTaskStack(isInMultiWindowMode(), false);
        }
    }

    private void reloadTaskStack(boolean z, boolean z2) {
        RecentsActivityLaunchState launchState = LegacyRecentsImpl.getConfiguration().getLaunchState();
        RecentsTaskLoader taskLoader = LegacyRecentsImpl.getTaskLoader();
        RecentsTaskLoadPlan recentsTaskLoadPlan = new RecentsTaskLoadPlan(this);
        taskLoader.preloadTasks(recentsTaskLoadPlan, -1);
        RecentsTaskLoadPlan.Options options = new RecentsTaskLoadPlan.Options();
        options.numVisibleTasks = launchState.launchedNumVisibleTasks;
        options.numVisibleTaskThumbnails = launchState.launchedNumVisibleThumbnails;
        taskLoader.loadTasks(recentsTaskLoadPlan, options);
        TaskStack taskStack = recentsTaskLoadPlan.getTaskStack();
        int taskCount = taskStack.getTaskCount();
        boolean z3 = taskCount > 0;
        if (z2) {
            EventBus.getDefault().send(new ConfigurationChangedEvent(true, false, false, taskCount > 0));
        }
        EventBus.getDefault().send(new MultiWindowStateChangedEvent(z, z3, taskStack));
    }

    public boolean onPreDraw() {
        this.mRecentsView.getViewTreeObserver().removeOnPreDrawListener(this);
        return true;
    }

    public void onPackageChanged(String str, int i) {
        LegacyRecentsImpl.getTaskLoader().onPackageChanged(str);
        EventBus.getDefault().send(new PackagesChangedEvent(str, i));
    }

    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(str, fileDescriptor, printWriter, strArr);
        EventBus.getDefault().dump(str, printWriter);
        LegacyRecentsImpl.getTaskLoader().dump(str, printWriter);
        String hexString = Integer.toHexString(System.identityHashCode(this));
        printWriter.print(str);
        printWriter.print("RecentsActivity");
        printWriter.print(" visible=");
        printWriter.print(this.mIsVisible ? "Y" : "N");
        printWriter.print(" currentTime=");
        printWriter.print(System.currentTimeMillis());
        printWriter.print(" [0x");
        printWriter.print(hexString);
        printWriter.print("]");
        printWriter.println();
        RecentsView recentsView = this.mRecentsView;
        if (recentsView != null) {
            recentsView.dump(str, printWriter);
        }
    }
}
