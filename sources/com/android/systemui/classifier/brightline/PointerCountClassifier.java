package com.android.systemui.classifier.brightline;

import android.view.MotionEvent;

/* access modifiers changed from: package-private */
public class PointerCountClassifier extends FalsingClassifier {
    private int mMaxPointerCount;

    PointerCountClassifier(FalsingDataProvider falsingDataProvider) {
        super(falsingDataProvider);
    }

    @Override // com.android.systemui.classifier.brightline.FalsingClassifier
    public void onTouchEvent(MotionEvent motionEvent) {
        int i = this.mMaxPointerCount;
        if (motionEvent.getActionMasked() == 0) {
            this.mMaxPointerCount = motionEvent.getPointerCount();
        } else {
            this.mMaxPointerCount = Math.max(this.mMaxPointerCount, motionEvent.getPointerCount());
        }
        if (i != this.mMaxPointerCount) {
            String str = "Pointers observed:" + this.mMaxPointerCount;
        }
    }

    @Override // com.android.systemui.classifier.brightline.FalsingClassifier
    public boolean isFalseTouch() {
        return this.mMaxPointerCount > 1;
    }
}
