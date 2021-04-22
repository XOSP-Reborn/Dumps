package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.dreams.IDreamManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.RemoteAnimationAdapter;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.ActivityIntentHelper;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationActivityStarter;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.Objects;

public class StatusBarNotificationActivityStarter implements NotificationActivityStarter {
    protected static final boolean DEBUG = Log.isLoggable("NotificationClickHandler", 3);
    private final ActivityIntentHelper mActivityIntentHelper;
    private final ActivityLaunchAnimator mActivityLaunchAnimator;
    private final ActivityStarter mActivityStarter;
    private final AssistManager mAssistManager;
    private final Handler mBackgroundHandler;
    private final IStatusBarService mBarService;
    private final BubbleController mBubbleController;
    private final CommandQueue mCommandQueue;
    private final Context mContext;
    private final IDreamManager mDreamManager;
    private final NotificationEntryManager mEntryManager;
    private final NotificationGroupManager mGroupManager;
    private final HeadsUpManagerPhone mHeadsUpManager;
    private boolean mIsCollapsingToShowActivityOverLockscreen;
    private final KeyguardManager mKeyguardManager;
    private final KeyguardMonitor mKeyguardMonitor;
    private final LockPatternUtils mLockPatternUtils;
    private final NotificationLockscreenUserManager mLockscreenUserManager;
    private final Handler mMainThreadHandler;
    private final MetricsLogger mMetricsLogger;
    private final NotificationInterruptionStateProvider mNotificationInterruptionStateProvider;
    private final NotificationPanelView mNotificationPanel;
    private final NotificationPresenter mPresenter;
    private final NotificationRemoteInputManager mRemoteInputManager;
    private final ShadeController mShadeController;
    private final StatusBarRemoteInputCallback mStatusBarRemoteInputCallback;
    private final StatusBarStateController mStatusBarStateController;

    public StatusBarNotificationActivityStarter(Context context, CommandQueue commandQueue, AssistManager assistManager, NotificationPanelView notificationPanelView, NotificationPresenter notificationPresenter, NotificationEntryManager notificationEntryManager, HeadsUpManagerPhone headsUpManagerPhone, ActivityStarter activityStarter, ActivityLaunchAnimator activityLaunchAnimator, IStatusBarService iStatusBarService, StatusBarStateController statusBarStateController, KeyguardManager keyguardManager, IDreamManager iDreamManager, NotificationRemoteInputManager notificationRemoteInputManager, StatusBarRemoteInputCallback statusBarRemoteInputCallback, NotificationGroupManager notificationGroupManager, NotificationLockscreenUserManager notificationLockscreenUserManager, ShadeController shadeController, KeyguardMonitor keyguardMonitor, NotificationInterruptionStateProvider notificationInterruptionStateProvider, MetricsLogger metricsLogger, LockPatternUtils lockPatternUtils, Handler handler, Handler handler2, ActivityIntentHelper activityIntentHelper, BubbleController bubbleController) {
        this.mContext = context;
        this.mNotificationPanel = notificationPanelView;
        this.mPresenter = notificationPresenter;
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mActivityLaunchAnimator = activityLaunchAnimator;
        this.mBarService = iStatusBarService;
        this.mCommandQueue = commandQueue;
        this.mKeyguardManager = keyguardManager;
        this.mDreamManager = iDreamManager;
        this.mRemoteInputManager = notificationRemoteInputManager;
        this.mLockscreenUserManager = notificationLockscreenUserManager;
        this.mShadeController = shadeController;
        this.mKeyguardMonitor = keyguardMonitor;
        this.mActivityStarter = activityStarter;
        this.mEntryManager = notificationEntryManager;
        this.mStatusBarStateController = statusBarStateController;
        this.mNotificationInterruptionStateProvider = notificationInterruptionStateProvider;
        this.mMetricsLogger = metricsLogger;
        this.mAssistManager = assistManager;
        this.mGroupManager = notificationGroupManager;
        this.mLockPatternUtils = lockPatternUtils;
        this.mBackgroundHandler = handler2;
        this.mEntryManager.addNotificationEntryListener(new NotificationEntryListener() {
            /* class com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter.AnonymousClass1 */

            @Override // com.android.systemui.statusbar.notification.NotificationEntryListener
            public void onPendingEntryAdded(NotificationEntry notificationEntry) {
                StatusBarNotificationActivityStarter.this.handleFullScreenIntent(notificationEntry);
            }
        });
        this.mStatusBarRemoteInputCallback = statusBarRemoteInputCallback;
        this.mMainThreadHandler = handler;
        this.mActivityIntentHelper = activityIntentHelper;
        this.mBubbleController = bubbleController;
    }

