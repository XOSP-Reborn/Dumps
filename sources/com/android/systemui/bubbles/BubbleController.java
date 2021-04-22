package com.android.systemui.bubbles;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.Pair;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.C0007R$id;
import com.android.systemui.Dependency;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.bubbles.BubbleData;
import com.android.systemui.bubbles.BubbleStackView;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.NotificationRemoveInterceptor;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.List;

public class BubbleController implements ConfigurationController.ConfigurationListener {
    private IStatusBarService mBarService;
    private BubbleData mBubbleData;
    private final BubbleData.Listener mBubbleDataListener;
    private final Context mContext;
    private final NotificationEntryListener mEntryListener;
    private BubbleExpandListener mExpandListener;
    private final NotificationEntryManager mNotificationEntryManager;
    private final NotificationInterruptionStateProvider mNotificationInterruptionStateProvider;
    private int mOrientation;
    private final NotificationRemoveInterceptor mRemoveInterceptor;
    private BubbleStackView mStackView;
    private BubbleStateChangeListener mStateChangeListener;
    private StatusBarStateListener mStatusBarStateListener;
    private final StatusBarWindowController mStatusBarWindowController;
    private BubbleStackView.SurfaceSynchronizer mSurfaceSynchronizer;
    private final BubbleTaskStackListener mTaskStackListener;
    private Rect mTempRect;
    private final ZenModeController mZenModeController;

    public interface BubbleExpandListener {
        void onBubbleExpandChanged(boolean z, String str);
    }

    public interface BubbleStateChangeListener {
        void onHasBubblesChanged(boolean z);
    }

    /* access modifiers changed from: private */
    public class StatusBarStateListener implements StatusBarStateController.StateListener {
        private int mState;

        private StatusBarStateListener() {
        }

        public int getCurrentState() {
            return this.mState;
        }

        @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
        public void onStateChanged(int i) {
            this.mState = i;
            if (this.mState != 0) {
                BubbleController.this.collapseStack();
            }
            BubbleController.this.updateStack();
        }
    }

    public BubbleController(Context context, StatusBarWindowController statusBarWindowController, BubbleData bubbleData, ConfigurationController configurationController, NotificationInterruptionStateProvider notificationInterruptionStateProvider, ZenModeController zenModeController) {
        this(context, statusBarWindowController, bubbleData, null, configurationController, notificationInterruptionStateProvider, zenModeController);
    }

