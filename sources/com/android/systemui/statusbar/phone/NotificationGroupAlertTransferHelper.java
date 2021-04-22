package com.android.systemui.statusbar.phone;

import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AlertingNotificationManager;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.ArrayList;
import java.util.Objects;

public class NotificationGroupAlertTransferHelper implements OnHeadsUpChangedListener, AmbientPulseManager.OnAmbientChangedListener, StatusBarStateController.StateListener {
    private final AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    private NotificationEntryManager mEntryManager;
    private final ArrayMap<String, GroupAlertEntry> mGroupAlertEntries = new ArrayMap<>();
    private final NotificationGroupManager mGroupManager = ((NotificationGroupManager) Dependency.get(NotificationGroupManager.class));
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsDozing;
    private final NotificationEntryListener mNotificationEntryListener = new NotificationEntryListener() {
        /* class com.android.systemui.statusbar.phone.NotificationGroupAlertTransferHelper.AnonymousClass2 */

        @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
        public void onPendingEntryAdded(NotificationEntry notificationEntry) {
            GroupAlertEntry groupAlertEntry = (GroupAlertEntry) NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.get(NotificationGroupAlertTransferHelper.this.mGroupManager.getGroupKey(notificationEntry.notification));
            if (groupAlertEntry != null) {
                NotificationGroupAlertTransferHelper.this.checkShouldTransferBack(groupAlertEntry);
            }
        }

        @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
        public void onEntryReinflated(NotificationEntry notificationEntry) {
            PendingAlertInfo pendingAlertInfo = (PendingAlertInfo) NotificationGroupAlertTransferHelper.this.mPendingAlerts.remove(notificationEntry.key);
            if (pendingAlertInfo == null) {
                return;
            }
            if (pendingAlertInfo.isStillValid()) {
                NotificationGroupAlertTransferHelper notificationGroupAlertTransferHelper = NotificationGroupAlertTransferHelper.this;
                notificationGroupAlertTransferHelper.alertNotificationWhenPossible(notificationEntry, notificationGroupAlertTransferHelper.getActiveAlertManager());
                return;
            }
            notificationEntry.getRow().freeContentViewWhenSafe(pendingAlertInfo.mAlertManager.getContentFlag());
        }

        @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
        public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
            NotificationGroupAlertTransferHelper.this.mPendingAlerts.remove(notificationEntry.key);
        }
    };
    private final NotificationGroupManager.OnGroupChangeListener mOnGroupChangeListener = new NotificationGroupManager.OnGroupChangeListener() {
        /* class com.android.systemui.statusbar.phone.NotificationGroupAlertTransferHelper.AnonymousClass1 */

        @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
        public void onGroupCreated(NotificationGroupManager.NotificationGroup notificationGroup, String str) {
            NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.put(str, new GroupAlertEntry(notificationGroup));
        }

        @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
        public void onGroupRemoved(NotificationGroupManager.NotificationGroup notificationGroup, String str) {
            NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.remove(str);
        }

        @Override // com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener
        public void onGroupSuppressionChanged(NotificationGroupManager.NotificationGroup notificationGroup, boolean z) {
            AlertingNotificationManager activeAlertManager = NotificationGroupAlertTransferHelper.this.getActiveAlertManager();
            if (z) {
                if (activeAlertManager.isAlerting(notificationGroup.summary.key)) {
                    NotificationGroupAlertTransferHelper.this.handleSuppressedSummaryAlerted(notificationGroup.summary, activeAlertManager);
                }
            } else if (notificationGroup.summary != null) {
                GroupAlertEntry groupAlertEntry = (GroupAlertEntry) NotificationGroupAlertTransferHelper.this.mGroupAlertEntries.get(NotificationGroupAlertTransferHelper.this.mGroupManager.getGroupKey(notificationGroup.summary.notification));
                if (groupAlertEntry.mAlertSummaryOnNextAddition) {
                    if (!activeAlertManager.isAlerting(notificationGroup.summary.key)) {
                        NotificationGroupAlertTransferHelper.this.alertNotificationWhenPossible(notificationGroup.summary, activeAlertManager);
                    }
                    groupAlertEntry.mAlertSummaryOnNextAddition = false;
                    return;
                }
                NotificationGroupAlertTransferHelper.this.checkShouldTransferBack(groupAlertEntry);
            }
        }
    };
    private final ArrayMap<String, PendingAlertInfo> mPendingAlerts = new ArrayMap<>();

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
    }

    public NotificationGroupAlertTransferHelper() {
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public void bind(NotificationEntryManager notificationEntryManager, NotificationGroupManager notificationGroupManager) {
        if (this.mEntryManager == null) {
            this.mEntryManager = notificationEntryManager;
            this.mEntryManager.addNotificationEntryListener(this.mNotificationEntryListener);
            notificationGroupManager.addOnGroupChangeListener(this.mOnGroupChangeListener);
            return;
        }
        throw new IllegalStateException("Already bound.");
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onDozingChanged(boolean z) {
        if (this.mIsDozing != z) {
            for (GroupAlertEntry groupAlertEntry : this.mGroupAlertEntries.values()) {
                groupAlertEntry.mLastAlertTransferTime = 0;
                groupAlertEntry.mAlertSummaryOnNextAddition = false;
            }
        }
        this.mIsDozing = z;
    }

    @Override // com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener
    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z, this.mAmbientPulseManager);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z, this.mHeadsUpManager);
    }

    private void onAlertStateChanged(NotificationEntry notificationEntry, boolean z, AlertingNotificationManager alertingNotificationManager) {
        if (z && this.mGroupManager.isSummaryOfSuppressedGroup(notificationEntry.notification)) {
            handleSuppressedSummaryAlerted(notificationEntry, alertingNotificationManager);
        }
    }

    private int getPendingChildrenNotAlerting(NotificationGroupManager.NotificationGroup notificationGroup) {
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        int i = 0;
        if (notificationEntryManager == null) {
            return 0;
        }
        for (NotificationEntry notificationEntry : notificationEntryManager.getPendingNotificationsIterator()) {
            if (isPendingNotificationInGroup(notificationEntry, notificationGroup) && onlySummaryAlerts(notificationEntry)) {
                i++;
            }
        }
        return i;
    }

    private boolean pendingInflationsWillAddChildren(NotificationGroupManager.NotificationGroup notificationGroup) {
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        if (notificationEntryManager == null) {
            return false;
        }
        for (NotificationEntry notificationEntry : notificationEntryManager.getPendingNotificationsIterator()) {
            if (isPendingNotificationInGroup(notificationEntry, notificationGroup)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPendingNotificationInGroup(NotificationEntry notificationEntry, NotificationGroupManager.NotificationGroup notificationGroup) {
        return this.mGroupManager.isGroupChild(notificationEntry.notification) && Objects.equals(this.mGroupManager.getGroupKey(notificationEntry.notification), this.mGroupManager.getGroupKey(notificationGroup.summary.notification)) && !notificationGroup.children.containsKey(notificationEntry.key);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleSuppressedSummaryAlerted(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
        NotificationEntry next;
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        GroupAlertEntry groupAlertEntry = this.mGroupAlertEntries.get(this.mGroupManager.getGroupKey(statusBarNotification));
        if (this.mGroupManager.isSummaryOfSuppressedGroup(notificationEntry.notification) && alertingNotificationManager.isAlerting(statusBarNotification.getKey()) && groupAlertEntry != null && !pendingInflationsWillAddChildren(groupAlertEntry.mGroup) && (next = this.mGroupManager.getLogicalChildren(notificationEntry.notification).iterator().next()) != null && !next.getRow().keepInParent() && !next.isRowRemoved() && !next.isRowDismissed()) {
            if (!alertingNotificationManager.isAlerting(next.key) && onlySummaryAlerts(notificationEntry)) {
                groupAlertEntry.mLastAlertTransferTime = SystemClock.elapsedRealtime();
            }
            transferAlertState(notificationEntry, next, alertingNotificationManager);
        }
    }

    private void transferAlertState(NotificationEntry notificationEntry, NotificationEntry notificationEntry2, AlertingNotificationManager alertingNotificationManager) {
        alertingNotificationManager.removeNotification(notificationEntry.key, true);
        alertNotificationWhenPossible(notificationEntry2, alertingNotificationManager);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void checkShouldTransferBack(GroupAlertEntry groupAlertEntry) {
        ArrayList<NotificationEntry> logicalChildren;
        int pendingChildrenNotAlerting;
        int size;
        if (SystemClock.elapsedRealtime() - groupAlertEntry.mLastAlertTransferTime < 300) {
            NotificationEntry notificationEntry = groupAlertEntry.mGroup.summary;
            AlertingNotificationManager activeAlertManager = getActiveAlertManager();
            if (onlySummaryAlerts(notificationEntry) && (size = (logicalChildren = this.mGroupManager.getLogicalChildren(notificationEntry.notification)).size() + (pendingChildrenNotAlerting = getPendingChildrenNotAlerting(groupAlertEntry.mGroup))) > 1) {
                boolean z = false;
                boolean z2 = false;
                for (int i = 0; i < logicalChildren.size(); i++) {
                    NotificationEntry notificationEntry2 = logicalChildren.get(i);
                    if (onlySummaryAlerts(notificationEntry2) && activeAlertManager.isAlerting(notificationEntry2.key)) {
                        activeAlertManager.removeNotification(notificationEntry2.key, true);
                        z2 = true;
                    }
                    if (this.mPendingAlerts.containsKey(notificationEntry2.key)) {
                        this.mPendingAlerts.get(notificationEntry2.key).mAbortOnInflation = true;
                        z2 = true;
                    }
                }
                if (z2 && !activeAlertManager.isAlerting(notificationEntry.key)) {
                    if (size - pendingChildrenNotAlerting > 1) {
                        z = true;
                    }
                    if (z) {
                        alertNotificationWhenPossible(notificationEntry, activeAlertManager);
                    } else {
                        groupAlertEntry.mAlertSummaryOnNextAddition = true;
                    }
                    groupAlertEntry.mLastAlertTransferTime = 0;
                }
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void alertNotificationWhenPossible(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
        int contentFlag = alertingNotificationManager.getContentFlag();
        if (!notificationEntry.getRow().isInflationFlagSet(contentFlag)) {
            this.mPendingAlerts.put(notificationEntry.key, new PendingAlertInfo(notificationEntry, alertingNotificationManager));
            notificationEntry.getRow().updateInflationFlag(contentFlag, true);
            notificationEntry.getRow().inflateViews();
        } else if (alertingNotificationManager.isAlerting(notificationEntry.key)) {
            alertingNotificationManager.updateNotification(notificationEntry.key, true);
        } else {
            alertingNotificationManager.showNotification(notificationEntry);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private AlertingNotificationManager getActiveAlertManager() {
        return this.mIsDozing ? this.mAmbientPulseManager : this.mHeadsUpManager;
    }

    private boolean onlySummaryAlerts(NotificationEntry notificationEntry) {
        return notificationEntry.notification.getNotification().getGroupAlertBehavior() == 1;
    }

    /* access modifiers changed from: private */
    public class PendingAlertInfo {
        boolean mAbortOnInflation;
        final AlertingNotificationManager mAlertManager;
        final NotificationEntry mEntry;
        final StatusBarNotification mOriginalNotification;

        PendingAlertInfo(NotificationEntry notificationEntry, AlertingNotificationManager alertingNotificationManager) {
            this.mOriginalNotification = notificationEntry.notification;
            this.mEntry = notificationEntry;
            this.mAlertManager = alertingNotificationManager;
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean isStillValid() {
            if (!this.mAbortOnInflation && this.mAlertManager == NotificationGroupAlertTransferHelper.this.getActiveAlertManager() && this.mEntry.notification.getGroupKey() == this.mOriginalNotification.getGroupKey() && this.mEntry.notification.getNotification().isGroupSummary() == this.mOriginalNotification.getNotification().isGroupSummary()) {
                return true;
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public static class GroupAlertEntry {
        boolean mAlertSummaryOnNextAddition;
        final NotificationGroupManager.NotificationGroup mGroup;
        long mLastAlertTransferTime;

        GroupAlertEntry(NotificationGroupManager.NotificationGroup notificationGroup) {
            this.mGroup = notificationGroup;
        }
    }
}