    @Override // com.android.systemui.statusbar.notification.NotificationActivityStarter
    public void onNotificationClicked(StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow) {
        PendingIntent pendingIntent;
        RemoteInputController controller = this.mRemoteInputManager.getController();
        if (!controller.isRemoteInputActive(expandableNotificationRow.getEntry()) || TextUtils.isEmpty(expandableNotificationRow.getActiveRemoteInputText())) {
            Notification notification = statusBarNotification.getNotification();
            PendingIntent pendingIntent2 = notification.contentIntent;
            if (pendingIntent2 != null) {
                pendingIntent = pendingIntent2;
            } else {
                pendingIntent = notification.fullScreenIntent;
            }
            boolean isBubble = expandableNotificationRow.getEntry().isBubble();
            if (pendingIntent != null || isBubble) {
                String key = statusBarNotification.getKey();
                boolean z = pendingIntent != null && pendingIntent.isActivity() && !isBubble;
                boolean z2 = z && this.mActivityIntentHelper.wouldLaunchResolverActivity(pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId());
                boolean isOccluded = this.mShadeController.isOccluded();
                boolean z3 = this.mKeyguardMonitor.isShowing() && pendingIntent != null && this.mActivityIntentHelper.wouldShowOverLockscreen(pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId());
                $$Lambda$StatusBarNotificationActivityStarter$yPRbQ0J1oyZW5IHADurUixbhRxg r14 = new ActivityStarter.OnDismissAction(statusBarNotification, expandableNotificationRow, controller, pendingIntent, key, z, isOccluded, z3) {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$yPRbQ0J1oyZW5IHADurUixbhRxg */
                    private final /* synthetic */ StatusBarNotification f$1;
                    private final /* synthetic */ ExpandableNotificationRow f$2;
                    private final /* synthetic */ RemoteInputController f$3;
                    private final /* synthetic */ PendingIntent f$4;
                    private final /* synthetic */ String f$5;
                    private final /* synthetic */ boolean f$6;
                    private final /* synthetic */ boolean f$7;
                    private final /* synthetic */ boolean f$8;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                        this.f$4 = r5;
                        this.f$5 = r6;
                        this.f$6 = r7;
                        this.f$7 = r8;
                        this.f$8 = r9;
                    }

                    @Override // com.android.systemui.plugins.ActivityStarter.OnDismissAction
                    public final boolean onDismiss() {
                        return StatusBarNotificationActivityStarter.this.lambda$onNotificationClicked$0$StatusBarNotificationActivityStarter(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8);
                    }
                };
                if (z3) {
                    this.mIsCollapsingToShowActivityOverLockscreen = true;
                    r14.onDismiss();
                    return;
                }
                this.mActivityStarter.dismissKeyguardThenExecute(r14, null, z2);
                return;
            }
            Log.e("NotificationClickHandler", "onNotificationClicked called for non-clickable notification!");
            return;
        }
        controller.closeRemoteInputs();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x006a  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0093 A[ORIG_RETURN, RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:30:? A[RETURN, SYNTHETIC] */
    /* renamed from: handleNotificationClickAfterKeyguardDismissed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean lambda$onNotificationClicked$0$StatusBarNotificationActivityStarter(android.service.notification.StatusBarNotification r14, com.android.systemui.statusbar.notification.row.ExpandableNotificationRow r15, com.android.systemui.statusbar.RemoteInputController r16, android.app.PendingIntent r17, java.lang.String r18, boolean r19, boolean r20, boolean r21) {
        /*
        // Method dump skipped, instructions count: 149
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter.lambda$onNotificationClicked$0$StatusBarNotificationActivityStarter(android.service.notification.StatusBarNotification, com.android.systemui.statusbar.notification.row.ExpandableNotificationRow, com.android.systemui.statusbar.RemoteInputController, android.app.PendingIntent, java.lang.String, boolean, boolean, boolean):boolean");
    }

    /* access modifiers changed from: private */
    /* renamed from: handleNotificationClickAfterPanelCollapsed */
    public void lambda$handleNotificationClickAfterKeyguardDismissed$1$StatusBarNotificationActivityStarter(StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow, RemoteInputController remoteInputController, PendingIntent pendingIntent, String str, boolean z, boolean z2, StatusBarNotification statusBarNotification2) {
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException unused) {
        }
        if (z) {
            int identifier = pendingIntent.getCreatorUserHandle().getIdentifier();
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(identifier) && this.mKeyguardManager.isDeviceLocked(identifier) && this.mStatusBarRemoteInputCallback.startWorkChallengeIfNecessary(identifier, pendingIntent.getIntentSender(), str)) {
                collapseOnMainThread();
                return;
            }
        }
        NotificationEntry entry = expandableNotificationRow.getEntry();
        boolean isBubble = entry.isBubble();
        Intent intent = null;
        CharSequence charSequence = !TextUtils.isEmpty(entry.remoteInputText) ? entry.remoteInputText : null;
        if (!TextUtils.isEmpty(charSequence) && !remoteInputController.isSpinning(entry.key)) {
            intent = new Intent().putExtra("android.remoteInputDraft", charSequence.toString());
        }
        if (isBubble) {
            expandBubbleStackOnMainThread(str);
        } else {
            startNotificationIntent(pendingIntent, intent, expandableNotificationRow, z2, z);
        }
        if (z || isBubble) {
            this.mAssistManager.hideAssist();
        }
        if (shouldCollapse()) {
            collapseOnMainThread();
        }
        try {
            this.mBarService.onNotificationClick(str, NotificationVisibility.obtain(str, this.mEntryManager.getNotificationData().getRank(str), this.mEntryManager.getNotificationData().getActiveNotifications().size(), true, NotificationLogger.getNotificationLocation(this.mEntryManager.getNotificationData().get(str))));
        } catch (RemoteException unused2) {
        }
        if (!isBubble) {
            if (statusBarNotification2 != null) {
                removeNotification(statusBarNotification2);
            }
            if (shouldAutoCancel(statusBarNotification) || this.mRemoteInputManager.isNotificationKeptForRemoteInputHistory(str)) {
                removeNotification(statusBarNotification);
            }
        }
        this.mIsCollapsingToShowActivityOverLockscreen = false;
    }

    private void expandBubbleStackOnMainThread(String str) {
        if (Looper.getMainLooper().isCurrentThread()) {
            this.mBubbleController.expandStackAndSelectBubble(str);
        } else {
            this.mMainThreadHandler.post(new Runnable(str) {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$SAG_ctHvOhll_OxtSgOBbXZGGw */
                private final /* synthetic */ String f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    StatusBarNotificationActivityStarter.this.lambda$expandBubbleStackOnMainThread$2$StatusBarNotificationActivityStarter(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$expandBubbleStackOnMainThread$2$StatusBarNotificationActivityStarter(String str) {
        this.mBubbleController.expandStackAndSelectBubble(str);
    }

    private void startNotificationIntent(PendingIntent pendingIntent, Intent intent, ExpandableNotificationRow expandableNotificationRow, boolean z, boolean z2) {
        RemoteAnimationAdapter launchAnimation = this.mActivityLaunchAnimator.getLaunchAnimation(expandableNotificationRow, z);
        if (launchAnimation != null) {
            try {
                ActivityTaskManager.getService().registerRemoteAnimationForNextActivityStart(pendingIntent.getCreatorPackage(), launchAnimation);
            } catch (PendingIntent.CanceledException | RemoteException e) {
                Log.w("NotificationClickHandler", "Sending contentIntent failed: " + e);
                return;
            }
        }
        this.mActivityLaunchAnimator.setLaunchResult(pendingIntent.sendAndReturnResult(this.mContext, 0, intent, null, null, null, StatusBar.getActivityOptions(launchAnimation)), z2);
    }

    @Override // com.android.systemui.statusbar.notification.NotificationActivityStarter
    public void startNotificationGutsIntent(Intent intent, int i, ExpandableNotificationRow expandableNotificationRow) {
        this.mActivityStarter.dismissKeyguardThenExecute(new ActivityStarter.OnDismissAction(intent, expandableNotificationRow, i) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$cyhnCXwOFANppGr5Crfg0gR112k */
            private final /* synthetic */ Intent f$1;
            private final /* synthetic */ ExpandableNotificationRow f$2;
            private final /* synthetic */ int f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            @Override // com.android.systemui.plugins.ActivityStarter.OnDismissAction
            public final boolean onDismiss() {
                return StatusBarNotificationActivityStarter.this.lambda$startNotificationGutsIntent$5$StatusBarNotificationActivityStarter(this.f$1, this.f$2, this.f$3);
            }
        }, null, false);
    }

    public /* synthetic */ boolean lambda$startNotificationGutsIntent$5$StatusBarNotificationActivityStarter(Intent intent, ExpandableNotificationRow expandableNotificationRow, int i) {
        AsyncTask.execute(new Runnable(intent, expandableNotificationRow, i) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$SrsXjl_aP_YXf0BoPG0DcKfnIqA */
            private final /* synthetic */ Intent f$1;
            private final /* synthetic */ ExpandableNotificationRow f$2;
            private final /* synthetic */ int f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                StatusBarNotificationActivityStarter.this.lambda$startNotificationGutsIntent$4$StatusBarNotificationActivityStarter(this.f$1, this.f$2, this.f$3);
            }
        });
        return true;
    }

    public /* synthetic */ void lambda$startNotificationGutsIntent$4$StatusBarNotificationActivityStarter(Intent intent, ExpandableNotificationRow expandableNotificationRow, int i) {
        this.mActivityLaunchAnimator.setLaunchResult(TaskStackBuilder.create(this.mContext).addNextIntentWithParentStack(intent).startActivities(StatusBar.getActivityOptions(this.mActivityLaunchAnimator.getLaunchAnimation(expandableNotificationRow, this.mShadeController.isOccluded())), new UserHandle(UserHandle.getUserId(i))), true);
        if (shouldCollapse()) {
            this.mMainThreadHandler.post(new Runnable() {
                /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$ZBMwRNC8tX8dffchdtumyW_afiA */

                public final void run() {
                    StatusBarNotificationActivityStarter.this.lambda$startNotificationGutsIntent$3$StatusBarNotificationActivityStarter();
                }
            });
        }
    }

    public /* synthetic */ void lambda$startNotificationGutsIntent$3$StatusBarNotificationActivityStarter() {
        this.mCommandQueue.animateCollapsePanels(2, true);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleFullScreenIntent(NotificationEntry notificationEntry) {
        if (!this.mNotificationInterruptionStateProvider.shouldHeadsUp(notificationEntry) && notificationEntry.notification.getNotification().fullScreenIntent != null) {
            if (shouldSuppressFullScreenIntent(notificationEntry)) {
                if (DEBUG) {
                    Log.d("NotificationClickHandler", "No Fullscreen intent: suppressed by DND: " + notificationEntry.key);
                }
            } else if (notificationEntry.importance >= 4) {
                ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit(new Runnable() {
                    /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$r9RsnGtfcZuVJemyK82SlR0x7o */

                    public final void run() {
                        StatusBarNotificationActivityStarter.this.lambda$handleFullScreenIntent$6$StatusBarNotificationActivityStarter();
                    }
                });
                if (DEBUG) {
                    Log.d("NotificationClickHandler", "Notification has fullScreenIntent; sending fullScreenIntent");
                }
                try {
                    EventLog.writeEvent(36002, notificationEntry.key);
                    notificationEntry.notification.getNotification().fullScreenIntent.send();
                    notificationEntry.notifyFullScreenIntentLaunched();
                    this.mMetricsLogger.count("note_fullscreen", 1);
                } catch (PendingIntent.CanceledException unused) {
                }
            } else if (DEBUG) {
                Log.d("NotificationClickHandler", "No Fullscreen intent: not important enough: " + notificationEntry.key);
            }
        }
    }

    public /* synthetic */ void lambda$handleFullScreenIntent$6$StatusBarNotificationActivityStarter() {
        try {
            this.mDreamManager.awaken();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override // com.android.systemui.statusbar.notification.NotificationActivityStarter
    public boolean isCollapsingToShowActivityOverLockscreen() {
        return this.mIsCollapsingToShowActivityOverLockscreen;
    }

    private static boolean shouldAutoCancel(StatusBarNotification statusBarNotification) {
        int i = statusBarNotification.getNotification().flags;
        return (i & 16) == 16 && (i & 64) == 0;
    }

    private void collapseOnMainThread() {
        if (Looper.getMainLooper().isCurrentThread()) {
            this.mShadeController.collapsePanel();
            return;
        }
        Handler handler = this.mMainThreadHandler;
        ShadeController shadeController = this.mShadeController;
        Objects.requireNonNull(shadeController);
        handler.post(new Runnable() {
            /* class com.android.systemui.statusbar.phone.$$Lambda$XDmf1V0qHGBRkxV63RRNIpOXuQ */

            public final void run() {
                ShadeController.this.collapsePanel();
            }
        });
    }

    private boolean shouldCollapse() {
        return this.mStatusBarStateController.getState() != 0 || !this.mActivityLaunchAnimator.isAnimationPending();
    }

    private boolean shouldSuppressFullScreenIntent(NotificationEntry notificationEntry) {
        if (this.mPresenter.isDeviceInVrMode()) {
            return true;
        }
        return notificationEntry.shouldSuppressFullScreenIntent();
    }

    private void removeNotification(StatusBarNotification statusBarNotification) {
        this.mMainThreadHandler.post(new Runnable(statusBarNotification) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$SjtS704WtC3nfx3PLxg0F_Agha4 */
            private final /* synthetic */ StatusBarNotification f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBarNotificationActivityStarter.this.lambda$removeNotification$8$StatusBarNotificationActivityStarter(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$removeNotification$8$StatusBarNotificationActivityStarter(StatusBarNotification statusBarNotification) {
        $$Lambda$StatusBarNotificationActivityStarter$66g6EDAhWCxfzMKE6qPXX_8qGwI r0 = new Runnable(statusBarNotification) {
            /* class com.android.systemui.statusbar.phone.$$Lambda$StatusBarNotificationActivityStarter$66g6EDAhWCxfzMKE6qPXX_8qGwI */
            private final /* synthetic */ StatusBarNotification f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBarNotificationActivityStarter.this.lambda$removeNotification$7$StatusBarNotificationActivityStarter(this.f$1);
            }
        };
        if (this.mPresenter.isCollapsing()) {
            this.mShadeController.addPostCollapseAction(r0);
        } else {
            r0.run();
        }
    }

    public /* synthetic */ void lambda$removeNotification$7$StatusBarNotificationActivityStarter(StatusBarNotification statusBarNotification) {
        this.mEntryManager.performRemoveNotification(statusBarNotification, 1);
    }
}
