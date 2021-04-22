package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.MathUtils;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.doze.AlwaysOnDisplayPolicy;
import com.android.systemui.tuner.TunerService;

public class DozeParameters implements TunerService.Tunable {
    public static final boolean FORCE_BLANKING = SystemProperties.getBoolean("debug.force_blanking", false);
    public static final boolean FORCE_NO_BLANKING = SystemProperties.getBoolean("debug.force_no_blanking", false);
    private static DozeParameters sInstance;
    private static IntInOutMatcher sPickupSubtypePerformsProxMatcher;
    private final AlwaysOnDisplayPolicy mAlwaysOnPolicy = new AlwaysOnDisplayPolicy(this.mContext);
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
    private final Context mContext;
    private boolean mControlScreenOffAnimation = (!getDisplayNeedsBlanking());
    private boolean mDozeAlwaysOn;
    private boolean mDozePickupOn;
    private boolean mDozeSmartOn;
    private final PowerManager mPowerManager = ((PowerManager) this.mContext.getSystemService(PowerManager.class));

    public static DozeParameters getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DozeParameters(context);
        }
        return sInstance;
    }

    @VisibleForTesting
    protected DozeParameters(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPowerManager.setDozeAfterScreenOff(!this.mControlScreenOffAnimation);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "doze_always_on", "accessibility_display_inversion_enabled", "doze_take_on", "doze_pulse_on_pick_up");
    }

    public boolean getDisplayStateSupported() {
        return getBoolean("doze.display.supported", C0003R$bool.doze_display_state_supported);
    }

    public boolean getDozeSuspendDisplayStateSupported() {
        return this.mContext.getResources().getBoolean(C0003R$bool.doze_suspend_display_state_supported);
    }

    public float getScreenBrightnessDoze() {
        return ((float) this.mContext.getResources().getInteger(17694887)) / 255.0f;
    }

    public int getPulseVisibleDuration() {
        return getInt("doze.pulse.duration.visible", C0008R$integer.doze_pulse_duration_visible);
    }

    public boolean getPulseOnSigMotion() {
        return getBoolean("doze.pulse.sigmotion", C0003R$bool.doze_pulse_on_significant_motion);
    }

    public boolean getProxCheckBeforePulse() {
        return getBoolean("doze.pulse.proxcheck", C0003R$bool.doze_proximity_check_before_pulse);
    }

    public int getPickupVibrationThreshold() {
        return getInt("doze.pickup.vibration.threshold", C0008R$integer.doze_pickup_vibration_threshold);
    }

    public long getWallpaperAodDuration() {
        if (shouldControlScreenOff()) {
            return 4500;
        }
        return this.mAlwaysOnPolicy.wallpaperVisibilityDuration;
    }

    public long getWallpaperFadeOutDuration() {
        return this.mAlwaysOnPolicy.wallpaperFadeOutDuration;
    }

    public boolean getAlwaysOn() {
        return this.mDozeAlwaysOn || this.mDozeSmartOn;
    }

    public boolean getSmartOn() {
        return this.mDozeSmartOn;
    }

    public boolean getPickupOn() {
        return this.mDozePickupOn;
    }

    public boolean getDisplayNeedsBlanking() {
        return FORCE_BLANKING || (!FORCE_NO_BLANKING && this.mContext.getResources().getBoolean(17891414));
    }

    public boolean shouldControlScreenOff() {
        return this.mControlScreenOffAnimation;
    }

    public void setControlScreenOffAnimation(boolean z) {
        if (this.mControlScreenOffAnimation != z) {
            this.mControlScreenOffAnimation = z;
            getPowerManager().setDozeAfterScreenOff(!z);
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public PowerManager getPowerManager() {
        return this.mPowerManager;
    }

    private boolean getBoolean(String str, int i) {
        return SystemProperties.getBoolean(str, this.mContext.getResources().getBoolean(i));
    }

    private int getInt(String str, int i) {
        return MathUtils.constrain(SystemProperties.getInt(str, this.mContext.getResources().getInteger(i)), 0, 60000);
    }

    private String getString(String str, int i) {
        return SystemProperties.get(str, this.mContext.getString(i));
    }

    public boolean getPickupSubtypePerformsProxCheck(int i) {
        String string = getString("doze.pickup.proxcheck", C0014R$string.doze_pickup_subtype_performs_proximity_check);
        if (TextUtils.isEmpty(string)) {
            return this.mContext.getResources().getBoolean(C0003R$bool.doze_pickup_performs_proximity_check);
        }
        IntInOutMatcher intInOutMatcher = sPickupSubtypePerformsProxMatcher;
        if (intInOutMatcher == null || !TextUtils.equals(string, intInOutMatcher.mSpec)) {
            sPickupSubtypePerformsProxMatcher = new IntInOutMatcher(string);
        }
        return sPickupSubtypePerformsProxMatcher.isIn(i);
    }

    public int getPulseVisibleDurationExtended() {
        return getPulseVisibleDuration() * 2;
    }

    public boolean doubleTapReportsTouchCoordinates() {
        return this.mContext.getResources().getBoolean(C0003R$bool.doze_double_tap_reports_touch_coordinates);
    }

    @Override // com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        this.mDozeAlwaysOn = this.mAmbientDisplayConfiguration.alwaysOnEnabled(-2);
        boolean z = false;
        this.mDozeSmartOn = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "doze_take_on", this.mAmbientDisplayConfiguration.alwaysOnAvailable() ? 1 : 0, -2) != 0 && !this.mAmbientDisplayConfiguration.accessibilityInversionEnabled(-2);
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "doze_pulse_on_pick_up", !this.mAmbientDisplayConfiguration.alwaysOnAvailable() ? 1 : 0, -2) != 0 && (!this.mAmbientDisplayConfiguration.alwaysOnAvailable() || !this.mAmbientDisplayConfiguration.accessibilityInversionEnabled(-2))) {
            z = true;
        }
        this.mDozePickupOn = z;
    }

    public AlwaysOnDisplayPolicy getPolicy() {
        return this.mAlwaysOnPolicy;
    }

    public static class IntInOutMatcher {
        private final boolean mDefaultIsIn;
        private final SparseBooleanArray mIsIn;
        final String mSpec;

        public IntInOutMatcher(String str) {
            String str2;
            if (!TextUtils.isEmpty(str)) {
                this.mSpec = str;
                this.mIsIn = new SparseBooleanArray();
                String[] split = str.split(",", -1);
                boolean z = false;
                boolean z2 = false;
                for (String str3 : split) {
                    if (str3.length() != 0) {
                        boolean z3 = str3.charAt(0) != '!';
                        if (z3) {
                            str2 = str3;
                        } else {
                            str2 = str3.substring(1);
                        }
                        if (str3.length() != 0) {
                            if (!"*".equals(str2)) {
                                int parseInt = Integer.parseInt(str2);
                                if (this.mIsIn.indexOfKey(parseInt) < 0) {
                                    this.mIsIn.put(parseInt, z3);
                                } else {
                                    throw new IllegalArgumentException("Illegal spec, `" + parseInt + "` must not appear multiple times in `" + str + "`");
                                }
                            } else if (!z) {
                                z2 = z3;
                                z = true;
                            } else {
                                throw new IllegalArgumentException("Illegal spec, `*` must not appear multiple times in `" + str + "`");
                            }
                        } else {
                            throw new IllegalArgumentException("Illegal spec, must not have zero-length items: `" + str + "`");
                        }
                    } else {
                        throw new IllegalArgumentException("Illegal spec, must not have zero-length items: `" + str + "`");
                    }
                }
                if (z) {
                    this.mDefaultIsIn = z2;
                    return;
                }
                throw new IllegalArgumentException("Illegal spec, must specify either * or !*");
            }
            throw new IllegalArgumentException("Spec must not be empty");
        }

        public boolean isIn(int i) {
            return this.mIsIn.get(i, this.mDefaultIsIn);
        }
    }
}
