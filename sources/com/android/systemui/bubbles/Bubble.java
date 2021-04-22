package com.android.systemui.bubbles;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0010R$layout;
import com.android.systemui.bubbles.BubbleExpandedView;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import java.util.Objects;

/* access modifiers changed from: package-private */
public class Bubble {
    public NotificationEntry entry;
    BubbleExpandedView expandedView;
    BubbleView iconView;
    private String mAppName;
    private final String mGroupId;
    private boolean mInflated;
    private final String mKey;
    private long mLastAccessed;
    private long mLastUpdated;
    private final BubbleExpandedView.OnBubbleBlockedListener mListener;
    private PackageManager mPm;

    public static String groupId(NotificationEntry notificationEntry) {
        UserHandle user = notificationEntry.notification.getUser();
        return user.getIdentifier() + "|" + notificationEntry.notification.getPackageName();
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    Bubble(Context context, NotificationEntry notificationEntry) {
        this(context, notificationEntry, null);
    }

    Bubble(Context context, NotificationEntry notificationEntry, BubbleExpandedView.OnBubbleBlockedListener onBubbleBlockedListener) {
        this.entry = notificationEntry;
        this.mKey = notificationEntry.key;
        this.mLastUpdated = notificationEntry.notification.getPostTime();
        this.mGroupId = groupId(notificationEntry);
        this.mListener = onBubbleBlockedListener;
        this.mPm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(this.entry.notification.getPackageName(), 795136);
            if (applicationInfo != null) {
                this.mAppName = String.valueOf(this.mPm.getApplicationLabel(applicationInfo));
            }
        } catch (PackageManager.NameNotFoundException unused) {
            this.mAppName = this.entry.notification.getPackageName();
        }
    }

    public String getKey() {
        return this.mKey;
    }

    public String getGroupId() {
        return this.mGroupId;
    }

    public String getAppName() {
        return this.mAppName;
    }

    public void updateDotVisibility() {
        BubbleView bubbleView = this.iconView;
        if (bubbleView != null) {
            bubbleView.updateDotVisibility(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void inflate(LayoutInflater layoutInflater, BubbleStackView bubbleStackView) {
        if (!this.mInflated) {
            this.iconView = (BubbleView) layoutInflater.inflate(C0010R$layout.bubble_view, (ViewGroup) bubbleStackView, false);
            this.iconView.setNotif(this.entry);
            this.expandedView = (BubbleExpandedView) layoutInflater.inflate(C0010R$layout.bubble_expanded_view, (ViewGroup) bubbleStackView, false);
            this.expandedView.setEntry(this.entry, bubbleStackView, this.mAppName);
            this.expandedView.setOnBlockedListener(this.mListener);
            this.mInflated = true;
        }
    }

    /* access modifiers changed from: package-private */
    public void setDismissed() {
        this.entry.setBubbleDismissed(true);
        BubbleExpandedView bubbleExpandedView = this.expandedView;
        if (bubbleExpandedView != null) {
            bubbleExpandedView.cleanUpExpandedState();
        }
    }

    /* access modifiers changed from: package-private */
    public void setEntry(NotificationEntry notificationEntry) {
        this.entry = notificationEntry;
        this.mLastUpdated = notificationEntry.notification.getPostTime();
        if (this.mInflated) {
            this.iconView.update(notificationEntry);
            this.expandedView.update(notificationEntry);
        }
    }

    public long getLastActivity() {
        return Math.max(this.mLastUpdated, this.mLastAccessed);
    }

    public long getLastUpdateTime() {
        return this.mLastUpdated;
    }

    /* access modifiers changed from: package-private */
    public void markAsAccessedAt(long j) {
        this.mLastAccessed = j;
        this.entry.setShowInShadeWhenBubble(false);
    }

    public boolean isOngoing() {
        return this.entry.isForegroundService();
    }

    public String toString() {
        return "Bubble{" + this.mKey + '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Bubble)) {
            return false;
        }
        return Objects.equals(this.mKey, ((Bubble) obj).mKey);
    }

    public int hashCode() {
        return Objects.hash(this.mKey);
    }
}
