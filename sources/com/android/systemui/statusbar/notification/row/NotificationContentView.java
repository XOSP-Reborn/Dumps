package com.android.systemui.statusbar.notification.row;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0004R$color;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0010R$layout;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.MediaTransferManager;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SmartReplyController;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.notification.MultiWindowButtonManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationCustomViewWrapper;
import com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.InflatedSmartReplies;
import com.android.systemui.statusbar.policy.RemoteInputView;
import com.android.systemui.statusbar.policy.SmartReplyConstants;
import com.android.systemui.statusbar.policy.SmartReplyView;
import com.sonymobile.systemui.statusbar.MultiWindowButtonLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class NotificationContentView extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("NotificationContentView", 3);
    private View mAmbientChild;
    private NotificationViewWrapper mAmbientWrapper;
    private boolean mAnimate;
    private int mAnimationStartVisibleType = -1;
    private boolean mBeforeN;
    private RemoteInputView mCachedExpandedRemoteInput;
    private RemoteInputView mCachedHeadsUpRemoteInput;
    private int mClipBottomAmount;
    private final Rect mClipBounds = new Rect();
    private boolean mClipToActualHeight = true;
    private int mClipTopAmount;
    private ExpandableNotificationRow mContainingNotification;
    private int mContentHeight;
    private int mContentHeightAtAnimationStart = -1;
    private View mContractedChild;
    private NotificationViewWrapper mContractedWrapper;
    private InflatedSmartReplies.SmartRepliesAndActions mCurrentSmartRepliesAndActions;
    private boolean mDark;
    private final ViewTreeObserver.OnPreDrawListener mEnableAnimationPredrawListener = new ViewTreeObserver.OnPreDrawListener() {
        /* class com.android.systemui.statusbar.notification.row.NotificationContentView.AnonymousClass1 */

        public boolean onPreDraw() {
            NotificationContentView.this.post(new Runnable() {
                /* class com.android.systemui.statusbar.notification.row.NotificationContentView.AnonymousClass1.AnonymousClass1 */

                public void run() {
                    NotificationContentView.this.mAnimate = true;
                }
            });
            NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };
    private View.OnClickListener mExpandClickListener;
    private boolean mExpandable;
    private View mExpandedChild;
    private InflatedSmartReplies mExpandedInflatedSmartReplies;
    private RemoteInputView mExpandedRemoteInput;
    private SmartReplyView mExpandedSmartReplyView;
    private Runnable mExpandedVisibleListener;
    private NotificationViewWrapper mExpandedWrapper;
    private boolean mFocusOnVisibilityChange;
    private boolean mForceSelectNextLayout = true;
    private NotificationGroupManager mGroupManager;
    private boolean mHadMultiWindowButtonShown = false;
    private float mHeaderVisibleAmount = 1.0f;
    private boolean mHeadsUpAnimatingAway;
    private View mHeadsUpChild;
    private int mHeadsUpHeight;
    private InflatedSmartReplies mHeadsUpInflatedSmartReplies;
    private RemoteInputView mHeadsUpRemoteInput;
    private SmartReplyView mHeadsUpSmartReplyView;
    private NotificationViewWrapper mHeadsUpWrapper;
    private HybridGroupManager mHybridGroupManager = new HybridGroupManager(getContext(), this);
    private boolean mIconsVisible;
    private boolean mIsChildInGroup;
    private boolean mIsContentExpandable;
    private boolean mIsHeadsUp;
    private boolean mIsLowPriority;
    private boolean mLegacy;
    private MediaTransferManager mMediaTransferManager = new MediaTransferManager(getContext());
    private int mMinContractedHeight;
    private ViewGroup mMultiWindowButton;
    private MultiWindowButtonManager mMultiWindowButtonManager;
    private int mNotificationAmbientHeight;
    private int mNotificationContentMarginEnd;
    private int mNotificationMaxHeight;
    private final ArrayMap<View, Runnable> mOnContentViewInactiveListeners = new ArrayMap<>();
    private PendingIntent mPreviousExpandedRemoteInputIntent;
    private PendingIntent mPreviousHeadsUpRemoteInputIntent;
    private RemoteInputController mRemoteInputController;
    private boolean mRemoteInputVisible;
    private Set<View> mSetMarginViews = Collections.newSetFromMap(new WeakHashMap());
    private HybridNotificationView mSingleLineView;
    private int mSingleLineWidthIndention;
    private int mSmallHeight;
    private SmartReplyConstants mSmartReplyConstants = ((SmartReplyConstants) Dependency.get(SmartReplyConstants.class));
    private SmartReplyController mSmartReplyController = ((SmartReplyController) Dependency.get(SmartReplyController.class));
    private StatusBarNotification mStatusBarNotification;
    private int mStatusBarState;
    private int mTransformationStartVisibleType;
    private int mUnrestrictedContentHeight;
    private boolean mUserExpanding;
    private int mVisibleType = 0;

    public boolean hasOverlappingRendering() {
        return false;
    }

    public NotificationContentView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }

    public void initView() {
        this.mMinContractedHeight = getResources().getDimensionPixelSize(C0005R$dimen.min_notification_layout_height);
        this.mNotificationContentMarginEnd = getResources().getDimensionPixelSize(17105312);
        if (getResources().getBoolean(C0003R$bool.config_enable_multiwindow_button) && !MultiWindowButtonManager.isSpecialHome(((FrameLayout) this).mContext)) {
            this.mMultiWindowButtonManager = new MultiWindowButtonManager(((FrameLayout) this).mContext);
        }
    }

    public void setHeights(int i, int i2, int i3, int i4) {
        this.mSmallHeight = i;
        this.mHeadsUpHeight = i2;
        this.mNotificationMaxHeight = i3;
        this.mNotificationAmbientHeight = i4;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        boolean z;
        boolean z2;
        int i4;
        int i5;
        boolean z3;
        int mode = View.MeasureSpec.getMode(i2);
        boolean z4 = true;
        int i6 = 1073741824;
        boolean z5 = mode == 1073741824;
        boolean z6 = mode == Integer.MIN_VALUE;
        int i7 = 1073741823;
        int size = View.MeasureSpec.getSize(i);
        if (z5 || z6) {
            i7 = View.MeasureSpec.getSize(i2);
        }
        if (this.mExpandedChild != null) {
            int i8 = this.mNotificationMaxHeight;
            SmartReplyView smartReplyView = this.mExpandedSmartReplyView;
            if (smartReplyView != null) {
                i8 += smartReplyView.getHeightUpperLimit();
            }
            int extraMeasureHeight = i8 + this.mExpandedWrapper.getExtraMeasureHeight();
            int i9 = this.mExpandedChild.getLayoutParams().height;
            if (i9 >= 0) {
                extraMeasureHeight = Math.min(extraMeasureHeight, i9);
                z3 = true;
            } else {
                z3 = false;
            }
            measureChildWithMargins(this.mExpandedChild, i, 0, View.MeasureSpec.makeMeasureSpec(extraMeasureHeight, z3 ? 1073741824 : Integer.MIN_VALUE), 0);
            i3 = Math.max(0, this.mExpandedChild.getMeasuredHeight());
        } else {
            i3 = 0;
        }
        View view = this.mContractedChild;
        if (view != null) {
            int i10 = this.mSmallHeight;
            int i11 = view.getLayoutParams().height;
            if (i11 >= 0) {
                i10 = Math.min(i10, i11);
                z2 = true;
            } else {
                z2 = false;
            }
            if (shouldContractedBeFixedSize() || z2) {
                i4 = View.MeasureSpec.makeMeasureSpec(i10, 1073741824);
            } else {
                i4 = View.MeasureSpec.makeMeasureSpec(i10, Integer.MIN_VALUE);
            }
            measureChildWithMargins(this.mContractedChild, i, 0, i4, 0);
            int measuredHeight = this.mContractedChild.getMeasuredHeight();
            int i12 = this.mMinContractedHeight;
            if (measuredHeight < i12) {
                i5 = View.MeasureSpec.makeMeasureSpec(i12, 1073741824);
                measureChildWithMargins(this.mContractedChild, i, 0, i5, 0);
            } else {
                i5 = i4;
            }
            i3 = Math.max(i3, measuredHeight);
            if (updateContractedHeaderWidth()) {
                measureChildWithMargins(this.mContractedChild, i, 0, i5, 0);
            }
            if (this.mExpandedChild != null && this.mContractedChild.getMeasuredHeight() > this.mExpandedChild.getMeasuredHeight()) {
                measureChildWithMargins(this.mExpandedChild, i, 0, View.MeasureSpec.makeMeasureSpec(this.mContractedChild.getMeasuredHeight(), 1073741824), 0);
            }
        }
        if (this.mHeadsUpChild != null) {
            int i13 = this.mHeadsUpHeight;
            SmartReplyView smartReplyView2 = this.mHeadsUpSmartReplyView;
            if (smartReplyView2 != null) {
                i13 += smartReplyView2.getHeightUpperLimit();
            }
            int extraMeasureHeight2 = i13 + this.mHeadsUpWrapper.getExtraMeasureHeight();
            int i14 = this.mHeadsUpChild.getLayoutParams().height;
            if (i14 >= 0) {
                extraMeasureHeight2 = Math.min(extraMeasureHeight2, i14);
                z = true;
            } else {
                z = false;
            }
            measureChildWithMargins(this.mHeadsUpChild, i, 0, View.MeasureSpec.makeMeasureSpec(extraMeasureHeight2, z ? 1073741824 : Integer.MIN_VALUE), 0);
            i3 = Math.max(i3, this.mHeadsUpChild.getMeasuredHeight());
        }
        if (this.mSingleLineView != null) {
            this.mSingleLineView.measure((this.mSingleLineWidthIndention == 0 || View.MeasureSpec.getMode(i) == 0) ? i : View.MeasureSpec.makeMeasureSpec((size - this.mSingleLineWidthIndention) + this.mSingleLineView.getPaddingEnd(), 1073741824), View.MeasureSpec.makeMeasureSpec(this.mNotificationMaxHeight, Integer.MIN_VALUE));
            i3 = Math.max(i3, this.mSingleLineView.getMeasuredHeight());
        }
        View view2 = this.mAmbientChild;
        if (view2 != null) {
            int i15 = this.mNotificationAmbientHeight;
            int i16 = view2.getLayoutParams().height;
            if (i16 >= 0) {
                i15 = Math.min(i15, i16);
            } else {
                z4 = false;
            }
            View view3 = this.mAmbientChild;
            if (!z4) {
                i6 = Integer.MIN_VALUE;
            }
            view3.measure(i, View.MeasureSpec.makeMeasureSpec(i15, i6));
            i3 = Math.max(i3, this.mAmbientChild.getMeasuredHeight());
        }
        ViewGroup viewGroup = this.mMultiWindowButton;
        if (viewGroup != null) {
            viewGroup.measure(0, 0);
        }
        if (MultiWindowButtonManager.DEBUG_MW) {
            Log.d(MultiWindowButtonManager.DEBUG_MW_TAG, this.mStatusBarNotification + ": onMeasure  maxChildHeight=" + i3 + " maxSize=" + i7);
        }
        setMeasuredDimension(size, Math.min(i3, i7));
    }

    private int getExtraRemoteInputHeight(RemoteInputView remoteInputView) {
        if (remoteInputView == null) {
            return 0;
        }
        if (remoteInputView.isActive() || remoteInputView.isSending()) {
            return getResources().getDimensionPixelSize(17105311);
        }
        return 0;
    }

    private boolean updateContractedHeaderWidth() {
        int i;
        NotificationHeaderView notificationHeader = this.mContractedWrapper.getNotificationHeader();
        if (notificationHeader != null) {
            if (this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null) {
                int i2 = this.mNotificationContentMarginEnd;
                if (notificationHeader.getPaddingEnd() != i2) {
                    if (notificationHeader.isLayoutRtl()) {
                        i = i2;
                    } else {
                        i = notificationHeader.getPaddingLeft();
                    }
                    int paddingTop = notificationHeader.getPaddingTop();
                    if (notificationHeader.isLayoutRtl()) {
                        i2 = notificationHeader.getPaddingLeft();
                    }
                    notificationHeader.setPadding(i, paddingTop, i2, notificationHeader.getPaddingBottom());
                    notificationHeader.setShowWorkBadgeAtEnd(false);
                    return true;
                }
            } else {
                int headerTextMarginEnd = this.mExpandedWrapper.getNotificationHeader().getHeaderTextMarginEnd();
                if (headerTextMarginEnd != notificationHeader.getHeaderTextMarginEnd()) {
                    notificationHeader.setHeaderTextMarginEnd(headerTextMarginEnd);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldContractedBeFixedSize() {
        return this.mBeforeN && (this.mContractedWrapper instanceof NotificationCustomViewWrapper);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        View view = this.mExpandedChild;
        int height = view != null ? view.getHeight() : 0;
        super.onLayout(z, i, i2, i3, i4);
        if (!(height == 0 || this.mExpandedChild.getHeight() == height)) {
            this.mContentHeightAtAnimationStart = height;
        }
        updateClipping();
        invalidateOutline();
        selectLayout(false, this.mForceSelectNextLayout);
        this.mForceSelectNextLayout = false;
        updateExpandButtons(this.mExpandable);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public View getHeadsUpChild() {
        return this.mHeadsUpChild;
    }

    public View getAmbientChild() {
        return this.mAmbientChild;
    }

    public void setContractedChild(View view) {
        View view2 = this.mContractedChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mContractedChild);
        }
        if (view == null) {
            this.mContractedChild = null;
            this.mContractedWrapper = null;
            if (this.mTransformationStartVisibleType == 0) {
                this.mTransformationStartVisibleType = -1;
                return;
            }
            return;
        }
        addView(view);
        this.mContractedChild = view;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    private NotificationViewWrapper getWrapperForView(View view) {
        if (view == this.mContractedChild) {
            return this.mContractedWrapper;
        }
        if (view == this.mExpandedChild) {
            return this.mExpandedWrapper;
        }
        if (view == this.mHeadsUpChild) {
            return this.mHeadsUpWrapper;
        }
        if (view == this.mAmbientChild) {
            return this.mAmbientWrapper;
        }
        return null;
    }

    public void setExpandedChild(View view) {
        if (this.mExpandedChild != null) {
            this.mPreviousExpandedRemoteInputIntent = null;
            RemoteInputView remoteInputView = this.mExpandedRemoteInput;
            if (remoteInputView != null) {
                remoteInputView.onNotificationUpdateOrReset();
                if (this.mExpandedRemoteInput.isActive()) {
                    this.mPreviousExpandedRemoteInputIntent = this.mExpandedRemoteInput.getPendingIntent();
                    RemoteInputView remoteInputView2 = this.mExpandedRemoteInput;
                    this.mCachedExpandedRemoteInput = remoteInputView2;
                    remoteInputView2.dispatchStartTemporaryDetach();
                    ((ViewGroup) this.mExpandedRemoteInput.getParent()).removeView(this.mExpandedRemoteInput);
                }
            }
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
            this.mExpandedRemoteInput = null;
        }
        if (view == null) {
            this.mExpandedChild = null;
            this.mExpandedWrapper = null;
            if (this.mTransformationStartVisibleType == 1) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 1) {
                selectLayout(false, true);
                return;
            }
            return;
        }
        addView(view);
        this.mExpandedChild = view;
        this.mExpandedWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void setHeadsUpChild(View view) {
        if (this.mHeadsUpChild != null) {
            this.mPreviousHeadsUpRemoteInputIntent = null;
            RemoteInputView remoteInputView = this.mHeadsUpRemoteInput;
            if (remoteInputView != null) {
                remoteInputView.onNotificationUpdateOrReset();
                if (this.mHeadsUpRemoteInput.isActive()) {
                    this.mPreviousHeadsUpRemoteInputIntent = this.mHeadsUpRemoteInput.getPendingIntent();
                    RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
                    this.mCachedHeadsUpRemoteInput = remoteInputView2;
                    remoteInputView2.dispatchStartTemporaryDetach();
                    ((ViewGroup) this.mHeadsUpRemoteInput.getParent()).removeView(this.mHeadsUpRemoteInput);
                }
            }
            this.mHeadsUpChild.animate().cancel();
            removeView(this.mHeadsUpChild);
            this.mHeadsUpRemoteInput = null;
        }
        if (view == null) {
            this.mHeadsUpChild = null;
            this.mHeadsUpWrapper = null;
            if (this.mTransformationStartVisibleType == 2) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 2) {
                selectLayout(false, true);
                return;
            }
            return;
        }
        addView(view);
        this.mHeadsUpChild = view;
        this.mHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void setAmbientChild(View view) {
        View view2 = this.mAmbientChild;
        if (view2 != null) {
            view2.animate().cancel();
            removeView(this.mAmbientChild);
        }
        if (view == null) {
            this.mAmbientChild = null;
            this.mAmbientWrapper = null;
            if (this.mTransformationStartVisibleType == 4) {
                this.mTransformationStartVisibleType = -1;
            }
            if (this.mVisibleType == 4) {
                selectLayout(false, true);
                return;
            }
            return;
        }
        addView(view);
        this.mAmbientChild = view;
        this.mAmbientWrapper = NotificationViewWrapper.wrap(getContext(), view, this.mContainingNotification);
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        view.setTag(C0007R$id.row_tag_for_content_view, this.mContainingNotification);
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        updateVisibility();
        if (i != 0) {
            for (Runnable runnable : this.mOnContentViewInactiveListeners.values()) {
                runnable.run();
            }
            this.mOnContentViewInactiveListeners.clear();
        }
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
    }

    private void setVisible(boolean z) {
        if (z) {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
            return;
        }
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
        this.mAnimate = false;
    }

    private void focusExpandButtonIfNecessary() {
        ImageView expandButton;
        if (this.mFocusOnVisibilityChange) {
            NotificationHeaderView visibleNotificationHeader = getVisibleNotificationHeader();
            if (!(visibleNotificationHeader == null || (expandButton = visibleNotificationHeader.getExpandButton()) == null)) {
                expandButton.requestAccessibilityFocus();
            }
            this.mFocusOnVisibilityChange = false;
        }
    }

    public void setContentHeight(int i) {
        this.mUnrestrictedContentHeight = Math.max(i, getMinHeight());
        this.mContentHeight = Math.min(this.mUnrestrictedContentHeight, (this.mContainingNotification.getIntrinsicHeight() - getExtraRemoteInputHeight(this.mExpandedRemoteInput)) - getExtraRemoteInputHeight(this.mHeadsUpRemoteInput));
        selectLayout(this.mAnimate, false);
        if (this.mContractedChild != null) {
            int minContentHeightHint = getMinContentHeightHint();
            NotificationViewWrapper visibleWrapper = getVisibleWrapper(this.mVisibleType);
            if (visibleWrapper != null) {
                visibleWrapper.setContentHeight(this.mUnrestrictedContentHeight, minContentHeightHint);
            }
            NotificationViewWrapper visibleWrapper2 = getVisibleWrapper(this.mTransformationStartVisibleType);
            if (visibleWrapper2 != null) {
                visibleWrapper2.setContentHeight(this.mUnrestrictedContentHeight, minContentHeightHint);
            }
            updateClipping();
            invalidateOutline();
        }
    }

    private int getMinContentHeightHint() {
        int i;
        int i2;
        if (this.mIsChildInGroup && isVisibleOrTransitioning(3)) {
            return ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(17105302);
        }
        if (!(this.mHeadsUpChild == null || this.mExpandedChild == null)) {
            boolean z = isTransitioningFromTo(2, 1) || isTransitioningFromTo(1, 2);
            boolean z2 = !isVisibleOrTransitioning(0) && (this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && !this.mContainingNotification.isOnKeyguard();
            if (z || z2) {
                return Math.min(getViewHeight(2), getViewHeight(1));
            }
        }
        if (this.mVisibleType == 1 && (i2 = this.mContentHeightAtAnimationStart) >= 0 && this.mExpandedChild != null) {
            return Math.min(i2, getViewHeight(1));
        }
        if (this.mAmbientChild != null && isVisibleOrTransitioning(4)) {
            i = this.mAmbientChild.getHeight();
        } else if (this.mHeadsUpChild != null && isVisibleOrTransitioning(2)) {
            i = getViewHeight(2);
        } else if (this.mExpandedChild != null) {
            i = getViewHeight(1);
        } else {
            i = getViewHeight(0) + ((FrameLayout) this).mContext.getResources().getDimensionPixelSize(17105302);
        }
        return (this.mExpandedChild == null || !isVisibleOrTransitioning(1)) ? i : Math.min(i, getViewHeight(1));
    }

    private boolean isTransitioningFromTo(int i, int i2) {
        return (this.mTransformationStartVisibleType == i || this.mAnimationStartVisibleType == i) && this.mVisibleType == i2;
    }

    private boolean isVisibleOrTransitioning(int i) {
        return this.mVisibleType == i || this.mTransformationStartVisibleType == i || this.mAnimationStartVisibleType == i;
    }

    private void updateContentTransformation() {
        int calculateVisibleType = calculateVisibleType();
        int i = this.mVisibleType;
        if (calculateVisibleType != i) {
            this.mTransformationStartVisibleType = i;
            TransformableView transformableViewForVisibleType = getTransformableViewForVisibleType(calculateVisibleType);
            TransformableView transformableViewForVisibleType2 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            transformableViewForVisibleType.transformFrom(transformableViewForVisibleType2, 0.0f);
            getViewForVisibleType(calculateVisibleType).setVisibility(0);
            transformableViewForVisibleType2.transformTo(transformableViewForVisibleType, 0.0f);
            this.mVisibleType = calculateVisibleType;
            updateBackgroundColor(true);
        }
        if (this.mForceSelectNextLayout) {
            forceUpdateVisibilities();
        }
        int i2 = this.mTransformationStartVisibleType;
        if (i2 == -1 || this.mVisibleType == i2 || getViewForVisibleType(i2) == null) {
            updateViewVisibilities(calculateVisibleType);
            updateBackgroundColor(false);
            return;
        }
        TransformableView transformableViewForVisibleType3 = getTransformableViewForVisibleType(this.mVisibleType);
        TransformableView transformableViewForVisibleType4 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
        float calculateTransformationAmount = calculateTransformationAmount();
        transformableViewForVisibleType3.transformFrom(transformableViewForVisibleType4, calculateTransformationAmount);
        transformableViewForVisibleType4.transformTo(transformableViewForVisibleType3, calculateTransformationAmount);
        updateBackgroundTransformation(calculateTransformationAmount);
    }

    private void updateBackgroundTransformation(float f) {
        int backgroundColor = getBackgroundColor(this.mVisibleType);
        int backgroundColor2 = getBackgroundColor(this.mTransformationStartVisibleType);
        if (backgroundColor != backgroundColor2) {
            if (backgroundColor2 == 0) {
                backgroundColor2 = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            if (backgroundColor == 0) {
                backgroundColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            backgroundColor = NotificationUtils.interpolateColors(backgroundColor2, backgroundColor, f);
        }
        this.mContainingNotification.updateBackgroundAlpha(f);
        this.mContainingNotification.setContentBackground(backgroundColor, false, this);
    }

    private float calculateTransformationAmount() {
        int viewHeight = getViewHeight(this.mTransformationStartVisibleType);
        int viewHeight2 = getViewHeight(this.mVisibleType);
        int abs = Math.abs(this.mContentHeight - viewHeight);
        int abs2 = Math.abs(viewHeight2 - viewHeight);
        if (abs2 != 0) {
            return Math.min(1.0f, ((float) abs) / ((float) abs2));
        }
        Log.wtf("NotificationContentView", "the total transformation distance is 0\n StartType: " + this.mTransformationStartVisibleType + " height: " + viewHeight + "\n VisibleType: " + this.mVisibleType + " height: " + viewHeight2 + "\n mContentHeight: " + this.mContentHeight);
        return 1.0f;
    }

    public int getMaxHeight() {
        int viewHeight;
        int extraRemoteInputHeight;
        if (this.mExpandedChild != null) {
            viewHeight = getViewHeight(1);
            extraRemoteInputHeight = getExtraRemoteInputHeight(this.mExpandedRemoteInput);
        } else if (this.mContainingNotification.isOnAmbient() && getShowingAmbientView() != null) {
            return getShowingAmbientView().getHeight();
        } else {
            if (this.mIsHeadsUp && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard()) {
                viewHeight = getViewHeight(2);
                extraRemoteInputHeight = getExtraRemoteInputHeight(this.mHeadsUpRemoteInput);
            } else if (this.mContractedChild != null) {
                return getViewHeight(0);
            } else {
                return this.mNotificationMaxHeight;
            }
        }
        return viewHeight + extraRemoteInputHeight;
    }

    private int getViewHeight(int i) {
        View viewForVisibleType = getViewForVisibleType(i);
        int height = viewForVisibleType.getHeight();
        NotificationViewWrapper wrapperForView = getWrapperForView(viewForVisibleType);
        return wrapperForView != null ? height + wrapperForView.getHeaderTranslation() : height;
    }

    public int getMinHeight() {
        return getMinHeight(false);
    }

    public int getMinHeight(boolean z) {
        if (this.mContainingNotification.isOnAmbient() && getShowingAmbientView() != null) {
            return getShowingAmbientView().getHeight();
        }
        if (z || !this.mIsChildInGroup || isGroupExpanded()) {
            return this.mContractedChild != null ? getViewHeight(0) : this.mMinContractedHeight;
        }
        return this.mSingleLineView.getHeight();
    }

    public View getShowingAmbientView() {
        View view = this.mIsChildInGroup ? this.mSingleLineView : this.mAmbientChild;
        if (view != null) {
            return view;
        }
        return this.mContractedChild;
    }

    private boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    public void setClipTopAmount(int i) {
        this.mClipTopAmount = i;
        updateClipping();
    }

    public void setClipBottomAmount(int i) {
        this.mClipBottomAmount = i;
        updateClipping();
    }

    public void setTranslationY(float f) {
        super.setTranslationY(f);
        updateClipping();
    }

    private void updateClipping() {
        if (this.mClipToActualHeight) {
            int translationY = (int) (((float) this.mClipTopAmount) - getTranslationY());
            this.mClipBounds.set(0, translationY, getWidth(), Math.max(translationY, (int) (((float) (this.mUnrestrictedContentHeight - this.mClipBottomAmount)) - getTranslationY())));
            setClipBounds(this.mClipBounds);
            return;
        }
        setClipBounds(null);
    }

    public void setClipToActualHeight(boolean z) {
        this.mClipToActualHeight = z;
        updateClipping();
    }

    private void selectLayout(boolean z, boolean z2) {
        if (this.mContractedChild != null) {
            if (this.mUserExpanding) {
                updateContentTransformation();
                return;
            }
            int calculateVisibleType = calculateVisibleType();
            boolean z3 = calculateVisibleType != this.mVisibleType;
            if (z3 || z2) {
                View viewForVisibleType = getViewForVisibleType(calculateVisibleType);
                if (viewForVisibleType != null) {
                    viewForVisibleType.setVisibility(0);
                    transferRemoteInputFocus(calculateVisibleType);
                }
                if (!z || ((calculateVisibleType != 1 || this.mExpandedChild == null) && ((calculateVisibleType != 2 || this.mHeadsUpChild == null) && ((calculateVisibleType != 3 || this.mSingleLineView == null) && calculateVisibleType != 0)))) {
                    updateViewVisibilities(calculateVisibleType);
                } else {
                    animateToVisibleType(calculateVisibleType);
                }
                this.mVisibleType = calculateVisibleType;
                if (z3) {
                    focusExpandButtonIfNecessary();
                }
                NotificationViewWrapper visibleWrapper = getVisibleWrapper(calculateVisibleType);
                if (visibleWrapper != null) {
                    visibleWrapper.setContentHeight(this.mUnrestrictedContentHeight, getMinContentHeightHint());
                }
                updateBackgroundColor(z);
            }
        }
    }

    private void forceUpdateVisibilities() {
        forceUpdateVisibility(0, this.mContractedChild, this.mContractedWrapper);
        forceUpdateVisibility(1, this.mExpandedChild, this.mExpandedWrapper);
        forceUpdateVisibility(2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        HybridNotificationView hybridNotificationView = this.mSingleLineView;
        forceUpdateVisibility(3, hybridNotificationView, hybridNotificationView);
        forceUpdateVisibility(4, this.mAmbientChild, this.mAmbientWrapper);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
    }

    private void fireExpandedVisibleListenerIfVisible() {
        if (this.mExpandedVisibleListener != null && this.mExpandedChild != null && isShown() && this.mExpandedChild.getVisibility() == 0) {
            Runnable runnable = this.mExpandedVisibleListener;
            this.mExpandedVisibleListener = null;
            runnable.run();
        }
    }

    private void forceUpdateVisibility(int i, View view, TransformableView transformableView) {
        if (view != null) {
            if (!(this.mVisibleType == i || this.mTransformationStartVisibleType == i)) {
                view.setVisibility(4);
            } else {
                transformableView.setVisible(true);
            }
        }
    }

    public void updateBackgroundColor(boolean z) {
        int backgroundColor = getBackgroundColor(this.mVisibleType);
        this.mContainingNotification.resetBackgroundAlpha();
        this.mContainingNotification.setContentBackground(backgroundColor, z, this);
    }

    public void setBackgroundTintColor(int i) {
        SmartReplyView smartReplyView = this.mExpandedSmartReplyView;
        if (smartReplyView != null) {
            smartReplyView.setBackgroundTintColor(i);
        }
        SmartReplyView smartReplyView2 = this.mHeadsUpSmartReplyView;
        if (smartReplyView2 != null) {
            smartReplyView2.setBackgroundTintColor(i);
        }
    }

    public int getVisibleType() {
        return this.mVisibleType;
    }

    public int getBackgroundColorForExpansionState() {
        int i;
        if (this.mContainingNotification.isGroupExpanded() || this.mContainingNotification.isUserLocked()) {
            i = calculateVisibleType();
        } else {
            i = getVisibleType();
        }
        return getBackgroundColor(i);
    }

    public int getBackgroundColor(int i) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(i);
        if (visibleWrapper != null) {
            return visibleWrapper.getCustomBackgroundColor();
        }
        return 0;
    }

    private void updateViewVisibilities(int i) {
        updateViewVisibility(i, 0, this.mContractedChild, this.mContractedWrapper);
        updateViewVisibility(i, 1, this.mExpandedChild, this.mExpandedWrapper);
        updateViewVisibility(i, 2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        HybridNotificationView hybridNotificationView = this.mSingleLineView;
        updateViewVisibility(i, 3, hybridNotificationView, hybridNotificationView);
        updateViewVisibility(i, 4, this.mAmbientChild, this.mAmbientWrapper);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
        updateMultiWindowButton();
    }

    private void updateViewVisibility(int i, int i2, View view, TransformableView transformableView) {
        if (view != null) {
            transformableView.setVisible(i == i2);
        }
    }

    private void animateToVisibleType(int i) {
        TransformableView transformableViewForVisibleType = getTransformableViewForVisibleType(i);
        final TransformableView transformableViewForVisibleType2 = getTransformableViewForVisibleType(this.mVisibleType);
        if (transformableViewForVisibleType == transformableViewForVisibleType2 || transformableViewForVisibleType2 == null) {
            transformableViewForVisibleType.setVisible(true);
            return;
        }
        this.mAnimationStartVisibleType = this.mVisibleType;
        transformableViewForVisibleType.transformFrom(transformableViewForVisibleType2);
        getViewForVisibleType(i).setVisibility(0);
        transformableViewForVisibleType2.transformTo(transformableViewForVisibleType, new Runnable() {
            /* class com.android.systemui.statusbar.notification.row.NotificationContentView.AnonymousClass2 */

            public void run() {
                TransformableView transformableView = transformableViewForVisibleType2;
                NotificationContentView notificationContentView = NotificationContentView.this;
                if (transformableView != notificationContentView.getTransformableViewForVisibleType(notificationContentView.mVisibleType)) {
                    transformableViewForVisibleType2.setVisible(false);
                }
                NotificationContentView.this.mAnimationStartVisibleType = -1;
            }
        });
        fireExpandedVisibleListenerIfVisible();
    }

    private void transferRemoteInputFocus(int i) {
        RemoteInputView remoteInputView;
        RemoteInputView remoteInputView2;
        if (i == 2 && this.mHeadsUpRemoteInput != null && (remoteInputView2 = this.mExpandedRemoteInput) != null && remoteInputView2.isActive()) {
            this.mHeadsUpRemoteInput.stealFocusFrom(this.mExpandedRemoteInput);
        }
        if (i == 1 && this.mExpandedRemoteInput != null && (remoteInputView = this.mHeadsUpRemoteInput) != null && remoteInputView.isActive()) {
            this.mExpandedRemoteInput.stealFocusFrom(this.mHeadsUpRemoteInput);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private TransformableView getTransformableViewForVisibleType(int i) {
        if (i == 1) {
            return this.mExpandedWrapper;
        }
        if (i == 2) {
            return this.mHeadsUpWrapper;
        }
        if (i == 3) {
            return this.mSingleLineView;
        }
        if (i != 4) {
            return this.mContractedWrapper;
        }
        return this.mAmbientWrapper;
    }

    private View getViewForVisibleType(int i) {
        if (i == 1) {
            return this.mExpandedChild;
        }
        if (i == 2) {
            return this.mHeadsUpChild;
        }
        if (i == 3) {
            return this.mSingleLineView;
        }
        if (i != 4) {
            return this.mContractedChild;
        }
        return this.mAmbientChild;
    }

    public NotificationViewWrapper getVisibleWrapper(int i) {
        if (i == 0) {
            return this.mContractedWrapper;
        }
        if (i == 1) {
            return this.mExpandedWrapper;
        }
        if (i == 2) {
            return this.mHeadsUpWrapper;
        }
        if (i != 4) {
            return null;
        }
        return this.mAmbientWrapper;
    }

    public int calculateVisibleType() {
        int i;
        int i2;
        if (this.mUserExpanding) {
            if (!this.mIsChildInGroup || isGroupExpanded() || this.mContainingNotification.isExpanded(true)) {
                i = this.mContainingNotification.getMaxContentHeight();
            } else {
                i = this.mContainingNotification.getShowingLayout().getMinHeight();
            }
            if (i == 0) {
                i = this.mContentHeight;
            }
            int visualTypeForHeight = getVisualTypeForHeight((float) i);
            if (!this.mIsChildInGroup || isGroupExpanded()) {
                i2 = getVisualTypeForHeight((float) this.mContainingNotification.getCollapsedHeight());
            } else {
                i2 = 3;
            }
            return this.mTransformationStartVisibleType == i2 ? visualTypeForHeight : i2;
        }
        int intrinsicHeight = this.mContainingNotification.getIntrinsicHeight();
        int i3 = this.mContentHeight;
        if (intrinsicHeight != 0) {
            i3 = Math.min(i3, intrinsicHeight);
        }
        return getVisualTypeForHeight((float) i3);
    }

    private int getVisualTypeForHeight(float f) {
        int i = 0;
        boolean z = this.mExpandedChild == null;
        if (!z && f == ((float) getViewHeight(1))) {
            return 1;
        }
        boolean isOnAmbient = this.mContainingNotification.isOnAmbient();
        if (!this.mUserExpanding && this.mIsChildInGroup && !isGroupExpanded()) {
            return 3;
        }
        if ((!this.mIsHeadsUp && !this.mHeadsUpAnimatingAway) || this.mHeadsUpChild == null || this.mContainingNotification.isOnKeyguard()) {
            if (isOnAmbient && this.mAmbientChild != null) {
                i = 4;
            }
            if (z || (this.mContractedChild != null && f <= ((float) getViewHeight(i)) && (!this.mIsChildInGroup || isGroupExpanded() || !this.mContainingNotification.isExpanded(true)))) {
                return i;
            }
            return 1;
        } else if (f <= ((float) getViewHeight(2)) || z) {
            return 2;
        } else {
            return 1;
        }
    }

    public boolean isContentExpandable() {
        return this.mIsContentExpandable;
    }

    public void setDark(boolean z, boolean z2, long j) {
        if (this.mContractedChild != null) {
            this.mDark = z;
            selectLayout(!z && z2, false);
        }
    }

    public void setHeadsUp(boolean z) {
        this.mIsHeadsUp = z;
        selectLayout(false, true);
        updateExpandButtons(this.mExpandable);
    }

    public void setLegacy(boolean z) {
        this.mLegacy = z;
        updateLegacy();
    }

    private void updateLegacy() {
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setLegacy(this.mLegacy);
        }
    }

    public void setIsChildInGroup(boolean z) {
        this.mIsChildInGroup = z;
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        updateAllSingleLineViews();
    }

    public void onNotificationUpdated(NotificationEntry notificationEntry) {
        this.mStatusBarNotification = notificationEntry.notification;
        this.mOnContentViewInactiveListeners.clear();
        this.mBeforeN = notificationEntry.targetSdk < 24;
        updateAllSingleLineViews();
        ExpandableNotificationRow row = notificationEntry.getRow();
        if (this.mContractedChild != null) {
            this.mContractedWrapper.onContentUpdated(row);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.onContentUpdated(row);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.onContentUpdated(row);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.onContentUpdated(row);
        }
        applyRemoteInputAndSmartReply(notificationEntry);
        applyMediaTransfer(notificationEntry);
        if (this.mMultiWindowButtonManager != null) {
            applyMultiWindowButton(notificationEntry);
        }
        updateLegacy();
        this.mForceSelectNextLayout = true;
        setDark(this.mDark, false, 0);
        this.mPreviousExpandedRemoteInputIntent = null;
        this.mPreviousHeadsUpRemoteInputIntent = null;
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str, this.mStatusBarNotification + "onNotificationUpdated  entry=" + notificationEntry + " mContractedChild=" + this.mContractedChild + " mExpandedChild=" + this.mExpandedChild + " mHeadsUpChild=" + this.mHeadsUpChild + " mAmbientChild=" + this.mAmbientChild + " mVisibleType=" + this.mVisibleType + " getTranslationY=" + getTranslationY());
        }
    }

    private void updateAllSingleLineViews() {
        updateSingleLineView();
    }

    private void updateSingleLineView() {
        if (this.mIsChildInGroup) {
            boolean z = this.mSingleLineView == null;
            this.mSingleLineView = this.mHybridGroupManager.bindFromNotification(this.mSingleLineView, this.mStatusBarNotification.getNotification());
            if (z) {
                int i = this.mVisibleType;
                HybridNotificationView hybridNotificationView = this.mSingleLineView;
                updateViewVisibility(i, 3, hybridNotificationView, hybridNotificationView);
                return;
            }
            return;
        }
        View view = this.mSingleLineView;
        if (view != null) {
            removeView(view);
            this.mSingleLineView = null;
        }
    }

    private void applyMediaTransfer(NotificationEntry notificationEntry) {
        View view = this.mExpandedChild;
        if (view != null && notificationEntry.isMediaNotification()) {
            View findViewById = view.findViewById(16909101);
            if (findViewById instanceof LinearLayout) {
                this.mMediaTransferManager.applyMediaTransferView((ViewGroup) findViewById, notificationEntry);
            }
        }
    }

    private void applyRemoteInputAndSmartReply(NotificationEntry notificationEntry) {
        InflatedSmartReplies.SmartRepliesAndActions smartRepliesAndActions;
        int i;
        if (this.mRemoteInputController != null) {
            applyRemoteInput(notificationEntry, InflatedSmartReplies.hasFreeformRemoteInput(notificationEntry));
            if (this.mExpandedInflatedSmartReplies != null || this.mHeadsUpInflatedSmartReplies != null) {
                InflatedSmartReplies inflatedSmartReplies = this.mExpandedInflatedSmartReplies;
                if (inflatedSmartReplies != null) {
                    smartRepliesAndActions = inflatedSmartReplies.getSmartRepliesAndActions();
                } else {
                    smartRepliesAndActions = this.mHeadsUpInflatedSmartReplies.getSmartRepliesAndActions();
                }
                this.mCurrentSmartRepliesAndActions = smartRepliesAndActions;
                if (DEBUG) {
                    Object[] objArr = new Object[3];
                    int i2 = 0;
                    objArr[0] = notificationEntry.notification.getKey();
                    SmartReplyView.SmartActions smartActions = this.mCurrentSmartRepliesAndActions.smartActions;
                    if (smartActions == null) {
                        i = 0;
                    } else {
                        i = smartActions.actions.size();
                    }
                    objArr[1] = Integer.valueOf(i);
                    SmartReplyView.SmartReplies smartReplies = this.mCurrentSmartRepliesAndActions.smartReplies;
                    if (smartReplies != null) {
                        i2 = smartReplies.choices.length;
                    }
                    objArr[2] = Integer.valueOf(i2);
                    Log.d("NotificationContentView", String.format("Adding suggestions for %s, %d actions, and %d replies.", objArr));
                }
                applySmartReplyView(this.mCurrentSmartRepliesAndActions, notificationEntry);
            } else if (DEBUG) {
                Log.d("NotificationContentView", "Both expanded, and heads-up InflatedSmartReplies are null, don't add smart replies.");
            }
        }
    }

    private void applyRemoteInput(NotificationEntry notificationEntry, boolean z) {
        View view = this.mExpandedChild;
        if (view != null) {
            this.mExpandedRemoteInput = applyRemoteInput(view, notificationEntry, z, this.mPreviousExpandedRemoteInputIntent, this.mCachedExpandedRemoteInput, this.mExpandedWrapper);
        } else {
            this.mExpandedRemoteInput = null;
        }
        RemoteInputView remoteInputView = this.mCachedExpandedRemoteInput;
        if (!(remoteInputView == null || remoteInputView == this.mExpandedRemoteInput)) {
            remoteInputView.dispatchFinishTemporaryDetach();
        }
        this.mCachedExpandedRemoteInput = null;
        View view2 = this.mHeadsUpChild;
        if (view2 != null) {
            this.mHeadsUpRemoteInput = applyRemoteInput(view2, notificationEntry, z, this.mPreviousHeadsUpRemoteInputIntent, this.mCachedHeadsUpRemoteInput, this.mHeadsUpWrapper);
        } else {
            this.mHeadsUpRemoteInput = null;
        }
        RemoteInputView remoteInputView2 = this.mCachedHeadsUpRemoteInput;
        if (!(remoteInputView2 == null || remoteInputView2 == this.mHeadsUpRemoteInput)) {
            remoteInputView2.dispatchFinishTemporaryDetach();
        }
        this.mCachedHeadsUpRemoteInput = null;
    }

    private RemoteInputView applyRemoteInput(View view, NotificationEntry notificationEntry, boolean z, PendingIntent pendingIntent, RemoteInputView remoteInputView, NotificationViewWrapper notificationViewWrapper) {
        View findViewById = view.findViewById(16908695);
        if (!(findViewById instanceof FrameLayout)) {
            return null;
        }
        RemoteInputView remoteInputView2 = (RemoteInputView) view.findViewWithTag(RemoteInputView.VIEW_TAG);
        if (remoteInputView2 != null) {
            remoteInputView2.onNotificationUpdateOrReset();
        }
        if (remoteInputView2 != null || !z) {
            remoteInputView = remoteInputView2;
        } else {
            FrameLayout frameLayout = (FrameLayout) findViewById;
            if (remoteInputView == null) {
                remoteInputView = RemoteInputView.inflate(((FrameLayout) this).mContext, frameLayout, notificationEntry, this.mRemoteInputController);
                remoteInputView.setVisibility(4);
                frameLayout.addView(remoteInputView, new FrameLayout.LayoutParams(-1, -1));
            } else {
                frameLayout.addView(remoteInputView);
                remoteInputView.dispatchFinishTemporaryDetach();
                remoteInputView.requestFocus();
            }
        }
        if (z) {
            int i = notificationEntry.notification.getNotification().color;
            if (i == 0) {
                i = ((FrameLayout) this).mContext.getColor(C0004R$color.default_remote_input_background);
            }
            remoteInputView.setBackgroundColor(ContrastColorUtil.ensureTextBackgroundColor(i, ((FrameLayout) this).mContext.getColor(C0004R$color.remote_input_text_enabled), ((FrameLayout) this).mContext.getColor(C0004R$color.remote_input_hint)));
            remoteInputView.setWrapper(notificationViewWrapper);
            remoteInputView.setOnVisibilityChangedListener(new Consumer() {
                /* class com.android.systemui.statusbar.notification.row.$$Lambda$GC_EXjlJWjwU2u0y95DlTq2QVf0 */

                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    NotificationContentView.this.setRemoteInputVisible(((Boolean) obj).booleanValue());
                }
            });
            if (pendingIntent != null || remoteInputView.isActive()) {
                Notification.Action[] actionArr = notificationEntry.notification.getNotification().actions;
                if (pendingIntent != null) {
                    remoteInputView.setPendingIntent(pendingIntent);
                }
                if (remoteInputView.updatePendingIntentFromActions(actionArr)) {
                    if (!remoteInputView.isActive()) {
                        remoteInputView.focus();
                    }
                } else if (remoteInputView.isActive()) {
                    remoteInputView.close();
                }
            }
        }
        return remoteInputView;
    }

    private void applySmartReplyView(InflatedSmartReplies.SmartRepliesAndActions smartRepliesAndActions, NotificationEntry notificationEntry) {
        int i;
        int i2;
        boolean z;
        View view = this.mExpandedChild;
        if (view != null) {
            this.mExpandedSmartReplyView = applySmartReplyView(view, smartRepliesAndActions, notificationEntry, this.mExpandedInflatedSmartReplies);
            if (!(this.mExpandedSmartReplyView == null || (smartRepliesAndActions.smartReplies == null && smartRepliesAndActions.smartActions == null))) {
                SmartReplyView.SmartReplies smartReplies = smartRepliesAndActions.smartReplies;
                boolean z2 = false;
                if (smartReplies == null) {
                    i = 0;
                } else {
                    i = smartReplies.choices.length;
                }
                SmartReplyView.SmartActions smartActions = smartRepliesAndActions.smartActions;
                if (smartActions == null) {
                    i2 = 0;
                } else {
                    i2 = smartActions.actions.size();
                }
                SmartReplyView.SmartReplies smartReplies2 = smartRepliesAndActions.smartReplies;
                if (smartReplies2 == null) {
                    z = smartRepliesAndActions.smartActions.fromAssistant;
                } else {
                    z = smartReplies2.fromAssistant;
                }
                SmartReplyView.SmartReplies smartReplies3 = smartRepliesAndActions.smartReplies;
                if (smartReplies3 != null && this.mSmartReplyConstants.getEffectiveEditChoicesBeforeSending(smartReplies3.remoteInput.getEditChoicesBeforeSending())) {
                    z2 = true;
                }
                this.mSmartReplyController.smartSuggestionsAdded(notificationEntry, i, i2, z, z2);
            }
        }
        if (this.mHeadsUpChild != null && this.mSmartReplyConstants.getShowInHeadsUp()) {
            this.mHeadsUpSmartReplyView = applySmartReplyView(this.mHeadsUpChild, smartRepliesAndActions, notificationEntry, this.mHeadsUpInflatedSmartReplies);
        }
    }

    private SmartReplyView applySmartReplyView(View view, InflatedSmartReplies.SmartRepliesAndActions smartRepliesAndActions, NotificationEntry notificationEntry, InflatedSmartReplies inflatedSmartReplies) {
        View findViewById = view.findViewById(16909379);
        SmartReplyView smartReplyView = null;
        if (!(findViewById instanceof LinearLayout)) {
            return null;
        }
        LinearLayout linearLayout = (LinearLayout) findViewById;
        if (!InflatedSmartReplies.shouldShowSmartReplyView(notificationEntry, smartRepliesAndActions)) {
            linearLayout.setVisibility(8);
            return null;
        }
        if (linearLayout.getChildCount() == 1 && (linearLayout.getChildAt(0) instanceof SmartReplyView)) {
            linearLayout.removeAllViews();
        }
        if (!(linearLayout.getChildCount() != 0 || inflatedSmartReplies == null || inflatedSmartReplies.getSmartReplyView() == null)) {
            smartReplyView = inflatedSmartReplies.getSmartReplyView();
            linearLayout.addView(smartReplyView);
        }
        if (smartReplyView != null) {
            smartReplyView.resetSmartSuggestions(linearLayout);
            smartReplyView.addPreInflatedButtons(inflatedSmartReplies.getSmartSuggestionButtons());
            smartReplyView.setBackgroundTintColor(notificationEntry.getRow().getCurrentBackgroundTint());
            linearLayout.setVisibility(0);
        }
        return smartReplyView;
    }

    public void setExpandedInflatedSmartReplies(InflatedSmartReplies inflatedSmartReplies) {
        this.mExpandedInflatedSmartReplies = inflatedSmartReplies;
        if (inflatedSmartReplies == null) {
            this.mExpandedSmartReplyView = null;
        }
    }

    public void setHeadsUpInflatedSmartReplies(InflatedSmartReplies inflatedSmartReplies) {
        this.mHeadsUpInflatedSmartReplies = inflatedSmartReplies;
        if (inflatedSmartReplies == null) {
            this.mHeadsUpSmartReplyView = null;
        }
    }

    public InflatedSmartReplies.SmartRepliesAndActions getCurrentSmartRepliesAndActions() {
        return this.mCurrentSmartRepliesAndActions;
    }

    private void applyMultiWindowButton(NotificationEntry notificationEntry) {
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str, this.mStatusBarNotification + ": applyMultiWindowButton entry=" + notificationEntry + " mContractedChild=" + this.mContractedChild + " mExpandedChild=" + this.mExpandedChild + " mHeadsUpChild=" + this.mHeadsUpChild);
        }
        Runnable multiWindowButtonInvoker = this.mMultiWindowButtonManager.getMultiWindowButtonInvoker(notificationEntry);
        if (multiWindowButtonInvoker == null) {
            if (findViewById(C0007R$id.multiwindow_button) != null) {
                removeView(this.mMultiWindowButton);
            }
            if (this.mMultiWindowButton != null) {
                this.mMultiWindowButton = null;
                return;
            }
            return;
        }
        if (this.mMultiWindowButton == null || findViewById(C0007R$id.multiwindow_button) == null) {
            this.mMultiWindowButton = (ViewGroup) LayoutInflater.from(((FrameLayout) this).mContext).inflate(C0010R$layout.multiwindow_button, (ViewGroup) this, false);
            addView(this.mMultiWindowButton);
        }
        this.mMultiWindowButton.setOnClickListener(new View.OnClickListener(multiWindowButtonInvoker) {
            /* class com.android.systemui.statusbar.notification.row.$$Lambda$NotificationContentView$3P8AvEbW7CvgWjNG5rrdbDfXQsk */
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void onClick(View view) {
                NotificationContentView.this.lambda$applyMultiWindowButton$0$NotificationContentView(this.f$1, view);
            }
        });
        this.mMultiWindowButton.bringToFront();
        this.mMultiWindowButton.setVisibility(0);
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str2 = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str2, this.mStatusBarNotification + ": applyMultiWindowButton");
        }
    }

    public /* synthetic */ void lambda$applyMultiWindowButton$0$NotificationContentView(Runnable runnable, View view) {
        runnable.run();
        MultiWindowButtonLogger.logEvent(((FrameLayout) this).mContext, "notification_click");
    }

    private void updateMultiWindowButton() {
        int i;
        if (this.mMultiWindowButton == null) {
            resetMarginForMultiWindowButton();
            return;
        }
        int calculateVisibleType = calculateVisibleType();
        View viewForVisibleType = getViewForVisibleType(calculateVisibleType);
        if (viewForVisibleType == null) {
            if (MultiWindowButtonManager.DEBUG_MW) {
                String str = MultiWindowButtonManager.DEBUG_MW_TAG;
                Log.d(str, this.mStatusBarNotification + ": updateMultiWindowButton visibleView is null");
            }
            resetMarginForMultiWindowButton();
            return;
        }
        if (MultiWindowButtonManager.DEBUG_MW) {
            String str2 = MultiWindowButtonManager.DEBUG_MW_TAG;
            Log.d(str2, this.mStatusBarNotification + ": updateMultiWindowButton this:" + this + " visibleType:" + calculateVisibleType + " visibleAmount:" + this.mHeaderVisibleAmount + " mStatusBarState:" + this.mStatusBarState + " backgroundColor:" + getBackgroundColor(calculateVisibleType));
        }
        if (this.mStatusBarState == 0 && this.mHeaderVisibleAmount == 1.0f && (calculateVisibleType == 0 || calculateVisibleType == 1 || calculateVisibleType == 2)) {
            if (calculateVisibleType == 0 || calculateVisibleType == 1) {
                setMarginForMultiWindowButton(getViewForVisibleType(0));
                setMarginForMultiWindowButton(getViewForVisibleType(1));
            } else if (calculateVisibleType == 2) {
                setMarginForMultiWindowButton(viewForVisibleType);
            }
            View findViewById = viewForVisibleType.findViewById(16909169);
            TextView textView = null;
            if (findViewById != null) {
                textView = (TextView) findViewById.findViewById(16908731);
            }
            ImageView imageView = (ImageView) this.mMultiWindowButton.findViewById(C0007R$id.multiwindow_button_image);
            if (textView != null) {
                imageView.setImageTintList(textView.getTextColors());
            } else {
                if (shouldUseDark()) {
                    i = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_dark_image_color);
                } else {
                    i = ((FrameLayout) this).mContext.getColor(C0004R$color.notification_button_light_image_color);
                }
                imageView.setImageTintList(ColorStateList.valueOf(i));
            }
            this.mMultiWindowButton.findViewById(C0007R$id.multiwindow_button_background).setBackgroundTintList(ColorStateList.valueOf(getBackgroundColor(calculateVisibleType)));
            this.mMultiWindowButton.setVisibility(0);
            if (!this.mHadMultiWindowButtonShown) {
                MultiWindowButtonLogger.logEvent(((FrameLayout) this).mContext, "notification_show");
                this.mHadMultiWindowButtonShown = true;
                return;
            }
            return;
        }
        this.mMultiWindowButton.setVisibility(8);
        resetMarginForMultiWindowButton();
    }

    private void setMarginForMultiWindowButton(View view) {
        if (view != null) {
            View findViewById = view.findViewById(16909169);
            if (findViewById != null) {
                view = findViewById;
            }
            if (view instanceof NotificationHeaderView) {
                ((NotificationHeaderView) view).setExtraContentEndMargin(this.mMultiWindowButton.getWidth());
            } else {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                marginLayoutParams.setMarginEnd(this.mMultiWindowButton.getWidth());
                view.setLayoutParams(marginLayoutParams);
            }
            this.mSetMarginViews.add(view);
        }
    }

    private void resetMarginForMultiWindowButton() {
        Iterator<View> it = this.mSetMarginViews.iterator();
        while (it.hasNext()) {
            NotificationHeaderView notificationHeaderView = (View) it.next();
            if (notificationHeaderView instanceof NotificationHeaderView) {
                notificationHeaderView.setExtraContentEndMargin(0);
            } else {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) notificationHeaderView.getLayoutParams();
                marginLayoutParams.setMarginEnd(0);
                notificationHeaderView.setLayoutParams(marginLayoutParams);
            }
        }
        this.mSetMarginViews.clear();
    }

    public void setStatusBarState(int i) {
        this.mStatusBarState = i;
        updateMultiWindowButton();
    }

    public boolean shouldUseDark() {
        int backgroundColor = getBackgroundColor(calculateVisibleType());
        if (backgroundColor != 0) {
            return ContrastColorUtil.isColorLight(backgroundColor);
        }
        return !((((FrameLayout) this).mContext.getResources().getConfiguration().uiMode & 48) == 32);
    }

    public void closeRemoteInput() {
        RemoteInputView remoteInputView = this.mHeadsUpRemoteInput;
        if (remoteInputView != null) {
            remoteInputView.close();
        }
        RemoteInputView remoteInputView2 = this.mExpandedRemoteInput;
        if (remoteInputView2 != null) {
            remoteInputView2.close();
        }
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
    }

    public void setRemoteInputController(RemoteInputController remoteInputController) {
        this.mRemoteInputController = remoteInputController;
    }

    public void setExpandClickListener(View.OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
    }

    public void updateExpandButtons(boolean z) {
        this.mExpandable = z;
        View view = this.mExpandedChild;
        boolean z2 = false;
        if (view == null || view.getHeight() == 0 || ((this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard() ? this.mExpandedChild.getHeight() > this.mHeadsUpChild.getHeight() : this.mExpandedChild.getHeight() > this.mContractedChild.getHeight())) {
            z2 = z;
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.updateExpandability(z2, this.mExpandClickListener);
        }
        if (this.mContractedChild != null) {
            this.mContractedWrapper.updateExpandability(z2, this.mExpandClickListener);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.updateExpandability(z2, this.mExpandClickListener);
        }
        this.mIsContentExpandable = z2;
    }

    public NotificationHeaderView getNotificationHeader() {
        NotificationHeaderView notificationHeader = this.mContractedChild != null ? this.mContractedWrapper.getNotificationHeader() : null;
        if (notificationHeader == null && this.mExpandedChild != null) {
            notificationHeader = this.mExpandedWrapper.getNotificationHeader();
        }
        if (notificationHeader == null && this.mHeadsUpChild != null) {
            notificationHeader = this.mHeadsUpWrapper.getNotificationHeader();
        }
        return (notificationHeader != null || this.mAmbientChild == null) ? notificationHeader : this.mAmbientWrapper.getNotificationHeader();
    }

    public void showAppOpsIcons(ArraySet<Integer> arraySet) {
        if (!(this.mContractedChild == null || this.mContractedWrapper.getNotificationHeader() == null)) {
            this.mContractedWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
        if (!(this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null)) {
            this.mExpandedWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
        if (this.mHeadsUpChild != null && this.mHeadsUpWrapper.getNotificationHeader() != null) {
            this.mHeadsUpWrapper.getNotificationHeader().showAppOpsIcons(arraySet);
        }
    }

    public void setRecentlyAudiblyAlerted(boolean z) {
        if (!(this.mContractedChild == null || this.mContractedWrapper.getNotificationHeader() == null)) {
            this.mContractedWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
        if (!(this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null)) {
            this.mExpandedWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
        if (this.mHeadsUpChild != null && this.mHeadsUpWrapper.getNotificationHeader() != null) {
            this.mHeadsUpWrapper.getNotificationHeader().setRecentlyAudiblyAlerted(z);
        }
    }

    public NotificationHeaderView getContractedNotificationHeader() {
        if (this.mContractedChild != null) {
            return this.mContractedWrapper.getNotificationHeader();
        }
        return null;
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(this.mVisibleType);
        if (visibleWrapper == null) {
            return null;
        }
        return visibleWrapper.getNotificationHeader();
    }

    public void setContainingNotification(ExpandableNotificationRow expandableNotificationRow) {
        this.mContainingNotification = expandableNotificationRow;
    }

    public void requestSelectLayout(boolean z) {
        selectLayout(z, false);
    }

    public void reInflateViews() {
        HybridNotificationView hybridNotificationView;
        if (this.mIsChildInGroup && (hybridNotificationView = this.mSingleLineView) != null) {
            removeView(hybridNotificationView);
            this.mSingleLineView = null;
            updateAllSingleLineViews();
        }
        ViewGroup viewGroup = this.mMultiWindowButton;
        if (viewGroup != null) {
            removeView(viewGroup);
            this.mMultiWindowButton = null;
            applyMultiWindowButton(this.mContainingNotification.getEntry());
            updateMultiWindowButton();
        }
    }

    public void setUserExpanding(boolean z) {
        this.mUserExpanding = z;
        if (z) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            return;
        }
        this.mTransformationStartVisibleType = -1;
        this.mVisibleType = calculateVisibleType();
        updateViewVisibilities(this.mVisibleType);
        updateBackgroundColor(false);
    }

    public void setSingleLineWidthIndention(int i) {
        if (i != this.mSingleLineWidthIndention) {
            this.mSingleLineWidthIndention = i;
            this.mContainingNotification.forceLayout();
            forceLayout();
        }
    }

    public HybridNotificationView getSingleLineView() {
        return this.mSingleLineView;
    }

    public void setRemoved() {
        RemoteInputView remoteInputView = this.mExpandedRemoteInput;
        if (remoteInputView != null) {
            remoteInputView.setRemoved();
        }
        RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
        if (remoteInputView2 != null) {
            remoteInputView2.setRemoved();
        }
    }

    public void setContentHeightAnimating(boolean z) {
        if (!z) {
            this.mContentHeightAtAnimationStart = -1;
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isAnimatingVisibleType() {
        return this.mAnimationStartVisibleType != -1;
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        selectLayout(false, true);
    }

    public void setFocusOnVisibilityChange() {
        this.mFocusOnVisibilityChange = true;
    }

    public void setIconsVisible(boolean z) {
        this.mIconsVisible = z;
        updateIconVisibilities();
    }

    private void updateIconVisibilities() {
        NotificationHeaderView notificationHeader;
        NotificationHeaderView notificationHeader2;
        NotificationHeaderView notificationHeader3;
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        if (!(notificationViewWrapper == null || (notificationHeader3 = notificationViewWrapper.getNotificationHeader()) == null)) {
            notificationHeader3.getIcon().setForceHidden(!this.mIconsVisible);
        }
        NotificationViewWrapper notificationViewWrapper2 = this.mHeadsUpWrapper;
        if (!(notificationViewWrapper2 == null || (notificationHeader2 = notificationViewWrapper2.getNotificationHeader()) == null)) {
            notificationHeader2.getIcon().setForceHidden(!this.mIconsVisible);
        }
        NotificationViewWrapper notificationViewWrapper3 = this.mExpandedWrapper;
        if (notificationViewWrapper3 != null && (notificationHeader = notificationViewWrapper3.getNotificationHeader()) != null) {
            notificationHeader.getIcon().setForceHidden(!this.mIconsVisible);
        }
    }

    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (z) {
            fireExpandedVisibleListenerIfVisible();
        }
    }

    public void setOnExpandedVisibleListener(Runnable runnable) {
        this.mExpandedVisibleListener = runnable;
        fireExpandedVisibleListenerIfVisible();
    }

    public void performWhenContentInactive(int i, Runnable runnable) {
        View viewForVisibleType = getViewForVisibleType(i);
        if (viewForVisibleType == null || isContentViewInactive(i)) {
            runnable.run();
        } else {
            this.mOnContentViewInactiveListeners.put(viewForVisibleType, runnable);
        }
    }

    public boolean isContentViewInactive(int i) {
        return isContentViewInactive(getViewForVisibleType(i));
    }

    private boolean isContentViewInactive(View view) {
        if (view == null || !isShown()) {
            return true;
        }
        if (view.getVisibility() == 0 || getViewForVisibleType(this.mVisibleType) == view) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onChildVisibilityChanged(View view, int i, int i2) {
        Runnable remove;
        super.onChildVisibilityChanged(view, i, i2);
        if (isContentViewInactive(view) && (remove = this.mOnContentViewInactiveListeners.remove(view)) != null) {
            remove.run();
        }
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
    }

    public boolean isDimmable() {
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        return notificationViewWrapper != null && notificationViewWrapper.isDimmable();
    }

    public boolean disallowSingleClick(float f, float f2) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(getVisibleType());
        if (visibleWrapper != null) {
            return visibleWrapper.disallowSingleClick(f, f2);
        }
        return false;
    }

    public boolean shouldClipToRounding(boolean z, boolean z2) {
        boolean shouldClipToRounding = shouldClipToRounding(getVisibleType(), z, z2);
        return this.mUserExpanding ? shouldClipToRounding | shouldClipToRounding(this.mTransformationStartVisibleType, z, z2) : shouldClipToRounding;
    }

    private boolean shouldClipToRounding(int i, boolean z, boolean z2) {
        NotificationViewWrapper visibleWrapper = getVisibleWrapper(i);
        if (visibleWrapper == null) {
            return false;
        }
        return visibleWrapper.shouldClipToRounding(z, z2);
    }

    public CharSequence getActiveRemoteInputText() {
        RemoteInputView remoteInputView = this.mExpandedRemoteInput;
        if (remoteInputView != null && remoteInputView.isActive()) {
            return this.mExpandedRemoteInput.getText();
        }
        RemoteInputView remoteInputView2 = this.mHeadsUpRemoteInput;
        if (remoteInputView2 == null || !remoteInputView2.isActive()) {
            return null;
        }
        return this.mHeadsUpRemoteInput.getText();
    }

    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        float y = motionEvent.getY();
        RemoteInputView remoteInputForView = getRemoteInputForView(getViewForVisibleType(this.mVisibleType));
        if (remoteInputForView != null && remoteInputForView.getVisibility() == 0) {
            int height = this.mUnrestrictedContentHeight - remoteInputForView.getHeight();
            if (y <= ((float) this.mUnrestrictedContentHeight) && y >= ((float) height)) {
                motionEvent.offsetLocation(0.0f, (float) (-height));
                return remoteInputForView.dispatchTouchEvent(motionEvent);
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public boolean pointInView(float f, float f2, float f3) {
        return f >= (-f3) && f2 >= ((float) this.mClipTopAmount) - f3 && f < ((float) (((FrameLayout) this).mRight - ((FrameLayout) this).mLeft)) + f3 && f2 < ((float) this.mUnrestrictedContentHeight) + f3;
    }

    private RemoteInputView getRemoteInputForView(View view) {
        if (view == this.mExpandedChild) {
            return this.mExpandedRemoteInput;
        }
        if (view == this.mHeadsUpChild) {
            return this.mHeadsUpRemoteInput;
        }
        return null;
    }

    public int getExpandHeight() {
        return getViewHeight(this.mExpandedChild == null ? 0 : 1) + getExtraRemoteInputHeight(this.mExpandedRemoteInput);
    }

    public int getHeadsUpHeight() {
        return getViewHeight(this.mHeadsUpChild == null ? 0 : 2) + getExtraRemoteInputHeight(this.mHeadsUpRemoteInput) + getExtraRemoteInputHeight(this.mExpandedRemoteInput);
    }

    public void setRemoteInputVisible(boolean z) {
        this.mRemoteInputVisible = z;
        setClipChildren(!z);
    }

    public void setClipChildren(boolean z) {
        super.setClipChildren(z && !this.mRemoteInputVisible);
    }

    public void setHeaderVisibleAmount(float f) {
        if (this.mMultiWindowButtonManager != null) {
            if (MultiWindowButtonManager.DEBUG_MW) {
                String str = MultiWindowButtonManager.DEBUG_MW_TAG;
                Log.d(str, this.mStatusBarNotification + ": setHeaderVisibleAmount " + f);
            }
            this.mHeaderVisibleAmount = f;
            updateMultiWindowButton();
        }
        NotificationViewWrapper notificationViewWrapper = this.mContractedWrapper;
        if (notificationViewWrapper != null) {
            notificationViewWrapper.setHeaderVisibleAmount(f);
        }
        NotificationViewWrapper notificationViewWrapper2 = this.mHeadsUpWrapper;
        if (notificationViewWrapper2 != null) {
            notificationViewWrapper2.setHeaderVisibleAmount(f);
        }
        NotificationViewWrapper notificationViewWrapper3 = this.mExpandedWrapper;
        if (notificationViewWrapper3 != null) {
            notificationViewWrapper3.setHeaderVisibleAmount(f);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("    ");
        printWriter.print("contentView visibility: " + getVisibility());
        printWriter.print(", alpha: " + getAlpha());
        printWriter.print(", clipBounds: " + getClipBounds());
        printWriter.print(", contentHeight: " + this.mContentHeight);
        printWriter.print(", visibleType: " + this.mVisibleType);
        View viewForVisibleType = getViewForVisibleType(this.mVisibleType);
        printWriter.print(", visibleView ");
        if (viewForVisibleType != null) {
            printWriter.print(" visibility: " + viewForVisibleType.getVisibility());
            printWriter.print(", alpha: " + viewForVisibleType.getAlpha());
            printWriter.print(", clipBounds: " + viewForVisibleType.getClipBounds());
        } else {
            printWriter.print("null");
        }
        printWriter.println();
    }

    public RemoteInputView getExpandedRemoteInput() {
        return this.mExpandedRemoteInput;
    }
}