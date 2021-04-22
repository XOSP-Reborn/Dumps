package com.sonymobile.keyguard.aod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.android.keyguard.R$id;
import com.android.keyguard.R$layout;
import com.android.keyguard.R$string;

public class PhotoPlaybackIntroductionActivity extends Activity {
    private static final Intent INTENT_LOCKSCREEN_PHOTOPLAYBACK = new Intent().setAction("com.sonymobile.settings.intent.action.LOCKSCREEN_AMBIENT_PHOTOPLAYBACK").setPackage("com.sonyericsson.lockscreen.uxpnxt").putExtra("SHOW_PERMISSION", true).setFlags(335544320);
    private Button mButton;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R$layout.somc_aod_introduction_view);
        setShowWhenLocked(true);
        judgeChangeToSummaryForMLC();
        this.mButton = (Button) findViewById(R$id.aod_introduction_continue_button);
        this.mButton.setOnClickListener(new View.OnClickListener() {
            /* class com.sonymobile.keyguard.aod.PhotoPlaybackIntroductionActivity.AnonymousClass1 */

            public void onClick(View view) {
                PhotoPlaybackIntroductionActivity.this.startPhotoPlaybackSettings();
            }
        });
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            hideSystemUI();
        } else {
            finishAndRemoveTask();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(18945284);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startPhotoPlaybackSettings() {
        startActivity(INTENT_LOCKSCREEN_PHOTOPLAYBACK);
    }

    private void judgeChangeToSummaryForMLC() {
        if (!PhotoPlaybackProviderUtils.isIdiInstalled(getApplicationContext())) {
            ((TextView) findViewById(R$id.aod_introduction_summary_txt)).setText(R$string.lockscreen_ambient_q_recall_demo_summary_noloc_txt);
        }
    }
}
