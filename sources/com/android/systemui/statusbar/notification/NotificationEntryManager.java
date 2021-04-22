package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.NotificationLifetimeExtender;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationRemoveInterceptor;
import com.android.systemui.statusbar.NotificationUiAdjustment;
import com.android.systemui.statusbar.NotificationUpdateHandler;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinder;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.NotificationContentInflater;
import com.android.systemui.statusbar.notification.stack.NotificationListContainer;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.util.leak.LeakDetector;
import com.sonymobile.lockscreen.notifications.SomcLockscreenNotifications;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NotificationEntryManager implements Dumpable, NotificationContentInflater.InflationCallback, NotificationUpdateHandler, VisualStabilityManager.Callback {
    private static final boolean DEBUG = Log.isLoggable("NotificationEntryMgr", 3);
    protected final Context mContext;
    private NotificationListenerService.RankingMap mLatestRankingMap;
    @VisibleForTesting
    protected NotificationData mNotificationData = new NotificationData();
    private final List<NotificationEntryListener> mNotificationEntryListeners = new ArrayList();
    @VisibleForTesting
    final ArrayList<NotificationLifetimeExtender> mNotificationLifetimeExtenders = new ArrayList<>();
    private NotificationRowBinder mNotificationRowBinder;
    @VisibleForTesting
    protected final HashMap<String, NotificationEntry> mPendingNotifications = new HashMap<>();
    private NotificationPresenter mPresenter;
    private NotificationRemoteInputManager mRemoteInputManager;
    private NotificationRemoveInterceptor mRemoveInterceptor;
    private final Map<NotificationEntry, NotificationLifetimeExtender> mRetainedNotifications = new ArrayMap();

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationEntryManager state:");
        printWriter.print("  mPendingNotifications=");
        if (this.mPendingNotifications.size() == 0) {
            printWriter.println("null");
        } else {
            for (NotificationEntry notificationEntry : this.mPendingNotifications.values()) {
                printWriter.println(notificationEntry.notification);
            }
        }
        printWriter.println("  Lifetime-extended notifications:");
        if (this.mRetainedNotifications.isEmpty()) {
            printWriter.println("    None");
            return;
        }
        for (Map.Entry<NotificationEntry, NotificationLifetimeExtender> entry : this.mRetainedNotifications.entrySet()) {
            printWriter.println("    " + entry.getKey().notification + " retained by " + entry.getValue().getClass().getName());
        }
    }

    public NotificationEntryManager(Context context) {
        this.mContext = context;
    }

    public void addNotificationEntryListener(NotificationEntryListener notificationEntryListener) {
        this.mNotificationEntryListeners.add(notificationEntryListener);
    }

    public void setNotificationRemoveInterceptor(NotificationRemoveInterceptor notificationRemoveInterceptor) {
        this.mRemoveInterceptor = notificationRemoveInterceptor;
    }

    private NotificationRemoteInputManager getRemoteInputManager() {
        if (this.mRemoteInputManager == null) {
            this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        }
        return this.mRemoteInputManager;
    }

    public void setRowBinder(NotificationRowBinder notificationRowBinder) {
        this.mNotificationRowBinder = notificationRowBinder;
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationListContainer notificationListContainer, HeadsUpManager headsUpManager) {
        this.mPresenter = notificationPresenter;
        this.mNotificationData.setHeadsUpManager(headsUpManager);
    }

    public void addNotificationLifetimeExtenders(List<NotificationLifetimeExtender> list) {
        for (NotificationLifetimeExtender notificationLifetimeExtender : list) {
            addNotificationLifetimeExtender(notificationLifetimeExtender);
        }
    }

    public void addNotificationLifetimeExtender(NotificationLifetimeExtender notificationLifetimeExtender) {
        this.mNotificationLifetimeExtenders.add(notificationLifetimeExtender);
        notificationLifetimeExtender.setCallback(new NotificationLifetimeExtender.NotificationSafeToRemoveCallback() {
            /* class com.android.systemui.statusbar.notification.$$Lambda$NotificationEntryManager$B9Rprc7VWCrqKYHxmFbKGPst6oI */

            @Override // com.android.systemui.statusbar.NotificationLifetimeExtender.NotificationSafeToRemoveCallback
            public final void onSafeToRemove(String str) {
                NotificationEntryManager.this.lambda$addNotificationLifetimeExtender$0$NotificationEntryManager(str);
            }
        });
    }

    public /* synthetic */ void lambda$addNotificationLifetimeExtender$0$NotificationEntryManager(String str) {
        removeNotification(str, this.mLatestRankingMap, 0);
    }

    public NotificationData getNotificationData() {
        return this.mNotificationData;
    }

    @Override // com.android.systemui.statusbar.notification.VisualStabilityManager.Callback
    public void onReorderingAllowed() {
        updateNotifications();
    }

    public void performRemoveNotification(StatusBarNotification statusBarNotification, int i) {
        removeNotificationInternal(statusBarNotification.getKey(), null, obtainVisibility(statusBarNotification.getKey()), false, true, i);
    }

    private NotificationVisibility obtainVisibility(String str) {
        return NotificationVisibility.obtain(str, this.mNotificationData.getRank(str), this.mNotificationData.getActiveNotifications().size(), true, NotificationLogger.getNotificationLocation(getNotificationData().get(str)));
    }

    private void abortExistingInflation(String str) {
        if (this.mPendingNotifications.containsKey(str)) {
            this.mPendingNotifications.get(str).abortTask();
            this.mPendingNotifications.remove(str);
        }
        NotificationEntry notificationEntry = this.mNotificationData.get(str);
        if (notificationEntry != null) {
            notificationEntry.abortTask();
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.InflationCallback
    public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
        removeNotificationInternal(statusBarNotification.getKey(), null, null, true, false, 4);
        for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
            notificationEntryListener.onInflationError(statusBarNotification, exc);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.InflationCallback
    public void onAsyncInflationFinished(NotificationEntry notificationEntry, int i) {
        this.mPendingNotifications.remove(notificationEntry.key);
        if (!notificationEntry.isRowRemoved()) {
            if (this.mNotificationData.get(notificationEntry.key) == null) {
                for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
                    notificationEntryListener.onEntryInflated(notificationEntry, i);
                }
                this.mNotificationData.add(notificationEntry);
                for (NotificationEntryListener notificationEntryListener2 : this.mNotificationEntryListeners) {
                    notificationEntryListener2.onBeforeNotificationAdded(notificationEntry);
                }
                updateNotifications();
                for (NotificationEntryListener notificationEntryListener3 : this.mNotificationEntryListeners) {
                    notificationEntryListener3.onNotificationAdded(notificationEntry);
                }
                return;
            }
            for (NotificationEntryListener notificationEntryListener4 : this.mNotificationEntryListeners) {
                notificationEntryListener4.onEntryReinflated(notificationEntry);
            }
        }
    }

    public void removeNotification(String str, NotificationListenerService.RankingMap rankingMap, int i) {
        removeNotificationInternal(str, rankingMap, obtainVisibility(str), false, false, i);
    }

    private void removeNotificationInternal(String str, NotificationListenerService.RankingMap rankingMap, NotificationVisibility notificationVisibility, boolean z, boolean z2, int i) {
        NotificationEntry notificationEntry;
        NotificationRemoveInterceptor notificationRemoveInterceptor = this.mRemoveInterceptor;
        if (notificationRemoveInterceptor == null || !notificationRemoveInterceptor.onNotificationRemoveRequested(str, i)) {
            NotificationEntry notificationEntry2 = this.mNotificationData.get(str);
            boolean z3 = false;
            if (notificationEntry2 == null && (notificationEntry = this.mPendingNotifications.get(str)) != null) {
                Iterator<NotificationLifetimeExtender> it = this.mNotificationLifetimeExtenders.iterator();
                while (it.hasNext()) {
                    NotificationLifetimeExtender next = it.next();
                    if (next.shouldExtendLifetimeForPendingNotification(notificationEntry)) {
                        extendLifetime(notificationEntry, next);
                        z3 = true;
                    }
                }
            }
            if (!z3) {
                abortExistingInflation(str);
            }
            if (notificationEntry2 != null) {
                boolean isRowDismissed = notificationEntry2.isRowDismissed();
                if (!z && !isRowDismissed) {
                    Iterator<NotificationLifetimeExtender> it2 = this.mNotificationLifetimeExtenders.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        NotificationLifetimeExtender next2 = it2.next();
                        if (next2.shouldExtendLifetime(notificationEntry2)) {
                            this.mLatestRankingMap = rankingMap;
                            extendLifetime(notificationEntry2, next2);
                            z3 = true;
                            break;
                        }
                    }
                }
                if (!z3) {
                    cancelLifetimeExtension(notificationEntry2);
                    if (notificationEntry2.rowExists()) {
                        notificationEntry2.removeRow();
                    }
                    handleGroupSummaryRemoved(str);
                    this.mNotificationData.remove(str, rankingMap);
                    updateNotifications();
                    ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(notificationEntry2);
                    boolean z4 = z2 | isRowDismissed;
                    for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
                        notificationEntryListener.onEntryRemoved(notificationEntry2, notificationVisibility, z4);
                    }
                }
            }
        }
    }

    private void handleGroupSummaryRemoved(String str) {
        List<NotificationEntry> children;
        NotificationEntry notificationEntry = this.mNotificationData.get(str);
        if (notificationEntry != null && notificationEntry.rowExists() && notificationEntry.isSummaryWithChildren()) {
            if ((notificationEntry.notification.getOverrideGroupKey() == null || notificationEntry.isRowDismissed()) && (children = notificationEntry.getChildren()) != null) {
                for (int i = 0; i < children.size(); i++) {
                    NotificationEntry notificationEntry2 = children.get(i);
                    boolean z = (notificationEntry.notification.getNotification().flags & 64) != 0;
                    boolean z2 = getRemoteInputManager().shouldKeepForRemoteInputHistory(notificationEntry2) || getRemoteInputManager().shouldKeepForSmartReplyHistory(notificationEntry2);
                    if (!z && !z2) {
                        notificationEntry2.setKeepInParent(true);
                        notificationEntry2.removeRow();
                    }
                }
            }
        }
    }

    private void addNotificationInternal(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) throws InflationException {
        String key = statusBarNotification.getKey();
        if (DEBUG) {
            Log.d("NotificationEntryMgr", "addNotification key=" + key);
        }
        this.mNotificationData.updateRanking(rankingMap);
        NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
        rankingMap.getRanking(key, ranking);
        NotificationEntry notificationEntry = new NotificationEntry(statusBarNotification, ranking);
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackInstance(notificationEntry);
        requireBinder().inflateViews(notificationEntry, new Runnable(statusBarNotification) {
            /* class com.android.systemui.statusbar.notification.$$Lambda$NotificationEntryManager$lOGPG9l6kx5UZEzr26g7h2LQR6w */
            private final /* synthetic */ StatusBarNotification f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NotificationEntryManager.this.lambda$addNotificationInternal$1$NotificationEntryManager(this.f$1);
            }
        });
        abortExistingInflation(key);
        this.mPendingNotifications.put(key, notificationEntry);
        for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
            notificationEntryListener.onPendingEntryAdded(notificationEntry);
        }
    }

    public /* synthetic */ void lambda$addNotificationInternal$1$NotificationEntryManager(StatusBarNotification statusBarNotification) {
        performRemoveNotification(statusBarNotification, 2);
    }

    public void addNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        try {
            addNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    private void updateNotificationInternal(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) throws InflationException {
        if (DEBUG) {
            Log.d("NotificationEntryMgr", "updateNotification(" + statusBarNotification + ")");
        }
        String key = statusBarNotification.getKey();
        abortExistingInflation(key);
        NotificationEntry notificationEntry = this.mNotificationData.get(key);
        if (notificationEntry != null) {
            cancelLifetimeExtension(notificationEntry);
            this.mNotificationData.update(notificationEntry, rankingMap, statusBarNotification);
            for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
                notificationEntryListener.onPreEntryUpdated(notificationEntry);
            }
            requireBinder().inflateViews(notificationEntry, new Runnable(statusBarNotification) {
                /* class com.android.systemui.statusbar.notification.$$Lambda$NotificationEntryManager$RJEcTAo4cuGvAgvl2zrMgzSF4kM */
                private final /* synthetic */ StatusBarNotification f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    NotificationEntryManager.this.lambda$updateNotificationInternal$2$NotificationEntryManager(this.f$1);
                }
            });
            updateNotifications();
            if (DEBUG) {
                boolean isNotificationForCurrentProfiles = ((NotificationData.KeyguardEnvironment) Dependency.get(NotificationData.KeyguardEnvironment.class)).isNotificationForCurrentProfiles(statusBarNotification);
                StringBuilder sb = new StringBuilder();
                sb.append("notification is ");
                sb.append(isNotificationForCurrentProfiles ? "" : "not ");
                sb.append("for you");
                Log.d("NotificationEntryMgr", sb.toString());
            }
            for (NotificationEntryListener notificationEntryListener2 : this.mNotificationEntryListeners) {
                notificationEntryListener2.onPostEntryUpdated(notificationEntry);
            }
        }
    }

    public /* synthetic */ void lambda$updateNotificationInternal$2$NotificationEntryManager(StatusBarNotification statusBarNotification) {
        performRemoveNotification(statusBarNotification, 2);
    }

    public void updateNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        try {
            updateNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    public void updateNotifications() {
        this.mNotificationData.filterAndSort(SomcLockscreenNotifications.shouldFilterOutOldNotifications(this.mContext));
        NotificationPresenter notificationPresenter = this.mPresenter;
        if (notificationPresenter != null) {
            notificationPresenter.updateNotificationViews();
        }
    }

    public void updateNotificationRanking(NotificationListenerService.RankingMap rankingMap) {
        ArrayList<NotificationEntry> arrayList = new ArrayList();
        arrayList.addAll(this.mNotificationData.getActiveNotifications());
        arrayList.addAll(this.mPendingNotifications.values());
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        for (NotificationEntry notificationEntry : arrayList) {
            arrayMap.put(notificationEntry.key, NotificationUiAdjustment.extractFromNotificationEntry(notificationEntry));
            arrayMap2.put(notificationEntry.key, Integer.valueOf(notificationEntry.importance));
        }
        this.mNotificationData.updateRanking(rankingMap);
        updateRankingOfPendingNotifications(rankingMap);
        for (NotificationEntry notificationEntry2 : arrayList) {
            requireBinder().onNotificationRankingUpdated(notificationEntry2, (Integer) arrayMap2.get(notificationEntry2.key), (NotificationUiAdjustment) arrayMap.get(notificationEntry2.key), NotificationUiAdjustment.extractFromNotificationEntry(notificationEntry2));
        }
        updateNotifications();
        for (NotificationEntryListener notificationEntryListener : this.mNotificationEntryListeners) {
            notificationEntryListener.onNotificationRankingUpdated(rankingMap);
        }
    }

    private void updateRankingOfPendingNotifications(NotificationListenerService.RankingMap rankingMap) {
        if (rankingMap != null) {
            NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
            for (NotificationEntry notificationEntry : this.mPendingNotifications.values()) {
                rankingMap.getRanking(notificationEntry.key, ranking);
                notificationEntry.populateFromRanking(ranking);
            }
        }
    }

    public Iterable<NotificationEntry> getPendingNotificationsIterator() {
        return this.mPendingNotifications.values();
    }

    private void extendLifetime(NotificationEntry notificationEntry, NotificationLifetimeExtender notificationLifetimeExtender) {
        NotificationLifetimeExtender notificationLifetimeExtender2 = this.mRetainedNotifications.get(notificationEntry);
        if (!(notificationLifetimeExtender2 == null || notificationLifetimeExtender2 == notificationLifetimeExtender)) {
            notificationLifetimeExtender2.setShouldManageLifetime(notificationEntry, false);
        }
        this.mRetainedNotifications.put(notificationEntry, notificationLifetimeExtender);
        notificationLifetimeExtender.setShouldManageLifetime(notificationEntry, true);
    }

    private void cancelLifetimeExtension(NotificationEntry notificationEntry) {
        NotificationLifetimeExtender remove = this.mRetainedNotifications.remove(notificationEntry);
        if (remove != null) {
            remove.setShouldManageLifetime(notificationEntry, false);
        }
    }

    private NotificationRowBinder requireBinder() {
        NotificationRowBinder notificationRowBinder = this.mNotificationRowBinder;
        if (notificationRowBinder != null) {
            return notificationRowBinder;
        }
        throw new RuntimeException("You must initialize NotificationEntryManager by callingsetRowBinder() before using.");
    }
}
