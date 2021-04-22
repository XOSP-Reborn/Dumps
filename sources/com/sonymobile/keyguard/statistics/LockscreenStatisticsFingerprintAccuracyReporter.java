package com.sonymobile.keyguard.statistics;

import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Build;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.Random;

public class LockscreenStatisticsFingerprintAccuracyReporter extends KeyguardUpdateMonitorCallback {
    private Context mContext;
    private long mFailedAttempts = 0;
    private long mFailedAttemptsWhenScreenOff = 0;

    public LockscreenStatisticsFingerprintAccuracyReporter(Context context) {
        this.mContext = context;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
        if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
            this.mFailedAttempts++;
            if (!KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn()) {
                this.mFailedAttemptsWhenScreenOff++;
            }
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
        if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
            int nextInt = new Random().nextInt(10);
            if (!"user".equals(Build.TYPE)) {
                sendIddReport();
            } else if (nextInt == 0) {
                sendIddReport();
            }
            this.mFailedAttempts = 0;
            this.mFailedAttemptsWhenScreenOff = 0;
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onKeyguardVisibilityChanged(boolean z) {
        if (!z) {
            this.mFailedAttempts = 0;
        }
    }

    public void sendIddReport() {
        Intent intent = new Intent();
        intent.setAction("com.sonymobile.lockscreen.idd.ACTION_FINGERPRINT_ACCURACY");
        intent.setPackage("com.sonyericsson.lockscreen.uxpnxt");
        intent.putExtra("is_screen_on", KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn());
        intent.putExtra("failure_count", this.mFailedAttempts);
        intent.putExtra("failure_count_screen_off", this.mFailedAttemptsWhenScreenOff);
        this.mContext.sendBroadcast(intent);
    }
}
