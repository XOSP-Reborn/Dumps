package com.android.systemui.statusbar.notification.row;

import android.app.Notification;
import android.content.Context;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ImageMessageConsumer;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.notification.MediaNotificationProcessor;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.InflatedSmartReplies;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.util.Assert;
import java.util.HashMap;

public class NotificationContentInflater {
    private final ArrayMap<Integer, RemoteViews> mCachedContentViews = new ArrayMap<>();
    private InflationCallback mCallback;
    private boolean mInflateSynchronously = false;
    private int mInflationFlags = 3;
    private boolean mIsChildInGroup;
    private boolean mIsLowPriority;
    private boolean mRedactAmbient;
    private RemoteViews.OnClickHandler mRemoteViewClickHandler;
    private final ExpandableNotificationRow mRow;
    private boolean mUsesIncreasedHeadsUpHeight;
    private boolean mUsesIncreasedHeight;

    public interface InflationCallback {
        void handleInflationException(StatusBarNotification statusBarNotification, Exception exc);

        void onAsyncInflationFinished(NotificationEntry notificationEntry, int i);
    }

    public NotificationContentInflater(ExpandableNotificationRow expandableNotificationRow) {
        this.mRow = expandableNotificationRow;
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
    }

    public void setIsChildInGroup(boolean z) {
        if (z != this.mIsChildInGroup) {
            this.mIsChildInGroup = z;
            if (this.mIsLowPriority) {
                inflateNotificationViews(3);
            }
        }
    }

    public void setUsesIncreasedHeight(boolean z) {
        this.mUsesIncreasedHeight = z;
    }

    public void setUsesIncreasedHeadsUpHeight(boolean z) {
        this.mUsesIncreasedHeadsUpHeight = z;
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mRemoteViewClickHandler = onClickHandler;
    }

    public void updateNeedsRedaction(boolean z) {
        this.mRedactAmbient = z;
        if (this.mRow.getEntry() != null) {
            int i = 8;
            if (z) {
                i = 24;
            }
            inflateNotificationViews(i);
        }
    }

    public void updateInflationFlag(int i, boolean z) {
        if (z) {
            this.mInflationFlags = i | this.mInflationFlags;
        } else if ((i & 3) == 0) {
            this.mInflationFlags = (~i) & this.mInflationFlags;
        }
    }

    @VisibleForTesting
    public void addInflationFlags(int i) {
        this.mInflationFlags = i | this.mInflationFlags;
    }

    public boolean isInflationFlagSet(int i) {
        return (this.mInflationFlags & i) != 0;
    }

    public void inflateNotificationViews() {
        inflateNotificationViews(this.mInflationFlags);
    }

