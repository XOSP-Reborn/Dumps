package com.sonymobile.keyguard.aod;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class LaunchAlbumActivity extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setShowWhenLocked(true);
        launchAlbumActivity(Uri.parse(getIntent().getStringExtra("URI")));
    }

    private void launchAlbumActivity(Uri uri) {
        Intent intent = new Intent("com.sonymobile.album.action.VIEW");
        intent.setPackage("com.sonyericsson.album");
        intent.setData(uri);
        intent.setFlags(268435456);
        dismissKeyguardIfSwipe();
        startActivityForResult(intent, 0);
        finishAndRemoveTask();
    }

    private void dismissKeyguardIfSwipe() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService("keyguard");
        if (keyguardManager.inKeyguardRestrictedInputMode() && !keyguardManager.isDeviceSecure()) {
            keyguardManager.requestDismissKeyguard(this, null);
        }
    }
}
