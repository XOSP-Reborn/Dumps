package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.SharedPreferences;

public class LockscreenStatisticsPreference {
    public long getNextReportTime(Context context, long j) {
        return context.getSharedPreferences("LockscreenStatistics", 0).getLong("NEXT_REPORT_TIME", j);
    }

    public void setNextReportTime(Context context, long j) {
        SharedPreferences.Editor edit = context.getSharedPreferences("LockscreenStatistics", 0).edit();
        edit.putLong("NEXT_REPORT_TIME", j);
        edit.commit();
    }

    public long getNextReportTimeWeekly(Context context, long j) {
        return context.getSharedPreferences("LockscreenStatistics", 0).getLong("NEXT_WEEKLY_REPORT_TIME", j);
    }

    public void setNextReportTimeWeekly(Context context, long j) {
        SharedPreferences.Editor edit = context.getSharedPreferences("LockscreenStatistics", 0).edit();
        edit.putLong("NEXT_WEEKLY_REPORT_TIME", j);
        edit.commit();
    }
}
