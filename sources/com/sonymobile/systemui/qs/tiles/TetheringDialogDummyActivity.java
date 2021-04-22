package com.sonymobile.systemui.qs.tiles;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.HotspotController;

public class TetheringDialogDummyActivity extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        startActivityForResult(new Intent("com.sonymobile.intent.action.TETHER_CONFIRMATION_DIALOG"), 3);
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 3) {
            Log.v("TetheringDialogDummy", "REQUEST_CODE_CONFIRM_DIALOG");
            if (i2 == -1) {
                ((HotspotController) Dependency.get(HotspotController.class)).setHotspotEnabled(true);
            }
            finish();
        }
    }

    public void onBackPressed() {
        finish();
    }
}
