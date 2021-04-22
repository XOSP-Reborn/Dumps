package com.android.systemui.qs;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0015R$style;
import com.android.systemui.DualToneHandler;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.qs.QSDetail;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.DateView;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Locale;
import java.util.Objects;

public class QuickStatusBarHeader extends RelativeLayout implements View.OnClickListener, NextAlarmController.NextAlarmChangeCallback, ZenModeController.Callback {
    private final ActivityStarter mActivityStarter;
    private final NextAlarmController mAlarmController;
    private BatteryMeterView mBatteryRemainingIcon;
    private QSCarrierGroup mCarrierGroup;
    private Clock mClockView;
    private DateView mDateView;
    private DualToneHandler mDualToneHandler;
    private boolean mExpanded;
    private final Handler mHandler = new Handler();
    private boolean mHasTopCutout = false;
    protected QuickQSPanel mHeaderQsPanel;
    private TouchAnimator mHeaderTextContainerAlphaAnimator;
    private View mHeaderTextContainerView;
    protected QSTileHost mHost;
    private StatusBarIconController.TintedIconManager mIconManager;
    private boolean mListening;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private View mNextAlarmContainer;
    private ImageView mNextAlarmIcon;
    private TextView mNextAlarmTextView;
    private boolean mQsDisabled;
    private QSPanel mQsPanel;
    private View mQuickQsStatusIcons;
    private View mRingerContainer;
    private int mRingerMode = 2;
    private ImageView mRingerModeIcon;
    private TextView mRingerModeTextView;
    private final BroadcastReceiver mRingerReceiver = new BroadcastReceiver() {
        /* class com.android.systemui.qs.QuickStatusBarHeader.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            QuickStatusBarHeader.this.mRingerMode = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
            QuickStatusBarHeader.this.updateStatusText();
        }
    };
    private final StatusBarIconController mStatusBarIconController;
    private TouchAnimator mStatusIconsAlphaAnimator;
    private View mStatusSeparator;
    private View mSystemIconsView;
    private final ZenModeController mZenController;

    public static float getColorIntensity(int i) {
        return i == -1 ? 0.0f : 1.0f;
    }

    public QuickStatusBarHeader(Context context, AttributeSet attributeSet, NextAlarmController nextAlarmController, ZenModeController zenModeController, StatusBarIconController statusBarIconController, ActivityStarter activityStarter) {
        super(context, attributeSet);
        this.mAlarmController = nextAlarmController;
        this.mZenController = zenModeController;
        this.mStatusBarIconController = statusBarIconController;
        this.mActivityStarter = activityStarter;
        this.mDualToneHandler = new DualToneHandler(new ContextThemeWrapper(context, C0015R$style.QSHeaderTheme));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHeaderQsPanel = (QuickQSPanel) findViewById(C0007R$id.quick_qs_panel);
        this.mSystemIconsView = findViewById(C0007R$id.quick_status_bar_system_icons);
        this.mQuickQsStatusIcons = findViewById(C0007R$id.quick_qs_status_icons);
        StatusIconContainer statusIconContainer = (StatusIconContainer) findViewById(C0007R$id.statusIcons);
        statusIconContainer.setShouldRestrictIcons(false);
        this.mIconManager = new StatusBarIconController.TintedIconManager(statusIconContainer);
        this.mHeaderTextContainerView = findViewById(C0007R$id.header_text_container);
        this.mStatusSeparator = findViewById(C0007R$id.status_separator);
        this.mNextAlarmIcon = (ImageView) findViewById(C0007R$id.next_alarm_icon);
        this.mNextAlarmTextView = (TextView) findViewById(C0007R$id.next_alarm_text);
        this.mNextAlarmContainer = findViewById(C0007R$id.alarm_container);
        this.mNextAlarmContainer.setOnClickListener(new View.OnClickListener() {
            /* class com.android.systemui.qs.$$Lambda$p8TkVReSUo0LsQ3y9iKja9mJXE */

            public final void onClick(View view) {
                QuickStatusBarHeader.this.onClick(view);
            }
        });
        this.mRingerModeIcon = (ImageView) findViewById(C0007R$id.ringer_mode_icon);
        this.mRingerModeTextView = (TextView) findViewById(C0007R$id.ringer_mode_text);
        this.mRingerContainer = findViewById(C0007R$id.ringer_container);
        this.mCarrierGroup = (QSCarrierGroup) findViewById(C0007R$id.carrier_group);
        updateResources();
        Rect rect = new Rect(0, 0, 0, 0);
        int singleColor = this.mDualToneHandler.getSingleColor(getColorIntensity(Utils.getColorAttrDefaultColor(getContext(), 16842800)));
        applyDarkness(C0007R$id.clock, rect, 0.0f, -1);
        this.mIconManager.setTint(singleColor);
        this.mNextAlarmIcon.setImageTintList(ColorStateList.valueOf(singleColor));
        this.mRingerModeIcon.setImageTintList(ColorStateList.valueOf(singleColor));
        this.mClockView = (Clock) findViewById(C0007R$id.clock);
        this.mClockView.setOnClickListener(this);
        this.mDateView = (DateView) findViewById(C0007R$id.date);
        this.mBatteryRemainingIcon = (BatteryMeterView) findViewById(C0007R$id.batteryRemainingIcon);
        this.mBatteryRemainingIcon.setIgnoreTunerUpdates(true);
        this.mBatteryRemainingIcon.setPercentShowMode(3);
        this.mRingerModeTextView.setSelected(true);
        this.mNextAlarmTextView.setSelected(true);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateStatusText() {
        boolean z = true;
        int i = 0;
        if (updateRingerStatus() || updateAlarmStatus()) {
            boolean z2 = this.mNextAlarmTextView.getVisibility() == 0;
            if (this.mRingerModeTextView.getVisibility() != 0) {
                z = false;
            }
            View view = this.mStatusSeparator;
            if (!z2 || !z) {
                i = 8;
            }
            view.setVisibility(i);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0051  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0053  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x005b  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0065  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean updateRingerStatus() {
        /*
        // Method dump skipped, instructions count: 122
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.qs.QuickStatusBarHeader.updateRingerStatus():boolean");
    }

    private boolean updateAlarmStatus() {
        boolean z;
        boolean z2 = this.mNextAlarmTextView.getVisibility() == 0;
        CharSequence text = this.mNextAlarmTextView.getText();
        AlarmManager.AlarmClockInfo alarmClockInfo = this.mNextAlarm;
        if (alarmClockInfo != null) {
            this.mNextAlarmTextView.setText(formatNextAlarm(alarmClockInfo));
            z = true;
        } else {
            z = false;
        }
        int i = 8;
        this.mNextAlarmIcon.setVisibility(z ? 0 : 8);
        this.mNextAlarmTextView.setVisibility(z ? 0 : 8);
        View view = this.mNextAlarmContainer;
        if (z) {
            i = 0;
        }
        view.setVisibility(i);
        return z2 != z || !Objects.equals(text, this.mNextAlarmTextView.getText());
    }

    private void applyDarkness(int i, Rect rect, float f, int i2) {
        View findViewById = findViewById(i);
        if (findViewById instanceof DarkIconDispatcher.DarkReceiver) {
            ((DarkIconDispatcher.DarkReceiver) findViewById).onDarkChanged(rect, f, i2);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateResources();
        this.mClockView.useWallpaperTextColor(configuration.orientation == 2);
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateResources();
    }

    private void updateMinimumHeight() {
        setMinimumHeight(((RelativeLayout) this).mContext.getResources().getDimensionPixelSize(17105427) + ((RelativeLayout) this).mContext.getResources().getDimensionPixelSize(C0005R$dimen.qs_quick_header_panel_height));
    }

    private void updateResources() {
        Resources resources = ((RelativeLayout) this).mContext.getResources();
        updateMinimumHeight();
        this.mHeaderTextContainerView.getLayoutParams().height = resources.getDimensionPixelSize(C0005R$dimen.qs_header_tooltip_height);
        View view = this.mHeaderTextContainerView;
        view.setLayoutParams(view.getLayoutParams());
        this.mSystemIconsView.getLayoutParams().height = resources.getDimensionPixelSize(17105397);
        View view2 = this.mSystemIconsView;
        view2.setLayoutParams(view2.getLayoutParams());
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        if (this.mQsDisabled) {
            layoutParams.height = resources.getDimensionPixelSize(17105397);
        } else {
            layoutParams.height = Math.max(getMinimumHeight(), resources.getDimensionPixelSize(17105398));
        }
        setLayoutParams(layoutParams);
        updateStatusIconAlphaAnimator();
        updateHeaderTextContainerAlphaAnimator();
    }

    private void updateStatusIconAlphaAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        builder.addFloat(this.mQuickQsStatusIcons, "alpha", 1.0f, 0.0f, 0.0f);
        this.mStatusIconsAlphaAnimator = builder.build();
    }

    private void updateHeaderTextContainerAlphaAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        builder.addFloat(this.mHeaderTextContainerView, "alpha", 0.0f, 0.0f, 1.0f);
        this.mHeaderTextContainerAlphaAnimator = builder.build();
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded != z) {
            this.mExpanded = z;
            this.mHeaderQsPanel.setExpanded(z);
            updateEverything();
        }
    }

    public void setExpansion(boolean z, float f, float f2) {
        if (z) {
            f = 1.0f;
        }
        TouchAnimator touchAnimator = this.mStatusIconsAlphaAnimator;
        if (touchAnimator != null) {
            touchAnimator.setPosition(f);
        }
        if (z) {
            this.mHeaderTextContainerView.setTranslationY(f2);
        } else {
            this.mHeaderTextContainerView.setTranslationY(0.0f);
        }
        TouchAnimator touchAnimator2 = this.mHeaderTextContainerAlphaAnimator;
        if (touchAnimator2 != null) {
            touchAnimator2.setPosition(f);
            if (f > 0.0f) {
                this.mHeaderTextContainerView.setVisibility(0);
            } else {
                this.mHeaderTextContainerView.setVisibility(4);
            }
        }
    }

    public void disable(int i, int i2, boolean z) {
        boolean z2 = true;
        int i3 = 0;
        if ((i2 & 1) == 0) {
            z2 = false;
        }
        if (z2 != this.mQsDisabled) {
            this.mQsDisabled = z2;
            this.mHeaderQsPanel.setDisabledByPolicy(z2);
            this.mHeaderTextContainerView.setVisibility(this.mQsDisabled ? 8 : 0);
            View view = this.mQuickQsStatusIcons;
            if (this.mQsDisabled) {
                i3 = 8;
            }
            view.setVisibility(i3);
            updateResources();
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mStatusBarIconController.addIconGroup(this.mIconManager);
        requestApplyInsets();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        Pair<Integer, Integer> cornerCutoutMargins = PhoneStatusBarView.cornerCutoutMargins(windowInsets.getDisplayCutout(), getDisplay());
        if (cornerCutoutMargins == null) {
            this.mSystemIconsView.setPaddingRelative(getResources().getDimensionPixelSize(C0005R$dimen.status_bar_padding_start), 0, getResources().getDimensionPixelSize(C0005R$dimen.status_bar_padding_end), 0);
        } else {
            this.mSystemIconsView.setPadding(((Integer) cornerCutoutMargins.first).intValue(), 0, ((Integer) cornerCutoutMargins.second).intValue(), 0);
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    public void onDetachedFromWindow() {
        setListening(false);
        this.mStatusBarIconController.removeIconGroup(this.mIconManager);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean z) {
        if (z != this.mListening) {
            this.mHeaderQsPanel.setListening(z);
            this.mListening = z;
            this.mCarrierGroup.setListening(this.mListening);
            if (z) {
                this.mZenController.addCallback(this);
                this.mAlarmController.addCallback(this);
                ((RelativeLayout) this).mContext.registerReceiver(this.mRingerReceiver, new IntentFilter("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION"));
                return;
            }
            this.mZenController.removeCallback(this);
            this.mAlarmController.removeCallback(this);
            ((RelativeLayout) this).mContext.unregisterReceiver(this.mRingerReceiver);
        }
    }

    public void onClick(View view) {
        if (view == this.mClockView) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.intent.action.SHOW_ALARMS"), 0);
            return;
        }
        View view2 = this.mNextAlarmContainer;
        if (view != view2 || !view2.isVisibleToUser()) {
            View view3 = this.mRingerContainer;
            if (view == view3 && view3.isVisibleToUser()) {
                this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.SOUND_SETTINGS"), 0);
            }
        } else if (this.mNextAlarm.getShowIntent() != null) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(this.mNextAlarm.getShowIntent());
        } else {
            Log.d("QuickStatusBarHeader", "No PendingIntent for next alarm. Using default intent");
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.intent.action.SHOW_ALARMS"), 0);
        }
    }

    @Override // com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo alarmClockInfo) {
        this.mNextAlarm = alarmClockInfo;
        updateStatusText();
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onZenChanged(int i) {
        updateStatusText();
    }

    @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
    public void onConfigChanged(ZenModeConfig zenModeConfig) {
        updateStatusText();
    }

    public /* synthetic */ void lambda$updateEverything$0$QuickStatusBarHeader() {
        setClickable(!this.mExpanded);
    }

    public void updateEverything() {
        post(new Runnable() {
            /* class com.android.systemui.qs.$$Lambda$QuickStatusBarHeader$AvsHoBxZXMvvH_WD73mLXoXpNWs */

            public final void run() {
                QuickStatusBarHeader.this.lambda$updateEverything$0$QuickStatusBarHeader();
            }
        });
    }

    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        setupHost(qSPanel.getHost());
    }

    public void setupHost(QSTileHost qSTileHost) {
        this.mHost = qSTileHost;
        this.mHeaderQsPanel.setQSPanelAndHeader(this.mQsPanel, this);
        this.mHeaderQsPanel.setHost(qSTileHost, null);
        Rect rect = new Rect(0, 0, 0, 0);
        float colorIntensity = getColorIntensity(Utils.getColorAttrDefaultColor(getContext(), 16842800));
        this.mBatteryRemainingIcon.onDarkChanged(rect, colorIntensity, this.mDualToneHandler.getSingleColor(colorIntensity));
    }

    public void setCallback(QSDetail.Callback callback) {
        this.mHeaderQsPanel.setCallback(callback);
    }

    private String formatNextAlarm(AlarmManager.AlarmClockInfo alarmClockInfo) {
        if (alarmClockInfo == null) {
            return "";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(((RelativeLayout) this).mContext, ActivityManager.getCurrentUser()) ? "EHm" : "Ehma"), alarmClockInfo.getTriggerTime()).toString();
    }

    public void setMargins(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (!(childAt == this.mSystemIconsView || childAt == this.mQuickQsStatusIcons || childAt == this.mHeaderQsPanel || childAt == this.mHeaderTextContainerView)) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) childAt.getLayoutParams();
                layoutParams.leftMargin = i;
                layoutParams.rightMargin = i;
            }
        }
    }
}
