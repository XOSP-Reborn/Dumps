package com.sonymobile.systemui.multiwindowcontroller;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import com.android.systemui.C0014R$string;
import java.util.List;

public class MultiWindowController {
    private static final String TAG = "MultiWindowController";
    private final Context mContext;
    private Handler mHandler = new Handler();

    private boolean isActivityTypeStandardOrUndefined(int i) {
        return i == 1 || i == 0;
    }

    public MultiWindowController(Context context) {
        this.mContext = context;
    }

    public void launchMultiWindow(PendingIntent pendingIntent, String str) {
        if (getDockSide() == -1) {
            dismissPipIfNeeded(str);
            ActivityManager.StackInfo topFullscreenStack = getTopFullscreenStack();
            if (topFullscreenStack == null) {
                launchApp(pendingIntent);
                return;
            }
            int activityType = getActivityType(topFullscreenStack);
            if (activityType == 2 || activityType == 3) {
                showToast(this.mContext.getString(C0014R$string.dialog_mw_button_is_valid_only_when_using_apps));
                launchApp(pendingIntent);
                return;
            }
            int topTaskId = getTopTaskId(topFullscreenStack);
            boolean z = false;
            ActivityManager.RecentTaskInfo recentTask = getRecentTask(str, false);
            if (recentTask != null && recentTask.persistentId == topTaskId) {
                z = true;
            }
            if (z) {
                showToast(this.mContext.getString(C0014R$string.dialog_open_app_with_foreground));
                launchApp(pendingIntent);
                return;
            }
            setForceMinimizedDisabled(true);
            launchPrimaryAppFromRecent(topTaskId, topFullscreenStack);
            this.mHandler.postDelayed(new Runnable(pendingIntent, str) {
                /* class com.sonymobile.systemui.multiwindowcontroller.$$Lambda$MultiWindowController$EziFkpEpeGi2QqZocofrc29rA4g */
                private final /* synthetic */ PendingIntent f$1;
                private final /* synthetic */ String f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    MultiWindowController.this.lambda$launchMultiWindow$0$MultiWindowController(this.f$1, this.f$2);
                }
            }, 500);
            return;
        }
        launchApp(pendingIntent);
    }

    public /* synthetic */ void lambda$launchMultiWindow$0$MultiWindowController(PendingIntent pendingIntent, String str) {
        setForceMinimizedDisabled(false);
        launchSecondaryApp(pendingIntent, str);
    }

    private static void setForceMinimizedDisabled(boolean z) {
        try {
            IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setForceMinimizedDisabled(z);
        } catch (RemoteException e) {
            String str = TAG;
            Log.e(str, "setForceMinimizedDisabled RemoteException: " + e);
        }
    }

    private void showToast(String str) {
        this.mHandler.post(new Runnable(str) {
            /* class com.sonymobile.systemui.multiwindowcontroller.$$Lambda$MultiWindowController$fQNhGbfyFQasUK1XwlxI6ozdPCU */
            private final /* synthetic */ String f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                MultiWindowController.this.lambda$showToast$1$MultiWindowController(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$showToast$1$MultiWindowController(String str) {
        Toast.makeText(this.mContext, str, 1).show();
    }

    private void launchApp(PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            Log.w(TAG, "PendingIntent is null");
            return;
        }
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            String str = TAG;
            Log.w(str, "Sending intent failed: " + e);
        }
    }

    private void launchSecondaryApp(PendingIntent pendingIntent, String str) {
        if (pendingIntent == null) {
            Log.w(TAG, "pendingIntent is null");
            return;
        }
        String appName = getAppName(str);
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "Cannot launch secondary app because package name is null or invalid");
            return;
        }
        if (TextUtils.isEmpty(appName)) {
            appName = "Unknown app";
        }
        ActivityOptions makeBasic = ActivityOptions.makeBasic();
        makeBasic.setLaunchWindowingMode(4);
        makeBasic.setSplitScreenCreateMode(1);
        ActivityManager.RecentTaskInfo recentTask = getRecentTask(str, false);
        if (recentTask == null) {
            try {
                pendingIntent.send(null, 0, null, null, null, null, makeBasic.toBundle());
            } catch (PendingIntent.CanceledException e) {
                String str2 = TAG;
                Log.w(str2, "Sending intent failed: " + e);
            }
        } else if (!recentTask.supportsSplitScreenMultiWindow) {
            showToast(this.mContext.getString(C0014R$string.activity_not_support_mw, appName));
        } else {
            int i = recentTask.persistentId;
            if (i > 0) {
                launchFromRecents(i, makeBasic);
                launchApp(pendingIntent);
            }
        }
    }

    private void launchPrimaryAppFromRecent(int i, ActivityManager.StackInfo stackInfo) {
        ActivityManager.RecentTaskInfo recentTask = getRecentTask(i);
        if (recentTask == null) {
            String appName = getAppName(getPackageName(stackInfo));
            showToast(this.mContext.getString(C0014R$string.activity_launch_with_mw_failed, appName));
        } else if (!recentTask.supportsSplitScreenMultiWindow) {
            CharSequence appName2 = getAppName(recentTask.realActivity);
            showToast(this.mContext.getString(C0014R$string.activity_not_support_mw, appName2));
        } else {
            ActivityOptions makeBasic = ActivityOptions.makeBasic();
            makeBasic.setLaunchWindowingMode(3);
            makeBasic.setSplitScreenCreateMode(0);
            launchFromRecents(i, makeBasic);
        }
    }

    private void launchFromRecents(int i, ActivityOptions activityOptions) {
        try {
            ActivityManager.getService().startActivityFromRecents(i, activityOptions.toBundle());
        } catch (RemoteException unused) {
            Log.e(TAG, "Failed to start activity from recents");
        }
    }

    private ActivityManager.RecentTaskInfo getRecentTask(String str, boolean z) {
        try {
            for (ActivityManager.RecentTaskInfo recentTaskInfo : ActivityManager.getService().getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), 1, UserHandle.myUserId()).getList()) {
                ComponentName componentName = recentTaskInfo.realActivity;
                int activityType = getActivityType(recentTaskInfo);
                if (componentName != null) {
                    if (isActivityTypeStandardOrUndefined(activityType)) {
                        if (str.equals(componentName.getPackageName()) && (z || recentTaskInfo.supportsSplitScreenMultiWindow)) {
                            return recentTaskInfo;
                        }
                    }
                }
            }
            return null;
        } catch (RemoteException unused) {
            Log.e(TAG, "Failed to get task info");
            return null;
        }
    }

    private ActivityManager.RecentTaskInfo getRecentTask(int i) {
        if (i == -1) {
            Log.e(TAG, "Cannot launch primary app because taskId is invalid");
            return null;
        }
        try {
            for (ActivityManager.RecentTaskInfo recentTaskInfo : ActivityManager.getService().getRecentTasks(ActivityManager.getMaxRecentTasksStatic(), 1, UserHandle.myUserId()).getList()) {
                int activityType = getActivityType(recentTaskInfo);
                if (recentTaskInfo.realActivity != null) {
                    if (isActivityTypeStandardOrUndefined(activityType)) {
                        if (recentTaskInfo.persistentId == i) {
                            return recentTaskInfo;
                        }
                    }
                }
            }
        } catch (RemoteException unused) {
            Log.e(TAG, "Failed to get task info");
        }
        return null;
    }

    private ActivityManager.StackInfo getTopFullscreenStack() {
        try {
            List allStackInfos = ActivityManager.getService().getAllStackInfos();
            int size = allStackInfos.size();
            for (int i = 0; i < size; i++) {
                ActivityManager.StackInfo stackInfo = (ActivityManager.StackInfo) allStackInfos.get(i);
                if (getWindowingMode(stackInfo) == 1 && getActivityType(stackInfo) != 4) {
                    return stackInfo;
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get top fullscreen stack", e);
            return null;
        }
    }

    private int getActivityType(ActivityManager.RecentTaskInfo recentTaskInfo) {
        if (recentTaskInfo == null) {
            return -1;
        }
        return recentTaskInfo.configuration.windowConfiguration.getActivityType();
    }

    private int getActivityType(ActivityManager.StackInfo stackInfo) {
        if (stackInfo == null) {
            return -1;
        }
        return stackInfo.configuration.windowConfiguration.getActivityType();
    }

    private int getWindowingMode(ActivityManager.StackInfo stackInfo) {
        if (stackInfo == null) {
            return -1;
        }
        return stackInfo.configuration.windowConfiguration.getWindowingMode();
    }

    private String getPackageName(ActivityManager.StackInfo stackInfo) {
        ComponentName componentName;
        if (stackInfo == null || (componentName = stackInfo.topActivity) == null) {
            return null;
        }
        return componentName.getPackageName();
    }

    private String getAppName(String str) {
        if (str == null) {
            return null;
        }
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(str, 0)).toString();
        } catch (PackageManager.NameNotFoundException unused) {
            String str2 = TAG;
            Log.w(str2, "Package not available: " + str);
            return null;
        }
    }

    private CharSequence getAppName(ComponentName componentName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            CharSequence loadLabel = packageManager.getActivityInfo(componentName, 0).loadLabel(packageManager);
            if (TextUtils.isEmpty(loadLabel)) {
                return packageManager.getApplicationLabel(packageManager.getApplicationInfo(componentName.getPackageName(), 128));
            }
            return loadLabel;
        } catch (PackageManager.NameNotFoundException e) {
            String str = TAG;
            Log.w(str, ".getAppName componentName=" + componentName + " NameNotFoundException=" + e);
            return null;
        }
    }

    private int getDockSide() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
        } catch (RemoteException e) {
            String str = TAG;
            Log.w(str, "Failed to get dock side: " + e);
            return -1;
        }
    }

    private int getTopTaskId(ActivityManager.StackInfo stackInfo) {
        int[] iArr;
        if (stackInfo == null || (iArr = stackInfo.taskIds) == null || iArr.length <= 0) {
            return -1;
        }
        return iArr[iArr.length - 1];
    }

    private void dismissPipIfNeeded(String str) {
        IActivityTaskManager service = ActivityTaskManager.getService();
        try {
            ActivityManager.StackInfo stackInfo = service.getStackInfo(2, 0);
            if (stackInfo != null) {
                List tasks = service.getTasks(1);
                ComponentName componentName = null;
                if (tasks != null && !tasks.isEmpty() && ((ActivityManager.RunningTaskInfo) tasks.get(0)).stackId == stackInfo.stackId) {
                    componentName = ((ActivityManager.RunningTaskInfo) tasks.get(0)).baseActivity;
                }
                if ((stackInfo.topActivity != null && str.equals(stackInfo.topActivity.getPackageName())) || (componentName != null && str.equals(componentName.getPackageName()))) {
                    service.removeStacksInWindowingModes(new int[]{2});
                    SystemClock.sleep(500);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to dismiss pip.", e);
        }
    }
}
