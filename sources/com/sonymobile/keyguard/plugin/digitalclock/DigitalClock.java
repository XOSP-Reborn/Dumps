package com.sonymobile.keyguard.plugin.digitalclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Locale;

public class DigitalClock extends LinearLayout implements ClockPlugin {
    private TextView mAlarmStatusView;
    private TextClock mClockView;
    private Context mContext;
    private TextClock mDate;
    private TextClock mDigitalClockAmPm;
    private String mNextAlarmText;

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void stopClockTicking() {
    }

    public DigitalClock(Context context) {
        this(context, null, 0);
    }

    public DigitalClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DigitalClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = null;
        this.mClockView = null;
        this.mDigitalClockAmPm = null;
        this.mDate = null;
        this.mAlarmStatusView = null;
        this.mNextAlarmText = null;
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        super.onFinishInflate();
        fetchViewHandles();
        updateThemeColors();
    }

    private void fetchViewHandles() {
        this.mClockView = (TextClock) findViewById(C0007R$id.somc_digital_clock_view_clock);
        this.mDigitalClockAmPm = (TextClock) findViewById(C0007R$id.somc_digital_clock_view_am_pm);
        TextClock textClock = this.mDigitalClockAmPm;
        if (textClock != null) {
            textClock.setShowCurrentUserTime(true);
        }
        this.mDate = (TextClock) findViewById(C0007R$id.somc_digital_clock_date);
        this.mAlarmStatusView = (TextView) findViewById(C0007R$id.somc_digital_clock_alarm);
        ((SomcKeyguardClockScaleContainer) findViewById(C0007R$id.somc_keyguard_clockplugin_scale_container)).setMinScaleForComputeNotifications(0.0f);
        refresh();
    }

    private void refresh() {
        Patterns.update(this.mContext);
        refreshTime();
        refreshAlarmStatus();
    }

    private void refreshTime() {
        this.mClockView.setShowCurrentUserTime(true);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
        this.mClockView.setElegantTextHeight(false);
        this.mDate.setFormat24Hour(Patterns.dateView);
        this.mDate.setFormat12Hour(Patterns.dateView);
        this.mDate.setShowCurrentUserTime(true);
        this.mDate.setVisibility(0);
    }

    private void updateAmPmVisibility() {
        int i = 0;
        this.mDigitalClockAmPm.setVisibility(0);
        if (this.mDigitalClockAmPm.is24HourModeEnabled()) {
            this.mDigitalClockAmPm.setVisibility(8);
            return;
        }
        this.mDigitalClockAmPm.setFormat12Hour("a");
        if (this.mDigitalClockAmPm.length() > getResources().getInteger(C0008R$integer.somc_digital_clock_max_ampm_chars)) {
            i = 8;
        }
        this.mDigitalClockAmPm.setVisibility(i);
    }

    private void refreshAlarmStatus() {
        String str = this.mNextAlarmText;
        if (str == null || str.length() <= 0) {
            this.mAlarmStatusView.setVisibility(8);
            return;
        }
        this.mAlarmStatusView.setText(this.mNextAlarmText);
        this.mAlarmStatusView.setContentDescription(getResources().getString(C0014R$string.keyguard_accessibility_next_alarm, this.mNextAlarmText));
        this.mAlarmStatusView.setVisibility(0);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void setNextAlarmText(String str) {
        this.mNextAlarmText = str;
        refreshAlarmStatus();
        updateAmPmVisibility();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void startClockTicking() {
        refresh();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        this.mClockView.setTextColor(-1);
        this.mDigitalClockAmPm.setTextColor(-1);
        this.mDate.setTextColor(-1);
        this.mAlarmStatusView.setTextColor(-1);
        this.mAlarmStatusView.setCompoundDrawableTintList(ColorStateList.valueOf(-1));
        this.mAlarmStatusView.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        invalidate();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        refresh();
    }

    private void updateThemeColors() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            this.mClockView.setTextColor(color);
            this.mDigitalClockAmPm.setTextColor(color);
            this.mDate.setTextColor(color);
            this.mAlarmStatusView.setTextColor(color);
            this.mAlarmStatusView.setCompoundDrawableTintList(ColorStateList.valueOf(color));
            this.mAlarmStatusView.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
        }
    }

    /* access modifiers changed from: private */
    public static final class Patterns {
        static String cacheKey;
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
                cacheKey = str;
            }
        }
    }
}