    private void inflateNotificationViews(int i) {
        if (!this.mRow.isRemoved()) {
            int i2 = i & this.mInflationFlags;
            StatusBarNotification statusBarNotification = this.mRow.getEntry().notification;
            this.mRow.getImageResolver().preloadImages(statusBarNotification.getNotification());
            AsyncInflationTask asyncInflationTask = new AsyncInflationTask(statusBarNotification, this.mInflateSynchronously, i2, this.mCachedContentViews, this.mRow, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, this.mCallback, this.mRemoteViewClickHandler);
            if (this.mInflateSynchronously) {
                asyncInflationTask.onPostExecute(asyncInflationTask.doInBackground(new Void[0]));
            } else {
                asyncInflationTask.execute(new Void[0]);
            }
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public InflationProgress inflateNotificationViews(boolean z, int i, Notification.Builder builder, Context context) {
        InflationProgress createRemoteViews = createRemoteViews(i, builder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, context);
        inflateSmartReplyViews(createRemoteViews, i, this.mRow.getEntry(), this.mRow.getContext(), this.mRow.getHeadsUpManager(), this.mRow.getExistingSmartRepliesAndActions());
        apply(z, createRemoteViews, i, this.mCachedContentViews, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, null);
        return createRemoteViews;
    }

    public void freeNotificationView(int i) {
        if ((this.mInflationFlags & i) == 0) {
            if (i != 4) {
                if (i == 8) {
                    boolean isContentViewInactive = this.mRow.getPrivateLayout().isContentViewInactive(4);
                    boolean isContentViewInactive2 = this.mRow.getPublicLayout().isContentViewInactive(4);
                    if (isContentViewInactive) {
                        this.mRow.getPrivateLayout().setAmbientChild(null);
                    }
                    if (isContentViewInactive2) {
                        this.mRow.getPublicLayout().setAmbientChild(null);
                    }
                    if (isContentViewInactive && isContentViewInactive2) {
                        this.mCachedContentViews.remove(8);
                    }
                } else if (i == 16 && this.mRow.getPublicLayout().isContentViewInactive(0)) {
                    this.mRow.getPublicLayout().setContractedChild(null);
                    this.mCachedContentViews.remove(16);
                }
            } else if (this.mRow.getPrivateLayout().isContentViewInactive(2)) {
                this.mRow.getPrivateLayout().setHeadsUpChild(null);
                this.mCachedContentViews.remove(4);
                this.mRow.getPrivateLayout().setHeadsUpInflatedSmartReplies(null);
            }
        }
    }

    /* access modifiers changed from: private */
    public static InflationProgress inflateSmartReplyViews(InflationProgress inflationProgress, int i, NotificationEntry notificationEntry, Context context, HeadsUpManager headsUpManager, InflatedSmartReplies.SmartRepliesAndActions smartRepliesAndActions) {
        SmartReplyConstants smartReplyConstants = (SmartReplyConstants) Dependency.get(SmartReplyConstants.class);
        SmartReplyController smartReplyController = (SmartReplyController) Dependency.get(SmartReplyController.class);
        if (!((i & 2) == 0 || inflationProgress.newExpandedView == null)) {
            inflationProgress.expandedInflatedSmartReplies = InflatedSmartReplies.inflate(context, notificationEntry, smartReplyConstants, smartReplyController, headsUpManager, smartRepliesAndActions);
        }
        if (!((i & 4) == 0 || inflationProgress.newHeadsUpView == null)) {
            inflationProgress.headsUpInflatedSmartReplies = InflatedSmartReplies.inflate(context, notificationEntry, smartReplyConstants, smartReplyController, headsUpManager, smartRepliesAndActions);
        }
        return inflationProgress;
    }

    /* access modifiers changed from: private */
    public static InflationProgress createRemoteViews(int i, Notification.Builder builder, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, Context context) {
        RemoteViews remoteViews;
        InflationProgress inflationProgress = new InflationProgress();
        boolean z6 = z && !z2;
        if ((i & 1) != 0) {
            inflationProgress.newContentView = createContentView(builder, z6, z3);
        }
        if ((i & 2) != 0) {
            inflationProgress.newExpandedView = createExpandedView(builder, z6);
        }
        if ((i & 4) != 0) {
            inflationProgress.newHeadsUpView = builder.createHeadsUpContentView(z4);
        }
        if ((i & 16) != 0) {
            inflationProgress.newPublicView = builder.makePublicContentView();
        }
        if ((i & 8) != 0) {
            if (z5) {
                remoteViews = builder.makePublicAmbientNotification();
            } else {
                remoteViews = builder.makeAmbientNotification();
            }
            inflationProgress.newAmbientView = remoteViews;
        }
        inflationProgress.packageContext = context;
        inflationProgress.headsUpStatusBarText = builder.getHeadsUpStatusBarText(false);
        inflationProgress.headsUpStatusBarTextPublic = builder.getHeadsUpStatusBarText(true);
        return inflationProgress;
    }

    public static CancellationSignal apply(boolean z, final InflationProgress inflationProgress, int i, ArrayMap<Integer, RemoteViews> arrayMap, ExpandableNotificationRow expandableNotificationRow, boolean z2, RemoteViews.OnClickHandler onClickHandler, InflationCallback inflationCallback) {
        HashMap hashMap;
        NotificationContentView notificationContentView;
        NotificationContentView notificationContentView2;
        ArrayMap<Integer, RemoteViews> arrayMap2;
        NotificationContentView notificationContentView3;
        boolean z3;
        final InflationProgress inflationProgress2;
        NotificationContentView notificationContentView4;
        boolean z4;
        ArrayMap<Integer, RemoteViews> arrayMap3;
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        HashMap hashMap2 = new HashMap();
        if ((i & 1) != 0) {
            hashMap = hashMap2;
            notificationContentView2 = publicLayout;
            notificationContentView = privateLayout;
            arrayMap2 = arrayMap;
            applyRemoteView(z, inflationProgress, i, 1, arrayMap, expandableNotificationRow, z2, !canReapplyRemoteView(inflationProgress.newContentView, arrayMap.get(1)), onClickHandler, inflationCallback, privateLayout, privateLayout.getContractedChild(), privateLayout.getVisibleWrapper(0), hashMap, new ApplyCallback() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass1 */

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public void setResultView(View view) {
                    InflationProgress.this.inflatedContentView = view;
                }

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newContentView;
                }
            });
        } else {
            hashMap = hashMap2;
            notificationContentView2 = publicLayout;
            notificationContentView = privateLayout;
            arrayMap2 = arrayMap;
        }
        if ((i & 2) == 0 || inflationProgress.newExpandedView == null) {
            inflationProgress2 = inflationProgress;
            notificationContentView3 = notificationContentView;
            z3 = true;
        } else {
            inflationProgress2 = inflationProgress;
            AnonymousClass2 r12 = new ApplyCallback() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass2 */

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public void setResultView(View view) {
                    InflationProgress.this.inflatedExpandedView = view;
                }

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newExpandedView;
                }
            };
            View expandedChild = notificationContentView.getExpandedChild();
            NotificationViewWrapper visibleWrapper = notificationContentView.getVisibleWrapper(1);
            notificationContentView3 = notificationContentView;
            z3 = true;
            applyRemoteView(z, inflationProgress, i, 2, arrayMap, expandableNotificationRow, z2, !canReapplyRemoteView(inflationProgress.newExpandedView, arrayMap2.get(2)), onClickHandler, inflationCallback, notificationContentView, expandedChild, visibleWrapper, hashMap, r12);
        }
        if (!((i & 4) == 0 || inflationProgress.newHeadsUpView == null)) {
            applyRemoteView(z, inflationProgress, i, 4, arrayMap, expandableNotificationRow, z2, !canReapplyRemoteView(inflationProgress.newHeadsUpView, arrayMap.get(4)), onClickHandler, inflationCallback, notificationContentView3, notificationContentView3.getHeadsUpChild(), notificationContentView3.getVisibleWrapper(2), hashMap, new ApplyCallback() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass3 */

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public void setResultView(View view) {
                    InflationProgress.this.inflatedHeadsUpView = view;
                }

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newHeadsUpView;
                }
            });
        }
        if ((i & 16) != 0) {
            AnonymousClass4 r13 = new ApplyCallback() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass4 */

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public void setResultView(View view) {
                    InflationProgress.this.inflatedPublicView = view;
                }

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newPublicView;
                }
            };
            z4 = false;
            notificationContentView4 = notificationContentView2;
            arrayMap3 = arrayMap;
            applyRemoteView(z, inflationProgress, i, 16, arrayMap, expandableNotificationRow, z2, !canReapplyRemoteView(inflationProgress.newPublicView, arrayMap.get(16)), onClickHandler, inflationCallback, notificationContentView2, notificationContentView2.getContractedChild(), notificationContentView2.getVisibleWrapper(0), hashMap, r13);
        } else {
            arrayMap3 = arrayMap;
            notificationContentView4 = notificationContentView2;
            z4 = false;
        }
        if ((i & 8) != 0) {
            NotificationContentView notificationContentView5 = z2 ? notificationContentView4 : notificationContentView3;
            applyRemoteView(z, inflationProgress, i, 8, arrayMap, expandableNotificationRow, z2, (!canReapplyAmbient(expandableNotificationRow, z2) || !canReapplyRemoteView(inflationProgress.newAmbientView, arrayMap3.get(8))) ? z3 : z4, onClickHandler, inflationCallback, notificationContentView5, notificationContentView5.getAmbientChild(), notificationContentView5.getVisibleWrapper(4), hashMap, new ApplyCallback() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass5 */

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public void setResultView(View view) {
                    InflationProgress.this.inflatedAmbientView = view;
                }

                @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newAmbientView;
                }
            });
        }
        finishIfDone(inflationProgress, i, arrayMap, hashMap, inflationCallback, expandableNotificationRow, z2);
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener(hashMap) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationContentInflater$WjCddtvZmmNqAdGsBYXcbiOdWQY */
            private final /* synthetic */ HashMap f$0;

            {
                this.f$0 = r1;
            }

            public final void onCancel() {
                this.f$0.values().forEach($$Lambda$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
            }
        });
        return cancellationSignal;
    }

    @VisibleForTesting
    static void applyRemoteView(boolean z, final InflationProgress inflationProgress, final int i, final int i2, final ArrayMap<Integer, RemoteViews> arrayMap, final ExpandableNotificationRow expandableNotificationRow, final boolean z2, final boolean z3, final RemoteViews.OnClickHandler onClickHandler, final InflationCallback inflationCallback, final NotificationContentView notificationContentView, final View view, final NotificationViewWrapper notificationViewWrapper, final HashMap<Integer, CancellationSignal> hashMap, final ApplyCallback applyCallback) {
        CancellationSignal cancellationSignal;
        final RemoteViews remoteView = applyCallback.getRemoteView();
        if (!z) {
            AnonymousClass6 r17 = new RemoteViews.OnViewAppliedListener() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentInflater.AnonymousClass6 */

                public void onViewInflated(View view) {
                    if (view instanceof ImageMessageConsumer) {
                        ((ImageMessageConsumer) view).setImageResolver(ExpandableNotificationRow.this.getImageResolver());
                    }
                }

                public void onViewApplied(View view) {
                    if (z3) {
                        view.setIsRootNamespace(true);
                        applyCallback.setResultView(view);
                    } else {
                        NotificationViewWrapper notificationViewWrapper = notificationViewWrapper;
                        if (notificationViewWrapper != null) {
                            notificationViewWrapper.onReinflated();
                        }
                    }
                    hashMap.remove(Integer.valueOf(i2));
                    NotificationContentInflater.finishIfDone(inflationProgress, i, arrayMap, hashMap, inflationCallback, ExpandableNotificationRow.this, z2);
                }

                public void onError(Exception exc) {
                    try {
                        View view = view;
                        if (z3) {
                            view = remoteView.apply(inflationProgress.packageContext, notificationContentView, onClickHandler);
                        } else {
                            remoteView.reapply(inflationProgress.packageContext, view, onClickHandler);
                        }
                        Log.wtf("NotifContentInflater", "Async Inflation failed but normal inflation finished normally.", exc);
                        onViewApplied(view);
                    } catch (Exception unused) {
                        hashMap.remove(Integer.valueOf(i2));
                        NotificationContentInflater.handleInflationError(hashMap, exc, ExpandableNotificationRow.this.getStatusBarNotification(), inflationCallback);
                    }
                }
            };
            if (z3) {
                cancellationSignal = remoteView.applyAsync(inflationProgress.packageContext, notificationContentView, null, r17, onClickHandler);
            } else {
                cancellationSignal = remoteView.reapplyAsync(inflationProgress.packageContext, view, null, r17, onClickHandler);
            }
            hashMap.put(Integer.valueOf(i2), cancellationSignal);
        } else if (z3) {
            try {
                View apply = remoteView.apply(inflationProgress.packageContext, notificationContentView, onClickHandler);
                apply.setIsRootNamespace(true);
                applyCallback.setResultView(apply);
            } catch (Exception e) {
                handleInflationError(hashMap, e, expandableNotificationRow.getStatusBarNotification(), inflationCallback);
                hashMap.put(Integer.valueOf(i2), new CancellationSignal());
            }
        } else {
            remoteView.reapply(inflationProgress.packageContext, view, onClickHandler);
            notificationViewWrapper.onReinflated();
        }
    }

    /* access modifiers changed from: private */
    public static void handleInflationError(HashMap<Integer, CancellationSignal> hashMap, Exception exc, StatusBarNotification statusBarNotification, InflationCallback inflationCallback) {
        Assert.isMainThread();
        hashMap.values().forEach($$Lambda$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
        if (inflationCallback != null) {
            inflationCallback.handleInflationException(statusBarNotification, exc);
        }
    }

    /* access modifiers changed from: private */
    public static boolean finishIfDone(InflationProgress inflationProgress, int i, ArrayMap<Integer, RemoteViews> arrayMap, HashMap<Integer, CancellationSignal> hashMap, InflationCallback inflationCallback, ExpandableNotificationRow expandableNotificationRow, boolean z) {
        Assert.isMainThread();
        NotificationEntry entry = expandableNotificationRow.getEntry();
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        boolean z2 = false;
        if (!hashMap.isEmpty()) {
            return false;
        }
        if ((i & 1) != 0) {
            if (inflationProgress.inflatedContentView != null) {
                privateLayout.setContractedChild(inflationProgress.inflatedContentView);
                arrayMap.put(1, inflationProgress.newContentView);
            } else if (arrayMap.get(1) != null) {
                arrayMap.put(1, inflationProgress.newContentView);
            }
        }
        if ((i & 2) != 0) {
            if (inflationProgress.inflatedExpandedView != null) {
                privateLayout.setExpandedChild(inflationProgress.inflatedExpandedView);
                arrayMap.put(2, inflationProgress.newExpandedView);
            } else if (inflationProgress.newExpandedView == null) {
                privateLayout.setExpandedChild(null);
                arrayMap.put(2, null);
            } else if (arrayMap.get(2) != null) {
                arrayMap.put(2, inflationProgress.newExpandedView);
            }
            if (inflationProgress.newExpandedView != null) {
                privateLayout.setExpandedInflatedSmartReplies(inflationProgress.expandedInflatedSmartReplies);
            } else {
                privateLayout.setExpandedInflatedSmartReplies(null);
            }
            if (inflationProgress.newExpandedView != null) {
                z2 = true;
            }
            expandableNotificationRow.setExpandable(z2);
        }
        if ((i & 4) != 0) {
            if (inflationProgress.inflatedHeadsUpView != null) {
                privateLayout.setHeadsUpChild(inflationProgress.inflatedHeadsUpView);
                arrayMap.put(4, inflationProgress.newHeadsUpView);
            } else if (inflationProgress.newHeadsUpView == null) {
                privateLayout.setHeadsUpChild(null);
                arrayMap.put(4, null);
            } else if (arrayMap.get(4) != null) {
                arrayMap.put(4, inflationProgress.newHeadsUpView);
            }
            if (inflationProgress.newHeadsUpView != null) {
                privateLayout.setHeadsUpInflatedSmartReplies(inflationProgress.headsUpInflatedSmartReplies);
            } else {
                privateLayout.setHeadsUpInflatedSmartReplies(null);
            }
        }
        if ((i & 16) != 0) {
            if (inflationProgress.inflatedPublicView != null) {
                publicLayout.setContractedChild(inflationProgress.inflatedPublicView);
                arrayMap.put(16, inflationProgress.newPublicView);
            } else if (arrayMap.get(16) != null) {
                arrayMap.put(16, inflationProgress.newPublicView);
            }
        }
        if ((i & 8) != 0) {
            if (inflationProgress.inflatedAmbientView != null) {
                NotificationContentView notificationContentView = z ? publicLayout : privateLayout;
                if (!z) {
                    privateLayout = publicLayout;
                }
                notificationContentView.setAmbientChild(inflationProgress.inflatedAmbientView);
                privateLayout.setAmbientChild(null);
                arrayMap.put(8, inflationProgress.newAmbientView);
            } else if (arrayMap.get(8) != null) {
                arrayMap.put(8, inflationProgress.newAmbientView);
            }
        }
        entry.headsUpStatusBarText = inflationProgress.headsUpStatusBarText;
        entry.headsUpStatusBarTextPublic = inflationProgress.headsUpStatusBarTextPublic;
        if (inflationCallback != null) {
            inflationCallback.onAsyncInflationFinished(expandableNotificationRow.getEntry(), i);
        }
        return true;
    }

    private static RemoteViews createExpandedView(Notification.Builder builder, boolean z) {
        RemoteViews createBigContentView = builder.createBigContentView();
        if (createBigContentView != null) {
            return createBigContentView;
        }
        if (!z) {
            return null;
        }
        RemoteViews createContentView = builder.createContentView();
        Notification.Builder.makeHeaderExpanded(createContentView);
        return createContentView;
    }

    private static RemoteViews createContentView(Notification.Builder builder, boolean z, boolean z2) {
        if (z) {
            return builder.makeLowPriorityContentView(false);
        }
        return builder.createContentView(z2);
    }

    @VisibleForTesting
    static boolean canReapplyRemoteView(RemoteViews remoteViews, RemoteViews remoteViews2) {
        if (remoteViews == null && remoteViews2 == null) {
            return true;
        }
        if (remoteViews == null || remoteViews2 == null || remoteViews2.getPackage() == null || remoteViews.getPackage() == null || !remoteViews.getPackage().equals(remoteViews2.getPackage()) || remoteViews.getLayoutId() != remoteViews2.getLayoutId() || remoteViews2.hasFlags(1)) {
            return false;
        }
        return true;
    }

    public void setInflationCallback(InflationCallback inflationCallback) {
        this.mCallback = inflationCallback;
    }

    public void clearCachesAndReInflate() {
        this.mCachedContentViews.clear();
        inflateNotificationViews();
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setInflateSynchronously(boolean z) {
        this.mInflateSynchronously = z;
    }

    private static boolean canReapplyAmbient(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        NotificationContentView notificationContentView;
        if (z) {
            notificationContentView = expandableNotificationRow.getPublicLayout();
        } else {
            notificationContentView = expandableNotificationRow.getPrivateLayout();
        }
        return notificationContentView.getAmbientChild() != null;
    }

    public static class AsyncInflationTask extends AsyncTask<Void, Void, InflationProgress> implements InflationCallback, InflationTask {
        private final ArrayMap<Integer, RemoteViews> mCachedContentViews;
        private final InflationCallback mCallback;
        private CancellationSignal mCancellationSignal;
        private final Context mContext;
        private Exception mError;
        private final boolean mInflateSynchronously;
        private final boolean mIsChildInGroup;
        private final boolean mIsLowPriority;
        private int mReInflateFlags;
        private final boolean mRedactAmbient;
        private RemoteViews.OnClickHandler mRemoteViewClickHandler;
        private ExpandableNotificationRow mRow;
        private final StatusBarNotification mSbn;
        private final boolean mUsesIncreasedHeadsUpHeight;
        private final boolean mUsesIncreasedHeight;

        private AsyncInflationTask(StatusBarNotification statusBarNotification, boolean z, int i, ArrayMap<Integer, RemoteViews> arrayMap, ExpandableNotificationRow expandableNotificationRow, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, InflationCallback inflationCallback, RemoteViews.OnClickHandler onClickHandler) {
            this.mRow = expandableNotificationRow;
            this.mSbn = statusBarNotification;
            this.mInflateSynchronously = z;
            this.mReInflateFlags = i;
            this.mCachedContentViews = arrayMap;
            this.mContext = this.mRow.getContext();
            this.mIsLowPriority = z2;
            this.mIsChildInGroup = z3;
            this.mUsesIncreasedHeight = z4;
            this.mUsesIncreasedHeadsUpHeight = z5;
            this.mRedactAmbient = z6;
            this.mRemoteViewClickHandler = onClickHandler;
            this.mCallback = inflationCallback;
            expandableNotificationRow.getEntry().setInflationTask(this);
        }

        @VisibleForTesting
        public int getReInflateFlags() {
            return this.mReInflateFlags;
        }

        /* access modifiers changed from: protected */
        public InflationProgress doInBackground(Void... voidArr) {
            try {
                Notification.Builder recoverBuilder = Notification.Builder.recoverBuilder(this.mContext, this.mSbn.getNotification());
                Context packageContext = this.mSbn.getPackageContext(this.mContext);
                Notification notification = this.mSbn.getNotification();
                if (notification.isMediaNotification()) {
                    new MediaNotificationProcessor(this.mContext, packageContext).processNotification(notification, recoverBuilder);
                }
                InflationProgress createRemoteViews = NotificationContentInflater.createRemoteViews(this.mReInflateFlags, recoverBuilder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, packageContext);
                NotificationContentInflater.inflateSmartReplyViews(createRemoteViews, this.mReInflateFlags, this.mRow.getEntry(), this.mRow.getContext(), this.mRow.getHeadsUpManager(), this.mRow.getExistingSmartRepliesAndActions());
                return createRemoteViews;
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(InflationProgress inflationProgress) {
            Exception exc = this.mError;
            if (exc == null) {
                this.mCancellationSignal = NotificationContentInflater.apply(this.mInflateSynchronously, inflationProgress, this.mReInflateFlags, this.mCachedContentViews, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, this);
            } else {
                handleError(exc);
            }
        }

        private void handleError(Exception exc) {
            this.mRow.getEntry().onInflationTaskFinished();
            StatusBarNotification statusBarNotification = this.mRow.getStatusBarNotification();
            Log.e("StatusBar", "couldn't inflate view for notification " + (statusBarNotification.getPackageName() + "/0x" + Integer.toHexString(statusBarNotification.getId())), exc);
            this.mCallback.handleInflationException(statusBarNotification, new InflationException("Couldn't inflate contentViews" + exc));
        }

        @Override // com.android.systemui.statusbar.InflationTask
        public void abort() {
            cancel(true);
            CancellationSignal cancellationSignal = this.mCancellationSignal;
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }

        @Override // com.android.systemui.statusbar.InflationTask
        public void supersedeTask(InflationTask inflationTask) {
            if (inflationTask instanceof AsyncInflationTask) {
                this.mReInflateFlags = ((AsyncInflationTask) inflationTask).mReInflateFlags | this.mReInflateFlags;
            }
        }

        @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.InflationCallback
        public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
            handleError(exc);
        }

        @Override // com.android.systemui.statusbar.notification.row.NotificationContentInflater.InflationCallback
        public void onAsyncInflationFinished(NotificationEntry notificationEntry, int i) {
            this.mRow.getEntry().onInflationTaskFinished();
            this.mRow.onNotificationUpdated();
            this.mCallback.onAsyncInflationFinished(this.mRow.getEntry(), i);
            this.mRow.getImageResolver().purgeCache();
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public static class InflationProgress {
        private InflatedSmartReplies expandedInflatedSmartReplies;
        private InflatedSmartReplies headsUpInflatedSmartReplies;
        private CharSequence headsUpStatusBarText;
        private CharSequence headsUpStatusBarTextPublic;
        private View inflatedAmbientView;
        private View inflatedContentView;
        private View inflatedExpandedView;
        private View inflatedHeadsUpView;
        private View inflatedPublicView;
        private RemoteViews newAmbientView;
        private RemoteViews newContentView;
        private RemoteViews newExpandedView;
        private RemoteViews newHeadsUpView;
        private RemoteViews newPublicView;
        @VisibleForTesting
        Context packageContext;

        InflationProgress() {
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public static abstract class ApplyCallback {
        public abstract RemoteViews getRemoteView();

        public abstract void setResultView(View view);

        ApplyCallback() {
        }
    }
}
