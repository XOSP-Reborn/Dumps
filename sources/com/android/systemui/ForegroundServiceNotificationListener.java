package com.android.systemui;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;

public class ForegroundServiceNotificationListener {
    private final Context mContext;
    private final ForegroundServiceController mForegroundServiceController;

    public ForegroundServiceNotificationListener(Context context, ForegroundServiceController foregroundServiceController, NotificationEntryManager notificationEntryManager) {
        this.mContext = context;
        this.mForegroundServiceController = foregroundServiceController;
        notificationEntryManager.addNotificationEntryListener(new NotificationEntryListener() {
            /* class com.android.systemui.ForegroundServiceNotificationListener.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPendingEntryAdded(NotificationEntry notificationEntry) {
                ForegroundServiceNotificationListener.this.addNotification(notificationEntry.notification, notificationEntry.importance);
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPostEntryUpdated(NotificationEntry notificationEntry) {
                ForegroundServiceNotificationListener.this.updateNotification(notificationEntry.notification, notificationEntry.importance);
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
                ForegroundServiceNotificationListener.this.removeNotification(notificationEntry.notification);
            }
        });
        notificationEntryManager.addNotificationLifetimeExtender(new ForegroundServiceLifetimeExtender());
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void addNotification(StatusBarNotification statusBarNotification, int i) {
        updateNotification(statusBarNotification, i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void removeNotification(final StatusBarNotification statusBarNotification) {
        this.mForegroundServiceController.updateUserState(statusBarNotification.getUserId(), new ForegroundServiceController.UserStateUpdateCallback() {
            /* class com.android.systemui.ForegroundServiceNotificationListener.AnonymousClass2 */

            @Override // com.android.systemui.ForegroundServiceController.UserStateUpdateCallback
            public boolean updateUserState(ForegroundServicesUserState foregroundServicesUserState) {
                if (!ForegroundServiceNotificationListener.this.mForegroundServiceController.isDisclosureNotification(statusBarNotification)) {
                    return foregroundServicesUserState.removeNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                }
                foregroundServicesUserState.setRunningServices(null, 0);
                return true;
            }
        }, false);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateNotification(StatusBarNotification statusBarNotification, int i) {
        this.mForegroundServiceController.updateUserState(statusBarNotification.getUserId(), new ForegroundServiceController.UserStateUpdateCallback(statusBarNotification, i) {
            /* class com.android.systemui.$$Lambda$ForegroundServiceNotificationListener$AqXxERjDYAcDwpvtlCENdvbntCk */
            private final /* synthetic */ StatusBarNotification f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            @Override // com.android.systemui.ForegroundServiceController.UserStateUpdateCallback
            public final boolean updateUserState(ForegroundServicesUserState foregroundServicesUserState) {
                return ForegroundServiceNotificationListener.this.lambda$updateNotification$0$ForegroundServiceNotificationListener(this.f$1, this.f$2, foregroundServicesUserState);
            }
        }, true);
    }

    public /* synthetic */ boolean lambda$updateNotification$0$ForegroundServiceNotificationListener(StatusBarNotification statusBarNotification, int i, ForegroundServicesUserState foregroundServicesUserState) {
        if (this.mForegroundServiceController.isDisclosureNotification(statusBarNotification)) {
            Bundle bundle = statusBarNotification.getNotification().extras;
            if (bundle != null) {
                foregroundServicesUserState.setRunningServices(bundle.getStringArray("android.foregroundApps"), statusBarNotification.getNotification().when);
            }
        } else {
            foregroundServicesUserState.removeNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
            if ((statusBarNotification.getNotification().flags & 64) != 0) {
                if (i > 1) {
                    foregroundServicesUserState.addImportantNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                }
                if (Notification.Builder.recoverBuilder(this.mContext, statusBarNotification.getNotification()).usesStandardHeader()) {
                    foregroundServicesUserState.addStandardLayoutNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                }
            }
        }
        return true;
    }
}
