package com.sonymobile.notifyassist;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import com.sonymobile.notifyassist.common.debugmode.DebugModeUtils;

public class NotifyNotificationJobScheduler extends JobService {
    private static final boolean DEBUG = Log.isLoggable("NotifyNotificationJobScheduler", 3);

    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    public void scheduleNotifyNotification(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        if (jobScheduler.getPendingJob(1) == null) {
            if (DEBUG) {
                Log.d("NotifyNotificationJobScheduler", "Job scheduled by NOTIFY_NOTIFICATION_JOB_ID");
            }
            jobScheduler.schedule(new JobInfo.Builder(1, new ComponentName(context, NotifyNotificationJobScheduler.class)).setPersisted(true).setMinimumLatency(getLatencyTime().longValue()).build());
        } else if (DEBUG) {
            Log.d("NotifyNotificationJobScheduler", "Already exist id, can not used it :1");
        }
    }

    public boolean onStartJob(JobParameters jobParameters) {
        if (DEBUG) {
            Log.d("NotifyNotificationJobScheduler", "onStartJob()");
        }
        new Thread(new Runnable() {
            /* class com.sonymobile.notifyassist.NotifyNotificationJobScheduler.AnonymousClass1 */

            public void run() {
                if (!NotifyAssistUtils.isLaunchedGoogleAssistOnceByDoubleTap(this) && NotifyAssistUtils.isSetByDefaultGoogleAssistant(this)) {
                    NotifyAssistUtils.sendNotifyNotification(this);
                }
            }
        }).start();
        setOldTimePassIfNeed(this);
        return false;
    }

    private Long getLatencyTime() {
        long latencyTimeForDebug = DebugModeUtils.getLatencyTimeForDebug();
        if (DEBUG) {
            Log.d("NotifyNotificationJobScheduler", "Job scheduler Latency is " + String.valueOf(latencyTimeForDebug));
        }
        return Long.valueOf(latencyTimeForDebug);
    }

    private void setOldTimePassIfNeed(Context context) {
        if (!NotifyAssistUtils.isOldTimePass(context)) {
            NotifyAssistUtils.setOldTimePass(context);
        }
    }
}
