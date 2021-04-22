package com.android.systemui.doze;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.C0014R$string;
import com.android.systemui.doze.DozeSensors;
import com.android.systemui.plugins.SensorManagerPlugin;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AlarmTimeout;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class DozeSensors {
    private static final boolean DEBUG = DozeService.DEBUG;
    private final AlarmManager mAlarmManager;
    private final Callback mCallback;
    private final AmbientDisplayConfiguration mConfig;
    private final Context mContext;
    private long mDebounceFrom;
    private final DozeParameters mDozeParameters;
    private final Handler mHandler = new Handler();
    private final TriggerSensor mMotionSensorForSod;
    private final TriggerSensor mPickupSensor;
    private final TriggerSensor mPickupSensorForSod;
    private final Consumer<Boolean> mProxCallback;
    private final ProxSensor mProxSensor;
    private final ContentResolver mResolver;
    private final SensorManager mSensorManager;
    protected TriggerSensor[] mSensors;
    private boolean mSettingRegistered;
    private final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        /* class com.android.systemui.doze.DozeSensors.AnonymousClass1 */

        public void onChange(boolean z, Uri uri, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                for (TriggerSensor triggerSensor : DozeSensors.this.mSensors) {
                    triggerSensor.updateListener();
                }
            }
        }
    };
    private final WakeLock mWakeLock;

    public interface Callback {
        void onSensorPulse(int i, boolean z, float f, float f2, float[] fArr);
    }

    public DozeSensors(Context context, AlarmManager alarmManager, SensorManager sensorManager, DozeParameters dozeParameters, AmbientDisplayConfiguration ambientDisplayConfiguration, WakeLock wakeLock, Callback callback, Consumer<Boolean> consumer, AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
        this.mContext = context;
        this.mAlarmManager = alarmManager;
        this.mSensorManager = sensorManager;
        this.mDozeParameters = dozeParameters;
        this.mConfig = ambientDisplayConfiguration;
        this.mWakeLock = wakeLock;
        this.mProxCallback = consumer;
        this.mResolver = this.mContext.getContentResolver();
        boolean alwaysOnEnabled = this.mConfig.alwaysOnEnabled(-2);
        TriggerSensor[] triggerSensorArr = new TriggerSensor[9];
        triggerSensorArr[0] = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(17), null, dozeParameters.getPulseOnSigMotion(), 2, false, false);
        TriggerSensor triggerSensor = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(25), "doze_pulse_on_pick_up", ambientDisplayConfiguration.dozePickupSensorAvailable() && ambientDisplayConfiguration.ambientDisplayAvailable() && (!ambientDisplayConfiguration.alwaysOnAvailable() || !ambientDisplayConfiguration.accessibilityInversionEnabled(-2)), 3, false, false);
        this.mPickupSensor = triggerSensor;
        triggerSensorArr[1] = triggerSensor;
        triggerSensorArr[2] = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.doubleTapSensorType()), "doze_pulse_on_double_tap", true, 4, dozeParameters.doubleTapReportsTouchCoordinates(), true);
        triggerSensorArr[3] = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.tapSensorType()), "doze_tap_gesture", true, 9, false, true);
        triggerSensorArr[4] = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.longPressSensorType()), "doze_pulse_on_long_press", false, true, 5, true, true);
        triggerSensorArr[5] = new PluginSensor(this, new SensorManagerPlugin.Sensor(2), "doze_wake_screen_gesture", this.mConfig.wakeScreenGestureAvailable() && alwaysOnEnabled, 7, false, false);
        triggerSensorArr[6] = new PluginSensor(new SensorManagerPlugin.Sensor(1), "doze_wake_screen_gesture", this.mConfig.wakeScreenGestureAvailable() && alwaysOnEnabled, 8, false, false, this.mConfig.getWakeLockScreenDebounce());
        TriggerSensor triggerSensor2 = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(25), "doze_take_on", ambientDisplayConfiguration.alwaysOnAvailableForUser(-2), 3, false, false);
        this.mPickupSensorForSod = triggerSensor2;
        triggerSensorArr[7] = triggerSensor2;
        TriggerSensor triggerSensor3 = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(30), "doze_take_on", ambientDisplayConfiguration.alwaysOnAvailableForUser(-2), 20, false, false);
        this.mMotionSensorForSod = triggerSensor3;
        triggerSensorArr[8] = triggerSensor3;
        this.mSensors = triggerSensorArr;
        this.mProxSensor = new ProxSensor(alwaysOnDisplayPolicy);
        this.mCallback = callback;
    }

    public void requestTemporaryDisable() {
        this.mDebounceFrom = SystemClock.uptimeMillis();
    }

    private Sensor findSensorWithType(String str) {
        return findSensorWithType(this.mSensorManager, str);
    }

    static Sensor findSensorWithType(SensorManager sensorManager, String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        for (Sensor sensor : sensorManager.getSensorList(-1)) {
            if (str.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    public void setListening(boolean z) {
        for (TriggerSensor triggerSensor : this.mSensors) {
            triggerSensor.setListening(z);
        }
        registerSettingsObserverIfNeeded(z);
    }

    public void setTouchscreenSensorsListening(boolean z) {
        TriggerSensor[] triggerSensorArr = this.mSensors;
        for (TriggerSensor triggerSensor : triggerSensorArr) {
            if (triggerSensor.mRequiresTouchscreen) {
                triggerSensor.setListening(z);
            }
        }
    }

    public void onUserSwitched() {
        for (TriggerSensor triggerSensor : this.mSensors) {
            triggerSensor.updateListener();
        }
    }

    public void setProxListening(boolean z) {
        this.mProxSensor.setRequested(z);
    }

    public void setDisableSensorsInterferingWithProximity(boolean z) {
        this.mPickupSensor.setDisabled(z);
    }

    public void dump(PrintWriter printWriter) {
        TriggerSensor[] triggerSensorArr = this.mSensors;
        for (TriggerSensor triggerSensor : triggerSensorArr) {
            printWriter.print("  Sensor: ");
            printWriter.println(triggerSensor.toString());
        }
        printWriter.print("  ProxSensor: ");
        printWriter.println(this.mProxSensor.toString());
    }

    public Boolean isProximityCurrentlyFar() {
        return this.mProxSensor.mCurrentlyFar;
    }

    private void registerSettingsObserverIfNeeded(boolean z) {
        if (!z) {
            this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        } else if (!this.mSettingRegistered) {
            for (TriggerSensor triggerSensor : this.mSensors) {
                triggerSensor.registerSettingsObserver(this.mSettingsObserver);
            }
        }
        this.mSettingRegistered = z;
    }

    public Boolean isProximityCurrentlyRegistered() {
        return Boolean.valueOf(this.mProxSensor.mRegistered);
    }

    /* access modifiers changed from: private */
    public class ProxSensor implements SensorEventListener {
        final AlarmTimeout mCooldownTimer;
        Boolean mCurrentlyFar;
        long mLastNear;
        final AlwaysOnDisplayPolicy mPolicy;
        boolean mRegistered;
        boolean mRequested;
        final Sensor mSensor;

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public ProxSensor(AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
            this.mPolicy = alwaysOnDisplayPolicy;
            this.mCooldownTimer = new AlarmTimeout(DozeSensors.this.mAlarmManager, new AlarmManager.OnAlarmListener() {
                /* class com.android.systemui.doze.$$Lambda$DozeSensors$ProxSensor$1rrJyrKR8bANwbetqs61eKIcvs */

                public final void onAlarm() {
                    DozeSensors.ProxSensor.this.updateRegistered();
                }
            }, "prox_cooldown", DozeSensors.this.mHandler);
            Sensor findSensorWithType = DozeSensors.findSensorWithType(DozeSensors.this.mSensorManager, DozeSensors.this.mContext.getString(C0014R$string.doze_brightness_sensor_type));
            this.mSensor = findSensorWithType == null ? DozeSensors.this.mSensorManager.getDefaultSensor(8) : findSensorWithType;
        }

        /* access modifiers changed from: package-private */
        public void setRequested(boolean z) {
            if (this.mRequested == z) {
                DozeSensors.this.mHandler.post(new Runnable() {
                    /* class com.android.systemui.doze.$$Lambda$DozeSensors$ProxSensor$ocSoA7n0sI8mkM1nacSopw2_2Oc */

                    public final void run() {
                        DozeSensors.ProxSensor.this.lambda$setRequested$0$DozeSensors$ProxSensor();
                    }
                });
                return;
            }
            this.mRequested = z;
            updateRegistered();
        }

        public /* synthetic */ void lambda$setRequested$0$DozeSensors$ProxSensor() {
            if (this.mCurrentlyFar != null) {
                DozeSensors.this.mProxCallback.accept(this.mCurrentlyFar);
            }
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private void updateRegistered() {
            setRegistered(this.mRequested && !this.mCooldownTimer.isScheduled());
        }

        private void setRegistered(boolean z) {
            if (this.mRegistered != z) {
                if (z) {
                    this.mRegistered = DozeSensors.this.mSensorManager.registerListener(this, DozeSensors.this.mSensorManager.getDefaultSensor(8), 3, DozeSensors.this.mHandler);
                    return;
                }
                DozeSensors.this.mSensorManager.unregisterListener(this);
                this.mRegistered = false;
                this.mCurrentlyFar = null;
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            if (DozeSensors.DEBUG) {
                Log.d("DozeSensors", "onSensorChanged " + sensorEvent);
            }
            boolean z = false;
            if (sensorEvent.values[0] >= sensorEvent.sensor.getMaximumRange()) {
                z = true;
            }
            this.mCurrentlyFar = Boolean.valueOf(z);
            DozeSensors.this.mProxCallback.accept(this.mCurrentlyFar);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            Boolean bool = this.mCurrentlyFar;
            if (bool != null) {
                if (!bool.booleanValue()) {
                    this.mLastNear = elapsedRealtime;
                } else if (this.mCurrentlyFar.booleanValue()) {
                    long j = this.mPolicy.proxCooldownTriggerMs;
                }
            }
        }

        public String toString() {
            return String.format("{registered=%s, requested=%s, coolingDown=%s, currentlyFar=%s, sensor=%s}", Boolean.valueOf(this.mRegistered), Boolean.valueOf(this.mRequested), Boolean.valueOf(this.mCooldownTimer.isScheduled()), this.mCurrentlyFar, this.mSensor);
        }
    }

    /* access modifiers changed from: package-private */
    public class TriggerSensor extends TriggerEventListener {
        final boolean mConfigured;
        protected boolean mDisabled;
        protected boolean mIgnoresSetting;
        final int mPulseReason;
        protected boolean mRegistered;
        final boolean mReportsTouchCoordinates;
        protected boolean mRequested;
        final boolean mRequiresTouchscreen;
        final Sensor mSensor;
        final String mSetting;
        final boolean mSettingDefault;

        public TriggerSensor(DozeSensors dozeSensors, Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3) {
            this(dozeSensors, sensor, str, true, z, i, z2, z3);
        }

        public TriggerSensor(DozeSensors dozeSensors, Sensor sensor, String str, boolean z, boolean z2, int i, boolean z3, boolean z4) {
            this(sensor, str, z, z2, i, z3, z4, false);
        }

        private TriggerSensor(Sensor sensor, String str, boolean z, boolean z2, int i, boolean z3, boolean z4, boolean z5) {
            this.mSensor = sensor;
            this.mSetting = str;
            this.mSettingDefault = z;
            this.mConfigured = z2;
            this.mPulseReason = i;
            this.mReportsTouchCoordinates = z3;
            this.mRequiresTouchscreen = z4;
            this.mIgnoresSetting = z5;
        }

        public void setListening(boolean z) {
            if (this.mRequested != z) {
                this.mRequested = z;
                updateListener();
            }
        }

        public void setDisabled(boolean z) {
            if (this.mDisabled != z) {
                this.mDisabled = z;
                updateListener();
            }
        }

        public void updateListener() {
            if (this.mConfigured && this.mSensor != null) {
                if (this.mRequested && !this.mDisabled && ((enabledBySetting() || this.mIgnoresSetting) && !this.mRegistered)) {
                    this.mRegistered = DozeSensors.this.mSensorManager.requestTriggerSensor(this, this.mSensor);
                    if (DozeSensors.DEBUG) {
                        Log.d("DozeSensors", "requestTriggerSensor " + this.mRegistered);
                    }
                } else if (this.mRegistered) {
                    boolean cancelTriggerSensor = DozeSensors.this.mSensorManager.cancelTriggerSensor(this, this.mSensor);
                    if (DozeSensors.DEBUG) {
                        Log.d("DozeSensors", "cancelTriggerSensor " + cancelTriggerSensor);
                    }
                    this.mRegistered = false;
                }
            }
        }

        /* access modifiers changed from: protected */
        public boolean enabledBySetting() {
            if (!DozeSensors.this.mConfig.enabled(-2)) {
                return false;
            }
            if (TextUtils.isEmpty(this.mSetting)) {
                return true;
            }
            if (Settings.Secure.getIntForUser(DozeSensors.this.mResolver, this.mSetting, this.mSettingDefault ? 1 : 0, -2) != 0) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "{mRegistered=" + this.mRegistered + ", mRequested=" + this.mRequested + ", mDisabled=" + this.mDisabled + ", mConfigured=" + this.mConfigured + ", mIgnoresSetting=" + this.mIgnoresSetting + ", mSensor=" + this.mSensor + "}";
        }

        public void onTrigger(TriggerEvent triggerEvent) {
            if (this.mPulseReason < 12) {
                DozeLog.traceSensor(DozeSensors.this.mContext, this.mPulseReason);
            }
            DozeSensors.this.mHandler.post(DozeSensors.this.mWakeLock.wrap(new Runnable(triggerEvent) {
                /* class com.android.systemui.doze.$$Lambda$DozeSensors$TriggerSensor$O2XJN2HKJ96bSF_1qNx6jPKeFk */
                private final /* synthetic */ TriggerEvent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    DozeSensors.TriggerSensor.this.lambda$onTrigger$0$DozeSensors$TriggerSensor(this.f$1);
                }
            }));
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0073  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public /* synthetic */ void lambda$onTrigger$0$DozeSensors$TriggerSensor(android.hardware.TriggerEvent r9) {
            /*
            // Method dump skipped, instructions count: 128
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeSensors.TriggerSensor.lambda$onTrigger$0$DozeSensors$TriggerSensor(android.hardware.TriggerEvent):void");
        }

        public void registerSettingsObserver(ContentObserver contentObserver) {
            if (this.mConfigured && !TextUtils.isEmpty(this.mSetting)) {
                DozeSensors.this.mResolver.registerContentObserver(Settings.Secure.getUriFor(this.mSetting), false, DozeSensors.this.mSettingsObserver, -1);
            }
        }

        /* access modifiers changed from: protected */
        public String triggerEventToString(TriggerEvent triggerEvent) {
            if (triggerEvent == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder("SensorEvent[");
            sb.append(triggerEvent.timestamp);
            sb.append(',');
            sb.append(triggerEvent.sensor.getName());
            if (triggerEvent.values != null) {
                for (int i = 0; i < triggerEvent.values.length; i++) {
                    sb.append(',');
                    sb.append(triggerEvent.values[i]);
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /* access modifiers changed from: package-private */
    public class PluginSensor extends TriggerSensor implements SensorManagerPlugin.SensorEventListener {
        private long mDebounce;
        final SensorManagerPlugin.Sensor mPluginSensor;

        PluginSensor(DozeSensors dozeSensors, SensorManagerPlugin.Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3) {
            this(sensor, str, z, i, z2, z3, 0);
        }

        PluginSensor(SensorManagerPlugin.Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3, long j) {
            super(DozeSensors.this, null, str, z, i, z2, z3);
            this.mPluginSensor = sensor;
            this.mDebounce = j;
        }

        @Override // com.android.systemui.doze.DozeSensors.TriggerSensor
        public void updateListener() {
            if (this.mConfigured) {
                AsyncSensorManager asyncSensorManager = (AsyncSensorManager) DozeSensors.this.mSensorManager;
                if (this.mRequested && !this.mDisabled && ((enabledBySetting() || this.mIgnoresSetting) && !this.mRegistered)) {
                    asyncSensorManager.registerPluginListener(this.mPluginSensor, this);
                    this.mRegistered = true;
                    if (DozeSensors.DEBUG) {
                        Log.d("DozeSensors", "registerPluginListener");
                    }
                } else if (this.mRegistered) {
                    asyncSensorManager.unregisterPluginListener(this.mPluginSensor, this);
                    this.mRegistered = false;
                    if (DozeSensors.DEBUG) {
                        Log.d("DozeSensors", "unregisterPluginListener");
                    }
                }
            }
        }

        @Override // com.android.systemui.doze.DozeSensors.TriggerSensor
        public String toString() {
            return "{mRegistered=" + this.mRegistered + ", mRequested=" + this.mRequested + ", mDisabled=" + this.mDisabled + ", mConfigured=" + this.mConfigured + ", mIgnoresSetting=" + this.mIgnoresSetting + ", mSensor=" + this.mPluginSensor + "}";
        }

        private String triggerEventToString(SensorManagerPlugin.SensorEvent sensorEvent) {
            if (sensorEvent == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder("PluginTriggerEvent[");
            sb.append(sensorEvent.getSensor());
            sb.append(',');
            sb.append(sensorEvent.getVendorType());
            if (sensorEvent.getValues() != null) {
                for (int i = 0; i < sensorEvent.getValues().length; i++) {
                    sb.append(',');
                    sb.append(sensorEvent.getValues()[i]);
                }
            }
            sb.append(']');
            return sb.toString();
        }

        @Override // com.android.systemui.plugins.SensorManagerPlugin.SensorEventListener
        public void onSensorChanged(SensorManagerPlugin.SensorEvent sensorEvent) {
            DozeLog.traceSensor(DozeSensors.this.mContext, this.mPulseReason);
            DozeSensors.this.mHandler.post(DozeSensors.this.mWakeLock.wrap(new Runnable(sensorEvent) {
                /* class com.android.systemui.doze.$$Lambda$DozeSensors$PluginSensor$EFDqlQhDL6RwEmmtbTd8M88V_8Y */
                private final /* synthetic */ SensorManagerPlugin.SensorEvent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    DozeSensors.PluginSensor.this.lambda$onSensorChanged$0$DozeSensors$PluginSensor(this.f$1);
                }
            }));
        }

        public /* synthetic */ void lambda$onSensorChanged$0$DozeSensors$PluginSensor(SensorManagerPlugin.SensorEvent sensorEvent) {
            if (SystemClock.uptimeMillis() >= DozeSensors.this.mDebounceFrom + this.mDebounce) {
                if (DozeSensors.DEBUG) {
                    Log.d("DozeSensors", "onSensorEvent: " + triggerEventToString(sensorEvent));
                }
                DozeSensors.this.mCallback.onSensorPulse(this.mPulseReason, true, -1.0f, -1.0f, sensorEvent.getValues());
            } else if (DozeSensors.DEBUG) {
                Log.d("DozeSensors", "onSensorEvent dropped: " + triggerEventToString(sensorEvent));
            }
        }
    }
}
