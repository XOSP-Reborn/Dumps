package com.android.systemui.shared.system;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.WindowConfiguration;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class ActivityManagerWrapper {
    private static final ActivityManagerWrapper sInstance = new ActivityManagerWrapper();
    private final BackgroundExecutor mBackgroundExecutor = BackgroundExecutor.get();
    private final PackageManager mPackageManager = AppGlobals.getInitialApplication().getPackageManager();
    private final TaskStackChangeListeners mTaskStackChangeListeners = new TaskStackChangeListeners(Looper.getMainLooper());

    private ActivityManagerWrapper() {
    }

    public static ActivityManagerWrapper getInstance() {
        return sInstance;
    }

    public int getCurrentUserId() {
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            if (currentUser != null) {
                return currentUser.id;
            }
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ActivityManager.RunningTaskInfo getRunningTask() {
        return getRunningTask(3);
    }

    public ActivityManager.RunningTaskInfo getRunningTask(@WindowConfiguration.ActivityType int i) {
        try {
            List filteredTasks = ActivityTaskManager.getService().getFilteredTasks(1, i, 2);
            if (filteredTasks.isEmpty()) {
                return null;
            }
            return (ActivityManager.RunningTaskInfo) filteredTasks.get(0);
        } catch (RemoteException unused) {
            return null;
        }
    }

    public List<ActivityManager.RecentTaskInfo> getRecentTasks(int i, int i2) {
        try {
            return ActivityTaskManager.getService().getRecentTasks(i, 2, i2).getList();
        } catch (RemoteException e) {
            Log.e("ActivityManagerWrapper", "Failed to get recent tasks", e);
            return new ArrayList();
        }
    }

    public ThumbnailData getTaskThumbnail(int i, boolean z) {
        ActivityManager.TaskSnapshot taskSnapshot;
        try {
            taskSnapshot = ActivityTaskManager.getService().getTaskSnapshot(i, z);
        } catch (RemoteException e) {
            Log.w("ActivityManagerWrapper", "Failed to retrieve task snapshot", e);
            taskSnapshot = null;
        }
        if (taskSnapshot != null) {
            return new ThumbnailData(taskSnapshot);
        }
        return new ThumbnailData();
    }

    public String getBadgedActivityLabel(ActivityInfo activityInfo, int i) {
        return getBadgedLabel(activityInfo.loadLabel(this.mPackageManager).toString(), i);
    }

    public String getBadgedApplicationLabel(ApplicationInfo applicationInfo, int i) {
        return getBadgedLabel(applicationInfo.loadLabel(this.mPackageManager).toString(), i);
    }

    public String getBadgedContentDescription(ActivityInfo activityInfo, int i, ActivityManager.TaskDescription taskDescription) {
        String str;
        if (taskDescription == null || taskDescription.getLabel() == null) {
            str = activityInfo.loadLabel(this.mPackageManager).toString();
        } else {
            str = taskDescription.getLabel();
        }
        String charSequence = activityInfo.applicationInfo.loadLabel(this.mPackageManager).toString();
        String badgedLabel = getBadgedLabel(charSequence, i);
        if (charSequence.equals(str)) {
            return badgedLabel;
        }
        return badgedLabel + " " + str;
    }

    private String getBadgedLabel(String str, int i) {
        return i != UserHandle.myUserId() ? this.mPackageManager.getUserBadgedLabel(str, new UserHandle(i)).toString() : str;
    }

    public void startActivityFromRecentsAsync(Task.TaskKey taskKey, ActivityOptions activityOptions, Consumer<Boolean> consumer, Handler handler) {
        startActivityFromRecentsAsync(taskKey, activityOptions, 0, 0, consumer, handler);
    }

    public void startActivityFromRecentsAsync(final Task.TaskKey taskKey, final ActivityOptions activityOptions, int i, int i2, final Consumer<Boolean> consumer, final Handler handler) {
        if (taskKey.windowingMode == 3) {
            if (activityOptions == null) {
                activityOptions = ActivityOptions.makeBasic();
            }
            activityOptions.setLaunchWindowingMode(4);
        } else if (!(i == 0 && i2 == 0)) {
            if (activityOptions == null) {
                activityOptions = ActivityOptions.makeBasic();
            }
            activityOptions.setLaunchWindowingMode(i);
            activityOptions.setLaunchActivityType(i2);
        }
        this.mBackgroundExecutor.submit(new Runnable() {
            /* class com.android.systemui.shared.system.ActivityManagerWrapper.AnonymousClass5 */

            public void run() {
                final boolean z;
                try {
                    z = ActivityManagerWrapper.this.startActivityFromRecents(taskKey.id, activityOptions);
                } catch (Exception unused) {
                    z = false;
                }
                if (consumer != null) {
                    handler.post(new Runnable() {
                        /* class com.android.systemui.shared.system.ActivityManagerWrapper.AnonymousClass5.AnonymousClass1 */

                        public void run() {
                            consumer.accept(Boolean.valueOf(z));
                        }
                    });
                }
            }
        });
    }

    public boolean startActivityFromRecents(int i, ActivityOptions activityOptions) {
        Bundle bundle;
        if (activityOptions == null) {
            bundle = null;
        } else {
            try {
                bundle = activityOptions.toBundle();
            } catch (Exception unused) {
                return false;
            }
        }
        ActivityTaskManager.getService().startActivityFromRecents(i, bundle);
        return true;
    }

    public boolean setTaskWindowingModeSplitScreenPrimary(int i, int i2, Rect rect) {
        try {
            return ActivityTaskManager.getService().setTaskWindowingModeSplitScreenPrimary(i, i2, true, false, rect, true);
        } catch (RemoteException unused) {
            return false;
        }
    }

    public void registerTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.addListener(ActivityManager.getService(), taskStackChangeListener);
        }
    }

    public void unregisterTaskStackListener(TaskStackChangeListener taskStackChangeListener) {
        synchronized (this.mTaskStackChangeListeners) {
            this.mTaskStackChangeListeners.removeListener(taskStackChangeListener);
        }
    }

    public Future<?> closeSystemWindows(final String str) {
        return this.mBackgroundExecutor.submit(new Runnable() {
            /* class com.android.systemui.shared.system.ActivityManagerWrapper.AnonymousClass6 */

            public void run() {
                try {
                    ActivityManager.getService().closeSystemDialogs(str);
                } catch (RemoteException e) {
                    Log.w("ActivityManagerWrapper", "Failed to close system windows", e);
                }
            }
        });
    }

    public void removeTask(final int i) {
        this.mBackgroundExecutor.submit(new Runnable() {
            /* class com.android.systemui.shared.system.ActivityManagerWrapper.AnonymousClass7 */

            public void run() {
                try {
                    ActivityTaskManager.getService().removeTask(i);
                } catch (RemoteException e) {
                    Log.w("ActivityManagerWrapper", "Failed to remove task=" + i, e);
                }
            }
        });
    }

    public void cancelWindowTransition(int i) {
        try {
            ActivityTaskManager.getService().cancelTaskWindowTransition(i);
        } catch (RemoteException e) {
            Log.w("ActivityManagerWrapper", "Failed to cancel window transition for task=" + i, e);
        }
    }

    public boolean isScreenPinningActive() {
        try {
            return ActivityTaskManager.getService().getLockTaskModeState() == 2;
        } catch (RemoteException unused) {
            return false;
        }
    }

    public boolean isScreenPinningEnabled() {
        if (Settings.System.getInt(AppGlobals.getInitialApplication().getContentResolver(), "lock_to_app_enabled", 0) != 0) {
            return true;
        }
        return false;
    }

    public boolean isLockToAppActive() {
        try {
            return ActivityTaskManager.getService().getLockTaskModeState() != 0;
        } catch (RemoteException unused) {
            return false;
        }
    }

    public boolean isLockTaskKioskModeActive() {
        try {
            return ActivityTaskManager.getService().getLockTaskModeState() == 1;
        } catch (RemoteException unused) {
            return false;
        }
    }
}
