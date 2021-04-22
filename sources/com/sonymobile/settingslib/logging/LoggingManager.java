package com.sonymobile.settingslib.logging;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.sonyericsson.idd.api.Idd;
import com.sonymobile.settingslib.qs.Notifier;
import org.json.JSONException;
import org.json.JSONObject;

public class LoggingManager {
    private static final String TAG = "LoggingManager";

    public static void logQSEvent(Context context, String str, String str2, Object obj) {
        Notifier.onQsEvent(context, str, str2, obj);
        if (!"long_click".equals(str2)) {
            logEvent(context, "qs_event", str, str2, obj, 1);
        }
    }

    private static void logEvent(Context context, String str, String str2, String str3, Object obj, int i) {
        if ((i & 1) != 0) {
            sendIddEvent(context, str, str2, str3, obj);
        }
    }

    private static void sendIddEvent(Context context, String str, String str2, String str3, Object obj) {
        JSONObject jSONObject = new JSONObject();
        putValueToJSON(jSONObject, "type", str);
        if (str2 != null) {
            putValueToJSON(jSONObject, "sub_type", str2);
        }
        putValueToJSON(jSONObject, "action", str3);
        putValueToJSON(jSONObject, "value", obj);
        Idd.addAppDataJSON(context.getPackageName(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT, jSONObject);
    }

    private static void putValueToJSON(JSONObject jSONObject, String str, Object obj) {
        try {
            jSONObject.put(str, JSONObject.wrap(obj));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to put value to JSON", e);
        }
    }
}
