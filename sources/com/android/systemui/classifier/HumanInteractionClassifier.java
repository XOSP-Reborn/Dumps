package com.android.systemui.classifier;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.SensorEvent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import com.android.systemui.C0003R$bool;
import java.util.ArrayDeque;

public class HumanInteractionClassifier extends Classifier {
    private static HumanInteractionClassifier sInstance;
    private final ArrayDeque<MotionEvent> mBufferedEvents = new ArrayDeque<>();
    private final Context mContext;
    private int mCurrentType = 7;
    private final float mDpi;
    private boolean mEnableClassifier = false;
    private final GestureClassifier[] mGestureClassifiers;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final HistoryEvaluator mHistoryEvaluator;
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        /* class com.android.systemui.classifier.HumanInteractionClassifier.AnonymousClass1 */

        public void onChange(boolean z) {
            HumanInteractionClassifier.this.updateConfiguration();
        }
    };
    private final StrokeClassifier[] mStrokeClassifiers;

    private HumanInteractionClassifier(Context context) {
        this.mContext = context;
        DisplayMetrics displayMetrics = this.mContext.getResources().getDisplayMetrics();
        this.mDpi = (displayMetrics.xdpi + displayMetrics.ydpi) / 2.0f;
        this.mClassifierData = new ClassifierData(this.mDpi);
        this.mHistoryEvaluator = new HistoryEvaluator();
        this.mStrokeClassifiers = new StrokeClassifier[]{new AnglesClassifier(this.mClassifierData), new SpeedClassifier(this.mClassifierData), new DurationCountClassifier(this.mClassifierData), new EndPointRatioClassifier(this.mClassifierData), new EndPointLengthClassifier(this.mClassifierData), new AccelerationClassifier(this.mClassifierData), new SpeedAnglesClassifier(this.mClassifierData), new LengthCountClassifier(this.mClassifierData), new DirectionClassifier(this.mClassifierData)};
        this.mGestureClassifiers = new GestureClassifier[]{new PointerCountClassifier(this.mClassifierData), new ProximityClassifier(this.mClassifierData)};
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("HIC_enable"), false, this.mSettingsObserver, -1);
        updateConfiguration();
    }

    public static HumanInteractionClassifier getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HumanInteractionClassifier(context);
        }
        return sInstance;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateConfiguration() {
        this.mEnableClassifier = Settings.Global.getInt(this.mContext.getContentResolver(), "HIC_enable", this.mContext.getResources().getBoolean(C0003R$bool.config_lockscreenAntiFalsingClassifierEnabled) ? 1 : 0) != 0;
    }

    public void setType(int i) {
        this.mCurrentType = i;
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onTouchEvent(MotionEvent motionEvent) {
        if (this.mEnableClassifier) {
            int i = this.mCurrentType;
            if (i == 2 || i == 9) {
                this.mBufferedEvents.add(MotionEvent.obtain(motionEvent));
                Point point = new Point(motionEvent.getX() / this.mDpi, motionEvent.getY() / this.mDpi);
                while (point.dist(new Point(this.mBufferedEvents.getFirst().getX() / this.mDpi, this.mBufferedEvents.getFirst().getY() / this.mDpi)) > 0.1f) {
                    addTouchEvent(this.mBufferedEvents.getFirst());
                    this.mBufferedEvents.remove();
                }
                if (motionEvent.getActionMasked() == 1) {
                    this.mBufferedEvents.getFirst().setAction(1);
                    addTouchEvent(this.mBufferedEvents.getFirst());
                    this.mBufferedEvents.clear();
                    return;
                }
                return;
            }
            addTouchEvent(motionEvent);
        }
    }

    private void addTouchEvent(MotionEvent motionEvent) {
        StringBuilder sb;
        float f;
        if (this.mClassifierData.update(motionEvent)) {
            for (StrokeClassifier strokeClassifier : this.mStrokeClassifiers) {
                strokeClassifier.onTouchEvent(motionEvent);
            }
            for (GestureClassifier gestureClassifier : this.mGestureClassifiers) {
                gestureClassifier.onTouchEvent(motionEvent);
            }
            int size = this.mClassifierData.getEndingStrokes().size();
            int i = 0;
            while (true) {
                sb = null;
                f = 0.0f;
                if (i >= size) {
                    break;
                }
                Stroke stroke = this.mClassifierData.getEndingStrokes().get(i);
                if (FalsingLog.ENABLED) {
                    sb = new StringBuilder("stroke");
                }
                StrokeClassifier[] strokeClassifierArr = this.mStrokeClassifiers;
                float f2 = 0.0f;
                for (StrokeClassifier strokeClassifier2 : strokeClassifierArr) {
                    float falseTouchEvaluation = strokeClassifier2.getFalseTouchEvaluation(this.mCurrentType, stroke);
                    if (FalsingLog.ENABLED) {
                        String tag = strokeClassifier2.getTag();
                        sb.append(" ");
                        if (falseTouchEvaluation < 1.0f) {
                            tag = tag.toLowerCase();
                        }
                        sb.append(tag);
                        sb.append("=");
                        sb.append(falseTouchEvaluation);
                    }
                    f2 += falseTouchEvaluation;
                }
                if (FalsingLog.ENABLED) {
                    FalsingLog.i(" addTouchEvent", sb.toString());
                }
                this.mHistoryEvaluator.addStroke(f2);
                i++;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 1 || actionMasked == 3) {
                if (FalsingLog.ENABLED) {
                    sb = new StringBuilder("gesture");
                }
                GestureClassifier[] gestureClassifierArr = this.mGestureClassifiers;
                for (GestureClassifier gestureClassifier2 : gestureClassifierArr) {
                    float falseTouchEvaluation2 = gestureClassifier2.getFalseTouchEvaluation(this.mCurrentType);
                    if (FalsingLog.ENABLED) {
                        String tag2 = gestureClassifier2.getTag();
                        sb.append(" ");
                        if (falseTouchEvaluation2 < 1.0f) {
                            tag2 = tag2.toLowerCase();
                        }
                        sb.append(tag2);
                        sb.append("=");
                        sb.append(falseTouchEvaluation2);
                    }
                    f += falseTouchEvaluation2;
                }
                if (FalsingLog.ENABLED) {
                    FalsingLog.i(" addTouchEvent", sb.toString());
                }
                this.mHistoryEvaluator.addGesture(f);
                setType(7);
            }
            this.mClassifierData.cleanUp(motionEvent);
        }
    }

    @Override // com.android.systemui.classifier.Classifier
    public void onSensorChanged(SensorEvent sensorEvent) {
        for (StrokeClassifier strokeClassifier : this.mStrokeClassifiers) {
            strokeClassifier.onSensorChanged(sensorEvent);
        }
        for (GestureClassifier gestureClassifier : this.mGestureClassifiers) {
            gestureClassifier.onSensorChanged(sensorEvent);
        }
    }

    public boolean isFalseTouch() {
        boolean z = false;
        z = false;
        if (this.mEnableClassifier) {
            float evaluation = this.mHistoryEvaluator.getEvaluation();
            if (evaluation >= 5.0f) {
                z = true;
            }
            if (FalsingLog.ENABLED) {
                StringBuilder sb = new StringBuilder();
                sb.append("eval=");
                sb.append(evaluation);
                sb.append(" result=");
                int i = z ? 1 : 0;
                int i2 = z ? 1 : 0;
                int i3 = z ? 1 : 0;
                int i4 = z ? 1 : 0;
                sb.append(i);
                FalsingLog.i("isFalseTouch", sb.toString());
            }
        }
        return z;
    }

    public boolean isEnabled() {
        return this.mEnableClassifier;
    }
}
