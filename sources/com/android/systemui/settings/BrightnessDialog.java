package com.android.systemui.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;

public class BrightnessDialog extends Activity {
    private BrightnessController mBrightnessController;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Window window = getWindow();
        window.setGravity(48);
        window.clearFlags(2);
        window.requestFeature(1);
        setContentView(LayoutInflater.from(this).inflate(C0010R$layout.quick_settings_brightness_dialog, (ViewGroup) null));
        this.mBrightnessController = new BrightnessController(this, (ToggleSliderView) findViewById(C0007R$id.brightness_slider));
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        this.mBrightnessController.registerCallbacks();
        MetricsLogger.visible(this, 220);
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        MetricsLogger.hidden(this, 220);
        this.mBrightnessController.unregisterCallbacks();
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 25 || i == 24 || i == 164) {
            finish();
        }
        return super.onKeyDown(i, keyEvent);
    }
}
