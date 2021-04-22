package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.Log;
import android.util.Pools;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.collection.ArraySet;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.AlertingNotificationManager;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class HeadsUpManagerPhone extends HeadsUpManager implements Dumpable, VisualStabilityManager.Callback, OnHeadsUpChangedListener, ConfigurationController.ConfigurationListener, StatusBarStateController.StateListener {
    private AnimationStateHandler mAnimationStateHandler;
    private int mDisplayCutoutTouchableRegionSize;
    private HashSet<NotificationEntry> mEntriesToRemoveAfterExpand = new HashSet<>();
    private ArraySet<NotificationEntry> mEntriesToRemoveWhenReorderingAllowed = new ArraySet<>();
    private final Pools.Pool<HeadsUpEntryPhone> mEntryPool = new Pools.Pool<HeadsUpEntryPhone>() {
        /* class com.android.systemui.statusbar.phone.HeadsUpManagerPhone.AnonymousClass1 */
        private Stack<HeadsUpEntryPhone> mPoolObjects = new Stack<>();

        public HeadsUpEntryPhone acquire() {
            if (!this.mPoolObjects.isEmpty()) {
                return this.mPoolObjects.pop();
            }
            return new HeadsUpEntryPhone();
        }

        public boolean release(HeadsUpEntryPhone headsUpEntryPhone) {
            this.mPoolObjects.push(headsUpEntryPhone);
            return true;
        }
    };
    private final NotificationGroupManager mGroupManager;
    private boolean mHeadsUpGoingAway;
    private int mHeadsUpInset;
    private boolean mIsExpanded;
    private boolean mReleaseOnExpandFinish;
    private int mStatusBarHeight;
    private int mStatusBarState;
    private final StatusBarTouchableRegionManager mStatusBarTouchableRegionManager;
    private final View mStatusBarWindowView;
    private HashSet<String> mSwipedOutKeys = new HashSet<>();
    private int[] mTmpTwoArray = new int[2];
    private Region mTouchableRegion = new Region();
    private boolean mTrackingHeadsUp;
    private final VisualStabilityManager mVisualStabilityManager;

    public interface AnimationStateHandler {
        void setHeadsUpGoingAwayAnimationsAllowed(boolean z);
    }

    public HeadsUpManagerPhone(Context context, View view, NotificationGroupManager notificationGroupManager, StatusBar statusBar, VisualStabilityManager visualStabilityManager) {
        super(context);
        this.mStatusBarWindowView = view;
        this.mStatusBarTouchableRegionManager = new StatusBarTouchableRegionManager(context, this, statusBar, view);
        this.mGroupManager = notificationGroupManager;
        this.mVisualStabilityManager = visualStabilityManager;
        initResources();
        addListener(new OnHeadsUpChangedListener() {
            /* class com.android.systemui.statusbar.phone.HeadsUpManagerPhone.AnonymousClass2 */

            @Override // com.android.systemui.statusbar.policy.OnHeadsUpChangedListener
            public void onHeadsUpPinnedModeChanged(boolean z) {
                if (Log.isLoggable("HeadsUpManagerPhone", 5)) {
                    Log.w("HeadsUpManagerPhone", "onHeadsUpPinnedModeChanged");
                }
                HeadsUpManagerPhone.this.mStatusBarTouchableRegionManager.updateTouchableRegion();
            }
        });
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
    }

    public void setAnimationStateHandler(AnimationStateHandler animationStateHandler) {
        this.mAnimationStateHandler = animationStateHandler;
    }

    private void initResources() {
        Resources resources = this.mContext.getResources();
        this.mStatusBarHeight = resources.getDimensionPixelSize(17105427);
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(C0005R$dimen.heads_up_status_bar_padding);
        this.mDisplayCutoutTouchableRegionSize = resources.getDimensionPixelSize(17105145);
    }

    @Override // com.android.systemui.statusbar.policy.HeadsUpManager, com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        initResources();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        initResources();
    }

    public boolean shouldSwallowClick(String str) {
        HeadsUpManager.HeadsUpEntry headsUpEntry = getHeadsUpEntry(str);
        return headsUpEntry != null && this.mClock.currentTimeMillis() < headsUpEntry.mPostTime;
    }

    public void onExpandingFinished() {
        if (this.mReleaseOnExpandFinish) {
            releaseAllImmediately();
            this.mReleaseOnExpandFinish = false;
        } else {
            Iterator<NotificationEntry> it = this.mEntriesToRemoveAfterExpand.iterator();
            while (it.hasNext()) {
                NotificationEntry next = it.next();
                if (isAlerting(next.key)) {
                    removeAlertEntry(next.key);
                }
            }
        }
        this.mEntriesToRemoveAfterExpand.clear();
    }

    public void setTrackingHeadsUp(boolean z) {
        this.mTrackingHeadsUp = z;
    }

    public void setIsPanelExpanded(boolean z) {
        if (z != this.mIsExpanded) {
            this.mIsExpanded = z;
            if (z) {
                this.mHeadsUpGoingAway = false;
            }
            this.mStatusBarTouchableRegionManager.setIsStatusBarExpanded(z);
            this.mStatusBarTouchableRegionManager.updateTouchableRegion();
        }
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        this.mStatusBarState = i;
    }

    public void setHeadsUpGoingAway(boolean z) {
        if (z != this.mHeadsUpGoingAway) {
            this.mHeadsUpGoingAway = z;
            if (!z) {
                this.mStatusBarTouchableRegionManager.updateTouchableRegionAfterLayout();
            } else {
                this.mStatusBarTouchableRegionManager.updateTouchableRegion();
            }
        }
    }

    public boolean isHeadsUpGoingAway() {
        return this.mHeadsUpGoingAway;
    }

    public void setRemoteInputActive(NotificationEntry notificationEntry, boolean z) {
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(notificationEntry.key);
        if (headsUpEntryPhone != null && headsUpEntryPhone.remoteInputActive != z) {
            headsUpEntryPhone.remoteInputActive = z;
            if (z) {
                headsUpEntryPhone.removeAutoRemovalCallbacks();
            } else {
                headsUpEntryPhone.updateEntry(false);
            }
        }
    }

    public void setMenuShown(NotificationEntry notificationEntry, boolean z) {
        HeadsUpManager.HeadsUpEntry headsUpEntry = getHeadsUpEntry(notificationEntry.key);
        if ((headsUpEntry instanceof HeadsUpEntryPhone) && notificationEntry.isRowPinned()) {
            ((HeadsUpEntryPhone) headsUpEntry).setMenuShownPinned(z);
        }
    }

    @Override // com.android.systemui.statusbar.policy.HeadsUpManager
    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    @Override // com.android.systemui.statusbar.policy.HeadsUpManager
    public void snooze() {
        super.snooze();
        this.mReleaseOnExpandFinish = true;
    }

    public void addSwipedOutNotification(String str) {
        this.mSwipedOutKeys.add(str);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("HeadsUpManagerPhone state:");
        dumpInternal(fileDescriptor, printWriter, strArr);
    }

    public void updateTouchableRegion(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
        internalInsetsInfo.setTouchableInsets(3);
        internalInsetsInfo.touchableRegion.set(calculateTouchableRegion());
    }

    public Region calculateTouchableRegion() {
        NotificationEntry groupSummary;
        NotificationEntry topEntry = getTopEntry();
        if (!hasPinnedHeadsUp() || topEntry == null) {
            this.mTouchableRegion.set(0, 0, this.mStatusBarWindowView.getWidth(), this.mStatusBarHeight);
            updateRegionForNotch(this.mTouchableRegion);
        } else {
            if (topEntry.isChildInGroup() && (groupSummary = this.mGroupManager.getGroupSummary(topEntry.notification)) != null) {
                topEntry = groupSummary;
            }
            ExpandableNotificationRow row = topEntry.getRow();
            row.getLocationOnScreen(this.mTmpTwoArray);
            int[] iArr = this.mTmpTwoArray;
            int i = iArr[0];
            int width = iArr[0] + row.getWidth();
            int intrinsicHeight = row.getIntrinsicHeight();
            this.mTouchableRegion.set(i, 0, width, this.mHeadsUpInset + intrinsicHeight);
            Rect headsUpMwButtonRect = this.mStatusBarTouchableRegionManager.getHeadsUpMwButtonRect();
            if (headsUpMwButtonRect != null) {
                headsUpMwButtonRect.offsetTo(i + headsUpMwButtonRect.left, this.mHeadsUpInset + intrinsicHeight);
                this.mTouchableRegion.union(headsUpMwButtonRect);
            }
        }
        return this.mTouchableRegion;
    }

    private void updateRegionForNotch(Region region) {
        DisplayCutout displayCutout = this.mStatusBarWindowView.getRootWindowInsets().getDisplayCutout();
        if (displayCutout != null) {
            Rect rect = new Rect();
            ScreenDecorations.DisplayCutoutView.boundsFromDirection(displayCutout, 48, rect);
            rect.offset(0, this.mDisplayCutoutTouchableRegionSize);
            region.union(rect);
        }
    }

    @Override // com.android.systemui.statusbar.AlertingNotificationManager, com.android.systemui.statusbar.NotificationLifetimeExtender
    public boolean shouldExtendLifetime(NotificationEntry notificationEntry) {
        return this.mVisualStabilityManager.isReorderingAllowed() && super.shouldExtendLifetime(notificationEntry);
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onConfigChanged(Configuration configuration) {
        initResources();
    }

    @Override // com.android.systemui.statusbar.notification.VisualStabilityManager.Callback
    public void onReorderingAllowed() {
        this.mAnimationStateHandler.setHeadsUpGoingAwayAnimationsAllowed(false);
        Iterator<NotificationEntry> it = this.mEntriesToRemoveWhenReorderingAllowed.iterator();
        while (it.hasNext()) {
            NotificationEntry next = it.next();
            if (isAlerting(next.key)) {
                removeAlertEntry(next.key);
            }
        }
        this.mEntriesToRemoveWhenReorderingAllowed.clear();
        this.mAnimationStateHandler.setHeadsUpGoingAwayAnimationsAllowed(true);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager, com.android.systemui.statusbar.policy.HeadsUpManager, com.android.systemui.statusbar.policy.HeadsUpManager
    public HeadsUpManager.HeadsUpEntry createAlertEntry() {
        return (HeadsUpManager.HeadsUpEntry) this.mEntryPool.acquire();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager, com.android.systemui.statusbar.policy.HeadsUpManager
    public void onAlertEntryRemoved(AlertingNotificationManager.AlertEntry alertEntry) {
        super.onAlertEntryRemoved(alertEntry);
        this.mEntryPool.release((HeadsUpEntryPhone) alertEntry);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.policy.HeadsUpManager
    public boolean shouldHeadsUpBecomePinned(NotificationEntry notificationEntry) {
        if ((this.mStatusBarState == 1 || this.mIsExpanded) && !super.shouldHeadsUpBecomePinned(notificationEntry)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.policy.HeadsUpManager
    public void dumpInternal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dumpInternal(fileDescriptor, printWriter, strArr);
        printWriter.print("  mBarState=");
        printWriter.println(this.mStatusBarState);
        printWriter.print("  mTouchableRegion=");
        printWriter.println(this.mTouchableRegion);
    }

    private HeadsUpEntryPhone getHeadsUpEntryPhone(String str) {
        return (HeadsUpEntryPhone) this.mAlertEntries.get(str);
    }

    private HeadsUpEntryPhone getTopHeadsUpEntryPhone() {
        return (HeadsUpEntryPhone) getTopHeadsUpEntry();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.AlertingNotificationManager
    public boolean canRemoveImmediately(String str) {
        if (this.mSwipedOutKeys.contains(str)) {
            this.mSwipedOutKeys.remove(str);
            return true;
        }
        HeadsUpEntryPhone headsUpEntryPhone = getHeadsUpEntryPhone(str);
        HeadsUpEntryPhone topHeadsUpEntryPhone = getTopHeadsUpEntryPhone();
        if (headsUpEntryPhone == null || headsUpEntryPhone != topHeadsUpEntryPhone || super.canRemoveImmediately(str)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public class HeadsUpEntryPhone extends HeadsUpManager.HeadsUpEntry {
        private boolean mMenuShownPinned;

        protected HeadsUpEntryPhone() {
            super();
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry, com.android.systemui.statusbar.policy.HeadsUpManager.HeadsUpEntry
        public boolean isSticky() {
            return super.isSticky() || this.mMenuShownPinned;
        }

        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry
        public void setEntry(NotificationEntry notificationEntry) {
            setEntry(notificationEntry, new Runnable(notificationEntry) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$HeadsUpManagerPhone$HeadsUpEntryPhone$adyrhF30JE9Yr0JaVKYkiAV0Clw */
                private final /* synthetic */ NotificationEntry f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HeadsUpManagerPhone.HeadsUpEntryPhone.this.lambda$setEntry$0$HeadsUpManagerPhone$HeadsUpEntryPhone(this.f$1);
                }
            });
        }

        public /* synthetic */ void lambda$setEntry$0$HeadsUpManagerPhone$HeadsUpEntryPhone(NotificationEntry notificationEntry) {
            if (!HeadsUpManagerPhone.this.mVisualStabilityManager.isReorderingAllowed()) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.add(notificationEntry);
                HeadsUpManagerPhone.this.mVisualStabilityManager.addReorderingAllowedCallback(HeadsUpManagerPhone.this);
            } else if (!HeadsUpManagerPhone.this.mTrackingHeadsUp) {
                HeadsUpManagerPhone.this.removeAlertEntry(notificationEntry.key);
            } else {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.add(notificationEntry);
            }
        }

        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry
        public void updateEntry(boolean z) {
            super.updateEntry(z);
            if (HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.contains(this.mEntry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveAfterExpand.remove(this.mEntry);
            }
            if (HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.contains(this.mEntry)) {
                HeadsUpManagerPhone.this.mEntriesToRemoveWhenReorderingAllowed.remove(this.mEntry);
            }
        }

        @Override // com.android.systemui.statusbar.policy.HeadsUpManager.HeadsUpEntry
        public void setExpanded(boolean z) {
            if (this.expanded != z) {
                this.expanded = z;
                if (z) {
                    removeAutoRemovalCallbacks();
                } else {
                    updateEntry(false);
                }
            }
        }

        public void setMenuShownPinned(boolean z) {
            if (this.mMenuShownPinned != z) {
                this.mMenuShownPinned = z;
                if (z) {
                    removeAutoRemovalCallbacks();
                } else {
                    updateEntry(false);
                }
            }
        }

        @Override // com.android.systemui.statusbar.AlertingNotificationManager.AlertEntry, com.android.systemui.statusbar.policy.HeadsUpManager.HeadsUpEntry
        public void reset() {
            super.reset();
            this.mMenuShownPinned = false;
        }
    }
}
