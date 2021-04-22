package com.android.systemui.assist;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import com.android.systemui.Dependency;
import com.android.systemui.assist.AssistHandleBehaviorController;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.PackageManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

final class AssistHandleReminderExpBehavior implements AssistHandleBehaviorController.BehaviorController {
    private static final String[] DEFAULT_HOME_CHANGE_ACTIONS = {"android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED", "android.intent.action.BOOT_COMPLETED", "android.intent.action.PACKAGE_ADDED", "android.intent.action.PACKAGE_CHANGED", "android.intent.action.PACKAGE_REMOVED"};
    private static final long DEFAULT_LEARNING_TIME_MS = TimeUnit.DAYS.toMillis(10);
    private static final long DEFAULT_SHOW_AND_GO_DELAYED_LONG_DELAY_MS = TimeUnit.SECONDS.toMillis(1);
    private static final long DEFAULT_SHOW_AND_GO_DELAY_RESET_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3);
    private final ActivityManagerWrapper mActivityManagerWrapper;
    private AssistHandleCallbacks mAssistHandleCallbacks;
    private int mConsecutiveTaskSwitches;
    private Context mContext;
    private ComponentName mDefaultHome;
    private final BroadcastReceiver mDefaultHomeBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.assist.AssistHandleReminderExpBehavior.AnonymousClass4 */

        public void onReceive(Context context, Intent intent) {
            AssistHandleReminderExpBehavior.this.mDefaultHome = AssistHandleReminderExpBehavior.getCurrentDefaultHome();
        }
    };
    private final IntentFilter mDefaultHomeIntentFilter;
    private final Handler mHandler;
    private boolean mIsDozing;
    private boolean mIsLauncherShowing;
    private boolean mIsLearned;
    private boolean mIsNavBarHidden;
    private long mLastLearningTimestamp;
    private long mLearnedHintLastShownEpochDay;
    private int mLearningCount;
    private long mLearningTimeElapsed;
    private boolean mOnLockscreen;
    private final OverviewProxyService.OverviewProxyListener mOverviewProxyListener = new OverviewProxyService.OverviewProxyListener() {
        /* class com.android.systemui.assist.AssistHandleReminderExpBehavior.AnonymousClass3 */

        @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
        public void onOverviewShown(boolean z) {
            AssistHandleReminderExpBehavior.this.handleOverviewShown();
        }

        @Override // com.android.systemui.recents.OverviewProxyService.OverviewProxyListener
        public void onSystemUiStateChanged(int i) {
            AssistHandleReminderExpBehavior.this.handleSystemUiStateChanged(i);
        }
    };
    private final OverviewProxyService mOverviewProxyService;
    private final PhenotypeHelper mPhenotypeHelper;
    private final Runnable mResetConsecutiveTaskSwitches = new Runnable() {
        /* class com.android.systemui.assist.$$Lambda$AssistHandleReminderExpBehavior$pwcnWUhYSvHUPTaX_vnnVqcvKYA */

        public final void run() {
            AssistHandleReminderExpBehavior.this.resetConsecutiveTaskSwitches();
        }
    };
    private int mRunningTaskId;
    private final StatusBarStateController mStatusBarStateController;
    private final StatusBarStateController.StateListener mStatusBarStateListener = new StatusBarStateController.StateListener() {
        /* class com.android.systemui.assist.AssistHandleReminderExpBehavior.AnonymousClass1 */

        @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
        public void onStateChanged(int i) {
            AssistHandleReminderExpBehavior.this.handleStatusBarStateChanged(i);
        }

        @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
        public void onDozingChanged(boolean z) {
            AssistHandleReminderExpBehavior.this.handleDozingChanged(z);
        }
    };
    private final TaskStackChangeListener mTaskStackChangeListener = new TaskStackChangeListener() {
        /* class com.android.systemui.assist.AssistHandleReminderExpBehavior.AnonymousClass2 */

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskMovedToFront(ActivityManager.RunningTaskInfo runningTaskInfo) {
            AssistHandleReminderExpBehavior.this.handleTaskStackTopChanged(runningTaskInfo.taskId, runningTaskInfo.topActivity);
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskCreated(int i, ComponentName componentName) {
            AssistHandleReminderExpBehavior.this.handleTaskStackTopChanged(i, componentName);
        }
    };

    private static boolean isNavBarHidden(int i) {
        return (i & 2) != 0;
    }

    private boolean onLockscreen(int i) {
        return i == 1 || i == 2;
    }

    AssistHandleReminderExpBehavior(Handler handler, PhenotypeHelper phenotypeHelper) {
        this.mHandler = handler;
        this.mPhenotypeHelper = phenotypeHelper;
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        this.mActivityManagerWrapper = ActivityManagerWrapper.getInstance();
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mDefaultHomeIntentFilter = new IntentFilter();
        for (String str : DEFAULT_HOME_CHANGE_ACTIONS) {
            this.mDefaultHomeIntentFilter.addAction(str);
        }
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void onModeActivated(Context context, AssistHandleCallbacks assistHandleCallbacks) {
        int i;
        this.mContext = context;
        this.mAssistHandleCallbacks = assistHandleCallbacks;
        this.mConsecutiveTaskSwitches = 0;
        this.mDefaultHome = getCurrentDefaultHome();
        context.registerReceiver(this.mDefaultHomeBroadcastReceiver, this.mDefaultHomeIntentFilter);
        this.mOnLockscreen = onLockscreen(this.mStatusBarStateController.getState());
        this.mIsDozing = this.mStatusBarStateController.isDozing();
        this.mStatusBarStateController.addCallback(this.mStatusBarStateListener);
        ActivityManager.RunningTaskInfo runningTask = this.mActivityManagerWrapper.getRunningTask();
        if (runningTask == null) {
            i = 0;
        } else {
            i = runningTask.taskId;
        }
        this.mRunningTaskId = i;
        this.mActivityManagerWrapper.registerTaskStackListener(this.mTaskStackChangeListener);
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        this.mLearningTimeElapsed = Settings.Secure.getLong(context.getContentResolver(), "reminder_exp_learning_time_elapsed", 0);
        this.mLearningCount = Settings.Secure.getInt(context.getContentResolver(), "reminder_exp_learning_event_count", 0);
        this.mLearnedHintLastShownEpochDay = Settings.Secure.getLong(context.getContentResolver(), "reminder_exp_learned_hint_last_shown", 0);
        this.mLastLearningTimestamp = SystemClock.uptimeMillis();
        callbackForCurrentState(false);
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void onModeDeactivated() {
        this.mAssistHandleCallbacks = null;
        Context context = this.mContext;
        if (context != null) {
            context.unregisterReceiver(this.mDefaultHomeBroadcastReceiver);
            Settings.Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learning_time_elapsed", 0);
            Settings.Secure.putInt(this.mContext.getContentResolver(), "reminder_exp_learning_event_count", 0);
            Settings.Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learned_hint_last_shown", 0);
            this.mContext = null;
        }
        this.mStatusBarStateController.removeCallback(this.mStatusBarStateListener);
        this.mActivityManagerWrapper.unregisterTaskStackListener(this.mTaskStackChangeListener);
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void onAssistantGesturePerformed() {
        Context context = this.mContext;
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            int i = this.mLearningCount + 1;
            this.mLearningCount = i;
            Settings.Secure.putLong(contentResolver, "reminder_exp_learning_event_count", (long) i);
        }
    }

    /* access modifiers changed from: private */
    public static ComponentName getCurrentDefaultHome() {
        ArrayList arrayList = new ArrayList();
        ComponentName homeActivities = PackageManagerWrapper.getInstance().getHomeActivities(arrayList);
        if (homeActivities != null) {
            return homeActivities;
        }
        Iterator it = arrayList.iterator();
        int i = Integer.MIN_VALUE;
        while (true) {
            ComponentName componentName = null;
            while (true) {
                if (!it.hasNext()) {
                    return componentName;
                }
                ResolveInfo resolveInfo = (ResolveInfo) it.next();
                int i2 = resolveInfo.priority;
                if (i2 > i) {
                    componentName = resolveInfo.activityInfo.getComponentName();
                    i = resolveInfo.priority;
                } else if (i2 == i) {
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleStatusBarStateChanged(int i) {
        boolean onLockscreen = onLockscreen(i);
        if (this.mOnLockscreen != onLockscreen) {
            resetConsecutiveTaskSwitches();
            this.mOnLockscreen = onLockscreen;
            callbackForCurrentState(!onLockscreen);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            resetConsecutiveTaskSwitches();
            this.mIsDozing = z;
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleTaskStackTopChanged(int i, ComponentName componentName) {
        if (this.mRunningTaskId != i && componentName != null) {
            this.mRunningTaskId = i;
            this.mIsLauncherShowing = componentName.equals(this.mDefaultHome);
            if (this.mIsLauncherShowing) {
                resetConsecutiveTaskSwitches();
            } else {
                rescheduleConsecutiveTaskSwitchesReset();
                this.mConsecutiveTaskSwitches++;
            }
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSystemUiStateChanged(int i) {
        boolean isNavBarHidden = isNavBarHidden(i);
        if (this.mIsNavBarHidden != isNavBarHidden) {
            resetConsecutiveTaskSwitches();
            this.mIsNavBarHidden = isNavBarHidden;
            callbackForCurrentState(false);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleOverviewShown() {
        resetConsecutiveTaskSwitches();
        callbackForCurrentState(false);
    }

    private void callbackForCurrentState(boolean z) {
        updateLearningStatus();
        if (this.mIsLearned) {
            callbackForLearnedState(z);
        } else {
            callbackForUnlearnedState();
        }
    }

    private void callbackForLearnedState(boolean z) {
        if (this.mAssistHandleCallbacks != null) {
            if (this.mIsDozing || this.mIsNavBarHidden || this.mOnLockscreen || !getShowWhenTaught()) {
                this.mAssistHandleCallbacks.hide();
            } else if (z) {
                long epochDay = LocalDate.now().toEpochDay();
                if (this.mLearnedHintLastShownEpochDay < epochDay) {
                    Context context = this.mContext;
                    if (context != null) {
                        Settings.Secure.putLong(context.getContentResolver(), "reminder_exp_learned_hint_last_shown", epochDay);
                    }
                    this.mLearnedHintLastShownEpochDay = epochDay;
                    this.mAssistHandleCallbacks.showAndGo();
                }
            }
        }
    }

    private void callbackForUnlearnedState() {
        if (this.mAssistHandleCallbacks != null) {
            if (this.mIsDozing || this.mIsNavBarHidden || isSuppressed()) {
                this.mAssistHandleCallbacks.hide();
            } else if (this.mOnLockscreen) {
                this.mAssistHandleCallbacks.showAndStay();
            } else if (this.mIsLauncherShowing) {
                this.mAssistHandleCallbacks.showAndGo();
            } else if (this.mConsecutiveTaskSwitches == 1) {
                this.mAssistHandleCallbacks.showAndGoDelayed(getShowAndGoDelayedShortDelayMs(), false);
            } else {
                this.mAssistHandleCallbacks.showAndGoDelayed(getShowAndGoDelayedLongDelayMs(), true);
            }
        }
    }

    private boolean isSuppressed() {
        if (this.mOnLockscreen) {
            return getSuppressOnLockscreen();
        }
        if (this.mIsLauncherShowing) {
            return getSuppressOnLauncher();
        }
        return getSuppressOnApps();
    }

    private void updateLearningStatus() {
        if (this.mContext != null) {
            long uptimeMillis = SystemClock.uptimeMillis();
            this.mLearningTimeElapsed += uptimeMillis - this.mLastLearningTimestamp;
            this.mLastLearningTimestamp = uptimeMillis;
            Settings.Secure.putLong(this.mContext.getContentResolver(), "reminder_exp_learning_time_elapsed", this.mLearningTimeElapsed);
            this.mIsLearned = this.mLearningCount >= getLearningCount() || this.mLearningTimeElapsed >= getLearningTimeMs();
        }
    }

    /* access modifiers changed from: private */
    public void resetConsecutiveTaskSwitches() {
        this.mHandler.removeCallbacks(this.mResetConsecutiveTaskSwitches);
        this.mConsecutiveTaskSwitches = 0;
    }

    private void rescheduleConsecutiveTaskSwitchesReset() {
        this.mHandler.removeCallbacks(this.mResetConsecutiveTaskSwitches);
        this.mHandler.postDelayed(this.mResetConsecutiveTaskSwitches, getShowAndGoDelayResetTimeoutMs());
    }

    private long getLearningTimeMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_learn_time_ms", DEFAULT_LEARNING_TIME_MS);
    }

    private int getLearningCount() {
        return this.mPhenotypeHelper.getInt("assist_handles_learn_count", 10);
    }

    private long getShowAndGoDelayedShortDelayMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delayed_short_delay_ms", 150);
    }

    private long getShowAndGoDelayedLongDelayMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delayed_long_delay_ms", DEFAULT_SHOW_AND_GO_DELAYED_LONG_DELAY_MS);
    }

    private long getShowAndGoDelayResetTimeoutMs() {
        return this.mPhenotypeHelper.getLong("assist_handles_show_and_go_delay_reset_timeout_ms", DEFAULT_SHOW_AND_GO_DELAY_RESET_TIMEOUT_MS);
    }

    private boolean getSuppressOnLockscreen() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_lockscreen", false);
    }

    private boolean getSuppressOnLauncher() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_launcher", false);
    }

    private boolean getSuppressOnApps() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_suppress_on_apps", true);
    }

    private boolean getShowWhenTaught() {
        return this.mPhenotypeHelper.getBoolean("assist_handles_show_when_taught", false);
    }

    @Override // com.android.systemui.assist.AssistHandleBehaviorController.BehaviorController
    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "Current AssistHandleReminderExpBehavior State:");
        printWriter.println(str + "   mOnLockscreen=" + this.mOnLockscreen);
        printWriter.println(str + "   mIsDozing=" + this.mIsDozing);
        printWriter.println(str + "   mRunningTaskId=" + this.mRunningTaskId);
        printWriter.println(str + "   mDefaultHome=" + this.mDefaultHome);
        printWriter.println(str + "   mIsNavBarHidden=" + this.mIsNavBarHidden);
        printWriter.println(str + "   mIsLauncherShowing=" + this.mIsLauncherShowing);
        printWriter.println(str + "   mConsecutiveTaskSwitches=" + this.mConsecutiveTaskSwitches);
        printWriter.println(str + "   mIsLearned=" + this.mIsLearned);
        printWriter.println(str + "   mLastLearningTimestamp=" + this.mLastLearningTimestamp);
        printWriter.println(str + "   mLearningTimeElapsed=" + this.mLearningTimeElapsed);
        printWriter.println(str + "   mLearningCount=" + this.mLearningCount);
        printWriter.println(str + "   mLearnedHintLastShownEpochDay=" + this.mLearnedHintLastShownEpochDay);
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("   mAssistHandleCallbacks present: ");
        sb.append(this.mAssistHandleCallbacks != null);
        printWriter.println(sb.toString());
        printWriter.println(str + "   Phenotype Flags:");
        printWriter.println(str + "      " + "assist_handles_learn_time_ms" + "=" + getLearningTimeMs());
        printWriter.println(str + "      " + "assist_handles_learn_count" + "=" + getLearningCount());
        printWriter.println(str + "      " + "assist_handles_show_and_go_delayed_short_delay_ms" + "=" + getShowAndGoDelayedShortDelayMs());
        printWriter.println(str + "      " + "assist_handles_show_and_go_delayed_long_delay_ms" + "=" + getShowAndGoDelayedLongDelayMs());
        printWriter.println(str + "      " + "assist_handles_show_and_go_delay_reset_timeout_ms" + "=" + getShowAndGoDelayResetTimeoutMs());
        printWriter.println(str + "      " + "assist_handles_suppress_on_lockscreen" + "=" + getSuppressOnLockscreen());
        printWriter.println(str + "      " + "assist_handles_suppress_on_launcher" + "=" + getSuppressOnLauncher());
        printWriter.println(str + "      " + "assist_handles_suppress_on_apps" + "=" + getSuppressOnApps());
        printWriter.println(str + "      " + "assist_handles_show_when_taught" + "=" + getShowWhenTaught());
    }
}
