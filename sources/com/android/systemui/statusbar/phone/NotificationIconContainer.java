package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.util.AttributeSet;
import android.view.View;
import androidx.collection.ArrayMap;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.AlphaOptimizedFrameLayout;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.stack.AnimationFilter;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.ViewState;
import java.util.ArrayList;
import java.util.HashMap;

public class NotificationIconContainer extends AlphaOptimizedFrameLayout {
    private static final AnimationProperties ADD_ICON_PROPERTIES;
    private static final AnimationProperties DOT_ANIMATION_PROPERTIES;
    private static final AnimationProperties ICON_ANIMATION_PROPERTIES;
    private static final AnimationProperties UNISOLATION_PROPERTY;
    private static final AnimationProperties UNISOLATION_PROPERTY_OTHERS;
    private static final AnimationProperties sTempProperties = new AnimationProperties() {
        /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass3 */
        private AnimationFilter mAnimationFilter = new AnimationFilter();

        @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    };
    private int[] mAbsolutePosition = new int[2];
    private int mActualLayoutWidth = Integer.MIN_VALUE;
    private float mActualPaddingEnd = -2.14748365E9f;
    private float mActualPaddingStart = -2.14748365E9f;
    private int mAddAnimationStartIndex = -1;
    private boolean mAnimationsEnabled = true;
    private int mCannedAnimationStartIndex = -1;
    private boolean mChangingViewPositions;
    private boolean mDark;
    private boolean mDisallowNextAnimation;
    private int mDotPadding;
    private IconState mFirstVisibleIconState;
    private int mIconSize;
    private final HashMap<View, IconState> mIconStates = new HashMap<>();
    private boolean mIsStaticLayout = true;
    private StatusBarIconView mIsolatedIcon;
    private View mIsolatedIconForAnimation;
    private Rect mIsolatedIconLocation;
    private IconState mLastVisibleIconState;
    private int mNumDots;
    private float mOpenedAmount = 0.0f;
    private int mOverflowWidth;
    private ArrayMap<String, ArrayList<StatusBarIcon>> mReplacingIcons;
    private int mSpeedBumpIndex = -1;
    private int mStaticDotDiameter;
    private int mStaticDotRadius;
    private float mVisualOverflowStart;

    static {
        AnonymousClass1 r0 = new AnimationProperties() {
            /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass1 */
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateX();
                this.mAnimationFilter = animationFilter;
            }

            @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r0.setDuration(200);
        DOT_ANIMATION_PROPERTIES = r0;
        AnonymousClass2 r02 = new AnimationProperties() {
            /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass2 */
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateY();
                animationFilter.animateAlpha();
                animationFilter.animateScale();
                this.mAnimationFilter = animationFilter;
            }

            @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r02.setDuration(100);
        r02.setCustomInterpolator(View.TRANSLATION_Y, Interpolators.ICON_OVERSHOT);
        ICON_ANIMATION_PROPERTIES = r02;
        AnonymousClass4 r03 = new AnimationProperties() {
            /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass4 */
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateAlpha();
                this.mAnimationFilter = animationFilter;
            }

            @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r03.setDuration(200);
        r03.setDelay(50);
        ADD_ICON_PROPERTIES = r03;
        AnonymousClass5 r04 = new AnimationProperties() {
            /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass5 */
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateAlpha();
                this.mAnimationFilter = animationFilter;
            }

            @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r04.setDuration(110);
        UNISOLATION_PROPERTY_OTHERS = r04;
        AnonymousClass6 r05 = new AnimationProperties() {
            /* class com.android.systemui.statusbar.phone.NotificationIconContainer.AnonymousClass6 */
            private AnimationFilter mAnimationFilter;

            {
                AnimationFilter animationFilter = new AnimationFilter();
                animationFilter.animateX();
                this.mAnimationFilter = animationFilter;
            }

            @Override // com.android.systemui.statusbar.notification.stack.AnimationProperties
            public AnimationFilter getAnimationFilter() {
                return this.mAnimationFilter;
            }
        };
        r05.setDuration(110);
        UNISOLATION_PROPERTY = r05;
    }

