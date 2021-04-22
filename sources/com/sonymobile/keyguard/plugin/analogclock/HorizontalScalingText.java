package com.sonymobile.keyguard.plugin.analogclock;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class HorizontalScalingText {
    private final Rect mBounds = new Rect();
    private final Paint mPaint;
    private float mSize = 100.0f;
    private String mText = "";

    public HorizontalScalingText(Paint paint) {
        this.mPaint = paint;
    }

    public static Paint createTextPaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setSubpixelText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        return paint;
    }

    public final void setTextSize(float f) {
        this.mSize = f;
    }

    public final void measureTextBounds() {
        this.mPaint.setTextScaleX(1.0f);
        this.mPaint.setTextSize(this.mSize);
        Paint paint = this.mPaint;
        String str = this.mText;
        paint.getTextBounds(str, 0, str.length(), this.mBounds);
    }

    public final void drawText(Canvas canvas, float f, float f2, Typeface typeface) {
        if (typeface != null) {
            this.mPaint.setTypeface(typeface);
        }
        this.mPaint.setTextSize(this.mSize);
        canvas.drawText(this.mText, f, f2, this.mPaint);
    }

    public final String getText() {
        return this.mText;
    }

    public final int getLeft() {
        return this.mBounds.left;
    }

    public final int getWidth() {
        return this.mBounds.width();
    }

    public final int getBottom() {
        return this.mBounds.bottom;
    }

    public final int getTop() {
        return this.mBounds.top;
    }

    public final int getHeight() {
        return this.mBounds.height();
    }

    public final void setText(String str) {
        this.mText = str;
    }

    public final Rect getBoundsReadOnly() {
        return this.mBounds;
    }
}
