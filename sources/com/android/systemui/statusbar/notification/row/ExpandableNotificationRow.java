package com.android.systemui.statusbar.notification.row;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.MathUtils;
import android.util.Property;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ContrastColorUtil;
import com.android.internal.widget.CachingIconView;
import com.android.settingslib.Utils;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.AboveShelfChangedListener;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationContentInflater;
import com.android.systemui.statusbar.notification.row.NotificationGuts;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.notification.stack.AmbientState;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.ExpandableViewState;
import com.android.systemui.statusbar.notification.stack.NotificationChildrenContainer;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.InflatedSmartReplies;
import com.sonymobile.settingslib.SomcConfig;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ExpandableNotificationRow extends ActivatableNotificationView implements PluginListener<NotificationMenuRowPlugin> {
    private static final long RECENTLY_ALERTED_THRESHOLD_MS = TimeUnit.SECONDS.toMillis(30);
    private static final Property<ExpandableNotificationRow, Float> TRANSLATE_CONTENT = new FloatProperty<ExpandableNotificationRow>("translate") {
        /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass2 */

        public void setValue(ExpandableNotificationRow expandableNotificationRow, float f) {
            expandableNotificationRow.setTranslation(f);
        }

        public Float get(ExpandableNotificationRow expandableNotificationRow) {
            return Float.valueOf(expandableNotificationRow.getTranslation());
        }
    };
    private boolean mAboveShelf;
    private AboveShelfChangedListener mAboveShelfChangedListener;
    private boolean mAmbientGoingAway;
    private String mAppName;
    private View mChildAfterViewWhenDismissed;
    private boolean mChildIsExpanding;
    private NotificationChildrenContainer mChildrenContainer;
    private ViewStub mChildrenContainerStub;
    private boolean mChildrenExpanded;
    private float mContentTransformationAmount;
    private boolean mDismissed;
    private boolean mEnableNonGroupedNotificationExpand;
    private NotificationEntry mEntry;
    private boolean mExpandAnimationRunning;
    private View.OnClickListener mExpandClickListener = new View.OnClickListener() {
        /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass1 */

        public void onClick(View view) {
            boolean z;
            if (!ExpandableNotificationRow.this.shouldShowPublic() && ((!ExpandableNotificationRow.this.mIsLowPriority || ExpandableNotificationRow.this.isExpanded()) && ExpandableNotificationRow.this.mGroupManager.isSummaryOfGroup(ExpandableNotificationRow.this.mStatusBarNotification))) {
                ExpandableNotificationRow.this.mGroupExpansionChanging = true;
                boolean isGroupExpanded = ExpandableNotificationRow.this.mGroupManager.isGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification);
                boolean z2 = ExpandableNotificationRow.this.mGroupManager.toggleGroupExpansion(ExpandableNotificationRow.this.mStatusBarNotification);
                ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z2);
                MetricsLogger.action(((FrameLayout) ExpandableNotificationRow.this).mContext, 408, z2);
                ExpandableNotificationRow.this.onExpansionChanged(true, isGroupExpanded);
            } else if (ExpandableNotificationRow.this.mEnableNonGroupedNotificationExpand) {
                if (view.isAccessibilityFocused()) {
                    ExpandableNotificationRow.this.mPrivateLayout.setFocusOnVisibilityChange();
                }
                if (ExpandableNotificationRow.this.isPinned()) {
                    z = !ExpandableNotificationRow.this.mExpandedWhenPinned;
                    ExpandableNotificationRow.this.mExpandedWhenPinned = z;
                } else {
                    z = !ExpandableNotificationRow.this.isExpanded();
                    ExpandableNotificationRow.this.setUserExpanded(z);
                }
                ExpandableNotificationRow.this.notifyHeightChanged(true);
                ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, z);
                MetricsLogger.action(((FrameLayout) ExpandableNotificationRow.this).mContext, 407, z);
            }
        }
    };
    private boolean mExpandable;
    private boolean mExpandedWhenPinned;
    private final Runnable mExpireRecentlyAlertedFlag = new Runnable() {
        /* class com.android.systemui.statusbar.notification.row.$$Lambda$ExpandableNotificationRow$i9dza2pC3is4TOlr6JzMO8_yGSI */

        public final void run() {
            ExpandableNotificationRow.this.lambda$new$1$ExpandableNotificationRow();
        }
    };
    private FalsingManager mFalsingManager;
    private boolean mForceUnlocked;
    private boolean mGroupExpansionChanging;
    private NotificationGroupManager mGroupManager;
    private View mGroupParentWhenDismissed;
    private NotificationGuts mGuts;
    private ViewStub mGutsStub;
    private boolean mHasUserChangedExpansion;
    private float mHeaderVisibleAmount = 1.0f;
    private Consumer<Boolean> mHeadsUpAnimatingAwayListener;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHeadsupDisappearRunning;
    private boolean mHideSensitiveForIntrinsicHeight;
    private boolean mIconAnimationRunning;
    private int mIconTransformContentShift;
    private int mIconTransformContentShiftNoIcon;
    private boolean mIconsVisible = true;
    private NotificationInlineImageResolver mImageResolver;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsAmbientPulsing;
    private boolean mIsBlockingHelperShowing;
    private boolean mIsColorized;
    private boolean mIsHeadsUp;
    private boolean mIsLastChild;
    private boolean mIsLowPriority;
    private boolean mIsPinned;
    private boolean mIsSummaryWithChildren;
    private boolean mIsSystemChildExpanded;
    private boolean mIsSystemExpanded;
    private boolean mJustClicked;
    private boolean mKeepInParent;
    private boolean mLastChronometerRunning = true;
    private LayoutListener mLayoutListener;
    private NotificationContentView[] mLayouts;
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private LongPressListener mLongPressListener;
    private int mMaxHeadsUpHeight;
    private int mMaxHeadsUpHeightBeforeN;
    private int mMaxHeadsUpHeightBeforeP;
    private int mMaxHeadsUpHeightIncreased;
    private NotificationMediaManager mMediaManager;
    private NotificationMenuRowPlugin mMenuRow;
    private boolean mMustStayOnScreen;
    private boolean mNeedsRedaction;
    private int mNotificationColor;
    private final NotificationContentInflater mNotificationInflater;
    private int mNotificationLaunchHeight;
    private int mNotificationMaxHeight;
    private int mNotificationMinHeight;
    private int mNotificationMinHeightBeforeN;
    private int mNotificationMinHeightBeforeP;
    private int mNotificationMinHeightLarge;
    private int mNotificationMinHeightMedia;
    private ExpandableNotificationRow mNotificationParent;
    private boolean mNotificationTranslationFinished = false;
    private boolean mOnAmbient;
    private View.OnClickListener mOnAppOpsClickListener;
    private View.OnClickListener mOnClickListener;
    private Runnable mOnDismissRunnable;
    private OnExpandClickListener mOnExpandClickListener;
    private boolean mOnKeyguard;
    private NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private boolean mRefocusOnDismiss;
    private boolean mRemoved;
    private BooleanSupplier mSecureStateProvider;
    private boolean mSensitive;
    private boolean mSensitiveHiddenInGeneral;
    private boolean mShowGroupBackgroundWhenExpanded;
    private boolean mShowNoBackground;
    private boolean mShowingPublic;
    private boolean mShowingPublicInitialized;
    private StatusBarNotification mStatusBarNotification;
    private int mStatusBarState = -1;
    private SystemNotificationAsyncTask mSystemNotificationAsyncTask = new SystemNotificationAsyncTask();
    private Animator mTranslateAnim;
    private ArrayList<View> mTranslateableViews;
    private float mTranslationWhenRemoved;
    private boolean mUpdateBackgroundOnUpdate;
    private boolean mUseIncreasedCollapsedHeight;
    private boolean mUseIncreasedHeadsUpHeight;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private boolean mWasChildInGroupWhenRemoved;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public interface LayoutListener {
        void onLayout();
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public interface OnAppOpsClickListener {
        boolean onClick(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public interface OnExpandClickListener {
        void onExpandClicked(NotificationEntry notificationEntry, boolean z);
    }

    /* access modifiers changed from: private */
    public static Boolean isSystemNotification(Context context, StatusBarNotification statusBarNotification) {
        PackageManager packageManagerForUser = StatusBar.getPackageManagerForUser(context, statusBarNotification.getUser().getIdentifier());
        try {
            return Boolean.valueOf(Utils.isSystemPackage(context.getResources(), packageManagerForUser, packageManagerForUser.getPackageInfo(statusBarNotification.getPackageName(), 64)) && !SomcConfig.isBlockableNotificationPackages(context.getResources(), statusBarNotification.getPackageName()));
        } catch (PackageManager.NameNotFoundException unused) {
            Log.e("ExpandableNotifRow", "cacheIsSystemNotification: Could not find package info");
            return null;
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isGroupExpansionChanging() {
        if (isChildInGroup()) {
            return this.mNotificationParent.isGroupExpansionChanging();
        }
        return this.mGroupExpansionChanging;
    }

    public void setGroupExpansionChanging(boolean z) {
        this.mGroupExpansionChanging = z;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void setActualHeightAnimating(boolean z) {
        NotificationContentView notificationContentView = this.mPrivateLayout;
        if (notificationContentView != null) {
            notificationContentView.setContentHeightAnimating(z);
        }
    }

    public NotificationContentView getPrivateLayout() {
        return this.mPrivateLayout;
    }

    public NotificationContentView getPublicLayout() {
        return this.mPublicLayout;
    }

    public void setIconAnimationRunning(boolean z) {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            setIconAnimationRunning(z, notificationContentView);
        }
        if (this.mIsSummaryWithChildren) {
            setIconAnimationRunningForChild(z, this.mChildrenContainer.getHeaderView());
            setIconAnimationRunningForChild(z, this.mChildrenContainer.getLowPriorityHeaderView());
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setIconAnimationRunning(z);
            }
        }
        this.mIconAnimationRunning = z;
    }

    private void setIconAnimationRunning(boolean z, NotificationContentView notificationContentView) {
        if (notificationContentView != null) {
            View contractedChild = notificationContentView.getContractedChild();
            View expandedChild = notificationContentView.getExpandedChild();
            View headsUpChild = notificationContentView.getHeadsUpChild();
            setIconAnimationRunningForChild(z, contractedChild);
            setIconAnimationRunningForChild(z, expandedChild);
            setIconAnimationRunningForChild(z, headsUpChild);
        }
    }

    private void setIconAnimationRunningForChild(boolean z, View view) {
        if (view != null) {
            setIconRunning((ImageView) view.findViewById(16908294), z);
            setIconRunning((ImageView) view.findViewById(16909300), z);
        }
    }

    private void setIconRunning(ImageView imageView, boolean z) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                if (z) {
                    animationDrawable.start();
                } else {
                    animationDrawable.stop();
                }
            } else if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) drawable;
                if (z) {
                    animatedVectorDrawable.start();
                } else {
                    animatedVectorDrawable.stop();
                }
            }
        }
    }

    public void setEntry(NotificationEntry notificationEntry) {
        this.mEntry = notificationEntry;
        this.mStatusBarNotification = notificationEntry.notification;
        cacheIsSystemNotification();
    }

    public void inflateViews() {
        this.mNotificationInflater.inflateNotificationViews();
    }

    public void freeContentViewWhenSafe(int i) {
        updateInflationFlag(i, false);
        $$Lambda$ExpandableNotificationRow$RmUEmS0GEHf9L7pp2cHmxPWsfmA r1 = new Runnable(i) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$ExpandableNotificationRow$RmUEmS0GEHf9L7pp2cHmxPWsfmA */
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                ExpandableNotificationRow.this.lambda$freeContentViewWhenSafe$0$ExpandableNotificationRow(this.f$1);
            }
        };
        if (i == 4) {
            getPrivateLayout().performWhenContentInactive(2, r1);
        } else if (i == 8) {
            getPrivateLayout().performWhenContentInactive(4, r1);
            getPublicLayout().performWhenContentInactive(4, r1);
        } else if (i == 16) {
            getPublicLayout().performWhenContentInactive(0, r1);
        }
    }

    public /* synthetic */ void lambda$freeContentViewWhenSafe$0$ExpandableNotificationRow(int i) {
        this.mNotificationInflater.freeNotificationView(i);
    }

    public void updateInflationFlag(int i, boolean z) {
        this.mNotificationInflater.updateInflationFlag(i, z);
    }

    public boolean isInflationFlagSet(int i) {
        return this.mNotificationInflater.isInflationFlagSet(i);
    }

    private void cacheIsSystemNotification() {
        NotificationEntry notificationEntry = this.mEntry;
        if (notificationEntry != null && notificationEntry.mIsSystemNotification == null && this.mSystemNotificationAsyncTask.getStatus() == AsyncTask.Status.PENDING) {
            this.mSystemNotificationAsyncTask.execute(new Void[0]);
        }
    }

    public boolean getIsNonblockable() {
        NotificationEntry notificationEntry;
        Boolean bool;
        NotificationChannel notificationChannel;
        boolean isNonblockable = ((NotificationBlockingHelperManager) Dependency.get(NotificationBlockingHelperManager.class)).isNonblockable(this.mStatusBarNotification.getPackageName(), this.mEntry.channel.getId());
        NotificationEntry notificationEntry2 = this.mEntry;
        if (notificationEntry2 != null && notificationEntry2.mIsSystemNotification == null) {
            this.mSystemNotificationAsyncTask.cancel(true);
            this.mEntry.mIsSystemNotification = isSystemNotification(((FrameLayout) this).mContext, this.mStatusBarNotification);
        }
        boolean isImportanceLockedByOEM = isNonblockable | this.mEntry.channel.isImportanceLockedByOEM() | this.mEntry.channel.isImportanceLockedByCriticalDeviceFunction();
        if (isImportanceLockedByOEM || (notificationEntry = this.mEntry) == null || (bool = notificationEntry.mIsSystemNotification) == null || !bool.booleanValue() || (notificationChannel = this.mEntry.channel) == null || notificationChannel.isBlockableSystem()) {
            return isImportanceLockedByOEM;
        }
        return true;
    }

    public void onNotificationUpdated() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.onNotificationUpdated(this.mEntry);
        }
        this.mIsColorized = this.mStatusBarNotification.getNotification().isColorized();
        this.mShowingPublicInitialized = false;
        updateNotificationColor();
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null) {
            notificationMenuRowPlugin.onNotificationUpdated(this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
            this.mChildrenContainer.onNotificationUpdated();
        }
        if (this.mIconAnimationRunning) {
            setIconAnimationRunning(true);
        }
        if (this.mLastChronometerRunning) {
            setChronometerRunning(true);
        }
        ExpandableNotificationRow expandableNotificationRow = this.mNotificationParent;
        if (expandableNotificationRow != null) {
            expandableNotificationRow.updateChildrenHeaderAppearance();
        }
        onChildrenCountChanged();
        this.mPublicLayout.updateExpandButtons(true);
        updateLimits();
        updateIconVisibilities();
        updateShelfIconColor();
        updateRippleAllowed();
        if (this.mUpdateBackgroundOnUpdate) {
            this.mUpdateBackgroundOnUpdate = false;
            updateBackgroundColors();
        }
    }

    public void onNotificationRankingUpdated() {
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null) {
            notificationMenuRowPlugin.onNotificationUpdated(this.mStatusBarNotification);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void updateShelfIconColor() {
        StatusBarIconView statusBarIconView = this.mEntry.expandedIcon;
        boolean z = true;
        int i = 0;
        if (!Boolean.TRUE.equals(statusBarIconView.getTag(C0007R$id.icon_is_pre_L)) || NotificationUtils.isGrayscale(statusBarIconView, ContrastColorUtil.getInstance(((FrameLayout) this).mContext))) {
            NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
            if (visibleNotificationHeader != null) {
                i = visibleNotificationHeader.getOriginalIconColor();
            } else {
                NotificationEntry notificationEntry = this.mEntry;
                Context context = ((FrameLayout) this).mContext;
                if (!this.mIsLowPriority || isExpanded()) {
                    z = false;
                }
                i = notificationEntry.getContrastedColor(context, z, getBackgroundColorWithoutTint());
            }
        }
        statusBarIconView.setStaticDrawableColor(i);
    }

    public void setAboveShelfChangedListener(AboveShelfChangedListener aboveShelfChangedListener) {
        this.mAboveShelfChangedListener = aboveShelfChangedListener;
    }

    public void setSecureStateProvider(BooleanSupplier booleanSupplier) {
        this.mSecureStateProvider = booleanSupplier;
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean isDimmable() {
        if (getShowingLayout().isDimmable() && !showingAmbientPulsing()) {
            return super.isDimmable();
        }
        return false;
    }

    private void updateLimits() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            updateLimitsForView(notificationContentView);
        }
    }

    private void updateLimitsForView(NotificationContentView notificationContentView) {
        int i;
        int i2;
        boolean z = true;
        boolean z2 = (notificationContentView.getContractedChild() == null || notificationContentView.getContractedChild().getId() == 16909412) ? false : true;
        boolean z3 = this.mEntry.targetSdk < 24;
        boolean z4 = this.mEntry.targetSdk < 28;
        View expandedChild = notificationContentView.getExpandedChild();
        boolean z5 = (expandedChild == null || expandedChild.findViewById(16909101) == null) ? false : true;
        boolean showCompactMediaSeekbar = this.mMediaManager.getShowCompactMediaSeekbar();
        if (!z2 || !z4 || this.mIsSummaryWithChildren) {
            i = (!z5 || !showCompactMediaSeekbar) ? (!this.mUseIncreasedCollapsedHeight || notificationContentView != this.mPrivateLayout) ? this.mNotificationMinHeight : this.mNotificationMinHeightLarge : this.mNotificationMinHeightMedia;
        } else {
            i = z3 ? this.mNotificationMinHeightBeforeN : this.mNotificationMinHeightBeforeP;
        }
        if (notificationContentView.getHeadsUpChild() == null || notificationContentView.getHeadsUpChild().getId() == 16909412) {
            z = false;
        }
        if (!z || !z4) {
            i2 = (!this.mUseIncreasedHeadsUpHeight || notificationContentView != this.mPrivateLayout) ? this.mMaxHeadsUpHeight : this.mMaxHeadsUpHeightIncreased;
        } else {
            i2 = z3 ? this.mMaxHeadsUpHeightBeforeN : this.mMaxHeadsUpHeightBeforeP;
        }
        NotificationViewWrapper visibleWrapper = notificationContentView.getVisibleWrapper(2);
        if (visibleWrapper != null) {
            i2 = Math.max(i2, visibleWrapper.getMinLayoutHeight());
        }
        notificationContentView.setHeights(i, i2, this.mNotificationMaxHeight, i2);
    }

    public StatusBarNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public NotificationEntry getEntry() {
        return this.mEntry;
    }

    public boolean isHeadsUp() {
        return this.mIsHeadsUp;
    }

    public void setHeadsUp(boolean z) {
        boolean isAboveShelf = isAboveShelf();
        int intrinsicHeight = getIntrinsicHeight();
        this.mIsHeadsUp = z;
        this.mPrivateLayout.setHeadsUp(z);
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateGroupOverflow();
        }
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (z) {
            this.mMustStayOnScreen = true;
            setAboveShelf(true);
        } else if (isAboveShelf() != isAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!isAboveShelf);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean showingAmbientPulsing() {
        return this.mIsAmbientPulsing || this.mAmbientGoingAway;
    }

    public void setAmbientPulsing(boolean z) {
        this.mIsAmbientPulsing = z;
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
        this.mPrivateLayout.setGroupManager(notificationGroupManager);
    }

    public void setRemoteInputController(RemoteInputController remoteInputController) {
        this.mPrivateLayout.setRemoteInputController(remoteInputController);
    }

    public void setAppName(String str) {
        this.mAppName = str;
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null && notificationMenuRowPlugin.getMenuView() != null) {
            this.mMenuRow.setAppName(this.mAppName);
        }
    }

    public void setHeaderVisibleAmount(float f) {
        if (this.mHeaderVisibleAmount != f) {
            this.mHeaderVisibleAmount = f;
            for (NotificationContentView notificationContentView : this.mLayouts) {
                notificationContentView.setHeaderVisibleAmount(f);
            }
            NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
            if (notificationChildrenContainer != null) {
                notificationChildrenContainer.setHeaderVisibleAmount(f);
            }
            notifyHeightChanged(false);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public float getHeaderVisibleAmount() {
        return this.mHeaderVisibleAmount;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void setHeadsUpIsVisible() {
        super.setHeadsUpIsVisible();
        this.mMustStayOnScreen = false;
    }

    public void addChildNotification(ExpandableNotificationRow expandableNotificationRow, int i) {
        if (this.mChildrenContainer == null) {
            this.mChildrenContainerStub.inflate();
        }
        this.mChildrenContainer.addNotification(expandableNotificationRow, i);
        onChildrenCountChanged();
        expandableNotificationRow.setIsChildInGroup(true, this);
    }

    public void removeChildNotification(ExpandableNotificationRow expandableNotificationRow) {
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.removeNotification(expandableNotificationRow);
        }
        onChildrenCountChanged();
        expandableNotificationRow.setIsChildInGroup(false, null);
        expandableNotificationRow.setBottomRoundness(0.0f, false);
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isChildInGroup() {
        return this.mNotificationParent != null;
    }

    public boolean isOnlyChildInGroup() {
        return this.mGroupManager.isOnlyChildInGroup(getStatusBarNotification());
    }

    public ExpandableNotificationRow getNotificationParent() {
        return this.mNotificationParent;
    }

    public void setIsChildInGroup(boolean z, ExpandableNotificationRow expandableNotificationRow) {
        ExpandableNotificationRow expandableNotificationRow2;
        boolean z2 = StatusBar.ENABLE_CHILD_NOTIFICATIONS && z;
        if (this.mExpandAnimationRunning && !z && (expandableNotificationRow2 = this.mNotificationParent) != null) {
            expandableNotificationRow2.setChildIsExpanding(false);
            this.mNotificationParent.setExtraWidthForClipping(0.0f);
            this.mNotificationParent.setMinimumHeightForClipping(0);
        }
        if (!z2) {
            expandableNotificationRow = null;
        }
        this.mNotificationParent = expandableNotificationRow;
        this.mPrivateLayout.setIsChildInGroup(z2);
        this.mNotificationInflater.setIsChildInGroup(z2);
        resetBackgroundAlpha();
        updateBackgroundForGroupState();
        updateClickAndFocus();
        if (this.mNotificationParent != null) {
            setOverrideTintColor(0, 0.0f);
            setDistanceToTopRoundness(-1.0f);
            this.mNotificationParent.updateBackgroundForGroupState();
        }
        updateIconVisibilities();
        updateBackgroundClipping();
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() != 0 || !isChildInGroup() || isGroupExpanded()) {
            return super.onTouchEvent(motionEvent);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean handleSlideBack() {
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin == null || !notificationMenuRowPlugin.isMenuVisible()) {
            return false;
        }
        animateTranslateNotification(0.0f);
        return true;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mShowNoBackground;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isSummaryWithChildren() {
        return this.mIsSummaryWithChildren;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean areChildrenExpanded() {
        return this.mChildrenExpanded;
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer == null) {
            return null;
        }
        return notificationChildrenContainer.getNotificationChildren();
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> list, VisualStabilityManager visualStabilityManager, VisualStabilityManager.Callback callback) {
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        return notificationChildrenContainer != null && notificationChildrenContainer.applyChildOrder(list, visualStabilityManager, callback);
    }

    public void updateChildrenStates(AmbientState ambientState) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateState(getViewState(), ambientState);
        }
    }

    public void applyChildrenState() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.applyState();
        }
    }

    public void prepareExpansionChanged() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.prepareExpansionChanged();
        }
    }

    public void startChildAnimation(AnimationProperties animationProperties) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.startAnimationToState(animationProperties);
        }
    }

    public ExpandableNotificationRow getViewAtPosition(float f) {
        ExpandableNotificationRow viewAtPosition;
        return (!this.mIsSummaryWithChildren || !this.mChildrenExpanded || (viewAtPosition = this.mChildrenContainer.getViewAtPosition(f)) == null) ? this : viewAtPosition;
    }

    public NotificationGuts getGuts() {
        return this.mGuts;
    }

    public void setPinned(boolean z) {
        int intrinsicHeight = getIntrinsicHeight();
        boolean isAboveShelf = isAboveShelf();
        this.mIsPinned = z;
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (z) {
            setIconAnimationRunning(true);
            this.mExpandedWhenPinned = false;
        } else if (this.mExpandedWhenPinned) {
            setUserExpanded(true);
        }
        setChronometerRunning(this.mLastChronometerRunning);
        if (isAboveShelf() != isAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!isAboveShelf);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean isPinned() {
        return this.mIsPinned;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getPinnedHeadsUpHeight() {
        return getPinnedHeadsUpHeight(true);
    }

    private int getPinnedHeadsUpHeight(boolean z) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (this.mExpandedWhenPinned) {
            return Math.max(getMaxExpandHeight(), getHeadsUpHeight());
        }
        if (z) {
            return Math.max(getCollapsedHeight(), getHeadsUpHeight());
        }
        return getHeadsUpHeight();
    }

    public void setJustClicked(boolean z) {
        this.mJustClicked = z;
    }

    public boolean wasJustClicked() {
        return this.mJustClicked;
    }

    public void setChronometerRunning(boolean z) {
        this.mLastChronometerRunning = z;
        setChronometerRunning(z, this.mPrivateLayout);
        setChronometerRunning(z, this.mPublicLayout);
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            List<ExpandableNotificationRow> notificationChildren = notificationChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setChronometerRunning(z);
            }
        }
    }

    private void setChronometerRunning(boolean z, NotificationContentView notificationContentView) {
        if (notificationContentView != null) {
            boolean z2 = z || isPinned();
            View contractedChild = notificationContentView.getContractedChild();
            View expandedChild = notificationContentView.getExpandedChild();
            View headsUpChild = notificationContentView.getHeadsUpChild();
            setChronometerRunningForChild(z2, contractedChild);
            setChronometerRunningForChild(z2, expandedChild);
            setChronometerRunningForChild(z2, headsUpChild);
        }
    }

    private void setChronometerRunningForChild(boolean z, View view) {
        if (view != null) {
            View findViewById = view.findViewById(16908808);
            if (findViewById instanceof Chronometer) {
                ((Chronometer) findViewById).setStarted(z);
            }
        }
    }

    public NotificationHeaderView getNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getNotificationHeader();
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return getShowingLayout().getVisibleNotificationHeader();
        }
        return this.mChildrenContainer.getVisibleHeader();
    }

    public NotificationHeaderView getContractedNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getContractedNotificationHeader();
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        this.mOnExpandClickListener = onExpandClickListener;
    }

    public void setLongPressListener(LongPressListener longPressListener) {
        this.mLongPressListener = longPressListener;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        super.setOnClickListener(onClickListener);
        this.mOnClickListener = onClickListener;
        updateClickAndFocus();
    }

    private void updateClickAndFocus() {
        boolean z = false;
        boolean z2 = !isChildInGroup() || isGroupExpanded();
        if (this.mOnClickListener != null && z2) {
            z = true;
        }
        if (isFocusable() != z2) {
            setFocusable(z2);
        }
        if (isClickable() != z) {
            setClickable(z);
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public HeadsUpManager getHeadsUpManager() {
        return this.mHeadsUpManager;
    }

    public void setGutsView(NotificationMenuRowPlugin.MenuItem menuItem) {
        if (this.mGuts != null && (menuItem.getGutsView() instanceof NotificationGuts.GutsContent)) {
            ((NotificationGuts.GutsContent) menuItem.getGutsView()).setGutsParent(this.mGuts);
            this.mGuts.setGutsContent((NotificationGuts.GutsContent) menuItem.getGutsView());
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mEntry.setInitializationTime(SystemClock.elapsedRealtime());
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) this, NotificationMenuRowPlugin.class, false);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
    }

    public void onPluginConnected(NotificationMenuRowPlugin notificationMenuRowPlugin, Context context) {
        NotificationMenuRowPlugin notificationMenuRowPlugin2 = this.mMenuRow;
        boolean z = (notificationMenuRowPlugin2 == null || notificationMenuRowPlugin2.getMenuView() == null) ? false : true;
        if (z) {
            removeView(this.mMenuRow.getMenuView());
        }
        if (notificationMenuRowPlugin != null) {
            this.mMenuRow = notificationMenuRowPlugin;
            if (this.mMenuRow.shouldUseDefaultMenuItems()) {
                ArrayList<NotificationMenuRowPlugin.MenuItem> arrayList = new ArrayList<>();
                arrayList.add(NotificationMenuRow.createInfoItem(((FrameLayout) this).mContext));
                arrayList.add(NotificationMenuRow.createSnoozeItem(((FrameLayout) this).mContext));
                arrayList.add(NotificationMenuRow.createAppOpsItem(((FrameLayout) this).mContext));
                this.mMenuRow.setMenuItems(arrayList);
            }
            if (z) {
                createMenu();
            }
        }
    }

    public void onPluginDisconnected(NotificationMenuRowPlugin notificationMenuRowPlugin) {
        boolean z = this.mMenuRow.getMenuView() != null;
        this.mMenuRow = new NotificationMenuRow(((FrameLayout) this).mContext);
        if (z) {
            createMenu();
        }
    }

    public NotificationMenuRowPlugin createMenu() {
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin == null) {
            return null;
        }
        if (notificationMenuRowPlugin.getMenuView() == null) {
            this.mMenuRow.createMenu(this, this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), 0, new FrameLayout.LayoutParams(-1, -1));
        }
        return this.mMenuRow;
    }

    public NotificationMenuRowPlugin getProvider() {
        return this.mMenuRow;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        initDimens();
        initBackground();
        reInflateViews();
    }

    private void reInflateViews() {
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.reInflateViews(this.mExpandClickListener, this.mEntry.notification);
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts != null) {
            int indexOfChild = indexOfChild(notificationGuts);
            removeView(notificationGuts);
            this.mGuts = (NotificationGuts) LayoutInflater.from(((FrameLayout) this).mContext).inflate(C0010R$layout.notification_guts, (ViewGroup) this, false);
            this.mGuts.setVisibility(notificationGuts.getVisibility());
            addView(this.mGuts, indexOfChild);
        }
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        View menuView = notificationMenuRowPlugin == null ? null : notificationMenuRowPlugin.getMenuView();
        if (menuView != null) {
            int indexOfChild2 = indexOfChild(menuView);
            removeView(menuView);
            this.mMenuRow.createMenu(this, this.mStatusBarNotification);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), indexOfChild2);
        }
        NotificationContentView[] notificationContentViewArr = this.mLayouts;
        for (NotificationContentView notificationContentView : notificationContentViewArr) {
            notificationContentView.initView();
            notificationContentView.reInflateViews();
        }
        this.mStatusBarNotification.clearPackageContext();
        this.mNotificationInflater.clearCachesAndReInflate();
    }

    public void onConfigurationChanged(Configuration configuration) {
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null && notificationMenuRowPlugin.getMenuView() != null) {
            this.mMenuRow.onConfigurationChanged();
        }
    }

    public void onUiModeChanged() {
        this.mUpdateBackgroundOnUpdate = true;
        reInflateViews();
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            for (ExpandableNotificationRow expandableNotificationRow : notificationChildrenContainer.getNotificationChildren()) {
                expandableNotificationRow.onUiModeChanged();
            }
        }
    }

    public void setContentBackground(int i, boolean z, NotificationContentView notificationContentView) {
        if (getShowingLayout() == notificationContentView) {
            setTintColor(i, z);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void setBackgroundTintColor(int i) {
        super.setBackgroundTintColor(i);
        NotificationContentView showingLayout = getShowingLayout();
        if (showingLayout != null) {
            showingLayout.setBackgroundTintColor(i);
        }
    }

    public void closeRemoteInput() {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.closeRemoteInput();
        }
    }

    public void setSingleLineWidthIndention(int i) {
        this.mPrivateLayout.setSingleLineWidthIndention(i);
    }

    public int getNotificationColor() {
        return this.mNotificationColor;
    }

    private void updateNotificationColor() {
        this.mNotificationColor = ContrastColorUtil.resolveContrastColor(((FrameLayout) this).mContext, getStatusBarNotification().getNotification().color, getBackgroundColorWithoutTint(), (getResources().getConfiguration().uiMode & 48) == 32);
    }

    public HybridNotificationView getSingleLineView() {
        return this.mPrivateLayout.getSingleLineView();
    }

    public boolean isOnKeyguard() {
        return this.mOnKeyguard;
    }

    public void removeAllChildren() {
        ArrayList arrayList = new ArrayList(this.mChildrenContainer.getNotificationChildren());
        for (int i = 0; i < arrayList.size(); i++) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) arrayList.get(i);
            if (!expandableNotificationRow.keepInParent()) {
                this.mChildrenContainer.removeNotification(expandableNotificationRow);
                expandableNotificationRow.setIsChildInGroup(false, null);
            }
        }
        onChildrenCountChanged();
    }

    public void setDismissed(boolean z) {
        List<ExpandableNotificationRow> notificationChildren;
        int indexOf;
        setLongPressListener(null);
        this.mDismissed = true;
        this.mGroupParentWhenDismissed = this.mNotificationParent;
        this.mRefocusOnDismiss = z;
        this.mChildAfterViewWhenDismissed = null;
        this.mEntry.icon.setDismissed();
        if (isChildInGroup() && (indexOf = (notificationChildren = this.mNotificationParent.getNotificationChildren()).indexOf(this)) != -1 && indexOf < notificationChildren.size() - 1) {
            this.mChildAfterViewWhenDismissed = notificationChildren.get(indexOf + 1);
        }
    }

    public boolean isDismissed() {
        return this.mDismissed;
    }

    public boolean keepInParent() {
        return this.mKeepInParent;
    }

    public void setKeepInParent(boolean z) {
        this.mKeepInParent = z;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isRemoved() {
        return this.mRemoved;
    }

    public void setRemoved() {
        this.mRemoved = true;
        this.mTranslationWhenRemoved = getTranslationY();
        this.mWasChildInGroupWhenRemoved = isChildInGroup();
        if (isChildInGroup()) {
            this.mTranslationWhenRemoved += getNotificationParent().getTranslationY();
        }
        this.mPrivateLayout.setRemoved();
    }

    public boolean wasChildInGroupWhenRemoved() {
        return this.mWasChildInGroupWhenRemoved;
    }

    public float getTranslationWhenRemoved() {
        return this.mTranslationWhenRemoved;
    }

    public NotificationChildrenContainer getChildrenContainer() {
        return this.mChildrenContainer;
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        Consumer<Boolean> consumer;
        boolean isAboveShelf = isAboveShelf();
        boolean z2 = z != this.mHeadsupDisappearRunning;
        this.mHeadsupDisappearRunning = z;
        this.mPrivateLayout.setHeadsUpAnimatingAway(z);
        if (z2 && (consumer = this.mHeadsUpAnimatingAwayListener) != null) {
            consumer.accept(Boolean.valueOf(z));
        }
        if (isAboveShelf() != isAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!isAboveShelf);
        }
    }

    public void setHeadsUpAnimatingAwayListener(Consumer<Boolean> consumer) {
        this.mHeadsUpAnimatingAwayListener = consumer;
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean isHeadsUpAnimatingAway() {
        return this.mHeadsupDisappearRunning;
    }

    public View getChildAfterViewWhenDismissed() {
        return this.mChildAfterViewWhenDismissed;
    }

    public View getGroupParentWhenDismissed() {
        return this.mGroupParentWhenDismissed;
    }

    public boolean performDismissWithBlockingHelper(boolean z) {
        boolean perhapsShowBlockingHelper = ((NotificationBlockingHelperManager) Dependency.get(NotificationBlockingHelperManager.class)).perhapsShowBlockingHelper(this, this.mMenuRow);
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).count("notification_dismissed", 1);
        performDismiss(z);
        return perhapsShowBlockingHelper;
    }

    public void performDismiss(boolean z) {
        Runnable runnable;
        if (isOnlyChildInGroup()) {
            NotificationEntry logicalGroupSummary = this.mGroupManager.getLogicalGroupSummary(getStatusBarNotification());
            if (logicalGroupSummary.isClearable()) {
                logicalGroupSummary.getRow().performDismiss(z);
            }
        }
        setDismissed(z);
        if (this.mEntry.isClearable() && (runnable = this.mOnDismissRunnable) != null) {
            runnable.run();
        }
    }

    public void setBlockingHelperShowing(boolean z) {
        this.mIsBlockingHelperShowing = z;
    }

    public boolean isBlockingHelperShowing() {
        return this.mIsBlockingHelperShowing;
    }

    public boolean isBlockingHelperShowingAndTranslationFinished() {
        return this.mIsBlockingHelperShowing && this.mNotificationTranslationFinished;
    }

    public void setOnDismissRunnable(Runnable runnable) {
        this.mOnDismissRunnable = runnable;
    }

    public View getNotificationIcon() {
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null) {
            return visibleNotificationHeader.getIcon();
        }
        return null;
    }

    public boolean isShowingIcon() {
        if (!areGutsExposed() && getVisibleNotificationHeader() != null) {
            return true;
        }
        return false;
    }

    public void setContentTransformationAmount(float f, boolean z) {
        boolean z2 = true;
        boolean z3 = z != this.mIsLastChild;
        if (this.mContentTransformationAmount == f) {
            z2 = false;
        }
        boolean z4 = z3 | z2;
        this.mIsLastChild = z;
        this.mContentTransformationAmount = f;
        if (z4) {
            updateContentTransformation();
        }
    }

    public void setIconsVisible(boolean z) {
        if (z != this.mIconsVisible) {
            this.mIconsVisible = z;
            updateIconVisibilities();
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void onBelowSpeedBumpChanged() {
        updateIconVisibilities();
    }

    private void updateContentTransformation() {
        if (!this.mExpandAnimationRunning) {
            float f = this.mContentTransformationAmount;
            float f2 = (-f) * ((float) this.mIconTransformContentShift);
            float f3 = 1.0f;
            if (this.mIsLastChild) {
                f3 = Interpolators.ALPHA_OUT.getInterpolation(Math.min((1.0f - f) / 0.5f, 1.0f));
                f2 *= 0.4f;
            }
            NotificationContentView[] notificationContentViewArr = this.mLayouts;
            for (NotificationContentView notificationContentView : notificationContentViewArr) {
                notificationContentView.setAlpha(f3);
                notificationContentView.setTranslationY(f2);
            }
            NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
            if (notificationChildrenContainer != null) {
                notificationChildrenContainer.setAlpha(f3);
                this.mChildrenContainer.setTranslationY(f2);
            }
        }
    }

    private void updateIconVisibilities() {
        boolean z = isChildInGroup() || this.mIconsVisible;
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setIconsVisible(z);
        }
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.setIconsVisible(z);
        }
    }

    public int getRelativeTopPadding(View view) {
        int i = 0;
        while (view.getParent() instanceof ViewGroup) {
            i += view.getTop();
            view = (View) view.getParent();
            if (view instanceof ExpandableNotificationRow) {
                break;
            }
        }
        return i;
    }

    public float getContentTranslation() {
        return this.mPrivateLayout.getTranslationY();
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
        this.mPrivateLayout.setIsLowPriority(z);
        this.mNotificationInflater.setIsLowPriority(this.mIsLowPriority);
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.setIsLowPriority(z);
        }
    }

    public boolean isLowPriority() {
        return this.mIsLowPriority;
    }

    public void setUseIncreasedCollapsedHeight(boolean z) {
        this.mUseIncreasedCollapsedHeight = z;
        this.mNotificationInflater.setUsesIncreasedHeight(z);
    }

    public void setUseIncreasedHeadsUpHeight(boolean z) {
        this.mUseIncreasedHeadsUpHeight = z;
        this.mNotificationInflater.setUsesIncreasedHeadsUpHeight(z);
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mNotificationInflater.setRemoteViewClickHandler(onClickHandler);
    }

    public void setInflationCallback(NotificationContentInflater.InflationCallback inflationCallback) {
        this.mNotificationInflater.setInflationCallback(inflationCallback);
    }

    public void setNeedsRedaction(boolean z) {
        if (this.mNeedsRedaction != z) {
            this.mNeedsRedaction = z;
            updateInflationFlag(16, z);
            this.mNotificationInflater.updateNeedsRedaction(z);
            if (!z) {
                freeContentViewWhenSafe(16);
            }
        }
    }

    @VisibleForTesting
    public NotificationContentInflater getNotificationInflater() {
        return this.mNotificationInflater;
    }

    public ExpandableNotificationRow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mNotificationInflater = new NotificationContentInflater(this);
        this.mMenuRow = new NotificationMenuRow(((FrameLayout) this).mContext);
        this.mImageResolver = new NotificationInlineImageResolver(context, new NotificationInlineImageCache());
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        initDimens();
    }

    private void initDimens() {
        this.mNotificationMinHeightBeforeN = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_min_height_legacy);
        this.mNotificationMinHeightBeforeP = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_min_height_before_p);
        this.mNotificationMinHeight = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_min_height);
        this.mNotificationMinHeightLarge = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_min_height_increased);
        this.mNotificationMinHeightMedia = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_min_height_media);
        this.mNotificationMaxHeight = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_max_height);
        this.mMaxHeadsUpHeightBeforeN = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_max_heads_up_height_legacy);
        this.mMaxHeadsUpHeightBeforeP = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_max_heads_up_height_before_p);
        this.mMaxHeadsUpHeight = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_max_heads_up_height);
        this.mMaxHeadsUpHeightIncreased = NotificationUtils.getFontScaledHeight(((FrameLayout) this).mContext, C0005R$dimen.notification_max_heads_up_height_increased);
        Resources resources = getResources();
        this.mIncreasedPaddingBetweenElements = resources.getDimensionPixelSize(C0005R$dimen.notification_divider_height_increased);
        this.mIconTransformContentShiftNoIcon = resources.getDimensionPixelSize(C0005R$dimen.notification_icon_transform_content_shift);
        this.mEnableNonGroupedNotificationExpand = resources.getBoolean(C0003R$bool.config_enableNonGroupedNotificationExpand);
        this.mShowGroupBackgroundWhenExpanded = resources.getBoolean(C0003R$bool.config_showGroupNotificationBgWhenExpanded);
    }

    /* access modifiers changed from: package-private */
    public NotificationInlineImageResolver getImageResolver() {
        return this.mImageResolver;
    }

    public void reset() {
        this.mShowingPublicInitialized = false;
        onHeightReset();
        requestLayout();
    }

    public void showAppOpsIcons(ArraySet<Integer> arraySet) {
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() != null) {
            this.mChildrenContainer.getHeaderView().showAppOpsIcons(arraySet);
        }
        this.mPrivateLayout.showAppOpsIcons(arraySet);
        this.mPublicLayout.showAppOpsIcons(arraySet);
    }

    public void setLastAudiblyAlertedMs(long j) {
        if (NotificationUtils.useNewInterruptionModel(((FrameLayout) this).mContext)) {
            long currentTimeMillis = System.currentTimeMillis() - j;
            boolean z = currentTimeMillis < RECENTLY_ALERTED_THRESHOLD_MS;
            applyAudiblyAlertedRecently(z);
            removeCallbacks(this.mExpireRecentlyAlertedFlag);
            if (z) {
                postDelayed(this.mExpireRecentlyAlertedFlag, RECENTLY_ALERTED_THRESHOLD_MS - currentTimeMillis);
            }
        }
    }

    public /* synthetic */ void lambda$new$1$ExpandableNotificationRow() {
        applyAudiblyAlertedRecently(false);
    }

    private void applyAudiblyAlertedRecently(boolean z) {
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() != null) {
            this.mChildrenContainer.getHeaderView().setRecentlyAudiblyAlerted(z);
        }
        this.mPrivateLayout.setRecentlyAudiblyAlerted(z);
        this.mPublicLayout.setRecentlyAudiblyAlerted(z);
    }

    public View.OnClickListener getAppOpsOnClickListener() {
        return this.mOnAppOpsClickListener;
    }

    public void setAppOpsOnClickListener(OnAppOpsClickListener onAppOpsClickListener) {
        this.mOnAppOpsClickListener = new View.OnClickListener(onAppOpsClickListener) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$ExpandableNotificationRow$pZQ5iMMRBs0QlgeqJrDmlq2VhEQ */
            private final /* synthetic */ ExpandableNotificationRow.OnAppOpsClickListener f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                ExpandableNotificationRow.this.lambda$setAppOpsOnClickListener$2$ExpandableNotificationRow(this.f$1, view);
            }
        };
    }

    public /* synthetic */ void lambda$setAppOpsOnClickListener$2$ExpandableNotificationRow(OnAppOpsClickListener onAppOpsClickListener, View view) {
        NotificationMenuRowPlugin.MenuItem appOpsMenuItem;
        createMenu();
        NotificationMenuRowPlugin provider = getProvider();
        if (provider != null && (appOpsMenuItem = provider.getAppOpsMenuItem(((FrameLayout) this).mContext)) != null) {
            onAppOpsClickListener.onClick(this, view.getWidth() / 2, view.getHeight() / 2, appOpsMenuItem);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPublicLayout = (NotificationContentView) findViewById(C0007R$id.expandedPublic);
        this.mPrivateLayout = (NotificationContentView) findViewById(C0007R$id.expanded);
        this.mLayouts = new NotificationContentView[]{this.mPrivateLayout, this.mPublicLayout};
        NotificationContentView[] notificationContentViewArr = this.mLayouts;
        for (NotificationContentView notificationContentView : notificationContentViewArr) {
            notificationContentView.setExpandClickListener(this.mExpandClickListener);
            notificationContentView.setContainingNotification(this);
        }
        this.mGutsStub = (ViewStub) findViewById(C0007R$id.notification_guts_stub);
        this.mGutsStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass3 */

            public void onInflate(ViewStub viewStub, View view) {
                ExpandableNotificationRow.this.mGuts = (NotificationGuts) view;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
                ExpandableNotificationRow.this.mGutsStub = null;
            }
        });
        this.mChildrenContainerStub = (ViewStub) findViewById(C0007R$id.child_container_stub);
        this.mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass4 */

            public void onInflate(ViewStub viewStub, View view) {
                ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) view;
                ExpandableNotificationRow.this.mChildrenContainer.setIsLowPriority(ExpandableNotificationRow.this.mIsLowPriority);
                ExpandableNotificationRow.this.mChildrenContainer.setContainingNotification(ExpandableNotificationRow.this);
                ExpandableNotificationRow.this.mChildrenContainer.setStatusBarState(ExpandableNotificationRow.this.mStatusBarState);
                ExpandableNotificationRow.this.mChildrenContainer.onNotificationUpdated();
                ExpandableNotificationRow expandableNotificationRow = ExpandableNotificationRow.this;
                if (expandableNotificationRow.mShouldTranslateContents) {
                    expandableNotificationRow.mTranslateableViews.add(ExpandableNotificationRow.this.mChildrenContainer);
                }
            }
        });
        if (this.mShouldTranslateContents) {
            this.mTranslateableViews = new ArrayList<>();
            for (int i = 0; i < getChildCount(); i++) {
                this.mTranslateableViews.add(getChildAt(i));
            }
            this.mTranslateableViews.remove(this.mChildrenContainerStub);
            this.mTranslateableViews.remove(this.mGutsStub);
        }
    }

    private void doLongClickCallback() {
        doLongClickCallback(getWidth() / 2, getHeight() / 2);
    }

    public void doLongClickCallback(int i, int i2) {
        createMenu();
        NotificationMenuRowPlugin provider = getProvider();
        doLongClickCallback(i, i2, provider != null ? provider.getLongpressMenuItem(((FrameLayout) this).mContext) : null);
    }

    private void doLongClickCallback(int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem) {
        LongPressListener longPressListener = this.mLongPressListener;
        if (longPressListener != null && menuItem != null) {
            longPressListener.onLongPress(this, i, i2, menuItem);
        }
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (!KeyEvent.isConfirmKey(i)) {
            return super.onKeyDown(i, keyEvent);
        }
        keyEvent.startTracking();
        return true;
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (!KeyEvent.isConfirmKey(i)) {
            return super.onKeyUp(i, keyEvent);
        }
        if (keyEvent.isCanceled()) {
            return true;
        }
        performClick();
        return true;
    }

    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        if (!KeyEvent.isConfirmKey(i)) {
            return false;
        }
        doLongClickCallback();
        return true;
    }

    public void resetTranslation() {
        Animator animator = this.mTranslateAnim;
        if (animator != null) {
            animator.cancel();
        }
        if (!this.mShouldTranslateContents) {
            setTranslationX(0.0f);
        } else if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                this.mTranslateableViews.get(i).setTranslationX(0.0f);
            }
            invalidateOutline();
            getEntry().expandedIcon.setScrollX(0);
        }
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null) {
            notificationMenuRowPlugin.resetMenu();
        }
    }

    /* access modifiers changed from: package-private */
    public void onGutsOpened() {
        resetTranslation();
        updateContentAccessibilityImportanceForGuts(false);
    }

    /* access modifiers changed from: package-private */
    public void onGutsClosed() {
        updateContentAccessibilityImportanceForGuts(true);
    }

    private void updateContentAccessibilityImportanceForGuts(boolean z) {
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            updateChildAccessibilityImportance(notificationChildrenContainer, z);
        }
        NotificationContentView[] notificationContentViewArr = this.mLayouts;
        if (notificationContentViewArr != null) {
            for (NotificationContentView notificationContentView : notificationContentViewArr) {
                updateChildAccessibilityImportance(notificationContentView, z);
            }
        }
        if (z) {
            requestAccessibilityFocus();
        }
    }

    private void updateChildAccessibilityImportance(View view, boolean z) {
        view.setImportantForAccessibility(z ? 0 : 4);
    }

    public CharSequence getActiveRemoteInputText() {
        return this.mPrivateLayout.getActiveRemoteInputText();
    }

    public void animateTranslateNotification(float f) {
        Animator animator = this.mTranslateAnim;
        if (animator != null) {
            animator.cancel();
        }
        this.mTranslateAnim = getTranslateViewAnimator(f, null);
        Animator animator2 = this.mTranslateAnim;
        if (animator2 != null) {
            animator2.start();
        }
    }

    public void setTranslation(float f) {
        if (isBlockingHelperShowingAndTranslationFinished()) {
            this.mGuts.setTranslationX(f);
            return;
        }
        if (!this.mShouldTranslateContents) {
            setTranslationX(f);
        } else if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                if (this.mTranslateableViews.get(i) != null) {
                    this.mTranslateableViews.get(i).setTranslationX(f);
                }
            }
            invalidateOutline();
            getEntry().expandedIcon.setScrollX((int) (-f));
        }
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (!(notificationMenuRowPlugin == null || notificationMenuRowPlugin.getMenuView() == null)) {
            this.mMenuRow.onParentTranslationUpdate(f);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public float getTranslation() {
        if (!this.mShouldTranslateContents) {
            return getTranslationX();
        }
        if (isBlockingHelperShowingAndCanTranslate()) {
            return this.mGuts.getTranslationX();
        }
        ArrayList<View> arrayList = this.mTranslateableViews;
        if (arrayList == null || arrayList.size() <= 0) {
            return 0.0f;
        }
        return this.mTranslateableViews.get(0).getTranslationX();
    }

    private boolean isBlockingHelperShowingAndCanTranslate() {
        return areGutsExposed() && this.mIsBlockingHelperShowing && this.mNotificationTranslationFinished;
    }

    public Animator getTranslateViewAnimator(final float f, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        Animator animator = this.mTranslateAnim;
        if (animator != null) {
            animator.cancel();
        }
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, TRANSLATE_CONTENT, f);
        if (animatorUpdateListener != null) {
            ofFloat.addUpdateListener(animatorUpdateListener);
        }
        ofFloat.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass5 */
            boolean cancelled = false;

            public void onAnimationCancel(Animator animator) {
                this.cancelled = true;
            }

            public void onAnimationEnd(Animator animator) {
                if (ExpandableNotificationRow.this.mIsBlockingHelperShowing) {
                    ExpandableNotificationRow.this.mNotificationTranslationFinished = true;
                }
                if (!this.cancelled && f == 0.0f) {
                    if (ExpandableNotificationRow.this.mMenuRow != null) {
                        ExpandableNotificationRow.this.mMenuRow.resetMenu();
                    }
                    ExpandableNotificationRow.this.mTranslateAnim = null;
                }
            }
        });
        this.mTranslateAnim = ofFloat;
        return ofFloat;
    }

    /* access modifiers changed from: package-private */
    public void ensureGutsInflated() {
        if (this.mGuts == null) {
            this.mGutsStub.inflate();
        }
    }

    private void updateChildrenVisibility() {
        NotificationGuts notificationGuts;
        int i = 0;
        boolean z = this.mExpandAnimationRunning && (notificationGuts = this.mGuts) != null && notificationGuts.isExposed();
        this.mPrivateLayout.setVisibility((this.mShowingPublic || this.mIsSummaryWithChildren || z) ? 4 : 0);
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            if (this.mShowingPublic || !this.mIsSummaryWithChildren || z) {
                i = 4;
            }
            notificationChildrenContainer.setVisibility(i);
        }
        updateLimits();
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (!super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            return false;
        }
        AccessibilityEvent obtain = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(obtain);
        dispatchPopulateAccessibilityEvent(obtain);
        accessibilityEvent.appendRecord(obtain);
        return true;
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setDark(boolean z, boolean z2, long j) {
        if (((ActivatableNotificationView) this).mDark != z) {
            super.setDark(z, z2, j);
            if (!this.mIsAmbientPulsing) {
                z2 = false;
            }
            NotificationContentView showingLayout = getShowingLayout();
            if (showingLayout != null) {
                showingLayout.setDark(z, z2, j);
            }
            updateShelfIconColor();
        }
    }

    public void applyExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
        if (expandAnimationParameters != null) {
            float lerp = MathUtils.lerp(expandAnimationParameters.getStartTranslationZ(), (float) this.mNotificationLaunchHeight, Interpolators.FAST_OUT_SLOW_IN.getInterpolation(expandAnimationParameters.getProgress(0, 50)));
            setTranslationZ(lerp);
            float width = ((float) (expandAnimationParameters.getWidth() - getWidth())) + MathUtils.lerp(0.0f, this.mOutlineRadius * 2.0f, expandAnimationParameters.getProgress());
            setExtraWidthForClipping(width);
            int top = expandAnimationParameters.getTop();
            float interpolation = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(expandAnimationParameters.getProgress());
            int startClipTopAmount = expandAnimationParameters.getStartClipTopAmount();
            ExpandableNotificationRow expandableNotificationRow = this.mNotificationParent;
            if (expandableNotificationRow != null) {
                float translationY = expandableNotificationRow.getTranslationY();
                top = (int) (((float) top) - translationY);
                this.mNotificationParent.setTranslationZ(lerp);
                int parentStartClipTopAmount = expandAnimationParameters.getParentStartClipTopAmount();
                if (startClipTopAmount != 0) {
                    this.mNotificationParent.setClipTopAmount((int) MathUtils.lerp((float) parentStartClipTopAmount, (float) (parentStartClipTopAmount - startClipTopAmount), interpolation));
                }
                this.mNotificationParent.setExtraWidthForClipping(width);
                this.mNotificationParent.setMinimumHeightForClipping((int) (Math.max((float) expandAnimationParameters.getBottom(), (((float) this.mNotificationParent.getActualHeight()) + translationY) - ((float) this.mNotificationParent.getClipBottomAmount())) - Math.min((float) expandAnimationParameters.getTop(), translationY)));
            } else if (startClipTopAmount != 0) {
                setClipTopAmount((int) MathUtils.lerp((float) startClipTopAmount, 0.0f, interpolation));
            }
            setTranslationY((float) top);
            setActualHeight(expandAnimationParameters.getHeight());
            this.mBackgroundNormal.setExpandAnimationParams(expandAnimationParameters);
        }
    }

    public void setExpandAnimationRunning(boolean z) {
        View view;
        if (this.mIsSummaryWithChildren) {
            view = this.mChildrenContainer;
        } else {
            view = getShowingLayout();
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts != null && notificationGuts.isExposed()) {
            view = this.mGuts;
        }
        if (z) {
            view.animate().alpha(0.0f).setDuration(67).setInterpolator(Interpolators.ALPHA_OUT);
            setAboveShelf(true);
            this.mExpandAnimationRunning = true;
            getViewState().cancelAnimations(this);
            this.mNotificationLaunchHeight = AmbientState.getNotificationLaunchHeight(getContext());
        } else {
            this.mExpandAnimationRunning = false;
            setAboveShelf(isAboveShelf());
            NotificationGuts notificationGuts2 = this.mGuts;
            if (notificationGuts2 != null) {
                notificationGuts2.setAlpha(1.0f);
            }
            if (view != null) {
                view.setAlpha(1.0f);
            }
            setExtraWidthForClipping(0.0f);
            ExpandableNotificationRow expandableNotificationRow = this.mNotificationParent;
            if (expandableNotificationRow != null) {
                expandableNotificationRow.setExtraWidthForClipping(0.0f);
                this.mNotificationParent.setMinimumHeightForClipping(0);
            }
        }
        ExpandableNotificationRow expandableNotificationRow2 = this.mNotificationParent;
        if (expandableNotificationRow2 != null) {
            expandableNotificationRow2.setChildIsExpanding(this.mExpandAnimationRunning);
        }
        updateChildrenVisibility();
        updateClipping();
        this.mBackgroundNormal.setExpandAnimationRunning(z);
    }

    private void setChildIsExpanding(boolean z) {
        this.mChildIsExpanding = z;
        updateClipping();
        invalidate();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean hasExpandingChild() {
        return this.mChildIsExpanding;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean shouldClipToActualHeight() {
        return super.shouldClipToActualHeight() && !this.mExpandAnimationRunning;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isExpandAnimationRunning() {
        return this.mExpandAnimationRunning;
    }

    public boolean isSoundEffectsEnabled() {
        BooleanSupplier booleanSupplier;
        return !(((ActivatableNotificationView) this).mDark && (booleanSupplier = this.mSecureStateProvider) != null && !booleanSupplier.getAsBoolean()) && super.isSoundEffectsEnabled();
    }

    public boolean isExpandable() {
        if (this.mIsSummaryWithChildren && !shouldShowPublic()) {
            return !this.mChildrenExpanded;
        }
        if (!this.mEnableNonGroupedNotificationExpand || !this.mExpandable) {
            return false;
        }
        return true;
    }

    public void setExpandable(boolean z) {
        this.mExpandable = z;
        this.mPrivateLayout.updateExpandButtons(isExpandable());
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void setClipToActualHeight(boolean z) {
        boolean z2 = false;
        super.setClipToActualHeight(z || isUserLocked());
        NotificationContentView showingLayout = getShowingLayout();
        if (z || isUserLocked()) {
            z2 = true;
        }
        showingLayout.setClipToActualHeight(z2);
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean z) {
        setUserExpanded(z, false);
    }

    public void setUserExpanded(boolean z, boolean z2) {
        this.mFalsingManager.setNotificationExpanded();
        if (this.mIsSummaryWithChildren && !shouldShowPublic() && z2 && !this.mChildrenContainer.showingAsLowPriority()) {
            boolean isGroupExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, z);
            onExpansionChanged(true, isGroupExpanded);
        } else if (!z || this.mExpandable) {
            boolean isExpanded = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = z;
            onExpansionChanged(true, isExpanded);
            if (!isExpanded && isExpanded() && getActualHeight() != getIntrinsicHeight()) {
                notifyHeightChanged(true);
            }
        }
    }

    public void resetUserExpansion() {
        boolean isExpanded = isExpanded();
        this.mHasUserChangedExpansion = false;
        this.mUserExpanded = false;
        if (isExpanded != isExpanded()) {
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.onExpansionChanged();
            }
            notifyHeightChanged(false);
        }
        updateShelfIconColor();
    }

    public boolean isUserLocked() {
        return this.mUserLocked && !this.mForceUnlocked;
    }

    public void setUserLocked(boolean z) {
        this.mUserLocked = z;
        this.mPrivateLayout.setUserExpanding(z);
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.setUserLocked(z);
            if (!this.mIsSummaryWithChildren) {
                return;
            }
            if (z || !isGroupExpanded()) {
                updateBackgroundForGroupState();
            }
        }
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean z) {
        if (z != this.mIsSystemExpanded) {
            boolean isExpanded = isExpanded();
            this.mIsSystemExpanded = z;
            notifyHeightChanged(false);
            onExpansionChanged(false, isExpanded);
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.updateGroupOverflow();
            }
        }
    }

    public void setOnKeyguard(boolean z) {
        if (z != this.mOnKeyguard) {
            boolean isAboveShelf = isAboveShelf();
            boolean isExpanded = isExpanded();
            this.mOnKeyguard = z;
            onExpansionChanged(false, isExpanded);
            if (isExpanded != isExpanded()) {
                if (this.mIsSummaryWithChildren) {
                    this.mChildrenContainer.updateGroupOverflow();
                }
                notifyHeightChanged(false);
            }
            if (isAboveShelf() != isAboveShelf) {
                this.mAboveShelfChangedListener.onAboveShelfStateChanged(!isAboveShelf);
            }
        }
        updateRippleAllowed();
    }

    private void updateRippleAllowed() {
        setRippleAllowed(isOnKeyguard() || this.mEntry.notification.getNotification().contentIntent == null);
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getIntrinsicHeight() {
        if (isShownAsBubble()) {
            return getMaxExpandHeight();
        }
        if (isUserLocked()) {
            return getActualHeight();
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts != null && notificationGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (isChildInGroup() && !isGroupExpanded()) {
            return this.mPrivateLayout.getMinHeight();
        }
        if (this.mSensitive && this.mHideSensitiveForIntrinsicHeight) {
            return getMinHeight();
        }
        if (this.mIsSummaryWithChildren && !this.mOnKeyguard) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (!isHeadsUpAllowed() || (!this.mIsHeadsUp && !this.mHeadsupDisappearRunning)) {
            if (isExpanded()) {
                return getMaxExpandHeight();
            }
            return getCollapsedHeight();
        } else if (isPinned() || this.mHeadsupDisappearRunning) {
            return getPinnedHeadsUpHeight(true);
        } else {
            if (isExpanded()) {
                return Math.max(getMaxExpandHeight(), getHeadsUpHeight());
            }
            return Math.max(getCollapsedHeight(), getHeadsUpHeight());
        }
    }

    private boolean isHeadsUpAllowed() {
        return !this.mOnKeyguard && !this.mOnAmbient;
    }

    private boolean isShownAsBubble() {
        return this.mEntry.isBubble() && !this.mEntry.showInShadeWhenBubble() && !this.mEntry.isBubbleDismissed();
    }

    public void setStatusBarState(int i) {
        if (this.mStatusBarState != i) {
            this.mStatusBarState = i;
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    private void onChildrenCountChanged() {
        NotificationChildrenContainer notificationChildrenContainer;
        this.mIsSummaryWithChildren = StatusBar.ENABLE_CHILD_NOTIFICATIONS && (notificationChildrenContainer = this.mChildrenContainer) != null && notificationChildrenContainer.getNotificationChildCount() > 0;
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() == null) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
        }
        getShowingLayout().updateBackgroundColor(false);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
        applyChildrenRoundness();
    }

    public int getNumUniqueChannels() {
        return getUniqueChannels().size();
    }

    public ArraySet<NotificationChannel> getUniqueChannels() {
        ArraySet<NotificationChannel> arraySet = new ArraySet<>();
        arraySet.add(this.mEntry.channel);
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = getNotificationChildren();
            int size = notificationChildren.size();
            for (int i = 0; i < size; i++) {
                ExpandableNotificationRow expandableNotificationRow = notificationChildren.get(i);
                NotificationChannel notificationChannel = expandableNotificationRow.getEntry().channel;
                StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
                if (statusBarNotification.getUser().equals(this.mStatusBarNotification.getUser()) && statusBarNotification.getPackageName().equals(this.mStatusBarNotification.getPackageName())) {
                    arraySet.add(notificationChannel);
                }
            }
        }
        return arraySet;
    }

    public void updateChildrenHeaderAppearance() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateChildrenHeaderAppearance();
        }
    }

    public boolean isExpanded() {
        return isExpanded(false);
    }

    public boolean isExpanded(boolean z) {
        return (!this.mOnKeyguard || z) && ((!hasUserChangedExpansion() && (isSystemExpanded() || isSystemChildExpanded())) || isUserExpanded());
    }

    private boolean isSystemChildExpanded() {
        return this.mIsSystemChildExpanded;
    }

    public void setSystemChildExpanded(boolean z) {
        this.mIsSystemChildExpanded = z;
    }

    public void setLayoutListener(LayoutListener layoutListener) {
        this.mLayoutListener = layoutListener;
    }

    public void removeListener() {
        this.mLayoutListener = null;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int intrinsicHeight = getIntrinsicHeight();
        super.onLayout(z, i, i2, i3, i4);
        if (!(intrinsicHeight == getIntrinsicHeight() || intrinsicHeight == 0)) {
            notifyHeightChanged(true);
        }
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (!(notificationMenuRowPlugin == null || notificationMenuRowPlugin.getMenuView() == null)) {
            this.mMenuRow.onParentHeightUpdate();
        }
        updateContentShiftHeight();
        LayoutListener layoutListener = this.mLayoutListener;
        if (layoutListener != null) {
            layoutListener.onLayout();
        }
    }

    private void updateContentShiftHeight() {
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null) {
            CachingIconView icon = visibleNotificationHeader.getIcon();
            this.mIconTransformContentShift = getRelativeTopPadding(icon) + icon.getHeight();
            return;
        }
        this.mIconTransformContentShift = this.mIconTransformContentShiftNoIcon;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void notifyHeightChanged(boolean z) {
        super.notifyHeightChanged(z);
        getShowingLayout().requestSelectLayout(z || isUserLocked());
    }

    public void setSensitive(boolean z, boolean z2) {
        this.mSensitive = z;
        this.mSensitiveHiddenInGeneral = z2;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void setHideSensitiveForIntrinsicHeight(boolean z) {
        this.mHideSensitiveForIntrinsicHeight = z;
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setHideSensitiveForIntrinsicHeight(z);
            }
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public void setHideSensitive(boolean z, boolean z2, long j, long j2) {
        if (getVisibility() != 8) {
            boolean z3 = this.mShowingPublic;
            this.mShowingPublic = this.mSensitive && z;
            if ((!this.mShowingPublicInitialized || this.mShowingPublic != z3) && this.mPublicLayout.getChildCount() != 0) {
                if (!z2) {
                    this.mPublicLayout.animate().cancel();
                    this.mPrivateLayout.animate().cancel();
                    NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
                    if (notificationChildrenContainer != null) {
                        notificationChildrenContainer.animate().cancel();
                        this.mChildrenContainer.setAlpha(1.0f);
                    }
                    this.mPublicLayout.setAlpha(1.0f);
                    this.mPrivateLayout.setAlpha(1.0f);
                    this.mPublicLayout.setVisibility(this.mShowingPublic ? 0 : 4);
                    updateChildrenVisibility();
                } else {
                    animateShowingPublic(j, j2, this.mShowingPublic);
                }
                NotificationContentView showingLayout = getShowingLayout();
                showingLayout.updateBackgroundColor(z2);
                this.mPrivateLayout.updateExpandButtons(isExpandable());
                updateShelfIconColor();
                showingLayout.setDark(isDark(), false, 0);
                this.mShowingPublicInitialized = true;
            }
        }
    }

    private void animateShowingPublic(long j, long j2, boolean z) {
        View[] viewArr = this.mIsSummaryWithChildren ? new View[]{this.mChildrenContainer} : new View[]{this.mPrivateLayout};
        View[] viewArr2 = {this.mPublicLayout};
        View[] viewArr3 = z ? viewArr : viewArr2;
        if (z) {
            viewArr = viewArr2;
        }
        for (final View view : viewArr3) {
            view.setVisibility(0);
            view.animate().cancel();
            view.animate().alpha(0.0f).setStartDelay(j).setDuration(j2).withEndAction(new Runnable() {
                /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass6 */

                public void run() {
                    view.setVisibility(4);
                }
            });
        }
        for (View view2 : viewArr) {
            view2.setVisibility(0);
            view2.setAlpha(0.0f);
            view2.animate().cancel();
            view2.animate().alpha(1.0f).setStartDelay(j).setDuration(j2);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean mustStayOnScreen() {
        return this.mIsHeadsUp && this.mMustStayOnScreen;
    }

    public boolean canViewBeDismissed() {
        return this.mEntry.isClearable() && (!shouldShowPublic() || !this.mSensitiveHiddenInGeneral);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean shouldShowPublic() {
        return this.mSensitive && this.mHideSensitiveForIntrinsicHeight;
    }

    public void makeActionsVisibile() {
        setUserExpanded(true, true);
        if (isChildInGroup()) {
            this.mGroupManager.setGroupExpanded(this.mStatusBarNotification, true);
        }
        notifyHeightChanged(false);
    }

    public void setChildrenExpanded(boolean z, boolean z2) {
        this.mChildrenExpanded = z;
        NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
        if (notificationChildrenContainer != null) {
            notificationChildrenContainer.setChildrenExpanded(z);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
    }

    public int getMaxExpandHeight() {
        return this.mPrivateLayout.getExpandHeight();
    }

    private int getHeadsUpHeight() {
        return getShowingLayout().getHeadsUpHeight();
    }

    public boolean areGutsExposed() {
        NotificationGuts notificationGuts = this.mGuts;
        return notificationGuts != null && notificationGuts.isExposed();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isContentExpandable() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return getShowingLayout().isContentExpandable();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public View getContentView() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return getShowingLayout();
        }
        return this.mChildrenContainer;
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public long performRemoveAnimation(final long j, final long j2, final float f, final boolean z, final float f2, final Runnable runnable, final AnimatorListenerAdapter animatorListenerAdapter) {
        Animator translateViewAnimator;
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin == null || !notificationMenuRowPlugin.isMenuVisible() || (translateViewAnimator = getTranslateViewAnimator(0.0f, null)) == null) {
            return super.performRemoveAnimation(j, j2, f, z, f2, runnable, animatorListenerAdapter);
        }
        translateViewAnimator.addListener(new AnimatorListenerAdapter() {
            /* class com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.AnonymousClass7 */

            public void onAnimationEnd(Animator animator) {
                ExpandableNotificationRow.super.performRemoveAnimation(j, j2, f, z, f2, runnable, animatorListenerAdapter);
            }
        });
        translateViewAnimator.start();
        return translateViewAnimator.getDuration();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void onAppearAnimationFinished(boolean z) {
        super.onAppearAnimationFinished(z);
        if (z) {
            NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
            if (notificationChildrenContainer != null) {
                notificationChildrenContainer.setAlpha(1.0f);
                this.mChildrenContainer.setLayerType(0, null);
            }
            NotificationContentView[] notificationContentViewArr = this.mLayouts;
            for (NotificationContentView notificationContentView : notificationContentViewArr) {
                notificationContentView.setAlpha(1.0f);
                notificationContentView.setLayerType(0, null);
            }
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getExtraBottomPadding() {
        if (!this.mIsSummaryWithChildren || !isGroupExpanded()) {
            return 0;
        }
        return this.mIncreasedPaddingBetweenElements;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setActualHeight(int i, boolean z) {
        ViewGroup viewGroup;
        boolean z2 = i != getActualHeight();
        super.setActualHeight(i, z);
        if (z2 && isRemoved() && (viewGroup = (ViewGroup) getParent()) != null) {
            viewGroup.invalidate();
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts == null || !notificationGuts.isExposed()) {
            int max = Math.max(getMinHeight(), i);
            for (NotificationContentView notificationContentView : this.mLayouts) {
                notificationContentView.setContentHeight(max);
            }
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.setActualHeight(i);
            }
            NotificationGuts notificationGuts2 = this.mGuts;
            if (notificationGuts2 != null) {
                notificationGuts2.setActualHeight(i);
            }
            NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
            if (!(notificationMenuRowPlugin == null || notificationMenuRowPlugin.getMenuView() == null)) {
                this.mMenuRow.onParentHeightUpdate();
                return;
            }
            return;
        }
        this.mGuts.setActualHeight(i);
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getMaxContentHeight() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return getShowingLayout().getMaxHeight();
        }
        return this.mChildrenContainer.getMaxContentHeight();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getMinHeight(boolean z) {
        NotificationGuts notificationGuts;
        if (!z && (notificationGuts = this.mGuts) != null && notificationGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (!z && isHeadsUpAllowed() && this.mIsHeadsUp && this.mHeadsUpManager.isTrackingHeadsUp()) {
            return getPinnedHeadsUpHeight(false);
        }
        if (this.mIsSummaryWithChildren && !isGroupExpanded() && !shouldShowPublic()) {
            return this.mChildrenContainer.getMinHeight();
        }
        if (z || !isHeadsUpAllowed() || !this.mIsHeadsUp) {
            return getShowingLayout().getMinHeight();
        }
        return getHeadsUpHeight();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public int getCollapsedHeight() {
        if (!this.mIsSummaryWithChildren || shouldShowPublic()) {
            return getMinHeight();
        }
        return this.mChildrenContainer.getCollapsedHeight();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setClipTopAmount(int i) {
        super.setClipTopAmount(i);
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setClipTopAmount(i);
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts != null) {
            notificationGuts.setClipTopAmount(i);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setClipBottomAmount(int i) {
        if (!this.mExpandAnimationRunning) {
            if (i != this.mClipBottomAmount) {
                super.setClipBottomAmount(i);
                for (NotificationContentView notificationContentView : this.mLayouts) {
                    notificationContentView.setClipBottomAmount(i);
                }
                NotificationGuts notificationGuts = this.mGuts;
                if (notificationGuts != null) {
                    notificationGuts.setClipBottomAmount(i);
                }
            }
            NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
            if (!(notificationChildrenContainer == null || this.mChildIsExpanding)) {
                notificationChildrenContainer.setClipBottomAmount(i);
            }
        }
    }

    public NotificationContentView getShowingLayout() {
        return shouldShowPublic() ? this.mPublicLayout : this.mPrivateLayout;
    }

    public View getExpandedContentView() {
        return getPrivateLayout().getExpandedChild();
    }

    public void setLegacy(boolean z) {
        for (NotificationContentView notificationContentView : this.mLayouts) {
            notificationContentView.setLegacy(z);
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void updateBackgroundTint() {
        super.updateBackgroundTint();
        updateBackgroundForGroupState();
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).updateBackgroundForGroupState();
            }
            this.mChildrenContainer.updateBackgroundTint();
        }
    }

    public void onFinishedExpansionChange() {
        this.mGroupExpansionChanging = false;
        updateBackgroundForGroupState();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v4, resolved type: boolean */
    /* JADX WARN: Multi-variable type inference failed */
    public void updateBackgroundForGroupState() {
        boolean z = true;
        int i = 0;
        if (this.mIsSummaryWithChildren) {
            if (this.mShowGroupBackgroundWhenExpanded || !isGroupExpanded() || isGroupExpansionChanging() || isUserLocked()) {
                z = false;
            }
            this.mShowNoBackground = z;
            this.mChildrenContainer.updateHeaderForExpansion(this.mShowNoBackground);
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            while (i < notificationChildren.size()) {
                notificationChildren.get(i).updateBackgroundForGroupState();
                i++;
            }
        } else if (isChildInGroup()) {
            int backgroundColorForExpansionState = getShowingLayout().getBackgroundColorForExpansionState();
            if (isGroupExpanded() || ((this.mNotificationParent.isGroupExpansionChanging() || this.mNotificationParent.isUserLocked()) && backgroundColorForExpansionState != 0)) {
                i = 1;
            }
            this.mShowNoBackground = i ^ 1;
        } else {
            this.mShowNoBackground = false;
        }
        updateOutline();
        updateBackground();
    }

    public int getPositionOfChild(ExpandableNotificationRow expandableNotificationRow) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getPositionInLinearLayout(expandableNotificationRow);
        }
        return 0;
    }

    public void setExpansionLogger(ExpansionLogger expansionLogger, String str) {
        this.mLogger = expansionLogger;
        this.mLoggingKey = str;
    }

    public void onExpandedByGesture(boolean z) {
        MetricsLogger.action(((FrameLayout) this).mContext, this.mGroupManager.isSummaryOfGroup(getStatusBarNotification()) ? 410 : 409, z);
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public float getIncreasedPaddingAmount() {
        if (this.mIsSummaryWithChildren) {
            if (isGroupExpanded()) {
                return 1.0f;
            }
            if (isUserLocked()) {
                return this.mChildrenContainer.getIncreasedPaddingAmount();
            }
            return 0.0f;
        } else if (isColorized()) {
            return (!this.mIsLowPriority || isExpanded()) ? -1.0f : 0.0f;
        } else {
            return 0.0f;
        }
    }

    private boolean isColorized() {
        return this.mIsColorized && this.mBgTint != 0;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean disallowSingleClick(MotionEvent motionEvent) {
        if (areGutsExposed()) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
        if (visibleNotificationHeader != null && visibleNotificationHeader.isInTouchRect(x - getTranslation(), y)) {
            return true;
        }
        if ((!this.mIsSummaryWithChildren || shouldShowPublic()) && getShowingLayout().disallowSingleClick(x, y)) {
            return true;
        }
        return super.disallowSingleClick(motionEvent);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void onExpansionChanged(boolean z, boolean z2) {
        boolean isExpanded = isExpanded();
        if (this.mIsSummaryWithChildren && (!this.mIsLowPriority || z2)) {
            isExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
        }
        if (isExpanded != z2) {
            updateShelfIconColor();
            ExpansionLogger expansionLogger = this.mLogger;
            if (expansionLogger != null) {
                expansionLogger.logNotificationExpansion(this.mLoggingKey, z, isExpanded);
            }
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.onExpansionChanged();
            }
        }
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK);
        if (canViewBeDismissed()) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
        }
        boolean shouldShowPublic = shouldShowPublic();
        boolean z = false;
        if (!shouldShowPublic) {
            if (this.mIsSummaryWithChildren) {
                shouldShowPublic = true;
                if (!this.mIsLowPriority || isExpanded()) {
                    z = isGroupExpanded();
                }
            } else {
                shouldShowPublic = this.mPrivateLayout.isContentExpandable();
                z = isExpanded();
            }
        }
        if (shouldShowPublic) {
            if (z) {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
            } else {
                accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            }
        }
        NotificationMenuRowPlugin provider = getProvider();
        if (provider != null && provider.getSnoozeMenuItem(getContext()) != null) {
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(C0007R$id.action_snooze, getContext().getResources().getString(C0014R$string.notification_menu_snooze_action)));
        }
    }

    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (super.performAccessibilityActionInternal(i, bundle)) {
            return true;
        }
        if (i == 32) {
            doLongClickCallback();
            return true;
        } else if (i == 262144 || i == 524288) {
            this.mExpandClickListener.onClick(this);
            return true;
        } else if (i == 1048576) {
            performDismissWithBlockingHelper(true);
            return true;
        } else if (i != C0007R$id.action_snooze || getProvider() != null || this.mMenuRow == null) {
            return false;
        } else {
            NotificationMenuRowPlugin.MenuItem snoozeMenuItem = createMenu().getSnoozeMenuItem(getContext());
            if (snoozeMenuItem != null) {
                doLongClickCallback(getWidth() / 2, getHeight() / 2, snoozeMenuItem);
            }
            return true;
        }
    }

    public boolean shouldRefocusOnDismiss() {
        return this.mRefocusOnDismiss || isAccessibilityFocused();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public ExpandableViewState createExpandableViewState() {
        return new NotificationViewState();
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean isAboveShelf() {
        return showingAmbientPulsing() || (!isOnKeyguard() && (this.mIsPinned || this.mHeadsupDisappearRunning || ((this.mIsHeadsUp && this.mAboveShelf) || this.mExpandAnimationRunning || this.mChildIsExpanding)));
    }

    public void setOnAmbient(boolean z) {
        if (z != this.mOnAmbient) {
            this.mOnAmbient = z;
            notifyHeightChanged(false);
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView
    public boolean topAmountNeedsClipping() {
        if (isGroupExpanded() || isGroupExpansionChanging() || getShowingLayout().shouldClipToRounding(true, false)) {
            return true;
        }
        NotificationGuts notificationGuts = this.mGuts;
        if (notificationGuts == null || notificationGuts.getAlpha() == 0.0f) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean childNeedsClipping(View view) {
        if (view instanceof NotificationContentView) {
            NotificationContentView notificationContentView = (NotificationContentView) view;
            if (isClippingNeeded()) {
                return true;
            }
            if (!hasNoRounding()) {
                boolean z = false;
                boolean z2 = getCurrentTopRoundness() != 0.0f;
                if (getCurrentBottomRoundness() != 0.0f) {
                    z = true;
                }
                if (notificationContentView.shouldClipToRounding(z2, z)) {
                    return true;
                }
            }
        } else if (view == this.mChildrenContainer) {
            if (isClippingNeeded() || !hasNoRounding()) {
                return true;
            }
        } else if (view instanceof NotificationGuts) {
            return !hasNoRounding();
        }
        return super.childNeedsClipping(view);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView, com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public void applyRoundness() {
        super.applyRoundness();
        applyChildrenRoundness();
    }

    private void applyChildrenRoundness() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setCurrentBottomRoundness(getCurrentBottomRoundness());
        }
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView
    public Path getCustomClipPath(View view) {
        if (view instanceof NotificationGuts) {
            return getClipPath(true);
        }
        return super.getCustomClipPath(view);
    }

    private boolean hasNoRounding() {
        return getCurrentBottomRoundness() == 0.0f && getCurrentTopRoundness() == 0.0f;
    }

    public boolean isOnAmbient() {
        return this.mOnAmbient;
    }

    public boolean isMediaRow() {
        return (getExpandedContentView() == null || getExpandedContentView().findViewById(16909101) == null) ? false : true;
    }

    public boolean isTopLevelChild() {
        return getParent() instanceof NotificationStackScrollLayout;
    }

    public boolean isGroupNotFullyVisible() {
        return getClipTopAmount() > 0 || getTranslationY() < 0.0f;
    }

    public void setAboveShelf(boolean z) {
        boolean isAboveShelf = isAboveShelf();
        this.mAboveShelf = z;
        if (isAboveShelf() != isAboveShelf) {
            this.mAboveShelfChangedListener.onAboveShelfStateChanged(!isAboveShelf);
        }
    }

    public void setDismissRtl(boolean z) {
        NotificationMenuRowPlugin notificationMenuRowPlugin = this.mMenuRow;
        if (notificationMenuRowPlugin != null) {
            notificationMenuRowPlugin.setDismissRtl(z);
        }
    }

    private static class NotificationViewState extends ExpandableViewState {
        private NotificationViewState() {
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState, com.android.systemui.statusbar.notification.stack.ExpandableViewState
        public void applyToView(View view) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (!expandableNotificationRow.isExpandAnimationRunning()) {
                    handleFixedTranslationZ(expandableNotificationRow);
                    super.applyToView(view);
                    expandableNotificationRow.applyChildrenState();
                }
            }
        }

        private void handleFixedTranslationZ(ExpandableNotificationRow expandableNotificationRow) {
            if (expandableNotificationRow.hasExpandingChild()) {
                this.zTranslation = expandableNotificationRow.getTranslationZ();
                this.clipTopAmount = expandableNotificationRow.getClipTopAmount();
            }
        }

        /* access modifiers changed from: protected */
        @Override // com.android.systemui.statusbar.notification.stack.ViewState
        public void onYTranslationAnimationFinished(View view) {
            super.onYTranslationAnimationFinished(view);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (expandableNotificationRow.isHeadsUpAnimatingAway()) {
                    expandableNotificationRow.setHeadsUpAnimatingAway(false);
                }
            }
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState, com.android.systemui.statusbar.notification.stack.ExpandableViewState
        public void animateTo(View view, AnimationProperties animationProperties) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (!expandableNotificationRow.isExpandAnimationRunning()) {
                    handleFixedTranslationZ(expandableNotificationRow);
                    super.animateTo(view, animationProperties);
                    expandableNotificationRow.startChildAnimation(animationProperties);
                }
            }
        }
    }

    public void setAmbientGoingAway(boolean z) {
        this.mAmbientGoingAway = z;
    }

    public InflatedSmartReplies.SmartRepliesAndActions getExistingSmartRepliesAndActions() {
        return this.mPrivateLayout.getCurrentSmartRepliesAndActions();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setChildrenContainer(NotificationChildrenContainer notificationChildrenContainer) {
        this.mChildrenContainer = notificationChildrenContainer;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setPrivateLayout(NotificationContentView notificationContentView) {
        this.mPrivateLayout = notificationContentView;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setPublicLayout(NotificationContentView notificationContentView) {
        this.mPublicLayout = notificationContentView;
    }

    @Override // com.android.systemui.Dumpable, com.android.systemui.statusbar.notification.row.ExpandableView
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("  Notification: " + getStatusBarNotification().getKey());
        printWriter.print("    visibility: " + getVisibility());
        printWriter.print(", alpha: " + getAlpha());
        printWriter.print(", translation: " + getTranslation());
        printWriter.print(", removed: " + isRemoved());
        printWriter.print(", expandAnimationRunning: " + this.mExpandAnimationRunning);
        NotificationContentView showingLayout = getShowingLayout();
        StringBuilder sb = new StringBuilder();
        sb.append(", privateShowing: ");
        sb.append(showingLayout == this.mPrivateLayout);
        printWriter.print(sb.toString());
        printWriter.println();
        showingLayout.dump(fileDescriptor, printWriter, strArr);
        printWriter.print("    ");
        if (getViewState() != null) {
            getViewState().dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.print("no viewState!!!");
        }
        printWriter.println();
        printWriter.println();
        if (this.mIsSummaryWithChildren) {
            printWriter.print("  ChildrenContainer");
            printWriter.print(" visibility: " + this.mChildrenContainer.getVisibility());
            printWriter.print(", alpha: " + this.mChildrenContainer.getAlpha());
            printWriter.print(", translationY: " + this.mChildrenContainer.getTranslationY());
            printWriter.println();
            List<ExpandableNotificationRow> notificationChildren = getNotificationChildren();
            printWriter.println("  Children: " + notificationChildren.size());
            printWriter.println("  {");
            for (ExpandableNotificationRow expandableNotificationRow : notificationChildren) {
                expandableNotificationRow.dump(fileDescriptor, printWriter, strArr);
            }
            printWriter.println("  }");
            printWriter.println();
        }
    }

    /* access modifiers changed from: private */
    public class SystemNotificationAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private SystemNotificationAsyncTask() {
        }

        /* access modifiers changed from: protected */
        public Boolean doInBackground(Void... voidArr) {
            return ExpandableNotificationRow.isSystemNotification(((FrameLayout) ExpandableNotificationRow.this).mContext, ExpandableNotificationRow.this.mStatusBarNotification);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Boolean bool) {
            if (ExpandableNotificationRow.this.mEntry != null) {
                ExpandableNotificationRow.this.mEntry.mIsSystemNotification = bool;
            }
        }
    }
}
