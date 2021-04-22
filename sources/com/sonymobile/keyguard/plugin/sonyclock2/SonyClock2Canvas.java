package com.sonymobile.keyguard.plugin.sonyclock2;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.PathParser;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0008R$integer;

public class SonyClock2Canvas extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String[] STRING_PATH_HOURS = {"M41.39,5.64C20.81,5.64,5.29,21.1,5.29,61c0,39,15.4,55,36.1,55,19.94,0,35.37-16.3,35.37-55,0-39.9-15.43-55.36-35.37-55.36", "M21.68,23.83c7.69-5.1,24.49-16.07,24.49-16.07h1.39V115.7", "M12.65,18C28.15-.45,58.31,3.94,66,17.45c17.57,30.7-20.66,59.23-56.4,95.65v1.15H73.82", "M10.55,13C33,0.28,67.58,3,67.58,32.43c0,24.37-25.85,27.11-36.79,27.11h0s40.29-4,40.29,28.59c0,33.59-43.58,32.66-62.9,21", "M79.65,83.61H4.53V83.37L57.77,7.76H60V115.7", "M67.26,7.76H16.2l-1.64,47s27.17-7.93,44.85,3.74c15.87,10.47,17.5,40.8-2.55,52.65-14,8.26-32.67,5.45-47.44-.57", "M67.62,9S46.83-.08,29.15,11.41C12.44,22.26,8,54.9,9.06,75.4s9.7,40.48,32.86,40.48c19.33,0,33-12.93,33-34.46,0-18.23-12-33.18-32.45-33.18C26.41,48.24,8.6,60.07,9.68,82", "M8.91,7.76H71.63V8.49s-18,25.69-30.29,65.15c-4,12.71-9.37,35.73-9.37,42", "M42.22,58.3C22.63,58.3,6.84,69.2,6.84,87.48c0,20.44,15.87,28.44,35.46,28.44s33.83-8.81,33.83-29.25c0-19.1-18.63-28.37-33.91-28.37C24.69,58.3,10.49,50.44,10.49,32c0-16.45,14.2-26.26,31.73-26.26s30.27,11,30.27,25.53c0,18.39-12.75,27-30.27,27", "M73.38,40.25c1.7,16.9-12.08,33.29-33.73,33.29-19.21,0-31.6-14.61-31.6-33.33C8.05,19.55,22.39,5.79,39.9,5.79c15.56,0,34.64,8.78,34,46.46-0.51,31.8-7.7,51.84-23.73,60.33-14.74,7.81-36.2.08-36.2,0.08", "M31.48,23.83C39.17,18.74,56,7.76,56,7.76h1.39V115.7"};
    private static final String[] STRING_PATH_MINUTES = {"M18.2,5C9.82,5,3.9,11.2,3.9,27S9.82,48.93,18.2,48.93c8.06,0,13.89-6.13,13.89-21.93S26.26,5,18.2,5", "M8.09,12.87L19.37,5.72h1.4V49.83", "M5.57,9.62C12.82,3,24.68,4,27.8,9.53c7.22,12.69-8,23.23-22.31,37.84v0.79H32", "M4.91,7.87c9.47-5,23.66-4.26,23.66,7.49,0,8.91-11.71,10.88-15.59,10.88h0c3.07,0,16.87.1,16.87,11.34,0,13.4-18,13-25.88,8.65", "M34.13,36.59H3.24V34.81l19.52-29h3v44", "M29.26,5.69h-21L7.43,24.08c4.86-.66,12.88-1.46,18.22,2.4,6,4.35,6.35,15.95-1.64,20.46-5.64,3.19-14.13,2.21-19.61-.33", "M29.33,6.39s-9.87-3.45-16,.71c-6.56,4.43-8.55,16.25-8,26.07,0.44,8.22,4.47,15.75,13.23,15.75,7.85,0,13.26-5,13.26-13.71C31.76,27.8,27.33,22,19,22,12.51,22,5.1,27,5.53,35.81", "M4.61,5.84h25.6V6.45c-6.66,10.44-9.35,17-12.74,28.2-0.84,2.78-3.15,11.87-3.15,15.08", "M18.49,25.92c-7.5,0-14.14,4.36-14.14,11.67C4.35,45.76,10.7,49,18.53,49c7.5,0,13.53-3.52,13.53-11.69,0-7.64-7.93-11.34-13.56-11.34-6.84,0-12.56-3.14-12.56-10.5,0-6.58,5.56-10.5,12.56-10.5s12,4.41,12,10.21c0,7.35-5,10.79-12,10.79", "M30.91,17.95c0.68,6.72-4.84,14-13.45,14C9.18,32,4.6,26.18,4.6,18.74c0-8.21,5.36-13.68,13-13.68,6.18,0,13.92,3.49,13.68,18.47C31,36.16,28,44.13,21.66,47.5c-5.86,3.11-15.3,0-15.3,0", "M12.09,12.87L23.37,5.72h1.4V49.83"};
    private int mAnimDurationStroke;
    private int mAnimDurationTotal;
    private int mAnimDurationZoomout;
    private Context mContext;
    private float[] mDotPos;
    private float mDotRadius;
    private float mDotRadiusRate;
    private PathMeasure mMeasure = new PathMeasure();
    private Paint mPaint = new Paint();
    private Path mPath;
    private final Path[] mPathHours = new Path[STRING_PATH_HOURS.length];
    private final Path[] mPathMinutes = new Path[STRING_PATH_MINUTES.length];
    private int mTextColor = -1;

    public SonyClock2Canvas(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
        Resources resources = context.getResources();
        this.mPaint.setAntiAlias(true);
        this.mAnimDurationStroke = resources.getInteger(C0008R$integer.somc_sony_clock_2_digit_draw_animation_duration);
        this.mAnimDurationZoomout = resources.getInteger(C0008R$integer.somc_sony_clock_2_dot_zoomout_animation_duration);
        this.mAnimDurationTotal = this.mAnimDurationStroke + this.mAnimDurationZoomout;
        this.mTextColor = Utils.getColorAttrDefaultColor(this.mContext, C0002R$attr.wallpaperTextColor);
        initializeColor();
        initializePath();
    }

    private void initializeColor() {
        this.mPaint.setColor(this.mTextColor);
    }

    private void initializePath() {
        float f = this.mContext.getResources().getDisplayMetrics().density;
        Matrix matrix = new Matrix();
        matrix.setScale(f, f);
        int i = 0;
        int i2 = 0;
        while (true) {
            Path[] pathArr = this.mPathHours;
            if (i2 >= pathArr.length) {
                break;
            }
            pathArr[i2] = PathParser.createPathFromPathData(STRING_PATH_HOURS[i2]);
            this.mPathHours[i2].transform(matrix);
            i2++;
        }
        while (true) {
            Path[] pathArr2 = this.mPathMinutes;
            if (i < pathArr2.length) {
                pathArr2[i] = PathParser.createPathFromPathData(STRING_PATH_MINUTES[i]);
                this.mPathMinutes[i].transform(matrix);
                i++;
            } else {
                return;
            }
        }
    }

    private float convertDp2Px(float f) {
        return f * this.mContext.getResources().getDisplayMetrics().density;
    }

    /* access modifiers changed from: protected */
    public final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(this.mPath, this.mPaint);
        if (this.mDotPos != null) {
            this.mPaint.setStyle(Paint.Style.FILL);
            float[] fArr = this.mDotPos;
            canvas.drawCircle(fArr[0], fArr[1], this.mDotRadius * this.mDotRadiusRate, this.mPaint);
        }
    }

    public final void setDigit(int i, boolean z, boolean z2, boolean z3) {
        Path[] pathArr;
        this.mDotPos = null;
        this.mDotRadiusRate = 1.0f;
        this.mPath = new Path();
        if (z) {
            pathArr = this.mPathHours;
            this.mPaint.setStrokeWidth(convertDp2Px(4.17f));
            this.mDotRadius = convertDp2Px(5.0f);
        } else {
            pathArr = this.mPathMinutes;
            this.mPaint.setStrokeWidth(convertDp2Px(3.35f));
            this.mDotRadius = convertDp2Px(4.0f);
        }
        this.mPath.set((!z2 || i != 1) ? pathArr[i] : pathArr[10]);
        this.mMeasure.setPath(this.mPath, false);
        if (!z3) {
            invalidate();
        } else {
            startAnimation();
        }
    }

    private void startAnimation() {
        ObjectAnimator ofInt = ObjectAnimator.ofInt(this, "elapsed_time", 0, this.mAnimDurationTotal);
        ofInt.addUpdateListener(this);
        ofInt.setDuration((long) this.mAnimDurationTotal);
        ofInt.start();
    }

    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        int intValue = ((Integer) valueAnimator.getAnimatedValue("elapsed_time")).intValue();
        this.mPath.reset();
        if (intValue <= this.mAnimDurationStroke) {
            PathMeasure pathMeasure = this.mMeasure;
            float f = (float) intValue;
            pathMeasure.getSegment(0.0f, pathMeasure.getLength() * (f / ((float) this.mAnimDurationStroke)), this.mPath, true);
            float[] fArr = new float[2];
            PathMeasure pathMeasure2 = this.mMeasure;
            pathMeasure2.getPosTan(pathMeasure2.getLength() * (f / ((float) this.mAnimDurationStroke)), fArr, new float[2]);
            this.mDotPos = fArr;
            this.mDotRadiusRate = 1.0f;
        } else {
            PathMeasure pathMeasure3 = this.mMeasure;
            pathMeasure3.getSegment(0.0f, pathMeasure3.getLength(), this.mPath, true);
            float[] fArr2 = new float[2];
            PathMeasure pathMeasure4 = this.mMeasure;
            pathMeasure4.getPosTan(pathMeasure4.getLength(), fArr2, new float[2]);
            this.mDotPos = fArr2;
            this.mDotRadiusRate = ((float) Math.abs(this.mAnimDurationTotal - intValue)) / ((float) this.mAnimDurationZoomout);
        }
        invalidate();
    }

    public void updateThemeColor(int i) {
        this.mTextColor = i;
        initializeColor();
        invalidate();
    }
}
