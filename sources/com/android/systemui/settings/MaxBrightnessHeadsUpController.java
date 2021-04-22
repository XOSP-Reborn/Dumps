package com.android.systemui.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.C0003R$bool;
import com.android.systemui.settings.MaxBrightnessHeadsUp;
import com.android.systemui.settings.ToggleSlider;
import com.sonymobile.systemui.emergencymode.EmergencyModeStatus;

public class MaxBrightnessHeadsUpController implements MaxBrightnessHeadsUp.Listener, ToggleSlider.OnSliderMaximizedListener {
    private static final String TAG = "MaxBrightnessHeadsUpController";
    private static boolean sUserResponded = false;
    private final Context mContext;
    private final MaxBrightnessHeadsUp mMaxBrightnessHeadsUp;

    public MaxBrightnessHeadsUpController(Context context, MaxBrightnessHeadsUp maxBrightnessHeadsUp) {
        this.mContext = context;
        this.mMaxBrightnessHeadsUp = maxBrightnessHeadsUp;
    }

    @Override // com.android.systemui.settings.ToggleSlider.OnSliderMaximizedListener
    public void onSliderMaximized() {
        if (this.mContext.getResources().getBoolean(C0003R$bool.config_enable_max_brightness_heads_up) && !sUserResponded && !isBrightnessModeAutomatic() && !EmergencyModeStatus.isEmergencyModeOn(this.mContext)) {
            if (isGameEnhancerLaunched()) {
                this.mMaxBrightnessHeadsUp.showWithGameEnhancer();
            } else {
                this.mMaxBrightnessHeadsUp.show();
            }
        }
    }

    @Override // com.android.systemui.settings.MaxBrightnessHeadsUp.Listener
    public void onUserResponded() {
        sUserResponded = true;
    }

    private boolean isBrightnessModeAutomatic() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2) == 1;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0036, code lost:
        if (r1 != null) goto L_0x0038;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0038, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0046, code lost:
        if (0 == 0) goto L_0x0049;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0049, code lost:
        return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isGameEnhancerLaunched() {
        /*
            r4 = this;
            android.content.Context r4 = r4.getGameEnhancerContext()
            r0 = 0
            if (r4 != 0) goto L_0x0008
            return r0
        L_0x0008:
            r1 = 0
            android.content.ContentResolver r4 = r4.getContentResolver()     // Catch:{ Exception -> 0x003e }
            java.lang.String r2 = "content://com.sonymobile.gameenhancer.provider/status"
            android.net.Uri r2 = android.net.Uri.parse(r2)     // Catch:{ Exception -> 0x003e }
            android.database.Cursor r1 = r4.query(r2, r1, r1, r1)     // Catch:{ Exception -> 0x003e }
            if (r1 == 0) goto L_0x0036
            int r4 = r1.getCount()     // Catch:{ Exception -> 0x003e }
            if (r4 <= 0) goto L_0x0036
            boolean r4 = r1.moveToFirst()     // Catch:{ Exception -> 0x003e }
            if (r4 == 0) goto L_0x0036
            java.lang.String r4 = "is_enabled"
            int r4 = r1.getColumnIndex(r4)     // Catch:{ Exception -> 0x003e }
            r2 = -1
            if (r4 == r2) goto L_0x0036
            int r4 = r1.getInt(r4)     // Catch:{ Exception -> 0x003e }
            r2 = 1
            if (r4 != r2) goto L_0x0036
            r0 = r2
        L_0x0036:
            if (r1 == 0) goto L_0x0049
        L_0x0038:
            r1.close()
            goto L_0x0049
        L_0x003c:
            r4 = move-exception
            goto L_0x004a
        L_0x003e:
            r4 = move-exception
            java.lang.String r2 = com.android.systemui.settings.MaxBrightnessHeadsUpController.TAG     // Catch:{ all -> 0x003c }
            java.lang.String r3 = "isGameEnhancerLaunched: "
            android.util.Log.e(r2, r3, r4)     // Catch:{ all -> 0x003c }
            if (r1 == 0) goto L_0x0049
            goto L_0x0038
        L_0x0049:
            return r0
        L_0x004a:
            if (r1 == 0) goto L_0x004f
            r1.close()
        L_0x004f:
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.settings.MaxBrightnessHeadsUpController.isGameEnhancerLaunched():boolean");
    }

    private Context getGameEnhancerContext() {
        Context context = null;
        try {
            context = this.mContext.createPackageContextAsUser("com.sonymobile.gameenhancer", 0, new UserHandle(ActivityManager.getCurrentUser()));
            if (context == null) {
                Log.w(TAG, "Context creation failed.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getGameEnhancerContext: ", e);
        }
        return context;
    }
}
