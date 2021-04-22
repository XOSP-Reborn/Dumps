package com.sonymobile.settingslib.qs;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public class Notifier {
    private static void sendBroadcast(Context context, String str, String str2) {
        Intent intent = new Intent("com.sonymobile.settingslib.intent.action.QUICK_SETTINGS_EVENT");
        intent.putExtra("com.sonymobile.settingslib.intent.extra.EVENT_TYPE", str);
        intent.putExtra("com.sonymobile.settingslib.intent.extra.TILE_SPEC", str2);
        context.getApplicationContext().sendBroadcastAsUser(intent, UserHandle.ALL, "com.sonymobile.systemui.permission.RECEIVE_QS_EVENTS");
    }

    public static void onClickEvent(Context context, String str) {
        sendBroadcast(context, "click", str);
    }

    public static void onLongClickEvent(Context context, String str) {
        sendBroadcast(context, "long_click", str);
    }

    public static void onQsEvent(Context context, String str, String str2, Object obj) {
        if ("click".equals(str2)) {
            if ("custom".equals(str)) {
                onClickEvent(context, "custom(" + obj.toString() + ")");
                return;
            }
            onClickEvent(context, str);
        } else if ("long_click".equals(str2)) {
            onLongClickEvent(context, str);
        }
    }
}
