package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IActivityManager;
import android.app.IStopUserCallback;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardPluginFactoryLoader;
import com.sonymobile.keyguard.plugininfrastructure.KeyguardStatusViewHelper;
import com.sonymobile.systemui.lockscreen.LockscreenAssistIconController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.TimeZone;

public class KeyguardStatusView extends GridLayout implements ConfigurationController.ConfigurationListener {
    private AlarmManager mAlarmManager;
    private ImageView mAssistIcon;
    private int mBottomMargin;
    private int mBottomMarginWithHeader;
    private ViewGroup mClockPluginView;
    private KeyguardClockSwitch mClockView;
    private float mDarkAmount;
    private ViewGroup mDigitalClockContainer;
    private Handler mHandler;
    private final IActivityManager mIActivityManager;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private boolean mIsSkinningEnabled;
    private KeyguardPluginFactoryLoader mKeyguardPluginFactoryLoader;
    private KeyguardSliceView mKeyguardSlice;
    private LinearLayout mLinearClockLayout;
    private final LockPatternUtils mLockPatternUtils;
    private TextView mLogoutView;
    private TextView mOwnerInfo;
    private Runnable mPendingMarqueeStart;
    private boolean mPulsing;
    private boolean mShowingHeader;
    private LinearLayout mStatusViewContainer;
    private int mTextColor;
    private final UserSwitchState mUserSwitchState;

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDarkAmount = 0.0f;
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            /* class com.android.keyguard.KeyguardStatusView.AnonymousClass1 */

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onTimeChanged() {
                KeyguardStatusView.this.refreshTime();
                KeyguardStatusView.this.refreshFormat();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onTimeZoneChanged(TimeZone timeZone) {
                KeyguardStatusView.this.updateTimeZone(timeZone);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onKeyguardVisibilityChanged(boolean z) {
                if (z) {
                    KeyguardStatusView.this.refreshTime();
                    KeyguardStatusView.this.updateOwnerInfo();
                    KeyguardStatusView.this.updateLogoutView();
                    if (KeyguardUpdateMonitor.getInstance(KeyguardStatusView.this.getContext().getApplicationContext()).isDeviceInteractive()) {
                        KeyguardStatusView.this.startClockTicking();
                        return;
                    }
                    return;
                }
                KeyguardStatusView.this.stopClockTicking();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onStartedWakingUp() {
                KeyguardStatusView.this.setEnableMarquee(true);
                KeyguardStatusView.this.startClockTicking();
                if (KeyguardStatusView.this.mKeyguardPluginFactoryLoader != null && "com.sonymobile.keyguard.plugin.themeableanalogclock.ThemeableAnalogClockPluginFactory".equals(KeyguardStatusView.this.mKeyguardPluginFactoryLoader.getActiveFullPluginClassName())) {
                    KeyguardStatusView.this.loadClockPluginView(true);
                }
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onFinishedGoingToSleep(int i) {
                KeyguardStatusView.this.setEnableMarquee(false);
                KeyguardStatusView.this.loadClockPluginView();
                KeyguardStatusView.this.stopClockTicking();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserSwitching(int i) {
                KeyguardStatusView.this.mUserSwitchState.startUserSwitch();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserSwitchComplete(int i) {
                KeyguardStatusView.this.refreshFormat();
                KeyguardStatusView.this.updateOwnerInfo();
                KeyguardStatusView.this.updateLogoutView();
                KeyguardStatusView.this.loadClockForUser(i);
                ((LockscreenAssistIconController) Dependency.get(LockscreenAssistIconController.class)).setAssistIconView(KeyguardStatusView.this.mAssistIcon);
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onLogoutEnabledChanged() {
                KeyguardStatusView.this.updateLogoutView();
            }

            @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
            public void onUserClockChanged() {
                if (KeyguardUpdateMonitor.getInstance(((GridLayout) KeyguardStatusView.this).mContext).isKeyguardVisible()) {
                    KeyguardStatusView.this.loadClockPluginView();
                }
            }
        };
        this.mUserSwitchState = new UserSwitchState();
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mIActivityManager = ActivityManager.getService();
        this.mLockPatternUtils = new LockPatternUtils(getContext());
        this.mHandler = new Handler(Looper.myLooper());
        onDensityOrFontScaleChanged();
        this.mIsSkinningEnabled = context.getResources().getBoolean(R$bool.somc_keyguard_theme_enabled);
    }

    public boolean hasCustomClock() {
        return this.mClockView.hasCustomClock();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setEnableMarquee(boolean z) {
        if (!z) {
            Runnable runnable = this.mPendingMarqueeStart;
            if (runnable != null) {
                this.mHandler.removeCallbacks(runnable);
                this.mPendingMarqueeStart = null;
            }
            setEnableMarqueeImpl(false);
        } else if (this.mPendingMarqueeStart == null) {
            this.mPendingMarqueeStart = new Runnable() {
                /* class com.android.keyguard.$$Lambda$KeyguardStatusView$ps9yj97ShIVR2u2hJB8SKuKkkQ */

                public final void run() {
                    KeyguardStatusView.this.lambda$setEnableMarquee$0$KeyguardStatusView();
                }
            };
            this.mHandler.postDelayed(this.mPendingMarqueeStart, 2000);
        }
    }

    public /* synthetic */ void lambda$setEnableMarquee$0$KeyguardStatusView() {
        setEnableMarqueeImpl(true);
        this.mPendingMarqueeStart = null;
    }

    private void setEnableMarqueeImpl(boolean z) {
        TextView textView = this.mOwnerInfo;
        if (textView != null) {
            textView.setSelected(z);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mStatusViewContainer = (LinearLayout) findViewById(R$id.status_view_container);
        this.mLogoutView = (TextView) findViewById(R$id.logout);
        TextView textView = this.mLogoutView;
        if (textView != null) {
            textView.setOnClickListener(new View.OnClickListener() {
                /* class com.android.keyguard.$$Lambda$KeyguardStatusView$Pryio69yVoRI9F153p5QiMZebw */

                public final void onClick(View view) {
                    KeyguardStatusView.m1lambda$Pryio69yVoRI9F153p5QiMZebw(KeyguardStatusView.this, view);
                }
            });
        }
        this.mClockView = (KeyguardClockSwitch) findViewById(R$id.keyguard_clock_container);
        this.mClockView.setShowCurrentUserTime(true);
        if (KeyguardClockAccessibilityDelegate.isNeeded(((GridLayout) this).mContext)) {
            this.mClockView.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(((GridLayout) this).mContext));
        }
        this.mAssistIcon = (ImageView) findViewById(R$id.assist_icon);
        this.mOwnerInfo = (TextView) findViewById(R$id.owner_info);
        this.mKeyguardSlice = (KeyguardSliceView) findViewById(R$id.keyguard_status_area);
        this.mTextColor = this.mClockView.getCurrentTextColor();
        this.mKeyguardSlice.setContentChangeListener(new Runnable() {
            /* class com.android.keyguard.$$Lambda$KeyguardStatusView$Xo7rGDTjuOiD9nJpe80IUZ1ddFw */

            public final void run() {
                KeyguardStatusView.lambda$Xo7rGDTjuOiD9nJpe80IUZ1ddFw(KeyguardStatusView.this);
            }
        });
        onSliceContentChanged();
        setEnableMarquee(KeyguardUpdateMonitor.getInstance(((GridLayout) this).mContext).isDeviceInteractive());
        this.mKeyguardPluginFactoryLoader = KeyguardStatusViewHelper.createKeyguardPluginFactoryForUser(KeyguardUpdateMonitor.getCurrentUser(), ((GridLayout) this).mContext.getApplicationContext());
        this.mLinearClockLayout = (LinearLayout) findViewById(R$id.somc_keyguard_plugin_clock_super_container);
        this.mDigitalClockContainer = this.mClockView;
        loadClockPluginView();
        refreshFormat();
        updateOwnerInfo();
        updateLogoutView();
        updateDark();
    }

    /* access modifiers changed from: private */
    public void onSliceContentChanged() {
        boolean hasHeader = this.mKeyguardSlice.hasHeader();
        this.mClockView.setKeyguardShowingHeader(hasHeader);
        if (this.mShowingHeader != hasHeader) {
            this.mShowingHeader = hasHeader;
            LinearLayout linearLayout = this.mStatusViewContainer;
            if (linearLayout != null) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) linearLayout.getLayoutParams();
                marginLayoutParams.setMargins(marginLayoutParams.leftMargin, marginLayoutParams.topMargin, marginLayoutParams.rightMargin, hasHeader ? this.mBottomMarginWithHeader : this.mBottomMargin);
                this.mStatusViewContainer.setLayoutParams(marginLayoutParams);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void loadClockForUser(int i) {
        if (!isNotLockscreenClockPluginInflated()) {
            this.mKeyguardPluginFactoryLoader = KeyguardStatusViewHelper.createKeyguardPluginFactoryForUser(i, getContext().getApplicationContext());
            if (!this.mIsSkinningEnabled || this.mUserSwitchState.notifyFactoryInitializedAndCheckLoadNeeded()) {
                loadClockPluginView();
            }
        }
    }

    public void loadClockPluginView() {
        KeyguardPluginFactoryLoader keyguardPluginFactoryLoader = this.mKeyguardPluginFactoryLoader;
        if (keyguardPluginFactoryLoader == null || !"com.sonymobile.keyguard.plugin.themeableanalogclock.ThemeableAnalogClockPluginFactory".equals(keyguardPluginFactoryLoader.getActiveFullPluginClassName())) {
            loadClockPluginView(false);
        } else {
            loadClockPluginView(true);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void loadClockPluginView(boolean z) {
        if (!isNotLockscreenClockPluginInflated()) {
            if (this.mKeyguardPluginFactoryLoader.refreshLoader() || z) {
                this.mClockPluginView = KeyguardStatusViewHelper.loadCurrentClock(((GridLayout) this).mContext, this.mKeyguardPluginFactoryLoader, this.mClockPluginView, this.mLinearClockLayout, this.mDigitalClockContainer);
                if (isAttachedToWindow()) {
                    startClockTicking();
                }
            }
            refreshFormat();
        }
    }

    public void updateSkinnedResources(Resources resources) {
        if (this.mUserSwitchState.notifyResourceInitializedAndCheckLoadNeeded()) {
            loadClockPluginView(true);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        layoutOwnerInfo();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        KeyguardClockSwitch keyguardClockSwitch = this.mClockView;
        if (keyguardClockSwitch != null) {
            keyguardClockSwitch.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.widget_big_font_size));
        }
        TextView textView = this.mOwnerInfo;
        if (textView != null) {
            textView.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.widget_label_font_size));
        }
        ImageView imageView = this.mAssistIcon;
        if (imageView != null) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
            marginLayoutParams.bottomMargin = getResources().getDimensionPixelSize(R$dimen.somc_keyguard_assist_icon_bottom_margin);
            marginLayoutParams.topMargin = getResources().getDimensionPixelSize(R$dimen.somc_keyguard_assist_icon_top_margin);
            int dimensionPixelSize = getResources().getDimensionPixelSize(R$dimen.somc_keyguard_assist_icon_size);
            marginLayoutParams.width = dimensionPixelSize;
            marginLayoutParams.height = dimensionPixelSize;
            this.mAssistIcon.setLayoutParams(marginLayoutParams);
        }
        loadBottomMargin();
        loadClockPluginView(true);
    }

    public void dozeTimeTick() {
        refreshTime();
        this.mKeyguardSlice.refresh();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void refreshTime() {
        this.mClockView.refresh();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateTimeZone(TimeZone timeZone) {
        this.mClockView.onTimeZoneChanged(timeZone);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void refreshFormat() {
        Patterns.update(((GridLayout) this).mContext);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
        refreshTime();
        refreshAlarmStatus();
        if (this.mClockPluginView != null) {
            Time time = new Time();
            time.setToNow();
            String formatDateTime = DateUtils.formatDateTime(getContext(), time.toMillis(false), 1);
            if (formatDateTime != null) {
                this.mClockPluginView.setContentDescription(formatDateTime);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void refreshAlarmStatus() {
        AlarmManager.AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(-2);
        if (nextAlarmClock != null) {
            KeyguardStatusViewHelper.setNextAlarm(this.mClockPluginView, formatNextAlarm(((GridLayout) this).mContext, nextAlarmClock));
            return;
        }
        KeyguardStatusViewHelper.setNextAlarm(this.mClockPluginView, "");
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo alarmClockInfo) {
        if (alarmClockInfo == null) {
            return "";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString();
    }

    public int getLogoutButtonHeight() {
        TextView textView = this.mLogoutView;
        if (textView != null && textView.getVisibility() == 0) {
            return this.mLogoutView.getHeight();
        }
        return 0;
    }

    public float getClockTextSize() {
        return this.mClockView.getTextSize();
    }

    public int getClockPreferredY(int i) {
        return this.mClockView.getPreferredY(i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateLogoutView() {
        TextView textView = this.mLogoutView;
        if (textView != null) {
            textView.setVisibility(shouldShowLogout() ? 0 : 8);
            this.mLogoutView.setText(((GridLayout) this).mContext.getResources().getString(17040139));
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            String deviceOwnerInfo = this.mLockPatternUtils.getDeviceOwnerInfo();
            if (deviceOwnerInfo == null && this.mLockPatternUtils.isOwnerInfoEnabled(KeyguardUpdateMonitor.getCurrentUser())) {
                deviceOwnerInfo = this.mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
            this.mOwnerInfo.setText(deviceOwnerInfo);
            updateDark();
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(((GridLayout) this).mContext).registerCallback(this.mInfoCallback);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        startClockTicking();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(((GridLayout) this).mContext).removeCallback(this.mInfoCallback);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
        stopClockTicking();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onLocaleListChanged() {
        refreshFormat();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Object obj;
        printWriter.println("KeyguardStatusView:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mOwnerInfo: ");
        TextView textView = this.mOwnerInfo;
        boolean z = true;
        if (textView == null) {
            obj = "null";
        } else {
            obj = Boolean.valueOf(textView.getVisibility() == 0);
        }
        sb.append(obj);
        printWriter.println(sb.toString());
        printWriter.println("  mPulsing: " + this.mPulsing);
        printWriter.println("  mDarkAmount: " + this.mDarkAmount);
        printWriter.println("  mTextColor: " + Integer.toHexString(this.mTextColor));
        if (this.mLogoutView != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("  logout visible: ");
            if (this.mLogoutView.getVisibility() != 0) {
                z = false;
            }
            sb2.append(z);
            printWriter.println(sb2.toString());
        }
        KeyguardClockSwitch keyguardClockSwitch = this.mClockView;
        if (keyguardClockSwitch != null) {
            keyguardClockSwitch.dump(fileDescriptor, printWriter, strArr);
        }
        KeyguardSliceView keyguardSliceView = this.mKeyguardSlice;
        if (keyguardSliceView != null) {
            keyguardSliceView.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void loadBottomMargin() {
        this.mBottomMargin = getResources().getDimensionPixelSize(R$dimen.widget_vertical_padding);
        this.mBottomMarginWithHeader = getResources().getDimensionPixelSize(R$dimen.widget_vertical_padding_with_header);
    }

    /* access modifiers changed from: private */
    public static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;

        static void update(Context context) {
            Locale locale = Locale.getDefault();
            Resources resources = context.getResources();
            String string = resources.getString(R$string.clock_12hr_format);
            String string2 = resources.getString(R$string.clock_24hr_format);
            String str = locale.toString() + string + string2;
            if (!str.equals(cacheKey)) {
                clockView12 = DateFormat.getBestDateTimePattern(locale, string);
                if (!string.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, string2);
                clockView24 = clockView24.replace(':', (char) 60929);
                clockView12 = clockView12.replace(':', (char) 60929);
                cacheKey = str;
            }
        }
    }

    public void setDarkAmount(float f) {
        if (this.mDarkAmount != f) {
            this.mDarkAmount = f;
            this.mClockView.setDarkAmount(f);
            updateDark();
        }
    }

    private void updateDark() {
        float f = 1.0f;
        int i = 0;
        boolean z = this.mDarkAmount == 1.0f;
        TextView textView = this.mLogoutView;
        if (textView != null) {
            if (z) {
                f = 0.0f;
            }
            textView.setAlpha(f);
        }
        TextView textView2 = this.mOwnerInfo;
        if (textView2 != null) {
            boolean z2 = !TextUtils.isEmpty(textView2.getText());
            TextView textView3 = this.mOwnerInfo;
            if (!z2) {
                i = 8;
            }
            textView3.setVisibility(i);
            layoutOwnerInfo();
        }
        int blendARGB = ColorUtils.blendARGB(this.mTextColor, -1, this.mDarkAmount);
        this.mKeyguardSlice.setDarkAmount(this.mDarkAmount);
        this.mClockView.setTextColor(blendARGB);
        ((LockscreenAssistIconController) Dependency.get(LockscreenAssistIconController.class)).setDoze(z);
    }

    private void layoutOwnerInfo() {
        TextView textView = this.mOwnerInfo;
        if (textView != null && textView.getVisibility() != 8) {
            this.mOwnerInfo.setAlpha(1.0f - this.mDarkAmount);
            setBottom(getMeasuredHeight() - ((int) (((float) ((this.mOwnerInfo.getBottom() + this.mOwnerInfo.getPaddingBottom()) - (this.mOwnerInfo.getTop() - this.mOwnerInfo.getPaddingTop()))) * this.mDarkAmount)));
        }
    }

    public void setPulsing(boolean z) {
        if (this.mPulsing != z) {
            this.mPulsing = z;
        }
    }

    private boolean shouldShowLogout() {
        return KeyguardUpdateMonitor.getInstance(((GridLayout) this).mContext).isLogoutEnabled() && KeyguardUpdateMonitor.getCurrentUser() != 0;
    }

    /* access modifiers changed from: private */
    public void onLogoutClicked(View view) {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        try {
            this.mIActivityManager.switchUser(0);
            this.mIActivityManager.stopUser(currentUser, true, (IStopUserCallback) null);
        } catch (RemoteException e) {
            Log.e("KeyguardStatusView", "Failed to logout user", e);
        }
    }

    private boolean isNotLockscreenClockPluginInflated() {
        return this.mLinearClockLayout == null;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startClockTicking() {
        if (!isNotLockscreenClockPluginInflated()) {
            KeyguardStatusViewHelper.startClockTicking(this.mClockPluginView);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopClockTicking() {
        if (!isNotLockscreenClockPluginInflated()) {
            KeyguardStatusViewHelper.stopClockTicking(this.mClockPluginView);
        }
    }

    /* access modifiers changed from: private */
    public static final class UserSwitchState {
        private boolean mFactoryInitialized;
        private boolean mResourcesInitialized;

        private UserSwitchState() {
            this.mFactoryInitialized = true;
            this.mResourcesInitialized = true;
        }

        public void startUserSwitch() {
            this.mFactoryInitialized = false;
            this.mResourcesInitialized = false;
        }

        public boolean notifyFactoryInitializedAndCheckLoadNeeded() {
            this.mFactoryInitialized = true;
            return isCompleted();
        }

        public boolean notifyResourceInitializedAndCheckLoadNeeded() {
            this.mResourcesInitialized = true;
            return isCompleted();
        }

        private boolean isCompleted() {
            return this.mFactoryInitialized && this.mResourcesInitialized;
        }
    }
}
