package com.sonymobile.keyguard.plugin.docomoclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import com.android.internal.os.BackgroundThread;
import com.android.keyguard.MachiCharaWidget;
import com.android.settingslib.Utils;
import com.android.systemui.C0002R$attr;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0006R$drawable;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0008R$integer;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.sonymobile.keyguard.SomcKeyguardClockScaleContainer;
import com.sonymobile.keyguard.plugininfrastructure.ClockPlugin;
import com.sonymobile.systemui.lockscreen.theme.LockscreenSkinningController;
import java.util.Locale;

public class DocomoClockContainer extends SomcKeyguardClockScaleContainer implements ClockPlugin, View.OnClickListener {
    private static BroadcastReceiver mLockscreenMascotReceiver;
    private ActivityStarter mActivityStarter;
    private TextClock mAmPm;
    private boolean mBootCompleted;
    private MachiCharaWidget mCharalayout;
    private TextClock mClockView;
    private TextClock mDateView;
    private Handler mHandler;
    private boolean mIsTicking;
    private ImageView mMicImageView;
    private BroadcastReceiver mTimeEventReceiver;
    private ContentObserver mTimeSettingsChangeObserver;
    private final Runnable mUpdateMicIconRunnable;

    public DocomoClockContainer(Context context) {
        this(context, null, 0, 0);
    }

