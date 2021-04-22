package com.sonymobile.keyguard.plugin.analogclock;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;

public class StandardClockHand extends ClockPart {
    private final int mCenterSizeResourceId;
    private float mLength;
    private final int mLengthId;
    private float mOffsetRadius;
    private final int mThicknessId;

    public StandardClockHand(int i, int i2, int i3, int i4) {
        super(i4);
        this.mCenterSizeResourceId = i;
        this.mLengthId = i2;
        this.mThicknessId = i3;
        Paint paint = getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public final void applyAttributes(TypedArray typedArray) {
        this.mLength = typedArray.getDimension(this.mLengthId, 100.0f);
        this.mOffsetRadius = typedArray.getDimension(this.mCenterSizeResourceId, 100.0f) / 2.0f;
        Paint paint = getPaint();
        paint.setStrokeWidth(typedArray.getDimension(this.mThicknessId, 15.0f));
        paint.setColor(typedArray.getColor(getColorResourceId(), -16711681));
    }

    public void updateClockColor(int i) {
        getPaint().setColor(i);
    }

    public final void draw(Canvas canvas, float f, float f2) {
        float sin = (float) Math.sin((double) getRotation());
        float f3 = -((float) Math.cos((double) getRotation()));
        float f4 = this.mOffsetRadius;
        drawLine(canvas, sin, f3, f + ((float) ((int) (f4 * sin))), f2 + ((float) ((int) (f4 * f3))), f, f2);
    }

    private void drawLine(Canvas canvas, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = this.mOffsetRadius + this.mLength;
        canvas.drawLine(f3, f4, (float) ((int) (f5 + (f * f7))), (float) ((int) (f6 + (f7 * f2))), getPaint());
    }

    public final float getLength() {
        return this.mLength;
    }
}
