package com.android.systemui.screenrecord;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;

public class ScreenRecordDialog extends Activity {
    private boolean mShowTaps;
    private boolean mUseAudio;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(C0010R$layout.screen_record_dialog);
        ((Button) findViewById(C0007R$id.record_button)).setOnClickListener(new View.OnClickListener((CheckBox) findViewById(C0007R$id.checkbox_mic), (CheckBox) findViewById(C0007R$id.checkbox_taps)) {
            /* class com.android.systemui.screenrecord.$$Lambda$ScreenRecordDialog$H9qHbhSc2WYgQqs87Jfr3hmOoA */
            private final /* synthetic */ CheckBox f$1;
            private final /* synthetic */ CheckBox f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void onClick(View view) {
                ScreenRecordDialog.this.lambda$onCreate$0$ScreenRecordDialog(this.f$1, this.f$2, view);
            }
        });
    }

    public /* synthetic */ void lambda$onCreate$0$ScreenRecordDialog(CheckBox checkBox, CheckBox checkBox2, View view) {
        this.mUseAudio = checkBox.isChecked();
        this.mShowTaps = checkBox2.isChecked();
        Log.d("ScreenRecord", "Record button clicked: audio " + this.mUseAudio + ", taps " + this.mShowTaps);
        if (!this.mUseAudio || checkSelfPermission("android.permission.RECORD_AUDIO") == 0) {
            requestScreenCapture();
            return;
        }
        Log.d("ScreenRecord", "Requesting permission for audio");
        requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 399);
    }

    private void requestScreenCapture() {
        Intent createScreenCaptureIntent = ((MediaProjectionManager) getSystemService("media_projection")).createScreenCaptureIntent();
        if (this.mUseAudio) {
            startActivityForResult(createScreenCaptureIntent, this.mShowTaps ? 301 : 300);
        } else {
            startActivityForResult(createScreenCaptureIntent, this.mShowTaps ? 201 : 200);
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int i, int i2, Intent intent) {
        boolean z = true;
        this.mShowTaps = i == 201 || i == 301;
        if (!(i == 200 || i == 201)) {
            if (i != 399) {
                switch (i) {
                    case 299:
                        if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                            Toast.makeText(this, getResources().getString(C0014R$string.screenrecord_permission_error), 0).show();
                            finish();
                            return;
                        }
                        requestScreenCapture();
                        return;
                    case 300:
                    case 301:
                        break;
                    default:
                        return;
                }
            } else {
                int checkSelfPermission = checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
                int checkSelfPermission2 = checkSelfPermission("android.permission.RECORD_AUDIO");
                if (checkSelfPermission == 0 && checkSelfPermission2 == 0) {
                    requestScreenCapture();
                    return;
                }
                Toast.makeText(this, getResources().getString(C0014R$string.screenrecord_permission_error), 0).show();
                finish();
                return;
            }
        }
        if (i2 == -1) {
            if (!(i == 300 || i == 301)) {
                z = false;
            }
            this.mUseAudio = z;
            startForegroundService(RecordingService.getStartIntent(this, i2, intent, this.mUseAudio, this.mShowTaps));
        } else {
            Toast.makeText(this, getResources().getString(C0014R$string.screenrecord_permission_error), 0).show();
        }
        finish();
    }
}
