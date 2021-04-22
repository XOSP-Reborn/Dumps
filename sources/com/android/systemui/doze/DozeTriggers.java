package com.android.systemui.doze;

import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.systemui.Dependency;
import com.android.systemui.dock.DockManager;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.doze.DozeSensors;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AlarmTimeout;
import com.android.systemui.util.Assert;
import com.android.systemui.util.wakelock.WakeLock;
import com.sonymobile.keyguard.aod.AodStateReportHelper;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback;
import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class DozeTriggers implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    private static boolean sWakeDisplaySensorState = true;
    private final boolean mAllowPulseTriggers;
    private final TriggerReceiver mBroadcastReceiver = new TriggerReceiver();
    private final AlarmTimeout mCalibProxTimer;
    private final AmbientDisplayConfiguration mConfig;
    private final Context mContext;
    private final DockEventListener mDockEventListener = new DockEventListener();
    private final DockManager mDockManager;
    private final DozeHost mDozeHost;
    private final DozeParameters mDozeParameters;
    private final DozeSensors mDozeSensors;
    private final Handler mHandler;
    private DozeHost.Callback mHostCallback = new DozeHost.Callback() {
        /* class com.android.systemui.doze.DozeTriggers.AnonymousClass2 */

        @Override // com.android.systemui.doze.DozeHost.Callback
        public void onNotificationAlerted() {
            DozeTriggers.this.onNotification();
        }

        @Override // com.android.systemui.doze.DozeHost.Callback
        public void onPowerSaveChanged(boolean z) {
            if (z) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
        }
    };
    private final LockscreenStyleCoverController mLockscreenStyleCoverController;
    private final DozeMachine mMachine;
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private long mNotificationPulseTime;
    private boolean mPulsePending;
    private int mReason;
    private final SensorManager mSensorManager;
    private boolean mSodExtTimeouted = false;
    private final AlarmTimeout mSodExtTimer;
    private final AlarmTimeout mSodTimer;
    private LockscreenStyleCoverControllerCallback mStyleCoverCallback = new LockscreenStyleCoverControllerCallback() {
        /* class com.android.systemui.doze.DozeTriggers.AnonymousClass3 */

        @Override // com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback
        public void onStyleCoverClosed(boolean z) {
            if (z) {
                DozeTriggers dozeTriggers = DozeTriggers.this;
                dozeTriggers.showAmbientDisplay(dozeTriggers.mDozeParameters.getSmartOn());
            }
        }
    };
    private final UiModeManager mUiModeManager;
    private final WakeLock mWakeLock;

    public DozeTriggers(Context context, DozeMachine dozeMachine, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, SensorManager sensorManager, Handler handler, WakeLock wakeLock, boolean z, DockManager dockManager) {
        this.mContext = context;
        this.mMachine = dozeMachine;
        this.mDozeHost = dozeHost;
        this.mConfig = ambientDisplayConfiguration;
        this.mDozeParameters = dozeParameters;
        this.mSensorManager = sensorManager;
        this.mHandler = handler;
        this.mWakeLock = wakeLock;
        this.mAllowPulseTriggers = z;
        this.mDozeSensors = new DozeSensors(context, alarmManager, this.mSensorManager, dozeParameters, ambientDisplayConfiguration, wakeLock, new DozeSensors.Callback() {
            /* class com.android.systemui.doze.$$Lambda$kkl61cam7GE2Q1hXP_ErF91yLeg */

            @Override // com.android.systemui.doze.DozeSensors.Callback
            public final void onSensorPulse(int i, boolean z, float f, float f2, float[] fArr) {
                DozeTriggers.this.onSensor(i, z, f, f2, fArr);
            }
        }, new Consumer() {
            /* class com.android.systemui.doze.$$Lambda$DozeTriggers$ulqUMEXi8OgK7771oZ9BOr21BBk */

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DozeTriggers.this.onProximityFar(((Boolean) obj).booleanValue());
            }
        }, dozeParameters.getPolicy());
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        this.mDockManager = dockManager;
        this.mSodTimer = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            /* class com.android.systemui.doze.$$Lambda$DozeTriggers$Hk7D9DokrIOf8jrRympre4WsAk */

            public final void onAlarm() {
                DozeTriggers.this.sodTimeout();
            }
        }, "sod_timer", this.mHandler);
        this.mSodExtTimer = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            /* class com.android.systemui.doze.$$Lambda$DozeTriggers$jgQFnrg5MVDUvbAzHBymTtw785g */

            public final void onAlarm() {
                DozeTriggers.this.sodExtensionTimeout();
            }
        }, "sod_ext_timer", this.mHandler);
        this.mCalibProxTimer = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            /* class com.android.systemui.doze.$$Lambda$DozeTriggers$ncROSNI2UGg4qZVzxBtbfznUA */

            public final void onAlarm() {
                DozeTriggers.this.calibProx();
            }
        }, "calib_prox", this.mHandler);
        this.mLockscreenStyleCoverController = (LockscreenStyleCoverController) Dependency.get(LockscreenStyleCoverController.class);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sodTimeout() {
        this.mMachine.requestState(DozeMachine.State.DOZE);
        if (this.mSodExtTimer.isScheduled()) {
            this.mSodExtTimer.cancel();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void sodExtensionTimeout() {
        this.mSodExtTimeouted = true;
        if (this.mSodTimer.isScheduled()) {
            this.mSodTimer.cancel();
        }
        this.mMachine.requestState(DozeMachine.State.DOZE);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void calibProx() {
        if (this.mDozeSensors.isProximityCurrentlyRegistered().booleanValue()) {
            this.mDozeSensors.setProxListening(false);
            this.mDozeSensors.setProxListening(true);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onNotification() {
        if (DozeMachine.DEBUG) {
            Log.d("DozeTriggers", "requestNotificationPulse");
        }
        this.mNotificationPulseTime = SystemClock.elapsedRealtime();
        if (!this.mConfig.pulseOnNotificationEnabled(-2)) {
            return;
        }
        if (!this.mConfig.alwaysOnAvailable() || !this.mConfig.accessibilityInversionEnabled(-2)) {
            requestPulse(1, false);
            DozeLog.traceNotificationPulse(this.mContext);
        }
    }

    private void proximityCheckThenCall(final IntConsumer intConsumer, boolean z, final int i) {
        Boolean isProximityCurrentlyFar = this.mDozeSensors.isProximityCurrentlyFar();
        if (z) {
            intConsumer.accept(3);
        } else if (isProximityCurrentlyFar != null) {
            intConsumer.accept(isProximityCurrentlyFar.booleanValue() ? 2 : 1);
        } else {
            final long uptimeMillis = SystemClock.uptimeMillis();
            new ProximityCheck() {
                /* class com.android.systemui.doze.DozeTriggers.AnonymousClass1 */

                @Override // com.android.systemui.doze.DozeTriggers.ProximityCheck
                public void onProximityResult(int i) {
                    long uptimeMillis = SystemClock.uptimeMillis();
                    Context context = DozeTriggers.this.mContext;
                    boolean z = true;
                    if (i != 1) {
                        z = false;
                    }
                    DozeLog.traceProximityResult(context, z, uptimeMillis - uptimeMillis, i);
                    intConsumer.accept(i);
                }
            }.check();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void onSensor(int i, boolean z, float f, float f2, float[] fArr) {
        DockManager dockManager;
        boolean z2 = true;
        boolean z3 = i == 4;
        boolean z4 = i == 9;
        boolean z5 = i == 3;
        boolean z6 = i == 5;
        boolean z7 = i == 7;
        boolean z8 = i == 8;
        boolean z9 = (fArr == null || fArr.length <= 0 || fArr[0] == 0.0f) ? false : true;
        if (z7) {
            onWakeScreen(z9, this.mMachine.isExecutingTransition() ? null : this.mMachine.getState());
        } else if (z6 || z5) {
            requestPulse(i, z);
        } else if (!z8) {
            proximityCheckThenCall(new IntConsumer(z3, z4, f, f2, i, z5) {
                /* class com.android.systemui.doze.$$Lambda$DozeTriggers$KXJDb4lGP0PpY23yKRXX1q0y7kA */
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ boolean f$2;
                private final /* synthetic */ float f$3;
                private final /* synthetic */ float f$4;
                private final /* synthetic */ int f$5;
                private final /* synthetic */ boolean f$6;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                    this.f$6 = r7;
                }

                public final void accept(int i) {
                    DozeTriggers.this.lambda$onSensor$0$DozeTriggers(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, i);
                }
            }, z || ((dockManager = this.mDockManager) != null && dockManager.isDocked()), i);
        } else if (z9) {
            requestPulse(i, z);
        }
        if (z5) {
            if (SystemClock.elapsedRealtime() - this.mNotificationPulseTime >= ((long) this.mDozeParameters.getPickupVibrationThreshold())) {
                z2 = false;
            }
            DozeLog.tracePickupWakeUp(this.mContext, z2);
        }
    }

    public /* synthetic */ void lambda$onSensor$0$DozeTriggers(boolean z, boolean z2, float f, float f2, int i, boolean z3, int i2) {
        if (i2 != 1) {
            if (z || z2) {
                if (!(f == -1.0f || f2 == -1.0f)) {
                    this.mDozeHost.onSlpiTap(f, f2);
                }
                gentleWakeUp(i);
            } else if (z3) {
                gentleWakeUp(i);
            } else {
                this.mDozeHost.extendPulse(i);
            }
        }
    }

    private void gentleWakeUp(int i) {
        this.mMetricsLogger.write(new LogMaker(223).setType(6).setSubtype(i));
        if (this.mDozeParameters.getDisplayNeedsBlanking()) {
            this.mDozeHost.setAodDimmingScrim(255.0f);
        }
        this.mMachine.wakeUp();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onProximityFar(boolean z) {
        if (this.mMachine.isExecutingTransition()) {
            Log.w("DozeTriggers", "onProximityFar called during transition. Ignoring sensor response.");
            return;
        }
        boolean z2 = !z;
        DozeMachine.State state = this.mMachine.getState();
        boolean z3 = true;
        boolean z4 = state == DozeMachine.State.DOZE_AOD_PAUSED;
        boolean z5 = state == DozeMachine.State.DOZE_AOD_PAUSING;
        if (state != DozeMachine.State.DOZE_AOD) {
            z3 = false;
        }
        boolean smartOn = this.mDozeParameters.getSmartOn();
        if (state == DozeMachine.State.DOZE_PULSING || state == DozeMachine.State.DOZE_PULSING_BRIGHT) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox changed, ignore touch = " + z2);
            }
            this.mDozeHost.onIgnoreTouchWhilePulsing(z2);
        }
        if (z && (z4 || z5)) {
            showAmbientDisplay(smartOn);
        } else if (z2 && z3 && !this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed()) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox NEAR, pausing AOD");
            }
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD_PAUSING);
        } else if (z2 && smartOn && !this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed()) {
            if (DEBUG) {
                Log.i("DozeTriggers", "Prox NEAR, paused AOD");
            }
            SystemProperties.set("sys.enable.smart_on_doze", "2");
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD_PAUSED);
            this.mDozeSensors.setListening(false);
        }
    }

    private void onWakeScreen(boolean z, DozeMachine.State state) {
        DozeLog.traceWakeDisplay(z);
        sWakeDisplaySensorState = z;
        boolean z2 = false;
        if (z) {
            proximityCheckThenCall(new IntConsumer(state) {
                /* class com.android.systemui.doze.$$Lambda$DozeTriggers$vUNGpAqR9niD5s7OS6n7KlXtw9c */
                private final /* synthetic */ DozeMachine.State f$1;

                {
                    this.f$1 = r2;
                }

                public final void accept(int i) {
                    DozeTriggers.this.lambda$onWakeScreen$1$DozeTriggers(this.f$1, i);
                }
            }, false, 7);
            return;
        }
        boolean z3 = state == DozeMachine.State.DOZE_AOD_PAUSED;
        if (state == DozeMachine.State.DOZE_AOD_PAUSING) {
            z2 = true;
        }
        if (!z2 && !z3) {
            this.mMachine.requestState(DozeMachine.State.DOZE);
            this.mMetricsLogger.write(new LogMaker(223).setType(2).setSubtype(7));
        }
    }

    public /* synthetic */ void lambda$onWakeScreen$1$DozeTriggers(DozeMachine.State state, int i) {
        if (i != 1 && state == DozeMachine.State.DOZE) {
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
            this.mMetricsLogger.write(new LogMaker(223).setType(1).setSubtype(7));
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showAmbientDisplay(boolean z) {
        if (DEBUG) {
            Log.i("DozeTriggers", "Prox FAR, unpausing AOD");
        }
        if (!z) {
            this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
            return;
        }
        if (DEBUG) {
            Log.i("DozeTriggers", "Prox FAR");
        }
        SystemProperties.set("sys.enable.smart_on_doze", "0");
        requestPulse(21, true);
    }

    /* renamed from: com.android.systemui.doze.DozeTriggers$4  reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$doze$DozeMachine$State = new int[DozeMachine.State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(20:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|20) */
        /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
            return;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x004b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:15:0x0056 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:17:0x0062 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
            // Method dump skipped, instructions count: 111
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeTriggers.AnonymousClass4.<clinit>():void");
        }
    }

    @Override // com.android.systemui.doze.DozeMachine.Part
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        switch (AnonymousClass4.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state2.ordinal()]) {
            case 1:
                this.mBroadcastReceiver.register(this.mContext);
                this.mDozeHost.addCallback(this.mHostCallback);
                DockManager dockManager = this.mDockManager;
                if (dockManager != null) {
                    dockManager.addListener(this.mDockEventListener);
                }
                this.mDozeSensors.requestTemporaryDisable();
                if (this.mDozeParameters.getAlwaysOn()) {
                    this.mLockscreenStyleCoverController.registerCallback(this.mStyleCoverCallback);
                }
                checkTriggersAtInit();
                if (this.mDozeParameters.getSmartOn() || this.mDozeParameters.getAlwaysOn()) {
                    this.mCalibProxTimer.schedule(700, 2);
                    break;
                }
            case 2:
            case 3:
                this.mDozeSensors.setProxListening(state2 != DozeMachine.State.DOZE || this.mDozeParameters.getSmartOn());
                DozeMachine.State state3 = DozeMachine.State.INITIALIZED;
                this.mDozeSensors.setListening(true);
                if (state2 == DozeMachine.State.DOZE_AOD && !sWakeDisplaySensorState) {
                    onWakeScreen(false, state2);
                    break;
                }
                break;
            case 4:
            case 5:
                this.mDozeSensors.setProxListening(true);
                this.mDozeSensors.setListening(false);
                break;
            case 6:
            case 7:
                this.mDozeSensors.setTouchscreenSensorsListening(false);
                this.mDozeSensors.setProxListening(true);
                break;
            case 8:
                this.mDozeSensors.requestTemporaryDisable();
                break;
            case 9:
                if (this.mSodTimer.isScheduled()) {
                    this.mSodTimer.cancel();
                }
                if (this.mSodExtTimer.isScheduled()) {
                    this.mSodExtTimer.cancel();
                }
                this.mSodExtTimeouted = false;
                if (this.mCalibProxTimer.isScheduled()) {
                    this.mCalibProxTimer.cancel();
                }
                if (DEBUG) {
                    Log.i("DozeTriggers", "Finish");
                }
                SystemProperties.set("sys.enable.smart_on_doze", "0");
                this.mBroadcastReceiver.unregister(this.mContext);
                this.mDozeHost.removeCallback(this.mHostCallback);
                DockManager dockManager2 = this.mDockManager;
                if (dockManager2 != null) {
                    dockManager2.removeListener(this.mDockEventListener);
                }
                this.mLockscreenStyleCoverController.removeCallback(this.mStyleCoverCallback);
                this.mDozeSensors.setListening(false);
                this.mDozeSensors.setProxListening(false);
                break;
        }
        AodStateReportHelper.reportState(this.mContext, this.mDozeParameters, state2, this.mReason);
    }

    private void checkTriggersAtInit() {
        if (this.mUiModeManager.getCurrentModeType() == 3 || this.mDozeHost.isPowerSaveActive() || this.mDozeHost.isBlockingDoze() || !this.mDozeHost.isProvisioned()) {
            this.mMachine.requestState(DozeMachine.State.FINISH);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void requestPulse(int i, boolean z) {
        int i2;
        Assert.isMainThread();
        this.mDozeHost.extendPulse(i);
        this.mReason = i;
        if (this.mMachine.getState() == DozeMachine.State.DOZE_PULSING && i == 8) {
            this.mMachine.requestState(DozeMachine.State.DOZE_PULSING_BRIGHT);
        } else if (this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed() && ((i2 = this.mReason) == 3 || i2 == 20)) {
        } else {
            if (this.mDozeParameters.getSmartOn() && this.mMachine.getState() != DozeMachine.State.UNINITIALIZED) {
                if (this.mSodExtTimeouted) {
                    if (this.mReason != 20) {
                        this.mSodExtTimeouted = false;
                    } else {
                        return;
                    }
                }
                if (!this.mSodTimer.isScheduled() && !this.mSodExtTimer.isScheduled()) {
                    this.mSodExtTimer.schedule(600000, 2);
                }
                if (DEBUG) {
                    Log.i("DozeTriggers", "requestPulse DOZE_AOD");
                }
                this.mSodTimer.schedule(15000, 2);
                this.mMachine.requestState(DozeMachine.State.DOZE_AOD);
            } else if (!this.mPulsePending && this.mAllowPulseTriggers && canPulse()) {
                boolean z2 = true;
                this.mPulsePending = true;
                $$Lambda$DozeTriggers$EDCYzTgUQ8bpFfKolETll4jmVsA r1 = new IntConsumer(i) {
                    /* class com.android.systemui.doze.$$Lambda$DozeTriggers$EDCYzTgUQ8bpFfKolETll4jmVsA */
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void accept(int i) {
                        DozeTriggers.this.lambda$requestPulse$2$DozeTriggers(this.f$1, i);
                    }
                };
                if (this.mDozeParameters.getProxCheckBeforePulse() && !z) {
                    z2 = false;
                }
                proximityCheckThenCall(r1, z2, i);
                this.mMetricsLogger.write(new LogMaker(223).setType(6).setSubtype(i));
            } else if (this.mAllowPulseTriggers) {
                DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
            }
        }
    }

    public /* synthetic */ void lambda$requestPulse$2$DozeTriggers(int i, int i2) {
        if (i2 != 1 || this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed()) {
            continuePulseRequest(i);
        } else {
            this.mPulsePending = false;
        }
    }

    private boolean canPulse() {
        return this.mMachine.getState() == DozeMachine.State.DOZE || this.mMachine.getState() == DozeMachine.State.DOZE_AOD;
    }

    private void continuePulseRequest(int i) {
        this.mPulsePending = false;
        if (this.mDozeHost.isPulsingBlocked() || !canPulse()) {
            DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
        } else {
            this.mMachine.requestPulse(i);
        }
    }

    @Override // com.android.systemui.doze.DozeMachine.Part
    public void dump(PrintWriter printWriter) {
        printWriter.print(" notificationPulseTime=");
        printWriter.println(Formatter.formatShortElapsedTime(this.mContext, this.mNotificationPulseTime));
        printWriter.print(" pulsePending=");
        printWriter.println(this.mPulsePending);
        printWriter.println("DozeSensors:");
        this.mDozeSensors.dump(printWriter);
    }

    /* access modifiers changed from: private */
    public abstract class ProximityCheck implements SensorEventListener, Runnable {
        private boolean mFinished;
        private float mMaxRange;
        private boolean mRegistered;

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        /* access modifiers changed from: protected */
        public abstract void onProximityResult(int i);

        private ProximityCheck() {
        }

        public void check() {
            Preconditions.checkState(!this.mFinished && !this.mRegistered);
            Sensor defaultSensor = DozeTriggers.this.mSensorManager.getDefaultSensor(8);
            if (defaultSensor == null) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "ProxCheck: No sensor found");
                }
                finishWithResult(0);
                return;
            }
            DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(true);
            this.mMaxRange = defaultSensor.getMaximumRange();
            DozeTriggers.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeTriggers.this.mHandler);
            DozeTriggers.this.mHandler.postDelayed(this, 500);
            DozeTriggers.this.mWakeLock.acquire("DozeTriggers");
            this.mRegistered = true;
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            boolean z = false;
            if (sensorEvent.values.length == 0) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "ProxCheck: Event has no values!");
                }
                finishWithResult(0);
                return;
            }
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: Event: value=" + sensorEvent.values[0] + " max=" + this.mMaxRange);
            }
            int i = 1;
            if (sensorEvent.values[0] < this.mMaxRange) {
                z = true;
            }
            if (!z) {
                i = 2;
            }
            finishWithResult(i);
        }

        public void run() {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: No event received before timeout");
            }
            finishWithResult(0);
        }

        private void finishWithResult(int i) {
            if (!this.mFinished) {
                boolean z = this.mRegistered;
                if (z) {
                    DozeTriggers.this.mHandler.removeCallbacks(this);
                    DozeTriggers.this.mSensorManager.unregisterListener(this);
                    DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(false);
                    this.mRegistered = false;
                }
                onProximityResult(i);
                if (z) {
                    DozeTriggers.this.mWakeLock.release("DozeTriggers");
                }
                this.mFinished = true;
            }
        }
    }

    private class TriggerReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private TriggerReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.doze.pulse".equals(intent.getAction())) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "Received pulse intent");
                }
                DozeTriggers.this.requestPulse(0, false);
            }
            if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intent.getAction())) {
                DozeTriggers.this.mMachine.requestState(DozeMachine.State.FINISH);
            }
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                DozeTriggers.this.mDozeSensors.onUserSwitched();
            }
        }

        public void register(Context context) {
            if (!this.mRegistered) {
                IntentFilter intentFilter = new IntentFilter("com.android.systemui.doze.pulse");
                intentFilter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
                intentFilter.addAction("android.intent.action.USER_SWITCHED");
                context.registerReceiver(this, intentFilter);
                this.mRegistered = true;
            }
        }

        public void unregister(Context context) {
            if (this.mRegistered) {
                context.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }

    private class DockEventListener implements DockManager.DockEventListener {
        private DockEventListener() {
        }
    }
}
