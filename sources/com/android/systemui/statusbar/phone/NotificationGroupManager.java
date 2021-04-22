package com.android.systemui.statusbar.phone;

import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class NotificationGroupManager implements OnHeadsUpChangedListener, AmbientPulseManager.OnAmbientChangedListener, StatusBarStateController.StateListener {
    private AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    private int mBarState = -1;
    private final HashMap<String, NotificationGroup> mGroupMap = new HashMap<>();
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsUpdatingUnchangedGroup;
    private HashMap<String, StatusBarNotification> mIsolatedEntries = new HashMap<>();
    private final ArraySet<OnGroupChangeListener> mListeners = new ArraySet<>();

    public interface OnGroupChangeListener {
        default void onGroupCreated(NotificationGroup notificationGroup, String str) {
        }

        default void onGroupCreatedFromChildren(NotificationGroup notificationGroup) {
        }

        default void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        }

        default void onGroupRemoved(NotificationGroup notificationGroup, String str) {
        }

        default void onGroupSuppressionChanged(NotificationGroup notificationGroup, boolean z) {
        }

        default void onGroupsChanged() {
        }
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpPinnedModeChanged(boolean z) {
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
    }

    public NotificationGroupManager() {
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public void addOnGroupChangeListener(OnGroupChangeListener onGroupChangeListener) {
        this.mListeners.add(onGroupChangeListener);
    }

    public boolean isGroupExpanded(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup == null) {
            return false;
        }
        return notificationGroup.expanded;
    }

    public void setGroupExpanded(StatusBarNotification statusBarNotification, boolean z) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup != null) {
            setGroupExpanded(notificationGroup, z);
        }
    }

    private void setGroupExpanded(NotificationGroup notificationGroup, boolean z) {
        notificationGroup.expanded = z;
        if (notificationGroup.summary != null) {
            Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onGroupExpansionChanged(notificationGroup.summary.getRow(), z);
            }
        }
    }

    public void onEntryRemoved(NotificationEntry notificationEntry) {
        onEntryRemovedInternal(notificationEntry, notificationEntry.notification);
        this.mIsolatedEntries.remove(notificationEntry.key);
    }

    private void onEntryRemovedInternal(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification) {
        String groupKey = getGroupKey(statusBarNotification);
        NotificationGroup notificationGroup = this.mGroupMap.get(groupKey);
        if (notificationGroup != null) {
            if (isGroupChild(statusBarNotification)) {
                notificationGroup.children.remove(notificationEntry.key);
            } else {
                notificationGroup.summary = null;
            }
            updateSuppression(notificationGroup);
            if (notificationGroup.children.isEmpty() && notificationGroup.summary == null) {
                this.mGroupMap.remove(groupKey);
                Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    it.next().onGroupRemoved(notificationGroup, groupKey);
                }
            }
        }
    }

    public void onEntryAdded(NotificationEntry notificationEntry) {
        String str;
        if (notificationEntry.isRowRemoved()) {
            notificationEntry.setDebugThrowable(new Throwable());
        }
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        boolean isGroupChild = isGroupChild(statusBarNotification);
        String groupKey = getGroupKey(statusBarNotification);
        NotificationGroup notificationGroup = this.mGroupMap.get(groupKey);
        if (notificationGroup == null) {
            notificationGroup = new NotificationGroup();
            this.mGroupMap.put(groupKey, notificationGroup);
            Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onGroupCreated(notificationGroup, groupKey);
            }
        }
        if (isGroupChild) {
            NotificationEntry notificationEntry2 = notificationGroup.children.get(notificationEntry.key);
            if (!(notificationEntry2 == null || notificationEntry2 == notificationEntry)) {
                Throwable debugThrowable = notificationEntry2.getDebugThrowable();
                StringBuilder sb = new StringBuilder();
                sb.append("Inconsistent entries found with the same key ");
                sb.append(notificationEntry.key);
                sb.append("existing removed: ");
                sb.append(notificationEntry2.isRowRemoved());
                if (debugThrowable != null) {
                    str = Log.getStackTraceString(debugThrowable) + "\n";
                } else {
                    str = "";
                }
                sb.append(str);
                sb.append(" added removed");
                sb.append(notificationEntry.isRowRemoved());
                Log.wtf("NotificationGroupManager", sb.toString(), new Throwable());
            }
            notificationGroup.children.put(notificationEntry.key, notificationEntry);
            updateSuppression(notificationGroup);
            return;
        }
        notificationGroup.summary = notificationEntry;
        notificationGroup.expanded = notificationEntry.areChildrenExpanded();
        updateSuppression(notificationGroup);
        if (!notificationGroup.children.isEmpty()) {
            Iterator it2 = new ArrayList(notificationGroup.children.values()).iterator();
            while (it2.hasNext()) {
                onEntryBecomingChild((NotificationEntry) it2.next());
            }
            Iterator<OnGroupChangeListener> it3 = this.mListeners.iterator();
            while (it3.hasNext()) {
                it3.next().onGroupCreatedFromChildren(notificationGroup);
            }
        }
    }

    private void onEntryBecomingChild(NotificationEntry notificationEntry) {
        if (shouldIsolate(notificationEntry)) {
            isolateNotification(notificationEntry);
        }
    }

    private void updateSuppression(NotificationGroup notificationGroup) {
        if (notificationGroup != null) {
            boolean z = notificationGroup.suppressed;
            boolean z2 = true;
            if (notificationGroup.summary == null || notificationGroup.expanded || (notificationGroup.children.size() != 1 && (notificationGroup.children.size() != 0 || !notificationGroup.summary.notification.getNotification().isGroupSummary() || !hasIsolatedChildren(notificationGroup)))) {
                z2 = false;
            }
            notificationGroup.suppressed = z2;
            if (z != notificationGroup.suppressed) {
                Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
                while (it.hasNext()) {
                    OnGroupChangeListener next = it.next();
                    if (!this.mIsUpdatingUnchangedGroup) {
                        next.onGroupSuppressionChanged(notificationGroup, notificationGroup.suppressed);
                        next.onGroupsChanged();
                    }
                }
            }
        }
    }

    private boolean hasIsolatedChildren(NotificationGroup notificationGroup) {
        return getNumberOfIsolatedChildren(notificationGroup.summary.notification.getGroupKey()) != 0;
    }

    private int getNumberOfIsolatedChildren(String str) {
        int i = 0;
        for (StatusBarNotification statusBarNotification : this.mIsolatedEntries.values()) {
            if (statusBarNotification.getGroupKey().equals(str) && isIsolated(statusBarNotification)) {
                i++;
            }
        }
        return i;
    }

    private NotificationEntry getIsolatedChild(String str) {
        for (StatusBarNotification statusBarNotification : this.mIsolatedEntries.values()) {
            if (statusBarNotification.getGroupKey().equals(str) && isIsolated(statusBarNotification)) {
                return this.mGroupMap.get(statusBarNotification.getKey()).summary;
            }
        }
        return null;
    }

    public void onEntryUpdated(NotificationEntry notificationEntry, StatusBarNotification statusBarNotification) {
        String groupKey = statusBarNotification.getGroupKey();
        String groupKey2 = notificationEntry.notification.getGroupKey();
        boolean z = true;
        boolean z2 = !groupKey.equals(groupKey2);
        boolean isGroupChild = isGroupChild(statusBarNotification);
        boolean isGroupChild2 = isGroupChild(notificationEntry.notification);
        if (z2 || isGroupChild != isGroupChild2) {
            z = false;
        }
        this.mIsUpdatingUnchangedGroup = z;
        if (this.mGroupMap.get(getGroupKey(statusBarNotification)) != null) {
            onEntryRemovedInternal(notificationEntry, statusBarNotification);
        }
        onEntryAdded(notificationEntry);
        this.mIsUpdatingUnchangedGroup = false;
        if (isIsolated(notificationEntry.notification)) {
            this.mIsolatedEntries.put(notificationEntry.key, notificationEntry.notification);
            if (z2) {
                updateSuppression(this.mGroupMap.get(groupKey));
                updateSuppression(this.mGroupMap.get(groupKey2));
            }
        } else if (!isGroupChild && isGroupChild2) {
            onEntryBecomingChild(notificationEntry);
        }
    }

    public boolean isSummaryOfSuppressedGroup(StatusBarNotification statusBarNotification) {
        return isGroupSuppressed(getGroupKey(statusBarNotification)) && statusBarNotification.getNotification().isGroupSummary();
    }

    private boolean isOnlyChild(StatusBarNotification statusBarNotification) {
        if (statusBarNotification.getNotification().isGroupSummary() || getTotalNumberOfChildren(statusBarNotification) != 1) {
            return false;
        }
        return true;
    }

    public boolean isOnlyChildInGroup(StatusBarNotification statusBarNotification) {
        NotificationEntry logicalGroupSummary;
        if (isOnlyChild(statusBarNotification) && (logicalGroupSummary = getLogicalGroupSummary(statusBarNotification)) != null && !logicalGroupSummary.notification.equals(statusBarNotification)) {
            return true;
        }
        return false;
    }

    private int getTotalNumberOfChildren(StatusBarNotification statusBarNotification) {
        int numberOfIsolatedChildren = getNumberOfIsolatedChildren(statusBarNotification.getGroupKey());
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        return numberOfIsolatedChildren + (notificationGroup != null ? notificationGroup.children.size() : 0);
    }

    private boolean isGroupSuppressed(String str) {
        NotificationGroup notificationGroup = this.mGroupMap.get(str);
        return notificationGroup != null && notificationGroup.suppressed;
    }

    private void setStatusBarState(int i) {
        this.mBarState = i;
        if (this.mBarState == 1) {
            collapseAllGroups();
        }
    }

    public void collapseAllGroups() {
        ArrayList arrayList = new ArrayList(this.mGroupMap.values());
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            NotificationGroup notificationGroup = (NotificationGroup) arrayList.get(i);
            if (notificationGroup.expanded) {
                setGroupExpanded(notificationGroup, false);
            }
            updateSuppression(notificationGroup);
        }
    }

    public boolean isChildInGroupWithSummary(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup;
        if (isGroupChild(statusBarNotification) && (notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification))) != null && notificationGroup.summary != null && !notificationGroup.suppressed && !notificationGroup.children.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isSummaryOfGroup(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup;
        if (isGroupSummary(statusBarNotification) && (notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification))) != null && notificationGroup.summary != null && !notificationGroup.children.isEmpty() && Objects.equals(notificationGroup.summary.notification, statusBarNotification)) {
            return true;
        }
        return false;
    }

    public NotificationEntry getGroupSummary(StatusBarNotification statusBarNotification) {
        return getGroupSummary(getGroupKey(statusBarNotification));
    }

    public NotificationEntry getLogicalGroupSummary(StatusBarNotification statusBarNotification) {
        return getGroupSummary(statusBarNotification.getGroupKey());
    }

    private NotificationEntry getGroupSummary(String str) {
        NotificationEntry notificationEntry;
        NotificationGroup notificationGroup = this.mGroupMap.get(str);
        if (notificationGroup == null || (notificationEntry = notificationGroup.summary) == null) {
            return null;
        }
        return notificationEntry;
    }

    public ArrayList<NotificationEntry> getLogicalChildren(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        if (notificationGroup == null) {
            return null;
        }
        ArrayList<NotificationEntry> arrayList = new ArrayList<>(notificationGroup.children.values());
        NotificationEntry isolatedChild = getIsolatedChild(statusBarNotification.getGroupKey());
        if (isolatedChild != null) {
            arrayList.add(isolatedChild);
        }
        return arrayList;
    }

    public String getGroupKey(StatusBarNotification statusBarNotification) {
        if (isIsolated(statusBarNotification)) {
            return statusBarNotification.getKey();
        }
        return statusBarNotification.getGroupKey();
    }

    public boolean toggleGroupExpansion(StatusBarNotification statusBarNotification) {
        NotificationGroup notificationGroup = this.mGroupMap.get(getGroupKey(statusBarNotification));
        if (notificationGroup == null) {
            return false;
        }
        setGroupExpanded(notificationGroup, !notificationGroup.expanded);
        return notificationGroup.expanded;
    }

    private boolean isIsolated(StatusBarNotification statusBarNotification) {
        return this.mIsolatedEntries.containsKey(statusBarNotification.getKey());
    }

    public boolean isGroupSummary(StatusBarNotification statusBarNotification) {
        if (isIsolated(statusBarNotification)) {
            return true;
        }
        return statusBarNotification.getNotification().isGroupSummary();
    }

    public boolean isGroupChild(StatusBarNotification statusBarNotification) {
        if (!isIsolated(statusBarNotification) && statusBarNotification.isGroup() && !statusBarNotification.getNotification().isGroupSummary()) {
            return true;
        }
        return false;
    }

    @Override // com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener
    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z);
    }

    @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        onAlertStateChanged(notificationEntry, z);
    }

    private void onAlertStateChanged(NotificationEntry notificationEntry, boolean z) {
        if (!z) {
            stopIsolatingNotification(notificationEntry);
        } else if (shouldIsolate(notificationEntry)) {
            isolateNotification(notificationEntry);
        }
    }

    private boolean shouldIsolate(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        NotificationGroup notificationGroup = this.mGroupMap.get(statusBarNotification.getGroupKey());
        if (!statusBarNotification.isGroup() || statusBarNotification.getNotification().isGroupSummary()) {
            return false;
        }
        if (!this.mHeadsUpManager.isAlerting(notificationEntry.key) && !this.mAmbientPulseManager.isAlerting(notificationEntry.key)) {
            return false;
        }
        if (statusBarNotification.getNotification().fullScreenIntent != null || notificationGroup == null || !notificationGroup.expanded || isGroupNotFullyVisible(notificationGroup)) {
            return true;
        }
        return false;
    }

    private void isolateNotification(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        onEntryRemovedInternal(notificationEntry, statusBarNotification);
        this.mIsolatedEntries.put(statusBarNotification.getKey(), statusBarNotification);
        onEntryAdded(notificationEntry);
        updateSuppression(this.mGroupMap.get(notificationEntry.notification.getGroupKey()));
        Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onGroupsChanged();
        }
    }

    private void stopIsolatingNotification(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        if (this.mIsolatedEntries.containsKey(statusBarNotification.getKey())) {
            onEntryRemovedInternal(notificationEntry, notificationEntry.notification);
            this.mIsolatedEntries.remove(statusBarNotification.getKey());
            onEntryAdded(notificationEntry);
            Iterator<OnGroupChangeListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onGroupsChanged();
            }
        }
    }

    private boolean isGroupNotFullyVisible(NotificationGroup notificationGroup) {
        NotificationEntry notificationEntry = notificationGroup.summary;
        return notificationEntry == null || notificationEntry.isGroupNotFullyVisible();
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("GroupManager state:");
        printWriter.println("  number of groups: " + this.mGroupMap.size());
        for (Map.Entry<String, NotificationGroup> entry : this.mGroupMap.entrySet()) {
            printWriter.println("\n    key: " + entry.getKey());
            printWriter.println(entry.getValue());
        }
        printWriter.println("\n    isolated entries: " + this.mIsolatedEntries.size());
        for (Map.Entry<String, StatusBarNotification> entry2 : this.mIsolatedEntries.entrySet()) {
            printWriter.print("      ");
            printWriter.print(entry2.getKey());
            printWriter.print(", ");
            printWriter.println(entry2.getValue());
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        setStatusBarState(i);
    }

    public static class NotificationGroup {
        public final HashMap<String, NotificationEntry> children = new HashMap<>();
        public boolean expanded;
        public NotificationEntry summary;
        public boolean suppressed;

        public String toString() {
            String str;
            String str2;
            StringBuilder sb = new StringBuilder();
            sb.append("    summary:\n      ");
            NotificationEntry notificationEntry = this.summary;
            sb.append(notificationEntry != null ? notificationEntry.notification : "null");
            NotificationEntry notificationEntry2 = this.summary;
            if (notificationEntry2 == null || notificationEntry2.getDebugThrowable() == null) {
                str = "";
            } else {
                str = Log.getStackTraceString(this.summary.getDebugThrowable());
            }
            sb.append(str);
            String str3 = sb.toString() + "\n    children size: " + this.children.size();
            for (NotificationEntry notificationEntry3 : this.children.values()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str3);
                sb2.append("\n      ");
                sb2.append(notificationEntry3.notification);
                if (notificationEntry3.getDebugThrowable() != null) {
                    str2 = Log.getStackTraceString(notificationEntry3.getDebugThrowable());
                } else {
                    str2 = "";
                }
                sb2.append(str2);
                str3 = sb2.toString();
            }
            return str3;
        }
    }
}
