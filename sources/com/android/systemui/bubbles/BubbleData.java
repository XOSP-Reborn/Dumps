package com.android.systemui.bubbles;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.bubbles.BubbleExpandedView;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BubbleData {
    private static final Comparator<Bubble> BUBBLES_BY_SORT_KEY_DESCENDING = Comparator.comparing($$Lambda$BubbleData$vPZCImnk7rTPTX1c7nr0PX7FO2o.INSTANCE).reversed();
    private static final Comparator<Map.Entry<String, Long>> GROUPS_BY_MAX_SORT_KEY_DESCENDING = Comparator.comparing($$Lambda$JmVHPWbzq5woEs3Hauzhf2I3Jc.INSTANCE).reversed();
    private final List<Bubble> mBubbles;
    private final Context mContext;
    private boolean mExpanded;
    private Listener mListener;
    private Bubble mSelectedBubble;
    private Update mStateChange;
    private TimeSource mTimeSource = $$Lambda$0E0fwzH9SS6aB9lL5npMzupI4Q.INSTANCE;
    private NotificationListenerService.Ranking mTmpRanking;

    /* access modifiers changed from: package-private */
    public interface Listener {
        void applyUpdate(Update update);
    }

    /* access modifiers changed from: package-private */
    public interface TimeSource {
        long currentTimeMillis();
    }

    /* access modifiers changed from: package-private */
    public static final class Update {
        Bubble addedBubble;
        final List<Bubble> bubbles;
        boolean expanded;
        boolean expandedChanged;
        boolean orderChanged;
        final List<Pair<Bubble, Integer>> removedBubbles;
        Bubble selectedBubble;
        boolean selectionChanged;
        Bubble updatedBubble;

        private Update(List<Bubble> list) {
            this.removedBubbles = new ArrayList();
            this.bubbles = Collections.unmodifiableList(list);
        }

        /* access modifiers changed from: package-private */
        public boolean anythingChanged() {
            return this.expandedChanged || this.selectionChanged || this.addedBubble != null || this.updatedBubble != null || !this.removedBubbles.isEmpty() || this.orderChanged;
        }

        /* access modifiers changed from: package-private */
        public void bubbleRemoved(Bubble bubble, int i) {
            this.removedBubbles.add(new Pair<>(bubble, Integer.valueOf(i)));
        }
    }

    public BubbleData(Context context) {
        this.mContext = context;
        this.mBubbles = new ArrayList();
        this.mStateChange = new Update(this.mBubbles);
    }

    public boolean hasBubbles() {
        return !this.mBubbles.isEmpty();
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    public boolean hasBubbleWithKey(String str) {
        return getBubbleWithKey(str) != null;
    }

    public void setExpanded(boolean z) {
        setExpandedInternal(z);
        dispatchPendingChanges();
    }

    public void setSelectedBubble(Bubble bubble) {
        setSelectedBubbleInternal(bubble);
        dispatchPendingChanges();
    }

    public void notificationEntryUpdated(NotificationEntry notificationEntry) {
        Bubble bubbleWithKey = getBubbleWithKey(notificationEntry.key);
        if (bubbleWithKey == null) {
            bubbleWithKey = new Bubble(this.mContext, notificationEntry, new BubbleExpandedView.OnBubbleBlockedListener() {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleData$zesn1_xjTP9VJj3ngsaJURNhUiY */
            });
            doAdd(bubbleWithKey);
            trim();
        } else {
            bubbleWithKey.setEntry(notificationEntry);
            doUpdate(bubbleWithKey);
        }
        if (shouldAutoExpand(notificationEntry)) {
            setSelectedBubbleInternal(bubbleWithKey);
            if (!this.mExpanded) {
                setExpandedInternal(true);
            }
        } else if (this.mSelectedBubble == null) {
            setSelectedBubbleInternal(bubbleWithKey);
        }
        dispatchPendingChanges();
    }

    public void notificationEntryRemoved(NotificationEntry notificationEntry, int i) {
        doRemove(notificationEntry.key, i);
        dispatchPendingChanges();
    }

    public void notificationRankingUpdated(NotificationListenerService.RankingMap rankingMap) {
        if (this.mTmpRanking == null) {
            this.mTmpRanking = new NotificationListenerService.Ranking();
        }
        String[] orderedKeys = rankingMap.getOrderedKeys();
        for (String str : orderedKeys) {
            if (hasBubbleWithKey(str)) {
                rankingMap.getRanking(str, this.mTmpRanking);
                if (!this.mTmpRanking.canBubble()) {
                    doRemove(str, 4);
                }
            }
        }
        dispatchPendingChanges();
    }

    private void doAdd(Bubble bubble) {
        if (insertBubble((!isExpanded() || (hasBubbleWithGroupId(bubble.getGroupId()) ^ true)) ? 0 : findFirstIndexForGroup(bubble.getGroupId()), bubble) < this.mBubbles.size() - 1) {
            this.mStateChange.orderChanged = true;
        }
        this.mStateChange.addedBubble = bubble;
        if (!isExpanded()) {
            Update update = this.mStateChange;
            update.orderChanged = packGroup(findFirstIndexForGroup(bubble.getGroupId())) | update.orderChanged;
            setSelectedBubbleInternal(this.mBubbles.get(0));
        }
    }

    private void trim() {
        if (this.mBubbles.size() > 5) {
            this.mBubbles.stream().sorted(Comparator.comparingLong($$Lambda$x9O8XLDgnXklCbpbq_xgakOvcgY.INSTANCE)).filter(new Predicate() {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleData$cMHgi74d7w0GcIwOfMT2Vp3u6PQ */

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return BubbleData.this.lambda$trim$0$BubbleData((Bubble) obj);
                }
            }).findFirst().ifPresent(new Consumer() {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleData$29N_uZXST8y3Cv7BRkVQkHhseh0 */

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BubbleData.this.lambda$trim$1$BubbleData((Bubble) obj);
                }
            });
        }
    }

    public /* synthetic */ boolean lambda$trim$0$BubbleData(Bubble bubble) {
        return !bubble.equals(this.mSelectedBubble);
    }

    public /* synthetic */ void lambda$trim$1$BubbleData(Bubble bubble) {
        doRemove(bubble.getKey(), 2);
    }

    private void doUpdate(Bubble bubble) {
        this.mStateChange.updatedBubble = bubble;
        if (!isExpanded()) {
            int indexOf = this.mBubbles.indexOf(bubble);
            this.mBubbles.remove(bubble);
            int insertBubble = insertBubble(0, bubble);
            if (indexOf != insertBubble) {
                packGroup(insertBubble);
                this.mStateChange.orderChanged = true;
            }
            setSelectedBubbleInternal(this.mBubbles.get(0));
        }
    }

    private void doRemove(String str, int i) {
        int indexForKey = indexForKey(str);
        if (indexForKey != -1) {
            Bubble bubble = this.mBubbles.get(indexForKey);
            if (this.mBubbles.size() == 1) {
                setExpandedInternal(false);
                setSelectedBubbleInternal(null);
            }
            if (indexForKey < this.mBubbles.size() - 1) {
                this.mStateChange.orderChanged = true;
            }
            this.mBubbles.remove(indexForKey);
            this.mStateChange.bubbleRemoved(bubble, i);
            if (!isExpanded()) {
                this.mStateChange.orderChanged |= repackAll();
            }
            if (Objects.equals(this.mSelectedBubble, bubble)) {
                setSelectedBubbleInternal(this.mBubbles.get(Math.min(indexForKey, this.mBubbles.size() - 1)));
            }
            bubble.setDismissed();
            maybeSendDeleteIntent(i, bubble.entry);
        }
    }

    public void dismissAll(int i) {
        if (!this.mBubbles.isEmpty()) {
            setExpandedInternal(false);
            setSelectedBubbleInternal(null);
            while (!this.mBubbles.isEmpty()) {
                Bubble remove = this.mBubbles.remove(0);
                remove.setDismissed();
                maybeSendDeleteIntent(i, remove.entry);
                this.mStateChange.bubbleRemoved(remove, i);
            }
            dispatchPendingChanges();
        }
    }

    private void dispatchPendingChanges() {
        if (this.mListener != null && this.mStateChange.anythingChanged()) {
            this.mListener.applyUpdate(this.mStateChange);
        }
        this.mStateChange = new Update(this.mBubbles);
    }

    private void setSelectedBubbleInternal(Bubble bubble) {
        if (!Objects.equals(bubble, this.mSelectedBubble)) {
            if (bubble == null || this.mBubbles.contains(bubble)) {
                if (this.mExpanded && bubble != null) {
                    bubble.markAsAccessedAt(this.mTimeSource.currentTimeMillis());
                }
                this.mSelectedBubble = bubble;
                Update update = this.mStateChange;
                update.selectedBubble = bubble;
                update.selectionChanged = true;
                return;
            }
            Log.e("BubbleData", "Cannot select bubble which doesn't exist! (" + bubble + ") bubbles=" + this.mBubbles);
        }
    }

    private void setExpandedInternal(boolean z) {
        if (this.mExpanded != z) {
            if (z) {
                if (this.mBubbles.isEmpty()) {
                    Log.e("BubbleData", "Attempt to expand stack when empty!");
                    return;
                }
                Bubble bubble = this.mSelectedBubble;
                if (bubble == null) {
                    Log.e("BubbleData", "Attempt to expand stack without selected bubble!");
                    return;
                }
                bubble.markAsAccessedAt(this.mTimeSource.currentTimeMillis());
                this.mStateChange.orderChanged |= repackAll();
            } else if (!this.mBubbles.isEmpty()) {
                this.mStateChange.orderChanged |= repackAll();
                if (this.mBubbles.indexOf(this.mSelectedBubble) > 0) {
                    if (this.mSelectedBubble.isOngoing() || !this.mBubbles.get(0).isOngoing()) {
                        this.mBubbles.remove(this.mSelectedBubble);
                        this.mBubbles.add(0, this.mSelectedBubble);
                        packGroup(0);
                    } else {
                        setSelectedBubbleInternal(this.mBubbles.get(0));
                    }
                }
            }
            this.mExpanded = z;
            Update update = this.mStateChange;
            update.expanded = z;
            update.expandedChanged = true;
        }
    }

    /* access modifiers changed from: private */
    public static long sortKey(Bubble bubble) {
        long lastUpdateTime = bubble.getLastUpdateTime();
        return bubble.isOngoing() ? lastUpdateTime | 4611686018427387904L : lastUpdateTime;
    }

    private int insertBubble(int i, Bubble bubble) {
        long sortKey = sortKey(bubble);
        String str = null;
        while (i < this.mBubbles.size()) {
            Bubble bubble2 = this.mBubbles.get(i);
            String groupId = bubble2.getGroupId();
            if (!(!groupId.equals(str)) || sortKey <= sortKey(bubble2)) {
                i++;
                str = groupId;
            } else {
                this.mBubbles.add(i, bubble);
                return i;
            }
        }
        this.mBubbles.add(bubble);
        return this.mBubbles.size() - 1;
    }

    private boolean hasBubbleWithGroupId(String str) {
        return this.mBubbles.stream().anyMatch(new Predicate(str) {
            /* class com.android.systemui.bubbles.$$Lambda$BubbleData$H_9shD4W4k6iZvs8GpmXTAxbTbM */
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((Bubble) obj).getGroupId().equals(this.f$0);
            }
        });
    }

    private int findFirstIndexForGroup(String str) {
        for (int i = 0; i < this.mBubbles.size(); i++) {
            if (this.mBubbles.get(i).getGroupId().equals(str)) {
                return i;
            }
        }
        return 0;
    }

    private boolean packGroup(int i) {
        String groupId = this.mBubbles.get(i).getGroupId();
        ArrayList arrayList = new ArrayList();
        for (int size = this.mBubbles.size() - 1; size > i; size--) {
            if (this.mBubbles.get(size).getGroupId().equals(groupId)) {
                arrayList.add(0, this.mBubbles.get(size));
            }
        }
        if (arrayList.isEmpty()) {
            return false;
        }
        this.mBubbles.removeAll(arrayList);
        this.mBubbles.addAll(i + 1, arrayList);
        return true;
    }

    private boolean repackAll() {
        if (this.mBubbles.isEmpty()) {
            return false;
        }
        HashMap hashMap = new HashMap();
        for (Bubble bubble : this.mBubbles) {
            long longValue = ((Long) hashMap.getOrDefault(bubble.getGroupId(), 0L)).longValue();
            long sortKey = sortKey(bubble);
            if (sortKey > longValue) {
                hashMap.put(bubble.getGroupId(), Long.valueOf(sortKey));
            }
        }
        ArrayList arrayList = new ArrayList(this.mBubbles.size());
        for (String str : (List) hashMap.entrySet().stream().sorted(GROUPS_BY_MAX_SORT_KEY_DESCENDING).map($$Lambda$CSz_ibwXhtkKNl72Q8tR5oBgkWk.INSTANCE).collect(Collectors.toList())) {
            this.mBubbles.stream().filter(new Predicate(str) {
                /* class com.android.systemui.bubbles.$$Lambda$BubbleData$dNlU6_h6UYMtjKJV6CpiMlj80Mk */
                private final /* synthetic */ String f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ((Bubble) obj).getGroupId().equals(this.f$0);
                }
            }).sorted(BUBBLES_BY_SORT_KEY_DESCENDING).forEachOrdered(new Consumer(arrayList) {
                /* class com.android.systemui.bubbles.$$Lambda$0tU2wih_2wwdAnw6hE7FT9YuCis */
                private final /* synthetic */ List f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    this.f$0.add((Bubble) obj);
                }
            });
        }
        if (arrayList.equals(this.mBubbles)) {
            return false;
        }
        this.mBubbles.clear();
        this.mBubbles.addAll(arrayList);
        return true;
    }

    private void maybeSendDeleteIntent(int i, NotificationEntry notificationEntry) {
        if (i == 1) {
            Notification.BubbleMetadata bubbleMetadata = notificationEntry.getBubbleMetadata();
            PendingIntent deleteIntent = bubbleMetadata != null ? bubbleMetadata.getDeleteIntent() : null;
            if (deleteIntent != null) {
                try {
                    deleteIntent.send();
                } catch (PendingIntent.CanceledException unused) {
                    Log.w("BubbleData", "Failed to send delete intent for bubble with key: " + notificationEntry.key);
                }
            }
        }
    }

    private int indexForKey(String str) {
        for (int i = 0; i < this.mBubbles.size(); i++) {
            if (this.mBubbles.get(i).getKey().equals(str)) {
                return i;
            }
        }
        return -1;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public List<Bubble> getBubbles() {
        return Collections.unmodifiableList(this.mBubbles);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public Bubble getBubbleWithKey(String str) {
        for (int i = 0; i < this.mBubbles.size(); i++) {
            Bubble bubble = this.mBubbles.get(i);
            if (bubble.getKey().equals(str)) {
                return bubble;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void setTimeSource(TimeSource timeSource) {
        this.mTimeSource = timeSource;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    /* access modifiers changed from: package-private */
    public boolean shouldAutoExpand(NotificationEntry notificationEntry) {
        Notification.BubbleMetadata bubbleMetadata = notificationEntry.getBubbleMetadata();
        return bubbleMetadata != null && bubbleMetadata.getAutoExpandBubble() && BubbleController.isForegroundApp(this.mContext, notificationEntry.notification.getPackageName());
    }
}
