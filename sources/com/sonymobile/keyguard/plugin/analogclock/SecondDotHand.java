package com.sonymobile.keyguard.plugin.analogclock;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

public class SecondDotHand extends ClockPart {
    private final RectF mBounds = new RectF();
    private float mDiameter;
    private final int mDiameterResourceId;

    public SecondDotHand(int i, int i2) {
        super(i2);
        this.mDiameterResourceId = i;
    }

    public final void applyAttributes(TypedArray typedArray) {
        this.mDiameter = typedArray.getDimension(this.mDiameterResourceId, 20.0f);
        getPaint().setColor(typedArray.getColor(getColorResourceId(), -16711936));
    }

    public final void calculateSecondsDotPosition(float f, float f2, float f3, float f4) {
        float f5 = this.mDiameter / 2.0f;
        double d = (double) f2;
        double d2 = (double) f;
        float sin = (float) (((double) f3) + (Math.sin(d2) * d));
        float cos = (float) (((double) f4) - (d * Math.cos(d2)));
        this.mBounds.set(sin - f5, cos - f5, sin + f5, cos + f5);
    }

    public final void getBounds(Rect rect) {
        this.mBounds.round(rect);
    }

    public final void draw(Canvas canvas) {
        canvas.drawOval(this.mBounds, getPaint());
    }

    public void updateClockColor(int i) {
        getPaint().setColor(i);
    }
}
