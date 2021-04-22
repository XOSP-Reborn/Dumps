package com.android.systemui.recents.views.grid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import com.android.systemui.recents.views.TaskViewThumbnail;

public class GridTaskViewThumbnail extends TaskViewThumbnail {
    private final Path mRestBackgroundOutline;
    private final Path mThumbnailOutline;
    private boolean mUpdateThumbnailOutline;

    public GridTaskViewThumbnail(Context context) {
        this(context, null);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public GridTaskViewThumbnail(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mThumbnailOutline = new Path();
        this.mRestBackgroundOutline = new Path();
        this.mUpdateThumbnailOutline = true;
        this.mCornerRadius = getResources().getDimensionPixelSize(2131166225);
    }

    @Override // com.android.systemui.recents.views.TaskViewThumbnail
    public void onTaskViewSizeChanged(int i, int i2) {
        this.mUpdateThumbnailOutline = true;
        super.onTaskViewSizeChanged(i, i2);
    }

    @Override // com.android.systemui.recents.views.TaskViewThumbnail
    public void updateThumbnailMatrix() {
        this.mUpdateThumbnailOutline = true;
        super.updateThumbnailMatrix();
    }

    private void updateThumbnailOutline() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(2131166224);
        int width = this.mTaskViewRect.width();
        int height = this.mTaskViewRect.height() - dimensionPixelSize;
        int min = Math.min(width, (int) (((float) this.mThumbnailRect.width()) * this.mThumbnailScale));
        int min2 = Math.min(height, (int) (((float) this.mThumbnailRect.height()) * this.mThumbnailScale));
        if (this.mBitmapShader == null || min <= 0 || min2 <= 0) {
            createThumbnailPath(0, 0, width, height, this.mThumbnailOutline);
            return;
        }
        int i = min + 0;
        int i2 = min2 + 0;
        createThumbnailPath(0, 0, i, i2, this.mThumbnailOutline);
        if (min < width) {
            int max = Math.max(0, i - this.mCornerRadius);
            this.mRestBackgroundOutline.reset();
            float f = (float) max;
            this.mRestBackgroundOutline.moveTo(f, 0.0f);
            float f2 = (float) i;
            this.mRestBackgroundOutline.lineTo(f2, 0.0f);
            this.mRestBackgroundOutline.lineTo(f2, (float) (i2 - this.mCornerRadius));
            Path path = this.mRestBackgroundOutline;
            int i3 = this.mCornerRadius;
            float f3 = (float) (i2 - (i3 * 2));
            float f4 = (float) i2;
            path.arcTo((float) (i - (i3 * 2)), f3, f2, f4, 0.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo(f, f4);
            this.mRestBackgroundOutline.lineTo(f, 0.0f);
            this.mRestBackgroundOutline.close();
        }
        if (min2 < height) {
            int max2 = Math.max(0, min2 - this.mCornerRadius);
            this.mRestBackgroundOutline.reset();
            float f5 = (float) max2;
            this.mRestBackgroundOutline.moveTo(0.0f, f5);
            float f6 = (float) i;
            this.mRestBackgroundOutline.lineTo(f6, f5);
            this.mRestBackgroundOutline.lineTo(f6, (float) (i2 - this.mCornerRadius));
            Path path2 = this.mRestBackgroundOutline;
            int i4 = this.mCornerRadius;
            float f7 = (float) (i2 - (i4 * 2));
            float f8 = (float) i2;
            path2.arcTo((float) (i - (i4 * 2)), f7, f6, f8, 0.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo((float) (this.mCornerRadius + 0), f8);
            Path path3 = this.mRestBackgroundOutline;
            int i5 = this.mCornerRadius;
            path3.arcTo(0.0f, (float) (i2 - (i5 * 2)), (float) ((i5 * 2) + 0), f8, 90.0f, 90.0f, false);
            this.mRestBackgroundOutline.lineTo(0.0f, f5);
            this.mRestBackgroundOutline.close();
        }
    }

    private void createThumbnailPath(int i, int i2, int i3, int i4, Path path) {
        path.reset();
        float f = (float) i;
        float f2 = (float) i2;
        path.moveTo(f, f2);
        float f3 = (float) i3;
        path.lineTo(f3, f2);
        path.lineTo(f3, (float) (i4 - this.mCornerRadius));
        int i5 = this.mCornerRadius;
        float f4 = (float) i4;
        path.arcTo((float) (i3 - (i5 * 2)), (float) (i4 - (i5 * 2)), f3, f4, 0.0f, 90.0f, false);
        path.lineTo((float) (this.mCornerRadius + i), f4);
        int i6 = this.mCornerRadius;
        path.arcTo(f, (float) (i4 - (i6 * 2)), (float) (i + (i6 * 2)), f4, 90.0f, 90.0f, false);
        path.lineTo(f, f2);
        path.close();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.recents.views.TaskViewThumbnail
    public void onDraw(Canvas canvas) {
        int dimensionPixelSize = getResources().getDimensionPixelSize(2131166224);
        int width = this.mTaskViewRect.width();
        int height = this.mTaskViewRect.height() - dimensionPixelSize;
        int min = Math.min(width, (int) (((float) this.mThumbnailRect.width()) * this.mThumbnailScale));
        int min2 = Math.min(height, (int) (((float) this.mThumbnailRect.height()) * this.mThumbnailScale));
        if (this.mUpdateThumbnailOutline) {
            updateThumbnailOutline();
            this.mUpdateThumbnailOutline = false;
        }
        if (this.mUserLocked) {
            canvas.drawPath(this.mThumbnailOutline, this.mLockedPaint);
        } else if (this.mBitmapShader == null || min <= 0 || min2 <= 0) {
            canvas.drawPath(this.mThumbnailOutline, this.mBgFillPaint);
        } else {
            if (min < width) {
                canvas.drawPath(this.mRestBackgroundOutline, this.mBgFillPaint);
            }
            if (min2 < height) {
                canvas.drawPath(this.mRestBackgroundOutline, this.mBgFillPaint);
            }
            canvas.drawPath(this.mThumbnailOutline, getDrawPaint());
        }
    }
}
