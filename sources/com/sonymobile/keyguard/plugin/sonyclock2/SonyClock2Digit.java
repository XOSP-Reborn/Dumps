package com.sonymobile.keyguard.plugin.sonyclock2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.C0007R$id;

public class SonyClock2Digit extends FrameLayout {
    private static final String[] ARABIC_NUMS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private int mCurrentDigit;
    private SonyClock2Canvas mImageDigit;
    private boolean mIsHourDigit;
    private boolean mIsTensDigit;
    private TextView mTextDigit;

    public SonyClock2Digit(Context context) {
        this(context, null, 0, 0);
    }

    public SonyClock2Digit(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public SonyClock2Digit(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SonyClock2Digit(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mIsHourDigit = true;
        this.mIsTensDigit = true;
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        super.onFinishInflate();
        int id = getId();
        if (id == C0007R$id.somc_sony_clock_2_hour_tens_digit) {
            this.mImageDigit = (SonyClock2Canvas) findViewById(C0007R$id.hour_tens_digit);
            this.mTextDigit = (TextView) findViewById(C0007R$id.hour_tens_digit_text);
            this.mIsHourDigit = true;
            this.mIsTensDigit = true;
        } else if (id == C0007R$id.somc_sony_clock_2_hour_ones_digit) {
            this.mImageDigit = (SonyClock2Canvas) findViewById(C0007R$id.hour_ones_digit);
            this.mTextDigit = (TextView) findViewById(C0007R$id.hour_ones_digit_text);
            this.mIsHourDigit = true;
            this.mIsTensDigit = false;
        } else if (id == C0007R$id.somc_sony_clock_2_minute_tens_digit) {
            this.mImageDigit = (SonyClock2Canvas) findViewById(C0007R$id.minute_tens_digit);
            this.mTextDigit = (TextView) findViewById(C0007R$id.minute_tens_digit_text);
            this.mIsHourDigit = false;
            this.mIsTensDigit = true;
        } else if (id == C0007R$id.somc_sony_clock_2_minute_ones_digit) {
            this.mImageDigit = (SonyClock2Canvas) findViewById(C0007R$id.minute_ones_digit);
            this.mTextDigit = (TextView) findViewById(C0007R$id.minute_ones_digit_text);
            this.mIsHourDigit = false;
            this.mIsTensDigit = false;
        } else {
            this.mIsHourDigit = false;
            this.mIsTensDigit = false;
        }
    }

    public final void updateDigit(char c, boolean z) {
        updateDigit(c, z, false);
    }

    public final void updateDigit(char c, boolean z, boolean z2) {
        int parseInt = Integer.parseInt(String.valueOf(c));
        if (isArabicNumber(c)) {
            this.mTextDigit.setVisibility(8);
            if (!z2 || parseInt != 0) {
                this.mImageDigit.setDigit(parseInt, this.mIsHourDigit, this.mIsTensDigit, z && parseInt != this.mCurrentDigit);
                this.mImageDigit.setVisibility(0);
            } else {
                this.mImageDigit.setVisibility(8);
            }
        } else {
            this.mImageDigit.setVisibility(8);
            this.mTextDigit.setText(String.valueOf(c));
            if (!z2 || parseInt != 0) {
                this.mTextDigit.setVisibility(0);
            } else {
                this.mTextDigit.setVisibility(8);
            }
        }
        this.mCurrentDigit = parseInt;
    }

    private boolean isArabicNumber(char c) {
        return String.valueOf(c).equals(ARABIC_NUMS[Integer.parseInt(String.valueOf(c))]);
    }

    public void updateThemeColor(int i) {
        this.mImageDigit.updateThemeColor(i);
        this.mTextDigit.setTextColor(i);
    }
}
