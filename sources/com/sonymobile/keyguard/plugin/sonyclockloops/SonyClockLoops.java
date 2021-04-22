package com.sonymobile.keyguard.plugin.sonyclockloops;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverController;
import com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.Locale;

public class SonyClockLoops extends LinearLayout implements ClockPlugin {
    private TextView mAlarm;
    private TextView mAmPm;
    private TextView mClockHour;
    private TextView mClockMinute;
    private TextView mClockSeparator;
    private LinearLayout mClockView;
    private Context mContext;
    private TextView mDate;
    private boolean mIs24HourFormat;
    private final LockscreenStyleCoverController mLockscreenStyleCoverController;
    private String mNextAlarmText;
    private SecondHand mSecondHand;
    private LockscreenStyleCoverControllerCallback mStyleCoverCallback;
    private final ContentObserver mTimeSettingsChangeObserver;

    public SonyClockLoops(Context context) {
        this(context, null, 0);
    }

    public SonyClockLoops(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SonyClockLoops(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = null;
        this.mClockView = null;
        this.mClockHour = null;
        this.mClockSeparator = null;
        this.mClockMinute = null;
        this.mAmPm = null;
        this.mDate = null;
        this.mAlarm = null;
        this.mNextAlarmText = null;
        this.mSecondHand = null;
        this.mIs24HourFormat = false;
        this.mLockscreenStyleCoverController = (LockscreenStyleCoverController) Dependency.get(LockscreenStyleCoverController.class);
        this.mTimeSettingsChangeObserver = new ContentObserver(new Handler()) {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SonyClockLoops.AnonymousClass1 */

            public void onChange(boolean z) {
                onChange(z, null);
            }

            public void onChange(boolean z, Uri uri) {
                SonyClockLoops.this.update24HourFormat();
            }
        };
        this.mStyleCoverCallback = new LockscreenStyleCoverControllerCallback() {
            /* class com.sonymobile.keyguard.plugin.sonyclockloops.SonyClockLoops.AnonymousClass2 */

            @Override // com.sonymobile.systemui.lockscreen.LockscreenStyleCoverControllerCallback
            public void onStyleCoverClosed(boolean z) {
                SonyClockLoops.this.resizeClock(z);
            }
        };
        this.mContext = context;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resizeClock(boolean z) {
        float f;
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(C0005R$dimen.sony_clock_loops_sencond_hand_width);
        if (z) {
            f = this.mContext.getResources().getFloat(C0008R$integer.sony_clock_loops_style_cover_view_scale);
            dimensionPixelSize = (int) (((float) dimensionPixelSize) * f);
        } else {
            f = 1.0f;
        }
        ViewGroup.LayoutParams layoutParams = this.mSecondHand.getLayoutParams();
        layoutParams.height = dimensionPixelSize;
        layoutParams.width = dimensionPixelSize;
        this.mSecondHand.setLayoutParams(layoutParams);
        LinearLayout linearLayout = this.mClockView;
        if (linearLayout != null) {
            linearLayout.setScaleX(f);
            this.mClockView.setScaleY(f);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mLockscreenStyleCoverController.registerCallback(this.mStyleCoverCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mLockscreenStyleCoverController.removeCallback(this.mStyleCoverCallback);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void update24HourFormat() {
        this.mIs24HourFormat = DateFormat.is24HourFormat(getContext(), -2);
    }

    private void registerTimeSettingsChangeObserver() {
        getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mTimeSettingsChangeObserver);
    }

    private void unregisterTimeSettingsChangeObserver() {
        getContext().getContentResolver().unregisterContentObserver(this.mTimeSettingsChangeObserver);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        fetchViewHandles();
        updateThemeColors();
    }

    private void fetchViewHandles() {
        this.mClockView = (LinearLayout) findViewById(C0007R$id.somc_sony_clock_loops_clock_view);
        this.mClockHour = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_clock_hour);
        this.mClockSeparator = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_clock_separator);
        this.mClockMinute = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_clock_minute);
        this.mAmPm = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_ampm);
        this.mDate = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_date);
        this.mAlarm = (TextView) findViewById(C0007R$id.somc_sony_clock_loops_alarm);
        this.mSecondHand = (SecondHand) findViewById(C0007R$id.somc_sony_clock_loops_second_hand);
    }

    public void refresh(Calendar calendar) {
        Patterns.update(this.mContext);
        if (calendar == null) {
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
        }
        refreshTime(calendar);
        refreshAmPmDate(calendar);
        refreshAlarmStatus();
    }

    private void refreshTime(Calendar calendar) {
        CharSequence charSequence;
        if (this.mIs24HourFormat) {
            charSequence = DateFormat.format(Patterns.clockHourView24, calendar);
        } else {
            charSequence = DateFormat.format(Patterns.clockHourView12, calendar);
        }
        CharSequence format = DateFormat.format(Patterns.clockMinuteView, calendar);
        CharSequence format2 = DateFormat.format(this.mIs24HourFormat ? Patterns.clockSeparator24 : Patterns.clockSeparator12, calendar);
        if (!this.mClockHour.getText().equals(charSequence)) {
            this.mClockHour.setText(charSequence);
        }
        if (!this.mClockMinute.getText().equals(format)) {
            this.mClockMinute.setText(format);
        }
        if (!this.mClockSeparator.getText().equals(format2)) {
            this.mClockSeparator.setText(format2);
        }
        this.mClockHour.setElegantTextHeight(false);
        this.mClockMinute.setElegantTextHeight(false);
        this.mClockSeparator.setElegantTextHeight(false);
    }

    private void refreshAmPmDate(Calendar calendar) {
        int i = 8;
        if (this.mIs24HourFormat) {
            this.mAmPm.setVisibility(8);
        } else {
            CharSequence format = DateFormat.format("a", calendar);
            if (!this.mAmPm.getText().equals(format)) {
                this.mAmPm.setText(format);
            }
            if (this.mAmPm.length() <= getResources().getInteger(C0008R$integer.somc_digital_clock_max_ampm_chars)) {
                i = 0;
            }
            this.mAmPm.setVisibility(i);
        }
        CharSequence format2 = DateFormat.format(Patterns.dateView, calendar);
        if (!this.mDate.getText().equals(format2)) {
            this.mDate.setText(format2);
        }
        this.mDate.setVisibility(0);
    }

    private void refreshAlarmStatus() {
        String str = this.mNextAlarmText;
        if (str == null || str.length() <= 0) {
            this.mAlarm.setVisibility(4);
            return;
        }
        if (!this.mAlarm.getText().equals(this.mNextAlarmText)) {
            this.mAlarm.setText(this.mNextAlarmText);
        }
        this.mAlarm.setContentDescription(getResources().getString(C0014R$string.keyguard_accessibility_next_alarm, this.mNextAlarmText));
        this.mAlarm.setVisibility(0);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setNextAlarmText(String str) {
        this.mNextAlarmText = str;
        refreshAlarmStatus();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void startClockTicking() {
        update24HourFormat();
        this.mSecondHand.startClockTicking(this);
        registerTimeSettingsChangeObserver();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void stopClockTicking() {
        this.mSecondHand.stopClockTicking();
        unregisterTimeSettingsChangeObserver();
    }

    public void setPicker(boolean z) {
        this.mSecondHand.setPicker(z);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        if (this.mLockscreenStyleCoverController.isStyleCoverViewSelectedAndClosed()) {
            resizeClock(true);
        }
        this.mSecondHand.setDoze();
        this.mClockHour.setTextColor(-1);
        this.mClockMinute.setTextColor(-1);
        this.mClockSeparator.setTextColor(-1);
        this.mAmPm.setTextColor(-1);
        this.mDate.setTextColor(-1);
        this.mAlarm.setTextColor(-1);
        this.mAlarm.setCompoundDrawableTintList(ColorStateList.valueOf(-1));
        this.mAlarm.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        invalidate();
    }

    private void updateThemeColors() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            this.mClockHour.setTextColor(color);
            this.mClockMinute.setTextColor(color);
            this.mClockSeparator.setTextColor(color);
            this.mAmPm.setTextColor(color);
            this.mDate.setTextColor(color);
            this.mAlarm.setTextColor(color);
            this.mAlarm.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            this.mAlarm.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        refresh(null);
    }

    /* access modifiers changed from: private */
    public static final class Patterns {
        static String cacheKey;
        static String clockHourView12;
        static String clockHourView24;
        static String clockMinuteView;
        static String clockSeparator12;
        static String clockSeparator24;
        static String clockView12;
        static String clockView24;
        static String dateView;

        static void update(Context context) {
            Locale locale = Locale.getDefault();
            Resources resources = context.getResources();
            String string = resources.getString(C0014R$string.abbrev_wday_month_day_no_year);
            String string2 = resources.getString(C0014R$string.clock_12hr_format);
            String string3 = resources.getString(C0014R$string.clock_24hr_format);
            String str = locale.toString() + string + string2 + string3;
            if (!str.equals(cacheKey)) {
                dateView = DateFormat.getBestDateTimePattern(locale, string);
                clockView12 = DateFormat.getBestDateTimePattern(locale, string2);
                if (!string2.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, string3);
                clockSeparator12 = selectSeparator(clockView12);
                clockSeparator24 = selectSeparator(clockView24);
                clockHourView12 = clockView12.replaceAll("'.'", "").replaceAll("[^hHkK]", "").trim();
                clockHourView24 = clockView24.replaceAll("'.'", "").replaceAll("[^hHkK]", "").trim();
                clockMinuteView = clockView12.replaceAll("[^m]", "").trim();
                cacheKey = str;
            }
        }

        private static String selectSeparator(String str) {
            if (str.contains(":") || str.contains("'h'")) {
                return ":";
            }
            if (str.contains("'.'")) {
                return "'.'";
            }
            return str.replaceAll("[a-zA-Z]", "").trim();
        }
    }
}
