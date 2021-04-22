package com.android.systemui.classifier.brightline;

import android.provider.DeviceConfig;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import java.util.List;

/* access modifiers changed from: package-private */
public class DistanceClassifier extends FalsingClassifier {
    private DistanceVectors mCachedDistance;
    private boolean mDistanceDirty;
    private final float mHorizontalFlingThresholdPx;
    private final float mHorizontalSwipeThresholdPx;
    private final float mVelocityToDistanceMultiplier = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_velcoity_to_distance", 80.0f);
    private final float mVerticalFlingThresholdPx;
    private final float mVerticalSwipeThresholdPx;

    DistanceClassifier(FalsingDataProvider falsingDataProvider) {
        super(falsingDataProvider);
        float f = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_horizontal_fling_threshold_in", 1.0f);
        float f2 = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_vertical_fling_threshold_in", 1.0f);
        float f3 = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_horizontal_swipe_threshold_in", 3.0f);
        float f4 = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_horizontal_swipe_threshold_in", 3.0f);
        float f5 = DeviceConfig.getFloat("systemui", "brightline_falsing_distance_screen_fraction_max_distance", 0.8f);
        this.mHorizontalFlingThresholdPx = Math.min(((float) getWidthPixels()) * f5, f * getXdpi());
        this.mVerticalFlingThresholdPx = Math.min(((float) getHeightPixels()) * f5, f2 * getYdpi());
        this.mHorizontalSwipeThresholdPx = Math.min(((float) getWidthPixels()) * f5, f3 * getXdpi());
        this.mVerticalSwipeThresholdPx = Math.min(((float) getHeightPixels()) * f5, f4 * getYdpi());
        this.mDistanceDirty = true;
    }

    private DistanceVectors getDistances() {
        if (this.mDistanceDirty) {
            this.mCachedDistance = calculateDistances();
            this.mDistanceDirty = false;
        }
        return this.mCachedDistance;
    }

    private DistanceVectors calculateDistances() {
        VelocityTracker obtain = VelocityTracker.obtain();
        List<MotionEvent> recentMotionEvents = getRecentMotionEvents();
        if (recentMotionEvents.size() < 3) {
            String str = "Only " + recentMotionEvents.size() + " motion events recorded.";
            return new DistanceVectors(0.0f, 0.0f, 0.0f, 0.0f);
        }
        for (MotionEvent motionEvent : recentMotionEvents) {
            obtain.addMovement(motionEvent);
        }
        obtain.computeCurrentVelocity(1);
        float xVelocity = obtain.getXVelocity();
        float yVelocity = obtain.getYVelocity();
        obtain.recycle();
        float x = getLastMotionEvent().getX() - getFirstMotionEvent().getX();
        float y = getLastMotionEvent().getY() - getFirstMotionEvent().getY();
        FalsingClassifier.logInfo("dX: " + x + " dY: " + y + " xV: " + xVelocity + " yV: " + yVelocity);
        return new DistanceVectors(x, y, xVelocity, yVelocity);
    }

    @Override // com.android.systemui.classifier.brightline.FalsingClassifier
    public void onTouchEvent(MotionEvent motionEvent) {
        this.mDistanceDirty = true;
    }

    @Override // com.android.systemui.classifier.brightline.FalsingClassifier
    public boolean isFalseTouch() {
        return !getDistances().getPassedFlingThreshold();
    }

    /* access modifiers changed from: package-private */
    public boolean isLongSwipe() {
        boolean passedDistanceThreshold = getDistances().getPassedDistanceThreshold();
        String str = "Is longSwipe? " + passedDistanceThreshold;
        return passedDistanceThreshold;
    }

    /* access modifiers changed from: private */
    public class DistanceVectors {
        final float mDx;
        final float mDy;
        private final float mVx;
        private final float mVy;

        DistanceVectors(float f, float f2, float f3, float f4) {
            this.mDx = f;
            this.mDy = f2;
            this.mVx = f3;
            this.mVy = f4;
        }

        /* access modifiers changed from: package-private */
        public boolean getPassedDistanceThreshold() {
            if (DistanceClassifier.this.isHorizontal()) {
                String str = "Horizontal swipe distance: " + Math.abs(this.mDx);
                String str2 = "Threshold: " + DistanceClassifier.this.mHorizontalSwipeThresholdPx;
                return Math.abs(this.mDx) >= DistanceClassifier.this.mHorizontalSwipeThresholdPx;
            }
            String str3 = "Vertical swipe distance: " + Math.abs(this.mDy);
            String str4 = "Threshold: " + DistanceClassifier.this.mVerticalSwipeThresholdPx;
            return Math.abs(this.mDy) >= DistanceClassifier.this.mVerticalSwipeThresholdPx;
        }

        /* access modifiers changed from: package-private */
        public boolean getPassedFlingThreshold() {
            float f = this.mDx + (this.mVx * DistanceClassifier.this.mVelocityToDistanceMultiplier);
            float f2 = this.mDy + (this.mVy * DistanceClassifier.this.mVelocityToDistanceMultiplier);
            if (DistanceClassifier.this.isHorizontal()) {
                String str = "Horizontal swipe and fling distance: " + this.mDx + ", " + (this.mVx * DistanceClassifier.this.mVelocityToDistanceMultiplier);
                String str2 = "Threshold: " + DistanceClassifier.this.mHorizontalFlingThresholdPx;
                return Math.abs(f) >= DistanceClassifier.this.mHorizontalFlingThresholdPx;
            }
            String str3 = "Vertical swipe and fling distance: " + this.mDy + ", " + (this.mVy * DistanceClassifier.this.mVelocityToDistanceMultiplier);
            String str4 = "Threshold: " + DistanceClassifier.this.mVerticalFlingThresholdPx;
            return Math.abs(f2) >= DistanceClassifier.this.mVerticalFlingThresholdPx;
        }
    }
}