    public NotificationIconContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initDimens();
        setWillNotDraw(true);
    }

    private void initDimens() {
        this.mDotPadding = getResources().getDimensionPixelSize(C0005R$dimen.overflow_icon_dot_padding);
        this.mStaticDotRadius = getResources().getDimensionPixelSize(C0005R$dimen.overflow_dot_radius);
        this.mStaticDotDiameter = this.mStaticDotRadius * 2;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(-65536);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(getActualPaddingStart(), 0.0f, getLayoutEnd(), (float) getHeight(), paint);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        initDimens();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        float height = ((float) getHeight()) / 2.0f;
        this.mIconSize = 0;
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            int measuredWidth = childAt.getMeasuredWidth();
            int measuredHeight = childAt.getMeasuredHeight();
            int i6 = (int) (height - (((float) measuredHeight) / 2.0f));
            childAt.layout(0, i6, measuredWidth, measuredHeight + i6);
            if (i5 == 0) {
                setIconSize(childAt.getWidth());
            }
        }
        getLocationOnScreen(this.mAbsolutePosition);
        if (this.mIsStaticLayout) {
            updateState();
        }
    }

    private void setIconSize(int i) {
        this.mIconSize = i;
        this.mOverflowWidth = this.mIconSize + ((this.mStaticDotDiameter + this.mDotPadding) * 0);
    }

    private void updateState() {
        resetViewStates();
        calculateIconTranslations();
        applyIconStates();
    }

    public void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            IconState iconState = this.mIconStates.get(childAt);
            if (iconState != null) {
                iconState.applyToView(childAt);
            }
        }
        this.mAddAnimationStartIndex = -1;
        this.mCannedAnimationStartIndex = -1;
        this.mDisallowNextAnimation = false;
        this.mIsolatedIconForAnimation = null;
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        boolean isReplacingIcon = isReplacingIcon(view);
        if (!this.mChangingViewPositions) {
            IconState iconState = new IconState();
            if (isReplacingIcon) {
                iconState.justAdded = false;
                iconState.justReplaced = true;
            }
            this.mIconStates.put(view, iconState);
        }
        int indexOfChild = indexOfChild(view);
        if (indexOfChild < getChildCount() - 1 && !isReplacingIcon && this.mIconStates.get(getChildAt(indexOfChild + 1)).iconAppearAmount > 0.0f) {
            int i = this.mAddAnimationStartIndex;
            if (i < 0) {
                this.mAddAnimationStartIndex = indexOfChild;
            } else {
                this.mAddAnimationStartIndex = Math.min(i, indexOfChild);
            }
        }
        if (view instanceof StatusBarIconView) {
            ((StatusBarIconView) view).setDark(this.mDark, false, 0);
        }
    }

    private boolean isReplacingIcon(View view) {
        if (this.mReplacingIcons == null || !(view instanceof StatusBarIconView)) {
            return false;
        }
        StatusBarIconView statusBarIconView = (StatusBarIconView) view;
        Icon sourceIcon = statusBarIconView.getSourceIcon();
        ArrayList<StatusBarIcon> arrayList = this.mReplacingIcons.get(statusBarIconView.getNotification().getGroupKey());
        if (arrayList == null || !sourceIcon.sameAs(arrayList.get(0).icon)) {
            return false;
        }
        return true;
    }

    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if (view instanceof StatusBarIconView) {
            boolean isReplacingIcon = isReplacingIcon(view);
            StatusBarIconView statusBarIconView = (StatusBarIconView) view;
            if (this.mAnimationsEnabled && statusBarIconView.getVisibleState() != 2 && view.getVisibility() == 0 && isReplacingIcon) {
                int findFirstViewIndexAfter = findFirstViewIndexAfter(statusBarIconView.getTranslationX());
                int i = this.mAddAnimationStartIndex;
                if (i < 0) {
                    this.mAddAnimationStartIndex = findFirstViewIndexAfter;
                } else {
                    this.mAddAnimationStartIndex = Math.min(i, findFirstViewIndexAfter);
                }
            }
            if (!this.mChangingViewPositions) {
                this.mIconStates.remove(view);
                if (this.mAnimationsEnabled && !isReplacingIcon) {
                    boolean z = false;
                    addTransientView(statusBarIconView, 0);
                    if (view == this.mIsolatedIcon) {
                        z = true;
                    }
                    statusBarIconView.setVisibleState(2, true, new Runnable(statusBarIconView) {
                        /* class com.android.systemui.statusbar.phone.$$Lambda$NotificationIconContainer$sYOppFQ4vSNRi0SYdFbv716CxNY */
                        private final /* synthetic */ StatusBarIconView f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            NotificationIconContainer.this.lambda$onViewRemoved$0$NotificationIconContainer(this.f$1);
                        }
                    }, z ? 110 : 0);
                }
            }
        }
    }

    public /* synthetic */ void lambda$onViewRemoved$0$NotificationIconContainer(StatusBarIconView statusBarIconView) {
        removeTransientView(statusBarIconView);
    }

    private int findFirstViewIndexAfter(float f) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getTranslationX() > f) {
                return i;
            }
        }
        return getChildCount();
    }

    public void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            IconState iconState = this.mIconStates.get(childAt);
            iconState.initFrom(childAt);
            StatusBarIconView statusBarIconView = this.mIsolatedIcon;
            iconState.alpha = (statusBarIconView == null || childAt == statusBarIconView) ? 1.0f : 0.0f;
            iconState.hidden = false;
        }
    }

    public void calculateIconTranslations() {
        int i;
        IconState iconState;
        float f;
        boolean z;
        int i2;
        float actualPaddingStart = getActualPaddingStart();
        int childCount = getChildCount();
        if (this.mDark) {
            i = 5;
        } else {
            i = this.mIsStaticLayout ? 4 : childCount;
        }
        float layoutEnd = getLayoutEnd();
        float maxOverflowStart = getMaxOverflowStart();
        float f2 = 0.0f;
        this.mVisualOverflowStart = 0.0f;
        this.mFirstVisibleIconState = null;
        int i3 = this.mSpeedBumpIndex;
        int i4 = -1;
        boolean z2 = i3 != -1 && i3 < getChildCount();
        float f3 = actualPaddingStart;
        int i5 = -1;
        int i6 = 0;
        while (i6 < childCount) {
            View childAt = getChildAt(i6);
            IconState iconState2 = this.mIconStates.get(childAt);
            iconState2.xTranslation = f3;
            if (this.mFirstVisibleIconState == null) {
                this.mFirstVisibleIconState = iconState2;
            }
            int i7 = this.mSpeedBumpIndex;
            boolean z3 = (i7 != i4 && i6 >= i7 && iconState2.iconAppearAmount > f2) || i6 >= i;
            boolean z4 = i6 == childCount + -1;
            float iconScaleFullyDark = (!this.mDark || !(childAt instanceof StatusBarIconView)) ? 1.0f : ((StatusBarIconView) childAt).getIconScaleFullyDark();
            if (this.mOpenedAmount != f2) {
                z4 = z4 && !z2 && !z3;
            }
            iconState2.visibleState = 0;
            if (z4) {
                f = layoutEnd - ((float) this.mIconSize);
            } else {
                f = maxOverflowStart - ((float) this.mIconSize);
            }
            if (f3 > f) {
                i2 = -1;
                z = true;
            } else {
                z = false;
                i2 = -1;
            }
            if (i5 == i2 && (z3 || z)) {
                int i8 = (!z4 || z3) ? i6 : i6 - 1;
                this.mVisualOverflowStart = layoutEnd - ((float) this.mOverflowWidth);
                if (z3 || this.mIsStaticLayout) {
                    this.mVisualOverflowStart = Math.min(f3, this.mVisualOverflowStart);
                }
                i5 = i8;
            }
            f3 += iconState2.iconAppearAmount * ((float) childAt.getWidth()) * iconScaleFullyDark;
            i6++;
            f2 = 0.0f;
            i4 = -1;
        }
        this.mNumDots = 0;
        if (i5 != -1) {
            f3 = this.mVisualOverflowStart;
            for (int i9 = i5; i9 < childCount; i9++) {
                IconState iconState3 = this.mIconStates.get(getChildAt(i9));
                int i10 = this.mStaticDotDiameter + this.mDotPadding;
                iconState3.xTranslation = f3;
                int i11 = this.mNumDots;
                if (i11 < 1) {
                    if (i11 != 0 || iconState3.iconAppearAmount >= 0.8f) {
                        iconState3.visibleState = 1;
                        this.mNumDots++;
                    } else {
                        iconState3.visibleState = 0;
                    }
                    if (this.mNumDots == 1) {
                        i10 *= 1;
                    }
                    f3 += ((float) i10) * iconState3.iconAppearAmount;
                    this.mLastVisibleIconState = iconState3;
                } else {
                    iconState3.visibleState = 2;
                }
            }
        } else if (childCount > 0) {
            this.mLastVisibleIconState = this.mIconStates.get(getChildAt(childCount - 1));
            this.mFirstVisibleIconState = this.mIconStates.get(getChildAt(0));
        }
        if (this.mDark && f3 < getLayoutEnd()) {
            IconState iconState4 = this.mFirstVisibleIconState;
            float f4 = iconState4 == null ? 0.0f : iconState4.xTranslation;
            IconState iconState5 = this.mLastVisibleIconState;
            float layoutEnd2 = ((getLayoutEnd() - getActualPaddingStart()) - (iconState5 != null ? Math.min((float) getWidth(), iconState5.xTranslation + ((float) this.mIconSize)) - f4 : 0.0f)) / 2.0f;
            if (i5 != -1) {
                layoutEnd2 = (((getLayoutEnd() - this.mVisualOverflowStart) / 2.0f) + layoutEnd2) / 2.0f;
            }
            for (int i12 = 0; i12 < childCount; i12++) {
                this.mIconStates.get(getChildAt(i12)).xTranslation += layoutEnd2;
            }
        }
        if (isLayoutRtl()) {
            for (int i13 = 0; i13 < childCount; i13++) {
                View childAt2 = getChildAt(i13);
                IconState iconState6 = this.mIconStates.get(childAt2);
                iconState6.xTranslation = (((float) getWidth()) - iconState6.xTranslation) - ((float) childAt2.getWidth());
            }
        }
        StatusBarIconView statusBarIconView = this.mIsolatedIcon;
        if (!(statusBarIconView == null || (iconState = this.mIconStates.get(statusBarIconView)) == null)) {
            iconState.xTranslation = ((float) (this.mIsolatedIconLocation.left - this.mAbsolutePosition[0])) - (((1.0f - this.mIsolatedIcon.getIconScale()) * ((float) this.mIsolatedIcon.getWidth())) / 2.0f);
            iconState.visibleState = 0;
        }
    }

    private float getLayoutEnd() {
        return ((float) getActualWidth()) - getActualPaddingEnd();
    }

    private float getActualPaddingEnd() {
        float f = this.mActualPaddingEnd;
        return f == -2.14748365E9f ? (float) getPaddingEnd() : f;
    }

    private float getActualPaddingStart() {
        float f = this.mActualPaddingStart;
        return f == -2.14748365E9f ? (float) getPaddingStart() : f;
    }

    public void setIsStaticLayout(boolean z) {
        this.mIsStaticLayout = z;
    }

    public void setActualLayoutWidth(int i) {
        this.mActualLayoutWidth = i;
    }

    public void setActualPaddingEnd(float f) {
        this.mActualPaddingEnd = f;
    }

    public void setActualPaddingStart(float f) {
        this.mActualPaddingStart = f;
    }

    public int getActualWidth() {
        int i = this.mActualLayoutWidth;
        return i == Integer.MIN_VALUE ? getWidth() : i;
    }

    public int getFinalTranslationX() {
        float f;
        if (this.mLastVisibleIconState == null) {
            return 0;
        }
        if (isLayoutRtl()) {
            f = ((float) getWidth()) - this.mLastVisibleIconState.xTranslation;
        } else {
            f = this.mLastVisibleIconState.xTranslation + ((float) this.mIconSize);
        }
        return Math.min(getWidth(), (int) f);
    }

    private float getMaxOverflowStart() {
        return getLayoutEnd() - ((float) this.mOverflowWidth);
    }

    public void setChangingViewPositions(boolean z) {
        this.mChangingViewPositions = z;
    }

    public void setDark(boolean z, boolean z2, long j) {
        this.mDark = z;
        this.mDisallowNextAnimation |= !z2;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof StatusBarIconView) {
                ((StatusBarIconView) childAt).setDark(z, z2, j);
            }
        }
    }

    public IconState getIconState(StatusBarIconView statusBarIconView) {
        return this.mIconStates.get(statusBarIconView);
    }

    public void setSpeedBumpIndex(int i) {
        this.mSpeedBumpIndex = i;
    }

    public void setOpenedAmount(float f) {
        this.mOpenedAmount = f;
    }

    public boolean hasOverflow() {
        return this.mNumDots > 0;
    }

    public boolean hasPartialOverflow() {
        int i = this.mNumDots;
        return i > 0 && i < 1;
    }

    public int getPartialOverflowExtraPadding() {
        if (!hasPartialOverflow()) {
            return 0;
        }
        int i = (1 - this.mNumDots) * (this.mStaticDotDiameter + this.mDotPadding);
        return getFinalTranslationX() + i > getWidth() ? getWidth() - getFinalTranslationX() : i;
    }

    public int getNoOverflowExtraPadding() {
        if (this.mNumDots != 0) {
            return 0;
        }
        int i = this.mOverflowWidth;
        return getFinalTranslationX() + i > getWidth() ? getWidth() - getFinalTranslationX() : i;
    }

    public void setAnimationsEnabled(boolean z) {
        if (!z && this.mAnimationsEnabled) {
            for (int i = 0; i < getChildCount(); i++) {
                View childAt = getChildAt(i);
                IconState iconState = this.mIconStates.get(childAt);
                if (iconState != null) {
                    iconState.cancelAnimations(childAt);
                    iconState.applyToView(childAt);
                }
            }
        }
        this.mAnimationsEnabled = z;
    }

    public void setReplacingIcons(ArrayMap<String, ArrayList<StatusBarIcon>> arrayMap) {
        this.mReplacingIcons = arrayMap;
    }

    public void showIconIsolated(StatusBarIconView statusBarIconView, boolean z) {
        if (z) {
            this.mIsolatedIconForAnimation = statusBarIconView != null ? statusBarIconView : this.mIsolatedIcon;
        }
        this.mIsolatedIcon = statusBarIconView;
        updateState();
    }

    public void setIsolatedIconLocation(Rect rect, boolean z) {
        this.mIsolatedIconLocation = rect;
        if (z) {
            updateState();
        }
    }

    public class IconState extends ViewState {
        public float clampedAppearAmount = 1.0f;
        public int customTransformHeight = Integer.MIN_VALUE;
        public float iconAppearAmount = 1.0f;
        public int iconColor = 0;
        public boolean isLastExpandIcon;
        public boolean justAdded = true;
        private boolean justReplaced;
        public boolean needsCannedAnimation;
        public boolean noAnimations;
        public boolean translateContent;
        public boolean useFullTransitionAmount;
        public boolean useLinearTransitionAmount;
        public int visibleState;

        public IconState() {
        }

        /* JADX WARNING: Removed duplicated region for block: B:37:0x0084  */
        /* JADX WARNING: Removed duplicated region for block: B:42:0x00cf  */
        /* JADX WARNING: Removed duplicated region for block: B:54:0x0118  */
        @Override // com.android.systemui.statusbar.notification.stack.ViewState
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void applyToView(android.view.View r13) {
            /*
            // Method dump skipped, instructions count: 374
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NotificationIconContainer.IconState.applyToView(android.view.View):void");
        }

        public boolean hasCustomTransformHeight() {
            return this.isLastExpandIcon && this.customTransformHeight != Integer.MIN_VALUE;
        }

        @Override // com.android.systemui.statusbar.notification.stack.ViewState
        public void initFrom(View view) {
            super.initFrom(view);
            if (view instanceof StatusBarIconView) {
                this.iconColor = ((StatusBarIconView) view).getStaticDrawableColor();
            }
        }
    }
}
