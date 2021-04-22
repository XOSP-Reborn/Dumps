package com.google.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.internal.app.AssistUtils;

public class AssistantStateReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        boolean booleanExtra = intent.getBooleanExtra("OPA_ENABLED", false);
        Log.i("AssistantStateReceiver", "Received " + intent + " with enabled = " + booleanExtra);
        UserSettingsUtils.save(context.getContentResolver(), booleanExtra);
        new OpaEnableDispatcher(context, new AssistUtils(context)).dispatchOpaEnabled(booleanExtra);
    }
}