    public BubbleController(Context context, StatusBarWindowController statusBarWindowController, BubbleData bubbleData, BubbleStackView.SurfaceSynchronizer surfaceSynchronizer, ConfigurationController configurationController, NotificationInterruptionStateProvider notificationInterruptionStateProvider, ZenModeController zenModeController) {
        this.mTempRect = new Rect();
        this.mOrientation = 0;
        this.mRemoveInterceptor = new NotificationRemoveInterceptor() {
            /* class com.android.systemui.bubbles.BubbleController.AnonymousClass2 */

            @Override // com.android.systemui.statusbar.NotificationRemoveInterceptor
            public boolean onNotificationRemoveRequested(String str, int i) {
                if (!BubbleController.this.mBubbleData.hasBubbleWithKey(str)) {
                    return false;
                }
                NotificationEntry notificationEntry = BubbleController.this.mBubbleData.getBubbleWithKey(str).entry;
                boolean z = (notificationEntry.isRowDismissed() && !(i == 8 || i == 9)) || (i == 3) || (i == 2);
                if (notificationEntry.isBubble() && !notificationEntry.isBubbleDismissed() && z) {
                    notificationEntry.setShowInShadeWhenBubble(false);
                    if (BubbleController.this.mStackView != null) {
                        BubbleController.this.mStackView.updateDotVisibility(notificationEntry.key);
                    }
                    BubbleController.this.mNotificationEntryManager.updateNotifications();
                    return true;
                }
                if (!z && !notificationEntry.isBubbleDismissed()) {
                    BubbleController.this.mBubbleData.notificationEntryRemoved(notificationEntry, 5);
                }
                return false;
            }
        };
        this.mEntryListener = new NotificationEntryListener() {
            /* class com.android.systemui.bubbles.BubbleController.AnonymousClass3 */

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPendingEntryAdded(NotificationEntry notificationEntry) {
                if (BubbleController.areBubblesEnabled(BubbleController.this.mContext) && BubbleController.this.mNotificationInterruptionStateProvider.shouldBubbleUp(notificationEntry) && BubbleController.canLaunchInActivityView(BubbleController.this.mContext, notificationEntry)) {
                    BubbleController.this.updateShowInShadeForSuppressNotification(notificationEntry);
                }
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onEntryInflated(NotificationEntry notificationEntry, int i) {
                if (BubbleController.areBubblesEnabled(BubbleController.this.mContext) && BubbleController.this.mNotificationInterruptionStateProvider.shouldBubbleUp(notificationEntry) && BubbleController.canLaunchInActivityView(BubbleController.this.mContext, notificationEntry)) {
                    BubbleController.this.updateBubble(notificationEntry);
                }
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPreEntryUpdated(NotificationEntry notificationEntry) {
                if (BubbleController.areBubblesEnabled(BubbleController.this.mContext)) {
                    boolean z = BubbleController.this.mNotificationInterruptionStateProvider.shouldBubbleUp(notificationEntry) && BubbleController.canLaunchInActivityView(BubbleController.this.mContext, notificationEntry);
                    if (!z && BubbleController.this.mBubbleData.hasBubbleWithKey(notificationEntry.key)) {
                        BubbleController.this.removeBubble(notificationEntry.key, 7);
                    } else if (z) {
                        BubbleController.this.updateShowInShadeForSuppressNotification(notificationEntry);
                        notificationEntry.setBubbleDismissed(false);
                        BubbleController.this.updateBubble(notificationEntry);
                    }
                }
            }

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onNotificationRankingUpdated(NotificationListenerService.RankingMap rankingMap) {
                BubbleController.this.mBubbleData.notificationRankingUpdated(rankingMap);
            }
        };
        this.mBubbleDataListener = new BubbleData.Listener() {
            /* class com.android.systemui.bubbles.BubbleController.AnonymousClass4 */

            @Override // com.android.systemui.bubbles.BubbleData.Listener
            public void applyUpdate(BubbleData.Update update) {
                if (BubbleController.this.mStackView == null && update.addedBubble != null) {
                    BubbleController.this.ensureStackViewCreated();
                }
                if (BubbleController.this.mStackView != null) {
                    if (update.addedBubble != null) {
                        BubbleController.this.mStackView.addBubble(update.addedBubble);
                    }
                    if (update.expandedChanged && !update.expanded) {
                        BubbleController.this.mStackView.setExpanded(false);
                    }
                    for (Pair<Bubble, Integer> pair : update.removedBubbles) {
                        Bubble bubble = (Bubble) pair.first;
                        ((Integer) pair.second).intValue();
                        BubbleController.this.mStackView.removeBubble(bubble);
                        if (BubbleController.this.mBubbleData.hasBubbleWithKey(bubble.getKey()) || bubble.entry.showInShadeWhenBubble()) {
                            bubble.entry.notification.getNotification().flags &= -4097;
                            try {
                                BubbleController.this.mBarService.onNotificationBubbleChanged(bubble.getKey(), false);
                            } catch (RemoteException unused) {
                            }
                        } else {
                            BubbleController.this.mNotificationEntryManager.performRemoveNotification(bubble.entry.notification, 0);
                        }
                    }
                    if (update.updatedBubble != null) {
                        BubbleController.this.mStackView.updateBubble(update.updatedBubble);
                    }
                    if (update.orderChanged) {
                        BubbleController.this.mStackView.updateBubbleOrder(update.bubbles);
                    }
                    if (update.selectionChanged) {
                        BubbleController.this.mStackView.setSelectedBubble(update.selectedBubble);
                    }
                    if (update.expandedChanged && update.expanded) {
                        BubbleController.this.mStackView.setExpanded(true);
                    }
                    BubbleController.this.mNotificationEntryManager.updateNotifications();
                    BubbleController.this.updateStack();
                }
            }
        };
        this.mContext = context;
        this.mNotificationInterruptionStateProvider = notificationInterruptionStateProvider;
        this.mZenModeController = zenModeController;
        this.mZenModeController.addCallback(new ZenModeController.Callback() {
            /* class com.android.systemui.bubbles.BubbleController.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
            public void onZenChanged(int i) {
                BubbleController.this.updateStackViewForZenConfig();
            }

            @Override // com.android.systemui.statusbar.policy.ZenModeController.Callback
            public void onConfigChanged(ZenModeConfig zenModeConfig) {
                BubbleController.this.updateStackViewForZenConfig();
            }
        });
        configurationController.addCallback(this);
        this.mBubbleData = bubbleData;
        this.mBubbleData.setListener(this.mBubbleDataListener);
        this.mNotificationEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mNotificationEntryManager.addNotificationEntryListener(this.mEntryListener);
        this.mNotificationEntryManager.setNotificationRemoveInterceptor(this.mRemoveInterceptor);
        this.mStatusBarWindowController = statusBarWindowController;
        this.mStatusBarStateListener = new StatusBarStateListener();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this.mStatusBarStateListener);
        this.mTaskStackListener = new BubbleTaskStackListener();
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        try {
            WindowManagerWrapper.getInstance().addPinnedStackListener(new BubblesImeListener());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        this.mSurfaceSynchronizer = surfaceSynchronizer;
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void ensureStackViewCreated() {
        if (this.mStackView == null) {
            this.mStackView = new BubbleStackView(this.mContext, this.mBubbleData, this.mSurfaceSynchronizer);
            ViewGroup statusBarView = this.mStatusBarWindowController.getStatusBarView();
            statusBarView.addView(this.mStackView, statusBarView.indexOfChild(statusBarView.findViewById(C0007R$id.scrim_behind)) + 1, new FrameLayout.LayoutParams(-1, -1));
            BubbleExpandListener bubbleExpandListener = this.mExpandListener;
            if (bubbleExpandListener != null) {
                this.mStackView.setExpandListener(bubbleExpandListener);
            }
            updateStackViewForZenConfig();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onUiModeChanged() {
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView != null) {
            bubbleStackView.onThemeChanged();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView != null) {
            bubbleStackView.onThemeChanged();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onConfigChanged(Configuration configuration) {
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView != null && configuration != null && configuration.orientation != this.mOrientation) {
            bubbleStackView.onOrientationChanged();
            this.mOrientation = configuration.orientation;
        }
    }

    public void setBubbleStateChangeListener(BubbleStateChangeListener bubbleStateChangeListener) {
        this.mStateChangeListener = bubbleStateChangeListener;
    }

    public void setExpandListener(BubbleExpandListener bubbleExpandListener) {
        this.mExpandListener = new BubbleExpandListener(bubbleExpandListener) {
            /* class com.android.systemui.bubbles.$$Lambda$BubbleController$B9Rf8Lqgsvsjhuncdnt9rJlYfA */
            private final /* synthetic */ BubbleController.BubbleExpandListener f$1;

            {
                this.f$1 = r2;
            }

            @Override // com.android.systemui.bubbles.BubbleController.BubbleExpandListener
            public final void onBubbleExpandChanged(boolean z, String str) {
                BubbleController.this.lambda$setExpandListener$0$BubbleController(this.f$1, z, str);
            }
        };
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView != null) {
            bubbleStackView.setExpandListener(this.mExpandListener);
        }
    }

    public /* synthetic */ void lambda$setExpandListener$0$BubbleController(BubbleExpandListener bubbleExpandListener, boolean z, String str) {
        if (bubbleExpandListener != null) {
            bubbleExpandListener.onBubbleExpandChanged(z, str);
        }
        this.mStatusBarWindowController.setBubbleExpanded(z);
    }

    public boolean hasBubbles() {
        if (this.mStackView == null) {
            return false;
        }
        return this.mBubbleData.hasBubbles();
    }

    public boolean isStackExpanded() {
        return this.mBubbleData.isExpanded();
    }

    public void collapseStack() {
        this.mBubbleData.setExpanded(false);
    }

    /* access modifiers changed from: package-private */
    public void selectBubble(Bubble bubble) {
        this.mBubbleData.setSelectedBubble(bubble);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void selectBubble(String str) {
        selectBubble(this.mBubbleData.getBubbleWithKey(str));
    }

    public void expandStackAndSelectBubble(String str) {
        Bubble bubbleWithKey = this.mBubbleData.getBubbleWithKey(str);
        if (bubbleWithKey != null) {
            this.mBubbleData.setSelectedBubble(bubbleWithKey);
            this.mBubbleData.setExpanded(true);
        }
    }

    /* access modifiers changed from: package-private */
    public void dismissStack(int i) {
        this.mBubbleData.dismissAll(i);
    }

    public void performBackPressIfNeeded() {
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView != null) {
            bubbleStackView.performBackPressIfNeeded();
        }
    }

    /* access modifiers changed from: package-private */
    public void updateBubble(NotificationEntry notificationEntry) {
        if (notificationEntry.importance >= 4) {
            notificationEntry.setInterruption();
        }
        this.mBubbleData.notificationEntryUpdated(notificationEntry);
    }

    /* access modifiers changed from: package-private */
    public void removeBubble(String str, int i) {
        Bubble bubbleWithKey = this.mBubbleData.getBubbleWithKey(str);
        if (bubbleWithKey != null) {
            this.mBubbleData.notificationEntryRemoved(bubbleWithKey.entry, i);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateStackViewForZenConfig() {
        ZenModeConfig config = this.mZenModeController.getConfig();
        if (config != null && this.mStackView != null) {
            int i = config.suppressedVisualEffects;
            boolean z = true;
            boolean z2 = (i & 64) != 0;
            boolean z3 = (i & 16) != 0;
            boolean z4 = (i & 256) != 0;
            boolean z5 = this.mZenModeController.getZen() != 0;
            this.mStackView.setSuppressNewDot(z5 && z2);
            BubbleStackView bubbleStackView = this.mStackView;
            if (!z5 || (!z3 && !z4)) {
                z = false;
            }
            bubbleStackView.setSuppressFlyout(z);
        }
    }

    public void updateStack() {
        if (this.mStackView != null) {
            boolean z = false;
            int i = 4;
            if (this.mStatusBarStateListener.getCurrentState() != 0 || !hasBubbles()) {
                BubbleStackView bubbleStackView = this.mStackView;
                if (bubbleStackView != null) {
                    bubbleStackView.setVisibility(4);
                }
            } else {
                BubbleStackView bubbleStackView2 = this.mStackView;
                if (hasBubbles()) {
                    i = 0;
                }
                bubbleStackView2.setVisibility(i);
            }
            boolean bubblesShowing = this.mStatusBarWindowController.getBubblesShowing();
            if (hasBubbles() && this.mStackView.getVisibility() == 0) {
                z = true;
            }
            this.mStatusBarWindowController.setBubblesShowing(z);
            BubbleStateChangeListener bubbleStateChangeListener = this.mStateChangeListener;
            if (!(bubbleStateChangeListener == null || bubblesShowing == z)) {
                bubbleStateChangeListener.onHasBubblesChanged(z);
            }
            this.mStackView.updateContentDescription();
        }
    }

    public Rect getTouchableRegion() {
        BubbleStackView bubbleStackView = this.mStackView;
        if (bubbleStackView == null || bubbleStackView.getVisibility() != 0) {
            return null;
        }
        this.mStackView.getBoundsOnScreen(this.mTempRect);
        return this.mTempRect;
    }

    public int getExpandedDisplayId(Context context) {
        if (this.mStackView == null) {
            return -1;
        }
        boolean z = context.getDisplay() != null && context.getDisplay().getDisplayId() == 0;
        Bubble expandedBubble = this.mStackView.getExpandedBubble();
        if (!z || expandedBubble == null || !isStackExpanded() || this.mStatusBarWindowController.getPanelExpanded()) {
            return -1;
        }
        return expandedBubble.expandedView.getVirtualDisplayId();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public BubbleStackView getStackView() {
        return this.mStackView;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean shouldAutoBubbleForFlags(Context context, NotificationEntry notificationEntry) {
        boolean z;
        boolean equals;
        if (notificationEntry.isBubbleDismissed()) {
            return false;
        }
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        boolean z2 = shouldAutoBubbleMessages(context);
        boolean z3 = shouldAutoBubbleOngoing(context);
        boolean z4 = shouldAutoBubbleAll(context);
        if (statusBarNotification.getNotification().actions != null) {
            Notification.Action[] actionArr = statusBarNotification.getNotification().actions;
            int length = actionArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                } else if (actionArr[i].getRemoteInputs() != null) {
                    z = true;
                    break;
                } else {
                    i++;
                }
            }
            boolean z5 = !statusBarNotification.getNotification().hasMediaSession() || (!"call".equals(statusBarNotification.getNotification().category) && statusBarNotification.isOngoing());
            Class notificationStyle = statusBarNotification.getNotification().getNotificationStyle();
            equals = "msg".equals(statusBarNotification.getNotification().category);
            boolean equals2 = Notification.MessagingStyle.class.equals(notificationStyle);
            if ((((equals || !z) && !equals2) || !z2) && ((!z5 || !z3) && !z4)) {
                return false;
            }
            return true;
        }
        z = false;
        if (!statusBarNotification.getNotification().hasMediaSession()) {
        }
        Class notificationStyle2 = statusBarNotification.getNotification().getNotificationStyle();
        equals = "msg".equals(statusBarNotification.getNotification().category);
        boolean equals22 = Notification.MessagingStyle.class.equals(notificationStyle2);
        if (equals) {
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateShowInShadeForSuppressNotification(NotificationEntry notificationEntry) {
        notificationEntry.setShowInShadeWhenBubble(!(notificationEntry.getBubbleMetadata() != null && notificationEntry.getBubbleMetadata().isNotificationSuppressed() && isForegroundApp(this.mContext, notificationEntry.notification.getPackageName())));
    }

    public static boolean isForegroundApp(Context context, String str) {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) context.getSystemService(ActivityManager.class)).getRunningTasks(1);
        if (runningTasks.isEmpty() || !str.equals(runningTasks.get(0).topActivity.getPackageName())) {
            return false;
        }
        return true;
    }

    private class BubbleTaskStackListener extends TaskStackChangeListener {
        private BubbleTaskStackListener() {
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onTaskMovedToFront(ActivityManager.RunningTaskInfo runningTaskInfo) {
            if (BubbleController.this.mStackView != null && runningTaskInfo.displayId == 0) {
                BubbleController.this.mBubbleData.setExpanded(false);
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onActivityLaunchOnSecondaryDisplayRerouted() {
            if (BubbleController.this.mStackView != null) {
                BubbleController.this.mBubbleData.setExpanded(false);
            }
        }

        @Override // com.android.systemui.shared.system.TaskStackChangeListener
        public void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo runningTaskInfo) {
            if (BubbleController.this.mStackView != null) {
                int i = runningTaskInfo.displayId;
                BubbleController bubbleController = BubbleController.this;
                if (i == bubbleController.getExpandedDisplayId(bubbleController.mContext)) {
                    BubbleController.this.mBubbleData.setExpanded(false);
                }
            }
        }
    }

    private static boolean shouldAutoBubbleMessages(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "experiment_autobubble_messaging", 0) != 0;
    }

    private static boolean shouldAutoBubbleOngoing(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "experiment_autobubble_ongoing", 0) != 0;
    }

    private static boolean shouldAutoBubbleAll(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "experiment_autobubble_all", 0) != 0;
    }

    /* access modifiers changed from: private */
    public static boolean areBubblesEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "experiment_enable_bubbles", 1) != 0;
    }

    static boolean canLaunchInActivityView(Context context, NotificationEntry notificationEntry) {
        PendingIntent intent = notificationEntry.getBubbleMetadata() != null ? notificationEntry.getBubbleMetadata().getIntent() : null;
        if (intent == null) {
            Log.w("BubbleController", "Unable to create bubble -- no intent");
            return false;
        }
        ActivityInfo resolveActivityInfo = intent.getIntent().resolveActivityInfo(context.getPackageManager(), 0);
        if (resolveActivityInfo == null) {
            Log.w("BubbleController", "Unable to send as bubble -- couldn't find activity info for intent: " + intent);
            return false;
        } else if (!ActivityInfo.isResizeableMode(resolveActivityInfo.resizeMode)) {
            Log.w("BubbleController", "Unable to send as bubble -- activity is not resizable for intent: " + intent);
            return false;
        } else if (resolveActivityInfo.documentLaunchMode != 2) {
            Log.w("BubbleController", "Unable to send as bubble -- activity is not documentLaunchMode=always for intent: " + intent);
            return false;
        } else if ((resolveActivityInfo.flags & Integer.MIN_VALUE) != 0) {
            return true;
        } else {
            Log.w("BubbleController", "Unable to send as bubble -- activity is not embeddable for intent: " + intent);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public class BubblesImeListener extends IPinnedStackListener.Stub {
        public void onActionsChanged(ParceledListSlice parceledListSlice) throws RemoteException {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) throws RemoteException {
        }

        public void onMinimizedStateChanged(boolean z) throws RemoteException {
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) throws RemoteException {
        }

        public void onShelfVisibilityChanged(boolean z, int i) throws RemoteException {
        }

        private BubblesImeListener() {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            if (BubbleController.this.mStackView != null && BubbleController.this.mStackView.getBubbleCount() > 0) {
                BubbleController.this.mStackView.post(new Runnable(z, i) {
                    /* class com.android.systemui.bubbles.$$Lambda$BubbleController$BubblesImeListener$k3Ccv01hiK8jFFaKEuMmcHqId4 */
                    private final /* synthetic */ boolean f$1;
                    private final /* synthetic */ int f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        BubbleController.BubblesImeListener.this.lambda$onImeVisibilityChanged$0$BubbleController$BubblesImeListener(this.f$1, this.f$2);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$onImeVisibilityChanged$0$BubbleController$BubblesImeListener(boolean z, int i) {
            BubbleController.this.mStackView.onImeVisibilityChanged(z, i);
        }
    }
}
