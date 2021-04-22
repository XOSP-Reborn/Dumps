package com.android.systemui.classifier.brightline;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import com.android.systemui.plugins.FalsingManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class BrightLineFalsingManager implements FalsingManager {
    private final ExecutorService mBackgroundExecutor = Executors.newSingleThreadExecutor();
    private final List<FalsingClassifier> mClassifiers;
    private final FalsingDataProvider mDataProvider;
    private boolean mScreenOn;
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        /* class com.android.systemui.classifier.brightline.BrightLineFalsingManager.AnonymousClass1 */

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public synchronized void onSensorChanged(SensorEvent sensorEvent) {
            BrightLineFalsingManager.this.onSensorEvent(sensorEvent);
        }
    };
    private final SensorManager mSensorManager;
    private boolean mSessionStarted;
    private boolean mShowingAod;

    @Override // com.android.systemui.plugins.FalsingManager
    public void dump(PrintWriter printWriter) {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isClassiferEnabled() {
        return true;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isReportingEnabled() {
        return false;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isUnlockingDisabled() {
        return false;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onAffordanceSwipingAborted() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onBouncerHidden() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onBouncerShown() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onCameraHintStarted() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onCameraOn() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onExpansionFromPulseStopped() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onLeftAffordanceHintStarted() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onLeftAffordanceOn() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationActive() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationDismissed() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificationDoubleTap(boolean z, float f, float f2) {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStopDismissing() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStopDraggingDown() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTrackingStopped() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onUnlockHintStarted() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public Uri reportRejectedTouch() {
        return null;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setNotificationExpanded() {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setQsExpanded(boolean z) {
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean shouldEnforceBouncer() {
        return false;
    }

    public BrightLineFalsingManager(FalsingDataProvider falsingDataProvider, SensorManager sensorManager) {
        this.mDataProvider = falsingDataProvider;
        this.mSensorManager = sensorManager;
        this.mClassifiers = new ArrayList();
        DistanceClassifier distanceClassifier = new DistanceClassifier(this.mDataProvider);
        ProximityClassifier proximityClassifier = new ProximityClassifier(distanceClassifier, this.mDataProvider);
        this.mClassifiers.add(new PointerCountClassifier(this.mDataProvider));
        this.mClassifiers.add(new TypeClassifier(this.mDataProvider));
        this.mClassifiers.add(new DiagonalClassifier(this.mDataProvider));
        this.mClassifiers.add(distanceClassifier);
        this.mClassifiers.add(proximityClassifier);
        this.mClassifiers.add(new ZigZagClassifier(this.mDataProvider));
    }

    private void registerSensors() {
        Sensor defaultSensor = this.mSensorManager.getDefaultSensor(8);
        if (defaultSensor != null) {
            this.mBackgroundExecutor.submit(new Runnable(defaultSensor) {
                /* class com.android.systemui.classifier.brightline.$$Lambda$BrightLineFalsingManager$oXy7WDc2eJ5hwJ2IlTpSo0Szms */
                private final /* synthetic */ Sensor f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    BrightLineFalsingManager.this.lambda$registerSensors$0$BrightLineFalsingManager(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$registerSensors$0$BrightLineFalsingManager(Sensor sensor) {
        this.mSensorManager.registerListener(this.mSensorEventListener, sensor, 1);
    }

    private void unregisterSensors() {
        this.mBackgroundExecutor.submit(new Runnable() {
            /* class com.android.systemui.classifier.brightline.$$Lambda$BrightLineFalsingManager$0k2Os8V4mSfPPowZ1inEJqhsAE */

            public final void run() {
                BrightLineFalsingManager.this.lambda$unregisterSensors$1$BrightLineFalsingManager();
            }
        });
    }

    public /* synthetic */ void lambda$unregisterSensors$1$BrightLineFalsingManager() {
        this.mSensorManager.unregisterListener(this.mSensorEventListener);
    }

    private void sessionStart() {
        if (!this.mSessionStarted && !this.mShowingAod && this.mScreenOn) {
            this.mSessionStarted = true;
            registerSensors();
            this.mClassifiers.forEach($$Lambda$HclOlu42IVtKALxwbwHP3Y1rdRk.INSTANCE);
        }
    }

    private void sessionEnd() {
        if (this.mSessionStarted) {
            this.mSessionStarted = false;
            unregisterSensors();
            this.mDataProvider.onSessionEnd();
            this.mClassifiers.forEach($$Lambda$47wU6WxQ76Gt_ecwypSCrFl04Q.INSTANCE);
        }
    }

    private void updateInteractionType(int i) {
        String str = "InteractionType: " + i;
        this.mClassifiers.forEach(new Consumer(i) {
            /* class com.android.systemui.classifier.brightline.$$Lambda$BrightLineFalsingManager$whJKACq72TioWHJA2gFUw_2uKEw */
            private final /* synthetic */ int f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((FalsingClassifier) obj).setInteractionType(this.f$0);
            }
        });
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public boolean isFalseTouch() {
        boolean anyMatch = this.mClassifiers.stream().anyMatch($$Lambda$BrightLineFalsingManager$AiaSOz8R7zJnTp0oazL7DWdY4Kg.INSTANCE);
        String str = "Is false touch? " + anyMatch;
        return anyMatch;
    }

    static /* synthetic */ boolean lambda$isFalseTouch$3(FalsingClassifier falsingClassifier) {
        boolean isFalseTouch = falsingClassifier.isFalseTouch();
        if (isFalseTouch) {
            logInfo(falsingClassifier.getClass().getName() + ": true");
        } else {
            String str = falsingClassifier.getClass().getName() + ": false";
        }
        return isFalseTouch;
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTouchEvent(MotionEvent motionEvent, int i, int i2) {
        this.mDataProvider.onMotionEvent(motionEvent);
        this.mClassifiers.forEach(new Consumer(motionEvent) {
            /* class com.android.systemui.classifier.brightline.$$Lambda$BrightLineFalsingManager$y2EhyJ78U2M2gK2mt8maWHpnYsU */
            private final /* synthetic */ MotionEvent f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((FalsingClassifier) obj).onTouchEvent(this.f$0);
            }
        });
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onSensorEvent(SensorEvent sensorEvent) {
        this.mClassifiers.forEach(new Consumer(sensorEvent) {
            /* class com.android.systemui.classifier.brightline.$$Lambda$BrightLineFalsingManager$AsH_lc0LzYUN0FND3sobD9zcVwo */
            private final /* synthetic */ SensorEvent f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((FalsingClassifier) obj).onSensorEvent(this.f$0);
            }
        });
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onSucccessfulUnlock() {
        sessionEnd();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void setShowingAod(boolean z) {
        this.mShowingAod = z;
        if (z) {
            sessionEnd();
        } else {
            sessionStart();
        }
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStartDraggingDown() {
        updateInteractionType(2);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onQsDown() {
        updateInteractionType(0);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onTrackingStarted(boolean z) {
        updateInteractionType(z ? 8 : 4);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onAffordanceSwipingStarted(boolean z) {
        updateInteractionType(z ? 6 : 5);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onStartExpandingFromPulse() {
        updateInteractionType(9);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenOnFromTouch() {
        onScreenTurningOn();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenTurningOn() {
        this.mScreenOn = true;
        sessionStart();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onScreenOff() {
        this.mScreenOn = false;
        sessionEnd();
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void onNotificatonStartDismissing() {
        updateInteractionType(1);
    }

    @Override // com.android.systemui.plugins.FalsingManager
    public void cleanup() {
        unregisterSensors();
    }

    static void logInfo(String str) {
        Log.i("FalsingManagerPlugin", str);
    }
}
