package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.os.Build;
import com.sonymobile.keyguard.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LockscreenStatisticsReportTimeChecker implements Runnable {
    private static final long DEBUG_REPORT_INTERVAL_MS = TimeUnit.MINUTES.toMillis(3);
    private static final long INTERNAL_REPORT_INTERVAL_MS = TimeUnit.DAYS.toMillis(3);
    private static final long REPORT_INTERVAL_MS = TimeUnit.DAYS.toMillis(15);
    private static final long REPORT_WEEKLY_INTERVAL_MS = TimeUnit.DAYS.toMillis(7);
    private final Context mContext;
    private final Clock mCurrentTimeClock;
    private final LockscreenStatisticsHelper mLockscreenStatisticsHelper;
    private final LockscreenStatisticsPreference mLockscreenStatisticsPreference;

    public LockscreenStatisticsReportTimeChecker(Context context, Clock clock, LockscreenStatisticsPreference lockscreenStatisticsPreference, LockscreenStatisticsHelper lockscreenStatisticsHelper) {
        this.mContext = context;
        this.mCurrentTimeClock = clock;
        this.mLockscreenStatisticsPreference = lockscreenStatisticsPreference;
        this.mLockscreenStatisticsHelper = lockscreenStatisticsHelper;
    }

    public void run() {
        if (checkTime()) {
            report();
        }
        if (checkTimeWeekly()) {
            reportWeekly();
        }
    }

    private boolean checkTime() {
        long nextReportTime = this.mLockscreenStatisticsPreference.getNextReportTime(this.mContext, -1);
        long timeInMillis = this.mCurrentTimeClock.getTimeInMillis();
        boolean z = false;
        if (nextReportTime == -1 || ((double) nextReportTime) > ((double) timeInMillis) + (((double) getReportTimeInterval()) * 1.5d)) {
            setNextReportTime();
        } else {
            if (nextReportTime < timeInMillis) {
                z = true;
            }
            if (z) {
                setNextReportTime();
            }
        }
        return z;
    }

    private long getReportTimeInterval() {
        if (isEngOrUserdebugVariant()) {
            return INTERNAL_REPORT_INTERVAL_MS;
        }
        return REPORT_INTERVAL_MS;
    }

    private void report() {
        List<LockscreenStatisticsReporter> reporters;
        if (!(this.mLockscreenStatisticsHelper == null || (reporters = LockscreenStatisticsHelper.getReporters(this.mContext)) == null)) {
            for (LockscreenStatisticsReporter lockscreenStatisticsReporter : reporters) {
                if (lockscreenStatisticsReporter != null) {
                    lockscreenStatisticsReporter.sendIddReport();
                }
            }
        }
    }

    private void setNextReportTime() {
        this.mLockscreenStatisticsPreference.setNextReportTime(this.mContext, this.mCurrentTimeClock.getTimeInMillis() + getReportTimeInterval());
    }

    private boolean isEngOrUserdebugVariant() {
        return "eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE);
    }

    private boolean checkTimeWeekly() {
        long nextReportTimeWeekly = this.mLockscreenStatisticsPreference.getNextReportTimeWeekly(this.mContext, -1);
        long timeInMillis = this.mCurrentTimeClock.getTimeInMillis();
        boolean z = false;
        if (nextReportTimeWeekly == -1 || ((double) nextReportTimeWeekly) > ((double) timeInMillis) + (((double) getReportTimeIntervalWeekly()) * 1.5d)) {
            setNextReportTimeWeekly();
        } else {
            if (nextReportTimeWeekly < timeInMillis) {
                z = true;
            }
            if (z) {
                setNextReportTimeWeekly();
            }
        }
        return z;
    }

    private long getReportTimeIntervalWeekly() {
        return REPORT_WEEKLY_INTERVAL_MS;
    }

    private void reportWeekly() {
        List<LockscreenStatisticsReporter> reportersWeekly = LockscreenStatisticsHelper.getReportersWeekly(this.mContext);
        if (reportersWeekly != null) {
            for (LockscreenStatisticsReporter lockscreenStatisticsReporter : reportersWeekly) {
                if (lockscreenStatisticsReporter != null) {
                    lockscreenStatisticsReporter.sendIddReport();
                }
            }
        }
    }

    public void setNextReportTimeWeekly() {
        this.mLockscreenStatisticsPreference.setNextReportTimeWeekly(this.mContext, this.mCurrentTimeClock.getTimeInMillis() + getReportTimeIntervalWeekly());
    }
}
