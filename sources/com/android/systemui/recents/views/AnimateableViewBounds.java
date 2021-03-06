package com.android.systemui.recents.views;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.android.systemui.recents.utilities.Utilities;

public class AnimateableViewBounds extends ViewOutlineProvider {
    protected float mAlpha = 1.0f;
    protected Rect mClipBounds = new Rect();
    protected Rect mClipRect = new Rect();
    protected int mCornerRadius;
    protected Rect mLastClipBounds = new Rect();
    protected View mSourceView;

    public AnimateableViewBounds(View view, int i) {
        this.mSourceView = view;
        this.mCornerRadius = i;
    }

    public void reset() {
        this.mClipRect.set(0, 0, 0, 0);
        updateClipBounds();
    }

    public void getOutline(View view, Outline outline) {
        outline.setAlpha(Utilities.mapRange(this.mAlpha, 0.1f, 0.8f));
        if (this.mCornerRadius > 0) {
            Rect rect = this.mClipRect;
            outline.setRoundRect(rect.left, rect.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom, (float) this.mCornerRadius);
            return;
        }
        Rect rect2 = this.mClipRect;
        outline.setRect(rect2.left, rect2.top, this.mSourceView.getWidth() - this.mClipRect.right, this.mSourceView.getHeight() - this.mClipRect.bottom);
    }

    public void setAlpha(float f) {
        if (Float.compare(f, this.mAlpha) != 0) {
            this.mAlpha = f;
            this.mSourceView.invalidateOutline();
        }
    }

    public float getAlpha() {
        return this.mAlpha;
    }

    public void setClipBottom(int i) {
        this.mClipRect.bottom = i;
        updateClipBounds();
    }

    public Rect getClipBounds() {
        return this.mClipBounds;
    }

    /* access modifiers changed from: protected */
    public void updateClipBounds() {
        this.mClipBounds.set(Math.max(0, this.mClipRect.left), Math.max(0, this.mClipRect.top), this.mSourceView.getWidth() - Math.max(0, this.mClipRect.right), this.mSourceView.getHeight() - Math.max(0, this.mClipRect.bottom));
        if (!this.mLastClipBounds.equals(this.mClipBounds)) {
            this.mSourceView.setClipBounds(this.mClipBounds);
            this.mSourceView.invalidateOutline();
            this.mLastClipBounds.set(this.mClipBounds);
        }
    }
}
