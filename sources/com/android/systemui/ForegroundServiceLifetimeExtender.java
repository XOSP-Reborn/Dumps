package com.android.systemui;

import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.NotificationLifetimeExtender;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;

public class ForegroundServiceLifetimeExtender implements NotificationLifetimeExtender {
    @VisibleForTesting
    static final int MIN_FGS_TIME_MS = 5000;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ArraySet<NotificationEntry> mManagedEntries = new ArraySet<>();
    private NotificationLifetimeExtender.NotificationSafeToRemoveCallback mNotificationSafeToRemoveCallback;

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public void setCallback(NotificationLifetimeExtender.NotificationSafeToRemoveCallback notificationSafeToRemoveCallback) {
        this.mNotificationSafeToRemoveCallback = notificationSafeToRemoveCallback;
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public boolean shouldExtendLifetime(NotificationEntry notificationEntry) {
        if ((notificationEntry.notification.getNotification().flags & 64) != 0 && System.currentTimeMillis() - notificationEntry.notification.getPostTime() < 5000) {
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public boolean shouldExtendLifetimeForPendingNotification(NotificationEntry notificationEntry) {
        return shouldExtendLifetime(notificationEntry);
    }

    @Override // com.android.systemui.statusbar.NotificationLifetimeExtender
    public void setShouldManageLifetime(NotificationEntry notificationEntry, boolean z) {
        if (!z) {
            this.mManagedEntries.remove(notificationEntry);
            return;
        }
        this.mManagedEntries.add(notificationEntry);
        this.mHandler.postDelayed(new Runnable(notificationEntry) {
            /* class com.android.systemui.$$Lambda$ForegroundServiceLifetimeExtender$eZMtetouaKnxc7j2jqc6zpz_AA */
            private final /* synthetic */ NotificationEntry f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                ForegroundServiceLifetimeExtender.this.lambda$setShouldManageLifetime$0$ForegroundServiceLifetimeExtender(this.f$1);
            }
        }, 5000 - (System.currentTimeMillis() - notificationEntry.notification.getPostTime()));
    }

    public /* synthetic */ void lambda$setShouldManageLifetime$0$ForegroundServiceLifetimeExtender(NotificationEntry notificationEntry) {
        if (this.mManagedEntries.contains(notificationEntry)) {
            this.mManagedEntries.remove(notificationEntry);
            NotificationLifetimeExtender.NotificationSafeToRemoveCallback notificationSafeToRemoveCallback = this.mNotificationSafeToRemoveCallback;
            if (notificationSafeToRemoveCallback != null) {
                notificationSafeToRemoveCallback.onSafeToRemove(notificationEntry.key);
            }
        }
    }
}
