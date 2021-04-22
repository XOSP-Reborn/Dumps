package com.sonymobile.systemui.statusbar;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.sonyericsson.idd.api.Idd;
import org.json.JSONException;
import org.json.JSONObject;

public class MultiWindowButtonLogger {
    private static final String TAG = "MultiWindowButtonLogger";

    public static void logEvent(Context context, String str) {
        JSONObject jSONObject = new JSONObject();
        putValueToJSON(jSONObject, "type", "mw_event");
        putValueToJSON(jSONObject, "action", str);
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
