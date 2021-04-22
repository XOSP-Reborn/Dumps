package com.sonymobile.notifyassist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;

public class NotifyActivity extends Activity {
    private static final boolean DEBUG = Log.isLoggable("NotifyActivity", 3);
    private Button mButton;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(C0010R$layout.notify_activity);
        this.mButton = (Button) findViewById(C0007R$id.notify_activity_button);
        setPreferenceIfNeed();
    }

    public void onStart() {
        super.onStart();
        updateButtonVisibility();
    }

    public void onButtonClicked(View view) {
        Intent intent = new Intent();
        intent.setClassName("com.sonyericsson.settings", "com.sonymobile.settings.powerbtnoption.PowerBtnOptionSettings");
        startActivity(intent);
    }

    private void updateButtonVisibility() {
        this.mButton.setVisibility((!NotifyAssistUtils.isSetByDefaultGoogleAssistant(getApplicationContext()) || !NotifyAssistUtils.isSetAssistant(getApplicationContext())) ? 0 : 8);
    }

    private void setPreferenceIfNeed() {
        if (!NotifyAssistUtils.isTapNotifyNotification(getApplicationContext())) {
            if (DEBUG) {
                Log.d("NotifyActivity", "set is tap Notify Notification");
            }
            NotifyAssistUtils.setTapNotifyNotification(getApplicationContext());
        }
    }
}
