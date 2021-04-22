package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.statusbar.AlertingNotificationManager;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import dagger.Lazy;

public class NotificationAlertingManager {
    private final AmbientPulseManager mAmbientPulseManager;
    private HeadsUpManager mHeadsUpManager;
    private final NotificationInterruptionStateProvider mNotificationInterruptionStateProvider;
    private final NotificationListener mNotificationListener;
    private final NotificationRemoteInputManager mRemoteInputManager;
    private final Lazy<ShadeController> mShadeController;
    private final VisualStabilityManager mVisualStabilityManager;

    public NotificationAlertingManager(NotificationEntryManager notificationEntryManager, AmbientPulseManager ambientPulseManager, NotificationRemoteInputManager notificationRemoteInputManager, VisualStabilityManager visualStabilityManager, Lazy<ShadeController> lazy, NotificationInterruptionStateProvider notificationInterruptionStateProvider, NotificationListener notificationListener) {
        this.mAmbientPulseManager = ambientPulseManager;
        this.mRemoteInputManager = notificationRemoteInputManager;
        this.mVisualStabilityManager = visualStabilityManager;
        this.mShadeController = lazy;
        this.mNotificationInterruptionStateProvider = notificationInterruptionStateProvider;
        this.mNotificationListener = notificationListener;
        notificationEntryManager.addNotificationEntryListener(new NotificationEntryListener() {
            /* class com.android.systemui.statusbar.notification.NotificationAlertingManager.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onEntryInflated(NotificationEntry notificationEntry, int i) {
                NotificationAlertingManager.this.showAlertingView(notificationEntry, i);
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPostEntryUpdated(NotificationEntry notificationEntry) {
                NotificationAlertingManager.this.updateAlertState(notificationEntry);
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
                NotificationAlertingManager.this.stopAlerting(notificationEntry.key);
            }
        });
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showAlertingView(NotificationEntry notificationEntry, int i) {
        if ((i & 4) != 0) {
            if (this.mNotificationInterruptionStateProvider.shouldHeadsUp(notificationEntry)) {
                this.mHeadsUpManager.showNotification(notificationEntry);
                setNotificationShown(notificationEntry.notification);
            } else {
                notificationEntry.freeContentViewWhenSafe(4);
            }
        }
        if ((i & 8) == 0) {
            return;
        }
        if (this.mNotificationInterruptionStateProvider.shouldPulse(notificationEntry)) {
            this.mAmbientPulseManager.showNotification(notificationEntry);
        } else {
            notificationEntry.freeContentViewWhenSafe(8);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateAlertState(NotificationEntry notificationEntry) {
        boolean z;
        AlertingNotificationManager alertingNotificationManager;
        boolean alertAgain = alertAgain(notificationEntry, notificationEntry.notification.getNotification());
        if (this.mShadeController.get().isDozing()) {
            alertingNotificationManager = this.mAmbientPulseManager;
            z = this.mNotificationInterruptionStateProvider.shouldPulse(notificationEntry);
        } else {
            alertingNotificationManager = this.mHeadsUpManager;
            z = this.mNotificationInterruptionStateProvider.shouldHeadsUp(notificationEntry);
        }
        if (alertingNotificationManager.isAlerting(notificationEntry.key)) {
            if (!z) {
                alertingNotificationManager.removeNotification(notificationEntry.key, false);
            } else {
                alertingNotificationManager.updateNotification(notificationEntry.key, alertAgain);
            }
        } else if (z && alertAgain) {
            alertingNotificationManager.showNotification(notificationEntry);
        }
    }

    public static boolean alertAgain(NotificationEntry notificationEntry, Notification notification) {
        return notificationEntry == null || !notificationEntry.hasInterrupted() || (notification.flags & 8) == 0;
    }

    private void setNotificationShown(StatusBarNotification statusBarNotification) {
        try {
            this.mNotificationListener.setNotificationsShown(new String[]{statusBarNotification.getKey()});
        } catch (RuntimeException e) {
            Log.d("NotifAlertManager", "failed setNotificationsShown: ", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopAlerting(String str) {
        if (this.mHeadsUpManager.isAlerting(str)) {
            this.mHeadsUpManager.removeNotification(str, (this.mRemoteInputManager.getController().isSpinning(str) && !NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY) || !this.mVisualStabilityManager.isReorderingAllowed());
        }
        if (this.mAmbientPulseManager.isAlerting(str)) {
            this.mAmbientPulseManager.removeNotification(str, false);
        }
    }
}
