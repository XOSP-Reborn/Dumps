package com.sonymobile.notifyassist;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.RemoteViews;
import com.android.internal.app.AssistUtils;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.util.NotificationChannels;

public class NotifyAssistUtils {
    private static boolean getBoolean(Context context, String str) {
        return context.getSharedPreferences("notifyassist_pref", 0).getBoolean(str, false);
    }

    public static boolean isCompletedNotifyNotification(Context context) {
        return getBoolean(context, "is_completed_notify_notification");
    }

    public static boolean isLaunchedGoogleAssistOnceByDoubleTap(Context context) {
        return getBoolean(context, "is_launch_assistant_once_by_double_tap");
    }

    public static boolean isTapNotifyNotification(Context context) {
        return getBoolean(context, "is_tap_notify_notification");
    }

    public static boolean isNeedToShowNotifyNotification(Context context) {
        return isOldTimePass(context) && isSetByDefaultGoogleAssistant(context);
    }

    public static boolean isOldTimePass(Context context) {
        return getBoolean(context, "is_old_time_pass");
    }

    public static boolean isSetAssistant(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "double_tap_power_gesture_mode", 0, ActivityManager.getCurrentUser()) == 2;
    }

    public static boolean isSetByDefaultGoogleAssistant(Context context) {
        ComponentName assistComponentForUser = new AssistUtils(context).getAssistComponentForUser(ActivityManager.getCurrentUser());
        if (assistComponentForUser == null) {
            return false;
        }
        return assistComponentForUser.getPackageName().equals("com.google.android.googlequicksearchbox");
    }

    private static void setBoolean(Context context, String str, boolean z) {
        SharedPreferences.Editor edit = context.getSharedPreferences("notifyassist_pref", 0).edit();
        edit.putBoolean(str, z);
        edit.apply();
    }

    public static boolean setCompletedNotifyNotification(Context context) {
        boolean z = isLaunchedGoogleAssistOnceByDoubleTap(context) || isTapNotifyNotification(context);
        setBoolean(context, "is_completed_notify_notification", z);
        return z;
    }

    public static void setLaunchedGoogleAssistOnceByDoubleTap(Context context) {
        setBoolean(context, "is_launch_assistant_once_by_double_tap", true);
    }

    public static void setTapNotifyNotification(Context context) {
        setBoolean(context, "is_tap_notify_notification", true);
    }

    public static void setOldTimePass(Context context) {
        setBoolean(context, "is_old_time_pass", true);
    }

    public static void sendNotifyNotification(Context context) {
        Intent intent = new Intent(context, NotifyActivity.class);
        intent.setFlags(536870912);
        intent.putExtra("KEY_NOTIFY_NOTIFICATION", true);
        PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 268435456);
        String string = context.getString(C0014R$string.power_key_double_tap_notification_title);
        String string2 = context.getString(C0014R$string.power_key_double_tap_notification_subtitle);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), C0010R$layout.notify_notification_expand_layout);
        String string3 = context.getString(17039502);
        Bundle bundle = new Bundle();
        bundle.putString("android.substName", string3);
        Notification build = new Notification.Builder(context, NotificationChannels.HINTS).setContentTitle(string).setContentText(string2).setSmallIcon(C0006R$drawable.notify_notification_small_icn).setAutoCancel(true).setShowWhen(false).setContentIntent(activity).setExtras(bundle).setStyle(new Notification.DecoratedCustomViewStyle()).setCustomBigContentView(remoteViews).build();
        build.flags |= 32;
        ((NotificationManager) context.getSystemService("notification")).notify(C0014R$string.app_label, build);
    }
}
