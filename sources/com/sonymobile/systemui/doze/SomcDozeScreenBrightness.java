package com.sonymobile.systemui.doze;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Trace;
import com.android.systemui.C0008R$integer;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.statusbar.phone.DozeParameters;

public class SomcDozeScreenBrightness implements DozeMachine.Part, SensorEventListener {
    private final int mAodBrightnessBright;
    private final int mAodBrightnessDark;
    private final int mAodScrimOpacity;
    private final int mBrightnessSwitchLuxToBright;
    private final int mBrightnessSwitchLuxToDark;
    private final Context mContext;
    private final DozeHost mDozeHost;
    private final DozeMachine.Service mDozeService;
    private final Handler mHandler;
    private int mLastAodBrightness = -1;
    private int mLatestSensorValue = -1;
    private final Sensor mLightSensor;
    private DozeParameters mParameters;
    private boolean mPaused = false;
    private int mPreviousSensorValue = -1;
    private boolean mRegistered;
    private final SensorManager mSensorManager;

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public SomcDozeScreenBrightness(Context context, DozeMachine.Service service, SensorManager sensorManager, Sensor sensor, DozeHost dozeHost, Handler handler, DozeParameters dozeParameters) {
        this.mContext = context;
        this.mDozeService = service;
        this.mSensorManager = sensorManager;
        this.mLightSensor = sensor;
        this.mDozeHost = dozeHost;
        this.mHandler = handler;
        Resources resources = this.mContext.getResources();
        this.mBrightnessSwitchLuxToDark = resources.getInteger(C0008R$integer.config_aodBrightnessSwitchToDark);
        this.mBrightnessSwitchLuxToBright = resources.getInteger(C0008R$integer.config_aodBrightnessSwitchToBright);
        this.mAodBrightnessDark = resources.getInteger(C0008R$integer.config_aodScreenBrightnessDark);
        this.mAodBrightnessBright = resources.getInteger(C0008R$integer.config_aodScreenBrightnessBright);
        this.mAodScrimOpacity = resources.getInteger(C0008R$integer.config_aodScrimOpacity);
        this.mLastAodBrightness = this.mAodBrightnessBright;
        this.mParameters = dozeParameters;
    }

    /* renamed from: com.sonymobile.systemui.doze.SomcDozeScreenBrightness$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$doze$DozeMachine$State = new int[DozeMachine.State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(12:0|1|2|3|4|5|6|7|8|9|10|(3:11|12|14)) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
                com.android.systemui.doze.DozeMachine$State[] r0 = com.android.systemui.doze.DozeMachine.State.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State = r0
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.INITIALIZED     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_REQUEST_PULSE     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD_PAUSED     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r2 = 5
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                int[] r0 = com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x004b }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.FINISH     // Catch:{ NoSuchFieldError -> 0x004b }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r2 = 6
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.sonymobile.systemui.doze.SomcDozeScreenBrightness.AnonymousClass1.<clinit>():void");
        }
    }

    @Override // com.android.systemui.doze.DozeMachine.Part
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        boolean z = true;
        switch (AnonymousClass1.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state2.ordinal()]) {
            case 1:
                resetBrightnessToDefault();
                break;
            case 2:
            case 3:
                setLightSensorEnabled(true);
                break;
            case 4:
                setLightSensorEnabled(false);
                break;
            case 5:
                setLightSensorEnabled(false);
                resetBrightnessToDefault();
                break;
            case 6:
                setLightSensorEnabled(false);
                break;
        }
        if (state2 != DozeMachine.State.FINISH) {
            if (state2 != DozeMachine.State.DOZE_AOD_PAUSED) {
                z = false;
            }
            setPaused(z);
        }
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        Trace.beginSection("SomcDozeScreenBrightness.onSensorChanged" + sensorEvent.values[0]);
        try {
            if (this.mRegistered) {
                this.mLatestSensorValue = (int) sensorEvent.values[0];
                updateBrightnessAndReady();
            }
        } finally {
            Trace.endSection();
        }
    }

    private void updateBrightnessAndReady() {
        boolean z = false;
        if (this.mRegistered) {
            int computeBrightness = computeBrightness(this.mLatestSensorValue, this.mLastAodBrightness);
            if (computeBrightness > 0) {
                z = true;
            }
            if (z) {
                this.mDozeService.setDozeScreenBrightness(computeBrightness);
                this.mLastAodBrightness = computeBrightness;
            }
        }
        int i = -1;
        if (this.mPaused) {
            i = 255;
        } else if (z) {
            i = computeScrimOpacity(this.mLatestSensorValue);
        }
        if (i >= 0) {
            this.mDozeHost.setAodDimmingScrim(((float) i) / 255.0f);
        }
    }

    private int computeScrimOpacity(int i) {
        if (i < 0) {
            return -1;
        }
        return this.mAodScrimOpacity;
    }

    private int computeBrightness(int i, int i2) {
        if (i < 0) {
            return -1;
        }
        if (i2 != this.mAodBrightnessDark || this.mBrightnessSwitchLuxToBright >= i) {
            return this.mBrightnessSwitchLuxToDark > i ? this.mAodBrightnessDark : i2;
        }
        return this.mAodBrightnessBright;
    }

    private void resetBrightnessToDefault() {
        this.mDozeService.setDozeScreenBrightness(this.mLastAodBrightness);
        this.mDozeHost.setAodDimmingScrim(0.0f);
    }

    private void setLightSensorEnabled(boolean z) {
        Sensor sensor;
        if (z && !this.mRegistered && (sensor = this.mLightSensor) != null) {
            this.mRegistered = this.mSensorManager.registerListener(this, sensor, 3, this.mHandler);
            this.mLatestSensorValue = -1;
            this.mPreviousSensorValue = -1;
        } else if (!z && this.mRegistered) {
            this.mSensorManager.unregisterListener(this);
            this.mRegistered = false;
            this.mLatestSensorValue = -1;
            this.mPreviousSensorValue = -1;
        }
    }

    private void setPaused(boolean z) {
        if (this.mPaused != z) {
            this.mPaused = z;
            updateBrightnessAndReady();
        }
    }
}
