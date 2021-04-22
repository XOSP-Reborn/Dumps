package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.android.systemui.C0005R$dimen;
import com.android.systemui.C0010R$layout;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class PagedTileLayout extends ViewPager implements QSPanel.QSTileLayout {
    private static final Interpolator SCROLL_CUBIC = $$Lambda$PagedTileLayout$fHkBmUM3caZV4_eDd9apVT7Ho.INSTANCE;
    private final PagerAdapter mAdapter = new PagerAdapter() {
        /* class com.android.systemui.qs.PagedTileLayout.AnonymousClass3 */

        @Override // androidx.viewpager.widget.PagerAdapter
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        @Override // androidx.viewpager.widget.PagerAdapter
        public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
            viewGroup.removeView((View) obj);
            PagedTileLayout.this.updateListening();
        }

        @Override // androidx.viewpager.widget.PagerAdapter
        public Object instantiateItem(ViewGroup viewGroup, int i) {
            if (PagedTileLayout.this.isLayoutRtl()) {
                i = (PagedTileLayout.this.mPages.size() - 1) - i;
            }
            ViewGroup viewGroup2 = (ViewGroup) PagedTileLayout.this.mPages.get(i);
            if (viewGroup2.getParent() != null) {
                viewGroup.removeView(viewGroup2);
            }
            viewGroup.addView(viewGroup2);
            PagedTileLayout.this.updateListening();
            return viewGroup2;
        }

        @Override // androidx.viewpager.widget.PagerAdapter
        public int getCount() {
            return PagedTileLayout.this.mPages.size();
        }
    };
    private AnimatorSet mBounceAnimatorSet;
    private final Rect mClippingRect;
    private boolean mDistributeTiles = false;
    private int mHorizontalClipBound;
    private float mLastExpansion;
    private int mLastMaxHeight = -1;
    private int mLayoutDirection;
    private int mLayoutOrientation;
    private boolean mListening;
    private final ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        /* class com.android.systemui.qs.PagedTileLayout.AnonymousClass2 */

        @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageSelected(int i) {
            PagedTileLayout.this.updateSelected();
            if (PagedTileLayout.this.mPageIndicator != null && PagedTileLayout.this.mPageListener != null) {
                PageListener pageListener = PagedTileLayout.this.mPageListener;
                boolean z = false;
                if (!PagedTileLayout.this.isLayoutRtl() ? i == 0 : i == PagedTileLayout.this.mPages.size() - 1) {
                    z = true;
                }
                pageListener.onPageChanged(z);
            }
        }

        @Override // androidx.viewpager.widget.ViewPager.OnPageChangeListener
        public void onPageScrolled(int i, float f, int i2) {
            if (PagedTileLayout.this.mPageIndicator != null) {
                PagedTileLayout.this.mPageIndicatorPosition = ((float) i) + f;
                PagedTileLayout.this.mPageIndicator.setLocation(PagedTileLayout.this.mPageIndicatorPosition);
                if (PagedTileLayout.this.mPageListener != null) {
                    PageListener pageListener = PagedTileLayout.this.mPageListener;
                    boolean z = true;
                    if (i2 != 0 || (!PagedTileLayout.this.isLayoutRtl() ? i != 0 : i != PagedTileLayout.this.mPages.size() - 1)) {
                        z = false;
                    }
                    pageListener.onPageChanged(z);
                }
            }
        }
    };
    private PageIndicator mPageIndicator;
    private float mPageIndicatorPosition;
    private PageListener mPageListener;
    private int mPageToRestore = -1;
    private final ArrayList<TilePage> mPages = new ArrayList<>();
    private Scroller mScroller;
    private final ArrayList<QSPanel.TileRecord> mTiles = new ArrayList<>();

    public interface PageListener {
        void onPageChanged(boolean z);
    }

    static /* synthetic */ float lambda$static$0(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public PagedTileLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScroller = new Scroller(context, SCROLL_CUBIC);
        setAdapter(this.mAdapter);
        setOnPageChangeListener(this.mOnPageChangeListener);
        setCurrentItem(0, false);
        this.mLayoutOrientation = getResources().getConfiguration().orientation;
        this.mLayoutDirection = getLayoutDirection();
        this.mClippingRect = new Rect();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void saveInstanceState(Bundle bundle) {
        bundle.putInt("current_page", getCurrentItem());
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void restoreInstanceState(Bundle bundle) {
        this.mPageToRestore = bundle.getInt("current_page", -1);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = this.mLayoutOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mLayoutOrientation = i2;
            setCurrentItem(0, false);
            this.mPageToRestore = 0;
        }
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mLayoutDirection != i) {
            this.mLayoutDirection = i;
            setAdapter(this.mAdapter);
            setCurrentItem(0, false);
            this.mPageToRestore = 0;
        }
    }

    @Override // androidx.viewpager.widget.ViewPager
    public void setCurrentItem(int i, boolean z) {
        if (isLayoutRtl()) {
            i = (this.mPages.size() - 1) - i;
        }
        super.setCurrentItem(i, z);
    }

    private int getCurrentPageNumber() {
        int currentItem = getCurrentItem();
        return this.mLayoutDirection == 1 ? (this.mPages.size() - 1) - currentItem : currentItem;
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setListening(boolean z) {
        if (this.mListening != z) {
            this.mListening = z;
            updateListening();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateListening() {
        Iterator<TilePage> it = this.mPages.iterator();
        while (it.hasNext()) {
            TilePage next = it.next();
            next.setListening(next.getParent() == null ? false : this.mListening);
        }
    }

    @Override // androidx.viewpager.widget.ViewPager
    public void computeScroll() {
        if (this.mScroller.isFinished() || !this.mScroller.computeScrollOffset()) {
            if (isFakeDragging()) {
                endFakeDrag();
                this.mBounceAnimatorSet.start();
                setOffscreenPageLimit(1);
            }
            super.computeScroll();
            return;
        }
        fakeDragBy((float) (getScrollX() - this.mScroller.getCurrX()));
        postInvalidateOnAnimation();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(C0010R$layout.qs_paged_page, (ViewGroup) this, false));
        this.mAdapter.notifyDataSetChanged();
    }

    public void setPageIndicator(PageIndicator pageIndicator) {
        this.mPageIndicator = pageIndicator;
        this.mPageIndicator.setNumPages(this.mPages.size());
        this.mPageIndicator.setLocation(this.mPageIndicatorPosition);
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getOffsetTop(QSPanel.TileRecord tileRecord) {
        ViewGroup viewGroup = (ViewGroup) tileRecord.tileView.getParent();
        if (viewGroup == null) {
            return 0;
        }
        return viewGroup.getTop() + getTop();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void addTile(QSPanel.TileRecord tileRecord) {
        this.mTiles.add(tileRecord);
        this.mDistributeTiles = true;
        requestLayout();
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void removeTile(QSPanel.TileRecord tileRecord) {
        if (this.mTiles.remove(tileRecord)) {
            this.mDistributeTiles = true;
            requestLayout();
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public void setExpansion(float f) {
        this.mLastExpansion = f;
        updateSelected();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateSelected() {
        float f = this.mLastExpansion;
        if (f <= 0.0f || f >= 1.0f) {
            boolean z = this.mLastExpansion == 1.0f;
            setImportantForAccessibility(4);
            int currentPageNumber = getCurrentPageNumber();
            int i = 0;
            while (i < this.mPages.size()) {
                this.mPages.get(i).setSelected(i == currentPageNumber ? z : false);
                i++;
            }
            setImportantForAccessibility(0);
        }
    }

    public void setPageListener(PageListener pageListener) {
        this.mPageListener = pageListener;
    }

    private void distributeTiles() {
        emptyAndInflateOrRemovePages();
        int maxTiles = this.mPages.get(0).maxTiles();
        int size = this.mTiles.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            QSPanel.TileRecord tileRecord = this.mTiles.get(i2);
            if (this.mPages.get(i).mRecords.size() == maxTiles) {
                i++;
            }
            this.mPages.get(i).addTile(tileRecord);
        }
    }

    private void emptyAndInflateOrRemovePages() {
        int size = this.mTiles.size();
        int max = Math.max(size / this.mPages.get(0).maxTiles(), 1);
        if (size > this.mPages.get(0).maxTiles() * max) {
            max++;
        }
        int size2 = this.mPages.size();
        for (int i = 0; i < size2; i++) {
            this.mPages.get(i).removeAllViews();
        }
        if (size2 != max) {
            while (this.mPages.size() < max) {
                this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(C0010R$layout.qs_paged_page, (ViewGroup) this, false));
            }
            while (this.mPages.size() > max) {
                ArrayList<TilePage> arrayList = this.mPages;
                arrayList.remove(arrayList.size() - 1);
            }
            this.mPageIndicator.setNumPages(this.mPages.size());
            setAdapter(this.mAdapter);
            this.mAdapter.notifyDataSetChanged();
            int i2 = this.mPageToRestore;
            if (i2 != -1) {
                setCurrentItem(i2, false);
                this.mPageToRestore = -1;
            }
        }
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public boolean updateResources() {
        this.mHorizontalClipBound = getContext().getResources().getDimensionPixelSize(C0005R$dimen.notification_side_paddings);
        setPadding(0, 0, 0, getContext().getResources().getDimensionPixelSize(C0005R$dimen.qs_paged_tile_layout_padding_bottom));
        boolean z = false;
        for (int i = 0; i < this.mPages.size(); i++) {
            z |= this.mPages.get(i).updateResources();
        }
        if (z) {
            this.mDistributeTiles = true;
            requestLayout();
        }
        return z;
    }

    /* access modifiers changed from: protected */
    @Override // androidx.viewpager.widget.ViewPager
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        Rect rect = this.mClippingRect;
        int i5 = this.mHorizontalClipBound;
        rect.set(i5, 0, (i3 - i) - i5, i4 - i2);
        setClipBounds(this.mClippingRect);
    }

    /* access modifiers changed from: protected */
    @Override // androidx.viewpager.widget.ViewPager
    public void onMeasure(int i, int i2) {
        int size = this.mTiles.size();
        if (this.mDistributeTiles || this.mLastMaxHeight != View.MeasureSpec.getSize(i2)) {
            this.mLastMaxHeight = View.MeasureSpec.getSize(i2);
            if (this.mPages.get(0).updateMaxRows(i2, size) || this.mDistributeTiles) {
                this.mDistributeTiles = false;
                distributeTiles();
            }
            int i3 = this.mPages.get(0).mRows;
            for (int i4 = 0; i4 < this.mPages.size(); i4++) {
                this.mPages.get(i4).mRows = i3;
            }
        }
        super.onMeasure(i, i2);
        int childCount = getChildCount();
        int i5 = 0;
        for (int i6 = 0; i6 < childCount; i6++) {
            int measuredHeight = getChildAt(i6).getMeasuredHeight();
            if (measuredHeight > i5) {
                i5 = measuredHeight;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), i5 + getPaddingBottom());
    }

    public int getColumnCount() {
        if (this.mPages.size() == 0) {
            return 0;
        }
        return this.mPages.get(0).mColumns;
    }

    @Override // com.android.systemui.qs.QSPanel.QSTileLayout
    public int getNumVisibleTiles() {
        if (this.mPages.size() == 0) {
            return 0;
        }
        return this.mPages.get(getCurrentPageNumber()).mRecords.size();
    }

    public void startTileReveal(Set<String> set, final Runnable runnable) {
        if (!set.isEmpty() && this.mPages.size() >= 2 && getScrollX() == 0 && beginFakeDrag()) {
            int size = this.mPages.size() - 1;
            ArrayList arrayList = new ArrayList();
            Iterator<QSPanel.TileRecord> it = this.mPages.get(size).mRecords.iterator();
            while (it.hasNext()) {
                QSPanel.TileRecord next = it.next();
                if (set.contains(next.tile.getTileSpec())) {
                    arrayList.add(setupBounceAnimator(next.tileView, arrayList.size()));
                }
            }
            if (arrayList.isEmpty()) {
                endFakeDrag();
                return;
            }
            this.mBounceAnimatorSet = new AnimatorSet();
            this.mBounceAnimatorSet.playTogether(arrayList);
            this.mBounceAnimatorSet.addListener(new AnimatorListenerAdapter() {
                /* class com.android.systemui.qs.PagedTileLayout.AnonymousClass1 */

                public void onAnimationEnd(Animator animator) {
                    PagedTileLayout.this.mBounceAnimatorSet = null;
                    runnable.run();
                }
            });
            setOffscreenPageLimit(size);
            int width = getWidth() * size;
            Scroller scroller = this.mScroller;
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (isLayoutRtl()) {
                width = -width;
            }
            scroller.startScroll(scrollX, scrollY, width, 0, 750);
            postInvalidateOnAnimation();
        }
    }

    private static Animator setupBounceAnimator(View view, int i) {
        view.setAlpha(0.0f);
        view.setScaleX(0.0f);
        view.setScaleY(0.0f);
        ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f), PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f), PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f));
        ofPropertyValuesHolder.setDuration(450L);
        ofPropertyValuesHolder.setStartDelay((long) (i * 85));
        ofPropertyValuesHolder.setInterpolator(new OvershootInterpolator(1.3f));
        return ofPropertyValuesHolder;
    }

    public static class TilePage extends TileLayout {
        public TilePage(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public int maxTiles() {
            return Math.max(this.mColumns * this.mRows, 1);
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout, com.android.systemui.qs.TileLayout
        public boolean updateResources() {
            int dimensionPixelSize = getContext().getResources().getDimensionPixelSize(C0005R$dimen.notification_side_paddings);
            setPadding(dimensionPixelSize, 0, dimensionPixelSize, 0);
            return super.updateResources();
        }
    }
}
