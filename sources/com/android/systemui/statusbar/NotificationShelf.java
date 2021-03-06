package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.C0003R$bool;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0007R$id;
import com.android.systemui.C0014R$string;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.stack.AmbientState;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.ExpandableViewState;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.notification.stack.ViewState;
import com.android.systemui.statusbar.phone.NotificationIconContainer;

public class NotificationShelf extends ActivatableNotificationView implements View.OnLayoutChangeListener, StatusBarStateController.StateListener {
    private static final boolean ICON_ANMATIONS_WHILE_SCROLLING = SystemProperties.getBoolean("debug.icon_scroll_animations", true);
    private static final int TAG_CONTINUOUS_CLIPPING = C0007R$id.continuous_clipping_tag;
    private static final boolean USE_ANIMATIONS_WHEN_OPENING = SystemProperties.getBoolean("debug.icon_opening_animations", true);
    private AmbientState mAmbientState;
    private boolean mAnimationsEnabled = true;
    private Rect mClipRect = new Rect();
    private NotificationIconContainer mCollapsedIcons;
    private int mCutoutHeight;
    private float mDarkShelfIconSize;
    private float mDarkShelfPadding;
    private float mFirstElementRoundness;
    private int mGapHeight;
    private boolean mHasItemsInStableShelf;
    private boolean mHideBackground;
    private NotificationStackScrollLayout mHostLayout;
    private int mIconAppearTopPadding;
    private int mIconSize;
    private boolean mInteractive;
    private int mMaxLayoutHeight;
    private float mMaxShelfEnd;
    private boolean mNoAnimationsInThisFrame;
    private int mNotGoneIndex;
    private float mOpenedAmount;
    private int mPaddingBetweenElements;
    private int mRelativeOffset;
    private int mScrollFastThreshold;
    private int mShelfAppearTranslation;
    private NotificationIconContainer mShelfIcons;
    private boolean mShowNotificationShelf;
    private int mStatusBarHeight;
    private int mStatusBarPaddingStart;
    private int mStatusBarState;
    private int[] mTmp = new int[2];

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean hasNoContentHeight() {
        return true;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public boolean hasOverlappingRendering() {
        return false;
    }

    public NotificationShelf(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    @VisibleForTesting
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mShelfIcons = (NotificationIconContainer) findViewById(C0007R$id.content);
        this.mShelfIcons.setClipChildren(false);
        this.mShelfIcons.setClipToPadding(false);
        setClipToActualHeight(false);
        setClipChildren(false);
        setClipToPadding(false);
        this.mShelfIcons.setIsStaticLayout(false);
        setBottomRoundness(1.0f, false);
        initDimens();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this, 3);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this);
    }

    public void bind(AmbientState ambientState, NotificationStackScrollLayout notificationStackScrollLayout) {
        this.mAmbientState = ambientState;
        this.mHostLayout = notificationStackScrollLayout;
    }

    private void initDimens() {
        Resources resources = getResources();
        this.mIconAppearTopPadding = resources.getDimensionPixelSize(C0005R$dimen.notification_icon_appear_padding);
        this.mStatusBarHeight = resources.getDimensionPixelOffset(C0005R$dimen.status_bar_height);
        this.mStatusBarPaddingStart = resources.getDimensionPixelOffset(C0005R$dimen.status_bar_padding_start);
        this.mPaddingBetweenElements = resources.getDimensionPixelSize(C0005R$dimen.notification_divider_height);
        this.mShelfAppearTranslation = resources.getDimensionPixelSize(C0005R$dimen.shelf_appear_translation);
        this.mDarkShelfPadding = (float) resources.getDimensionPixelSize(C0005R$dimen.widget_bottom_separator_padding);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = resources.getDimensionPixelOffset(C0005R$dimen.notification_shelf_height);
        setLayoutParams(layoutParams);
        int dimensionPixelOffset = resources.getDimensionPixelOffset(C0005R$dimen.shelf_icon_container_padding);
        this.mShelfIcons.setPadding(dimensionPixelOffset, 0, dimensionPixelOffset, 0);
        this.mScrollFastThreshold = resources.getDimensionPixelOffset(C0005R$dimen.scroll_fast_threshold);
        this.mShowNotificationShelf = resources.getBoolean(C0003R$bool.config_showNotificationShelf);
        this.mIconSize = resources.getDimensionPixelSize(17105430);
        this.mDarkShelfIconSize = (float) resources.getDimensionPixelOffset(C0005R$dimen.dark_shelf_icon_size);
        this.mGapHeight = resources.getDimensionPixelSize(C0005R$dimen.qs_notification_padding);
        if (!this.mShowNotificationShelf) {
            setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        initDimens();
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setDark(boolean z, boolean z2, long j) {
        if (((ActivatableNotificationView) this).mDark != z) {
            super.setDark(z, z2, j);
            this.mShelfIcons.setDark(z, z2, j);
            updateInteractiveness();
            updateOutline();
        }
    }

    public void fadeInTranslating() {
        this.mShelfIcons.setTranslationY((float) (-this.mShelfAppearTranslation));
        this.mShelfIcons.setAlpha(0.0f);
        this.mShelfIcons.animate().setInterpolator(Interpolators.DECELERATE_QUINT).translationY(0.0f).setDuration(200).start();
        this.mShelfIcons.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR).setDuration(200).start();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public View getContentView() {
        return this.mShelfIcons;
    }

    public NotificationIconContainer getShelfIcons() {
        return this.mShelfIcons;
    }

    @Override // com.android.systemui.statusbar.notification.row.ExpandableView
    public ExpandableViewState createExpandableViewState() {
        return new ShelfState();
    }

    public void updateState(AmbientState ambientState) {
        float f;
        ActivatableNotificationView lastVisibleBackgroundChild = ambientState.getLastVisibleBackgroundChild();
        ShelfState shelfState = (ShelfState) getViewState();
        boolean z = true;
        if (!this.mShowNotificationShelf || lastVisibleBackgroundChild == null) {
            shelfState.hidden = true;
            shelfState.location = 64;
            shelfState.hasItemsInStableShelf = false;
            return;
        }
        float innerHeight = ((float) ambientState.getInnerHeight()) + ambientState.getTopPadding() + ambientState.getStackTranslation();
        ExpandableViewState viewState = lastVisibleBackgroundChild.getViewState();
        float f2 = viewState.yTranslation + ((float) viewState.height);
        shelfState.copyFrom(viewState);
        shelfState.height = getIntrinsicHeight();
        float max = Math.max(Math.min(f2, innerHeight) - ((float) shelfState.height), getFullyClosedTranslation());
        if (this.mAmbientState.hasPulsingNotifications()) {
            f = 0.0f;
        } else {
            f = this.mAmbientState.getDarkAmount();
        }
        shelfState.yTranslation = max + (this.mDarkShelfPadding * f);
        shelfState.zTranslation = (float) ambientState.getBaseZHeight();
        shelfState.openedAmount = Math.min(1.0f, (shelfState.yTranslation - getFullyClosedTranslation()) / ((float) ((getIntrinsicHeight() * 2) + this.mCutoutHeight)));
        shelfState.clipTopAmount = 0;
        shelfState.alpha = 1.0f;
        shelfState.belowSpeedBump = this.mAmbientState.getSpeedBumpIndex() == 0;
        shelfState.hideSensitive = false;
        shelfState.xTranslation = getTranslationX();
        int i = this.mNotGoneIndex;
        if (i != -1) {
            shelfState.notGoneIndex = Math.min(shelfState.notGoneIndex, i);
        }
        shelfState.hasItemsInStableShelf = viewState.inShelf;
        if (this.mAmbientState.isShadeExpanded() && !this.mAmbientState.isQsCustomizerShowing()) {
            z = false;
        }
        shelfState.hidden = z;
        shelfState.maxShelfEnd = innerHeight;
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x018d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateAppearance() {
        /*
        // Method dump skipped, instructions count: 695
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.NotificationShelf.updateAppearance():void");
    }

    private void clipTransientViews() {
        for (int i = 0; i < this.mHostLayout.getTransientViewCount(); i++) {
            View transientView = this.mHostLayout.getTransientView(i);
            if (transientView instanceof ExpandableNotificationRow) {
                updateNotificationClipHeight((ExpandableNotificationRow) transientView, getTranslationY(), -1);
            } else {
                Log.e("NotificationShelf", "NotificationShelf.clipTransientViews(): Trying to clip non-row transient view");
            }
        }
    }

    private void setFirstElementRoundness(float f) {
        if (this.mFirstElementRoundness != f) {
            this.mFirstElementRoundness = f;
            setTopRoundness(f, false);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateIconClipAmount(ExpandableNotificationRow expandableNotificationRow) {
        float translationY = expandableNotificationRow.getTranslationY();
        if (getClipTopAmount() != 0) {
            translationY = Math.max(translationY, getTranslationY() + ((float) getClipTopAmount()));
        }
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        float translationY2 = getTranslationY() + ((float) statusBarIconView.getTop()) + statusBarIconView.getTranslationY();
        if (translationY2 >= translationY || this.mAmbientState.isFullyDark()) {
            statusBarIconView.setClipBounds(null);
            return;
        }
        int i = (int) (translationY - translationY2);
        statusBarIconView.setClipBounds(new Rect(0, i, statusBarIconView.getWidth(), Math.max(i, statusBarIconView.getHeight())));
    }

    private void updateContinuousClipping(final ExpandableNotificationRow expandableNotificationRow) {
        final StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        boolean z = true;
        boolean z2 = ViewState.isAnimatingY(statusBarIconView) && !this.mAmbientState.isDark();
        if (statusBarIconView.getTag(TAG_CONTINUOUS_CLIPPING) == null) {
            z = false;
        }
        if (z2 && !z) {
            final ViewTreeObserver viewTreeObserver = statusBarIconView.getViewTreeObserver();
            final AnonymousClass1 r2 = new ViewTreeObserver.OnPreDrawListener() {
                /* class com.android.systemui.statusbar.NotificationShelf.AnonymousClass1 */

                public boolean onPreDraw() {
                    if (!ViewState.isAnimatingY(statusBarIconView)) {
                        if (viewTreeObserver.isAlive()) {
                            viewTreeObserver.removeOnPreDrawListener(this);
                        }
                        statusBarIconView.setTag(NotificationShelf.TAG_CONTINUOUS_CLIPPING, null);
                        return true;
                    }
                    NotificationShelf.this.updateIconClipAmount(expandableNotificationRow);
                    return true;
                }
            };
            viewTreeObserver.addOnPreDrawListener(r2);
            statusBarIconView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                /* class com.android.systemui.statusbar.NotificationShelf.AnonymousClass2 */

                public void onViewAttachedToWindow(View view) {
                }

                public void onViewDetachedFromWindow(View view) {
                    if (view == statusBarIconView) {
                        if (viewTreeObserver.isAlive()) {
                            viewTreeObserver.removeOnPreDrawListener(r2);
                        }
                        statusBarIconView.setTag(NotificationShelf.TAG_CONTINUOUS_CLIPPING, null);
                    }
                }
            });
            statusBarIconView.setTag(TAG_CONTINUOUS_CLIPPING, r2);
        }
    }

    private int updateNotificationClipHeight(ActivatableNotificationView activatableNotificationView, float f, int i) {
        float translationY = activatableNotificationView.getTranslationY() + ((float) activatableNotificationView.getActualHeight());
        boolean z = true;
        boolean z2 = (activatableNotificationView.isPinned() || activatableNotificationView.isHeadsUpAnimatingAway()) && !this.mAmbientState.isDozingAndNotPulsing(activatableNotificationView);
        if ((!activatableNotificationView.showingAmbientPulsing() || this.mAmbientState.isFullyDark()) && (!this.mAmbientState.isPulseExpanding() || i != 0)) {
            z = false;
        }
        if (translationY <= f || z || (!this.mAmbientState.isShadeExpanded() && z2)) {
            activatableNotificationView.setClipBottomAmount(0);
        } else {
            int i2 = (int) (translationY - f);
            if (z2) {
                i2 = Math.min(activatableNotificationView.getIntrinsicHeight() - activatableNotificationView.getCollapsedHeight(), i2);
            }
            activatableNotificationView.setClipBottomAmount(i2);
        }
        if (z) {
            return (int) (translationY - getTranslationY());
        }
        return 0;
    }

    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void setFakeShadowIntensity(float f, float f2, int i, int i2) {
        if (!this.mHasItemsInStableShelf) {
            f = 0.0f;
        }
        super.setFakeShadowIntensity(f, f2, i, i2);
    }

    private float updateIconAppearance(ExpandableNotificationRow expandableNotificationRow, float f, boolean z, boolean z2, boolean z3, boolean z4) {
        float f2;
        NotificationIconContainer.IconState iconState = getIconState(expandableNotificationRow.getEntry().expandedIcon);
        if (iconState == null) {
            return 0.0f;
        }
        float translationY = expandableNotificationRow.getTranslationY();
        int actualHeight = expandableNotificationRow.getActualHeight() + this.mPaddingBetweenElements;
        float f3 = 1.0f;
        float min = Math.min(((float) getIntrinsicHeight()) * 1.5f * NotificationUtils.interpolate(1.0f, 1.5f, f), (float) actualHeight);
        if (z4) {
            actualHeight = Math.min(actualHeight, expandableNotificationRow.getMinHeight() - getIntrinsicHeight());
            min = Math.min(min, (float) (expandableNotificationRow.getMinHeight() - getIntrinsicHeight()));
        }
        float f4 = ((float) actualHeight) + translationY;
        boolean z5 = true;
        if (z3 && this.mAmbientState.getScrollY() == 0 && !this.mAmbientState.isOnKeyguard() && !iconState.isLastExpandIcon) {
            float intrinsicPadding = (float) (this.mAmbientState.getIntrinsicPadding() + this.mHostLayout.getPositionInLinearLayout(expandableNotificationRow));
            float intrinsicHeight = (float) (this.mMaxLayoutHeight - getIntrinsicHeight());
            if (intrinsicPadding < intrinsicHeight && ((float) expandableNotificationRow.getIntrinsicHeight()) + intrinsicPadding >= intrinsicHeight && expandableNotificationRow.getTranslationY() < intrinsicPadding) {
                iconState.isLastExpandIcon = true;
                iconState.customTransformHeight = Integer.MIN_VALUE;
                if (!(((float) (this.mMaxLayoutHeight - getIntrinsicHeight())) - intrinsicPadding < ((float) getIntrinsicHeight()))) {
                    iconState.customTransformHeight = (int) (((float) (this.mMaxLayoutHeight - getIntrinsicHeight())) - intrinsicPadding);
                }
            }
        }
        float translationY2 = getTranslationY();
        if (iconState.hasCustomTransformHeight()) {
            actualHeight = iconState.customTransformHeight;
            min = (float) actualHeight;
        }
        if (f4 < translationY2 || ((this.mAmbientState.isUnlockHintRunning() && !expandableNotificationRow.isInShelf()) || (!this.mAmbientState.isShadeExpanded() && (expandableNotificationRow.isPinned() || expandableNotificationRow.isHeadsUpAnimatingAway())))) {
            f3 = 0.0f;
        } else if (translationY < translationY2) {
            float f5 = translationY2 - translationY;
            float min2 = Math.min(1.0f, f5 / ((float) actualHeight));
            f3 = 1.0f - Math.min(1.0f, f5 / min);
            f2 = 1.0f - NotificationUtils.interpolate(Interpolators.ACCELERATE_DECELERATE.getInterpolation(min2), min2, f);
            z5 = false;
            if (z5 && !z3 && iconState.isLastExpandIcon) {
                iconState.isLastExpandIcon = false;
                iconState.customTransformHeight = Integer.MIN_VALUE;
            }
            updateIconPositioning(expandableNotificationRow, f3, f2, min, z, z2, z3, z4);
            return f2;
        }
        f2 = f3;
        iconState.isLastExpandIcon = false;
        iconState.customTransformHeight = Integer.MIN_VALUE;
        updateIconPositioning(expandableNotificationRow, f3, f2, min, z, z2, z3, z4);
        return f2;
    }

    private void updateIconPositioning(ExpandableNotificationRow expandableNotificationRow, float f, float f2, float f3, boolean z, boolean z2, boolean z3, boolean z4) {
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(statusBarIconView);
        if (iconState != null) {
            boolean z5 = false;
            boolean z6 = iconState.isLastExpandIcon && !iconState.hasCustomTransformHeight();
            float f4 = 1.0f;
            float f5 = 0.0f;
            float f6 = f > 0.5f ? 1.0f : 0.0f;
            if (f6 == f2) {
                iconState.noAnimations = (z2 || z3) && !z6;
                iconState.useFullTransitionAmount = iconState.noAnimations || (!ICON_ANMATIONS_WHILE_SCROLLING && f2 == 0.0f && z);
                iconState.useLinearTransitionAmount = !ICON_ANMATIONS_WHILE_SCROLLING && f2 == 0.0f && !this.mAmbientState.isExpansionChanging();
                iconState.translateContent = (((float) this.mMaxLayoutHeight) - getTranslationY()) - ((float) getIntrinsicHeight()) > 0.0f;
            }
            if (!z6 && (z2 || (z3 && iconState.useFullTransitionAmount && !ViewState.isAnimatingY(statusBarIconView)))) {
                iconState.cancelAnimations(statusBarIconView);
                iconState.useFullTransitionAmount = true;
                iconState.noAnimations = true;
            }
            if (iconState.hasCustomTransformHeight()) {
                iconState.useFullTransitionAmount = true;
            }
            if (iconState.isLastExpandIcon) {
                iconState.translateContent = false;
            }
            if (!this.mAmbientState.isDarkAtAll() || expandableNotificationRow.isInShelf()) {
                if (z4 || !USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount || iconState.useLinearTransitionAmount) {
                    f4 = f;
                } else {
                    iconState.needsCannedAnimation = iconState.clampedAppearAmount != f6 && !this.mNoAnimationsInThisFrame;
                    f4 = f6;
                }
            } else if (!this.mAmbientState.isFullyDark()) {
                f4 = 0.0f;
            }
            iconState.iconAppearAmount = (!USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount) ? f2 : f4;
            iconState.clampedAppearAmount = f6;
            if (!expandableNotificationRow.isAboveShelf() && (z4 || iconState.translateContent)) {
                f5 = f;
            }
            expandableNotificationRow.setContentTransformationAmount(f5, z4);
            if (f6 != f4) {
                z5 = true;
            }
            setIconTransformationAmount(expandableNotificationRow, f4, f3, z5, z4);
        }
    }

    private void setIconTransformationAmount(ExpandableNotificationRow expandableNotificationRow, float f, float f2, boolean z, boolean z2) {
        int i;
        float f3;
        float f4;
        StatusBarIconView statusBarIconView = expandableNotificationRow.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(statusBarIconView);
        View notificationIcon = expandableNotificationRow.getNotificationIcon();
        float translationY = expandableNotificationRow.getTranslationY() + expandableNotificationRow.getContentTranslation();
        boolean z3 = expandableNotificationRow.isInShelf() && !expandableNotificationRow.isTransformingIntoShelf();
        if (z && !z3) {
            translationY = getTranslationY() - f2;
        }
        if (notificationIcon != null) {
            i = expandableNotificationRow.getRelativeTopPadding(notificationIcon);
            f3 = (float) notificationIcon.getHeight();
        } else {
            i = this.mIconAppearTopPadding;
            f3 = 0.0f;
        }
        float f5 = translationY + ((float) i);
        float translationY2 = getTranslationY() + ((float) statusBarIconView.getTop());
        float f6 = ((ActivatableNotificationView) this).mDark ? this.mDarkShelfIconSize : (float) this.mIconSize;
        float interpolate = NotificationUtils.interpolate(f5 - (translationY2 + ((((float) statusBarIconView.getHeight()) - (statusBarIconView.getIconScale() * f6)) / 2.0f)), 0.0f, f);
        float iconScale = f6 * statusBarIconView.getIconScale();
        boolean z4 = !expandableNotificationRow.isShowingIcon();
        if (z4) {
            f3 = iconScale / 2.0f;
            f4 = f;
        } else {
            f4 = 1.0f;
        }
        float interpolate2 = NotificationUtils.interpolate(f3, iconScale, f);
        if (iconState != null) {
            iconState.scaleX = interpolate2 / iconScale;
            iconState.scaleY = iconState.scaleX;
            iconState.hidden = f == 0.0f && !iconState.isAnimating(statusBarIconView);
            if (expandableNotificationRow.isDrawingAppearAnimation() && !expandableNotificationRow.isInShelf()) {
                iconState.hidden = true;
                iconState.iconAppearAmount = 0.0f;
            }
            iconState.alpha = f4;
            iconState.yTranslation = interpolate;
            if (z3) {
                iconState.iconAppearAmount = 1.0f;
                iconState.alpha = 1.0f;
                iconState.scaleX = 1.0f;
                iconState.scaleY = 1.0f;
                iconState.hidden = false;
            }
            if ((expandableNotificationRow.isAboveShelf() || (!expandableNotificationRow.isInShelf() && ((z2 && expandableNotificationRow.areGutsExposed()) || expandableNotificationRow.getTranslationZ() > ((float) this.mAmbientState.getBaseZHeight())))) && !this.mAmbientState.isFullyDark()) {
                iconState.hidden = true;
            }
            int contrastedStaticDrawableColor = statusBarIconView.getContrastedStaticDrawableColor(getBackgroundColorWithoutTint());
            if (!z4 && contrastedStaticDrawableColor != 0) {
                contrastedStaticDrawableColor = NotificationUtils.interpolateColors(expandableNotificationRow.getVisibleNotificationHeader().getOriginalIconColor(), contrastedStaticDrawableColor, iconState.iconAppearAmount);
            }
            iconState.iconColor = contrastedStaticDrawableColor;
        }
    }

    private NotificationIconContainer.IconState getIconState(StatusBarIconView statusBarIconView) {
        return this.mShelfIcons.getIconState(statusBarIconView);
    }

    private float getFullyClosedTranslation() {
        return (float) ((-(getIntrinsicHeight() - this.mStatusBarHeight)) / 2);
    }

    public int getNotificationMergeSize() {
        return getIntrinsicHeight();
    }

    private void setHideBackground(boolean z) {
        if (this.mHideBackground != z) {
            this.mHideBackground = z;
            updateBackground();
            updateOutline();
        }
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ExpandableOutlineView
    public boolean needsOutline() {
        return !this.mHideBackground && !((ActivatableNotificationView) this).mDark && super.needsOutline();
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mHideBackground || ((ActivatableNotificationView) this).mDark;
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView, com.android.systemui.statusbar.notification.row.ExpandableView
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        updateRelativeOffset();
        int i5 = getResources().getDisplayMetrics().heightPixels;
        this.mClipRect.set(0, -i5, getWidth(), i5);
        this.mShelfIcons.setClipBounds(this.mClipRect);
    }

    private void updateRelativeOffset() {
        this.mCollapsedIcons.getLocationOnScreen(this.mTmp);
        int[] iArr = this.mTmp;
        this.mRelativeOffset = iArr[0];
        getLocationOnScreen(iArr);
        this.mRelativeOffset -= this.mTmp[0];
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        WindowInsets onApplyWindowInsets = super.onApplyWindowInsets(windowInsets);
        DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        this.mCutoutHeight = (displayCutout == null || displayCutout.getSafeInsetTop() < 0) ? 0 : displayCutout.getSafeInsetTop();
        return onApplyWindowInsets;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setOpenedAmount(float f) {
        int i;
        this.mNoAnimationsInThisFrame = f == 1.0f && this.mOpenedAmount == 0.0f;
        this.mOpenedAmount = f;
        if (!this.mAmbientState.isPanelFullWidth() || this.mAmbientState.isDark()) {
            f = 1.0f;
        }
        int i2 = this.mRelativeOffset;
        if (isLayoutRtl()) {
            i2 = (getWidth() - i2) - this.mCollapsedIcons.getWidth();
        }
        this.mShelfIcons.setActualLayoutWidth((int) NotificationUtils.interpolate((float) (this.mCollapsedIcons.getFinalTranslationX() + i2), (float) this.mShelfIcons.getWidth(), Interpolators.FAST_OUT_SLOW_IN_REVERSE.getInterpolation(f)));
        boolean hasOverflow = this.mCollapsedIcons.hasOverflow();
        int paddingEnd = this.mCollapsedIcons.getPaddingEnd();
        if (!hasOverflow) {
            i = this.mCollapsedIcons.getNoOverflowExtraPadding();
        } else {
            i = this.mCollapsedIcons.getPartialOverflowExtraPadding();
        }
        this.mShelfIcons.setActualPaddingEnd(NotificationUtils.interpolate((float) (paddingEnd - i), (float) this.mShelfIcons.getPaddingEnd(), f));
        this.mShelfIcons.setActualPaddingStart(NotificationUtils.interpolate((float) i2, (float) this.mShelfIcons.getPaddingStart(), f));
        this.mShelfIcons.setOpenedAmount(f);
    }

    public void setMaxLayoutHeight(int i) {
        this.mMaxLayoutHeight = i;
    }

    public int getNotGoneIndex() {
        return this.mNotGoneIndex;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setHasItemsInStableShelf(boolean z) {
        if (this.mHasItemsInStableShelf != z) {
            this.mHasItemsInStableShelf = z;
            updateInteractiveness();
        }
    }

    public void setCollapsedIcons(NotificationIconContainer notificationIconContainer) {
        this.mCollapsedIcons = notificationIconContainer;
        this.mCollapsedIcons.addOnLayoutChangeListener(this);
    }

    @Override // com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener
    public void onStateChanged(int i) {
        this.mStatusBarState = i;
        updateInteractiveness();
    }

    private void updateInteractiveness() {
        int i = 1;
        this.mInteractive = this.mStatusBarState == 1 && this.mHasItemsInStableShelf && !((ActivatableNotificationView) this).mDark;
        setClickable(this.mInteractive);
        setFocusable(this.mInteractive);
        if (!this.mInteractive) {
            i = 4;
        }
        setImportantForAccessibility(i);
    }

    /* access modifiers changed from: protected */
    @Override // com.android.systemui.statusbar.notification.row.ActivatableNotificationView
    public boolean isInteractive() {
        return this.mInteractive;
    }

    public void setMaxShelfEnd(float f) {
        this.mMaxShelfEnd = f;
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        if (!z) {
            this.mShelfIcons.setAnimationsEnabled(false);
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.mInteractive) {
            accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            accessibilityNodeInfo.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(C0014R$string.accessibility_overflow_action)));
        }
    }

    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateRelativeOffset();
    }

    public void onUiModeChanged() {
        updateBackgroundColors();
    }

    /* access modifiers changed from: private */
    public class ShelfState extends ExpandableViewState {
        private boolean hasItemsInStableShelf;
        private float maxShelfEnd;
        private float openedAmount;

        private ShelfState() {
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState, com.android.systemui.statusbar.notification.stack.ExpandableViewState
        public void applyToView(View view) {
            if (NotificationShelf.this.mShowNotificationShelf) {
                super.applyToView(view);
                NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
                NotificationShelf.this.setOpenedAmount(this.openedAmount);
                NotificationShelf.this.updateAppearance();
                NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
                NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
            }
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState, com.android.systemui.statusbar.notification.stack.ExpandableViewState
        public void animateTo(View view, AnimationProperties animationProperties) {
            if (NotificationShelf.this.mShowNotificationShelf) {
                super.animateTo(view, animationProperties);
                NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
                NotificationShelf.this.setOpenedAmount(this.openedAmount);
                NotificationShelf.this.updateAppearance();
                NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
                NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
            }
        }
    }
}