    public DocomoClockContainer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0, 0);
    }

    public DocomoClockContainer(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DocomoClockContainer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTimeSettingsChangeObserver = null;
        this.mTimeEventReceiver = null;
        this.mBootCompleted = false;
        this.mIsTicking = false;
        this.mUpdateMicIconRunnable = new Runnable() {
            /* class com.sonymobile.keyguard.plugin.docomoclock.DocomoClockContainer.AnonymousClass1 */

            public void run() {
                DocomoClockContainer.this.updateMicImage();
            }
        };
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mClockView = (TextClock) findViewById(C0007R$id.docomo_clock_view);
        this.mDateView = (TextClock) findViewById(C0007R$id.docomo_date_view);
        this.mAmPm = (TextClock) findViewById(C0007R$id.docomo_digital_clock_am_pm);
        this.mClockView.setElegantTextHeight(false);
        this.mMicImageView = (ImageView) findViewById(C0007R$id.mic_button);
        if ((Utils.getColorAttrDefaultColor(getContext(), C0002R$attr.wallpaperTextColor) & 16777215) == 16777215) {
            this.mMicImageView.setBackgroundDrawable(getResources().getDrawable(C0006R$drawable.docomo_clock_ic_d_mic_lock));
        } else {
            this.mMicImageView.setBackgroundDrawable(getResources().getDrawable(C0006R$drawable.docomo_clock_ic_d_mic_lock_inverse));
        }
        this.mMicImageView.setOnClickListener(this);
        updateThemeColors();
        this.mCharalayout = (MachiCharaWidget) findViewById(C0007R$id.chara);
        this.mHandler = new Handler();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        unregisterLockscreenMascotReceiver();
        registerLockscreenMascotReceiver();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void startClockTicking() {
        if (!isMascotApplication()) {
            MachiCharaWidget machiCharaWidget = this.mCharalayout;
            if (machiCharaWidget != null) {
                machiCharaWidget.setVisibility(8);
            }
            ImageView imageView = this.mMicImageView;
            if (imageView != null) {
                imageView.setVisibility(8);
            }
        }
        if (!this.mIsTicking) {
            this.mIsTicking = true;
            registerTimeSettingsChangeObserver();
            registerTimeEventReceiver();
        }
        refreshTime();
        this.mBootCompleted = SystemProperties.getBoolean("sys.boot_completed", false);
        if (this.mBootCompleted && isMascotApplication()) {
            BackgroundThread.getHandler().post(new Runnable() {
                /* class com.sonymobile.keyguard.plugin.docomoclock.DocomoClockContainer.AnonymousClass2 */

                public void run() {
                    DocomoClockContainer.this.requestMachiCharaViewToMascotApp();
                }
            });
        }
        MachiCharaWidget machiCharaWidget2 = this.mCharalayout;
        if (machiCharaWidget2 != null) {
            machiCharaWidget2.mUpdateViewFlg = false;
        }
        this.mHandler.removeCallbacks(this.mUpdateMicIconRunnable);
        this.mHandler.postDelayed(this.mUpdateMicIconRunnable, 1000);
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void stopClockTicking() {
        if (this.mIsTicking) {
            this.mIsTicking = false;
            unregisterTimeSettingsChangeObserver();
            unregisterTimeEventReceiver();
        }
        this.mHandler.removeCallbacks(this.mUpdateMicIconRunnable);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void requestMachiCharaViewToMascotApp() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException unused) {
            Log.w("DocomoLockScreen", "DocomoClockContainer.requestMachiCharaViewToMascotApp():InterruptedException");
        }
        try {
            ((FrameLayout) this).mContext.sendBroadcast(new Intent("com.android.internal.policy.impl.keyguard.ACTION_SCREEN_DISPLAY"), "com.nttdocomo.android.screenlockservice.DCM_SCREEN");
        } catch (IllegalStateException unused2) {
            Log.w("DocomoLockScreen", "DocomoClockContainer.requestMachiCharaViewToMascotApp():IllegalStateException");
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setNextAlarmText(String str) {
        refreshTime();
    }

    public void onClick(View view) {
        if (view == this.mMicImageView && isMascotApplication()) {
            launchMascotApp(0, "LOCK_CLICK_MASCOT");
        }
    }

    private void registerLockscreenMascotReceiver() {
        if (mLockscreenMascotReceiver == null) {
            mLockscreenMascotReceiver = new BroadcastReceiver() {
                /* class com.sonymobile.keyguard.plugin.docomoclock.DocomoClockContainer.AnonymousClass3 */

                public void onReceive(Context context, Intent intent) {
                    String str;
                    String action = intent.getAction();
                    int intExtra = intent.getIntExtra("eventType", 0);
                    Log.i("DocomoLockScreen", "DocomoClockContainer.onReceive(): eventType = " + intExtra);
                    if ("com.nttdocomo.android.mascot.widget.LockScreenMascotWidget.ACTION_SCREEN_UNLOCK".equals(action)) {
                        Log.i("DocomoLockScreen", "DocomoClockContainer.onReceive(): ACTION_SCREEN_UNLOCK");
                        if (intExtra != 1) {
                            if (intExtra != 2) {
                                if (intExtra == 3) {
                                    str = "ACTION_UNLOCK";
                                } else if (intExtra != 4) {
                                    str = null;
                                }
                            }
                            str = "LOCK_CLICK_POPUP";
                        } else {
                            str = "LOCK_CLICK_MASCOT";
                        }
                        Log.i("DocomoLockScreen", "DocomoClockContainer.onReceive(): mascotAction = " + str);
                        if (str != null) {
                            DocomoClockContainer.this.launchMascotApp(intExtra, str);
                        }
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.nttdocomo.android.mascot.widget.LockScreenMascotWidget.ACTION_SCREEN_UNLOCK");
            ((FrameLayout) this).mContext.registerReceiver(mLockscreenMascotReceiver, intentFilter);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void launchMascotApp(int i, String str) {
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setAction(str);
        intent.setClassName("com.nttdocomo.android.mascot", "com.nttdocomo.android.mascot.application.MascotApplicationProxy");
        intent.setFlags(270532608);
        if (i != 0) {
            intent.putExtra("eventType", i);
        }
        this.mActivityStarter.startActivity(intent, true);
    }

    private void unregisterLockscreenMascotReceiver() {
        try {
            if (mLockscreenMascotReceiver != null) {
                ((FrameLayout) this).mContext.unregisterReceiver(mLockscreenMascotReceiver);
                mLockscreenMascotReceiver = null;
                if (DocomoClockLog.DEBUG) {
                    Log.d("DocomoLockScreen", "DocomoClockContainer.unregisterLockscreenMascotReceiver(): unregist");
                }
            }
        } catch (Exception e) {
            Log.w("DocomoLockScreen", "DocomoClockContainer.unregisterLockscreenMascotReceiver(): exception:" + e.toString());
        }
    }

    private void registerTimeSettingsChangeObserver() {
        if (this.mTimeSettingsChangeObserver == null) {
            this.mTimeSettingsChangeObserver = new ContentObserver(new Handler()) {
                /* class com.sonymobile.keyguard.plugin.docomoclock.DocomoClockContainer.AnonymousClass4 */

                public void onChange(boolean z) {
                    onChange(z, null);
                }

                public void onChange(boolean z, Uri uri) {
                    DocomoClockContainer.this.refreshTime();
                }
            };
            getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, this.mTimeSettingsChangeObserver);
        }
    }

    private void unregisterTimeSettingsChangeObserver() {
        if (this.mTimeSettingsChangeObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(this.mTimeSettingsChangeObserver);
            this.mTimeSettingsChangeObserver = null;
        }
    }

    private void registerTimeEventReceiver() {
        if (this.mTimeEventReceiver == null) {
            this.mTimeEventReceiver = new BroadcastReceiver() {
                /* class com.sonymobile.keyguard.plugin.docomoclock.DocomoClockContainer.AnonymousClass5 */

                public void onReceive(Context context, Intent intent) {
                    DocomoClockContainer.this.refreshTime();
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiver(this.mTimeEventReceiver, intentFilter, null, getHandler());
        }
    }

    private void unregisterTimeEventReceiver() {
        if (this.mTimeEventReceiver != null) {
            getContext().unregisterReceiver(this.mTimeEventReceiver);
            this.mTimeEventReceiver = null;
        }
    }

    public void refreshTime() {
        Patterns.update(getContext(), false);
        this.mDateView.setFormat24Hour(Patterns.dateView);
        this.mDateView.setFormat12Hour(Patterns.dateView);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
        this.mClockView.setTextSize(0, (float) getResources().getDimensionPixelSize(C0005R$dimen.docomo_widget_big_font_size));
        this.mDateView.setTextSize(0, (float) getResources().getDimensionPixelSize(C0005R$dimen.docomo_widget_label_font_size));
        this.mAmPm.setTextSize(0, (float) getResources().getDimensionPixelSize(C0005R$dimen.widget_label_font_size));
        updateAmPmVisibility();
    }

    private void updateAmPmVisibility() {
        int i = 8;
        if (DateFormat.is24HourFormat(getContext())) {
            this.mAmPm.setVisibility(8);
            return;
        }
        if (this.mAmPm.length() <= getResources().getInteger(C0008R$integer.somc_digital_clock_max_ampm_chars)) {
            i = 0;
        }
        this.mAmPm.setVisibility(i);
        this.mAmPm.setFormat12Hour("a");
    }

    /* access modifiers changed from: private */
    public static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;
        static String dateView;

        static void update(Context context, boolean z) {
            Locale locale = Locale.getDefault();
            Resources resources = context.getResources();
            String string = resources.getString(C0014R$string.abbrev_wday_month_day_no_year);
            String string2 = resources.getString(C0014R$string.clock_12hr_format);
            String string3 = resources.getString(C0014R$string.clock_24hr_format);
            String str = locale.toString() + string + string2 + string3;
            if (!str.equals(cacheKey)) {
                dateView = DateFormat.getBestDateTimePattern(locale, string);
                cacheKey = str;
                clockView12 = DateFormat.getBestDateTimePattern(locale, string2);
                if (!string2.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, string3);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateMicImage() {
        MachiCharaWidget machiCharaWidget = this.mCharalayout;
        if (machiCharaWidget != null && !machiCharaWidget.mUpdateViewFlg) {
            machiCharaWidget.setVisibility(8);
            if (this.mMicImageView == null) {
                return;
            }
            if (!isMascotApplication()) {
                this.mMicImageView.setVisibility(8);
            } else if (this.mMicImageView.getVisibility() == 8) {
                this.mMicImageView.setVisibility(0);
            }
        }
    }

    private boolean isMascotApplication() {
        try {
            return ((FrameLayout) this).mContext.getPackageManager().getApplicationInfo("com.nttdocomo.android.mascot", 128).enabled;
        } catch (Exception unused) {
            return false;
        }
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void setDoze() {
        this.mClockView.setTextColor(-1);
        this.mDateView.setTextColor(-1);
        this.mAmPm.setTextColor(-1);
        this.mHandler.removeCallbacks(this.mUpdateMicIconRunnable);
        this.mCharalayout.setDoze();
        this.mMicImageView.setBackgroundDrawable(getResources().getDrawable(C0006R$drawable.docomo_clock_ic_d_mic_lock));
        invalidate();
    }

    @Override // com.sonymobile.keyguard.plugininfrastructure.ClockPlugin
    public void dozeTimeTick() {
        refreshTime();
    }

    private void updateThemeColors() {
        Resources resources = ((LockscreenSkinningController) Dependency.get(LockscreenSkinningController.class)).getResources();
        if (resources != null) {
            int color = resources.getColor(C0004R$color.somc_keyguard_theme_color_solid_foreground, null);
            if ((color & 16777215) == 16777215) {
                this.mMicImageView.setBackgroundDrawable(getResources().getDrawable(C0006R$drawable.docomo_clock_ic_d_mic_lock));
            } else {
                this.mMicImageView.setBackgroundDrawable(getResources().getDrawable(C0006R$drawable.docomo_clock_ic_d_mic_lock_inverse));
            }
            this.mClockView.setTextColor(color);
            this.mDateView.setTextColor(color);
            this.mAmPm.setTextColor(color);
        }
    }
}
