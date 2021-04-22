package com.android.systemui.statusbar.notification.row;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.row.NotificationGuts;
import java.util.List;
import java.util.Set;

public class NotificationInfo extends LinearLayout implements NotificationGuts.GutsContent {
    private String mAppName;
    private OnAppSettingsClickListener mAppSettingsClickListener;
    private int mAppUid;
    private ChannelEditorDialogController mChannelEditorDialogController;
    private CheckSaveListener mCheckSaveListener;
    private Integer mChosenImportance;
    private String mDelegatePkg;
    private String mExitReason = "blocking_helper_dismissed";
    private AnimatorSet mExpandAnimation;
    private NotificationGuts mGutsContainer;
    private INotificationManager mINotificationManager;
    private boolean mIsDeviceProvisioned;
    private boolean mIsForBlockingHelper;
    private boolean mIsNonblockable;
    private boolean mIsSingleDefaultChannel;
    private MetricsLogger mMetricsLogger;
    private int mNumUniqueChannelsInRow;
    private View.OnClickListener mOnAlert = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$lxdNUTZhRsTq1qLdFuCftTaKsI */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$0$NotificationInfo(view);
        }
    };
    private View.OnClickListener mOnDeliverSilently = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$QY1Am5dutJVQ0eHq5s0Z3dSQLu8 */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$4$NotificationInfo(view);
        }
    };
    private View.OnClickListener mOnDismissSettings = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$p3qjyEUB89vA_NRs8XRVogtSM4k */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$2$NotificationInfo(view);
        }
    };
    private View.OnClickListener mOnKeepShowing = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$j11VBERw7GgslLu77BlqWodFHxk */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$3$NotificationInfo(view);
        }
    };
    private OnSettingsClickListener mOnSettingsClickListener;
    private View.OnClickListener mOnSilent = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$x1Q8n0IIdzsrzqhyaxjftYvWg5M */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$1$NotificationInfo(view);
        }
    };
    private View.OnClickListener mOnUndo = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$kcLdDg1SWpjkhvCX9FbMHOcKdX8 */

        public final void onClick(View view) {
            NotificationInfo.this.lambda$new$6$NotificationInfo(view);
        }
    };
    private String mPackageName;
    private Drawable mPkgIcon;
    private PackageManager mPm;
    private boolean mPresentingChannelEditorDialog = false;
    private boolean mPressedApply;
    private TextView mPriorityDescriptionView;
    private StatusBarNotification mSbn;
    private TextView mSilentDescriptionView;
    private NotificationChannel mSingleNotificationChannel;
    private int mStartingChannelImportance;
    private Set<NotificationChannel> mUniqueChannelsInRow;
    private VisualStabilityManager mVisualStabilityManager;
    private boolean mWasShownHighPriority;

    public interface CheckSaveListener {
        void checkSave(Runnable runnable, StatusBarNotification statusBarNotification);
    }

    public interface OnAppSettingsClickListener {
        void onClick(View view, Intent intent);
    }

    public interface OnSettingsClickListener {
        void onClick(View view, NotificationChannel notificationChannel, int i);
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public View getContentView() {
        return this;
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public boolean willBeRemoved() {
        return false;
    }

    public /* synthetic */ void lambda$new$0$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_keep_showing";
        this.mChosenImportance = 3;
        applyAlertingBehavior(0, true);
    }

    public /* synthetic */ void lambda$new$1$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_deliver_silently";
        this.mChosenImportance = 2;
        applyAlertingBehavior(1, true);
    }

    public /* synthetic */ void lambda$new$2$NotificationInfo(View view) {
        this.mPressedApply = true;
        closeControls(view, true);
    }

    public /* synthetic */ void lambda$new$3$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_keep_showing";
        closeControls(view, true);
        this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(4).setSubtype(5));
    }

    public /* synthetic */ void lambda$new$4$NotificationInfo(View view) {
        handleSaveImportance(4, 5);
    }

    private void handleSaveImportance(int i, int i2) {
        $$Lambda$NotificationInfo$VsGw7yinvO7eMlSnQkbAlXJJig r0 = new Runnable(i, i2) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$VsGw7yinvO7eMlSnQkbAlXJJig */
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NotificationInfo.this.lambda$handleSaveImportance$5$NotificationInfo(this.f$1, this.f$2);
            }
        };
        CheckSaveListener checkSaveListener = this.mCheckSaveListener;
        if (checkSaveListener != null) {
            checkSaveListener.checkSave(r0, this.mSbn);
        } else {
            r0.run();
        }
    }

    public /* synthetic */ void lambda$handleSaveImportance$5$NotificationInfo(int i, int i2) {
        saveImportanceAndExitReason(i);
        if (this.mIsForBlockingHelper) {
            swapContent(i, true);
            this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(4).setSubtype(i2));
        }
    }

    public /* synthetic */ void lambda$new$6$NotificationInfo(View view) {
        this.mExitReason = "blocking_helper_dismissed";
        if (this.mIsForBlockingHelper) {
            logBlockingHelperCounter("blocking_helper_undo");
            this.mMetricsLogger.write(getLogMaker().setCategory(1621).setType(5).setSubtype(7));
        } else {
            this.mMetricsLogger.write(importanceChangeLogMaker().setType(5));
        }
        saveImportanceAndExitReason(1);
        swapContent(1, true);
    }

    public NotificationInfo(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPriorityDescriptionView = (TextView) findViewById(C0007R$id.alert_summary);
        this.mSilentDescriptionView = (TextView) findViewById(C0007R$id.silence_summary);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void bindNotification(PackageManager packageManager, INotificationManager iNotificationManager, VisualStabilityManager visualStabilityManager, String str, NotificationChannel notificationChannel, Set<NotificationChannel> set, StatusBarNotification statusBarNotification, CheckSaveListener checkSaveListener, OnSettingsClickListener onSettingsClickListener, OnAppSettingsClickListener onAppSettingsClickListener, boolean z, boolean z2, int i, boolean z3) throws RemoteException {
        bindNotification(packageManager, iNotificationManager, visualStabilityManager, str, notificationChannel, set, statusBarNotification, checkSaveListener, onSettingsClickListener, onAppSettingsClickListener, z, z2, false, i, z3);
    }

    public void bindNotification(PackageManager packageManager, INotificationManager iNotificationManager, VisualStabilityManager visualStabilityManager, String str, NotificationChannel notificationChannel, Set<NotificationChannel> set, StatusBarNotification statusBarNotification, CheckSaveListener checkSaveListener, OnSettingsClickListener onSettingsClickListener, OnAppSettingsClickListener onAppSettingsClickListener, boolean z, boolean z2, boolean z3, int i, boolean z4) throws RemoteException {
        this.mINotificationManager = iNotificationManager;
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mVisualStabilityManager = visualStabilityManager;
        this.mChannelEditorDialogController = (ChannelEditorDialogController) Dependency.get(ChannelEditorDialogController.class);
        this.mPackageName = str;
        this.mUniqueChannelsInRow = set;
        this.mNumUniqueChannelsInRow = set.size();
        this.mSbn = statusBarNotification;
        this.mPm = packageManager;
        this.mAppSettingsClickListener = onAppSettingsClickListener;
        this.mAppName = this.mPackageName;
        this.mCheckSaveListener = checkSaveListener;
        this.mOnSettingsClickListener = onSettingsClickListener;
        this.mSingleNotificationChannel = notificationChannel;
        this.mStartingChannelImportance = this.mSingleNotificationChannel.getImportance();
        this.mWasShownHighPriority = z4;
        this.mIsNonblockable = z2;
        this.mIsForBlockingHelper = z3;
        this.mAppUid = this.mSbn.getUid();
        this.mDelegatePkg = this.mSbn.getOpPkg();
        this.mIsDeviceProvisioned = z;
        boolean z5 = false;
        int numNotificationChannelsForPackage = this.mINotificationManager.getNumNotificationChannelsForPackage(str, this.mAppUid, false);
        int i2 = this.mNumUniqueChannelsInRow;
        if (i2 != 0) {
            if (i2 == 1 && this.mSingleNotificationChannel.getId().equals("miscellaneous") && numNotificationChannelsForPackage == 1) {
                z5 = true;
            }
            this.mIsSingleDefaultChannel = z5;
            bindHeader();
            bindChannelDetails();
            if (this.mIsForBlockingHelper) {
                bindBlockingHelper();
            } else {
                bindInlineControls();
            }
            this.mMetricsLogger.write(notificationControlsLogMaker());
            return;
        }
        throw new IllegalArgumentException("bindNotification requires at least one channel");
    }

    private void bindBlockingHelper() {
        int i = 8;
        findViewById(C0007R$id.inline_controls).setVisibility(8);
        findViewById(C0007R$id.blocking_helper).setVisibility(0);
        findViewById(C0007R$id.undo).setOnClickListener(this.mOnUndo);
        View findViewById = findViewById(C0007R$id.blocking_helper_turn_off_notifications);
        findViewById.setOnClickListener(getSettingsOnClickListener());
        if (findViewById.hasOnClickListeners()) {
            i = 0;
        }
        findViewById.setVisibility(i);
        ((TextView) findViewById(C0007R$id.keep_showing)).setOnClickListener(this.mOnKeepShowing);
        findViewById(C0007R$id.deliver_silently).setOnClickListener(this.mOnDeliverSilently);
    }

    private void bindInlineControls() {
        findViewById(C0007R$id.inline_controls).setVisibility(0);
        int i = 8;
        findViewById(C0007R$id.blocking_helper).setVisibility(8);
        if (this.mIsNonblockable) {
            findViewById(C0007R$id.non_configurable_text).setVisibility(0);
            findViewById(C0007R$id.non_configurable_multichannel_text).setVisibility(8);
            findViewById(C0007R$id.interruptiveness_settings).setVisibility(8);
            ((TextView) findViewById(C0007R$id.done)).setText(C0014R$string.inline_done_button);
            findViewById(C0007R$id.turn_off_notifications).setVisibility(8);
        } else if (this.mNumUniqueChannelsInRow > 1) {
            findViewById(C0007R$id.non_configurable_text).setVisibility(8);
            findViewById(C0007R$id.interruptiveness_settings).setVisibility(8);
            findViewById(C0007R$id.non_configurable_multichannel_text).setVisibility(0);
        } else {
            findViewById(C0007R$id.non_configurable_text).setVisibility(8);
            findViewById(C0007R$id.non_configurable_multichannel_text).setVisibility(8);
            findViewById(C0007R$id.interruptiveness_settings).setVisibility(0);
        }
        View findViewById = findViewById(C0007R$id.turn_off_notifications);
        findViewById.setOnClickListener(getTurnOffNotificationsClickListener());
        if (findViewById.hasOnClickListeners() && !this.mIsNonblockable) {
            i = 0;
        }
        findViewById.setVisibility(i);
        findViewById(C0007R$id.done).setOnClickListener(this.mOnDismissSettings);
        View findViewById2 = findViewById(C0007R$id.silence);
        View findViewById3 = findViewById(C0007R$id.alert);
        findViewById2.setOnClickListener(this.mOnSilent);
        findViewById3.setOnClickListener(this.mOnAlert);
        applyAlertingBehavior(!this.mWasShownHighPriority ? 1 : 0, false);
    }

    private void bindHeader() {
        this.mPkgIcon = null;
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(this.mPackageName, 795136);
            if (applicationInfo != null) {
                this.mAppName = String.valueOf(this.mPm.getApplicationLabel(applicationInfo));
                this.mPkgIcon = this.mPm.getApplicationIcon(applicationInfo);
            }
        } catch (PackageManager.NameNotFoundException unused) {
            this.mPkgIcon = this.mPm.getDefaultActivityIcon();
        }
        ((ImageView) findViewById(C0007R$id.pkgicon)).setImageDrawable(this.mPkgIcon);
        ((TextView) findViewById(C0007R$id.pkgname)).setText(this.mAppName);
        bindDelegate();
        View findViewById = findViewById(C0007R$id.app_settings);
        Intent appSettingsIntent = getAppSettingsIntent(this.mPm, this.mPackageName, this.mSingleNotificationChannel, this.mSbn.getId(), this.mSbn.getTag());
        int i = 0;
        if (appSettingsIntent == null || TextUtils.isEmpty(this.mSbn.getNotification().getSettingsText())) {
            findViewById.setVisibility(8);
        } else {
            findViewById.setVisibility(0);
            findViewById.setOnClickListener(new View.OnClickListener(appSettingsIntent) {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$tDyIQiSMKPJwexxQ_nMHuNF9Llk */
                private final /* synthetic */ Intent f$1;

                {
                    this.f$1 = r2;
                }

                public final void onClick(View view) {
                    NotificationInfo.this.lambda$bindHeader$7$NotificationInfo(this.f$1, view);
                }
            });
        }
        View findViewById2 = findViewById(C0007R$id.info);
        findViewById2.setOnClickListener(getSettingsOnClickListener());
        if (!findViewById2.hasOnClickListeners()) {
            i = 8;
        }
        findViewById2.setVisibility(i);
    }

    public /* synthetic */ void lambda$bindHeader$7$NotificationInfo(Intent intent, View view) {
        this.mAppSettingsClickListener.onClick(view, intent);
    }

    private View.OnClickListener getSettingsOnClickListener() {
        int i = this.mAppUid;
        if (i < 0 || this.mOnSettingsClickListener == null || !this.mIsDeviceProvisioned) {
            return null;
        }
        return new View.OnClickListener(i) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$0pyRe_YAFdEij18HwbLfjxZpe4 */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                NotificationInfo.this.lambda$getSettingsOnClickListener$8$NotificationInfo(this.f$1, view);
            }
        };
    }

    public /* synthetic */ void lambda$getSettingsOnClickListener$8$NotificationInfo(int i, View view) {
        logBlockingHelperCounter("blocking_helper_notif_settings");
        this.mOnSettingsClickListener.onClick(view, this.mNumUniqueChannelsInRow > 1 ? null : this.mSingleNotificationChannel, i);
    }

    private View.OnClickListener getTurnOffNotificationsClickListener() {
        return new View.OnClickListener() {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$lHPMwwhdR5jQxtA0fAOND0eRPUs */

            public final void onClick(View view) {
                NotificationInfo.this.lambda$getTurnOffNotificationsClickListener$10$NotificationInfo(view);
            }
        };
    }

    public /* synthetic */ void lambda$getTurnOffNotificationsClickListener$10$NotificationInfo(View view) {
        ChannelEditorDialogController channelEditorDialogController;
        if (!this.mPresentingChannelEditorDialog && (channelEditorDialogController = this.mChannelEditorDialogController) != null) {
            this.mPresentingChannelEditorDialog = true;
            channelEditorDialogController.prepareDialogForApp(this.mAppName, this.mPackageName, this.mAppUid, this.mUniqueChannelsInRow, this.mPkgIcon, this.mOnSettingsClickListener);
            this.mChannelEditorDialogController.setOnFinishListener(new OnChannelEditorDialogFinishedListener() {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$tUHhb1UHSajuu5XM15HG0P0_DVY */

                @Override // com.android.systemui.statusbar.notification.row.OnChannelEditorDialogFinishedListener
                public final void onChannelEditorDialogFinished() {
                    NotificationInfo.this.lambda$getTurnOffNotificationsClickListener$9$NotificationInfo();
                }
            });
            this.mChannelEditorDialogController.show();
        }
    }

    public /* synthetic */ void lambda$getTurnOffNotificationsClickListener$9$NotificationInfo() {
        this.mPresentingChannelEditorDialog = false;
        closeControls(this, false);
    }

    private void bindChannelDetails() throws RemoteException {
        bindName();
        bindGroup();
    }

    private void bindName() {
        TextView textView = (TextView) findViewById(C0007R$id.channel_name);
        if (this.mIsSingleDefaultChannel || this.mNumUniqueChannelsInRow > 1) {
            textView.setVisibility(8);
        } else {
            textView.setText(this.mSingleNotificationChannel.getName());
        }
    }

    private void bindDelegate() {
        TextView textView = (TextView) findViewById(C0007R$id.delegate_name);
        TextView textView2 = (TextView) findViewById(C0007R$id.pkg_divider);
        if (!TextUtils.equals(this.mPackageName, this.mDelegatePkg)) {
            textView.setVisibility(0);
            textView2.setVisibility(0);
            return;
        }
        textView.setVisibility(8);
        textView2.setVisibility(8);
    }

    private void bindGroup() throws RemoteException {
        NotificationChannelGroup notificationChannelGroupForPackage;
        NotificationChannel notificationChannel = this.mSingleNotificationChannel;
        CharSequence name = (notificationChannel == null || notificationChannel.getGroup() == null || (notificationChannelGroupForPackage = this.mINotificationManager.getNotificationChannelGroupForPackage(this.mSingleNotificationChannel.getGroup(), this.mPackageName, this.mAppUid)) == null) ? null : notificationChannelGroupForPackage.getName();
        TextView textView = (TextView) findViewById(C0007R$id.group_name);
        if (name != null) {
            textView.setText(name);
            textView.setVisibility(0);
            return;
        }
        textView.setVisibility(8);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void logBlockingHelperCounter(String str) {
        if (this.mIsForBlockingHelper) {
            this.mMetricsLogger.count(str, 1);
        }
    }

    private void saveImportance() {
        if (!this.mIsNonblockable || this.mExitReason != "blocking_helper_stop_notifications") {
            if (this.mChosenImportance == null) {
                this.mChosenImportance = Integer.valueOf(this.mStartingChannelImportance);
            }
            updateImportance();
        }
    }

    private void updateImportance() {
        if (this.mChosenImportance != null) {
            this.mMetricsLogger.write(importanceChangeLogMaker());
            int intValue = this.mChosenImportance.intValue();
            if (this.mStartingChannelImportance != -1000 && ((this.mWasShownHighPriority && this.mChosenImportance.intValue() >= 3) || (!this.mWasShownHighPriority && this.mChosenImportance.intValue() < 3))) {
                intValue = this.mStartingChannelImportance;
            }
            new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)).post(new UpdateImportanceRunnable(this.mINotificationManager, this.mPackageName, this.mAppUid, this.mNumUniqueChannelsInRow == 1 ? this.mSingleNotificationChannel : null, this.mStartingChannelImportance, intValue));
            this.mVisualStabilityManager.temporarilyAllowReordering();
        }
    }

    private void applyAlertingBehavior(int i, boolean z) {
        boolean z2 = true;
        if (z) {
            TransitionSet transitionSet = new TransitionSet();
            transitionSet.setOrdering(0);
            transitionSet.addTransition(new Fade(2)).addTransition(new ChangeBounds()).addTransition(new Fade(1).setStartDelay(150).setDuration(200).setInterpolator(Interpolators.FAST_OUT_SLOW_IN));
            transitionSet.setDuration(350L);
            transitionSet.setInterpolator((TimeInterpolator) Interpolators.FAST_OUT_SLOW_IN);
            TransitionManager.beginDelayedTransition(this, transitionSet);
        }
        View findViewById = findViewById(C0007R$id.alert);
        View findViewById2 = findViewById(C0007R$id.silence);
        if (i == 0) {
            this.mPriorityDescriptionView.setVisibility(0);
            this.mSilentDescriptionView.setVisibility(8);
            post(new Runnable(findViewById, findViewById2) {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$z1_znMHrt0xHowqdvbrSsphINf0 */
                private final /* synthetic */ View f$0;
                private final /* synthetic */ View f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    NotificationInfo.lambda$applyAlertingBehavior$11(this.f$0, this.f$1);
                }
            });
        } else if (i == 1) {
            this.mSilentDescriptionView.setVisibility(0);
            this.mPriorityDescriptionView.setVisibility(8);
            post(new Runnable(findViewById, findViewById2) {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationInfo$JpCUeqH4NTReIw4dQ3BiR4iSog */
                private final /* synthetic */ View f$0;
                private final /* synthetic */ View f$1;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                }

                public final void run() {
                    NotificationInfo.lambda$applyAlertingBehavior$12(this.f$0, this.f$1);
                }
            });
        } else {
            throw new IllegalArgumentException("Unrecognized alerting behavior: " + i);
        }
        if (this.mWasShownHighPriority == (i == 0)) {
            z2 = false;
        }
        ((TextView) findViewById(C0007R$id.done)).setText(z2 ? C0014R$string.inline_ok_button : C0014R$string.inline_done_button);
    }

    static /* synthetic */ void lambda$applyAlertingBehavior$11(View view, View view2) {
        view.setSelected(true);
        view2.setSelected(false);
    }

    static /* synthetic */ void lambda$applyAlertingBehavior$12(View view, View view2) {
        view.setSelected(false);
        view2.setSelected(true);
    }

    private void saveImportanceAndExitReason(int i) {
        int i2;
        if (i == 1) {
            this.mChosenImportance = Integer.valueOf(this.mStartingChannelImportance);
        } else if (i == 4) {
            this.mExitReason = "blocking_helper_deliver_silently";
            if (this.mWasShownHighPriority) {
                i2 = 2;
            } else {
                i2 = this.mStartingChannelImportance;
            }
            this.mChosenImportance = Integer.valueOf(i2);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void swapContent(int i, boolean z) {
        AnimatorSet animatorSet = this.mExpandAnimation;
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        final View findViewById = findViewById(C0007R$id.blocking_helper);
        final ViewGroup viewGroup = (ViewGroup) findViewById(C0007R$id.confirmation);
        TextView textView = (TextView) findViewById(C0007R$id.confirmation_text);
        saveImportanceAndExitReason(i);
        if (i != 1) {
            if (i == 4) {
                textView.setText(C0014R$string.notification_channel_silenced);
            } else {
                throw new IllegalArgumentException();
            }
        }
        final boolean z2 = i == 1;
        int i2 = 8;
        findViewById.setVisibility(z2 ? 0 : 8);
        findViewById(C0007R$id.channel_info).setVisibility(z2 ? 0 : 8);
        findViewById(C0007R$id.header).setVisibility(z2 ? 0 : 8);
        if (!z2) {
            i2 = 0;
        }
        viewGroup.setVisibility(i2);
        if (z) {
            Property property = View.ALPHA;
            float[] fArr = new float[2];
            fArr[0] = findViewById.getAlpha();
            float f = 1.0f;
            fArr[1] = z2 ? 1.0f : 0.0f;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(findViewById, property, fArr);
            ofFloat.setInterpolator(z2 ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT);
            Property property2 = View.ALPHA;
            float[] fArr2 = new float[2];
            fArr2[0] = viewGroup.getAlpha();
            if (z2) {
                f = 0.0f;
            }
            fArr2[1] = f;
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(viewGroup, property2, fArr2);
            ofFloat2.setInterpolator(z2 ? Interpolators.ALPHA_OUT : Interpolators.ALPHA_IN);
            this.mExpandAnimation = new AnimatorSet();
            this.mExpandAnimation.playTogether(ofFloat, ofFloat2);
            this.mExpandAnimation.setDuration(150L);
            this.mExpandAnimation.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.statusbar.notification.row.NotificationInfo.AnonymousClass1 */
                boolean mCancelled = false;

                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        int i = 0;
                        findViewById.setVisibility(z2 ? 0 : 8);
                        ViewGroup viewGroup = viewGroup;
                        if (z2) {
                            i = 8;
                        }
                        viewGroup.setVisibility(i);
                    }
                }
            });
            this.mExpandAnimation.start();
        }
        NotificationGuts notificationGuts = this.mGutsContainer;
        if (notificationGuts != null) {
            notificationGuts.resetFalsingCheck();
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public void onFinishedClosing() {
        Integer num = this.mChosenImportance;
        if (num != null) {
            this.mStartingChannelImportance = num.intValue();
        }
        this.mExitReason = "blocking_helper_dismissed";
        if (this.mIsForBlockingHelper) {
            bindBlockingHelper();
        } else {
            bindInlineControls();
        }
        this.mMetricsLogger.write(notificationControlsLogMaker().setType(2));
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (this.mGutsContainer != null && accessibilityEvent.getEventType() == 32) {
            if (this.mGutsContainer.isExposed()) {
                accessibilityEvent.getText().add(((LinearLayout) this).mContext.getString(C0014R$string.notification_channel_controls_opened_accessibility, this.mAppName));
                return;
            }
            accessibilityEvent.getText().add(((LinearLayout) this).mContext.getString(C0014R$string.notification_channel_controls_closed_accessibility, this.mAppName));
        }
    }

    private Intent getAppSettingsIntent(PackageManager packageManager, String str, NotificationChannel notificationChannel, int i, String str2) {
        Intent intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.NOTIFICATION_PREFERENCES").setPackage(str);
        List<ResolveInfo> queryIntentActivities = packageManager.queryIntentActivities(intent, 65536);
        if (queryIntentActivities == null || queryIntentActivities.size() == 0 || queryIntentActivities.get(0) == null) {
            return null;
        }
        ActivityInfo activityInfo = queryIntentActivities.get(0).activityInfo;
        intent.setClassName(activityInfo.packageName, activityInfo.name);
        if (notificationChannel != null) {
            intent.putExtra("android.intent.extra.CHANNEL_ID", notificationChannel.getId());
        }
        intent.putExtra("android.intent.extra.NOTIFICATION_ID", i);
        intent.putExtra("android.intent.extra.NOTIFICATION_TAG", str2);
        return intent;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void closeControls(View view, boolean z) {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        this.mGutsContainer.getLocationOnScreen(iArr);
        view.getLocationOnScreen(iArr2);
        int i = iArr2[0] - iArr[0];
        int i2 = iArr2[1] - iArr[1];
        this.mGutsContainer.closeControls(i + (view.getWidth() / 2), i2 + (view.getHeight() / 2), z, false);
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public void setGutsParent(NotificationGuts notificationGuts) {
        this.mGutsContainer = notificationGuts;
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public boolean shouldBeSaved() {
        return this.mPressedApply;
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public boolean handleCloseControls(boolean z, boolean z2) {
        ChannelEditorDialogController channelEditorDialogController;
        if (this.mPresentingChannelEditorDialog && (channelEditorDialogController = this.mChannelEditorDialogController) != null) {
            this.mPresentingChannelEditorDialog = false;
            channelEditorDialogController.setOnFinishListener(null);
            this.mChannelEditorDialogController.close();
        }
        if (z) {
            saveImportance();
        }
        logBlockingHelperCounter(this.mExitReason);
        return false;
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationGuts.GutsContent
    public int getActualHeight() {
        return getHeight();
    }

    @VisibleForTesting
    public boolean isAnimating() {
        AnimatorSet animatorSet = this.mExpandAnimation;
        return animatorSet != null && animatorSet.isRunning();
    }

    /* access modifiers changed from: private */
    public static class UpdateImportanceRunnable implements Runnable {
        private final int mAppUid;
        private final NotificationChannel mChannelToUpdate;
        private final int mCurrentImportance;
        private final INotificationManager mINotificationManager;
        private final int mNewImportance;
        private final String mPackageName;

        public UpdateImportanceRunnable(INotificationManager iNotificationManager, String str, int i, NotificationChannel notificationChannel, int i2, int i3) {
            this.mINotificationManager = iNotificationManager;
            this.mPackageName = str;
            this.mAppUid = i;
            this.mChannelToUpdate = notificationChannel;
            this.mCurrentImportance = i2;
            this.mNewImportance = i3;
        }

        public void run() {
            try {
                if (this.mChannelToUpdate != null) {
                    this.mChannelToUpdate.setImportance(this.mNewImportance);
                    this.mChannelToUpdate.lockFields(4);
                    this.mINotificationManager.updateNotificationChannelForPackage(this.mPackageName, this.mAppUid, this.mChannelToUpdate);
                    return;
                }
                this.mINotificationManager.setNotificationsEnabledWithImportanceLockForPackage(this.mPackageName, this.mAppUid, this.mNewImportance >= this.mCurrentImportance);
            } catch (RemoteException e) {
                Log.e("InfoGuts", "Unable to update notification importance", e);
            }
        }
    }

    private LogMaker getLogMaker() {
        StatusBarNotification statusBarNotification = this.mSbn;
        if (statusBarNotification == null) {
            return new LogMaker(1621);
        }
        return statusBarNotification.getLogMaker().setCategory(1621);
    }

    private LogMaker importanceChangeLogMaker() {
        Integer num = this.mChosenImportance;
        return getLogMaker().setCategory(291).setType(4).setSubtype(Integer.valueOf(num != null ? num.intValue() : this.mStartingChannelImportance).intValue() - this.mStartingChannelImportance);
    }

    private LogMaker notificationControlsLogMaker() {
        return getLogMaker().setCategory(204).setType(1).setSubtype(this.mIsForBlockingHelper ? 1 : 0);
    }
}
