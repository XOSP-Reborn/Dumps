package com.sonymobile.notifyassist;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.sonymobile.notifyassist.common.debugmode.DebugModeUtils;

public class NotifyBootReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = Log.isLoggable("NotifyBootReceiver", 3);
    private static final Long NOTIFY_NOTIFICATION_MARGIN_TIME = 600000L;

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DEBUG) {
            Log.d("NotifyBootReceiver", "onReceive: " + action);
        }
        if (!"android.intent.action.BOOT_COMPLETED".equals(action)) {
            return;
        }
        if (isAmazonProduct(context)) {
            if (DEBUG) {
                Log.d("NotifyBootReceiver", "Do not schedule Notify because Amazon Phone");
            }
            if (!NotifyAssistUtils.isLaunchedGoogleAssistOnceByDoubleTap(context)) {
                NotifyAssistUtils.setLaunchedGoogleAssistOnceByDoubleTap(context);
            }
        } else if (!NotifyAssistUtils.isCompletedNotifyNotification(context) && !NotifyAssistUtils.setCompletedNotifyNotification(context)) {
            if (DEBUG) {
                Log.d("NotifyBootReceiver", "not Completed Notify Notification");
            }
            startNotifyNotificationJobScheduler(context);
        }
    }

    private boolean isAmazonProduct(Context context) {
        try {
            if ((context.getPackageManager().getApplicationInfo("com.quicinc.voice.activation", 0).flags & 1) != 0) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException unused) {
        }
    }

    private void startNotifyNotificationJobScheduler(Context context) {
        if (NotifyAssistUtils.isNeedToShowNotifyNotification(context)) {
            NotifyAssistUtils.sendNotifyNotification(context);
            return;
        }
        debugModeNotifyNotificationIfNeed(context);
        new NotifyNotificationJobScheduler().scheduleNotifyNotification(context);
    }

    private void debugModeNotifyNotificationIfNeed(Context context) {
        JobScheduler jobScheduler;
        JobInfo pendingJob;
        long latencyTimeForDebug = DebugModeUtils.getLatencyTimeForDebug();
        if (latencyTimeForDebug != 1209600000 && (pendingJob = (jobScheduler = (JobScheduler) context.getSystemService("jobscheduler")).getPendingJob(1)) != null && pendingJob.getMinLatencyMillis() >= latencyTimeForDebug + NOTIFY_NOTIFICATION_MARGIN_TIME.longValue()) {
            Log.d("NotifyBootReceiver", "Cancel the job scheduler for debug.");
            jobScheduler.cancel(1);
        }
    }
}
