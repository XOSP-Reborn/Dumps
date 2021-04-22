package com.sonymobile.systemui.qs.tiles;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.content.Context;
import java.util.List;

public class ForegroundChecker implements Runnable {
    String mActivity = getForegroundActivityName();
    private Context mContext;

    public ForegroundChecker(Context context) {
        this.mContext = context;
    }

    public void run() {
        StatusBarManager statusBarManager;
        if (!this.mActivity.equals(getForegroundActivityName()) && (statusBarManager = (StatusBarManager) this.mContext.getApplicationContext().getSystemService("statusbar")) != null) {
            statusBarManager.collapsePanels();
        }
    }

    private String getForegroundActivityName() {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
        if (runningTasks == null || runningTasks.size() <= 0) {
            return null;
        }
        return runningTasks.get(0).topActivity.getClassName();
    }
}
