package com.sonymobile.keyguard.plugin.sonyclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class SonyClock extends LinearLayout implements ClockPlugin {
    private TextView mAlarmDisplay;
    private TextView mAmPmDisplay;
    private CharSequence mAmPmFormat;
    private TextView mDashDisplay;
    private TextView mDateDisplay;
    private CharSequence mDateFormat;
    private CharSequence mHourFormat12HourClock;
    private CharSequence mHourFormat24HourClock;
    private SonyClockDigit mHourOnesDigit;
    private SonyClockDigit mHourTensDigit;
    private boolean mIsTicking;
    private CharSequence mMinuteFormat;
    private SonyClockDigit mMinuteOnesDigit;
    private SonyClockDigit mMinuteTensDigit;
    private String mNextAlarmText;
    private Calendar mTime;
    private final BroadcastReceiver mTimeEventReceiver;
    private final ContentObserver mTimeSettingsChangeObserver;

    public SonyClock(Context context) {
        this(context, null, 0);
    }

    public SonyClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SonyClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsTicking = false;
        this.mTimeEventReceiver = new BroadcastReceiver() {
            /* class com.sonymobile.keyguard.plugin.sonyclock.SonyClock.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    SonyClock.this.createTime(intent.getStringExtra("time-zone"));
                    SonyClock.this.updateTime();
                } else if ("android.intent.action.TIME_TICK".equals(intent.getAction())) {
                    SonyClock.this.updateTime(true);
                } else {
                    SonyClock.this.updateTime();
                }
            }
        };
        this.mTimeSettingsChangeObserver = new ContentObserver(new Handler()) {
            /* class com.sonymobile.keyguard.plugin.sonyclock.SonyClock.AnonymousClass2 */

            public void onChange(boolean z) {
                onChange(z, null);
            }

            public void onChange(boolean z, Uri uri) {
                SonyClock.this.updateTime();
            }
        };
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        super.onFinishInflate();
        fetchViewHandles();
        updateThemeColors();
    }

    private void fetchViewHandles() {
        fetchDigitViewHandles();
        this.mAmPmDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_ampm);
        this.mDashDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_dash);
        this.mDateDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_date);
        this.mAlarmDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_alarm);
        Resources resources = getResources();
        this.mHourFormat12HourClock = resources.getString(C0014R$string.somc_sony_clock_hour_format_12_hour_clock);
        this.mHourFormat24HourClock = resources.getString(C0014R$string.somc_sony_clock_hour_format_24_hour_clock);
        this.mMinuteFormat = resources.getString(C0014R$string.somc_sony_clock_minute_format);
        updateDateFormat();
        this.mAmPmFormat = resources.getString(C0014R$string.somc_sony_clock_am_pm_format);
        SomcKeyguardClockScaleContainer somcKeyguardClockScaleContainer = (SomcKeyguardClockScaleContainer) findViewById(C0007R$id.somc_keyguard_clockplugin_scale_container);
        somcKeyguardClockScaleContainer.setMinScale(0.4f);
        somcKeyguardClockScaleContainer.setMinScaleForComputeNotifications(0.5f);
    }

    private void fetchDigitViewHandles() {
        this.mHourTensDigit = (SonyClockDigit) findViewById(C0007R$id.somc_sony_clock_hour_tens_digit);
        this.mHourOnesDigit = (SonyClockDigit) findViewById(C0007R$id.somc_sony_clock_hour_ones_digit);
        this.mMinuteTensDigit = (SonyClockDigit) findViewById(C0007R$id.somc_sony_clock_minute_tens_digit);
        this.mMinuteOnesDigit = (SonyClockDigit) findViewById(C0007R$id.somc_sony_clock_minute_ones_digit);
        this.mHourTensDigit.setDigitViews(findViewById(C0007R$id.somc_sony_clock_digit_current_hour_tens), findViewById(C0007R$id.somc_sony_clock_digit_old_hour_tens), findViewById(C0007R$id.somc_sony_clock_digit_place_holder_hour_tens));
        this.mHourOnesDigit.setDigitViews(findViewById(C0007R$id.somc_sony_clock_digit_current_hour_ones), findViewById(C0007R$id.somc_sony_clock_digit_old_hour_ones), findViewById(C0007R$id.somc_sony_clock_digit_place_holder_hour_ones));
        this.mMinuteTensDigit.setDigitViews(findViewById(C0007R$id.somc_sony_clock_digit_current_minute_tens), findViewById(C0007R$id.somc_sony_clock_digit_old_minute_tens), findViewById(C0007R$id.somc_sony_clock_digit_place_holder_minute_tens));
        this.mMinuteOnesDigit.setDigitViews(findViewById(C0007R$id.somc_sony_clock_digit_current_minute_ones), findViewById(C0007R$id.somc_sony_clock_digit_old_minute_ones), findViewById(C0007R$id.somc_sony_clock_digit_place_holder_minute_ones));
    }

    private void updateDateFormat() {
        this.mDateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), getResources().getString(C0014R$string.somc_sony_clock_date_format));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void createTime(String str) {
        if (str != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(str));
        } else {
            this.mTime = Calendar.getInstance();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTime() {
        updateTime(false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTime(boolean z) {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        updateClockDisplay(z);
    }

    private void updateClockDisplay(boolean z) {
        CharSequence charSequence;
        if (this.mIsTicking) {
            if (DateFormat.is24HourFormat(getContext(), -2)) {
                charSequence = DateFormat.format(this.mHourFormat24HourClock, this.mTime);
            } else {
                charSequence = DateFormat.format(this.mHourFormat12HourClock, this.mTime);
            }
            CharSequence format = DateFormat.format(this.mMinuteFormat, this.mTime);
            this.mHourTensDigit.updateDigit(charSequence.charAt(0), z);
            this.mHourOnesDigit.updateDigit(charSequence.charAt(1), z);
            this.mMinuteTensDigit.updateDigit(format.charAt(0), z);
            this.mMinuteOnesDigit.updateDigit(format.charAt(1), z);
            updateDateDisplay();
            updateAlarmDisplay();
        }
    }

    private void updateDateDisplay() {
        if (DateFormat.is24HourFormat(getContext(), -2)) {
            this.mAmPmDisplay.setVisibility(8);
            this.mDashDisplay.setVisibility(8);
        } else {
            this.mAmPmDisplay.setText(DateFormat.format(this.mAmPmFormat, this.mTime).toString().toUpperCase(Locale.getDefault()));
            this.mAmPmDisplay.setVisibility(0);
            this.mAmPmDisplay.setTextSize(0, getResources().getDimension(C0005R$dimen.somc_sony_clock_date_digit_size));
            this.mDashDisplay.setTextSize(0, getResources().getDimension(C0005R$dimen.somc_sony_clock_date_digit_size));
            this.mDashDisplay.setVisibility(0);
        }
        this.mDateDisplay.setText(DateFormat.format(this.mDateFormat, this.mTime).toString().toUpperCase(Locale.getDefault()));
        this.mDateDisplay.setTextSize(0, getResources().getDimension(C0005R$dimen.somc_sony_clock_date_digit_size));
    }

    private void updateAlarmDisplay() {
        String str = this.mNextAlarmText;
        if (str == null || str.length() <= 0) {
            this.mAlarmDisplay.setVisibility(8);
            return;
        }
        this.mAlarmDisplay.setText(this.mNextAlarmText);
        this.mAlarmDisplay.setTextSize(0, (float) getResources().getDimensionPixelSize(C0005R$dimen.widget_label_font_size));
        this.mAlarmDisplay.setVisibility(0);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void setNextAlarmText(String str) {
        this.mNextAlarmText = str.toUpperCase(Locale.getDefault());
        updateClockDisplay(false);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void startClockTicking() {
        if (!this.mIsTicking) {
            this.mIsTicking = true;
            updateDateFormat();
            createTime(null);
            registerTimeEventReceiver();
            registerTimeSettingsChangeObserver();
        }
        updateTime();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void stopClockTicking() {
        if (this.mIsTicking) {
            this.mIsTicking = false;
            unregisterTimeEventReceiver();
            unregisterTimeSettingsChangeObserver();
        }
    }

    private void registerTimeEventReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(this.mTimeEventReceiver, intentFilter, null, getHandler());
    }

    private void unregisterTimeEventReceiver() {
        getContext().unregisterReceiver(this.mTimeEventReceiver);
    }

    private void registerTimeSettingsChangeObserver() {
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mTimeSettingsChangeObserver);
    }

    private void unregisterTimeSettingsChangeObserver() {
        getContext().getContentResolver().unregisterContentObserver(this.mTimeSettingsChangeObserver);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        this.mHourTensDigit.updateThemeColor(-1);
        this.mHourOnesDigit.updateThemeColor(-1);
        this.mMinuteTensDigit.updateThemeColor(-1);
        this.mMinuteOnesDigit.updateThemeColor(-1);
        this.mAmPmDisplay.setTextColor(-1);
        this.mDashDisplay.setTextColor(-1);
        this.mDateDisplay.setTextColor(-1);
        this.mAlarmDisplay.setTextColor(-1);
        this.mAlarmDisplay.setCompoundDrawableTintList(ColorStateList.valueOf(-1));
        this.mAlarmDisplay.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        invalidate();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        updateTime();
    }

    private void updateThemeColors() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            this.mHourTensDigit.updateThemeColor(color);
            this.mHourOnesDigit.updateThemeColor(color);
            this.mMinuteTensDigit.updateThemeColor(color);
            this.mMinuteOnesDigit.updateThemeColor(color);
            this.mAmPmDisplay.setTextColor(color);
            this.mDashDisplay.setTextColor(color);
            this.mDateDisplay.setTextColor(color);
            this.mAlarmDisplay.setTextColor(color);
            this.mAlarmDisplay.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            this.mAlarmDisplay.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        }
    }
}
