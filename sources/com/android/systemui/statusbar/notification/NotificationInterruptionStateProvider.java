package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.policy.HeadsUpManager;

public class NotificationInterruptionStateProvider {
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private final Context mContext;
    private boolean mDisableNotificationAlerts;
    private final IDreamManager mDreamManager;
    private HeadsUpManager mHeadsUpManager;
    private ContentObserver mHeadsUpObserver;
    private HeadsUpSuppressor mHeadsUpSuppressor;
    private final NotificationFilter mNotificationFilter;
    private final PowerManager mPowerManager;
    private NotificationPresenter mPresenter;
    private ShadeController mShadeController;
    private final StatusBarStateController mStatusBarStateController;
    @VisibleForTesting
    protected boolean mUseHeadsUp;

    public interface HeadsUpSuppressor {
        boolean canHeadsUp(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification);
    }

    public NotificationInterruptionStateProvider(Context context) {
        this(context, (PowerManager) context.getSystemService("power"), IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams")), new AmbientDisplayConfiguration(context));
    }

    @VisibleForTesting
    protected NotificationInterruptionStateProvider(Context context, PowerManager powerManager, IDreamManager iDreamManager, AmbientDisplayConfiguration ambientDisplayConfiguration) {
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        this.mNotificationFilter = (NotificationFilter) Dependency.get(NotificationFilter.class);
        this.mUseHeadsUp = false;
        this.mContext = context;
        this.mPowerManager = powerManager;
        this.mDreamManager = iDreamManager;
        this.mAmbientDisplayConfiguration = ambientDisplayConfiguration;
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, HeadsUpManager headsUpManager, HeadsUpSuppressor headsUpSuppressor) {
        this.mPresenter = notificationPresenter;
        this.mHeadsUpManager = headsUpManager;
        this.mHeadsUpSuppressor = headsUpSuppressor;
        this.mHeadsUpObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            /* class com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider.AnonymousClass1 */

            public void onChange(boolean z) {
                NotificationInterruptionStateProvider notificationInterruptionStateProvider = NotificationInterruptionStateProvider.this;
                boolean z2 = notificationInterruptionStateProvider.mUseHeadsUp;
                boolean z3 = false;
                if (!notificationInterruptionStateProvider.mDisableNotificationAlerts && Settings.Global.getInt(NotificationInterruptionStateProvider.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) != 0) {
                    z3 = true;
                }
                notificationInterruptionStateProvider.mUseHeadsUp = z3;
                StringBuilder sb = new StringBuilder();
                sb.append("heads up is ");
                sb.append(NotificationInterruptionStateProvider.this.mUseHeadsUp ? "enabled" : "disabled");
                Log.d("InterruptionStateProvider", sb.toString());
                boolean z4 = NotificationInterruptionStateProvider.this.mUseHeadsUp;
                if (z2 != z4 && !z4) {
                    Log.d("InterruptionStateProvider", "dismissing any existing heads up notification on disable event");
                    NotificationInterruptionStateProvider.this.mHeadsUpManager.releaseAllImmediately();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mHeadsUpObserver.onChange(true);
    }

    private ShadeController getShadeController() {
        if (this.mShadeController == null) {
            this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        }
        return this.mShadeController;
    }

    public boolean shouldBubbleUp(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (!notificationEntry.canBubble || !notificationEntry.isBubble()) {
            return false;
        }
        Notification notification = statusBarNotification.getNotification();
        if (notification.getBubbleMetadata() == null || notification.getBubbleMetadata().getIntent() == null || !canHeadsUpCommon(notificationEntry)) {
            return false;
        }
        return true;
    }

    public boolean shouldHeadsUp(NotificationEntry notificationEntry) {
        boolean z;
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (getShadeController().isDozing()) {
            return false;
        }
        boolean z2 = this.mStatusBarStateController.getState() == 0;
        if ((notificationEntry.isBubble() && z2) || !canAlertCommon(notificationEntry) || !canHeadsUpCommon(notificationEntry) || notificationEntry.importance < 4) {
            return false;
        }
        try {
            z = this.mDreamManager.isDreaming();
        } catch (RemoteException e) {
            Log.e("InterruptionStateProvider", "Failed to query dream manager.", e);
            z = false;
        }
        if ((this.mPowerManager.isScreenOn() && !z) && this.mHeadsUpSuppressor.canHeadsUp(notificationEntry, statusBarNotification)) {
            return true;
        }
        return false;
    }

    public boolean shouldPulse(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (!this.mAmbientDisplayConfiguration.pulseOnNotificationEnabled(-2) || !getShadeController().isDozing() || !canAlertCommon(notificationEntry) || notificationEntry.shouldSuppressAmbient() || notificationEntry.importance < 3) {
            return false;
        }
        Bundle bundle = statusBarNotification.getNotification().extras;
        CharSequence charSequence = bundle.getCharSequence("android.title");
        CharSequence charSequence2 = bundle.getCharSequence("android.text");
        if (!TextUtils.isEmpty(charSequence) || !TextUtils.isEmpty(charSequence2)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean canAlertCommon(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (this.mNotificationFilter.shouldFilterOut(notificationEntry)) {
            return false;
        }
        if (!statusBarNotification.isGroup() || !statusBarNotification.getNotification().suppressAlertingDueToGrouping()) {
            return true;
        }
        return false;
    }

    public boolean canHeadsUpCommon(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (!this.mUseHeadsUp || this.mPresenter.isDeviceInVrMode() || notificationEntry.shouldSuppressPeek() || isSnoozedPackage(statusBarNotification) || notificationEntry.hasJustLaunchedFullScreenIntent()) {
            return false;
        }
        return true;
    }

    private boolean isSnoozedPackage(StatusBarNotification statusBarNotification) {
        return this.mHeadsUpManager.isSnoozed(statusBarNotification.getPackageName());
    }

    public void setDisableNotificationAlerts(boolean z) {
        this.mDisableNotificationAlerts = z;
        this.mHeadsUpObserver.onChange(true);
    }
}
