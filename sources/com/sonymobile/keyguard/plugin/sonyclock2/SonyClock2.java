package com.sonymobile.keyguard.plugin.sonyclock2;

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
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class SonyClock2 extends LinearLayout implements ClockPlugin {
    private TextView mAlarmDisplay;
    private TextView mAmPmDisplay;
    private CharSequence mAmPmFormat;
    private TextView mDateDisplay;
    private CharSequence mDateFormat;
    private CharSequence mHourFormat12HourClock;
    private CharSequence mHourFormat24HourClock;
    private SonyClock2Digit mHourOnesDigit;
    private SonyClock2Digit mHourTensDigit;
    private boolean mIsTicking;
    private CharSequence mMinuteFormat;
    private SonyClock2Digit mMinuteOnesDigit;
    private SonyClock2Digit mMinuteTensDigit;
    private String mNextAlarmText;
    private Calendar mTime;
    private final BroadcastReceiver mTimeEventReceiver;
    private final ContentObserver mTimeSettingsChangeObserver;

    public SonyClock2(Context context) {
        this(context, null, 0);
    }

    public SonyClock2(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SonyClock2(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsTicking = false;
        this.mTimeEventReceiver = new BroadcastReceiver() {
            /* class com.sonymobile.keyguard.plugin.sonyclock2.SonyClock2.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    SonyClock2.this.createTime(intent.getStringExtra("time-zone"));
                    SonyClock2.this.updateTime();
                } else if ("android.intent.action.TIME_TICK".equals(intent.getAction())) {
                    SonyClock2.this.updateTime(true);
                } else {
                    SonyClock2.this.updateTime();
                }
            }
        };
        this.mTimeSettingsChangeObserver = new ContentObserver(new Handler()) {
            /* class com.sonymobile.keyguard.plugin.sonyclock2.SonyClock2.AnonymousClass2 */

            public void onChange(boolean z) {
                onChange(z, null);
            }

            public void onChange(boolean z, Uri uri) {
                SonyClock2.this.updateTime();
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
        this.mAmPmDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_2_ampm);
        this.mDateDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_2_date);
        this.mAlarmDisplay = (TextView) findViewById(C0007R$id.somc_sony_clock_2_alarm);
        Resources resources = getResources();
        this.mHourFormat12HourClock = resources.getString(C0014R$string.somc_sony_clock_hour_format_12_hour_clock);
        this.mHourFormat24HourClock = resources.getString(C0014R$string.somc_sony_clock_hour_format_24_hour_clock);
        this.mMinuteFormat = resources.getString(C0014R$string.somc_sony_clock_minute_format);
        updateDateFormat();
        this.mAmPmFormat = resources.getString(C0014R$string.somc_sony_clock_am_pm_format);
        SomcKeyguardClockScaleContainer somcKeyguardClockScaleContainer = (SomcKeyguardClockScaleContainer) findViewById(C0007R$id.somc_keyguard_clockplugin_scale_container);
        somcKeyguardClockScaleContainer.setMinScale(0.6f);
        somcKeyguardClockScaleContainer.setMinScaleForComputeNotifications(0.70000005f);
    }

    private void fetchDigitViewHandles() {
        this.mHourTensDigit = (SonyClock2Digit) findViewById(C0007R$id.somc_sony_clock_2_hour_tens_digit);
        this.mHourOnesDigit = (SonyClock2Digit) findViewById(C0007R$id.somc_sony_clock_2_hour_ones_digit);
        this.mMinuteTensDigit = (SonyClock2Digit) findViewById(C0007R$id.somc_sony_clock_2_minute_tens_digit);
        this.mMinuteOnesDigit = (SonyClock2Digit) findViewById(C0007R$id.somc_sony_clock_2_minute_ones_digit);
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
            this.mHourTensDigit.updateDigit(charSequence.charAt(0), z, !DateFormat.is24HourFormat(getContext(), -2));
            this.mHourOnesDigit.updateDigit(charSequence.charAt(1), z);
            this.mMinuteTensDigit.updateDigit(format.charAt(0), z);
            this.mMinuteOnesDigit.updateDigit(format.charAt(1), z);
            updateDateDisplay();
            updateAlarmDisplay();
        }
    }

    private void updateDateDisplay() {
        int i = 8;
        if (DateFormat.is24HourFormat(getContext(), -2)) {
            this.mAmPmDisplay.setVisibility(8);
        } else {
            this.mAmPmDisplay.setText(DateFormat.format(this.mAmPmFormat, this.mTime).toString().toUpperCase(Locale.getDefault()));
            if (this.mAmPmDisplay.length() <= getResources().getInteger(C0008R$integer.somc_digital_clock_max_ampm_chars)) {
                i = 0;
            }
            this.mAmPmDisplay.setVisibility(i);
        }
        this.mDateDisplay.setText(DateFormat.format(this.mDateFormat, this.mTime).toString().toUpperCase(Locale.getDefault()));
    }

    private void updateAlarmDisplay() {
        String str = this.mNextAlarmText;
        if (str == null || str.length() <= 0) {
            this.mAlarmDisplay.setVisibility(8);
            return;
        }
        this.mAlarmDisplay.setText(this.mNextAlarmText);
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
            this.mDateDisplay.setTextColor(color);
            this.mAlarmDisplay.setTextColor(color);
            this.mAlarmDisplay.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            this.mAlarmDisplay.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        }
    }
}
