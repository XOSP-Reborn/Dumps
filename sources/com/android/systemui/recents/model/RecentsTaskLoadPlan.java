package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTaskLoadPlan {
    private final Context mContext;
    private final KeyguardManager mKeyguardManager;
    private List<ActivityManager.RecentTaskInfo> mRawTasks;
    private TaskStack mStack;
    private final SparseBooleanArray mTmpLockedUsers = new SparseBooleanArray();

    public static class Options {
        public boolean loadIcons = true;
        public boolean loadThumbnails = false;
        public int numVisibleTaskThumbnails = 0;
        public int numVisibleTasks = 0;
        public boolean onlyLoadForCache = false;
        public boolean onlyLoadPausedActivities = false;
        public int runningTaskId = -1;
    }

    public static class PreloadOptions {
        public boolean loadTitles = true;
    }

    public RecentsTaskLoadPlan(Context context) {
        this.mContext = context;
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
    }

    public void preloadPlan(PreloadOptions preloadOptions, RecentsTaskLoader recentsTaskLoader, int i, int i2) {
        ComponentName componentName;
        String str;
        String str2;
        boolean z;
        Drawable drawable;
        this.mContext.getResources();
        ArrayList arrayList = new ArrayList();
        if (this.mRawTasks == null) {
            this.mRawTasks = ActivityManagerWrapper.getInstance().getRecentTasks(ActivityTaskManager.getMaxRecentTasksStatic(), i2);
            Collections.reverse(this.mRawTasks);
        }
        int i3 = 0;
        for (int size = this.mRawTasks.size(); i3 < size; size = size) {
            ActivityManager.RecentTaskInfo recentTaskInfo = this.mRawTasks.get(i3);
            if (recentTaskInfo.origActivity != null) {
                componentName = recentTaskInfo.origActivity;
            } else {
                componentName = recentTaskInfo.realActivity;
            }
            int windowingMode = recentTaskInfo.configuration.windowConfiguration.getWindowingMode();
            Task.TaskKey taskKey = new Task.TaskKey(recentTaskInfo.persistentId, windowingMode, recentTaskInfo.baseIntent, componentName, recentTaskInfo.userId, recentTaskInfo.lastActiveTime, recentTaskInfo.displayId);
            boolean z2 = !(windowingMode == 5);
            boolean z3 = taskKey.id == i;
            ActivityInfo andUpdateActivityInfo = recentsTaskLoader.getAndUpdateActivityInfo(taskKey);
            if (andUpdateActivityInfo != null) {
                if (preloadOptions.loadTitles) {
                    str = recentsTaskLoader.getAndUpdateActivityTitle(taskKey, recentTaskInfo.taskDescription);
                } else {
                    str = "";
                }
                if (preloadOptions.loadTitles) {
                    str2 = recentsTaskLoader.getAndUpdateContentDescription(taskKey, recentTaskInfo.taskDescription);
                } else {
                    str2 = "";
                }
                if (z2) {
                    z = false;
                    drawable = recentsTaskLoader.getAndUpdateActivityIcon(taskKey, recentTaskInfo.taskDescription, false);
                } else {
                    z = false;
                    drawable = null;
                }
                ThumbnailData andUpdateThumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey, z, z);
                int activityPrimaryColor = recentsTaskLoader.getActivityPrimaryColor(recentTaskInfo.taskDescription);
                int activityBackgroundColor = recentsTaskLoader.getActivityBackgroundColor(recentTaskInfo.taskDescription);
                boolean z4 = (andUpdateActivityInfo == null || (andUpdateActivityInfo.applicationInfo.flags & 1) == 0) ? false : true;
                if (this.mTmpLockedUsers.indexOfKey(recentTaskInfo.userId) < 0) {
                    this.mTmpLockedUsers.put(recentTaskInfo.userId, this.mKeyguardManager.isDeviceLocked(recentTaskInfo.userId));
                }
                arrayList.add(new Task(taskKey, drawable, andUpdateThumbnail, str, str2, activityPrimaryColor, activityBackgroundColor, z3, z2, z4, recentTaskInfo.supportsSplitScreenMultiWindow, recentTaskInfo.taskDescription, recentTaskInfo.resizeMode, recentTaskInfo.topActivity, this.mTmpLockedUsers.get(recentTaskInfo.userId)));
            }
            i3++;
        }
        this.mStack = new TaskStack();
        this.mStack.setTasks((List<Task>) arrayList, false);
    }

    public void executePlan(Options options, RecentsTaskLoader recentsTaskLoader) {
        this.mContext.getResources();
        ArrayList<Task> tasks = this.mStack.getTasks();
        int size = tasks.size();
        int i = 0;
        while (i < size) {
            Task task = tasks.get(i);
            Task.TaskKey taskKey = task.key;
            boolean z = taskKey.id == options.runningTaskId;
            boolean z2 = i >= size - options.numVisibleTasks;
            boolean z3 = i >= size - options.numVisibleTaskThumbnails;
            if (!options.onlyLoadPausedActivities || !z) {
                if (options.loadIcons && ((z || z2) && task.icon == null)) {
                    task.icon = recentsTaskLoader.getAndUpdateActivityIcon(taskKey, task.taskDescription, true);
                }
                if (options.loadThumbnails && z3) {
                    task.thumbnail = recentsTaskLoader.getAndUpdateThumbnail(taskKey, true, true);
                }
            }
            i++;
        }
    }

    public TaskStack getTaskStack() {
        return this.mStack;
    }

    public boolean hasTasks() {
        TaskStack taskStack = this.mStack;
        if (taskStack == null || taskStack.getTaskCount() <= 0) {
            return false;
        }
        return true;
    }
}
