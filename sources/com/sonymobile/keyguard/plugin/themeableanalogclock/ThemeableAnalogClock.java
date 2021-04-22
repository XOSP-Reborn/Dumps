package com.sonymobile.keyguard.plugin.themeableanalogclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Calendar;
import java.util.TimeZone;

public class ThemeableAnalogClock extends FrameLayout implements ClockPlugin {
    private static final String TAG = "ThemeableAnalogClock";
    private final Handler mHandler;
    private ClockHand mHoursHand;
    private final BroadcastReceiver mIntentReceiver;
    private ClockHand mMinutesHand;
    private final Canvas mOffscreenCanvas;
    private ClockHand mSecondsHand;
    private final Runnable mTicker;
    private boolean mTicking;
    private Calendar mTime;

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setNextAlarmText(String str) {
    }

    public ThemeableAnalogClock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ThemeableAnalogClock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTicker = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.themeableanalogclock.ThemeableAnalogClock.AnonymousClass1 */

            public void run() {
                ThemeableAnalogClock.this.mHandler.removeCallbacks(ThemeableAnalogClock.this.mTicker);
                long uptimeMillis = SystemClock.uptimeMillis();
                ThemeableAnalogClock.this.onTimeChanged();
                ThemeableAnalogClock.this.mHandler.postAtTime(ThemeableAnalogClock.this.mTicker, uptimeMillis + (1000 - (uptimeMillis % 1000)));
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            /* class com.sonymobile.keyguard.plugin.themeableanalogclock.ThemeableAnalogClock.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    ThemeableAnalogClock.this.setTimeZone(intent.getStringExtra("time-zone"));
                }
                ThemeableAnalogClock.this.onTimeChanged();
            }
        };
        this.mOffscreenCanvas = new Canvas();
        this.mHandler = new Handler();
        this.mTicking = false;
        createTime(null);
    }

    private void setTimeZone() {
        setTimeZone(null);
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

    private void createTime(String str) {
        if (str != null) {
            this.mTime = Calendar.getInstance(TimeZone.getTimeZone(str));
        } else {
            this.mTime = Calendar.getInstance();
        }
    }

    /* access modifiers changed from: protected */
    public final void onFinishInflate() {
        super.onFinishInflate();
        setWillNotDraw(false);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ClockHand) {
                storeClockHand((ClockHand) childAt);
            }
        }
        updateThemeResources();
    }

    private void storeClockHand(ClockHand clockHand) {
        int handType = clockHand.getHandType();
        if (handType == 0) {
            this.mSecondsHand = clockHand;
        } else if (handType == 1) {
            this.mMinutesHand = clockHand;
        } else if (handType != 2) {
            String str = TAG;
            Log.w(str, "Unhandled clock hand type:" + clockHand);
        } else {
            this.mHoursHand = clockHand;
        }
    }

    private void update() {
        int i = this.mTime.get(13);
        int i2 = this.mTime.get(12);
        int i3 = this.mTime.get(10);
        updateHandsRotation(this.mSecondsHand, (float) (i * 6));
        updateHandsRotation(this.mMinutesHand, (float) (i2 * 6));
        updateHandsRotation(this.mHoursHand, ((float) (i3 * 30)) + (((float) i2) * 0.5f));
    }

    private void updateHandsRotation(ClockHand clockHand, float f) {
        clockHand.setRotation(f);
    }

    public final void onTimeChanged() {
        this.mTime.setTimeInMillis(System.currentTimeMillis());
        update();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void startClockTicking() {
        if (!this.mTicking) {
            this.mTicking = true;
            setTimeZone();
            registerReceiver();
            this.mTicker.run();
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public final void stopClockTicking() {
        if (this.mTicking) {
            this.mHandler.removeCallbacks(this.mTicker);
            unregisterReceiver();
            this.mTicking = false;
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, getHandler());
    }

    private void unregisterReceiver() {
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    private void updateThemeResources() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            Drawable drawable = resources.getDrawable(C0006R$drawable.somc_themeable_analog_clock_face, null);
            String string = resources.getString(C0014R$string.somc_keyguard_themeable_analog_clock_name);
            Drawable drawable2 = resources.getDrawable(C0006R$drawable.somc_themeable_analog_clock_hour_hand, null);
            float dimension = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_hour_hand_pivot_x);
            float dimension2 = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_hour_hand_pivot_y);
            Drawable drawable3 = resources.getDrawable(C0006R$drawable.somc_themeable_analog_clock_minute_hand, null);
            float dimension3 = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_minute_hand_pivot_x);
            float dimension4 = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_minute_hand_pivot_y);
            Drawable drawable4 = resources.getDrawable(C0006R$drawable.somc_themeable_analog_clock_second_hand, null);
            float dimension5 = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_second_hand_pivot_x);
            float dimension6 = resources.getDimension(C0005R$dimen.somc_themeable_analog_clock_second_hand_pivot_y);
            ImageView clockFaceView = getClockFaceView();
            if (clockFaceView != null) {
                clockFaceView.setImageDrawable(drawable);
                clockFaceView.setContentDescription(string);
            }
            this.mHoursHand.updateThemeResources(drawable2, dimension, dimension2, string);
            this.mMinutesHand.updateThemeResources(drawable3, dimension3, dimension4, string);
            this.mSecondsHand.updateThemeResources(drawable4, dimension5, dimension6, string);
        }
    }

    private ImageView getClockFaceView() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ImageView) {
                return (ImageView) childAt;
            }
        }
        return null;
    }
}
