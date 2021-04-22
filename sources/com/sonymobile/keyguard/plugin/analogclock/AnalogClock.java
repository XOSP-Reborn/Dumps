package com.sonymobile.keyguard.plugin.analogclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.R$styleable;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class AnalogClock extends View implements ClockPlugin {
    private static final int[] DATE_FORMAT_RESOURCES = {C0014R$string.somc_analog_clock_date_format_month, C0014R$string.somc_analog_clock_date_format_day_in_month, C0014R$string.somc_analog_clock_date_format_day_in_week};
    private static final String TAG = AnalogClock.class.getSimpleName();
    private Drawable mAlarmDrawable;
    private float mAlarmDrawableMargin;
    private float mAlarmDrawableVerticalOffset;
    private final HorizontalScalingText mAlarmText;
    private final RectF mCenter;
    private final Path mCenterClipPath;
    private float mCenterSize;
    private float mCenterStrokeWidth;
    private final String[] mDateFormats;
    private float mDateTextSize;
    private float mDateTextSpace;
    private final HorizontalScalingText[] mDateTexts;
    private boolean mDozing;
    private float mEllipseDepth;
    private float mEllipsePower;
    private final Handler mHandler;
    private final StandardClockHand mHourHand;
    private float mHourTickLength;
    private final Path mHourTickPath;
    private final BroadcastReceiver mIntentReceiver;
    private final Rect mInvalidateBounds;
    private boolean mIsTicking;
    private final StandardClockHand mMinuteHand;
    private float mMinuteTickLength;
    private final Path mMinuteTickPath;
    private final String[] mOldDates;
    private Rect mOldSecondsDotBounds;
    private final SecondDotHand mSecondDot;
    private final Runnable mSecondRunnable;
    private Rect mSecondsDotUpdateBounds;
    private Paint mTextPaint;
    private float mTickOffset;
    private final Paint mTickPaint;
    private float mTickRadius;
    private float mTickWidth;
    private final Calendar mTime;

    public AnalogClock(Context context) {
        this(context, null, 0);
    }

    public AnalogClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AnalogClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mSecondRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.analogclock.AnalogClock.AnonymousClass1 */

            public void run() {
                AnalogClock.this.mHandler.removeCallbacks(AnalogClock.this.mSecondRunnable);
                long uptimeMillis = SystemClock.uptimeMillis();
                long j = uptimeMillis + (1000 - (uptimeMillis % 1000));
                AnalogClock.this.updateTime();
                if (AnalogClock.this.mTime.get(11) == 0 && AnalogClock.this.mTime.get(12) == 0 && AnalogClock.this.mTime.get(13) == 0) {
                    AnalogClock.this.updateDateTexts();
                }
                AnalogClock.this.mHandler.postAtTime(this, j);
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            /* class com.sonymobile.keyguard.plugin.analogclock.AnalogClock.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    AnalogClock.this.setTimeZone(intent.getStringExtra("time-zone"));
                }
                AnalogClock.this.updateTime();
                AnalogClock.this.updateDateTexts();
            }
        };
        this.mEllipseDepth = 0.0f;
        this.mEllipsePower = 0.0f;
        this.mTickWidth = 1.0f;
        this.mTickRadius = 100.0f;
        this.mMinuteTickPath = new Path();
        this.mHourTickPath = new Path();
        this.mHourTickLength = 22.0f;
        this.mMinuteTickLength = 12.0f;
        this.mTickOffset = (this.mHourTickLength / 2.0f) - (this.mMinuteTickLength / 2.0f);
        this.mTickPaint = new Paint();
        this.mHandler = new Handler();
        this.mHourHand = new StandardClockHand(R$styleable.AnalogClock_centerSize, R$styleable.AnalogClock_hourHandLength, R$styleable.AnalogClock_hourHandThickness, R$styleable.AnalogClock_clockColor);
        this.mMinuteHand = new StandardClockHand(R$styleable.AnalogClock_centerSize, R$styleable.AnalogClock_minuteHandLength, R$styleable.AnalogClock_minuteHandThickness, R$styleable.AnalogClock_clockColor);
        this.mSecondDot = new SecondDotHand(R$styleable.AnalogClock_secondDotDiameter, R$styleable.AnalogClock_secondDotColor);
        this.mTime = Calendar.getInstance();
        this.mDateTexts = new HorizontalScalingText[3];
        this.mDateFormats = new String[3];
        this.mOldDates = new String[3];
        this.mInvalidateBounds = new Rect();
        this.mTextPaint = HorizontalScalingText.createTextPaint();
        this.mCenter = new RectF();
        this.mCenterClipPath = new Path();
        this.mOldSecondsDotBounds = new Rect();
        this.mSecondsDotUpdateBounds = new Rect();
        this.mIsTicking = false;
        this.mDozing = false;
        this.mAlarmText = new HorizontalScalingText(this.mTextPaint);
        for (int i2 = 0; i2 < 3; i2++) {
            this.mDateFormats[i2] = context.getResources().getString(DATE_FORMAT_RESOURCES[i2]);
            this.mOldDates[i2] = "";
            this.mDateTexts[i2] = new HorizontalScalingText(this.mTextPaint);
        }
        setupAttributes(attributeSet);
        setupPaint();
        updateThemeColors();
    }

    private void setupAttributes(AttributeSet attributeSet) {
        TypedArray obtainStyledAttributes = getContext().getTheme().obtainStyledAttributes(attributeSet, R$styleable.AnalogClock, 0, 0);
        try {
            setupEllipseAttributes(obtainStyledAttributes);
            setupTicksAttributes(obtainStyledAttributes);
            this.mHourHand.applyAttributes(obtainStyledAttributes);
            this.mMinuteHand.applyAttributes(obtainStyledAttributes);
            this.mSecondDot.applyAttributes(obtainStyledAttributes);
            setupClockColor(obtainStyledAttributes);
            setupAlarmAttributes(obtainStyledAttributes);
            this.mCenterSize = obtainStyledAttributes.getDimension(R$styleable.AnalogClock_centerSize, 100.0f);
            setupCenterAttributes(obtainStyledAttributes);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }

    private void setupAlarmAttributes(TypedArray typedArray) {
        this.mAlarmDrawable = typedArray.getDrawable(R$styleable.AnalogClock_alarmDrawable).mutate();
        this.mAlarmDrawableMargin = typedArray.getDimension(R$styleable.AnalogClock_alarmDrawableMargin, 0.0f);
        this.mAlarmDrawableVerticalOffset = typedArray.getDimension(R$styleable.AnalogClock_alarmDrawableVerticalOffset, 20.0f);
    }

    private void setupClockColor(TypedArray typedArray) {
        int color = typedArray.getColor(R$styleable.AnalogClock_clockColor, -65281);
        setupDateTextAttributes(typedArray, color);
        this.mTickPaint.setColor(color);
    }

    private void setupDateTextAttributes(TypedArray typedArray, int i) {
        this.mDateTextSize = typedArray.getDimension(R$styleable.AnalogClock_dateTextSize, 100.0f);
        this.mDateTextSpace = typedArray.getDimension(R$styleable.AnalogClock_dateTextSpace, 2.0f);
        this.mTextPaint.setColor(i);
        setupClockText(this.mDateTexts[1], 1.8461539f);
        setupClockText(this.mDateTexts[2], 1.0f);
        setupClockText(this.mDateTexts[0], 1.0f);
        setupClockText(this.mAlarmText, 1.0f);
    }

    private void setupTicksAttributes(TypedArray typedArray) {
        this.mTickWidth = typedArray.getDimension(R$styleable.AnalogClock_tickWidth, 1.0f);
        this.mTickRadius = typedArray.getDimension(R$styleable.AnalogClock_tickRadius, 100.0f);
        this.mHourTickLength = typedArray.getDimension(R$styleable.AnalogClock_hourTickLength, 22.0f);
        this.mMinuteTickLength = typedArray.getDimension(R$styleable.AnalogClock_minuteTickLength, 12.0f);
        this.mTickOffset = (this.mHourTickLength / 2.0f) - (this.mMinuteTickLength / 2.0f);
    }

    private void setupEllipseAttributes(TypedArray typedArray) {
        this.mEllipseDepth = typedArray.getDimension(R$styleable.AnalogClock_ellipseDepth, 1.0f);
        this.mEllipsePower = typedArray.getFloat(R$styleable.AnalogClock_ellipsePower, 1.0f);
    }

    private void setupCenterAttributes(TypedArray typedArray) {
        this.mCenterStrokeWidth = typedArray.getDimension(R$styleable.AnalogClock_centerStrokeWidth, 5.0f);
    }

    private void setupPaint() {
        this.mTickPaint.setStyle(Paint.Style.STROKE);
        this.mTickPaint.setAntiAlias(true);
    }

    private void setupClockText(HorizontalScalingText horizontalScalingText, float f) {
        horizontalScalingText.setTextSize(this.mDateTextSize * f);
    }

    /* access modifiers changed from: protected */
    public final void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawClockFace(canvas, getWidth(), getHeight());
        if (!this.mDozing) {
            this.mSecondDot.draw(canvas);
        }
    }

    private void drawClockFace(Canvas canvas, int i, int i2) {
        float f = ((float) i) / 2.0f;
        float f2 = ((float) i2) / 2.0f;
        drawTicks(canvas);
        drawDateBlock(canvas, f, f2);
        int save = canvas.save();
        canvas.clipPath(this.mCenterClipPath, Region.Op.DIFFERENCE);
        this.mHourHand.draw(canvas, f, f2);
        this.mMinuteHand.draw(canvas, f, f2);
        canvas.restoreToCount(save);
        drawCenter(canvas);
        drawAlarm(canvas, f, f2);
    }

    private void drawTicks(Canvas canvas) {
        this.mTickPaint.setStrokeWidth(this.mTickWidth / 3.0f);
        canvas.drawPath(this.mMinuteTickPath, this.mTickPaint);
        this.mTickPaint.setStrokeWidth(this.mTickWidth);
        canvas.drawPath(this.mHourTickPath, this.mTickPaint);
    }

    private void drawAlarm(Canvas canvas, float f, float f2) {
        if (this.mAlarmText.getText().length() > 0) {
            float f3 = f2 * 1.6f;
            this.mAlarmText.measureTextBounds();
            float intrinsicWidth = (float) (this.mAlarmDrawable.getIntrinsicWidth() / 2);
            drawAlarmDrawable(canvas, (((((float) this.mAlarmText.getLeft()) + f) - intrinsicWidth) - (((float) this.mAlarmText.getWidth()) / 2.0f)) - this.mAlarmDrawableMargin, f3);
            this.mAlarmText.drawText(canvas, f + intrinsicWidth, f3, null);
        }
    }

    private void drawAlarmDrawable(Canvas canvas, float f, float f2) {
        int intrinsicWidth = this.mAlarmDrawable.getIntrinsicWidth();
        int intrinsicHeight = this.mAlarmDrawable.getIntrinsicHeight();
        int i = (int) f;
        int bottom = (int) (f2 + (((float) this.mAlarmText.getBottom()) / 2.0f) + this.mAlarmDrawableVerticalOffset);
        Drawable drawable = this.mAlarmDrawable;
        drawable.setBounds(i, bottom - intrinsicHeight, intrinsicWidth + i, bottom);
        this.mAlarmDrawable.draw(canvas);
    }

    private void drawCenter(Canvas canvas) {
        this.mTickPaint.setStrokeWidth(this.mCenterStrokeWidth);
        canvas.drawOval(this.mCenter, this.mTickPaint);
    }

    private void drawDateBlock(Canvas canvas, float f, float f2) {
        measureDateStrings();
        float length = (((this.mCenterSize / 4.0f) + f) + (((this.mHourHand.getLength() + f) - this.mHourTickLength) / 2.0f)) - this.mMinuteTickLength;
        float height = (f2 - (((float) this.mDateTexts[1].getHeight()) / 2.0f)) - this.mDateTextSpace;
        Typeface create = Typeface.create("sans-serif-medium", 0);
        Typeface create2 = Typeface.create("sst", 0);
        this.mDateTexts[0].drawText(canvas, length, height, create);
        float bottom = height + ((float) (this.mDateTexts[0].getBottom() - this.mDateTexts[1].getTop())) + this.mDateTextSpace;
        this.mDateTexts[1].drawText(canvas, length, bottom, create2);
        this.mDateTexts[2].drawText(canvas, length, bottom + ((float) (this.mDateTexts[1].getBottom() - this.mDateTexts[2].getTop())) + this.mDateTextSpace, create);
    }

    private void measureDateStrings() {
        this.mDateTexts[0].measureTextBounds();
        this.mDateTexts[2].measureTextBounds();
        this.mDateTexts[1].measureTextBounds();
    }

    private void setupTickPath(int i, int i2) {
        this.mMinuteTickPath.reset();
        this.mHourTickPath.reset();
        for (int i3 = 0; i3 < 360; i3 += 6) {
            float radians = (float) Math.toRadians((double) i3);
            if (i3 % 30 == 0) {
                drawTick(this.mHourTickPath, this.mHourTickLength, (float) i, (float) i2, radians, 0.0f);
            } else {
                drawTick(this.mMinuteTickPath, this.mMinuteTickLength, (float) i, (float) i2, radians, this.mTickOffset);
            }
        }
    }

    private void drawTick(Path path, float f, float f2, float f3, float f4, float f5) {
        float radiusOffset = getRadiusOffset(f4);
        double d = (double) f4;
        float sin = (float) Math.sin(d);
        float cos = (float) Math.cos(d);
        float f6 = ((this.mTickRadius - f) + radiusOffset) - f5;
        float f7 = f + f6;
        path.moveTo((f6 * sin) + f2, (f6 * cos) + f3);
        path.lineTo(f2 + (sin * f7), f3 + (f7 * cos));
    }

    private float getRadiusOffset(float f) {
        return (float) (this.mEllipseDepth > 0.0f ? (int) (Math.pow(Math.abs(Math.sin((double) (f * 2.0f))), (double) this.mEllipsePower) * ((double) this.mEllipseDepth)) : 0);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTime() {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        updateClock(getWidth(), getHeight());
    }

    private void updateClock(int i, int i2) {
        float f = (float) this.mTime.get(12);
        boolean rotation = this.mHourHand.setRotation((((float) this.mTime.get(10)) * 0.5235988f) + (0.008726646f * f));
        if (this.mMinuteHand.setRotation(f * 0.10471976f) || rotation) {
            invalidate();
        }
        if (!this.mDozing) {
            updateSecondsHand();
        }
    }

    private void updateSecondsHand() {
        float f = ((float) this.mTime.get(13)) * 0.10471976f;
        this.mSecondDot.setRotation(f);
        this.mSecondDot.getBounds(this.mOldSecondsDotBounds);
        this.mSecondDot.calculateSecondsDotPosition(f, ((getRadiusOffset(f) + this.mTickRadius) - (this.mMinuteTickLength / 2.0f)) - this.mTickOffset, ((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f);
        this.mSecondDot.getBounds(this.mSecondsDotUpdateBounds);
        this.mSecondsDotUpdateBounds.union(this.mOldSecondsDotBounds);
        invalidate(this.mSecondsDotUpdateBounds);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateDateTexts() {
        invalidateDateBounds();
        Locale locale = Locale.getDefault();
        for (int i = 0; i < 3; i++) {
            String str = (String) DateFormat.format(this.mDateFormats[i], this.mTime);
            if (!this.mOldDates[i].equals(str)) {
                this.mOldDates[i] = str;
                this.mDateTexts[i].setText(str.toUpperCase(locale));
            }
        }
    }

    private void updateDateFormats() {
        for (int i = 0; i < 3; i++) {
            this.mDateFormats[i] = getContext().getResources().getString(DATE_FORMAT_RESOURCES[i]);
        }
    }

    private void invalidateDateBounds() {
        this.mInvalidateBounds.set(this.mDateTexts[0].getBoundsReadOnly());
        this.mInvalidateBounds.union(this.mDateTexts[1].getBoundsReadOnly());
        this.mInvalidateBounds.union(this.mDateTexts[2].getBoundsReadOnly());
        invalidate(this.mInvalidateBounds);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void startClockTicking() {
        if (!this.mIsTicking) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
            setTimeZone(null);
            updateTime();
            updateDateFormats();
            updateDateTexts();
            if (!this.mDozing) {
                this.mSecondRunnable.run();
            }
            this.mIsTicking = true;
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void stopClockTicking() {
        if (!this.mDozing) {
            this.mHandler.removeCallbacks(this.mSecondRunnable);
        }
        if (this.mIsTicking) {
            getContext().unregisterReceiver(this.mIntentReceiver);
            this.mIsTicking = false;
        }
    }

    /* access modifiers changed from: protected */
    public final void onSizeChanged(int i, int i2, int i3, int i4) {
        RectF rectF = this.mCenter;
        float f = (float) i;
        float f2 = this.mCenterSize;
        rectF.set((f - f2) / 2.0f, (((float) i2) - f2) / 2.0f, (f + f2) / 2.0f, (f + f2) / 2.0f);
        setupTickPath(i / 2, i2 / 2);
        this.mCenterClipPath.reset();
        this.mCenterClipPath.addOval(this.mCenter, Path.Direction.CW);
        updateClock(i, i2);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void setNextAlarmText(String str) {
        this.mAlarmText.setText(str.toUpperCase(Locale.getDefault()));
        this.mInvalidateBounds.set(this.mAlarmText.getBoundsReadOnly());
        this.mInvalidateBounds.union(this.mAlarmDrawable.getBounds());
        invalidate(this.mInvalidateBounds);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setTimeZone(String str) {
        if (str == null) {
            this.mTime.setTimeZone(TimeZone.getDefault());
        } else {
            this.mTime.setTimeZone(TimeZone.getTimeZone(str));
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        this.mDozing = true;
        this.mHourHand.updateClockColor(-1);
        this.mMinuteHand.updateClockColor(-1);
        this.mTickPaint.setColor(-1);
        this.mTextPaint.setColor(-1);
        this.mAlarmDrawable.setColorFilter(-1, PorterDuff.Mode.SRC_IN);
        invalidate();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        updateTime();
        updateDateTexts();
    }

    private void updateThemeColors() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            int color2 = resources.getColor(C0004R$color.somc_keyguard_theme_color_analog_clock_seconds_hand, null);
            this.mHourHand.updateClockColor(color);
            this.mMinuteHand.updateClockColor(color);
            this.mSecondDot.updateClockColor(color2);
            this.mTickPaint.setColor(color);
            this.mTextPaint.setColor(color);
            this.mAlarmDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            invalidate();
        }
    }
}
